package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bonded blade is never an ingredient in the crafting grid (a rune's recipe asks for an axe, and would eat a bonded one, its bond and
 * its story with it). Smithing still takes it: an upgrade or a forging carries every component over, the bond included.
 */
@Mixin({ShapedRecipe.class, ShapelessRecipe.class})
public abstract class BondedBladeIngredientMixin {
	@Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"),
		cancellable = true)
	private void wildercord$neverABondedBlade(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
		for (ItemStack stack : input.items()) {
			if (BondedBlades.bonded(stack)) {
				cir.setReturnValue(false);
				return;
			}
		}
	}
}
