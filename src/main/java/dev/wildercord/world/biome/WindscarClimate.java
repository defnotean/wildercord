package dev.wildercord.world.biome;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.Wildercord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.function.Consumer;

/** Dry windswept hill country, retaining the original elevation, erosion and terrain transitions. */
public final class WindscarClimate {
	private WindscarClimate() {}
	public static final ResourceKey<Biome> WINDSCAR = ResourceKey.create(Registries.BIOME, Wildercord.id("windscar_highlands"));
	public static final String JOURNAL_ENTRY = "place:windscar_highlands";
	/** Quantized humidity: the dry band ends at -0.25, and ordinary hills resume at the next coordinate. */
	static final long MAX_HUMIDITY = -2500;

	public static void emit(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry,
		Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		var point = entry.getFirst();
		if (!entry.getSecond().equals(Biomes.WINDSWEPT_HILLS) || point.humidity().min() > MAX_HUMIDITY) {
			output.accept(entry);
			return;
		}
		output.accept(Pair.of(withHumidity(point, point.humidity().min(), Math.min(MAX_HUMIDITY, point.humidity().max())), WINDSCAR));
		if (point.humidity().max() > MAX_HUMIDITY) {
			output.accept(Pair.of(withHumidity(point, MAX_HUMIDITY + 1, point.humidity().max()), Biomes.WINDSWEPT_HILLS));
		}
	}

	private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint point, long min, long max) {
		return new Climate.ParameterPoint(point.temperature(), new Climate.Parameter(min, max), point.continentalness(),
			point.erosion(), point.depth(), point.weirdness(), point.offset());
	}
}
