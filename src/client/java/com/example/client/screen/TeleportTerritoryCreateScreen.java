package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.SubmitResult;
import com.example.client.mvvm.TerritoryCreateViewModel;
import com.example.client.widget.TeleportIcons;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;

public class TeleportTerritoryCreateScreen extends BaseTeleportScreen {
	private final TeleportMainScreen mainScreen;
	private final TerritoryCreateViewModel viewModel;

	public TeleportTerritoryCreateScreen(TeleportMainScreen mainScreen, int ax, int ay, int az, int bx, int by, int bz) {
		super(ClientI18n.tr("screen.teleportmenu.territories.create_title"));
		this.mainScreen = mainScreen;
		this.viewModel = new TerritoryCreateViewModel(ax, ay, az, bx, by, bz);
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.territories.create_title")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.territories.range",
			this.viewModel.ax(), this.viewModel.ay(), this.viewModel.az(),
			this.viewModel.bx(), this.viewModel.by(), this.viewModel.bz())));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.territories.input_prompt")));

		TextBoxComponent textBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		textBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		textBox.setMaxLength(20);
		textBox.text(this.viewModel.name().get());
		textBox.onChanged().subscribe(value -> this.viewModel.name().set(value == null ? "" : value));
		rootComponent.child(textBox);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.territories.confirm_create"), TeleportIcons.CONFIRM, () -> {
			if (this.viewModel.create() == SubmitResult.EMPTY) {
				if (this.minecraft != null && this.minecraft.player != null) {
					this.minecraft.player.sendSystemMessage(ClientI18n.tr("message.teleportmenu.territory_name_empty"));
				}
				return;
			}

			this.onClose();
		}));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.territories.cancel_create"), TeleportIcons.CANCEL, () -> {
			this.viewModel.cancel().execute();
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.mainScreen);
			}
		}));
	}
}
