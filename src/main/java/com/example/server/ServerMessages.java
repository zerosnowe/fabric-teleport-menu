package com.example.server;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Centralised player/command feedback helpers.
 */
public final class ServerMessages {
	private ServerMessages() {
	}

	public static Component coloredTranslatable(ChatFormatting color, String key, Object... args) {
		return Component.translatable(key, args).withStyle(color);
	}

	public static void closeTeleportMenuIfOpen(ServerPlayer player) {
		if (player == null) {
			return;
		}
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.CloseMenuPayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, new TeleportMenuPayloads.CloseMenuPayload());
	}

	public static void sendSuccess(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(coloredTranslatable(ChatFormatting.GREEN, key, args));
	}

	public static void sendError(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(coloredTranslatable(ChatFormatting.RED, key, args));
	}

	public static void sendWarn(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(coloredTranslatable(ChatFormatting.YELLOW, key, args));
	}

	public static void sendSuccess(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		source.sendSuccess(() -> coloredTranslatable(ChatFormatting.GREEN, key, args), false);
	}

	public static void sendError(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		source.sendFailure(coloredTranslatable(ChatFormatting.RED, key, args));
	}

	public static void sendWarn(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		source.sendSuccess(() -> coloredTranslatable(ChatFormatting.YELLOW, key, args), false);
	}
}
