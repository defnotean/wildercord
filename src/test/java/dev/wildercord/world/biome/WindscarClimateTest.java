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

class WindscarClimateTest {
	@Test
	void dryPartitionPreservesDistancesAndEveryTerrainCoordinate() {
		var original = point(-10000, 10000);
		var split = split(Pair.of(original, Biomes.WINDSWEPT_HILLS));
		assertEquals(List.of(WindscarClimate.WINDSCAR, Biomes.WINDSWEPT_HILLS), split.stream().map(Pair::getSecond).toList());
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
			assertEquals(original.humidity().distance(humidity),
				split.stream().mapToLong(entry -> entry.getFirst().humidity().distance(coordinate)).min().orElseThrow(),
				"Climate distance at " + humidity);
			if (humidity >= -10000 && humidity <= 10000) {
				var covering = split.stream().filter(entry -> entry.getFirst().humidity().distance(coordinate) == 0).toList();
				assertEquals(1, covering.size(), "Exactly one biome at " + humidity);
				assertEquals(humidity <= -2500 ? WindscarClimate.WINDSCAR : Biomes.WINDSWEPT_HILLS, covering.getFirst().getSecond());
			}
		}
	}

	@Test
	void ordinaryWetHillsOtherMountainsAndReedlandsStayUntouched() {
		for (var biome : List.of(Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.MEADOW,
			Biomes.STONY_PEAKS, Biomes.RIVER, Biomes.DEEP_DARK, Biomes.SWAMP, ReedlandClimate.REEDLANDS)) {
			var entry = Pair.of(point(-10000, 10000), biome);
			assertEquals(List.of(entry), split(entry));
			assertSame(entry, split(entry).getFirst());
		}
		var wet = Pair.of(point(-2499, 10000), Biomes.WINDSWEPT_HILLS);
		assertSame(wet, split(wet).getFirst());
	}

	@Test
	void actualDryHillHumidityBandsAndBoundariesAreValidAndIdempotent() {
		// 26.3's temperate shattered hills use [-1,-.35], [-.35,-.1] and [-.1,.1].
		for (long[] range : List.of(new long[] {-10000, -3500}, new long[] {-3500, -1000},
			new long[] {-1000, 1000}, new long[] {-2500, -2500}, new long[] {-2500, -2499})) {
			var once = split(Pair.of(point(range[0], range[1]), Biomes.WINDSWEPT_HILLS));
			var twice = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			once.forEach(entry -> WindscarClimate.emit(entry, twice::add));
			assertEquals(once, twice);
			for (var entry : once) assertTrue(entry.getFirst().humidity().min() <= entry.getFirst().humidity().max());
		}
		assertEquals(WindscarClimate.WINDSCAR, split(Pair.of(point(-10000, -3500), Biomes.WINDSWEPT_HILLS)).getFirst().getSecond());
		assertEquals(2, split(Pair.of(point(-3500, -1000), Biomes.WINDSWEPT_HILLS)).size());
		assertEquals(Biomes.WINDSWEPT_HILLS, split(Pair.of(point(-1000, 1000), Biomes.WINDSWEPT_HILLS)).getFirst().getSecond());
	}

	@Test
	void regionalAdaptersComposeWithoutChangingEitherPartition() {
		for (var biome : List.of(Biomes.SWAMP, Biomes.WINDSWEPT_HILLS, Biomes.PLAINS)) {
			var input = Pair.of(point(-10000, 10000), biome);
			var wetFirst = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			var dryFirst = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			ReedlandClimate.emit(input, entry -> WindscarClimate.emit(entry, wetFirst::add));
			WindscarClimate.emit(input, entry -> ReedlandClimate.emit(entry, dryFirst::add));
			assertEquals(wetFirst, dryFirst);
		}
	}

	private static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> split(Pair<Climate.ParameterPoint, ResourceKey<Biome>> input) {
		var result = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
		WindscarClimate.emit(input, result::add);
		return result;
	}

	private static Climate.ParameterPoint point(long low, long high) {
		return new Climate.ParameterPoint(new Climate.Parameter(-1500, 2000), new Climate.Parameter(low, high),
			new Climate.Parameter(-1100, 10000), new Climate.Parameter(4500, 5500), new Climate.Parameter(0, 0),
			new Climate.Parameter(-10000, -9333), 0);
	}
}
