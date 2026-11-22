package com.example.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeleportMenuPayloads {
	private TeleportMenuPayloads() {
	}

	public record PlayerTarget(UUID uuid, String name) {
		public PlayerTarget {
			name = name == null ? "" : name;
		}
	}

	public record HomeTarget(UUID id, String name, int x, int y, int z, String iconItemId) {
		public HomeTarget {
			name = name == null ? "" : name;
			iconItemId = iconItemId == null ? "minecraft:compass" : iconItemId;
		}
	}

	public record MomentEntry(
		UUID id,
		UUID authorId,
		String authorName,
		String content,
		long createdAt,
		int likeCount,
		boolean likedByViewer,
		boolean own
	) {
		public MomentEntry {
			authorName = authorName == null ? "" : authorName;
			content = content == null ? "" : content;
			likeCount = Math.max(0, likeCount);
		}
	}

	public record TerritoryTarget(
		UUID id,
		String name,
		String ownerName,
		int minX,
		int minY,
		int minZ,
		int maxX,
		int maxY,
		int maxZ,
		String iconItemId,
		boolean buildBreakAllowed,
		boolean containerAllowed,
		boolean interactAllowed,
		boolean damageEntitiesAllowed,
		boolean blockUseAllowed,
		List<UUID> whitelistPlayerIds
	) {
		public TerritoryTarget {
			name = name == null ? "" : name;
			ownerName = ownerName == null ? "" : ownerName;
			iconItemId = iconItemId == null ? "minecraft:grass_block" : iconItemId;
			whitelistPlayerIds = List.copyOf(whitelistPlayerIds);
		}
	}

	public record OpenMenuPayload(
		List<PlayerTarget> targets,
		List<HomeTarget> homes,
		List<TerritoryTarget> territories,
		boolean admin,
		List<String> disabledItemIds,
		boolean territorySelectionReady,
		int selectionAx,
		int selectionAy,
		int selectionAz,
		int selectionBx,
		int selectionBy,
		int selectionBz
	) implements CustomPacketPayload {
		public static final Type<OpenMenuPayload> TYPE = new Type<>(id("open_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> STREAM_CODEC =
			CustomPacketPayload.codec(OpenMenuPayload::write, OpenMenuPayload::new);

		public OpenMenuPayload {
			targets = List.copyOf(targets);
			homes = List.copyOf(homes);
			territories = List.copyOf(territories);
			disabledItemIds = List.copyOf(disabledItemIds);
		}

		private OpenMenuPayload(RegistryFriendlyByteBuf buf) {
			this(
				readTargets(buf),
				readHomes(buf),
				readTerritories(buf),
				buf.readBoolean(),
				readDisabledItemIds(buf),
				buf.readBoolean(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt()
			);
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.targets.size());
			for (PlayerTarget target : this.targets) {
				buf.writeUUID(target.uuid());
				buf.writeUtf(target.name(), 64);
			}

			buf.writeVarInt(this.homes.size());
			for (HomeTarget home : this.homes) {
				buf.writeUUID(home.id());
				buf.writeUtf(home.name(), 64);
				buf.writeVarInt(home.x());
				buf.writeVarInt(home.y());
				buf.writeVarInt(home.z());
				buf.writeUtf(home.iconItemId(), 128);
			}

			buf.writeVarInt(this.territories.size());
			for (TerritoryTarget territory : this.territories) {
				buf.writeUUID(territory.id());
				buf.writeUtf(territory.name(), 64);
				buf.writeUtf(territory.ownerName(), 64);
				buf.writeVarInt(territory.minX());
				buf.writeVarInt(territory.minY());
				buf.writeVarInt(territory.minZ());
				buf.writeVarInt(territory.maxX());
				buf.writeVarInt(territory.maxY());
				buf.writeVarInt(territory.maxZ());
				buf.writeUtf(territory.iconItemId(), 128);
				buf.writeBoolean(territory.buildBreakAllowed());
				buf.writeBoolean(territory.containerAllowed());
				buf.writeBoolean(territory.interactAllowed());
				buf.writeBoolean(territory.damageEntitiesAllowed());
				buf.writeBoolean(territory.blockUseAllowed());
				buf.writeVarInt(territory.whitelistPlayerIds().size());
				for (UUID playerId : territory.whitelistPlayerIds()) {
					buf.writeUUID(playerId);
				}
			}

			buf.writeBoolean(this.admin);
			buf.writeVarInt(this.disabledItemIds.size());
			for (String itemId : this.disabledItemIds) {
				buf.writeUtf(itemId, 128);
			}

			buf.writeBoolean(this.territorySelectionReady);
			buf.writeVarInt(this.selectionAx);
			buf.writeVarInt(this.selectionAy);
			buf.writeVarInt(this.selectionAz);
			buf.writeVarInt(this.selectionBx);
			buf.writeVarInt(this.selectionBy);
			buf.writeVarInt(this.selectionBz);
		}

		private static List<PlayerTarget> readTargets(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<PlayerTarget> targets = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				targets.add(new PlayerTarget(buf.readUUID(), buf.readUtf(64)));
			}
			return targets;
		}

		private static List<HomeTarget> readHomes(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<HomeTarget> homes = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				homes.add(new HomeTarget(
					buf.readUUID(),
					buf.readUtf(64),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readUtf(128)
				));
			}
			return homes;
		}

		private static List<TerritoryTarget> readTerritories(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<TerritoryTarget> territories = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				territories.add(new TerritoryTarget(
					buf.readUUID(),
					buf.readUtf(64),
					buf.readUtf(64),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readUtf(128),
					buf.readBoolean(),
					buf.readBoolean(),
					buf.readBoolean(),
					buf.readBoolean(),
					buf.readBoolean(),
					readWhitelistPlayerIds(buf)
				));
			}
			return territories;
		}

		private static List<UUID> readWhitelistPlayerIds(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<UUID> whitelist = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				whitelist.add(buf.readUUID());
			}
			return whitelist;
		}

		private static List<String> readDisabledItemIds(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<String> ids = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				ids.add(buf.readUtf(128));
			}
			return ids;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportRequestPayload(UUID targetId) implements CustomPacketPayload {
		public static final Type<TeleportRequestPayload> TYPE = new Type<>(id("teleport_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(TeleportRequestPayload::write, TeleportRequestPayload::new);

		private TeleportRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.targetId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportHomeRequestPayload(UUID homeId) implements CustomPacketPayload {
		public static final Type<TeleportHomeRequestPayload> TYPE = new Type<>(id("teleport_home_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportHomeRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(TeleportHomeRequestPayload::write, TeleportHomeRequestPayload::new);

		private TeleportHomeRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.homeId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record TeleportTerritoryRequestPayload(UUID territoryId) implements CustomPacketPayload {
		public static final Type<TeleportTerritoryRequestPayload> TYPE = new Type<>(id("teleport_territory_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TeleportTerritoryRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(TeleportTerritoryRequestPayload::write, TeleportTerritoryRequestPayload::new);

		private TeleportTerritoryRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.territoryId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CreateHomeRequestPayload(String name) implements CustomPacketPayload {
		public static final Type<CreateHomeRequestPayload> TYPE = new Type<>(id("create_home_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CreateHomeRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(CreateHomeRequestPayload::write, CreateHomeRequestPayload::new);

		public CreateHomeRequestPayload {
			name = name == null ? "" : name;
		}

		private CreateHomeRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUtf(64));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(this.name, 64);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CreateTerritoryRequestPayload(String name) implements CustomPacketPayload {
		public static final Type<CreateTerritoryRequestPayload> TYPE = new Type<>(id("create_territory_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CreateTerritoryRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(CreateTerritoryRequestPayload::write, CreateTerritoryRequestPayload::new);

		public CreateTerritoryRequestPayload {
			name = name == null ? "" : name;
		}

		private CreateTerritoryRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUtf(64));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(this.name, 64);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record UpdateTerritoryPermissionPayload(UUID territoryId, String permissionKey, boolean allowed) implements CustomPacketPayload {
		public static final Type<UpdateTerritoryPermissionPayload> TYPE = new Type<>(id("update_territory_permission"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTerritoryPermissionPayload> STREAM_CODEC =
			CustomPacketPayload.codec(UpdateTerritoryPermissionPayload::write, UpdateTerritoryPermissionPayload::new);

		public UpdateTerritoryPermissionPayload {
			permissionKey = permissionKey == null ? "" : permissionKey;
		}

		private UpdateTerritoryPermissionPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(64), buf.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.territoryId);
			buf.writeUtf(this.permissionKey, 64);
			buf.writeBoolean(this.allowed);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record UpdateTerritoryWhitelistPayload(UUID territoryId, UUID playerId, boolean allowed) implements CustomPacketPayload {
		public static final Type<UpdateTerritoryWhitelistPayload> TYPE = new Type<>(id("update_territory_whitelist"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTerritoryWhitelistPayload> STREAM_CODEC =
			CustomPacketPayload.codec(UpdateTerritoryWhitelistPayload::write, UpdateTerritoryWhitelistPayload::new);

		private UpdateTerritoryWhitelistPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUUID(), buf.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.territoryId);
			buf.writeUUID(this.playerId);
			buf.writeBoolean(this.allowed);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CancelTerritoryCreatePayload() implements CustomPacketPayload {
		public static final Type<CancelTerritoryCreatePayload> TYPE = new Type<>(id("cancel_territory_create"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CancelTerritoryCreatePayload> STREAM_CODEC =
			CustomPacketPayload.codec(CancelTerritoryCreatePayload::write, CancelTerritoryCreatePayload::new);

		private CancelTerritoryCreatePayload(RegistryFriendlyByteBuf buf) {
			this();
		}

		private void write(RegistryFriendlyByteBuf buf) {
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ClientHelloPayload(int protocol) implements CustomPacketPayload {
		public static final Type<ClientHelloPayload> TYPE = new Type<>(id("client_hello"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ClientHelloPayload> STREAM_CODEC =
			CustomPacketPayload.codec(ClientHelloPayload::write, ClientHelloPayload::new);

		private ClientHelloPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readVarInt());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.protocol);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DeleteHomeRequestPayload(UUID homeId) implements CustomPacketPayload {
		public static final Type<DeleteHomeRequestPayload> TYPE = new Type<>(id("delete_home_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DeleteHomeRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(DeleteHomeRequestPayload::write, DeleteHomeRequestPayload::new);

		private DeleteHomeRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.homeId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RenameHomeRequestPayload(UUID homeId, String name) implements CustomPacketPayload {
		public static final Type<RenameHomeRequestPayload> TYPE = new Type<>(id("rename_home_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RenameHomeRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(RenameHomeRequestPayload::write, RenameHomeRequestPayload::new);

		public RenameHomeRequestPayload {
			name = name == null ? "" : name;
		}

		private RenameHomeRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(64));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.homeId);
			buf.writeUtf(this.name, 64);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DisableItemRequestPayload(String itemId, boolean disable) implements CustomPacketPayload {
		public static final Type<DisableItemRequestPayload> TYPE = new Type<>(id("disable_item_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisableItemRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(DisableItemRequestPayload::write, DisableItemRequestPayload::new);

		public DisableItemRequestPayload {
			itemId = itemId == null ? "" : itemId;
		}

		private DisableItemRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUtf(128), buf.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(this.itemId, 128);
			buf.writeBoolean(this.disable);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record EconomyAdminActionPayload(UUID targetId, String action, double amount) implements CustomPacketPayload {
		public static final Type<EconomyAdminActionPayload> TYPE = new Type<>(id("economy_admin_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EconomyAdminActionPayload> STREAM_CODEC =
			CustomPacketPayload.codec(EconomyAdminActionPayload::write, EconomyAdminActionPayload::new);

		public EconomyAdminActionPayload {
			action = action == null ? "" : action;
		}

		private EconomyAdminActionPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(32), buf.readDouble());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.targetId);
			buf.writeUtf(this.action, 32);
			buf.writeDouble(this.amount);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record EconomySidebarVisibilityPayload(boolean hidden) implements CustomPacketPayload {
		public static final Type<EconomySidebarVisibilityPayload> TYPE = new Type<>(id("economy_sidebar_visibility"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EconomySidebarVisibilityPayload> STREAM_CODEC =
			CustomPacketPayload.codec(EconomySidebarVisibilityPayload::write, EconomySidebarVisibilityPayload::new);

		private EconomySidebarVisibilityPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readBoolean());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeBoolean(this.hidden);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record EconomyLeaderboardEntry(UUID playerId, String name, double balance, String formattedBalance) {
		public EconomyLeaderboardEntry {
			name = name == null ? "" : name;
			formattedBalance = formattedBalance == null ? "0" : formattedBalance;
		}
	}

	public record ShopGoodTarget(String itemId, String name, String iconItemId, int stock, double price) {
		public ShopGoodTarget {
			itemId = itemId == null ? "" : itemId;
			name = name == null ? "" : name;
			iconItemId = iconItemId == null ? "minecraft:chest" : iconItemId;
		}
	}

	public record ShopTarget(
		UUID id,
		UUID ownerId,
		String ownerName,
		String name,
		String iconItemId,
		List<ShopGoodTarget> goods
	) {
		public ShopTarget {
			ownerName = ownerName == null ? "" : ownerName;
			name = name == null ? "" : name;
			iconItemId = iconItemId == null ? "minecraft:chest" : iconItemId;
			goods = List.copyOf(goods);
		}
	}

	public record BoxShopTarget(
		UUID boxShopId,
		UUID shopId,
		UUID ownerId,
		String ownerName,
		String name,
		boolean owner,
		boolean needsNaming,
		List<ShopGoodTarget> goods
	) {
		public BoxShopTarget {
			ownerName = ownerName == null ? "" : ownerName;
			name = name == null ? "" : name;
			goods = List.copyOf(goods);
		}
	}

	public record OpenBoxShopPayload(BoxShopTarget target) implements CustomPacketPayload {
		public static final Type<OpenBoxShopPayload> TYPE = new Type<>(id("open_box_shop"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenBoxShopPayload> STREAM_CODEC =
			CustomPacketPayload.codec(OpenBoxShopPayload::write, OpenBoxShopPayload::new);

		private OpenBoxShopPayload(RegistryFriendlyByteBuf buf) {
			this(new BoxShopTarget(
				buf.readUUID(),
				buf.readUUID(),
				buf.readUUID(),
				buf.readUtf(64),
				buf.readUtf(64),
				buf.readBoolean(),
				buf.readBoolean(),
				readGoods(buf)
			));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.target.boxShopId());
			buf.writeUUID(this.target.shopId());
			buf.writeUUID(this.target.ownerId());
			buf.writeUtf(this.target.ownerName(), 64);
			buf.writeUtf(this.target.name(), 64);
			buf.writeBoolean(this.target.owner());
			buf.writeBoolean(this.target.needsNaming());
			buf.writeVarInt(this.target.goods().size());
			for (ShopGoodTarget good : this.target.goods()) {
				buf.writeUtf(good.itemId(), 128);
				buf.writeUtf(good.name(), 64);
				buf.writeUtf(good.iconItemId(), 128);
				buf.writeVarInt(good.stock());
				buf.writeDouble(good.price());
			}
		}

		private static List<ShopGoodTarget> readGoods(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<ShopGoodTarget> goods = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				goods.add(new ShopGoodTarget(
					buf.readUtf(128),
					buf.readUtf(64),
					buf.readUtf(128),
					buf.readVarInt(),
					buf.readDouble()
				));
			}
			return goods;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CloseMenuPayload() implements CustomPacketPayload {
		public static final Type<CloseMenuPayload> TYPE = new Type<>(id("close_menu"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CloseMenuPayload> STREAM_CODEC =
			CustomPacketPayload.codec(CloseMenuPayload::write, CloseMenuPayload::new);

		private CloseMenuPayload(RegistryFriendlyByteBuf buf) {
			this();
		}

		private void write(RegistryFriendlyByteBuf buf) {
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RenameBoxShopPayload(UUID boxShopId, String name) implements CustomPacketPayload {
		public static final Type<RenameBoxShopPayload> TYPE = new Type<>(id("rename_box_shop"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RenameBoxShopPayload> STREAM_CODEC =
			CustomPacketPayload.codec(RenameBoxShopPayload::write, RenameBoxShopPayload::new);

		public RenameBoxShopPayload {
			name = name == null ? "" : name;
		}

		private RenameBoxShopPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(64));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.boxShopId);
			buf.writeUtf(this.name, 64);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record QueryBoxShopHudPayload(int x, int y, int z) implements CustomPacketPayload {
		public static final Type<QueryBoxShopHudPayload> TYPE = new Type<>(id("query_box_shop_hud"));
		public static final StreamCodec<RegistryFriendlyByteBuf, QueryBoxShopHudPayload> STREAM_CODEC =
			CustomPacketPayload.codec(QueryBoxShopHudPayload::write, QueryBoxShopHudPayload::new);

		private QueryBoxShopHudPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.x);
			buf.writeVarInt(this.y);
			buf.writeVarInt(this.z);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BoxShopHudPayload(boolean present, String name, int goodsCount) implements CustomPacketPayload {
		public static final Type<BoxShopHudPayload> TYPE = new Type<>(id("box_shop_hud"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BoxShopHudPayload> STREAM_CODEC =
			CustomPacketPayload.codec(BoxShopHudPayload::write, BoxShopHudPayload::new);

		public BoxShopHudPayload {
			name = name == null ? "" : name;
			goodsCount = Math.max(0, goodsCount);
		}

		private BoxShopHudPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readBoolean(), buf.readUtf(64), buf.readVarInt());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeBoolean(this.present);
			buf.writeUtf(this.name, 64);
			buf.writeVarInt(this.goodsCount);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record EconomyStatePayload(
		List<EconomyLeaderboardEntry> leaderboard,
		List<ShopTarget> shops,
		int ownShopCount,
		int ownShopLimit
	) implements CustomPacketPayload {
		public static final Type<EconomyStatePayload> TYPE = new Type<>(id("economy_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EconomyStatePayload> STREAM_CODEC =
			CustomPacketPayload.codec(EconomyStatePayload::write, EconomyStatePayload::new);

		public EconomyStatePayload {
			leaderboard = List.copyOf(leaderboard);
			shops = List.copyOf(shops);
		}

		private EconomyStatePayload(RegistryFriendlyByteBuf buf) {
			this(readLeaderboard(buf), readShops(buf), buf.readVarInt(), buf.readVarInt());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.leaderboard.size());
			for (EconomyLeaderboardEntry entry : this.leaderboard) {
				buf.writeUUID(entry.playerId());
				buf.writeUtf(entry.name(), 64);
				buf.writeDouble(entry.balance());
				buf.writeUtf(entry.formattedBalance(), 64);
			}

			buf.writeVarInt(this.shops.size());
			for (ShopTarget shop : this.shops) {
				buf.writeUUID(shop.id());
				buf.writeUUID(shop.ownerId());
				buf.writeUtf(shop.ownerName(), 64);
				buf.writeUtf(shop.name(), 64);
				buf.writeUtf(shop.iconItemId(), 128);
				buf.writeVarInt(shop.goods().size());
				for (ShopGoodTarget good : shop.goods()) {
					buf.writeUtf(good.itemId(), 128);
					buf.writeUtf(good.name(), 64);
					buf.writeUtf(good.iconItemId(), 128);
					buf.writeVarInt(good.stock());
					buf.writeDouble(good.price());
				}
			}

			buf.writeVarInt(this.ownShopCount);
			buf.writeVarInt(this.ownShopLimit);
		}

		private static List<EconomyLeaderboardEntry> readLeaderboard(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<EconomyLeaderboardEntry> entries = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				entries.add(new EconomyLeaderboardEntry(buf.readUUID(), buf.readUtf(64), buf.readDouble(), buf.readUtf(64)));
			}
			return entries;
		}

		private static List<ShopTarget> readShops(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<ShopTarget> shops = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				UUID shopId = buf.readUUID();
				UUID ownerId = buf.readUUID();
				String ownerName = buf.readUtf(64);
				String name = buf.readUtf(64);
				String icon = buf.readUtf(128);
				int goodsSize = buf.readVarInt();
				List<ShopGoodTarget> goods = new ArrayList<>(goodsSize);
				for (int j = 0; j < goodsSize; j++) {
					goods.add(new ShopGoodTarget(
						buf.readUtf(128),
						buf.readUtf(64),
						buf.readUtf(128),
						buf.readVarInt(),
						buf.readDouble()
					));
				}
				shops.add(new ShopTarget(shopId, ownerId, ownerName, name, icon, goods));
			}
			return shops;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record CreateShopRequestPayload(String name) implements CustomPacketPayload {
		public static final Type<CreateShopRequestPayload> TYPE = new Type<>(id("create_shop_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CreateShopRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(CreateShopRequestPayload::write, CreateShopRequestPayload::new);

		public CreateShopRequestPayload {
			name = name == null ? "" : name;
		}

		private CreateShopRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUtf(64));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(this.name, 64);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ExpandShopLimitRequestPayload() implements CustomPacketPayload {
		public static final Type<ExpandShopLimitRequestPayload> TYPE = new Type<>(id("expand_shop_limit_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ExpandShopLimitRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(ExpandShopLimitRequestPayload::write, ExpandShopLimitRequestPayload::new);

		private ExpandShopLimitRequestPayload(RegistryFriendlyByteBuf buf) {
			this();
		}

		private void write(RegistryFriendlyByteBuf buf) {
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DeleteShopRequestPayload(UUID shopId) implements CustomPacketPayload {
		public static final Type<DeleteShopRequestPayload> TYPE = new Type<>(id("delete_shop_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DeleteShopRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(DeleteShopRequestPayload::write, DeleteShopRequestPayload::new);

		private DeleteShopRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.shopId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RenameShopRequestPayload(UUID shopId, String name) implements CustomPacketPayload {
		public static final Type<RenameShopRequestPayload> TYPE = new Type<>(id("rename_shop_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RenameShopRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(RenameShopRequestPayload::write, RenameShopRequestPayload::new);

		public RenameShopRequestPayload {
			name = name == null ? "" : name;
		}

		private RenameShopRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(64));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.shopId);
			buf.writeUtf(this.name, 64);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetShopIconRequestPayload(UUID shopId, String iconItemId) implements CustomPacketPayload {
		public static final Type<SetShopIconRequestPayload> TYPE = new Type<>(id("set_shop_icon_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetShopIconRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(SetShopIconRequestPayload::write, SetShopIconRequestPayload::new);

		public SetShopIconRequestPayload {
			iconItemId = iconItemId == null ? "" : iconItemId;
		}

		private SetShopIconRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(128));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.shopId);
			buf.writeUtf(this.iconItemId, 128);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record AddShopGoodRequestPayload(UUID shopId, String itemId, int stock, double price) implements CustomPacketPayload {
		public static final Type<AddShopGoodRequestPayload> TYPE = new Type<>(id("add_shop_good_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AddShopGoodRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(AddShopGoodRequestPayload::write, AddShopGoodRequestPayload::new);

		public AddShopGoodRequestPayload {
			itemId = itemId == null ? "" : itemId;
		}

		private AddShopGoodRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(128), buf.readVarInt(), buf.readDouble());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.shopId);
			buf.writeUtf(this.itemId, 128);
			buf.writeVarInt(this.stock);
			buf.writeDouble(this.price);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BuyShopGoodRequestPayload(UUID shopId, String itemId, int amount) implements CustomPacketPayload {
		public static final Type<BuyShopGoodRequestPayload> TYPE = new Type<>(id("buy_shop_good_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BuyShopGoodRequestPayload> STREAM_CODEC =
			CustomPacketPayload.codec(BuyShopGoodRequestPayload::write, BuyShopGoodRequestPayload::new);

		public BuyShopGoodRequestPayload {
			itemId = itemId == null ? "" : itemId;
		}

		private BuyShopGoodRequestPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID(), buf.readUtf(128), buf.readVarInt());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.shopId);
			buf.writeUtf(this.itemId, 128);
			buf.writeVarInt(this.amount);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record LanguagePack(String languageCode, List<LangEntry> entries) {
		public LanguagePack {
			languageCode = languageCode == null ? "zh_cn" : languageCode;
			entries = List.copyOf(entries);
		}
	}

	public record LangEntry(String key, String value) {
		public LangEntry {
			key = key == null ? "" : key;
			value = value == null ? "" : value;
		}
	}

	public record LocalizationSyncPayload(List<LanguagePack> packs, long version) implements CustomPacketPayload {
		public static final Type<LocalizationSyncPayload> TYPE = new Type<>(id("localization_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LocalizationSyncPayload> STREAM_CODEC =
			CustomPacketPayload.codec(LocalizationSyncPayload::write, LocalizationSyncPayload::new);

		public LocalizationSyncPayload {
			packs = List.copyOf(packs);
		}

		private LocalizationSyncPayload(RegistryFriendlyByteBuf buf) {
			this(readPacks(buf), buf.readVarLong());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.packs.size());
			for (LanguagePack pack : this.packs) {
				buf.writeUtf(pack.languageCode(), 16);
				buf.writeVarInt(pack.entries().size());
				for (LangEntry entry : pack.entries()) {
					buf.writeUtf(entry.key(), 200);
					buf.writeUtf(entry.value(), 400);
				}
			}
			buf.writeVarLong(this.version);
		}

		private static List<LanguagePack> readPacks(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<LanguagePack> packs = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				String languageCode = buf.readUtf(16);
				int entriesSize = buf.readVarInt();
				List<LangEntry> entries = new ArrayList<>(entriesSize);
				for (int j = 0; j < entriesSize; j++) {
					entries.add(new LangEntry(buf.readUtf(200), buf.readUtf(400)));
				}
				packs.add(new LanguagePack(languageCode, entries));
			}
			return packs;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HomeListPayload(List<HomeTarget> homes) implements CustomPacketPayload {
		public static final Type<HomeListPayload> TYPE = new Type<>(id("home_list"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HomeListPayload> STREAM_CODEC =
			CustomPacketPayload.codec(HomeListPayload::write, HomeListPayload::new);

		public HomeListPayload {
			homes = List.copyOf(homes);
		}

		private HomeListPayload(RegistryFriendlyByteBuf buf) {
			this(readHomes(buf));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.homes.size());
			for (HomeTarget home : this.homes) {
				buf.writeUUID(home.id());
				buf.writeUtf(home.name(), 64);
				buf.writeVarInt(home.x());
				buf.writeVarInt(home.y());
				buf.writeVarInt(home.z());
				buf.writeUtf(home.iconItemId(), 128);
			}
		}

		private static List<HomeTarget> readHomes(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<HomeTarget> homes = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				homes.add(new HomeTarget(
					buf.readUUID(),
					buf.readUtf(64),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readVarInt(),
					buf.readUtf(128)
				));
			}
			return homes;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DisabledItemListPayload(List<String> disabledItemIds) implements CustomPacketPayload {
		public static final Type<DisabledItemListPayload> TYPE = new Type<>(id("disabled_item_list"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisabledItemListPayload> STREAM_CODEC =
			CustomPacketPayload.codec(DisabledItemListPayload::write, DisabledItemListPayload::new);

		public DisabledItemListPayload {
			disabledItemIds = List.copyOf(disabledItemIds);
		}

		private DisabledItemListPayload(RegistryFriendlyByteBuf buf) {
			this(readDisabledItemIds(buf));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.disabledItemIds.size());
			for (String id : this.disabledItemIds) {
				buf.writeUtf(id, 128);
			}
		}

		private static List<String> readDisabledItemIds(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<String> ids = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				ids.add(buf.readUtf(128));
			}
			return ids;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record MomentFeedPayload(List<MomentEntry> moments, String draft) implements CustomPacketPayload {
		public static final Type<MomentFeedPayload> TYPE = new Type<>(id("moment_feed"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MomentFeedPayload> STREAM_CODEC =
			CustomPacketPayload.codec(MomentFeedPayload::write, MomentFeedPayload::new);

		public MomentFeedPayload {
			moments = List.copyOf(moments);
			draft = draft == null ? "" : draft;
		}

		private MomentFeedPayload(RegistryFriendlyByteBuf buf) {
			this(readMoments(buf), buf.readUtf(1024));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeVarInt(this.moments.size());
			for (MomentEntry moment : this.moments) {
				buf.writeUUID(moment.id());
				buf.writeUUID(moment.authorId());
				buf.writeUtf(moment.authorName(), 64);
				buf.writeUtf(moment.content(), 1024);
				buf.writeLong(moment.createdAt());
				buf.writeVarInt(moment.likeCount());
				buf.writeBoolean(moment.likedByViewer());
				buf.writeBoolean(moment.own());
			}
			buf.writeUtf(this.draft, 1024);
		}

		private static List<MomentEntry> readMoments(RegistryFriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<MomentEntry> moments = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				moments.add(new MomentEntry(
					buf.readUUID(),
					buf.readUUID(),
					buf.readUtf(64),
					buf.readUtf(1024),
					buf.readLong(),
					buf.readVarInt(),
					buf.readBoolean(),
					buf.readBoolean()
				));
			}
			return moments;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record RequestMomentFeedPayload() implements CustomPacketPayload {
		public static final Type<RequestMomentFeedPayload> TYPE = new Type<>(id("request_moment_feed"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RequestMomentFeedPayload> STREAM_CODEC =
			CustomPacketPayload.codec(RequestMomentFeedPayload::write, RequestMomentFeedPayload::new);

		private RequestMomentFeedPayload(RegistryFriendlyByteBuf buf) {
			this();
		}

		private void write(RegistryFriendlyByteBuf buf) {
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record PostMomentPayload(String content) implements CustomPacketPayload {
		public static final Type<PostMomentPayload> TYPE = new Type<>(id("post_moment"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PostMomentPayload> STREAM_CODEC =
			CustomPacketPayload.codec(PostMomentPayload::write, PostMomentPayload::new);

		public PostMomentPayload {
			content = content == null ? "" : content;
		}

		private PostMomentPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUtf(1024));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(this.content, 1024);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record LikeMomentPayload(UUID momentId) implements CustomPacketPayload {
		public static final Type<LikeMomentPayload> TYPE = new Type<>(id("like_moment"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LikeMomentPayload> STREAM_CODEC =
			CustomPacketPayload.codec(LikeMomentPayload::write, LikeMomentPayload::new);

		private LikeMomentPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.momentId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record DeleteMomentPayload(UUID momentId) implements CustomPacketPayload {
		public static final Type<DeleteMomentPayload> TYPE = new Type<>(id("delete_moment"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DeleteMomentPayload> STREAM_CODEC =
			CustomPacketPayload.codec(DeleteMomentPayload::write, DeleteMomentPayload::new);

		private DeleteMomentPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUUID());
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUUID(this.momentId);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SaveMomentDraftPayload(String content) implements CustomPacketPayload {
		public static final Type<SaveMomentDraftPayload> TYPE = new Type<>(id("save_moment_draft"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SaveMomentDraftPayload> STREAM_CODEC =
			CustomPacketPayload.codec(SaveMomentDraftPayload::write, SaveMomentDraftPayload::new);

		public SaveMomentDraftPayload {
			content = content == null ? "" : content;
		}

		private SaveMomentDraftPayload(RegistryFriendlyByteBuf buf) {
			this(buf.readUtf(1024));
		}

		private void write(RegistryFriendlyByteBuf buf) {
			buf.writeUtf(this.content, 1024);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("teleportmenu", path);
	}
}
