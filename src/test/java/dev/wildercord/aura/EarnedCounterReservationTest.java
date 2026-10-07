package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.aura.EarnedCounterReservation.Take.*;

class EarnedCounterReservationTest {
	@Test void ordinaryAdmissionIsOneUse() {
		var receipt = new EarnedCounterReservation();
		assertEquals(VALID, receipt.take(null, 200));
		assertEquals(UNAVAILABLE, receipt.take(null, 200));
		assertThrows(IllegalStateException.class, () -> receipt.hold(new Object(), 201));
	}
	@Test void onlyOriginalClashCanConsumeItsReceipt() {
		var receipt = new EarnedCounterReservation(); var clash = new Object();
		receipt.hold(clash, 100);
		assertEquals(UNAVAILABLE, receipt.take(null, 110));
		assertEquals(UNAVAILABLE, receipt.take(new Object(), 143));
		assertEquals(VALID, receipt.take(clash, 143));
		assertEquals(UNAVAILABLE, receipt.take(clash, 143));
	}
	@Test void existingClashLifetimeIsExactly43AndCannotExtendOrRenewEvidence() {
		assertEquals(43, ClashRules.serverLength());
		var receipt = new EarnedCounterReservation(); var clash = new Object();
		receipt.hold(clash, 100);
		assertThrows(IllegalStateException.class, () -> receipt.hold(clash, 120));
		assertEquals(STALE, receipt.take(clash, 144));
		assertEquals(UNAVAILABLE, receipt.take(clash, 143), "Expiry retires the receipt, even if the clock reverses");
	}
	@Test void reversedClockCannotMakeAReservationYounger() {
		var receipt = new EarnedCounterReservation(); var clash = new Object();
		receipt.hold(clash, 100);
		assertEquals(STALE, receipt.take(clash, 99));
		assertEquals(UNAVAILABLE, receipt.take(clash, 100));
	}
}
