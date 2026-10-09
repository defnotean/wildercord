package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;

/** Minecraft-free grammar, storage and lifetime contract for the Reweave lesson. */
public final class ReweaveRules {
    private ReweaveRules() {}
    public static final String ID = "wildercord:reweave", STUDY = "study:reweave";
    public static final int BASE_MANA = 32, REST_TICKS = 160, LIFETIME = 80, WARNING = 8;
    public static final double PLACE_RANGE = 8, DISC_RADIUS = 2, LANE_LENGTH = 7, LANE_WIDTH = 1.25, HEIGHT = 2;
    /** Maximum owner-to-field and owner-to-target distance; runtime also requires clear line of sight. */
    public static final double LOS_RANGE = 16;
    public static final double BEAT_POWER = .5;
    public static final List<Integer> BEATS = List.of(8, 28, 48, 68);
    public static final RuneDef RUNE = Runes.REWEAVE;
    public static final List<RuneDef> RUNES = List.of(RUNE, Runes.HARM);
    public static final List<String> IDS = List.of(ID, Runes.HARM.id());

    public static final String GRAMMAR_PROBLEM = "Reweave needs exactly Reweave then Harm in one active spell: no modifiers, links, extra groups, Knots or woven runes.";
    public static final String STORAGE_PROBLEM = "Reweave is an active master lesson: it cannot be stored, imbued, inscribed on a scroll or sustained as a passive.";

    /** Only the original registered definitions are accepted, never altered definitions with the same ids. */
    public static boolean valid(List<RuneDef> runes) { return RUNES.equals(runes); }
    public static String problem(List<RuneDef> runes) {
        return contains(runes) && !valid(runes) ? GRAMMAR_PROBLEM : null;
    }
    public static boolean contains(List<RuneDef> runes) {
        return containsIds(runes.stream().map(RuneDef::id).toList());
    }

    /** Preserve invalid drafts within the bound; an oversized restricted row remains uncastable. */
    public static List<String> boundedIds(List<String> ids, int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Negative rune limit");
        if (ids.size() <= maximum) return List.copyOf(ids);
        if (RelayRules.containsIds(ids)) return RelayRules.boundedIds(ids, maximum);
        if (containsIds(ids)) return maximum == 0 ? List.of() : List.of(ID);
        return List.copyOf(ids.subList(0, maximum));
    }

    /**
     * Inspect original ids before unknown-rune filtering or compiler expansion. Read composite
     * contents without resolving their siblings or trusting a nesting limit; unreadable and
     * oversized wrappers fail closed rather than silently leaving a castable Harm behind.
     */
    public static boolean containsIds(List<String> ids) {
        ArrayDeque<String> pending = new ArrayDeque<>();
        for (String id : ids) {
            if (id == null) return true;
            pending.add(id);
        }
        int remaining = Knots.MAX_ID_LENGTH * 8;
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (ID.equals(id)) return true;
            boolean knot = Knots.isKnot(id);
            boolean woven = WovenRunes.isWoven(id);
            if (!knot && !woven) continue;
            if (id.length() > Knots.MAX_ID_LENGTH || (remaining -= id.length()) < 0) return true;
            String prefix = knot ? Knots.PREFIX : WovenRunes.PREFIX;
            byte[] bytes = Knots.decode(id.substring(prefix.length()));
            if (bytes == null) return true;
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (knot && body.contains("|")) body = body.substring(0, body.indexOf('|'));
            if (body.isEmpty()) return true;
            for (String part : body.split(knot ? "," : "\n", -1)) {
                if (part.isEmpty()) return true;
                pending.addLast(knot && !part.contains(":") ? "wildercord:" + part : part);
            }
        }
        return false;
    }
    public static boolean eligible(int activeCircles, boolean lowTide, boolean studied) {
        return activeCircles >= 12 && lowTide && studied;
    }

    /** Immutable schedule. Consumed warning/late beats are never deferred, copied, refunded or replayed. */
    public record Timeline(long created, int nextBeat, long convertedAt) {
        public Timeline {
            if (nextBeat < 0 || nextBeat > BEATS.size() || convertedAt < -1 || convertedAt != -1 && convertedAt < created)
                throw new IllegalArgumentException("Invalid Reweave timeline");
            Math.addExact(created, LIFETIME);
        }
        public static Timeline start(long tick) { return new Timeline(tick, 0, -1); }
        public long expires() { return created + LIFETIME; }
        public boolean converted() { return convertedAt != -1; }
        public boolean warning(long tick) { return converted() && tick < convertedAt + WARNING; }
        public boolean expired(long tick) { return tick >= expires(); }
        public Advance advance(long tick) {
            int next = nextBeat;
            boolean strike = false;
            while (next < BEATS.size() && created + BEATS.get(next) <= tick) {
                strike |= created + BEATS.get(next) == tick && !expired(tick) && !warning(tick);
                next++;
            }
            return new Advance(new Timeline(created, next, convertedAt), strike);
        }
        /** Null means refusal: callers must keep the exact old state and footprint. */
        public Timeline convert(long tick) {
            if (converted() || tick < created || expired(tick)) return null;
            boolean useful = false;
            for (int i = nextBeat; i < BEATS.size(); i++)
                if (created + BEATS.get(i) >= tick + WARNING) useful = true;
            if (!useful) return null;
            return new Timeline(created, advance(tick).timeline.nextBeat, tick);
        }
    }
    public record Advance(Timeline timeline, boolean strike) {}

    /** Target feet must be inside the footprint. Wide bodies do not enlarge the paid area. */
    public static boolean insideDisc(double dx, double dz) {
        return Double.isFinite(dx) && Double.isFinite(dz) && dx * dx + dz * dz <= DISC_RADIUS * DISC_RADIUS;
    }
    public static boolean insideLane(double dx, double dz, double ux, double uz) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz) || !Double.isFinite(ux) || !Double.isFinite(uz)
            || Math.abs(ux * ux + uz * uz - 1) > 1.0e-6) return false;
        double along = dx * ux + dz * uz, across = dx * -uz + dz * ux;
        return along >= 0 && along <= LANE_LENGTH && Math.abs(across) <= LANE_WIDTH / 2;
    }
}
