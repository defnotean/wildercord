package dev.wildercord.aura.world;

import dev.wildercord.aura.ArticulatedCombatPose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterKilnAnimationRulesTest {
	@Test
	void onlyKilnAddsAWholeModelTurnAndItNeverRewindsDuringRecovery() {
		assertEquals(9, MasterAnimationRules.KILN_RING);
		for (int id = 1; id <= 8; id++) for (float age : new float[] {4, 12, 20, 32, 40, 65})
			assertEquals(0, MasterAnimationRules.sample(id, age, 40, 1, 47).rootYaw());
		float previous = 0;
		for (float age = 0; age < 88; age += .125F) {
			var pose = sample(age);
			assertTrue(Float.isFinite(pose.rootYaw()) && pose.rootYaw() <= previous + .00001F);
			assertTrue(Math.abs(pose.body().y()) < .8F, "Only the root turns all the way; torso twist remains modest");
			assertEquals(.24F, pose.stance(), .00001F);
			if (age < 8) assertEquals(0, pose.rootYaw());
			if (age >= 32) assertEquals(-2 * Math.PI, pose.rootYaw(), .000001F);
			previous = pose.rootYaw();
		}
		assertEquals(0, sample(88).rootYaw());
		assertSame(MasterAnimationRules.NONE, MasterAnimationRules.sample(0, 20, 40, 1, 47));
		for (float age : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY}) assertEquals(0, MasterAnimationRules.kilnTurn(age, 40));
	}

	@Test
	void lowCircularRigidBladeAgreesWithSegmentedReleaseAndIsMirrored() {
		var blade = new ArticulatedCombatPose.Vec3(0, .17364818F, -.98480775F);
		for (float age : new float[] {8, 32, 40, 44}) {
			var pose = sample(age);
			var sword = pose.sword();
			var right = matrix(sword).multiply(rotation((float) Math.toRadians(pose.bladeTilt()), 0, 0));
			var left = matrix(MasterAnimationRules.mirrored(sword, true)).multiply(rotation((float) Math.toRadians(pose.bladeTilt()), 0, 0));
			var a = right.direction(blade); var b = left.direction(blade);
			assertEquals(0, a.y(), .00001F, "The held blade releases parallel to the fixed ground annulus");
			assertEquals(-a.x(), b.x(), .00001F); assertEquals(a.y(), b.y(), .00001F); assertEquals(a.z(), b.z(), .00001F);
		}
		assertTrue(sample(32).sword().y() > 0 && sample(40).sword().y() < 0, "The low coil opens into a circular cross-body release");
		assertNotEquals(sample(32).offhand(), sample(40).offhand());
		assertNotEquals(sample(40), sample(44));
	}

	private static MasterAnimationRules.Pose sample(float age) { return MasterAnimationRules.sample(9, age, 40, 1, 47); }
	private static ArticulatedCombatPose.Matrix matrix(MasterAnimationRules.Joint joint) { return rotation(joint.x(), joint.y(), joint.z()); }
	private static ArticulatedCombatPose.Matrix rotation(float x, float y, float z) {
		return new ArticulatedCombatPose.Transform(0, 0, 0, new ArticulatedCombatPose.Rotation(x, y, z)).matrix();
	}
}
