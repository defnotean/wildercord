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
 * The more humid wooded-badlands band becomes Emberwood Reach—a warm scorched oak-and-ash woodland
 * retaining the parent's temperature, continentalness, erosion and weirdness transitions.
 */
public final class EmberwoodClimate {
	private EmberwoodClimate() {}
	public static final ResourceKey<Biome> EMBERWOOD = ResourceKey.create(Registries.BIOME, Wildercord.id("emberwood_reach"));
	public static final String JOURNAL_ENTRY = "place:emberwood_reach";
	/** Quantized humidity: the wooded ember belt starts at 0.25, leaving drier wooded badlands intact below it. */
	static final long MIN_HUMIDITY = 2500;

	public static void emit(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry,
		Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		var point = entry.getFirst();
		if (!entry.getSecond().equals(Biomes.WOODED_BADLANDS) || point.humidity().max() < MIN_HUMIDITY) {
			output.accept(entry);
			return;
		}
		if (point.humidity().min() < MIN_HUMIDITY) {
			output.accept(Pair.of(withHumidity(point, point.humidity().min(), MIN_HUMIDITY - 1), Biomes.WOODED_BADLANDS));
		}
		output.accept(Pair.of(withHumidity(point, Math.max(MIN_HUMIDITY, point.humidity().min()), point.humidity().max()), EMBERWOOD));
	}

	private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint point, long min, long max) {
		return new Climate.ParameterPoint(point.temperature(), new Climate.Parameter(min, max), point.continentalness(),
			point.erosion(), point.depth(), point.weirdness(), point.offset());
	}
}
