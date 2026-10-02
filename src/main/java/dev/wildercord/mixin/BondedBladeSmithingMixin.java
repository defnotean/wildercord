package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * A bonded blade at an anvil: it can be mended and enchanted as the left-hand item as any weapon can (the bond goes with it), but it is
 * never used up as the right-hand one (its story would go with the anvil's leftovers), and once it has a name of its own an anvil
 * can't give it another (that's its swordsman's to give, on the Aura page).
 */
@Mixin(AnvilMenu.class)
public abstract class BondedBladeSmithingMixin extends ItemCombinerMenu {
	@Shadow
	@Final
	private DataSlot cost;

	private BondedBladeSmithingMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slots) {
		super(type, id, inventory, access, slots);
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void wildercord$bondedBlade(CallbackInfo ci) {
		ItemStack left = inputSlots.getItem(0);
		ItemStack right = inputSlots.getItem(1);
		ItemStack result = resultSlots.getItem(0);
		if (BondedBlades.bonded(right)) {
			resultSlots.setItem(0, ItemStack.EMPTY);
			cost.set(0);
			return;
		}
		if (result.isEmpty() || !BondedBlades.bonded(left) || BondedBlades.bond(left).shownName().isEmpty()) {
			return;
		}
		if (!Objects.equals(result.get(DataComponents.CUSTOM_NAME), left.get(DataComponents.CUSTOM_NAME))) {
			// The name stays the blade's own; and an anvil asked for nothing but a new name has nothing to do.
			ItemStack kept = result.copy();
			if (left.has(DataComponents.CUSTOM_NAME)) {
				kept.set(DataComponents.CUSTOM_NAME, left.get(DataComponents.CUSTOM_NAME));
			} else {
				kept.remove(DataComponents.CUSTOM_NAME);
			}
			if (ItemStack.isSameItemSameComponents(kept, left)) {
				resultSlots.setItem(0, ItemStack.EMPTY);
				cost.set(0);
			} else {
				resultSlots.setItem(0, kept);
			}
		}
	}
}
