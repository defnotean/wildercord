package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WayfarerRulesTest {
	private static final List<String> PACK = List.of("land_reading", "depth_sounding", "spawn_bearing", "home_bearing", "grave_bearing",
		"portal_reckoning", "slime_sense", "sky_reading", "moon_reading", "sun_reading", "lux_reading", "chalk_line", "village_sense", "ruin_sense",
		"shipwreck_sense", "portal_sense", "fortress_sense", "stronghold_compass", "spire_sense", "trail_blaze", "relic_sense", "spawner_sense",
		"steady_brush", "appraise", "trade_renew", "haggle", "folk_call", "folk_census", "lapis_thrift", "quickbrew", "potion_steep", "lore_reading",
		"shelf_count", "beacon_swell", "dye_wash", "checker_dye", "glyph_carve", "lamplighter", "snuff_out", "sign_glow", "frame_veil", "stand_pose",
		"lava_crust", "void_step", "lava_sense", "gold_parley");

	@Test
	void thePackIsFortySixHarmlessCraftableEffects() {
		assertEquals(46, PACK.size());
		assertEquals(46, new HashSet<>(PACK).size());
		Set<String> categories = new HashSet<>();
		for (String path : PACK) {
			RuneDef rune = Runes.get("wildercord:" + path).orElse(null);
			assertNotNull(rune, path);
			assertEquals(RuneFamily.EFFECT, rune.family(), path);
			assertNotEquals(EffectKind.HARMFUL, rune.kind(), path + " must never hurt anything");
			assertTrue(rune.tier() >= 1 && rune.tier() <= 3, path + " is craftable");
			assertTrue(rune.cost() > 0, path);
			categories.add(rune.category());
		}
		assertEquals(Set.of("world", "support"), categories);
	}

	@Test
	void compassPointsWithNorthAtMinusZ() {
		assertEquals("north", WayfarerRules.COMPASS[WayfarerRules.compass(0, -10)]);
		assertEquals("east", WayfarerRules.COMPASS[WayfarerRules.compass(10, 0)]);
		assertEquals("south", WayfarerRules.COMPASS[WayfarerRules.compass(0, 10)]);
		assertEquals("west", WayfarerRules.COMPASS[WayfarerRules.compass(-10, 0)]);
		assertEquals("northeast", WayfarerRules.COMPASS[WayfarerRules.compass(10, -10)]);
		assertEquals("southwest", WayfarerRules.COMPASS[WayfarerRules.compass(-7, 7)]);
	}

	@Test
	void distancesAreToldRoughly() {
		assertEquals(10, WayfarerRules.rough(0));
		assertEquals(10, WayfarerRules.rough(4));
		assertEquals(40, WayfarerRules.rough(37));
		assertEquals(100, WayfarerRules.rough(110));
		assertEquals(1250, WayfarerRules.rough(1234));
	}

	@Test
	void structureSensesShareABoundedSearchAndARest() {
		assertEquals(WayfarerRules.LOCATE_CELLS, WayfarerRules.locateCells(1.0));
		assertEquals(WayfarerRules.LOCATE_CELLS_MAX, WayfarerRules.locateCells(10.0));
		assertEquals(1, WayfarerRules.locateCells(0));
		assertFalse(WayfarerRules.resting(Long.MIN_VALUE, 0));
		assertTrue(WayfarerRules.resting(100, 100));
		assertTrue(WayfarerRules.resting(100, 100 + WayfarerRules.LOCATE_REST - 1));
		assertFalse(WayfarerRules.resting(100, 100 + WayfarerRules.LOCATE_REST));
		assertFalse(WayfarerRules.resting(500, 100), "a clock that went back (a new world) never locks the senses");
	}

	@Test
	void skyAndClockReadings() {
		assertEquals(0, WayfarerRules.nightsToFull(0));
		assertEquals(4, WayfarerRules.nightsToFull(4));
		assertEquals(1, WayfarerRules.nightsToFull(7));
		assertTrue(WayfarerRules.day(0));
		assertFalse(WayfarerRules.day(13000));
		assertTrue(WayfarerRules.day(24000 + 100));
		assertEquals(12000, WayfarerRules.untilTurn(0));
		assertEquals(1000, WayfarerRules.untilTurn(23000));
		assertEquals("06:00", WayfarerRules.hour(0));
		assertEquals("12:00", WayfarerRules.hour(6000));
		assertEquals("00:00", WayfarerRules.hour(18000));
		assertEquals("07:30", WayfarerRules.hour(1500));
		assertEquals(1, WayfarerRules.minutes(0));
		assertEquals(10, WayfarerRules.minutes(12000));
	}

	@Test
	void portalReckoningIsEightToOne() {
		assertEquals(12, WayfarerRules.toNether(100));
		assertEquals(-13, WayfarerRules.toNether(-100));
		assertEquals(800, WayfarerRules.toOverworld(100));
		assertEquals(-4, WayfarerRules.toOverworld(-0.5));
	}

	@Test
	void dyeIsPaidForByTheBlock() {
		assertEquals(0, WayfarerRules.dyesFor(0));
		assertEquals(1, WayfarerRules.dyesFor(1));
		assertEquals(1, WayfarerRules.dyesFor(8));
		assertEquals(2, WayfarerRules.dyesFor(9));
		assertEquals(8, WayfarerRules.dyeable(1));
		assertEquals(WayfarerRules.MAX_DYED, WayfarerRules.dyeable(64));
		assertEquals(0, WayfarerRules.dyeable(0));
		assertTrue(WayfarerRules.checker(0, 0, 0));
		assertFalse(WayfarerRules.checker(1, 0, 0));
		assertTrue(WayfarerRules.checker(1, 1, 0));
		assertTrue(WayfarerRules.checker(-1, 0, 1));
	}

	@Test
	void steepingIsBoundedAndNeverShortens() {
		assertEquals(0, WayfarerRules.steeped(0));
		assertEquals(1000 + 250, WayfarerRules.steeped(1000));
		assertEquals(6000 + WayfarerRules.STEEP_ADD_MAX, WayfarerRules.steeped(6000));
		assertEquals(WayfarerRules.STEEP_CAP, WayfarerRules.steeped(9000));
		assertEquals(20000, WayfarerRules.steeped(20000));
		for (int t = 1; t < 12000; t += 37) {
			assertTrue(WayfarerRules.steeped(t) >= t);
		}
	}

	@Test
	void beaconsSwellByHalf() {
		assertEquals(0, WayfarerRules.beaconReach(0));
		assertEquals(20, WayfarerRules.beaconReach(1));
		assertEquals(50, WayfarerRules.beaconReach(4));
		assertEquals(75.0, WayfarerRules.swelledReach(4));
	}

	@Test
	void restockIsOncePerDay() {
		assertEquals(0, WayfarerRules.dayOf(23999));
		assertEquals(1, WayfarerRules.dayOf(24000));
		assertEquals("wildercord.restocked.3", WayfarerRules.restockTag(WayfarerRules.dayOf(3 * 24000 + 5)));
		assertNotEquals(WayfarerRules.restockTag(0), WayfarerRules.restockTag(1));
	}

	@Test
	void areasPosesCrustsAndPlatforms() {
		assertEquals(0, WayfarerRules.nextPose(-1));
		assertEquals(0, WayfarerRules.nextPose(WayfarerRules.POSES - 1));
		assertEquals(6.0, WayfarerRules.area(6, 1.0, 10));
		assertEquals(10.0, WayfarerRules.area(6, 3.0, 10));
		assertEquals(1.0, WayfarerRules.area(6, 0.0, 10));
		assertEquals(WayfarerRules.CRUST, WayfarerRules.crust(1.0));
		assertEquals(WayfarerRules.CRUST_MAX, WayfarerRules.crust(5.0));
		assertTrue(WayfarerRules.crumble(1, 120) < WayfarerRules.crumble(0, 120), "the edge goes first");
		assertEquals(120, WayfarerRules.crumble(1, 120));
		assertEquals(15, WayfarerRules.shelves(20));
		assertEquals(0, WayfarerRules.shelves(-1));
		assertFalse(WayfarerRules.tooCostly(38));
		assertTrue(WayfarerRules.tooCostly(39));
	}
}
