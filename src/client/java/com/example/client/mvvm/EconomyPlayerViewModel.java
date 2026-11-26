package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Economy admin actions for a single player.
 */
public final class EconomyPlayerViewModel extends ViewModel {
	private final TeleportMenuPayloads.PlayerTarget target;
	private final ObservableProperty<String> amount = property("10");

	public EconomyPlayerViewModel(TeleportMenuPayloads.PlayerTarget target) {
		this.target = target;
	}

	public TeleportMenuPayloads.PlayerTarget target() {
		return this.target;
	}

	public ObservableProperty<String> amount() {
		return this.amount;
	}

	public Command query() {
		return new Command(() -> sendAction("query", 0D));
	}

	public Command reset() {
		return new Command(() -> sendAction("reset", 0D));
	}

	public SubmitResult add() {
		return apply("add");
	}

	public SubmitResult subtract() {
		return apply("subtract");
	}

	public SubmitResult set() {
		return apply("set");
	}

	private SubmitResult apply(String action) {
		Double value = parseAmount(this.amount.get());
		if (value == null) {
			return SubmitResult.INVALID;
		}
		sendAction(action, value);
		return SubmitResult.OK;
	}

	private void sendAction(String action, double amount) {
		ClientPlayNetworking.send(new TeleportMenuPayloads.EconomyAdminActionPayload(this.target.uuid(), action, amount));
	}

	private static Double parseAmount(String raw) {
		String trimmed = raw == null ? "" : raw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		try {
			double value = Double.parseDouble(trimmed);
			if (Double.isFinite(value) && value >= 0D) {
				return value;
			}
		} catch (NumberFormatException ignored) {
		}

		return null;
	}
}
