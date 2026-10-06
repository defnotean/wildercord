package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

class RelayRulesTest {
	@Test void spellCodesCannotLoseRelayThroughTheirDisplayLimit() {
		String code = "wc:" + "harm.".repeat(12) + "relay";
		assertEquals(List.of(RelayRules.ID), SpellCodes.decode(SpellCodes.find("Try " + code + " now")));
		assertEquals("Unfinished Relay", SpellNames.auto(List.of(Runes.RELAY, Runes.HEAL)));
		assertEquals("Arcane Relay", SpellNames.auto(List.of(Runes.RELAY, Runes.HARM)));
		String composite = Knots.PREFIX + Knots.encode("relay,harm".getBytes(StandardCharsets.UTF_8));
		String hidden = SpellCodes.encode(List.of(SELF.id(), HEAL.id(), composite));
		assertEquals(hidden, SpellCodes.find("Try " + hidden + " !"));
		assertTrue(RelayRules.containsIds(SpellCodes.decode(SpellCodes.find(hidden))));
		var matcher = SpellCodes.PATTERN.matcher("Try " + hidden + " !");
		assertTrue(matcher.find()); assertEquals(hidden, matcher.group(), "Chat copy and direct paste see the same complete composite");
	}

	@Test
	void threePayloadsShareOneShapeWithOneUpFrontPriceAndAnUnmodifiedRest() {
		for (RuneDef effect : List.of(HARM, FROST, SHOCK)) {
			List<RuneDef> spell = List.of(RELAY, effect);
			var compiled = SpellCompiler.compile(spell, id -> 3);
			assertTrue(RelayRules.valid(spell));
			assertNull(RelayRules.problem(spell));
			assertFalse(compiled.isEmpty());
			assertEquals(1, compiled.root().groups.size());
			assertNull(compiled.root().link);
			var group = compiled.root().groups.getFirst();
			assertSame(RELAY, group.shape);
			assertFalse(group.implicit);
			assertEquals(1, group.effects.size());
			assertSame(effect, group.effects.getFirst().effect);
			assertEquals(3, group.effects.getFirst().rank, "normal effect ranks still apply");
			assertEquals(24 + effect.cost(), compiled.cost(), 1e-9, "a shape's power reduction never discounts its effect price");
			assertEquals(160, compiled.cooldownTicks(), "the fixed rest does not scale with selected payload cost");
			assertEquals(0.9, SpellNumbers.groupPower(group), 1e-9);
			assertEquals(0, compiled.healthCost());
			assertTrue(compiled.warnings().isEmpty());
			String readout = String.join(" ", compiled.lines());
			assertTrue(readout.contains("paid once") && readout.contains("90%") && readout.contains("shared rest"), readout);
		}
	}

	@Test
	void anythingAddedToEitherSideRefusesTheWholeSpell() {
		for (RuneDef addition : Runes.all()) {
			for (int at = 0; at <= 2; at++) {
				List<RuneDef> spell = new ArrayList<>(List.of(RELAY, HARM));
				spell.add(at, addition);
				assertRefused(spell);
			}
		}
		assertRefused(List.of(RELAY));
		assertRefused(List.of(HARM, RELAY));
		for (RuneDef effect : Runes.all()) {
			if (effect != HARM && effect != FROST && effect != SHOCK) assertRefused(List.of(RELAY, effect));
		}
	}

	@Test
	void aValidEarlierOrLaterGroupDoesNotSurviveAForbiddenRelay() {
		assertRefused(List.of(SELF, HEAL, RELAY, HARM));
		assertRefused(List.of(RELAY, HARM, BOLT, FIRE));
		assertRefused(List.of(BOLT, HARM, ON_HIT, RELAY, FROST));
		assertRefused(List.of(RELAY, HARM, ECHO, SELF, HEAL));
		assertRefused(List.of(RELAY, WovenRunes.bind(FROST, SHOCK)));
		assertFalse(SpellCompiler.compile(List.of(BOLT, HARM, ON_HIT, FROST)).isEmpty(), "ordinary links remain usable");
	}

	@Test
	void storedKnotAndPassiveRoutesCannotCarryTheLesson() {
		List<RuneDef> spell = List.of(RELAY, HARM);
		assertTrue(SpellCompiler.compileStored(spell).isEmpty());
		assertTrue(SpellCompiler.compileStored(spell).warnings().contains(RelayRules.STORAGE_PROBLEM));
		assertTrue(SpellCompiler.stored(List.of(SELF, IMBUE, RELAY, HARM)).isEmpty());
		assertNotNull(Knots.problem(spell));
		assertFalse(Passives.allowed(RELAY));
		assertNotNull(Passives.problem(spell));
		assertThrows(IllegalArgumentException.class, () -> WovenRunes.bind(RELAY, HARM));
		String knot = Knots.id(spell, "A forbidden shortcut");
		assertTrue(Knots.def(knot).isEmpty());
		assertRefused(List.of(wrapper(knot, RuneFamily.KNOT)));
		assertFalse(SpellCompiler.compileStored(List.of(HARM)).isEmpty(), "ordinary imbued effects still work");
	}

