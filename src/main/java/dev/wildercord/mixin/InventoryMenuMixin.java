package dev.wildercord.mixin;

import dev.wildercord.content.CordItem;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import dev.wildercord.menu.CordSlot;
import dev.wildercord.menu.GearInventorySlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends the Cord slot (menu index 46) and then a slot for each kind of casting gear (47 onwards, in
 * {@link GearSlot#all()} order) to the player inventory menu, and teaches shift-click to move a Cord or a
 * piece of gear in and out of them.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
	/** Main inventory and hotbar, from InventoryMenu's own layout: indices 9-44. */
	private static final int WILDERCORD_INV_START = 9;
	private static final int WILDERCORD_INV_END = 45;

	/** Where the Cord slot really is: 46 on its own, later if another mod added slots to this menu first. */
	@Unique
	private int wildercord$cordIndex = CordSlot.MENU_INDEX;

	private InventoryMenuMixin() {
		super(null, 0);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void wildercord$addCordSlot(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
		this.wildercord$cordIndex = this.addSlot(new CordSlot(player, CordSlot.INVENTORY_X, CordSlot.INVENTORY_Y)).index;
		for (GearSlot kind : GearSlot.all()) {
			this.addSlot(new GearInventorySlot(player, kind));
		}
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void wildercord$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index < 0 || index >= this.slots.size()) {
			return;
		}
		Slot slot = this.slots.get(index);
		if (slot instanceof CordSlot || slot instanceof GearInventorySlot) {
			cir.setReturnValue(slot.hasItem() ? wildercord$move(player, slot, WILDERCORD_INV_START, WILDERCORD_INV_END, true) : ItemStack.EMPTY);
			return;
		}
		// Index 0 is the crafting result; taking over there would skip crafting bookkeeping.
		if (index == 0 || !slot.hasItem() || !slot.mayPickup(player)) {
			return;
		}
		ItemStack stack = slot.getItem();
		Slot target = stack.getItem() instanceof CordItem ? this.slots.get(this.wildercord$cordIndex) : wildercord$gearSlot(stack);
		if (target != null && !target.hasItem()) {
			ItemStack moved = wildercord$move(player, slot, target.index, target.index + 1, false);
			if (!moved.isEmpty()) {
				cir.setReturnValue(moved);
			}
		}
	}

	/** The gear slot this stack goes in, or null (it isn't gear). */
	@Unique
	private Slot wildercord$gearSlot(ItemStack stack) {
		GearSlot kind = GearSlots.slotFor(stack).orElse(null);
		if (kind == null) {
			return null;
		}
		for (Slot slot : this.slots) {
			if (slot instanceof GearInventorySlot gear && gear.kind() == kind) {
				return slot;
			}
		}
		return null;
	}

	private ItemStack wildercord$move(Player player, Slot slot, int destStart, int destEnd, boolean reverse) {
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (!this.moveItemStackTo(stack, destStart, destEnd, reverse)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}
}
