package dev.wildercord.api;

import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.Trait;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Registering add-on runes through the API: they join the roster, read like built-in runes, and their
 * modifiers' numbers reach the readout and the cast. (Behaviours need a world, so the game tests and
 * the example in docs/API.md cover those.)
 */
class ApiRegistrationTest {
	private static final WildercordApi API = WildercordApi.get();

	@AfterAll
	@SuppressWarnings("unchecked")
	static void forgetTheTestRunes() throws ReflectiveOperationException {
		// The roster is global: take the test runes out again so other tests see the built-in one.
		var field = Runes.class.getDeclaredField("ALL");
		field.setAccessible(true);
		((Map<String, RuneDef>) field.get(null)).keySet().removeIf(id -> id.startsWith("apitest:") || id.startsWith("example:"));
	}

	@Test
	void theExampleAddOnRegistersEverything() {
		new ExampleAddon().onWildercordInit(API);
		for (String id : List.of("example:drench", "example:brutal", "example:halo", "example:at_dusk")) {
			assertTrue(API.rune(id).isPresent(), id);
		}
		assertTrue(API.categories(RuneFamily.EFFECT).contains("weather"));
		RuneDef drench = API.rune("example:drench").orElseThrow();
		RuneDef brutal = API.rune("example:brutal").orElseThrow();
		// Brutal needs POWER: Drench has none, so it reaches past it to Harm.
		SpellCompiler.Compiled c = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM, drench, brutal));
		SpellPlan.EffectNode harm = c.root().groups.getFirst().effects.getFirst();
		assertEquals(1, harm.count(brutal));
		assertEquals(1.8, SpellNumbers.power(harm), 1e-9);
		SpellCompiler.Compiled halo = SpellCompiler.compile(List.of(API.rune("example:halo").orElseThrow(), Runes.HARM, API.rune("example:at_dusk").orElseThrow(), Runes.HEAL));
		assertTrue(halo.warnings().isEmpty(), halo.warnings().toString());
	}

	@Test
	void anEffectJoinsTheRosterAndReadsLikeABuiltInOne() {
		RuneDef frostbite = API.effect("apitest:frostbite", "Frostbite", "frost", EffectKind.HARMFUL)
			.tier(2).cost(7).traits(Trait.POWER, Trait.DURATION).description("4 frost damage.").register();
		assertSame(frostbite, Runes.get("apitest:frostbite").orElseThrow());
		assertTrue(frostbite.has(Trait.FRUGAL), "every effect gets Frugal");
		assertEquals("damage", frostbite.category());
		assertEquals("frost", frostbite.element());
		SpellCompiler.Compiled c = SpellCompiler.compile(List.of(Runes.BOLT, frostbite, Runes.AMPLIFY));
		SpellPlan.EffectNode node = c.root().groups.getFirst().effects.getFirst();
		assertSame(frostbite, node.effect);
		assertEquals(1, node.count(Runes.AMPLIFY), "Amplify attaches to an add-on effect with POWER");
		assertEquals(3 + 7 * 1.6 * 1.1, c.cost(), 1e-9);
	}

	@Test
	void aModifiersNumbersReachTheReadoutAndTheCast() {
		RuneDef mighty = API.modifier("apitest:mighty", "Mighty").tier(1).multiplier(1.25).needs(Trait.POWER).numbers(2.0, 1.0, 1.0).register();
		assertEquals(RuneFamily.MODIFIER, mighty.family());
		assertEquals(0.0, mighty.cost());
		SpellCompiler.Compiled c = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM, mighty));
		SpellPlan.EffectNode node = c.root().groups.getFirst().effects.getFirst();
		assertEquals(1, node.count(mighty));
		assertEquals(2.0, SpellNumbers.power(node), 1e-9);
		assertEquals((3 + 8 * 1.25 * 1.1), c.cost(), 1e-9);
	}

	@Test
	void aNewCategoryIsAddedToItsFamily() {
		RuneDef hex = API.effect("apitest:wither_hex", "Wither Hex", "void", EffectKind.HARMFUL).cost(5).category("hexes").register();
		assertTrue(API.categories(RuneFamily.EFFECT).contains("hexes"));
		assertEquals("hexes", hex.category());
		// Built-in categories keep their order, with the new one after them.
		assertEquals("damage", API.categories(RuneFamily.EFFECT).getFirst());
	}

	@Test
	void badRunesAreRefusedBeforeTheyReachTheRoster() {
		assertThrows(IllegalArgumentException.class, () -> API.effect("frostbite", "No namespace", "frost", EffectKind.HARMFUL).build());
		assertThrows(IllegalArgumentException.class, () -> API.effect("wildercord:sneaky", "Ours", "frost", EffectKind.HARMFUL).build());
		assertThrows(IllegalArgumentException.class, () -> API.effect("apitest:x", "Kindless", "frost", EffectKind.NONE).build());
		assertThrows(IllegalArgumentException.class, () -> API.shape("apitest:y", "Tier five").tier(5).build());
		assertThrows(IllegalArgumentException.class, () -> API.modifier("apitest:z", "Needs nothing").build());
		assertThrows(IllegalArgumentException.class, () -> API.link("apitest:Upper", "Bad id").build());
		assertTrue(Runes.get("apitest:x").isEmpty());
		API.shape("apitest:twice", "Twice").register();
		assertThrows(IllegalStateException.class, () -> API.shape("apitest:twice", "Twice").register());
	}

	@Test
	void shapesAndLinksGetTheirFamilyRules() {
		RuneDef ring = API.shape("apitest:halo", "Halo").tier(2).cost(5).multiplier(1.5).traits(Trait.RADIUS).register();
		assertTrue(ring.has(Trait.COOLDOWN), "every shape gets Cooldown");
		assertEquals("direct", ring.category());
		RuneDef later = API.link("apitest:at_dusk", "At Dusk").cost(2).register();
		SpellCompiler.Compiled c = SpellCompiler.compile(List.of(ring, Runes.HARM, later, Runes.HEAL));
		assertSame(later, c.root().link.link);
		assertSame(Runes.TRIGGER, c.root().link.next.groups.getFirst().shape, "the rest lands on whatever set it off");
		assertEquals("At Dusk:", c.lines().get(1));
	}
}
