package dev.wildercord.mixin;

import dev.wildercord.world.biome.BelowkeeperClimate;
import dev.wildercord.world.biome.EmberwoodClimate;
import dev.wildercord.world.biome.ReedlandClimate;
import dev.wildercord.world.biome.WindscarClimate;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Regional variants retain their parent biome's surface rules in 26.2 SurfaceRules. */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource")
abstract class ReedlandSurfaceMixin {
	@Shadow
	@Final
	private HolderSet<Biome> biomes;

	@Inject(method = "canNeverMatch(Ljava/util/Set;)Z", at = @At("RETURN"), cancellable = true)
	private void wildercord$regionalSurfaceCanMatch(Set<Holder<Biome>> possibleBiomes, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) return;
		boolean match = (possibleBiomes.stream().anyMatch(b -> b.is(ReedlandClimate.REEDLANDS)) && biomes.stream().anyMatch(b -> b.is(Biomes.SWAMP)))
			|| (possibleBiomes.stream().anyMatch(b -> b.is(WindscarClimate.WINDSCAR)) && biomes.stream().anyMatch(b -> b.is(Biomes.WINDSWEPT_HILLS)))
			|| (possibleBiomes.stream().anyMatch(b -> b.is(BelowkeeperClimate.HOLLOWS)) && biomes.stream().anyMatch(b -> b.is(Biomes.DRIPSTONE_CAVES)))
			|| (possibleBiomes.stream().anyMatch(b -> b.is(EmberwoodClimate.EMBERWOOD)) && biomes.stream().anyMatch(b -> b.is(Biomes.OLD_GROWTH_PINE_TAIGA)));
		if (match) cir.setReturnValue(false);
	}
}
