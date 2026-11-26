package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import com.example.server.model.ShopGood;
import com.example.server.model.ShopPoint;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerState.BOX_SHOP_SHOP_IDS;
import static com.example.server.ServerState.PLAYER_SHOPS;
import static com.example.server.ServerState.PLAYER_SHOP_LIMITS;
import static com.example.server.service.EconomyService.economyAdd;
import static com.example.server.service.EconomyService.economyFormat;
import static com.example.server.service.EconomyService.economyGetMoney;
import static com.example.server.service.EconomyService.economySubtract;
import static com.example.server.service.EconomyService.isEconomyReady;
import static com.example.server.service.EconomyService.syncEconomyState;
import static com.example.server.service.PersistenceService.persistDataIfPossible;

/**
 * Normal shop domain: creation, goods, purchases and shop limits.
 */
public final class ShopService {
	public static final int DEFAULT_SHOP_LIMIT = 2;
	private static final int SHOP_LIMIT_EXPAND_COST = 100;
	private static final int SHOP_DELETE_COST = 10;

	private ShopService() {
	}

	public static void handleCreateShop(ServerPlayer source, String rawName) {
		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.shop_name_empty");
			return;
		}
		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<ShopPoint> shops = PLAYER_SHOPS.computeIfAbsent(source.getUUID(), key -> new ArrayList<>());
		int limit = PLAYER_SHOP_LIMITS.getOrDefault(source.getUUID(), DEFAULT_SHOP_LIMIT);
		int ownNormalShopCount = countOwnedNormalShops(source.getUUID());
		if (ownNormalShopCount >= limit) {
			sendError(source, "message.teleportmenu.shop_limit_reached", ownNormalShopCount, limit);
			return;
		}

