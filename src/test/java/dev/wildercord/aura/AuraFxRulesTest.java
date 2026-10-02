package dev.wildercord.aura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static dev.wildercord.aura.SwordString.Token.COUNTER;
import static dev.wildercord.aura.SwordString.Token.FULL;
import static dev.wildercord.aura.SwordString.Token.LEAP;
import static dev.wildercord.aura.SwordString.Token.LOW;
import static dev.wildercord.aura.SwordString.Token.RUN;
import static dev.wildercord.aura.SwordString.Token.STEP;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Aura's feel, the pure parts ({@link AuraFxRules}): the strokes a trail cuts and which swing cuts which, the run of cuts, how a
 * trail grows by stage and how your own first-person trail is held small, how hard impacts land and how the impact setting
 * scales them, the body's aura calm, flaring and surging, the banners' timing, and every method's own sounds ({@link AuraFx}).
 */
class AuraFxRulesTest {
	// ------------------------------------------------------------------ strokes

	@Test
	void everyStrokeIsAnArcAboutAnArmsReachThatLivesLongerThanItSweeps() {
		for (AuraFxRules.Stroke stroke : AuraFxRules.Stroke.values()) {
			assertTrue(stroke.radius >= 1.2 && stroke.radius <= 3.0, stroke + " radius " + stroke.radius);
			assertTrue(stroke.sweep >= 1 && stroke.sweep <= 6, stroke + " sweeps in " + stroke.sweep);
			assertTrue(stroke.life > stroke.sweep + 3 && stroke.life <= 14, stroke + " lives " + stroke.life);
			assertTrue(stroke.height > 0.5 && stroke.height < 1.7, stroke + " at height " + stroke.height);
			assertEquals(stroke, AuraFxRules.Stroke.of(stroke.ordinal()));
		}
		assertEquals(AuraFxRules.Stroke.CUT, AuraFxRules.Stroke.of(-1));
		assertEquals(AuraFxRules.Stroke.CUT, AuraFxRules.Stroke.of(99));
		// The cuts sweep most of the way round the front; a spin goes all the way round.
		assertTrue(AuraFxRules.Stroke.CUT.span() >= 120);
		assertTrue(AuraFxRules.Stroke.SWEEP.span() > AuraFxRules.Stroke.CUT.span());
		assertTrue(AuraFxRules.Stroke.LOW.height < AuraFxRules.Stroke.SWEEP.height, "a low swing cuts lower than a sweep");
		assertEquals(360, AuraFxRules.Stroke.SPIN.span(), 1.0E-9);
		assertEquals(0, AuraFxRules.Stroke.THRUST.span(), 1.0E-9);
		// A rising cut climbs the same plane a cut comes down.
		assertTrue(AuraFxRules.Stroke.RISING.from < 0 && AuraFxRules.Stroke.RISING.to > 0);
		assertTrue(AuraFxRules.Stroke.CUT.from > 0 && AuraFxRules.Stroke.CUT.to < 0);
	}

	@Test
	void eachKindOfSwingCutsItsOwnStroke() {
		assertEquals(AuraFxRules.Stroke.CUT, AuraFxRules.stroke(SwordString.Token.marks(), false));
		assertEquals(AuraFxRules.Stroke.CUT, AuraFxRules.stroke(SwordString.Token.marks(FULL), false));
		assertEquals(AuraFxRules.Stroke.LOW, AuraFxRules.stroke(SwordString.Token.marks(LOW), false));
		assertEquals(AuraFxRules.Stroke.FALLING, AuraFxRules.stroke(SwordString.Token.marks(LEAP), false));
		assertEquals(AuraFxRules.Stroke.THRUST, AuraFxRules.stroke(SwordString.Token.marks(RUN), false));
		assertEquals(AuraFxRules.Stroke.THRUST, AuraFxRules.stroke(SwordString.Token.marks(STEP), false));
		// A spear thrusts whatever else the swing was.
		assertEquals(AuraFxRules.Stroke.THRUST, AuraFxRules.stroke(SwordString.Token.marks(LOW, LEAP), true));
		// A counter is struck sneaking (the guard holds while sneak does), and crosses rather than sweeps.
		assertEquals(AuraFxRules.Stroke.CROSS, AuraFxRules.stroke(SwordString.Token.marks(LOW, COUNTER), false));
		// In the air and crouching, the leap wins: the cut comes down.
		assertEquals(AuraFxRules.Stroke.FALLING, AuraFxRules.stroke(SwordString.Token.marks(LOW, LEAP), false));
	}

