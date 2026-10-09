package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

class ReweaveGrammarTest {
    @Test void exactlyOneRegisteredShapeAndHarmWithOnePriceAndFixedRest() {
        List<RuneDef> spell = List.of(REWEAVE, HARM);
        assertTrue(ReweaveRules.valid(spell));
        assertNull(ReweaveRules.problem(spell));
        var compiled = SpellCompiler.compile(spell, id -> 3);
        assertFalse(compiled.isEmpty());
        assertEquals(1, compiled.root().groups.size());
        assertNull(compiled.root().link);
        var group = compiled.root().groups.getFirst();
        assertSame(REWEAVE, group.shape);
        assertFalse(group.implicit);
        assertEquals(1, group.effects.size());
        assertSame(HARM, group.effects.getFirst().effect);
        assertEquals(3, group.effects.getFirst().rank);
        assertEquals(ReweaveRules.BASE_MANA + HARM.cost(), compiled.cost(), 1e-9);
        assertEquals(ReweaveRules.REST_TICKS, compiled.cooldownTicks());
        assertEquals(0, compiled.healthCost());
        assertTrue(compiled.warnings().isEmpty());
        String readout = String.join(" ", compiled.lines());
        assertTrue(readout.contains("paid once") && readout.contains("50%") && readout.contains("shared rest") && readout.contains("warning"), readout);
        assertEquals("Arcane Reweave", SpellNames.auto(spell));
    }

    @Test void everyAdditionalRuneAndEveryOtherPayloadRefusesTheWholeSpell() {
        for (RuneDef addition : Runes.all()) {
            for (int at = 0; at <= 2; at++) {
                List<RuneDef> spell = new ArrayList<>(List.of(REWEAVE, HARM));
                spell.add(at, addition);
                assertRefused(spell);
            }
            if (addition != HARM) assertRefused(List.of(REWEAVE, addition));
        }
        assertRefused(List.of(REWEAVE));
        assertRefused(List.of(HARM, REWEAVE));
        assertRefused(List.of(SELF, HEAL, REWEAVE, HARM));
        assertRefused(List.of(REWEAVE, HARM, BOLT, FIRE));
        assertRefused(List.of(BOLT, HARM, ON_HIT, REWEAVE, HARM));
        assertRefused(List.of(REWEAVE, HARM, ECHO, SELF, HEAL));
        assertRefused(List.of(REWEAVE, WovenRunes.bind(FROST, SHOCK)));
        RuneDef changed = new RuneDef(REWEAVE.id(), REWEAVE.name(), REWEAVE.family(), REWEAVE.tier(),
            0, REWEAVE.multiplier(), REWEAVE.element(), REWEAVE.kind(), REWEAVE.traits(), REWEAVE.needs(), REWEAVE.description(), REWEAVE.category());
        assertRefused(List.of(changed, HARM));
        assertEquals("Unfinished Reweave", SpellNames.auto(List.of(REWEAVE, FROST)));
        assertFalse(SpellCompiler.compile(List.of(BOLT, HARM, ON_HIT, FROST)).isEmpty());
    }

    @Test void storedKnotWeaveAndPassiveRoutesCannotCarryReweave() {
        assertTrue(SpellCompiler.compileStored(ReweaveRules.RUNES).isEmpty());
        assertEquals(List.of(ReweaveRules.STORAGE_PROBLEM), SpellCompiler.compileStored(ReweaveRules.RUNES).warnings());
        assertTrue(SpellCompiler.stored(List.of(SELF, IMBUE, REWEAVE, HARM)).isEmpty());
        assertNotNull(Knots.problem(ReweaveRules.RUNES));
        assertFalse(Passives.allowed(REWEAVE));
        assertNotNull(Passives.problem(ReweaveRules.RUNES));
        assertThrows(IllegalArgumentException.class, () -> WovenRunes.bind(REWEAVE, HARM));
        String id = Knots.id(ReweaveRules.RUNES, "A forbidden shortcut");
        assertTrue(Knots.def(id).isEmpty());
        assertRefused(List.of(wrapper(id, RuneFamily.KNOT)));
        assertFalse(SpellCompiler.compileStored(List.of(HARM)).isEmpty());
        assertFalse(SpellCompiler.compile(List.of(RELAY, HARM)).isEmpty());
    }

