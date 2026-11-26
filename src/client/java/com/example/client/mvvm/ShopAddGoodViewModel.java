package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Add-good form shared by the economy shop and the box shop.
 */
public final class ShopAddGoodViewModel extends ViewModel {
	private final UUID shopId;
	private final ItemStack selectedStack;
	private final String selectedItemId;
	private final String selectedItemName;
	private final int selectedStackCount;
	private final ObservableProperty<String> amount;
	private final ObservableProperty<String> price;

	public ShopAddGoodViewModel(UUID shopId, ItemStack selectedStack, String amountInput, String priceInput) {
		this.shopId = shopId;
		this.selectedStack = selectedStack == null ? ItemStack.EMPTY : selectedStack.copy();
		this.selectedStackCount = this.selectedStack.isEmpty() ? 0 : this.selectedStack.getCount();
		if (!this.selectedStack.isEmpty()) {
			this.selectedItemId = BuiltInRegistries.ITEM.getKey(this.selectedStack.getItem()).toString();
			this.selectedItemName = this.selectedStack.getHoverName().getString();
		} else {
			this.selectedItemId = "";
			this.selectedItemName = "";
		}
		this.amount = property(amountInput == null || amountInput.isBlank() ? "1" : amountInput);
		this.price = property(priceInput == null || priceInput.isBlank() ? "10" : priceInput);
	}

	public UUID shopId() {
		return this.shopId;
	}

	public ItemStack selectedStack() {
		return this.selectedStack.copy();
	}

	public String selectedItemId() {
		return this.selectedItemId;
	}

	public String selectedItemName() {
		return this.selectedItemName;
	}

	public int selectedStackCount() {
		return this.selectedStackCount;
	}

	public boolean hasSelection() {
		return !this.selectedItemId.isBlank();
	}

	public ObservableProperty<String> amount() {
		return this.amount;
	}

	public ObservableProperty<String> price() {
		return this.price;
	}

	public SubmitResult submit() {
		if (this.selectedItemId.isBlank()) {
			return SubmitResult.EMPTY;
		}

		int stock;
		double price;
		try {
			stock = Integer.parseInt(this.amount.get().trim());
			price = Double.parseDouble(this.price.get().trim());
		} catch (Exception ignored) {
			return SubmitResult.INVALID;
		}
		if (stock <= 0 || price < 0D || !Double.isFinite(price)) {
			return SubmitResult.INVALID;
		}

		ResourceLocation itemKey = ResourceLocation.tryParse(this.selectedItemId);
		if (itemKey == null || !BuiltInRegistries.ITEM.containsKey(itemKey)) {
			return SubmitResult.INVALID;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.AddShopGoodRequestPayload(this.shopId, this.selectedItemId, stock, price));
		return SubmitResult.OK;
	}
}
