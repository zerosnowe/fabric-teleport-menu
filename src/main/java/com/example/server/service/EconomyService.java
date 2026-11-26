package com.example.server.service;

import com.example.network.TeleportMenuPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.server.ServerLog.LOGGER;
import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendSuccess;
import static com.example.server.ServerPlayers.isAdmin;
import static com.example.server.ServerState.ECONOMY_SIDEBAR_ASSERTED;
import static com.example.server.ServerState.ECONOMY_SIDEBAR_HIDDEN;
import static com.example.server.ServerState.ECONOMY_SIDEBAR_HIDDEN_SENT;
import static com.example.server.ServerState.ECONOMY_SIDEBAR_LAST_SCORE;
import static com.example.server.ServerState.PLAYER_EQUIPMENT_COUNTS;
import static com.example.server.ServerState.PLAYER_REWARDED_EQUIPMENT;
import static com.example.server.ServerState.PLAYER_SHOP_LIMITS;
import static com.example.server.service.PersistenceService.persistDataIfPossible;
import static com.example.server.service.ShopService.DEFAULT_SHOP_LIMIT;
import static com.example.server.service.ShopService.countOwnedNormalShops;
import static com.example.server.service.ShopService.getAllShopTargets;

/**
 * Bridges the optional external "economy" mod, awards rewards and drives the sidebar.
 */
public final class EconomyService {
	private static final boolean ECONOMY_MOD_LOADED = FabricLoader.getInstance().isModLoaded("economy");
	private static final double ECONOMY_MONSTER_KILL_REWARD = 5D;
	private static final double ECONOMY_EQUIPMENT_REWARD = 3D;
	private static final double ECONOMY_MINING_COAL_REWARD = 5D;
	private static final double ECONOMY_MINING_IRON_COPPER_REWARD = 8D;
	private static final double ECONOMY_MINING_REDSTONE_LAPIS_REWARD = 10D;
	private static final double ECONOMY_MINING_DIAMOND_EMERALD_REWARD = 15D;
	private static final double ECONOMY_SPECIAL_MINING_REWARD = 20D;
	private static final String ECONOMY_SIDEBAR_ENTRY = "\u25C6 \u4F59\u989D";

	private static boolean ECONOMY_API_INITIALIZED = false;
	private static boolean ECONOMY_API_READY = false;
	private static Method ECONOMY_GET_MONEY = null;
	private static Method ECONOMY_ADD = null;
	private static Method ECONOMY_SUBTRACT = null;
	private static Method ECONOMY_SET = null;
	private static Method ECONOMY_RESET = null;
	private static Method ECONOMY_FORMAT = null;

	private EconomyService() {
	}

	public static boolean isEconomyReady() {
		if (!ECONOMY_MOD_LOADED) {
			return false;
		}

		if (ECONOMY_API_INITIALIZED) {
			return ECONOMY_API_READY;
		}
		ECONOMY_API_INITIALIZED = true;

		try {
			Class<?> apiClass = Class.forName("me.isaiah.mods.economy.api.Economy");
			ECONOMY_GET_MONEY = apiClass.getMethod("getMoney", String.class);
			ECONOMY_ADD = apiClass.getMethod("add", String.class, double.class);
			ECONOMY_SUBTRACT = apiClass.getMethod("subtract", String.class, double.class);
			ECONOMY_SET = apiClass.getMethod("setMoney", String.class, double.class);
			ECONOMY_RESET = apiClass.getMethod("resetBalance", String.class);
			ECONOMY_FORMAT = apiClass.getMethod("format", double.class);
			ECONOMY_API_READY = true;
		} catch (Exception error) {
			ECONOMY_API_READY = false;
			LOGGER.warn("Economy mod is installed but API binding failed", error);
		}

		return ECONOMY_API_READY;
	}

	public static double economyGetMoney(String playerName) throws InvocationTargetException, IllegalAccessException {
		return (double) ECONOMY_GET_MONEY.invoke(null, playerName);
	}

