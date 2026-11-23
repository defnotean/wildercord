package dev.wildercord.mixin;

import dev.wildercord.world.sites.oathhold.OathholdTerrain;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SnowAndFreezeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cold-biome worldgen snow and surface ice stay out of Oathhold's 2,048x2,048 metropolis footprint. */
@Mixin(SnowAndFreezeFeature.class)
abstract class OathholdSnowMixin {
	// Before 26.3 every feature still takes the FeaturePlaceContext bag; 26.3 flattened it to
	// (WorldGenLevel, ChunkGenerator, RandomSource, BlockPos).
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepSnowOutOfOathhold(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		if (OathholdTerrain.blocksSnowAndFreeze(context.level(), context.origin())) {
			cir.setReturnValue(false);
		}
	}
}
