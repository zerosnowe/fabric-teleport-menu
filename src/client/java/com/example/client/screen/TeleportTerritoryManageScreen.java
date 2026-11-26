package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.TerritoryManageViewModel;
import com.example.client.widget.OreToggleSliderWidget;
import com.example.client.widget.OreSmallActionButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TeleportTerritoryManageScreen extends BaseTeleportScreen {
	private static final int TOGGLE_WIDTH = 52;

	private final TeleportTerritoryScreen parent;
	private final TerritoryManageViewModel viewModel;

	public TeleportTerritoryManageScreen(TeleportMenuPayloads.TerritoryTarget territory, List<TeleportMenuPayloads.PlayerTarget> players, TeleportTerritoryScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.territories.manage_title"));
		this.parent = parent;
		this.viewModel = new TerritoryManageViewModel(territory, players);
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

		TeleportMenuPayloads.TerritoryTarget territory = this.viewModel.territory().get();
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.territories.manage_title")).shadow(true));
		rootComponent.child(Components.label(Component.literal(territory.name())));

		rootComponent.child(permissionRow(ClientI18n.tr("screen.teleportmenu.territories.permission.build_break"), territory.buildBreakAllowed(), "build_break"));
		rootComponent.child(permissionRow(ClientI18n.tr("screen.teleportmenu.territories.permission.container"), territory.containerAllowed(), "container"));
		rootComponent.child(permissionRow(ClientI18n.tr("screen.teleportmenu.territories.permission.interact"), territory.interactAllowed(), "interact"));
		rootComponent.child(permissionRow(ClientI18n.tr("screen.teleportmenu.territories.permission.damage_entities"), territory.damageEntitiesAllowed(), "damage_entities"));
		rootComponent.child(permissionRow(ClientI18n.tr("screen.teleportmenu.territories.permission.block_use"), territory.blockUseAllowed(), "block_use"));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.territories.whitelist"), TeleportIcons.WHITELIST, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportTerritoryWhitelistScreen(this.viewModel.territory().get(), this.viewModel.players(), this));
			}
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}

	public TeleportMenuPayloads.TerritoryTarget territory() {
		return this.viewModel.territory().get();
	}

	void updateTerritory(TeleportMenuPayloads.TerritoryTarget territory) {
		this.viewModel.applyTerritory(territory);
	}

	private io.wispforest.owo.ui.core.Component permissionRow(Component label, boolean enabled, String key) {
		FlowLayout row = Containers.horizontalFlow(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		row.gap(6);
		row.verticalAlignment(VerticalAlignment.CENTER);

		OreSmallActionButtonWidget labelWidget = new OreSmallActionButtonWidget(172, BUTTON_HEIGHT, label, button -> {
		});
		labelWidget.active = false;
		var labelComponent = Components.wrapVanillaWidget(labelWidget);
		labelComponent.sizing(Sizing.fixed(172), Sizing.fixed(BUTTON_HEIGHT));
		row.child(labelComponent);

		OreToggleSliderWidget toggleWidget = new OreToggleSliderWidget(TOGGLE_WIDTH, BUTTON_HEIGHT - 4, enabled, value ->
			this.viewModel.updatePermission(key, value));
		var toggle = Components.wrapVanillaWidget(toggleWidget);
		toggle.sizing(Sizing.fixed(TOGGLE_WIDTH), Sizing.fixed(BUTTON_HEIGHT - 4));
		row.child(toggle);

		return row;
	}
}
