package dev.wildercord.world.biome;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReedlandClimateTest {
	@Test
	void partitionPreservesCoverageAndEveryOtherClimateCoordinate() {
		var original = point(-10000, 10000);
		var split = split(original, Biomes.SWAMP);
		assertEquals(List.of(Biomes.SWAMP, ReedlandClimate.REEDLANDS), split.stream().map(Pair::getSecond).toList());
		for (var entry : split) {
			var next = entry.getFirst();
			assertEquals(original.temperature(), next.temperature());
			assertEquals(original.continentalness(), next.continentalness());
			assertEquals(original.erosion(), next.erosion());
			assertEquals(original.depth(), next.depth());
			assertEquals(original.weirdness(), next.weirdness());
			assertEquals(original.offset(), next.offset());
		}
		for (long humidity = -15000; humidity <= 15000; humidity++) {
			long coordinate = humidity;
			long nearest = split.stream().mapToLong(entry -> entry.getFirst().humidity().distance(coordinate)).min().orElseThrow();
			assertEquals(original.humidity().distance(humidity), nearest, "Climate distance at " + humidity);
			if (humidity >= -10000 && humidity <= 10000) {
				var covering = split.stream().filter(entry -> entry.getFirst().humidity().distance(coordinate) == 0).toList();
				assertEquals(1, covering.size(), "Exactly one band at " + humidity);
				assertEquals(humidity >= 3000 ? ReedlandClimate.REEDLANDS : Biomes.SWAMP, covering.getFirst().getSecond());
			}
		}
	}

	@Test
	void drySwampsAndEveryOtherBiomePassThroughUnchanged() {
		for (var biome : List.of(Biomes.MANGROVE_SWAMP, Biomes.RIVER, Biomes.PLAINS, Biomes.DEEP_DARK)) {
			var entry = Pair.of(point(-10000, 10000), biome);
			var output = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			ReedlandClimate.emit(entry, output::add);
			assertEquals(1, output.size());
			assertSame(entry, output.getFirst());
		}
		var dry = point(-10000, 2999);
		assertSame(dry, split(dry, Biomes.SWAMP).getFirst().getFirst());
	}

	@Test
	void boundaryAndAlreadySplitBandsStayValidAndIdempotent() {
		for (long[] range : List.of(new long[] {3000, 3000}, new long[] {3000, 10000}, new long[] {2999, 3000})) {
			var once = split(point(range[0], range[1]), Biomes.SWAMP);
			var twice = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			once.forEach(entry -> ReedlandClimate.emit(entry, twice::add));
			assertEquals(once, twice);
			for (var entry : once) assertTrue(entry.getFirst().humidity().min() <= entry.getFirst().humidity().max());
		}
	}

	private static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> split(Climate.ParameterPoint point, ResourceKey<Biome> biome) {
		var result = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
		ReedlandClimate.emit(Pair.of(point, biome), result::add);
		return result;
	}

	private static Climate.ParameterPoint point(long low, long high) {
		return new Climate.ParameterPoint(new Climate.Parameter(-4500, 2000), new Climate.Parameter(low, high),
			new Climate.Parameter(-1100, 10000), new Climate.Parameter(5500, 10000), new Climate.Parameter(0, 0),
			new Climate.Parameter(-10000, -9333), 0);
	}
}
