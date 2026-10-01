package dev.wildercord.aura;

import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.Parry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Aura's numbers: stages, what fills it and what spends it, experience, breakthroughs, the methods and where they're found. */
class AuraRulesTest {
	@Test
	void stagesHoldMoreAndNeedMoreAsTheyClimb() {
		assertEquals(0, AuraRules.capacity(AuraRules.NONE));
		assertEquals(20, AuraRules.capacity(AuraRules.GLOW));
		assertEquals(40, AuraRules.capacity(AuraRules.FLOW));
		assertEquals(70, AuraRules.capacity(AuraRules.EDGE));
		assertEquals(110, AuraRules.capacity(AuraRules.FORM));
		assertEquals(160, AuraRules.capacity(AuraRules.SOVEREIGN));
		assertEquals(0, AuraRules.threshold(AuraRules.GLOW), "Glow comes with the method");
		for (int s = AuraRules.FLOW; s <= AuraRules.MAX_STAGE; s++) {
			assertTrue(AuraRules.threshold(s) >= 2.5 * Math.max(1, AuraRules.threshold(s - 1)), "stage " + s + " is a real leap");
			assertTrue(AuraRules.capacity(s) > AuraRules.capacity(s - 1));
		}
		assertEquals("edge", AuraRules.id(AuraRules.EDGE));
		assertEquals("none", AuraRules.id(-3));
		assertEquals("sovereign", AuraRules.id(99));
	}

	@Test
	void experienceFillsToTheThresholdAndWaitsForTheBreakthrough() {
		assertEquals(140, AuraRules.fill(100, 40, 150), 1e-9);
		assertEquals(150, AuraRules.fill(140, 40, 150), 1e-9, "it waits at the threshold");
		assertEquals(150, AuraRules.fill(150, 900, 150), 1e-9);
		assertEquals(10_000, AuraRules.fill(9000, 1000, -1), 1e-9, "past the last stage nothing caps it");
		assertEquals(100, AuraRules.fill(100, -5, 150), 1e-9, "a loss is no gain");
		assertFalse(AuraRules.ready(149, 150));
		assertTrue(AuraRules.ready(150, 150));
		assertFalse(AuraRules.ready(1e9, -1));
		assertEquals(0.5, AuraRules.progress(375, 150, 600), 1e-9);
		assertEquals(0.0, AuraRules.progress(10, 150, 600), 1e-9);
		assertEquals(1.0, AuraRules.progress(10, 600, 600), 1e-9);
	}

	@Test
	void switchingMethodsGoesBackToTheStartOfTheStage() {
		assertEquals(0, AuraRules.afterSwitch(AuraRules.GLOW, 0), 1e-9);
		assertEquals(150, AuraRules.afterSwitch(AuraRules.FLOW, AuraRules.threshold(AuraRules.FLOW)), 1e-9);
		assertEquals(600, AuraRules.afterSwitch(AuraRules.EDGE, AuraRules.threshold(AuraRules.EDGE)), 1e-9);
	}

	@Test
	void onlyMeaningfulBlowsFillAura() {
		assertEquals(2.4, AuraRules.hitGain(6, 1.0, 1.0, false), 1e-9);
		assertEquals(0.0, AuraRules.hitGain(6, 0.5, 1.0, false), 1e-9, "a half swing is spam");
		assertEquals(AuraRules.MAX_HIT_GAIN, AuraRules.hitGain(500, 1.0, 1.0, false), 1e-9, "one blow can only give so much");
		assertEquals(2.4 * AuraRules.PRACTICE_GAIN, AuraRules.hitGain(6, 1.0, 1.0, true), 1e-9, "a dummy gives a quarter");
		assertEquals(1.2, AuraRules.hitGain(6, 1.0, 0.5, false), 1e-9, "repetition takes its share");
		assertEquals(0.0, AuraRules.hitGain(0, 1.0, 1.0, false), 1e-9);
		// Farming one spot: after a hundred blows the same place gives a fraction.
		double farmed = AuraRules.hitGain(6, 1.0, MasteryRules.repetition(100), false);
		assertTrue(farmed < 0.2 * AuraRules.hitGain(6, 1.0, 1.0, false), "farmed: " + farmed);
	}

