package dev.wildercord.menu;

import dev.wildercord.backpack.BackpackTier;
import dev.wildercord.backpack.Backpacks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

/**
 * An open backpack: its rows above the player's inventory, laid out like a chest's. Only a backpack in its
 * owner's hand or Backpack slot is opened (see {@link Backpacks#open}).
 *
 * <p>On the server the menu holds the very stack it opened and where it was, and writes every change to its
 * slots straight back into that stack, so the backpack is always up to date: saved with the player at any
 * moment (a logout saves before it closes the menu), dropped on death with what was in it. The open backpack
 * can't be moved while the menu is open: its hotbar slot is locked, and a number key or the off-hand key can't
 * swap it out from another slot. Anything that moves it anyway (a thrown item, a death, a command) leaves the
 * stack somewhere else, and from then on the menu takes no clicks, writes nothing, and is closed at the end of
 * the tick; the backpack keeps everything it held at that moment.</p>
 */
public class BackpackMenu extends AbstractContainerMenu {
	/** What the client needs to build the screen: how many rows, and which inventory slot holds the open backpack ({@code -1}: worn). */
	public record Opening(int rows, int locked) {
		public static final StreamCodec<ByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Opening::rows, ByteBufCodecs.VAR_INT, Opening::locked, Opening::new);

		public Opening {
			// A bad packet can't build a screen bigger than the biggest backpack.
			rows = Math.clamp(rows, 1, BackpackTier.RUNEWOVEN.rows);
		}
	}

	/** The backpack's own slots are laid out from here, like a chest's (and the inventory below them). */
	public static final int LEFT = 8;
	public static final int TOP = 18;

	private final int rows;
	/** The inventory slot the open backpack is in (0-8, or 40 for the off-hand), or -1 when it's worn. */
	private final int locked;
	private final Contents contents;
	/** Server only: whose backpack, where it is and the stack itself. Null on the client. */
	private final @Nullable Player owner;
	private final Backpacks.@Nullable Place place;
	private final @Nullable ItemStack stack;

	/** The client's side. */
	public BackpackMenu(int containerId, Inventory inventory, Opening opening) {
		this(containerId, inventory, opening.rows(), opening.locked(), null, null, null);
	}

	/** The server's side: the backpack {@code stack}, which is at {@code place}. */
	public BackpackMenu(int containerId, Inventory inventory, Player owner, Backpacks.Place place, ItemStack stack, BackpackTier tier) {
		this(containerId, inventory, tier.rows, place.slot(), owner, place, stack);
	}

	private BackpackMenu(int containerId, Inventory inventory, int rows, int locked, @Nullable Player owner, Backpacks.@Nullable Place place,
			@Nullable ItemStack stack) {
		super(WildercordMenus.BACKPACK, containerId);
		this.rows = rows;
		this.locked = locked;
		this.owner = owner;
		this.place = place;
		this.stack = stack;
		this.contents = new Contents(rows * 9);
		if (stack != null) {
			stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents.getItems());
		}
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < 9; x++) {
				addSlot(new Inside(contents, x + y * 9, LEFT + x * 18, TOP + y * 18));
			}
		}
		int inventoryTop = TOP + rows * 18 + 13;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				addSlot(inventorySlot(inventory, x + y * 9 + 9, LEFT + x * 18, inventoryTop + y * 18));
			}
		}
		for (int x = 0; x < 9; x++) {
			addSlot(inventorySlot(inventory, x, LEFT + x * 18, inventoryTop + 58));
		}
	}

	private Slot inventorySlot(Inventory inventory, int index, int x, int y) {
		return index == locked ? new Locked(inventory, index, x, y) : new Slot(inventory, index, x, y);
	}

	public int rows() {
		return rows;
	}

	/** The inventory slot holding the open backpack, or -1 when it's worn. */
	public int locked() {
		return locked;
	}

	/** Whether a slot of this menu is the open backpack's own, which can't be touched. */
	public static boolean isLocked(Slot slot) {
		return slot instanceof Locked;
	}

	/**
	 * Whether the backpack is still exactly where it was opened: the same stack, not a copy, not empty, and its
	 * owner alive. Always true on the client, which only mirrors the server.
	 */
	public boolean holds(Player player) {
		if (stack == null || place == null) {
			return true;
		}
		return player == owner && player.isAlive() && !stack.isEmpty() && place.stack(player) == stack;
	}

	@Override
	public boolean stillValid(Player player) {
		return holds(player);
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		// The open backpack stays put: a number key (or F, for the off-hand) can't swap it out from any slot.
		if (input == ContainerInput.SWAP && locked >= 0 && buttonNum == locked) {
			return;
		}
		// Moved some other way since it opened: nothing more happens here, and the menu closes this tick.
		if (!holds(player)) {
			return;
		}
		super.clicked(slotIndex, buttonNum, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack moving = slot.getItem();
		ItemStack before = moving.copy();
		int inside = rows * 9;
		if (slotIndex < inside) {
			if (!this.moveItemStackTo(moving, inside, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!Backpacks.fitsInside(moving) || !this.moveItemStackTo(moving, 0, inside, false)) {
			return ItemStack.EMPTY;
		}
		if (moving.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return before;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (stack == null || place == null) {
			return;
		}
		if (holds(player)) {
			save();
			// Worn, the stack was written in place: hand the owner's game a fresh copy, so its Backpack slot shows
			// what's in it now.
			if (place.worn()) {
				Backpacks.wear(player, stack);
			}
		}
		Backpacks.playClose(player);
	}

	/** Writes the slots into the backpack, if it's still where it was opened (see {@link #holds}). */
	private void save() {
		if (stack != null && owner != null && holds(owner)) {
			stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
		}
	}

	/** The backpack's slots: every change is written straight back into the backpack. */
	private final class Contents extends SimpleContainer {
		Contents(int size) {
			super(size);
		}

		@Override
		public void setChanged() {
			super.setChanged();
			save();
		}
	}

	/** One of the backpack's own slots: takes anything that doesn't hold items itself. */
	private static final class Inside extends Slot {
		Inside(SimpleContainer container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return Backpacks.fitsInside(stack);
		}
	}

	/** The inventory slot of the open backpack: it can't be picked up, swapped, thrown or filled while it's open. */
	private static final class Locked extends Slot {
		Locked(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean isHighlightable() {
			return false;
		}
	}
}
