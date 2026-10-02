package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In a chest (or any container that isn't a player's own inventory) only its swordsman can take a bonded blade out: anyone else sees
 * it, and its tooltip says whose it is, but it won't come. Every way of taking something out of a menu (a click, a shift-click, a
 * number key, a throw, a double-click gathering) asks {@code mayPickup} first.
 */
@Mixin(Slot.class)
public abstract class SlotBondMixin {
	@Shadow
	@Final
	public Container container;

	@Shadow
	public abstract ItemStack getItem();

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void wildercord$onlyItsSwordsman(Player player, CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = getItem();
		if (BondedBlades.bonded(stack) && !(container instanceof Inventory inv && inv.player == player) && !BondedBlades.mayTake(player, stack)) {
			cir.setReturnValue(false);
		}
	}
}
