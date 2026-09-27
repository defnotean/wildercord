package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

class SpellCompilerTest {
	private static SpellCompiler.Compiled compile(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes));
	}

	@Test
	void effectsWithoutAShapeTargetYou() {
		SpellCompiler.Compiled c = compile(FEATHER_FALL, EXTEND);
		SpellPlan.Group g = c.root().groups.getFirst();
		assertTrue(g.implicit);
		assertSame(SELF, g.shape);
		assertEquals(1, g.effects.getFirst().count(EXTEND));
		assertEquals(List.of("You: Feather Fall (2x duration)"), c.lines());
	}

	@Test
	void splitSkipsPastEffectsToFindTheShape() {
		SpellCompiler.Compiled c = compile(BOLT, FIRE, SPLIT_MOD);
		SpellPlan.Group g = c.root().groups.getFirst();
		assertEquals(1, g.count(SPLIT_MOD));
		assertTrue(g.effects.getFirst().mods.isEmpty());
		assertEquals(0, c.attachedTo()[2]); // attached to the Bolt at index 0
		assertEquals((3 + 8 * 1.1) * 2.4, c.cost(), 1e-9);
		assertEquals("3 bolts: Fire", c.lines().getFirst());
	}

	@Test
	void amplifyTakesTheClosestEffect() {
		SpellCompiler.Compiled c = compile(BOLT, FIRE, HARM, AMPLIFY);
		SpellPlan.Group g = c.root().groups.getFirst();
		assertEquals(0, g.effects.get(0).count(AMPLIFY));
		assertEquals(1, g.effects.get(1).count(AMPLIFY));
	}

	@Test
	void modifierWithNothingToChangeWarns() {
		SpellCompiler.Compiled c = compile(SELF, HEAL, PIERCE_MOD);
		assertEquals(SpellCompiler.UNATTACHED, c.attachedTo()[2]);
		assertTrue(c.warnings().stream().anyMatch(w -> w.startsWith("Pierce does nothing")));
	}

	@Test
	void linksSplitTheSpellAndUseTheTriggerAfterwards() {
		SpellCompiler.Compiled c = compile(BOLT, ON_HIT, LIGHTNING);
		SpellPlan.Link link = c.root().link;
		assertNotNull(link);
		assertSame(c.root().groups.getFirst(), link.anchor);
		SpellPlan.Group after = link.next.groups.getFirst();
		assertSame(TRIGGER, after.shape);
		assertEquals(List.of("A bolt: nothing yet", "On hit:", "  The target: Lightning"), c.lines());
		assertTrue(c.warnings().isEmpty(), c.warnings().toString()); // the bolt feeds the link, so it isn't "empty"
	}

	@Test
	void extendAfterDelayStretchesTheDelayUntilSomethingCloserTakesIt() {
		SpellCompiler.Compiled onDelay = compile(BOLT, FIRE, DELAY, EXTEND, BOLT, FIRE);
		assertEquals(40, SpellNumbers.delayTicks(onDelay.root().link));
		SpellCompiler.Compiled onFire = compile(BOLT, FIRE, DELAY, BOLT, FIRE, EXTEND);
		assertEquals(20, SpellNumbers.delayTicks(onFire.root().link));
		assertEquals(1, onFire.root().link.next.groups.getFirst().effects.getFirst().count(EXTEND));
	}

	@Test
	void modifiersNeverReachBackPastALink() {
		SpellCompiler.Compiled c = compile(BOLT, ON_HIT, SPLIT_MOD);
		assertEquals(SpellCompiler.UNATTACHED, c.attachedTo()[2]);
	}

	@Test
	void echoRepeatsAndPaysForEverythingBeforeIt() {
		SpellCompiler.Compiled c = compile(BOLT, FIRE, ECHO);
		double bolt = 3 + 8 * 1.1;
		assertEquals(bolt + 2 + bolt, c.cost(), 1e-9);
		assertNotNull(c.root().link.echoPrefix);
		assertEquals(List.of("A bolt: Fire", "0.5s later, everything before this fires again."), c.lines());
	}

	@Test
	void designDocExampleCostsWhatTheDocSays() {
		// Self · Launch · On Land · Burst · Lightning
		SpellCompiler.Compiled c = compile(SELF, LAUNCH, ON_LAND, BURST, LIGHTNING);
		assertEquals(8 + 2 + (6 + 20 * 1.5), c.cost(), 1e-9);
		assertEquals(46, c.manaCost());
		assertEquals(46, c.cooldownTicks());
	}

	@Test
	void cooldownIsClamped() {
		assertEquals(10, SpellNumbers.cooldownTicks(1));
		assertEquals(400, SpellNumbers.cooldownTicks(10_000));
	}

	@Test
	void emptyShapeWarns() {
		SpellCompiler.Compiled c = compile(BURST);
		assertTrue(c.warnings().contains("Burst has no effect after it."));
	}

	@Test
	void pulsePaysForTheRestThreeTimes() {
		SpellCompiler.Compiled c = compile(PULSE, BOLT, FIRE);
		assertEquals(2 + 3 * (3 + 8 * 1.1), c.cost(), 1e-9);
		assertSame(SELF, c.root().link.next.implicitShape);
		assertEquals("3 times, every 1s:", c.lines().getFirst());
	}

	@Test
	void onHurtTargetsTheAttacker() {
		SpellCompiler.Compiled c = compile(ON_HURT, FIRE);
		assertSame(TRIGGER, c.root().link.next.groups.getFirst().shape);
		assertEquals(List.of("When something hurts you:", "  The target: Fire"), c.lines());
	}

	@Test
	void frugalHalvesCostAndWeakensAnyEffect() {
		SpellCompiler.Compiled c = compile(BOLT, LIGHTNING, FRUGAL_MOD);
		SpellPlan.EffectNode e = c.root().groups.getFirst().effects.getFirst();
		assertEquals(1, e.count(FRUGAL_MOD));
		assertEquals(0.6, SpellNumbers.power(e), 1e-9);
		assertEquals(3 + 20 * 0.5 * 1.1, c.cost(), 1e-9);
	}

	@Test
	void volleyAndLingerAttachWhereTheyCan() {
		SpellCompiler.Compiled c = compile(ARC, FIRE, LINGER_MOD, VOLLEY_MOD);
		SpellPlan.Group g = c.root().groups.getFirst();
		assertEquals(3, SpellNumbers.volleyShots(g));
		assertEquals(2, SpellNumbers.lingerHits(g.effects.getFirst()));
		// Linger can't change Heal-free utility like Light.
		assertEquals(SpellCompiler.UNATTACHED, compile(SELF, LIGHT, LINGER_MOD).attachedTo()[2]);
	}

	@Test
	void bloodPricePaysInHealthAtOnePerFiveMana() {
		SpellCompiler.Compiled c = compile(BOLT, BLOOD_PRICE_MOD, HARM);
		assertEquals(0, c.attachedTo()[1]); // on the Bolt, like Rapid
		double cost = 3 + 8 * 1.1;
		assertEquals(cost, c.cost(), 1e-9);
		assertTrue(c.paysInHealth());
		assertEquals((int) Math.ceil(cost / 5), c.healthCost());
		assertEquals("Costs 3 health instead of mana.", c.lines().getLast());
		assertFalse(compile(BOLT, HARM).paysInHealth());
	}

	@Test
	void vowDoublesPowerAndQuadruplesCooldown() {
		SpellCompiler.Compiled plain = compile(BOLT, HARM);
		SpellCompiler.Compiled vowed = compile(BOLT, VOW_MOD, HARM);
		assertEquals(2.0, SpellNumbers.groupPower(vowed.root().groups.getFirst()), 1e-9);
		assertEquals(plain.cooldownTicks() * 4, vowed.cooldownTicks());
		assertTrue(vowed.lines().getFirst().contains("vowed"));
	}

	@Test
	void conditionsKeepTheShapeTheyInterrupt() {
		for (RuneDef condition : List.of(IF_AIRBORNE, COMBO)) {
			SpellCompiler.Compiled c = compile(condition, HARM);
			assertSame(SELF, c.root().link.next.implicitShape, condition.name());
		}
		assertEquals(List.of("Every 3rd cast:", "  You: Swift"), compile(COMBO, SWIFT).lines());
	}

	@Test
	void manyHitShapesAreSofterPerHit() {
		assertEquals(0.6, SpellNumbers.groupPower(compile(STAND, HARM).root().groups.getFirst()), 1e-9);
		assertEquals(0.35, SpellNumbers.groupPower(compile(BARRAGE, HARM).root().groups.getFirst()), 1e-9);
		assertEquals(12, SpellNumbers.barrageBlows(compile(BARRAGE, QUICKEN, HARM).root().groups.getFirst()));
		assertEquals(2.0, SpellNumbers.executeBonus(compile(BOLT, HARM, EXECUTE_MOD).root().groups.getFirst().effects.getFirst()), 1e-9);
	}

	@Test
	void domainWidensUpTo24Blocks() {
		assertEquals(9.0, SpellNumbers.domainRadius(compile(DOMAIN, HARM).root().groups.getFirst()), 1e-9);
		assertEquals(13.5, SpellNumbers.domainRadius(compile(DOMAIN, WIDEN, HARM).root().groups.getFirst()), 1e-9);
		assertEquals(24.0, SpellNumbers.domainRadius(compile(DOMAIN, WIDEN, WIDEN, WIDEN, HARM).root().groups.getFirst()), 1e-9);
	}

	@Test
	void everyRuneHasAKnownCategory() {
		for (RuneDef rune : Runes.all()) {
			if (rune != TRIGGER) {
				assertTrue(RuneCategories.of(rune.family()).contains(rune.category()), rune.id() + " -> " + rune.category());
			}
		}
		assertEquals("time", STASIS.category());
		assertEquals("personal", STAND.category());
	}

	@Test
	void everyRuneIdIsNamespacedAndUnique() {
		assertEquals(Runes.all().size(), Runes.all().stream().map(RuneDef::id).distinct().count());
		assertTrue(Runes.all().stream().allMatch(r -> r.id().startsWith("wildercord:")));
		assertTrue(Runes.all().stream().filter(r -> r.family() == RuneFamily.MODIFIER).allMatch(r -> !r.needs().isEmpty()));
	}
}
