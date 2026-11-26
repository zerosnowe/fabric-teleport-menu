package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import com.example.server.model.BoxShopNames;
import com.example.server.model.BoxShopPoint;
import com.example.server.model.ShopGood;
import com.example.server.model.ShopPoint;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerState.BOX_SHOPS;
import static com.example.server.ServerState.BOX_SHOP_HOLOGRAMS;
import static com.example.server.ServerState.BOX_SHOP_SHOP_IDS;
import static com.example.server.ServerState.PLAYER_SHOPS;
import static com.example.server.service.PersistenceService.persistDataIfPossible;
import static com.example.server.service.ShopService.findAnyShop;
import static com.example.server.service.ShopService.findOwnedShop;

/**
 * Box shop domain: placement block, legacy display cleanup and HUD queries.
 */
public final class BoxShopService {
	public static final String DEFAULT_BOX_SHOP_NAME = BoxShopNames.DEFAULT_NAME;
	private static final String BOX_SHOP_HOLOGRAM_TAG = "teleportmenu_boxshop_holo";

	private BoxShopService() {
	}

	public static String sanitizeBoxShopName(String rawName) {
		return BoxShopNames.sanitize(rawName);
	}

	public static void handleRenameBoxShop(ServerPlayer source, UUID boxShopId, String rawName) {
		BoxShopPoint boxShop = BOX_SHOPS.get(boxShopId);
		if (boxShop == null || !boxShop.ownerId().equals(source.getUUID())) {
			sendError(source, "message.teleportmenu.boxshop_not_found");
			return;
		}

		ShopPoint shop = findOwnedShop(source, boxShop.shopId());
		if (shop == null) {
			sendError(source, "message.teleportmenu.shop_not_found");
			return;
		}

		String name = sanitizeBoxShopName(rawName);

		shop.setName(name);
		BOX_SHOPS.put(boxShop.id(), boxShop.withName(name, true));
		updateBoxShopBlock(source.server, boxShop.id());
		refreshBoxShopHologram(source.server, BOX_SHOPS.get(boxShop.id()));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.boxshop_renamed", name);
	}

	public static BoxShopPoint findBoxShopByShopId(UUID shopId) {
		if (shopId == null) {
			return null;
		}
		for (BoxShopPoint boxShop : BOX_SHOPS.values()) {
			if (boxShop.shopId().equals(shopId)) {
				return boxShop;
			}
		}
		return null;
	}

	public static BoxShopPoint findBoxShopByBlock(ResourceLocation dimensionId, BlockPos pos) {
		if (dimensionId == null || pos == null) {
			return null;
		}
		for (BoxShopPoint point : BOX_SHOPS.values()) {
			if (!point.dimensionId().equals(dimensionId)) {
				continue;
			}
			if (point.x() == pos.getX() && point.y() == pos.getY() && point.z() == pos.getZ()) {
				return point;
			}
		}
		return null;
	}

	public static BoxShopPoint findOwnedBoxShopAtFeet(ServerPlayer player) {
		ResourceLocation dimensionId = player.level().dimension().location();
		BoxShopPoint point = findBoxShopByBlock(dimensionId, player.blockPosition());
		if (point != null && point.ownerId().equals(player.getUUID())) {
			return point;
		}

		point = findBoxShopByBlock(dimensionId, player.blockPosition().below());
		if (point != null && point.ownerId().equals(player.getUUID())) {
			return point;
		}

		return null;
	}

