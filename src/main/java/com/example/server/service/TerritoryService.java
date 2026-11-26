package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import com.example.server.ServerState.TerritoryHudState;
import com.example.server.ServerState.TerritorySelection;
import com.example.server.model.TerritoryPermission;
import com.example.server.model.TerritoryPoint;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.server.ServerMessages.sendActionBar;
import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerMessages.sendWarn;
import static com.example.server.ServerMessages.translate;
import static com.example.server.ServerState.LAST_DENY_ACTIONBAR_TICK;
import static com.example.server.ServerState.PLAYER_TERRITORIES;
import static com.example.server.ServerState.SERVER_TICK;
import static com.example.server.ServerState.TERRITORY_HUD_STATES;
import static com.example.server.ServerState.TERRITORY_SELECTIONS;
import static com.example.server.service.HomeService.randomHomeIcon;
import static com.example.server.service.PersistenceService.persistDataIfPossible;

/**
 * Territory domain: selection, creation, permissions, HUD and particles.
 */
public final class TerritoryService {
	private static final DustParticleOptions TERRITORY_SELECTION_PARTICLE = new DustParticleOptions(new Vector3f(0.25F, 1.0F, 0.25F), 1.0F);

	private TerritoryService() {
	}

	public static int showTerritoryUsage(CommandSourceStack source) {
		sendWarn(source, "message.teleportmenu.territory_usage_1");
		sendWarn(source, "message.teleportmenu.territory_usage_2");
		sendWarn(source, "message.teleportmenu.territory_usage_3");
		return 1;
	}

	public static int setTerritoryPoint(CommandSourceStack source, boolean setA) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		UUID playerId = player.getUUID();
		ResourceLocation dimensionId = player.level().dimension().location();
		BlockPos pos = player.blockPosition();

		if (setA) {
			if (isInsideOtherTerritory(playerId, dimensionId, pos)) {
				sendWarn(source, "message.teleportmenu.territory_too_close");
				return 0;
			}
			TERRITORY_SELECTIONS.put(playerId, new TerritorySelection(dimensionId, pos, null));
			sendWarn(source, "message.teleportmenu.territory_set_a", pos.getX(), pos.getY(), pos.getZ());
			sendWarn(source, "message.teleportmenu.territory_step_next_b");
			return 1;
		}

		TerritorySelection selection = TERRITORY_SELECTIONS.get(playerId);
		if (selection == null || selection.pointA() == null) {
			sendWarn(source, "message.teleportmenu.territory_need_a_first");
			return 0;
		}

		if (!selection.dimensionId().equals(dimensionId)) {
			sendWarn(source, "message.teleportmenu.territory_dimension_mismatch");
			return 0;
		}

		if (isInsideOtherTerritory(playerId, dimensionId, pos)) {
			sendWarn(source, "message.teleportmenu.territory_too_close");
			return 0;
		}

