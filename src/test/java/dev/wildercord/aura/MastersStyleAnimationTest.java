package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MastersStyleAnimationTest {
	@Test
	void everyFirstFormHasItsOwnBodyAndWeaponPoseWithoutOpeningANewInputOrdinal() {
		var bodies = new HashSet<MastersArtAnimation.Pose>();
		var hands = new HashSet<MastersArtAnimation.Hand>();
		assertEquals(10, MastersStyleRules.STYLES.size());
		for (var style : MastersStyleRules.STYLES) {
			assertTrue(MastersArtAnimation.supports(style.animation()));
			assertNull(MastersArtRules.move(style.animation()), "Style poses are not trusted client action IDs");
			var pose = sample(style, style.windup());
			assertEquals(1, pose.weight());
			assertTrue(bodies.add(pose), "Distinct body choreography for " + style.art());
			assertTrue(hands.add(pose.hand()), "Distinct weapon choreography for " + style.art());
		}
		for (int id : new int[] {-1, 13, Integer.MAX_VALUE}) assertFalse(MastersArtAnimation.supports(id));
	}

	@Test
	void everyServerTimelineEntersAndExitsCleanlyAndStaysFinite() {
		for (var style : MastersStyleRules.STYLES) {
			assertEquals(0, sample(style, 0).weight());
			assertSame(MastersArtAnimation.NONE, sample(style, -1));
			assertSame(MastersArtAnimation.NONE, sample(style, Float.NaN));
			assertSame(MastersArtAnimation.NONE, sample(style, style.windup() + style.recovery()));
			for (float age = 0; age < style.windup() + style.recovery(); age += .025F) {
				var pose = sample(style, age);
				assertTrue(pose.weight() >= 0 && pose.weight() <= 1);
				assertTrue(Float.isFinite(pose.sword().x()) && Float.isFinite(pose.body().y()) && Float.isFinite(pose.hand().pitch()));
			}
		}
	}

	@Test
	void phaseBoundariesStayContinuousIncludingRepeatedHits() {
		for (var style : MastersStyleRules.STYLES) {
			int windup = style.windup(), recovery = style.recovery();
			List<Float> boundaries = new ArrayList<>(List.of(windup * .65F, (float) windup, (float) (windup + recovery)));
			if (style.art().equals("crackle")) {
				boundaries.addAll(List.of((float) (windup + ArtRules.CRACKLE_GAP), (float) (windup + 2 * ArtRules.CRACKLE_GAP),
					(float) (windup + 2 * ArtRules.CRACKLE_GAP + 2)));
			} else if (style.art().equals("echo_cut")) {
				boundaries.addAll(List.of((float) (windup + 3), (float) (windup + ArtRules.ECHO_DELAY - 3),
					(float) (windup + ArtRules.ECHO_DELAY), (float) (windup + ArtRules.ECHO_DELAY + 2)));
			} else boundaries.add(windup + Math.min(4, recovery * .25F));
			for (float boundary : boundaries) {
				var before = sample(style, boundary - .001F);
				var after = sample(style, boundary + .001F);
				assertEquals(before.weight(), after.weight(), .004F);
				assertEquals(before.sword().x() * before.weight(), after.sword().x() * after.weight(), .004F);
				assertEquals(before.body().y() * before.weight(), after.body().y() * after.weight(), .004F);
				assertEquals(before.hand().z() * before.weight(), after.hand().z() * after.weight(), .004F);
			}
		}
	}

	@Test
	void crackleAlternatesTwiceThenThrustsOnTheActualDamageBeats() {
		var style = MastersStyleRules.of("crackle");
		var first = sample(style, style.windup());
		var second = sample(style, style.windup() + ArtRules.CRACKLE_GAP);
		var third = sample(style, style.windup() + 2 * ArtRules.CRACKLE_GAP);
		assertTrue(first.sword().y() < 0 && second.sword().y() > 0);
		assertTrue(third.hand().z() < first.hand().z());
		assertEquals(1, third.weight());
	}

	@Test
	void echoCutHoldsThenAnswersItsAfterimageOnTheActualReturnBeat() {
		var style = MastersStyleRules.of("echo_cut");
		var first = sample(style, style.windup());
		var held = sample(style, style.windup() + 5);
		var stillHeld = sample(style, style.windup() + ArtRules.ECHO_DELAY - 4);
		var echo = sample(style, style.windup() + ArtRules.ECHO_DELAY);
		assertEquals(held, stillHeld);
		assertTrue(first.sword().y() < 0 && echo.sword().y() > 0);
		assertEquals(1, echo.weight());
	}

	@Test
	void heavyLeansKeepTheHipRootConnectedAndPreserveShoulderSpacing() {
		for (var style : MastersStyleRules.STYLES) {
			var pose = sample(style, style.windup());
			var hip = MastersArtAnimation.pivot(pose, 0, 12, 0, false);
			assertEquals(0, hip.x(), .0001F);
			assertEquals(12 + pose.lower(), hip.y(), .0001F);
			assertEquals(pose.forward(), hip.z(), .0001F);
			var right = MastersArtAnimation.pivot(pose, -5, 2, 0, false);
			var left = MastersArtAnimation.pivot(pose, 5, 2, 0, false);
			assertEquals(10, distance(right, left), .0001);
			var mirrored = MastersArtAnimation.pivot(pose, 5, 2, 0, true);
			assertEquals(-right.x(), mirrored.x(), .0001F);
			assertEquals(right.y(), mirrored.y(), .0001F);
			assertEquals(right.z(), mirrored.z(), .0001F);
		}
	}

	@Test
	void turningDuringWindupKeepsTheBodyCommittedAndHeadFree() {
		var locked = MastersArtAnimation.facing(90, 0, 30, 0, 0, 1);
		assertEquals(0, locked.bodyYaw());
		assertEquals(75, locked.headYaw(), "The model stops short of an impossible neck twist");
		assertEquals(-90, locked.yawDelta());
		assertEquals(-30, locked.pitchDelta());
		var halfway = MastersArtAnimation.facing(90, 0, 30, 0, 0, .5F);
		assertEquals(45, halfway.bodyYaw());
		assertEquals(45, halfway.headYaw());
		assertEquals(90, halfway.bodyYaw() + halfway.headYaw(), "Free-look world heading is unchanged");
		var seam = MastersArtAnimation.facing(179, 0, 0, -179, 0, .5F);
		assertEquals(180, Math.abs(seam.bodyYaw()), "Takes the short route across the angle wrap");
		assertEquals(-1, seam.headYaw());
	}

	@Test
	void fullTurnAndVerticalLookNeverTwistTheNeckBeyondTheAuthoredTorsoLimits() {
		float radians = (float) (Math.PI / 180);
		for (float look : new float[] {-180, -90, 90, 180}) {
			var facing = MastersArtAnimation.facing(look, 0, 90, 0, 0, 1);
			assertEquals(0, facing.bodyYaw(), .0001F);
			assertTrue(Math.abs(facing.headYaw()) <= 75);
			for (var style : MastersStyleRules.STYLES) {
				var body = sample(style, style.windup()).body();
				var head = MastersArtAnimation.boundedHead(body, new MastersArtAnimation.Joint(90 * radians, facing.headYaw() * radians, 0));
				assertTrue(head.x() - body.x() <= 60 * radians + .0001F);
				assertTrue(Math.abs(head.y() - body.y()) <= 75 * radians + .0001F);
				var up = MastersArtAnimation.boundedHead(body, new MastersArtAnimation.Joint(-90 * radians, facing.headYaw() * radians, 0));
				assertTrue(up.x() - body.x() >= -45 * radians - .0001F);
			}
		}
	}

	@Test
	void neckLimitsBlendWithTheArtInsteadOfSnappingAtEntryAndExpiry() {
		var body = new MastersArtAnimation.Joint(0, 0, 0);
		for (float look : new float[] {-90, 90}) {
			var wanted = new MastersArtAnimation.Joint((float) Math.toRadians(look), 0, 0);
			assertEquals(wanted, MastersArtAnimation.boundedHead(body, wanted, 0));
			var entering = MastersArtAnimation.boundedHead(body, wanted, .0001F);
			assertEquals(wanted.x(), entering.x(), .0001F);
			assertEquals(MastersArtAnimation.boundedHead(body, wanted), MastersArtAnimation.boundedHead(body, wanted, 1));
		}
	}

	@Test
	void firstPersonRotatesAroundTheMirroredGripAndBoundsOffscreenAim() {
		var style = MastersStyleRules.of("star_needle");
		var pose = sample(style, style.windup());
		var right = MastersArtAnimation.view(pose, false, 0, 0, 0);
		var left = MastersArtAnimation.view(pose, true, 0, 0, 0);
		assertEquals(.56F, right.grip().x());
		assertEquals(-.56F, left.grip().x());
		assertEquals(-.52F, right.grip().y());
		assertEquals(-.72F, right.grip().z());
		assertEquals(-right.transform().x(), left.transform().x(), .0001F);
		assertEquals(-right.transform().yaw(), left.transform().yaw(), .0001F);
		assertEquals(-right.transform().roll(), left.transform().roll(), .0001F);
		var turned = MastersArtAnimation.view(pose, false, 1, 90, 90);
		assertEquals(-1.12F, turned.grip().y(), .0001F);
		assertEquals(right.transform().yaw() - 55, turned.transform().yaw());
		assertEquals(right.transform().pitch() - 40, turned.transform().pitch());
		var resting = MastersArtAnimation.view(MastersArtAnimation.NONE, false, 0, 180, 90);
		assertEquals(0, resting.transform().yaw(), .0001F);
		assertEquals(0, resting.transform().pitch(), .0001F);
		assertEquals("right", MastersArtAnimation.offscreenDirection(90, 0));
		assertEquals("left", MastersArtAnimation.offscreenDirection(-90, 0));
		assertEquals("up", MastersArtAnimation.offscreenDirection(0, -90));
		assertEquals("down", MastersArtAnimation.offscreenDirection(0, 90));
	}

	@Test
	void thirdPersonThrustTurnsTheSwordRatherThanLeavingItsPointVertical() {
		assertEquals(-80, MastersArtAnimation.bladeTilt(2, 6, 6, 1));
		assertEquals(-80, MastersArtAnimation.bladeTilt(10, 4, 4, 1));
		assertEquals(0, MastersArtAnimation.bladeTilt(5, 4, 4, 1), .0001F);
		assertEquals(0, MastersArtAnimation.bladeTilt(5, 4 + ArtRules.CRACKLE_GAP, 4, 1), .0001F);
		assertEquals(-80, MastersArtAnimation.bladeTilt(5, 4 + 2 * ArtRules.CRACKLE_GAP, 4, 1));
		assertEquals(-40, MastersArtAnimation.bladeTilt(2, 18, 6, .5F));
		assertEquals(0, MastersArtAnimation.bladeTilt(7, 10, 10, 1), .0001F);
	}

	private static MastersArtAnimation.Pose sample(MastersStyleRules.Style style, float age) {
		return MastersArtAnimation.sample(style.animation(), age, style.windup(), style.recovery());
	}

	private static double distance(MastersArtAnimation.Pivot a, MastersArtAnimation.Pivot b) {
		return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2) + Math.pow(a.z() - b.z(), 2));
	}
}
