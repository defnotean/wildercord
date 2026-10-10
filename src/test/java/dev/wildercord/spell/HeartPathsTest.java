package dev.wildercord.spell;

import dev.wildercord.spell.HeartPaths.Path;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HeartPathsTest {
	@Test
	void savedNumbersAndIdsAreStable() {
		assertNull(HeartPaths.of(0), "an old save walks no path");
		assertNull(HeartPaths.of(-3));
		assertNull(HeartPaths.of(99), "a malformed or future save grants nothing");
		Set<String> ids = new HashSet<>();
		for (Path path : Path.values()) {
			assertSame(path, HeartPaths.of(path.saved()));
			assertSame(path, HeartPaths.byId(path.id));
			assertTrue(ids.add(path.id));
			assertFalse(path.text.isBlank());
		}
		assertEquals(1, Path.STORM.saved(), "saved numbers never move");
		assertEquals(3, Path.WARD.saved());
	}

	@Test
	void aPathIsSilentBelowTheTenthCircle() {
		int storm = Path.STORM.saved();
		assertEquals(CircleVows.Effect.NONE, HeartPaths.effect(storm, HeartPaths.CIRCLE - 1));
		assertEquals(Path.STORM.effect, HeartPaths.effect(storm, HeartPaths.CIRCLE));
		assertEquals(CircleVows.Effect.NONE, HeartPaths.effect(0, Circles.MAX));
		assertEquals(Circles.MANA_SKIN_SHARE, HeartPaths.skinShare(Path.WARD.saved(), 9));
	}

	@Test
	void eachPathHasItsSignature() {
		int storm = Path.STORM.saved(), well = Path.WELL.saved(), ward = Path.WARD.saved();
		assertTrue(HeartPaths.overflowing(storm, 12, 80, 100), "the storm overflows at three-quarters");
		assertFalse(HeartPaths.overflowing(well, 12, 80, 100), "everyone else needs full mana");
		assertTrue(HeartPaths.overflowing(well, 12, 99.6F, 100));
		assertFalse(HeartPaths.overflowing(storm, 9, 80, 100), "a silent storm is no help");
		assertEquals(2F, HeartPaths.meditation(well, 10, 1F));
		assertEquals(1F, HeartPaths.meditation(ward, 10, 1F));
		assertTrue(HeartPaths.skinShare(ward, 10) > Circles.MANA_SKIN_SHARE);
		assertTrue(Path.WELL.effect.mana() > 0 && Path.STORM.effect.mana() < 0, "the storm trades mana for power");
	}

	@Test
	void choosingIsFreeAndLeavingCostsLevels() {
		assertEquals(HeartPaths.Refusal.NOT_REACHED, HeartPaths.take(0, 9));
		assertNull(HeartPaths.take(0, 10));
		assertEquals(HeartPaths.Refusal.ALREADY_WALKING, HeartPaths.take(Path.WELL.saved(), 15));
		assertEquals(HeartPaths.Refusal.NOT_WALKING, HeartPaths.leave(0, 50, false));
		assertEquals(HeartPaths.Refusal.LEVELS, HeartPaths.leave(Path.WELL.saved(), HeartPaths.LEAVE_LEVELS - 1, false));
		assertNull(HeartPaths.leave(Path.WELL.saved(), HeartPaths.LEAVE_LEVELS, false));
		assertNull(HeartPaths.leave(Path.WELL.saved(), 0, true), "creative leaves for free");
		assertNull(HeartPaths.leave(Path.WELL.saved(), HeartPaths.LEAVE_LEVELS, false), "a cracked path can still be left");
	}
}
