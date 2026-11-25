package dev.wildercord.world.biome;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.Wildercord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.function.Consumer;

/**
 * The high-weirdness band of eroded badlands becomes Cinder Caldera: a basalt-and-ash volcanic
 * basin dotted with smoking slag vents and Ash-Forged Crucibles.
 */
public final class CinderCalderaClimate {
	private CinderCalderaClimate() {}

	public static final ResourceKey<Biome> CINDER_CALDERA = ResourceKey.create(Registries.BIOME, Wildercord.id("cinder_caldera"));
	public static final String JOURNAL_ENTRY = "place:cinder_caldera";
	/** Quantized weirdness threshold keeping standard eroded badlands below +0.25 intact. */
	static final long MIN_WEIRDNESS = 2500;

	public static void emit(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry,
			Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		var point = entry.getFirst();
		if (!entry.getSecond().equals(Biomes.ERODED_BADLANDS) || point.weirdness().max() < MIN_WEIRDNESS) {
			output.accept(entry);
			return;
		}
		if (point.weirdness().min() < MIN_WEIRDNESS) {
			output.accept(Pair.of(withWeirdness(point, point.weirdness().min(), MIN_WEIRDNESS - 1), Biomes.ERODED_BADLANDS));
		}
		output.accept(Pair.of(withWeirdness(point, Math.max(MIN_WEIRDNESS, point.weirdness().min()), point.weirdness().max()), CINDER_CALDERA));
	}

	private static Climate.ParameterPoint withWeirdness(Climate.ParameterPoint point, long min, long max) {
		return new Climate.ParameterPoint(point.temperature(), point.humidity(), point.continentalness(),
			point.erosion(), point.depth(), new Climate.Parameter(min, max), point.offset());
	}
}
