package dev.wildercord.spell;

import java.util.List;
import java.util.Set;

/** Unregistered feasibility contract. No item, recipe, lesson or client input exposes this rune yet. */
public final class ReweaveRules {
    private ReweaveRules() {}
    public static final String ID = "wildercord:reweave", STUDY = "study:reweave";
    public static final int BASE_MANA = 32, REST_TICKS = 160, LIFETIME = 80, WARNING = 8;
    public static final double PLACE_RANGE = 8, DISC_RADIUS = 2, LANE_LENGTH = 7, LANE_WIDTH = 1.25, HEIGHT = 2;
    public static final double BEAT_POWER = .5;
    public static final List<Integer> BEATS = List.of(8, 28, 48, 68);
    public static final RuneDef RUNE = new RuneDef(ID, "Reweave", RuneFamily.SHAPE, 4, BASE_MANA, 1,
        "", EffectKind.NONE, Set.of(), "", "Feasibility only: one paid Harm field may be rewritten once.", "area");
    public static final List<RuneDef> RUNES = List.of(RUNE, Runes.HARM);
    public static final List<String> IDS = List.of(ID, Runes.HARM.id());

    public static boolean valid(List<RuneDef> runes) { return RUNES.equals(runes); }
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
