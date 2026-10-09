package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormDashRulesTest {
	private static final int CINDER = FormDashRules.CINDER_LUNGE, REED = FormDashRules.REED_SLIP;
	@Test void lessonsNeedTheirOwnSchoolClearAndStage() {
		for (int stage = 0; stage <= AuraRules.SOVEREIGN + 1; stage++) {
			assertFalse(FormDashRules.eligible(CINDER, stage, false)); assertFalse(FormDashRules.eligible(REED, stage, false));
			assertEquals(stage >= AuraRules.SOVEREIGN, FormDashRules.eligible(CINDER, stage, true));
			assertEquals(stage >= AuraRules.FORM, FormDashRules.eligible(REED, stage, true));
			assertFalse(FormDashRules.eligible(1, stage, true), "Wall Turn's number is not a field form");
		}
		assertNotEquals(FormDashRules.school(CINDER), FormDashRules.school(REED));
		assertEquals(dev.wildercord.aura.world.MastersRules.EMBER, FormDashRules.school(CINDER));
		assertEquals(dev.wildercord.aura.world.MastersRules.GALE, FormDashRules.school(REED));
	}
	@Test void fixedPriceRestAndRecoveryGateEveryUse() {
		for (int form : new int[] {CINDER, REED}) {
			double cost = FormDashRules.cost(form);
			for (double aura : new double[] {0, cost - .001, Double.NaN, Double.NEGATIVE_INFINITY}) assertFalse(FormDashRules.canPay(form, aura, 100, 100, 100));
			assertTrue(FormDashRules.canPay(form, cost, 100, 100, 100));
			assertFalse(FormDashRules.canPay(form, 500, 99, 100, 0), "rest");
			assertFalse(FormDashRules.canPay(form, 500, 99, 0, 100), "recovery");
			assertTrue(FormDashRules.rest(form) > FormDashRules.travelTicks(form) + FormDashRules.recovery(form, false), "rest outlasts a full use");
			assertTrue(FormDashRules.recovery(form, false) >= 10, "every clean end is exposed for at least half a second");
		}
		for (int bad : new int[] {0, 1, 4, -2}) assertFalse(FormDashRules.canPay(bad, 500, 100, 0, 0));
		assertTrue(FormDashRules.recovery(CINDER, true) > FormDashRules.recovery(CINDER, false), "a stopped lunge costs more time than a clean one");
		assertTrue(FormDashRules.cost(CINDER) > FormDashRules.cost(REED) && FormDashRules.rest(CINDER) > FormDashRules.rest(REED));
	}
	@Test void travelIsShortAndEvenlyPacedUnderTheSweptStepLimit() {
		for (int form : new int[] {CINDER, REED}) {
			assertTrue(FormDashRules.stride(form) < 1, "each swept stretch is under a block, well inside the swept-step limit");
			assertEquals(FormDashRules.distance(form), FormDashRules.stride(form) * FormDashRules.travelTicks(form), 1.0E-9);
			assertTrue(FormDashRules.recovery(form, true) <= FormDashRules.MAX_TICKS && FormDashRules.CINDER_SET_TICKS <= FormDashRules.MAX_TICKS, "every phase fits the event bound");
		}
		assertTrue(FormDashRules.CINDER_DISTANCE <= 5 && FormDashRules.REED_DISTANCE < 3);
		assertTrue(FormDashRules.STEP < 1, "a full block ends travel");
		assertEquals(1 << CINDER | 1 << REED, FormDashRules.bit(CINDER) | FormDashRules.bit(REED));
		assertEquals(0, FormDashRules.bit(1)); assertEquals(0, FormDashRules.bit(31));
	}
	@Test void slipNeverClosesDistanceAndDefaultsToABackstep() {
		for (float yaw = -360; yaw <= 360; yaw += 7.5F) {
			double rad = Math.toRadians(yaw), fx = -Math.sin(rad), fz = Math.cos(rad);
			double[] back = FormDashRules.slip(yaw, 0, 0);
			assertEquals(-1, back[0] * fx + back[1] * fz, 1.0E-9, "no intent steps straight back");
			assertArrayEquals(back, FormDashRules.slip(yaw, Double.NaN, 1), 1.0E-9);
			assertArrayEquals(back, FormDashRules.slip(yaw, fx, fz), 1.0E-9, "pure forward intent becomes a backstep");
			for (int a = 0; a < 16; a++) {
				double angle = a * Math.PI / 8;
				double[] lane = FormDashRules.slip(yaw, Math.cos(angle) * 3, Math.sin(angle) * 3);
				assertEquals(1, Math.hypot(lane[0], lane[1]), 1.0E-9);
				assertTrue(lane[0] * fx + lane[1] * fz <= 1.0E-9, "never forward");
			}
			double[] left = FormDashRules.slip(yaw, fz, -fx), diagonal = FormDashRules.slip(yaw, fz + fx, -fx + fz);
			assertArrayEquals(left, diagonal, 1.0E-9, "a forward-diagonal intent keeps only its side share");
		}
	}
	@Test void posesEndAndStayBoundedForEveryPhase() {
		for (int form : new int[] {CINDER, REED}) for (int phase = 0; phase <= 8; phase++) for (int remaining : new int[] {0, 3, 16, 24}) {
			for (float age = 0; age < 40; age += .5F) assertNotNull(FormDashAnimation.sample(form, phase, remaining, age));
			assertSame(MastersArtAnimation.NONE, FormDashAnimation.sample(form, phase, remaining, 40));
		}
		assertSame(MastersArtAnimation.NONE, FormDashAnimation.sample(CINDER, FormDashRules.SET, 5, Float.NaN));
		assertSame(MastersArtAnimation.NONE, FormDashAnimation.sample(1, FormDashRules.SET, 5, 1));
		assertNotSame(MastersArtAnimation.NONE, FormDashAnimation.sample(CINDER, FormDashRules.SET, 5, 1));
	}
	@Test void fieldFormChannelRefusesStoneHingeEquipSoItCannotCountAsAPress() {
		var input = FormDashRules.input();
		assertFalse(input.admit(1, StoneHingeRules.EQUIP, 1), "action 5 is not a field-form action");
		assertTrue(input.admit(1, WallTurnRules.PRESS, 1), "a refused action does not advance the sequence");
		assertFalse(input.admit(2, StoneHingeRules.EQUIP, 3), "and it cannot release-and-repress either");
		assertFalse(input.admit(3, WallTurnRules.PRESS, 4), "the held press still needs a real release");
		for (int action = WallTurnRules.PRESS; action <= WallTurnRules.UNEQUIP; action++) assertTrue(WallTurnRules.action(action));
		assertTrue(new WallTurnRules.Input().admit(1, StoneHingeRules.EQUIP, 1), "the Master-form channel still carries Stone Hinge's equip");
	}
}
