package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.HomeCreateViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.TeleportIcons;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

public class TeleportHomeCreateScreen extends BaseTeleportScreen {
	private final TeleportHomeScreen parent;
	private final HomeCreateViewModel viewModel;

	public TeleportHomeCreateScreen(TeleportHomeScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.homes.create"));
		this.parent = parent;
		this.viewModel = new HomeCreateViewModel();
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.create")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.current_pos", this.viewModel.x(), this.viewModel.y(), this.viewModel.z())));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.input_prompt")));

		TextBoxComponent textBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		textBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		textBox.setMaxLength(20);
		textBox.text(this.viewModel.name().get());
		textBox.onChanged().subscribe(value -> this.viewModel.name().set(value == null ? "" : value));
		rootComponent.child(textBox);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.homes.confirm_create"), TeleportIcons.CONFIRM, () -> {
			if (this.viewModel.create() == SubmitResult.EMPTY) {
				if (this.minecraft != null && this.minecraft.player != null) {
					this.minecraft.player.sendSystemMessage(ClientI18n.tr("message.teleportmenu.home_name_empty"));
				}
				return;
			}

			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}
}
