package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;

/**
 * List state for the territory screen.
 */
public final class TerritoryListViewModel extends ViewModel {
	private final ObservableProperty<List<TeleportMenuPayloads.TerritoryTarget>> territories;

	public TerritoryListViewModel(List<TeleportMenuPayloads.TerritoryTarget> territories) {
		this.territories = property(List.copyOf(territories == null ? List.of() : territories));
	}

	public ObservableProperty<List<TeleportMenuPayloads.TerritoryTarget>> territories() {
		return this.territories;
	}

	public Command teleport(TeleportMenuPayloads.TerritoryTarget territory) {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.TeleportTerritoryRequestPayload(territory.id())));
	}
}
