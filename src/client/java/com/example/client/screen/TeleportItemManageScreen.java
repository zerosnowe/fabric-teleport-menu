package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ItemManageViewModel;
import com.example.client.model.ItemManageEntry;
import com.example.client.widget.OreRegistryEntryButtonWidget;
import com.example.client.widget.TeleportIcons;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;

import java.util.List;
import java.util.Set;

public class TeleportItemManageScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 184;

	private final TeleportAdminScreen parent;
	private final ItemManageViewModel viewModel;
	private FlowLayout listLayout;

	public TeleportItemManageScreen(TeleportAdminScreen parent, Set<String> disabledItemIds, String searchQuery) {
		super(ClientI18n.tr("screen.teleportmenu.admin.items.title"));
		this.parent = parent;
		this.viewModel = new ItemManageViewModel(disabledItemIds, searchQuery);
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	public TeleportAdminScreen parentScreen() {
		return this.parent;
	}

	public String searchQuery() {
		return this.viewModel.searchQuery().get();
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(6);

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.title")).shadow(true));

		TextBoxComponent searchBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		searchBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		searchBox.setMaxLength(120);
		searchBox.text(this.viewModel.searchQuery().get());
		searchBox.onChanged().subscribe(value -> {
			this.viewModel.searchQuery().set(value == null ? "" : value);
			this.refreshList();
		});
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.search")));
		rootComponent.child(searchBox);

		this.listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		this.listLayout.gap(3);
		this.refreshList();

		ScrollContainer<FlowLayout> itemList = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), this.listLayout);
		itemList.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		itemList.scrollbarThiccness(4);
		itemList.surface(Surface.BLANK);
		rootComponent.child(itemList);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.close"), TeleportIcons.CLOSE, this::onClose));
	}

	private void refreshList() {
		if (this.listLayout == null) {
			return;
		}

		this.listLayout.clearChildren();
		List<ItemManageEntry> entries = this.viewModel.filteredEntries();
		if (entries.isEmpty()) {
			this.listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.empty")));
			return;
		}

		for (ItemManageEntry entry : entries) {
			OreRegistryEntryButtonWidget widget = new OreRegistryEntryButtonWidget(
				MAIN_BUTTON_WIDTH,
				PLAYER_ENTRY_HEIGHT,
				entry,
				this.viewModel.isDisabled(entry),
				clickedEntry -> {
					if (this.minecraft != null) {
						this.minecraft.setScreen(new TeleportItemDisableConfirmScreen(clickedEntry, this, this.viewModel.disabledItemIds()));
					}
				}
			);

			var wrapped = Components.wrapVanillaWidget(widget);
			wrapped.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
			this.listLayout.child(wrapped);
		}
	}
}
