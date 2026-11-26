package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ShopGoodsViewModel;
import com.example.client.widget.OreIconListEntryButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

public class TeleportShopGoodsScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 174;

	private final TeleportShopListScreen parent;
	private final ShopGoodsViewModel viewModel;
	private FlowLayout listLayout;

	public TeleportShopGoodsScreen(TeleportShopListScreen parent, TeleportMenuPayloads.ShopTarget shop) {
		super(ClientI18n.tr("screen.teleportmenu.shop.goods.title", shop.name()));
		this.parent = parent;
		this.viewModel = new ShopGoodsViewModel(shop);
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
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.title", this.viewModel.shop().name())).shadow(true));

		TextBoxComponent search = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		search.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		search.setMaxLength(80);
		search.text(this.viewModel.searchQuery().get());
		search.onChanged().subscribe(value -> {
			this.viewModel.searchQuery().set(value == null ? "" : value);
			this.refreshList();
		});
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.search")));
		rootComponent.child(search);

		this.listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		this.listLayout.gap(3);
		this.refreshList();

		ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), this.listLayout);
		scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		scroll.scrollbarThiccness(4);
		scroll.surface(Surface.BLANK);
		rootComponent.child(scroll);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}

	private void refreshList() {
		if (this.listLayout == null) {
			return;
		}
		this.listLayout.clearChildren();

		List<TeleportMenuPayloads.ShopGoodTarget> goods = this.viewModel.filteredGoods();
		if (goods.isEmpty()) {
			this.listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.shop.goods.empty")));
			return;
		}

		for (TeleportMenuPayloads.ShopGoodTarget good : goods) {
			String subtitle = ClientI18n.tr("screen.teleportmenu.shop.goods.stock", good.stock()).getString();
			String rightTop = String.format(Locale.ROOT, "%.2f", good.price());
			OreIconListEntryButtonWidget widget = new OreIconListEntryButtonWidget(
				MAIN_BUTTON_WIDTH,
				PLAYER_ENTRY_HEIGHT,
				new ItemStack(resolveItem(good.iconItemId())),
				good.name(),
				subtitle,
				rightTop,
				"$",
				button -> {
					if (this.minecraft != null) {
						this.minecraft.setScreen(new TeleportShopBuyScreen(this, this.viewModel.shop(), good));
					}
				}
			);
			var wrapped = Components.wrapVanillaWidget(widget);
			wrapped.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
			this.listLayout.child(wrapped);
		}
	}

	private static Item resolveItem(String rawId) {
		try {
			return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rawId)).orElse(Items.CHEST);
		} catch (Exception ignored) {
			return Items.CHEST;
		}
	}
}
