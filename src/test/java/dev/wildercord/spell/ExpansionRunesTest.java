package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/** Batch 6: sparks, energy balls and beams, protection, mining and a simple spell for every element. */
class ExpansionRunesTest {
	private static final List<RuneDef> BATCH_6 = List.of(SPARK, RAY, NOVA, WISP, COMET, RICOCHET, CLUSTER, LANCE, SWEEP, PRISM, STREAM,
		BARRIER, BRACE, ANCHOR, BRAMBLE, FROSTWARD, CUSHION, DEFLECT, HAVEN, CHISEL, GLIMMER, PRUNE, TUNNEL, VEIN, SMELT, FELL, SPAN,
		EMBER, ICICLE, PELT, WINDCUT, LEECH, HEX, REND, COUNTDOWN, JOLT, BLEED, COLDSNAP, FLASHFIRE, BANISH, CYCLONE);

	private static SpellCompiler.Compiled compile(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes));
	}

	private static SpellPlan.Group group(RuneDef... runes) {
		return compile(runes).root().groups.getFirst();
	}

	@Test
	void theBatchIsEarlyAndMidGame() {
		assertEquals(41, BATCH_6.size());
		assertEquals(BATCH_6.size(), BATCH_6.stream().distinct().count());
		for (RuneDef rune : BATCH_6) {
			assertTrue(rune.tier() == 1 || rune.tier() == 2, rune.name());
			assertTrue(Runes.get(rune.id()).isPresent(), rune.name());
			assertFalse(Runes.innate(rune), rune.name());
		}
	}

	@Test
	void newRunesAreFiledWhereTheyBelong() {
		for (RuneDef shape : List.of(RAY, LANCE, SWEEP, PRISM, STREAM)) {
			assertEquals("direct", shape.category(), shape.name());
		}
		for (RuneDef shape : List.of(SPARK, WISP, COMET, RICOCHET, CLUSTER)) {
			assertEquals("projectile", shape.category(), shape.name());
		}
		assertEquals("area", NOVA.category());
		for (RuneDef ward : List.of(BARRIER, BRACE, ANCHOR, BRAMBLE, FROSTWARD, CUSHION, DEFLECT, HAVEN)) {
			assertEquals("support", ward.category(), ward.name());
			assertEquals(EffectKind.HELPFUL, ward.kind(), ward.name());
		}
		for (RuneDef world : List.of(CHISEL, GLIMMER, PRUNE, TUNNEL, VEIN, SMELT, FELL, SPAN)) {
			assertEquals("world", world.category(), world.name());
			assertEquals(EffectKind.WORLD, world.kind(), world.name());
		}
		for (RuneDef control : List.of(HEX, REND, JOLT, BANISH, CYCLONE)) {
			assertEquals("control", control.category(), control.name());
		}
		assertEquals("damage", EMBER.category());
	}

	@Test
	void aSparkIsCheapButSoft() {
		SpellCompiler.Compiled c = compile(SPARK, HARM);
		// About as much damage per mana as a Bolt, but cheaper to cast and quicker to recover.
		assertEquals(1 + 8 * 1.0, c.cost(), 1e-9);
		assertEquals(0.75, SpellNumbers.groupPower(c.root().groups.getFirst()), 1e-9);
		assertEquals("A spark (75% power): Harm", c.lines().getFirst());
		assertEquals("3 sparks x3 (75% power): Harm", compile(SPARK, SPLIT_MOD, VOLLEY_MOD, HARM).lines().getFirst());
		assertEquals(4.8, SpellNumbers.sparkSpeed(group(SPARK, QUICKEN, HARM)), 1e-9);
	}

	@Test
	void aStreamStrikesOftenAtAThirdOfThePower() {
		SpellPlan.Group g = group(STREAM, HARM);
		assertEquals(0.35, SpellNumbers.groupPower(g), 1e-9);
		assertEquals(6, SpellNumbers.streamStrikes(g));
		assertEquals(12, SpellNumbers.streamStrikes(group(STREAM, QUICKEN, HARM)));
		assertEquals(12, SpellNumbers.streamStrikes(group(STREAM, QUICKEN, QUICKEN, HARM)));
		assertEquals("A stream of 6 strikes (35% power each): Harm", compile(STREAM, HARM).lines().getFirst());
	}

	@Test
	void aRayIsAShortBeamThatPiercesAndChains() {
		SpellCompiler.Compiled c = compile(RAY, PIERCE_MOD, HARM);
		assertEquals(0, c.attachedTo()[1]);
		assertEquals("A ray (10 blocks, pierces 3): Harm", c.lines().getFirst());
		assertEquals("A ray (10 blocks): Shock", compile(RAY, SHOCK).lines().getFirst());
		assertEquals(SpellCompiler.UNATTACHED, compile(RAY, SPLIT_MOD, HARM).attachedTo()[1]);
	}

	@Test
	void shapesGrowWithTheirModifiers() {
		assertEquals(2.5, SpellNumbers.novaRadius(group(NOVA, HARM)), 1e-9);
		assertEquals(3.75, SpellNumbers.novaRadius(group(NOVA, WIDEN, HARM)), 1e-9);
		assertEquals(3.0, SpellNumbers.cometRadius(group(COMET, HARM)), 1e-9);
		assertEquals(4.5, SpellNumbers.cometRadius(group(COMET, WIDEN, HARM)), 1e-9);
		assertEquals(1.5, SpellNumbers.clusterRadius(group(CLUSTER, HARM)), 1e-9);
		assertEquals(0.7, SpellNumbers.lanceWidth(group(LANCE, HARM)), 1e-9);
		assertEquals(15.0, SpellNumbers.sweepLength(group(SWEEP, WIDEN, HARM)), 1e-9);
		assertEquals(10, SpellNumbers.sweepTicks(group(SWEEP, HARM)));
		assertEquals(5, SpellNumbers.sweepTicks(group(SWEEP, QUICKEN, HARM)));
		assertEquals(4, SpellNumbers.ricochetBounces(group(RICOCHET, HARM)));
		assertEquals(7, SpellNumbers.ricochetBounces(group(RICOCHET, BOUNCE_MOD, HARM)));
		assertEquals("A ricocheting orb (7 bounces): Harm", compile(RICOCHET, BOUNCE_MOD, HARM).lines().getFirst());
		assertEquals("A comet (bursts 3 blocks): Harm", compile(COMET, HARM).lines().getFirst());
		assertEquals("A cluster (5 shards, 1.5 blocks): Harm", compile(CLUSTER, HARM).lines().getFirst());
		assertEquals("A nova (2.5 blocks): Harm", compile(NOVA, HARM).lines().getFirst());
		assertEquals("A 10-block sweeping beam: Harm", compile(SWEEP, HARM).lines().getFirst());
		assertEquals("3 prism beams (splits in 3): Harm", compile(PRISM, SPLIT_MOD, HARM).lines().getFirst());
	}

	@Test
	void modifiersFindTheNewRunes() {
		// Widen skips an effect it can't change to reach the shape; Amplify lifts a mining rune to the next pickaxe.
		SpellCompiler.Compiled wide = compile(NOVA, HARM, WIDEN);
		assertEquals(0, wide.attachedTo()[2]);
		SpellCompiler.Compiled haven = compile(SELF, HAVEN, WIDEN);
		assertEquals(1, haven.attachedTo()[2]);
		assertEquals(1, compile(RAY, CHISEL, AMPLIFY).attachedTo()[2]);
		assertEquals(SpellCompiler.UNATTACHED, compile(RAY, FELL, AMPLIFY).attachedTo()[2]);
		assertEquals(1, compile(SELF, SPAN, EXTEND).attachedTo()[2]);
	}

	@Test
	void wardsThatCanBeSustainedAreThoseThatDontHeal() {
		for (RuneDef ok : List.of(ANCHOR, FROSTWARD, CUSHION)) {
			assertTrue(Passives.allowed(ok), ok.name());
		}
		for (RuneDef no : List.of(BARRIER, BRACE, BRAMBLE, DEFLECT, HAVEN, SPARK, COMET, LEECH, SPAN, TUNNEL)) {
			assertFalse(Passives.allowed(no), no.name());
		}
		assertNull(Passives.problem(List.of(ORBIT, EMBER)));
		assertNull(Passives.problem(List.of(ANCHOR, CUSHION)));
		assertTrue(Passives.problem(List.of(SELF, WINDCUT)).contains("needs an Orbit"));
	}

	@Test
	void newSpellsGetReadableNames() {
		assertEquals("Ember Spark", SpellNames.auto(List.of(SPARK, EMBER)));
		assertEquals("Wide Frost Comet", SpellNames.auto(List.of(COMET, WIDEN, FROST)));
		assertEquals("Barrier & Brace", SpellNames.auto(List.of(SELF, BARRIER, BRACE)));
	}
}
