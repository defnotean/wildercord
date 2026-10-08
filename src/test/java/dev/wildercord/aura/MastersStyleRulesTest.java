package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class MastersStyleRulesTest {
	@Test
	void authoredArtsHaveStableUniqueCosmeticIds() {
		assertEquals(41, MastersStyleRules.STYLES.size());
		var ids = new HashSet<Integer>();
		var names = new HashSet<String>();
		for (var style : MastersStyleRules.STYLES) {
			assertTrue(ids.add(style.animation()));
			assertTrue(names.add(style.art()));
			assertEquals(style.animation() <= 12 ? 0 : style.animation() == 19 ? 4 : (style.animation() >= 20 && style.animation() <= 29) ? 2 : (style.animation() >= 34 && style.animation() <= 43) ? 3 : 1, ArtRules.art(style.art()).slot());
			assertEquals(style.animation() == 15 ? MastersStyleRules.TargetPolicy.HAILFALL_RECEIPT
				: style.animation() == 16 ? MastersStyleRules.TargetPolicy.SKYFALL_RECEIPT
				: style.animation() == 17 || style.animation() == 18 || style.animation() == 31 || style.animation() == 32 ? MastersStyleRules.TargetPolicy.GROUND_AHEAD
				: style.animation() >= 20 && style.animation() <= 29 ? MastersStyleRules.TargetPolicy.EARNED_COUNTER
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
		assertThrows(NullPointerException.class, () -> new MastersStyleRules.Style(20, "counter_fixture", 6, 12, null));
		var counter = new MastersStyleRules.Style(20, "counter_fixture", 6, 12, MastersStyleRules.TargetPolicy.STRING_TARGET);
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
	void untargetedGroundFieldsUseTheirOwnExplicitPolicyAndOriginalValues() {
		for (String art : new String[] {"collapse", "red_rain"}) {
			var style = MastersStyleRules.of(art);
			assertEquals(art.equals("collapse") ? 17 : 18, style.animation());
			assertEquals(8, style.windup());
			assertEquals(art.equals("collapse") ? 18 : 16, style.recovery());
			assertEquals(MastersStyleRules.TargetPolicy.GROUND_AHEAD, style.targets());
			assertEquals(8, ArtRules.art(art).cost());
			assertEquals(80, ArtRules.art(art).cooldown());
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
			assertNull(MastersArtRules.move(style.animation()));
		}
		assertEquals(16, ArtRules.COLLAPSE_TICKS);
		assertEquals(2.5, ArtRules.COLLAPSE_AHEAD);
		assertEquals(4.5, ArtRules.COLLAPSE_PULL);
		assertEquals(2.2, ArtRules.COLLAPSE_RADIUS);
		assertEquals(40, ArtRules.RAIN_TICKS);
		assertEquals(10, ArtRules.BLEED_PERIOD);
		assertEquals(2, ArtRules.RAIN_AHEAD);
		assertEquals(3.5, ArtRules.RAIN_RADIUS);
		assertEquals(.30, ArtRules.RAIN_DRINK);
		assertEquals(4, ArtRules.RAIN_DRINK_MAX);
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

	@Test void earnedCountersAppendWithoutChangingTheirOriginalArtOrInput() {
		for (String id : new String[] {"backdraft", "rooted_parry"}) {
			var style = MastersStyleRules.of(id); var art = ArtRules.art(id);
			assertEquals(id.equals("backdraft") ? 20 : 21, style.animation());
			assertEquals(id.equals("backdraft") ? 4 : 6, style.windup());
			assertEquals(id.equals("backdraft") ? 14 : 16, style.recovery());
			assertEquals(8, art.cost()); assertEquals(80, art.cooldown()); assertEquals(2, art.slot());
			assertEquals(MastersStyleRules.TargetPolicy.EARNED_COUNTER, style.targets());
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
		}
		assertEquals(50, ArtRules.ARTS.size());
		assertEquals(9, ArtRules.ARTS.size() - MastersStyleRules.STYLES.size());
	}

	@Test void nextCountersKeepTheirAuthoredBaseValuesAndSeparateReleaseWindows() {
		for (String id : new String[] {"unmoved", "null_parry"}) {
			var style = MastersStyleRules.of(id); var art = ArtRules.art(id);
			assertEquals(id.equals("unmoved") ? 24 : 25, style.animation());
			assertEquals(id.equals("unmoved") ? 6 : 4, style.windup());
			assertEquals(id.equals("unmoved") ? 16 : 14, style.recovery());
			assertEquals(8, art.cost()); assertEquals(80, art.cooldown()); assertEquals(2, art.slot());
			assertEquals(MastersStyleRules.TargetPolicy.EARNED_COUNTER, style.targets());
			assertNull(MastersArtRules.move(style.animation()));
		}
		assertEquals(1, ArtRules.UNMOVED_FACTOR); assertEquals(1.6, ArtRules.UNMOVED_THROW); assertEquals(80, ArtRules.UNMOVED_TICKS);
		assertEquals(1, ArtRules.NULL_FACTOR); assertEquals(.25, ArtRules.NULL_PULSE_FACTOR); assertEquals(3, ArtRules.NULL_PULSE);
		assertEquals(.5, ArtRules.NULL_SHOVE); assertEquals(8, ArtRules.NULL_HOLD); assertEquals(60, ArtRules.NULL_SILENCE);
		assertEquals(30, ArtRules.SILENCE_PLAYER_TICKS); assertEquals(100, ArtRules.SILENCE_REST);
	}

	@Test void batch1CountersKeepTheirAuthoredBaseValuesAndSeparateReleaseWindows() {
		for (String id : new String[] {"eye_of_the_storm", "sanguine_parry", "constellation_guard", "stopped_moment"}) {
			var style = MastersStyleRules.of(id); var art = ArtRules.art(id);
			int expectedAnimation = switch (id) {
				case "eye_of_the_storm" -> 26;
				case "sanguine_parry" -> 27;
				case "constellation_guard" -> 28;
				case "stopped_moment" -> 29;
				default -> -1;
			};
			int expectedWindup = id.equals("stopped_moment") ? 6 : 4;
			int expectedRecovery = id.equals("stopped_moment") ? 16 : 14;
			assertEquals(expectedAnimation, style.animation());
			assertEquals(expectedWindup, style.windup());
			assertEquals(expectedRecovery, style.recovery());
			assertEquals(8, art.cost()); assertEquals(80, art.cooldown()); assertEquals(2, art.slot());
			assertEquals(MastersStyleRules.TargetPolicy.EARNED_COUNTER, style.targets());
			assertNull(MastersArtRules.move(style.animation()));
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
		}
	}

	@Test void batch2SecondFormsKeepTheirAuthoredBaseValuesAndSeparateReleaseWindows() {
		for (String id : new String[] {"updraft", "avalanche", "meteor_shower", "rewind_leap"}) {
			var style = MastersStyleRules.of(id); var art = ArtRules.art(id);
			int expectedAnimation = switch (id) {
				case "updraft" -> 30;
				case "avalanche" -> 31;
				case "meteor_shower" -> 32;
				case "rewind_leap" -> 33;
				default -> -1;
			};
			int expectedWindup = 8;
			int expectedRecovery = id.equals("meteor_shower") ? 16 : 18;
			assertEquals(expectedAnimation, style.animation());
			assertEquals(expectedWindup, style.windup());
			assertEquals(expectedRecovery, style.recovery());
			assertEquals(1, art.slot());
			assertNull(MastersArtRules.move(style.animation()));
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
		}
	}

	@Test void batch3aChargesKeepTheirAuthoredBaseValuesAndSeparateReleaseWindows() {
		for (String id : new String[] {"wildfire_rush", "skate", "tailwind", "landslide", "wild_growth"}) {
			var style = MastersStyleRules.of(id); var art = ArtRules.art(id);
			int expectedAnimation = switch (id) {
				case "wildfire_rush" -> 34;
				case "skate" -> 35;
				case "tailwind" -> 36;
				case "landslide" -> 37;
				case "wild_growth" -> 38;
				default -> -1;
			};
			int expectedWindup = id.equals("skate") || id.equals("tailwind") ? 4 : id.equals("landslide") ? 8 : 6;
			int expectedRecovery = id.equals("skate") ? 14 : id.equals("landslide") ? 18 : 16;
			assertEquals(expectedAnimation, style.animation());
			assertEquals(expectedWindup, style.windup());
			assertEquals(expectedRecovery, style.recovery());
			assertEquals(3, art.slot());
			assertEquals(MastersStyleRules.TargetPolicy.ACTIVE_CONE, style.targets());
			assertNull(MastersArtRules.move(style.animation()));
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
		}
	}

	@Test void batch3bChargesKeepTheirAuthoredBaseValuesAndSeparateReleaseWindows() {
		for (String id : new String[] {"bolt_step", "rift_step", "comet_dash", "blur", "frenzy"}) {
			var style = MastersStyleRules.of(id); var art = ArtRules.art(id);
			int expectedAnimation = switch (id) {
				case "bolt_step" -> 39;
				case "rift_step" -> 40;
				case "comet_dash" -> 41;
				case "blur" -> 42;
				case "frenzy" -> 43;
				default -> -1;
			};
			int expectedWindup = id.equals("frenzy") ? 6 : 4;
			int expectedRecovery = id.equals("bolt_step") || id.equals("rift_step") || id.equals("comet_dash") ? 14 : 16;
			assertEquals(expectedAnimation, style.animation());
			assertEquals(expectedWindup, style.windup());
			assertEquals(expectedRecovery, style.recovery());
			assertEquals(3, art.slot());
			assertEquals(MastersStyleRules.TargetPolicy.ACTIVE_CONE, style.targets());
			assertNull(MastersArtRules.move(style.animation()));
			assertEquals(0, MastersStyleRules.attackPitch(style.animation(), 90));
		}
	}
}