	private static net.minecraft.world.entity.Entity findEntityByUuid(MinecraftServer server, UUID entityId) {
		if (server == null || entityId == null) {
			return null;
		}
		for (ServerLevel level : server.getAllLevels()) {
			net.minecraft.world.entity.Entity found = level.getEntity(entityId);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static String boxShopHologramTag(UUID boxShopId) {
		return "teleportmenu_boxshop_" + boxShopId.toString().replace("-", "");
	}

	private static double[] boxShopHologramPos(BoxShopPoint boxShop, ServerLevel level) {
		BlockPos pos = boxShop.blockPos();
		Direction facing = Direction.SOUTH;
		if (level != null) {
			var state = level.getBlockState(pos);
			if (state.is(Blocks.ENDER_CHEST) && state.hasProperty(EnderChestBlock.FACING)) {
				facing = state.getValue(EnderChestBlock.FACING);
			}
		}

		double x = pos.getX() + 0.5D + facing.getStepX() * 0.74D;
		double y = pos.getY() + 0.92D;
		double z = pos.getZ() + 0.5D + facing.getStepZ() * 0.74D;
		return new double[] { x, y, z };
	}

	private static Display.TextDisplay findBoxShopHologram(MinecraftServer server, BoxShopPoint boxShop, ServerLevel level) {
		if (server == null || boxShop == null || level == null) {
			return null;
		}

		UUID mappedId = BOX_SHOP_HOLOGRAMS.get(boxShop.id());
		if (mappedId != null) {
			net.minecraft.world.entity.Entity entity = findEntityByUuid(server, mappedId);
			if (entity instanceof Display.TextDisplay display && !display.isRemoved()) {
				return display;
			}
			BOX_SHOP_HOLOGRAMS.remove(boxShop.id());
		}

		BlockPos pos = boxShop.blockPos();
		AABB scanArea = new AABB(pos).inflate(1.5D, 3.0D, 1.5D);
		String tag = boxShopHologramTag(boxShop.id());
		List<Display.TextDisplay> displays = level.getEntitiesOfClass(
			Display.TextDisplay.class,
			scanArea,
			display -> display.getTags().contains(BOX_SHOP_HOLOGRAM_TAG) && display.getTags().contains(tag)
		);
		if (displays.isEmpty()) {
			return null;
		}

		Display.TextDisplay first = displays.get(0);
		BOX_SHOP_HOLOGRAMS.put(boxShop.id(), first.getUUID());
		for (int i = 1; i < displays.size(); i++) {
			displays.get(i).discard();
		}
		return first;
	}

	private static Display.TextDisplay dedupeBoxShopHologramsAtPos(BoxShopPoint boxShop, ServerLevel level, Display.TextDisplay preferred) {
		if (boxShop == null || level == null) {
			return preferred;
		}

		double[] hologramPos = boxShopHologramPos(boxShop, level);
		double x = hologramPos[0];
		double y = hologramPos[1];
		double z = hologramPos[2];
		AABB exactArea = new AABB(
			x - 0.45D, y - 0.9D, z - 0.45D,
			x + 0.45D, y + 0.9D, z + 0.45D
		);
		String tag = boxShopHologramTag(boxShop.id());
		List<Display.TextDisplay> nearby = level.getEntitiesOfClass(
			Display.TextDisplay.class,
			exactArea,
			entity -> entity.getTags().contains(BOX_SHOP_HOLOGRAM_TAG)
		);
		if (nearby.isEmpty()) {
			return preferred;
		}

		Display.TextDisplay keeper = preferred;
		if (keeper == null || keeper.isRemoved()) {
			for (Display.TextDisplay candidate : nearby) {
				if (candidate.getTags().contains(tag)) {
					keeper = candidate;
					break;
				}
			}
			if (keeper == null) {
				keeper = nearby.get(0);
			}
		}

		for (Display.TextDisplay display : nearby) {
			if (display != keeper) {
				display.discard();
			}
		}

		if (!keeper.getTags().contains(BOX_SHOP_HOLOGRAM_TAG)) {
			keeper.addTag(BOX_SHOP_HOLOGRAM_TAG);
		}
		if (!keeper.getTags().contains(tag)) {
			keeper.addTag(tag);
		}
		BOX_SHOP_HOLOGRAMS.put(boxShop.id(), keeper.getUUID());
		return keeper;
	}

	private static void cleanupBoxShopDisplayArtifacts(BoxShopPoint boxShop, ServerLevel level, @Nullable UUID keeperId) {
		if (boxShop == null || level == null) {
			return;
		}

		String tag = boxShopHologramTag(boxShop.id());
		String shopName = sanitizeBoxShopName(boxShop.name());
		BlockPos pos = boxShop.blockPos();
		AABB area = new AABB(pos).inflate(2.0D, 2.2D, 2.0D);

		for (Display display : level.getEntitiesOfClass(Display.class, area, entity -> true)) {
			if (keeperId != null && keeperId.equals(display.getUUID())) {
				continue;
			}

			boolean tagged = display.getTags().contains(BOX_SHOP_HOLOGRAM_TAG) || display.getTags().contains(tag);
			boolean likelyOldText = false;
			if (display instanceof Display.TextDisplay textDisplay) {
				String raw = textDisplay.getText().getString();
				likelyOldText = raw.contains("商品数量") || raw.contains("Goods") || raw.startsWith(shopName);
			}

			if (tagged || likelyOldText) {
				display.discard();
			}
		}
	}

	public static boolean isLegacyBoxShopDisplay(Display display) {
		if (display == null || display.isRemoved()) {
			return false;
		}
		for (String tag : display.getTags()) {
			if (BOX_SHOP_HOLOGRAM_TAG.equals(tag) || tag.startsWith("teleportmenu_boxshop_")) {
				return true;
			}
		}
		if (display instanceof Display.TextDisplay textDisplay) {
			String raw = textDisplay.getText().getString();
			if (raw == null) {
				return false;
			}
			return raw.contains("商品数量:") || raw.contains("Goods:");
		}
		return false;
	}

	public static void cleanupLegacyBoxShopDisplaysInChunk(ServerLevel level, LevelChunk chunk) {
		if (level == null || chunk == null) {
			return;
		}
		AABB area = new AABB(
			chunk.getPos().getMinBlockX(), level.getMinBuildHeight(), chunk.getPos().getMinBlockZ(),
			chunk.getPos().getMaxBlockX() + 1D, level.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ() + 1D
		);
		for (Display.TextDisplay textDisplay : level.getEntitiesOfClass(Display.TextDisplay.class, area, BoxShopService::isLegacyBoxShopDisplay)) {
			textDisplay.discard();
		}
	}

	public static void cleanupAllLegacyBoxShopDisplays(MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (Display.TextDisplay textDisplay : level.getEntitiesOfClass(
				Display.TextDisplay.class,
				new AABB(-3.0E7D, level.getMinBuildHeight(), -3.0E7D, 3.0E7D, level.getMaxBuildHeight(), 3.0E7D),
				BoxShopService::isLegacyBoxShopDisplay
			)) {
				textDisplay.discard();
			}
		}
	}

	private static void removeBoxShopHologram(MinecraftServer server, BoxShopPoint boxShop) {
		if (server == null || boxShop == null) {
			return;
		}

		UUID mappedId = BOX_SHOP_HOLOGRAMS.remove(boxShop.id());
		if (mappedId != null) {
			net.minecraft.world.entity.Entity entity = findEntityByUuid(server, mappedId);
			if (entity instanceof Display.TextDisplay display) {
				display.discard();
			}
		}

		ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, boxShop.dimensionId());
		ServerLevel level = server.getLevel(dimensionKey);
		if (level == null) {
			return;
		}

		AABB scanArea = new AABB(boxShop.blockPos()).inflate(1.5D, 3.0D, 1.5D);
		String tag = boxShopHologramTag(boxShop.id());
		for (Display.TextDisplay display : level.getEntitiesOfClass(
			Display.TextDisplay.class,
			scanArea,
			entity -> entity.getTags().contains(BOX_SHOP_HOLOGRAM_TAG) && entity.getTags().contains(tag)
		)) {
			display.discard();
		}
	}

