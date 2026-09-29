package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The limits on the runes of the world: Soulfire's mana back, Manaburn's drain, lingering pulses, Attunement's rest. */
class ExplorerNumbersTest {
	@Test
	void soulfireGivesBackOnlyForDamageDealt() {
		assertEquals(0, ExplorerNumbers.soulfireRefund(0, 0), 1e-9, "a blocked or shrugged-off burn gives nothing");
		assertEquals(0, ExplorerNumbers.soulfireRefund(-2, 0), 1e-9);
		assertEquals(3 * ExplorerNumbers.SOULFIRE_REFUND, ExplorerNumbers.soulfireRefund(3, 0), 1e-9);
		// A Barrage's burn deals a fraction of the damage, so gives back a fraction.
		assertTrue(ExplorerNumbers.soulfireRefund(3 * SpellNumbers.shapeStrength(Runes.BARRAGE), 0) < ExplorerNumbers.soulfireRefund(3, 0));
	}

	@Test
	void soulfireNeverGivesBackMoreThanItsCapInOneCast() {
		double total = 0;
		// Eight targets burning five times each, at full strength.
		for (int i = 0; i < 40; i++) {
			total += ExplorerNumbers.soulfireRefund(3, total);
		}
		assertEquals(ExplorerNumbers.SOULFIRE_REFUND_MAX, total, 1e-9);
		assertTrue(total < Runes.SOULFIRE.cost(), "a cast never gives back what it cost");
		assertEquals(0, ExplorerNumbers.soulfireRefund(100, ExplorerNumbers.SOULFIRE_REFUND_MAX), 1e-9);
	}

	@Test
	void manaburnScalesWithPowerAndPvpAndIsCappedPerTarget() {
		assertEquals(20, ExplorerNumbers.manaburnDrain(1.0, 1.0, 0), 1e-9);
		assertEquals(12, ExplorerNumbers.manaburnDrain(1.0, 0.6, 0), 1e-9, "PvP's scale");
		assertEquals(7, ExplorerNumbers.manaburnDrain(SpellNumbers.shapeStrength(Runes.BARRAGE), 1.0, 0), 1e-9, "a Barrage hit takes little");
		assertEquals(20, ExplorerNumbers.manaburnDrain(3.0, 1.0, 0), 1e-9, "never more than the cap, however amplified");
		double taken = 0;
		for (int i = 0; i < 8; i++) {
			taken += ExplorerNumbers.manaburnDrain(SpellNumbers.shapeStrength(Runes.BARRAGE), 1.0, taken);
		}
		assertEquals(ExplorerNumbers.MANABURN_DRAIN_MAX, taken, 1e-9, "a whole Barrage takes at most the cap from one target");
	}

	@Test
	void lingeringEffectsPulseOncePerSecondOfTheirLength() {
		assertEquals(List.of(0, 20, 40, 60, 80), ExplorerNumbers.pulses(100, 20), "Soulfire: 5 burns in 5 seconds, not 6");
		assertEquals(12, ExplorerNumbers.pulses(240, 20).size(), "Cinderheart: 12 in 12 seconds");
		assertEquals(4, ExplorerNumbers.pulses(80, 5).stream().filter(t -> t % 20 == 0).count(), "Sandstorm: 4 in 4 seconds");
		assertEquals(8, ExplorerNumbers.pulses(80, 10).size(), "Infest: every half second for 4 seconds");
		assertEquals(List.of(0), ExplorerNumbers.pulses(0, 20), "at least one");
		assertEquals(List.of(0, 1, 2), ExplorerNumbers.pulses(3, 0), "a step of 0 is taken as 1");
	}

	@Test
	void manatideGivesThirtyManaADrinkAtMostHoweverExtended() {
		assertEquals(200, ExplorerNumbers.manatideTicks(200));
		// Four Extends made it 160 seconds: it still flows 10.
		assertEquals(ExplorerNumbers.MANATIDE_MOST_TICKS, ExplorerNumbers.manatideTicks(3200));
		assertEquals(10, ExplorerNumbers.manatideTicks(10));
		int most = ExplorerNumbers.pulses(ExplorerNumbers.manatideTicks(3200), 20).size() * ExplorerNumbers.MANATIDE_PER_SECOND;
		assertEquals(30, most);
		// A drink is over long before the next is allowed, so two never flow at once.
		assertTrue(ExplorerNumbers.MANATIDE_MOST_TICKS < ExplorerNumbers.MANATIDE_WAIT);
	}

