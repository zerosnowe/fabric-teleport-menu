package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Naming form state for a box shop.
 */
public final class BoxShopNameViewModel extends ViewModel {
	private final TeleportMenuPayloads.BoxShopTarget boxShop;
	private final ObservableProperty<String> name;

	public BoxShopNameViewModel(TeleportMenuPayloads.BoxShopTarget boxShop) {
		this.boxShop = boxShop;
		this.name = property(boxShop.name());
	}

	public TeleportMenuPayloads.BoxShopTarget boxShop() {
		return this.boxShop;
	}

	public ObservableProperty<String> name() {
		return this.name;
	}

	public SubmitResult rename() {
		String value = this.name.get() == null ? "" : this.name.get().trim();
		if (value.isEmpty()) {
			return SubmitResult.EMPTY;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.RenameBoxShopPayload(this.boxShop.boxShopId(), value));
		return SubmitResult.OK;
	}
}
