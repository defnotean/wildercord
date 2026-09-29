package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The second batch of new runes: three shapes (Glaive, Imprint, Latch), three modifiers (Kindred, Thirst,
 * Belated), two links (On Reaction, On Weakness) and an effect for every element. How they read, what they
 * cost and what the readout says.
 */
class CraftedRunesTest {
	private static final List<RuneDef> BATCH = List.of(GLAIVE, IMPRINT, LATCH, KINDRED, THIRST, BELATED, ON_REACTION, ON_WEAKNESS,
		SPELLBRAND, GASH, PROSPECT, SEARING_EDGE, FLASH_FREEZE, DROWSE, GALVANIZE, PROLONG, UMBRA, DISARM);

	private static SpellCompiler.Compiled compile(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes));
	}

	private static SpellPlan.Group group(RuneDef... runes) {
		return compile(runes).root().groups.getFirst();
	}

	private static SpellPlan.EffectNode effect(RuneDef... runes) {
		return group(runes).effects.getFirst();
	}

	@Test
	void theBatchIsEighteenCraftedRunesWithAnEffectForEveryElement() {
		assertEquals(18, BATCH.size());
		assertEquals(BATCH.size(), BATCH.stream().distinct().count());
		assertEquals(3, BATCH.stream().filter(r -> r.family() == RuneFamily.SHAPE).count());
		assertEquals(3, BATCH.stream().filter(r -> r.family() == RuneFamily.MODIFIER).count());
		assertEquals(2, BATCH.stream().filter(r -> r.family() == RuneFamily.LINK).count());
		Set<String> elements = BATCH.stream().filter(r -> r.family() == RuneFamily.EFFECT).map(RuneDef::element).collect(Collectors.toSet());
		assertEquals(Set.of("arcane", "blood", "earth", "fire", "frost", "life", "storm", "time", "void", "wind"), elements);
		for (RuneDef rune : BATCH) {
			assertTrue(rune.tier() >= 1 && rune.tier() <= 3, rune.name());
			assertTrue(Runes.common(rune), rune.name() + " turns up like any crafted rune");
			assertTrue(Runes.obtainable(rune), rune.name());
			assertFalse(RuneSources.foundOnly(rune), rune.name() + " is crafted, not a rune of the world");
			assertSame(rune, Runes.get(rune.id()).orElseThrow());
		}
	}

	@Test
	void newRunesAreFiledWhereTheyBelong() {
		assertEquals("projectile", GLAIVE.category());
		assertEquals("lingering", IMPRINT.category());
		assertEquals("direct", LATCH.category());
		assertEquals("area", KINDRED.category());
		assertEquals("power", THIRST.category());
		assertEquals("timing", BELATED.category());
		assertEquals("trigger", ON_REACTION.category());
		assertEquals("trigger", ON_WEAKNESS.category());
		for (RuneDef control : List.of(GASH, FLASH_FREEZE, DROWSE, DISARM)) {
			assertEquals("control", control.category(), control.name());
		}
		assertEquals("support", SEARING_EDGE.category());
		assertEquals("world", PROSPECT.category());
		assertEquals("world", GALVANIZE.category());
		assertEquals("time", PROLONG.category());
		assertEquals("damage", SPELLBRAND.category());
		assertEquals("damage", UMBRA.category());
		assertEquals(EffectKind.WORLD, PROSPECT.kind());
		assertEquals(EffectKind.WORLD, GALVANIZE.kind());
		assertEquals(EffectKind.HELPFUL, SEARING_EDGE.kind());
		assertEquals(EffectKind.HELPFUL, PROLONG.kind());
	}

	// ------------------------------------------------------------------ shapes

	@Test
	void aGlaiveFliesOutAndBackAndGrowsWithItsModifiers() {
		SpellCompiler.Compiled c = compile(GLAIVE, HARM);
		assertEquals(5 + 8 * 1.8, c.cost(), 1e-9);
		assertEquals("A glaive (out 12 blocks and back): Harm", c.lines().getFirst());
		assertEquals("3 glaives (out 12 blocks and back): Harm", compile(GLAIVE, SPLIT_MOD, HARM).lines().getFirst());
		assertEquals(1.0, SpellNumbers.glaiveWidth(group(GLAIVE, HARM)), 1e-9);
		assertEquals(1.5, SpellNumbers.glaiveWidth(group(GLAIVE, WIDEN, HARM)), 1e-9);
		assertEquals(1.0, SpellNumbers.glaiveSpeed(group(GLAIVE, HARM)), 1e-9);
		assertEquals(1.5, SpellNumbers.glaiveSpeed(group(GLAIVE, QUICKEN, HARM)), 1e-9);
		// Volley, Pierce and Homing have nothing on a glaive to change.
		assertEquals(SpellCompiler.UNATTACHED, compile(GLAIVE, VOLLEY_MOD, HARM).attachedTo()[1]);
	}

	@Test
	void anImprintWaitsWhereYouStoodThenErupts() {
		SpellCompiler.Compiled c = compile(IMPRINT, HARM);
		assertEquals(3 + 8 * 1.3, c.cost(), 1e-9);
		assertEquals("An imprint (3 blocks, erupts after 2s): Harm", c.lines().getFirst());
		assertEquals(40, SpellNumbers.imprintDelay(group(IMPRINT, HARM)));
		assertEquals(20, SpellNumbers.imprintDelay(group(IMPRINT, QUICKEN, HARM)));
		assertEquals(10, SpellNumbers.imprintDelay(group(IMPRINT, QUICKEN, QUICKEN, QUICKEN, HARM)));
		assertEquals(4.5, SpellNumbers.imprintRadius(group(IMPRINT, WIDEN, HARM)), 1e-9);
		assertEquals("3 imprints (3 blocks, erupts after 2s): Harm", compile(IMPRINT, SPLIT_MOD, HARM).lines().getFirst());
	}

	@Test
	void aLatchStrikesItsCreatureAgainAndAgainAtSeventyPercent() {
		SpellPlan.Group g = group(LATCH, HARM);
		assertEquals(0.7, SpellNumbers.groupPower(g), 1e-9);
		assertEquals(4, SpellNumbers.latchStrikes(g));
		assertEquals(20, SpellNumbers.latchInterval(g));
		assertEquals("A latch (4 strikes, every 1s, 70% power each): Harm", compile(LATCH, HARM).lines().getFirst());
		// Extend holds it twice as long; Quicken strikes twice as often in the same time.
		SpellPlan.Group extended = group(LATCH, EXTEND, HARM);
		assertEquals(8, SpellNumbers.latchStrikes(extended));
		assertEquals(20, SpellNumbers.latchInterval(extended));
		SpellPlan.Group quick = group(LATCH, QUICKEN, HARM);
		assertEquals(8, SpellNumbers.latchStrikes(quick));
		assertEquals(10, SpellNumbers.latchInterval(quick));
		assertEquals(16, SpellNumbers.latchStrikes(group(LATCH, EXTEND, EXTEND, QUICKEN, QUICKEN, HARM)));
		// Extend right after Harm changes Harm? Harm has no duration, so it reaches back to the Latch.
		assertEquals(0, compile(LATCH, HARM, EXTEND).attachedTo()[2]);
	}

	// ------------------------------------------------------------------ modifiers

	@Test
	void kindredSharesOnlyHelpfulEffectsThatLandOnEachCreature() {
		SpellCompiler.Compiled heal = compile(SELF, HEAL, KINDRED);
		assertEquals(1, heal.attachedTo()[2]);
		assertEquals(12 * 1.4, heal.cost(), 1e-9);
		assertEquals("You: Heal (shared at 50% power)", heal.lines().getFirst());
		// A harmful effect has nothing to share, and neither does a summon or a dome, which act on a place.
		assertEquals(SpellCompiler.UNATTACHED, compile(BOLT, FIRE, KINDRED).attachedTo()[2]);
		assertEquals(SpellCompiler.UNATTACHED, compile(SELF, SUMMON, KINDRED).attachedTo()[2]);
		assertEquals(SpellCompiler.UNATTACHED, compile(SELF, HAVEN, KINDRED).attachedTo()[2]);
		assertEquals(SpellCompiler.UNATTACHED, compile(SELF, REVERSAL, KINDRED).attachedTo()[2]);
		// It skips a harmful effect to find the helpful one before it.
		assertEquals(1, compile(BURST, REGROWTH, FIRE, KINDRED).attachedTo()[3]);
		assertTrue(SEARING_EDGE.has(Trait.SHARE));
		assertTrue(PROLONG.has(Trait.SHARE));
		assertFalse(UMBRA.has(Trait.SHARE));
		assertEquals(0.5, SpellNumbers.KINDRED_SHARE, 1e-9);
	}

	@Test
	void thirstHealsAQuarterOfTheDamageAndStacksToThreeQuarters() {
		SpellCompiler.Compiled c = compile(BOLT, HARM, THIRST);
		assertEquals(1, c.attachedTo()[2]);
		assertEquals(3 + 8 * 1.4 * 1.1, c.cost(), 1e-9);
		assertEquals("A bolt: Harm (heals you 25% of its damage)", c.lines().getFirst());
		assertEquals(0.25, SpellNumbers.thirstShare(effect(BOLT, HARM, THIRST)), 1e-9);
		assertEquals(0.5, SpellNumbers.thirstShare(effect(BOLT, HARM, THIRST, THIRST)), 1e-9);
		assertEquals(0.75, SpellNumbers.thirstShare(effect(BOLT, HARM, THIRST, THIRST, THIRST, THIRST)), 1e-9);
		assertEquals(0.0, SpellNumbers.thirstShare(effect(BOLT, HARM)), 1e-9);
		// It needs power: nothing to change on a Night Eye.
		assertEquals(SpellCompiler.UNATTACHED, compile(SELF, NIGHT_EYE, THIRST).attachedTo()[2]);
		assertEquals("Thirsting Arcane Bolt", SpellNames.auto(List.of(BOLT, HARM, THIRST)));
	}

	@Test
	void belatedLandsLateButStronger() {
		SpellCompiler.Compiled c = compile(BOLT, HARM, BELATED);
		assertEquals(1, c.attachedTo()[2]);
		assertEquals(3 + 8 * 1.25 * 1.1, c.cost(), 1e-9);
		assertEquals("A bolt: Harm (+40% power, lands 1.5s late)", c.lines().getFirst());
		SpellPlan.EffectNode once = effect(BOLT, HARM, BELATED);
		assertEquals(1.4, SpellNumbers.power(once), 1e-9);
		assertEquals(30, SpellNumbers.belatedTicks(once));
		// Only three count, for power and for the wait alike.
		SpellPlan.EffectNode many = effect(BOLT, HARM, BELATED, BELATED, BELATED, BELATED);
		assertEquals(Math.pow(1.4, 3), SpellNumbers.power(many), 1e-9);
		assertEquals(90, SpellNumbers.belatedTicks(many));
		assertEquals(0, SpellNumbers.belatedTicks(effect(BOLT, HARM)));
		// With Amplify too, the readout says the whole of it.
		assertEquals("A bolt: Harm (+110% power, lands 1.5s late)", compile(BOLT, HARM, AMPLIFY, BELATED).lines().getFirst());
	}

	@Test
	void theNewModifiersStayOutOfPassives() {
		for (RuneDef no : List.of(KINDRED, THIRST, BELATED, ON_REACTION, ON_WEAKNESS, GLAIVE, IMPRINT, LATCH, PROLONG, DROWSE)) {
			assertFalse(Passives.allowed(no), no.name());
		}
		assertTrue(Passives.allowed(SEARING_EDGE));
		assertNull(Passives.problem(List.of(SELF, SEARING_EDGE)));
		assertNull(Passives.problem(List.of(ORBIT, UMBRA)));
		assertTrue(Passives.problem(List.of(SELF, UMBRA)).contains("needs an Orbit"));
	}

	// ------------------------------------------------------------------ links

	@Test
	void onReactionAndOnWeaknessWatchTheGroupBeforeThemLikeOnHit() {
		for (RuneDef link : List.of(ON_REACTION, ON_WEAKNESS)) {
			SpellCompiler.Compiled c = compile(BOLT, FROST, link, BLIND);
			SpellPlan.Link l = c.root().link;
			assertSame(link, l.link);
			assertSame(c.root().groups.getFirst(), l.anchor, link.name() + " watches the bolt");
			assertSame(TRIGGER, l.next.groups.getFirst().shape, link.name() + ": what follows lands on the creature");
			assertTrue(SpellCompiler.firesAtHits(link));
			// Bolt and its Frost, the link's flat 2, then Blind on the target.
			assertEquals(3 + 8 * 1.1 + 2 + 5, c.cost(), 1e-9);
			assertTrue(c.warnings().isEmpty(), c.warnings().toString());
			assertTrue(compile(FROST, link, BLIND).warnings().isEmpty(), "the implicit Self is a shape to watch");
		}
		assertEquals(List.of("A bolt: Frost", "On a reaction:", "  The target: Blind"), compile(BOLT, FROST, ON_REACTION, BLIND).lines());
		assertEquals(List.of("A bolt: Venom", "On a weakness struck:", "  The target: Blind"), compile(BOLT, VENOM, ON_WEAKNESS, BLIND).lines());
		assertFalse(SpellCompiler.firesAtHits(DELAY));
		assertTrue(SpellCompiler.firesAtHits(ON_HIT));
	}

	@Test
	void aRepeatAfterThemGoesOffOnce() {
		SpellCompiler.Compiled c = compile(BOLT, FIRE, ON_REACTION, BURST, EXPLODE, ECHO);
		assertTrue(c.root().link.next.link.firstOnly);
		assertTrue(c.lines().contains("  0.5s later, everything before this fires again (first reaction only)."), c.lines().toString());
		SpellCompiler.Compiled weak = compile(BOLT, VENOM, ON_WEAKNESS, PULSE, BLIND);
		assertTrue(weak.root().link.next.link.firstOnly);
		assertTrue(weak.lines().contains("  3 times, every 1s (first hit only):"), weak.lines().toString());
	}

	@Test
	void theyNeedAShapeToWatchAndSomethingToFire() {
		// Straight after another link there's no group of this segment's for it to watch.
		assertTrue(compile(BOLT, FIRE, DELAY, ON_REACTION, BLIND).warnings().contains("On Reaction needs a shape before it to watch."));
		assertTrue(compile(BOLT, FIRE, ON_WEAKNESS).warnings().contains("On Weakness has nothing after it."));
	}

	// ------------------------------------------------------------------ effects and reactions

	@Test
	void theNewEffectsPlayTheirPartInReactions() {
		assertEquals(ReactionRules.SHADOWED, ReactionRules.marks(UMBRA));
		assertEquals(ReactionRules.BLEEDING, ReactionRules.marks(GASH));
		// Drowse and Disarm deal no damage of their own, so they set nothing off.
		assertNull(ReactionRules.triggers(DROWSE));
		assertNull(ReactionRules.triggers(DISARM));
		// Spellbrand's burst is arcane damage: on two marks or more it unweaves them.
		assertEquals(ReactionRules.UNWEAVE, ReactionRules.triggers(SPELLBRAND));
		assertNull(ReactionRules.triggers(PROSPECT));
		assertNull(ReactionRules.triggers(GALVANIZE));
		assertEquals(WorldRules.Interaction.FREEZE, WorldRules.of(FLASH_FREEZE));
		assertEquals(WorldRules.Interaction.NONE, WorldRules.of(GALVANIZE));
	}

	@Test
	void theWikisWorkedExamplesAddUp() {
		SpellCompiler.Compiled reaction = compile(BEAM, FROST, FIRE, ON_REACTION, BURST, EXPLODE);
		assertEquals(59, reaction.manaCost());
		assertEquals(58, reaction.cooldownTicks());
		assertEquals(List.of("A beam: Frost, Fire", "On a reaction:", "  Everything within 4 blocks: Explode"), reaction.lines());
		SpellCompiler.Compiled shared = compile(SELF, HEAL, KINDRED);
		assertEquals(17, shared.manaCost());
		assertEquals(17, shared.cooldownTicks());
	}

	@Test
	void newSpellsGetReadableNames() {
		assertEquals("Umbra Glaive", SpellNames.auto(List.of(GLAIVE, UMBRA)));
		assertEquals("Belated Gash Latch", SpellNames.auto(List.of(LATCH, GASH, BELATED)));
		assertEquals("Kindred Heal", SpellNames.auto(List.of(SELF, HEAL, KINDRED)));
	}
}