		TERRITORY_SELECTIONS.put(playerId, new TerritorySelection(dimensionId, selection.pointA(), pos));
		sendWarn(source, "message.teleportmenu.territory_set_b", pos.getX(), pos.getY(), pos.getZ());
		sendWarn(source, "message.teleportmenu.territory_open_clock_to_create");
		return 1;
	}

	public static void handleTeleportTerritoryRequest(ServerPlayer source, UUID territoryId) {
		if (territoryId == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		TerritoryPoint territory = PLAYER_TERRITORIES
			.getOrDefault(source.getUUID(), List.of())
			.stream()
			.filter(candidate -> candidate.id().equals(territoryId))
			.findFirst()
			.orElse(null);

		if (territory == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, territory.dimensionId());
		ServerLevel targetLevel = source.server.getLevel(levelKey);
		if (targetLevel == null) {
			sendError(source, "message.teleportmenu.territory_dimension_missing");
			return;
		}

		double targetX = ((double) territory.minX() + (double) territory.maxX() + 1D) / 2D;
		double targetY = territory.maxY() + 1D;
		double targetZ = ((double) territory.minZ() + (double) territory.maxZ() + 1D) / 2D;

		source.teleportTo(targetLevel, targetX, targetY, targetZ, source.getYRot(), source.getXRot());
		sendSuccess(source, "message.teleportmenu.teleport_success_territory", territory.name());
	}

	public static void handleCreateTerritoryRequest(ServerPlayer source, String rawName) {
		TerritorySelection selection = TERRITORY_SELECTIONS.get(source.getUUID());
		if (selection == null || selection.pointA() == null || selection.pointB() == null) {
			sendError(source, "message.teleportmenu.territory_need_ab");
			return;
		}

		if (!selection.dimensionId().equals(source.level().dimension().location())) {
			sendError(source, "message.teleportmenu.territory_dimension_mismatch");
			return;
		}

		String name = rawName == null ? "" : rawName.trim();
		if (name.isEmpty()) {
			sendError(source, "message.teleportmenu.territory_name_empty");
			return;
		}

		if (name.length() > 20) {
			name = name.substring(0, 20);
		}

		List<TerritoryPoint> territories = PLAYER_TERRITORIES.computeIfAbsent(source.getUUID(), key -> new java.util.ArrayList<>());
		final String finalName = name;
		if (territories.stream().anyMatch(t -> t.name().equalsIgnoreCase(finalName))) {
			sendError(source, "message.teleportmenu.territory_name_exists", finalName);
			return;
		}

		BlockPos a = selection.pointA();
		BlockPos b = selection.pointB();
		int minX = Math.min(a.getX(), b.getX());
		int minY = Math.min(a.getY(), b.getY());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxX = Math.max(a.getX(), b.getX());
		int maxY = Math.max(a.getY(), b.getY());
		int maxZ = Math.max(a.getZ(), b.getZ());

		if (overlapsOtherTerritory(source.getUUID(), selection.dimensionId(), minX, minY, minZ, maxX, maxY, maxZ)) {
			sendError(source, "message.teleportmenu.territory_chunk_occupied");
			return;
		}

		String iconItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(randomHomeIcon(source)).toString();
		territories.add(new TerritoryPoint(
			UUID.randomUUID(),
			source.getUUID(),
			source.getName().getString(),
			finalName,
			selection.dimensionId(),
			minX,
			minY,
			minZ,
			maxX,
			maxY,
			maxZ,
			iconItemId,
			true,
			true,
			true,
			true,
			true,
			ConcurrentHashMap.newKeySet()
		));

		TERRITORY_SELECTIONS.remove(source.getUUID());
		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.territory_created", finalName, minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static void handleCancelTerritoryCreate(ServerPlayer source) {
		if (TERRITORY_SELECTIONS.remove(source.getUUID()) != null) {
			sendWarn(source, "message.teleportmenu.territory_create_cancelled");
		} else {
			sendWarn(source, "message.teleportmenu.territory_nothing_to_cancel");
		}
	}

	public static void handleUpdateTerritoryPermission(ServerPlayer source, UUID territoryId, String permissionKey, boolean allowed) {
		TerritoryPoint territory = findOwnedTerritory(source, territoryId);
		if (territory == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		TerritoryPermission permission = TerritoryPermission.fromKey(permissionKey);
		if (permission == null) {
			sendError(source, "message.teleportmenu.territory_permission_invalid");
			return;
		}

		applyTerritoryPermission(territory, permission, allowed);
		persistDataIfPossible();
		sendSuccess(
			source,
			"message.teleportmenu.territory_permission_updated",
			translate(source, permission.translationKey()),
			translate(source, allowed ? "screen.teleportmenu.territories.allow" : "screen.teleportmenu.territories.deny")
		);
	}

	public static void handleUpdateTerritoryWhitelist(ServerPlayer source, UUID territoryId, UUID playerId, boolean allowed) {
		if (playerId == null || playerId.equals(source.getUUID())) {
			sendError(source, "message.teleportmenu.territory_whitelist_invalid_player");
			return;
		}

		TerritoryPoint territory = findOwnedTerritory(source, territoryId);
		if (territory == null) {
			sendError(source, "message.teleportmenu.territory_not_found");
			return;
		}

		if (allowed) {
			territory.whitelistPlayerIds().add(playerId);
		} else {
			territory.whitelistPlayerIds().remove(playerId);
		}
		persistDataIfPossible();
	}

	public static void showTerritorySelectionParticles(ServerPlayer player) {
		TerritorySelection selection = TERRITORY_SELECTIONS.get(player.getUUID());
		if (selection == null || selection.pointA() == null || selection.pointB() == null) {
			return;
		}

		if (!selection.dimensionId().equals(player.level().dimension().location())) {
			return;
		}

		spawnSelectionBox(player, selection.pointA(), selection.pointB());
	}

	private static void spawnSelectionBox(ServerPlayer player, BlockPos a, BlockPos b) {
		ServerLevel level = (ServerLevel) player.level();
		int minX = Math.min(a.getX(), b.getX());
		int minY = Math.min(a.getY(), b.getY());
		int minZ = Math.min(a.getZ(), b.getZ());
		int maxX = Math.max(a.getX(), b.getX());
		int maxY = Math.max(a.getY(), b.getY());
		int maxZ = Math.max(a.getZ(), b.getZ());

		int maxSpan = Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
		int step = Math.max(1, maxSpan / 48 + 1);

		for (int x = minX; x <= maxX; x += step) {
			spawnEdgeColumn(level, player, x, minY, maxY, minZ, maxZ, step);
		}
		for (int y = minY; y <= maxY; y += step) {
			spawnEdgeRow(level, player, minX, maxX, y, minZ, maxZ, step);
		}
		for (int z = minZ; z <= maxZ; z += step) {
			spawnEdgeDepth(level, player, minX, maxX, minY, maxY, z, step);
		}

		spawnBoxCorner(level, player, minX, minY, minZ);
		spawnBoxCorner(level, player, minX, minY, maxZ);
		spawnBoxCorner(level, player, minX, maxY, minZ);
		spawnBoxCorner(level, player, minX, maxY, maxZ);
		spawnBoxCorner(level, player, maxX, minY, minZ);
		spawnBoxCorner(level, player, maxX, minY, maxZ);
		spawnBoxCorner(level, player, maxX, maxY, minZ);
		spawnBoxCorner(level, player, maxX, maxY, maxZ);
	}

	private static void spawnEdgeColumn(ServerLevel level, ServerPlayer player, int x, int minY, int maxY, int minZ, int maxZ, int step) {
		for (int y = minY; y <= maxY; y += step) {
			spawnBoxCorner(level, player, x, y, minZ);
			spawnBoxCorner(level, player, x, y, maxZ);
		}
	}

	private static void spawnEdgeRow(ServerLevel level, ServerPlayer player, int minX, int maxX, int y, int minZ, int maxZ, int step) {
		for (int x = minX; x <= maxX; x += step) {
			spawnBoxCorner(level, player, x, y, minZ);
			spawnBoxCorner(level, player, x, y, maxZ);
		}
	}

	private static void spawnEdgeDepth(ServerLevel level, ServerPlayer player, int minX, int maxX, int minY, int maxY, int z, int step) {
		for (int x = minX; x <= maxX; x += step) {
			spawnBoxCorner(level, player, x, minY, z);
			spawnBoxCorner(level, player, x, maxY, z);
		}
	}

	private static void spawnBoxCorner(ServerLevel level, ServerPlayer player, int x, int y, int z) {
		level.sendParticles(player, TERRITORY_SELECTION_PARTICLE, true, x + 0.5D, y + 0.5D, z + 0.5D, 1, 0D, 0D, 0D, 0D);
	}

	public static void updateTerritoryHud(ServerPlayer player) {
		UUID playerId = player.getUUID();
		TerritoryPoint inTerritory = findTerritoryContaining(player);
		TerritoryHudState state = TERRITORY_HUD_STATES.get(playerId);

		if (inTerritory == null) {
			if (state != null) {
				if (SERVER_TICK < state.clearAtTick()) {
					player.displayClientMessage(Component.empty(), true);
				}
				TERRITORY_HUD_STATES.remove(playerId);
			}
			return;
		}

		if (state == null || !inTerritory.id().equals(state.territoryId())) {
			if (inTerritory.ownerId().equals(player.getUUID())) {
				player.displayClientMessage(Component.literal(translate(player, "message.teleportmenu.territory_owner_welcome", inTerritory.name(), player.getName().getString())), true);
			} else {
				player.displayClientMessage(Component.literal(translate(player, "message.teleportmenu.territory_entered", inTerritory.ownerName())), true);
			}
			TERRITORY_HUD_STATES.put(playerId, new TerritoryHudState(inTerritory.id(), SERVER_TICK + 40L));
			return;
		}

		if (SERVER_TICK >= state.clearAtTick()) {
			player.displayClientMessage(Component.empty(), true);
			TERRITORY_HUD_STATES.put(playerId, new TerritoryHudState(state.territoryId(), Long.MAX_VALUE));
		}
	}

	private static TerritoryPoint findTerritoryContaining(ServerPlayer player) {
		ResourceLocation currentDimension = player.level().dimension().location();
		BlockPos pos = player.blockPosition();

		for (List<TerritoryPoint> territories : PLAYER_TERRITORIES.values()) {
			for (TerritoryPoint territory : territories) {
				if (!territory.dimensionId().equals(currentDimension)) {
					continue;
				}
				if (territory.contains(pos.getX(), pos.getY(), pos.getZ())) {
					return territory;
				}
			}
		}

		return null;
	}

	public static boolean checkTerritoryPermission(ServerPlayer player, BlockPos pos, TerritoryPermission permission) {
		TerritoryPoint territory = findTerritoryContaining(player.level().dimension().location(), pos);
		if (territory == null) {
			return true;
		}

		if (territory.ownerId().equals(player.getUUID()) || territory.whitelistPlayerIds().contains(player.getUUID())) {
			return true;
		}

		boolean allowed = switch (permission) {
			case BUILD_BREAK -> territory.buildBreakAllowed();
			case CONTAINER -> territory.containerAllowed();
			case INTERACT -> territory.interactAllowed();
			case DAMAGE_ENTITIES -> territory.damageEntitiesAllowed();
			case BLOCK_USE -> territory.blockUseAllowed();
		};

		if (!allowed) {
			showNoPermissionActionbar(player);
		}
		return allowed;
	}

	private static TerritoryPoint findTerritoryContaining(ResourceLocation dimensionId, BlockPos pos) {
		for (List<TerritoryPoint> territories : PLAYER_TERRITORIES.values()) {
			for (TerritoryPoint territory : territories) {
				if (!territory.dimensionId().equals(dimensionId)) {
					continue;
				}
				if (territory.contains(pos.getX(), pos.getY(), pos.getZ())) {
					return territory;
				}
			}
		}
		return null;
	}

	private static boolean isInsideOtherTerritory(UUID ownerId, ResourceLocation dimensionId, BlockPos pos) {
		for (Map.Entry<UUID, List<TerritoryPoint>> entry : PLAYER_TERRITORIES.entrySet()) {
			if (entry.getKey().equals(ownerId)) {
				continue;
			}
			for (TerritoryPoint territory : entry.getValue()) {
				if (!territory.dimensionId().equals(dimensionId)) {
					continue;
				}
				if (territory.contains(pos.getX(), pos.getY(), pos.getZ())) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean overlapsOtherTerritory(UUID ownerId, ResourceLocation dimensionId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		for (Map.Entry<UUID, List<TerritoryPoint>> entry : PLAYER_TERRITORIES.entrySet()) {
			if (entry.getKey().equals(ownerId)) {
				continue;
			}
			for (TerritoryPoint territory : entry.getValue()) {
				if (!territory.dimensionId().equals(dimensionId)) {
					continue;
				}
				boolean overlapX = maxX >= territory.minX() && minX <= territory.maxX();
				boolean overlapY = maxY >= territory.minY() && minY <= territory.maxY();
				boolean overlapZ = maxZ >= territory.minZ() && minZ <= territory.maxZ();
				if (overlapX && overlapY && overlapZ) {
					return true;
				}
			}
		}
		return false;
	}

	private static void showNoPermissionActionbar(ServerPlayer player) {
		long last = LAST_DENY_ACTIONBAR_TICK.getOrDefault(player.getUUID(), -100L);
		if (SERVER_TICK - last < 10L) {
			return;
		}
		LAST_DENY_ACTIONBAR_TICK.put(player.getUUID(), SERVER_TICK);
		sendActionBar(player, ChatFormatting.RED, "message.teleportmenu.no_permission_action");
	}

	private static TerritoryPoint findOwnedTerritory(ServerPlayer owner, UUID territoryId) {
		if (territoryId == null) {
			return null;
		}
		return PLAYER_TERRITORIES
			.getOrDefault(owner.getUUID(), List.of())
			.stream()
			.filter(candidate -> candidate.id().equals(territoryId))
			.findFirst()
			.orElse(null);
	}

	private static void applyTerritoryPermission(TerritoryPoint territory, TerritoryPermission permission, boolean allowed) {
		switch (permission) {
			case BUILD_BREAK -> territory.setBuildBreakAllowed(allowed);
			case CONTAINER -> territory.setContainerAllowed(allowed);
			case INTERACT -> territory.setInteractAllowed(allowed);
			case DAMAGE_ENTITIES -> territory.setDamageEntitiesAllowed(allowed);
			case BLOCK_USE -> territory.setBlockUseAllowed(allowed);
		}
	}

	public static List<TeleportMenuPayloads.TerritoryTarget> getTerritoryTargets(ServerPlayer player) {
		return PLAYER_TERRITORIES
			.getOrDefault(player.getUUID(), List.of())
			.stream()
			.map(territory -> new TeleportMenuPayloads.TerritoryTarget(
				territory.id(),
				territory.name(),
				territory.ownerName(),
				territory.minX(),
				territory.minY(),
				territory.minZ(),
				territory.maxX(),
				territory.maxY(),
				territory.maxZ(),
				territory.iconItemId(),
				territory.buildBreakAllowed(),
				territory.containerAllowed(),
				territory.interactAllowed(),
				territory.damageEntitiesAllowed(),
				territory.blockUseAllowed(),
				List.copyOf(territory.whitelistPlayerIds())
			))
			.toList();
	}
}
