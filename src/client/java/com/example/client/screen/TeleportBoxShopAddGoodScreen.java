package com.example.client.screen;

import com.example.client.i18n.ClientI18n;
import com.example.client.mvvm.ShopAddGoodViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.OreIconListEntryButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.world.item.ItemStack;

public class TeleportBoxShopAddGoodScreen extends BaseTeleportScreen {
	private final TeleportBoxShopManageScreen parent;
	private final TeleportMenuPayloads.BoxShopTarget boxShop;
	private final ShopAddGoodViewModel viewModel;

	public TeleportBoxShopAddGoodScreen(TeleportBoxShopManageScreen parent, TeleportMenuPayloads.BoxShopTarget boxShop) {
		this(parent, boxShop, ItemStack.EMPTY, "1", "10");
	}

	public TeleportBoxShopAddGoodScreen(
		TeleportBoxShopManageScreen parent,
		TeleportMenuPayloads.BoxShopTarget boxShop,
		ItemStack selectedStack,
		String amountInput,
		String priceInput
	) {
		super(ClientI18n.tr("screen.teleportmenu.shop.goods.add"));
		this.parent = parent;
		this.boxShop = boxShop;
		this.viewModel = new ShopAddGoodViewModel(boxShop.shopId(), selectedStack, amountInput, priceInput);
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.add")).shadow(true));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.goods.select"), TeleportIcons.SELECT, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportBoxShopGoodPickerScreen(
					this.parent, this.boxShop, this.viewModel.selectedStack(), this.viewModel.amount().get(), this.viewModel.price().get()));
			}
		}));

		if (!this.viewModel.hasSelection()) {
			rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.select_empty")));
			rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.select_hint")));
		} else {
			ItemStack selectedStack = this.viewModel.selectedStack();
			OreIconListEntryButtonWidget widget = new OreIconListEntryButtonWidget(
				MAIN_BUTTON_WIDTH,
				PLAYER_ENTRY_HEIGHT,
				new ItemStack(selectedStack.getItem()),
				this.viewModel.selectedItemName(),
				ClientI18n.tr("screen.teleportmenu.shop.goods.selected_id", this.viewModel.selectedItemId()).getString(),
				String.valueOf(this.viewModel.selectedStackCount()),
				ClientI18n.tr("screen.teleportmenu.shop.goods.stock_unit").getString(),
				button -> {}
			);
			widget.active = false;
			var wrapped = Components.wrapVanillaWidget(widget);
			wrapped.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
			rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.selected", this.viewModel.selectedItemName())));
			rootComponent.child(wrapped);
		}

		TextBoxComponent amount = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		amount.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		amount.setMaxLength(6);
		amount.text(this.viewModel.amount().get());
		amount.onChanged().subscribe(value -> this.viewModel.amount().set(value == null ? "" : value));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.stock_input")));
		rootComponent.child(amount);

		TextBoxComponent price = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		price.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		price.setMaxLength(12);
		price.text(this.viewModel.price().get());
		price.onChanged().subscribe(value -> this.viewModel.price().set(value == null ? "" : value));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.price_input")));
		rootComponent.child(price);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.goods.add_confirm"), TeleportIcons.CONFIRM, this::submit));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}

	private void submit() {
		if (this.viewModel.submit() != SubmitResult.OK) {
			return;
		}

		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}
}
