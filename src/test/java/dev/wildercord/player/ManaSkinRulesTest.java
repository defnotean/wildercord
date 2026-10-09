package dev.wildercord.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManaSkinRulesTest {
	@Test void onlyThisNonlethalHealthWoundCanBeRepaid() {
		assertEquals(1, ManaSkinRules.recovery(20, 15, 15, 100));
		assertEquals(1, ManaSkinRules.recovery(10, 5, 5, 100), "Old wounds cannot enlarge the current rebate");
		assertEquals(0, ManaSkinRules.recovery(10, 10, 10, 100), "Absorption or prevention caused no red-health loss");
		assertEquals(0, ManaSkinRules.recovery(10, 12, 12, 100));
		assertEquals(0, ManaSkinRules.recovery(20, 0, 0, 100), "Death prevention owns lethal recovery");
		assertEquals(0, ManaSkinRules.recovery(20, -1, -1, 100));
	}

	@Test void availableManaAndExistingMinimumStillBoundRecovery() {
		assertEquals(.5F, ManaSkinRules.recovery(20, 15, 15, 1));
		assertEquals(.25F, ManaSkinRules.recovery(20, 15, 15, .5F));
		assertEquals(0, ManaSkinRules.recovery(20, 15, 15, Math.nextDown(.5F)));
		assertEquals(.25F, ManaSkinRules.recovery(20, 18.75F, 18.75F, 100));
		assertEquals(0, ManaSkinRules.recovery(20, 19, 19, 100));
		assertEquals(0, ManaSkinRules.recovery(20, 15, 15, 0));
		assertEquals(0, ManaSkinRules.recovery(20, 15, 15, -1));
	}

	@Test void suppressedOrPartialHealingPaysOnlyWhatWasRestored() {
		assertEquals(0, ManaSkinRules.payment(0, 100));
		assertEquals(0, ManaSkinRules.payment(-1, 100));
		assertEquals(.4F, ManaSkinRules.payment(.2F, 100));
		assertEquals(1, ManaSkinRules.payment(.500001F, 1), "Float rounding cannot overdraw mana");
	}

	@Test void earlierCallbacksCannotTurnRecoveryIntoHealingOldWoundsOrADeathSave() {
		assertEquals(0, ManaSkinRules.recovery(10, 5, 10, 100));
		assertEquals(.25F, ManaSkinRules.recovery(10, 5, 9.75F, 100));
		assertEquals(0, ManaSkinRules.recovery(10, 5, 9.9F, 100));
		assertEquals(1, ManaSkinRules.recovery(10, 5, 2, 100), "A later wound cannot increase this hit's share");
		assertEquals(0, ManaSkinRules.recovery(10, 0, 5, 100), "Later death-save recovery cannot make a lethal wound eligible");
		assertEquals(0, ManaSkinRules.recovery(10, 5, 0, 100));
		assertEquals(0, ManaSkinRules.recovery(10, 5, Float.NaN, 100));
	}

	@Test void malformedInputsNeverProduceARebateOrPayment() {
		for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
			assertEquals(0, ManaSkinRules.recovery(invalid, 15, 15, 100));
			assertEquals(0, ManaSkinRules.recovery(20, invalid, invalid, 100));
			assertEquals(0, ManaSkinRules.recovery(20, 15, 15, invalid));
			assertEquals(0, ManaSkinRules.payment(invalid, 100));
			assertEquals(0, ManaSkinRules.payment(1, invalid));
		}
	}

	@Test void endgameArmourCannotTurnManaIntoFullHealing() {
		float nativeWound = 4.273926F;
		float recovery = ManaSkinRules.recovery(200, 200 - nativeWound, 200 - nativeWound, 600);
		assertEquals(nativeWound * .2F, recovery, .00001F);
		assertEquals(nativeWound * .4F, ManaSkinRules.payment(recovery, 600), .00001F);
		assertTrue(nativeWound - recovery > 3.4F);
	}

	@Test void finiteWoundsNeverExceedTheirShareOrAvailableMana() {
		for (float health : new float[] {2, 20, 200, 1024, Float.MAX_VALUE})
			for (float fraction : new float[] {.01F, .1F, .5F, .99F})
				for (float mana : new float[] {.49F, .5F, 1, 100, 600, Float.MAX_VALUE}) {
					float after = health * fraction;
					float recovery = ManaSkinRules.recovery(health, after, after, mana);
					assertTrue(Float.isFinite(recovery) && recovery >= 0);
					assertTrue(recovery <= (health - after) * .200001F);
					assertTrue(recovery <= mana / 2);
				}
	}
}
