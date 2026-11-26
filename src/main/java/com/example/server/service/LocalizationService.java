package com.example.server.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.server.ServerLog.LOGGER;

/**
 * Loads bundled language packs that the server can use to resolve messages.
 */
public final class LocalizationService {
	private static final Map<String, Map<String, String>> SERVER_LOCALIZATIONS = new ConcurrentHashMap<>();
	private static long LOCALIZATION_VERSION = 1L;

	private LocalizationService() {
	}

	public static void load() {
		Map<String, Map<String, String>> loaded = new HashMap<>();
		loaded.put("zh_cn", readLocalizationResource("assets/teleportmenu/lang/zh_cn.json"));
		loaded.put("en_us", readLocalizationResource("assets/teleportmenu/lang/en_us.json"));

		SERVER_LOCALIZATIONS.clear();
		SERVER_LOCALIZATIONS.putAll(loaded);
		LOCALIZATION_VERSION = computeLocalizationVersion(loaded);
	}

	public static void clear() {
		SERVER_LOCALIZATIONS.clear();
	}

	private static Map<String, String> readLocalizationResource(String resourcePath) {
		try (InputStream input = LocalizationService.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (input == null) {
				LOGGER.warn("Missing localization resource: {}", resourcePath);
				return Map.of();
			}
			String raw = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
			Map<String, String> map = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					map.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			return Map.copyOf(map);
		} catch (Exception error) {
			LOGGER.error("Failed to load localization resource: {}", resourcePath, error);
			return Map.of();
		}
	}

	private static long computeLocalizationVersion(Map<String, Map<String, String>> packs) {
		long hash = 1125899906842597L;
		for (String languageCode : List.of("zh_cn", "en_us")) {
			hash = 31 * hash + languageCode.hashCode();
			Map<String, String> entries = packs.getOrDefault(languageCode, Map.of());
			List<String> keys = new ArrayList<>(entries.keySet());
			keys.sort(String::compareTo);
			for (String key : keys) {
				hash = 31 * hash + key.hashCode();
				hash = 31 * hash + entries.get(key).hashCode();
			}
		}
		return hash;
	}

	public static void syncToPlayer(ServerPlayer player) {
		// Disabled: keep UI localization bundled client-side to avoid protocol mismatch on join.
	}

	public static long version() {
		return LOCALIZATION_VERSION;
	}

	/**
	 * Resolves a translation key on the server so that clients without the mod still
	 * receive readable chat/actionbar text instead of raw keys.
	 */
	public static String translate(String languageCode, String key, Object... args) {
		if (key == null || key.isBlank()) {
			return "";
		}
		if (SERVER_LOCALIZATIONS.isEmpty()) {
			load();
		}

		String pattern = lookup(languageCode, key);
		if (pattern == null) {
			return key;
		}
		return format(pattern, args);
	}

	private static String lookup(String languageCode, String key) {
		Map<String, String> pack = packFor(languageCode);
		if (pack != null && pack.containsKey(key)) {
			return pack.get(key);
		}
		for (String fallback : List.of("en_us", "zh_cn")) {
			Map<String, String> entries = SERVER_LOCALIZATIONS.get(fallback);
			if (entries != null && entries.containsKey(key)) {
				return entries.get(key);
			}
		}
		return null;
	}

	private static Map<String, String> packFor(String languageCode) {
		if (languageCode == null || languageCode.isBlank()) {
			return null;
		}
		String normalized = languageCode.toLowerCase(Locale.ROOT).replace('-', '_');
		Map<String, String> exact = SERVER_LOCALIZATIONS.get(normalized);
		if (exact != null) {
			return exact;
		}
		for (String available : List.of("zh_cn", "en_us")) {
			if (available.startsWith(normalized + "_") || available.equals(normalized)) {
				return SERVER_LOCALIZATIONS.get(available);
			}
		}
		return null;
	}

	private static String format(String pattern, Object... args) {
		if (args == null || args.length == 0) {
			return pattern;
		}

		StringBuilder out = new StringBuilder(pattern.length() + 16);
		int nextArg = 0;
		for (int i = 0; i < pattern.length(); i++) {
			char current = pattern.charAt(i);
			if (current != '%' || i + 1 >= pattern.length()) {
				out.append(current);
				continue;
			}

			char next = pattern.charAt(i + 1);
			if (next == '%') {
				out.append('%');
				i++;
				continue;
			}
			if (next == 's' || next == 'd' || next == 'f') {
				out.append(nextArg < args.length ? stringify(args[nextArg++]) : "%" + next);
				i++;
				continue;
			}
			if (Character.isDigit(next)) {
				int digitsEnd = i + 1;
				while (digitsEnd < pattern.length() && Character.isDigit(pattern.charAt(digitsEnd))) {
					digitsEnd++;
				}
				if (digitsEnd + 1 < pattern.length() && pattern.charAt(digitsEnd) == '$'
					&& "sdf".indexOf(pattern.charAt(digitsEnd + 1)) >= 0) {
					int index = Integer.parseInt(pattern.substring(i + 1, digitsEnd)) - 1;
					out.append(index >= 0 && index < args.length ? stringify(args[index]) : "");
					i = digitsEnd + 1;
					continue;
				}
			}

			out.append(current);
		}
		return out.toString();
	}

	private static String stringify(Object arg) {
		if (arg == null) {
			return "null";
		}
		if (arg instanceof Component component) {
			return component.getString();
		}
		return String.valueOf(arg);
	}
}
