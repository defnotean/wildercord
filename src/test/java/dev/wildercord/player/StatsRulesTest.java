package dev.wildercord.player;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsRulesTest {
	private static List<String> keys(StatsRules.Snapshot s) {
		return StatsRules.rows(s).stream().map(StatsRules.Row::key).toList();
	}

	@Test
	void playTimeReadsInHoursAndMinutes() {
		assertEquals("0m", StatsRules.playTime(0));
		assertEquals("42m", StatsRules.playTime(42L * StatsRules.TICKS_PER_MINUTE));
		assertEquals("12h 05m", StatsRules.playTime((12 * 60 + 5L) * StatsRules.TICKS_PER_MINUTE));
		assertEquals("0m", StatsRules.playTime(-5));
	}

	@Test
	void percentStaysInRange() {
		assertEquals(0, StatsRules.percent(3, 0));
		assertEquals(33, StatsRules.percent(1, 3));
		assertEquals(100, StatsRules.percent(9, 3));
	}

	@Test
	void aNewcomerSeesOnlyWhatTheyHave() {
		var fresh = new StatsRules.Snapshot(100, 0, 0, -1, "", 0, 0, 0, 0, 30, 0, 0, 0);
		assertEquals(List.of("play_time", "field_guide", "feats", "deaths"), keys(fresh));
	}

	@Test
	void aVeteranSeesBothRoads() {
		var veteran = new StatsRules.Snapshot(100, 20, 2, 4, "ember", 5, 1, 12, 20, 30, 300, 4, 7);
		assertEquals(List.of("play_time", "circles_ascended", "aura", "duels", "field_guide", "codex", "feats", "deaths"), keys(veteran));
		assertEquals(List.of(20, 30, 67), StatsRules.rows(veteran).get(4).args());
	}
}
