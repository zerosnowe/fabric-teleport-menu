package com.example.client.screen;

import com.example.client.TeleportMenuClient;
import com.example.client.i18n.ClientI18n;
import com.example.client.mvvm.MomentComposeViewModel;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

/**
 * Full-page composer for a moment. "保存" publishes and throws the text away,
 * "存为草稿" persists the current text through the server JSON store.
 */
public class TeleportMomentComposeScreen extends BaseTeleportScreen {
	public static final int MAX_LENGTH = MomentComposeViewModel.MAX_LENGTH;

	private static final int INPUT_HEIGHT = 112;

	private final TeleportMomentScreen parent;
	private final MomentComposeViewModel viewModel;

	public TeleportMomentComposeScreen(TeleportMomentScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.moments.compose.title"));
		this.parent = parent;
		this.viewModel = new MomentComposeViewModel(TeleportMenuClient.momentDraft());
	}

	public TeleportMomentScreen parentScreen() {
		return this.parent;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(6);

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.moments.compose.title")).shadow(true));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.moments.compose.hint", MAX_LENGTH)));

		TextAreaComponent textArea = Components.textArea(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(INPUT_HEIGHT));
		textArea.setCharacterLimit(MAX_LENGTH);
		textArea.text(this.viewModel.text().get());
		textArea.onChanged().subscribe(value -> this.viewModel.text().set(value == null ? "" : value));
		rootComponent.child(textArea);

		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.moments.compose.save"), TeleportIcons.CONFIRM, this::publish));
		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.moments.compose.draft"), TeleportIcons.SET, this::saveDraft));
		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}

	private void publish() {
		switch (this.viewModel.publish()) {
			case OK -> {
				if (this.minecraft != null) {
					this.minecraft.setScreen(this.parent);
				}
			}
			case EMPTY -> this.showMessage(ClientI18n.tr("message.teleportmenu.moment_empty"));
			case TOO_LONG -> this.showMessage(ClientI18n.tr("message.teleportmenu.moment_too_long", MAX_LENGTH));
			case UNSUPPORTED -> this.showMessage(ClientI18n.tr("message.teleportmenu.moment_unsupported"));
		}
	}

	private void saveDraft() {
		switch (this.viewModel.saveDraft()) {
			case OK -> {
			}
			case TOO_LONG -> this.showMessage(ClientI18n.tr("message.teleportmenu.moment_too_long", MAX_LENGTH));
			case UNSUPPORTED -> this.showMessage(ClientI18n.tr("message.teleportmenu.moment_unsupported"));
			default -> {
			}
		}
	}

	private void showMessage(Component message) {
		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.sendSystemMessage(message);
		}
	}
}
