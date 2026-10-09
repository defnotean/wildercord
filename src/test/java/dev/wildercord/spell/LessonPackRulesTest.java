package dev.wildercord.spell;

import dev.wildercord.content.ScrollSpell;
import dev.wildercord.player.Spellbook;
import dev.wildercord.spell.LessonPackRules.Lesson;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.spell.Runes.*;

class LessonPackRulesTest {
    @Test void eachExactRowPricesItsOwnBaseManaOnceWithItsOwnRest() {
        Set<Integer> circles = new HashSet<>();
        for (Lesson lesson : LessonPackRules.ALL) {
            var compiled = SpellCompiler.compile(lesson.runes, id -> 3);
            assertFalse(compiled.isEmpty(), lesson.name); assertTrue(compiled.warnings().isEmpty(), lesson.name);
            assertEquals(lesson.baseMana, compiled.cost(), .1, lesson.name); assertEquals(lesson.baseMana, compiled.manaCost(), lesson.name);
            assertEquals(lesson.restTicks, compiled.cooldownTicks(), lesson.name);
            assertSame(lesson.shape, compiled.root().groups.getFirst().shape);
            assertSame(lesson.rune, compiled.root().groups.getFirst().effects.getFirst().effect);
            assertNull(compiled.root().link); assertEquals(0, compiled.healthCost());
            assertTrue(String.join(" ", compiled.lines()).contains("paid once"));
            assertSame(lesson, LessonPackRules.byRune(lesson.id));
            assertTrue(circles.add(lesson.circle));
        }
        // Three circles that taught nothing before, three distinct boss feats and three distinct shapes.
        assertEquals(Set.of(10, 14, 18), circles);
        assertEquals(3, LessonPackRules.ALL.stream().map(l -> l.feat).distinct().count());
        assertEquals(3, LessonPackRules.ALL.stream().map(l -> l.shape).distinct().count());
    }
    @Test void everyExtraRuneAndWrongPairRefusesTheWholeAction() {
        for (Lesson lesson : LessonPackRules.ALL) {
            for (RuneDef extra : Runes.all()) {
                for (int index = 0; index < 3; index++) {
                    var row = new ArrayList<>(lesson.runes); row.add(index, extra); refused(row);
                }
                if (extra != lesson.shape) refused(List.of(extra, lesson.rune));
            }
            refused(List.of(lesson.rune)); refused(List.of(lesson.rune, lesson.shape));
            RuneDef altered = new RuneDef(lesson.rune.id(), lesson.rune.name(), lesson.rune.family(), lesson.rune.tier(), 0,
                lesson.rune.multiplier(), lesson.rune.element(), lesson.rune.kind(), lesson.rune.traits(), lesson.rune.needs(), lesson.rune.description(), lesson.rune.category());
            refused(List.of(lesson.shape, altered));
            assertNotNull(LessonPackRules.problem(List.of(SELF, lesson.rune)));
        }
        assertFalse(SpellCompiler.compile(List.of(WALL, HARM)).isEmpty());
        assertFalse(SpellCompiler.compile(List.of(PILLAR, HARM)).isEmpty());
    }
    @Test void storageCompositesAndPassivesRefuseBeforeAnyCast() {
        for (Lesson lesson : LessonPackRules.ALL) {
            assertTrue(SpellCompiler.compileStored(lesson.runes).isEmpty(), lesson.name);
            assertTrue(SpellCompiler.stored(List.of(SELF, IMBUE, lesson.shape, lesson.rune)).isEmpty(), lesson.name);
            assertFalse(Passives.allowed(lesson.rune), lesson.name);
            assertFalse(Fusions.fusible(lesson.rune)); assertFalse(Fusions.weavable(lesson.rune));
            String nested = Knots.PREFIX + Knots.encode(("missing:unloaded," + lesson.shape.path() + "," + lesson.path).getBytes(StandardCharsets.UTF_8));
            for (int depth = 0; depth < 8; depth++) {
                assertSame(lesson, LessonPackRules.lessonOfIds(List.of(nested)));
                refused(List.of(new RuneDef(nested, "forged", RuneFamily.KNOT, 1, 0, 1, "", EffectKind.NONE, Set.of(), "", "", "")));
                nested = Knots.PREFIX + Knots.encode(("self," + nested).getBytes(StandardCharsets.UTF_8));
            }
        }
        assertSame(Lesson.TOLLGATE, LessonPackRules.lessonOfIds(List.of(Knots.PREFIX + "!")));
        assertFalse(LessonPackRules.containsIds(List.of("missing:tollgate", "wildercord:conduit_extra")));
    }
    @Test void malformedRowsFailClosedButAreNeverNamedAsALesson() {
        String broken = Knots.PREFIX + "!";
        assertTrue(LessonPackRules.malformedIds(List.of(broken)));
        assertTrue(LessonPackRules.malformedIds(List.of(SELF.id(), broken)));
        assertTrue(LessonPackRules.containsIds(List.of(broken)), "still kept out of every other cast path");
        assertFalse(LessonPackRules.malformedIds(List.of()));
        assertFalse(LessonPackRules.malformedIds(List.of(SELF.id(), IMBUE.id())));
        for (Lesson lesson : LessonPackRules.ALL) {
            assertFalse(LessonPackRules.malformedIds(lesson.ids), lesson.name);
            String nested = Knots.PREFIX + Knots.encode(("self," + lesson.shape.path() + "," + lesson.path).getBytes(StandardCharsets.UTF_8));
            assertFalse(LessonPackRules.malformedIds(List.of(nested)), lesson.name);
        }
    }
    @Test void aThreadedAllyAlwaysHasTheConsentGapToCrouchAndRefuse() {
        long threaded = 1000;
        assertEquals(20, LessonPackRules.CONSENT_TICKS);
        for (long now = threaded; now < threaded + LessonPackRules.CONSENT_TICKS; now++) {
            assertTrue(LessonPackRules.consentOpen(threaded, now), "open at " + (now - threaded));
            assertFalse(LessonPackRules.mayReel(threaded, now, false), "no reel at " + (now - threaded));
        }
        long after = threaded + LessonPackRules.CONSENT_TICKS;
        assertFalse(LessonPackRules.consentOpen(threaded, after));
        assertTrue(LessonPackRules.mayReel(threaded, after, false));
        assertFalse(LessonPackRules.mayReel(threaded, after, true), "a crouching ally is never reeled");
        assertFalse(LessonPackRules.mayReel(threaded, threaded - 1, false), "a clock that ran backwards never reels");
        assertFalse(LessonPackRules.consentOpen(threaded, threaded - 1));
    }
    @Test void oversizedRowsRetainARefusingMarker() {
        for (Lesson lesson : LessonPackRules.ALL) {
            var ids = new ArrayList<String>();
            for (int i = 0; i < 16; i++) ids.add(lesson.shape.id());
            ids.add(lesson.id);
            assertEquals(List.of(lesson.id), LessonPackRules.boundedIds(ids, 12));
            assertEquals(List.of(lesson.id), new Spellbook(List.of(), List.of(ids), 0, false).spells().getFirst());
            assertTrue(LessonPackRules.containsIds(SpellCodes.decode(SpellCodes.encode(ids))));
            assertEquals(List.of(lesson.id), new ScrollSpell(ids, "", "").runes());
        }
    }
    @Test void bossFeatRecoversTheStudyButCircleAndReadingStaySeparate() {
        for (Lesson lesson : LessonPackRules.ALL) {
            assertTrue(lesson.hasLesson(true, false, false)); assertFalse(lesson.hasLesson(false, false, false));
            assertFalse(lesson.eligible(lesson.circle - 1, true)); assertFalse(lesson.eligible(20, false));
            assertTrue(lesson.eligible(lesson.circle, true));
            assertFalse(lesson.mayStudy(lesson.circle, true, false)); assertTrue(lesson.mayStudy(lesson.circle, true, true));
            assertTrue(Feats.FEATS.stream().anyMatch(f -> f.id().equals(lesson.feat) && f.name().equals(lesson.featName)), lesson.feat);
            assertEquals(List.of(lesson.rune), RuneSources.forSource("lesson:" + lesson.path));
            assertTrue(RuneSources.lessonOnly(lesson.rune));
            assertFalse(Runes.common(lesson.rune));
            assertFalse(ResonanceForge.inPool(lesson.rune));
            assertFalse(dev.wildercord.runesmith.RuneTrades.takes(lesson.rune));
            for (int tier = 1; tier <= 4; tier++) assertFalse(dev.wildercord.runesmith.RuneTrades.pool(tier).contains(lesson.rune));
            for (int choice = 0; choice < 64; choice++)
                assertFalse(WildMagic.swapElement(List.of(BOLT, HARM), lesson.rune.element(), choice).contains(lesson.rune));
        }
    }
    @Test void timelinesAndRestNeverRenewOrLockForever() {
        var line = LessonPackRules.Timeline.start(100, LessonPackRules.LINE_TICKS);
        assertEquals(160, line.left(100)); assertFalse(line.expired(259)); assertTrue(line.expired(260));
        assertTrue(line.expired(99), "a rewound clock retires the object");
        assertEquals(Long.MAX_VALUE, LessonPackRules.Timeline.start(Long.MAX_VALUE - 5, 400).expires());
        Lesson gate = Lesson.TOLLGATE;
        assertEquals(150, LessonPackRules.restLeft(gate, 50, 200)); assertEquals(0, LessonPackRules.restLeft(gate, 200, 200));
        assertEquals(0, LessonPackRules.restLeft(gate, 0, 10_000), "a foreign clock cannot lock the lesson");
        assertEquals(0, LessonPackRules.restLeft(gate, -1, 10));
        assertEquals(10, gate.seconds()); assertEquals(15, Lesson.CONDUIT.seconds());
    }
    @Test void gateGeometryTollsWalkersFromTheirOwnSideAndLetsJumpersPass() {
        double ax = 1, az = 0; // a gate along +X
        assertEquals(2, LessonPackRules.along(2, 1, ax, az), 1e-9); assertEquals(1, LessonPackRules.across(2, 1, ax, az), 1e-9);
        assertTrue(LessonPackRules.atGate(0, .3, 0, .3));
        assertFalse(LessonPackRules.atGate(0, .3, 1.2, .3), "a jump clears the threshold");
        assertFalse(LessonPackRules.atGate(3.2, 0, 0, .3), "going around the end passes");
        assertFalse(LessonPackRules.atGate(0, 1.2, 0, .3));
        assertEquals(1, LessonPackRules.side(.8, -.1, -2)); assertEquals(-1, LessonPackRules.side(0, -.2, 2));
        assertEquals(1, LessonPackRules.side(0, 0, -2), "a body exactly on the line returns away from the caster");
        assertEquals(-1, LessonPackRules.side(0, 0, 2));
    }
    @Test void rodIsGroundedByANearHostileAndSparksBeforeArrival() {
        assertTrue(LessonPackRules.grounded(1.5)); assertFalse(LessonPackRules.grounded(1.6));
        assertFalse(LessonPackRules.sparked(100, 107)); assertTrue(LessonPackRules.sparked(100, 108));
        assertTrue(LessonPackRules.ROD_ARRIVE > LessonPackRules.ROD_RANGE && LessonPackRules.LINE_BREAK > LessonPackRules.LINE_RANGE);
    }
    private static void refused(List<RuneDef> row) {
        assertFalse(LessonPackRules.valid(row)); assertTrue(SpellCompiler.compile(row).isEmpty(), row.toString());
    }
}
