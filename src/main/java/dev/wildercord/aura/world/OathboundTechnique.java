package dev.wildercord.aura.world;

/**
 * One scripted Oathbound attack: when it winds up, how long it stays committed, which volume it sweeps, what it does to
 * whoever is caught in that volume, and which pose archetype the client rig plays.
 *
 * <p>Kept as pure data in the same package as the per-knight {@code *Rules} classes. The rules unit tests walk every
 * declared technique, so an attack cannot ship without a legal tell, active window, recovery, reach, and damage.</p>
 */
public record OathboundTechnique(
		int id,
		String key,
		Shape shape,
		Motion motion,
		Archetype archetype,
		int tell,
		int active,
		int recovery,
		int hitDelay,
		double damage,
		double knockback,
		double reach,
		double inner,
		double half,
		double lift,
		Effect effect,
		int effectTicks,
		int color,
		double speed,
		int hits,
		boolean empoweredOnly
) {
	/** The volume an attack sweeps once it lands. */
	public enum Shape {
		/** Frontal cone: {@code reach} deep and {@code half} degrees either side of the facing. */
		CONE,
		/** Frontal cone wide enough to be a full body turn. */
		WIDE_ARC,
		/** Forward rectangle: {@code reach} long and {@code half} wide either side. */
		LINE,
		/** Full circle out to {@code reach}. */
		RADIUS,
		/** Annulus between {@code inner} and {@code reach}. */
		RING,
		/** Close burst inside {@code inner} plus four orthogonal arms out to {@code reach}. */
		CROSS,
		/** Rectangle that runs both forward and backward: a weapon swept behind the knight. */
		TWIN_LINE,
		/** Everything inside {@code reach} is dragged toward the knight. */
		PULL_RADIUS,
		/** Forward rectangle whose occupants are dragged toward the knight. */
		PULL_LINE,
		/** Annulus swept repeatedly as the knight turns. */
		ORBITAL,
		/** Repeated frontal cones, one per {@code hits}. */
		MULTI_HIT
	}

	/** How the knight's body travels while the attack is committed. */
	public enum Motion {
		PLANTED,
		DASH,
		LEAP,
		BACKSTEP
	}

	/** What a caught target suffers beyond the blow itself. */
	public enum Effect {
		NONE,
		SLOW,
		WEAKNESS,
		BLIND,
		WITHER,
		LAUNCH,
		FEAR
	}

	/** The shared client pose vocabulary. Each knight's rig interprets it with its own weapon and proportions. */
	public enum Archetype {
		OVERHEAD_SLAM,
		HORIZONTAL_SWEEP,
		DIAGONAL_SLASH,
		THRUST,
		UPPERCUT,
		SPIN_SWEEP,
		LEAP_SLAM,
		SHIELD_PUSH,
		STOMP,
		ROAR,
		OVERHAND_THROW,
		GUARD_BRACE,
		CAST_RAISE,
		FLURRY,
		BACKSTEP,
		WEAPON_TWIRL,
		KNEEL_STRIKE,
		CROSS_CUT
	}

	/** Total ticks the move runs: windup, committed window, then recovery. */
	public int duration() {
		return tell + active + recovery;
	}

	/** Ticks of committed motion after the windup. */
	public int committedTicks() {
		return active;
	}

	/** Whether this attack can be rolled right now at {@code distance}. */
	public boolean inRange(double distance) {
		return switch (shape) {
			case CONE, WIDE_ARC, MULTI_HIT, LINE, PULL_LINE -> distance <= reach + 0.9;
			case RADIUS, RING, PULL_RADIUS, ORBITAL -> distance <= reach;
			case TWIN_LINE -> distance <= reach;
			case CROSS -> distance <= reach;
		};
	}

	/** Builder so a table of seventeen attacks stays readable. */
	public static Builder b(int id, String key, Shape shape, Archetype archetype) {
		return new Builder(id, key, shape, archetype);
	}

	public static final class Builder {
		private final int id;
		private final String key;
		private final Shape shape;
		private final Archetype archetype;
		private Motion motion = Motion.PLANTED;
		private int tell = 10;
		private int active = 6;
		private int recovery = 10;
		private int hitDelay = 2;
		private double damage = 8.0;
		private double knockback = 0.5;
		private double reach = 3.2;
		private double inner = 0.0;
		private double half = 60.0;
		private double lift = 0.0;
		private Effect effect = Effect.NONE;
		private int effectTicks = 0;
		private int color = 0xFFD048;
		private double speed = 0.0;
		private int hits = 1;
		private boolean empoweredOnly = false;

		private Builder(int id, String key, Shape shape, Archetype archetype) {
			this.id = id;
			this.key = key;
			this.shape = shape;
			this.archetype = archetype;
		}

		public Builder motion(Motion v) { motion = v; return this; }
		public Builder tell(int v) { tell = v; return this; }
		public Builder active(int v) { active = v; return this; }
		public Builder recovery(int v) { recovery = v; return this; }
		public Builder hitDelay(int v) { hitDelay = v; return this; }
		public Builder damage(double v) { damage = v; return this; }
		public Builder knockback(double v) { knockback = v; return this; }
		public Builder reach(double v) { reach = v; return this; }
		public Builder inner(double v) { inner = v; return this; }
		public Builder half(double v) { half = v; return this; }
		public Builder lift(double v) { lift = v; return this; }
		public Builder effect(Effect v, int ticks) { effect = v; effectTicks = ticks; return this; }
		public Builder color(int v) { color = v; return this; }
		public Builder speed(double v) { speed = v; return this; }
		public Builder hits(int v) { hits = Math.max(1, v); return this; }
		public Builder empoweredOnly() { empoweredOnly = true; return this; }

		public OathboundTechnique build() {
			return new OathboundTechnique(id, key, shape, motion, archetype, tell, active, recovery, hitDelay,
					damage, knockback, reach, inner, half, lift, effect, effectTicks, color, speed, hits,
					empoweredOnly);
		}
	}
}