    @Test void unknownSiblingsOverdeepKnotsAndForgedWeavesDoNotHideTheShape() {
        String inner = knot("missing:unloaded,reweave,harm");
        assertTrue(Runes.get(inner).isEmpty());
        for (int depth = 0; depth < 5; depth++) {
            assertTrue(ReweaveRules.containsIds(List.of(inner)));
            assertRefused(List.of(SELF, HEAL, wrapper(inner, RuneFamily.KNOT)));
            inner = knot("self," + inner);
        }
        String woven = WovenRunes.PREFIX + Knots.encode((HARM.id() + "\n" + REWEAVE.id()).getBytes(StandardCharsets.UTF_8));
        assertTrue(Runes.get(woven).isEmpty());
        assertTrue(ReweaveRules.containsIds(List.of(woven)));
        assertRefused(List.of(SELF, HEAL, wrapper(woven, RuneFamily.EFFECT)));
        assertTrue(ReweaveRules.containsIds(List.of(Knots.PREFIX + "!")));
        assertTrue(ReweaveRules.containsIds(List.of(Knots.PREFIX + "a".repeat(Knots.MAX_ID_LENGTH))));
        assertFalse(ReweaveRules.containsIds(List.of("missing:reweave", "wildercord:reweave_extra")));
        assertFalse(ReweaveRules.containsIds(List.of(knot("missing:unloaded,self,heal"))));
        String harmless = Knots.id(List.of(SELF, HEAL), "wildercord:reweave");
        assertFalse(ReweaveRules.containsIds(List.of(harmless)), "a custom name is not a rune");
        assertTrue(Runes.get(harmless).isPresent());
    }

    @Test void rowLimitsPreserveDraftsAndCannotStripTheRestrictedRuneOrForbiddenSuffix() {
        List<String> draft = List.of(REWEAVE.id(), HARM.id(), AMPLIFY.id());
        assertEquals(draft, ReweaveRules.boundedIds(draft, 12), "invalid editable rows remain intact within the limit");
        for (List<String> raw : List.of(draft,
            List.of(SELF.id(), HEAL.id(), REWEAVE.id()),
            List.of(SELF.id(), HEAL.id(), knot("missing:unloaded,reweave,harm")))) {
            var bounded = ReweaveRules.boundedIds(raw, 2);
            assertTrue(ReweaveRules.containsIds(bounded));
            assertTrue(SpellCompiler.compile(bounded.stream().map(id -> Runes.get(id).orElseThrow()).toList()).isEmpty());
        }
        assertEquals(ReweaveRules.IDS, ReweaveRules.boundedIds(ReweaveRules.IDS, 2));
        assertEquals(List.of(SELF.id(), HEAL.id()), ReweaveRules.boundedIds(List.of(SELF.id(), HEAL.id(), AMPLIFY.id()), 2));
        assertEquals(List.of(RELAY.id()), ReweaveRules.boundedIds(List.of(RELAY.id(), HARM.id(), AMPLIFY.id()), 2));
        assertEquals(List.of(), ReweaveRules.boundedIds(ReweaveRules.IDS, 0));
        assertThrows(IllegalArgumentException.class, () -> ReweaveRules.boundedIds(ReweaveRules.IDS, -1));
    }

    @Test void spellCodesPreserveInvalidRowsAndNeverConvertTruncationIntoHarm() {
        assertEquals(ReweaveRules.IDS, SpellCodes.decode("wc:reweave.harm"));
        assertEquals(List.of(REWEAVE.id(), "missing:unloaded", HARM.id()), SpellCodes.decode("wc:reweave.missing~unloaded.harm"));
        for (String code : List.of("wc:" + "harm.".repeat(12) + "reweave", "wc:reweave.harm." + "amplify.".repeat(12))) {
            assertEquals(List.of(REWEAVE.id()), SpellCodes.decode(SpellCodes.find("Try " + code + " now")));
        }
        String composite = knot("reweave,harm");
        String code = SpellCodes.encode(List.of(SELF.id(), HEAL.id(), composite));
        assertEquals(code, SpellCodes.find("Try " + code + " !"));
        assertTrue(ReweaveRules.containsIds(SpellCodes.decode(code)));
        String oversized = "wc:" + "harm.".repeat(60_000) + "reweave";
        var decoded = SpellCodes.decode(oversized);
        assertTrue(SpellCompiler.compile(decoded.stream().map(id -> Runes.get(id).orElseThrow()).toList()).isEmpty());
    }

    private static String knot(String raw) {
        return Knots.PREFIX + Knots.encode(raw.getBytes(StandardCharsets.UTF_8));
    }
    private static RuneDef wrapper(String id, RuneFamily family) {
        return new RuneDef(id, "Forged wrapper", family, 4, 0, 1, "", EffectKind.NONE, Set.of(), "", "", "knot");
    }
    private static void assertRefused(List<RuneDef> spell) {
        assertFalse(ReweaveRules.valid(spell));
        var compiled = SpellCompiler.compile(spell);
        assertTrue(compiled.isEmpty(), spell.toString());
        assertTrue(compiled.root().groups.isEmpty(), "no surviving earlier group or implicit Self");
        assertNull(compiled.root().link);
        assertEquals(0, compiled.cost());
        assertEquals(0, compiled.healthCost());
        assertEquals(List.of(RelayRules.contains(spell) ? RelayRules.GRAMMAR_PROBLEM : ReweaveRules.GRAMMAR_PROBLEM), compiled.warnings());
    }
}