	@Test
	void theBreathKeepsABeat() {
		long settled = 1000;
		assertFalse(AuraRules.onBeat(settled, settled), "no beat the moment the stance settles");
		assertTrue(AuraRules.onBeat(settled, settled + AuraRules.BREATH_PERIOD));
		assertTrue(AuraRules.onBeat(settled, settled + AuraRules.BREATH_PERIOD - AuraRules.BEAT_WINDOW));
		assertTrue(AuraRules.onBeat(settled, settled + 2 * AuraRules.BREATH_PERIOD + AuraRules.BEAT_WINDOW));
		assertFalse(AuraRules.onBeat(settled, settled + AuraRules.BREATH_PERIOD + AuraRules.BREATH_PERIOD / 2));
		assertEquals(settled + AuraRules.BREATH_PERIOD, AuraRules.nextBeat(settled, settled));
		assertEquals(settled + 2 * AuraRules.BREATH_PERIOD, AuraRules.nextBeat(settled, settled + AuraRules.BREATH_PERIOD));
		assertTrue(AuraRules.BOB_TICKS < AuraRules.BREATH_PERIOD, "a breath is quicker than the beat");
		// A minute in the stance fills Glow several times over; on the beat it's quicker still.
		double minute = AuraRules.BREATH_GAIN * 60;
		assertTrue(minute > 2 * AuraRules.capacity(AuraRules.GLOW));
		assertTrue(minute + 30 * AuraRules.BEAT_GAIN >= 2 * minute, "breathing on every beat doubles it");
	}

	@Test
	void theGuardHalvesWhatAuraPaysForAndItsPerfectMomentIsAParry() {
		assertEquals(5.0, AuraRules.guardAbsorb(10, 100, 0.5), 1e-9);
		assertEquals(3.0 / AuraRules.GUARD_COST_PER_POINT, AuraRules.guardAbsorb(10, 3, 1.0), 1e-9, "only as far as aura pays");
		assertEquals(0.0, AuraRules.guardAbsorb(10, 0, 0.5), 1e-9);
		assertEquals(Parry.WINDOW, AuraRules.PERFECT_TICKS, "the same moment as a parry");
		assertTrue(AuraRules.perfect(100, 100));
		assertTrue(AuraRules.perfect(100, 100 + AuraRules.PERFECT_TICKS));
		assertFalse(AuraRules.perfect(100, 101 + AuraRules.PERFECT_TICKS));
		assertFalse(AuraRules.perfect(-1, 5));
		assertTrue(AuraRules.PERFECT_TICKS < AuraRules.GUARD_TICKS);
		assertTrue(AuraRules.GUARD_REST > AuraRules.PERFECT_TICKS, "tapping can't hold the perfect moment open");
	}

	@Test
	void theEdgePiercesPartOfABlowThroughArmour() {
		assertEquals(10.0, AuraRules.pierced(10, 0.25, 1.0), 1e-9, "no armour, nothing to pierce");
		// Armour leaving half: 7.5 as it was plus 2.5 lifted to 5, so that 2.5 lands whole after armour.
		assertEquals(12.5, AuraRules.pierced(10, 0.25, 0.5), 1e-9);
		double dealt = AuraRules.pierced(10, AuraRules.EDGE_PIERCE, 0.5) * 0.5;
		assertEquals(10 * (1 - AuraRules.EDGE_PIERCE) * 0.5 + 10 * AuraRules.EDGE_PIERCE, dealt, 1e-9);
		assertTrue(AuraRules.pierced(10, 0.25, 0.01) <= 10 * 0.75 + 2.5 / 0.2 + 1e-9, "never lifted past armour's floor");
	}

	@Test
	void theSlashIsTheWeaponTimesItsFactorAndOnlyWhatWasPaid() {
		assertEquals(8.4, AuraRules.slashDamage(7, 1.2, 12, 12), 1e-9);
		assertEquals(4.2, AuraRules.slashDamage(7, 1.2, 12, 6), 1e-9, "spent past empty, it goes out weakened");
		assertEquals(0.0, AuraRules.slashDamage(7, 1.2, 12, 0), 1e-9);
		assertTrue(AuraRules.SLASH_COST < AuraRules.capacity(AuraRules.EDGE), "Edge holds several slashes");
	}

	@Test
	void spendingPastEmptyIsBacklash() {
		AuraRules.Spend paid = AuraRules.spend(20, 12);
		assertEquals(8, paid.left(), 1e-9);
		assertFalse(paid.backlash());
		AuraRules.Spend short_ = AuraRules.spend(5, 12);
		assertEquals(0, short_.left(), 1e-9);
		assertEquals(5, short_.paid(), 1e-9);
		assertTrue(short_.backlash());
		assertTrue(AuraRules.spend(0, 12).backlash(), "a technique failing at nothing");
		assertFalse(AuraRules.spend(12, 12).backlash(), "exactly enough is enough");
	}

