package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.EconomyLeaderboardViewModel;
import com.example.client.widget.OreIconListEntryButtonWidget;
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
import net.minecraft.world.item.Items;

import java.util.List;

public class TeleportEconomyLeaderboardScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 184;

	private final TeleportEconomyScreen parent;
	private final EconomyLeaderboardViewModel viewModel = new EconomyLeaderboardViewModel();

	public TeleportEconomyLeaderboardScreen(TeleportEconomyScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.economy.leaderboard"));
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.economy.leaderboard")).shadow(true));

		FlowLayout listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		listLayout.gap(3);

		List<TeleportMenuPayloads.EconomyLeaderboardEntry> entries = this.viewModel.entries();
		if (entries.isEmpty()) {
			listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.economy.leaderboard.empty")));
		} else {
			for (int i = 0; i < entries.size(); i++) {
				TeleportMenuPayloads.EconomyLeaderboardEntry entry = entries.get(i);
				OreIconListEntryButtonWidget widget = new OreIconListEntryButtonWidget(
					MAIN_BUTTON_WIDTH,
					PLAYER_ENTRY_HEIGHT,
					new ItemStack(Items.GOLD_INGOT),
					entry.name(),
					ClientI18n.tr("screen.teleportmenu.economy.leaderboard.rank", i + 1).getString(),
					entry.formattedBalance(),
					"$",
					button -> {
					}
				);
				widget.active = false;
				var wrapped = Components.wrapVanillaWidget(widget);
				wrapped.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
				listLayout.child(wrapped);
			}
		}

		ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), listLayout);
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
}
