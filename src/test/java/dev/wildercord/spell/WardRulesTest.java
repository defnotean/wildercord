package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The support pack's plain numbers (Worst First to Faithful): what keeps support from becoming free immortality. */
class WardRulesTest {
	private static final List<RuneDef> PACK = List.of(Runes.WORST_FIRST, Runes.SALVE, Runes.MENDING_MIST, Runes.HEARTHGLOW, Runes.AFTERCARE,
		Runes.HEARTHSONG, Runes.GRACE, Runes.MANAGIFT, Runes.MANAWELL, Runes.GUARDLINK, Runes.RALLY, Runes.MORALE, Runes.SHRUG_OFF,
		Runes.HEXGUARD, Runes.STOUTHEART, Runes.IRONHOLD, Runes.EVADE, Runes.EMBERGUARD, Runes.BEASTGUARD, Runes.HEARTHGUARD, Runes.HEEL,
		Runes.BELLWARD, Runes.SANCTUARY, Runes.ARROWVEIL, Runes.BLASTWARD, Runes.FIREBREAK, Runes.PACIFY, Runes.LURE, Runes.STILLBIND,
		Runes.TAUNT, Runes.NUDGE, Runes.HOBBLE, Runes.CORRAL, Runes.TRUCE, Runes.SPOOK, Runes.AEGIS, Runes.ACCORD, Runes.CITADEL,
		Runes.SHIELDWALL, Runes.STAUNCH, Runes.SENTRY, Runes.TEND, Runes.SOOTHE, Runes.WITHDRAW, Runes.KEEPSAFE, Runes.FAITHFUL);

