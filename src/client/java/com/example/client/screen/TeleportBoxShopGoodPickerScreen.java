package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ItemSlotPickerViewModel;
import com.example.client.widget.OreItemSlotButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class TeleportBoxShopGoodPickerScreen extends BaseTeleportScreen {
	private static final int SLOT_SIZE = 22;
	private static final int COLUMNS = 9;
	private static final int LIST_HEIGHT = 150;

	private final TeleportBoxShopManageScreen parent;
	private final TeleportMenuPayloads.BoxShopTarget boxShop;
	private final ItemStack previousSelected;
	private final String amountInput;
	private final String priceInput;
	private final ItemSlotPickerViewModel viewModel = new ItemSlotPickerViewModel();

	public TeleportBoxShopGoodPickerScreen(
		TeleportBoxShopManageScreen parent,
		TeleportMenuPayloads.BoxShopTarget boxShop,
		ItemStack previousSelected,
		String amountInput,
		String priceInput
	) {
		super(ClientI18n.tr("screen.teleportmenu.shop.goods.select_chest_title"));
		this.parent = parent;
		this.boxShop = boxShop;
		this.previousSelected = previousSelected == null ? ItemStack.EMPTY : previousSelected.copy();
		this.amountInput = amountInput == null || amountInput.isBlank() ? "1" : amountInput;
		this.priceInput = priceInput == null || priceInput.isBlank() ? "10" : priceInput;
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.select_chest_title")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.select_hint")));

		FlowLayout rows = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		rows.gap(2);

		List<ItemStack> inventoryStacks = this.viewModel.inventoryStacks();
		if (inventoryStacks.isEmpty()) {
			rows.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.empty")));
		} else {
			for (int start = 0; start < inventoryStacks.size(); start += COLUMNS) {
				FlowLayout row = Containers.horizontalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
				row.gap(2);
				int end = Math.min(start + COLUMNS, inventoryStacks.size());
				for (int i = start; i < end; i++) {
					ItemStack stack = inventoryStacks.get(i);
					OreItemSlotButtonWidget slotButton = new OreItemSlotButtonWidget(SLOT_SIZE, stack, button -> {
						if (this.minecraft != null) {
							this.minecraft.setScreen(new TeleportBoxShopAddGoodScreen(this.parent, this.boxShop, button.stack(), this.amountInput, this.priceInput));
						}
					});
					var wrapped = Components.wrapVanillaWidget(slotButton);
					wrapped.sizing(Sizing.fixed(SLOT_SIZE), Sizing.fixed(SLOT_SIZE));
					row.child(wrapped);
				}
				rows.child(row);
			}
		}

		ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), rows);
		scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		scroll.scrollbarThiccness(4);
		scroll.surface(Surface.BLANK);
		rootComponent.child(scroll);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportBoxShopAddGoodScreen(this.parent, this.boxShop, this.previousSelected, this.amountInput, this.priceInput));
			}
		}));
	}
}
