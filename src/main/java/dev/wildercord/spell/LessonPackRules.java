package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

/**
 * Minecraft-free grammar, entitlement and timing contract for the first authored lesson pack:
 * Tollgate (Circle X control), Lifeline (Circle XIV cooperation) and Conduit (Circle XVIII traversal).
 * Each lesson is one exact two-rune Echo Cord row, paid once per placement, with its own shared rest.
 */
public final class LessonPackRules {
    private LessonPackRules() {}

    public enum Lesson {
        TOLLGATE("tollgate", Runes.WALL, Runes.TOLLGATE, 10, Feats.CINDER_WARDEN, "Tempered", 30, 200, "The Warden's Threshold", "X"),
        LIFELINE("lifeline", Runes.BEAM, Runes.LIFELINE, 14, Feats.STAR_EATER, "Starbreaker", 28, 200, "The Thread Between Stars", "XIV"),
        CONDUIT("conduit", Runes.PILLAR, Runes.CONDUIT, 18, Feats.STORM_CONDUCTOR, "Grounded", 32, 300, "Notes on a Grounded Storm", "XVIII");

        public final String path, id, name, study, copied, practice, feat, featName, title, numeral, grammarProblem, storageProblem;
        public final RuneDef shape, rune;
        public final List<RuneDef> runes;
        public final List<String> ids;
        public final int circle, baseMana, restTicks;
        Lesson(String path, RuneDef shape, RuneDef rune, int circle, String feat, String featName, int baseMana, int restTicks, String title, String numeral) {
            this.path = path; this.id = rune.id(); this.name = rune.name(); this.shape = shape; this.rune = rune;
            this.study = "study:" + path; this.copied = "study:" + path + "_copied"; this.practice = "practice:" + path;
            this.feat = feat; this.featName = featName; this.title = title; this.numeral = numeral;
            this.circle = circle; this.baseMana = baseMana; this.restTicks = restTicks;
            this.runes = List.of(shape, rune); this.ids = List.of(shape.id(), rune.id());
            this.grammarProblem = name + " needs exactly " + shape.name() + " then " + name + " in one active spell: no modifiers, links, extra groups, Knots or woven runes.";
            this.storageProblem = name + " is an active master lesson: it cannot be stored, imbued, inscribed on a scroll or sustained as a passive.";
        }
        /** Only the original registered definitions are accepted, never altered definitions with the same ids. */
        public boolean valid(List<RuneDef> row) { return runes.equals(row); }
        public boolean eligible(int activeCircles, boolean feat) { return activeCircles >= circle && feat; }
        /** A boss victory recorded before this lesson existed already recovers its pages. */
        public boolean hasLesson(boolean feat, boolean copied, boolean learned) { return feat || copied || learned; }
        public boolean mayStudy(int activeCircles, boolean feat, boolean copied) { return copied && eligible(activeCircles, feat); }
        public int seconds() { return restTicks / 20; }
    }
    public static final List<Lesson> ALL = List.of(Lesson.values());
    private static final Set<String> IDS = Set.of(Runes.TOLLGATE.id(), Runes.LIFELINE.id(), Runes.CONDUIT.id());

    // Tollgate: a five-block threshold line that halts each hostile crosser once, three tolls in all.
    public static final double GATE_RANGE = 10, GATE_HALF_LENGTH = 2.5, GATE_BAND = .45, GATE_HEIGHT = 1, GATE_PUSH = .55;
    public static final int GATE_TICKS = 120, GATE_TOLLS = 3, GATE_SLOW_TICKS = 30;
    // Lifeline: thread one ally in sight, then a fresh press reels them to a safe spot beside you.
    public static final double LINE_RANGE = 16, LINE_BREAK = 24;
    public static final int LINE_TICKS = 160;
    /** A threaded ally always gets this long to crouch and refuse before any reel. */
    public static final int CONSENT_TICKS = 20;
    // Conduit: plant one rod on visible floor, then a fresh press sparks up and arrives on it.
    public static final double ROD_RANGE = 20, ROD_ARRIVE = 32, ROD_GROUND = 1.5;
    public static final int ROD_TICKS = 400, SPARK_TICKS = 8;

    public static Lesson lesson(List<RuneDef> runes) { return lessonOfIds(runes.stream().map(RuneDef::id).toList()); }
    public static boolean valid(List<RuneDef> runes) { return ALL.stream().anyMatch(l -> l.valid(runes)); }
    public static boolean contains(List<RuneDef> runes) { return containsIds(runes.stream().map(RuneDef::id).toList()); }
    public static String problem(List<RuneDef> runes) {
        Lesson lesson = lesson(runes);
        return lesson != null && !lesson.valid(runes) ? lesson.grammarProblem : null;
    }
    public static Lesson byRune(String id) {
        for (Lesson lesson : ALL) if (lesson.id.equals(id)) return lesson;
        return null;
    }

