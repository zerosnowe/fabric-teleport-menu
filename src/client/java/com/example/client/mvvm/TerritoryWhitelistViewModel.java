package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Whitelist toggles for a territory.
 */
public final class TerritoryWhitelistViewModel extends ViewModel {
	private final ObservableProperty<TeleportMenuPayloads.TerritoryTarget> territory;
	private final List<TeleportMenuPayloads.PlayerTarget> players;

	public TerritoryWhitelistViewModel(TeleportMenuPayloads.TerritoryTarget territory, List<TeleportMenuPayloads.PlayerTarget> players) {
		this.territory = property(territory);
		this.players = List.copyOf(players);
	}

	public TeleportMenuPayloads.TerritoryTarget territory() {
		return this.territory.get();
	}

	public List<TeleportMenuPayloads.PlayerTarget> players() {
		return this.players;
	}

	public boolean isWhitelisted(UUID playerId) {
		return this.territory.get().whitelistPlayerIds().contains(playerId);
	}

	public TeleportMenuPayloads.TerritoryTarget toggleWhitelist(UUID playerId, boolean allow) {
		TeleportMenuPayloads.TerritoryTarget current = this.territory.get();
		ClientPlayNetworking.send(new TeleportMenuPayloads.UpdateTerritoryWhitelistPayload(current.id(), playerId, allow));
		TeleportMenuPayloads.TerritoryTarget next = withWhitelist(current, playerId, allow);
		this.territory.set(next);
		return next;
	}

	private static TeleportMenuPayloads.TerritoryTarget withWhitelist(TeleportMenuPayloads.TerritoryTarget t, UUID playerId, boolean allow) {
		Set<UUID> whitelist = new LinkedHashSet<>(t.whitelistPlayerIds());
		if (allow) {
			whitelist.add(playerId);
		} else {
			whitelist.remove(playerId);
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
			t.buildBreakAllowed(),
			t.containerAllowed(),
			t.interactAllowed(),
			t.damageEntitiesAllowed(),
			t.blockUseAllowed(),
			new ArrayList<>(whitelist)
		);
	}
}
