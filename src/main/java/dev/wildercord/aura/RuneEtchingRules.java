package dev.wildercord.aura;

import dev.wildercord.spell.*;

/** The etched blade carries an ordinary effect; a body's innate rune cannot be transplanted. */
public final class RuneEtchingRules {
	private RuneEtchingRules() {}
	public static final int LEVELS = 5;
	public static final int REST = 100;
	public static boolean accepts(RuneDef rune) {
		return rune != null && rune.family() == RuneFamily.EFFECT && !Runes.innate(rune)
			&& rune.kind() != EffectKind.NONE && Double.isFinite(rune.cost()) && rune.cost() >= 0;
	}
	public static int price(RuneDef rune) {
		if (!accepts(rune)) return -1;
		return Math.max(1, SpellCompiler.compile(java.util.List.of(Runes.TOUCH, rune)).manaCost());
	}
	public static boolean ready(long now, long until) { return now >= 0 && now >= until && now <= Long.MAX_VALUE - REST; }
}
