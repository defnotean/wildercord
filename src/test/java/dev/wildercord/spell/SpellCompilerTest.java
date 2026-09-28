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
	void imbueStoresTheRestForThreeChargesAndPaysForThem() {
		SpellCompiler.Compiled c = compile(SELF, IMBUE, FIRE);
		SpellPlan.Link link = c.root().link;
		assertNotNull(link);
		assertSame(c.root().groups.getFirst(), link.anchor);
		assertSame(TRIGGER, link.next.groups.getFirst().shape);
		// Self (0) + Imbue (3) + Fire (8) three times over.
		assertEquals(3 + 8 * 3, c.cost(), 1e-9);
		assertEquals(List.of("Stored in the item in your hand (3 charges), then:", "  The target: Fire"), c.lines());
		assertTrue(c.warnings().isEmpty(), c.warnings().toString());
		assertEquals(List.of(FIRE), SpellCompiler.stored(List.of(SELF, IMBUE, FIRE)));
	}

	@Test
	void storedRunesAreReadAsAfterALink() {
		SpellCompiler.Compiled stored = SpellCompiler.compileStored(List.of(FIRE, EXTEND));
		SpellPlan.Group g = stored.root().groups.getFirst();
		assertTrue(g.implicit);
		assertSame(TRIGGER, g.shape);
		SpellCompiler.Compiled bolt = SpellCompiler.compileStored(List.of(BOLT, FROST));
		assertSame(BOLT, bolt.root().groups.getFirst().shape);
	}

	@Test
	void anImbueCantStoreAnother() {
		SpellCompiler.Compiled c = compile(SELF, IMBUE, BOLT, IMBUE, FIRE);
		assertTrue(c.warnings().contains("An Imbue can't store another Imbue."));
	}

	@Test
	void anEchoInAStoredSpellRepeatsOnlyWhatWasStoredAtItsTarget() {
		SpellCompiler.Compiled released = SpellCompiler.compileStored(List.of(FIRE, ECHO));
		SpellPlan.Segment prefix = released.root().link.echoPrefix;
		assertSame(TRIGGER, prefix.groups.getFirst().shape);
		// Priced the same inside the whole spell as when it's released on its own: never with Self and Imbue again.
		SpellCompiler.Compiled whole = compile(SELF, IMBUE, FIRE, ECHO);
		assertEquals(IMBUE.cost() + released.cost() * SpellNumbers.IMBUE_CHARGES, whole.cost(), 1e-9);
		SpellPlan.Segment inWhole = whole.root().link.next.link.echoPrefix;
		assertEquals(List.of(FIRE), inWhole.groups.getFirst().effects.stream().map(e -> e.effect).toList());
		assertSame(TRIGGER, inWhole.groups.getFirst().shape);
	}

	@Test
	void comboInAStoredSpellIsFlagged() {
		String warning = "Combo never fires in an imbued spell: every release counts as a first cast.";
		assertTrue(compile(SELF, IMBUE, COMBO, FIRE).warnings().contains(warning));
		assertFalse(compile(COMBO, SWIFT).warnings().contains(warning));
	}

	@Test
	void shieldShowsHowLongItHolds() {
		assertEquals(List.of("You: Shield (30s)"), compile(SHIELD).lines());
		assertEquals(List.of("You: Shield (2x duration, 60s)"), compile(SHIELD, EXTEND).lines());
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
	void rapidIsPricedOnTheWholeSpellWhereverItSits() {
		double bolt = 3 + 8 * 1.1;
		// On an empty Self, on the Bolt, after the Fire: the same spell, the same price, the same cooldown.
		SpellCompiler.Compiled onSelf = compile(SELF, RAPID_MOD, BOLT, FIRE);
		SpellCompiler.Compiled onBolt = compile(BOLT, RAPID_MOD, FIRE);
		SpellCompiler.Compiled after = compile(BOLT, FIRE, RAPID_MOD);
		for (SpellCompiler.Compiled c : List.of(onSelf, onBolt, after)) {
			assertEquals(bolt * 1.4, c.cost(), 1e-9);
			assertEquals(SpellNumbers.cooldownTicks(bolt * 1.4, 1), c.cooldownTicks());
		}
		// Across a link: the cheap group or the dear one, the whole spell pays.
		double whole = (bolt + 2 + (6 + 18 * 1.5)) * 1.4;
		assertEquals(whole, compile(BOLT, FIRE, RAPID_MOD, ON_HIT, BURST, EXPLODE).cost(), 1e-9);
		assertEquals(whole, compile(BOLT, FIRE, ON_HIT, BURST, EXPLODE, RAPID_MOD).cost(), 1e-9);
		// Two Rapids pay twice; one repeated by an Echo halves the cooldown once and pays once.
		assertEquals(bolt * 1.4 * 1.4, compile(SELF, RAPID_MOD, RAPID_MOD, BOLT, FIRE).cost(), 1e-9);
		SpellCompiler.Compiled echoed = compile(BOLT, RAPID_MOD, FIRE, ECHO);
		assertEquals((bolt + 2 + bolt) * 1.4, echoed.cost(), 1e-9);
		assertEquals(SpellNumbers.cooldownTicks(echoed.cost(), 1), echoed.cooldownTicks());
		assertTrue(SpellCompiler.wholeSpell(RAPID_MOD) && SpellCompiler.wholeSpell(VOW_MOD) && SpellCompiler.wholeSpell(BLOOD_PRICE_MOD));
		assertFalse(SpellCompiler.wholeSpell(SPLIT_MOD));
	}

	@Test
	void vowAndBloodPriceChangeTheWholeSpellWhereverTheySit() {
		double bolt = 3 + 8 * 1.1;
		SpellCompiler.Compiled vowOnSelf = compile(SELF, VOW_MOD, BOLT, FIRE);
		assertEquals(bolt, vowOnSelf.cost(), 1e-9);
		assertEquals(compile(BOLT, VOW_MOD, FIRE).cooldownTicks(), vowOnSelf.cooldownTicks());
		// It strengthens only its own shape's effects: on an empty one, it's a longer cooldown for nothing.
		assertTrue(vowOnSelf.warnings().contains("Vow strengthens only Self's effects, and it has none: the cooldown is 4x longer for nothing."));
		assertTrue(compile(BOLT, VOW_MOD, ON_HIT, FIRE).warnings().stream().anyMatch(w -> w.startsWith("Vow strengthens only Bolt's")));
		assertFalse(compile(BOLT, VOW_MOD, FIRE).warnings().stream().anyMatch(w -> w.startsWith("Vow")));
		// Blood Price pays for the whole spell in health, wherever it sits.
		SpellCompiler.Compiled blood = compile(SELF, BLOOD_PRICE_MOD, BOLT, FIRE, ON_HIT, BURST, EXPLODE);
		assertEquals(SpellNumbers.healthCost(blood.cost()), blood.healthCost());
		assertEquals(compile(BOLT, FIRE, ON_HIT, BURST, BLOOD_PRICE_MOD, EXPLODE).healthCost(), blood.healthCost());
	}

	@Test
	void anEchoOrPulseAfterOnHitGoesOffForTheFirstHitOnly() {
		// Paid for once (the Echo repeats the whole spell once), so it goes off once, not for every bolt that hits.
		SpellCompiler.Compiled c = compile(BOLT, FIRE, SPLIT_MOD, ON_HIT, BURST, EXPLODE, ECHO);
		SpellPlan.Link echo = c.root().link.next.link;
		assertSame(ECHO, echo.link);
		assertTrue(echo.firstOnly);
		double once = (3 + 8 * 1.1) * 2.4 + 2 + (6 + 18 * 1.5);
		assertEquals(once + 2 + once, c.cost(), 1e-9);
		assertEquals(List.of("3 bolts: Fire", "On hit:", "  Everything within 4 blocks: Explode",
			"  0.5s later, everything before this fires again (first hit only)."), c.lines());
		// What it repeats has no Echo of its own.
		assertNull(echo.echoPrefix.link.next.link);

		// Not after a link that fires once, nor at the start.
		assertFalse(compile(BOLT, FIRE, ECHO).root().link.firstOnly);
		assertFalse(compile(PULSE, BOLT, FIRE).root().link.firstOnly);
		assertFalse(compile(BOLT, FIRE, DELAY, BOLT, ECHO).root().link.next.link.firstOnly);

		// A Pulse after On Kill: the first kill's.
		SpellCompiler.Compiled pulse = compile(BOLT, SPLIT_MOD, ON_KILL, PULSE, BURST, FIRE);
		assertTrue(pulse.root().link.next.link.firstOnly);
		assertEquals("  3 times, every 1s (first kill only):", pulse.lines().get(2));
		// Still after the hit through a Delay or a condition.
		assertTrue(compile(BOLT, ON_HIT, DELAY, BURST, FIRE, ECHO).root().link.next.link.next.link.firstOnly);
		// Each of a Pulse's runs is paid for: an Echo in one goes off every run.
		SpellPlan.Link inPulse = compile(BOLT, ON_HIT, PULSE, FIRE, ECHO).root().link.next.link.next.link;
		assertSame(ECHO, inPulse.link);
		assertFalse(inPulse.firstOnly);
		// And a stored spell is released on its own, even one stored on a hit.
		SpellPlan.Link stored = compile(BOLT, ON_HIT, SELF, IMBUE, FIRE, ECHO).root().link.next.link.next.link;
		assertSame(ECHO, stored.link);
		assertFalse(stored.firstOnly);
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
		assertEquals(1.0, SpellNumbers.groupPower(compile(BOLT, HARM).root().groups.getFirst()), 1e-9);
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
		assertEquals("personal", ORBIT.category());
	}

	@Test
	void everyRuneIdIsNamespacedAndUnique() {
		assertEquals(Runes.all().size(), Runes.all().stream().map(RuneDef::id).distinct().count());
		assertTrue(Runes.all().stream().allMatch(r -> r.id().startsWith("wildercord:")));
		assertTrue(Runes.all().stream().filter(r -> r.family() == RuneFamily.MODIFIER).allMatch(r -> !r.needs().isEmpty()));
	}
}
