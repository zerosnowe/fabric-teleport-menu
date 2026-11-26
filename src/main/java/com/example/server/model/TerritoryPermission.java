package com.example.server.model;

/**
 * The protection flags a territory owner can toggle.
 */
public enum TerritoryPermission {
	BUILD_BREAK("build_break", "screen.teleportmenu.territories.permission.build_break"),
	CONTAINER("container", "screen.teleportmenu.territories.permission.container"),
	INTERACT("interact", "screen.teleportmenu.territories.permission.interact"),
	DAMAGE_ENTITIES("damage_entities", "screen.teleportmenu.territories.permission.damage_entities"),
	BLOCK_USE("block_use", "screen.teleportmenu.territories.permission.block_use");

	private final String key;
	private final String translationKey;

	TerritoryPermission(String key, String translationKey) {
		this.key = key;
		this.translationKey = translationKey;
	}

	public String key() {
		return this.key;
	}

	public String translationKey() {
		return this.translationKey;
	}

	public static TerritoryPermission fromKey(String key) {
		if (key == null) {
			return null;
		}
		for (TerritoryPermission permission : values()) {
			if (permission.key.equalsIgnoreCase(key.trim())) {
				return permission;
			}
		}
		return null;
	}
}