	@Test
	void aLandRestsForADayAfterGivingItsRune() {
		assertEquals(0, ExplorerNumbers.attuneRestLeft(null, 5000));
		assertEquals(ExplorerNumbers.ATTUNE_REST, ExplorerNumbers.attuneRestLeft(1000L, 1000));
		assertEquals(ExplorerNumbers.ATTUNE_REST - 400, ExplorerNumbers.attuneRestLeft(1000L, 1400));
		assertEquals(0, ExplorerNumbers.attuneRestLeft(1000L, 1000 + ExplorerNumbers.ATTUNE_REST));
		assertEquals(0, ExplorerNumbers.attuneRestLeft(1000L, 500), "a clock that went back doesn't lock the land forever");
	}

	@Test
	void tidehookReelsHarderFromFurtherOutAndStopsAtYourFeet() {
		assertEquals(0, ExplorerNumbers.tidehookTug(0), 1e-9);
		assertEquals(0, ExplorerNumbers.tidehookTug(ExplorerNumbers.TIDEHOOK_REST), 1e-9, "close enough: no more pulling");
		assertTrue(ExplorerNumbers.tidehookTug(3) > 0);
		assertTrue(ExplorerNumbers.tidehookTug(10) > ExplorerNumbers.tidehookTug(5), "harder from further out");
		assertEquals(ExplorerNumbers.TIDEHOOK_TUG_MAX, ExplorerNumbers.tidehookTug(200), 1e-9, "never past the hardest tug, however far");
		assertEquals(0, ExplorerNumbers.tidehookTug(Double.NaN), 1e-9);
		// It reels in over about a second: every tug lands before the next spell could.
		assertTrue(ExplorerNumbers.TIDEHOOK_TUGS * ExplorerNumbers.TIDEHOOK_TUG_EVERY <= 30);
	}

	@Test
	void currentCarriesFurtherWithPowerButWithinLimits() {
		assertEquals(ExplorerNumbers.CURRENT_SPEED, ExplorerNumbers.currentSpeed(1.0), 1e-9);
		assertEquals(9.0, ExplorerNumbers.CURRENT_TICKS * ExplorerNumbers.currentSpeed(1.0), 1e-9, "9 blocks while it holds, at normal power");
		double amplified = ExplorerNumbers.currentSpeed(1.5);
		assertTrue(amplified > ExplorerNumbers.CURRENT_SPEED && amplified < 1.5 * ExplorerNumbers.CURRENT_SPEED, "Amplify carries further, not half as far again");
		assertEquals(1.6 * ExplorerNumbers.CURRENT_SPEED, ExplorerNumbers.currentSpeed(100), 1e-9, "never a launch into the sky");
		assertEquals(0.6 * ExplorerNumbers.CURRENT_SPEED, ExplorerNumbers.currentSpeed(0.1), 1e-9, "Frugal still carries you somewhere");
		assertEquals(0.6 * ExplorerNumbers.CURRENT_SPEED, ExplorerNumbers.currentSpeed(-1), 1e-9);
		assertTrue(ExplorerNumbers.CURRENT_GUARD_TICKS > ExplorerNumbers.CURRENT_TICKS, "the landing is watched for after the surge");
		// Settled in water means staying there long enough that a surge up out of the sea would have left it.
		assertTrue(ExplorerNumbers.CURRENT_SETTLE_TICKS * ExplorerNumbers.CURRENT_SPEED >= 9, "a surge leaves the water within the settling time");
		assertTrue(ExplorerNumbers.CURRENT_SETTLE_TICKS < ExplorerNumbers.CURRENT_GUARD_TICKS);
	}

	@Test
	void manatideReturnsAQuarterOfEachSpellUpToThirty() {
		assertEquals(8.0, ExplorerNumbers.manatideRefund(32, 0), 1e-9);
		assertEquals(0.0, ExplorerNumbers.manatideRefund(0, 0), 1e-9);
		assertEquals(2.0, ExplorerNumbers.manatideRefund(32, 28), 1e-9, "only what is left of the 30");
		assertEquals(0.0, ExplorerNumbers.manatideRefund(32, 30), 1e-9);
	}

	@Test
	void moonpetalWaxesAndWanes() {
		assertEquals(1.4, ExplorerNumbers.moonFactor(0, false), 1e-9, "full moon");
		assertEquals(1.0, ExplorerNumbers.moonFactor(2, false), 1e-9, "quarter");
		assertEquals(0.7, ExplorerNumbers.moonFactor(4, false), 1e-9, "new moon");
		assertEquals(1.4 * 1.25, ExplorerNumbers.moonFactor(0, true), 1e-9, "and stronger at night under open sky");
		assertEquals(ExplorerNumbers.moonFactor(1, false), ExplorerNumbers.moonFactor(9, false), 1e-9);
	}
}
