package dev.wildercord.mixin;

import dev.wildercord.world.biome.ReedlandClimate;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.BiomeCondition;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reedlands inherit swamp bank/water conditions in 26.3's data-driven material rules. */
@Mixin(BiomeCondition.class)
abstract class ReedlandSurfaceMixin {
	@Inject(method = "compile", at = @At("RETURN"), cancellable = true)
	private void wildercord$wetlandSurface(MaterialRuleContext ruleContext, CallbackInfoReturnable<ConditionEvaluator> result) {
		var biomes = ((BiomeCondition) (Object) this).biomes();
		if (biomes.stream().noneMatch(biome -> biome.is(Biomes.SWAMP))) return;
		ConditionEvaluator original = result.getReturnValue();
		result.setReturnValue(new MaterialRuleContext.LazyYCondition(ruleContext) {
			@Override
			protected boolean compute() {
				return context.getBiome().is(ReedlandClimate.REEDLANDS) || original.test();
			}
		});
	}
}
