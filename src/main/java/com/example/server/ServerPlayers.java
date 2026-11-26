package com.example.server;

import net.minecraft.server.level.ServerPlayer;

/**
 * Small predicates shared across server services.
 */
public final class ServerPlayers {
	private ServerPlayers() {
	}

	public static boolean isAdmin(ServerPlayer player) {
		return player.hasPermissions(2);
	}
}
