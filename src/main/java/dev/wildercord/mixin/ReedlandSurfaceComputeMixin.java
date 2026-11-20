package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.world.biome.BelowkeeperClimate;
import dev.wildercord.world.biome.EmberwoodClimate;
import dev.wildercord.world.biome.ReedlandClimate;
import dev.wildercord.world.biome.WindscarClimate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Maps regional variant biomes to their parent vanilla biome when SurfaceRules tests BiomeCondition. */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource$1BiomeCondition")
abstract class ReedlandSurfaceComputeMixin {
	@WrapOperation(method = "compute()Z", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/core/HolderSet;contains(Lnet/minecraft/core/Holder;)Z"))
	private boolean wildercord$regionalSurfaceContains(HolderSet<Biome> biomes, Holder<Biome> current, Operation<Boolean> original) {
		if (original.call(biomes, current)) return true;
		if (current.is(ReedlandClimate.REEDLANDS) && biomes.stream().anyMatch(b -> b.is(Biomes.SWAMP))) return true;
		if (current.is(WindscarClimate.WINDSCAR) && biomes.stream().anyMatch(b -> b.is(Biomes.WINDSWEPT_HILLS))) return true;
		if (current.is(BelowkeeperClimate.HOLLOWS) && biomes.stream().anyMatch(b -> b.is(Biomes.DRIPSTONE_CAVES))) return true;
		if (current.is(EmberwoodClimate.EMBERWOOD) && biomes.stream().anyMatch(b -> b.is(Biomes.OLD_GROWTH_PINE_TAIGA))) return true;
		return false;
	}
}
