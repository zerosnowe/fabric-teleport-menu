package com.example.server.storage;

import com.example.server.model.BoxShopNames;
import com.example.server.model.BoxShopPoint;
import com.example.server.model.HomePoint;
import com.example.server.model.MomentContent;
import com.example.server.model.MomentPoint;
import com.example.server.model.ShopGood;
import com.example.server.model.ShopPoint;
import com.example.server.model.TerritoryPoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-shot importer that copies the legacy {@code data.json} file into SQLite and
 * renames the original file to {@code data.json.bak}.
 */
public final class JsonDataImporter {
	public static final String JSON_IMPORTED_KEY = "json_imported";

	private static final Logger LOGGER = LoggerFactory.getLogger("teleportmenu");
	private static final Gson GSON = new Gson();

	private JsonDataImporter() {
	}

	public static boolean importIfNeeded(ServerDataStore store, Path legacyFile) throws SQLException {
		if (legacyFile == null || !Files.exists(legacyFile)) {
			return false;
		}
		if (store.database().hasMeta(JSON_IMPORTED_KEY)) {
			return false;
		}

		JsonObject root;
		try {
			String json = Files.readString(legacyFile, StandardCharsets.UTF_8);
			JsonElement element = JsonParser.parseString(json);
			if (!element.isJsonObject()) {
				LOGGER.warn("Legacy teleport menu data is not a JSON object, skipping import: {}", legacyFile);
				return false;
			}
			root = element.getAsJsonObject();
		} catch (IOException | RuntimeException error) {
			LOGGER.error("Failed to read legacy teleport menu data from {}", legacyFile, error);
			return false;
		}

		PersistedSnapshot snapshot = readSnapshot(root);
		store.saveAll(snapshot);
		store.database().setMeta(JSON_IMPORTED_KEY, "1");
		backup(legacyFile);
		LOGGER.info("Imported legacy teleport menu data from {} into SQLite", legacyFile);
		return true;
	}

	private static void backup(Path legacyFile) {
		Path backup = legacyFile.resolveSibling(legacyFile.getFileName() + ".bak");
		try {
			Files.move(legacyFile, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException error) {
			LOGGER.warn("Imported legacy data but failed to rename {} to {}", legacyFile, backup, error);
		}
	}

	private static PersistedSnapshot readSnapshot(JsonObject root) {
		Map<UUID, List<HomePoint>> homes = new LinkedHashMap<>();
		Map<UUID, List<TerritoryPoint>> territories = new LinkedHashMap<>();
		Map<UUID, List<ShopPoint>> shops = new LinkedHashMap<>();
		Map<UUID, BoxShopPoint> boxShops = new LinkedHashMap<>();
		Map<UUID, Integer> shopLimits = new LinkedHashMap<>();
		Map<UUID, Set<String>> rewardedEquipment = new LinkedHashMap<>();
		Map<UUID, MomentPoint> moments = new LinkedHashMap<>();
		Map<UUID, String> momentDrafts = new LinkedHashMap<>();
		Set<String> disabledItems = ConcurrentHashMap.newKeySet();
		Set<UUID> economySidebarHidden = ConcurrentHashMap.newKeySet();

		for (JsonElement element : array(root, "disabledItems")) {
			if (element.isJsonPrimitive()) {
				String itemId = element.getAsString();
				if (!itemId.isBlank()) {
					disabledItems.add(itemId);
				}
			}
		}

		for (JsonElement element : array(root, "homes")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID owner = uuid(obj, "owner");
			UUID id = uuid(obj, "id");
			if (owner == null || id == null) {
				continue;
			}
			homes.computeIfAbsent(owner, key -> new ArrayList<>()).add(new HomePoint(
				id,
				string(obj, "name", ""),
				integer(obj, "x", 0),
				integer(obj, "y", 0),
				integer(obj, "z", 0),
				string(obj, "iconItemId", "minecraft:compass")
			));
		}

		for (JsonElement element : array(root, "territories")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID owner = uuid(obj, "owner");
			UUID id = uuid(obj, "id");
			ResourceLocation dimension = dimension(obj, "dimensionId");
			if (owner == null || id == null || dimension == null) {
				continue;
			}
			territories.computeIfAbsent(owner, key -> new ArrayList<>()).add(new TerritoryPoint(
				id,
				owner,
				string(obj, "ownerName", ""),
				string(obj, "name", ""),
				dimension,
				integer(obj, "minX", 0),
				integer(obj, "minY", 0),
				integer(obj, "minZ", 0),
				integer(obj, "maxX", 0),
				integer(obj, "maxY", 0),
				integer(obj, "maxZ", 0),
				string(obj, "iconItemId", "minecraft:grass_block"),
				flag(obj, "buildBreakAllowed"),
				flag(obj, "containerAllowed"),
				flag(obj, "interactAllowed"),
				flag(obj, "damageEntitiesAllowed"),
				flag(obj, "blockUseAllowed"),
				whitelist(obj)
			));
		}

		for (JsonElement element : array(root, "shops")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID owner = uuid(obj, "owner");
			UUID id = uuid(obj, "id");
			if (owner == null || id == null) {
				continue;
			}
			List<ShopGood> goods = new ArrayList<>();
			for (JsonElement goodElement : array(obj, "goods")) {
				if (!goodElement.isJsonObject()) {
					continue;
				}
				JsonObject goodObj = goodElement.getAsJsonObject();
				String itemId = string(goodObj, "itemId", "");
				if (itemId.isBlank()) {
					continue;
				}
				goods.add(new ShopGood(
					itemId,
					string(goodObj, "name", ""),
					string(goodObj, "iconItemId", itemId),
					Math.max(0, integer(goodObj, "stock", 0)),
					Math.max(0D, decimal(goodObj, "price", 0D))
				));
			}
			shops.computeIfAbsent(owner, key -> new ArrayList<>()).add(new ShopPoint(
				id,
				owner,
				string(obj, "ownerName", ""),
				string(obj, "name", ""),
				string(obj, "iconItemId", "minecraft:chest"),
				goods
			));
		}

		for (JsonElement element : array(root, "boxShops")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID id = uuid(obj, "id");
			UUID shopId = uuid(obj, "shopId");
			UUID ownerId = uuid(obj, "ownerId");
			ResourceLocation dimension = dimension(obj, "dimensionId");
			if (id == null || shopId == null || ownerId == null || dimension == null
				|| !obj.has("x") || !obj.has("y") || !obj.has("z")) {
				// Legacy entity-id based box shops cannot be resolved during import.
				continue;
			}
			boxShops.put(id, new BoxShopPoint(
				id,
				shopId,
				ownerId,
				string(obj, "ownerName", ""),
				string(obj, "name", BoxShopNames.DEFAULT_NAME),
				dimension,
				integer(obj, "x", 0),
				integer(obj, "y", 0),
				integer(obj, "z", 0),
				!obj.has("named") || obj.get("named").getAsBoolean()
			));
		}

		for (JsonElement element : array(root, "shopLimits")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID owner = uuid(obj, "owner");
			if (owner != null) {
				shopLimits.put(owner, integer(obj, "limit", 0));
			}
		}

		for (JsonElement element : array(root, "rewardedEquipment")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID owner = uuid(obj, "owner");
			String itemId = string(obj, "itemId", "");
			if (owner == null || itemId.isBlank()) {
				continue;
			}
			rewardedEquipment.computeIfAbsent(owner, key -> ConcurrentHashMap.newKeySet()).add(itemId);
		}

		for (JsonElement element : array(root, "moments")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID id = uuid(obj, "id");
			UUID authorId = uuid(obj, "authorId");
			String content = MomentContent.truncate(string(obj, "content", ""));
			if (id == null || authorId == null || content.isBlank()) {
				continue;
			}
			MomentPoint moment = new MomentPoint(
				id,
				authorId,
				string(obj, "authorName", ""),
				content,
				number(obj, "createdAt", 0L)
			);
			for (JsonElement likeElement : array(obj, "likedBy")) {
				if (!likeElement.isJsonPrimitive()) {
					continue;
				}
				UUID playerId = parseUuid(likeElement.getAsString());
				if (playerId != null) {
					moment.likedBy().add(playerId);
				}
			}
			moments.put(id, moment);
		}

		for (JsonElement element : array(root, "momentDrafts")) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject obj = element.getAsJsonObject();
			UUID playerId = uuid(obj, "playerId");
			String content = MomentContent.truncate(string(obj, "content", ""));
			if (playerId != null && !content.isBlank()) {
				momentDrafts.put(playerId, content);
			}
		}

		for (JsonElement element : array(root, "economySidebarHiddenPlayerIds")) {
			if (!element.isJsonPrimitive()) {
				continue;
			}
			UUID playerId = parseUuid(element.getAsString());
			if (playerId != null) {
				economySidebarHidden.add(playerId);
			}
		}

		return new PersistedSnapshot(
			homes,
			territories,
			shops,
			boxShops,
			shopLimits,
			rewardedEquipment,
			moments,
			momentDrafts,
			disabledItems,
			economySidebarHidden
		);
	}

