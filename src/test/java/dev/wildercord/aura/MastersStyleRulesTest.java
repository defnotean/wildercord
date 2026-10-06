package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class MastersStyleRulesTest {
	@Test
	void authoredArtsHaveStableUniqueCosmeticIds() {
		assertEquals(14, MastersStyleRules.STYLES.size());
		var ids = new HashSet<Integer>();
		var names = new HashSet<String>();
		for (var style : MastersStyleRules.STYLES) {
			assertTrue(ids.add(style.animation()));
			assertTrue(names.add(style.art()));
			assertEquals(style.animation() <= 12 ? 0 : 1, ArtRules.art(style.art()).slot());
			assertEquals(style.animation() == 15 ? MastersStyleRules.TargetPolicy.HAILFALL_RECEIPT
				: style.animation() == 16 ? MastersStyleRules.TargetPolicy.SKYFALL_RECEIPT
				: MastersStyleRules.TargetPolicy.ACTIVE_CONE, style.targets());
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
	void firstFormIdsAndTimingsStayUnchangedAndSecondFormsKeepTheirOriginalRules() {
		String[] first = {"kindling_draw", "frostbite", "crackle", "cutting_breeze", "rockbreaker", "thorn_lash",
			"void_cut", "star_needle", "echo_cut", "bloodletting"};
		int[] windups = {6, 6, 4, 4, 10, 6, 6, 4, 6, 6};
		int[] recoveries = {12, 12, 12, 10, 18, 12, 12, 10, 20, 12};
		for (int i = 0; i < first.length; i++) {
			var style = MastersStyleRules.of(first[i]);
			assertEquals(i + 3, style.animation());
			assertEquals(windups[i], style.windup());
			assertEquals(recoveries[i], style.recovery());
		}
		for (String art : new String[] {"rising_cinders", "blossom_fall"}) {
			var style = MastersStyleRules.of(art);
			assertEquals(art.equals("rising_cinders") ? 13 : 14, style.animation());
			assertEquals(8, style.windup());
			assertEquals(art.equals("rising_cinders") ? 16 : 18, style.recovery());
			assertEquals(8, ArtRules.art(art).cost());
			assertEquals(80, ArtRules.art(art).cooldown());
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
		}
		assertEquals(12, ArtRules.CINDERS_RAIN_DELAY);
		assertEquals(80, ArtRules.BLOSSOM_TICKS);
	}

	@Test
	void profilesMustExplicitlyChooseWhetherTheObservedStringVictimIsRetained() {
		assertThrows(NullPointerException.class, () -> new MastersStyleRules.Style(17, "counter_fixture", 6, 12, null));
		var counter = new MastersStyleRules.Style(17, "counter_fixture", 6, 12, MastersStyleRules.TargetPolicy.STRING_TARGET);
		assertEquals(MastersStyleRules.TargetPolicy.STRING_TARGET, counter.targets());
		assertNull(MastersStyleRules.of(counter.art()), "A policy fixture does not expand the shipping catalog");
	}

	@Test
	void targetBearingSecondFormsReserveOnlyCosmeticIdsAndKeepOriginalArtValues() {
		for (String art : new String[] {"hailfall", "skyfall"}) {
			var style = MastersStyleRules.of(art);
			assertEquals(art.equals("hailfall") ? 15 : 16, style.animation());
			assertEquals(art.equals("hailfall") ? 8 : 6, style.windup());
			assertEquals(16, style.recovery());
			assertEquals(8, ArtRules.art(art).cost());
			assertEquals(80, ArtRules.art(art).cooldown());
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), -90));
			assertNull(MastersArtRules.move(style.animation()));
		}
		assertEquals(7, ArtRules.HAIL_STONES);
		assertEquals(3, ArtRules.HAIL_PER_FOE);
		assertEquals(6, ArtRules.SKYFALL_DELAY);
		assertEquals(.5, ArtRules.HAIL_CUT_FACTOR);
		assertEquals(.28, ArtRules.HAIL_STONE_FACTOR);
		assertEquals(.95, ArtRules.SKYFALL_FACTOR);
		assertEquals(.4, ArtRules.SKYFALL_ARC_FACTOR);
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
