package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraRules;
import dev.wildercord.config.WildercordConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The world of aura's numbers: duelists, knights, the forged gear, the clash, and a slash against a player. */
class AuraWorldRulesTest {
	@Test
	void aDuelistMeetsItsChallengerAtTheirOwnStage() {
		assertEquals(AuraRules.GLOW, AuraWorldRules.duelStage(AuraRules.NONE, AuraRules.EDGE), "no method yet: a Glow duelist");
		assertEquals(AuraRules.GLOW, AuraWorldRules.duelStage(AuraRules.GLOW, AuraRules.EDGE));
		assertEquals(AuraRules.FLOW, AuraWorldRules.duelStage(AuraRules.FLOW, AuraRules.EDGE));
		assertEquals(AuraRules.EDGE, AuraWorldRules.duelStage(AuraRules.EDGE, AuraRules.EDGE));
		assertEquals(AuraRules.EDGE, AuraWorldRules.duelStage(AuraRules.SOVEREIGN, AuraRules.EDGE), "never above the stages open on the server");
		assertEquals(AuraRules.FORM, AuraWorldRules.duelStage(AuraRules.FORM, AuraRules.SOVEREIGN), "once Form opens, a Form duelist");
		assertEquals(AuraRules.GLOW, AuraWorldRules.duelStage(AuraRules.EDGE, 0), "a broken registry still gives a duel");
	}

	@Test
	void aDuelistGrowsWithTheStageItFightsAt() {
		for (int s = AuraRules.FLOW; s <= AuraRules.MAX_STAGE; s++) {
			assertTrue(AuraWorldRules.duelistHealth(s) > AuraWorldRules.duelistHealth(s - 1));
			assertTrue(AuraWorldRules.duelistAttackTicks(s) < AuraWorldRules.duelistAttackTicks(s - 1));
			assertTrue(AuraWorldRules.duelXp(s) > AuraWorldRules.duelXp(s - 1));
			assertTrue(AuraWorldRules.duelistGuardChance(s) >= AuraWorldRules.duelistGuardChance(s - 1));
		}
		assertEquals(30, AuraWorldRules.duelistHealth(AuraRules.GLOW), 1e-9);
		assertEquals(50, AuraWorldRules.duelistHealth(AuraRules.EDGE), 1e-9);
		assertEquals(0, AuraWorldRules.duelistGuardChance(AuraRules.GLOW), 1e-9, "a Glow duelist has no guard to raise");
		// Every tell is long enough to answer: at least as long as a player's perfect guard, and the slash's aim fixes before it flies.
		for (int s = AuraRules.EDGE; s <= AuraRules.MAX_STAGE; s++) {
			assertTrue(AuraWorldRules.duelistSlashWindup(s) >= 12);
			assertTrue(AuraWorldRules.duelistSlashWindup(s) > AuraWorldRules.SLASH_LOCK);
			assertTrue(AuraWorldRules.duelistSlashFactor(s) < AuraRules.SLASH_FACTOR, "a duelist's slash is gentler than a player's");
			assertTrue(AuraWorldRules.duelistSlashCooldown(s) >= 3 * AuraRules.SLASH_COOLDOWN / 2);
		}
		assertTrue(AuraWorldRules.DUELIST_SLASH_SPEED < AuraRules.SLASH_SPEED, "it can be stepped aside from");
		// A lesson is a little: never as much as the road to the next stage.
		assertTrue(AuraWorldRules.duelXp(AuraRules.GLOW) < AuraRules.threshold(AuraRules.FLOW) / 3.0);
		assertTrue(AuraWorldRules.duelXp(AuraRules.EDGE) < (AuraRules.threshold(AuraRules.FORM) - AuraRules.threshold(AuraRules.EDGE)) / 10.0);
	}

