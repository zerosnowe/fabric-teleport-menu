package com.example.server.command;

import com.example.server.model.BoxShopPoint;
import com.example.server.model.ShopPoint;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.UUID;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerState.BOX_SHOPS;
import static com.example.server.ServerState.BOX_SHOP_SHOP_IDS;
import static com.example.server.ServerState.PLAYER_SHOPS;
import static com.example.server.service.BoxShopService.DEFAULT_BOX_SHOP_NAME;
import static com.example.server.service.BoxShopService.findBoxShopByBlock;
import static com.example.server.service.BoxShopService.findOwnedBoxShopAtFeet;
import static com.example.server.service.BoxShopService.refreshBoxShopHologram;
import static com.example.server.service.BoxShopService.removeBoxShop;
import static com.example.server.service.PersistenceService.persistDataIfPossible;
import static net.minecraft.commands.Commands.literal;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Registers the /clock, /territ and /boxshop commands.
 */
public final class CommandRegistrar {
	private CommandRegistrar() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("clock").executes(context -> giveClock(context.getSource())));
			dispatcher.register(literal("territ")
				.executes(context -> com.example.server.service.TerritoryService.showTerritoryUsage(context.getSource()))
				.then(literal("a").executes(context -> com.example.server.service.TerritoryService.setTerritoryPoint(context.getSource(), true)))
				.then(literal("b").executes(context -> com.example.server.service.TerritoryService.setTerritoryPoint(context.getSource(), false))));
			dispatcher.register(literal("boxshop")
				.then(literal("add").executes(context -> addBoxShop(context.getSource())))
				.then(literal("clean").executes(context -> cleanBoxShop(context.getSource()))));
		});
	}

	private static int giveClock(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (player.getInventory().contains(new ItemStack(Items.CLOCK))) {
			sendError(source, "message.teleportmenu.clock_already_have");
			return 0;
		}

		ItemStack clock = new ItemStack(Items.CLOCK);
		if (!player.getInventory().add(clock)) {
			player.drop(clock, false);
		}

		sendSuccess(source, "message.teleportmenu.clock_given");
		return 1;
	}

	private static int addBoxShop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = (ServerLevel) player.level();
		BlockPos boxPos = player.blockPosition();
		ResourceLocation dimensionId = level.dimension().location();

		if (findBoxShopByBlock(dimensionId, boxPos) != null) {
			sendError(source, "message.teleportmenu.boxshop_place_occupied");
			return 0;
		}

		UUID shopId = UUID.randomUUID();
		ShopPoint shop = new ShopPoint(
			shopId,
			player.getUUID(),
			player.getName().getString(),
			DEFAULT_BOX_SHOP_NAME,
			"minecraft:ender_chest",
			new ArrayList<>()
		);
		PLAYER_SHOPS.computeIfAbsent(player.getUUID(), key -> new ArrayList<>()).add(shop);
		BOX_SHOP_SHOP_IDS.add(shopId);
		level.setBlockAndUpdate(boxPos, Blocks.ENDER_CHEST.defaultBlockState());

		UUID boxShopId = UUID.randomUUID();
		BOX_SHOPS.put(boxShopId, new BoxShopPoint(
			boxShopId,
			shopId,
			player.getUUID(),
			player.getName().getString(),
			DEFAULT_BOX_SHOP_NAME,
			dimensionId,
			boxPos.getX(),
			boxPos.getY(),
			boxPos.getZ(),
			false
		));
		refreshBoxShopHologram(player.server, BOX_SHOPS.get(boxShopId));

		persistDataIfPossible();
		sendSuccess(source, "message.teleportmenu.boxshop_created");
		return 1;
	}

	private static int cleanBoxShop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		BoxShopPoint target = findOwnedBoxShopAtFeet(player);
		if (target == null) {
			sendError(source, "message.teleportmenu.boxshop_not_found");
			return 0;
		}

		removeBoxShop(player.server, target);
		sendSuccess(source, "message.teleportmenu.boxshop_cleaned");
		return 1;
	}
}
