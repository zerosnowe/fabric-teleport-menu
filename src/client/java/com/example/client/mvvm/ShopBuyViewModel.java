package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

/**
 * Buy form shared by the economy shop and the box shop.
 */
public final class ShopBuyViewModel extends ViewModel {
	private final UUID shopId;
	private final TeleportMenuPayloads.ShopGoodTarget good;
	private final ObservableProperty<String> amount = property("1");

	public ShopBuyViewModel(UUID shopId, TeleportMenuPayloads.ShopGoodTarget good) {
		this.shopId = shopId;
		this.good = good;
	}

	public TeleportMenuPayloads.ShopGoodTarget good() {
		return this.good;
	}

	public ObservableProperty<String> amount() {
		return this.amount;
	}

	public SubmitResult buy() {
		int amountValue;
		try {
			amountValue = Integer.parseInt(this.amount.get().trim());
		} catch (Exception ignored) {
			return SubmitResult.INVALID;
		}
		if (amountValue <= 0) {
			return SubmitResult.INVALID;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.BuyShopGoodRequestPayload(this.shopId, this.good.itemId(), amountValue));
		return SubmitResult.OK;
	}
}
