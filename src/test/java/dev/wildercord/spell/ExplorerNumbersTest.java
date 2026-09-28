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
	void aLandRestsForADayAfterGivingItsRune() {
		assertEquals(0, ExplorerNumbers.attuneRestLeft(null, 5000));
		assertEquals(ExplorerNumbers.ATTUNE_REST, ExplorerNumbers.attuneRestLeft(1000L, 1000));
		assertEquals(ExplorerNumbers.ATTUNE_REST - 400, ExplorerNumbers.attuneRestLeft(1000L, 1400));
		assertEquals(0, ExplorerNumbers.attuneRestLeft(1000L, 1000 + ExplorerNumbers.ATTUNE_REST));
		assertEquals(0, ExplorerNumbers.attuneRestLeft(1000L, 500), "a clock that went back doesn't lock the land forever");
	}
}
