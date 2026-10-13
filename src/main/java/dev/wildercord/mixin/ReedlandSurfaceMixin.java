package dev.wildercord.mixin;

import dev.wildercord.world.biome.ReedlandClimate;
import dev.wildercord.world.biome.WindscarClimate;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.BiomeCondition;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Regional variants retain their parent biome's surface materials in 26.3's data-driven rules. */
@Mixin(BiomeCondition.class)
abstract class ReedlandSurfaceMixin {
	@Inject(method = "compile", at = @At("RETURN"), cancellable = true)
	private void wildercord$regionalSurface(MaterialRuleContext ruleContext, CallbackInfoReturnable<ConditionEvaluator> result) {
		var biomes = ((BiomeCondition) (Object) this).biomes();
		boolean swamp = biomes.stream().anyMatch(biome -> biome.is(Biomes.SWAMP));
		boolean hills = biomes.stream().anyMatch(biome -> biome.is(Biomes.WINDSWEPT_HILLS));
		if (!swamp && !hills) return;
		ConditionEvaluator original = result.getReturnValue();
		result.setReturnValue(new MaterialRuleContext.LazyYCondition(ruleContext) {
			@Override
			protected boolean compute() {
				return (swamp && context.getBiome().is(ReedlandClimate.REEDLANDS))
					|| (hills && context.getBiome().is(WindscarClimate.WINDSCAR)) || original.test();
			}
		});
	}
}
