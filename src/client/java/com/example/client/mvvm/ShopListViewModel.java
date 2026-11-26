package com.example.client.mvvm;

import com.example.client.TeleportMenuClient;
import com.example.network.TeleportMenuPayloads;

import java.util.List;
import java.util.Locale;

/**
 * Server shop directory with client-side search.
 */
public final class ShopListViewModel extends ViewModel {
	private final ObservableProperty<String> searchQuery = property("");

	public ObservableProperty<String> searchQuery() {
		return this.searchQuery;
	}

	public List<TeleportMenuPayloads.ShopTarget> filteredShops() {
		List<TeleportMenuPayloads.ShopTarget> all = TeleportMenuClient.shopTargets();
		String needle = this.searchQuery.get() == null ? "" : this.searchQuery.get().trim().toLowerCase(Locale.ROOT);
		if (needle.isBlank()) {
			return all;
		}

		return all.stream().filter(shop ->
			shop.name().toLowerCase(Locale.ROOT).contains(needle)
				|| shop.ownerName().toLowerCase(Locale.ROOT).contains(needle)
		).toList();
	}
}
