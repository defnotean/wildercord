package dev.wildercord.mixin;

import dev.wildercord.world.sites.oathhold.OathholdTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.SnowAndFreezeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cold-biome worldgen snow and surface ice stay out of Oathhold's 2,048x2,048 metropolis footprint. */
@Mixin(SnowAndFreezeFeature.class)
abstract class OathholdSnowMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepSnowOutOfOathhold(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
		if (OathholdTerrain.blocksSnowAndFreeze(level, origin)) {
			cir.setReturnValue(false);
		}
	}
}
