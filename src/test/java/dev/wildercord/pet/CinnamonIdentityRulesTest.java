package dev.wildercord.pet;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static dev.wildercord.pet.CinnamonIdentityRules.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class CinnamonIdentityRulesTest {
	private static final UUID A = new UUID(1, 1), B = new UUID(2, 2);
	@Test void onlyPristineLegacyOwnerCanReceiveFirstIdentity() {
		assertEquals(NEW, CinnamonIdentityRules.status(null, null, false));
		assertEquals(UNCERTAIN, CinnamonIdentityRules.status(null, null, true));
	}
	@Test void lostOrTruncatedJournalDoesNotAuthorizeReplacement() {
		assertEquals(UNCERTAIN, CinnamonIdentityRules.status(A, null, false));
		assertEquals(UNCERTAIN, CinnamonIdentityRules.status(A, null, true));
	}
	@Test void matchingIndependentRecordsRetainIdentity() {
		assertEquals(REGISTERED, CinnamonIdentityRules.status(A, A, false));
		assertEquals(REGISTERED, CinnamonIdentityRules.status(A, A, true));
	}
	@Test void conflictingMarkerCannotBeSilentlyRebound() {
		assertEquals(UNCERTAIN, CinnamonIdentityRules.status(A, B, false));
		assertEquals(UNCERTAIN, CinnamonIdentityRules.status(B, A, true));
	}
	@Test void reconstructedMarkerNeverAuthorizesDeletingDifferentBody() {
		assertFalse(CinnamonIdentityRules.mayRetire(B, A, null, true));
		assertFalse(CinnamonIdentityRules.mayRetire(B, A, A, true));
		assertFalse(CinnamonIdentityRules.mayRetire(B, A, A, false));
	}
	@Test void deliberateRetirementRequiresExactBodyAndMarker() {
		assertTrue(CinnamonIdentityRules.mayRetire(A, A, A, true));
		assertFalse(CinnamonIdentityRules.mayRetire(A, A, A, false));
		assertFalse(CinnamonIdentityRules.mayRetire(A, A, B, true));
	}
	@Test void intactJournalCanRestoreOnlyItsOwnMissingMarker() {
		assertEquals(REGISTERED, CinnamonIdentityRules.status(null, A, false));
		assertNotEquals(NEW, CinnamonIdentityRules.status(null, A, true));
	}
}
