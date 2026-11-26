package com.example.server.model;

/**
 * Normalisation rules for moment (feed post) content.
 */
public final class MomentContent {
	public static final int MAX_LENGTH = 256;

	private MomentContent() {
	}

	public static String sanitize(String rawContent) {
		if (rawContent == null) {
			return "";
		}

		String normalized = rawContent
			.replace("\r\n", "\n")
			.replace('\r', '\n')
			.replace("\t", "    ");

		StringBuilder builder = new StringBuilder(Math.min(normalized.length(), MAX_LENGTH));
		for (int i = 0; i < normalized.length() && builder.length() < MAX_LENGTH; i++) {
			char current = normalized.charAt(i);
			if (current == '\n' || current >= 0x20) {
				builder.append(current);
			}
		}

		return builder.toString().strip();
	}

	public static String truncate(String content) {
		if (content == null) {
			return "";
		}

		return content.length() > MAX_LENGTH
			? content.substring(0, MAX_LENGTH)
			: content;
	}
}
