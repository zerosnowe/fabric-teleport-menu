package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.HomeListViewModel;
import com.example.client.widget.OreHomeEntryButtonWidget;
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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TeleportHomeScreen extends BaseTeleportScreen {
	private static final int LIST_HEIGHT = 176;
	private static final int MORE_BUTTON_WIDTH = 30;

	private final HomeListViewModel viewModel;
	private final TeleportMainScreen mainScreen;

	public TeleportHomeScreen(List<TeleportMenuPayloads.HomeTarget> homes, TeleportMainScreen mainScreen) {
		super(ClientI18n.tr("screen.teleportmenu.homes.title"));
		this.viewModel = new HomeListViewModel(homes);
		this.mainScreen = mainScreen;
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	public TeleportMainScreen mainScreen() {
		return this.mainScreen;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(8);

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.title")).shadow(true));

		FlowLayout listLayout = Containers.verticalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.content());
		listLayout.gap(3);

		List<TeleportMenuPayloads.HomeTarget> homes = this.viewModel.homes().get();
		if (homes.isEmpty()) {
			listLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.homes.empty")));
		} else {
			for (TeleportMenuPayloads.HomeTarget home : homes) {
				FlowLayout row = Containers.horizontalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));
				row.gap(4);

				OreHomeEntryButtonWidget widget = new OreHomeEntryButtonWidget(
					MAIN_BUTTON_WIDTH - MORE_BUTTON_WIDTH - 4,
					PLAYER_ENTRY_HEIGHT,
					home.id(),
					home.name(),
					home.x(),
					home.y(),
					home.z(),
					home.iconItemId(),
					homeId -> {
						this.viewModel.teleport(home).execute();
						this.onClose();
					}
				);

				var entryComponent = Components.wrapVanillaWidget(widget);
				entryComponent.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH - MORE_BUTTON_WIDTH - 4), Sizing.fixed(PLAYER_ENTRY_HEIGHT));

				OreSmallActionButtonWidget moreWidget = new OreSmallActionButtonWidget(
					MORE_BUTTON_WIDTH,
					PLAYER_ENTRY_HEIGHT,
					ClientI18n.tr("screen.teleportmenu.homes.more"),
					button -> {
						if (this.minecraft != null) {
							this.minecraft.setScreen(new TeleportHomeManageScreen(home, this));
						}
					}
				);
				var moreComponent = Components.wrapVanillaWidget(moreWidget);
				moreComponent.sizing(Sizing.fixed(MORE_BUTTON_WIDTH), Sizing.fixed(PLAYER_ENTRY_HEIGHT));

				row.child(entryComponent);
				row.child(moreComponent);
				listLayout.child(row);
			}
		}

		ScrollContainer<FlowLayout> homeList = Containers.verticalScroll(Sizing.fixed(MAIN_BUTTON_WIDTH + 4), Sizing.fixed(LIST_HEIGHT), listLayout);
		homeList.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		homeList.scrollbarThiccness(4);
		homeList.surface(Surface.BLANK);

		rootComponent.child(homeList);
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.homes.create"), TeleportIcons.ADD, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportHomeCreateScreen(this));
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.mainScreen);
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.close"), TeleportIcons.CLOSE, this::onClose));
	}
}
