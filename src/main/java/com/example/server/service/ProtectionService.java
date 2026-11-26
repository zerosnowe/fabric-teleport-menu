package com.example.server.service;

import com.example.server.model.TerritoryPermission;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import static com.example.server.ServerMessages.sendError;
import static com.example.server.ServerMessages.sendWarn;
import static com.example.server.service.BoxShopService.findBoxShopByBlock;
import static com.example.server.service.BoxShopService.openBoxShop;
import static com.example.server.service.EconomyService.rewardMining;
import static com.example.server.service.TerritoryService.checkTerritoryPermission;

/**
 * World protection hooks that enforce territory permissions and box shop interactions.
 */
public final class ProtectionService {
	private ProtectionService() {
	}

	public static boolean onBeforeBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}

		var boxShop = findBoxShopByBlock(level.dimension().location(), pos);
		if (boxShop != null) {
			if (boxShop.ownerId().equals(serverPlayer.getUUID())) {
				sendWarn(serverPlayer, "message.teleportmenu.boxshop_clean_hint");
			} else {
				sendError(serverPlayer, "message.teleportmenu.no_permission_action");
			}
			return false;
		}

		return checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.BUILD_BREAK);
	}

	public static void onAfterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		rewardMining(serverPlayer, state);
	}

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();
		var boxShop = findBoxShopByBlock(level.dimension().location(), pos);
		if (boxShop != null && hand == InteractionHand.MAIN_HAND) {
			openBoxShop(serverPlayer, boxShop);
			return InteractionResult.SUCCESS;
		}

		Block block = level.getBlockState(pos).getBlock();
		ItemStack held = player.getItemInHand(hand);

		if (held.getItem() instanceof BlockItem) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.BUILD_BREAK)) {
				return InteractionResult.FAIL;
			}
		}

		if (isContainerBlock(level, pos)) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.CONTAINER)) {
				return InteractionResult.FAIL;
			}
		}

		if (isRedstoneInteractBlock(block)) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.INTERACT)) {
				return InteractionResult.FAIL;
			}
		}

		if (isBlockUseControlled(block)) {
			if (!checkTerritoryPermission(serverPlayer, pos, TerritoryPermission.BLOCK_USE)) {
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	public static InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
		if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (!isProtectedEntity(entity)) {
			return InteractionResult.PASS;
		}

		if (!checkTerritoryPermission(serverPlayer, entity.blockPosition(), TerritoryPermission.DAMAGE_ENTITIES)) {
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	private static boolean isContainerBlock(Level level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof Container) {
			return true;
		}

		Block block = level.getBlockState(pos).getBlock();
		return block instanceof CraftingTableBlock;
	}

	private static boolean isRedstoneInteractBlock(Block block) {
		return block instanceof LeverBlock || block instanceof ButtonBlock;
	}

	private static boolean isBlockUseControlled(Block block) {
		return block instanceof BedBlock
			|| block instanceof DoorBlock
			|| block instanceof TrapDoorBlock
			|| block instanceof FenceGateBlock
			|| block instanceof CraftingTableBlock;
	}

	private static boolean isProtectedEntity(Entity entity) {
		if (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
			return true;
		}
		return entity instanceof Animal;
	}
}
