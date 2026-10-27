package dev.wildercord.aura.world;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BossTierTest {
	@Test void everyTierIsHarderThanTheOneBelow() {
		BossTier[] tiers = BossTier.values();
		for (int i = 1; i < tiers.length; i++) {
			BossTier lo = tiers[i - 1], hi = tiers[i];
			assertTrue(hi.health > lo.health && hi.armour > lo.armour && hi.melee > lo.melee, hi + " over " + lo);
			assertTrue(hi.hits >= lo.hits && hi.pace >= lo.pace && hi.hit(20) >= lo.hit(20), hi + " over " + lo);
			assertTrue(hi.xp > lo.xp && hi.shards >= lo.shards && hi.crystals >= lo.crystals, hi + " rewards over " + lo);
		}
		assertTrue(BossTier.HEROIC.xp >= 240 && BossTier.HEROIC.shards >= 3 && BossTier.HEROIC.crystals >= 3);
	}

	@Test void noTwoGuardiansShareATier() {
		BossTier[] used = {SerpentRules.TIER, LanternBearerRules.TIER, HollowMatronRules.TIER, AshenStagRules.TIER,
			FrostWyrmlingRules.TIER, MojitoRules.TIER, BellWardenRules.TIER};
		assertTrue(java.util.Arrays.stream(used).distinct().count() == used.length);
	}

	@Test void theSwordMasterStaysAboveThemAll() {
		BossTier top = BossTier.DREAD;
		assertTrue(MastersRules.HEALTH > top.health && top.hit(24) < MastersRules.TECHNIQUE_DAMAGE);
	}
}
