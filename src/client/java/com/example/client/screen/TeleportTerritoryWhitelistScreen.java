package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.TerritoryWhitelistViewModel;
import com.example.client.widget.OrePlayerEntryButtonWidget;
import com.example.client.widget.OreToggleSliderWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;

import java.util.List;

public class TeleportTerritoryWhitelistScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 176;
	private static final int TOGGLE_WIDTH = 52;

	private final TeleportTerritoryManageScreen parent;
	private final TerritoryWhitelistViewModel viewModel;

	public TeleportTerritoryWhitelistScreen(TeleportMenuPayloads.TerritoryTarget territory, List<TeleportMenuPayloads.PlayerTarget> players, TeleportTerritoryManageScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.territories.whitelist"));
		this.parent = parent;
		this.viewModel = new TerritoryWhitelistViewModel(territory, players);
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(8);

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.territories.whitelist")).shadow(true));

		FlowLayout listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		listLayout.gap(3);

		List<TeleportMenuPayloads.PlayerTarget> players = this.viewModel.players();
		if (players.isEmpty()) {
			listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.territories.whitelist.empty")));
		} else {
			for (TeleportMenuPayloads.PlayerTarget player : players) {
				FlowLayout row = Containers.horizontalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
				row.gap(4);
				row.verticalAlignment(VerticalAlignment.CENTER);

				OrePlayerEntryButtonWidget entryWidget = new OrePlayerEntryButtonWidget(
					MAIN_BUTTON_WIDTH - TOGGLE_WIDTH - 4,
					PLAYER_ENTRY_HEIGHT,
					player.uuid(),
					player.name(),
					uuid -> {
					}
				);
				entryWidget.active = false;
				var entryComponent = Components.wrapVanillaWidget(entryWidget);
				entryComponent.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH - TOGGLE_WIDTH - 4), Sizing.fixed(PLAYER_ENTRY_HEIGHT));

				boolean whitelisted = this.viewModel.isWhitelisted(player.uuid());
				OreToggleSliderWidget toggleWidget = new OreToggleSliderWidget(TOGGLE_WIDTH, 20, whitelisted, value -> {
					TeleportMenuPayloads.TerritoryTarget next = this.viewModel.toggleWhitelist(player.uuid(), value);
					this.parent.updateTerritory(next);
				});
				var toggleComponent = Components.wrapVanillaWidget(toggleWidget);
				toggleComponent.sizing(Sizing.fixed(TOGGLE_WIDTH), Sizing.fixed(20));

				row.child(entryComponent);
				row.child(toggleComponent);
				listLayout.child(row);
			}
		}

		ScrollContainer<FlowLayout> whitelistList = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), listLayout);
		whitelistList.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		whitelistList.scrollbarThiccness(4);
		whitelistList.surface(Surface.BLANK);

		rootComponent.child(whitelistList);
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.territories.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}
}
