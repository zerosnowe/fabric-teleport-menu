package com.example.server.storage;

import com.example.server.model.BoxShopPoint;
import com.example.server.model.HomePoint;
import com.example.server.model.MomentPoint;
import com.example.server.model.ShopPoint;
import com.example.server.model.TerritoryPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A full in-memory view of every persisted aggregate, written to SQLite in one transaction.
 */
public record PersistedSnapshot(
	Map<UUID, List<HomePoint>> homes,
	Map<UUID, List<TerritoryPoint>> territories,
	Map<UUID, List<ShopPoint>> shops,
	Map<UUID, BoxShopPoint> boxShops,
	Map<UUID, Integer> shopLimits,
	Map<UUID, Set<String>> rewardedEquipment,
	Map<UUID, MomentPoint> moments,
	Map<UUID, String> momentDrafts,
	Set<String> disabledItems,
	Set<UUID> economySidebarHidden
) {
}
