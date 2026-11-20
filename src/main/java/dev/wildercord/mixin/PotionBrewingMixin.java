package dev.wildercord.mixin;

import dev.wildercord.compat.WildercordBrewing;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers Wildercord brewing mixes in 26.2 where data-driven {@code minecraft:brewing} recipes do not exist. */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {
	@Inject(method = "addVanillaMixes", at = @At("TAIL"))
	private static void wildercord$addMixes(PotionBrewing.Builder builder, CallbackInfo ci) {
		WildercordBrewing.register(builder);
	}
}
