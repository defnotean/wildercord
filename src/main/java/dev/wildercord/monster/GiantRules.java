package dev.wildercord.monster;

import dev.wildercord.cast.MoveDeck;
import net.minecraft.util.Mth;

/**
 * Roaming overworld giants (0.13), as plain numbers. Now and then a giant wakes somewhere 48 to 96 blocks from a traveller:
 * one of the wilds' own monsters grown three times over, wearing the variant of where it walks. In the cold a Frost
 * Matriarch (a Gloomstalker who leads every pack in the snow), in the heat an Ash Colossus (a Bramblewalker gone to cinders),
 * and anywhere between an Elder Bramblewalker. It wanders far, fights with a moveset of its own ({@link Bramble},
 * {@link Matriarch}) and never the same move twice running, shows a bar to everyone near it, never despawns, and drops its
 * Giant's Heart when it falls.
 */
public final class GiantRules {
	private GiantRules() {}

	/** The giants there are, by the variant of the land they wake in. */
	public enum Kind {
		/** A Frost Gloomstalker, mother of the snow packs. */
		MATRIARCH(MonsterVariantRules.Variant.FROST),
		/** An Ash Bramblewalker. */
		COLOSSUS(MonsterVariantRules.Variant.ASH),
		/** A plain Bramblewalker, old as the woods. */
		ELDER(MonsterVariantRules.Variant.NONE);

		public final MonsterVariantRules.Variant variant;

		Kind(MonsterVariantRules.Variant variant) {
			this.variant = variant;
		}

