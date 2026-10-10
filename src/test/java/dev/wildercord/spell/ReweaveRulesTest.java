package dev.wildercord.spell;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReweaveRulesTest {
    @Test void exactGrammarAndExistingEntitlement() {
        assertTrue(ReweaveRules.valid(ReweaveRules.RUNES));
        assertFalse(ReweaveRules.valid(List.of(ReweaveRules.RUNE, Runes.FROST)));
        assertFalse(ReweaveRules.valid(List.of(ReweaveRules.RUNE, Runes.HARM, Runes.AMPLIFY)));
        assertTrue(ReweaveRules.eligible(12, true, true));
        assertFalse(ReweaveRules.eligible(11, true, true));
        assertFalse(ReweaveRules.eligible(12, false, true));
        assertFalse(ReweaveRules.eligible(12, true, false));
        assertEquals(810000, Circles.condenseNeeded(12));
        assertEquals(List.of(new Circles.Requirement(Circles.Need.RUNEBOUND, 20), Circles.Requirement.feat(Feats.TIDE_SCRIBE)), Circles.requirements(12));
        assertSame(Runes.REWEAVE, Runes.get(ReweaveRules.ID).orElseThrow());
        assertSame(Runes.REWEAVE, ReweaveRules.RUNE);
        assertEquals("Reweave", ReweaveRules.RUNE.name());
        assertNotNull(Passives.problem(ReweaveRules.RUNES));
    }
    @Test void fourAbsoluteBeatsAndNoSameTickReplay() {
        var t = ReweaveRules.Timeline.start(1000);
        List<Long> hits = new ArrayList<>();
        for (long now = 1000; now <= 1090; now++) {
            var a = t.advance(now); t = a.timeline(); if (a.strike()) hits.add(now);
            assertFalse(t.advance(now).strike());
        }
        assertEquals(List.of(1008L, 1028L, 1048L, 1068L), hits);
        assertEquals(1080, t.expires());
    }
    @Test void conversionConsumesWarningBeatsWithoutNewAllowance() {
        var original = ReweaveRules.Timeline.start(1000).advance(1008).timeline();
        var t = original.convert(1024);
        assertNotNull(t); assertEquals(1080, t.expires());
        assertEquals(original.nextBeat(), t.nextBeat());
        assertFalse(t.advance(1028).strike()); t = t.advance(1028).timeline();
        assertEquals(2, t.nextBeat()); assertNull(t.convert(1033));
        assertTrue(t.advance(1048).strike()); t = t.advance(1048).timeline();
        assertTrue(t.advance(1068).strike());
        assertEquals(1, original.nextBeat(), "Immutable original schedule");
    }
    @Test void everyConversionBoundaryIsUsefulOnlyWithARemainingBeat() {
        for (long now = 0; now <= 100; now++) {
            var original = ReweaveRules.Timeline.start(0).advance(now).timeline();
            var converted = original.convert(now);
            assertEquals(now <= 60, converted != null, "tick " + now);
            if (converted != null) assertEquals(80, converted.expires());
        }
        var onBeat = ReweaveRules.Timeline.start(0).convert(28);
        assertNotNull(onBeat); assertEquals(2, onBeat.nextBeat()); assertFalse(onBeat.advance(28).strike());
        var warningEndsOnBeat = ReweaveRules.Timeline.start(0).convert(20);
        assertNotNull(warningEndsOnBeat); assertTrue(warningEndsOnBeat.advance(28).strike());
    }
    @Test void lateTicksConsumeWithoutCatchupBurstAndBoundsFailClosed() {
        var late = ReweaveRules.Timeline.start(0).advance(49);
        assertEquals(3, late.timeline().nextBeat()); assertFalse(late.strike());
        assertTrue(late.timeline().advance(68).strike());
        assertFalse(ReweaveRules.Timeline.start(0).advance(81).strike());
        assertTrue(ReweaveRules.insideDisc(2, 0)); assertFalse(ReweaveRules.insideDisc(2.001, 0));
        assertTrue(ReweaveRules.insideLane(7, .625, 1, 0));
        assertFalse(ReweaveRules.insideLane(7.001, 0, 1, 0));
        assertFalse(ReweaveRules.insideLane(-.01, 0, 1, 0));
        assertFalse(ReweaveRules.insideLane(1, .626, 1, 0));
        assertFalse(ReweaveRules.insideLane(1, 0, 2, 0));
        assertFalse(ReweaveRules.insideDisc(Double.NaN, 0));
    }
}
