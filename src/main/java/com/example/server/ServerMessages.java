package com.example.server;

import com.example.network.TeleportMenuPayloads;
import com.example.server.service.LocalizationService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Centralised player/command feedback helpers.
 *
 * <p>Text is resolved against the server-bundled language packs before it is sent,
 * so players who did not install the client mod still see readable messages instead
 * of raw translation keys.</p>
 */
public final class ServerMessages {
	private static final String DEFAULT_LANGUAGE = "en_us";

	private ServerMessages() {
	}

	public static String languageOf(ServerPlayer player) {
		if (player == null) {
			return DEFAULT_LANGUAGE;
		}
		String language = player.clientInformation().language();
		return language == null || language.isBlank() ? DEFAULT_LANGUAGE : language;
	}

	public static String translate(ServerPlayer player, String key, Object... args) {
		return LocalizationService.translate(languageOf(player), key, args);
	}

	public static String translate(CommandSourceStack source, String key, Object... args) {
		if (source != null && source.getEntity() instanceof ServerPlayer player) {
			return translate(player, key, args);
		}
		return LocalizationService.translate(DEFAULT_LANGUAGE, key, args);
	}

	public static Component literal(ChatFormatting color, String key, Object... args) {
		return Component.literal(LocalizationService.translate(DEFAULT_LANGUAGE, key, args)).withStyle(color);
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
		player.sendSystemMessage(Component.literal(translate(player, key, args)).withStyle(ChatFormatting.GREEN));
	}

	public static void sendError(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(Component.literal(translate(player, key, args)).withStyle(ChatFormatting.RED));
	}

	public static void sendWarn(ServerPlayer player, String key, Object... args) {
		closeTeleportMenuIfOpen(player);
		player.sendSystemMessage(Component.literal(translate(player, key, args)).withStyle(ChatFormatting.YELLOW));
	}

	public static void sendSuccess(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		String message = translate(source, key, args);
		source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), false);
	}

	public static void sendError(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		String message = translate(source, key, args);
		source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
	}

	public static void sendWarn(CommandSourceStack source, String key, Object... args) {
		if (source.getEntity() instanceof ServerPlayer player) {
			closeTeleportMenuIfOpen(player);
		}
		String message = translate(source, key, args);
		source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.YELLOW), false);
	}

	public static void sendActionBar(ServerPlayer player, ChatFormatting color, String key, Object... args) {
		player.displayClientMessage(Component.literal(translate(player, key, args)).withStyle(color), true);
	}
}