	public static void economyAdd(String playerName, double amount) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_ADD.invoke(null, playerName, amount);
	}

	public static void economySubtract(String playerName, double amount) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_SUBTRACT.invoke(null, playerName, amount);
	}

	public static void economySet(String playerName, double amount) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_SET.invoke(null, playerName, amount);
	}

	public static void economyReset(String playerName) throws InvocationTargetException, IllegalAccessException {
		ECONOMY_RESET.invoke(null, playerName);
	}

	public static String economyFormat(double amount) throws InvocationTargetException, IllegalAccessException {
		Object formatted = ECONOMY_FORMAT.invoke(null, amount);
		return formatted == null ? String.format(Locale.ROOT, "%.2f", amount) : formatted.toString();
	}

	public static boolean isValidMoneyAmount(double amount) {
		return Double.isFinite(amount) && amount >= 0D;
	}

	public static void handleEconomyAdminAction(ServerPlayer source, UUID targetId, String rawAction, double amount) {
		if (!isAdmin(source)) {
			sendError(source, "message.teleportmenu.admin_no_permission");
			return;
		}

		if (!isEconomyReady()) {
			sendError(source, "message.teleportmenu.economy_missing");
			return;
		}

		if (targetId == null) {
			sendError(source, "message.teleportmenu.economy_player_offline");
			return;
		}

		ServerPlayer target = source.server.getPlayerList().getPlayer(targetId);
		if (target == null) {
			sendError(source, "message.teleportmenu.economy_player_offline");
			return;
		}

		String targetName = target.getName().getString();
		String action = rawAction == null ? "" : rawAction.trim().toLowerCase(Locale.ROOT);
		double safeAmount = Math.max(0D, amount);

		try {
			switch (action) {
				case "query" -> sendSuccess(source, "message.teleportmenu.economy_balance", targetName, economyFormat(economyGetMoney(targetName)));
				case "add" -> {
					if (!isValidMoneyAmount(safeAmount)) {
						sendError(source, "message.teleportmenu.economy_invalid_amount");
						return;
					}
					economyAdd(targetName, safeAmount);
					sendSuccess(source, "message.teleportmenu.economy_add_success", targetName, economyFormat(safeAmount));
					sendSuccess(target, "message.teleportmenu.economy_add_success", targetName, economyFormat(safeAmount));
				}
				case "subtract" -> {
					if (!isValidMoneyAmount(safeAmount)) {
						sendError(source, "message.teleportmenu.economy_invalid_amount");
						return;
					}
					economySubtract(targetName, safeAmount);
					sendSuccess(source, "message.teleportmenu.economy_subtract_success", targetName, economyFormat(safeAmount));
					sendSuccess(target, "message.teleportmenu.economy_subtract_success", targetName, economyFormat(safeAmount));
				}
				case "set" -> {
					if (!isValidMoneyAmount(safeAmount)) {
						sendError(source, "message.teleportmenu.economy_invalid_amount");
						return;
					}
					economySet(targetName, safeAmount);
					sendSuccess(source, "message.teleportmenu.economy_set_success", targetName, economyFormat(safeAmount));
					sendSuccess(target, "message.teleportmenu.economy_set_success", targetName, economyFormat(safeAmount));
				}
				case "reset" -> {
					economyReset(targetName);
					sendSuccess(source, "message.teleportmenu.economy_reset_success", targetName);
					sendSuccess(target, "message.teleportmenu.economy_reset_success", targetName);
				}
				default -> sendError(source, "message.teleportmenu.economy_invalid_amount");
			}
		} catch (Exception error) {
			LOGGER.error("Economy operation failed: action={}, player={}", action, targetName, error);
			sendError(source, "message.teleportmenu.economy_operation_failed");
		}
	}

	public static void rewardMonsterKill(ServerPlayer killer) {
		if (!isEconomyReady()) {
			return;
		}

		try {
			economyAdd(killer.getName().getString(), ECONOMY_MONSTER_KILL_REWARD);
			sendSuccess(killer, "message.teleportmenu.economy_reward_kill", economyFormat(ECONOMY_MONSTER_KILL_REWARD));
		} catch (Exception error) {
			LOGGER.debug("Failed to award monster kill economy reward", error);
		}
	}

	public static void rewardMining(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state) {
		if (!isEconomyReady() || state == null) {
			return;
		}

		if (hasSilkTouch(player)) {
			return;
		}

		double reward = miningRewardFor(state.getBlock());
		if (reward <= 0D) {
			return;
		}

		try {
			economyAdd(player.getName().getString(), reward);
			sendSuccess(player, "message.teleportmenu.economy_reward_mining", economyFormat(reward));
		} catch (Exception error) {
			LOGGER.debug("Failed to award mining economy reward", error);
		}
	}

	private static boolean hasSilkTouch(ServerPlayer player) {
		if (player == null) {
			return false;
		}
		ItemStack mainHand = player.getMainHandItem();
		if (mainHand.isEmpty()) {
			return false;
		}

		try {
			Holder<Enchantment> silkTouch = player.server.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SILK_TOUCH);
			return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, mainHand) > 0;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static double miningRewardFor(Block block) {
		if (block == null) {
			return 0D;
		}

		if (block == Blocks.ANCIENT_DEBRIS || block.builtInRegistryHolder().is(BlockTags.GOLD_ORES)) {
			return ECONOMY_SPECIAL_MINING_REWARD;
		}

		if (block.builtInRegistryHolder().is(BlockTags.DIAMOND_ORES) || block.builtInRegistryHolder().is(BlockTags.EMERALD_ORES)) {
			return ECONOMY_MINING_DIAMOND_EMERALD_REWARD;
		}
		if (block.builtInRegistryHolder().is(BlockTags.REDSTONE_ORES) || block.builtInRegistryHolder().is(BlockTags.LAPIS_ORES)) {
			return ECONOMY_MINING_REDSTONE_LAPIS_REWARD;
		}
		if (block.builtInRegistryHolder().is(BlockTags.IRON_ORES) || block.builtInRegistryHolder().is(BlockTags.COPPER_ORES)) {
			return ECONOMY_MINING_IRON_COPPER_REWARD;
		}
		if (block.builtInRegistryHolder().is(BlockTags.COAL_ORES)) {
			return ECONOMY_MINING_COAL_REWARD;
		}

		return 0D;
	}

	public static void rewardNewEquipmentInInventory(ServerPlayer player) {
		if (!isEconomyReady()) {
			return;
		}

		Map<String, Integer> current = snapshotRewardableEquipment(player);
		Map<String, Integer> previous = PLAYER_EQUIPMENT_COUNTS.put(player.getUUID(), current);
		if (previous == null) {
			return;
		}

		Set<String> rewarded = PLAYER_REWARDED_EQUIPMENT.computeIfAbsent(player.getUUID(), key -> ConcurrentHashMap.newKeySet());
		int newlyRewardedTypes = 0;
		boolean changed = false;
		for (Map.Entry<String, Integer> entry : current.entrySet()) {
			int oldValue = previous.getOrDefault(entry.getKey(), 0);
			int delta = entry.getValue() - oldValue;
			if (delta > 0 && !rewarded.contains(entry.getKey())) {
				rewarded.add(entry.getKey());
				newlyRewardedTypes++;
				changed = true;
			}
		}

		if (newlyRewardedTypes <= 0) {
			return;
		}

		double reward = ECONOMY_EQUIPMENT_REWARD * newlyRewardedTypes;
		try {
			economyAdd(player.getName().getString(), reward);
			if (changed) {
				persistDataIfPossible();
			}
			sendSuccess(player, "message.teleportmenu.economy_reward_equipment", newlyRewardedTypes, economyFormat(reward));
		} catch (Exception error) {
			LOGGER.debug("Failed to award equipment economy reward", error);
		}
	}

	private static Map<String, Integer> snapshotRewardableEquipment(ServerPlayer player) {
		Map<String, Integer> snapshot = new HashMap<>();
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty() || !isRewardableEquipment(stack.getItem())) {
				continue;
			}
			String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			snapshot.merge(id, stack.getCount(), Integer::sum);
		}
		return snapshot;
	}

	private static boolean isRewardableEquipment(Item item) {
		return item instanceof ArmorItem
			|| item instanceof SwordItem
			|| item instanceof AxeItem
			|| item instanceof TieredItem
			|| item instanceof BowItem
			|| item instanceof CrossbowItem
			|| item instanceof TridentItem
			|| item instanceof ShieldItem;
	}

	public static void refreshEconomySidebar(MinecraftServer server) {
		if (!isEconomyReady()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			applyEconomySidebarState(server, player.getUUID());
		}
	}

	public static void setEconomySidebarVisible(MinecraftServer server, UUID playerId, boolean visible) {
		if (server == null || playerId == null) {
			return;
		}

		runEconomySidebarOnServerThread(server, () -> {
			if (visible) {
				ECONOMY_SIDEBAR_HIDDEN.remove(playerId);
			} else {
				ECONOMY_SIDEBAR_HIDDEN.add(playerId);
				ECONOMY_SIDEBAR_LAST_SCORE.remove(playerId);
				ECONOMY_SIDEBAR_ASSERTED.remove(playerId);
				ECONOMY_SIDEBAR_HIDDEN_SENT.remove(playerId);
			}
			persistDataIfPossible();
			applyEconomySidebarState(server, playerId);
		});
	}

	public static void onPlayerJoin(MinecraftServer server, UUID playerId) {
		runEconomySidebarOnServerThread(server, () -> applyEconomySidebarState(server, playerId));
	}

	public static void onPlayerDisconnect(MinecraftServer server, UUID playerId) {
		runEconomySidebarOnServerThread(server, () -> {
			ECONOMY_SIDEBAR_LAST_SCORE.remove(playerId);
			ECONOMY_SIDEBAR_ASSERTED.remove(playerId);
			ECONOMY_SIDEBAR_HIDDEN_SENT.remove(playerId);
		});
	}

	private static void runEconomySidebarOnServerThread(MinecraftServer server, Runnable operation) {
		if (server == null || operation == null) {
			return;
		}
		if (server.isSameThread()) {
			operation.run();
		} else {
			server.execute(operation);
		}
	}

	private static void applyEconomySidebarState(MinecraftServer server, UUID playerId) {
		if (server == null || playerId == null) {
			return;
		}
		if (!server.isSameThread()) {
			runEconomySidebarOnServerThread(server, () -> applyEconomySidebarState(server, playerId));
			return;
		}

		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player == null || player.isRemoved()) {
			return;
		}

		if (ECONOMY_SIDEBAR_HIDDEN.contains(playerId)) {
			if (ECONOMY_SIDEBAR_HIDDEN_SENT.add(playerId)) {
				player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, null));
			}
			return;
		}

		ECONOMY_SIDEBAR_HIDDEN_SENT.remove(playerId);
		try {
			Scoreboard scoreboard = server.getScoreboard();
			String objectiveName = economySidebarObjectiveName(playerId);
			Objective objective = scoreboard.getObjective(objectiveName);
			boolean created = objective == null;
			if (created) {
				objective = scoreboard.addObjective(
					objectiveName,
					ObjectiveCriteria.DUMMY,
					Component.literal("\u25C6 \u91D1\u5E01\u4F59\u989D"),
					ObjectiveCriteria.RenderType.INTEGER,
					false,
					null
				);
			}

			double money = economyGetMoney(player.getName().getString());
			int score = (int) Math.max(
				Integer.MIN_VALUE,
				Math.min(Integer.MAX_VALUE, Math.round(money))
			);
			Integer previous = ECONOMY_SIDEBAR_LAST_SCORE.put(playerId, score);

			if (created) {
				player.connection.send(new ClientboundSetObjectivePacket(
					objective,
					ClientboundSetObjectivePacket.METHOD_ADD
				));
			}

			if (created || previous == null || previous != score) {
				scoreboard.getOrCreatePlayerScore(
					ScoreHolder.forNameOnly(ECONOMY_SIDEBAR_ENTRY),
					objective
				).set(score);
				player.connection.send(new ClientboundSetScorePacket(
					ECONOMY_SIDEBAR_ENTRY,
					objective.getName(),
					score,
					Optional.empty(),
					Optional.empty()
				));
			}

			if (ECONOMY_SIDEBAR_ASSERTED.add(playerId)) {
				player.connection.send(new ClientboundSetDisplayObjectivePacket(
					DisplaySlot.SIDEBAR,
					objective
				));
			}
		} catch (Exception error) {
			LOGGER.debug("Failed to apply economy sidebar for {}", player.getName().getString(), error);
		}
	}

	private static String economySidebarObjectiveName(UUID playerId) {
		String compact = playerId.toString().replace("-", "");
		return "te2_" + compact.substring(0, 12);
	}

	public static void syncEconomyState(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TeleportMenuPayloads.EconomyStatePayload.TYPE)) {
			return;
		}
		ServerPlayNetworking.send(player, new TeleportMenuPayloads.EconomyStatePayload(
			getLeaderboardEntries(player.server),
			getAllShopTargets(),
			countOwnedNormalShops(player.getUUID()),
			PLAYER_SHOP_LIMITS.getOrDefault(player.getUUID(), DEFAULT_SHOP_LIMIT)
		));
	}

	private static List<TeleportMenuPayloads.EconomyLeaderboardEntry> getLeaderboardEntries(MinecraftServer server) {
		List<TeleportMenuPayloads.EconomyLeaderboardEntry> entries = new ArrayList<>();
		for (ServerPlayer online : server.getPlayerList().getPlayers()) {
			double balance = 0D;
			String formatted = "0";
			if (isEconomyReady()) {
				try {
					balance = economyGetMoney(online.getName().getString());
					formatted = economyFormat(balance);
				} catch (Exception ignored) {
				}
			}
			entries.add(new TeleportMenuPayloads.EconomyLeaderboardEntry(
				online.getUUID(),
				online.getName().getString(),
				balance,
				formatted
			));
		}
		entries.sort((a, b) -> Double.compare(b.balance(), a.balance()));
		return entries;
	}
}
