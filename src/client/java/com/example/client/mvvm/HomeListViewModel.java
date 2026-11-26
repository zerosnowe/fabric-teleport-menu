package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

/**
 * List state for the home screen.
 */
public final class HomeListViewModel extends ViewModel {
	private final ObservableProperty<List<TeleportMenuPayloads.HomeTarget>> homes;

	public HomeListViewModel(List<TeleportMenuPayloads.HomeTarget> homes) {
		this.homes = property(List.copyOf(homes == null ? List.of() : homes));
	}

	public ObservableProperty<List<TeleportMenuPayloads.HomeTarget>> homes() {
		return this.homes;
	}

	public Command teleport(TeleportMenuPayloads.HomeTarget home) {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.TeleportHomeRequestPayload(home.id())));
	}
}
