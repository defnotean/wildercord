package dev.wildercord.world.biome;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.Wildercord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.function.Consumer;

/** Underground fungal-and-dripstone hollows, retaining the parent cave's depth, continentalness and transitions. */
public final class BelowkeeperClimate {
	private BelowkeeperClimate() {}
	public static final ResourceKey<Biome> HOLLOWS = ResourceKey.create(Registries.BIOME, Wildercord.id("belowkeeper_hollows"));
	public static final String JOURNAL_ENTRY = "place:belowkeeper_hollows";
	/** Quantized humidity: the cool hollow band ends at -0.25, leaving Lush Caves ([0.7, 1.0]) and wetter dripstone caves untouched. */
	static final long MAX_HUMIDITY = -2500;

	public static void emit(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry,
		Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		var point = entry.getFirst();
		if (!entry.getSecond().equals(Biomes.DRIPSTONE_CAVES) || point.humidity().min() > MAX_HUMIDITY) {
			output.accept(entry);
			return;
		}
		output.accept(Pair.of(withHumidity(point, point.humidity().min(), Math.min(MAX_HUMIDITY, point.humidity().max())), HOLLOWS));
		if (point.humidity().max() > MAX_HUMIDITY) {
			output.accept(Pair.of(withHumidity(point, MAX_HUMIDITY + 1, point.humidity().max()), Biomes.DRIPSTONE_CAVES));
		}
	}

	private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint point, long min, long max) {
		return new Climate.ParameterPoint(point.temperature(), new Climate.Parameter(min, max), point.continentalness(),
			point.erosion(), point.depth(), point.weirdness(), point.offset());
	}
}
