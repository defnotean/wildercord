package dev.wildercord.travel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TravelRulesTest {
	private static final UUID ALEX = UUID.randomUUID();
	private static final UUID SAM = UUID.randomUUID();
	private static final UUID KAI = UUID.randomUUID();

	@Test
	void namesAreTrimmedLowerCasedAndPlain() {
		assertEquals("base", TravelRules.name("  Base "));
		assertEquals("mine_2", TravelRules.name("Mine_2"));
		assertEquals("north-tower", TravelRules.name("north-tower"));
		assertNull(TravelRules.name(""));
		assertNull(TravelRules.name("   "));
		assertNull(TravelRules.name(null));
		assertNull(TravelRules.name("my base"));
		assertNull(TravelRules.name("café"));
		assertNull(TravelRules.name("a.b"));
		assertNull(TravelRules.name("x".repeat(TravelRules.MAX_NAME + 1)));
		assertEquals("x".repeat(TravelRules.MAX_NAME), TravelRules.name("x".repeat(TravelRules.MAX_NAME)));
	}

	@Test
	void homesFillUpButCanAlwaysBeMoved() {
		assertEquals(TravelRules.HomeCheck.NEW, TravelRules.setHome(Set.of(), "home", 3));
		assertEquals(TravelRules.HomeCheck.NEW, TravelRules.setHome(Set.of("a", "b"), "c", 3));
		assertEquals(TravelRules.HomeCheck.FULL, TravelRules.setHome(Set.of("a", "b", "c"), "d", 3));
		assertEquals(TravelRules.HomeCheck.REPLACE, TravelRules.setHome(Set.of("a", "b", "c"), "b", 3));
		// With homes switched off (0), none can be set.
		assertEquals(TravelRules.HomeCheck.FULL, TravelRules.setHome(Set.of(), "home", 0));
	}

	@Test
	void theUsualHomeIsHomeOrTheOnlyOne() {
		assertEquals("home", TravelRules.defaultHome(Set.of("home", "base")));
		assertEquals("base", TravelRules.defaultHome(Set.of("base")));
		assertNull(TravelRules.defaultHome(Set.of("base", "mine")));
		assertNull(TravelRules.defaultHome(Set.of()));
	}

	@Test
	void halfABlockOfDriftIsAllowed() {
		assertFalse(TravelRules.moved(0, 0, 0));
		assertFalse(TravelRules.moved(0.3, 0, 0.3));
		assertFalse(TravelRules.moved(0, 0.5, 0));
		assertTrue(TravelRules.moved(0.4, 0, 0.4));
		assertTrue(TravelRules.moved(0, -0.6, 0));
	}

	@Test
	void cooldownsCountDownInWholeSeconds() {
		assertEquals(0, TravelRules.remaining(100, 50));
		assertEquals(40, TravelRules.remaining(60, 100));
		assertEquals(0, TravelRules.seconds(0));
		assertEquals(1, TravelRules.seconds(1));
		assertEquals(1, TravelRules.seconds(20));
		assertEquals(2, TravelRules.seconds(21));
		assertEquals(30, TravelRules.seconds(600));
	}

	@Test
	void theArrowTurnsTowardTheWaypoint() {
		// Facing south (yaw 0): straight ahead is south, west is on the right, east on the left.
		assertEquals(0, TravelRules.bearing(0, 10, 0), 1e-9);
		assertEquals(90, TravelRules.bearing(-10, 0, 0), 1e-9);
		assertEquals(-90, TravelRules.bearing(10, 0, 0), 1e-9);
		assertEquals(180, Math.abs(TravelRules.bearing(0, -10, 0)), 1e-9);
		// Facing west (yaw 90): a point to the west is straight ahead, one to the south on the left.
		assertEquals(0, TravelRules.bearing(-10, 0, 90), 1e-9);
		assertEquals(-90, TravelRules.bearing(0, 10, 90), 1e-9);
		// Any number of turns round: the same answer.
		assertEquals(TravelRules.bearing(3, 7, 30), TravelRules.bearing(3, 7, 30 + 720), 1e-9);
		double b = TravelRules.bearing(5, -2, -1234.5);
		assertTrue(b >= -180 && b < 180, "wrapped: " + b);
	}

	@Test
	void randomSpotsStayWithinTheRadius() {
		for (int i = 0; i < 200; i++) {
			double u = (i * 0.618034) % 1.0;
			double v = (i * 0.414214) % 1.0;
			int[] p = TravelRules.randomPoint(u, v, 100, -50, 5000);
			double d = Math.hypot(p[0] - 100, p[1] + 50);
			assertTrue(d <= 5001, "within the radius: " + d);
		}
		int[] centre = TravelRules.randomPoint(0, 0, 7, 9, 5000);
		assertArrayEquals(new int[] {7, 9}, centre);
	}

	// ------------------------------------------------------------------ requests

	@Test
	void aNewRequestToTheSamePersonReplacesTheOld() {
		TravelRules.Requests book = new TravelRules.Requests();
		assertNull(book.add(new TravelRules.Request(ALEX, SAM, false, 0)));
		TravelRules.Request replaced = book.add(new TravelRules.Request(ALEX, SAM, true, 50));
		assertNotNull(replaced);
		assertFalse(replaced.here());
		assertEquals(1, book.waitingFor(SAM).size());
		assertTrue(book.waitingFor(SAM).getFirst().here());
		// A request to someone else is kept alongside.
		assertNull(book.add(new TravelRules.Request(ALEX, KAI, false, 60)));
		assertEquals(2, book.sentBy(ALEX).size());
	}

	@Test
	void answeringTakesTheNamedRequestOrTheNewest() {
		TravelRules.Requests book = new TravelRules.Requests();
		book.add(new TravelRules.Request(ALEX, SAM, false, 0));
		book.add(new TravelRules.Request(KAI, SAM, false, 10));
		assertEquals(List.of(KAI, ALEX), book.waitingFor(SAM).stream().map(TravelRules.Request::from).toList());
		assertEquals(ALEX, book.take(SAM, ALEX).from());
		assertNull(book.take(SAM, ALEX), "already answered");
		assertEquals(KAI, book.take(SAM, null).from());
		assertNull(book.take(SAM, null));
		assertTrue(book.isEmpty());
	}

	@Test
	void requestsRunOutAfterTheTimeout() {
		TravelRules.Requests book = new TravelRules.Requests();
		book.add(new TravelRules.Request(ALEX, SAM, false, 0));
		book.add(new TravelRules.Request(KAI, SAM, false, 600));
		assertTrue(book.expire(1199, 1200).isEmpty());
		List<TravelRules.Request> expired = book.expire(1200, 1200);
		assertEquals(1, expired.size());
		assertEquals(ALEX, expired.getFirst().from());
		assertEquals(1, book.waitingFor(SAM).size());
		assertFalse(new TravelRules.Request(ALEX, SAM, false, 100).expired(1299, 1200));
		assertTrue(new TravelRules.Request(ALEX, SAM, false, 100).expired(1300, 1200));
	}

	@Test
	void cancellingLeavingAndTurningAwayDropRequests() {
		TravelRules.Requests book = new TravelRules.Requests();
		book.add(new TravelRules.Request(ALEX, SAM, false, 0));
		book.add(new TravelRules.Request(ALEX, KAI, true, 0));
		book.add(new TravelRules.Request(KAI, SAM, false, 0));
		assertEquals(2, book.cancel(ALEX).size());
		assertEquals(1, book.waitingFor(SAM).size());
		book.add(new TravelRules.Request(SAM, KAI, false, 0));
		assertEquals(2, book.forget(KAI).size(), "both the one Kai sent and the one sent to Kai");
		assertTrue(book.isEmpty());
		book.add(new TravelRules.Request(ALEX, SAM, false, 0));
		book.add(new TravelRules.Request(KAI, SAM, false, 0));
		book.add(new TravelRules.Request(SAM, ALEX, false, 0));
		assertEquals(2, book.refuseAll(SAM).size());
		assertEquals(1, book.sentBy(SAM).size(), "Sam's own request is still out");
	}

	@Test
	void whoTravelsDependsOnTheKindOfRequest() {
		TravelRules.Request tpa = new TravelRules.Request(ALEX, SAM, false, 0);
		assertEquals(ALEX, tpa.traveller());
		assertEquals(SAM, tpa.host());
		TravelRules.Request here = new TravelRules.Request(ALEX, SAM, true, 0);
		assertEquals(SAM, here.traveller());
		assertEquals(ALEX, here.host());
	}
}
