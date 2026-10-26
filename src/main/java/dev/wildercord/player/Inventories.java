package dev.wildercord.player;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

/** Inventory lookups that go by item alone, so a renamed or otherwise marked stack still counts as what it is. */
public final class Inventories {
	private Inventories() {}

	/** The first main-inventory slot holding {@code item}, whatever its name or other components; -1 when there is none. */
	public static int slotOf(Inventory inventory, Item item) {
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			if (inventory.getItem(i).is(item)) return i;
		}
		return -1;
	}
}
