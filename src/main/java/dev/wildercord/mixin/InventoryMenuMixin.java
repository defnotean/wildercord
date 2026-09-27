package dev.wildercord.mixin;

import dev.wildercord.content.CordItem;
import dev.wildercord.menu.CordSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends the Cord slot (menu index 46) to the player inventory menu, and teaches
 * shift-click to move a Cord in and out of it.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
	/** Main inventory and hotbar, from InventoryMenu's own layout: indices 9-44. */
	private static final int WILDERCORD_INV_START = 9;
	private static final int WILDERCORD_INV_END = 45;

	private InventoryMenuMixin() {
		super(null, 0);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void wildercord$addCordSlot(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
		this.addSlot(new CordSlot(player, CordSlot.INVENTORY_X, CordSlot.INVENTORY_Y));
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void wildercord$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index < 0 || index >= this.slots.size()) {
			return;
		}
		Slot slot = this.slots.get(index);
		if (slot instanceof CordSlot) {
			cir.setReturnValue(slot.hasItem() ? wildercord$move(player, slot, WILDERCORD_INV_START, WILDERCORD_INV_END, true) : ItemStack.EMPTY);
		} else if (index != 0 && slot.hasItem() && slot.getItem().getItem() instanceof CordItem && slot.mayPickup(player)
				&& !this.slots.get(CordSlot.MENU_INDEX).hasItem()) {
			// Index 0 is the crafting result; taking over there would skip crafting bookkeeping.
			ItemStack moved = wildercord$move(player, slot, CordSlot.MENU_INDEX, CordSlot.MENU_INDEX + 1, false);
			if (!moved.isEmpty()) {
				cir.setReturnValue(moved);
			}
		}
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
