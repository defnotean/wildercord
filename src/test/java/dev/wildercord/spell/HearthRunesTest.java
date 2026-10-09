package dev.wildercord.spell;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The hearth pack (HearthLinkRules): every rune compiles on a host it fits, and is refused clearly where it doesn't. */
class HearthRunesTest {
	private static final List<RuneDef> MODIFIERS = List.of(TIDY, REPLANTING, KILNED, SILKEN, WINDFALL, VEINFOLLOW, TIMBERING, LEVEL_GROUND, STEADY,
		DAMP, MAGNETIC, SOWING, FURROWING, FERTILE, TORCHSET, ORE_SENSING, FETCHING, BOUNTIFUL, CULLING, HEADHUNTING, HALLOWED, TAPERING, POOLED,
		SUNLIT, GENTLE, SPARING, SOOTHING, CUSHIONED, MENDING_MOD, NOURISHING, PURIFYING, MATCHMAKING, FLEECING, INWARD, SELFLESS, TRIAGE);
	private static final List<RuneDef> CONDITIONS = List.of(IF_NIGHT, IF_DAY, IF_RAINING, IF_UNDERGROUND, IF_ALONE, IF_NEAR_ALLY, IF_UNHURT,
		IF_HOLDING_TOOL, IF_BRIMMING, IF_IN_FIELDS);
	private static final List<RuneDef> WATCHERS = List.of(ON_MINE, ON_HARVEST, ON_CATCH, ON_SPRINT, ON_SPLASH, ON_MOUNT, ON_WAKE);

	/** A host each kind of hearth modifier fits, and one it doesn't. */
	private static final Map<String, RuneDef> HOST = Map.of(HearthLinkRules.WORLD, BREAK, HearthLinkRules.HARMS, HARM, HearthLinkRules.HELPS, HEAL,
		HearthLinkRules.MOVES, GRAPPLE, HearthLinkRules.BURNS, FIRE, Trait.FRUGAL, HARM);
	private static final Map<String, RuneDef> WRONG = Map.of(HearthLinkRules.WORLD, HEAL, HearthLinkRules.HARMS, HEAL, HearthLinkRules.HELPS, HARM,
		HearthLinkRules.MOVES, HEAL, HearthLinkRules.BURNS, HARM);

