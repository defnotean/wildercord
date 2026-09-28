package dev.wildercord.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * An element reaction: spell damage of one element meeting a mark left by another. Checked for every
 * point of spell damage of its element; return a damage multiplier (1 for no reaction). Use
 * {@link WildercordApi#mark}, {@link WildercordApi#hasMark} and {@link WildercordApi#clearMark} to
 * leave and read marks from your effects.
 *
 * <pre>{@code
 * api.registerReaction("example:steam", "fire", (caster, target) -> {
 *     if (!api.hasMark(target, "example:soaked")) return 1.0;
 *     api.clearMark(target, "example:soaked");
 *     return 1.4;
 * });
 * }</pre>
 *
 * @since 1.0
 */
@FunctionalInterface
public interface ElementReaction {
	/**
	 * @param caster who cast the spell (a player or a monster)
	 * @param target what the damage is about to hit
	 * @return the damage multiplier
	 */
	double react(LivingEntity caster, LivingEntity target);
}
