package dev.wildercord.mixin;

import dev.wildercord.world.sites.oathhold.OathholdTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Live precipitation in cold edge biomes does not blanket Oathhold's streets, canals or harbor in snow/ice. */
@Mixin(Biome.class)
abstract class OathholdWeatherSnowMixin {
	@Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
	private void wildercord$preventCitySnowfall(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (OathholdTerrain.blocksLiveWeatherSnow(level, pos)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
	private void wildercord$preventCityFreezing(LevelReader level, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir) {
		if (OathholdTerrain.blocksLiveWeatherSnow(level, pos)) {
			cir.setReturnValue(false);
		}
	}
}
