package dev.wildercord.aura;

/**
 * The numbers of aura's feel, the look and sound every technique shares (see {@link AuraFx}): the strokes a blade's trail can
 * cut, how a trail grows with the stage, how hard an impact lands (hit-stop, camera nudge, flash), how the body's aura flares in
 * a fight, and how long a technique's banner shows. Pure, so the server, the client and the unit tests read the same numbers.
 *
 * <p>Nothing here decides what happens in a fight: these are only how it looks and sounds.</p>
 */
public final class AuraFxRules {
	private AuraFxRules() {}

	// ------------------------------------------------------------------ strokes

	/**
	 * The shape a blade's trail cuts. Each is an arc of a circle round a point near the swordsman's shoulder, in a plane through
	 * the way they face, swept from one angle to another (0 is straight ahead; positive toward the plane's side axis, which leans
	 * up from the blade hand by the stroke's tilt). {@link #THRUST} is a straight streak instead; {@link #CROSS} is a cut and its
	 * mirror a moment apart.
	 */
	public enum Stroke {
		/** The ordinary cut: from high on the blade's side, down across the body to low on the other. */
		CUT(38, 80, -70, 1.25, 0.15, 1.55, 3, 9, 0),
		/** Upward: from low on the off side to high on the blade's (a lifting cut). */
		RISING(38, -75, 80, 1.15, 0.15, 1.55, 3, 9, 0),
		/** Overhead: straight down in front, from above the head to the feet (a leaping cut). Your own view keeps its end, low. */
		FALLING(82, 105, -45, 1.35, 0.1, 1.5, 3, 9, 0.5),
		/** Wide and level, round the front at the waist (Flow's sweep). */
		SWEEP(0, 110, -110, 1.0, -0.2, 2.2, 4, 11, 0),
		/** Low and level across the legs, rising a little as it goes (a low swing, struck crouching). */
		LOW(-10, 95, -95, 0.62, 0.1, 1.7, 3, 9, 0),
		/** A fast level draw at the chest, from the off side across to the blade's (a slash leaving the blade). */
		DRAW(8, -95, 95, 1.2, 0.1, 1.7, 2, 8, 0),
		/** Straight ahead: a lance of light from the hand out past the blade's reach (a spear's thrust, a running cut, a step cut). */
		THRUST(0, 0, 0, 1.25, 0.45, 2.6, 2, 7, 0),
		/** A whole turn round the body at the waist (the Final Art). */
		SPIN(0, 0, 360, 0.95, 0.0, 2.4, 5, 13, 0),
		/** A cut, and its mirror two ticks later: an X in front (a counter). */
		CROSS(38, 80, -70, 1.25, 0.15, 1.5, 3, 11, 0);

		/** How far the plane's side axis leans up from the blade hand (degrees): 0 level, 90 straight up. */
		public final double tilt;
		/** Where the head of the trail starts and ends (degrees round the arc; 0 straight ahead). */
		public final double from;
		public final double to;
		/** The arc's centre: its height above the feet and how far ahead of the body it sits (blocks). */
		public final double height;
		public final double ahead;
		/** The arc's radius (blocks): about the reach of an arm and a blade. For {@link #THRUST}, the streak's length. */
		public final double radius;
		/** Ticks for the head to cross the arc, and the trail's whole life. */
		public final int sweep;
		public final int life;
		/**
		 * Where your own first-person view of it starts, as a share of the arc (it keeps {@link #OWN_SPAN} from there): 0 for the start,
		 * later for a stroke whose start would cross the middle of the view (a falling cut keeps its end, coming down past the bottom).
		 */
		public final double ownStart;

		Stroke(double tilt, double from, double to, double height, double ahead, double radius, int sweep, int life, double ownStart) {
			this.tilt = tilt;
			this.from = from;
			this.to = to;
			this.height = height;
			this.ahead = ahead;
			this.radius = radius;
			this.sweep = sweep;
			this.life = life;
			this.ownStart = ownStart;
		}

