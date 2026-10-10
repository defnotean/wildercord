package dev.wildercord.pairs;

import dev.wildercord.spell.EffectKind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks one hand-made pair fusion: the method under it is the pair's whole effect. {@code tools/pairs/index.py}
 * reads these to write {@link PairIndex}; nothing reads them at runtime. {@code a} and {@code b} are the two runes'
 * paths, alphabetically; {@code element} is one of their two elements.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Pair {
	String a();

	String b();

	String name();

	String element();

	EffectKind kind();

	/** From {@code power}, {@code duration}, {@code radius} and {@code linger}: what modifiers may change on it. */
	String[] traits() default {};

	String text();
}
