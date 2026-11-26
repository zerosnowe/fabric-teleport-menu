package com.example.server.storage;

import com.example.server.model.BoxShopPoint;
import com.example.server.model.HomePoint;
import com.example.server.model.MomentContent;
import com.example.server.model.MomentPoint;
import com.example.server.model.ShopGood;
import com.example.server.model.ShopPoint;
import com.example.server.model.TerritoryPoint;
import net.minecraft.resources.ResourceLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes every persisted aggregate through SQLite.
 */
public final class ServerDataStore implements AutoCloseable {
	private final DatabaseManager database;

	public ServerDataStore(DatabaseManager database) {
		this.database = database;
	}

	public DatabaseManager database() {
		return this.database;
	}

	// ---------------------------------------------------------------------
	// Loading
	// ---------------------------------------------------------------------

	public Map<UUID, List<HomePoint>> loadHomes() throws SQLException {
		Map<UUID, List<HomePoint>> homes = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery(
				 "SELECT owner, id, name, x, y, z, icon_item_id FROM homes ORDER BY owner, sort_order, rowid")) {
			while (result.next()) {
				UUID owner = parseUuid(result.getString("owner"));
				UUID id = parseUuid(result.getString("id"));
				if (owner == null || id == null) {
					continue;
				}
				String icon = result.getString("icon_item_id");
				homes.computeIfAbsent(owner, key -> new ArrayList<>()).add(new HomePoint(
					id,
					result.getString("name"),
					result.getInt("x"),
					result.getInt("y"),
					result.getInt("z"),
					icon == null || icon.isBlank() ? "minecraft:compass" : icon
				));
			}
		}
		return homes;
	}

	public Map<UUID, List<TerritoryPoint>> loadTerritories() throws SQLException {
		Map<UUID, Set<UUID>> whitelists = loadWhitelists();
		Map<UUID, List<TerritoryPoint>> territories = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("""
				 SELECT owner, id, owner_name, name, dimension_id, min_x, min_y, min_z, max_x, max_y, max_z,
						icon_item_id, build_break_allowed, container_allowed, interact_allowed,
						damage_entities_allowed, block_use_allowed
				 FROM territories ORDER BY owner, sort_order, rowid
				 """)) {
			while (result.next()) {
				UUID owner = parseUuid(result.getString("owner"));
				UUID id = parseUuid(result.getString("id"));
				ResourceLocation dimension = parseDimension(result.getString("dimension_id"));
				if (owner == null || id == null || dimension == null) {
					continue;
				}
				String icon = result.getString("icon_item_id");
				territories.computeIfAbsent(owner, key -> new ArrayList<>()).add(new TerritoryPoint(
					id,
					owner,
					result.getString("owner_name"),
					result.getString("name"),
					dimension,
					result.getInt("min_x"),
					result.getInt("min_y"),
					result.getInt("min_z"),
					result.getInt("max_x"),
					result.getInt("max_y"),
					result.getInt("max_z"),
					icon == null || icon.isBlank() ? "minecraft:grass_block" : icon,
					result.getInt("build_break_allowed") != 0,
					result.getInt("container_allowed") != 0,
					result.getInt("interact_allowed") != 0,
					result.getInt("damage_entities_allowed") != 0,
					result.getInt("block_use_allowed") != 0,
					whitelists.getOrDefault(id, ConcurrentHashMap.newKeySet())
				));
			}
		}
		return territories;
	}

	private Map<UUID, Set<UUID>> loadWhitelists() throws SQLException {
		Map<UUID, Set<UUID>> whitelists = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT territory_id, player_id FROM territory_whitelist")) {
			while (result.next()) {
				UUID territoryId = parseUuid(result.getString("territory_id"));
				UUID playerId = parseUuid(result.getString("player_id"));
				if (territoryId == null || playerId == null) {
					continue;
				}
				whitelists.computeIfAbsent(territoryId, key -> ConcurrentHashMap.newKeySet()).add(playerId);
			}
		}
		return whitelists;
	}

	public Map<UUID, List<ShopPoint>> loadShops() throws SQLException {
		Map<UUID, List<ShopGood>> goodsByShop = loadShopGoods();
		Map<UUID, List<ShopPoint>> shops = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery(
				 "SELECT owner, id, name, owner_name, icon_item_id FROM shops ORDER BY owner, sort_order, rowid")) {
			while (result.next()) {
				UUID owner = parseUuid(result.getString("owner"));
				UUID id = parseUuid(result.getString("id"));
				if (owner == null || id == null) {
					continue;
				}
				shops.computeIfAbsent(owner, key -> new ArrayList<>()).add(new ShopPoint(
					id,
					owner,
					result.getString("owner_name"),
					result.getString("name"),
					result.getString("icon_item_id"),
					goodsByShop.getOrDefault(id, new ArrayList<>())
				));
			}
		}
		return shops;
	}

	private Map<UUID, List<ShopGood>> loadShopGoods() throws SQLException {
		Map<UUID, List<ShopGood>> goods = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("""
				 SELECT shop_id, item_id, name, icon_item_id, stock, price
				 FROM shop_goods ORDER BY shop_id, sort_order, rowid
				 """)) {
			while (result.next()) {
				UUID shopId = parseUuid(result.getString("shop_id"));
				if (shopId == null) {
					continue;
				}
				String itemId = result.getString("item_id");
				goods.computeIfAbsent(shopId, key -> new ArrayList<>()).add(new ShopGood(
					itemId,
					result.getString("name"),
					result.getString("icon_item_id"),
					result.getInt("stock"),
					result.getDouble("price")
				));
			}
		}
		return goods;
	}

	public Map<UUID, BoxShopPoint> loadBoxShops() throws SQLException {
		Map<UUID, BoxShopPoint> boxShops = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("""
				 SELECT id, shop_id, owner_id, owner_name, name, dimension_id, x, y, z, named
				 FROM box_shops ORDER BY rowid
				 """)) {
			while (result.next()) {
				UUID id = parseUuid(result.getString("id"));
				UUID shopId = parseUuid(result.getString("shop_id"));
				UUID ownerId = parseUuid(result.getString("owner_id"));
				ResourceLocation dimension = parseDimension(result.getString("dimension_id"));
				if (id == null || shopId == null || ownerId == null || dimension == null) {
					continue;
				}
				boxShops.put(id, new BoxShopPoint(
					id,
					shopId,
					ownerId,
					result.getString("owner_name"),
					result.getString("name"),
					dimension,
					result.getInt("x"),
					result.getInt("y"),
					result.getInt("z"),
					result.getInt("named") != 0
				));
			}
		}
		return boxShops;
	}

	public Map<UUID, Integer> loadShopLimits() throws SQLException {
		Map<UUID, Integer> limits = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT owner, limit_value FROM shop_limits")) {
			while (result.next()) {
				UUID owner = parseUuid(result.getString("owner"));
				if (owner != null) {
					limits.put(owner, result.getInt("limit_value"));
				}
			}
		}
		return limits;
	}

	public Map<UUID, Set<String>> loadRewardedEquipment() throws SQLException {
		Map<UUID, Set<String>> rewarded = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT owner, item_id FROM rewarded_equipment")) {
			while (result.next()) {
				UUID owner = parseUuid(result.getString("owner"));
				String itemId = result.getString("item_id");
				if (owner == null || itemId == null || itemId.isBlank()) {
					continue;
				}
				rewarded.computeIfAbsent(owner, key -> ConcurrentHashMap.newKeySet()).add(itemId);
			}
		}
		return rewarded;
	}

	public Map<UUID, MomentPoint> loadMoments() throws SQLException {
		Map<UUID, Set<UUID>> likes = loadMomentLikes();
		Map<UUID, MomentPoint> moments = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery(
				 "SELECT id, author_id, author_name, content, created_at FROM moments ORDER BY created_at DESC, rowid")) {
			while (result.next()) {
				UUID id = parseUuid(result.getString("id"));
				UUID authorId = parseUuid(result.getString("author_id"));
				String content = MomentContent.truncate(result.getString("content"));
				if (id == null || authorId == null || content.isBlank()) {
					continue;
				}
				MomentPoint moment = new MomentPoint(
					id,
					authorId,
					result.getString("author_name"),
					content,
					result.getLong("created_at")
				);
				moment.likedBy().addAll(likes.getOrDefault(id, Set.of()));
				moments.put(id, moment);
			}
		}
		return moments;
	}

	private Map<UUID, Set<UUID>> loadMomentLikes() throws SQLException {
		Map<UUID, Set<UUID>> likes = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT moment_id, player_id FROM moment_likes")) {
			while (result.next()) {
				UUID momentId = parseUuid(result.getString("moment_id"));
				UUID playerId = parseUuid(result.getString("player_id"));
				if (momentId == null || playerId == null) {
					continue;
				}
				likes.computeIfAbsent(momentId, key -> ConcurrentHashMap.newKeySet()).add(playerId);
			}
		}
		return likes;
	}

	public Map<UUID, String> loadMomentDrafts() throws SQLException {
		Map<UUID, String> drafts = new LinkedHashMap<>();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT player_id, content FROM moment_drafts")) {
			while (result.next()) {
				UUID playerId = parseUuid(result.getString("player_id"));
				String content = MomentContent.truncate(result.getString("content"));
				if (playerId != null && !content.isBlank()) {
					drafts.put(playerId, content);
				}
			}
		}
		return drafts;
	}

	public Set<String> loadDisabledItems() throws SQLException {
		Set<String> items = ConcurrentHashMap.newKeySet();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT item_id FROM disabled_items")) {
			while (result.next()) {
				String itemId = result.getString("item_id");
				if (itemId != null && !itemId.isBlank()) {
					items.add(itemId);
				}
			}
		}
		return items;
	}

	public Set<UUID> loadEconomySidebarHidden() throws SQLException {
		Set<UUID> hidden = ConcurrentHashMap.newKeySet();
		try (Statement statement = this.database.connection().createStatement();
			 ResultSet result = statement.executeQuery("SELECT player_id FROM economy_sidebar_hidden")) {
			while (result.next()) {
				UUID playerId = parseUuid(result.getString("player_id"));
				if (playerId != null) {
					hidden.add(playerId);
				}
			}
		}
		return hidden;
	}

	// ---------------------------------------------------------------------
	// Saving
	// ---------------------------------------------------------------------

	public void saveAll(PersistedSnapshot snapshot) throws SQLException {
		this.database.transaction(connection -> {
			clearAll(connection);
			saveHomes(connection, snapshot.homes());
			saveTerritories(connection, snapshot.territories());
			saveShops(connection, snapshot.shops());
			saveBoxShops(connection, snapshot.boxShops());
			saveShopLimits(connection, snapshot.shopLimits());
			saveRewardedEquipment(connection, snapshot.rewardedEquipment());
			saveMoments(connection, snapshot.moments());
			saveMomentDrafts(connection, snapshot.momentDrafts());
			saveDisabledItems(connection, snapshot.disabledItems());
			saveEconomySidebarHidden(connection, snapshot.economySidebarHidden());
			updateMeta(connection, "last_saved_at", Long.toString(System.currentTimeMillis()));
		});
	}

	private void clearAll(Connection connection) throws SQLException {
		String[] tables = {
			"homes", "territories", "territory_whitelist", "shops", "shop_goods", "box_shops",
			"shop_limits", "rewarded_equipment", "moments", "moment_likes", "moment_drafts",
			"disabled_items", "economy_sidebar_hidden"
		};
		try (Statement statement = connection.createStatement()) {
			for (String table : tables) {
				statement.executeUpdate("DELETE FROM " + table);
			}
		}
	}

	private void saveHomes(Connection connection, Map<UUID, List<HomePoint>> homes) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT INTO homes(owner, id, name, x, y, z, icon_item_id, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
			for (Map.Entry<UUID, List<HomePoint>> entry : homes.entrySet()) {
				int order = 0;
				for (HomePoint home : entry.getValue()) {
					statement.setString(1, entry.getKey().toString());
					statement.setString(2, home.id().toString());
					statement.setString(3, home.name());
					statement.setInt(4, home.x());
					statement.setInt(5, home.y());
					statement.setInt(6, home.z());
					statement.setString(7, home.iconItemId());
					statement.setInt(8, order++);
					statement.addBatch();
				}
			}
			statement.executeBatch();
		}
	}

	private void saveTerritories(Connection connection, Map<UUID, List<TerritoryPoint>> territories) throws SQLException {
		try (PreparedStatement territoryStatement = connection.prepareStatement("""
				INSERT INTO territories(owner, id, owner_name, name, dimension_id, min_x, min_y, min_z,
					max_x, max_y, max_z, icon_item_id, build_break_allowed, container_allowed,
					interact_allowed, damage_entities_allowed, block_use_allowed, sort_order)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""");
			 PreparedStatement whitelistStatement = connection.prepareStatement(
				 "INSERT OR IGNORE INTO territory_whitelist(territory_id, player_id) VALUES (?, ?)")) {
			for (Map.Entry<UUID, List<TerritoryPoint>> entry : territories.entrySet()) {
				int order = 0;
				for (TerritoryPoint territory : entry.getValue()) {
					territoryStatement.setString(1, entry.getKey().toString());
					territoryStatement.setString(2, territory.id().toString());
					territoryStatement.setString(3, territory.ownerName());
					territoryStatement.setString(4, territory.name());
					territoryStatement.setString(5, territory.dimensionId().toString());
					territoryStatement.setInt(6, territory.minX());
					territoryStatement.setInt(7, territory.minY());
					territoryStatement.setInt(8, territory.minZ());
					territoryStatement.setInt(9, territory.maxX());
					territoryStatement.setInt(10, territory.maxY());
					territoryStatement.setInt(11, territory.maxZ());
					territoryStatement.setString(12, territory.iconItemId());
					territoryStatement.setInt(13, territory.buildBreakAllowed() ? 1 : 0);
					territoryStatement.setInt(14, territory.containerAllowed() ? 1 : 0);
					territoryStatement.setInt(15, territory.interactAllowed() ? 1 : 0);
					territoryStatement.setInt(16, territory.damageEntitiesAllowed() ? 1 : 0);
					territoryStatement.setInt(17, territory.blockUseAllowed() ? 1 : 0);
					territoryStatement.setInt(18, order++);
					territoryStatement.addBatch();

					for (UUID playerId : territory.whitelistPlayerIds()) {
						whitelistStatement.setString(1, territory.id().toString());
						whitelistStatement.setString(2, playerId.toString());
						whitelistStatement.addBatch();
					}
				}
			}
			territoryStatement.executeBatch();
			whitelistStatement.executeBatch();
		}
	}

	private void saveShops(Connection connection, Map<UUID, List<ShopPoint>> shops) throws SQLException {
		try (PreparedStatement shopStatement = connection.prepareStatement(
				"INSERT INTO shops(owner, id, name, owner_name, icon_item_id, sort_order) VALUES (?, ?, ?, ?, ?, ?)");
			 PreparedStatement goodStatement = connection.prepareStatement("""
				 INSERT INTO shop_goods(shop_id, item_id, name, icon_item_id, stock, price, sort_order)
				 VALUES (?, ?, ?, ?, ?, ?, ?)
				 """)) {
			for (Map.Entry<UUID, List<ShopPoint>> entry : shops.entrySet()) {
				int order = 0;
				for (ShopPoint shop : entry.getValue()) {
					shopStatement.setString(1, entry.getKey().toString());
					shopStatement.setString(2, shop.id().toString());
					shopStatement.setString(3, shop.name());
					shopStatement.setString(4, shop.ownerName());
					shopStatement.setString(5, shop.iconItemId());
					shopStatement.setInt(6, order++);
					shopStatement.addBatch();

					int goodOrder = 0;
					for (ShopGood good : shop.goods()) {
						goodStatement.setString(1, shop.id().toString());
						goodStatement.setString(2, good.itemId());
						goodStatement.setString(3, good.name());
						goodStatement.setString(4, good.iconItemId());
						goodStatement.setInt(5, good.stock());
						goodStatement.setDouble(6, good.price());
						goodStatement.setInt(7, goodOrder++);
						goodStatement.addBatch();
					}
				}
			}
			shopStatement.executeBatch();
			goodStatement.executeBatch();
		}
	}

	private void saveBoxShops(Connection connection, Map<UUID, BoxShopPoint> boxShops) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
				INSERT INTO box_shops(id, shop_id, owner_id, owner_name, name, dimension_id, x, y, z, named)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""")) {
			for (BoxShopPoint boxShop : boxShops.values()) {
				statement.setString(1, boxShop.id().toString());
				statement.setString(2, boxShop.shopId().toString());
				statement.setString(3, boxShop.ownerId().toString());
				statement.setString(4, boxShop.ownerName());
				statement.setString(5, boxShop.name());
				statement.setString(6, boxShop.dimensionId().toString());
				statement.setInt(7, boxShop.x());
				statement.setInt(8, boxShop.y());
				statement.setInt(9, boxShop.z());
				statement.setInt(10, boxShop.named() ? 1 : 0);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void saveShopLimits(Connection connection, Map<UUID, Integer> limits) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT INTO shop_limits(owner, limit_value) VALUES (?, ?)")) {
			for (Map.Entry<UUID, Integer> entry : limits.entrySet()) {
				statement.setString(1, entry.getKey().toString());
				statement.setInt(2, entry.getValue());
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void saveRewardedEquipment(Connection connection, Map<UUID, Set<String>> rewarded) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT OR IGNORE INTO rewarded_equipment(owner, item_id) VALUES (?, ?)")) {
			for (Map.Entry<UUID, Set<String>> entry : rewarded.entrySet()) {
				for (String itemId : entry.getValue()) {
					if (itemId == null || itemId.isBlank()) {
						continue;
					}
					statement.setString(1, entry.getKey().toString());
					statement.setString(2, itemId);
					statement.addBatch();
				}
			}
			statement.executeBatch();
		}
	}

	private void saveMoments(Connection connection, Map<UUID, MomentPoint> moments) throws SQLException {
		try (PreparedStatement momentStatement = connection.prepareStatement(
				"INSERT INTO moments(id, author_id, author_name, content, created_at) VALUES (?, ?, ?, ?, ?)");
			 PreparedStatement likeStatement = connection.prepareStatement(
				 "INSERT OR IGNORE INTO moment_likes(moment_id, player_id) VALUES (?, ?)")) {
			for (MomentPoint moment : moments.values()) {
				momentStatement.setString(1, moment.id().toString());
				momentStatement.setString(2, moment.authorId().toString());
				momentStatement.setString(3, moment.authorName());
				momentStatement.setString(4, moment.content());
				momentStatement.setLong(5, moment.createdAt());
				momentStatement.addBatch();

				for (UUID playerId : moment.likedBy()) {
					likeStatement.setString(1, moment.id().toString());
					likeStatement.setString(2, playerId.toString());
					likeStatement.addBatch();
				}
			}
			momentStatement.executeBatch();
			likeStatement.executeBatch();
		}
	}

	private void saveMomentDrafts(Connection connection, Map<UUID, String> drafts) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT INTO moment_drafts(player_id, content) VALUES (?, ?)")) {
			for (Map.Entry<UUID, String> entry : drafts.entrySet()) {
				statement.setString(1, entry.getKey().toString());
				statement.setString(2, entry.getValue() == null ? "" : entry.getValue());
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void saveDisabledItems(Connection connection, Set<String> disabledItems) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT OR IGNORE INTO disabled_items(item_id) VALUES (?)")) {
			for (String itemId : disabledItems) {
				if (itemId == null || itemId.isBlank()) {
					continue;
				}
				statement.setString(1, itemId);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void saveEconomySidebarHidden(Connection connection, Set<UUID> hidden) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT OR IGNORE INTO economy_sidebar_hidden(player_id) VALUES (?)")) {
			for (UUID playerId : hidden) {
				statement.setString(1, playerId.toString());
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void updateMeta(Connection connection, String key, String value) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"INSERT INTO meta(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
			statement.setString(1, key);
			statement.setString(2, value);
			statement.executeUpdate();
		}
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	private static UUID parseUuid(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException error) {
			return null;
		}
	}

	private static ResourceLocation parseDimension(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ResourceLocation.parse(value);
		} catch (RuntimeException error) {
			return null;
		}
	}

	@Override
	public void close() throws SQLException {
		this.database.close();
	}
}
