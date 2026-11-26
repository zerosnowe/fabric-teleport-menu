package com.example.server.network;

import com.example.network.TeleportMenuPayloads;
import com.example.server.service.BoxShopService;
import com.example.server.service.DisabledItemService;
import com.example.server.service.EconomyService;
import com.example.server.service.HomeService;
import com.example.server.service.LocalizationService;
import com.example.server.service.MenuService;
import com.example.server.service.MomentService;
import com.example.server.service.ShopService;
import com.example.server.service.TerritoryService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import static com.example.server.ServerState.PLAYER_UI_PROTOCOLS;

/**
 * Declares payload types and wires every client-to-server receiver.
 */
public final class ServerNetworkRegistrar {
	private ServerNetworkRegistrar() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.CloseMenuPayload.TYPE, TeleportMenuPayloads.CloseMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.OpenMenuPayload.TYPE, TeleportMenuPayloads.OpenMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.HomeListPayload.TYPE, TeleportMenuPayloads.HomeListPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.DisabledItemListPayload.TYPE, TeleportMenuPayloads.DisabledItemListPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.EconomyStatePayload.TYPE, TeleportMenuPayloads.EconomyStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.OpenBoxShopPayload.TYPE, TeleportMenuPayloads.OpenBoxShopPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.BoxShopHudPayload.TYPE, TeleportMenuPayloads.BoxShopHudPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportMenuPayloads.MomentFeedPayload.TYPE, TeleportMenuPayloads.MomentFeedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.TeleportRequestPayload.TYPE, TeleportMenuPayloads.TeleportRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.TeleportHomeRequestPayload.TYPE, TeleportMenuPayloads.TeleportHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.TeleportTerritoryRequestPayload.TYPE, TeleportMenuPayloads.TeleportTerritoryRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CreateHomeRequestPayload.TYPE, TeleportMenuPayloads.CreateHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CreateTerritoryRequestPayload.TYPE, TeleportMenuPayloads.CreateTerritoryRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CancelTerritoryCreatePayload.TYPE, TeleportMenuPayloads.CancelTerritoryCreatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.UpdateTerritoryPermissionPayload.TYPE, TeleportMenuPayloads.UpdateTerritoryPermissionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.UpdateTerritoryWhitelistPayload.TYPE, TeleportMenuPayloads.UpdateTerritoryWhitelistPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.ClientHelloPayload.TYPE, TeleportMenuPayloads.ClientHelloPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DeleteHomeRequestPayload.TYPE, TeleportMenuPayloads.DeleteHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RenameHomeRequestPayload.TYPE, TeleportMenuPayloads.RenameHomeRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DisableItemRequestPayload.TYPE, TeleportMenuPayloads.DisableItemRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.EconomyAdminActionPayload.TYPE, TeleportMenuPayloads.EconomyAdminActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.EconomySidebarVisibilityPayload.TYPE, TeleportMenuPayloads.EconomySidebarVisibilityPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.CreateShopRequestPayload.TYPE, TeleportMenuPayloads.CreateShopRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.ExpandShopLimitRequestPayload.TYPE, TeleportMenuPayloads.ExpandShopLimitRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DeleteShopRequestPayload.TYPE, TeleportMenuPayloads.DeleteShopRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RenameShopRequestPayload.TYPE, TeleportMenuPayloads.RenameShopRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.SetShopIconRequestPayload.TYPE, TeleportMenuPayloads.SetShopIconRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.AddShopGoodRequestPayload.TYPE, TeleportMenuPayloads.AddShopGoodRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.BuyShopGoodRequestPayload.TYPE, TeleportMenuPayloads.BuyShopGoodRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RenameBoxShopPayload.TYPE, TeleportMenuPayloads.RenameBoxShopPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.QueryBoxShopHudPayload.TYPE, TeleportMenuPayloads.QueryBoxShopHudPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.RequestMomentFeedPayload.TYPE, TeleportMenuPayloads.RequestMomentFeedPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.PostMomentPayload.TYPE, TeleportMenuPayloads.PostMomentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.LikeMomentPayload.TYPE, TeleportMenuPayloads.LikeMomentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.DeleteMomentPayload.TYPE, TeleportMenuPayloads.DeleteMomentPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportMenuPayloads.SaveMomentDraftPayload.TYPE, TeleportMenuPayloads.SaveMomentDraftPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.TeleportRequestPayload.TYPE, (payload, context) -> MenuService.handleTeleportRequest(context.player(), payload.targetId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.TeleportHomeRequestPayload.TYPE, (payload, context) -> HomeService.handleTeleportHomeRequest(context.player(), payload.homeId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.TeleportTerritoryRequestPayload.TYPE, (payload, context) -> TerritoryService.handleTeleportTerritoryRequest(context.player(), payload.territoryId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CreateHomeRequestPayload.TYPE, (payload, context) -> HomeService.handleCreateHomeRequest(context.player(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CreateTerritoryRequestPayload.TYPE, (payload, context) -> TerritoryService.handleCreateTerritoryRequest(context.player(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CancelTerritoryCreatePayload.TYPE, (payload, context) -> TerritoryService.handleCancelTerritoryCreate(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.UpdateTerritoryPermissionPayload.TYPE, (payload, context) -> TerritoryService.handleUpdateTerritoryPermission(context.player(), payload.territoryId(), payload.permissionKey(), payload.allowed()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.UpdateTerritoryWhitelistPayload.TYPE, (payload, context) -> TerritoryService.handleUpdateTerritoryWhitelist(context.player(), payload.territoryId(), payload.playerId(), payload.allowed()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.ClientHelloPayload.TYPE, (payload, context) -> {
			PLAYER_UI_PROTOCOLS.put(context.player().getUUID(), payload.protocol());
			LocalizationService.syncToPlayer(context.player());
		});
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DeleteHomeRequestPayload.TYPE, (payload, context) -> HomeService.handleDeleteHomeRequest(context.player(), payload.homeId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RenameHomeRequestPayload.TYPE, (payload, context) -> HomeService.handleRenameHomeRequest(context.player(), payload.homeId(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DisableItemRequestPayload.TYPE, (payload, context) -> DisabledItemService.handleDisableItemRequest(context.player(), payload.itemId(), payload.disable()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.EconomyAdminActionPayload.TYPE, (payload, context) ->
			EconomyService.handleEconomyAdminAction(context.player(), payload.targetId(), payload.action(), payload.amount()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.EconomySidebarVisibilityPayload.TYPE, (payload, context) ->
			EconomyService.setEconomySidebarVisible(context.player().server, context.player().getUUID(), !payload.hidden()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.CreateShopRequestPayload.TYPE, (payload, context) -> ShopService.handleCreateShop(context.player(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.ExpandShopLimitRequestPayload.TYPE, (payload, context) -> ShopService.handleExpandShopLimit(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DeleteShopRequestPayload.TYPE, (payload, context) -> ShopService.handleDeleteShop(context.player(), payload.shopId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RenameShopRequestPayload.TYPE, (payload, context) -> ShopService.handleRenameShop(context.player(), payload.shopId(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.SetShopIconRequestPayload.TYPE, (payload, context) -> ShopService.handleSetShopIcon(context.player(), payload.shopId(), payload.iconItemId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.AddShopGoodRequestPayload.TYPE, (payload, context) -> ShopService.handleAddShopGood(context.player(), payload.shopId(), payload.itemId(), payload.stock(), payload.price()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.BuyShopGoodRequestPayload.TYPE, (payload, context) -> ShopService.handleBuyShopGood(context.player(), payload.shopId(), payload.itemId(), payload.amount()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RenameBoxShopPayload.TYPE, (payload, context) -> BoxShopService.handleRenameBoxShop(context.player(), payload.boxShopId(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.QueryBoxShopHudPayload.TYPE, (payload, context) ->
			BoxShopService.handleQueryBoxShopHud(context.player(), payload.x(), payload.y(), payload.z()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.RequestMomentFeedPayload.TYPE, (payload, context) ->
			MomentService.handleRequestMomentFeed(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.PostMomentPayload.TYPE, (payload, context) ->
			MomentService.handlePostMoment(context.player(), payload.content()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.LikeMomentPayload.TYPE, (payload, context) ->
			MomentService.handleLikeMoment(context.player(), payload.momentId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.DeleteMomentPayload.TYPE, (payload, context) ->
			MomentService.handleDeleteMoment(context.player(), payload.momentId()));
		ServerPlayNetworking.registerGlobalReceiver(TeleportMenuPayloads.SaveMomentDraftPayload.TYPE, (payload, context) ->
			MomentService.handleSaveMomentDraft(context.player(), payload.content()));

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (!(entity instanceof Monster)) {
				return;
			}
			if (damageSource.getEntity() instanceof ServerPlayer killer) {
				EconomyService.rewardMonsterKill(killer);
			}
		});
	}
}