	@Test
	void aRunOfCutsGoesBackAndForthAndAPauseStartsOver() {
		long none = Long.MIN_VALUE / 4;
		assertFalse(AuraFxRules.mirrored(none, false, 100), "the first cut of a run goes the usual way");
		assertTrue(AuraFxRules.mirrored(100, false, 112), "the second cuts back");
		assertFalse(AuraFxRules.mirrored(112, true, 124), "the third the usual way again");
		assertFalse(AuraFxRules.mirrored(100, false, 100 + AuraFxRules.COMBO_TICKS + 1), "after a pause it starts over");
		assertTrue(AuraFxRules.mirrored(100, false, 100 + AuraFxRules.COMBO_TICKS));
		assertFalse(AuraFxRules.mirrored(200, false, 150), "a swing from before the last (a new world's clock) starts over");
	}

	@Test
	void aFlowSweepIsAFullStillSwingAtACreatureWithAuraToCoatIt() {
		double walk = 0.1;
		assertTrue(AuraFxRules.sweeps(AuraRules.FLOW, true, true, 1.0F, true, false, 0.0, walk));
		assertTrue(AuraFxRules.sweeps(AuraRules.SOVEREIGN, true, true, (float) AuraRules.FULL_SWING, true, false, 0.2, walk));
		assertFalse(AuraFxRules.sweeps(AuraRules.GLOW, true, true, 1.0F, true, false, 0.0, walk), "Glow doesn't sweep");
		assertFalse(AuraFxRules.sweeps(AuraRules.FLOW, false, true, 1.0F, true, false, 0.0, walk), "without aura to coat it");
		assertFalse(AuraFxRules.sweeps(AuraRules.FLOW, true, false, 1.0F, true, false, 0.0, walk), "at nothing");
		assertFalse(AuraFxRules.sweeps(AuraRules.FLOW, true, true, 0.5F, true, false, 0.0, walk), "a half swing");
		assertFalse(AuraFxRules.sweeps(AuraRules.FLOW, true, true, 1.0F, false, false, 0.0, walk), "in the air");
		assertFalse(AuraFxRules.sweeps(AuraRules.FLOW, true, true, 1.0F, true, true, 0.0, walk), "sprinting");
		assertFalse(AuraFxRules.sweeps(AuraRules.FLOW, true, true, 1.0F, true, false, 0.3, walk), "on the move");
	}

	// ------------------------------------------------------------------ trails by stage

	@Test
	void trailsGrowWiderLongerAndBrighterAtEachStage() {
		for (int stage = AuraRules.GLOW; stage < AuraRules.SOVEREIGN; stage++) {
			assertTrue(AuraFxRules.trailWidth(stage + 1) > AuraFxRules.trailWidth(stage), "width at " + stage);
			assertTrue(AuraFxRules.trailTail(stage + 1) > AuraFxRules.trailTail(stage), "length at " + stage);
			assertTrue(AuraFxRules.trailAlpha(stage + 1) > AuraFxRules.trailAlpha(stage), "brightness at " + stage);
		}
		assertTrue(AuraFxRules.trailTail(AuraRules.SOVEREIGN) < 1.0, "the tail never trails past the start of the arc");
		assertTrue(AuraFxRules.trailAlpha(AuraRules.SOVEREIGN) <= 0.7F, "even at Sovereign the glow lets the world through");
		assertEquals(AuraFxRules.trailWidth(AuraRules.GLOW), AuraFxRules.trailWidth(AuraRules.NONE), 1.0E-9, "below Glow: Glow's");
		// The extras come one stage at a time.
		assertFalse(AuraFxRules.sheds(AuraRules.GLOW));
		assertTrue(AuraFxRules.sheds(AuraRules.FLOW));
		assertFalse(AuraFxRules.edged(AuraRules.FLOW));
		assertTrue(AuraFxRules.edged(AuraRules.EDGE));
		assertFalse(AuraFxRules.echoes(AuraRules.EDGE));
		assertTrue(AuraFxRules.echoes(AuraRules.FORM));
		assertFalse(AuraFxRules.sparks(AuraRules.FORM));
		assertTrue(AuraFxRules.sparks(AuraRules.SOVEREIGN));
	}

