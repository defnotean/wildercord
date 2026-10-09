package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CircleVowsTest {
	@Test
	void everyCircleFromTheEighthOnGrantsAChoiceOrACapability() {
		// Lessons and perks that already belong to a circle; every other circle VIII..XX must ask for a vow.
		Set<Integer> granted = new HashSet<>(Set.of(Circles.ARCHMAGE, MasterStudyRules.REWEAVE_CIRCLE, MasterStudyRules.EXCISE_CIRCLE));
		for (LessonPackRules.Lesson lesson : LessonPackRules.ALL) granted.add(lesson.circle);
		for (int circle = 8; circle <= Circles.MAX; circle++)
			assertTrue(granted.contains(circle) ^ CircleVows.at(circle) != null, "Circle " + circle + " grants exactly one kind of new thing");
		for (int circle = 0; circle < 9; circle++) assertNull(CircleVows.at(circle));
	}

	@Test
	void vowIndicesAndIdsAreStableAndUnique() {
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < CircleVows.ALL.size(); i++) {
			CircleVows.Vow vow = CircleVows.ALL.get(i);
			assertEquals(i, vow.index(), "Saved bit positions must never move");
			assertTrue(ids.add(vow.first().id()) && ids.add(vow.second().id()));
			assertNotEquals(vow.first().effect(), vow.second().effect());
			assertFalse(vow.first().text().isBlank() || vow.second().text().isBlank());
		}
		assertTrue(CircleVows.ALL.size() * 2 <= 31, "All choices fit in one saved int");
	}

	@Test
	void oldSavesWithoutVowsHaveNoneAndNoEffect() {
		assertEquals(CircleVows.Effect.NONE, CircleVows.effect(0, Circles.MAX));
		for (CircleVows.Vow vow : CircleVows.ALL) assertEquals(CircleVows.NONE, CircleVows.choice(0, vow));
		assertEquals(CircleVows.ALL.size(), CircleVows.open(0, Circles.MAX).size());
		assertTrue(CircleVows.open(0, 8).isEmpty());
	}

	@Test
	void malformedOrForeignBitsNeverGrantBothSidesOrUnknownVows() {
		assertEquals(0, CircleVows.clean(-1), "All bits set means both sides everywhere: no vow");
		assertEquals(CircleVows.Effect.NONE, CircleVows.effect(-1, Circles.MAX));
		int foreign = 1 << 30;
		assertEquals(0, CircleVows.clean(foreign));
		CircleVows.Vow ix = CircleVows.at(9);
		int saved = CircleVows.with(foreign | 3, ix, CircleVows.SECOND);
		assertEquals(CircleVows.SECOND, CircleVows.choice(saved, ix));
		assertEquals(0, saved & foreign);
	}

	@Test
	void takeIsOneSidePerCircleAndOnlyWhileTheCircleIsActive() {
		CircleVows.Vow xi = CircleVows.at(11);
		assertEquals(CircleVows.Refusal.NOT_REACHED, CircleVows.take(0, 10, 11));
		assertNull(CircleVows.take(0, 11, 11));
		assertEquals(CircleVows.Refusal.NO_VOW, CircleVows.take(0, 20, 10));
		int saved = CircleVows.with(0, xi, CircleVows.FIRST);
		assertEquals(CircleVows.Refusal.ALREADY_TAKEN, CircleVows.take(saved, 20, 11));
		assertEquals(CircleVows.FIRST, CircleVows.choice(xi, "keen_edge"));
		assertEquals(CircleVows.SECOND, CircleVows.choice(xi, "spare_hand"));
		assertEquals(CircleVows.NONE, CircleVows.choice(xi, "wellspring"));
	}

	@Test
	void releaseCostsLevelsUnlessCreativeAndWorksWhileCracked() {
		CircleVows.Vow xv = CircleVows.at(15);
		int saved = CircleVows.with(0, xv, CircleVows.SECOND);
		assertEquals(CircleVows.Refusal.LEVELS, CircleVows.release(saved, 15, CircleVows.RELEASE_LEVELS - 1, false));
		assertNull(CircleVows.release(saved, 15, CircleVows.RELEASE_LEVELS, false));
		assertNull(CircleVows.release(saved, 15, 0, true));
		assertEquals(CircleVows.Refusal.NOT_TAKEN, CircleVows.release(0, 15, 99, false));
		assertEquals(CircleVows.Refusal.NO_VOW, CircleVows.release(saved, 14, 99, false));
		int released = CircleVows.with(saved, xv, CircleVows.NONE);
		assertEquals(CircleVows.NONE, CircleVows.choice(released, xv));
		assertNull(CircleVows.take(released, 15, 15), "After a release the circle can be chosen again");
	}

	@Test
	void effectsCountOnlyActiveCirclesAndStayBounded() {
		CircleVows.Vow ix = CircleVows.at(9), xix = CircleVows.at(19);
		int saved = CircleVows.with(CircleVows.with(0, ix, CircleVows.FIRST), xix, CircleVows.FIRST);
		assertEquals(30, CircleVows.effect(saved, 18).mana(), "A cracked or unformed XIX is silent");
		assertEquals(90, CircleVows.effect(saved, 20).mana());
		assertEquals(-.5F, CircleVows.effect(saved, 20).regen(), 1e-6);
		assertEquals(0, CircleVows.effect(saved, 8).mana());
		// The strongest and weakest stack of choices stays a modest change.
		double maxPower = 1, minCost = 1, minCooldown = 1, maxDuration = 1;
		for (CircleVows.Vow vow : CircleVows.ALL) {
			maxPower *= Math.max(vow.first().effect().power(), vow.second().effect().power());
			minCost *= Math.min(vow.first().effect().cost(), vow.second().effect().cost());
			minCooldown *= Math.min(vow.first().effect().cooldown(), vow.second().effect().cooldown());
			maxDuration *= Math.max(vow.first().effect().duration(), vow.second().effect().duration());
		}
		assertTrue(maxPower < 1.35 && minCost > .7 && minCooldown > .75 && maxDuration < 1.5,
			maxPower + " " + minCost + " " + minCooldown + " " + maxDuration);
	}
}
