package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MasterHitReceiptResultTest {
	@Test void restorationDoesNotEraseResolvedDamage() {
		assertTrue(new MasterHitReceipt.Result(0, 4, 0, true).damaging());
		assertTrue(new MasterHitReceipt.Result(0, 0, 28, true).damaging());
		assertTrue(new MasterHitReceipt.Result(0, 2, 6, true).damaging());
	}
	@Test void unrelatedNetHealthLossCannotManufactureAMatchingHit() {
		assertFalse(new MasterHitReceipt.Result(28, 0, 0, false).damaging());
		assertFalse(new MasterHitReceipt.Result(28, 28, 0, false).damaging());
		assertFalse(new MasterHitReceipt.Result(28, 0, 0, true).damaging());
	}
	@Test void invalidOrZeroResolvedLossCannotInterrupt() {
		for (float value : new float[] {0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
			assertFalse(new MasterHitReceipt.Result(0, value, 0, true).damaging());
			assertFalse(new MasterHitReceipt.Result(0, 0, value, true).damaging());
		}
	}
}