	@Test
	void yourOwnFirstPersonTrailIsThinShortLowAndFaint() {
		assertTrue(AuraFxRules.OWN_WIDTH > 0 && AuraFxRules.OWN_WIDTH <= 0.4, "thin");
		assertTrue(AuraFxRules.OWN_SPAN > 0.3 && AuraFxRules.OWN_SPAN <= 0.6, "short");
		assertTrue(AuraFxRules.OWN_DROP >= 0.3, "below the eye line");
		assertTrue(AuraFxRules.OWN_ALPHA > 0.2F && AuraFxRules.OWN_ALPHA <= 0.6F, "faint");
		assertTrue(AuraFxRules.OWN_LIFE < 1.0, "gone sooner");
		assertTrue(AuraFxRules.OWN_ASIDE > 0, "toward the blade hand, off the middle of the view");
		// The thickest own trail (Sovereign) is still thinner than the thinnest full one.
		assertTrue(AuraFxRules.trailWidth(AuraRules.SOVEREIGN) * AuraFxRules.OWN_WIDTH < AuraFxRules.trailWidth(AuraRules.GLOW));
	}

	@Test
	void yourOwnViewKeepsPartOfEachArcAndClearsTheMiddle() {
		for (AuraFxRules.Stroke stroke : AuraFxRules.Stroke.values()) {
			double lo = Math.min(stroke.from, stroke.to);
			double hi = Math.max(stroke.from, stroke.to);
			assertTrue(stroke.ownStart >= 0 && stroke.ownStart < 1, stroke + " starts within its arc");
			assertTrue(stroke.ownFrom() >= lo - 1.0E-9 && stroke.ownFrom() <= hi + 1.0E-9, stroke + "'s own view starts on its arc");
			assertTrue(stroke.ownTo() >= lo - 1.0E-9 && stroke.ownTo() <= hi + 1.0E-9, stroke + "'s own view ends on its arc");
			assertTrue(Math.abs(stroke.ownTo() - stroke.ownFrom()) <= stroke.span() * AuraFxRules.OWN_SPAN + 1.0E-9, stroke + " keeps only part");
		}
		// Most keep their start; a falling cut keeps its end, coming down past the bottom of the view.
		assertEquals(AuraFxRules.Stroke.CUT.from, AuraFxRules.Stroke.CUT.ownFrom(), 1.0E-9);
		assertEquals(AuraFxRules.Stroke.FALLING.to, AuraFxRules.Stroke.FALLING.ownTo(), 1.0E-9);
		// Gone at the middle of the view, whole a little way out, smoothly between.
		assertEquals(0, AuraFxRules.ownClear(0), 1.0E-6);
		assertEquals(0, AuraFxRules.ownClear(AuraFxRules.OWN_CLEAR), 1.0E-6);
		assertEquals(1, AuraFxRules.ownClear(AuraFxRules.OWN_CLEAR_FULL), 1.0E-6);
		assertEquals(1, AuraFxRules.ownClear(90), 1.0E-6);
		float last = 0;
		for (double d = AuraFxRules.OWN_CLEAR; d <= AuraFxRules.OWN_CLEAR_FULL; d += 0.5) {
			float k = AuraFxRules.ownClear(d);
			assertTrue(k >= last, "clears out steadily");
			last = k;
		}
	}

	// ------------------------------------------------------------------ impacts

	@Test
	void blowsWeighByHowFullTheSwingWasAndHeavierBlowsLandHarder() {
		assertEquals(AuraFxRules.Weight.LIGHT, AuraFxRules.blow(0.5F, false));
		assertEquals(AuraFxRules.Weight.LIGHT, AuraFxRules.blow(0.5F, true), "a half swing is light, critical or not");
		assertEquals(AuraFxRules.Weight.FULL, AuraFxRules.blow(1.0F, false));
		assertEquals(AuraFxRules.Weight.FULL, AuraFxRules.blow((float) AuraRules.FULL_SWING, false));
		assertEquals(AuraFxRules.Weight.HEAVY, AuraFxRules.blow(1.0F, true));
		AuraFxRules.Weight[] all = AuraFxRules.Weight.values();
		for (int i = 1; i < all.length; i++) {
			assertTrue(all[i].hitStop > all[i - 1].hitStop, all[i] + " holds longer");
			assertTrue(all[i].nudge > all[i - 1].nudge, all[i] + " nudges more");
			assertTrue(all[i].flash > all[i - 1].flash, all[i] + " flashes bigger");
		}
		assertEquals(0, AuraFxRules.Weight.LIGHT.hitStop, "a light blow never holds the moment");
		assertTrue(AuraFxRules.Weight.GRAND.hitStop <= 120, "a few frames at most, never a stall");
		assertTrue(AuraFxRules.Weight.GRAND.nudge <= 1.2F, "a nudge, never a shake");
		assertEquals(AuraFxRules.Weight.LIGHT, AuraFxRules.Weight.of(-1));
	}