	public static void refreshBoxShopHologram(MinecraftServer server, BoxShopPoint boxShop) {
		// Legacy server-side floating text is retired.
		// We keep this hook to aggressively clean residual display entities created by older versions.
		removeBoxShopHologram(server, boxShop);
		if (server == null || boxShop == null) {
			return;
		}
		ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, boxShop.dimensionId());
		ServerLevel level = server.getLevel(dimensionKey);
		if (level != null) {
			cleanupBoxShopDisplayArtifacts(boxShop, level, null);
		}
	}

	public static void ensureBoxShopBlock(MinecraftServer server, BoxShopPoint boxShop) {
		if (server == null || boxShop == null) {
			return;
		}
		ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, boxShop.dimensionId());
		ServerLevel level = server.getLevel(dimensionKey);
		if (level == null) {
			return;
		}
		BlockPos pos = new BlockPos(boxShop.x(), boxShop.y(), boxShop.z());
		if (level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
			return;
		}
		if (!level.getBlockState(pos).canBeReplaced()) {
			return;
		}
		level.setBlockAndUpdate(pos, Blocks.ENDER_CHEST.defaultBlockState());
	}

	public static void updateBoxShopBlock(MinecraftServer server, UUID boxShopId) {
		BoxShopPoint boxShop = BOX_SHOPS.get(boxShopId);
		if (boxShop == null) {
			return;
		}
		ensureBoxShopBlock(server, boxShop);
	}

	public static void removeBoxShop(MinecraftServer server, BoxShopPoint boxShop) {
		removeBoxShopHologram(server, boxShop);
		if (server != null) {
			ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, boxShop.dimensionId());
			ServerLevel level = server.getLevel(dimensionKey);
			if (level != null) {
				BlockPos pos = boxShop.blockPos();
				if (level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				}
			}
		}

		List<ShopPoint> ownerShops = PLAYER_SHOPS.get(boxShop.ownerId());
		if (ownerShops != null) {
			ownerShops.removeIf(shop -> shop.id().equals(boxShop.shopId()));
		}
		BOX_SHOP_SHOP_IDS.remove(boxShop.shopId());
		BOX_SHOPS.remove(boxShop.id());
		persistDataIfPossible();
	}

	public static void openBoxShop(ServerPlayer player, BoxShopPoint boxShop) {
		ShopPoint shop = findAnyShop(boxShop.shopId());
		if (shop == null) {
			sendError(player, "message.teleportmenu.shop_not_found");
			return;
		}

		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.OpenBoxShopPayload.TYPE)) {
			sendError(player, "message.teleportmenu.client_missing");
			return;
		}

		List<TeleportMenuPayloads.ShopGoodTarget> goods = shop.goods().stream()
			.map(good -> new TeleportMenuPayloads.ShopGoodTarget(good.itemId(), good.name(), good.iconItemId(), good.stock(), good.price()))
			.toList();
		boolean owner = shop.ownerId().equals(player.getUUID());

		ServerPlayNetworking.send(player, new TeleportMenuPayloads.OpenBoxShopPayload(
			new TeleportMenuPayloads.BoxShopTarget(
				boxShop.id(),
				shop.id(),
				shop.ownerId(),
				shop.ownerName(),
				boxShop.name(),
				owner,
				owner && !boxShop.named(),
				goods
			)
		));
	}

	public static void handleQueryBoxShopHud(ServerPlayer player, int x, int y, int z) {
		if (player == null || player.server == null) {
			return;
		}
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.BoxShopHudPayload.TYPE)) {
			return;
		}

		BlockPos pos = new BlockPos(x, y, z);
		BlockPos playerPos = player.blockPosition();
		if (playerPos.distSqr(pos) > 144D) {
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.BoxShopHudPayload(false, "", 0));
			return;
		}

		BoxShopPoint boxShop = findBoxShopByBlock(player.level().dimension().location(), pos);
		if (boxShop == null) {
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.BoxShopHudPayload(false, "", 0));
			return;
		}

		ShopPoint shop = findAnyShop(boxShop.shopId());
		if (shop == null) {
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.BoxShopHudPayload(false, "", 0));
			return;
		}

		int goodsCount = 0;
		for (ShopGood good : shop.goods()) {
			goodsCount += Math.max(0, good.stock());
		}

		ServerPlayNetworking.send(player, new TeleportMenuPayloads.BoxShopHudPayload(
			true,
			sanitizeBoxShopName(boxShop.name()),
			goodsCount
		));
	}
}
