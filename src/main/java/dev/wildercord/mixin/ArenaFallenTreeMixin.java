package dev.wildercord.mixin;

import dev.wildercord.world.dungeons.ArenaTrees;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The biome's fallen logs don't land in a dungeon either. */
@Mixin(FallenTreeFeature.class)
abstract class ArenaFallenTreeMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepOutOfDungeons(FeaturePlaceContext<FallenTreeConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		if (ArenaTrees.blocksTree(context.level(), context.origin())) {
			cir.setReturnValue(false);
		}
	}
}
