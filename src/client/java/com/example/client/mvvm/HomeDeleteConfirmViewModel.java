package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Confirmation state for deleting a home.
 */
public final class HomeDeleteConfirmViewModel extends ViewModel {
	private final TeleportMenuPayloads.HomeTarget home;

	public HomeDeleteConfirmViewModel(TeleportMenuPayloads.HomeTarget home) {
		this.home = home;
	}

	public TeleportMenuPayloads.HomeTarget home() {
		return this.home;
	}

	public Command delete() {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.DeleteHomeRequestPayload(this.home.id())));
	}
}