	private static SpellCompiler.Compiled compile(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes));
	}

	@Test
	void theRosterIsWhole() {
		assertEquals(36, MODIFIERS.size());
		assertEquals(17, CONDITIONS.size() + WATCHERS.size());
		assertEquals(36, HearthLinkRules.MODIFIERS.size());
		for (RuneDef mod : MODIFIERS) {
			assertEquals(RuneFamily.MODIFIER, mod.family(), mod.name());
			assertTrue(HearthLinkRules.ownsModifier(mod), mod.name());
			assertEquals(1, ModifierLimits.maximum(mod), mod.name());
			assertTrue(RuneCategories.of(RuneFamily.MODIFIER).contains(mod.category()), mod.name() + " " + mod.category());
		}
		for (RuneDef link : CONDITIONS) {
			assertEquals("condition", link.category(), link.name());
			assertTrue(HearthLinkRules.ownsLink(link) && HearthLinkRules.keepsShape(link), link.name());
		}
		for (RuneDef link : WATCHERS) {
			assertEquals("trigger", link.category(), link.name());
			assertTrue(HearthLinkRules.ownsLink(link) && HearthLinkRules.isWatcher(link.id()) && !HearthLinkRules.keepsShape(link), link.name());
			assertTrue(HearthLinkRules.watchTicks(link.path()) > 0, link.name());
		}
		// At least ten of the seventeen links work away from a fight: every condition and watcher but the
		// health and company checks.
		long everyday = java.util.stream.Stream.concat(CONDITIONS.stream(), WATCHERS.stream())
			.filter(l -> !List.of(IF_UNHURT, IF_ALONE, IF_NEAR_ALLY).contains(l)).count();
		assertTrue(everyday >= 10);
	}

	@Test
	void everyModifierCompilesOnAHostItFits() {
		for (RuneDef mod : MODIFIERS) {
			RuneDef host = HOST.get(mod.needs());
			assertNotNull(host, mod.name());
			SpellCompiler.Compiled c = compile(BOLT, host, mod);
			assertTrue(c.warnings().isEmpty(), mod.name() + ": " + c.warnings());
			assertEquals(1, c.attachedTo()[2], mod.name());
			SpellPlan.EffectNode node = c.root().groups.getFirst().effects.getFirst();
			assertEquals(1, node.count(mod), mod.name());
			assertTrue(c.cost() > 0, mod.name());
			// The readout says what it does.
			String line = c.lines().getFirst();
			assertTrue(line.contains(HearthLinkRules.phrase(mod.path())), mod.name() + ": " + line);
		}
	}

	@Test
	void aModifierWithNothingToChangeIsRefusedClearlyAndStaysLoose() {
		for (RuneDef mod : MODIFIERS) {
			RuneDef wrong = WRONG.get(mod.needs());
			if (wrong == null) {
				// Needs any effect: refused only with no effect at all on its left.
				SpellCompiler.Compiled bare = compile(BOLT, mod);
				assertTrue(bare.warnings().contains(mod.name() + " does nothing here: it needs an effect on its left."), bare.warnings().toString());
				assertEquals(SpellCompiler.UNATTACHED, bare.attachedTo()[1], mod.name());
				continue;
			}
			SpellCompiler.Compiled c = compile(BOLT, wrong, mod);
			String expected = mod.name() + " does nothing here: it needs " + HearthLinkRules.needsPhrase(mod.needs()) + " on its left.";
			assertTrue(c.warnings().contains(expected), mod.name() + ": " + c.warnings());
			assertEquals(SpellCompiler.UNATTACHED, c.attachedTo()[2], mod.name());
			assertEquals(0, c.root().groups.getFirst().effects.getFirst().count(mod), mod.name());
		}
	}

	@Test
	void eachModifierCountsOncePerEffect() {
		SpellCompiler.Compiled c = compile(TOUCH, BREAK, TIDY, TIDY);
		assertEquals(SpellCompiler.UNATTACHED, c.attachedTo()[3]);
		assertTrue(c.warnings().contains("Tidy accepts at most 1 per target rune; extra copies are ignored."), c.warnings().toString());
	}

	@Test
	void clashingModifiersAreRefused() {
		SpellCompiler.Compiled drops = compile(TOUCH, BREAK, SILKEN, KILNED);
		assertTrue(drops.warnings().contains("Kilned and Silken both decide what a block drops: only one per effect."), drops.warnings().toString());
		assertEquals(SpellCompiler.UNATTACHED, drops.attachedTo()[3]);
		SpellCompiler.Compiled ways = compile(BURST, HEAL, INWARD, SELFLESS);
		assertTrue(ways.warnings().contains("Selfless and Inward pull opposite ways: only one per effect."), ways.warnings().toString());
		SpellCompiler.Compiled still = compile(TOUCH, BREAK, STEADY, TIDY);
		assertTrue(still.warnings().contains("Steady stops the effect changing any block, so Tidy would have nothing to work on."),
			still.warnings().toString());
		SpellCompiler.Compiled still2 = compile(TOUCH, BREAK, VEINFOLLOW, STEADY);
		assertTrue(still2.warnings().contains("Steady stops the effect changing any block, so Veinfollow would have nothing to work on."),
			still2.warnings().toString());
		// Different rules on one effect are fine.
		SpellCompiler.Compiled fine = compile(TOUCH, BREAK, SILKEN, TIDY, VEINFOLLOW);
		assertTrue(fine.warnings().isEmpty(), fine.warnings().toString());
	}

	@Test
	void conditionsKeepTheShapeAndHaveTheirOwnHeader() {
		for (RuneDef link : CONDITIONS) {
			SpellCompiler.Compiled c = compile(BOLT, HARM, link, SHOCK);
			assertTrue(c.warnings().isEmpty(), link.name() + ": " + c.warnings());
			assertSame(link, c.root().link.link, link.name());
			assertEquals(SELF, c.root().link.next.groups.getFirst().shape, link.name());
			assertTrue(c.lines().contains(HearthLinkRules.header(link.id())), link.name() + ": " + c.lines());
			// Priced like the other conditions (If Wet): the same spell with If Wet in its place costs the same, less the links' own costs.
			assertEquals(compile(BOLT, HARM, IF_WET, SHOCK).cost() - IF_WET.cost() + link.cost(), c.cost(), 1e-6, link.name());
		}
		assertEquals("If it's night:", compile(IF_NIGHT, SELF, SWIFT).lines().getFirst());
	}

	@Test
	void watchersFireTheRestFromWhereItHappened() {
		for (RuneDef link : WATCHERS) {
			SpellCompiler.Compiled c = compile(link, BURST, GROW);
			assertTrue(c.warnings().isEmpty(), link.name() + ": " + c.warnings());
			assertSame(link, c.root().link.link, link.name());
			assertEquals(TRIGGER, c.root().link.next.implicitShape, link.name());
			assertEquals(HearthLinkRules.header(link.id()), c.lines().getFirst(), link.name());
		}
		assertEquals("At the next block you mine (30s):", HearthLinkRules.header(ON_MINE.id()));
		assertEquals("When you next wake from a bed (10 minutes):", HearthLinkRules.header(ON_WAKE.id()));
	}

	@Test
	void theirNumbers() {
		assertEquals(1.5, HearthLinkRules.taper(0), 1e-9);
		assertEquals(1.125, HearthLinkRules.taper(1), 1e-9);
		assertEquals(0.84375, HearthLinkRules.taper(2), 1e-9);
		assertEquals(2.0, HearthLinkRules.pooled(1), 1e-9);
		assertEquals(0.5, HearthLinkRules.pooled(4), 1e-9);
		assertEquals(0.0, HearthLinkRules.pooled(0), 1e-9);
		assertEquals(2.0, HearthLinkRules.sunlit(true), 1e-9);
		assertEquals(0.5, HearthLinkRules.sunlit(false), 1e-9);
		assertEquals(0, HearthLinkRules.mended(4));
		assertEquals(15, HearthLinkRules.mended(25));
		assertTrue(HearthLinkRules.brimming(11, 20));
		assertFalse(HearthLinkRules.brimming(10, 20));
		assertFalse(HearthLinkRules.brimming(5, 0));
	}

	@Test
	void derivedTraitsAreOnlyForEffects() {
		assertTrue(BREAK.has(HearthLinkRules.WORLD));
		assertTrue(HARM.has(HearthLinkRules.HARMS));
		assertTrue(GRAPPLE.has(HearthLinkRules.HARMS) && GRAPPLE.has(HearthLinkRules.MOVES));
		assertTrue(HEAL.has(HearthLinkRules.HELPS));
		assertTrue(FIRE.has(HearthLinkRules.BURNS));
		assertFalse(HEAL.has(HearthLinkRules.WORLD));
		assertFalse(BOLT.has(HearthLinkRules.HARMS));
		assertFalse(TIDY.has(HearthLinkRules.WORLD));
	}
}
