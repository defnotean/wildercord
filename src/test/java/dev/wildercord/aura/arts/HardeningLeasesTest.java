package dev.wildercord.aura.arts;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.wildercord.aura.arts.HardeningLeases.Source.*;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated production-lease tests. Native effect merging and Fabric event dispatch still need game-test coverage. */
class HardeningLeasesTest {
	private static final class World { long now = 100; }
	private static final class Body {
		final UUID id;
		World world;
		boolean alive = true, removed;
		Body(UUID id, World world) { this.id = id; this.world = world; }
		// Deliberately equate UUID replacements: the production coordinator must still use exact object identity.
		@Override public boolean equals(Object other) { return other instanceof Body body && id.equals(body.id); }
		@Override public int hashCode() { return id.hashCode(); }
	}
	private static final class Fixture {
		final HardeningLeases<Body> leases = new HardeningLeases<>();
		final Map<UUID, Body> connected = new HashMap<>();
		final World world = new World();
		final Body player = connect(new Body(UUID.randomUUID(), world));
		Body connect(Body body) { connected.put(body.id, body); return body; }
		HardeningLeases.Lease<Body> prepare(Body body, HardeningLeases.Source source, int ticks) {
			World acceptedWorld = body.world;
			return leases.receipt(body, acceptedWorld, source, acceptedWorld.now, ticks,
				() -> body.alive && !body.removed && body.world == acceptedWorld && connected.get(body.id) == body,
				() -> acceptedWorld.now);
		}
		HardeningLeases.Lease<Body> grant(HardeningLeases.Source source, int ticks) {
			var lease = prepare(player, source, ticks);
			assertTrue(leases.grant(lease));
			return lease;
		}
	}

	@Test void preparingAndInspectingAReceiptHasNoGrantSideEffects() {
		var f = new Fixture();
		var lease = f.prepare(f.player, UNMOVED, 80);
		assertFalse(f.leases.active(lease));
		assertFalse(f.leases.active(f.player));
		assertTrue(f.leases.bodies().isEmpty());
		assertTrue(f.leases.grant(lease));
		assertTrue(f.leases.active(lease));
	}

	@Test void mountainThenUnmovedKeepIndependentDeadlines() {
		var f = new Fixture();
		var mountain = f.grant(MOUNTAIN, 25);
		f.world.now = 110;
		var unmoved = f.grant(UNMOVED, 80);
		f.world.now = 126;
		assertFalse(f.leases.active(mountain));
		assertTrue(f.leases.active(unmoved));
		assertTrue(f.leases.active(f.player));
		assertEquals(1, f.leases.bodies().size());
		f.world.now = 190;
		assertTrue(f.leases.active(unmoved), "Original harden convention includes acceptedAt + ticks");
		f.world.now = 191;
		assertFalse(f.leases.active(f.player));
		assertTrue(f.leases.bodies().isEmpty());
	}

