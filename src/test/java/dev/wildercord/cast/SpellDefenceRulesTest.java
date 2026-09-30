package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellDefenceRulesTest {
	private static final double RATE = SpellDefenceRules.ARMOUR_RATE;

	@Test
	void fullNetheriteTurnsAsideAboutAThirdToTwoFifthsOfASpell() {
		// Netherite: 20 armour, 12 toughness. An everyday spell to a big one.
		for (double damage : new double[] {6, 10, 15, 20}) {
			double share = SpellDefenceRules.armourShare(damage, 20, 12, RATE);
			assertTrue(share >= 0.35 && share <= 0.42, damage + " damage: " + share);
		}
		// A huge hit still loses nearly a third.
		assertTrue(SpellDefenceRules.armourShare(30, 20, 12, RATE) > 0.3);
	}

	@Test
	void armourCountsForAboutHalfWhatItDoesAgainstABlade() {
		// Vanilla against a blade of 10 on full diamond (20 armour, 8 toughness): 17.5 points of 25, 70%.
		double blade = 17.5 / 25.0;
		assertEquals(blade * RATE, SpellDefenceRules.armourShare(10, 20, 8, RATE), 1e-9);
		// Iron keeps less of itself against a big hit, as it does against a blade.
		assertTrue(SpellDefenceRules.armourShare(20, 15, 0, RATE) < SpellDefenceRules.armourShare(5, 15, 0, RATE));
		// A fifth of the armour always counts, and never more than 20 points.
		assertEquals(0.2 * 15 / 25 * RATE, SpellDefenceRules.armourShare(1000, 15, 0, RATE), 1e-9);
		assertEquals(20.0 / 25 * RATE, SpellDefenceRules.armourShare(0, 30, 0, RATE), 1e-9);
	}

	@Test
	void noArmourOrARateOfNothingTurnsNothingAside() {
		assertEquals(0, SpellDefenceRules.armourShare(10, 0, 0, RATE), 1e-9);
		assertEquals(0, SpellDefenceRules.armourShare(10, 20, 12, 0), 1e-9);
		// A rate over 1 counts as all of it, never more.
		assertEquals(SpellDefenceRules.armourShare(10, 20, 12, 1), SpellDefenceRules.armourShare(10, 20, 12, 3), 1e-9);
	}

	@Test
	void wardingTakesEightPercentALevelUpToTheEnchantmentCap() {
		assertEquals(1.0, SpellDefenceRules.wardingFactor(0, 0), 1e-9);
		assertEquals(0.92, SpellDefenceRules.wardingFactor(0, 1), 1e-9);
		// One piece of Warding IV: 32% off.
		assertEquals(0.68, SpellDefenceRules.wardingFactor(0, 4), 1e-9);
		// Two pieces: 64%. A full set is 32 points, held to the cap of 20: 80% off and never immunity.
		assertEquals(0.36, SpellDefenceRules.wardingFactor(0, 8), 1e-9);
		assertEquals(0.2, SpellDefenceRules.wardingFactor(0, 16), 1e-9);
		assertEquals(0.8, SpellDefenceRules.enchantmentShare(0, 16), 1e-9);
	}

	@Test
	void wardingSharesTheCapWithProtection() {
		// Full Protection IV already takes 16 points (64%) off a spell; Warding can only add the last 4.
		double withProtection = (1 - 16 / 25.0) * SpellDefenceRules.wardingFactor(16, 16);
		assertEquals(0.2, withProtection, 1e-9);
		assertEquals(0.8, SpellDefenceRules.enchantmentShare(16, 16), 1e-9);
		// Three Protection IV and one Warding IV: 12 + 8 points, the cap exactly.
		assertEquals(0.2, (1 - 12 / 25.0) * SpellDefenceRules.wardingFactor(12, 4), 1e-9);
		// Already at the cap, Warding adds nothing (and a factor never strengthens a hit).
		assertEquals(1.0, SpellDefenceRules.wardingFactor(20, 16), 1e-9);
		assertEquals(1.0, SpellDefenceRules.wardingFactor(40, 4), 1e-9);
	}

	@Test
	void wardedTakesAFifthALevelAndNeverAll() {
		assertEquals(0, SpellDefenceRules.wardedShare(0), 1e-9);
		assertEquals(0.2, SpellDefenceRules.wardedShare(1), 1e-9);
		assertEquals(0.4, SpellDefenceRules.wardedShare(2), 1e-9);
		// However high a command sets it.
		assertEquals(0.8, SpellDefenceRules.wardedShare(5), 1e-9);
		assertEquals(0.8, SpellDefenceRules.wardedShare(255), 1e-9);
	}

	@Test
	void bonusesAreHeldToTheCapButNeverLifted() {
		double cap = SpellDefenceRules.MAX_BONUS;
		// Execute, a reaction, a weakness and a backstab together: over 10 times.
		assertEquals(cap, SpellDefenceRules.capBonus(2.0 * 1.5 * 1.5 * 1.5 * 1.25, cap), 1e-9);
		assertEquals(1.8, SpellDefenceRules.capBonus(1.8, cap), 1e-9);
		// A resistance or water on fire still weakens the hit.
		assertEquals(0.5, SpellDefenceRules.capBonus(0.5, cap), 1e-9);
		// A cap under 1 counts as 1, so it can never make a bonus a penalty or a penalty worse.
		assertEquals(1.0, SpellDefenceRules.capBonus(3, 0.2), 1e-9);
		assertEquals(0.5, SpellDefenceRules.capBonus(0.5, 0.2), 1e-9);
		assertEquals(1.0, SpellDefenceRules.capBonus(Double.NaN, cap), 1e-9);
	}

	@Test
	void theSpellguardHoldsOnlyFromHighHealthAndOnlyWhenCharged() {
		long now = 10_000;
		int recharge = SpellDefenceRules.GUARD_RECHARGE;
		double threshold = SpellDefenceRules.GUARD_HEALTH;
		// Full health, never used: it holds.
		assertTrue(SpellDefenceRules.guardHolds(true, 20, 20, threshold, now, null, recharge));
		// Exactly 80%, it holds; just under, it doesn't.
		assertTrue(SpellDefenceRules.guardHolds(true, 16, 20, threshold, now, null, recharge));
		assertFalse(SpellDefenceRules.guardHolds(true, 15.9F, 20, threshold, now, null, recharge));
		// Switched off, it never holds.
		assertFalse(SpellDefenceRules.guardHolds(false, 20, 20, threshold, now, null, recharge));
		// Held 30 seconds ago: still recharging. A minute on, ready again.
		assertFalse(SpellDefenceRules.guardHolds(true, 20, 20, threshold, now, now - 600, recharge));
		assertTrue(SpellDefenceRules.guardHolds(true, 20, 20, threshold, now, now - 1200, recharge));
		// Someone already on one heart has nothing it could leave them.
		assertFalse(SpellDefenceRules.guardHolds(true, 2, 2, threshold, now, null, recharge));
		// More health (absorption aside, a bigger heart pool) works the same way.
		assertTrue(SpellDefenceRules.guardHolds(true, 32, 40, threshold, now, null, recharge));
	}

	@Test
	void theSpellguardCountsDownInWholeSeconds() {
		assertEquals(0, SpellDefenceRules.guardSeconds(5000, null, 60), "never held: ready");
		assertEquals(60, SpellDefenceRules.guardSeconds(1000, 1000L, 60), "just held: the whole minute");
		assertEquals(30, SpellDefenceRules.guardSeconds(1600, 1000L, 60));
		assertEquals(1, SpellDefenceRules.guardSeconds(1000 + 1199, 1000L, 60), "never 0 while it lasts");
		assertEquals(0, SpellDefenceRules.guardSeconds(1000 + 1200, 1000L, 60), "back at the minute");
		// A server that shortens the recharge shortens a running one; one that turned its clock back doesn't hold it for ever.
		assertEquals(0, SpellDefenceRules.guardSeconds(1600, 1000L, 20));
		assertEquals(0, SpellDefenceRules.guardSeconds(1000, 9000L, 60));
		// No recharge at all: always ready.
		assertEquals(0, SpellDefenceRules.guardSeconds(1000, 1000L, 0));
	}

	@Test
	void layersMultiplySoNoStackIsImmunity() {
		assertEquals(0, SpellDefenceRules.combined(), 1e-9);
		assertEquals(0.5, SpellDefenceRules.combined(0.5), 1e-9);
		// Netherite against a 10 hit, full Warding, Warded II: 1 - 0.604 x 0.2 x 0.6.
		double armour = SpellDefenceRules.armourShare(10, 20, 12, RATE);
		double all = SpellDefenceRules.combined(armour, SpellDefenceRules.enchantmentShare(0, 16), SpellDefenceRules.wardedShare(2));
		assertEquals(1 - (1 - armour) * 0.2 * 0.6, all, 1e-9);
		assertTrue(all < 0.95, "strong, not immune: " + all);
	}

	@Test
	void aOneShotFromFullHealthIsGoneOnceTheLayersAreOn() {
		// A 26-damage spell (Sunfall's, at power 1) against full netherite, then Warding IV on every piece, then Warded:
		// what's left after each layer.
		double hit = 26;
		double netherite = hit * (1 - SpellDefenceRules.armourShare(hit, 20, 12, RATE));
		double warded = netherite * SpellDefenceRules.wardingFactor(0, 16);
		double potion = warded * (1 - SpellDefenceRules.wardedShare(1));
		assertTrue(netherite < hit && warded < netherite && potion < warded);
		assertTrue(netherite < 20, "full netherite alone should survive it from full health: " + netherite);
		assertTrue(potion < 4, "the full set should leave it at a couple of hearts: " + potion);
	}
}