	@Test
	void originalIdsDetectUnknownSiblingsAndOverdeepKnotsBeforeResolution() {
		String inner = knot("missing:unloaded,relay,harm");
		assertTrue(Runes.get(inner).isEmpty(), "the old resolver cannot expose this knot's contents");
		assertTrue(RelayRules.containsIds(List.of(inner)));
		for (int depth = 0; depth < 5; depth++) {
			inner = knot("self," + inner);
			assertTrue(RelayRules.containsIds(List.of(inner)), "nesting must not hide a restricted rune");
			assertRefused(List.of(SELF, HEAL, wrapper(inner, RuneFamily.KNOT)));
		}
		String forgedWeave = WovenRunes.PREFIX + Knots.encode((HARM.id() + "\n" + RELAY.id()).getBytes(StandardCharsets.UTF_8));
		assertTrue(Runes.get(forgedWeave).isEmpty());
		assertTrue(RelayRules.containsIds(List.of(forgedWeave)));
		assertRefused(List.of(SELF, wrapper(forgedWeave, RuneFamily.EFFECT)));
	}

	@Test
	void uninspectableCompositesFailClosedWithoutConfusingNamesForRuneIds() {
		assertTrue(RelayRules.containsIds(List.of(Knots.PREFIX + "!")));
		assertTrue(RelayRules.containsIds(List.of(Knots.PREFIX + "a".repeat(Knots.MAX_ID_LENGTH))));
		assertFalse(RelayRules.containsIds(List.of("missing:relay", "wildercord:relay_extra")));
		String safe = Knots.id(List.of(SELF, HEAL), "wildercord:relay");
		assertFalse(RelayRules.containsIds(List.of(safe)), "a custom name is not spell content");
		assertTrue(Runes.get(safe).isPresent());
		assertFalse(RelayRules.containsIds(List.of(knot("missing:unloaded,self,heal"))), "unknown siblings alone do not imply Relay");
	}

	@Test
	void truncationCannotTurnAForgedRowIntoAnotherSpell() {
		for (List<String> raw : List.of(
			List.of(SELF.id(), HEAL.id(), RELAY.id()),
			List.of(RELAY.id(), HARM.id(), AMPLIFY.id()),
			List.of(SELF.id(), HEAL.id(), knot("missing:unloaded,relay,harm")))) {
			List<String> bounded = RelayRules.boundedIds(raw, 2);
			assertTrue(RelayRules.containsIds(bounded));
			assertTrue(SpellCompiler.compile(bounded.stream().map(id -> Runes.get(id).orElseThrow()).toList()).isEmpty(),
				"both a hidden shape and forbidden trailing modifiers must refuse the entire saved row");
		}
		assertEquals(List.of(RELAY.id(), HARM.id()), RelayRules.boundedIds(List.of(RELAY.id(), HARM.id()), 2));
		assertEquals(List.of(SELF.id(), HEAL.id()), RelayRules.boundedIds(List.of(SELF.id(), HEAL.id(), AMPLIFY.id()), 2));
	}

	@Test
	void lessonSourceNeverEntersCommonLootOrCrafting() throws Exception {
		assertTrue(RuneSources.lessonOnly(RELAY));
		assertTrue(RuneSources.foundOnly(RELAY));
		assertFalse(Runes.common(RELAY));
		assertEquals(List.of(RuneSources.RELAY_LESSON), RuneSources.sourcesOf(RELAY));
		assertFalse(RuneSources.forSource("archive").contains(RELAY));
		Path data = Path.of("src/main/resources/data/wildercord");
		assertFalse(Files.exists(data.resolve("recipe/rune_relay.json")));
		try (var tables = Files.walk(data.resolve("loot_table"))) {
			for (Path table : tables.filter(Files::isRegularFile).toList()) {
				assertFalse(Files.readString(table).contains("wildercord:relay"), table.toString());
			}
		}
	}

	private static String knot(String raw) {
		return Knots.PREFIX + Knots.encode(raw.getBytes(StandardCharsets.UTF_8));
	}

	private static RuneDef wrapper(String id, RuneFamily family) {
		return new RuneDef(id, "Forged wrapper", family, 4, 0, 1, "", EffectKind.NONE, Set.of(), "", "", "knot");
	}

	private static void assertRefused(List<RuneDef> spell) {
		assertFalse(RelayRules.valid(spell));
		var compiled = SpellCompiler.compile(spell);
		assertTrue(compiled.isEmpty(), spell.toString());
		assertTrue(compiled.root().groups.isEmpty(), "whole spell refused, no implicit Self or surviving earlier group");
		assertNull(compiled.root().link);
		assertEquals(0, compiled.cost());
		assertEquals(0, compiled.healthCost());
		assertEquals(List.of(RelayRules.GRAMMAR_PROBLEM), compiled.warnings());
	}
}