	private static JsonArray array(JsonObject obj, String key) {
		if (obj.has(key) && obj.get(key).isJsonArray()) {
			return obj.getAsJsonArray(key);
		}
		return new JsonArray();
	}

	private static UUID uuid(JsonObject obj, String key) {
		if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
			return null;
		}
		return parseUuid(obj.get(key).getAsString());
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException error) {
			return null;
		}
	}

	private static ResourceLocation dimension(JsonObject obj, String key) {
		if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
			return null;
		}
		try {
			return ResourceLocation.parse(obj.get(key).getAsString());
		} catch (RuntimeException error) {
			return null;
		}
	}

	private static String string(JsonObject obj, String key, String fallback) {
		if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
			return fallback;
		}
		return obj.get(key).getAsString();
	}

	private static int integer(JsonObject obj, String key, int fallback) {
		if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
			return fallback;
		}
		try {
			return obj.get(key).getAsInt();
		} catch (RuntimeException error) {
			return fallback;
		}
	}

	private static long number(JsonObject obj, String key, long fallback) {
		if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
			return fallback;
		}
		try {
			return obj.get(key).getAsLong();
		} catch (RuntimeException error) {
			return fallback;
		}
	}

	private static double decimal(JsonObject obj, String key, double fallback) {
		if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
			return fallback;
		}
		try {
			return obj.get(key).getAsDouble();
		} catch (RuntimeException error) {
			return fallback;
		}
	}

	private static boolean flag(JsonObject obj, String key) {
		// Missing permission flags default to allowed, matching the legacy loader.
		return !obj.has(key) || obj.get(key).getAsBoolean();
	}

	private static Set<UUID> whitelist(JsonObject obj) {
		Set<UUID> whitelist = ConcurrentHashMap.newKeySet();
		for (JsonElement element : array(obj, "whitelist")) {
			if (!element.isJsonPrimitive()) {
				continue;
			}
			UUID playerId = parseUuid(element.getAsString());
			if (playerId != null) {
				whitelist.add(playerId);
			}
		}
		return whitelist;
	}
}
