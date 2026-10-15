package dev.wildercord.world.biome;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.Wildercord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.function.Consumer;

/** The damp end of the ordinary swamp climate becomes Reedlands; all other climate dimensions stay intact. */
public final class ReedlandClimate {
	private ReedlandClimate() {}
	public static final ResourceKey<Biome> REEDLANDS = ResourceKey.create(Registries.BIOME, Wildercord.id("luminous_reedlands"));
	public static final String JOURNAL_ENTRY = "place:luminous_reedlands";
	/** Climate coordinates are quantized to ten-thousandths, so these adjacent bands have neither a gap nor a tie. */
	static final long MIN_HUMIDITY = 3000;

	public static void emit(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry,
		Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		var point = entry.getFirst();
		if (!entry.getSecond().equals(Biomes.SWAMP) || point.humidity().max() < MIN_HUMIDITY) {
			output.accept(entry);
			return;
		}
		if (point.humidity().min() < MIN_HUMIDITY) {
			output.accept(Pair.of(withHumidity(point, point.humidity().min(), MIN_HUMIDITY - 1), Biomes.SWAMP));
		}
		output.accept(Pair.of(withHumidity(point, Math.max(MIN_HUMIDITY, point.humidity().min()), point.humidity().max()), REEDLANDS));
	}

	private static Climate.ParameterPoint withHumidity(Climate.ParameterPoint point, long min, long max) {
		return new Climate.ParameterPoint(point.temperature(), new Climate.Parameter(min, max), point.continentalness(),
			point.erosion(), point.depth(), point.weirdness(), point.offset());
	}
}
