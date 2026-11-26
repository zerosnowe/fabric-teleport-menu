package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import com.example.server.model.HomePoint;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerState.PLAYER_HOMES;
import static com.example.server.service.PersistenceService.persistDataIfPossible;

/**
 * Home point domain: creation, renaming, deletion and teleporting.
 */
public final class HomeService {
	private static final List<Item> HOME_ICON_POOL = List.of(
		Items.COMPASS, Items.CLOCK, Items.RED_BED, Items.OAK_DOOR, Items.CHEST, Items.TORCH,
		Items.CAMPFIRE, Items.BREAD, Items.GOLDEN_APPLE, Items.DIAMOND, Items.ENDER_PEARL, Items.BOOK
	);

	private HomeService() {
	}

	public static void handleTeleportHomeRequest(ServerPlayer source, UUID homeId) {
		if (homeId == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		HomePoint home = PLAYER_HOMES
			.getOrDefault(source.getUUID(), List.of())
			.stream()
			.filter(candidate -> candidate.id().equals(homeId))
			.findFirst()
			.orElse(null);

		if (home == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		source.teleportTo((ServerLevel)source.level(), home.x() + 0.5, home.y(), home.z() + 0.5, source.getYRot(), source.getXRot());
		sendSuccess(source, "message.teleportmenu.teleport_success_home", home.name(), home.x(), home.y(), home.z());
	}

	public static void handleCreateHomeRequest(ServerPlayer source, String rawName) {
		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.home_name_empty");
			return;
		}

		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<HomePoint> homes = PLAYER_HOMES.computeIfAbsent(source.getUUID(), key -> new ArrayList<>());
		final String finalName = name;
		boolean exists = homes.stream().anyMatch(home -> home.name().equalsIgnoreCase(finalName));
		if (exists) {
			sendError(source, "message.teleportmenu.home_name_exists", finalName);
			return;
		}

		int x = source.blockPosition().getX();
		int y = source.blockPosition().getY();
		int z = source.blockPosition().getZ();
		String iconItemId = BuiltInRegistries.ITEM.getKey(randomHomeIcon(source)).toString();

		homes.add(new HomePoint(UUID.randomUUID(), finalName, x, y, z, iconItemId));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.home_created", finalName, x, y, z);
		syncHomesToClient(source);
	}

	public static void handleDeleteHomeRequest(ServerPlayer source, UUID homeId) {
		if (homeId == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		List<HomePoint> homes = PLAYER_HOMES.get(source.getUUID());
		if (homes == null || homes.isEmpty()) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		HomePoint home = homes.stream().filter(candidate -> candidate.id().equals(homeId)).findFirst().orElse(null);
		if (home == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		homes.removeIf(candidate -> candidate.id().equals(homeId));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.home_deleted", home.name());
		syncHomesToClient(source);
	}

	public static void handleRenameHomeRequest(ServerPlayer source, UUID homeId, String rawName) {
		if (homeId == null) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.home_name_empty");
			return;
		}

		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<HomePoint> homes = PLAYER_HOMES.get(source.getUUID());
		if (homes == null || homes.isEmpty()) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		int index = -1;
		for (int i = 0; i < homes.size(); i++) {
			if (homes.get(i).id().equals(homeId)) {
				index = i;
				break;
			}
		}

		if (index < 0) {
			sendError(source, "message.teleportmenu.home_not_found");
			return;
		}

		final String finalName = name;
		boolean duplicate = homes.stream().anyMatch(home -> !home.id().equals(homeId) && home.name().equalsIgnoreCase(finalName));
		if (duplicate) {
			sendError(source, "message.teleportmenu.home_name_exists", finalName);
			return;
		}

		HomePoint old = homes.get(index);
		homes.set(index, new HomePoint(old.id(), finalName, old.x(), old.y(), old.z(), old.iconItemId()));
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.home_renamed", old.name(), finalName);
		syncHomesToClient(source);
	}

	public static void syncHomesToClient(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, TeleportMenuPayloads.HomeListPayload.TYPE)) {
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.HomeListPayload(getHomeTargets(player)));
		}
	}

	public static List<TeleportMenuPayloads.HomeTarget> getHomeTargets(ServerPlayer player) {
		return PLAYER_HOMES
			.getOrDefault(player.getUUID(), List.of())
			.stream()
			.map(home -> new TeleportMenuPayloads.HomeTarget(home.id(), home.name(), home.x(), home.y(), home.z(), home.iconItemId()))
			.toList();
	}

	public static Item randomHomeIcon(ServerPlayer player) {
		return HOME_ICON_POOL.get(player.getRandom().nextInt(HOME_ICON_POOL.size()));
	}
}
