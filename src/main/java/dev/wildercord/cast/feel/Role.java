package dev.wildercord.cast.feel;

import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;

/** What the first effect of a group does to its target; picks the character of the impact. From the rune's Codex category. */
public enum Role {
	STRIKE, BIND, MEND, MOVE, TIME, WORLD, CALL_UP;

	public static Role of(RuneDef effect) {
		if (effect == null) {
			return STRIKE;
		}
		return switch (effect.category()) {
			case "control" -> BIND;
			case "support" -> MEND;
			case "movement" -> MOVE;
			case "time" -> TIME;
			case "world" -> WORLD;
			case "summon" -> CALL_UP;
			// Damage, innate and anything an add-on names: helpful ones mend, the rest strike.
			default -> effect.kind() == EffectKind.HELPFUL ? MEND : STRIKE;
		};
	}
}
