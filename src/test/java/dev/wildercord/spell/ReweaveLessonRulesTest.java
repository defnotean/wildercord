package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ReweaveLessonRulesTest {
	@Test void oldLowTideVictoryRecoversTheLedgerWithoutLaterAddedReceipts() {
		assertTrue(MasterStudyRules.hasReweaveLesson(true, false, false));
		assertTrue(MasterStudyRules.hasReweaveLesson(false, true, false));
		assertTrue(MasterStudyRules.hasReweaveLesson(false, false, true));
		assertFalse(MasterStudyRules.hasReweaveLesson(false, false, false));
	}

	@Test void recoveredPagesStillRequireAnActiveTwelfthCircleAndLowTideToTeach() {
		for (int circles : new int[] {0, 8, 11}) assertFalse(MasterStudyRules.mayStudyReweave(circles, true, true));
		for (int circles : new int[] {12, 20}) {
			assertTrue(MasterStudyRules.eligibleReweave(circles, true));
			assertTrue(MasterStudyRules.mayStudyReweave(circles, true, true));
			assertFalse(MasterStudyRules.mayStudyReweave(circles, false, true));
			assertFalse(MasterStudyRules.mayStudyReweave(circles, true, false));
		}
	}

	@Test void readingExpiresAtFiveMinutesAndRejectsRewoundTime() {
		assertTrue(MasterStudyRules.liveReading(0));
		assertTrue(MasterStudyRules.liveReading(5 * 60 * 20 - 1));
		assertFalse(MasterStudyRules.liveReading(5 * 60 * 20));
		assertFalse(MasterStudyRules.liveReading(Long.MAX_VALUE));
		assertFalse(MasterStudyRules.liveReading(-1));
		assertFalse(MasterStudyRules.liveReading(Long.MIN_VALUE));
	}

	@Test void allThreeOrderedPagesAreNeededAndOldOrSkippedActionsCannotComplete() {
		assertEquals(3, MasterStudyRules.PAGES);
		for (int current = 0; current < 3; current++) {
			for (int requested = -1; requested < 6; requested++)
				assertEquals(requested == current + 1, MasterStudyRules.nextPage(current, requested));
		}
		assertFalse(MasterStudyRules.nextPage(3, 4));
		assertFalse(MasterStudyRules.nextPage(Integer.MAX_VALUE, Integer.MIN_VALUE));
	}

	@Test void physicalCopyAndRecoveryReceiptsAreDistinctFromTheCastEntitlement() {
		assertEquals(ReweaveRules.STUDY, MasterStudyRules.REWEAVE);
		assertEquals(4, Set.of(MasterStudyRules.REWEAVE, MasterStudyRules.REWEAVE_COPIED,
			MasterStudyRules.REWEAVE_COPY, MasterStudyRules.REWEAVE_REPLACEMENT).size());
		assertNotEquals(MasterStudyRules.RELAY, MasterStudyRules.REWEAVE);
		assertFalse(ReweaveRules.eligible(12, true, false));
		assertFalse(ReweaveRules.eligible(11, true, true));
		assertFalse(ReweaveRules.eligible(12, false, true));
		assertTrue(ReweaveRules.eligible(12, true, true));
	}
}
