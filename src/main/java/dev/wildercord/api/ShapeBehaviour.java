package dev.wildercord.api;

/**
 * Where an add-on shape rune goes: it finds what it hits and hands each hit to the group's effects
 * with {@link ShapeContext#hit}. Called on the server whenever a group with this shape fires.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface ShapeBehaviour {
	void deliver(ShapeContext context);
}
