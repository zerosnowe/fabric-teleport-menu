package com.example.client.mvvm;

import com.example.client.TeleportMenuClient;
import com.example.network.TeleportMenuPayloads;

import java.util.List;
import java.util.Set;

/**
 * Root menu data and the moment-feed capability check.
 */
public final class MainMenuViewModel extends ViewModel {
	public enum MomentsResult {
		OK,
		UNSUPPORTED
	}

	private final List<TeleportMenuPayloads.PlayerTarget> targets;
	private final List<TeleportMenuPayloads.HomeTarget> homes;
	private final List<TeleportMenuPayloads.TerritoryTarget> territories;
	private final boolean admin;
	private final Set<String> disabledItemIds;

	public MainMenuViewModel(
		List<TeleportMenuPayloads.PlayerTarget> targets,
		List<TeleportMenuPayloads.HomeTarget> homes,
		List<TeleportMenuPayloads.TerritoryTarget> territories,
		boolean admin,
		Set<String> disabledItemIds
	) {
		this.targets = List.copyOf(targets);
		this.homes = List.copyOf(homes);
		this.territories = List.copyOf(territories);
		this.admin = admin;
		this.disabledItemIds = Set.copyOf(disabledItemIds);
	}

	public List<TeleportMenuPayloads.PlayerTarget> targets() {
		return this.targets;
	}

	public List<TeleportMenuPayloads.HomeTarget> homes() {
		return this.homes;
	}

	public List<TeleportMenuPayloads.TerritoryTarget> territories() {
		return this.territories;
	}

	public boolean admin() {
		return this.admin;
	}

	public Set<String> disabledItemIds() {
		return this.disabledItemIds;
	}

	public MomentsResult requestMoments() {
		return TeleportMenuClient.requestMomentFeed() ? MomentsResult.OK : MomentsResult.UNSUPPORTED;
	}
}
