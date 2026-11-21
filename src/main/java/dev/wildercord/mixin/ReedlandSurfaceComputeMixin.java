package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.world.biome.BelowkeeperClimate;
import dev.wildercord.world.biome.EmberwoodClimate;
import dev.wildercord.world.biome.ReedlandClimate;
import dev.wildercord.world.biome.WindscarClimate;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Maps regional variant biomes to their parent vanilla biome when SurfaceRules tests BiomeCondition. */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource$1BiomeCondition")
abstract class ReedlandSurfaceComputeMixin {
	// 26.1.2 has no BuiltInRegistries.BIOME field; the biome registry is only reachable via REGISTRY.
	@SuppressWarnings("unchecked")
	private static Registry<Biome> wildercord$biomes() {
		return (Registry<Biome>) BuiltInRegistries.REGISTRY.getValue(ResourceLocation.withDefaultNamespace("biome"));
	}

	@WrapOperation(method = "compute()Z", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/core/Holder;is(Ljava/util/function/Predicate;)Z"))
	private boolean wildercord$regionalSurfaceContains(Holder<Biome> current, Predicate<Biome> nameTest, Operation<Boolean> original) {
		if (original.call(current, nameTest)) return true;
		if (current.is(ReedlandClimate.REEDLANDS) && nameTest.test(wildercord$biomes().getValue(Biomes.SWAMP))) return true;
		if (current.is(WindscarClimate.WINDSCAR) && nameTest.test(wildercord$biomes().getValue(Biomes.WINDSWEPT_HILLS))) return true;
		if (current.is(BelowkeeperClimate.HOLLOWS) && nameTest.test(wildercord$biomes().getValue(Biomes.DRIPSTONE_CAVES))) return true;
		if (current.is(EmberwoodClimate.EMBERWOOD) && nameTest.test(wildercord$biomes().getValue(Biomes.OLD_GROWTH_PINE_TAIGA))) return true;
		return false;
	}
}
