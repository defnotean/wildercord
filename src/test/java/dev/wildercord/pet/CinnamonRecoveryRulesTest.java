package dev.wildercord.pet;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CinnamonRecoveryRulesTest {
	private static final UUID A = new UUID(1, 1), B = new UUID(2, 2);
	@Test void exactlyOneLeaseAcrossOwnersAndWorlds() {
		var lease = new CinnamonRecoveryRules();
		assertTrue(lease.acquire(A, 100));
		assertFalse(lease.acquire(B, 100));
		assertFalse(lease.acquire(A, 101));
		assertTrue(lease.heldBy(A)); assertFalse(lease.heldBy(B));
		assertEquals(300, lease.deadline());
	}
	@Test void repeatedRequestsNeverRefreshFixedDeadline() {
		var lease = new CinnamonRecoveryRules();
		lease.acquire(A, 100);
		for (int i = 101; i < 300; i++) assertFalse(lease.acquire(A, i));
		assertFalse(lease.expired(299)); assertTrue(lease.expired(300));
		assertEquals(300, lease.deadline());
	}
	@Test void timeoutRequiresExplicitCleanupBeforeNextOwner() {
		var lease = new CinnamonRecoveryRules();
		lease.acquire(A, 0);
		assertTrue(lease.expired(200)); assertFalse(lease.acquire(B, 200));
		lease.clear(); assertTrue(lease.acquire(B, 200));
		assertTrue(lease.heldBy(B)); assertFalse(lease.heldBy(A));
	}
	@Test void offlineAndRestartCleanupAreIdempotent() {
		var lease = new CinnamonRecoveryRules();
		assertFalse(lease.acquire(null, 0));
		lease.acquire(A, 0); lease.clear(); lease.clear();
		assertFalse(lease.heldBy(A)); assertFalse(lease.expired(1_000));
		assertEquals(0, lease.deadline()); assertTrue(lease.acquire(B, 1_000));
		assertFalse(new CinnamonRecoveryRules().heldBy(B));
	}
}
