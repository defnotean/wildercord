package dev.wildercord.spell;

import dev.wildercord.content.Imbued;
import dev.wildercord.content.ScrollSpell;
import dev.wildercord.player.Spellbook;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.spell.Runes.*;

class ExciseRulesTest {
    @Test void exactNativeGrammarPricesTheWholeActionOnce() {
        var compiled = SpellCompiler.compile(ExciseRules.RUNES, id -> 3);
        assertFalse(compiled.isEmpty()); assertTrue(compiled.warnings().isEmpty());
        assertEquals(36, compiled.cost(), 1e-9); assertEquals(240, compiled.cooldownTicks());
        assertSame(BEAM, compiled.root().groups.getFirst().shape);
        assertSame(EXCISE, compiled.root().groups.getFirst().effects.getFirst().effect);
        assertNull(compiled.root().link); assertEquals(0, compiled.healthCost());
        assertTrue(String.join(" ", compiled.lines()).contains("paid once"));
        assertEquals(16, ExciseRules.COMMIT_TICKS); assertEquals(12, ExciseRules.RECOVERY_TICKS);
    }
    @Test void everyExtraRuneAndWrongPairRefusesTheWholeAction() {
        for (RuneDef extra : Runes.all()) {
            for (int index = 0; index < 3; index++) {
                var row = new ArrayList<>(ExciseRules.RUNES); row.add(index, extra); refused(row);
            }
            if (extra != BEAM) refused(List.of(extra, EXCISE));
            if (extra != EXCISE) refused(List.of(BEAM, EXCISE, extra));
        }
        refused(List.of(EXCISE)); refused(List.of(EXCISE, BEAM));
        RuneDef altered = new RuneDef(EXCISE.id(), EXCISE.name(), EXCISE.family(), EXCISE.tier(), 0,
            EXCISE.multiplier(), EXCISE.element(), EXCISE.kind(), EXCISE.traits(), EXCISE.needs(), EXCISE.description(), EXCISE.category());
        refused(List.of(BEAM, altered));
        assertFalse(SpellCompiler.compile(List.of(BEAM, HARM)).isEmpty());
    }
    @Test void allCompositeStorageAndPassiveRoutesRefuseBeforeAnyCast() {
        assertTrue(SpellCompiler.compileStored(ExciseRules.RUNES).isEmpty());
        assertTrue(SpellCompiler.stored(List.of(SELF, IMBUE, BEAM, EXCISE)).isEmpty());
        assertFalse(Passives.allowed(EXCISE)); assertNotNull(Passives.problem(ExciseRules.RUNES));
        assertNotNull(Knots.problem(ExciseRules.RUNES));
        assertFalse(Fusions.fusible(EXCISE)); assertFalse(Fusions.weavable(EXCISE));
        assertThrows(IllegalArgumentException.class, () -> WovenRunes.bind(EXCISE, HARM));
        String nested = Knots.PREFIX + Knots.encode("missing:unloaded,beam,excise".getBytes(StandardCharsets.UTF_8));
        for (int depth = 0; depth < 8; depth++) {
            assertTrue(ExciseRules.containsIds(List.of(nested)));
            refused(List.of(new RuneDef(nested, "forged", RuneFamily.KNOT, 1, 0, 1, "", EffectKind.NONE, Set.of(), "", "", "")));
            nested = Knots.PREFIX + Knots.encode(("self," + nested).getBytes(StandardCharsets.UTF_8));
        }
        assertTrue(ExciseRules.containsIds(List.of(Knots.PREFIX + "!")));
        assertFalse(ExciseRules.containsIds(List.of("missing:excise", "wildercord:excise_extra")));
    }
    @Test void oversizedRowsRetainARefusingMarkerAndNeverLeaveOrdinaryBeam() {
        var ids = new ArrayList<String>();
        for (int i = 0; i < 16; i++) ids.add(BEAM.id());
        ids.add(EXCISE.id());
        assertEquals(List.of(EXCISE.id()), ExciseRules.boundedIds(ids, 12));
        assertEquals(List.of(EXCISE.id()), new Spellbook(List.of(), List.of(ids), 0, false).spells().getFirst());
        assertTrue(ExciseRules.containsIds(SpellCodes.decode(SpellCodes.encode(ids))));
        assertEquals(List.of(EXCISE.id()), new ScrollSpell(ids, "", "").runes());
    }
    @Test void oldHeartwoodUnlocksRetrievalButStudyAndActiveSixteenthRemainSeparate() {
        assertTrue(MasterStudyRules.hasExciseLesson(true, false, false));
        assertFalse(MasterStudyRules.eligibleExcise(15, true));
        assertFalse(MasterStudyRules.eligibleExcise(20, false));
        assertTrue(MasterStudyRules.mayStudyExcise(16, true, true));
        assertFalse(ExciseRules.eligible(16, true, false));
        assertFalse(ExciseRules.eligible(15, true, true));
        assertTrue(ExciseRules.eligible(16, true, true));
        assertEquals(List.of(EXCISE), RuneSources.forSource("lesson:excise"));
        assertTrue(RuneSources.lessonOnly(EXCISE));
        assertFalse(RuneSources.forSource("archive").contains(EXCISE));
    }
    @Test void randomLootTradesResonancesAndWildSwapsCannotSupplyTheLesson() {
        assertFalse(Runes.common(EXCISE));
        assertFalse(ResonanceForge.inPool(EXCISE));
        assertFalse(dev.wildercord.runesmith.RuneTrades.pool(4).contains(EXCISE));
        assertFalse(dev.wildercord.runesmith.RuneTrades.takes(EXCISE));
        for (int choice = 0; choice < 64; choice++)
            assertFalse(WildMagic.swapElement(List.of(BOLT, DISMANTLE), "life", choice).contains(EXCISE));
    }
    @Test void savedRecoveryRetainsOnlyTheOriginalRemainingDeadline() {
        long until = ExciseRules.recoveryDeadline(100);
        assertEquals(112, until); assertEquals(12, ExciseRules.recoveryLeft(100, until));
        assertEquals(7, ExciseRules.recoveryLeft(105, until)); // replacement and reconnect copy the same value
        assertEquals(0, ExciseRules.recoveryLeft(112, until));
        assertEquals(0, ExciseRules.recoveryLeft(200, until));
        assertEquals(0, ExciseRules.recoveryLeft(0, until), "Another world's stale clock cannot lock forever");
        assertEquals(0, ExciseRules.recoveryLeft(-1, until));
        assertEquals(0, ExciseRules.recoveryLeft(Long.MAX_VALUE, until));
        assertEquals(0, ExciseRules.recoveryLeft(1, Long.MAX_VALUE));
        assertEquals(0, ExciseRules.recoveryDeadline(Long.MAX_VALUE - 11));
        assertEquals(Long.MAX_VALUE, ExciseRules.recoveryDeadline(Long.MAX_VALUE - 12));
    }
    private static void refused(List<RuneDef> row) {
        assertFalse(ExciseRules.valid(row)); assertTrue(SpellCompiler.compile(row).isEmpty(), row.toString());
    }
}
