package dev.wildercord.spell;

/** The four jobs a rune can have in a spell. */
public enum RuneFamily {
	/** Where the spell goes: who or what gets hit. */
	SHAPE,
	/** What happens to whatever got hit. */
	EFFECT,
	/** Changes the closest rune to its left that it can affect. */
	MODIFIER,
	/** When the rest of the spell fires. */
	LINK
}
