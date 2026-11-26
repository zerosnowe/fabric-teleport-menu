package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * State for the shop creation form.
 */
public final class ShopCreateViewModel extends ViewModel {
	private final ObservableProperty<String> name = property("");

	public ObservableProperty<String> name() {
		return this.name;
	}

	public SubmitResult create() {
		String value = this.name.get() == null ? "" : this.name.get().trim();
		if (value.isEmpty()) {
			return SubmitResult.EMPTY;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.CreateShopRequestPayload(value));
		return SubmitResult.OK;
	}
}
