package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;

import java.util.List;
import java.util.Locale;

/**
 * Goods list of a single shop with client-side search.
 */
public final class ShopGoodsViewModel extends ViewModel {
	private final TeleportMenuPayloads.ShopTarget shop;
	private final ObservableProperty<String> searchQuery = property("");

	public ShopGoodsViewModel(TeleportMenuPayloads.ShopTarget shop) {
		this.shop = shop;
	}

	public TeleportMenuPayloads.ShopTarget shop() {
		return this.shop;
	}

	public ObservableProperty<String> searchQuery() {
		return this.searchQuery;
	}

	public List<TeleportMenuPayloads.ShopGoodTarget> filteredGoods() {
		String needle = this.searchQuery.get() == null ? "" : this.searchQuery.get().trim().toLowerCase(Locale.ROOT);
		if (needle.isBlank()) {
			return this.shop.goods();
		}

		return this.shop.goods().stream().filter(good ->
			good.name().toLowerCase(Locale.ROOT).contains(needle)
				|| good.itemId().toLowerCase(Locale.ROOT).contains(needle)
		).toList();
	}
}
