package com.example.client.mvvm;

import com.example.client.model.ItemManageEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Registry-backed item/block list with disabled state and client-side search.
 */
public final class ItemManageViewModel extends ViewModel {
	private final Set<String> disabledItemIds;
	private final List<ItemManageEntry> allEntries;
	private final ObservableProperty<String> searchQuery;

	public ItemManageViewModel(Set<String> disabledItemIds, String searchQuery) {
		this.disabledItemIds = Set.copyOf(disabledItemIds);
		this.allEntries = buildEntries();
		this.searchQuery = property(searchQuery == null ? "" : searchQuery);
	}

	public Set<String> disabledItemIds() {
		return this.disabledItemIds;
	}

	public ObservableProperty<String> searchQuery() {
		return this.searchQuery;
	}

	public boolean isDisabled(ItemManageEntry entry) {
		return entry.disableSupported() && this.disabledItemIds.contains(entry.targetItemId());
	}

	public List<ItemManageEntry> filteredEntries() {
		String needle = this.searchQuery.get() == null ? "" : this.searchQuery.get().trim().toLowerCase(Locale.ROOT);
		if (needle.isBlank()) {
			return this.allEntries;
		}

		return this.allEntries.stream()
			.filter(entry ->
				entry.registryId().toLowerCase(Locale.ROOT).contains(needle)
					|| entry.materialLabel().toLowerCase(Locale.ROOT).contains(needle)
					|| entry.displayName().toLowerCase(Locale.ROOT).contains(needle))
			.toList();
	}

	private static List<ItemManageEntry> buildEntries() {
		List<ItemManageEntry> entries = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
			if ("minecraft:air".equals(itemId)) {
				continue;
			}
			ItemStack icon = new ItemStack(item);
			entries.add(new ItemManageEntry(itemId, icon.getHoverName().getString(), "Item", itemId, icon));
		}

		for (Block block : BuiltInRegistries.BLOCK) {
			String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
			Item blockItem = block.asItem();
			String targetId = blockItem != Items.AIR ? BuiltInRegistries.ITEM.getKey(blockItem).toString() : "";
			ItemStack icon = blockItem != Items.AIR ? new ItemStack(blockItem) : new ItemStack(Items.GRASS_BLOCK);
			String displayName = blockItem != Items.AIR ? icon.getHoverName().getString() : block.getName().getString();
			entries.add(new ItemManageEntry(blockId, displayName, "Block", targetId, icon));
		}

		entries.sort(Comparator.comparing(ItemManageEntry::registryId));
		return List.copyOf(entries);
	}
}
