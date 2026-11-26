package com.example;

import com.example.network.TeleportMenuPayloads;
import com.example.server.model.BoxShopNames;
import com.example.server.model.BoxShopPoint;
import com.example.server.model.HomePoint;
import com.example.server.model.MomentContent;
import com.example.server.model.MomentPoint;
import com.example.server.model.ShopGood;
import com.example.server.model.ShopPoint;
import com.example.server.model.TerritoryPermission;
import com.example.server.model.TerritoryPoint;
import com.example.server.storage.DatabaseManager;
import com.example.server.storage.JsonDataImporter;
import com.example.server.storage.PersistedSnapshot;
import com.example.server.storage.ServerDataStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.core.Holder;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.literal;

public class TeleportMenuMod implements ModInitializer {
	public static final String MOD_ID = "teleportmenu";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Map<UUID, List<HomePoint>> PLAYER_HOMES = new ConcurrentHashMap<>();
	private static final Map<UUID, List<TerritoryPoint>> PLAYER_TERRITORIES = new ConcurrentHashMap<>();
	private static final Map<UUID, List<ShopPoint>> PLAYER_SHOPS = new ConcurrentHashMap<>();
	private static final Map<UUID, BoxShopPoint> BOX_SHOPS = new ConcurrentHashMap<>();
	private static final Set<UUID> BOX_SHOP_SHOP_IDS = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, UUID> BOX_SHOP_HOLOGRAMS = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> PLAYER_SHOP_LIMITS = new ConcurrentHashMap<>();
	private static final Map<UUID, MomentPoint> MOMENTS = new ConcurrentHashMap<>();
	private static final Map<UUID, String> MOMENT_DRAFTS = new ConcurrentHashMap<>();
	private static final Map<UUID, TerritorySelection> TERRITORY_SELECTIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, TerritoryHudState> TERRITORY_HUD_STATES = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_DENY_ACTIONBAR_TICK = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<String, Integer>> PLAYER_EQUIPMENT_COUNTS = new ConcurrentHashMap<>();
	private static final Map<UUID, Set<String>> PLAYER_REWARDED_EQUIPMENT = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> PLAYER_UI_PROTOCOLS = new ConcurrentHashMap<>();
	private static final Set<String> DISABLED_ITEM_IDS = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> ECONOMY_SIDEBAR_HIDDEN = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, Integer> ECONOMY_SIDEBAR_LAST_SCORE = new ConcurrentHashMap<>();
	private static final Set<UUID> ECONOMY_SIDEBAR_ASSERTED = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> ECONOMY_SIDEBAR_HIDDEN_SENT = ConcurrentHashMap.newKeySet();
	private static final int STABLE_UI_PROTOCOL = 1;
	private static final int UNKNOWN_UI_PROTOCOL = 0;
	private static final boolean ECONOMY_MOD_LOADED = FabricLoader.getInstance().isModLoaded("economy");
	private static final double ECONOMY_MONSTER_KILL_REWARD = 5D;
	private static final double ECONOMY_EQUIPMENT_REWARD = 3D;
	private static final double ECONOMY_MINING_COAL_REWARD = 5D;
	private static final double ECONOMY_MINING_IRON_COPPER_REWARD = 8D;
	private static final double ECONOMY_MINING_REDSTONE_LAPIS_REWARD = 10D;
	private static final double ECONOMY_MINING_DIAMOND_EMERALD_REWARD = 15D;
	private static final double ECONOMY_SPECIAL_MINING_REWARD = 20D;
	private static final String ECONOMY_SIDEBAR_ENTRY = "\u25C6 \u4F59\u989D";
	private static final int DEFAULT_SHOP_LIMIT = 2;
	private static final int SHOP_LIMIT_EXPAND_COST = 100;
	private static final int SHOP_DELETE_COST = 10;
	private static final int MOMENT_MAX_CONTENT_LENGTH = MomentContent.MAX_LENGTH;
	private static final int MOMENT_MAX_COUNT = 100;
	private static final String DEFAULT_BOX_SHOP_NAME = BoxShopNames.DEFAULT_NAME;
	private static final String BOX_SHOP_HOLOGRAM_TAG = "teleportmenu_boxshop_holo";
	private static boolean ECONOMY_API_INITIALIZED = false;
	private static boolean ECONOMY_API_READY = false;
	private static Method ECONOMY_GET_MONEY = null;
	private static Method ECONOMY_ADD = null;
	private static Method ECONOMY_SUBTRACT = null;
	private static Method ECONOMY_SET = null;
	private static Method ECONOMY_RESET = null;
	private static Method ECONOMY_FORMAT = null;
	private static final Map<String, Map<String, String>> SERVER_LOCALIZATIONS = new ConcurrentHashMap<>();
	private static long LOCALIZATION_VERSION = 1L;
	private static final String DATA_DIR_NAME = "teleport-menu-mod";
	private static final String DATA_FILE_NAME = "data.json";
	private static final String DATA_DB_FILE_NAME = "data.sqlite";
	private static ServerDataStore STORAGE = null;
	private static MinecraftServer ACTIVE_SERVER = null;

	private static final DustParticleOptions TERRITORY_SELECTION_PARTICLE = new DustParticleOptions(new Vector3f(0.25F, 1.0F, 0.25F), 1.0F);
	private static long SERVER_TICK = 0L;

	private static String sanitizeBoxShopName(String rawName) {
		return BoxShopNames.sanitize(rawName);
	}