    /** Preserve invalid drafts within the bound; an oversized restricted row remains uncastable. */
    public static List<String> boundedIds(List<String> ids, int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Negative rune limit");
        if (ids.size() <= maximum) return List.copyOf(ids);
        Lesson lesson = RelayRules.containsIds(ids) || ReweaveRules.containsIds(ids) || ExciseRules.containsIds(ids) ? null : lessonOfIds(ids);
        if (lesson != null) return maximum == 0 ? List.of() : List.of(lesson.id);
        return ExciseRules.boundedIds(ids, maximum);
    }
    public static boolean containsIds(List<String> ids) { return lessonOfIds(ids) != null; }
    /** The first pack lesson named anywhere in these ids. Unreadable or oversized composites fail closed as Tollgate. */
    public static Lesson lessonOfIds(List<String> ids) {
        int found = scan(ids);
        return found == NONE ? null : found == MALFORMED ? Lesson.TOLLGATE : ALL.get(found);
    }
    /** An unreadable or oversized composite: kept out of every other path, but never named as any one lesson to the player. */
    public static boolean malformedIds(List<String> ids) { return scan(ids) == MALFORMED; }
    private static final int NONE = -1, MALFORMED = -2;
    private static int scan(List<String> ids) {
        ArrayDeque<String> pending = new ArrayDeque<>();
        for (String id : ids) {
            if (id == null) return MALFORMED;
            pending.add(id);
        }
        int remaining = Knots.MAX_ID_LENGTH * 8;
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (IDS.contains(id)) return byRune(id).ordinal();
            boolean knot = Knots.isKnot(id), woven = WovenRunes.isWoven(id);
            if (!knot && !woven) continue;
            if (id.length() > Knots.MAX_ID_LENGTH || (remaining -= id.length()) < 0) return MALFORMED;
            byte[] bytes = Knots.decode(id.substring((knot ? Knots.PREFIX : WovenRunes.PREFIX).length()));
            if (bytes == null) return MALFORMED;
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (knot && body.contains("|")) body = body.substring(0, body.indexOf('|'));
            if (body.isEmpty()) return MALFORMED;
            for (String part : body.split(knot ? "," : "\n", -1)) {
                if (part.isEmpty()) return MALFORMED;
                pending.addLast(knot && !part.contains(":") ? "wildercord:" + part : part);
            }
        }
        return NONE;
    }
    /** Consent: while the gap is open a crouch refuses; only after it, and never while crouching, may the ally be reeled. */
    public static boolean consentOpen(long threaded, long now) { return now >= threaded && now - threaded < CONSENT_TICKS; }
    public static boolean mayReel(long threaded, long now, boolean crouching) { return !crouching && now >= threaded && now - threaded >= CONSENT_TICKS; }

    /** Fresh-press timeline of one paid object: it never renews and never outlives its absolute expiry. */
    public record Timeline(long created, long expires) {
        public static Timeline start(long now, int ticks) { return new Timeline(now, now > Long.MAX_VALUE - ticks ? Long.MAX_VALUE : now + ticks); }
        public boolean expired(long now) { return now < created || now >= expires; }
        public int left(long now) { return expired(now) ? 0 : (int) Math.min(Integer.MAX_VALUE, expires - now); }
    }
    /** Shared rest left, clamped so a stale or foreign clock can never lock a lesson forever. */
    public static int restLeft(Lesson lesson, long now, long until) {
        if (now < 0 || until <= now || until - now > lesson.restTicks) return 0;
        return (int) (until - now);
    }

    /** Gate-local coordinates: along the line and across it, with (ax, az) the unit gate direction. */
    public static double along(double dx, double dz, double ax, double az) { return dx * ax + dz * az; }
    public static double across(double dx, double dz, double ax, double az) { return -dx * az + dz * ax; }
    /** A body is at the threshold when its feet are below the gate top and it overlaps the band. Jumping over passes. */
    public static boolean atGate(double along, double across, double feet, double radius) {
        return Math.abs(along) <= GATE_HALF_LENGTH + radius && Math.abs(across) <= GATE_BAND + radius && feet > -.6 && feet < GATE_HEIGHT;
    }
    /** The side a crosser came from: last tick's side, else its current side, else the caster's side. */
    public static int side(double previousAcross, double across, double casterAcross) {
        if (Math.abs(previousAcross) > 1e-3) return previousAcross > 0 ? 1 : -1;
        if (Math.abs(across) > 1e-3) return across > 0 ? 1 : -1;
        return casterAcross < 0 ? 1 : -1;
    }
    /** A rod is grounded, and refuses arrival, while any hostile body stands this close to it. */
    public static boolean grounded(double hostileDistance) { return hostileDistance <= ROD_GROUND; }
    public static boolean sparked(long began, long now) { return now >= began + SPARK_TICKS; }
}
