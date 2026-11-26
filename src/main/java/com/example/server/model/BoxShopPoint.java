package com.example.server.model;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * A physical barrel/chest based shop linked to a regular {@link ShopPoint}.
 */
public record BoxShopPoint(
	UUID id,
	UUID shopId,
	UUID ownerId,
	String ownerName,
	String name,
	ResourceLocation dimensionId,
	int x,
	int y,
	int z,
	boolean named
) {
	public BoxShopPoint {
		ownerName = ownerName == null ? "" : ownerName;
		name = BoxShopNames.sanitize(name);
	}

	public BoxShopPoint withName(String nextName, boolean nextNamed) {
		return new BoxShopPoint(
			this.id,
			this.shopId,
			this.ownerId,
			this.ownerName,
			nextName,
			this.dimensionId,
			this.x,
			this.y,
			this.z,
			nextNamed
		);
	}

	public BlockPos blockPos() {
		return new BlockPos(this.x, this.y, this.z);
	}
}
