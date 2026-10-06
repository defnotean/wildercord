package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MastersArtAnimationTest {
	private static final int[] WINDUP = {4, 8, 6};
	private static final int[] RECOVERY = {12, 18, 14};

	@Test
	void invalidExpiredAndNonFiniteTimelinesNeverAnimate() {
		for (int move : new int[] {-1, 17, Integer.MAX_VALUE}) {
			assertSame(MastersArtAnimation.NONE, MastersArtAnimation.sample(move, 2, 4, 12));
		}
		for (float age : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 16, 200}) {
			assertSame(MastersArtAnimation.NONE, MastersArtAnimation.sample(0, age, 4, 12));
		}
		for (int[] timing : new int[][] {{0, 12}, {-1, 12}, {61, 12}, {4, 0}, {4, -1}, {4, 121}, {Integer.MAX_VALUE, 12}}) {
			assertSame(MastersArtAnimation.NONE, MastersArtAnimation.sample(0, 1, timing[0], timing[1]));
		}
	}

	@Test
	void acceptedTimelinesBlendInAndOutAndReachFullWeightAtImpact() {
		for (int move = 0; move < 3; move++) {
			int windup = WINDUP[move], recovery = RECOVERY[move];
			assertEquals(0, MastersArtAnimation.sample(move, 0, windup, recovery).weight());
			assertEquals(1, MastersArtAnimation.sample(move, windup, windup, recovery).weight());
			assertTrue(MastersArtAnimation.sample(move, windup + recovery - .01F, windup, recovery).weight() < .0001F);
			for (float age = 0; age < windup + recovery; age += .05F) {
				var pose = MastersArtAnimation.sample(move, age, windup, recovery);
				assertTrue(pose.weight() >= 0 && pose.weight() <= 1);
				assertTrue(Float.isFinite(pose.body().x()) && Float.isFinite(pose.sword().x()) && Float.isFinite(pose.hand().pitch()));
			}
		}
	}

	@Test
	void allThreeMovesHaveDistinctBodyLegAndWeaponChoreography() {
		var cut = MastersArtAnimation.sample(0, 4, 4, 12);
		var rise = MastersArtAnimation.sample(1, 8, 8, 18);
		var drive = MastersArtAnimation.sample(2, 6, 6, 14);
		assertEquals(-1.35F, cut.sword().x(), .0001F);
		assertEquals(-2.75F, rise.sword().x(), .0001F);
		assertEquals(-1.65F, drive.sword().x(), .0001F);
		assertTrue(cut.body().y() < -.4F, "Spellcut turns through the target");
		assertTrue(rise.body().x() < 0, "Rising Break opens upward");
		assertTrue(drive.body().x() > .4F, "Driving Cut commits forward");
		assertTrue(drive.frontLeg().x() < -1, "The thrust has a long front-foot stance");
		assertTrue(rise.hand().y() > .3F, "The first-person blade rises");
		assertTrue(drive.hand().z() < -.6F, "The first-person point drives forward");
		assertNotEquals(cut.hand(), rise.hand());
		assertNotEquals(rise.hand(), drive.hand());
	}

	@Test
	void keyframeBoundariesAreContinuous() {
		for (int move = 0; move < 3; move++) {
			int windup = WINDUP[move], recovery = RECOVERY[move];
			for (float boundary : new float[] {windup * .65F, windup, windup + Math.min(4, recovery * .25F), windup + recovery}) {
				var before = MastersArtAnimation.sample(move, boundary - .001F, windup, recovery);
				var after = MastersArtAnimation.sample(move, boundary + .001F, windup, recovery);
				assertEquals(before.weight(), after.weight(), .002F);
				assertEquals(before.sword().x() * before.weight(), after.sword().x() * after.weight(), .002F);
				assertEquals(before.hand().z() * before.weight(), after.hand().z() * after.weight(), .002F);
			}
		}
	}

	@Test
	void spellcutCuttingEdgeStaysAboveTheSurvivalHudThroughFollowThrough() {
		// Canonical 70-degree hand camera, equipped right hand, and the vanilla handheld display.
		// The native capture additionally observes the actual submit matrix, HUD rows, and pixels.
		for (float age = 6; age <= 11; age += .25F) {
			var view = MastersArtAnimation.view(MastersArtAnimation.sample(0, age, 4, 12), false, 0, 0, 0);
			var h = view.transform(); var grip = view.grip();
			var matrix = new org.joml.Matrix4f().translate(h.x(), h.y(), h.z()).translate(grip.x(), grip.y(), grip.z())
				.rotateY((float) Math.toRadians(h.yaw())).rotateX((float) Math.toRadians(h.pitch())).rotateZ((float) Math.toRadians(h.roll()))
				.translate(1.13F / 16, 3.2F / 16, 1.13F / 16).rotateY((float) -Math.PI / 2)
				.rotateZ((float) Math.toRadians(25)).scale(.68F).translate(-.5F, -.5F, -.5F);
			for (float along : new float[] {10.5F / 16, 14F / 16}) {
				var point = matrix.transformPosition(new org.joml.Vector3f(along, along, .5F));
				double screenY = (1 - point.y / (-point.z * Math.tan(Math.toRadians(35)))) / 2;
				assertTrue(point.z < -.05F && screenY > 0 && screenY < 642.0 / 720,
					"The cutting edge must remain in front of the camera and above the 720p scale-2 survival HUD at age " + age);
			}
		}
	}

	@Test
	void alternateServerTimingsStillPlaceImpactOnTheAuthoritativeTick() {
		for (int move = 0; move < 3; move++) {
			var normal = MastersArtAnimation.sample(move, WINDUP[move], WINDUP[move], RECOVERY[move]);
			var slow = MastersArtAnimation.sample(move, 20, 20, 30);
			assertEquals(normal, slow);
		}
	}
}
