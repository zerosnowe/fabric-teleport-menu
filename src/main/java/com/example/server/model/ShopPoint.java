package com.example.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player-created shop holding a list of {@link ShopGood goods}.
 */
public final class ShopPoint {
	private final UUID id;
	private final UUID ownerId;
	private final String ownerName;
	private String name;
	private String iconItemId;
	private final List<ShopGood> goods;

	public ShopPoint(UUID id, UUID ownerId, String ownerName, String name, String iconItemId, List<ShopGood> goods) {
		this.id = id;
		this.ownerId = ownerId;
		this.ownerName = ownerName == null ? "" : ownerName;
		this.name = name == null ? "" : name;
		this.iconItemId = iconItemId == null ? "minecraft:chest" : iconItemId;
		this.goods = goods == null ? new ArrayList<>() : goods;
	}

	public UUID id() { return this.id; }
	public UUID ownerId() { return this.ownerId; }
	public String ownerName() { return this.ownerName; }
	public String name() { return this.name; }
	public String iconItemId() { return this.iconItemId; }
	public List<ShopGood> goods() { return this.goods; }

	public void setName(String name) { this.name = name == null ? "" : name; }
	public void setIconItemId(String iconItemId) { this.iconItemId = iconItemId == null ? "minecraft:chest" : iconItemId; }

	public ShopGood findGood(String itemId) {
		if (itemId == null) {
			return null;
		}
		for (ShopGood good : this.goods) {
			if (itemId.equals(good.itemId())) {
				return good;
			}
		}
		return null;
	}
}