		/** Your own view's first and last angles round the arc (degrees). */
		public double ownFrom() {
			return from + (to - from) * ownStart;
		}

		public double ownTo() {
			return from + (to - from) * Math.min(1, ownStart + OWN_SPAN);
		}

		/** How far round the arc the head travels (degrees, unsigned). */
		public double span() {
			return Math.abs(to - from);
		}

		public static Stroke of(int ordinal) {
			Stroke[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : CUT;
		}
	}

	/**
	 * What an ordinary swing cuts, from what it was (its sword string marks, {@link SwordString.Token#bit}): a spear's thrust, a
	 * running swing or a step cut goes straight ahead, a leaping swing comes down from above, a low swing cuts low across the legs,
	 * a counter crosses, and anything else is the ordinary cut. (A swing that sweeps with Flow is drawn as {@link Stroke#SWEEP},
	 * whatever it was: see {@link #sweeps}.)
	 */
	public static Stroke stroke(int marks, boolean thrust) {
		if (thrust || SwordString.Token.RUN.fits(marks) || SwordString.Token.STEP.fits(marks)) {
			return Stroke.THRUST;
		}
		if (SwordString.Token.COUNTER.fits(marks)) {
			return Stroke.CROSS;
		}
		if (SwordString.Token.LEAP.fits(marks)) {
			return Stroke.FALLING;
		}
		if (SwordString.Token.LOW.fits(marks)) {
			return Stroke.LOW;
		}
		return Stroke.CUT;
	}

	/**
	 * Whether a swing sweeps with Flow, as vanilla's sweep judges it with Flow's aura (see {@code mixin.PlayerAuraMixin}): Flow or
	 * above with aura to coat the blow, a full swing at a creature, on the ground, not sprinting and nearly still. The swinger's
	 * client asks this to draw the sweep at once; the server's own sweep tells everyone else.
	 *
	 * @param speed     how fast the swinger is moving (blocks a tick, level)
	 * @param walkSpeed their movement speed attribute (a sweep needs them under two and a half times it)
	 */
	public static boolean sweeps(int stage, boolean coated, boolean atCreature, float strength, boolean onGround, boolean sprinting, double speed,
			double walkSpeed) {
		return stage >= AuraRules.FLOW && coated && atCreature && strength >= AuraRules.FULL_SWING - 1.0E-4 && onGround && !sprinting
			&& speed < walkSpeed * 2.5;
	}

	/** A sweep the server confirms this soon (ticks) after one the swinger's client drew is the same sweep: drawn once. */
	public static final int SWEEP_ECHO = 4;

	/** Swings this close together (ticks) are a run of cuts: each cuts back the other way from the one before. */
	public static final int COMBO_TICKS = 24;

	/** Whether a swing at {@code now} cuts back the other way: it follows the last within {@link #COMBO_TICKS} and that one didn't. */
	public static boolean mirrored(long lastSwing, boolean lastMirrored, long now) {
		return now - lastSwing <= COMBO_TICKS && now >= lastSwing && !lastMirrored;
	}

	// ------------------------------------------------------------------ how a trail looks at each stage

	/** The trail's glow, blocks across, at {@code stage} (Glow to Sovereign): wider as aura grows. */
	public static double trailWidth(int stage) {
		return switch (AuraRules.clampStage(stage)) {
			case AuraRules.NONE, AuraRules.GLOW -> 0.14;
			case AuraRules.FLOW -> 0.19;
			case AuraRules.EDGE -> 0.24;
			case AuraRules.FORM -> 0.29;
			default -> 0.35;
		};
	}

	/** How much of its arc the ribbon trails behind its head (0 to 1): longer as aura grows. */
	public static double trailTail(int stage) {
		return 0.35 + 0.1 * Math.max(AuraRules.GLOW, AuraRules.clampStage(stage));
	}

	/** The glow's strength (0 to 1) at {@code stage}; its white-hot core is a little stronger. */
	public static float trailAlpha(int stage) {
		return (float) (0.3 + 0.07 * Math.max(AuraRules.GLOW, AuraRules.clampStage(stage)));
	}

