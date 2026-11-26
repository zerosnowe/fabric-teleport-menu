package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ShopEditViewModel;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TeleportShopEditScreen extends BaseTeleportScreen {
	private final TeleportShopManageScreen parent;
	private final ShopEditViewModel viewModel;

	public TeleportShopEditScreen(TeleportShopManageScreen parent, TeleportMenuPayloads.ShopTarget shop) {
		super(ClientI18n.tr("screen.teleportmenu.shop.edit.title", shop.name()));
		this.parent = parent;
		this.viewModel = new ShopEditViewModel(shop);
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.edit.title", this.viewModel.shop().name())).shadow(true));

		TextBoxComponent text = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		text.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		text.setMaxLength(20);
		text.text(this.viewModel.name().get());
		text.onChanged().subscribe(value -> this.viewModel.name().set(value == null ? "" : value));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.rename.prompt")));
		rootComponent.child(text);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.rename.confirm"), TeleportIcons.RENAME, () -> this.viewModel.rename()));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.icon.chest"), new ItemStack(Items.CHEST), () -> this.viewModel.setIcon("minecraft:chest").execute()));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.icon.ender"), new ItemStack(Items.ENDER_CHEST), () -> this.viewModel.setIcon("minecraft:ender_chest").execute()));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.icon.shulker"), new ItemStack(Items.SHULKER_BOX), () -> this.viewModel.setIcon("minecraft:shulker_box").execute()));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.icon.barrel"), new ItemStack(Items.BARREL), () -> this.viewModel.setIcon("minecraft:barrel").execute()));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.goods.add"), TeleportIcons.ADD, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportShopAddGoodScreen(this, this.viewModel.shop()));
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.delete"), TeleportIcons.DELETE, () -> {
			this.viewModel.delete().execute();
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
