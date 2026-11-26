package com.example.server.model;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single feed post together with the players who liked it.
 */
public final class MomentPoint {
	private final UUID id;
	private final UUID authorId;
	private final String authorName;
	private final String content;
	private final long createdAt;
	private final Set<UUID> likedBy = ConcurrentHashMap.newKeySet();

	public MomentPoint(UUID id, UUID authorId, String authorName, String content, long createdAt) {
		this.id = id;
		this.authorId = authorId;
		this.authorName = authorName == null ? "" : authorName;
		this.content = content == null ? "" : content;
		this.createdAt = createdAt;
	}

	public UUID id() { return this.id; }
	public UUID authorId() { return this.authorId; }
	public String authorName() { return this.authorName; }
	public String content() { return this.content; }
	public long createdAt() { return this.createdAt; }
	public Set<UUID> likedBy() { return this.likedBy; }

	public boolean toggleLike(UUID playerId) {
		if (this.likedBy.contains(playerId)) {
			this.likedBy.remove(playerId);
			return false;
		}

		this.likedBy.add(playerId);
		return true;
	}
}