	@Test
	void aDuelistYieldsRatherThanFalls() {
		assertEquals(7.5, AuraWorldRules.yieldHealth(50), 1e-9);
		assertEquals(1.0, AuraWorldRules.yieldHealth(2), 1e-9, "never below one health");
		assertFalse(AuraWorldRules.yields(50, 10, 50));
		assertTrue(AuraWorldRules.yields(12, 5, 50), "a blow that would leave 7 of 50 makes it yield");
		assertTrue(AuraWorldRules.yields(7.5, 0.1, 50));
		assertTrue(AuraWorldRules.yields(30, 1000, 50), "a blow far past its health only makes it yield");
	}

	@Test
	void mobGuardsHaveAPerfectMomentAndHalveTheRest() {
		assertTrue(AuraWorldRules.perfect(100, 100));
		assertTrue(AuraWorldRules.perfect(100, 100 + AuraWorldRules.MOB_PERFECT_TICKS));
		assertFalse(AuraWorldRules.perfect(100, 101 + AuraWorldRules.MOB_PERFECT_TICKS));
		assertFalse(AuraWorldRules.perfect(-1, 5), "no guard, no moment");
		assertFalse(AuraWorldRules.perfect(100, 99));
		assertEquals(4.0, AuraWorldRules.throughGuard(8), 1e-9);
		assertEquals(0.0, AuraWorldRules.throughGuard(-3), 1e-9);
		assertTrue(AuraWorldRules.MOB_GUARD_TICKS > AuraWorldRules.MOB_PERFECT_TICKS * 3, "most of a guard is held, not perfect");
	}

	@Test
	void knightsGrowWithTheDepthOfWhereTheyHaunt() {
		assertEquals(1, AuraWorldRules.knightRank(false, false));
		assertEquals(2, AuraWorldRules.knightRank(false, true));
		assertEquals(3, AuraWorldRules.knightRank(true, true));
		for (int rank = 2; rank <= 3; rank++) {
			assertTrue(AuraWorldRules.knightHealth(rank) > AuraWorldRules.knightHealth(rank - 1));
			assertTrue(AuraWorldRules.knightArmour(rank) > AuraWorldRules.knightArmour(rank - 1));
			assertTrue(AuraWorldRules.knightSlash(rank) > AuraWorldRules.knightSlash(rank - 1));
			assertTrue(AuraWorldRules.knightSlashWindup(rank) < AuraWorldRules.knightSlashWindup(rank - 1));
			assertTrue(AuraWorldRules.knightSlashCooldown(rank) < AuraWorldRules.knightSlashCooldown(rank - 1));
		}
		assertEquals(40, AuraWorldRules.knightHealth(1), 1e-9);
		assertEquals(60, AuraWorldRules.knightHealth(9), 1e-9, "ranks run 1 to 3");
		// Its tell is always longer than a duelist's: a knight is a dungeon's danger, read from across a hall.
		assertTrue(AuraWorldRules.knightSlashWindup(3) >= AuraWorldRules.duelistSlashWindup(AuraRules.EDGE) - 2);
		assertTrue(AuraWorldRules.KNIGHT_SLASH_SPEED < AuraWorldRules.DUELIST_SLASH_SPEED);
		assertTrue(AuraWorldRules.KNIGHT_RECOVER >= 20, "a slash leaves it open for a second");
		assertEquals(4, AuraWorldRules.PAGES_PER_MANUAL);
	}

	@Test
	void spawnChancesScaleWithTheirRateAndStayChances() {
		assertEquals(0.03, AuraWorldRules.chance(AuraWorldRules.DUELIST_CHANCE, 1.0), 1e-9);
		assertEquals(0.0, AuraWorldRules.chance(AuraWorldRules.DUELIST_CHANCE, 0), 1e-9);
		assertEquals(0.0, AuraWorldRules.chance(AuraWorldRules.DUELIST_CHANCE, -2), 1e-9);
		assertEquals(1.0, AuraWorldRules.chance(0.5, 4), 1e-9);
		// Rare: about one duelist an hour of daylight for a player out in the world, at the default rate (they only come by day).
		double perHour = 72000.0 / AuraWorldRules.DUELIST_PERIOD * AuraWorldRules.DUELIST_CHANCE;
		assertTrue(perHour > 0.5 && perHour < 6, perHour + " duelists an hour");
	}

