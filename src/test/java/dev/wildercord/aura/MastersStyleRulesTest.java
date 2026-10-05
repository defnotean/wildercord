package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class MastersStyleRulesTest {
	@Test
	void tenExistingFirstArtsHaveStableUniqueCosmeticIds() {
		assertEquals(10, MastersStyleRules.STYLES.size());
		var ids = new HashSet<Integer>();
		var names = new HashSet<String>();
		for (var style : MastersStyleRules.STYLES) {
			assertTrue(ids.add(style.animation()));
			assertTrue(names.add(style.art()));
			assertEquals(0, ArtRules.art(style.art()).slot());
			assertSame(style, MastersStyleRules.of(style.art()));
			assertSame(style, MastersStyleRules.animation(style.animation()));
			assertNull(MastersArtRules.move(style.animation()), "A cosmetic style id cannot become a free key action");
			assertTrue(style.windup() >= 4 && style.windup() <= 10);
			assertTrue(style.recovery() >= 10 && style.recovery() <= 20);
		}
		assertNull(MastersStyleRules.of("sunfall"));
		assertNull(MastersStyleRules.animation(Integer.MAX_VALUE));
	}

	@Test
	void multiHitChoreographyFitsWithinCommittedRecovery() {
		assertTrue(MastersStyleRules.of("crackle").recovery() > (ArtRules.CRACKLE_CUTS - 1) * ArtRules.CRACKLE_GAP);
		assertTrue(MastersStyleRules.of("echo_cut").recovery() > ArtRules.ECHO_DELAY);
	}
	@Test
	void transmittedAttackPitchMatchesEachRealAttackPlane() {
		assertEquals(0, MastersStyleRules.attackPitch(0, 75));
		assertEquals(0, MastersStyleRules.attackPitch(3, -75));
		assertEquals(Math.toDegrees(Math.atan(.4)), MastersStyleRules.attackPitch(6, 45), .00001);
		assertEquals(-Math.toDegrees(Math.atan(.35)), MastersStyleRules.attackPitch(10, -45), .00001);
		assertEquals(90, MastersStyleRules.attackPitch(6, 90), .00001);
		assertEquals(-90, MastersStyleRules.attackPitch(10, -90), .00001);
		assertEquals(0, MastersStyleRules.attackPitch(10, Float.NaN));
	}

}
