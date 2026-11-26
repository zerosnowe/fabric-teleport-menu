package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import com.example.server.ServerState.TerritorySelection;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerPlayers.isAdmin;
import static com.example.server.ServerState.PLAYER_UI_PROTOCOLS;
import static com.example.server.ServerState.TERRITORY_SELECTIONS;
import static com.example.server.service.DisabledItemService.getDisabledItemIdList;
import static com.example.server.service.EconomyService.syncEconomyState;
import static com.example.server.service.HomeService.getHomeTargets;
import static com.example.server.service.TerritoryService.getTerritoryTargets;

/**
 * Clock-triggered menu opening and direct player-to-player teleport.
 */
public final class MenuService {
	public static final int STABLE_UI_PROTOCOL = 1;
	public static final int UNKNOWN_UI_PROTOCOL = 0;

	private MenuService() {
	}

	public static InteractionResultHolder<ItemStack> onClockUse(Player player, Level level, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (!heldItem.is(Items.CLOCK)) {
			return InteractionResultHolder.pass(heldItem);
		}

		if (level.isClientSide) {
			return InteractionResultHolder.success(heldItem);
		}

		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.pass(heldItem);
		}

		if (!ServerPlayNetworking.canSend(serverPlayer, TeleportMenuPayloads.OpenMenuPayload.TYPE)) {
			sendError(serverPlayer, "message.teleportmenu.client_missing");
			return InteractionResultHolder.consume(heldItem);
		}
		int uiProtocol = PLAYER_UI_PROTOCOLS.getOrDefault(serverPlayer.getUUID(), UNKNOWN_UI_PROTOCOL);
		if (uiProtocol < STABLE_UI_PROTOCOL) {
			sendError(serverPlayer, "message.teleportmenu.client_outdated");
			return InteractionResultHolder.consume(heldItem);
		}

		List<TeleportMenuPayloads.PlayerTarget> targets = serverPlayer.server
			.getPlayerList()
			.getPlayers()
			.stream()
			.filter(other -> !other.getUUID().equals(serverPlayer.getUUID()))
			.map(other -> new TeleportMenuPayloads.PlayerTarget(other.getUUID(), other.getName().getString()))
			.toList();

		TerritorySelection selection = TERRITORY_SELECTIONS.get(serverPlayer.getUUID());
		boolean selectionReady = selection != null && selection.pointA() != null && selection.pointB() != null
			&& selection.dimensionId().equals(serverPlayer.level().dimension().location());

		BlockPos a = selectionReady ? selection.pointA() : BlockPos.ZERO;
		BlockPos b = selectionReady ? selection.pointB() : BlockPos.ZERO;

		boolean admin = isAdmin(serverPlayer);
		LocalizationService.syncToPlayer(serverPlayer);
		syncEconomyState(serverPlayer);
		ServerPlayNetworking.send(serverPlayer, new TeleportMenuPayloads.OpenMenuPayload(
			targets,
			getHomeTargets(serverPlayer),
			getTerritoryTargets(serverPlayer),
			admin,
			admin ? getDisabledItemIdList() : List.of(),
			selectionReady,
			a.getX(),
			a.getY(),
			a.getZ(),
			b.getX(),
			b.getY(),
			b.getZ()
		));

		return InteractionResultHolder.consume(heldItem);
	}

	public static void handleTeleportRequest(ServerPlayer source, UUID targetId) {
		if (targetId == null || targetId.equals(source.getUUID())) {
			sendError(source, "message.teleportmenu.invalid_target");
			return;
		}

		ServerPlayer target = source.server.getPlayerList().getPlayer(targetId);
		if (target == null) {
			sendError(source, "message.teleportmenu.target_offline");
			return;
		}

		source.teleportTo((ServerLevel)target.level(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
		sendSuccess(source, "message.teleportmenu.teleport_success_player", target.getDisplayName());
	}
}
