package com.example.server.service;

import com.example.server.ServerState;
import com.example.server.model.BoxShopPoint;
import com.example.server.storage.DatabaseManager;
import com.example.server.storage.JsonDataImporter;
import com.example.server.storage.PersistedSnapshot;
import com.example.server.storage.ServerDataStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static com.example.server.ServerLog.LOGGER;
import static com.example.server.ServerState.*;
import static com.example.server.service.BoxShopService.ensureBoxShopBlock;
import static com.example.server.service.BoxShopService.refreshBoxShopHologram;
import static com.example.server.service.ShopService.DEFAULT_SHOP_LIMIT;

/**
 * Owns SQLite lifecycle: locating the database, loading aggregates and saving snapshots.
 */
public final class PersistenceService {
	private static final String DATA_DIR_NAME = "teleport-menu-mod";
	private static final String DATA_FILE_NAME = "data.json";
	private static final String DATA_DB_FILE_NAME = "data.sqlite";

	private PersistenceService() {
	}

	public static Path dataDir(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(DATA_DIR_NAME);
	}

	public static Path legacyDataFile(MinecraftServer server) {
		return dataDir(server).resolve(DATA_FILE_NAME);
	}

	public static Path databaseFile(MinecraftServer server) {
		return dataDir(server).resolve(DATA_DB_FILE_NAME);
	}

	public static void persistDataIfPossible() {
		if (ACTIVE_SERVER != null) {
			savePersistentData(ACTIVE_SERVER);
		}
	}

	public static void loadPersistentData(MinecraftServer server) {
		ServerState.clearPersistent();

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

	public static void savePersistentData(MinecraftServer server) {
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

	public static void closeStorageQuietly() {
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
}
