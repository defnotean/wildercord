package dev.wildercord.aura.world;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossDamageRulesTest {
	/** Vanilla's armour step for full diamond: 20 armour, 8 toughness. */
	private static float diamond(float damage) {
		float armour = Math.clamp(20 - damage / ((8 + 8) / 4f), 20 * .2f, 20f);
		return damage * (1 - armour / 25);
	}
	/** Vanilla's Protection step for four pieces of Protection IV: 16 points, 64% off. */
	private static float protection(float damage) {
		return damage * (1 - 16 / 25f);
	}
	/** A blow of {@code damage} through diamond and Protection IV, each defence pierced by {@code share}. */
	private static float landed(float damage, double share) {
		float armoured = GuardianBite.pierced(damage, diamond(damage), share);
		return GuardianBite.pierced(armoured, protection(armoured), share);
	}

	@Test void aBossHitOnFullDiamondProtectionLandsSeveralTimesHarder() {
		float before = landed(10, 0), after = landed(BossDamageRules.scaled(10), BossDamageRules.share(true, 0));
		assertTrue(before < 1.2, "a 10-damage boss used to land " + before);
		assertTrue(after >= 4.5 && after <= 6, "a 10-damage boss now lands " + after);
	}

	@Test void aLairBossKeepsItsOwnPierceWhenThatIsHigher() {
		assertEquals(SerpentRules.SLAM_PIERCE, BossDamageRules.share(true, SerpentRules.SLAM_PIERCE), 1e-9);
		assertEquals(BossDamageRules.PIERCE, BossDamageRules.share(true, HollowMatronRules.PIERCE), 1e-9);
		assertTrue(landed(BossDamageRules.scaled(15), BossDamageRules.share(true, HollowMatronRules.PIERCE)) > landed(15, HollowMatronRules.PIERCE) * 1.4);
	}

	@Test void onlyABossesBlowIsChanged() {
		assertEquals(.2, BossDamageRules.share(false, .2), 1e-9);
		assertEquals(0, BossDamageRules.share(false, 0), 1e-9);
		assertEquals(13.5f, BossDamageRules.scaled(10), 1e-4);
	}
}
