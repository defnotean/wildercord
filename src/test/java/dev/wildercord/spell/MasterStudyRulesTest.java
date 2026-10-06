package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterStudyRulesTest {
	@Test
	void verifiedCopyIsRequiredIndependentlyOfCurrentQualification() {
		assertFalse(MasterStudyRules.mayStudyRelay(8, true, false), "Eligible characters still need real Archive access");
		assertFalse(MasterStudyRules.mayStudyRelay(7, true, true), "A copied lesson cannot bypass a cracked eighth circle");
		assertFalse(MasterStudyRules.mayStudyRelay(20, false, true), "Copied receipt never substitutes for the victory");
		assertTrue(MasterStudyRules.mayStudyRelay(8, true, true), "Verified copying permits later reading without a lectern parameter");
		assertNotEquals(MasterStudyRules.RELAY_COPIED, MasterStudyRules.RELAY);
		assertNotEquals(MasterStudyRules.RELAY_COPIED, MasterStudyRules.RELAY_COPY, "Physical book record is not the verified receipt");
	}
	@Test
	void activeEighthCircleAndOldPermanentVictoryAreBothRequired() {
		assertFalse(MasterStudyRules.eligibleRelay(7, true));
		assertFalse(MasterStudyRules.eligibleRelay(8, false));
		assertTrue(MasterStudyRules.eligibleRelay(8, true));
		assertTrue(MasterStudyRules.eligibleRelay(20, true));
		assertFalse(MasterStudyRules.eligibleRelay(0, true));
	}

	@Test
	void readingCannotSkipRepeatOrExtendPages() {
		assertTrue(MasterStudyRules.nextPage(0, 1));
		assertTrue(MasterStudyRules.nextPage(1, 2));
		assertTrue(MasterStudyRules.nextPage(2, 3));
		assertFalse(MasterStudyRules.nextPage(0, 3));
		assertFalse(MasterStudyRules.nextPage(1, 1));
		assertFalse(MasterStudyRules.nextPage(2, 1));
		assertFalse(MasterStudyRules.nextPage(3, 4));
		assertFalse(MasterStudyRules.nextPage(-1, 0));
		assertFalse(MasterStudyRules.nextPage(Integer.MAX_VALUE, Integer.MIN_VALUE));
	}
}
