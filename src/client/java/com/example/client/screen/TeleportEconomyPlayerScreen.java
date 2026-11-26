package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.EconomyPlayerViewModel;
import com.example.client.mvvm.SubmitResult;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;

public class TeleportEconomyPlayerScreen extends BaseTeleportScreen {
	private final TeleportEconomyManageScreen parent;
	private final EconomyPlayerViewModel viewModel;

	public TeleportEconomyPlayerScreen(TeleportEconomyManageScreen parent, TeleportMenuPayloads.PlayerTarget target) {
		super(ClientI18n.tr("screen.teleportmenu.admin.economy.player_title", target.name()));
		this.parent = parent;
		this.viewModel = new EconomyPlayerViewModel(target);
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

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.economy.player_title", this.viewModel.target().name())).shadow(true));

		TextBoxComponent amountBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		amountBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		amountBox.setMaxLength(32);
		amountBox.text(this.viewModel.amount().get());
		amountBox.onChanged().subscribe(value -> this.viewModel.amount().set(value == null ? "" : value));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.economy.amount")));
		rootComponent.child(amountBox);

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.economy.query"), TeleportIcons.QUERY, () -> this.viewModel.query().execute()));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.economy.add"), TeleportIcons.PLUS, () -> this.handle(this.viewModel.add())));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.economy.subtract"), TeleportIcons.MINUS, () -> this.handle(this.viewModel.subtract())));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.economy.set"), TeleportIcons.SET, () -> this.handle(this.viewModel.set())));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.economy.reset"), TeleportIcons.RESET, () -> this.viewModel.reset().execute()));

		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.players.close"), TeleportIcons.CLOSE, this::onClose));
	}

	private void handle(SubmitResult result) {
		if (result != SubmitResult.INVALID) {
			return;
		}

		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.displayClientMessage(ClientI18n.tr("message.teleportmenu.economy_invalid_amount").withStyle(ChatFormatting.RED), false);
		}
	}
}