	@Test
	void theImpactSettingScalesTheHitStopAndItNeverGrows() {
		for (AuraFxRules.Weight w : AuraFxRules.Weight.values()) {
			assertEquals(w.hitStop, AuraFxRules.hitStop(w, 1.0));
			assertEquals(Math.round(w.hitStop * 0.5), AuraFxRules.hitStop(w, 0.5));
			assertEquals(0, AuraFxRules.hitStop(w, 0.0));
			assertEquals(w.hitStop, AuraFxRules.hitStop(w, 3.0), "never more than the weight's own");
			assertEquals(0, AuraFxRules.hitStop(w, -1.0));
		}
		assertTrue(AuraFxRules.HIT_STOP_GAP >= AuraFxRules.Weight.GRAND.hitStop, "stops can't chain into one long freeze");
	}

	@Test
	void flashesGrowALittleWithTheStage() {
		for (AuraFxRules.Weight w : AuraFxRules.Weight.values()) {
			assertEquals(w.flash, AuraFxRules.flash(w, AuraRules.GLOW), 1.0E-6);
			assertTrue(AuraFxRules.flash(w, AuraRules.SOVEREIGN) > AuraFxRules.flash(w, AuraRules.GLOW));
			assertTrue(AuraFxRules.flash(w, AuraRules.SOVEREIGN) < w.flash * 1.3F, "a little, not a lot");
		}
	}

	// ------------------------------------------------------------------ the body's aura

	@Test
	void theBodysAuraIsCalmAtRestFlaresInAFightAndSurgesOverThat() {
		float idle = AuraFxRules.intensity(true, false, 0);
		float fight = AuraFxRules.intensity(true, true, 0);
		float surge = AuraFxRules.intensity(true, true, 1);
		assertEquals(AuraFxRules.IDLE, idle, 1.0E-6);
		assertEquals(AuraFxRules.FIGHTING, fight, 1.0E-6);
		assertTrue(idle < fight && fight < surge);
		assertTrue(surge <= 1.6F);
		assertTrue(AuraFxRules.intensity(true, false, 1) > idle, "a surge at rest still blazes");
		assertTrue(AuraFxRules.intensity(false, true, 0) < fight * 0.5F, "faint while too low to coat a blow");
		assertEquals(AuraFxRules.intensity(true, false, 2), AuraFxRules.intensity(true, false, 1), 1.0E-6, "a surge holds at full");
	}

	@Test
	void aSurgeDiesAwayOverItsTicks() {
		assertEquals(1.0F, AuraFxRules.surgeLeft(1, 0, 20), 1.0E-6);
		assertTrue(AuraFxRules.surgeLeft(1, 10, 20) < 0.5F);
		assertTrue(AuraFxRules.surgeLeft(1, 5, 20) > AuraFxRules.surgeLeft(1, 10, 20));
		assertEquals(0, AuraFxRules.surgeLeft(1, 20, 20));
		assertEquals(0, AuraFxRules.surgeLeft(1, -1, 20));
		assertEquals(0, AuraFxRules.surgeLeft(1, 0, 0));
		assertEquals(0.5F, AuraFxRules.surgeLeft(0.5F, 0, 20), 1.0E-6);
	}

	@Test
	void aFightsFlareIsRenewedOnlyNowAndThen() {
		assertTrue(AuraFxRules.renewFight(0, 100), "run out");
		assertTrue(AuraFxRules.renewFight(100 + AuraFxRules.FIGHT_RENEW - 1, 100), "nearly run out");
		assertFalse(AuraFxRules.renewFight(100 + AuraFxRules.FIGHT_TICKS, 100), "just renewed");
		assertTrue(AuraFxRules.FIGHT_TICKS > AuraFxRules.FIGHT_RENEW);
	}

