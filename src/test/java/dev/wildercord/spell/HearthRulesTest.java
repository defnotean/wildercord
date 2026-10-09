package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HearthRulesTest {
	private static final Set<String> HEARTH = Set.of("slowburn", "camp_ward", "warm_cloak", "softsole", "softfoot", "hollow_pocket", "lodestar",
		"homeward", "gravefinder", "skyread", "lullaby", "steedsong", "glidewind", "waymark", "ember_rest", "orbcall", "tinker_hum", "lantern_soul",
		"keenkeep", "landread", "rally_light", "dew_drink", "sunbask", "currentkin", "surefoot", "long_arm", "nightwatch", "trailblaze", "hearthpath",
		"lostfind", "stillwell", "starchart", "petward", "whistle", "luckcharm", "smoke_signal", "wayfarer_hymn", "steedmend", "dynamo_stride",
		"tarry", "clot", "heartsense", "quench", "hearthbond", "springseek", "savor", "deepwarn", "enderhush");

	@Test
	void recastsRefreshAndNeverStack() {
		assertEquals(1200, HearthRules.refreshed(0, 0, 1200));
		assertEquals(1300, HearthRules.refreshed(1200, 100, 1200));
		assertEquals(5000, HearthRules.refreshed(5000, 100, 1200), "a shorter recast never shortens a running one");
		assertEquals(100, HearthRules.refreshed(0, 100, -5));
	}

	@Test
	void slowburnGivesBackHalfInQuarterSteps() {
		float[] r = HearthRules.slowburn(1, 0);
		assertEquals(0.5f, r[0], 1e-6);
		assertEquals(0f, r[1], 1e-6);
		r = HearthRules.slowburn(0.1f, 0);
		assertEquals(0f, r[0], 1e-6);
		assertEquals(0.05f, r[1], 1e-6);
		r = HearthRules.slowburn(0.4f, 0.05f);
		assertEquals(0.25f, r[0], 1e-6);
		assertEquals(0f, HearthRules.slowburn(0, 0.2f)[0], 1e-6);
		assertEquals(0.2f, HearthRules.slowburn(-1, 0.2f)[1], 1e-6);
	}

	@Test
	void keenkeepUndoesHalfTheWear() {
		assertArrayEquals(new int[] {0, 1}, HearthRules.keenkeep(1, 0));
		assertArrayEquals(new int[] {1, 0}, HearthRules.keenkeep(1, 1));
		assertArrayEquals(new int[] {2, 0}, HearthRules.keenkeep(4, 0));
		assertArrayEquals(new int[] {0, 1}, HearthRules.keenkeep(0, 1));
	}

	@Test
	void tarryAndClotStayBounded() {
		assertEquals(20, HearthRules.tarry(400, false));
		assertEquals(0, HearthRules.tarry(400, true), "infinite effects are left alone");
		assertEquals(0, HearthRules.tarry(40, false), "nearly-ended effects end");
		assertEquals(80, HearthRules.clot(100));
		assertEquals(1, HearthRules.clot(10), "clot hastens, never cures");
	}

	@Test
	void savorNeverPassesTheFoodLevel() {
		assertEquals(5f, HearthRules.savor(4, 18, 3f), 1e-6);
		assertEquals(10f, HearthRules.savor(8, 10, 9f), 1e-6);
		assertEquals(3f, HearthRules.savor(0, 18, 3f), 1e-6);
	}

	@Test
	void manaGiftsAreCappedPerCast() {
		assertEquals(0, HearthRules.dynamo(23, 0));
		assertEquals(2, HearthRules.dynamo(24, 0));
		assertEquals(HearthRules.DYNAMO_MAX, HearthRules.dynamo(10_000, 0));
		assertEquals(0, HearthRules.dynamo(48, HearthRules.DYNAMO_MAX));
		assertEquals(0f, HearthRules.stillwell(HearthRules.STILL_AFTER - 1, 0), 1e-6);
		assertEquals(HearthRules.STILL_MANA, HearthRules.stillwell(HearthRules.STILL_AFTER, 0), 1e-6);
		assertEquals(0.1f, HearthRules.stillwell(999, HearthRules.STILL_MAX - 0.1f), 1e-5);
		assertEquals(0f, HearthRules.stillwell(999, HearthRules.STILL_MAX), 1e-6);
	}

	@Test
	void glidePushKeepsSpeedButNeverBuildsPastTheCap() {
		assertEquals(0.12, HearthRules.push(0.5, 1.2, 0.12), 1e-9);
		assertEquals(0.05, HearthRules.push(1.15, 1.2, 0.12), 1e-9);
		assertEquals(0, HearthRules.push(1.2, 1.2, 0.12));
		assertEquals(0, HearthRules.push(3, 1.2, 0.12));
	}

	@Test
	void homewardNeedsTheSameWorldAndRange() {
		assertTrue(HearthRules.homeward(true, 2000.0 * 2000));
		assertFalse(HearthRules.homeward(true, 2000.0 * 2000 + 1));
		assertFalse(HearthRules.homeward(false, 1));
	}

	@Test
	void campWardOnlyTakesFreshNamelessSpawns() {
		assertTrue(HearthRules.freshSpawn(0, false, false));
		assertFalse(HearthRules.freshSpawn(40, false, false));
		assertFalse(HearthRules.freshSpawn(0, true, false));
		assertFalse(HearthRules.freshSpawn(0, false, true));
	}

	@Test
	void softfootKeepsGrudgesAndCloseMonsters() {
		assertTrue(HearthRules.loses(9 * 9, Integer.MAX_VALUE));
		assertFalse(HearthRules.loses(8 * 8, Integer.MAX_VALUE));
		assertFalse(HearthRules.loses(20 * 20, HearthRules.GRUDGE_TICKS));
	}

	@Test
	void clockMoonAndMinutes() {
		assertEquals(6, HearthRules.hour(0));
		assertEquals(12, HearthRules.hour(6000));
		assertEquals(0, HearthRules.hour(18000));
		assertEquals(5, HearthRules.hour(23999));
		assertEquals(0, HearthRules.moon(0));
		assertEquals(1, HearthRules.moon(24000));
		assertEquals(0, HearthRules.moon(24000L * 8));
		assertEquals(1, HearthRules.minutes(0));
		assertEquals(1, HearthRules.minutes(1200));
		assertEquals(2, HearthRules.minutes(1201));
	}

	@Test
	void smallRules() {
		assertTrue(HearthRules.ready(0, 40, 40));
		assertFalse(HearthRules.ready(10, 40, 40));
		assertTrue(HearthRules.crumb(36));
		assertFalse(HearthRules.crumb(35.9));
		assertTrue(HearthRules.drop(6));
		assertFalse(HearthRules.drop(5));
	}

	@Test
	void theHearthRunesAreGentleEffectsWithAPlaceToBeFound() {
		Set<String> seen = new HashSet<>();
		for (RuneDef rune : Runes.all()) {
			if (HEARTH.contains(rune.path())) {
				seen.add(rune.path());
				assertEquals(RuneFamily.EFFECT, rune.family(), rune.id());
				assertNotEquals(EffectKind.HARMFUL, rune.kind(), rune.id() + " must not be offensive");
				assertTrue(rune.tier() >= 1 && rune.tier() <= 4, rune.id());
				assertTrue(rune.cost() > 0, rune.id());
			}
		}
		assertEquals(HEARTH, seen);
		assertEquals(48, HEARTH.size());
	}

	@Test
	void fourteenHearthRunesCanBePassives() {
		int passive = 0;
		for (RuneDef rune : Runes.all()) {
			if (HEARTH.contains(rune.path()) && Passives.allowed(rune)) {
				passive++;
			}
		}
		assertEquals(14, passive);
		assertFalse(Passives.allowed(Runes.HOMEWARD));
		assertFalse(Passives.allowed(Runes.HOLLOW_POCKET));
		assertFalse(Passives.allowed(Runes.CAMP_WARD));
	}
}
