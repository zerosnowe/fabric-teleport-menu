package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.EconomyViewModel;
import com.example.client.widget.TeleportIcons;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;

public class TeleportEconomyScreen extends BaseTeleportScreen {
	private final TeleportMainScreen mainScreen;
	private final EconomyViewModel viewModel = new EconomyViewModel();

	public TeleportEconomyScreen(TeleportMainScreen mainScreen) {
		super(ClientI18n.tr("screen.teleportmenu.economy.title"));
		this.mainScreen = mainScreen;
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(8);
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.economy.title")).shadow(true));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.economy.leaderboard"), TeleportIcons.LEADERBOARD, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportEconomyLeaderboardScreen(this));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.economy.shops"), TeleportIcons.SHOP, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportShopListScreen(this));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.economy.manage"), TeleportIcons.MANAGE, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportShopManageScreen(this));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.economy.boxshop"), TeleportIcons.BOXSHOP, () -> this.viewModel.boxShopHint().execute()));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.mainScreen);
			}
		}));
	}
}
