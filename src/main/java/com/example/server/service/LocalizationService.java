package com.example.server.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
}
