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

class EmberwoodClimateTest {
	@Test
	void humidWoodedBadlandsPartitionPreservesDistancesAndEveryTerrainCoordinate() {
		var original = point(-10000, 10000);
		var split = split(Pair.of(original, Biomes.WOODED_BADLANDS));
		assertEquals(List.of(Biomes.WOODED_BADLANDS, EmberwoodClimate.EMBERWOOD), split.stream().map(Pair::getSecond).toList());
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
				assertEquals(humidity >= 2500 ? EmberwoodClimate.EMBERWOOD : Biomes.WOODED_BADLANDS, covering.getFirst().getSecond());
			}
		}
	}

	@Test
	void ordinaryBadlandsForestsAndOtherRegionalBiomesStayUntouched() {
		for (var biome : List.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.FOREST, Biomes.SAVANNA,
			Biomes.SWAMP, Biomes.WINDSWEPT_HILLS, Biomes.DRIPSTONE_CAVES,
			ReedlandClimate.REEDLANDS, WindscarClimate.WINDSCAR, BelowkeeperClimate.HOLLOWS)) {
			var entry = Pair.of(point(-10000, 10000), biome);
			assertEquals(List.of(entry), split(entry));
			assertSame(entry, split(entry).getFirst());
		}
		var dryWooded = Pair.of(point(-10000, 2499), Biomes.WOODED_BADLANDS);
		assertSame(dryWooded, split(dryWooded).getFirst());
	}

	@Test
	void woodedBadlandsBandsAndBoundariesAreValidAndIdempotent() {
		for (long[] range : List.of(new long[] {1000, 3000}, new long[] {3000, 10000},
			new long[] {2500, 2500}, new long[] {2499, 2500})) {
			var once = split(Pair.of(point(range[0], range[1]), Biomes.WOODED_BADLANDS));
			var twice = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			once.forEach(entry -> EmberwoodClimate.emit(entry, twice::add));
			assertEquals(once, twice);
			for (var entry : once) assertTrue(entry.getFirst().humidity().min() <= entry.getFirst().humidity().max());
		}
		assertEquals(2, split(Pair.of(point(1000, 3000), Biomes.WOODED_BADLANDS)).size());
		assertEquals(EmberwoodClimate.EMBERWOOD, split(Pair.of(point(3000, 10000), Biomes.WOODED_BADLANDS)).getFirst().getSecond());
	}

	@Test
	void allFourRegionalAdaptersComposeCommutatively() {
		for (var biome : List.of(Biomes.SWAMP, Biomes.WINDSWEPT_HILLS, Biomes.DRIPSTONE_CAVES, Biomes.WOODED_BADLANDS, Biomes.FOREST)) {
			var input = Pair.of(point(-10000, 10000), biome);
			var orderA = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			var orderB = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			ReedlandClimate.emit(input, e1 -> WindscarClimate.emit(e1, e2 -> BelowkeeperClimate.emit(e2, e3 -> EmberwoodClimate.emit(e3, orderA::add))));
			EmberwoodClimate.emit(input, e1 -> BelowkeeperClimate.emit(e1, e2 -> WindscarClimate.emit(e2, e3 -> ReedlandClimate.emit(e3, orderB::add))));
			assertEquals(orderA, orderB);
		}
	}

	private static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> split(Pair<Climate.ParameterPoint, ResourceKey<Biome>> input) {
		var result = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
		EmberwoodClimate.emit(input, result::add);
		return result;
	}

	private static Climate.ParameterPoint point(long low, long high) {
		return new Climate.ParameterPoint(new Climate.Parameter(2000, 5500), new Climate.Parameter(low, high),
			new Climate.Parameter(300, 10000), new Climate.Parameter(-2225, 4500), new Climate.Parameter(0, 0),
			new Climate.Parameter(-10000, 10000), 0);
	}
}
