package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ShopManageViewModel;
import com.example.client.widget.OreIconListEntryButtonWidget;
import com.example.client.widget.OreSmallActionButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class TeleportShopManageScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 150;
	private static final int MORE_BUTTON_WIDTH = 30;

	private final TeleportEconomyScreen parent;
	private final ShopManageViewModel viewModel = new ShopManageViewModel();

	public TeleportShopManageScreen(TeleportEconomyScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.shop.manage.title"));
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.manage.title")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.manage.limit", this.viewModel.ownShopCount(), this.viewModel.ownShopLimit())));

		FlowLayout listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		listLayout.gap(3);
		List<TeleportMenuPayloads.ShopTarget> myShops = this.viewModel.ownShops();
		if (myShops.isEmpty()) {
			listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.manage.empty")));
		} else {
			for (TeleportMenuPayloads.ShopTarget shop : myShops) {
				FlowLayout row = Containers.horizontalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
				row.gap(4);

				OreIconListEntryButtonWidget entry = new OreIconListEntryButtonWidget(
					MAIN_BUTTON_WIDTH - MORE_BUTTON_WIDTH - 4,
					PLAYER_ENTRY_HEIGHT,
					new ItemStack(resolveItem(shop.iconItemId())),
					shop.name(),
					ClientI18n.tr("screen.teleportmenu.shop.list.goods_count", shop.goods().size()).getString(),
					"",
					"",
					button -> {
					}
				);
				entry.active = false;
				var entryWrapped = Components.wrapVanillaWidget(entry);
				entryWrapped.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH - MORE_BUTTON_WIDTH - 4), Sizing.fixed(PLAYER_ENTRY_HEIGHT));

				OreSmallActionButtonWidget more = new OreSmallActionButtonWidget(
					MORE_BUTTON_WIDTH,
					PLAYER_ENTRY_HEIGHT,
					Component.literal("..."),
					button -> {
						if (this.minecraft != null) {
							this.minecraft.setScreen(new TeleportShopEditScreen(this, shop));
						}
					}
				);
				var moreWrapped = Components.wrapVanillaWidget(more);
				moreWrapped.sizing(Sizing.fixed(MORE_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));

				row.child(entryWrapped);
				row.child(moreWrapped);
				listLayout.child(row);
			}
		}

		ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), listLayout);
		scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		scroll.scrollbarThiccness(4);
		scroll.surface(Surface.BLANK);
		rootComponent.child(scroll);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.manage.create"), TeleportIcons.ADD, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportShopCreateScreen(this));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.shop.manage.expand"), TeleportIcons.EXPAND, () -> {
			this.viewModel.expandLimit().execute();
			if (this.minecraft != null) {
				this.minecraft.setScreen(this);
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}

	private static Item resolveItem(String rawId) {
		try {
			return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rawId)).orElse(Items.CHEST);
		} catch (Exception ignored) {
			return Items.CHEST;
		}
	}
}
