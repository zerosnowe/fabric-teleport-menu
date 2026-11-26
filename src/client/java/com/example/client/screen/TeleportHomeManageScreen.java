package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.HomeManageViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

public class TeleportHomeManageScreen extends BaseTeleportScreen {
	private final TeleportHomeScreen parent;
	private final HomeManageViewModel viewModel;

	public TeleportHomeManageScreen(TeleportMenuPayloads.HomeTarget home, TeleportHomeScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.homes.manage_title"));
		this.parent = parent;
		this.viewModel = new HomeManageViewModel(home);
	}

	public TeleportHomeScreen parentScreen() {
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

		TeleportMenuPayloads.HomeTarget home = this.viewModel.home();
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.manage_title")).shadow(true));
		rootComponent.child(Components.label(Component.literal(home.name())));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.current_pos", home.x(), home.y(), home.z())));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.rename_prompt")));

		TextBoxComponent textBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		textBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		textBox.setMaxLength(20);
		textBox.text(this.viewModel.name().get());
		textBox.onChanged().subscribe(value -> this.viewModel.name().set(value == null ? "" : value));
		rootComponent.child(textBox);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.homes.rename"), TeleportIcons.RENAME, () -> {
			if (this.viewModel.rename() == SubmitResult.EMPTY) {
				if (this.minecraft != null && this.minecraft.player != null) {
					this.minecraft.player.sendSystemMessage(ClientI18n.tr("message.teleportmenu.home_name_empty"));
				}
				return;
			}

			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.homes.delete"), TeleportIcons.DELETE, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportHomeDeleteConfirmScreen(home, this));
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}
}
