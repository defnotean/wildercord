package dev.wildercord.monster;

import dev.wildercord.spell.RuneDef;

import java.util.List;

/**
 * A creature that can roll Runebound like a zombie or a witch does (see {@code cast.Runebound}): the spells that suit it,
 * one of which it carries on its Cord if it does.
 */
public interface RuneboundKin {
	List<List<RuneDef>> runeboundSpells();
}
