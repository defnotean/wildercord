package dev.wildercord.aura.world;

import dev.wildercord.aura.ArticulatedCombatPose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Classic fallback shares the same committed clock without borrowing a Thrust clip. */
class MasterStoneFaultMarchAnimationTest {
	@Test
	void fixedClockKeepsAllThreePulsesBracedAndTheFullExposedRecovery() {
		assertEquals(10, MasterAnimationRules.STONE_FAULT_MARCH);
		for (int tell : new int[] {1, 32, 80}) for (int active : new int[] {1, 10}) for (int recovery : new int[] {1, 63, 120})
			for (float age : new float[] {0, 12, 24, 32, 40, 48, 72, 95.999F, 96})
				assertEquals(sample(age), MasterAnimationRules.sample(10, age, tell, active, recovery));
		for (float age : new float[] {32, 40, 48, 60, 72}) assertEquals(1, sample(age).weight());
		assertTrue(sample(90).weight() > 0);
		assertSame(MasterAnimationRules.NONE, sample(StoneMarchRules.END));
		for (float age : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 100})
			assertSame(MasterAnimationRules.NONE, sample(age));
		for (int[] timing : new int[][] {{0, 1, 63}, {81, 1, 63}, {32, 0, 63}, {32, 11, 63}, {32, 1, 0}, {32, 1, 121}})
			assertSame(MasterAnimationRules.NONE, MasterAnimationRules.sample(10, 20, timing[0], timing[1], timing[2]));
	}

	@Test
	void longOverheadDropAndEmbeddedBladeShocksHaveAnOriginalSilhouette() {
		var overhead = sample(24); var strike = sample(32); var brace = sample(36); var recoil = sample(40); var extract = sample(72);
		assertTrue(overhead.sword().x() < -2.8F && overhead.offhand().x() < -2.2F, "Both arms gather a high lever");
		assertTrue(strike.body().x() > .5F && strike.sword().x() > -.4F, "The single downward stroke opens the body");
		assertTrue(recoil.body().x() > brace.body().x() && recoil.sword().x() > brace.sword().x(), "A small pulse compresses the planted brace");
		assertTrue(extract.sword().x() < brace.sword().x() - .5F && extract.offhand().z() > .6F, "Slow extraction leaves a readable open guard");
		for (int old = 1; old <= 9; old++) {
			var other = MasterAnimationRules.sample(old, 32, 32, 1, 63);
			assertNotEquals(strike.sword(), other.sword()); assertNotEquals(strike.body(), other.body());
		}
		var blade = new ArticulatedCombatPose.Vec3(0, .17364818F, -.98480775F);
		assertTrue(weapon(overhead, false).direction(blade).y() < -.99F);
		for (float age = 32; age <= 56; age += .125F) {
			var pose = sample(age); var right = weapon(pose, false).direction(blade); var left = weapon(pose, true).direction(blade);
			assertTrue(right.y() > .99F, "The later beats keep the blade down");
			assertEquals(-right.x(), left.x(), .00001F); assertEquals(right.y(), left.y(), .00001F); assertEquals(right.z(), left.z(), .00001F);
		}
	}

	@Test
	void fixedFacingAndPlantedStanceRemainBoundedAndEveryBeatIsContinuous() {
		for (float age = 0; age < StoneMarchRules.END; age += .125F) {
			var pose = sample(age);
			assertEquals(0, pose.rootYaw()); assertEquals(0, pose.body().y()); assertEquals(0, pose.body().z());
			assertEquals(.32F, pose.stance());
			for (float value : values(pose)) assertTrue(Float.isFinite(value) && Math.abs(value) < 120);
			float angle = pose.stance() * pose.weight(), lower = MasterAnimationRules.lower(angle);
			assertEquals(24, 12 + lower + 12 * (float) Math.cos(angle) + 2 * Math.abs((float) Math.sin(angle)), .00001F);
		}
		for (float edge : new float[] {0, 12, 24, 32, 36, 40, 44, 48, 56, 72, 96}) {
			float[] before = values(sample(edge - .0001F)), after = values(sample(edge + .0001F));
			for (int i = 0; i < before.length; i++) assertEquals(before[i], after[i], .003F, "Continuous weighted joint at " + edge);
		}
	}

	private static MasterAnimationRules.Pose sample(float age) { return MasterAnimationRules.sample(10, age, 32, 1, 63); }
	private static ArticulatedCombatPose.Matrix weapon(MasterAnimationRules.Pose pose, boolean left) {
		var arm = MasterAnimationRules.mirrored(pose.sword(), left);
		return new ArticulatedCombatPose.Transform(0, 0, 0, new ArticulatedCombatPose.Rotation(arm.x(), arm.y(), arm.z())).matrix()
			.multiply(new ArticulatedCombatPose.Transform(0, 0, 0, new ArticulatedCombatPose.Rotation((float) Math.toRadians(pose.bladeTilt()), 0, 0)).matrix());
	}
	private static float[] values(MasterAnimationRules.Pose pose) {
		float w = pose.weight();
		return new float[] {w, pose.body().x() * w, pose.body().y() * w, pose.body().z() * w,
			pose.sword().x() * w, pose.sword().y() * w, pose.sword().z() * w, pose.offhand().x() * w,
			pose.offhand().y() * w, pose.offhand().z() * w, pose.stance() * w, pose.bladeTilt() * w};
	}
}