	@Test
	void thePackIsFortySixEffectsOfTiersOneToFour() {
		assertEquals(46, PACK.size());
		Set<String> ids = new HashSet<>();
		for (RuneDef rune : PACK) {
			assertTrue(ids.add(rune.id()), rune.id());
			assertEquals(RuneFamily.EFFECT, rune.family(), rune.id());
			assertTrue(rune.tier() >= 1 && rune.tier() <= 4, rune.id());
			assertTrue(Set.of("support", "control", "world").contains(rune.category()), rune.id() + " " + rune.category());
			assertTrue(Runes.obtainable(rune), rune.id());
			assertSame(rune, Runes.get(rune.id()).orElseThrow());
		}
		assertEquals(Set.of("grace", "aegis", "accord", "citadel"),
			PACK.stream().filter(r -> r.tier() == 4).map(RuneDef::path).collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void theStrongestWardsCostTheMost() {
		for (RuneDef rune : PACK) {
			if (rune.tier() == 4) {
				assertTrue(rune.cost() >= 30, rune.id() + " is a Tier IV ward and costs at least 30");
			}
		}
		assertTrue(Runes.AEGIS.cost() > Runes.SHIELDWALL.cost());
		assertTrue(Runes.CITADEL.cost() > Runes.BLASTWARD.cost());
		assertTrue(Runes.ACCORD.cost() > Runes.TRUCE.cost());
	}

	@Test
	void guardlinkSharesFortyPercentButNeverDropsTheGuardianBelowSix() {
		assertEquals(4.0F, WardRules.redirect(10.0F, 20.0F), 1e-6);
		assertEquals(2.0F, WardRules.redirect(10.0F, 8.0F), 1e-6);
		assertEquals(0.0F, WardRules.redirect(10.0F, 6.0F), 1e-6);
		assertEquals(0.0F, WardRules.redirect(10.0F, 3.0F), 1e-6);
		assertEquals(0.0F, WardRules.redirect(0.0F, 20.0F), 1e-6);
		assertEquals(0.0F, WardRules.redirect(-5.0F, 20.0F), 1e-6);
	}

	@Test
	void ironholdCapsABlowAtFourAndLeavesSmallerOnesAlone() {
		assertEquals(4.0F, WardRules.ironhold(30.0F, 1.0), 1e-6);
		assertEquals(3.0F, WardRules.ironhold(3.0F, 1.0), 1e-6);
		assertEquals(4.0F, WardRules.ironhold(30.0F, 0.5), 1e-6, "half power from Kindred never lowers the cap");
		assertEquals(6.0F, WardRules.ironhold(30.0F, 1.5), 1e-6);
	}

	@Test
	void hearthsongGrowsWithThePartyUpToSix() {
		assertEquals(3.0, WardRules.hearthsong(1, 1.0), 1e-9);
		assertEquals(5.0, WardRules.hearthsong(3, 1.0), 1e-9);
		assertEquals(6.0, WardRules.hearthsong(4, 1.0), 1e-9);
		assertEquals(6.0, WardRules.hearthsong(40, 1.0), 1e-9);
		assertEquals(2.0, WardRules.hearthsong(-1, 1.0), 1e-9);
		assertEquals(3.0, WardRules.hearthsong(40, 0.5), 1e-9);
	}

	@Test
	void moraleGivesOneToThreeHearts() {
		assertEquals(1, WardRules.moraleHearts(0));
		assertEquals(1, WardRules.moraleHearts(1));
		assertEquals(2, WardRules.moraleHearts(2));
		assertEquals(3, WardRules.moraleHearts(3));
		assertEquals(3, WardRules.moraleHearts(12));
	}

	@Test
	void managiftLosesAQuarterAndNeverOverfillsOrOverspends() {
		assertEquals(20.0, WardRules.managiftGiven(100.0, 100.0, 1.0), 1e-9);
		assertEquals(15.0, WardRules.managiftReceived(20.0), 1e-9);
		assertEquals(8.0, WardRules.managiftGiven(100.0, 6.0, 1.0), 1e-9, "only what the ally lacks, before the loss");
		assertEquals(6.0, WardRules.managiftReceived(WardRules.managiftGiven(100.0, 6.0, 1.0)), 1e-9);
		assertEquals(5.0, WardRules.managiftGiven(5.0, 100.0, 1.0), 1e-9, "never more than the giver has");
		assertEquals(0.0, WardRules.managiftGiven(100.0, 0.0, 1.0), 1e-9);
		assertEquals(0.0, WardRules.managiftGiven(-3.0, 100.0, 1.0), 1e-9);
		assertEquals(10.0, WardRules.managiftGiven(100.0, 100.0, 0.5), 1e-9);
		assertTrue(WardRules.managiftReceived(WardRules.managiftGiven(50.0, 50.0, 1.0)) < WardRules.managiftGiven(50.0, 50.0, 1.0),
			"two players can't pass mana back and forth for profit");
	}

	@Test
	void savesAndAegisRestBeforeTheyWorkAgain() {
		assertEquals(12000, WardRules.SAVE_REST);
		assertEquals(2400, WardRules.AEGIS_REST);
		assertTrue(WardRules.resting(1000, 1000, WardRules.SAVE_REST));
		assertTrue(WardRules.resting(1000, 1000 + WardRules.SAVE_REST - 1, WardRules.SAVE_REST));
		assertFalse(WardRules.resting(1000, 1000 + WardRules.SAVE_REST, WardRules.SAVE_REST));
		assertFalse(WardRules.resting(5000, 1000, WardRules.AEGIS_REST), "a save from a later clock (a reset world) never blocks");
	}

	@Test
	void wardsCutDamageButNeverToNothing() {
		assertTrue(WardRules.VILLAGE_TAKES > 0 && WardRules.VILLAGE_TAKES < 1);
		assertTrue(WardRules.CITADEL_TAKES > 0 && WardRules.CITADEL_TAKES < 1);
		assertTrue(WardRules.AEGIS_TAKES > 0 && WardRules.AEGIS_TAKES < 1);
		assertTrue(WardRules.GUARDLINK_SHARE > 0 && WardRules.GUARDLINK_SHARE < 1);
	}

	@Test
	void aftercareHealsSixTimesAtFullPower() {
		assertEquals(6, WardRules.aftercareHeals(1.0));
		assertEquals(3, WardRules.aftercareHeals(0.5));
		assertEquals(1, WardRules.aftercareHeals(0.0));
		assertEquals(9, WardRules.aftercareHeals(1.5));
	}
}
