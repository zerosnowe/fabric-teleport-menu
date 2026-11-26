package com.example.server.model;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A protected cuboid region owned by a player.
 */
public final class TerritoryPoint {
	private final UUID id;
	private final UUID ownerId;
	private final String ownerName;
	private final String name;
	private final ResourceLocation dimensionId;
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;
	private final String iconItemId;
	private final Set<UUID> whitelistPlayerIds;
	private boolean buildBreakAllowed;
	private boolean containerAllowed;
	private boolean interactAllowed;
	private boolean damageEntitiesAllowed;
	private boolean blockUseAllowed;

	public TerritoryPoint(
		UUID id,
		UUID ownerId,
		String ownerName,
		String name,
		ResourceLocation dimensionId,
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
		Set<UUID> whitelistPlayerIds
	) {
		this.id = id;
		this.ownerId = ownerId;
		this.ownerName = ownerName == null ? "" : ownerName;
		this.name = name == null ? "" : name;
		this.dimensionId = dimensionId;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.iconItemId = iconItemId == null ? "minecraft:grass_block" : iconItemId;
		this.buildBreakAllowed = buildBreakAllowed;
		this.containerAllowed = containerAllowed;
		this.interactAllowed = interactAllowed;
		this.damageEntitiesAllowed = damageEntitiesAllowed;
		this.blockUseAllowed = blockUseAllowed;
		this.whitelistPlayerIds = whitelistPlayerIds == null ? ConcurrentHashMap.newKeySet() : whitelistPlayerIds;
	}

	public UUID id() { return this.id; }
	public UUID ownerId() { return this.ownerId; }
	public String ownerName() { return this.ownerName; }
	public String name() { return this.name; }
	public ResourceLocation dimensionId() { return this.dimensionId; }
	public int minX() { return this.minX; }
	public int minY() { return this.minY; }
	public int minZ() { return this.minZ; }
	public int maxX() { return this.maxX; }
	public int maxY() { return this.maxY; }
	public int maxZ() { return this.maxZ; }
	public String iconItemId() { return this.iconItemId; }
	public boolean buildBreakAllowed() { return this.buildBreakAllowed; }
	public boolean containerAllowed() { return this.containerAllowed; }
	public boolean interactAllowed() { return this.interactAllowed; }
	public boolean damageEntitiesAllowed() { return this.damageEntitiesAllowed; }
	public boolean blockUseAllowed() { return this.blockUseAllowed; }
	public Set<UUID> whitelistPlayerIds() { return this.whitelistPlayerIds; }

	public void setBuildBreakAllowed(boolean value) { this.buildBreakAllowed = value; }
	public void setContainerAllowed(boolean value) { this.containerAllowed = value; }
	public void setInteractAllowed(boolean value) { this.interactAllowed = value; }
	public void setDamageEntitiesAllowed(boolean value) { this.damageEntitiesAllowed = value; }
	public void setBlockUseAllowed(boolean value) { this.blockUseAllowed = value; }

	public boolean contains(int x, int y, int z) {
		return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
	}
}
