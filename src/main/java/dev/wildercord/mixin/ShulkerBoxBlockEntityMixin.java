package dev.wildercord.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A shulker box's own slots refuse anything that can't go inside a container item (another shulker box, a
 * backpack), but a hopper feeding the box only checked for shulker boxes. Now it asks the item too, so a
 * hopper can't pack full backpacks into a shulker box where a hand can't.
 */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin {
	@Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
	private void wildercord$onlyWhatFits(int slot, ItemStack stack, @Nullable Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (!stack.getItem().canFitInsideContainerItems()) {
			cir.setReturnValue(false);
		}
	}
}
