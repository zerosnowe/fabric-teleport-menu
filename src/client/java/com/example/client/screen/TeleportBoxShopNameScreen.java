package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.BoxShopNameViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;

public class TeleportBoxShopNameScreen extends BaseTeleportScreen {
	private final TeleportBoxShopScreen parent;
	private final BoxShopNameViewModel viewModel;

	public TeleportBoxShopNameScreen(TeleportMenuPayloads.BoxShopTarget boxShop, TeleportBoxShopScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.boxshop.name"));
		this.parent = parent;
		this.viewModel = new BoxShopNameViewModel(boxShop);
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.boxshop.name")).shadow(true));

		TextBoxComponent textBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		textBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		textBox.setMaxLength(20);
		textBox.text(this.viewModel.name().get());
		textBox.onChanged().subscribe(value -> this.viewModel.name().set(value == null ? "" : value));
		rootComponent.child(textBox);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.boxshop.name_confirm"), TeleportIcons.CONFIRM, () -> {
			if (this.viewModel.rename() != SubmitResult.OK) {
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
