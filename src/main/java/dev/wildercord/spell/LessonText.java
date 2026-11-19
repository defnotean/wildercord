package dev.wildercord.spell;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;

/**
 * The lesson grammar and storage problems as language keys. The compiler and the server keep the
 * plain English strings (tests, logs and the cache read them), and every player-facing site sends
 * them through here, so a translation reaches the Cord screen readout and the cast failures alike.
 */
public final class LessonText {
    private LessonText() {}

    private static final Map<String, String> KEYS = new HashMap<>();
    static {
        KEYS.put(ExciseRules.GRAMMAR_PROBLEM, ExciseRules.GRAMMAR_KEY);
        KEYS.put(ExciseRules.STORAGE_PROBLEM, ExciseRules.STORAGE_KEY);
        KEYS.put(RelayRules.GRAMMAR_PROBLEM, RelayRules.GRAMMAR_KEY);
        KEYS.put(RelayRules.STORAGE_PROBLEM, RelayRules.STORAGE_KEY);
        KEYS.put(ReweaveRules.GRAMMAR_PROBLEM, ReweaveRules.GRAMMAR_KEY);
        KEYS.put(ReweaveRules.STORAGE_PROBLEM, ReweaveRules.STORAGE_KEY);
        for (LessonPackRules.Lesson lesson : LessonPackRules.ALL) {
            KEYS.put(lesson.grammarProblem, lesson.grammarKey);
            KEYS.put(lesson.storageProblem, lesson.storageKey);
        }
    }

    /** The language key for a known lesson problem, or null when the string is the compiler's own words. */
    public static String key(String problem) { return KEYS.get(problem); }

    /** A known lesson problem renders from the language file with the English as fallback; anything else stays as written. */
    public static Component problem(String problem) {
        String key = key(problem);
        return key == null ? Component.literal(problem) : Component.translatableWithFallback(key, problem);
    }
}
