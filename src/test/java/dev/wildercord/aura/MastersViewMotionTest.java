package dev.wildercord.aura;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MastersViewMotionTest {
	private static final float[] PHASES = {0, .1F, .5F, .9F, 1};
	private static final float EPSILON = .000003F;

	@Test
	void ownershipRequiresEligibilityAndFiniteBoundedWeight() {
		for (float weight : new float[] {-.5F, 0, .2F, 1, 2, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			assertEquals(0, MastersViewMotion.ownership(weight, false));
		}
		assertEquals(0, MastersViewMotion.ownership(-.5F, true));
		assertEquals(.2F, MastersViewMotion.ownership(.2F, true));
		assertEquals(1, MastersViewMotion.ownership(2, true));
		for (float weight : new float[] {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			assertEquals(0, MastersViewMotion.ownership(weight, true));
		}
	}

	@Test
	void swingOwnershipEndpointsAreExactIndependentCopies() {
		for (float phase : PHASES) for (int side : new int[] {-1, 1}) {
			Matrix4f vanilla = whack(phase, side), original = new Matrix4f(vanilla);
			Matrix4f unchanged = MastersViewMotion.fadeSwing(vanilla, 0);
			assertNotSame(vanilla, unchanged);
			assertMatrix(vanilla, unchanged, 0);
			assertMatrix(new Matrix4f(), MastersViewMotion.fadeSwing(vanilla, 1), 0);
			unchanged.translate(1, 2, 3);
			assertMatrix(original, vanilla, 0);
		}
	}

	@Test
	void swingTranslationAndAngularDistanceShrinkMonotonicallyAtEveryPhaseForBothHands() {
		for (float phase : PHASES) for (int side : new int[] {-1, 1}) {
			Matrix4f vanilla = whack(phase, side);
			float initialDistance = translationLength(vanilla), initialAngle = rotationAngle(vanilla);
			float lastDistance = initialDistance, lastAngle = initialAngle;
			for (int step = 0; step <= 100; step++) {
				float ownership = step / 100F, remaining = 1 - ownership;
				Matrix4f faded = MastersViewMotion.fadeSwing(vanilla, ownership);
				float distance = translationLength(faded), angle = rotationAngle(faded);
				String label = "phase=" + phase + " side=" + side + " ownership=" + ownership;
				assertTrue(distance <= lastDistance + EPSILON, label);
				assertTrue(angle <= lastAngle + .00001F, label);
				assertEquals(initialDistance * remaining, distance, EPSILON, label);
				assertEquals(initialAngle * remaining, angle, .00001F, label);
				assertEquals(1, faded.determinant3x3(), EPSILON, label);
				assertEquals(1, faded.getColumn(0, new Vector3f()).length(), EPSILON, label);
				assertEquals(1, faded.getColumn(1, new Vector3f()).length(), EPSILON, label);
				assertEquals(1, faded.getColumn(2, new Vector3f()).length(), EPSILON, label);
				lastDistance = distance; lastAngle = angle;
			}
		}
	}

	@Test
	void ownershipIsContinuousAtBothEndpointsAndThroughoutTheBlend() {
		for (float phase : PHASES) for (int side : new int[] {-1, 1}) {
			Matrix4f vanilla = whack(phase, side);
			for (float ownership : new float[] {0, .1F, .5F, .9F, 1}) {
				Matrix4f before = MastersViewMotion.fadeSwing(vanilla, ownership - .00001F);
				Matrix4f after = MastersViewMotion.fadeSwing(vanilla, ownership + .00001F);
				assertMatrix(before, after, .00004F);
			}
		}
	}

	@Test
	void fadingALateSwingCannotRewindItIntoTheStrongerMiddleOfItsArc() {
		for (int side : new int[] {-1, 1}) {
			Matrix4f late = whack(.9F, side);
			Matrix4f wronglyResampled = whack(.9F * .5F, side);
			assertTrue(translationLength(wronglyResampled) > translationLength(late) * 2);
			assertTrue(rotationAngle(wronglyResampled) > rotationAngle(late) * 2);
			Matrix4f faded = MastersViewMotion.fadeSwing(late, .5F);
			assertEquals(translationLength(late) * .5F, translationLength(faded), EPSILON);
			assertEquals(rotationAngle(late) * .5F, rotationAngle(faded), EPSILON);
		}
	}

	@Test
	void fadingPreservesHandednessAndTheTranslationDirection() {
		for (float phase : PHASES) for (float weight : new float[] {0, .25F, .5F, .75F, 1}) {
			Matrix4f right = MastersViewMotion.fadeSwing(whack(phase, 1), weight);
			Matrix4f left = MastersViewMotion.fadeSwing(whack(phase, -1), weight);
			Matrix4f reflected = new Matrix4f().scaling(-1, 1, 1).mul(right).scale(-1, 1, 1);
			assertMatrix(reflected, left, EPSILON);
			assertEquals(whack(phase, 1).m30() * (1 - weight), right.m30(), EPSILON);
			assertEquals(whack(phase, 1).m31() * (1 - weight), right.m31(), EPSILON);
			assertEquals(whack(phase, 1).m32() * (1 - weight), right.m32(), EPSILON);
		}
	}

	@Test
	void invalidOwnershipLeavesVanillaUntouchedAndInvalidIntermediateMatricesStayFinite() {
		Matrix4f vanilla = whack(.5F, 1);
		for (float value : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			assertMatrix(vanilla, MastersViewMotion.fadeSwing(vanilla, value), 0);
		}
		assertMatrix(new Matrix4f(), MastersViewMotion.fadeSwing(vanilla, 2), 0);
		assertMatrix(new Matrix4f(), MastersViewMotion.fadeSwing(new Matrix4f().m30(Float.NaN), .5F), 0);
		assertMatrix(new Matrix4f(), MastersViewMotion.fadeSwing(new Matrix4f().m00(Float.POSITIVE_INFINITY), .5F), 0);
	}

	@Test
	void heightCompensationCancelsOnlyTheOwnedHeightDipWithoutChangingOtherCoordinates() {
		for (float inverseHeight : new float[] {0, .1F, .5F, .9F, 1}) {
			for (float weight : new float[] {0, .25F, .5F, .75F, 1}) {
				float baseline = -.52F - .6F * inverseHeight;
				float corrected = baseline + MastersViewMotion.heightCompensation(inverseHeight, weight);
				assertEquals(-.52F - .6F * inverseHeight * (1 - weight), corrected, EPSILON);
			}
		}
		assertEquals(-.3F, MastersViewMotion.heightCompensation(-1, .5F), EPSILON);
		assertEquals(.6F, MastersViewMotion.heightCompensation(2, .5F), EPSILON);
		assertEquals(0, MastersViewMotion.heightCompensation(1, -1));
		assertEquals(.6F, MastersViewMotion.heightCompensation(1, 2));
		for (float invalid : new float[] {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			assertEquals(0, MastersViewMotion.heightCompensation(invalid, 1));
			assertEquals(0, MastersViewMotion.heightCompensation(1, invalid));
		}
	}

	@Test
	void initialEquipProtectsRaisingUntilBothInterpolationEndpointsAreSettled() {
		var transition = new MastersViewMotion.EquipTransition();
		assertTrue(transition.active());
		for (float[] heights : new float[][] {{0, 0}, {0, .4F}, {.4F, .8F}, {.8F, 1}, {.998F, 1}, {1, .998F}}) {
			transition.tick(false, false, heights[0], heights[1]);
			assertTrue(transition.active());
		}
		transition.tick(false, false, .999F, .999F);
		assertFalse(transition.active());
	}

	@Test
	void aRealSwapStaysProtectedAfterTheVisibleItemChangesUntilRaisingFinishes() {
		var transition = settledTransition();
		transition.tick(true, false, 1, 1);
		assertTrue(transition.active(), "A swap takes priority even before height starts falling");
		transition.tick(true, false, 1, .6F);
		transition.tick(true, false, .6F, .2F);
		for (float[] heights : new float[][] {{.2F, 0}, {0, .4F}, {.4F, .8F}, {.8F, 1}}) {
			transition.tick(false, false, heights[0], heights[1]);
			assertTrue(transition.active(), "Replacing the visible item at the bottom cannot release protection");
		}
		transition.tick(false, false, 1, 1);
		assertFalse(transition.active());
	}

	@Test
	void aSettledSameItemAttackDipNeverBecomesAnEquipTransition() {
		var transition = settledTransition();
		for (float[] heights : new float[][] {{1, .6F}, {.6F, .2F}, {.2F, 0}, {0, .4F}, {.4F, .8F}, {.8F, 1}, {1, 1}}) {
			transition.tick(false, false, heights[0], heights[1]);
			assertFalse(transition.active());
		}
	}

	@Test
	void busyHandsReactivateProtectionUntilBothHeightsRecover() {
		var transition = settledTransition();
		transition.tick(false, true, 1, 1);
		assertTrue(transition.active());
		transition.tick(false, true, 1, .6F);
		transition.tick(false, true, .6F, .2F);
		transition.tick(false, false, .2F, .6F);
		assertTrue(transition.active());
		transition.tick(false, false, .6F, 1);
		assertTrue(transition.active());
		transition.tick(false, false, 1, 1);
		assertFalse(transition.active());
	}

	@Test
	void itemUsedAndOwningPlayerResetCanExplicitlyBeginANewTransition() {
		var transition = settledTransition();
		transition.begin();
		assertTrue(transition.active());
		transition.tick(false, false, 1, 0);
		transition.tick(false, false, 0, .4F);
		assertTrue(transition.active());
		transition.tick(false, false, .4F, .8F);
		transition.tick(false, false, .8F, 1);
		assertTrue(transition.active());
		transition.tick(false, false, 1, 1);
		assertFalse(transition.active());
		transition.begin();
		transition.tick(false, false, 0, 0);
		assertTrue(transition.active(), "An owning player reset starts with the new player's equip motion protected");
	}

	@Test
	void nonfiniteHeightsCannotEndAnActiveTransitionOrCreateOneFromAnAttack() {
		for (float invalid : new float[] {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			var transition = new MastersViewMotion.EquipTransition();
			transition.tick(false, false, invalid, 1);
			assertTrue(transition.active());
			transition.tick(false, false, 1, invalid);
			assertTrue(transition.active());
			transition.tick(false, false, 1, 1);
			transition.tick(false, false, invalid, invalid);
			assertFalse(transition.active());
		}
	}

	@Test
	void acceptedArtKeepsTheSameItemThroughoutAnOrdinaryAttackDipButIdleDoesNot() {
		var transition = settledTransition();
		for (float[] heights : new float[][] {{1, .6F}, {.6F, .2F}, {.2F, 0}, {0, .4F}, {.4F, .8F}, {.8F, 1}, {1, 1}}) {
			transition.tick(false, false, heights[0], heights[1]);
			float height = (heights[0] + heights[1]) / 2;
			assertEquals(MastersViewMotion.HandAdmission.ACCEPTED_ART,
				MastersViewMotion.articulatedHandAdmission(true, height, true, true, transition.active()));
			assertEquals(height >= .999F, MastersViewMotion.articulatedHandAdmission(false, height, true, true, transition.active()).admitted());
		}
	}

	@Test
	void acceptedArtCannotTakeOverARealSwapEvenBeforeLoweringOrAfterTheVisibleItemMatches() {
		var transition = settledTransition();
		transition.tick(true, false, 1, 1);
		assertEquals(MastersViewMotion.HandAdmission.EQUIP_OR_USE,
			MastersViewMotion.articulatedHandAdmission(true, 1, true, true, transition.active()));
		for (float[] heights : new float[][] {{1, .6F}, {.6F, .2F}, {.2F, 0}, {0, .4F}, {.4F, .8F}, {.8F, 1}}) {
			transition.tick(false, false, heights[0], heights[1]);
			assertEquals(MastersViewMotion.HandAdmission.EQUIP_OR_USE,
				MastersViewMotion.articulatedHandAdmission(true, (heights[0] + heights[1]) / 2, true, true, transition.active()));
		}
		transition.tick(false, false, 1, 1);
		assertTrue(MastersViewMotion.articulatedHandAdmission(true, 1, true, true, transition.active()).admitted());
		transition.begin(); // Native itemUsed uses the same provenance, including a same-item lowering.
		assertFalse(MastersViewMotion.articulatedHandAdmission(true, .2F, true, true, transition.active()).admitted());
	}

	@Test
	void mismatchedUnknownAndInvalidHandsNeverGainAcceptedArtOwnership() {
		for (float height : new float[] {0, .2F, .9F, 1}) {
			assertEquals(MastersViewMotion.HandAdmission.HELD_ITEM_MISMATCH,
				MastersViewMotion.articulatedHandAdmission(true, height, false, true, false));
			assertEquals(MastersViewMotion.HandAdmission.UNKNOWN_EQUIP,
				MastersViewMotion.articulatedHandAdmission(true, height, true, false, false));
		}
		for (float invalid : new float[] {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			assertEquals(MastersViewMotion.HandAdmission.INVALID_HEIGHT,
				MastersViewMotion.articulatedHandAdmission(true, invalid, true, true, false));
		}
	}

	private static MastersViewMotion.EquipTransition settledTransition() {
		var transition = new MastersViewMotion.EquipTransition();
		transition.tick(false, false, 1, 1);
		assertFalse(transition.active());
		return transition;
	}

	/** Vanilla 26.3 swingArm + applyItemArmAttackTransform, in their original compound order. */
	private static Matrix4f whack(float phase, int side) {
		float root = (float) Math.sqrt(phase), pi = (float) Math.PI;
		float arc = (float) Math.sin(root * pi), squaredArc = (float) Math.sin(phase * phase * pi);
		float radians = pi / 180;
		return new Matrix4f().translation(side * -.4F * arc, .2F * (float) Math.sin(root * 2 * pi),
			-.2F * (float) Math.sin(phase * pi))
			.rotateY(side * (45 - 20 * squaredArc) * radians)
			.rotateZ(side * -20 * arc * radians)
			.rotateX(-80 * arc * radians)
			.rotateY(side * -45 * radians);
	}

	private static float translationLength(Matrix4fc matrix) {
		return matrix.getTranslation(new Vector3f()).length();
	}

	private static float rotationAngle(Matrix4fc matrix) {
		Quaternionf rotation = matrix.getNormalizedRotation(new Quaternionf()).normalize();
		// atan2 retains precision near identity, where acos(w) loses tiny angular differences.
		float sine = (float) Math.sqrt(rotation.x * rotation.x + rotation.y * rotation.y + rotation.z * rotation.z);
		return 2 * (float) Math.atan2(sine, Math.abs(rotation.w));
	}

	private static void assertMatrix(Matrix4fc expected, Matrix4fc actual, float epsilon) {
		assertArrayEquals(expected.get(new float[16]), actual.get(new float[16]), epsilon);
	}
}
