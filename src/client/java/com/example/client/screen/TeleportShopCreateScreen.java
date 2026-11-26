package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ShopCreateViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.TeleportIcons;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;

public class TeleportShopCreateScreen extends BaseTeleportScreen {
	private final TeleportShopManageScreen parent;
	private final ShopCreateViewModel viewModel = new ShopCreateViewModel();

	public TeleportShopCreateScreen(TeleportShopManageScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.shop.manage.create"));
		this.parent = parent;
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.manage.create")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.create.prompt")));

		TextBoxComponent text = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		text.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		text.setMaxLength(20);
		text.text(this.viewModel.name().get());
		text.onChanged().subscribe(value -> this.viewModel.name().set(value == null ? "" : value));
		rootComponent.child(text);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.create.confirm"), TeleportIcons.CONFIRM, () -> {
			if (this.viewModel.create() == SubmitResult.EMPTY) {
				if (this.minecraft != null && this.minecraft.player != null) {
					this.minecraft.player.displayClientMessage(ClientI18n.tr("message.teleportmenu.shop_name_empty").withStyle(ChatFormatting.RED), false);
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
