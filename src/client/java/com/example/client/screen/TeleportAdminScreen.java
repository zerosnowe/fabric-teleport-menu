package com.example.client.screen;

import com.example.client.i18n.ClientI18n;
import com.example.client.mvvm.AdminViewModel;
import com.example.client.widget.TeleportIcons;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.network.chat.Component;

import java.util.Set;

public class TeleportAdminScreen extends BaseTeleportScreen {
	private final TeleportMainScreen mainScreen;
	private final AdminViewModel viewModel;

	public TeleportAdminScreen(TeleportMainScreen mainScreen, Set<String> disabledItemIds) {
		super(ClientI18n.tr("screen.teleportmenu.admin.title"));
		this.mainScreen = mainScreen;
		this.viewModel = new AdminViewModel(mainScreen.targets(), disabledItemIds);
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	public TeleportMainScreen mainScreen() {
		return this.mainScreen;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.title")).shadow(true));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.item_manage"), TeleportIcons.INVENTORY, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportItemManageScreen(this, this.viewModel.disabledItemIds(), ""));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.economy"), TeleportIcons.COIN, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportEconomyManageScreen(this, this.viewModel.targets()));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.mainScreen);
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.close"), TeleportIcons.CLOSE, this::onClose));
	}
}
