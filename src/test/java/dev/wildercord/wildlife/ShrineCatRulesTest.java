package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Pure rules for the four Shrine Cats (Yuki, Odie, Freya, Kuro) and anti-duplication identity invariants. */
class ShrineCatRulesTest {
	@Test
	void identityRulesPreventCloningSittingUnloadedAndCrossDimensionCats() {
		UUID canonical = UUID.randomUUID();
		UUID clone = UUID.randomUUID();
		UUID transferred = UUID.randomUUID();

		// Cannot tame a duplicate while canonical cat is alive (active).
		assertTrue(ShrineCatIdentityRules.canTameVariant(null, false));
		assertFalse(ShrineCatIdentityRules.canTameVariant(canonical, false));
		assertTrue(ShrineCatIdentityRules.canTameVariant(canonical, true));

		// Pre-journal adoption & canonical keep vs duplicate discard.
		assertEquals(ShrineCatIdentityRules.LoadResolution.REGISTER_NEW,
			ShrineCatIdentityRules.resolveLoadedBody(canonical, null, false, false));
		assertEquals(ShrineCatIdentityRules.LoadResolution.KEEP_CANONICAL,
			ShrineCatIdentityRules.resolveLoadedBody(canonical, canonical, false, false));
		assertEquals(ShrineCatIdentityRules.LoadResolution.DISCARD_DUPLICATE,
			ShrineCatIdentityRules.resolveLoadedBody(clone, canonical, false, false));

		// Cross-dimension transfer adopts transferred entity cleanly.
		assertEquals(ShrineCatIdentityRules.LoadResolution.ADOPT_TRANSFER,
			ShrineCatIdentityRules.resolveLoadedBody(transferred, canonical, false, true));

		// Only the active canonical body may retire on death/discard.
		assertTrue(ShrineCatIdentityRules.mayRetire(canonical, canonical, false));
		assertFalse(ShrineCatIdentityRules.mayRetire(clone, canonical, false));
		assertFalse(ShrineCatIdentityRules.mayRetire(canonical, canonical, true));

		// Cat-Bell Charm may NEVER respawn an active (sitting/unloaded/distant) cat — only a retired (dead) one.
		assertFalse(ShrineCatIdentityRules.shouldRespawnOnBell(null, false));
		assertFalse(ShrineCatIdentityRules.shouldRespawnOnBell(canonical, false));
		assertTrue(ShrineCatIdentityRules.shouldRespawnOnBell(canonical, true));
	}

	@Test
	void fourCatsGuardingFloorsInOrderHaveDistinctTraits() {
		assertEquals(4, ShrineCatRules.Variant.values().length);
		assertEquals(ShrineCatRules.Variant.YUKI, ShrineCatRules.Variant.byId(0));
		assertEquals(ShrineCatRules.Variant.ODIE, ShrineCatRules.Variant.byId(1));
		assertEquals(ShrineCatRules.Variant.FREYA, ShrineCatRules.Variant.byId(2));
		assertEquals(ShrineCatRules.Variant.KURO, ShrineCatRules.Variant.byId(3));

		assertEquals(1, ShrineCatRules.Variant.YUKI.sanctuaryFloor);
		assertEquals(2, ShrineCatRules.Variant.ODIE.sanctuaryFloor);
		assertEquals(3, ShrineCatRules.Variant.FREYA.sanctuaryFloor);
		assertEquals(4, ShrineCatRules.Variant.KURO.sanctuaryFloor);

		assertTrue(ShrineCatRules.Variant.YUKI.fluffyCoat);
		assertTrue(ShrineCatRules.Variant.ODIE.fluffyCoat);
		assertTrue(ShrineCatRules.Variant.FREYA.bellCollar);
		assertFalse(ShrineCatRules.Variant.FREYA.fluffyCoat);
		assertTrue(ShrineCatRules.Variant.KURO.notchedLeftEar);
	}

	@Test
	void guardianTrialsYieldGracefullyAndUnlockPerksAndTaming() {
		assertFalse(ShrineCatRules.shouldYieldInTrial(30.0F, 50.0F));
		assertTrue(ShrineCatRules.shouldYieldInTrial(10.0F, 50.0F));
		assertTrue(ShrineCatRules.shouldYieldInTrial(0.0F, 50.0F));

		assertFalse(ShrineCatRules.grantsDungeonPerk(ShrineCatRules.ROLE_GUARDIAN));
		assertTrue(ShrineCatRules.grantsDungeonPerk(ShrineCatRules.ROLE_CLEARED));
		assertTrue(ShrineCatRules.grantsDungeonPerk(ShrineCatRules.ROLE_TAMED));

		assertFalse(ShrineCatRules.canTame(ShrineCatRules.ROLE_GUARDIAN, true, true));
		assertFalse(ShrineCatRules.canTame(ShrineCatRules.ROLE_CLEARED, false, true));
		assertTrue(ShrineCatRules.canTame(ShrineCatRules.ROLE_CLEARED, true, false));
		assertTrue(ShrineCatRules.canTame(ShrineCatRules.ROLE_VILLAGE, false, true));
	}
}
