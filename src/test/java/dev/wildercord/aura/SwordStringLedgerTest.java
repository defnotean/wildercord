package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.SwordString.Token.*;
import static org.junit.jupiter.api.Assertions.*;

class SwordStringLedgerTest {
	private final Object body = new Object(), level = new Object(), weapon = new Object();
	private final SwordStringLedger.Context owner = new SwordStringLedger.Context(body, level, weapon);
	private final SwordStringLedger ledger = new SwordStringLedger();

	@Test void attackThenPunchKeepsPreResetChargeAndSprint() {
		ledger.attack(10, FULL.bit() | RUN.bit(), 11, owner);
		assertTrue(ledger.punch(10, SWING.bit(), 11, owner));
		assertTrue(FULL.fits(ledger.last().marks()));
		assertTrue(RUN.fits(ledger.last().marks()));
		assertEquals(1, ledger.size());
	}

	@Test void airPunchUsesItsOwnPreResetState() {
		ledger.punch(10, FULL.bit() | LOW.bit(), 11, owner);
		assertTrue(FULL.fits(ledger.last().marks()));
		assertTrue(LOW.fits(ledger.last().marks()));
	}

	@Test void pendingPartialAttackCannotBorrowLaterFullClaim() {
		ledger.attack(10, SWING.bit(), 11, owner);
		ledger.punch(11, FULL.bit(), 11, owner);
		assertFalse(FULL.fits(ledger.last().marks()));
	}

	@Test void attackSnapshotIsConsumedOnlyOnce() {
		ledger.attack(10, FULL.bit() | RUN.bit(), 11, owner);
		ledger.punch(10, 0, 11, owner);
		ledger.punch(11, LOW.bit(), 11, owner);
		assertFalse(FULL.fits(ledger.last().marks()));
		assertFalse(RUN.fits(ledger.last().marks()));
		assertTrue(LOW.fits(ledger.last().marks()));
	}

	@Test void newerAttackReplacesAnOlderFullSnapshot() {
		ledger.attack(10, FULL.bit(), 11, owner);
		ledger.attack(11, LOW.bit(), 11, owner);
		ledger.punch(11, 0, 11, owner);
		assertFalse(FULL.fits(ledger.last().marks()));
		assertTrue(LOW.fits(ledger.last().marks()));
	}

	@Test void snapshotCanCrossOneServerTickBoundaryButExpiresPromptly() {
		ledger.attack(10, FULL.bit(), 11, owner);
		ledger.punch(12, 0, 11, owner);
		assertTrue(FULL.fits(ledger.last().marks()));
		ledger.attack(20, FULL.bit(), 11, owner);
		ledger.punch(23, 0, 11, owner);
		assertFalse(FULL.fits(ledger.last().marks()));
	}

	@Test void sameTickPacketsAndSpearTargetsCountOnceAcrossRoutes() {
		assertTrue(ledger.thrust(10, FULL.bit(), 11, owner));
		assertFalse(ledger.thrust(10, FULL.bit(), 11, owner));
		assertFalse(ledger.punch(10, LOW.bit(), 11, owner));
		ledger.attack(10, FULL.bit(), 11, owner);
		assertTrue(ledger.punch(11, 0, 11, owner));
		assertEquals(2, ledger.size());
		assertFalse(FULL.fits(ledger.last().marks()), "A duplicate attack cannot leave a full snapshot for the next tick");
	}

	@Test void weaponSwapInvalidatesPendingStateWithoutInventingFullCharge() {
		ledger.attack(10, FULL.bit(), 11, owner);
		var swapped = new SwordStringLedger.Context(body, level, new Object());
		ledger.punch(11, LOW.bit(), 20, swapped);
		assertFalse(FULL.fits(ledger.last().marks()));
		assertEquals(20, ledger.last().recover());
	}

	@Test void newPlayerBodyOrLevelCannotReuseOldStrokes() {
		ledger.punch(10, LOW.bit(), 11, owner);
		ledger.attack(11, FULL.bit(), 11, owner);
		ledger.punch(11, 0, 11, new SwordStringLedger.Context(new Object(), level, weapon));
		assertEquals(1, ledger.size());
		assertFalse(FULL.fits(ledger.last().marks()));
		ledger.punch(12, 0, 11, new SwordStringLedger.Context(body, new Object(), weapon));
		assertEquals(1, ledger.size());
	}

	@Test void movementMarksMustActuallyBeObserved() {
		ledger.punch(10, SWING.bit(), 11, owner);
		for (var token : new SwordString.Token[] {FULL, LOW, LEAP, RUN, COUNTER, STEP}) {
			assertTrue(ledger.proof(SwordString.of(token), 10, 10, owner).isEmpty(), token.id);
		}
		assertTrue(ledger.proof(SwordString.of(SWING), 10, 10, owner).isPresent());
	}

	@Test void unknownAndUnprovenCueBitsAreNotTrusted() {
		ledger.punch(10, 1 << 30 | COUNTER.bit() | STEP.bit(), 11, owner);
		assertEquals(SWING.bit(), ledger.last().marks());
	}

	@Test void latestSuffixMustFitEachRequiredMark() {
		ledger.punch(10, FULL.bit(), 11, owner);
		ledger.punch(21, LOW.bit(), 11, owner);
		assertTrue(ledger.proof(SwordString.of(FULL, LOW), 21, 10, owner).isPresent());
		assertTrue(ledger.proof(SwordString.of(LOW, FULL), 21, 10, owner).isEmpty());
		ledger.punch(22, 0, 11, owner);
		assertTrue(ledger.proof(SwordString.of(FULL, LOW), 22, 10, owner).isEmpty());
	}

