package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hoppers (and hopper carts, which use the same) leave a bonded blade where its swordsman put it: they don't pick one up off the
 * ground, pull one out of a chest, or push one on, so a blade never wanders off into someone else's chest.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBondMixin {
	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
	private static void wildercord$leaveLying(Container container, ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(entity.getItem())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canTakeItemFromContainer", at = @At("HEAD"), cancellable = true)
	private static void wildercord$leaveInPlace(Container into, Container from, ItemStack stack, int slot, Direction direction,
			CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(stack)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canPlaceItemInContainer", at = @At("HEAD"), cancellable = true)
	private static void wildercord$noFurther(Container container, ItemStack stack, int slot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(stack)) {
			cir.setReturnValue(false);
		}
	}
}
