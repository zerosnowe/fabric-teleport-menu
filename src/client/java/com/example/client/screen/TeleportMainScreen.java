package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.MainMenuViewModel;
import com.example.client.widget.OreCardButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class TeleportMainScreen extends BaseTeleportScreen {
	private static final int CARD_WIDTH = 112;
	private static final int CARD_HEIGHT = 72;
	private static final int CARD_GAP = 8;

	private final MainMenuViewModel viewModel;

	public TeleportMainScreen(
		List<TeleportMenuPayloads.PlayerTarget> targets,
		List<TeleportMenuPayloads.HomeTarget> homes,
		List<TeleportMenuPayloads.TerritoryTarget> territories,
		boolean admin,
		Set<String> disabledItemIds
	) {
		super(ClientI18n.tr("screen.teleportmenu.main.title"));
		this.viewModel = new MainMenuViewModel(targets, homes, territories, admin, disabledItemIds);
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	public List<TeleportMenuPayloads.PlayerTarget> targets() {
		return this.viewModel.targets();
	}

	public List<TeleportMenuPayloads.HomeTarget> homes() {
		return this.viewModel.homes();
	}

	public List<TeleportMenuPayloads.TerritoryTarget> territories() {
		return this.viewModel.territories();
	}

	public boolean admin() {
		return this.viewModel.admin();
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(10);
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.main.title")).shadow(true));

		FlowLayout firstRow = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(CARD_HEIGHT));
		firstRow.gap(CARD_GAP);
		firstRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.players"), TeleportIcons.PLAYERS, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportPlayerListScreen(this.viewModel.targets(), this));
			}
		}));
		firstRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.homes"), TeleportIcons.HOMES, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportHomeScreen(this.viewModel.homes(), this));
			}
		}));
		firstRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.territories"), TeleportIcons.TERRITORIES, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportTerritoryScreen(this.viewModel.territories(), this.viewModel.targets(), this));
			}
		}));
		firstRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.economy"), TeleportIcons.ECONOMY, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportEconomyScreen(this));
			}
		}));
		rootComponent.child(firstRow);

		FlowLayout secondRow = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(CARD_HEIGHT));
		secondRow.gap(CARD_GAP);
		secondRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.moments"), TeleportIcons.MOMENTS, () -> {
			if (this.minecraft == null) {
				return;
			}

			if (this.viewModel.requestMoments() == MainMenuViewModel.MomentsResult.UNSUPPORTED) {
				if (this.minecraft.player != null) {
					this.minecraft.player.sendSystemMessage(ClientI18n.tr("message.teleportmenu.moment_unsupported"));
				}
				return;
			}

			this.minecraft.setScreen(new TeleportMomentScreen(this));
		}));
		if (this.viewModel.admin()) {
			secondRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.admin"), TeleportIcons.ADMIN, () -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new TeleportAdminScreen(this, this.viewModel.disabledItemIds()));
				}
			}));
		}
		secondRow.child(cardButton(ClientI18n.tr("screen.teleportmenu.main.close"), TeleportIcons.CLOSE, this::onClose));
		rootComponent.child(secondRow);
	}

	private io.wispforest.owo.ui.core.Component cardButton(Component text, ResourceLocation icon, Runnable onPress) {
		OreCardButtonWidget widget = new OreCardButtonWidget(CARD_WIDTH, CARD_HEIGHT, text, icon, button -> onPress.run());
		var wrapped = Components.wrapVanillaWidget(widget);
		wrapped.sizing(Sizing.fixed(CARD_WIDTH), Sizing.fixed(CARD_HEIGHT));
		return wrapped;
	}
}
