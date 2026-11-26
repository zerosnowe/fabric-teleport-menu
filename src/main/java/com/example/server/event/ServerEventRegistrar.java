package com.example.server.event;

import com.example.server.ServerState;
import com.example.server.service.BoxShopService;
import com.example.server.service.DisabledItemService;
import com.example.server.service.EconomyService;
import com.example.server.service.LocalizationService;
import com.example.server.service.MenuService;
import com.example.server.service.PersistenceService;
import com.example.server.service.ProtectionService;
import com.example.server.service.TerritoryService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static com.example.server.ServerLog.LOGGER;
import static com.example.server.ServerState.PLAYER_EQUIPMENT_COUNTS;
import static com.example.server.ServerState.PLAYER_REWARDED_EQUIPMENT;
import static com.example.server.ServerState.PLAYER_UI_PROTOCOLS;

/**
 * Wires all Fabric server-side event callbacks to the domain services.
 */
public final class ServerEventRegistrar {
	private ServerEventRegistrar() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register(ProtectionService::onBeforeBlockBreak);
		PlayerBlockBreakEvents.AFTER.register(ProtectionService::onAfterBlockBreak);
		UseBlockCallback.EVENT.register(ProtectionService::onUseBlock);
		AttackEntityCallback.EVENT.register(ProtectionService::onAttackEntity);
		UseItemCallback.EVENT.register(DisabledItemService::onAnyItemUse);
		UseItemCallback.EVENT.register(MenuService::onClockUse);

		ServerLifecycleEvents.SERVER_STARTED.register(ServerEventRegistrar::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(ServerEventRegistrar::onServerStopping);
		ServerChunkEvents.CHUNK_LOAD.register(BoxShopService::cleanupLegacyBoxShopDisplaysInChunk);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			LocalizationService.syncToPlayer(handler.player);
			EconomyService.onPlayerJoin(server, handler.player.getUUID());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.player.getUUID();
			PLAYER_UI_PROTOCOLS.remove(playerId);
			PLAYER_EQUIPMENT_COUNTS.remove(playerId);
			PLAYER_REWARDED_EQUIPMENT.remove(playerId);
			EconomyService.onPlayerDisconnect(server, playerId);
		});
		ServerTickEvents.END_SERVER_TICK.register(ServerEventRegistrar::onServerTick);
	}

	private static void onServerStarted(MinecraftServer server) {
		ServerState.ACTIVE_SERVER = server;
		LocalizationService.load();
		PersistenceService.loadPersistentData(server);
		BoxShopService.cleanupAllLegacyBoxShopDisplays(server);
		DisabledItemService.replaceDisabledForAllPlayers(server);
		EconomyService.refreshEconomySidebar(server);
	}

	private static void onServerStopping(MinecraftServer server) {
		PersistenceService.savePersistentData(server);
		PersistenceService.closeStorageQuietly();
		ServerState.clearRuntime();
		LocalizationService.clear();
	}

	private static void onServerTick(MinecraftServer server) {
		ServerState.SERVER_TICK++;
		if (ServerState.SERVER_TICK % 20L == 0L) {
			EconomyService.refreshEconomySidebar(server);
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			try {
				if (player == null || player.isRemoved() || player.level() == null) {
					continue;
				}

				DisabledItemService.replaceDisabledInInventory(player);
				EconomyService.rewardNewEquipmentInInventory(player);
				TerritoryService.updateTerritoryHud(player);
				if (ServerState.SERVER_TICK % 5L == 0L) {
					TerritoryService.showTerritorySelectionParticles(player);
				}
			} catch (Exception error) {
				LOGGER.debug("Skip ticking player during shutdown/transition: {}", player != null ? player.getName().getString() : "null", error);
			}
		}
	}
}
