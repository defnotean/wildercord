package dev.wildercord.aura;

import dev.wildercord.spell.*;

/** The etched blade carries an ordinary effect; a body's innate rune cannot be transplanted. */
public final class RuneEtchingRules {
	private RuneEtchingRules() {}
	public static final int LEVELS = 5;
	public static final int REST = 100;
	public static boolean accepts(RuneDef rune) {
		return rune != null && rune.family() == RuneFamily.EFFECT && !Runes.innate(rune) && !ExciseRules.contains(java.util.List.of(rune)) && !dev.wildercord.spell.LessonPackRules.contains(java.util.List.of(rune))
			&& rune.kind() != EffectKind.NONE && Double.isFinite(rune.cost()) && rune.cost() >= 0;
	}
	public static int price(RuneDef rune) {
		if (!accepts(rune)) return -1;
		var compiled = SpellCompiler.compile(java.util.List.of(Runes.TOUCH, rune));
        return compiled.isEmpty() ? -1 : Math.max(1, compiled.manaCost());
	}
	public static boolean ready(long now, long until) { return now >= 0 && now >= until && now <= Long.MAX_VALUE - REST; }
}
