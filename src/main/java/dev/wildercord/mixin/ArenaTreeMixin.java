package dev.wildercord.mixin;

import dev.wildercord.world.dungeons.ArenaTrees;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The biome's trees (and a sapling's) don't grow into a dungeon. */
@Mixin(TreeFeature.class)
abstract class ArenaTreeMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepOutOfDungeons(FeaturePlaceContext<TreeConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		if (ArenaTrees.blocksTree(context.level(), context.origin())) {
			cir.setReturnValue(false);
		}
	}
}
