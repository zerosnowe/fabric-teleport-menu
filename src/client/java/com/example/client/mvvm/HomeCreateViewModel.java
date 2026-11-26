package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * State for the home creation form.
 */
public final class HomeCreateViewModel extends ViewModel {
	private final ObservableProperty<String> name = property("");
	private final int x;
	private final int y;
	private final int z;

	public HomeCreateViewModel() {
		BlockPos pos = currentPlayerPos();
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public ObservableProperty<String> name() {
		return this.name;
	}

	public int x() {
		return this.x;
	}

	public int y() {
		return this.y;
	}

	public int z() {
		return this.z;
	}

	public SubmitResult create() {
		String value = this.name.get() == null ? "" : this.name.get().trim();
		if (value.isEmpty()) {
			return SubmitResult.EMPTY;
		}

		ClientPlayNetworking.send(new TeleportMenuPayloads.CreateHomeRequestPayload(value));
		return SubmitResult.OK;
	}

	private static BlockPos currentPlayerPos() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return BlockPos.ZERO;
		}

		return client.player.blockPosition();
	}
}
