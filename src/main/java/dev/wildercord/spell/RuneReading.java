package dev.wildercord.spell;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Reading a rune: how well a caster understands one they've learned. A rune learned (from its item, the Fusion Altar,
 * or a heart's awakening) starts <b>unread</b>: the Codex shows what it is (name, family, element, cost, tier) but only
 * a hint of what it does. The first cast <b>glimpses</b> it (its text, numbers veiled), and once it's been seen at work a
 * few times it's <b>understood</b>: its full text, as the wiki has it.
 *
 * <p>Progress is a count per rune, kept only while a rune is being read: a cast that holds it adds
 * {@link #CAST}, a cast of it that lands on something adds {@link #LANDED} more, and once it reaches
 * {@link #UNDERSTOOD_AT} the entry is dropped. So a rune <i>absent</i> from the count is understood: every rune a
 * player knew before reading existed, every starter rune, and everything learned while the server has it switched
 * off. Nobody who already knew a rune is ever asked to read it again.</p>
 */
public final class RuneReading {
	private RuneReading() {}

	public enum Stage {
		UNREAD, GLIMPSED, UNDERSTOOD
	}

	/** What a cast holding the rune adds, and what it adds more when it lands on something. */
	public static final int CAST = 1;
	public static final int LANDED = 1;
	/** Progress at which a rune is glimpsed (the first cast), and understood (three casts that land, or five that don't). */
	public static final int GLIMPSED_AT = 1;
	public static final int UNDERSTOOD_AT = 5;

	/** Where a rune with {@code progress} stands (null: not being read, so understood). */
	public static Stage stage(Integer progress) {
		if (progress == null || progress >= UNDERSTOOD_AT) {
			return Stage.UNDERSTOOD;
		}
		return progress >= GLIMPSED_AT ? Stage.GLIMPSED : Stage.UNREAD;
	}

	/** Whether a rune is read at all: a Knot (its contents are listed) and a weave (it follows its own runes) never are. */
	public static boolean tracked(RuneDef rune) {
		return !Knots.isKnot(rune) && !WovenRunes.isWoven(rune) && rune != Runes.TRIGGER;
	}

	/**
	 * Where {@code rune} stands for a caster who knows {@code knows} and is reading {@code progress}. A rune they
	 * don't know at all is only ever a hint; a weave stands where the least read of its runes does.
	 */
	public static Stage stage(RuneDef rune, Predicate<String> knows, Map<String, Integer> progress) {
		if (WovenRunes.isWoven(rune)) {
			Stage least = Stage.UNDERSTOOD;
			for (RuneDef part : WovenRunes.contents(rune)) {
				Stage stage = stage(progress.get(part.id()));
				if (stage.ordinal() < least.ordinal()) {
					least = stage;
				}
			}
			return least;
		}
		if (!tracked(rune)) {
			return Stage.UNDERSTOOD;
		}
		if (!knows.test(rune.id())) {
			return Stage.UNREAD;
		}
		return stage(progress.get(rune.id()));
	}

	/** {@code progress} after {@code amount} more, capped at {@link #UNDERSTOOD_AT}. */
	public static int add(Integer progress, int amount) {
		return Math.min(UNDERSTOOD_AT, (progress == null ? 0 : progress) + amount);
	}
}
