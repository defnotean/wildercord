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

class BelowkeeperClimateTest {
	@Test
	void dryDripstonePartitionPreservesDistancesAndEveryCaveCoordinate() {
		var original = point(-10000, 10000);
		var split = split(Pair.of(original, Biomes.DRIPSTONE_CAVES));
		assertEquals(List.of(BelowkeeperClimate.HOLLOWS, Biomes.DRIPSTONE_CAVES), split.stream().map(Pair::getSecond).toList());
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
				assertEquals(humidity <= -2500 ? BelowkeeperClimate.HOLLOWS : Biomes.DRIPSTONE_CAVES, covering.getFirst().getSecond());
			}
		}
	}

	@Test
	void lushCavesDeepDarkAndSurfaceRegionsStayUntouched() {
		for (var biome : List.of(Biomes.LUSH_CAVES, Biomes.DEEP_DARK, Biomes.SWAMP, Biomes.WINDSWEPT_HILLS,
			ReedlandClimate.REEDLANDS, WindscarClimate.WINDSCAR)) {
			var entry = Pair.of(point(-10000, 10000), biome);
			assertEquals(List.of(entry), split(entry));
			assertSame(entry, split(entry).getFirst());
		}
		var wetDripstone = Pair.of(point(-2499, 10000), Biomes.DRIPSTONE_CAVES);
		assertSame(wetDripstone, split(wetDripstone).getFirst());
	}

	@Test
	void dripstoneBandsAndBoundariesAreValidAndIdempotent() {
		for (long[] range : List.of(new long[] {-10000, -3500}, new long[] {-3500, -1000},
			new long[] {-1000, 10000}, new long[] {-2500, -2500}, new long[] {-2500, -2499})) {
			var once = split(Pair.of(point(range[0], range[1]), Biomes.DRIPSTONE_CAVES));
			var twice = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			once.forEach(entry -> BelowkeeperClimate.emit(entry, twice::add));
			assertEquals(once, twice);
			for (var entry : once) assertTrue(entry.getFirst().humidity().min() <= entry.getFirst().humidity().max());
		}
	}

	@Test
	void allThreeRegionalAdaptersComposeCommutatively() {
		for (var biome : List.of(Biomes.SWAMP, Biomes.WINDSWEPT_HILLS, Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES)) {
			var input = Pair.of(point(-10000, 10000), biome);
			var orderA = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			var orderB = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			ReedlandClimate.emit(input, e1 -> WindscarClimate.emit(e1, e2 -> BelowkeeperClimate.emit(e2, orderA::add)));
			BelowkeeperClimate.emit(input, e1 -> WindscarClimate.emit(e1, e2 -> ReedlandClimate.emit(e2, orderB::add)));
			assertEquals(orderA, orderB);
		}
	}

	private static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> split(Pair<Climate.ParameterPoint, ResourceKey<Biome>> input) {
		var result = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
		BelowkeeperClimate.emit(input, result::add);
		return result;
	}

	private static Climate.ParameterPoint point(long low, long high) {
		return new Climate.ParameterPoint(new Climate.Parameter(-10000, 10000), new Climate.Parameter(low, high),
			new Climate.Parameter(8000, 10000), new Climate.Parameter(-10000, 10000), new Climate.Parameter(2000, 9000),
			new Climate.Parameter(-10000, 10000), 0);
	}
}
