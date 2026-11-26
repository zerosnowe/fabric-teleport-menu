package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Rename/icon/delete state for a single shop.
 */
public final class ShopEditViewModel extends ViewModel {
	private final TeleportMenuPayloads.ShopTarget shop;
	private final ObservableProperty<String> name;

	public ShopEditViewModel(TeleportMenuPayloads.ShopTarget shop) {
		this.shop = shop;
		this.name = property(shop.name());
	}

	public TeleportMenuPayloads.ShopTarget shop() {
		return this.shop;
	}

	public ObservableProperty<String> name() {
		return this.name;
	}

	public SubmitResult rename() {
		String value = this.name.get() == null ? "" : this.name.get().trim();
		if (value.isEmpty()) {
			return SubmitResult.EMPTY;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.RenameShopRequestPayload(this.shop.id(), value));
		return SubmitResult.OK;
	}

	public Command setIcon(String iconItemId) {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.SetShopIconRequestPayload(this.shop.id(), iconItemId)));
	}

	public Command delete() {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.DeleteShopRequestPayload(this.shop.id())));
	}
}
