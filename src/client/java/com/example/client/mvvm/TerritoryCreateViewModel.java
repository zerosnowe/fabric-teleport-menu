package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * State for the territory creation form.
 */
public final class TerritoryCreateViewModel extends ViewModel {
	private final ObservableProperty<String> name = property("");
	private final int ax;
	private final int ay;
	private final int az;
	private final int bx;
	private final int by;
	private final int bz;

	public TerritoryCreateViewModel(int ax, int ay, int az, int bx, int by, int bz) {
		this.ax = ax;
		this.ay = ay;
		this.az = az;
		this.bx = bx;
		this.by = by;
		this.bz = bz;
	}

	public ObservableProperty<String> name() {
		return this.name;
	}

	public int ax() {
		return this.ax;
	}

	public int ay() {
		return this.ay;
	}

	public int az() {
		return this.az;
	}

	public int bx() {
		return this.bx;
	}

	public int by() {
		return this.by;
	}

	public int bz() {
		return this.bz;
	}

	public SubmitResult create() {
		String value = this.name.get() == null ? "" : this.name.get().trim();
		if (value.isEmpty()) {
			return SubmitResult.EMPTY;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.CreateTerritoryRequestPayload(value));
		return SubmitResult.OK;
	}

	public Command cancel() {
		return new Command(() -> ClientPlayNetworking.send(new TeleportMenuPayloads.CancelTerritoryCreatePayload()));
	}
}