	/** The white-hot core's width, as a share of the glow's. */
	public static final double CORE_SHARE = 0.28;

	/*
	 * Your own trail in first person: thin, short and low. It keeps part of its arc (the start, for most strokes), drops below your
	 * eye line, laid in your view as you looked when you cut (so looking down at a foe doesn't bring it up through the middle), fades
	 * out near the middle of the view, and lies faint, so a fight stays readable from inside it. Third person and everyone else see the
	 * full trail.
	 */

	/** How much thinner your own first-person trail is. */
	public static final double OWN_WIDTH = 0.34;
	/** How much fainter. */
	public static final float OWN_ALPHA = 0.55F;
	/** How far below where the full trail would run it drops (blocks). */
	public static final double OWN_DROP = 0.42;
	/** How much of its arc it keeps (from {@link Stroke#ownStart}). */
	public static final double OWN_SPAN = 0.55;
	/** How much shorter it lives. */
	public static final double OWN_LIFE = 0.7;
	/** How far toward the blade hand it moves (blocks), so it runs down the side of the view and not through its middle. */
	public static final double OWN_ASIDE = 0.22;
	/** Your own trail is gone this near the middle of your view (degrees from it), and whole from {@link #OWN_CLEAR_FULL}. */
	public static final double OWN_CLEAR = 12;
	public static final double OWN_CLEAR_FULL = 22;

	/** How much of your own trail shows {@code degrees} from the middle of your view (0 to 1). */
	public static float ownClear(double degrees) {
		double k = Math.clamp((degrees - OWN_CLEAR) / (OWN_CLEAR_FULL - OWN_CLEAR), 0.0, 1.0);
		return (float) (k * k * (3 - 2 * k));
	}

	/** The trail's extras by stage: motes shed (Flow), a crisp edge (Edge), an echo (Form), sparks off its tip (Sovereign). */
	public static boolean sheds(int stage) {
		return stage >= AuraRules.FLOW;
	}

	public static boolean edged(int stage) {
		return stage >= AuraRules.EDGE;
	}

	public static boolean echoes(int stage) {
		return stage >= AuraRules.FORM;
	}

	public static boolean sparks(int stage) {
		return stage >= AuraRules.SOVEREIGN;
	}

	// ------------------------------------------------------------------ impacts

	/** How hard a blow lands, to the eye: a light touch, a full swing, a heavy strike (an art, a slash), a grand one (the Final Art). */
	public enum Weight {
		LIGHT(0, 0.0F, 0.35F),
		FULL(45, 0.35F, 0.6F),
		HEAVY(70, 0.6F, 0.9F),
		GRAND(110, 1.0F, 1.3F);

		/** The hit-stop, milliseconds of real time (frames, not ticks): the blade bites and the moment holds. */
		public final int hitStop;
		/** The camera's nudge, degrees. */
		public final float nudge;
		/** The flash, blocks across, at Glow. */
		public final float flash;

		Weight(int hitStop, float nudge, float flash) {
			this.hitStop = hitStop;
			this.nudge = nudge;
			this.flash = flash;
		}

		public static Weight of(int ordinal) {
			Weight[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LIGHT;
		}
	}

	/** What a coated blow weighs: a full swing lands {@link Weight#FULL} (a critical one {@link Weight#HEAVY}), anything less light. */
	public static Weight blow(float swing, boolean critical) {
		if (swing < AuraRules.FULL_SWING - 1.0E-4) {
			return Weight.LIGHT;
		}
		return critical ? Weight.HEAVY : Weight.FULL;
	}

	/** The flash's size at {@code stage}: a little bigger at each stage. */
	public static float flash(Weight weight, int stage) {
		return weight.flash * (1.0F + 0.06F * (Math.max(AuraRules.GLOW, AuraRules.clampStage(stage)) - 1));
	}

	/** Hit-stops this close together (milliseconds) are one: a sweep through four foes holds once, not four times. */
	public static final int HIT_STOP_GAP = 150;

