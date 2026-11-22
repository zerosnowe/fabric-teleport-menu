package com.example.client.screen;

import com.example.client.TeleportMenuClient;
import com.example.client.i18n.ClientI18n;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

/**
 * Full-page composer for a moment. "保存" publishes and throws the text away,
 * "存为草稿" persists the current text through the server JSON store.
 */
public class TeleportMomentComposeScreen extends BaseTeleportScreen {
	public static final int MAX_LENGTH = 256;

	private final TeleportMomentScreen parent;

	private TextBoxComponent input;
	private String inputText;

	public TeleportMomentComposeScreen(TeleportMomentScreen parent) {
		super(ClientI18n.tr("screen.teleportmenu.moments.compose.title"));
		this.parent = parent;
		this.inputText = TeleportMenuClient.momentDraft();
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

		TextBoxComponent textBox = Components.textBox(Sizing.fixed(MAIN_BUTTON_WIDTH));
		textBox.sizing(Sizing.fixed(MAIN_BUTTON_WIDTH), Sizing.fixed(BUTTON_HEIGHT));
		textBox.setMaxLength(MAX_LENGTH);
		textBox.text(this.inputText);
		textBox.onChanged().subscribe(value -> this.inputText = value == null ? "" : value);
		this.input = textBox;
		rootComponent.child(textBox);

		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.moments.compose.save"), TeleportIcons.CONFIRM, this::publish));
		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.moments.compose.draft"), TeleportIcons.SET, this::saveDraft));
		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}

	private void publish() {
		String content = this.normalizedInput();
		if (content == null) {
			return;
		}

		if (content.isEmpty()) {
			this.showMessage(ClientI18n.tr("message.teleportmenu.moment_empty"));
			return;
		}

		if (!TeleportMenuClient.sendMomentPayload(new TeleportMenuPayloads.PostMomentPayload(content))) {
			this.showMessage(ClientI18n.tr("message.teleportmenu.moment_unsupported"));
			return;
		}

		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	private void saveDraft() {
		String content = this.currentText();
		if (content.strip().length() > MAX_LENGTH) {
			this.showMessage(ClientI18n.tr("message.teleportmenu.moment_too_long", MAX_LENGTH));
			return;
		}

		if (!TeleportMenuClient.sendMomentPayload(new TeleportMenuPayloads.SaveMomentDraftPayload(content))) {
			this.showMessage(ClientI18n.tr("message.teleportmenu.moment_unsupported"));
		}
	}

	private String currentText() {
		if (this.input != null) {
			return this.input.getValue();
		}

		return this.inputText == null ? "" : this.inputText;
	}

	/**
	 * @return the trimmed text ready for publishing, or {@code null} when the
	 *         length check already reported a problem.
	 */
	private String normalizedInput() {
		String content = this.currentText().strip();
		if (content.length() > MAX_LENGTH) {
			this.showMessage(ClientI18n.tr("message.teleportmenu.moment_too_long", MAX_LENGTH));
			return null;
		}

		return content;
	}

	private void showMessage(Component message) {
		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.sendSystemMessage(message);
		}
	}
}
