package dev.wildercord.mixin;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.world.biome.ReedlandClimate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/** Extend the vanilla Overworld preset without replacing its noise settings or other biome entries. */
@Mixin(OverworldBiomeBuilder.class)
abstract class ReedlandPlacementMixin {
	@ModifyVariable(method = "addBiomes", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> wildercord$reedlands(
		Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		return entry -> ReedlandClimate.emit(entry, output);
	}
}
