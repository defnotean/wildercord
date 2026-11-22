package dev.wildercord.cast;

import dev.wildercord.aura.world.BossTier;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.aura.world.OathboundBannerMarshalRules;
import dev.wildercord.aura.world.OathboundBastionRules;
import dev.wildercord.aura.world.OathboundBladeChampionRules;
import dev.wildercord.aura.world.OathboundHalberdierRules;
import dev.wildercord.monster.EliteTier;
import dev.wildercord.monster.GiantRules;
import dev.wildercord.wildlife.ShrineCatRules;
import dev.wildercord.world.cartographer.CartographerRules;
import dev.wildercord.world.ferryman.FerrymanRules;
import dev.wildercord.world.hollowbell.HollowBellRules;
import dev.wildercord.world.kilnwidow.KilnWidowRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossRespawnAndFireRulesTest {
	@Test
	void arenaRespawnWindowBlocksEarlyRetryAndAllowsRematchAfterDelay() {
		assertEquals(12000, BossRules.BOSS_RESPAWN_TICKS);

		long slainAt = 2400L;
		assertFalse(BossRules.canRespawn(slainAt, slainAt));
		assertEquals(12000L, BossRules.respawnTicksRemaining(slainAt, slainAt));
		assertEquals("10m 0s", BossRules.formatRespawnRemaining(12000L));

		assertFalse(BossRules.canRespawn(slainAt, slainAt + BossRules.BOSS_RESPAWN_TICKS - 1L));
		assertEquals(1L, BossRules.respawnTicksRemaining(slainAt, slainAt + BossRules.BOSS_RESPAWN_TICKS - 1L));
		assertEquals("1s", BossRules.formatRespawnRemaining(1L));

		assertTrue(BossRules.canRespawn(slainAt, slainAt + BossRules.BOSS_RESPAWN_TICKS));
		assertEquals(0L, BossRules.respawnTicksRemaining(slainAt, slainAt + BossRules.BOSS_RESPAWN_TICKS));

		// Legacy slain records with timestamp <= 0 can respawn immediately.
		assertTrue(BossRules.canRespawn(0L, 100L));
	}

	@Test
	void customMobFireRulesDetectNamespaceAndComputeCompactPerimeter() {
		assertTrue(CustomMobFireRules.isCustomMobNamespace("wildercord"));
		assertFalse(CustomMobFireRules.isCustomMobNamespace("minecraft"));
		assertFalse(CustomMobFireRules.isCustomMobNamespace(null));

		assertTrue(CustomMobFireRules.emberCount(0.5, 0.5) >= 2);
		assertEquals(5, CustomMobFireRules.emberCount(3.0, 5.0));
		assertTrue(CustomMobFireRules.perimeterRadius(0.4) >= 0.42);
		assertTrue(CustomMobFireRules.perimeterRadius(2.4) > 1.5);
	}

	@Test
	void raisedBossHealthPoolsPreserveProgressionOrdering() {
		assertEquals(750.0, BossTier.APPRENTICE.health);
		assertEquals(1950.0, BossTier.DREAD.health);
		assertEquals(2200.0, MastersRules.HEALTH);
		assertTrue(EliteTier.WARLORD.health < BossTier.APPRENTICE.health);
		assertTrue(BossTier.DREAD.health < MastersRules.HEALTH);

		assertEquals(720.0F, FerrymanRules.HEALTH);
		assertEquals(720.0F, CartographerRules.HEALTH);
		assertEquals(720.0F, HollowBellRules.HEALTH);
		assertEquals(720.0F, KilnWidowRules.HEALTH);

		assertTrue(OathboundHalberdierRules.MAX_HEALTH >= 500.0);
		assertTrue(OathboundBladeChampionRules.MAX_HEALTH >= 500.0);
		assertTrue(OathboundBannerMarshalRules.MAX_HEALTH >= 500.0);
		assertTrue(OathboundBastionRules.MAX_HEALTH >= 600.0);

		assertTrue(GiantRules.TOUGHNESS >= 10.0);
		assertTrue(ShrineCatRules.Variant.YUKI.trialHealth >= 400.0F);
		assertTrue(ShrineCatRules.Variant.KURO.trialHealth > ShrineCatRules.Variant.YUKI.trialHealth);

		assertTrue(BossRules.MAX_HEALTH_CAP >= MastersRules.health(4));
		assertTrue(BossRules.isMaxHealthAttribute("attribute.name.max_health"));
		assertTrue(BossRules.isMaxHealthAttribute("attribute.name.generic.max_health"));
		assertFalse(BossRules.isMaxHealthAttribute("attribute.name.armor"));
		assertFalse(BossRules.isMaxHealthAttribute(null));
	}
}
