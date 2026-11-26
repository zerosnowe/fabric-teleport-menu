package com.example.client.mvvm;

import com.example.client.i18n.ClientI18n;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

/**
 * Economy hub actions.
 */
public final class EconomyViewModel extends ViewModel {
	public Command boxShopHint() {
		return new Command(() -> {
			Minecraft client = Minecraft.getInstance();
			client.setScreen(null);
			if (client.player != null) {
				client.player.displayClientMessage(ClientI18n.tr("message.teleportmenu.boxshop_hint").withStyle(ChatFormatting.GREEN), false);
			}
		});
	}
}
