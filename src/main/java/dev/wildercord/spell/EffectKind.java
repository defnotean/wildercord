package dev.wildercord.spell;

/** Decides who an effect may touch. Friendly fire is off by design. */
public enum EffectKind {
	/** Only you and your allies. */
	HELPFUL,
	/** Never you or your allies. */
	HARMFUL,
	/** Acts on the block or point that was hit, not on creatures. */
	WORLD,
	/** Moves the caster, whatever was hit. */
	MOVEMENT,
	/** Not an effect. */
	NONE
}
