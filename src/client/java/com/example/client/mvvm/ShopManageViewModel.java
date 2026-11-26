package com.example.client.mvvm;

import com.example.client.TeleportMenuClient;
import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.UUID;

/**
 * Player-owned shops and the purchase limit.
 */
public final class ShopManageViewModel extends ViewModel {
	public int ownShopCount() {
		return TeleportMenuClient.ownShopCount();
	}

	public int ownShopLimit() {
		return TeleportMenuClient.ownShopLimit();
	}

	public List<TeleportMenuPayloads.ShopTarget> ownShops() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return List.of();
		}

		UUID self = client.player.getUUID();
		return TeleportMenuClient.shopTargets().stream().filter(shop -> shop.ownerId().equals(self)).toList();
	}

	public Command expandLimit() {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.ExpandShopLimitRequestPayload()));
	}
}
