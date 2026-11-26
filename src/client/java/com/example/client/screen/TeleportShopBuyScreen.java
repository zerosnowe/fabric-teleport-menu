package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ShopBuyViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;

import java.util.Locale;

public class TeleportShopBuyScreen extends BaseTeleportScreen {
	private final TeleportShopGoodsScreen parent;
	private final ShopBuyViewModel viewModel;

	public TeleportShopBuyScreen(TeleportShopGoodsScreen parent, TeleportMenuPayloads.ShopTarget shop, TeleportMenuPayloads.ShopGoodTarget good) {
		super(ClientI18n.tr("screen.teleportmenu.shop.buy.title", good.name()));
		this.parent = parent;
		this.viewModel = new ShopBuyViewModel(shop.id(), good);
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.buy.title", this.viewModel.good().name())).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.stock", this.viewModel.good().stock())));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.buy.price", String.format(Locale.ROOT, "%.2f", this.viewModel.good().price()))));

		TextBoxComponent amount = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		amount.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		amount.setMaxLength(6);
		amount.text(this.viewModel.amount().get());
		amount.onChanged().subscribe(value -> this.viewModel.amount().set(value == null ? "" : value));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.buy.amount")));
		rootComponent.child(amount);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.buy.confirm"), TeleportIcons.CONFIRM, () -> {
			if (this.viewModel.buy() != SubmitResult.OK) {
				if (this.minecraft != null && this.minecraft.player != null) {
					this.minecraft.player.displayClientMessage(ClientI18n.tr("message.teleportmenu.shop_buy_invalid_amount").withStyle(ChatFormatting.RED), false);
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
