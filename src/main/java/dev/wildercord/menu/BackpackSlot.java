package dev.wildercord.menu;

import dev.wildercord.Wildercord;
import dev.wildercord.backpack.Backpacks;
import dev.wildercord.gear.GearLayout;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Backpack slot in the player's inventory: it takes one backpack, which is worn on the back (and opened
 * with the backpack key). It is backed by the {@code wildercord:backpack} attachment, which saves and syncs
 * it, and sits on the gear tray after the gear slots (see {@link GearLayout#backpackIndex()}).
 */
public class BackpackSlot extends Slot implements PlacedSlot {
	private static final Identifier EMPTY_ICON = Wildercord.id("container/slot/backpack");

	public BackpackSlot(Player player) {
		super(new WornContainer(player), 0, GearLayout.inventoryX(GearLayout.backpackIndex()), GearLayout.INVENTORY_Y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return Backpacks.isBackpack(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Identifier getNoItemIcon() {
		return EMPTY_ICON;
	}

	@Override
	public int creativeX() {
		return GearLayout.creativeX(GearLayout.backpackIndex());
	}

	@Override
	public int creativeY() {
		return GearLayout.creativeY(GearLayout.backpackIndex());
	}

	/** A one-slot container that reads and writes the worn backpack directly. */
	public static final class WornContainer implements Container {
		private final Player player;

		WornContainer(Player player) {
			this.player = player;
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return Backpacks.worn(player).isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return slot == 0 ? Backpacks.worn(player) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			return slot == 0 && amount > 0 ? Backpacks.takeOff(player) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return slot == 0 ? Backpacks.takeOff(player) : ItemStack.EMPTY;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (slot == 0) {
				Backpacks.wear(player, stack);
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
			Backpacks.takeOff(player);
		}
	}
}
