package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Rename/delete state for a single home.
 */
public final class HomeManageViewModel extends ViewModel {
	private final TeleportMenuPayloads.HomeTarget home;
	private final ObservableProperty<String> name;

	public HomeManageViewModel(TeleportMenuPayloads.HomeTarget home) {
		this.home = home;
		this.name = property(home.name());
	}

	public TeleportMenuPayloads.HomeTarget home() {
		return this.home;
	}

	public ObservableProperty<String> name() {
		return this.name;
	}

	public SubmitResult rename() {
		String value = this.name.get() == null ? "" : this.name.get().trim();
		if (value.isEmpty()) {
			return SubmitResult.EMPTY;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.RenameHomeRequestPayload(this.home.id(), value));
		return SubmitResult.OK;
	}

	public Command delete() {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.DeleteHomeRequestPayload(this.home.id())));
	}
}
