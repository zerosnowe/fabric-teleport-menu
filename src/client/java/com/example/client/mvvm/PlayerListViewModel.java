package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;
import java.util.UUID;

/**
 * Player roster with teleport actions.
 */
public final class PlayerListViewModel extends ViewModel {
	private final List<TeleportMenuPayloads.PlayerTarget> targets;

	public PlayerListViewModel(List<TeleportMenuPayloads.PlayerTarget> targets) {
		this.targets = List.copyOf(targets);
	}

	public List<TeleportMenuPayloads.PlayerTarget> targets() {
		return this.targets;
	}

	public Command teleport(UUID playerId) {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.TeleportRequestPayload(playerId)));
	}
}
