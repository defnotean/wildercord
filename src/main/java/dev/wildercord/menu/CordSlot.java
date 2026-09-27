package dev.wildercord.menu;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordItem;
import dev.wildercord.player.Spellbooks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Cord slot in the player's inventory, just above the offhand. It is backed by the
 * {@code wildercord:cord} attachment, which saves and syncs it.
 */
public class CordSlot extends Slot {
	/** Survival inventory position, relative to the window: directly above the offhand slot. */
	public static final int INVENTORY_X = 77;
	public static final int INVENTORY_Y = 44;
	/** Creative inventory tab position: mirrors the offhand slot on the other side of the armour. */
	public static final int CREATIVE_X = 127;
	public static final int CREATIVE_Y = 20;
	/** Vanilla's InventoryMenu builds slots 0-45, so the Cord slot is index 46. */
	public static final int MENU_INDEX = 46;

	private static final Identifier EMPTY_ICON = Wildercord.id("container/slot/cord");

	public CordSlot(Player player, int x, int y) {
		super(new CordContainer(player), 0, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return stack.getItem() instanceof CordItem;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Identifier getNoItemIcon() {
		return EMPTY_ICON;
	}

	/** A one-slot container that reads and writes the Cord attachment directly. */
	public static final class CordContainer implements Container {
		private final Player player;

		CordContainer(Player player) {
			this.player = player;
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return Spellbooks.cord(player).isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return slot == 0 ? Spellbooks.cord(player) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			ItemStack current = getItem(slot);
			if (current.isEmpty() || amount <= 0) {
				return ItemStack.EMPTY;
			}
			Spellbooks.setCord(player, ItemStack.EMPTY);
			return current;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack current = getItem(slot);
			Spellbooks.setCord(player, ItemStack.EMPTY);
			return current;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (slot != 0) {
				return;
			}
			if (stack.isEmpty() || stack.getItem() instanceof CordItem) {
				Spellbooks.setCord(player, stack);
			}
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public void setChanged() {
			// Writes go straight to the attachment, which marks itself dirty.
		}

		@Override
		public boolean stillValid(Player who) {
			return who == player;
		}

		@Override
		public void clearContent() {
			Spellbooks.setCord(player, ItemStack.EMPTY);
		}
	}
}
