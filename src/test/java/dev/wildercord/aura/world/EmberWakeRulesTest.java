package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmberWakeRulesTest {
	@Test
	void existingFourMoveIdsAndLearnedTimingArePreserved() {
		assertEquals(0, MastersRules.Move.SWEEP.ordinal());
		assertEquals(1, MastersRules.Move.THRUST.ordinal());
		assertEquals(2, MastersRules.Move.CRESCENT.ordinal());
		assertEquals(3, MastersRules.Move.BREAK_CAST.ordinal());
		assertEquals(4, MastersRules.Move.CINDER_WAKE.ordinal());
		assertEquals(18, MastersRules.Move.SWEEP.tell);
		assertEquals(20, MastersRules.Move.SWEEP.recovery);
	}

	@Test
	void emberAddsPredictableTwoBeatPressureWithoutChangingOtherSchools() {
		for (int phase = 0; phase < 3; phase++) {
			int count = 0;
			for (int step = 0; step < 12; step++) {
				if (EmberWakeRules.next(MastersRules.EMBER, step, phase, 3)) count++;
				assertFalse(EmberWakeRules.next(MastersRules.GALE, step, phase, 3));
				assertFalse(EmberWakeRules.next(MastersRules.STONE, step, phase, 3));
			}
			assertEquals(12 / (4 - phase), count);
		}
		assertFalse(EmberWakeRules.next(MastersRules.EMBER, 0, 0, 7));
		assertFalse(EmberWakeRules.next(MastersRules.EMBER, 0, 0, Double.NaN));
		assertTrue(EmberWakeRules.TELL >= 24);
		assertTrue(EmberWakeRules.AFTERBURN_TELL >= 24, "A separately warned second beat gives more than a second to leave");
		assertTrue(EmberWakeRules.RECOVERY - EmberWakeRules.AFTERBURN_TELL >= 20, "A clear counter window follows even the second beat");
		assertTrue(EmberWakeRules.COST > MastersRules.ATTACK_COST);
	}

	@Test
	void backstepThenSidestepAnswersDifferentShapes() {
		assertTrue(MastersRules.hits(MastersRules.Move.CINDER_WAKE, 3, 1.5, 0));
		assertFalse(MastersRules.hits(MastersRules.Move.CINDER_WAKE, 5, 0, 0), "A backstep avoids the broad cut");
		assertTrue(EmberWakeRules.hits(5, 0, 0), "Staying in the backstep lane is threatened by the new visible warning");
		assertFalse(EmberWakeRules.hits(5, 1.1, 0), "One later sideways step clears the wake");
		assertFalse(EmberWakeRules.hits(.5, 0, 0), "Moving through beside the master also leaves the wake");
		assertFalse(EmberWakeRules.hits(8, 0, 0));
		assertFalse(EmberWakeRules.hits(3, 0, 2));
		assertFalse(EmberWakeRules.hits(Double.NaN, 0, 0));
		assertFalse(EmberWakeRules.hits(2, Double.POSITIVE_INFINITY, 0));
	}

	@Test
	void partyLanesStaySeparatedAndBoundedWithTheSameDamageAndTiming() {
		assertEquals(1, EmberWakeRules.lanes(1).size());
		assertEquals(2, EmberWakeRules.lanes(4).size());
		assertEquals(3, EmberWakeRules.lanes(8).size());
		assertEquals(EmberWakeRules.lanes(8), EmberWakeRules.lanes(999));
		for (int count = 1; count <= 8; count++) {
			var lanes = EmberWakeRules.lanes(count);
			for (int i = 1; i < lanes.size(); i++) assertTrue(lanes.get(i) - lanes.get(i - 1) - 2 * EmberWakeRules.HALF_WIDTH >= 1.6 - 1e-6);
			assertEquals(EmberWakeRules.CUT_DAMAGE, MastersRules.damage(count, MastersRules.EMBER, MastersRules.Move.CINDER_WAKE));
		}
	}

	@Test
	void ignitionCannotHappenBeforeWarningOrAfterItExpires() {
		for (long now = 90; now < 140; now++) assertEquals(now == 126, EmberWakeRules.ignites(now, 100));
	}

	@Test
	void bothOriginalGesturesMeetTheirAuthoritativeHitFramesAndRemainContinuous() {
		int tell = EmberWakeRules.TELL, recovery = EmberWakeRules.RECOVERY;
		var first = pose(tell);
		var second = pose(tell + EmberWakeRules.AFTERBURN_TELL);
		assertEquals(1, first.weight());
		assertEquals(1, second.weight());
		assertTrue(first.sword().y() < -.9F, "The first beat cuts across");
		assertTrue(second.bladeTilt() < -100, "The second beat drops the point toward the marked floor");
		assertNotEquals(first, second);
		for (float age : new float[] {tell + 6, tell + EmberWakeRules.AFTERBURN_TELL - 8, tell + EmberWakeRules.AFTERBURN_TELL, tell + EmberWakeRules.AFTERBURN_TELL + 4}) {
			var a = pose(age - .0001F); var b = pose(age + .0001F);
			assertEquals(a.weight(), b.weight(), .001F);
			assertEquals(a.sword().x() * a.weight(), b.sword().x() * b.weight(), .001F);
			assertEquals(a.bladeTilt() * a.weight(), b.bladeTilt() * b.weight(), .01F);
		}
		assertTrue(pose(tell + recovery - .01F).weight() < .0001F);
		assertSame(MasterAnimationRules.NONE, pose(tell + recovery));
	}

	@Test
	void ignitionChamberKeepsBothRigidArmsOutsideTheHeadCube() {
		var pose = pose(EmberWakeRules.TELL + EmberWakeRules.AFTERBURN_TELL - 8);
		assertEquals(1, pose.weight());
		for (boolean left : new boolean[] {false, true}) {
			var body = MasterAnimationRules.mirrored(pose.body(), left);
			double[][] head = axes(MasterAnimationRules.mirrored(pose.head(), left));
			double[] headCentre = add(anchor(body, pose.stance(), 0, 0, 0), turn(head, new double[] {0, -4, 0}));
			for (boolean main : new boolean[] {false, true}) {
				boolean armLeft = main == left;
				double[][] arm = axes(MasterAnimationRules.mirrored(main ? pose.sword() : pose.offhand(), left));
				double[] centre = add(anchor(body, pose.stance(), armLeft ? 5 : -5, 2, 0), turn(arm, new double[] {armLeft ? 1 : -1, 4, 0}));
				assertTrue(separation(headCentre, head, new double[] {4, 4, 4}, centre, arm, new double[] {2, 6, 2}) > .15,
					"The accepted ignition chamber clears the registered eight-pixel head with either hand");
			}
		}
	}

	/** ModelPart's Z/Y/X rotations, as world-space columns, for a pure rigid-box regression. */
	private static double[][] axes(MasterAnimationRules.Joint joint) {
		double[][] result = new double[3][];
		for (int i = 0; i < 3; i++) {
			double x = i == 0 ? 1 : 0, y = i == 1 ? 1 : 0, z = i == 2 ? 1 : 0;
			double ay = y * Math.cos(joint.x()) - z * Math.sin(joint.x()), az = y * Math.sin(joint.x()) + z * Math.cos(joint.x());
			double bx = x * Math.cos(joint.y()) + az * Math.sin(joint.y()), bz = -x * Math.sin(joint.y()) + az * Math.cos(joint.y());
			result[i] = new double[] {bx * Math.cos(joint.z()) - ay * Math.sin(joint.z()), bx * Math.sin(joint.z()) + ay * Math.cos(joint.z()), bz};
		}
		return result;
	}

	private static double[] anchor(MasterAnimationRules.Joint body, float stance, float x, float y, float z) {
		var p = MasterAnimationRules.pivot(body, MasterAnimationRules.lower(stance), x, y, z);
		return new double[] {p.x(), p.y(), p.z()};
	}

	private static double separation(double[] ac, double[][] a, double[] ah, double[] bc, double[][] b, double[] bh) {
		var axes = new java.util.ArrayList<double[]>();
		java.util.Collections.addAll(axes, a); java.util.Collections.addAll(axes, b);
		for (double[] x : a) for (double[] y : b) axes.add(new double[] {x[1] * y[2] - x[2] * y[1], x[2] * y[0] - x[0] * y[2], x[0] * y[1] - x[1] * y[0]});
		double gap = -Double.MAX_VALUE;
		for (double[] axis : axes) {
			double length = Math.sqrt(dot(axis, axis));
			if (length < 1e-8) continue;
			double radius = 0;
			for (int i = 0; i < 3; i++) radius += Math.abs(dot(axis, a[i])) * ah[i] + Math.abs(dot(axis, b[i])) * bh[i];
			gap = Math.max(gap, (Math.abs(dot(axis, new double[] {bc[0] - ac[0], bc[1] - ac[1], bc[2] - ac[2]})) - radius) / length);
		}
		return gap;
	}

	private static double[] turn(double[][] axes, double[] value) {
		return new double[] {axes[0][0] * value[0] + axes[1][0] * value[1] + axes[2][0] * value[2], axes[0][1] * value[0] + axes[1][1] * value[1] + axes[2][1] * value[2], axes[0][2] * value[0] + axes[1][2] * value[1] + axes[2][2] * value[2]};
	}
	private static double[] add(double[] a, double[] b) { return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }
	private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

	private static MasterAnimationRules.Pose pose(float age) {
		return MasterAnimationRules.sample(MasterAnimationRules.CINDER_WAKE, age, EmberWakeRules.TELL, 1, EmberWakeRules.RECOVERY - 1);
	}
}