	// ------------------------------------------------------------------ banners

	@Test
	void bannersSlideInHoldAndFade() {
		for (AuraFxRules.BannerKind kind : AuraFxRules.BannerKind.values()) {
			assertEquals(0, AuraFxRules.bannerAlpha(-1, kind.ticks));
			assertEquals(0, AuraFxRules.bannerAlpha(kind.ticks, kind.ticks));
			assertEquals(1, AuraFxRules.bannerAlpha(kind.ticks / 2.0, kind.ticks), 1.0E-6);
			assertTrue(AuraFxRules.bannerAlpha(kind.ticks - 2, kind.ticks) < 0.5F);
			assertTrue(kind.ticks >= 40 && kind.ticks <= 70, "about two to three seconds: " + kind);
		}
		assertTrue(AuraFxRules.BannerKind.GRAND.ticks > AuraFxRules.BannerKind.ART.ticks);
		assertEquals(0, AuraFxRules.bannerSlide(0), 1.0E-6);
		assertEquals(1, AuraFxRules.bannerSlide(AuraFxRules.BANNER_IN), 1.0E-6);
		assertTrue(AuraFxRules.bannerSlide(AuraFxRules.BANNER_IN / 2.0) > 0.5F, "it eases in, fast then slow");
		assertEquals(AuraFxRules.BannerKind.ART, AuraFxRules.BannerKind.of(99));
	}

	@Test
	void artsAreNamedByTheirOrdinalFromFirstToFinal() {
		assertEquals("aura.wildercord.banner.ordinal.1", AuraFxRules.ordinalKey(AuraRules.GLOW));
		assertEquals("aura.wildercord.banner.ordinal.5", AuraFxRules.ordinalKey(AuraRules.SOVEREIGN));
		assertEquals("aura.wildercord.banner.ordinal.1", AuraFxRules.ordinalKey(AuraRules.NONE), "never below the First");
		assertEquals("aura.wildercord.banner.ordinal.5", AuraFxRules.ordinalKey(99), "never past the Final");
	}

	// ------------------------------------------------------------------ the methods' sounds

	@Test
	void everyMethodHasItsOwnSoundsAndEveryOneOfThemExists() throws IOException {
		JsonObject kit;
		try (InputStream in = AuraFxRulesTest.class.getResourceAsStream("/assets/wildercord/kit_sounds.json")) {
			assertNotNull(in);
			kit = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("events");
		}
		Map<String, AuraFx.SoundFamily> families = AuraFx.families();
		Set<String> names = new HashSet<>();
		for (BreathingMethod method : BreathingMethods.BUILT_IN) {
			AuraFx.SoundFamily family = AuraFx.family(method.id());
			assertEquals(families.get(method.id()), family);
			assertNotEquals(AuraFx.STEEL, family, method.id() + " should have sounds of its own");
			for (AuraFx.Sound sound : AuraFx.Sound.values()) {
				String name = family.of(sound);
				assertTrue(name.startsWith("aura_" + method.id() + "_"), name);
				assertTrue(kit.has(name), name + " isn't in the feel kit: run python tools/feel/build.py --only aura");
				assertTrue(names.add(name), name + " is used twice");
			}
		}
		for (AuraFx.Sound sound : AuraFx.Sound.values()) {
			assertTrue(kit.has(AuraFx.STEEL.of(sound)), AuraFx.STEEL.of(sound));
			assertTrue(names.add(AuraFx.STEEL.of(sound)));
		}
		assertEquals(AuraFx.STEEL, AuraFx.family("example:unknown"), "a method without sounds of its own rings plain steel");
		assertEquals(AuraFx.STEEL, AuraFx.family(""));
		assertEquals(AuraFx.STEEL, AuraFx.family(null));
	}

	@Test
	void anAddOnCanGiveItsMethodSounds() {
		AuraFx.SoundFamily own = new AuraFx.SoundFamily("example_swing", "example_impact", "example_art");
		AuraFx.registerFamily("example:tidal", own);
		assertEquals(own, AuraFx.family("example:tidal"));
		assertEquals("example_impact", own.of(AuraFx.Sound.IMPACT));
		assertEquals(new AuraFx.SoundFamily("aura_ember_swing", "aura_ember_impact", "aura_ember_art"), AuraFx.SoundFamily.named("ember"));
	}
}
