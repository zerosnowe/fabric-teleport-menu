package com.example.client.mvvm;

import com.example.client.TeleportMenuClient;
import com.example.network.TeleportMenuPayloads;

/**
 * Draft/publish state for the moment composer.
 */
public final class MomentComposeViewModel extends ViewModel {
	public static final int MAX_LENGTH = 256;

	public enum Result {
		OK,
		EMPTY,
		TOO_LONG,
		UNSUPPORTED
	}

	private final ObservableProperty<String> text;

	public MomentComposeViewModel(String initialText) {
		this.text = property(initialText == null ? "" : initialText);
	}

	public ObservableProperty<String> text() {
		return this.text;
	}

	public Result publish() {
		String content = this.text.get() == null ? "" : this.text.get().strip();
		if (content.length() > MAX_LENGTH) {
			return Result.TOO_LONG;
		}
		if (content.isEmpty()) {
			return Result.EMPTY;
		}
		if (!TeleportMenuClient.sendMomentPayload(new TeleportMenuPayloads.PostMomentPayload(content))) {
			return Result.UNSUPPORTED;
		}
		this.text.set("");
		return Result.OK;
	}

	public Result saveDraft() {
		String content = this.text.get() == null ? "" : this.text.get();
		if (content.strip().length() > MAX_LENGTH) {
			return Result.TOO_LONG;
		}
		if (!TeleportMenuClient.sendMomentPayload(new TeleportMenuPayloads.SaveMomentDraftPayload(content))) {
			return Result.UNSUPPORTED;
		}
		return Result.OK;
	}
}