	@Test
	void theForgedGearAndTheSashDoWhatTheySay() {
		assertNotNull(AuraWorldRules.Forged.byId("lumenedge"));
		assertSame(AuraWorldRules.Forged.BULWARK_MAUL, AuraWorldRules.Forged.byId("bulwark_maul"));
		assertNull(AuraWorldRules.Forged.byId("excalibur"));
		assertTrue(AuraWorldRules.LUMENEDGE_GAIN > 1);
		assertTrue(AuraWorldRules.SKYREND_SLASH > 1 && AuraWorldRules.SKYREND_REACH > 1);
		assertTrue(AuraWorldRules.BULWARK_GUARD_COST < 1);
		assertEquals(25, AuraWorldRules.sashCapacity(20, 1.25));
		assertEquals(87, AuraWorldRules.sashCapacity(70, 1.25), "rounded down");
		assertEquals(70, AuraWorldRules.sashCapacity(70, 0.5), "never less than without it");
		assertEquals(0, AuraWorldRules.sashCapacity(0, 1.25), "no method, no aura");
		assertTrue(AuraWorldRules.SASH_SETTLE < AuraRules.SETTLE_TICKS);
		// The netherite spear's own damage is a sword's less three; the glaive's slash makes up the difference and more.
		double sword = 8 * AuraRules.SLASH_FACTOR;
		double glaive = 5 * AuraRules.SLASH_FACTOR * AuraWorldRules.SKYREND_SLASH;
		assertTrue(glaive >= sword - 1e-9, glaive + " against " + sword);
	}

	@Test
	void twoCrescentsMeetHeadOnButNotSideBySide() {
		double reach = AuraWorldRules.clashReach(3.0, 2.6);
		// Head on, three blocks apart, closing at 2.9 a tick: they pass within the tick.
		assertTrue(AuraWorldRules.meets(new double[] {0, 0, 0}, new double[] {1.6, 0, 0}, new double[] {3, 0, 0}, new double[] {1.7, 0, 0}, reach));
		// Head on, crossing between ticks without ever being at the same spot at a tick's end.
		assertTrue(AuraWorldRules.meets(new double[] {0, 0, 0}, new double[] {1.6, 0, 0}, new double[] {1.5, 0.2, 0}, new double[] {0.2, 0.2, 0}, reach));
		// Side by side, flying the same way four blocks apart: never.
		assertFalse(AuraWorldRules.meets(new double[] {0, 0, 0}, new double[] {1.6, 0, 0}, new double[] {0, 0, 4}, new double[] {1.6, 0, 4}, reach));
		// Head on but a whole crescent's width off to the side.
		assertFalse(AuraWorldRules.meets(new double[] {0, 0, 0}, new double[] {1.6, 0, 0}, new double[] {3, 0, 4}, new double[] {1.7, 0, 4}, reach));
		// Still far apart at the end of the tick.
		assertFalse(AuraWorldRules.meets(new double[] {0, 0, 0}, new double[] {1.6, 0, 0}, new double[] {10, 0, 0}, new double[] {8.7, 0, 0}, reach));
	}

	@Test
	void aSlashAgainstAPlayerIsHeldToTheCapAndScaled() {
		WildercordConfig d = WildercordConfig.DEFAULTS;
		double cap = d.defence().maxBonus();
		double pvp = d.aura().pvpScale();
		// A netherite sword's slash, no element bonus: 8 x 1.2 x 0.6.
		assertEquals(8 * 1.2 * 0.6, 8 * 1.2 * AuraWorldRules.slashAgainstPlayer(1.0, cap, pvp), 1e-9);
		// A glaive's bonus and a weakness together are held to the cap before the scale.
		assertEquals(cap * pvp, AuraWorldRules.slashAgainstPlayer(AuraWorldRules.SKYREND_SLASH * 2.0, cap, pvp), 1e-9);
		// No slash against a player ever does more than the cap allows a spell.
		assertTrue(AuraWorldRules.slashAgainstPlayer(100, cap, pvp) <= cap);
	}
}