	@Test
	void experienceComesFromMeaningfulBlowsOnRealFoes() {
		double zombie = AuraRules.worth(false, false, false, 20, 20);
		assertEquals(1.0, zombie, 1e-9);
		assertTrue(AuraRules.worth(false, false, false, 80, 20) > zombie, "a stronger foe teaches more");
		assertEquals(2.0, AuraRules.worth(false, false, false, 10_000, 20), 1e-9, "but only so much more");
		assertEquals(0.5, AuraRules.worth(false, false, false, 1, 20), 1e-9);
		assertEquals(MasteryRules.BOSS, AuraRules.worth(false, true, false, 300, 20), 1e-9);
		assertEquals(MasteryRules.RUNEBOUND, AuraRules.worth(false, false, true, 20, 20), 1e-9);
		assertEquals(MasteryRules.PLAYER, AuraRules.worth(true, false, false, 20, 20), 1e-9);
		// A zombie felled in four full swings.
		double kill = 3 * AuraRules.strike(1, 0.3, false) + AuraRules.strike(1, 0.1, true);
		assertEquals(4 * AuraRules.STRIKE + 1.0 + AuraRules.KILL, kill, 1e-9);
		assertTrue(kill > 2 && kill < 3, "about two and a half for an ordinary kill: " + kill);
		assertEquals(0, AuraRules.strike(0, 1, true), 1e-9);
		assertTrue(AuraRules.MAX_PER_STRIKE >= AuraRules.strike(2.0, 1, true), "a boss's killing blow isn't cut");
		// Practice: half rate, capped.
		assertEquals(5, AuraRules.practice(0, 10), 1e-9);
		assertEquals(0, AuraRules.practice(AuraRules.PRACTICE_CAP, 10), 1e-9);
		assertTrue(AuraRules.PRACTICE_CAP < AuraRules.threshold(AuraRules.FLOW), "dummies alone never make Flow");
	}

	@Test
	void theTuningWorksOut() {
		// About 300 an hour of meaningful fighting (a kill every ten seconds of combat, combat a third of play,
		// the moment and repetition about even): Flow in about half an hour, Edge in about two hours.
		double perKill = 4 * AuraRules.STRIKE + 1.0 + AuraRules.KILL;
		double perHour = perKill * 6 * 60 / 3.0;
		assertEquals(300, perHour, 1e-9);
		double flowHours = AuraRules.threshold(AuraRules.FLOW) / perHour;
		double edgeHours = AuraRules.threshold(AuraRules.EDGE) / perHour;
		assertTrue(flowHours >= 0.4 && flowHours <= 0.6, "Flow in " + flowHours + " h");
		assertTrue(edgeHours >= 1.5 && edgeHours <= 2.5, "Edge in " + edgeHours + " h");
	}

	@Test
	void aStrongerFoeIsABossARuneboundOrTwiceYourHealth() {
		assertTrue(AuraRules.stronger(true, false, 10, 20));
		assertTrue(AuraRules.stronger(false, true, 10, 20));
		assertTrue(AuraRules.stronger(false, false, 40, 20));
		assertFalse(AuraRules.stronger(false, false, 39, 20));
		assertTrue(AuraRules.STILLNESS_TICKS >= 20 * 25, "about half a minute of stillness");
		assertTrue(AuraRules.TRIAL_WINDOW >= 20 * 30);
	}

	@Test
	void everyFlavourGrowsWithItsStage() {
		for (int s = AuraRules.GLOW; s < AuraRules.MAX_STAGE; s++) {
			assertTrue(AuraRules.rimeTicks(s + 1) > AuraRules.rimeTicks(s));
			assertTrue(AuraRules.thunderChance(s + 1) > AuraRules.thunderChance(s));
			assertTrue(AuraRules.galeSpeed(s + 1) > AuraRules.galeSpeed(s));
			assertTrue(AuraRules.stoneResistance(s + 1) > AuraRules.stoneResistance(s));
			assertTrue(AuraRules.verdantHeal(s + 1) > AuraRules.verdantHeal(s));
			assertTrue(AuraRules.hollowPull(s + 1) > AuraRules.hollowPull(s));
			assertTrue(AuraRules.starlitGain(s + 1) > AuraRules.starlitGain(s));
			assertTrue(AuraRules.hourglassSpeed(s + 1) > AuraRules.hourglassSpeed(s));
			assertTrue(AuraRules.crimsonLeech(s + 1) > AuraRules.crimsonLeech(s));
			assertTrue(AuraRules.emberTicks(s + 1) >= AuraRules.emberTicks(s));
		}
		assertEquals(0, AuraRules.emberChance(AuraRules.GLOW), 1e-9);
		assertEquals(1, AuraRules.emberChance(AuraRules.EDGE), 1e-9, "Ember ignites at Edge");
		assertTrue(AuraRules.emberTicks(AuraRules.EDGE) >= 40);
		assertTrue(AuraRules.stoneResistance(AuraRules.SOVEREIGN) < 1.0, "never immune to knockback");
		assertTrue(AuraRules.galeSpeed(AuraRules.EDGE) <= 0.15, "a bit of speed, not a sprint");
	}

