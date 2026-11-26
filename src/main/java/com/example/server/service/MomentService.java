package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import com.example.server.model.MomentContent;
import com.example.server.model.MomentPoint;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerState.ACTIVE_SERVER;
import static com.example.server.ServerState.MOMENT_DRAFTS;
import static com.example.server.ServerState.MOMENTS;
import static com.example.server.service.PersistenceService.persistDataIfPossible;

/**
 * Player "moments" feed: posting, liking, deleting and drafts.
 */
public final class MomentService {
	private static final int MOMENT_MAX_COUNT = 100;

	private MomentService() {
	}

	public static void handleRequestMomentFeed(ServerPlayer player) {
		sendMomentFeed(player);
	}

	public static void handlePostMoment(ServerPlayer player, String rawContent) {
		if (player == null) {
			return;
		}

		String content = sanitizeMomentContent(rawContent);
		if (content.isBlank()) {
			sendError(player, "message.teleportmenu.moment_empty");
			return;
		}

		MomentPoint moment = new MomentPoint(
			UUID.randomUUID(),
			player.getUUID(),
			player.getName().getString(),
			content,
			System.currentTimeMillis()
		);
		MOMENTS.put(moment.id(), moment);
		MOMENT_DRAFTS.remove(player.getUUID());
		trimMoments();
		persistDataIfPossible();
		sendSuccess(player, "message.teleportmenu.moment_posted");
		broadcastMomentFeeds();
	}

	public static void handleLikeMoment(ServerPlayer player, UUID momentId) {
		if (player == null || momentId == null) {
			return;
		}

		MomentPoint moment = MOMENTS.get(momentId);
		if (moment == null) {
			sendError(player, "message.teleportmenu.moment_not_found");
			return;
		}

		moment.toggleLike(player.getUUID());
		persistDataIfPossible();
		broadcastMomentFeeds();
	}

	public static void handleDeleteMoment(ServerPlayer player, UUID momentId) {
		if (player == null || momentId == null) {
			return;
		}

		MomentPoint moment = MOMENTS.remove(momentId);
		if (moment == null) {
			sendError(player, "message.teleportmenu.moment_not_found");
			sendMomentFeed(player);
			return;
		}

		persistDataIfPossible();
		sendSuccess(player, "message.teleportmenu.moment_deleted");
		broadcastMomentFeeds();
	}

	public static void handleSaveMomentDraft(ServerPlayer player, String rawContent) {
		if (player == null) {
			return;
		}

		String content = rawContent == null ? "" : rawContent;
		if (content.isBlank()) {
			MOMENT_DRAFTS.remove(player.getUUID());
			sendSuccess(player, "message.teleportmenu.moment_draft_cleared");
		} else {
			MOMENT_DRAFTS.put(player.getUUID(), truncateMomentContent(content));
			sendSuccess(player, "message.teleportmenu.moment_draft_saved");
		}

		persistDataIfPossible();
		sendMomentFeed(player);
	}

	public static void sendMomentFeed(ServerPlayer player) {
		if (player == null || player.server == null) {
			return;
		}
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.MomentFeedPayload.TYPE)) {
			return;
		}

		ServerPlayNetworking.send(player, buildMomentFeed(player));
	}

	private static TeleportMenuPayloads.MomentFeedPayload buildMomentFeed(ServerPlayer viewer) {
		UUID viewerId = viewer.getUUID();
		List<TeleportMenuPayloads.MomentEntry> entries = new ArrayList<>();
		for (MomentPoint moment : MOMENTS.values()) {
			entries.add(new TeleportMenuPayloads.MomentEntry(
				moment.id(),
				moment.authorId(),
				moment.authorName(),
				moment.content(),
				moment.createdAt(),
				moment.likedBy().size(),
				moment.likedBy().contains(viewerId),
				moment.authorId().equals(viewerId)
			));
		}

		entries.sort(Comparator.comparingLong(TeleportMenuPayloads.MomentEntry::createdAt).reversed());
		return new TeleportMenuPayloads.MomentFeedPayload(entries, MOMENT_DRAFTS.getOrDefault(viewerId, ""));
	}

	public static void broadcastMomentFeeds() {
		MinecraftServer server = ACTIVE_SERVER;
		if (server == null) {
			return;
		}

		for (ServerPlayer online : server.getPlayerList().getPlayers()) {
			sendMomentFeed(online);
		}
	}

	private static void trimMoments() {
		if (MOMENTS.size() <= MOMENT_MAX_COUNT) {
			return;
		}

		List<MomentPoint> sorted = new ArrayList<>(MOMENTS.values());
		sorted.sort(Comparator.comparingLong(MomentPoint::createdAt));
		int removeCount = sorted.size() - MOMENT_MAX_COUNT;
		for (int i = 0; i < removeCount; i++) {
			MOMENTS.remove(sorted.get(i).id());
		}
	}

	private static String sanitizeMomentContent(String rawContent) {
		return MomentContent.sanitize(rawContent);
	}

	private static String truncateMomentContent(String content) {
		return MomentContent.truncate(content);
	}
}
