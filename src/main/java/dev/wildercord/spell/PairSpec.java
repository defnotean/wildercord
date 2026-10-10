package dev.wildercord.spell;

import java.util.Set;

/**
 * What one hand-made pair fusion is (see {@link PairRunes}): its name, the element it wears, what kind of effect it
 * is, what modifiers can change on it, and what its rune says it does. Every pair of effects has its own, written
 * by hand alongside its effect in {@code dev.wildercord.pairs}.
 */
public record PairSpec(String name, String element, EffectKind kind, Set<String> traits, String text) {
	public PairSpec {
		traits = Set.copyOf(traits);
	}

	public PairSpec(String name, String element, EffectKind kind, String text, String... traits) {
		this(name, element, kind, Set.of(traits), text);
	}
}
