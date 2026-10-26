package dev.wildercord.mixin;

import dev.wildercord.world.dungeons.ArenaTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The biome's fallen logs don't land in a dungeon either. */
@Mixin(FallenTreeFeature.class)
abstract class ArenaFallenTreeMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepOutOfDungeons(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
		if (ArenaTrees.blocksTree(level, origin)) {
			cir.setReturnValue(false);
		}
	}
}
