package dev.wildercord.menu;

import dev.wildercord.Wildercord;
import dev.wildercord.gear.GearLayout;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A casting-gear slot in the player's inventory (see {@link GearSlot}): it takes one piece of gear
 * that fits it, and is backed by the {@code wildercord:gear} attachment, which saves and syncs it. The
 * slots follow the Cord slot in the inventory menu, in {@link GearSlot#all()} order.
 */
public class GearInventorySlot extends Slot implements PlacedSlot {
	private final GearSlot kind;

	public GearInventorySlot(Player player, GearSlot kind) {
		super(new GearContainer(player, kind), 0, GearLayout.inventoryX(kind), GearLayout.inventoryY(kind));
		this.kind = kind;
	}

	public GearSlot kind() {
		return kind;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return GearSlots.fits(kind, stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Identifier getNoItemIcon() {
		return Wildercord.id(kind.iconPath());
	}

	@Override
	public int creativeX() {
		return GearLayout.creativeX(kind);
	}

	@Override
	public int creativeY() {
		return GearLayout.creativeY(kind);
	}

	/** A one-slot container that reads and writes one gear slot of the attachment directly. */
	public static final class GearContainer implements Container {
		private final Player player;
		private final GearSlot kind;

		GearContainer(Player player, GearSlot kind) {
			this.player = player;
			this.kind = kind;
		}

		/** The gear slot this container is (the creative tab wraps the slot, but keeps its container). */
		public GearSlot kind() {
			return kind;
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return GearSlots.get(player, kind).isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return slot == 0 ? GearSlots.get(player, kind) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			if (slot != 0 || amount <= 0) {
				return ItemStack.EMPTY;
			}
			return GearSlots.clear(player, kind);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return slot == 0 ? GearSlots.clear(player, kind) : ItemStack.EMPTY;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (slot == 0) {
				GearSlots.set(player, kind, stack);
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
			GearSlots.clear(player, kind);
		}
	}
}
