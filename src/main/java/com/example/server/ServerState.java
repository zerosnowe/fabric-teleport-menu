package com.example.server;

import com.example.server.model.BoxShopPoint;
import com.example.server.model.HomePoint;
import com.example.server.model.MomentPoint;
import com.example.server.model.ShopPoint;
import com.example.server.model.TerritoryPoint;
import com.example.server.storage.ServerDataStore;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory aggregate root for everything the server keeps between ticks.
 */
public final class ServerState {
	public static final Map<UUID, List<HomePoint>> PLAYER_HOMES = new ConcurrentHashMap<>();
	public static final Map<UUID, List<TerritoryPoint>> PLAYER_TERRITORIES = new ConcurrentHashMap<>();
	public static final Map<UUID, List<ShopPoint>> PLAYER_SHOPS = new ConcurrentHashMap<>();
	public static final Map<UUID, BoxShopPoint> BOX_SHOPS = new ConcurrentHashMap<>();
	public static final Set<UUID> BOX_SHOP_SHOP_IDS = ConcurrentHashMap.newKeySet();
	public static final Map<UUID, UUID> BOX_SHOP_HOLOGRAMS = new ConcurrentHashMap<>();
	public static final Map<UUID, Integer> PLAYER_SHOP_LIMITS = new ConcurrentHashMap<>();
	public static final Map<UUID, MomentPoint> MOMENTS = new ConcurrentHashMap<>();
	public static final Map<UUID, String> MOMENT_DRAFTS = new ConcurrentHashMap<>();
	public static final Map<UUID, TerritorySelection> TERRITORY_SELECTIONS = new ConcurrentHashMap<>();
	public static final Map<UUID, TerritoryHudState> TERRITORY_HUD_STATES = new ConcurrentHashMap<>();
	public static final Map<UUID, Long> LAST_DENY_ACTIONBAR_TICK = new ConcurrentHashMap<>();
	public static final Map<UUID, Map<String, Integer>> PLAYER_EQUIPMENT_COUNTS = new ConcurrentHashMap<>();
	public static final Map<UUID, Set<String>> PLAYER_REWARDED_EQUIPMENT = new ConcurrentHashMap<>();
	public static final Map<UUID, Integer> PLAYER_UI_PROTOCOLS = new ConcurrentHashMap<>();
	public static final Set<String> DISABLED_ITEM_IDS = ConcurrentHashMap.newKeySet();
	public static final Set<UUID> ECONOMY_SIDEBAR_HIDDEN = ConcurrentHashMap.newKeySet();
	public static final Map<UUID, Integer> ECONOMY_SIDEBAR_LAST_SCORE = new ConcurrentHashMap<>();
	public static final Set<UUID> ECONOMY_SIDEBAR_ASSERTED = ConcurrentHashMap.newKeySet();
	public static final Set<UUID> ECONOMY_SIDEBAR_HIDDEN_SENT = ConcurrentHashMap.newKeySet();

	public static ServerDataStore STORAGE = null;
	public static MinecraftServer ACTIVE_SERVER = null;
	public static long SERVER_TICK = 0L;

	private ServerState() {
	}

	/**
	 * Drops persisted aggregates before a fresh database load.
	 */
	public static void clearPersistent() {
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

	/**
	 * Drops every runtime map when the server stops without touching persisted data.
	 */
	public static void clearRuntime() {
		ACTIVE_SERVER = null;
		STORAGE = null;
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
	}

	public record TerritorySelection(ResourceLocation dimensionId, BlockPos pointA, BlockPos pointB) {
	}

	public record TerritoryHudState(UUID territoryId, long clearAtTick) {
	}
}