	private static final List<Item> HOME_ICON_POOL = List.of(
		Items.COMPASS, Items.CLOCK, Items.RED_BED, Items.OAK_DOOR, Items.CHEST, Items.TORCH,
		Items.CAMPFIRE, Items.BREAD, Items.GOLDEN_APPLE, Items.DIAMOND, Items.ENDER_PEARL, Items.BOOK
	);
	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.CloseMenuPayload.TYPE, TeleportMenuPayloads.CloseMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.OpenMenuPayload.TYPE, TeleportMenuPayloads.OpenMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.HomeListPayload.TYPE, TeleportMenuPayloads.HomeListPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.DisabledItemListPayload.TYPE, TeleportMenuPayloads.DisabledItemListPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.EconomyStatePayload.TYPE, TeleportMenuPayloads.EconomyStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.OpenBoxShopPayload.TYPE, TeleportMenuPayloads.OpenBoxShopPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.BoxShopHudPayload.TYPE, TeleportMenuPayloads.BoxShopHudPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.MomentFeedPayload.TYPE, TeleportMenuPayloads.MomentFeedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.TeleportRequestPayload.TYPE, TeleportMenuPayloads.TeleportRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.TeleportHomeRequestPayload.TYPE, TeleportMenuPayloads.TeleportHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.TeleportTerritoryRequestPayload.TYPE, TeleportMenuPayloads.TeleportTerritoryRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CreateHomeRequestPayload.TYPE, TeleportMenuPayloads.CreateHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CreateTerritoryRequestPayload.TYPE, TeleportMenuPayloads.CreateTerritoryRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CancelTerritoryCreatePayload.TYPE, TeleportMenuPayloads.CancelTerritoryCreatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.UpdateTerritoryPermissionPayload.TYPE, TeleportMenuPayloads.UpdateTerritoryPermissionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.UpdateTerritoryWhitelistPayload.TYPE, TeleportMenuPayloads.UpdateTerritoryWhitelistPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.ClientHelloPayload.TYPE, TeleportMenuPayloads.ClientHelloPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DeleteHomeRequestPayload.TYPE, TeleportMenuPayloads.DeleteHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RenameHomeRequestPayload.TYPE, TeleportMenuPayloads.RenameHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DisableItemRequestPayload.TYPE, TeleportMenuPayloads.DisableItemRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.EconomyAdminActionPayload.TYPE, TeleportMenuPayloads.EconomyAdminActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.EconomySidebarVisibilityPayload.TYPE, TeleportMenuPayloads.EconomySidebarVisibilityPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CreateShopRequestPayload.TYPE, TeleportMenuPayloads.CreateShopRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.ExpandShopLimitRequestPayload.TYPE, TeleportMenuPayloads.ExpandShopLimitRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DeleteShopRequestPayload.TYPE, TeleportMenuPayloads.DeleteShopRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RenameShopRequestPayload.TYPE, TeleportMenuPayloads.RenameShopRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.SetShopIconRequestPayload.TYPE, TeleportMenuPayloads.SetShopIconRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.AddShopGoodRequestPayload.TYPE, TeleportMenuPayloads.AddShopGoodRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.BuyShopGoodRequestPayload.TYPE, TeleportMenuPayloads.BuyShopGoodRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RenameBoxShopPayload.TYPE, TeleportMenuPayloads.RenameBoxShopPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.QueryBoxShopHudPayload.TYPE, TeleportMenuPayloads.QueryBoxShopHudPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RequestMomentFeedPayload.TYPE, TeleportMenuPayloads.RequestMomentFeedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.PostMomentPayload.TYPE, TeleportMenuPayloads.PostMomentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.LikeMomentPayload.TYPE, TeleportMenuPayloads.LikeMomentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DeleteMomentPayload.TYPE, TeleportMenuPayloads.DeleteMomentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.SaveMomentDraftPayload.TYPE, TeleportMenuPayloads.SaveMomentDraftPayload.STREAM_CODEC);

		PlayerBlockBreakEvents.BEFORE.register(TeleportMenuMod::onBeforeBlockBreak);
		PlayerBlockBreakEvents.AFTER.register(TeleportMenuMod::onAfterBlockBreak);
		UseBlockCallback.EVENT.register(TeleportMenuMod::onUseBlock);
		AttackEntityCallback.EVENT.register(TeleportMenuMod::onAttackEntity);
		UseItemCallback.EVENT.register(TeleportMenuMod::onAnyItemUse);
		UseItemCallback.EVENT.register(TeleportMenuMod::onClockUse);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("clock").executes(context -> giveClock(context.getSource())));
			dispatcher.register(literal("territ")
				.executes(context -> showTerritoryUsage(context.getSource()))
				.then(literal("a").executes(context -> setTerritoryPoint(context.getSource(), true)))
				.then(literal("b").executes(context -> setTerritoryPoint(context.getSource(), false))));
			dispatcher.register(literal("boxshop")
				.then(literal("add").executes(context -> addBoxShop(context.getSource())))
				.then(literal("clean").executes(context -> cleanBoxShop(context.getSource()))));
		});

		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.TeleportRequestPayload.TYPE, (payload, context) -> handleTeleportRequest(context.player(), payload.targetId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.TeleportHomeRequestPayload.TYPE, (payload, context) -> handleTeleportHomeRequest(context.player(), payload.homeId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.TeleportTerritoryRequestPayload.TYPE, (payload, context) -> handleTeleportTerritoryRequest(context.player(), payload.territoryId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CreateHomeRequestPayload.TYPE, (payload, context) -> handleCreateHomeRequest(context.player(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CreateTerritoryRequestPayload.TYPE, (payload, context) -> handleCreateTerritoryRequest(context.player(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CancelTerritoryCreatePayload.TYPE, (payload, context) -> handleCancelTerritoryCreate(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.UpdateTerritoryPermissionPayload.TYPE, (payload, context) -> handleUpdateTerritoryPermission(context.player(), payload.territoryId(), payload.permissionKey(), payload.allowed()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.UpdateTerritoryWhitelistPayload.TYPE, (payload, context) -> handleUpdateTerritoryWhitelist(context.player(), payload.territoryId(), payload.playerId(), payload.allowed()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.ClientHelloPayload.TYPE, (payload, context) -> {
			PLAYER_UI_PROTOCOLS.put(context.player().getUUID(), payload.protocol());
			syncLocalizations(context.player());
		});
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DeleteHomeRequestPayload.TYPE, (payload, context) -> handleDeleteHomeRequest(context.player(), payload.homeId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RenameHomeRequestPayload.TYPE, (payload, context) -> handleRenameHomeRequest(context.player(), payload.homeId(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DisableItemRequestPayload.TYPE, (payload, context) -> handleDisableItemRequest(context.player(), payload.itemId(), payload.disable()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.EconomyAdminActionPayload.TYPE, (payload, context) ->
			handleEconomyAdminAction(context.player(), payload.targetId(), payload.action(), payload.amount()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.EconomySidebarVisibilityPayload.TYPE, (payload, context) ->
			setEconomySidebarVisible(context.player().server, context.player().getUUID(), !payload.hidden()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CreateShopRequestPayload.TYPE, (payload, context) -> handleCreateShop(context.player(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.ExpandShopLimitRequestPayload.TYPE, (payload, context) -> handleExpandShopLimit(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DeleteShopRequestPayload.TYPE, (payload, context) -> handleDeleteShop(context.player(), payload.shopId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RenameShopRequestPayload.TYPE, (payload, context) -> handleRenameShop(context.player(), payload.shopId(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.SetShopIconRequestPayload.TYPE, (payload, context) -> handleSetShopIcon(context.player(), payload.shopId(), payload.iconItemId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.AddShopGoodRequestPayload.TYPE, (payload, context) -> handleAddShopGood(context.player(), payload.shopId(), payload.itemId(), payload.stock(), payload.price()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.BuyShopGoodRequestPayload.TYPE, (payload, context) -> handleBuyShopGood(context.player(), payload.shopId(), payload.itemId(), payload.amount()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RenameBoxShopPayload.TYPE, (payload, context) -> handleRenameBoxShop(context.player(), payload.boxShopId(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.QueryBoxShopHudPayload.TYPE, (payload, context) ->
			handleQueryBoxShopHud(context.player(), payload.x(), payload.y(), payload.z()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RequestMomentFeedPayload.TYPE, (payload, context) ->
			handleRequestMomentFeed(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.PostMomentPayload.TYPE, (payload, context) ->
			handlePostMoment(context.player(), payload.content()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.LikeMomentPayload.TYPE, (payload, context) ->
			handleLikeMoment(context.player(), payload.momentId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DeleteMomentPayload.TYPE, (payload, context) ->
			handleDeleteMoment(context.player(), payload.momentId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.SaveMomentDraftPayload.TYPE, (payload, context) ->
			handleSaveMomentDraft(context.player(), payload.content()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity instanceof Monster)) {
				return;
			}
			if (damageSource.getEntity() instanceof ServerPlayer killer) {
				rewardMonsterKill(killer);
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(TeleportMenuMod::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(TeleportMenuMod::onServerStopping);
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> cleanupLegacyBoxShopDisplaysInChunk(world, chunk));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			syncLocalizations(handler.player);
			runEconomySidebarOnServerThread(server, () ->
				applyEconomySidebarState(server, handler.player.getUUID()));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.player.getUUID();
			PLAYER_UI_PROTOCOLS.remove(playerId);
			PLAYER_EQUIPMENT_COUNTS.remove(playerId);
			PLAYER_REWARDED_EQUIPMENT.remove(playerId);
			runEconomySidebarOnServerThread(server, () -> {
				ECONOMY_SIDEBAR_LAST_SCORE.remove(playerId);
				ECONOMY_SIDEBAR_ASSERTED.remove(playerId);
				ECONOMY_SIDEBAR_HIDDEN_SENT.remove(playerId);
			});
		});
		ServerTickEvents.END_SERVER_TICK.register(TeleportMenuMod::onServerTick);
		LOGGER.info("Teleport Menu 2.3 initialized");
	}

	private static void onServerStarted(MinecraftServer server) {
		ACTIVE_SERVER = server;
		loadServerLocalizations();
		loadPersistentData(server);
		cleanupAllLegacyBoxShopDisplays(server);
		replaceDisabledForAllPlayers(server);
		refreshEconomySidebar(server);
	}

	private static void onServerStopping(MinecraftServer server) {
		savePersistentData(server);
		closeStorageQuietly();
		ACTIVE_SERVER = null;
		TERRITORY_SELECTIONS.clear();
		TERRITORY_HUD_STATES.clear();
		LAST_DENY_ACTIONBAR_TICK.clear();
		PLAYER_EQUIPMENT_COUNTS.clear();
		PLAYER_REWARDED_EQUIPMENT.clear();
		ECONOMY_SIDEBAR_LAST_SCORE.clear();
		ECONOMY_SIDEBAR_ASSERTED.clear();
		ECONOMY_SIDEBAR_HIDDEN_SENT.clear();
		PLAYER_SHOPS.clear();
		BOX_SHOPS.clear();
		BOX_SHOP_SHOP_IDS.clear();
		BOX_SHOP_HOLOGRAMS.clear();
		PLAYER_SHOP_LIMITS.clear();
		PLAYER_UI_PROTOCOLS.clear();
		MOMENTS.clear();
		MOMENT_DRAFTS.clear();
		SERVER_LOCALIZATIONS.clear();
	}

	private static Path dataDir(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(DATA_DIR_NAME);
	}

	private static Path legacyDataFile(MinecraftServer server) {
		return dataDir(server).resolve(DATA_FILE_NAME);
	}

	private static Path databaseFile(MinecraftServer server) {
		return dataDir(server).resolve(DATA_DB_FILE_NAME);
	}

	private static void persistDataIfPossible() {
		if (ACTIVE_SERVER != null) {
			savePersistentData(ACTIVE_SERVER);
		}
	}

	private static void loadPersistentData(MinecraftServer server) {
		clearPersistentState();

		try {
			Path directory = dataDir(server);
			Files.createDirectories(directory);

			ServerDataStore store = new ServerDataStore(new DatabaseManager(databaseFile(server)));
			STORAGE = store;
			JsonDataImporter.importIfNeeded(store, legacyDataFile(server));
			applyLoadedData(store);
			LOGGER.info("Loaded teleport menu data from {}", databaseFile(server));
		} catch (Exception error) {
			LOGGER.error("Failed to load teleport menu data from SQLite", error);
			closeStorageQuietly();
		}
	}

	private static void clearPersistentState() {
		PLAYER_HOMES.clear();
		PLAYER_TERRITORIES.clear();
		PLAYER_SHOPS.clear();
		BOX_SHOPS.clear();
		BOX_SHOP_SHOP_IDS.clear();
		BOX_SHOP_HOLOGRAMS.clear();
		PLAYER_SHOP_LIMITS.clear();
		DISABLED_ITEM_IDS.clear();
		PLAYER_REWARDED_EQUIPMENT.clear();
		MOMENTS.clear();
		MOMENT_DRAFTS.clear();
		ECONOMY_SIDEBAR_HIDDEN.clear();
	}

	private static void applyLoadedData(ServerDataStore store) throws SQLException {
		PLAYER_HOMES.putAll(store.loadHomes());
		PLAYER_TERRITORIES.putAll(store.loadTerritories());
		PLAYER_SHOPS.putAll(store.loadShops());

		for (Map.Entry<UUID, BoxShopPoint> entry : store.loadBoxShops().entrySet()) {
			BoxShopPoint boxShop = entry.getValue();
			BOX_SHOPS.put(boxShop.id(), boxShop);
			BOX_SHOP_SHOP_IDS.add(boxShop.shopId());
			if (ACTIVE_SERVER != null) {
				ensureBoxShopBlock(ACTIVE_SERVER, boxShop);
				refreshBoxShopHologram(ACTIVE_SERVER, boxShop);
			}
		}

		for (Map.Entry<UUID, Integer> entry : store.loadShopLimits().entrySet()) {
			PLAYER_SHOP_LIMITS.put(entry.getKey(), Math.max(DEFAULT_SHOP_LIMIT, entry.getValue()));
		}

		PLAYER_REWARDED_EQUIPMENT.putAll(store.loadRewardedEquipment());
		MOMENTS.putAll(store.loadMoments());
		MOMENT_DRAFTS.putAll(store.loadMomentDrafts());
		DISABLED_ITEM_IDS.addAll(store.loadDisabledItems());
		ECONOMY_SIDEBAR_HIDDEN.addAll(store.loadEconomySidebarHidden());
	}

	private static void savePersistentData(MinecraftServer server) {
		ServerDataStore store = STORAGE;
		if (store == null) {
			return;
		}

		try {
			store.saveAll(new PersistedSnapshot(
				PLAYER_HOMES,
				PLAYER_TERRITORIES,
				PLAYER_SHOPS,
				BOX_SHOPS,
				PLAYER_SHOP_LIMITS,
				PLAYER_REWARDED_EQUIPMENT,
				MOMENTS,
				MOMENT_DRAFTS,
				DISABLED_ITEM_IDS,
				ECONOMY_SIDEBAR_HIDDEN
			));
		} catch (SQLException error) {
			LOGGER.error("Failed to save teleport menu data", error);
		}
	}

	private static void closeStorageQuietly() {
		ServerDataStore store = STORAGE;
		STORAGE = null;
		if (store == null) {
			return;
		}

		try {
			store.close();
		} catch (SQLException error) {
			LOGGER.warn("Failed to close teleport menu database", error);
		}
	}

	private static InteractionResultHolder<ItemStack> onAnyItemUse(Player player, Level level, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.isEmpty() || !isDisabledItem(heldItem.getItem())) {
			return InteractionResultHolder.pass(heldItem);
		}

		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			replaceDisabledInInventory(serverPlayer);
			sendError(serverPlayer, "message.teleportmenu.item_disabled_use_blocked");
		}

		return InteractionResultHolder.fail(player.getItemInHand(hand));
	}

	private static InteractionResultHolder<ItemStack> onClockUse(Player player, Level level, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (!heldItem.is(Items.CLOCK)) {
			return InteractionResultHolder.pass(heldItem);
		}

		if (level.isClientSide) {
			return InteractionResultHolder.success(heldItem);
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.pass(heldItem);
		}

		if (!ServerPlayNetworking.canSend(serverPlayer, TeleportMenuPayloads.OpenMenuPayload.TYPE)) {
			sendError(serverPlayer, "message.teleportmenu.client_missing");
			return InteractionResultHolder.consume(heldItem);
		}
		int uiProtocol = PLAYER_UI_PROTOCOLS.getOrDefault(serverPlayer.getUUID(), UNKNOWN_UI_PROTOCOL);
		if (uiProtocol < STABLE_UI_PROTOCOL) {
			sendError(serverPlayer, "message.teleportmenu.client_outdated");
			return InteractionResultHolder.consume(heldItem);
		}

		List<TeleportMenuPayloads.PlayerTarget> targets = serverPlayer.server
			.getPlayerList()
			.getPlayers()
			.stream()
			.filter(other -> !other.getUUID().equals(serverPlayer.getUUID()))
			.map(other -> new TeleportMenuPayloads.PlayerTarget(other.getUUID(), other.getName().getString()))
			.toList();

		TerritorySelection selection = TERRITORY_SELECTIONS.get(serverPlayer.getUUID());
		boolean selectionReady = selection != null && selection.pointA() != null && selection.pointB() != null
			&& selection.dimensionId().equals(serverPlayer.level().dimension().location());

		BlockPos a = selectionReady ? selection.pointA() : BlockPos.ZERO;
		BlockPos b = selectionReady ? selection.pointB() : BlockPos.ZERO;

		boolean admin = isAdmin(serverPlayer);
		syncLocalizations(serverPlayer);
		syncEconomyState(serverPlayer);
		ServerPlayNetworking.send(serverPlayer, new TeleportMenuPayloads.OpenMenuPayload(
			targets,
			getHomeTargets(serverPlayer),
			getTerritoryTargets(serverPlayer),
			admin,
			admin ? getDisabledItemIdList() : List.of(),
			selectionReady,
			a.getX(),
			a.getY(),
			a.getZ(),
			b.getX(),
			b.getY(),
			b.getZ()
		));

		return InteractionResultHolder.consume(heldItem);
	}

	private static boolean onBeforeBlockBreak(Level level, Player player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}

		BoxShopPoint boxShop = findBoxShopByBlock(level.dimension().location(), pos);
		if (boxShop != null) {
			if (boxShop.ownerId().equals(serverPlayer.getUUID())) {
				sendWarn(serverPlayer, "message.teleportmenu.boxshop_clean_hint");
			} else {
				sendError(serverPlayer, "message.teleportmenu.no_permission_action");
			}
			return false;
		}

		return checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.BUILD_BREAK);
	}

	private static void onAfterBlockBreak(Level level, Player player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		rewardMining(serverPlayer, state);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();
		BoxShopPoint boxShop = findBoxShopByBlock(level.dimension().location(), pos);
		if (boxShop != null && hand == InteractionHand.MAIN_HAND) {
			openBoxShop(serverPlayer, boxShop);
			return InteractionResult.SUCCESS;
		}

		Block block = level.getBlockState(pos).getBlock();
		ItemStack held = player.getItemInHand(hand);

		if (held.getItem() instanceof BlockItem) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.BUILD_BREAK)) {
				return InteractionResult.FAIL;
			}
		}

		if (isContainerBlock(level, pos)) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.CONTAINER)) {
				return InteractionResult.FAIL;
			}
		}

		if (isRedstoneInteractBlock(block)) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.INTERACT)) {
				return InteractionResult.FAIL;
			}
		}

		if (isBlockUseControlled(block)) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.BLOCK_USE)) {
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	private static InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand, net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.EntityHitResult hitResult) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (!isProtectedEntity(entity)) {
			return InteractionResult.PASS;
		}

		if (!checkTerritoryPermission(serverPlayer, entity.blockPosition(), TerritoryPermission.DAMAGE_ENTITIES)) {
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	private static int giveClock(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (player.getInventory().contains(new ItemStack(Items.CLOCK))) {
			sendError(source, "message.teleportmenu.clock_already_have");
			return 0;
		}

		ItemStack clock = new ItemStack(Items.CLOCK);
		if (!player.getInventory().add(clock)) {
			player.drop(clock, false);
		}

		sendSuccess(source, "message.teleportmenu.clock_given");
		return 1;
	}

	private static int addBoxShop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos boxPos = player.blockPosition();
		ResourceLocation dimensionId = level.dimension().location();

		if (findBoxShopByBlock(dimensionId, boxPos) != null) {
			sendError(source, "message.teleportmenu.boxshop_place_occupied");
			return 0;
		}

		UUID shopId = UUID.randomUUID();
		ShopPoint shop = new ShopPoint(
			shopId,
			player.getUUID(),
			player.getName().getString(),
			DEFAULT_BOX_SHOP_NAME,
			"minecraft:ender_chest",
			new ArrayList<>()
		);
		PLAYER_SHOPS.computeIfAbsent(player.getUUID(), key -> new ArrayList<>()).add(shop);
		BOX_SHOP_SHOP_IDS.add(shopId);
		level.setBlockAndUpdate(boxPos, Blocks.ENDER_CHEST.defaultBlockState());

		UUID boxShopId = UUID.randomUUID();
		BOX_SHOPS.put(boxShopId, new BoxShopPoint(
			boxShopId,
			shopId,
			player.getUUID(),
			player.getName().getString(),
			DEFAULT_BOX_SHOP_NAME,
			dimensionId,
			boxPos.getX(),
			boxPos.getY(),
			boxPos.getZ(),
			false
		));
		refreshBoxShopHologram(player.server, BOX_SHOPS.get(boxShopId));

		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.boxshop_created");
		return 1;
	}

	private static int cleanBoxShop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		BoxShopPoint target = findOwnedBoxShopAtFeet(player);
		if (target == null) {
			sendError(source, "message.teleportmenu.boxshop_not_found");
			return 0;
		}

		removeBoxShop(player.server, target);
		sendSuccess(source, "message.teleportmenu.boxshop_cleaned");
		return 1;
	}

	private static int showTerritoryUsage(CommandSourceStack source) {
		sendWarn(source, "message.teleportmenu.territory_usage_1");
		sendWarn(source, "message.teleportmenu.territory_usage_2");
		sendWarn(source, "message.teleportmenu.territory_usage_3");
		return 1;
	}

	private static int setTerritoryPoint(CommandSourceStack source, boolean setA) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		UUID playerId = player.getUUID();
		ResourceLocation dimensionId = player.level().dimension().location();
		BlockPos pos = player.blockPosition();

		if (setA) {
			if (isInsideOtherTerritory(playerId, dimensionId, pos)) {
				sendWarn(source, "message.teleportmenu.territory_too_close");
				return 0;
			}
			TERRITORY_SELECTIONS.put(playerId, new TerritorySelection(dimensionId, pos, null));
			sendWarn(source, "message.teleportmenu.territory_set_a", pos.getX(), pos.getY(), pos.getZ());
			sendWarn(source, "message.teleportmenu.territory_step_next_b");
			return 1;
		}

		TerritorySelection selection = TERRITORY_SELECTIONS.get(playerId);
		if (selection == null || selection.pointA() == null) {
			sendWarn(source, "message.teleportmenu.territory_need_a_first");
			return 0;
		}

		if (!selection.dimensionId().equals(dimensionId)) {
			sendWarn(source, "message.teleportmenu.territory_dimension_mismatch");
			return 0;
		}

		if (isInsideOtherTerritory(playerId, dimensionId, pos)) {
			sendWarn(source, "message.teleportmenu.territory_too_close");
			return 0;
		}

		TERRITORY_SELECTIONS.put(playerId, new TerritorySelection(dimensionId, selection.pointA(), pos));
		sendWarn(source, "message.teleportmenu.territory_set_b", pos.getX(), pos.getY(), pos.getZ());
		sendWarn(source, "message.teleportmenu.territory_open_clock_to_create");
		return 1;
	}

	private static void handleTeleportRequest(ServerPlayer source, UUID targetId) {
		if (targetId == null || targetId.equals(source.getUUID())) {
			sendError(source, "message.teleportmenu.invalid_target");
			return;
		}

		ServerPlayer target = source.server.getPlayerList().getPlayer(targetId);
		if (target == null) {
			sendError(source, "message.teleportmenu.target_offline");
			return;
		}

		source.teleportTo((ServerLevel)target.level(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
		sendSuccess(source, "message.teleportmenu.teleport_success_player", target.getDisplayName());
	}

	private static void handleTeleportHomeRequest(ServerPlayer source, UUID homeId) {
		if (homeId == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		HomePoint home = PLAYER_HOMES
			.getOrDefault(source.getUUID(), List.of())
			.stream()
			.filter(candidate -> candidate.id().equals(homeId))
			.findFirst()
			.orElse(null);

		if (home == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		source.teleportTo((ServerLevel)source.level(), home.x() + 0.5, home.y(), home.z() + 0.5, source.getYRot(), source.getXRot());
		sendSuccess(source, "message.teleportmenu.teleport_success_home", home.name(), home.x(), home.y(), home.z());
	}

	private static void handleTeleportTerritoryRequest(ServerPlayer source, UUID territoryId) {
		if (territoryId == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		TerritoryPoint territory = PLAYER_TERRITORIES
			.getOrDefault(source.getUUID(), List.of())
			.stream()
			.filter(candidate -> candidate.id().equals(territoryId))
			.findFirst()
			.orElse(null);

		if (territory == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, territory.dimensionId());
		ServerLevel targetLevel = source.server.getLevel(levelKey);
		if (targetLevel == null) {
			sendError(source, "message.teleportmenu.territory_dimension_missing");
			return;
		}

		double targetX = ((double) territory.minX() + (double) territory.maxX() + 1D) / 2D;
		double targetY = territory.maxY() + 1D;
		double targetZ = ((double) territory.minZ() + (double) territory.maxZ() + 1D) / 2D;

		source.teleportTo(targetLevel, targetX, targetY, targetZ, source.getYRot(), source.getXRot());
		sendSuccess(source, "message.teleportmenu.teleport_success_territory", territory.name());
	}

	private static void handleCreateHomeRequest(ServerPlayer source, String rawName) {
		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.home_name_empty");
			return;
		}

		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<HomePoint> homes = PLAYER_HOMES.computeIfAbsent(source.getUUID(), key -> new ArrayList<>());
		final String finalName = name;
		boolean exists = homes.stream().anyMatch(home -> home.name().equalsIgnoreCase(finalName));
		if (exists) {
			sendError(source, "message.teleportmenu.home_name_exists", finalName);
			return;
		}

		int x = source.blockPosition().getX();
		int y = source.blockPosition().getY();
		int z = source.blockPosition().getZ();
		String iconItemId = BuiltInRegistries.ITEM.getKey(randomHomeIcon(source)).toString();

		homes.add(new HomePoint(UUID.randomUUID(), finalName, x, y, z, iconItemId));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.home_created", finalName, x, y, z);
		syncHomesToClient(source);
	}

	private static void handleCreateTerritoryRequest(ServerPlayer source, String rawName) {
		TerritorySelection selection = TERRITORY_SELECTIONS.get(source.getUUID());
		if (selection == null || selection.pointA() == null || selection.pointB() == null) {
			sendError(source, "message.teleportmenu.territory_need_ab");
			return;
		}

		if (!selection.dimensionId().equals(source.level().dimension().location())) {
			sendError(source, "message.teleportmenu.territory_dimension_mismatch");
			return;
		}

		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.territory_name_empty");
			return;
		}

		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<TerritoryPoint> territories = PLAYER_TERRITORIES.computeIfAbsent(source.getUUID(), key -> new ArrayList<>());
		final String finalName = name;
		if (territories.stream().anyMatch(t -> t.name().equalsIgnoreCase(finalName))) {
			sendError(source, "message.teleportmenu.territory_name_exists", finalName);
			return;
		}

		BlockPos a = selection.pointA();
		BlockPos b = selection.pointB();
		int minX = Math.min(a.getX(), b.getX());
		int minY = Math.min(a.getY(), b.getY());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxX = Math.max(a.getX(), b.getX());
		int maxY = Math.max(a.getY(), b.getY());
		int maxZ = Math.max(a.getZ(), b.getZ());

		if (overlapsOtherTerritory(source.getUUID(), selection.dimensionId(), minX, minY, minZ, maxX, maxY, maxZ)) {
			sendError(source, "message.teleportmenu.territory_chunk_occupied");
			return;
		}

		String iconItemId = BuiltInRegistries.ITEM.getKey(randomHomeIcon(source)).toString();
		territories.add(new TerritoryPoint(
			UUID.randomUUID(),
			source.getUUID(),
			source.getName().getString(),
			finalName,
			selection.dimensionId(),
			minX,
			minY,
			minZ,
			maxX,
			maxY,
			maxZ,
			iconItemId,
			true,
			true,
			true,
			true,
			true,
			ConcurrentHashMap.newKeySet()
		));

		TERRITORY_SELECTIONS.remove(source.getUUID());
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.territory_created", finalName, minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static void handleCancelTerritoryCreate(ServerPlayer source) {
		if (TERRITORY_SELECTIONS.remove(source.getUUID()) != null) {
			sendWarn(source, "message.teleportmenu.territory_create_cancelled");
		} else {
			sendWarn(source, "message.teleportmenu.territory_nothing_to_cancel");
		}
	}

	private static void handleUpdateTerritoryPermission(ServerPlayer source, UUID territoryId, String permissionKey, boolean allowed) {
		TerritoryPoint territory = findOwnedTerritory(source, territoryId);
		if (territory == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		TerritoryPermission permission = TerritoryPermission.fromKey(permissionKey);
		if (permission == null) {
			sendError(source, "message.teleportmenu.territory_permission_invalid");
			return;
		}

		applyTerritoryPermission(territory, permission, allowed);
		persistDataIfPossible();
		sendSuccess(
			source,
			"message.teleportmenu.territory_permission_updated",
			Component.translatable(permission.translationKey()),
			allowed ? Component.translatable("screen.teleportmenu.territories.allow") : Component.translatable("screen.teleportmenu.territories.deny")
		);
	}

	private static void handleUpdateTerritoryWhitelist(ServerPlayer source, UUID territoryId, UUID playerId, boolean allowed) {
		if (playerId == null || playerId.equals(source.getUUID())) {
			sendError(source, "message.teleportmenu.territory_whitelist_invalid_player");
			return;
		}

		TerritoryPoint territory = findOwnedTerritory(source, territoryId);
		if (territory == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		if (allowed) {
			territory.whitelistPlayerIds().add(playerId);
		} else {
			territory.whitelistPlayerIds().remove(playerId);
		}
		persistDataIfPossible();
	}

	private static void handleDeleteHomeRequest(ServerPlayer source, UUID homeId) {
		if (homeId == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		List<HomePoint> homes = PLAYER_HOMES.get(source.getUUID());
		if (homes == null || homes.isEmpty()) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		HomePoint home = homes.stream().filter(candidate -> candidate.id().equals(homeId)).findFirst().orElse(null);
		if (home == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		homes.removeIf(candidate -> candidate.id().equals(homeId));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.home_deleted", home.name());
		syncHomesToClient(source);
	}

	private static void handleRenameHomeRequest(ServerPlayer source, UUID homeId, String rawName) {
		if (homeId == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.home_name_empty");
			return;
		}

		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<HomePoint> homes = PLAYER_HOMES.get(source.getUUID());
		if (homes == null || homes.isEmpty()) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		int index = -1;
		for (int i = 0; i < homes.size(); i++) {
			if (homes.get(i).id().equals(homeId)) {
				index = i;
				break;
			}
		}

		if (index < 0) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		final String finalName = name;
		boolean duplicate = homes.stream().anyMatch(home -> !home.id().equals(homeId) && home.name().equalsIgnoreCase(finalName));
		if (duplicate) {
			sendError(source, "message.teleportmenu.home_name_exists", finalName);
			return;
		}

		HomePoint old = homes.get(index);
		homes.set(index, new HomePoint(old.id(), finalName, old.x(), old.y(), old.z(), old.iconItemId()));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.home_renamed", old.name(), finalName);
		syncHomesToClient(source);
	}

	private static void handleDisableItemRequest(ServerPlayer source, String rawItemId, boolean disable) {
		if (!isAdmin(source)) {
			sendError(source, "message.teleportmenu.admin_no_permission");
			return;
		}

		String itemId = rawItemId == null ? "" : rawItemId.trim();
		if (itemId.isEmpty()) {
			sendError(source, "message.teleportmenu.item_disable_not_found");
			return;
		}

		ResourceLocation id;
		try {
			id = ResourceLocation.parse(itemId);
		} catch (IllegalArgumentException error) {
			sendError(source, "message.teleportmenu.item_disable_not_found");
			return;
		}

		Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		if (item == null || item == Items.AIR || item == Items.GRASS_BLOCK) {
			sendError(source, "message.teleportmenu.item_disable_not_found");
			return;
		}

		String canonicalId = BuiltInRegistries.ITEM.getKey(item).toString();
		if (disable) {
			if (DISABLED_ITEM_IDS.contains(canonicalId)) {
				sendError(source, "message.teleportmenu.item_disable_already", canonicalId);
				return;
			}

			DISABLED_ITEM_IDS.add(canonicalId);
			persistDataIfPossible();
			disableRecipesForItem(source.server, item);
			replaceDisabledForAllPlayers(source.server);
			syncDisabledItemsToOperators(source.server);
			sendSuccess(source, "message.teleportmenu.item_disable_success", canonicalId);
		} else {
			if (!DISABLED_ITEM_IDS.contains(canonicalId)) {
				sendError(source, "message.teleportmenu.item_enable_already", canonicalId);
				return;
			}

			DISABLED_ITEM_IDS.remove(canonicalId);
			persistDataIfPossible();
			syncDisabledItemsToOperators(source.server);
			sendSuccess(source, "message.teleportmenu.item_enable_success", canonicalId);
		}
	}

	private static void handleEconomyAdminAction(ServerPlayer source, UUID targetId, String rawAction, double amount) {
		if (!isAdmin(source)) {
			sendError(source, "message.teleportmenu.admin_no_permission");
			return;
		}

		if (!isEconomyReady()) {
			sendError(source, "message.teleportmenu.economy_missing");
			return;
		}

		if (targetId == null) {
			sendError(source, "message.teleportmenu.economy_player_offline");
			return;
		}

		ServerPlayer target = source.server.getPlayerList().getPlayer(targetId);
		if (target == null) {
			sendError(source, "message.teleportmenu.economy_player_offline");
			return;
		}

		String targetName = target.getName().getString();
		String action = rawAction == null ? "" : rawAction.trim().toLowerCase(Locale.ROOT);
		double safeAmount = Math.max(0D, amount);

		try {
			switch (action) {
				case "query" -> sendSuccess(source, "message.teleportmenu.economy_balance", targetName, economyFormat(economyGetMoney(targetName)));
				case "add" -> {
					if (!isValidMoneyAmount(safeAmount)) {
						sendError(source, "message.teleportmenu.economy_invalid_amount");
						return;
					}
					economyAdd(targetName, safeAmount);
					sendSuccess(source, "message.teleportmenu.economy_add_success", targetName, economyFormat(safeAmount));
					sendSuccess(target, "message.teleportmenu.economy_add_success", targetName, economyFormat(safeAmount));
				}
				case "subtract" -> {
					if (!isValidMoneyAmount(safeAmount)) {
						sendError(source, "message.teleportmenu.economy_invalid_amount");
						return;
					}
					economySubtract(targetName, safeAmount);
					sendSuccess(source, "message.teleportmenu.economy_subtract_success", targetName, economyFormat(safeAmount));
					sendSuccess(target, "message.teleportmenu.economy_subtract_success", targetName, economyFormat(safeAmount));
				}
				case "set" -> {
					if (!isValidMoneyAmount(safeAmount)) {
						sendError(source, "message.teleportmenu.economy_invalid_amount");
						return;
					}
					economySet(targetName, safeAmount);
					sendSuccess(source, "message.teleportmenu.economy_set_success", targetName, economyFormat(safeAmount));
					sendSuccess(target, "message.teleportmenu.economy_set_success", targetName, economyFormat(safeAmount));
				}
				case "reset" -> {
					economyReset(targetName);
					sendSuccess(source, "message.teleportmenu.economy_reset_success", targetName);
					sendSuccess(target, "message.teleportmenu.economy_reset_success", targetName);
				}
				default -> sendError(source, "message.teleportmenu.economy_invalid_amount");
			}
		} catch (Exception error) {
			LOGGER.error("Economy operation failed: action={}, player={}", action, targetName, error);
			sendError(source, "message.teleportmenu.economy_operation_failed");
		}
	}

	private static void handleCreateShop(ServerPlayer source, String rawName) {
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

	private static void handleExpandShopLimit(ServerPlayer source) {
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

	private static void handleDeleteShop(ServerPlayer source, UUID shopId) {
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

	private static void handleRenameShop(ServerPlayer source, UUID shopId, String rawName) {
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

	private static void handleRenameBoxShop(ServerPlayer source, UUID boxShopId, String rawName) {
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

	private static void handleSetShopIcon(ServerPlayer source, UUID shopId, String iconItemId) {
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

	private static void handleAddShopGood(ServerPlayer source, UUID shopId, String itemId, int stock, double price) {
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
		BoxShopPoint boxShop = findBoxShopByShopId(shop.id());
		if (boxShop != null) {
			refreshBoxShopHologram(source.server, boxShop);
		}

		persistDataIfPossible();
		String formattedPrice;
		try {
			formattedPrice = economyFormat(price);
		} catch (Exception ignored) {
			formattedPrice = String.format(java.util.Locale.ROOT, "%.2f", price);
		}
		sendSuccess(source, "message.teleportmenu.shop_good_added", new ItemStack(item).getHoverName(), stock, formattedPrice);
		syncEconomyState(source);
	}

	private static void handleBuyShopGood(ServerPlayer buyer, UUID shopId, String itemId, int amount) {
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
			BoxShopPoint boxShop = findBoxShopByShopId(shop.id());
			if (boxShop != null) {
				refreshBoxShopHologram(buyer.server, boxShop);
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

	private static void rewardMonsterKill(ServerPlayer killer) {
		if (!isEconomyReady()) {
			return;
		}

		try {
			economyAdd(killer.getName().getString(), ECONOMY_MONSTER_KILL_REWARD);
			sendSuccess(killer, "message.teleportmenu.economy_reward_kill", economyFormat(ECONOMY_MONSTER_KILL_REWARD));
		} catch (Exception error) {
			LOGGER.debug("Failed to award monster kill economy reward", error);
		}
	}

	private static void rewardMining(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state) {
		if (!isEconomyReady() || state == null) {
			return;
		}

		if (hasSilkTouch(player)) {
			return;
		}

		double reward = miningRewardFor(state.getBlock());
		if (reward <= 0D) {
			return;
		}

		try {
			economyAdd(player.getName().getString(), reward);
			sendSuccess(player, "message.teleportmenu.economy_reward_mining", economyFormat(reward));
		} catch (Exception error) {
			LOGGER.debug("Failed to award mining economy reward", error);
		}
	}

	private static boolean hasSilkTouch(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) {
			return false;
		}

		try {
			Holder<Enchantment> silkTouch = player.server.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SILK_TOUCH);
			return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, mainHand) > 0;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static double miningRewardFor(Block block) {
		if (block == null) {
			return 0D;
		}

		if (block == Blocks.ANCIENT_DEBRIS || block.builtInRegistryHolder().is(BlockTags.GOLD_ORES)) {
			return ECONOMY_SPECIAL_MINING_REWARD;
		}

		if (block.builtInRegistryHolder().is(BlockTags.DIAMOND_ORES) || block.builtInRegistryHolder().is(BlockTags.EMERALD_ORES)) {
			return ECONOMY_MINING_DIAMOND_EMERALD_REWARD;
		}
		if (block.builtInRegistryHolder().is(BlockTags.REDSTONE_ORES) || block.builtInRegistryHolder().is(BlockTags.LAPIS_ORES)) {
			return ECONOMY_MINING_REDSTONE_LAPIS_REWARD;
		}
		if (block.builtInRegistryHolder().is(BlockTags.IRON_ORES) || block.builtInRegistryHolder().is(BlockTags.COPPER_ORES)) {
			return ECONOMY_MINING_IRON_COPPER_REWARD;
		}
		if (block.builtInRegistryHolder().is(BlockTags.COAL_ORES)) {
			return ECONOMY_MINING_COAL_REWARD;
		}

		return 0D;
	}

	private static void rewardNewEquipmentInInventory(ServerPlayer player) {
		if (!isEconomyReady()) {
			return;
		}

		Map<String, Integer> current = snapshotRewardableEquipment(player);
		Map<String, Integer> previous = PLAYER_EQUIPMENT_COUNTS.put(player.getUUID(), current);
		if (previous == null) {
			return;
		}

		Set<String> rewarded = PLAYER_REWARDED_EQUIPMENT.computeIfAbsent(player.getUUID(), key -> ConcurrentHashMap.newKeySet());
		int newlyRewardedTypes = 0;
		boolean changed = false;
		for (Map.Entry<String, Integer> entry : current.entrySet()) {
			int oldValue = previous.getOrDefault(entry.getKey(), 0);
			int delta = entry.getValue() - oldValue;
			if (delta > 0 && !rewarded.contains(entry.getKey())) {
				rewarded.add(entry.getKey());
				newlyRewardedTypes++;
				changed = true;
			}
		}

		if (newlyRewardedTypes <= 0) {
			return;
		}

		double reward = ECONOMY_EQUIPMENT_REWARD * newlyRewardedTypes;
		try {
			economyAdd(player.getName().getString(), reward);
			if (changed) {
				persistDataIfPossible();
			}
			sendSuccess(player, "message.teleportmenu.economy_reward_equipment", newlyRewardedTypes, economyFormat(reward));
		} catch (Exception error) {
			LOGGER.debug("Failed to award equipment economy reward", error);
		}
	}

	private static Map<String, Integer> snapshotRewardableEquipment(ServerPlayer player) {
		Map<String, Integer> snapshot = new HashMap<>();
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !isRewardableEquipment(stack.getItem())) {
				continue;
			}
			String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			snapshot.merge(id, stack.getCount(), Integer::sum);
		}
		return snapshot;
	}

	private static boolean isRewardableEquipment(Item item) {
		return item instanceof ArmorItem
			|| item instanceof SwordItem
			|| item instanceof AxeItem
			|| item instanceof TieredItem
			|| item instanceof BowItem
			|| item instanceof CrossbowItem
			|| item instanceof TridentItem
			|| item instanceof ShieldItem;
	}

	private static boolean isValidMoneyAmount(double amount) {
		return Double.isFinite(amount) && amount >= 0D;
	}

	private static boolean isEconomyReady() {
		if (!ECONOMY_MOD_LOADED) {
			return false;
		}

		if (ECONOMY_API_INITIALIZED) {
			return ECONOMY_API_READY;
		}
		ECONOMY_API_INITIALIZED = true;

		try {
			Class<?> apiClass = Class.forName("me.isaiah.mods.economy.api.Economy");
			ECONOMY_GET_MONEY = apiClass.getMethod("getMoney", String.class);
			ECONOMY_ADD = apiClass.getMethod("add", String.class, double.class);
			ECONOMY_SUBTRACT = apiClass.getMethod("subtract", String.class, double.class);
			ECONOMY_SET = apiClass.getMethod("setMoney", String.class, double.class);
			ECONOMY_RESET = apiClass.getMethod("resetBalance", String.class);
			ECONOMY_FORMAT = apiClass.getMethod("format", double.class);
			ECONOMY_API_READY = true;
		} catch (Exception error) {
			ECONOMY_API_READY = false;
			LOGGER.warn("Economy mod is installed but API binding failed", error);
		}

		return ECONOMY_API_READY;
	}

	private static void loadServerLocalizations() {
		Map<String, Map<String, String>> loaded = new HashMap<>();
		loaded.put("zh_cn", readLocalizationResource("assets/teleportmenu/lang/zh_cn.json"));
		loaded.put("en_us", readLocalizationResource("assets/teleportmenu/lang/en_us.json"));

		SERVER_LOCALIZATIONS.clear();
		SERVER_LOCALIZATIONS.putAll(loaded);
		LOCALIZATION_VERSION = computeLocalizationVersion(loaded);
	}

	private static Map<String, String> readLocalizationResource(String resourcePath) {
		try (InputStream input = TeleportMenuMod.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (input == null) {
				LOGGER.warn("Missing localization resource: {}", resourcePath);
				return Map.of();
			}
			String raw = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
			Map<String, String> map = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					map.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			return Map.copyOf(map);
		} catch (Exception error) {
			LOGGER.error("Failed to load localization resource: {}", resourcePath, error);
			return Map.of();
		}
	}

	private static long computeLocalizationVersion(Map<String, Map<String, String>> packs) {
		long hash = 1125899906842597L;
		for (String languageCode : List.of("zh_cn", "en_us")) {
			hash = 31 * hash + languageCode.hashCode();
			Map<String, String> entries = packs.getOrDefault(languageCode, Map.of());
			List<String> keys = new ArrayList<>(entries.keySet());
			keys.sort(String::compareTo);
			for (String key : keys) {
				hash = 31 * hash + key.hashCode();
				hash = 31 * hash + entries.get(key).hashCode();
			}
		}
		return hash;
	}

	private static void syncLocalizations(ServerPlayer player) {
		// Disabled: keep UI localization bundled client-side to avoid protocol mismatch on join.
	}

	private static double economyGetMoney(String playerName) throws InvocationTargetException, IllegalAccessException {
		return (double) ECONOMY_GET_MONEY.invoke(null, playerName);
	}

	private static void economyAdd(String playerName, double amount) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_ADD.invoke(null, playerName, amount);
	}

	private static void economySubtract(String playerName, double amount) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_SUBTRACT.invoke(null, playerName, amount);
	}

	private static void economySet(String playerName, double amount) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_SET.invoke(null, playerName, amount);
	}

	private static void economyReset(String playerName) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_RESET.invoke(null, playerName);
	}

	private static String economyFormat(double amount) throws InvocationTargetException, IllegalAccessException {
		Object formatted = ECONOMY_FORMAT.invoke(null, amount);
		return formatted == null ? String.format(Locale.ROOT, "%.2f", amount) : formatted.toString();
	}

	private static void onServerTick(MinecraftServer server) {
		SERVER_TICK++;
		if (SERVER_TICK % 20L == 0L) {
			refreshEconomySidebar(server);
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			try {
				if (player == null || player.isRemoved() || player.level() == null) {
					continue;
				}

				replaceDisabledInInventory(player);
				rewardNewEquipmentInInventory(player);
				updateTerritoryHud(player);
				if (SERVER_TICK % 5L == 0L) {
					showTerritorySelectionParticles(player);
				}
			} catch (Exception error) {
				LOGGER.debug("Skip ticking player during shutdown/transition: {}", player != null ? player.getName().getString() : "null", error);
			}
		}
	}

	private static void refreshEconomySidebar(MinecraftServer server) {
		if (!isEconomyReady()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyEconomySidebarState(server, player.getUUID());
		}
	}

	public static void setEconomySidebarVisible(MinecraftServer server, UUID playerId, boolean visible) {
		if (server == null || playerId == null) {
			return;
		}

		runEconomySidebarOnServerThread(server, () -> {
			if (visible) {
				ECONOMY_SIDEBAR_HIDDEN.remove(playerId);
			} else {
				ECONOMY_SIDEBAR_HIDDEN.add(playerId);
				ECONOMY_SIDEBAR_LAST_SCORE.remove(playerId);
				ECONOMY_SIDEBAR_ASSERTED.remove(playerId);
				ECONOMY_SIDEBAR_HIDDEN_SENT.remove(playerId);
			}
			persistDataIfPossible();
			applyEconomySidebarState(server, playerId);
		});
	}

	private static void runEconomySidebarOnServerThread(MinecraftServer server, Runnable operation) {
		if (server == null || operation == null) {
			return;
		}
		if (server.isSameThread()) {
			operation.run();
		} else {
			server.execute(operation);
		}
	}

	private static void applyEconomySidebarState(MinecraftServer server, UUID playerId) {
		if (server == null || playerId == null) {
			return;
		}
		if (!server.isSameThread()) {
			runEconomySidebarOnServerThread(server, () -> applyEconomySidebarState(server, playerId));
			return;
	}

	ServerPlayer player = server.getPlayerList().getPlayer(playerId);
	if (player == null || player.isRemoved()) {
		return;
	}

	if (ECONOMY_SIDEBAR_HIDDEN.contains(playerId)) {
		if (ECONOMY_SIDEBAR_HIDDEN_SENT.add(playerId)) {
			player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
		}
		return;
	}

	ECONOMY_SIDEBAR_HIDDEN_SENT.remove(playerId);
	try {
		Scoreboard scoreboard = server.getScoreboard();
		String objectiveName = economySidebarObjectiveName(playerId);
		Objective objective = scoreboard.getObjective(objectiveName);
		boolean created = objective == null;
		if (created) {
			objective = scoreboard.addObjective(
				objectiveName,
				ObjectiveCriteria.DUMMY,
				Component.literal("\u25C6 \u91D1\u5E01\u4F59\u989D"),
				ObjectiveCriteria.RenderType.INTEGER,
				false,
				null
			);
		}

		double money = economyGetMoney(player.getName().getString());
		int score = (int) Math.max(
			Integer.MIN_VALUE,
			Math.min(Integer.MAX_VALUE, Math.round(money))
		);
		Integer previous = ECONOMY_SIDEBAR_LAST_SCORE.put(playerId, score);

		if (created) {
			player.connection.send(new ClientboundSetObjectivePacket(
				objective,
				ClientboundSetObjectivePacket.METHOD_ADD
			));
		}

		if (created || previous == null || previous != score) {
			scoreboard.getOrCreatePlayerScore(
				ScoreHolder.forNameOnly(ECONOMY_SIDEBAR_ENTRY),
				objective
			).set(score);
			player.connection.send(new ClientboundSetScorePacket(
				ECONOMY_SIDEBAR_ENTRY,
				objective.getName(),
				score,
				Optional.empty(),
				Optional.empty()
			));
		}

		if (ECONOMY_SIDEBAR_ASSERTED.add(playerId)) {
			player.connection.send(new ClientboundSetDisplayObjectivePacket(
				DisplaySlot.SIDEBAR,
				objective
			));
		}
	} catch (Exception error) {
		LOGGER.debug("Failed to apply economy sidebar for {}", player.getName().getString(), error);
	}
	}

	private static String economySidebarObjectiveName(UUID playerId) {
		String compact = playerId.toString().replace("-", "");
		return "te2_" + compact.substring(0, 12);
	}

	private static void showTerritorySelectionParticles(ServerPlayer player) {
		TerritorySelection selection = TERRITORY_SELECTIONS.get(player.getUUID());
		if (selection == null || selection.pointA() == null || selection.pointB() == null) {
			return;
		}

		if (!selection.dimensionId().equals(player.level().dimension().location())) {
			return;
		}

		spawnSelectionBox(player, selection.pointA(), selection.pointB());
	}

	private static void spawnSelectionBox(ServerPlayer player, BlockPos a, BlockPos b) {
		ServerLevel level = (ServerLevel) player.level();
		int minX = Math.min(a.getX(), b.getX());
		int minY = Math.min(a.getY(), b.getY());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxX = Math.max(a.getX(), b.getX());
		int maxY = Math.max(a.getY(), b.getY());
		int maxZ = Math.max(a.getZ(), b.getZ());

		int maxSpan = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
		int step = Math.max(1, maxSpan / 48 + 1);

		for (int x = minX; x <= maxX; x += step) {
			spawnEdgeColumn(level, player, x, minY, maxY, minZ, maxZ, step);
		}
		for (int y = minY; y <= maxY; y += step) {
			spawnEdgeRow(level, player, minX, maxX, y, minZ, maxZ, step);
		}
		for (int z = minZ; z <= maxZ; z += step) {
			spawnEdgeDepth(level, player, minX, maxX, minY, maxY, z, step);
		}

		spawnBoxCorner(level, player, minX, minY, minZ);
		spawnBoxCorner(level, player, minX, minY, maxZ);
		spawnBoxCorner(level, player, minX, maxY, minZ);
		spawnBoxCorner(level, player, minX, maxY, maxZ);
		spawnBoxCorner(level, player, maxX, minY, minZ);
		spawnBoxCorner(level, player, maxX, minY, maxZ);
		spawnBoxCorner(level, player, maxX, maxY, minZ);
		spawnBoxCorner(level, player, maxX, maxY, maxZ);
	}

	private static void spawnEdgeColumn(ServerLevel level, ServerPlayer player, int x, int minY, int maxY, int minZ, int maxZ, int step) {
		for (int y = minY; y <= maxY; y += step) {
			spawnBoxCorner(level, player, x, y, minZ);
			spawnBoxCorner(level, player, x, y, maxZ);
		}
	}

	private static void spawnEdgeRow(ServerLevel level, ServerPlayer player, int minX, int maxX, int y, int minZ, int maxZ, int step) {
		for (int x = minX; x <= maxX; x += step) {
			spawnBoxCorner(level, player, x, y, minZ);
			spawnBoxCorner(level, player, x, y, maxZ);
		}
	}

	private static void spawnEdgeDepth(ServerLevel level, ServerPlayer player, int minX, int maxX, int minY, int maxY, int z, int step) {
		for (int x = minX; x <= maxX; x += step) {
			spawnBoxCorner(level, player, x, minY, z);
			spawnBoxCorner(level, player, x, maxY, z);
		}
	}

	private static void spawnBoxCorner(ServerLevel level, ServerPlayer player, int x, int y, int z) {
		level.sendParticles(player, TERRITORY_SELECTION_PARTICLE, true, x + 0.5D, y + 0.5D, z + 0.5D, 1, 0D, 0D, 0D, 0D);
	}

	private static void updateTerritoryHud(ServerPlayer player) {
		UUID playerId = player.getUUID();
		TerritoryPoint inTerritory = findTerritoryContaining(player);
		TerritoryHudState state = TERRITORY_HUD_STATES.get(playerId);

		if (inTerritory == null) {
			if (state != null) {
				if (SERVER_TICK < state.clearAtTick()) {
					player.displayClientMessage(Component.empty(), true);
				}
				TERRITORY_HUD_STATES.remove(playerId);
			}
			return;
		}

		if (state == null || !inTerritory.id().equals(state.territoryId())) {
			if (inTerritory.ownerId().equals(player.getUUID())) {
				player.displayClientMessage(Component.translatable("message.teleportmenu.territory_owner_welcome", inTerritory.name(), player.getName().getString()), true);
			} else {
				player.displayClientMessage(Component.translatable("message.teleportmenu.territory_entered", inTerritory.ownerName()), true);
			}
			TERRITORY_HUD_STATES.put(playerId, new TerritoryHudState(inTerritory.id(), SERVER_TICK + 40L));
			return;
		}

		if (SERVER_TICK >= state.clearAtTick()) {
			player.displayClientMessage(Component.empty(), true);
			TERRITORY_HUD_STATES.put(playerId, new TerritoryHudState(state.territoryId(), Long.MAX_VALUE));
		}
	}

	private static TerritoryPoint findTerritoryContaining(ServerPlayer player) {
		ResourceLocation currentDimension = player.level().dimension().location();
		BlockPos pos = player.blockPosition();

		for (List<TerritoryPoint> territories : PLAYER_TERRITORIES.values()) {
			for (TerritoryPoint territory : territories) {
				if (!territory.dimensionId().equals(currentDimension)) {
					continue;
				}
				if (territory.contains(pos.getX(), pos.getY(), pos.getZ())) {
					return territory;
				}
			}
		}

		return null;
	}

	private static boolean checkTerritoryPermission(ServerPlayer player, BlockPos pos, TerritoryPermission permission) {
		TerritoryPoint territory = findTerritoryContaining(player.level().dimension().location(), pos);
		if (territory == null) {
			return true;
		}

		if (territory.ownerId().equals(player.getUUID()) || territory.whitelistPlayerIds().contains(player.getUUID())) {
			return true;
		}

		boolean allowed = switch (permission) {
			case BUILD_BREAK -> territory.buildBreakAllowed();
			case CONTAINER -> territory.containerAllowed();
			case INTERACT -> territory.interactAllowed();
			case DAMAGE_ENTITIES -> territory.damageEntitiesAllowed();
			case BLOCK_USE -> territory.blockUseAllowed();
		};

		if (!allowed) {
			showNoPermissionActionbar(player);
		}
		return allowed;
	}

	private static TerritoryPoint findTerritoryContaining(ResourceLocation dimensionId, BlockPos pos) {
		for (List<TerritoryPoint> territories : PLAYER_TERRITORIES.values()) {
			for (TerritoryPoint territory : territories) {
				if (!territory.dimensionId().equals(dimensionId)) {
					continue;
				}
				if (territory.contains(pos.getX(), pos.getY(), pos.getZ())) {
					return territory;
				}
			}
		}
		return null;
	}

	private static boolean isInsideOtherTerritory(UUID ownerId, ResourceLocation dimensionId, BlockPos pos) {
		for (Map.Entry<UUID, List<TerritoryPoint>> entry : PLAYER_TERRITORIES.entrySet()) {
			if (entry.getKey().equals(ownerId)) {
				continue;
			}
			for (TerritoryPoint territory : entry.getValue()) {
				if (!territory.dimensionId().equals(dimensionId)) {
					continue;
				}
				if (territory.contains(pos.getX(), pos.getY(), pos.getZ())) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean overlapsOtherTerritory(UUID ownerId, ResourceLocation dimensionId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		for (Map.Entry<UUID, List<TerritoryPoint>> entry : PLAYER_TERRITORIES.entrySet()) {
			if (entry.getKey().equals(ownerId)) {
				continue;
			}
			for (TerritoryPoint territory : entry.getValue()) {
				if (!territory.dimensionId().equals(dimensionId)) {
					continue;
				}
				boolean overlapX = maxX >= territory.minX() && minX <= territory.maxX();
				boolean overlapY = maxY >= territory.minY() && minY <= territory.maxY();
				boolean overlapZ = maxZ >= territory.minZ() && minZ <= territory.maxZ();
				if (overlapX && overlapY && overlapZ) {
					return true;
				}
			}
		}
		return false;
	}

	private static void showNoPermissionActionbar(ServerPlayer player) {
		long last = LAST_DENY_ACTIONBAR_TICK.getOrDefault(player.getUUID(), -100L);
		if (SERVER_TICK - last < 10L) {
			return;
		}
		LAST_DENY_ACTIONBAR_TICK.put(player.getUUID(), SERVER_TICK);
		player.displayClientMessage(Component.translatable("message.teleportmenu.no_permission_action").withStyle(ChatFormatting.RED), true);
	}

	private static boolean isContainerBlock(Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof Container) {
			return true;
		}

		Block block = level.getBlockState(pos).getBlock();
		return block instanceof CraftingTableBlock;
	}

	private static boolean isRedstoneInteractBlock(Block block) {
		return block instanceof LeverBlock || block instanceof ButtonBlock;
	}

	private static boolean isBlockUseControlled(Block block) {
		return block instanceof BedBlock
			|| block instanceof DoorBlock
			|| block instanceof TrapDoorBlock
			|| block instanceof FenceGateBlock
			|| block instanceof CraftingTableBlock;
	}

	private static boolean isProtectedEntity(net.minecraft.world.entity.Entity entity) {
		if (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
			return true;
		}
		return entity instanceof Animal;
	}

	private static TerritoryPoint findOwnedTerritory(ServerPlayer owner, UUID territoryId) {
		if (territoryId == null) {
			return null;
		}
		return PLAYER_TERRITORIES
			.getOrDefault(owner.getUUID(), List.of())
			.stream()
			.filter(candidate -> candidate.id().equals(territoryId))
			.findFirst()
			.orElse(null);
	}

	private static void applyTerritoryPermission(TerritoryPoint territory, TerritoryPermission permission, boolean allowed) {
		switch (permission) {
			case BUILD_BREAK -> territory.setBuildBreakAllowed(allowed);
			case CONTAINER -> territory.setContainerAllowed(allowed);
			case INTERACT -> territory.setInteractAllowed(allowed);
			case DAMAGE_ENTITIES -> territory.setDamageEntitiesAllowed(allowed);
			case BLOCK_USE -> territory.setBlockUseAllowed(allowed);
		}
	}

	private static void replaceDisabledForAllPlayers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			replaceDisabledInInventory(player);
		}
	}

	private static void replaceDisabledInInventory(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		boolean changed = false;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !isDisabledItem(stack.getItem())) {
				continue;
			}

			int replacementCount = Math.max(1, Math.min(64, stack.getCount()));
			inventory.setItem(slot, new ItemStack(Blocks.GRASS_BLOCK, replacementCount));
			changed = true;
		}

		AbstractContainerMenu menu = player.containerMenu;
		if (menu != null) {
			ItemStack carried = menu.getCarried();
			if (!carried.isEmpty() && isDisabledItem(carried.getItem())) {
				menu.setCarried(new ItemStack(Blocks.GRASS_BLOCK, Math.max(1, Math.min(64, carried.getCount()))));
				changed = true;
			}
		}

		if (changed) {
			inventory.setChanged();
			if (menu != null) {
				menu.broadcastChanges();
			}
		}
	}

	private static void disableRecipesForItem(MinecraftServer server, Item disabledItem) {
		RegistryAccess registryAccess = server.registryAccess();
		List<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes().stream()
			.filter(recipe -> recipe.value().getResultItem(registryAccess).is(disabledItem))
			.toList();

		if (recipes.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.getRecipeBook().removeRecipes(recipes, player);
		}
	}

	private static void syncDisabledItemsToOperators(MinecraftServer server) {
		List<String> disabledIds = getDisabledItemIdList();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isAdmin(player) || !ServerPlayNetworking.canSend(player, TeleportMenuPayloads.DisabledItemListPayload.TYPE)) {
				continue;
			}
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.DisabledItemListPayload(disabledIds));
		}
	}

	private static boolean isDisabledItem(Item item) {
		if (item == Items.AIR || item == Items.GRASS_BLOCK) {
			return false;
		}
		return DISABLED_ITEM_IDS.contains(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	private static boolean isAdmin(ServerPlayer player) {
		return player.hasPermissions(2);
	}

	private static List<String> getDisabledItemIdList() {
		return DISABLED_ITEM_IDS.stream().sorted(Comparator.naturalOrder()).toList();
	}

	private static void syncHomesToClient(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, TeleportMenuPayloads.HomeListPayload.TYPE)) {
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.HomeListPayload(getHomeTargets(player)));
		}
	}

	private static void syncEconomyState(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.EconomyStatePayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, new TeleportMenuPayloads.EconomyStatePayload(
			getLeaderboardEntries(player.server),
			getAllShopTargets(),
			countOwnedNormalShops(player.getUUID()),
			PLAYER_SHOP_LIMITS.getOrDefault(player.getUUID(), DEFAULT_SHOP_LIMIT)
		));
	}

	private static List<TeleportMenuPayloads.EconomyLeaderboardEntry> getLeaderboardEntries(MinecraftServer server) {
		List<TeleportMenuPayloads.EconomyLeaderboardEntry> entries = new ArrayList<>();
		for (ServerPlayer online : server.getPlayerList().getPlayers()) {
			double balance = 0D;
			String formatted = "0";
			if (isEconomyReady()) {
				try {
					balance = economyGetMoney(online.getName().getString());
					formatted = economyFormat(balance);
				} catch (Exception ignored) {
				}
			}
			entries.add(new TeleportMenuPayloads.EconomyLeaderboardEntry(
				online.getUUID(),
				online.getName().getString(),
				balance,
				formatted
			));
		}
		entries.sort((a, b) -> Double.compare(b.balance(), a.balance()));
		return entries;
	}

	private static List<TeleportMenuPayloads.ShopTarget> getAllShopTargets() {
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

	private static int countOwnedNormalShops(UUID ownerId) {
		int count = 0;
		for (ShopPoint shop : PLAYER_SHOPS.getOrDefault(ownerId, List.of())) {
			if (!isBoxShopShop(shop.id())) {
				count++;
			}
		}
		return count;
	}

	private static boolean isBoxShopShop(UUID shopId) {
		if (shopId == null) {
			return false;
		}
		return BOX_SHOP_SHOP_IDS.contains(shopId);
	}

	private static BoxShopPoint findBoxShopByShopId(UUID shopId) {
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

	private static ShopPoint findOwnedShop(ServerPlayer owner, UUID shopId) {
		if (shopId == null) {
			return null;
		}
		return PLAYER_SHOPS.getOrDefault(owner.getUUID(), List.of()).stream()
			.filter(shop -> shop.id().equals(shopId))
			.findFirst().orElse(null);
	}

	private static ShopPoint findAnyShop(UUID shopId) {
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

	private static BoxShopPoint findBoxShopByBlock(ResourceLocation dimensionId, BlockPos pos) {
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

	private static BoxShopPoint findOwnedBoxShopAtFeet(ServerPlayer player) {
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

	private static void cleanupBoxShopDisplayArtifacts(BoxShopPoint boxShop, ServerLevel level, @org.jetbrains.annotations.Nullable UUID keeperId) {
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

	private static boolean isLegacyBoxShopDisplay(Display display) {
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

	private static void cleanupLegacyBoxShopDisplaysInChunk(ServerLevel level, LevelChunk chunk) {
		if (level == null || chunk == null) {
			return;
		}
		AABB area = new AABB(
			chunk.getPos().getMinBlockX(), level.getMinBuildHeight(), chunk.getPos().getMinBlockZ(),
			chunk.getPos().getMaxBlockX() + 1D, level.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ() + 1D
		);
		for (Display.TextDisplay textDisplay : level.getEntitiesOfClass(Display.TextDisplay.class, area, TeleportMenuMod::isLegacyBoxShopDisplay)) {
			textDisplay.discard();
		}
	}

	private static void cleanupAllLegacyBoxShopDisplays(MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (Display.TextDisplay textDisplay : level.getEntitiesOfClass(
				Display.TextDisplay.class,
				new AABB(-3.0E7D, level.getMinBuildHeight(), -3.0E7D, 3.0E7D, level.getMaxBuildHeight(), 3.0E7D),
				TeleportMenuMod::isLegacyBoxShopDisplay
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

	private static Component buildBoxShopHologramText(BoxShopPoint boxShop, ShopPoint shop) {
		int goodsCount = 0;
		if (shop != null) {
			for (ShopGood good : shop.goods()) {
				goodsCount += Math.max(0, good.stock());
			}
		}

		return Component.literal(sanitizeBoxShopName(boxShop.name()) + "\n\u5546\u54c1\u6570\u91cf: " + goodsCount);
	}

	private static void refreshBoxShopHologram(MinecraftServer server, BoxShopPoint boxShop) {
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

	private static void ensureBoxShopBlock(MinecraftServer server, BoxShopPoint boxShop) {
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

	private static void updateBoxShopBlock(MinecraftServer server, UUID boxShopId) {
		BoxShopPoint boxShop = BOX_SHOPS.get(boxShopId);
		if (boxShop == null) {
			return;
		}
		ensureBoxShopBlock(server, boxShop);
	}

	private static void removeBoxShop(MinecraftServer server, BoxShopPoint boxShop) {
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

	private static void openBoxShop(ServerPlayer player, BoxShopPoint boxShop) {
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

	private static void handleQueryBoxShopHud(ServerPlayer player, int x, int y, int z) {
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

	private static void handleRequestMomentFeed(ServerPlayer player) {
		sendMomentFeed(player);
	}

	private static void handlePostMoment(ServerPlayer player, String rawContent) {
		if (player == null) {
			return;
		}

		String content = sanitizeMomentContent(rawContent);
		if (content.isBlank()) {
			sendError(player, "message.teleportmenu.moment_empty");
			return;
		}

		MomentPoint moment = new MomentPoint(
			UUID.randomUUID(),
			player.getUUID(),
			player.getName().getString(),
			content,
			System.currentTimeMillis()
		);
		MOMENTS.put(moment.id(), moment);
		MOMENT_DRAFTS.remove(player.getUUID());
		trimMoments();
		persistDataIfPossible();
		sendSuccess(player, "message.teleportmenu.moment_posted");
		broadcastMomentFeeds();
	}

	private static void handleLikeMoment(ServerPlayer player, UUID momentId) {
		if (player == null || momentId == null) {
			return;
		}

		MomentPoint moment = MOMENTS.get(momentId);
		if (moment == null) {
			sendError(player, "message.teleportmenu.moment_not_found");
			return;
		}

		moment.toggleLike(player.getUUID());
		persistDataIfPossible();
		broadcastMomentFeeds();
	}

	private static void handleDeleteMoment(ServerPlayer player, UUID momentId) {
		if (player == null || momentId == null) {
			return;
		}

		MomentPoint moment = MOMENTS.remove(momentId);
		if (moment == null) {
			sendError(player, "message.teleportmenu.moment_not_found");
			sendMomentFeed(player);
			return;
		}

		persistDataIfPossible();
		sendSuccess(player, "message.teleportmenu.moment_deleted");
		broadcastMomentFeeds();
	}

	private static void handleSaveMomentDraft(ServerPlayer player, String rawContent) {
		if (player == null) {
			return;
		}

		String content = rawContent == null ? "" : rawContent;
		if (content.isBlank()) {
			MOMENT_DRAFTS.remove(player.getUUID());
			sendSuccess(player, "message.teleportmenu.moment_draft_cleared");
		} else {
			MOMENT_DRAFTS.put(player.getUUID(), truncateMomentContent(content));
			sendSuccess(player, "message.teleportmenu.moment_draft_saved");
		}

		persistDataIfPossible();
		sendMomentFeed(player);
	}

	private static void sendMomentFeed(ServerPlayer player) {
		if (player == null || player.server == null) {
			return;
		}
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.MomentFeedPayload.TYPE)) {
			return;
		}

		ServerPlayNetworking.send(player, buildMomentFeed(player));
	}

	private static TeleportMenuPayloads.MomentFeedPayload buildMomentFeed(ServerPlayer viewer) {
		UUID viewerId = viewer.getUUID();
		List<TeleportMenuPayloads.MomentEntry> entries = new ArrayList<>();
		for (MomentPoint moment : MOMENTS.values()) {
			entries.add(new TeleportMenuPayloads.MomentEntry(
				moment.id(),
				moment.authorId(),
				moment.authorName(),
				moment.content(),
				moment.createdAt(),
				moment.likedBy().size(),
				moment.likedBy().contains(viewerId),
				moment.authorId().equals(viewerId)
			));
		}

		entries.sort(Comparator.comparingLong(TeleportMenuPayloads.MomentEntry::createdAt).reversed());
		return new TeleportMenuPayloads.MomentFeedPayload(entries, MOMENT_DRAFTS.getOrDefault(viewerId, ""));
	}

	private static void broadcastMomentFeeds() {
		MinecraftServer server = ACTIVE_SERVER;
		if (server == null) {
			return;
		}

		for (ServerPlayer online : server.getPlayerList().getPlayers()) {
			sendMomentFeed(online);
		}
	}

	private static void trimMoments() {
		if (MOMENTS.size() <= MOMENT_MAX_COUNT) {
			return;
		}

		List<MomentPoint> sorted = new ArrayList<>(MOMENTS.values());
		sorted.sort(Comparator.comparingLong(MomentPoint::createdAt));
		int removeCount = sorted.size() - MOMENT_MAX_COUNT;
		for (int i = 0; i < removeCount; i++) {
			MOMENTS.remove(sorted.get(i).id());
		}
	}

	private static String sanitizeMomentContent(String rawContent) {
		return MomentContent.sanitize(rawContent);
	}

	private static String truncateMomentContent(String content) {
		return MomentContent.truncate(content);
	}

	private static int countItemInInventory(Inventory inventory, Item item) {
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static void removeItemFromInventory(Inventory inventory, Item item, int amount) {
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

	private static List<TeleportMenuPayloads.HomeTarget> getHomeTargets(ServerPlayer player) {
		return PLAYER_HOMES
			.getOrDefault(player.getUUID(), List.of())
			.stream()
			.map(home -> new TeleportMenuPayloads.HomeTarget(home.id(), home.name(), home.x(), home.y(), home.z(), home.iconItemId()))
			.toList();
	}

	private static List<TeleportMenuPayloads.TerritoryTarget> getTerritoryTargets(ServerPlayer player) {
		return PLAYER_TERRITORIES
			.getOrDefault(player.getUUID(), List.of())
			.stream()
			.map(territory -> new TeleportMenuPayloads.TerritoryTarget(
				territory.id(),
				territory.name(),
				territory.ownerName(),
				territory.minX(),
				territory.minY(),
				territory.minZ(),
				territory.maxX(),
				territory.maxY(),
				territory.maxZ(),
				territory.iconItemId(),
				territory.buildBreakAllowed(),
				territory.containerAllowed(),
				territory.interactAllowed(),
				territory.damageEntitiesAllowed(),
				territory.blockUseAllowed(),
				List.copyOf(territory.whitelistPlayerIds())
			))
			.toList();
	}

	private static Item randomHomeIcon(ServerPlayer player) {
		return HOME_ICON_POOL.get(player.getRandom().nextInt(HOME_ICON_POOL.size()));
	}

	private static Component coloredTranslatable(ChatFormatting color, String key, Object... args) {
		return Component.translatable(key, args).withStyle(color);
	}

	private static void closeTeleportMenuIfOpen(ServerPlayer player) {
		if (player == null) {
			return;
		}
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.CloseMenuPayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, new TeleportMenuPayloads.CloseMenuPayload());
	}

	private static void sendSuccess(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(coloredTranslatable(ChatFormatting.GREEN, key, args));
	}

	private static void sendError(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(coloredTranslatable(ChatFormatting.RED, key, args));
	}

	private static void sendWarn(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(coloredTranslatable(ChatFormatting.YELLOW, key, args));
	}

	private static void sendSuccess(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		source.sendSuccess(() -> coloredTranslatable(ChatFormatting.GREEN, key, args), false);
	}

	private static void sendError(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		source.sendFailure(coloredTranslatable(ChatFormatting.RED, key, args));
	}

	private static void sendWarn(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		source.sendSuccess(() -> coloredTranslatable(ChatFormatting.YELLOW, key, args), false);
	}

	private record TerritorySelection(ResourceLocation dimensionId, BlockPos pointA, BlockPos pointB) {
	}

	private record TerritoryHudState(UUID territoryId, long clearAtTick) {
	}


}
