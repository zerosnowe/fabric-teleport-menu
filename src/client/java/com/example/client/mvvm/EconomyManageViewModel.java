package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;

import java.util.List;

/**
 * Roster used by the economy admin player picker.
 */
public final class EconomyManageViewModel extends ViewModel {
	private final List<TeleportMenuPayloads.PlayerTarget> players;

	public EconomyManageViewModel(List<TeleportMenuPayloads.PlayerTarget> players) {
		this.players = List.copyOf(players);
	}

	public List<TeleportMenuPayloads.PlayerTarget> players() {
		return this.players;
	}
}
