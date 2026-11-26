package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;

import java.util.List;
import java.util.Set;

/**
 * Data shared by the admin hub pages.
 */
public final class AdminViewModel extends ViewModel {
	private final List<TeleportMenuPayloads.PlayerTarget> targets;
	private final Set<String> disabledItemIds;

	public AdminViewModel(List<TeleportMenuPayloads.PlayerTarget> targets, Set<String> disabledItemIds) {
		this.targets = List.copyOf(targets);
		this.disabledItemIds = Set.copyOf(disabledItemIds);
	}

	public List<TeleportMenuPayloads.PlayerTarget> targets() {
		return this.targets;
	}

	public Set<String> disabledItemIds() {
		return this.disabledItemIds;
	}
}
