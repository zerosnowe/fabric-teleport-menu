package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;

/**
 * Display state for a box shop.
 */
public final class BoxShopViewModel extends ViewModel {
	private final TeleportMenuPayloads.BoxShopTarget boxShop;

	public BoxShopViewModel(TeleportMenuPayloads.BoxShopTarget boxShop) {
		this.boxShop = boxShop;
	}

	public TeleportMenuPayloads.BoxShopTarget boxShop() {
		return this.boxShop;
	}

	public boolean shouldOpenNaming() {
		return this.boxShop.owner() && this.boxShop.needsNaming();
	}
}
