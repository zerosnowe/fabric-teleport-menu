package com.example.server.model;

/**
 * Shared sanitising rules for box shop display names.
 */
public final class BoxShopNames {
	public static final String DEFAULT_NAME = "箱子商店";
	public static final int MAX_LENGTH = 20;

	private BoxShopNames() {
	}

	public static String sanitize(String rawName) {
		String sanitized = rawName == null ? "" : rawName
			.replace('\r', ' ')
			.replace('\n', ' ')
			.replace('\t', ' ')
			.trim();
		if (sanitized.isEmpty()) {
			sanitized = DEFAULT_NAME;
		}
		if (sanitized.length() > MAX_LENGTH) {
			sanitized = sanitized.substring(0, MAX_LENGTH);
		}
		return sanitized;
	}
}
