package com.example.client.mvvm;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of the player inventory used by the good picker grid.
 */
public final class ItemSlotPickerViewModel extends ViewModel {
	private final List<ItemStack> inventoryStacks;

	public ItemSlotPickerViewModel() {
		this.inventoryStacks = collectInventoryStacks();
	}

	public List<ItemStack> inventoryStacks() {
		return this.inventoryStacks;
	}

	private static List<ItemStack> collectInventoryStacks() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return List.of();
		}

		List<ItemStack> result = new ArrayList<>();
		for (ItemStack stack : minecraft.player.getInventory().items) {
			if (!stack.isEmpty()) {
				result.add(stack.copy());
			}
		}
		for (ItemStack stack : minecraft.player.getInventory().armor) {
			if (!stack.isEmpty()) {
				result.add(stack.copy());
			}
		}
		for (ItemStack stack : minecraft.player.getInventory().offhand) {
			if (!stack.isEmpty()) {
				result.add(stack.copy());
			}
		}
		return result;
	}
}
