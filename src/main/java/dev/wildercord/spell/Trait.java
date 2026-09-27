package dev.wildercord.spell;

/**
 * Properties a rune exposes to modifiers. A modifier names the one trait it needs and
 * attaches to the closest rune on its left that has it, so add-on runes join in by
 * declaring traits instead of being listed anywhere.
 */
public final class Trait {
	private Trait() {}

	public static final String POWER = "power";
	public static final String DURATION = "duration";
	public static final String RADIUS = "radius";
	public static final String SPEED = "speed";
	public static final String PIERCE = "pierce";
	public static final String BOUNCE = "bounce";
	public static final String SPLIT = "split";
	public static final String HOMING = "homing";
	public static final String CHAIN = "chain";
	/** Effects that can land again over time (Linger). */
	public static final String LINGER = "linger";
	/** Every effect: Frugal trades power for cost. */
	public static final String FRUGAL = "frugal";
	/** Projectiles and beams that can fire in quick succession (Volley). */
	public static final String VOLLEY = "volley";
	/** Every shape: Rapid shortens the whole spell's cooldown. */
	public static final String COOLDOWN = "cooldown";
}