	/**
	 * How long a hit-stop holds (ms), scaled by the player's impact setting ({@code scale}: 1 full, 0.5 soft, 0 off) and never more
	 * than the heaviest weight's.
	 */
	public static int hitStop(Weight weight, double scale) {
		return (int) Math.round(Math.max(0, Math.min(1, scale)) * weight.hitStop);
	}

	/** The nudge's direction and how fast it dies away: a quick shove, gone in about this long (ms). */
	public static final int NUDGE_MILLIS = 140;

	// ------------------------------------------------------------------ the body's aura

	/** How long the body's aura flares after a blow given or taken (ticks), and when it's renewed on the next (ticks left). */
	public static final int FIGHT_TICKS = 100;
	public static final int FIGHT_RENEW = 60;

	/** Whether a fight's flare should be renewed now: it has run out, or has less than {@link #FIGHT_RENEW} ticks left. */
	public static boolean renewFight(long fightUntil, long now) {
		return fightUntil - now < FIGHT_RENEW;
	}

	/** At rest the body's aura is calm; in a fight it flares; a surge (an art, a perfect guard) blazes for a moment over that. */
	public static final float IDLE = 0.35F;
	public static final float FIGHTING = 0.8F;

	/**
	 * How strongly the body's aura shows (0 at nothing, 1 a full flare, up to 1.6 in a surge): calm at rest, flaring in a fight,
	 * a surge on top dying away, and all of it faint while the aura is too low to coat a blow.
	 *
	 * @param surge what is left of a surge (0 to 1, already dying away)
	 */
	public static float intensity(boolean lit, boolean fighting, float surge) {
		float base = fighting ? FIGHTING : IDLE;
		float s = Math.max(0, Math.min(1, surge));
		float level = Math.max(base, base + 0.6F * s);
		return lit ? level : level * 0.4F;
	}

	/** What is left of a surge {@code age} ticks into its {@code ticks}: easing away to nothing. */
	public static float surgeLeft(float strength, double age, int ticks) {
		if (ticks <= 0 || age < 0 || age >= ticks) {
			return 0;
		}
		double f = 1 - age / ticks;
		return (float) (Math.max(0, Math.min(1, strength)) * f * f);
	}

	// ------------------------------------------------------------------ banners

	/** What a banner names: one of the arts (I to IV), something grand (the Final Art, a Dominion), or a finisher. */
	public enum BannerKind {
		ART(44),
		GRAND(64),
		FINISHER(56);

		/** How long it shows (ticks). */
		public final int ticks;

		BannerKind(int ticks) {
			this.ticks = ticks;
		}

		public static BannerKind of(int ordinal) {
			BannerKind[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : ART;
		}
	}

	/** Ticks a banner takes to slide in, and to fade away at the end. */
	public static final int BANNER_IN = 4;
	public static final int BANNER_OUT = 9;

	/** How far a banner has slid in (0 to 1) and how strongly it shows (0 to 1), {@code age} ticks into its {@code ticks}. */
	public static float bannerSlide(double age) {
		double f = Math.max(0, Math.min(1, age / BANNER_IN));
		return (float) (1 - (1 - f) * (1 - f) * (1 - f));
	}

	public static float bannerAlpha(double age, int ticks) {
		if (age < 0 || age >= ticks) {
			return 0;
		}
		double in = Math.min(1, age / 2.0);
		double out = Math.min(1, (ticks - age) / BANNER_OUT);
		return (float) Math.max(0, Math.min(in, out));
	}

	/** The ordinal of an art opening at {@code stage}: First Art (Glow) to Final Art (Sovereign). */
	public static String ordinalKey(int stage) {
		return "aura.wildercord.banner.ordinal." + Math.max(AuraRules.GLOW, Math.min(AuraRules.SOVEREIGN, AuraRules.clampStage(stage)));
	}

	// ------------------------------------------------------------------ range

	/** How far away an onlooker sees another's trails and impacts (blocks), and their banners. */
	public static final double SEEN = 64;
	public static final double BANNER_SEEN = 40;
}
