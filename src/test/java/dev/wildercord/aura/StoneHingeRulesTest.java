package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoneHingeRulesTest {
	@Test void unlockNeedsSovereignAndAStoneClear() {
		assertTrue(StoneHingeRules.eligible(5, true));
		assertFalse(StoneHingeRules.eligible(4, true));
		assertFalse(StoneHingeRules.eligible(5, false));
	}
	@Test void paymentNeedsAuraRestAndNoSpentAirStep() {
		assertTrue(StoneHingeRules.canPay(20, 100, 100, false));
		assertFalse(StoneHingeRules.canPay(19.9, 100, 100, false));
		assertFalse(StoneHingeRules.canPay(50, 99, 100, false));
		assertFalse(StoneHingeRules.canPay(50, 100, 100, true));
		assertFalse(StoneHingeRules.canPay(Double.NaN, 100, 100, false));
	}
	@Test void exactlyOneStrafePicksTheSide() {
		assertEquals(1, StoneHingeRules.side(true, false));
		assertEquals(-1, StoneHingeRules.side(false, true));
		assertEquals(0, StoneHingeRules.side(true, true));
		assertEquals(0, StoneHingeRules.side(false, false));
	}
	@Test void onlyTheNativeImpulseTurnsTowardTheHeldSideWithItsMagnitude() {
		for (float yaw : new float[] {0, 37, 90, -135, 180}) for (int side : new int[] {1, -1}) {
			double[] lat = StoneHingeRules.lateral(yaw, side);
			double r = Math.toRadians(yaw), fx = -Math.sin(r), fz = Math.cos(r);
			// A frontal blow pushes backward: after = before/2 + impulse.
			double bx = .2, bz = -.1, ix = -fx * .4, iz = -fz * .4;
			double[] t = StoneHingeRules.turn(bx, bz, bx * .5 + ix, bz * .5 + iz, yaw, side);
			assertNotNull(t);
			double nx = t[0] - bx * .5, nz = t[1] - bz * .5;
			assertEquals(.4, Math.hypot(nx, nz), 1e-9, "the impulse keeps its strength");
			assertTrue(nx * lat[0] + nz * lat[1] > .399, "the impulse goes to the held side");
			assertEquals(0, nx * ix + nz * iz, 1e-9, "a quarter turn");
		}
		assertNull(StoneHingeRules.turn(.2, .2, .1, .1, 0, 1), "no impulse, nothing to turn");
		assertNull(StoneHingeRules.turn(0, 0, Double.NaN, 0, 0, 1));
	}
	@Test void anObliqueShoveInsideTheConeGoesPurelySidewaysAndOutsideItStands() {
		for (float yaw : new float[] {0, 37, 90, -135, 180}) for (int side : new int[] {1, -1}) {
			double r = Math.toRadians(yaw), fx = -Math.sin(r), fz = Math.cos(r);
			double[] lat = StoneHingeRules.lateral(yaw, side);
			for (double degrees : new double[] {-55, -45, -20, 20, 45, 55}) {
				// A shove `degrees` off straight back, toward either side.
				double a = Math.toRadians(degrees), bx = -fx, bz = -fz;
				double ix = (bx * Math.cos(a) - bz * Math.sin(a)) * .4, iz = (bx * Math.sin(a) + bz * Math.cos(a)) * .4;
				double[] t = StoneHingeRules.turn(.1, -.3, .05 + ix, -.15 + iz, yaw, side);
				assertNotNull(t, "inside the 60 degree cone behind the plant: " + degrees);
				double nx = t[0] - .05, nz = t[1] + .15;
				assertEquals(0, nx * fx + nz * fz, 1e-9, "no forward or backward part is left");
				assertEquals(.4, nx * lat[0] + nz * lat[1], 1e-9, "the whole strength goes to the held side");
			}
			for (double degrees : new double[] {65, 90, -90, 135, 180}) {
				double a = Math.toRadians(degrees), bx = -fx, bz = -fz;
				double ix = (bx * Math.cos(a) - bz * Math.sin(a)) * .4, iz = (bx * Math.sin(a) + bz * Math.cos(a)) * .4;
				assertNull(StoneHingeRules.turn(0, 0, ix, iz, yaw, side), "outside the cone the native shove stands: " + degrees);
			}
			// A shove exactly along the held side (cross product zero with the lateral axis's normal) is a side hit, not a frontal one.
			assertNull(StoneHingeRules.turn(0, 0, lat[0] * .4, lat[1] * .4, yaw, side));
			assertNull(StoneHingeRules.turn(0, 0, -lat[0] * .4, -lat[1] * .4, yaw, side));
		}
		assertNull(StoneHingeRules.turn(0, 0, 0, -.4, 0, 0), "no side, no turn");
	}
	@Test void frontalIsMeasuredAgainstThePlantedFacing() {
		assertTrue(StoneHingeRules.frontal(0, 0, 2));
		assertTrue(StoneHingeRules.frontal(0, 1, 1.8));
		assertFalse(StoneHingeRules.frontal(0, 1, .2), "a side attacker is not frontal");
		assertFalse(StoneHingeRules.frontal(0, 0, -2), "nor one behind");
		assertTrue(StoneHingeRules.frontal(90, -2, 0), "yaw 90 faces -X");
		assertFalse(StoneHingeRules.frontal(90, 0, 2));
		assertFalse(StoneHingeRules.frontal(0, 0, 0), "an attacker on top of the body has no direction");
		assertFalse(StoneHingeRules.frontal(Float.NaN, 0, 2));
	}
	@Test void oldSavesLoadWithoutTheHingeAndKeepWallTurn() {
		var old = com.google.gson.JsonParser.parseString("{\"learned\":true,\"equipped\":1,\"ready_at\":40}");
		var p = MasterForms.Progress.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow();
		assertTrue(p.learned() && !p.hingeLearned());
		assertEquals(MasterForms.WALL_TURN, p.equipped());
		assertFalse(p.knows(MasterForms.STONE_HINGE));
		var hinge = new MasterForms.Progress(false, MasterForms.STONE_HINGE, 0, false, false, 0, 0, true);
		assertEquals(MasterForms.STONE_HINGE, hinge.equipped());
		assertEquals(0, new MasterForms.Progress(true, MasterForms.STONE_HINGE, 0, false, false, 0, 0, false).equipped(), "an unlearned form never stays equipped");
		var json = MasterForms.Progress.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, hinge).getOrThrow();
		assertEquals(hinge, MasterForms.Progress.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow());
	}
	@Test void syncCodecRoundTripsAllEightFields() {
		var full = new MasterForms.Progress(true, MasterForms.STONE_HINGE, 4000, true, true, 3990, 3995, true);
		for (var p : new MasterForms.Progress[] {full, MasterForms.Progress.NONE, new MasterForms.Progress(true, MasterForms.WALL_TURN, 7, false, true, 3, 5, false)}) {
			var buf = io.netty.buffer.Unpooled.buffer();
			MasterForms.Progress.STREAM_CODEC.encode(buf, p);
			assertEquals(p, MasterForms.Progress.STREAM_CODEC.decode(buf));
			assertEquals(0, buf.readableBytes(), "nothing left over");
		}
	}
	@Test void posesFollowAcceptedPhasesOnly() {
		var plant = StoneHingeAnimation.sample(StoneHingeRules.BRACE, 6, 1);
		var hold = StoneHingeAnimation.sample(StoneHingeRules.CATCH, 12, 2);
		var turn = StoneHingeAnimation.sample(StoneHingeRules.TURN, 10, 1);
		assertTrue(plant.weight() > 0 && hold.weight() > 0 && turn.weight() > 0);
		assertNotEquals(hold.body(), turn.body());
		assertNotEquals(hold.hand(), turn.hand());
		assertSame(MastersArtAnimation.NONE, StoneHingeAnimation.sample(StoneHingeRules.TURN, 10, 10));
		assertSame(MastersArtAnimation.NONE, StoneHingeAnimation.sample(StoneHingeRules.SPENT, 10, 4));
		assertSame(MastersArtAnimation.NONE, StoneHingeAnimation.sample(StoneHingeRules.BRACE, 6, Float.NaN));
		assertEquals(WallTurnAnimation.sample(WallTurnRules.ABORT, 5, 1).weight(), StoneHingeAnimation.sampleForm(WallTurnRules.ABORT, 5, 1).weight());
		for (int phase = StoneHingeRules.BRACE; phase <= StoneHingeRules.SPENT; phase++) for (boolean left : new boolean[] {false, true}) {
			var pose = StoneHingeAnimation.sample(phase, 6, .5F);
			var view = MastersArtAnimation.view(pose, left, 0, 0, 0);
			assertTrue(Float.isFinite(view.transform().pitch()) && view.transform().z() <= 0);
			assertTrue(Math.abs(view.transform().x()) <= .15 && Math.abs(view.transform().roll()) <= 20);
		}
	}
}
