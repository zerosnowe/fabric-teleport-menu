package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

/**
 * Permission toggles for a territory and the player roster used by the whitelist page.
 */
public final class TerritoryManageViewModel extends ViewModel {
	private final ObservableProperty<TeleportMenuPayloads.TerritoryTarget> territory;
	private final List<TeleportMenuPayloads.PlayerTarget> players;

	public TerritoryManageViewModel(TeleportMenuPayloads.TerritoryTarget territory, List<TeleportMenuPayloads.PlayerTarget> players) {
		this.territory = property(territory);
		this.players = List.copyOf(players);
	}

	public ObservableProperty<TeleportMenuPayloads.TerritoryTarget> territory() {
		return this.territory;
	}

	public List<TeleportMenuPayloads.PlayerTarget> players() {
		return this.players;
	}

	public void applyTerritory(TeleportMenuPayloads.TerritoryTarget territory) {
		this.territory.set(territory);
	}

	public void updatePermission(String key, boolean value) {
		TeleportMenuPayloads.TerritoryTarget current = this.territory.get();
		ClientPlayNetworking.send(new TeleportMenuPayloads.UpdateTerritoryPermissionPayload(current.id(), key, value));
		this.territory.set(withPermission(current, key, value));
	}

	private static TeleportMenuPayloads.TerritoryTarget withPermission(TeleportMenuPayloads.TerritoryTarget t, String key, boolean value) {
		boolean buildBreak = t.buildBreakAllowed();
		boolean container = t.containerAllowed();
		boolean interact = t.interactAllowed();
		boolean damage = t.damageEntitiesAllowed();
		boolean blockUse = t.blockUseAllowed();

		switch (key) {
			case "build_break" -> buildBreak = value;
			case "container" -> container = value;
			case "interact" -> interact = value;
			case "damage_entities" -> damage = value;
			case "block_use" -> blockUse = value;
			default -> {
			}
		}

		return new TeleportMenuPayloads.TerritoryTarget(
			t.id(),
			t.name(),
			t.ownerName(),
			t.minX(),
			t.minY(),
			t.minZ(),
			t.maxX(),
			t.maxY(),
			t.maxZ(),
			t.iconItemId(),
			buildBreak,
			container,
			interact,
			damage,
			blockUse,
			new ArrayList<>(t.whitelistPlayerIds())
		);
	}
}
