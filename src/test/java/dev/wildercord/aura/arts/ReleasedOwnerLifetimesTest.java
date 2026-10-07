package dev.wildercord.aura.arts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleasedOwnerLifetimesTest {
	private final ReleasedOwnerLifetimes<Object, Object> lives = new ReleasedOwnerLifetimes<>();
	private final Object body = new Object(), a = new Object(), b = new Object();

	@Test void noIntermediateCaptureOrPollCanReviveOldAOnRoundTrip() {
		var oldA = lives.capture(body, a);
		var outward = lives.begin(body, a, b); lives.finish(outward, b);
		// Early outer AFTER callback returns immediately. Nothing captures or polls a lifetime in B.
		var inward = lives.begin(body, b, a); lives.finish(inward, a);
		var freshA = lives.capture(body, a);
		assertTrue(oldA.retired); assertFalse(freshA.retired); assertNotSame(oldA, freshA);
		// A late outer callback has no retirement authority. Even stale close is harmless.
		lives.finish(outward, b);
		assertFalse(freshA.retired);
	}

	@Test void earlyDestinationCaptureSurvivesLaterOriginNotifications() {
		var oldA = lives.capture(body, a);
		var outward = lives.begin(body, a, b); lives.finish(outward, b);
		var freshB = lives.capture(body, b);
		lives.finish(outward, b);
		assertTrue(oldA.retired); assertFalse(freshB.retired);
	}

	@Test void setterCallbacksCannotGrantFreshOutgoingAuthority() {
		var oldA = lives.capture(body, a);
		var outward = lives.begin(body, a, b);
		var tooLateA = lives.capture(body, a);
		var incomingB = lives.capture(body, b);
		lives.finish(outward, b);
		assertTrue(oldA.retired); assertTrue(tooLateA.retired); assertFalse(incomingB.retired);
		var inward = lives.begin(body, b, a); lives.finish(inward, a);
		assertTrue(tooLateA.retired); assertTrue(incomingB.retired); assertFalse(lives.capture(body, a).retired);
	}

	@Test void nestedSetterCannotSmuggleAnOutgoingReceiptThroughPendingOuterAssignment() {
		var oldA = lives.capture(body, a);
		var outer = lives.begin(body, a, b);
		var inner = lives.begin(body, b, a);
		var outgoingAgain = lives.capture(body, a);
		lives.finish(inner, a);
		var afterInner = lives.capture(body, a);
		lives.finish(outer, b);
		assertTrue(oldA.retired); assertTrue(outgoingAgain.retired); assertTrue(afterInner.retired);
		assertFalse(lives.capture(body, b).retired);
	}

	@Test void sameLevelSetterPreservesIdentityAndDoesNotAddATransition() {
		var old = lives.capture(body, a);
		var same = lives.begin(body, a, a);
		assertSame(old, lives.capture(body, a)); lives.finish(same, a);
		assertFalse(old.retired); assertSame(old, lives.capture(body, a));
	}

	@Test void sameLevelCallInsideATransitionCannotLiftItsCaptureBarrier() {
		var outer = lives.begin(body, a, b);
		var same = lives.begin(body, a, a); lives.finish(same, a);
		assertTrue(lives.capture(body, a).retired);
		lives.finish(outer, b);
		assertFalse(lives.capture(body, b).retired);
	}

	@Test void exceptionCleanupUsesActualWorldAndNeverReusesOutgoingLife() {
		var old = lives.capture(body, a);
		var change = lives.begin(body, a, b);
		var blocked = lives.capture(body, a);
		// Original setter throws before assignment; finally still closes exactly this scope.
		lives.finish(change, a);
		var fresh = lives.capture(body, a);
		assertTrue(old.retired); assertTrue(blocked.retired); assertFalse(fresh.retired);
		assertNotSame(old, fresh);
	}

	@Test void replacementBodyAndLateOldBodyRetirementAreIndependent() {
		var old = lives.capture(body, a); Object replacement = new Object();
		var fresh = lives.capture(replacement, a);
		lives.retire(body); lives.retire(body);
		assertTrue(old.retired); assertFalse(fresh.retired);
	}

	@Test void deathRespawnDisconnectAndShutdownPermanentlyRetireReceipts() {
		for (int event = 0; event < 3; event++) {
			var old = lives.capture(body, a); lives.retire(body);
			assertTrue(old.retired); assertNotSame(old, lives.capture(body, a));
		}
		var last = lives.capture(body, a); lives.clear(); assertTrue(last.retired);
	}

	@Test void observedUninstrumentedWorldMismatchNeverRebindsAnExistingLife() {
		var old = lives.capture(body, a); var fresh = lives.capture(body, b);
		assertTrue(old.retired); assertFalse(fresh.retired); assertNotSame(old, fresh);
	}
	@Test void nestedTailTransitionCanCreateFreshReturnGenerationAfterActualOuterAssignment() {
		var old = lives.capture(body, a);
		var outer = lives.begin(body, a, b); lives.assigned(body);
		var incoming = lives.capture(body, b);
		var inner = lives.begin(body, b, a); lives.assigned(body); lives.finish(inner, a);
		var fresh = lives.capture(body, a);
		assertFalse(fresh.retired, "The outer setter already assigned; A is no longer its outgoing world");
		lives.finish(outer, a);
		assertTrue(old.retired); assertTrue(incoming.retired); assertFalse(fresh.retired);
	}

	@Test void nestedSameWorldSetterCannotMarkPendingOuterAssignmentAsComplete() {
		var outer = lives.begin(body, a, b);
		var same = lives.begin(body, a, a); lives.assigned(body); lives.finish(same, a);
		assertTrue(lives.capture(body, a).retired, "A nested no-op assignment does not lift the outer departure barrier");
		lives.assigned(body); lives.finish(outer, b);
		assertFalse(lives.capture(body, b).retired);
	}

	private static final class EqualIdBody {
		int id;
		EqualIdBody(int id) { this.id = id; }
		@Override public int hashCode() { return id; }
		@Override public boolean equals(Object other) { return other instanceof EqualIdBody body && id == body.id; }
	}

	@Test void nativeEqualEntityIdsCannotAliasReplacementLifetimes() {
		var registry = new ReleasedOwnerLifetimes<EqualIdBody, Object>();
		var oldBody = new EqualIdBody(7); var newBody = new EqualIdBody(7);
		assertEquals(oldBody, newBody); assertEquals(oldBody.hashCode(), newBody.hashCode());
		var old = registry.capture(oldBody, a); var fresh = registry.capture(newBody, a);
		assertNotSame(old, fresh);
		registry.retire(oldBody);
		assertTrue(old.retired); assertFalse(fresh.retired);
		assertSame(fresh, registry.capture(newBody, a));
	}

	@Test void lateOldRespawnOrDisconnectCannotRetireAnEarlyReplacementGrant() {
		var registry = new ReleasedOwnerLifetimes<EqualIdBody, Object>();
		var oldBody = new EqualIdBody(19); var newBody = new EqualIdBody(19);
		var old = registry.capture(oldBody, a); registry.retire(oldBody);
		var early = registry.capture(newBody, a);
		registry.retire(oldBody); registry.retire(oldBody);
		assertTrue(old.retired); assertFalse(early.retired);
		var outward = registry.begin(oldBody, a, b); registry.assigned(oldBody); registry.finish(outward, b);
		assertFalse(early.retired, "An equal-ID old body's later world callback cannot touch the replacement either");
	}

	@Test void mutableEntityIdDoesNotLoseExactBodyRegistryOwnership() {
		var registry = new ReleasedOwnerLifetimes<EqualIdBody, Object>();
		var body = new EqualIdBody(1); var original = registry.capture(body, a);
		body.id = 100;
		assertSame(original, registry.capture(body, a)); registry.retire(body); assertTrue(original.retired);
	}

}
