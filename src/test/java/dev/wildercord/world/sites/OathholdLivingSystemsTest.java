package dev.wildercord.world.sites;

import dev.wildercord.world.sites.oathhold.OathholdCitizenRules;
import dev.wildercord.world.sites.oathhold.OathholdLawRules;
import dev.wildercord.world.sites.oathhold.OathholdOrigin;
import dev.wildercord.world.sites.oathhold.OathholdPopulation;
import dev.wildercord.world.sites.oathhold.OathholdQuestRules;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit contracts for Oathhold's 750+ named citizens, city law rules, and Guild Request Board (F–S). */
final class OathholdLivingSystemsTest {
	@Test
	void megacityPopulatesOverFiveHundredDiverseCitizensAcrossAllArchetypes() {
		int planned = OathholdPopulation.countPlannedCitizens();
		assertTrue(planned >= 500, "Expected >= 500 planned citizens across Oathhold, got " + planned);

		Set<OathholdCitizenRules.Archetype> covered = OathholdPopulation.coveredArchetypes();
		assertEquals(OathholdCitizenRules.Archetype.values().length, covered.size(),
			"Every citizen archetype must be placed in Oathhold; missing " + EnumSet.complementOf((EnumSet<OathholdCitizenRules.Archetype>) covered));

		List<OathholdPopulation.PlannedSpawn> all = OathholdPopulation.sampleAllPlannedCitizens();
		assertEquals(planned, all.size());

		Set<String> coords = new HashSet<>();
		Set<String> names = new HashSet<>();
		for (OathholdPopulation.PlannedSpawn s : all) {
			String key = s.lx() + "," + s.ly() + "," + s.lz();
			assertTrue(coords.add(key), "Duplicate citizen spawn coordinate at " + key);
			OathholdCitizenRules.Identity id = OathholdCitizenRules.identityFor(s.lx(), s.ly(), s.lz(), s.archetype());
			names.add(id.fullName());
			assertFalse(id.givenName().isBlank());
			assertEquals(s.archetype(), id.archetype());
			assertTrue(id.skinTone() >= 0 && id.skinTone() < 4);
		}
		assertTrue(names.size() >= 400, "Expected high name/role variety across city, got " + names.size() + " unique full names");
	}

	@Test
	void lawRulesTrackCityBoundsWantedWindowAndBountyCap() {
		assertTrue(OathholdLawRules.isInsideCityWalls(100, -200, 100, -200));
		assertTrue(OathholdLawRules.isInsideCityWalls(100, -200, 100 + OathholdOrigin.OUTER_WALL_R, -200 - OathholdOrigin.OUTER_WALL_R));
		assertFalse(OathholdLawRules.isInsideCityWalls(100, -200, 100 + OathholdOrigin.OUTER_WALL_R + 1, -200));

		assertFalse(OathholdLawRules.isWanted(0, 50000L, 100L));
		assertTrue(OathholdLawRules.isWanted(20, 24100L, 100L));
		assertFalse(OathholdLawRules.isWanted(20, 24100L, 24100L));

		int b = OathholdLawRules.updatedBounty(0, OathholdLawRules.CrimeKind.ASSAULT);
		assertEquals(20, b);
		b = OathholdLawRules.updatedBounty(b, OathholdLawRules.CrimeKind.THEFT);
		assertEquals(32, b);
		int capped = 0;
		for (int i = 0; i < 10; i++) {
			capped = OathholdLawRules.updatedBounty(capped, OathholdLawRules.CrimeKind.MURDER);
		}
		assertEquals(OathholdLawRules.MAX_BOUNTY, capped);
		assertEquals(OathholdLawRules.WANTED_DURATION_TICKS + 1200L, OathholdLawRules.updatedWantedUntil(500L, 1200L));
	}

	@Test
	void questBoardCoversAllSixCategoriesAndRankProgression() {
		assertEquals(30, OathholdQuestRules.JOBS.size());
		Set<OathholdQuestRules.JobCategory> allCats = EnumSet.noneOf(OathholdQuestRules.JobCategory.class);
		Set<String> ids = new HashSet<>();
		for (OathholdQuestRules.JobSpec spec : OathholdQuestRules.JOBS) {
			assertTrue(ids.add(spec.id()), "Duplicate job id: " + spec.id());
			assertSame(spec, OathholdQuestRules.byId(spec.id()));
			allCats.add(spec.category());
			assertTrue(spec.needed() >= 1 && spec.emeralds() >= 1 && spec.rankPoints() >= 1);
		}
		assertEquals(OathholdQuestRules.JobCategory.values().length, allCats.size());

		assertEquals(OathholdQuestRules.Rank.F, OathholdQuestRules.Rank.ofPoints(0));
		assertEquals(OathholdQuestRules.Rank.E, OathholdQuestRules.Rank.ofPoints(50));
		assertEquals(OathholdQuestRules.Rank.D, OathholdQuestRules.Rank.ofPoints(150));
		assertEquals(OathholdQuestRules.Rank.C, OathholdQuestRules.Rank.ofPoints(350));
		assertEquals(OathholdQuestRules.Rank.B, OathholdQuestRules.Rank.ofPoints(700));
		assertEquals(OathholdQuestRules.Rank.A, OathholdQuestRules.Rank.ofPoints(1200));
		assertEquals(OathholdQuestRules.Rank.S, OathholdQuestRules.Rank.ofPoints(2000));
		assertEquals(1, OathholdQuestRules.Rank.S.tradeDiscountEmeralds(4));
		assertEquals(5, OathholdQuestRules.Rank.S.tradeDiscountEmeralds(8));

		UUID player = new UUID(42L, 99L);
		for (OathholdQuestRules.Rank rank : OathholdQuestRules.Rank.values()) {
			List<OathholdQuestRules.JobSpec> board = OathholdQuestRules.boardSelection(player, 3L, rank);
			assertEquals(8, board.size(), "Expected 8 board jobs at rank " + rank);
			Set<OathholdQuestRules.JobCategory> cats = EnumSet.noneOf(OathholdQuestRules.JobCategory.class);
			board.forEach(j -> cats.add(j.category()));
			assertTrue(cats.contains(OathholdQuestRules.JobCategory.CLEANING));
			assertTrue(cats.contains(OathholdQuestRules.JobCategory.HERB_GATHERING));
			assertTrue(cats.contains(OathholdQuestRules.JobCategory.WORK_SHIFT));
			assertTrue(cats.contains(OathholdQuestRules.JobCategory.MONSTER_HUNT));
			assertTrue(cats.contains(OathholdQuestRules.JobCategory.ARENA_BOUT));
		}
	}
}