		shops.add(new ShopPoint(UUID.randomUUID(), source.getUUID(), source.getName().getString(), name, "minecraft:chest", new ArrayList<>()));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.shop_created", name);
		syncEconomyState(source);
	}

	public static void handleExpandShopLimit(ServerPlayer source) {
		if (!isEconomyReady()) {
			sendError(source, "message.teleportmenu.economy_missing");
			return;
		}

		try {
			double money = economyGetMoney(source.getName().getString());
			if (money < SHOP_LIMIT_EXPAND_COST) {
				sendError(source, "message.teleportmenu.shop_expand_not_enough", economyFormat(SHOP_LIMIT_EXPAND_COST));
				return;
			}
			economySubtract(source.getName().getString(), SHOP_LIMIT_EXPAND_COST);
			int next = PLAYER_SHOP_LIMITS.getOrDefault(source.getUUID(), DEFAULT_SHOP_LIMIT) + 1;
			PLAYER_SHOP_LIMITS.put(source.getUUID(), next);
			persistDataIfPossible();
			sendSuccess(source, "message.teleportmenu.shop_expand_success", next);
			syncEconomyState(source);
		} catch (Exception error) {
			sendError(source, "message.teleportmenu.economy_operation_failed");
		}
	}

	public static void handleDeleteShop(ServerPlayer source, UUID shopId) {
		if (shopId == null) {
			sendError(source, "message.teleportmenu.shop_not_found");
			return;
		}
		if (!isEconomyReady()) {
			sendError(source, "message.teleportmenu.economy_missing");
			return;
		}

		List<ShopPoint> shops = PLAYER_SHOPS.getOrDefault(source.getUUID(), List.of());
		ShopPoint target = shops.stream().filter(shop -> shop.id().equals(shopId)).findFirst().orElse(null);
		if (target == null) {
			sendError(source, "message.teleportmenu.shop_not_found");
			return;
		}
		if (isBoxShopShop(shopId)) {
			sendError(source, "message.teleportmenu.boxshop_manage_only");
			return;
		}

		try {
			double money = economyGetMoney(source.getName().getString());
			if (money < SHOP_DELETE_COST) {
				sendError(source, "message.teleportmenu.shop_delete_not_enough", economyFormat(SHOP_DELETE_COST));
				return;
			}
			economySubtract(source.getName().getString(), SHOP_DELETE_COST);
			shops.removeIf(shop -> shop.id().equals(shopId));
			persistDataIfPossible();
			sendSuccess(source, "message.teleportmenu.shop_deleted", target.name());
			syncEconomyState(source);
		} catch (Exception error) {
			sendError(source, "message.teleportmenu.economy_operation_failed");
		}
	}

	public static void handleRenameShop(ServerPlayer source, UUID shopId, String rawName) {
		ShopPoint shop = findOwnedShop(source, shopId);
		if (shop == null) {
			sendError(source, "message.teleportmenu.shop_not_found");
			return;
		}
		if (isBoxShopShop(shopId)) {
			sendError(source, "message.teleportmenu.boxshop_manage_only");
			return;
		}

		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.shop_name_empty");
			return;
		}
		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		shop.setName(name);
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.shop_renamed", name);
		syncEconomyState(source);
	}

	public static void handleSetShopIcon(ServerPlayer source, UUID shopId, String iconItemId) {
		ShopPoint shop = findOwnedShop(source, shopId);
		if (shop == null) {
			sendError(source, "message.teleportmenu.shop_not_found");
			return;
		}
		if (isBoxShopShop(shopId)) {
			sendError(source, "message.teleportmenu.boxshop_manage_only");
			return;
		}

		String canonical = canonicalShopIcon(iconItemId);
		shop.setIconItemId(canonical);
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.shop_icon_updated");
		syncEconomyState(source);
	}

	public static void handleAddShopGood(ServerPlayer source, UUID shopId, String itemId, int stock, double price) {
		ShopPoint shop = findOwnedShop(source, shopId);
		if (shop == null) {
			sendError(source, "message.teleportmenu.shop_not_found");
			return;
		}

		if (stock <= 0 || !Double.isFinite(price) || price < 0D) {
			sendError(source, "message.teleportmenu.shop_good_invalid");
			return;
		}

		ResourceLocation itemLocation;
		try {
			itemLocation = ResourceLocation.parse(itemId);
		} catch (Exception error) {
			sendError(source, "message.teleportmenu.shop_good_invalid");
			return;
		}
		Item item = BuiltInRegistries.ITEM.getOptional(itemLocation).orElse(null);
		if (item == null || item == Items.AIR) {
			sendError(source, "message.teleportmenu.shop_good_invalid");
			return;
		}

		int available = countItemInInventory(source.getInventory(), item);
		if (available < stock) {
			sendError(source, "message.teleportmenu.shop_good_not_enough", available);
			return;
		}

		removeItemFromInventory(source.getInventory(), item, stock);
		String canonicalId = BuiltInRegistries.ITEM.getKey(item).toString();
		ShopGood good = shop.findGood(canonicalId);
		if (good == null) {
			shop.goods().add(new ShopGood(canonicalId, new ItemStack(item).getHoverName().getString(), canonicalId, stock, price));
		} else {
			good.setStock(good.stock() + stock);
			good.setPrice(price);
		}
		var boxShop = BoxShopService.findBoxShopByShopId(shop.id());
		if (boxShop != null) {
			BoxShopService.refreshBoxShopHologram(source.server, boxShop);
		}

		persistDataIfPossible();
		String formattedPrice;
		try {
			formattedPrice = economyFormat(price);
		} catch (Exception ignored) {
			formattedPrice = String.format(Locale.ROOT, "%.2f", price);
		}
		sendSuccess(source, "message.teleportmenu.shop_good_added", new ItemStack(item).getHoverName(), stock, formattedPrice);
		syncEconomyState(source);
	}

	public static void handleBuyShopGood(ServerPlayer buyer, UUID shopId, String itemId, int amount) {
		if (amount <= 0) {
			sendError(buyer, "message.teleportmenu.shop_buy_invalid_amount");
			return;
		}
		if (!isEconomyReady()) {
			sendError(buyer, "message.teleportmenu.economy_missing");
			return;
		}

		ShopPoint shop = findAnyShop(shopId);
		if (shop == null) {
			sendError(buyer, "message.teleportmenu.shop_not_found");
			return;
		}

		ShopGood good = shop.findGood(itemId);
		if (good == null) {
			sendError(buyer, "message.teleportmenu.shop_good_not_found");
			return;
		}
		if (good.stock() < amount) {
			sendError(buyer, "message.teleportmenu.shop_stock_not_enough", good.stock());
			return;
		}

		double total = good.price() * amount;
		try {
			double money = economyGetMoney(buyer.getName().getString());
			if (money < total) {
				sendError(buyer, "message.teleportmenu.shop_buy_not_enough", economyFormat(total));
				return;
			}

			economySubtract(buyer.getName().getString(), total);
			if (!buyer.getUUID().equals(shop.ownerId())) {
				economyAdd(shop.ownerName(), total);
			}

			good.setStock(good.stock() - amount);
			if (good.stock() <= 0) {
				shop.goods().remove(good);
			}
			var boxShop = BoxShopService.findBoxShopByShopId(shop.id());
			if (boxShop != null) {
				BoxShopService.refreshBoxShopHologram(buyer.server, boxShop);
			}

			Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(good.itemId())).orElse(Items.AIR);
			ItemStack result = new ItemStack(item, amount);
			if (!buyer.getInventory().add(result)) {
				buyer.drop(result, false);
			}

			persistDataIfPossible();
			sendSuccess(buyer, "message.teleportmenu.shop_buy_success", Component.literal(good.name()), amount, economyFormat(total));
			syncEconomyState(buyer);
			ServerPlayer ownerOnline = buyer.server.getPlayerList().getPlayer(shop.ownerId());
			if (ownerOnline != null && !ownerOnline.getUUID().equals(buyer.getUUID())) {
				syncEconomyState(ownerOnline);
			}
		} catch (Exception error) {
			sendError(buyer, "message.teleportmenu.economy_operation_failed");
		}
	}

	public static List<TeleportMenuPayloads.ShopTarget> getAllShopTargets() {
		List<TeleportMenuPayloads.ShopTarget> shops = new ArrayList<>();
		for (List<ShopPoint> ownerShops : PLAYER_SHOPS.values()) {
			for (ShopPoint shop : ownerShops) {
				if (isBoxShopShop(shop.id())) {
					continue;
				}
				List<TeleportMenuPayloads.ShopGoodTarget> goods = shop.goods().stream()
					.map(good -> new TeleportMenuPayloads.ShopGoodTarget(good.itemId(), good.name(), good.iconItemId(), good.stock(), good.price()))
					.toList();
				shops.add(new TeleportMenuPayloads.ShopTarget(shop.id(), shop.ownerId(), shop.ownerName(), shop.name(), shop.iconItemId(), goods));
			}
		}
		return shops;
	}

	public static int countOwnedNormalShops(UUID ownerId) {
		int count = 0;
		for (ShopPoint shop : PLAYER_SHOPS.getOrDefault(ownerId, List.of())) {
			if (!isBoxShopShop(shop.id())) {
				count++;
			}
		}
		return count;
	}

	public static boolean isBoxShopShop(UUID shopId) {
		if (shopId == null) {
			return false;
		}
		return BOX_SHOP_SHOP_IDS.contains(shopId);
	}

	public static ShopPoint findOwnedShop(ServerPlayer owner, UUID shopId) {
		if (shopId == null) {
			return null;
		}
		return PLAYER_SHOPS.getOrDefault(owner.getUUID(), List.of()).stream()
			.filter(shop -> shop.id().equals(shopId))
			.findFirst().orElse(null);
	}

	public static ShopPoint findAnyShop(UUID shopId) {
		if (shopId == null) {
			return null;
		}
		for (List<ShopPoint> ownerShops : PLAYER_SHOPS.values()) {
			for (ShopPoint shop : ownerShops) {
				if (shop.id().equals(shopId)) {
					return shop;
				}
			}
		}
		return null;
	}

	static int countItemInInventory(Inventory inventory, Item item) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	static void removeItemFromInventory(Inventory inventory, Item item, int amount) {
		int remaining = amount;
		for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty() || !stack.is(item)) {
				continue;
			}

			int take = Math.min(remaining, stack.getCount());
			stack.shrink(take);
			remaining -= take;
			if (stack.isEmpty()) {
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
		inventory.setChanged();
	}

	private static String canonicalShopIcon(String iconItemId) {
		return switch (iconItemId == null ? "" : iconItemId) {
			case "minecraft:ender_chest" -> "minecraft:ender_chest";
			case "minecraft:shulker_box" -> "minecraft:shulker_box";
			case "minecraft:barrel" -> "minecraft:barrel";
			default -> "minecraft:chest";
		};
	}
}
