package com.example.client.mvvm;

import com.example.client.model.ItemManageEntry;
import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Disable/enable decision for a single registry entry.
 */
public final class ItemDisableConfirmViewModel extends ViewModel {
	private final ItemManageEntry entry;
	private final Set<String> disabledItemIds;

	public ItemDisableConfirmViewModel(ItemManageEntry entry, Set<String> disabledItemIds) {
		this.entry = entry;
		this.disabledItemIds = Set.copyOf(disabledItemIds);
	}

	public ItemManageEntry entry() {
		return this.entry;
	}

	public boolean disableSupported() {
		return this.entry.disableSupported();
	}

	public boolean alreadyDisabled() {
		return disableSupported() && this.disabledItemIds.contains(this.entry.targetItemId());
	}

	/**
	 * Sends the toggle request and returns the new disabled-id set, or {@code null}
	 * when the entry does not support disabling.
	 */
	public Set<String> toggle() {
		if (!disableSupported()) {
			return null;
		}

		boolean shouldDisable = !alreadyDisabled();
		ClientPlayNetworking.send(new TeleportMenuPayloads.DisableItemRequestPayload(this.entry.targetItemId(), shouldDisable));

		Set<String> next = new LinkedHashSet<>(this.disabledItemIds);
		if (shouldDisable) {
			next.add(this.entry.targetItemId());
		} else {
			next.remove(this.entry.targetItemId());
		}
		return next;
	}
}
