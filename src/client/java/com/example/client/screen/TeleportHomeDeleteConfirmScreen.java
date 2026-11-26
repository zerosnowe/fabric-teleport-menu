package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.HomeDeleteConfirmViewModel;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;

public class TeleportHomeDeleteConfirmScreen extends BaseTeleportScreen {
	private final TeleportHomeManageScreen parent;
	private final HomeDeleteConfirmViewModel viewModel;

	public TeleportHomeDeleteConfirmScreen(TeleportMenuPayloads.HomeTarget home, TeleportHomeManageScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.homes.delete_confirm_title"));
		this.parent = parent;
		this.viewModel = new HomeDeleteConfirmViewModel(home);
	}

	public TeleportHomeManageScreen parentScreen() {
		return this.parent;
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(6);

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.delete_confirm_title")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.delete_confirm_desc", this.viewModel.home().name())));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.homes.delete_confirm_yes"), TeleportIcons.CONFIRM, () -> {
			this.viewModel.delete().execute();
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent.parentScreen());
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.homes.delete_confirm_no"), TeleportIcons.CANCEL, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}
}
