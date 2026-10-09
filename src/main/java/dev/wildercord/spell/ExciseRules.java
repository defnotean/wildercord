package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;

/** Minecraft-free grammar, storage and lifetime contract for the Excise lesson. */
public final class ExciseRules {
    private ExciseRules() {}
    public static final String ID = "wildercord:excise", STUDY = "study:excise";
    public static final int BASE_MANA = 36, REST_TICKS = 240, COMMIT_TICKS = 16, RECOVERY_TICKS = 12;
    public static final double RANGE = 12, MAX_DISPLACEMENT = 1, CORE_RADIUS = .45;
    public static final RuneDef RUNE = Runes.EXCISE;
    public static final List<RuneDef> RUNES = List.of(Runes.BEAM, RUNE);
    public static final List<String> IDS = List.of(Runes.BEAM.id(), ID);

    public static final String GRAMMAR_PROBLEM = "Excise needs exactly Beam then Excise in one active spell: no modifiers, links, extra groups, Knots or woven runes.";
    public static final String STORAGE_PROBLEM = "Excise is an active master lesson: it cannot be stored, imbued, inscribed on a scroll or sustained as a passive.";

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
        if (ReweaveRules.containsIds(ids)) return ReweaveRules.boundedIds(ids, maximum);
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
    /** Saved absolute recovery never renews on load; stale, reversed or wrapped clocks fail open. */
    public static int recoveryLeft(long now, long until) {
        if (now < 0 || until <= now || until - now > RECOVERY_TICKS) return 0;
        return (int)(until - now);
    }
    public static long recoveryDeadline(long now) {
        return now < 0 || now > Long.MAX_VALUE - RECOVERY_TICKS ? 0 : now + RECOVERY_TICKS;
    }
    public static boolean eligible(int activeCircles, boolean heartwood, boolean studied) {
        return activeCircles >= 16 && heartwood && studied;
    }
}