		/** Its name's key: {@code monster.wildercord.giant.matriarch}. */
		public String key() {
			return "monster.wildercord.giant." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** The giant that wakes in land of this temperature (snowy or not). */
	public static Kind kind(float temperature, boolean snowy) {
		return switch (MonsterVariantRules.of(temperature, snowy)) {
			case FROST -> Kind.MATRIARCH;
			case ASH -> Kind.COLOSSUS;
			case NONE -> Kind.ELDER;
		};
	}

	/** How often the wilds are checked for a waking giant (ticks), and the chance one wakes each time. */
	public static final int CHECK_TICKS = 20 * 60 * 10;
	public static final double CHANCE = 0.25;
	/** Where it wakes: this far from the traveller. */
	public static final int WAKE_NEAR = 48, WAKE_FAR = 96;
	/** No giant wakes within this many blocks of another. */
	public static final double APART = 192;

	/** How much bigger it is than its kind (added to its scale: three times as big), and how hard it is to shove. */
	public static final double SCALE = 2.0;
	public static final double KNOCKBACK_RESISTANCE = 0.9;
	/** How much tougher it is than a champion of its kind's tempering, and the least threat it's tempered to. */
	public static final double TOUGHNESS = 4.0;
	public static final int THREAT_MIN = 6;

	/** A plain stomp (for the tests and the guide's pictures): how far it reaches, what it deals at its feet, how hard it throws. */
	public static final double STOMP_RADIUS = 6;
	public static final float STOMP_DAMAGE = 10;
	public static final double STOMP_THROW = 1.1;

	/** What its stomp deals {@code distance} blocks away: all of it at its feet, half at the edge, nothing past it. */
	public static float stompDamage(double distance) {
		if (distance > STOMP_RADIUS) return 0;
		return (float) (STOMP_DAMAGE * (1 - 0.5 * Math.max(0, distance) / STOMP_RADIUS));
	}

	/** How often an idle giant sets off somewhere new (ticks), and how far it goes. */
	public static final int ROAM_INTERVAL = 600;
	public static final int ROAM_NEAR = 24, ROAM_FAR = 48;

	/** Whether it sets off somewhere new on tick {@code now}: only with nothing to fight. */
	public static boolean roams(boolean fighting, long now, int id) {
		return !fighting && Math.floorMod(now + id, ROAM_INTERVAL) == 0;
	}

	/** Its bar shows to everyone within this many blocks. */
	public static final double BAR_RANGE = 48;

	/** How many Giant's Hearts it drops: one, and a second one time in four. */
	public static int hearts(double roll) {
		return roll < 0.25 ? 2 : 1;
	}

	// ------------------------------------------------------------------ their fights

	/** How far above it someone must stand to be out of reach of everything but what it throws or calls down. */
	public static final double REACH_UP = 4;

	/** Whether someone {@code across} blocks off stands in an arc {@code arc} degrees wide and {@code radius} deep, {@code angle} degrees off its middle. */
	public static boolean inArc(double across, double angle, double radius, double arc) {
		return across <= radius && Math.abs(Mth.wrapDegrees(angle)) <= arc / 2;
	}

	/** Whether someone stands in a lane out ahead: {@code along} it, {@code side} off its middle. */
	public static boolean inLane(double along, double side, double length, double halfWidth) {
		return along >= 0 && along <= length && Math.abs(side) <= halfWidth;
	}

	/** Whether it goes roused: once, at half health. */
	public static boolean rouses(boolean roused, double health, double max) {
		return !roused && health <= max / 2;
	}

	/** Picks its next move by its weights, never the one it made last; -1 to close in instead. */
	static int choose(MoveDeck deck, int[] picks, double[] weights, double roll) {
		int move = deck.pick(picks, weights, roll);
		return move == deck.last() ? -1 : move;
	}

	/**
	 * The Elder Bramblewalker's and the Ash Colossus's moves. The stomp, a quake at its feet and a shockwave rolling out (jump
	 * it); the sweep of an arm across its front (get behind it or out past it); a clod torn up and hurled at where you stand (a
	 * ring closes on it: move); a root sent burrowing after you (outrun it, or turn it on a corner); a grab that holds whoever
	 * it catches up and squeezes until it throws them (hit it to break its grip), and stumbles forward if it misses; both fists
	 * hammered down a lane (step out of it) and stuck in the ground after. The Elder alone raises bramble patches under
	 * everyone near; the Colossus alone kindles the air about it into a blast of fire. Half beaten it roars and fights on
	 * roused: shorter rests, sometimes two moves running, and most of its moves twice over.
	 */
	public static final class Bramble {
		private Bramble() {}

		public static final int IDLE = 0, STOMP = 1, SWEEP = 2, HURL = 3, ROOTS = 4, GRAB = 5, CLUTCH = 6, STUMBLE = 7, HAMMER = 8,
			STUCK = 9, BRAMBLES = 10, KINDLE = 11, ROAR = 12;
		public static final int MOVES = ROAR + 1;
		public static final int[] PICKS = {STOMP, SWEEP, HURL, ROOTS, GRAB, HAMMER, BRAMBLES, KINDLE};
		public static final int MEMORY = 3;
		/** The pause between moves (ticks), and roused; and the chance roused that it goes straight into another. */
		public static final int REST = 30, ROUSED_REST = 16;
		public static final double CHAIN = 0.35;

		/** The stomp: the tell, the second's tell roused, the quake at its feet, and the shockwave rolling out after. */
		public static final int STOMP_TELL = 22, STOMP_AGAIN_TELL = 10;
		public static final double STOMP_INNER = 4.5, STOMP_REACH = 9;
		public static final float STOMP_HIT = 8;
		public static final double WAVE_FAR = 13, WAVE_SPEED = 0.9, WAVE_BAND = 0.7;
		public static final float WAVE_HIT = 5;

		/** The sweep: the tell, the backhand's roused, how long the swing takes, its arc and reach, and how hard it flings. */
		public static final int SWEEP_TELL = 18, SWEEP_BACK_TELL = 8, SWEEP_SWING = 6;
		public static final double SWEEP_NEAR = 2, SWEEP_FAR = 9.5, SWEEP_ARC = 150, SWEEP_THROW = 1.1;
		public static final float SWEEP_HIT = 7;

		/** The hurl: the tell, when its aim fixes before the throw, the clod's flight, the blast, its reach, and the second throw roused. */
		public static final int HURL_TELL = 26, HURL_LOCK = 8, HURL_FLIGHT = 14, HURL_AGAIN = 10, BURN_TICKS = 60;
		public static final double HURL_RADIUS = 3, HURL_NEAR = 7, HURL_FAR = 26;
		public static final float HURL_HIT = 7;

		/** The root: the tell, how fast it burrows and turns, how long it lasts, how near it catches, its reach, and the spread roused. */
		public static final int ROOTS_TELL = 20, ROOT_TICKS = 40;
		public static final double ROOT_SPEED = 0.6, ROOT_TURN = 4, ROOT_CATCH = 1.2, ROOTS_NEAR = 5, ROOTS_FAR = 24, ROOT_SPREAD = 30;
		public static final float ROOT_HIT = 4;

		/** The grab: the tell, the reach, its cone and range, and the stumble if it misses. */
		public static final int GRAB_TELL = 14, GRAB_REACH = 8, STUMBLE_TICKS = 24;
		public static final double GRAB_NEAR = 2, GRAB_RANGE = 6, GRAB_CONE = 0.7;
		/** Held: how long, how often it squeezes and for what, the throw at the end, and how much it must be hurt to let go (of its health). */
		public static final int CLUTCH_TICKS = 40, SQUEEZE_EVERY = 10;
		public static final float SQUEEZE_HIT = 3, THROWN_HIT = 5;
		public static final double CLUTCH_THROW = 1.6, CLUTCH_THROW_UP = 0.6, GRAB_BREAK = 0.04;

		/** The hammer: the tell, when its lane fixes, the lane, and how long its fists stay stuck after (roused, less). */
		public static final int HAMMER_TELL = 26, HAMMER_LOCK = 6, STUCK_TICKS = 40, ROUSED_STUCK_TICKS = 28;
		public static final double HAMMER_NEAR = 3, HAMMER_LENGTH = 11, HAMMER_WIDTH = 1.6;
		public static final float HAMMER_HIT = 10;

		/** The Elder's brambles: the tell, how long until they burst up, how wide, how long they linger, and how many at most. */
		public static final int BRAMBLES_TELL = 16, BRAMBLE_FUSE = 20, BRAMBLE_LINGER = 100, BRAMBLE_ROOT = 30, BRAMBLE_MAX = 4;
		public static final double BRAMBLE_RADIUS = 2.2, BRAMBLE_REACH = 20;
		public static final float BRAMBLE_HIT = 2, BRAMBLE_PRICK = 2;

		/** The Colossus's kindling: the tell, the blast's reach, and how long it sets you burning (seconds). */
		public static final int KINDLE_TELL = 30, KINDLE_BURN = 5;
		public static final double KINDLE_RADIUS = 7;
		public static final float KINDLE_HIT = 7;

		/** The roar at half health. */
		public static final int ROAR_TICKS = 40;
		public static final double ROAR_RADIUS = 8;

		/**
		 * How much it wants each of {@link #PICKS} with its prey {@code d} blocks off across the ground ({@code high} if out of
		 * reach above), with a crowd about it or not, the Elder or the Colossus.
		 */
		public static double[] weights(double d, boolean high, boolean crowded, boolean elder) {
			double[] w = new double[PICKS.length];
			if (high) {
				w[2] = d <= HURL_FAR ? 3 : 0;
				w[6] = elder && d <= BRAMBLE_REACH ? 2 : 0;
				return w;
			}
			w[0] = d <= 7 ? (crowded ? 4 : 3) : d <= STOMP_REACH ? 1.5 : 0;
			w[1] = d >= SWEEP_NEAR && d <= SWEEP_FAR ? 3 : 0;
			w[2] = d >= HURL_NEAR && d <= HURL_FAR ? (d > 12 ? 3 : 2) : 0;
			w[3] = d >= ROOTS_NEAR && d <= ROOTS_FAR ? 2.5 : 0;
			w[4] = d >= GRAB_NEAR && d <= GRAB_RANGE ? 2 : 0;
			w[5] = d >= HAMMER_NEAR && d <= HAMMER_LENGTH ? 2 : 0;
			w[6] = elder && d <= BRAMBLE_REACH ? (crowded ? 3 : 1.5) : 0;
			w[7] = !elder && d <= KINDLE_RADIUS ? (crowded ? 3.5 : 1.5) : 0;
			return w;
		}

		public static int choose(MoveDeck deck, double d, boolean high, boolean crowded, boolean elder, double roll) {
			return GiantRules.choose(deck, PICKS, weights(d, high, crowded, elder), roll);
		}

		/** Whether the shockwave, now {@code radius} out, catches someone {@code across} off (only on the ground: jump it). */
		public static boolean onWave(double across, double radius, boolean grounded) {
			return grounded && Math.abs(across - radius) <= WAVE_BAND;
		}

		/** Whether its reach closes on someone ahead of it: {@code dot} their way with its facing, {@code across} off. */
		public static boolean grabs(double across, double dot) {
			return across <= GRAB_RANGE && dot >= GRAB_CONE;
		}

		/** Whether its grip breaks, hurt {@code taken} since it caught them. */
		public static boolean breaks(double taken, double max) {
			return taken >= GRAB_BREAK * max;
		}

		/** What a blow deals it in the middle of a move: caught out it takes more, roaring less. */
		public static float hurt(float damage, int move) {
			return switch (move) {
				case STUCK -> damage * 1.5F;
				case STUMBLE, ROOTS -> damage * 1.25F;
				case ROAR -> damage * 0.5F;
				default -> damage;
			};
		}
	}

	/**
	 * The Frost Matriarch's moves. A pounce on where you stand (a ring follows you until it settles: get out of it), sprawled
	 * on the snow a while if it misses; a flurry of swipes as it walks in on you (back off); its tail flung round at whoever is
	 * behind it; a howl that calls her pups to her; a jet of frost breathed across her front (get behind her); vanishing into
	 * the snow to circle unseen and lunge out along a line (step off it, or hit her while she circles to drag her out); icicles
	 * called down on marks under everyone near. Half beaten she roars and fights on roused, quicker, with more of everything.
	 */
	public static final class Matriarch {
		private Matriarch() {}

		public static final int IDLE = 0, POUNCE = 1, SPRAWL = 2, SWIPES = 3, TAIL = 4, HOWL = 5, BREATH = 6, VANISH = 7, LUNGE = 8,
			ICICLES = 9, ROAR = 10;
		public static final int MOVES = ROAR + 1;
		public static final int[] PICKS = {POUNCE, SWIPES, TAIL, HOWL, BREATH, VANISH, ICICLES};
		public static final int MEMORY = 3;
		public static final int REST = 24, ROUSED_REST = 12;
		public static final double CHAIN = 0.35;

		/** The pounce: the tell, when the ring stops following, the leap, where it lands, its reach, and the sprawl after a miss. */
		public static final int POUNCE_TELL = 20, POUNCE_LOCK = 8, POUNCE_LEAP = 10, POUNCE_AGAIN_TELL = 12, SPRAWL_TICKS = 40;
		public static final double POUNCE_RADIUS = 2.5, POUNCE_NEAR = 4, POUNCE_FAR = 14;
		public static final float POUNCE_HIT = 8;

		/** The swipes: how many (roused, more), each one's tell, the arc, the reach, how far each walks her in. */
		public static final int SWIPE_COUNT = 3, ROUSED_SWIPES = 4, SWIPE_TELL = 8;
		public static final double SWIPE_ARC = 120, SWIPE_RADIUS = 4.5, SWIPE_STEP = 1.2, SWIPES_REACH = 5;
		public static final float SWIPE_HIT = 4;

		/** The tail: the tell, the cone behind her and its reach, and how hard it throws. */
		public static final int TAIL_TELL = 12;
		public static final double TAIL_ARC = 140, TAIL_RADIUS = 5.5, TAIL_REACH = 6, TAIL_THROW = 1.2, BEHIND = -0.2;
		public static final float TAIL_HIT = 6;

		/** The howl: the tell, how far it chills, how many pups it calls (roused, more), and how many she keeps at most. */
		public static final int HOWL_TELL = 24, PUPS = 2, ROUSED_PUPS = 3, MAX_PUPS = 4;
		public static final double HOWL_RADIUS = 10, PUP_RANGE = 32;

		/** The breath: the tell, how long it sweeps, how wide the sweep and the jet, its reach, and how often it bites. */
		public static final int BREATH_TELL = 16, BREATH_TICKS = 30, BREATH_EVERY = 5;
		public static final double BREATH_SWEEP = 45, BREATH_WIDTH = 20, BREATH_NEAR = 3, BREATH_RADIUS = 9;
		public static final float BREATH_HIT = 1.5F;

		/** The vanishing: the tell, how long she circles at least and at most, how wide, the stagger if torn out, its reach. */
		public static final int VANISH_TELL = 10, CIRCLE_MIN = 40, CIRCLE_MAX = 60, TORN_TICKS = 24;
		public static final double CIRCLE_RADIUS = 8, VANISH_NEAR = 6, VANISH_FAR = 20;
		/** The lunge out of it: the line's tell, how fast she dashes, how near she catches, and how far she goes at most. */
		public static final int LUNGE_TELL = 12, LUNGE_TICKS = 16;
		public static final double LUNGE_SPEED = 1.2, LUNGE_CATCH = 1.8;
		public static final float LUNGE_HIT = 8;

		/** The icicles: the tell, how many waves, how often, the marks' width, how long they hang, how far they reach and how many. */
		public static final int ICICLES_TELL = 14, ICICLE_WAVES = 5, ICICLE_EVERY = 8, ICICLE_FALL = 14, ICICLE_MAX = 4;
		public static final double ICICLE_RADIUS = 1.8, ICICLE_REACH = 20;
		public static final float ICICLE_HIT = 5;

		/** The roar at half health. */
		public static final int ROAR_TICKS = 36;
		public static final double ROAR_RADIUS = 8;

		/**
		 * How much she wants each of {@link #PICKS} with her prey {@code d} blocks off across the ground ({@code high} if out of
		 * reach above), {@code behind} her or not, with a crowd about her or not, and {@code pups} of hers about.
		 */
		public static double[] weights(double d, boolean high, boolean behind, boolean crowded, int pups) {
			double[] w = new double[PICKS.length];
			double howl = pups < 2 ? 1.5 : pups < MAX_PUPS ? 0.8 : 0;
			if (high) {
				w[3] = howl;
				w[6] = d <= ICICLE_REACH ? 3 : 0;
				return w;
			}
			w[0] = d >= POUNCE_NEAR && d <= POUNCE_FAR ? 3 : 0;
			w[1] = d <= SWIPES_REACH && !behind ? 3 : 0;
			w[2] = behind && d <= TAIL_REACH ? 4 : 0;
			w[3] = d <= PUP_RANGE ? howl : 0;
			w[4] = d >= BREATH_NEAR && d <= BREATH_RADIUS && !behind ? 2.5 : 0;
			w[5] = d >= VANISH_NEAR && d <= VANISH_FAR ? 2 : 0;
			w[6] = d <= ICICLE_REACH ? (crowded ? 2.5 : 1.5) : 0;
			return w;
		}

		public static int choose(MoveDeck deck, double d, boolean high, boolean behind, boolean crowded, int pups, double roll) {
			return GiantRules.choose(deck, PICKS, weights(d, high, behind, crowded, pups), roll);
		}

		/** Whether someone is behind her: {@code dot} their way with her facing. */
		public static boolean behind(double dot) {
			return dot < BEHIND;
		}

		/** Where the breath points {@code t} ticks into its sweep, degrees off where she faced; back the other way {@code back}. */
		public static double breathAngle(int t, boolean back) {
			double f = Mth.clamp(t / (double) BREATH_TICKS, 0, 1);
			double a = -BREATH_SWEEP + 2 * BREATH_SWEEP * f;
			return back ? -a : a;
		}

		/** What a blow deals her in the middle of a move: sprawled she takes more, roaring less. */
		public static float hurt(float damage, int move) {
			return switch (move) {
				case SPRAWL -> damage * 1.5F;
				case ROAR -> damage * 0.5F;
				default -> damage;
			};
		}
	}
}
