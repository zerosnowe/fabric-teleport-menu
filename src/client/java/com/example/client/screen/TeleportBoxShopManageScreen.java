package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.BoxShopViewModel;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;

public class TeleportBoxShopManageScreen extends BaseTeleportScreen {
	private final TeleportBoxShopScreen parent;
	private final BoxShopViewModel viewModel;

	public TeleportBoxShopManageScreen(TeleportMenuPayloads.BoxShopTarget boxShop, TeleportBoxShopScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.boxshop.manage"));
		this.parent = parent;
		this.viewModel = new BoxShopViewModel(boxShop);
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.boxshop.manage")).shadow(true));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.boxshop.name"), TeleportIcons.RENAME, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportBoxShopNameScreen(this.viewModel.boxShop(), this.parent));
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.goods.add"), TeleportIcons.ADD, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportBoxShopAddGoodScreen(this, this.viewModel.boxShop()));
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}
}
