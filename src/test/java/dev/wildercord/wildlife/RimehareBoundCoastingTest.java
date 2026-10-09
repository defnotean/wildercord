package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RimehareBoundCoastingTest {
	private static final float HORIZONTAL = .75F - .45F / 2;
	private static final float VERTICAL = 1;
	private static final float SLAB_DRAG = .546000063419342F;

	@Test
	void theLoggedFinalApproachAlreadyHasEnoughMomentum() {
		Path path = finalNode(true);
		assertTrue(coasts(path, .665583785860739, 100.5, -12.401761844801435,
			-.12389357112694158, .2852089243163002, SLAB_DRAG));
		assertEquals(1, path.getNextNodeIndex());
		assertFalse(path.isDone(), "Only native navigation may consume the endpoint");
		assertEquals(new BlockPos(0, 101, -13), path.getTarget());
	}

	@Test
	void theLoggedLandingStillNeedsItsFirstRecoveryInput() {
		assertFalse(coasts(finalNode(true), .8924950619346909, 100.5, -12.924122451373842,
			-.06526172122249799, -.0641715013598202, SLAB_DRAG));
	}

	@Test
	void theLoggedExtraInputWouldCoastPastTheEndpoint() {
		assertFalse(coasts(finalNode(true), .43182004418969955, 100.5, -11.51815584254853,
			-.12763501777753022, .4824489332677972, SLAB_DRAG));
	}

	@Test
	void intermediateMissingAndConsumedPathsRetainNativeControl() {
		assertFalse(coasts(route(true), .5, 100.5, -12.4, 0, .28, SLAB_DRAG));
		assertFalse(coasts(null, .5, 100.5, -12.4, 0, .28, SLAB_DRAG));
		Path path = finalNode(true);
		path.advance();
		assertFalse(coasts(path, .5, 100.5, -12.4, 0, .28, SLAB_DRAG));
		assertFalse(coasts(new Path(List.of(), new BlockPos(0, 101, -13), false), .5, 100.5, -12.4, 0, .28, SLAB_DRAG));
	}

	@Test
	void aPartialRouteMayCoastToItsActualEndpointWithoutClaimingTheTarget() {
		Path path = finalNode(false);
		assertTrue(coasts(path, .5, 100.5, -12.4, 0, .28, SLAB_DRAG));
		assertFalse(path.canReach());
		assertFalse(path.isDone());
	}

	@Test
	void stationaryInsufficientAndRecedingMomentumCannotWithholdInput() {
		Path path = finalNode(true);
		assertFalse(coasts(path, .5, 100.5, -12.4, 0, 0, SLAB_DRAG));
		assertFalse(coasts(path, .5, 100.5, -12.4, 0, .08, SLAB_DRAG));
		assertFalse(coasts(path, .5, 100.5, -12.4, 0, -.28, SLAB_DRAG));
		assertFalse(coasts(path, .5, 100.5, -12.4, .5, .28, SLAB_DRAG), "Both endpoint axes must qualify");
	}

	@Test
	void coastingUsesTheStrictNativeHorizontalAndVerticalBoundaries() {
		Path path = finalNode(true);
		for (int sign : new int[] {-1, 1}) {
			double boundary = .5 + sign * HORIZONTAL;
			assertFalse(coasts(path, boundary - .01, 100.5, -11.5, .01, 0, 0));
			assertTrue(coasts(path, boundary - sign * .000001 - .01, 100.5, -11.5, .01, 0, 0));
			assertFalse(coasts(path, .49, 101 + sign * VERTICAL, -11.5, .01, 0, 0));
			assertTrue(coasts(path, .49, 101 + sign * (VERTICAL - .000001), -11.5, .01, 0, 0));
		}
	}

	@Test
	void nativeVelocityCutoffIsStrictAndAppliedPerAxis() {
		Path path = finalNode(true);
		double edge = -11.5 - HORIZONTAL;
		assertTrue(coasts(path, .5, 100.5, edge - .002, 0, .003, 0));
		assertFalse(coasts(path, .5, 100.5, edge - .002, 0, Math.nextDown(.003), 0));
		assertFalse(coasts(path, .5 - HORIZONTAL - .002, 100.5, edge - .002,
			Math.nextDown(.003), .003, 0), "A sufficient Z component cannot rescue clipped X momentum");
	}

	@Test
	void actualIceAndBlueIceFrictionChangeWhetherCoastingIsEnough() {
		float ice = RimehareBoundArrival.groundDrag(.98F, 1, 1);
		float blueIce = RimehareBoundArrival.groundDrag(.989F, 1, 1);
		assertEquals(.89180005F, ice);
		assertEquals(.89999F, blueIce);
		assertTrue(coasts(finalNode(true), .5, 100.5, -12.4, 0, .08, ice));
		assertTrue(coasts(finalNode(true), .5, 100.5, -12.4, 0, .08, blueIce));
		assertFalse(coasts(finalNode(true), .5, 100.5, -12.4, 0, .08, SLAB_DRAG));
		assertFalse(coasts(finalNode(true), .5, 100.5, -12.4, 0, .28, ice), "Slippery footing can coast beyond the box");
	}

	@Test
	void groundAndHorizontalAirModifiersUseNativeFloatArithmetic() {
		assertEquals(SLAB_DRAG, RimehareBoundArrival.groundDrag(.6F, 1, 1));
		assertEquals(.72800004F, RimehareBoundArrival.groundDrag(.8F, 1, 1),
			"Slime's friction coefficient is distinct; production separately excludes its step callback");
		float strongFriction = RimehareBoundArrival.groundDrag(.6F, 2, 1);
		float noGroundFriction = RimehareBoundArrival.groundDrag(.6F, 0, 1);
		float strongAirDrag = RimehareBoundArrival.groundDrag(.6F, 1, 3);
		assertFalse(coasts(finalNode(true), .665583785860739, 100.5, -12.401761844801435,
			-.12389357112694158, .2852089243163002, strongFriction));
		assertFalse(coasts(finalNode(true), .5, 100.5, -12.4, 0, .28, noGroundFriction));
		assertTrue(coasts(finalNode(true), .5, 100.5, -12.4, 0, .28, strongAirDrag));
		assertEquals(0F, RimehareBoundArrival.groundDrag(.6F, 10, 1), "Native modifiers clamp at zero");
		assertEquals(1F, RimehareBoundArrival.groundDrag(.6F, 0, 0));
	}

	@Test
	void invalidWaypointTolerancesCannotBroadenTheNativeAcceptanceBox() {
		Path path = finalNode(true);
		for (float invalid : new float[] {Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN, 0, -1}) {
			assertFalse(RimehareBoundArrival.canCoastToFinalWaypoint(path, .5, 100.5, -12.4, 0, .28,
				invalid, VERTICAL, SLAB_DRAG));
			assertFalse(RimehareBoundArrival.canCoastToFinalWaypoint(path, .5, 100.5, -12.4, 0, .28,
				HORIZONTAL, invalid, SLAB_DRAG));
		}
	}

	@Test
	void invalidNondecayingAndUnresolvedLongGlidesFallBackWithoutWaiting() {
		Path path = finalNode(true);
		for (float drag : new float[] {Float.NaN, Float.POSITIVE_INFINITY, -1, 1, 2})
			assertFalse(coasts(path, .5, 100.5, -12.4, 0, .28, drag));
		assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
			assertFalse(coasts(path, .5, 100.5, -12.4, 0, .28, Math.nextDown(1F))));
		assertFalse(coasts(path, Double.NaN, 100.5, -12.4, 0, .28, SLAB_DRAG));
		assertFalse(coasts(path, .5, 100.5, -12.4, Double.POSITIVE_INFINITY, .28, SLAB_DRAG));
		assertTrue(Float.isNaN(RimehareBoundArrival.groundDrag(Float.NaN, 1, 1)));
		assertTrue(Float.isNaN(RimehareBoundArrival.groundDrag(.6F, Double.NaN, 1)));
		assertTrue(Float.isNaN(RimehareBoundArrival.groundDrag(.6F, 1, Double.POSITIVE_INFINITY)));
	}

	private static boolean coasts(Path path, double x, double y, double z, double vx, double vz, float drag) {
		return RimehareBoundArrival.canCoastToFinalWaypoint(path, x, y, z, vx, vz, HORIZONTAL, VERTICAL, drag);
	}

	private static Path route(boolean canReach) {
		return new Path(List.of(new Node(0, 101, -11), new Node(0, 101, -12)), new BlockPos(0, 101, -13), canReach);
	}

	private static Path finalNode(boolean canReach) {
		Path path = route(canReach);
		path.advance();
		return path;
	}
}
