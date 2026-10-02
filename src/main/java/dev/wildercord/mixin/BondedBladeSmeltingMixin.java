package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bonded blade is never smelted into nuggets (or cut on a stonecutter): every one-item recipe (furnaces, smokers, blast furnaces,
 * campfires, the stonecutter) passes it by, so it sits in the furnace untouched until its swordsman takes it back.
 */
@Mixin(SingleItemRecipe.class)
public abstract class BondedBladeSmeltingMixin {
	@Inject(method = "matches(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"),
		cancellable = true)
	private void wildercord$neverABondedBlade(SingleRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(input.item())) {
			cir.setReturnValue(false);
		}
	}
}