	@Test void unmovedThenMountainCannotShortenUnmoved() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		f.world.now = 110;
		var mountain = f.grant(MOUNTAIN, 25);
		f.world.now = 135;
		assertTrue(f.leases.active(mountain));
		f.world.now = 136;
		assertFalse(f.leases.active(mountain));
		assertTrue(f.leases.active(unmoved));
		assertTrue(f.leases.active(f.player));
	}

	@Test void mountainRefreshOutlivesUnmovedWithoutRefreshingIt() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		var firstMountain = f.grant(MOUNTAIN, 25);
		f.world.now = 170;
		var refreshedMountain = f.grant(MOUNTAIN, 25);
		assertFalse(f.leases.active(firstMountain));
		f.world.now = 180;
		assertTrue(f.leases.active(unmoved));
		f.world.now = 181;
		assertFalse(f.leases.active(unmoved));
		assertTrue(f.leases.active(refreshedMountain));
		assertTrue(f.leases.active(f.player));
		f.world.now = 196;
		assertFalse(f.leases.active(f.player));
	}

	@Test void retiringEitherSourceDoesNotRemoveTheOther() {
		for (var removed : HardeningLeases.Source.values()) {
			var f = new Fixture();
			var unmoved = f.grant(UNMOVED, 80);
			var mountain = f.grant(MOUNTAIN, 25);
			f.leases.retire(removed == UNMOVED ? unmoved : mountain);
			assertTrue(f.leases.active(removed == UNMOVED ? mountain : unmoved));
			assertTrue(f.leases.active(f.player));
			f.leases.retire(removed == UNMOVED ? mountain : unmoved);
			assertFalse(f.leases.active(f.player));
		}
	}

	@Test void repeatedGrantNeverRefreshesOrReactivatesTheReceipt() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		f.world.now = 160;
		assertFalse(f.leases.grant(unmoved));
		f.world.now = 181;
		assertFalse(f.leases.active(unmoved));
		f.world.now = 100;
		assertFalse(f.leases.grant(unmoved));
		assertFalse(f.leases.active(unmoved));
	}

	@Test void staleAndExplicitlyRetiredPreparedReceiptsCannotBeGrantedLater() {
		var f = new Fixture();
		var stale = f.prepare(f.player, UNMOVED, 80);
		f.world.now = 181;
		assertFalse(f.leases.grant(stale));
		f.world.now = 100;
		assertFalse(f.leases.grant(stale));
		var retired = f.prepare(f.player, UNMOVED, 80);
		f.leases.retire(retired);
		assertFalse(f.leases.grant(retired));
	}

	@Test void oldTokenCleanupCannotRemoveANewerGrantOfTheSameSource() {
		var f = new Fixture();
		var old = f.grant(UNMOVED, 80);
		f.world.now = 110;
		var current = f.grant(UNMOVED, 80);
		f.leases.retire(old);
		assertFalse(f.leases.active(old));
		assertTrue(f.leases.active(current));
		assertFalse(f.leases.grant(old));
		assertTrue(f.leases.active(f.player));
	}

	@Test void sourceRefreshMayShortenOnlyItsOwnLease() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		var previous = f.grant(MOUNTAIN, 100);
		f.world.now = 110;
		var shortened = f.grant(MOUNTAIN, 25);
		assertFalse(f.leases.active(previous));
		f.world.now = 136;
		assertFalse(f.leases.active(shortened));
		assertTrue(f.leases.active(unmoved));
	}

	@Test void deathDisconnectAndRemovalPermanentlyRetireBothSources() {
		for (int lifecycle = 0; lifecycle < 3; lifecycle++) {
			var f = new Fixture();
			var unmoved = f.grant(UNMOVED, 80);
			var mountain = f.grant(MOUNTAIN, 25);
			if (lifecycle == 0) f.player.alive = false;
			if (lifecycle == 1) f.connected.remove(f.player.id);
			if (lifecycle == 2) f.player.removed = true;
			assertFalse(f.leases.active(f.player));
			f.player.alive = true; f.player.removed = false; f.connect(f.player);
			assertFalse(f.leases.active(unmoved));
			assertFalse(f.leases.active(mountain));
			assertFalse(f.leases.grant(unmoved));
		}
	}

	@Test void sameUuidRespawnNeverInheritsOrLosesAReplacementLease() {
		var f = new Fixture();
		var oldUnmoved = f.grant(UNMOVED, 80);
		var oldMountain = f.grant(MOUNTAIN, 25);
		Body replacement = f.connect(new Body(f.player.id, f.world));
		assertFalse(f.leases.active(replacement));
		var fresh = f.prepare(replacement, UNMOVED, 80);
		assertTrue(f.leases.grant(fresh));
		f.leases.retire(f.player, null);
		assertFalse(f.leases.active(oldUnmoved));
		assertFalse(f.leases.active(oldMountain));
		assertTrue(f.leases.active(fresh));
		assertTrue(f.leases.active(replacement));
		assertEquals(1, f.leases.bodies().size());
		assertSame(replacement, f.leases.bodies().getFirst());
	}

	@Test void worldDepartureAndReturnCannotReviveAnOldLease() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		var mountain = f.grant(MOUNTAIN, 25);
		f.player.world = new World();
		f.leases.retire(f.player, f.world);
		f.player.world = f.world;
		assertFalse(f.leases.active(unmoved));
		assertFalse(f.leases.active(mountain));
		assertFalse(f.leases.active(f.player));
	}

	@Test void lateOldWorldCallbackPreservesNewDestinationLease() {
		var f = new Fixture();
		var old = f.grant(UNMOVED, 80);
		f.player.world = new World();
		var destination = f.grant(MOUNTAIN, 25);
		f.leases.retire(f.player, f.world);
		assertFalse(f.leases.active(old));
		assertTrue(f.leases.active(destination));
		assertTrue(f.leases.active(f.player));
	}

	@Test void observedWorldMismatchAndReversedClockRetireRatherThanPause() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		f.player.world = new World();
		assertFalse(f.leases.active(unmoved));
		f.player.world = f.world;
		assertFalse(f.leases.active(unmoved));
		var mountain = f.grant(MOUNTAIN, 25);
		f.world.now = 99;
		assertFalse(f.leases.active(mountain));
		f.world.now = 100;
		assertFalse(f.leases.active(mountain));
	}

	@Test void clearingRetiresTokensBeforeDroppingBodyEntries() {
		var f = new Fixture();
		var unmoved = f.grant(UNMOVED, 80);
		var mountain = f.grant(MOUNTAIN, 25);
		f.leases.clear();
		assertTrue(f.leases.bodies().isEmpty());
		assertFalse(f.leases.active(unmoved));
		assertFalse(f.leases.active(mountain));
		assertFalse(f.leases.grant(unmoved));
	}
}