	@Test void freshnessAndEachPreviousWeaponsRecoveryBoundTheString() {
		ledger.punch(10, FULL.bit(), 20, owner);
		ledger.punch(40, LOW.bit(), 0, owner);
		assertTrue(ledger.proof(SwordString.of(FULL, LOW), 40, 10, owner).isPresent());
		assertTrue(ledger.proof(SwordString.of(FULL, LOW), 51, 10, owner).isEmpty());
		assertTrue(ledger.proof(SwordString.of(FULL, LOW), 39, 10, owner).isEmpty());
	}

	@Test void networkSlackIsOneBudgetRatherThanAnExtraBudgetPerGap() {
		ledger.punch(10, FULL.bit(), 0, owner);
		ledger.punch(35, FULL.bit(), 0, owner); // 15 ticks beyond the ten-tick window
		ledger.punch(50, LOW.bit(), 0, owner); // the remaining five ticks of slack
		assertTrue(ledger.proof(SwordString.of(FULL, FULL, LOW), 50, 10, owner).isPresent());
		var other = new SwordStringLedger();
		other.punch(10, FULL.bit(), 0, owner);
		other.punch(35, FULL.bit(), 0, owner);
		other.punch(51, LOW.bit(), 0, owner);
		assertTrue(other.proof(SwordString.of(FULL, FULL, LOW), 51, 10, owner).isEmpty());
	}

	@Test void acceptedStrokesCannotBeReplayedForAnotherArt() {
		ledger.punch(10, FULL.bit(), 11, owner);
		ledger.punch(21, LOW.bit(), 11, owner);
		var proof = ledger.proof(SwordString.of(FULL, LOW), 21, 10, owner).orElseThrow();
		assertEquals(java.util.List.of(SWING.bit() | FULL.bit(), SWING.bit() | LOW.bit()), proof.marks());
		assertTrue(ledger.consume(proof));
		assertFalse(ledger.consume(proof));
		assertTrue(ledger.proof(SwordString.of(SWING), 21, 10, owner).isEmpty());
		ledger.punch(22, LOW.bit(), 11, owner);
		assertTrue(ledger.proof(SwordString.of(SWING, LOW), 22, 10, owner).isEmpty());
		assertTrue(ledger.proof(SwordString.of(LOW), 22, 10, owner).isPresent());
	}

	@Test void aProofCannotConsumeAChangedSuffix() {
		ledger.punch(10, LOW.bit(), 11, owner);
		var old = ledger.proof(SwordString.of(LOW), 10, 10, owner).orElseThrow();
		ledger.punch(11, LOW.bit(), 11, owner);
		assertFalse(ledger.consume(old));
	}

	@Test void proofsCannotCrossOwnersOrReuseARespawnedBodiesIdenticalStroke() {
		ledger.punch(10, LOW.bit(), 11, owner);
		var old = ledger.proof(SwordString.of(LOW), 10, 10, owner).orElseThrow();
		var other = new SwordStringLedger();
		other.punch(10, LOW.bit(), 11, owner);
		assertFalse(other.consume(old));
		ledger.punch(10, LOW.bit(), 11, new SwordStringLedger.Context(new Object(), level, weapon));
		assertFalse(ledger.consume(old));
	}

	@Test void aNewBodiesCueIsKeptButAnOldBodiesCueIsNot() {
		ledger.cue(10, true, owner);
		var nextBody = new SwordStringLedger.Context(new Object(), level, weapon);
		ledger.cue(11, false, nextBody);
		ledger.punch(11, 0, 11, nextBody);
		assertFalse(COUNTER.fits(ledger.last().marks()));
		assertTrue(STEP.fits(ledger.last().marks()));
	}

	@Test void guardAndStepBelongOnlyToTheFirstObservedSwing() {
		ledger.cue(10, true, owner);
		ledger.cue(10, false, owner);
		ledger.attack(11, FULL.bit(), 11, owner);
		ledger.punch(11, 0, 11, owner);
		assertTrue(COUNTER.fits(ledger.last().marks()));
		assertTrue(STEP.fits(ledger.last().marks()));
		ledger.punch(12, 0, 11, owner);
		assertFalse(COUNTER.fits(ledger.last().marks()));
		assertFalse(STEP.fits(ledger.last().marks()));
	}

	@Test void expiredCuesCannotBeReplayed() {
		ledger.cue(10, true, owner);
		ledger.cue(10, false, owner);
		ledger.punch(27, 0, 11, owner);
		assertFalse(COUNTER.fits(ledger.last().marks()));
		assertFalse(STEP.fits(ledger.last().marks()));
	}

	@Test void aNewCueAfterAttackCaptureIsNotLostWhenThatPunchArrives() {
		ledger.cue(10, true, owner);
		ledger.attack(11, 0, 11, owner);
		ledger.cue(12, true, owner);
		ledger.punch(12, 0, 11, owner);
		assertTrue(COUNTER.fits(ledger.last().marks()));
		ledger.punch(13, 0, 11, owner);
		assertTrue(COUNTER.fits(ledger.last().marks()), "The newer distinct guard remains available");
		ledger.punch(14, 0, 11, owner);
		assertFalse(COUNTER.fits(ledger.last().marks()));
	}

	@Test void historyIsBoundedAndClockReversalClearsOldEvidence() {
		for (int i = 10; i < 100; i++) ledger.punch(i, FULL.bit(), 1000, owner);
		assertEquals(SwordString.MAX_LENGTH + 2, ledger.size());
		assertEquals(StringRules.MAX_RECOVER, ledger.last().recover());
		ledger.punch(1, LOW.bit(), -5, owner);
		assertEquals(1, ledger.size());
		assertEquals(0, ledger.last().recover());
		assertFalse(FULL.fits(ledger.last().marks()));
	}
}
