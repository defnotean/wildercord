package dev.wildercord.world;

import dev.wildercord.world.ResidueLedger.Caps;
import dev.wildercord.world.ResidueLedger.Entry;
import dev.wildercord.world.ResidueLedger.Refusal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The record of a dimension's residues: its caps, and a decay schedule that never looks at every residue. */
class ResidueLedgerTest {
	private static Entry<String> at(int x, int y, int z, long due, String owner) {
		return new Entry<>(x, y, z, "everfrost", 0, due, 0, owner, "data");
	}

	@Test
	void positionsAndChunksPackAsTheGameDoes() {
		for (int[] p : new int[][] {{0, 0, 0}, {-1, -64, -1}, {12345, 300, -987654}, {-29999999, 2000, 29999999}, {17, -1, 33}}) {
			assertEquals(BlockPos.asLong(p[0], p[1], p[2]), ResidueLedger.pack(p[0], p[1], p[2]));
			assertEquals(ChunkPos.pack(new BlockPos(p[0], p[1], p[2])), ResidueLedger.chunkOf(p[0], p[2]));
		}
	}

	@Test
	void aChunkHoldsOnlySoMany() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		Caps caps = new Caps(3, 99, 4, 999, 999);
		// Far apart within one chunk (0..15), so the area cap stays out of it.
		ledger.add(at(0, 64, 0, 100, ""));
		ledger.add(at(15, 64, 0, 100, ""));
		ledger.add(at(0, 64, 15, 100, ""));
		assertEquals(3, ledger.inChunk(ResidueLedger.chunkOf(0, 0)));
		assertEquals(Refusal.CHUNK_FULL, ledger.admit(15, 64, 15, "", caps));
		assertNull(ledger.admit(16, 64, 0, "", caps), "the next chunk has room");
		assertEquals(Refusal.TAKEN, ledger.admit(0, 64, 0, "", caps));
	}

	@Test
	void oneSpotHoldsOnlySoManyAcrossChunkBorders() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		Caps caps = new Caps(99, 3, 4, 999, 999);
		// Straddling the corner where four chunks meet.
		ledger.add(at(-1, 64, -1, 100, ""));
		ledger.add(at(0, 64, -1, 100, ""));
		ledger.add(at(-1, 64, 0, 100, ""));
		assertEquals(3, ledger.near(0, 64, 0, 4));
		assertEquals(Refusal.AREA_FULL, ledger.admit(1, 65, 1, "", caps));
		assertNull(ledger.admit(6, 64, 6, "", caps), "five blocks off is another area");
		assertNull(ledger.admit(0, 70, 0, "", caps), "and so is six blocks up");
	}

	@Test
	void aDimensionAndACasterHoldOnlySoMany() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		Caps caps = new Caps(99, 99, 4, 3, 2);
		ledger.add(at(0, 64, 0, 100, "alice"));
		ledger.add(at(100, 64, 0, 100, "alice"));
		assertEquals(2, ledger.owned("alice"));
		assertEquals(Refusal.OWNER_FULL, ledger.admit(200, 64, 0, "alice", caps));
		assertNull(ledger.admit(200, 64, 0, "bob", caps));
		assertNull(ledger.admit(200, 64, 0, "", caps), "the world's own (a boss's) aren't anyone's");
		ledger.add(at(300, 64, 0, 100, ""));
		assertEquals(Refusal.DIMENSION_FULL, ledger.admit(400, 64, 0, "bob", caps));
		ledger.remove(ResidueLedger.pack(0, 64, 0));
		assertEquals(1, ledger.owned("alice"), "a residue gone frees its caster's room");
		assertNull(ledger.admit(400, 64, 0, "alice", caps));
	}

	@Test
	void residuesFadeInTheOrderTheyreDueAndNoneEarly() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		ledger.add(at(1, 64, 0, 300, ""));
		ledger.add(at(2, 64, 0, 100, ""));
		ledger.add(at(3, 64, 0, 200, ""));
		ledger.add(at(4, 64, 0, 100, ""));
		assertEquals(100, ledger.nextDue());
		assertTrue(ledger.takeDue(99, 10).isEmpty(), "nothing fades before its time");
		List<Entry<String>> due = ledger.takeDue(200, 10);
		assertEquals(List.of(2, 4, 3), due.stream().map(Entry::x).toList(), "soonest first");
		assertEquals(4, ledger.size(), "taken, but still recorded until faded");
		due.forEach(e -> ledger.remove(e.pos()));
		assertEquals(1, ledger.size());
		assertEquals(300, ledger.nextDue());
		// A batch never runs away with the tick: at most so many at once, the rest next time.
		for (int i = 0; i < 100; i++) {
			ledger.add(at(10 + i, 64, 0, 400, ""));
		}
		assertEquals(16, ledger.takeDue(1000, 16).size());
		assertEquals(16, ledger.takeDue(1000, 16).size());
	}

	@Test
	void oneWhoseChunkIsntLoadedWaitsForIt() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		Entry<String> far = at(1000, 64, 1000, 100, "alice");
		ledger.add(far);
		Entry<String> taken = ledger.takeDue(150, 10).getFirst();
		ledger.park(taken);
		assertTrue(ledger.hasParked(far.chunk()));
		assertTrue(ledger.takeDue(10_000, 10).isEmpty(), "parked residues aren't due again: they wait for their chunk");
		assertEquals(1, ledger.size(), "and still count against the caps");
		assertEquals(1, ledger.owned("alice"));
		assertTrue(ledger.unpark(ResidueLedger.chunkOf(0, 0)).isEmpty());
		List<Entry<String>> back = ledger.unpark(far.chunk());
		assertEquals(List.of(far), back);
		assertFalse(ledger.hasParked(far.chunk()));
		ledger.remove(far.pos());
		assertTrue(ledger.isEmpty());
		assertEquals(0, ledger.owned("alice"));
	}

	@Test
	void returningChunkDrainsOnlyItsSweepBudget() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		for (int x = 0; x < 5; x++) {
			ledger.add(at(x, 64, 0, 100, "alice"));
		}
		ledger.takeDue(150, 10).forEach(ledger::park);
		long chunk = ResidueLedger.chunkOf(0, 0);
		assertTrue(ledger.unpark(chunk, 0).isEmpty());
		assertTrue(ledger.hasParked(chunk));
		for (int expected : new int[] {2, 2, 1}) {
			List<Entry<String>> batch = ledger.unpark(chunk, 2);
			assertEquals(expected, batch.size());
			assertTrue(ledger.takeDue(10_000, 10).isEmpty(), "remaining parked entries stay off the due schedule");
			batch.forEach(e -> ledger.remove(e.pos()));
			assertEquals(!ledger.isEmpty(), ledger.hasParked(chunk));
			assertEquals(ledger.size(), ledger.owned("alice"));
		}
		assertTrue(ledger.isEmpty());
		assertTrue(ledger.unpark(chunk, 2).isEmpty());
	}

	@Test
	void removedOrReplacedParkedEntriesNeverReturn() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		Entry<String> removed = at(0, 64, 0, 100, "alice");
		Entry<String> replaced = at(1, 64, 0, 100, "alice");
		Entry<String> waiting = at(2, 64, 0, 100, "alice");
		List.of(removed, replaced, waiting).forEach(ledger::add);
		ledger.takeDue(150, 10).forEach(ledger::park);
		ledger.remove(removed.pos());
		Entry<String> replacement = at(1, 64, 0, 1000, "bob");
		ledger.add(replacement);
		ledger.park(replaced); // A stale caller cannot park the replacement.
		assertEquals(List.of(waiting), ledger.unpark(waiting.chunk(), 1));
		assertFalse(ledger.hasParked(waiting.chunk()));
		assertTrue(ledger.unpark(waiting.chunk(), 1).isEmpty());
		assertTrue(ledger.takeDue(999, 10).isEmpty());
		assertEquals(List.of(replacement), ledger.takeDue(1000, 10));
	}

	@Test
	void timeCanBeMovedOnForEveryResidue() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		ledger.add(new Entry<>(0, 64, 0, "void_scar", 1000, 7000, 0, "", "d"));
		ledger.add(at(1, 64, 0, 30000, ""));
		ledger.shift(6500);
		Entry<String> scar = ledger.get(0, 64, 0);
		assertEquals(500, scar.due());
		assertEquals(-5500, scar.placedAt(), "its age moves on with it, so it shows its stage");
		assertEquals(1, ledger.takeDue(1000, 10).size());
	}

	@Test
	void recordingAgainReplacesAndRemovingCleansEveryIndex() {
		ResidueLedger<String> ledger = new ResidueLedger<>();
		ledger.add(at(5, 64, 5, 100, "alice"));
		ledger.add(at(5, 64, 5, 900, "bob"));
		assertEquals(1, ledger.size());
		assertEquals(0, ledger.owned("alice"));
		assertEquals(1, ledger.owned("bob"));
		assertEquals(900, ledger.nextDue());
		assertNull(ledger.remove(ResidueLedger.pack(9, 9, 9)));
		assertNotNull(ledger.remove(ResidueLedger.pack(5, 64, 5)));
		assertEquals(Long.MAX_VALUE, ledger.nextDue());
		assertEquals(0, ledger.inChunk(ResidueLedger.chunkOf(5, 5)));
		assertTrue(ledger.all().isEmpty());
	}
}
