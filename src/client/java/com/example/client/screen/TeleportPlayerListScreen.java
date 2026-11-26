package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.PlayerListViewModel;
import com.example.client.widget.OrePlayerEntryButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;

import java.util.List;

public class TeleportPlayerListScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 176;

	private final TeleportMainScreen parent;
	private final PlayerListViewModel viewModel;

	public TeleportPlayerListScreen(List<TeleportMenuPayloads.PlayerTarget> targets, TeleportMainScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.players.title"));
		this.parent = parent;
		this.viewModel = new PlayerListViewModel(targets);
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.players.title")).shadow(true));

		FlowLayout listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		listLayout.gap(3);

		List<TeleportMenuPayloads.PlayerTarget> targets = this.viewModel.targets();
		if (targets.isEmpty()) {
			listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.players.empty")));
		} else {
			for (TeleportMenuPayloads.PlayerTarget target : targets) {
				OrePlayerEntryButtonWidget widget = new OrePlayerEntryButtonWidget(
					MAIN_BUTTON_WIDTH,
					PLAYER_ENTRY_HEIGHT,
					target.uuid(),
					target.name(),
					playerId -> {
						this.viewModel.teleport(playerId).execute();
						this.onClose();
					});

				var entryComponent = Components.wrapVanillaWidget(widget);
				entryComponent.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
				listLayout.child(entryComponent);
			}
		}

		ScrollContainer<FlowLayout> playerList = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), listLayout);
		playerList.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		playerList.scrollbarThiccness(4);
		playerList.surface(Surface.BLANK);

		rootComponent.child(playerList);
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.close"), TeleportIcons.CLOSE, this::onClose));
	}
}