	@Test
	void theColourBurnsBrighterAsItGrows() {
		int ember = BreathingMethods.EMBER.color();
		assertEquals(ember, AuraRules.color(ember, BreathingMethods.EMBER.highlight(), AuraRules.GLOW));
		assertNotEquals(ember, AuraRules.color(ember, BreathingMethods.EMBER.highlight(), AuraRules.EDGE));
		assertEquals(0xFFFFFF, AuraRules.mix(0x000000, 0xFFFFFF, 1.0));
		assertEquals(0x808080, AuraRules.mix(0x000000, 0xFFFFFF, 0.5025));
	}

	@Test
	void tenMethodsOneForEachElement() {
		assertEquals(10, BreathingMethods.BUILT_IN.size());
		Set<String> elements = new HashSet<>();
		Set<String> ids = new HashSet<>();
		Set<BreathingMethod.Flavour> flavours = new HashSet<>();
		for (BreathingMethod m : BreathingMethods.BUILT_IN) {
			assertTrue(elements.add(m.element()), "one method per element: " + m.element());
			assertTrue(ids.add(m.id()));
			assertTrue(flavours.add(m.flavour()), "each its own flavour");
			assertEquals(m, BreathingMethods.byId(m.id()).orElseThrow());
			assertEquals(m, BreathingMethods.ofElement(m.element()).orElseThrow());
			assertEquals("aura.wildercord.method." + m.id(), m.nameKey());
		}
		assertEquals(Set.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"), elements);
		assertTrue(BreathingMethods.byId("").isEmpty());
		assertTrue(BreathingMethods.byId("nonsense").isEmpty());
		assertThrows(IllegalArgumentException.class, () -> new BreathingMethod(" ", "fire", 0, 0, null));
	}

	@Test
	void manualsAreFoundWhereFightingIsOld() {
		assertFalse(MethodSources.forLootTable("minecraft:chests/ancient_city").isEmpty());
		assertFalse(MethodSources.forLootTable("minecraft:chests/trial_chambers/reward_rare").isEmpty());
		assertFalse(MethodSources.forLootTable("minecraft:chests/stronghold_library").isEmpty());
		assertFalse(MethodSources.forLootTable("wildercord:chests/archive_vault").isEmpty());
		assertTrue(MethodSources.forLootTable("minecraft:chests/simple_dungeon").isEmpty());
		assertTrue(MethodSources.byId("weaponsmith").isPresent() && MethodSources.byId("cleric").isPresent(), "masters hand them down too");
		for (MethodSources.Source source : MethodSources.all()) {
			for (String method : source.weights().keySet()) {
				assertTrue(BreathingMethods.byId(method).isPresent(), source.id() + " gives an unknown method " + method);
			}
		}
		// Each place favours its methods: drawn many times, its favourite comes up most.
		MethodSources.Source sanctum = MethodSources.byId("ember_sanctum").orElseThrow();
		Map<String, Integer> counts = new java.util.HashMap<>();
		Random random = new Random(7);
		for (int i = 0; i < 4000; i++) {
			counts.merge(sanctum.draw(random).orElseThrow(), 1, Integer::sum);
		}
		assertTrue(counts.get("ember") > 2 * counts.getOrDefault("rime", 0), counts.toString());
		assertEquals(10, counts.size(), "the others can still turn up");
		assertTrue(new MethodSources.Source("empty", "", 0, Map.of()).draw(random).isEmpty());
	}

	@Test
	void theStageRegistryOpensGlowToEdgeAndKeepsTheRoadReady() {
		assertEquals(AuraRules.EDGE, AuraStages.highest());
		assertEquals(3, AuraStages.open().size());
		assertTrue(AuraStages.canBreakThrough(AuraRules.GLOW));
		assertTrue(AuraStages.canBreakThrough(AuraRules.FLOW));
		assertFalse(AuraStages.canBreakThrough(AuraRules.EDGE), "Form isn't open until a later wave registers it");
		assertEquals(AuraRules.threshold(AuraRules.FORM), AuraStages.cap(AuraRules.EDGE), "the road to Form still fills, and waits");
		assertEquals(-1, AuraStages.cap(AuraRules.SOVEREIGN));
		assertEquals(70, AuraStages.capacity(AuraRules.EDGE));
		assertEquals(110, AuraStages.capacity(AuraRules.FORM), "an unopened stage keeps its default");
		assertThrows(IllegalArgumentException.class, () -> new AuraStages.Stage(9, "nine", 1, 1));
	}
}
