package com.example.client.mvvm;

import com.example.client.TeleportMenuClient;
import com.example.network.TeleportMenuPayloads;

import java.util.List;

/**
 * Cached economy leaderboard snapshot.
 */
public final class EconomyLeaderboardViewModel extends ViewModel {
	private final List<TeleportMenuPayloads.EconomyLeaderboardEntry> entries;

	public EconomyLeaderboardViewModel() {
		this.entries = List.copyOf(TeleportMenuClient.leaderboardEntries());
	}

	public List<TeleportMenuPayloads.EconomyLeaderboardEntry> entries() {
		return this.entries;
	}
}
