package dev.wildercord.spell;

/**
 * Minecraft-free Heart Paths (0.13): at the Tenth Circle a heart chooses which way it grows, once. Each path tilts the heart's
 * numbers like a vow and adds one signature of its own. Leaving a path to walk another costs experience levels. Like a vow, a
 * path is silent while the Tenth Circle is unformed or cracked, and an old save has none chosen.
 */
public final class HeartPaths {
	private HeartPaths() {}

	/** The circle that asks for a path. */
	public static final int CIRCLE = 10;
	/** Experience levels to leave a path (nothing in creative). Choosing again afterwards is free. */
	public static final int LEAVE_LEVELS = 10;
	/** Storm: the share of max mana at or above which a spell counts as cast at full mana (Overflow); everyone else needs it full. */
	public static final double STORM_OVERFLOW = 0.75;
	/** Well: how much more meditation adds to regeneration. */
	public static final float WELL_MEDITATION = 2.0F;
	/** Ward: the share of a wound Mana Skin turns back into health; everyone else gets {@link Circles#MANA_SKIN_SHARE}. */
	public static final double WARD_SKIN_SHARE = 0.35;

	public enum Path {
		STORM("storm", "Path of the Storm", "Spells 8% stronger and cooldowns 5% shorter, but 20 less max mana. Overflow wakes at three-quarters mana.",
			new CircleVows.Effect(-20, 0, 1.08, 1, .95, 1)),
		WELL("well", "Path of the Well", "+40 max mana and +1 mana regen. Meditation fills you twice as fast.",
			new CircleVows.Effect(40, 1F, 1, 1, 1, 1)),
		WARD("ward", "Path of the Ward", "Spells cost 6% less and their effects last 15% longer. Mana Skin turns back 35% of a wound.",
			new CircleVows.Effect(0, 0, 1, .94, 1, 1.15));

		public final String id, name, text;
		public final CircleVows.Effect effect;

		Path(String id, String name, String text, CircleVows.Effect effect) {
			this.id = id;
			this.name = name;
			this.text = text;
			this.effect = effect;
		}

		/** The saved number: 0 is no path, so these never move. */
		public int saved() {
			return ordinal() + 1;
		}
	}

	/** The saved path, or null for none (or a malformed save). */
	public static Path of(int saved) {
		return saved >= 1 && saved <= Path.values().length ? Path.values()[saved - 1] : null;
	}

	public static Path byId(String id) {
		for (Path path : Path.values()) if (path.id.equals(id)) return path;
		return null;
	}

	/** The path that speaks for a heart with {@code activeCircles}: none while the Tenth Circle is unformed or cracked. */
	public static Path active(int saved, int activeCircles) {
		return activeCircles >= CIRCLE ? of(saved) : null;
	}

	public static CircleVows.Effect effect(int saved, int activeCircles) {
		Path path = active(saved, activeCircles);
		return path == null ? CircleVows.Effect.NONE : path.effect;
	}

	/** Whether a spell cast with {@code mana} of {@code max} counts as cast at full mana. */
	public static boolean overflowing(int saved, int activeCircles, float mana, float max) {
		double needed = active(saved, activeCircles) == Path.STORM ? max * STORM_OVERFLOW : max - .5;
		return mana >= needed;
	}

	public static float meditation(int saved, int activeCircles, float base) {
		return active(saved, activeCircles) == Path.WELL ? base * WELL_MEDITATION : base;
	}

	public static double skinShare(int saved, int activeCircles) {
		return active(saved, activeCircles) == Path.WARD ? WARD_SKIN_SHARE : Circles.MANA_SKIN_SHARE;
	}

	public enum Refusal { NOT_REACHED, ALREADY_WALKING, NOT_WALKING, LEVELS }

	public static Refusal take(int saved, int activeCircles) {
		if (activeCircles < CIRCLE) return Refusal.NOT_REACHED;
		return of(saved) != null ? Refusal.ALREADY_WALKING : null;
	}

	/** Leaving never needs the circle active, so a cracked path is never stuck. */
	public static Refusal leave(int saved, int levels, boolean creative) {
		if (of(saved) == null) return Refusal.NOT_WALKING;
		return creative || levels >= LEAVE_LEVELS ? null : Refusal.LEVELS;
	}
}
