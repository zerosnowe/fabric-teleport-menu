package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;
import java.util.List;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerPlayers.isAdmin;
import static com.example.server.ServerState.DISABLED_ITEM_IDS;
import static com.example.server.service.PersistenceService.persistDataIfPossible;

/**
 * Handles the admin-configurable item blacklist and keeps inventories/recipes clean.
 */
public final class DisabledItemService {
	private DisabledItemService() {
	}

	public static InteractionResultHolder<ItemStack> onAnyItemUse(Player player, Level level, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.isEmpty() || !isDisabledItem(heldItem.getItem())) {
			return InteractionResultHolder.pass(heldItem);
		}

		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			replaceDisabledInInventory(serverPlayer);
			sendError(serverPlayer, "message.teleportmenu.item_disabled_use_blocked");
		}

		return InteractionResultHolder.fail(player.getItemInHand(hand));
	}

	public static void handleDisableItemRequest(ServerPlayer source, String rawItemId, boolean disable) {
		if (!isAdmin(source)) {
			sendError(source, "message.teleportmenu.admin_no_permission");
			return;
		}

		String itemId = rawItemId == null ? "" : rawItemId.trim();
		if (itemId.isEmpty()) {
			sendError(source, "message.teleportmenu.item_disable_not_found");
			return;
		}

		ResourceLocation id;
		try {
			id = ResourceLocation.parse(itemId);
		} catch (IllegalArgumentException error) {
			sendError(source, "message.teleportmenu.item_disable_not_found");
			return;
		}

		Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		if (item == null || item == Items.AIR || item == Items.GRASS_BLOCK) {
			sendError(source, "message.teleportmenu.item_disable_not_found");
			return;
		}

		String canonicalId = BuiltInRegistries.ITEM.getKey(item).toString();
		if (disable) {
			if (DISABLED_ITEM_IDS.contains(canonicalId)) {
				sendError(source, "message.teleportmenu.item_disable_already", canonicalId);
				return;
			}

			DISABLED_ITEM_IDS.add(canonicalId);
			persistDataIfPossible();
			disableRecipesForItem(source.server, item);
			replaceDisabledForAllPlayers(source.server);
			syncDisabledItemsToOperators(source.server);
			sendSuccess(source, "message.teleportmenu.item_disable_success", canonicalId);
		} else {
			if (!DISABLED_ITEM_IDS.contains(canonicalId)) {
				sendError(source, "message.teleportmenu.item_enable_already", canonicalId);
				return;
			}

			DISABLED_ITEM_IDS.remove(canonicalId);
			persistDataIfPossible();
			syncDisabledItemsToOperators(source.server);
			sendSuccess(source, "message.teleportmenu.item_enable_success", canonicalId);
		}
	}

	public static void replaceDisabledForAllPlayers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			replaceDisabledInInventory(player);
		}
	}

	public static void replaceDisabledInInventory(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		boolean changed = false;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !isDisabledItem(stack.getItem())) {
				continue;
			}

			int replacementCount = Math.max(1, Math.min(64, stack.getCount()));
			inventory.setItem(slot, new ItemStack(Blocks.GRASS_BLOCK, replacementCount));
			changed = true;
		}

		AbstractContainerMenu menu = player.containerMenu;
		if (menu != null) {
			ItemStack carried = menu.getCarried();
			if (!carried.isEmpty() && isDisabledItem(carried.getItem())) {
				menu.setCarried(new ItemStack(Blocks.GRASS_BLOCK, Math.max(1, Math.min(64, carried.getCount()))));
				changed = true;
			}
		}

		if (changed) {
			inventory.setChanged();
			if (menu != null) {
				menu.broadcastChanges();
			}
		}
	}

	private static void disableRecipesForItem(MinecraftServer server, Item disabledItem) {
		RegistryAccess registryAccess = server.registryAccess();
		List<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes().stream()
			.filter(recipe -> recipe.value().getResultItem(registryAccess).is(disabledItem))
			.toList();

		if (recipes.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.getRecipeBook().removeRecipes(recipes, player);
		}
	}

	public static void syncDisabledItemsToOperators(MinecraftServer server) {
		List<String> disabledIds = getDisabledItemIdList();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isAdmin(player) || !ServerPlayNetworking.canSend(player, TeleportMenuPayloads.DisabledItemListPayload.TYPE)) {
				continue;
			}
			ServerPlayNetworking.send(player, new TeleportMenuPayloads.DisabledItemListPayload(disabledIds));
		}
	}

	public static boolean isDisabledItem(Item item) {
		if (item == Items.AIR || item == Items.GRASS_BLOCK) {
			return false;
		}
		return DISABLED_ITEM_IDS.contains(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	public static List<String> getDisabledItemIdList() {
		return DISABLED_ITEM_IDS.stream().sorted(Comparator.naturalOrder()).toList();
	}
}
