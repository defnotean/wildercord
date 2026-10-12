package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * The Drowned Bell-Warden, the sunken chapel's guardian: its states, its wake-up, the hall it keeps and how it fights. It kneels in
 * vigil before the fallen bell-frame with its own cracked bell over its head, until someone walks far enough up the nave; then it
 * rises, tolls and the fight is on. A step below a Master, like the serpent.
 * <p>
 * It never makes the same move twice running, and leans toward what suits where its prey stands: its clapper and its bell for
 * anyone close, its chain, the floor and its charge for anyone keeping away. Each move has a tell of its own and a way out of it.
 * Half beaten, it tears its bell open and fights on cracked: quicker, its moves in pairs, and most of them harder.
 */
public final class BellWardenRules {
	private BellWardenRules() {}

	/** Kneeling in vigil before the bell-frame. */
	public static final int DORMANT = 0;
	/** Getting up off its knees, tolling as it stands; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On its feet, tracking. */
	public static final int WATCH = 2;
	/** Kneeling again once the chapel has gone quiet: the rise, played backwards. */
	public static final int KNEEL = 3;
	/** Feet planted and twisted far round with the clapper trailing, it whirls once all the way round: get out of the circle it scrapes. */
	public static final int SWING = 4;
	/** The clapper raised overhead in both hands and brought down along a narrow line, splitting the floor beyond: sidestep it. */
	public static final int HAMMER = 5;
	/** Both fists punch the floor, and water spouts up a moment later wherever it bubbles: step off the marks. */
	public static final int SLAM = 6;
	/** Its bell lowered like a ram and its whole weight run along a fixed line: sidestep it, ideally with a pillar behind you. */
	public static final int CHARGE = 7;
	/** It heaves itself back out from under a blow. */
	public static final int BACKSTEP = 8;
	/** Spent after an attack, its bell tipped back and its chest bare: the opening. */
	public static final int RECOVER = 9;
	/** Hurt too fast, it hunches down under its bell, which takes nearly everything. */
	public static final int SHELL = 10;
	/** It strikes its own bell, and a ring rolls out along the floor from its feet: jump it. */
	public static final int TOLL = 11;
	/** The chain on its forearm whirled overhead and lashed out along a line, dragging whoever it catches in: sidestep it, or get in close. */
	public static final int LASH = 12;
	/** It lifts the bell off its own head and brings it down mouth first over a marked circle: leave the circle, then hit its bare head. */
	public static final int COVER = 13;
	/** Hands on its bell's rim, it makes it hum, and a cone of drowned sound sweeps after its prey: get behind it, or behind a pillar. */
	public static final int PEAL = 14;
	/** Water pours from its bell's lip and the whole floor drags toward it before its fists come down: walk out against the pull. */
	public static final int UNDERTOW = 15;
	/** Its clapper stuck fast in the floor after a hammer blow while it hauls on it: the opening. */
	public static final int STUCK = 16;
	/** Its bell rung on a wall or a pillar at the end of a charge, it stands swaying: the long opening. */
	public static final int DAZED = 17;
	/** Half beaten, it tears its bell open; once only. */
	public static final int CRACK = 18;

	/** The moves it picks from when it is free to choose. */
	public static final int[] PICKS = {SWING, HAMMER, SLAM, CHARGE, TOLL, LASH, COVER, PEAL, UNDERTOW};
	/** How many of its last moves it remembers, so as not to fall back into a habit. */
	public static final int MEMORY = 5;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.DREAD;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.24);

	/** Anyone in the hall this close to its vigil, horizontally, wakes it: about halfway up the nave. */
	public static final double WAKE_RADIUS = 18;
	/** How far above or below its knees a player still counts as on the chapel floor. */
	public static final double WAKE_HEIGHT = 5;
	public static final int RISE_TICKS = 46;
	/** When in its rise the bell tolls: as it straightens. */
	public static final int TOLL_AT = RISE_TICKS / 2;

	/** The hall it keeps is the chapel, nave and apse. It is put back if it strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the hall, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 10;
	/** Ticks between the kills it makes keeping its hall. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight it walks back to its vigil after this long, and kneels at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have knelt again unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 3.5;
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway: dead weight, not edges. */
	public static final double PIERCE = 0.4;
	/** Most degrees it turns a tick while it tracks its prey through a tell, before the aim locks. */
	public static final float TURN = 24;

	/**
	 * The whirl: its tell (shorter when it follows another move), how long the spin takes, how far the clapper reaches, what it does,
	 * and the dizzy beat after. Cracked, it whirls twice, a step toward its prey between, the second tell {@link #SWING_RETELL}.
	 * Get out past the scraped circle before it goes.
	 */
	public static final int SWING_TELL = 14, SWING_TELL_CHAINED = 9, SWING_SPIN = 5, SWING_RETELL = 8, SWING_RECOVER = 18;
	public static final double SWING_REACH = 5.5, SWING_DAMAGE = TIER.hit(18), SWING_STEP = 2.5;
	/**
	 * The hammer: its tell, when its aim locks, how far the blow lands and how wide, what it does; then the split running on through
	 * the floor to {@link #SPLIT_REACH} over {@link #SPLIT_TICKS}, narrower and weaker. The clapper sticks fast for {@link #STUCK_TICKS}.
	 * Cracked, it strikes twice, the second tell {@link #HAMMER_RETELL}, and only the second sticks. Sidestep the line.
	 */
	public static final int HAMMER_TELL = 16, HAMMER_TELL_CHAINED = 10, HAMMER_LOCK = 4, HAMMER_RETELL = 10, SPLIT_TICKS = 4, STUCK_TICKS = 36;
	public static final double HAMMER_REACH = 5, HAMMER_HALF = 1.4, HAMMER_DAMAGE = TIER.hit(24), SPLIT_REACH = 9, SPLIT_HALF = 0.9, SPLIT_DAMAGE = TIER.hit(12);
	/**
	 * The floor-breaker: its tell, how long its fists stay down, and its rest; what the fists do up close; how long the marked floor
	 * bubbles before it spouts, how many marks fall scattered round each of its prey and how far, how wide and high a spout is, and
	 * what it does. Cracked, a second volley comes up wherever its prey stood when the first burst. Step off the marks.
	 */
	public static final int SLAM_TELL = 14, SLAM_TELL_CHAINED = 9, SLAM_HOLD = 6, SLAM_RECOVER = 14, SPOUT_DELAY = 14, SPOUT_SCATTER = 4;
	public static final double FIST_DAMAGE = TIER.hit(14), SPOUT_SPREAD = 4.5, SPOUT_RADIUS = 1.3, SPOUT_HEIGHT = 4, SPOUT_DAMAGE = TIER.hit(16), SPOUT_LIFT = 0.9;
	/**
	 * The charge: its tell, when its aim locks, how long it runs and how far, what it does; the stumble when it runs out into open floor,
	 * and the daze when it rings its bell on a wall or a pillar. Cracked, from open floor it turns and charges once more, the second tell
	 * {@link #CHARGE_RETELL}. Sidestep it with a pillar at your back.
	 */
	public static final int CHARGE_TELL = 16, CHARGE_TELL_CHAINED = 11, CHARGE_LOCK = 4, CHARGE_TICKS = 10, CHARGE_RETELL = 11, CHARGE_RECOVER = 18, DAZED_TICKS = 60;
	public static final double CHARGE_REACH = 12, CHARGE_DAMAGE = TIER.hit(24);
	/**
	 * The toll: its tell, its rest after; the ring rolling out from its feet, how fast, how far and how high off the floor it reaches,
	 * and what it does. Cracked, a second ring follows {@link #TOLL_AGAIN} ticks later, faster. Jump it.
	 */
	public static final int TOLL_TELL = 14, TOLL_TELL_CHAINED = 10, TOLL_AGAIN = 10, TOLL_RECOVER = 16;
	public static final double WAVE_SPEED = 0.55, WAVE_FAST = 0.8, WAVE_REACH = 13, WAVE_HEIGHT = 0.9, TOLL_DAMAGE = TIER.hit(14);
	/**
	 * The lash: its tell, when its aim locks, how long the chain stays out, its rest after; where along its aim the chain bites and how
	 * wide, what it does, and how hard it drags whoever it catches back toward its feet. Cracked, it lashes again, the second tell
	 * {@link #LASH_RETELL}. Sidestep it, or stand inside its reach.
	 */
	public static final int LASH_TELL = 14, LASH_TELL_CHAINED = 10, LASH_LOCK = 4, LASH_HOLD = 4, LASH_RETELL = 9, LASH_RECOVER = 14;
	public static final double LASH_NEAR = 2.5, LASH_REACH = 10, LASH_HALF = 1.2, LASH_DAMAGE = TIER.hit(18), LASH_TUG = 0.16, LASH_TUG_MOST = 1.6;
	/**
	 * The bell brought down: its tell, when its aim locks, how long the bell stays down while it hauls it back up with its head bare;
	 * how near and far ahead it lands, how wide its mouth is, what it does, and (cracked) the ring the landing sends out; then a
	 * short rest once it has it back on. Leave the marked circle, then go for its head.
	 */
	public static final int COVER_TELL = 18, COVER_TELL_CHAINED = 12, COVER_LOCK = 4, COVER_HOLD = 30, COVER_RECOVER = 8;
	public static final double COVER_NEAR = 2.5, COVER_FAR = 4.5, COVER_RADIUS = 2.4, COVER_DAMAGE = TIER.hit(30), COVER_RING = TIER.hit(12);
	/**
	 * The peal: its tell, how long the bell hums, how often it pulses, its rest after; how far the sound carries, how wide it spreads
	 * either side (wider cracked), how fast it turns after its prey (faster cracked), what each pulse does and how long it slows.
	 * Get behind it, where its back is bare, or put a pillar between you.
	 */
	public static final int PEAL_TELL = 14, PEAL_TICKS = 48, PEAL_EVERY = 8, PEAL_RECOVER = 14, PEAL_SLOW = 40;
	public static final double PEAL_REACH = 10, PEAL_ANGLE = 40, PEAL_ANGLE_CRACKED = 60, PEAL_TURN = 2.5, PEAL_TURN_CRACKED = 4, PEAL_DAMAGE = TIER.hit(5);
	/**
	 * The undertow: how long the water pours before it pulls, how long it pulls, its rest after with its fists buried; how far the pull
	 * reaches, how hard it drags (harder cracked), and how wide the crush is when its fists come down (wider cracked) and what it does.
	 * Walk out against the pull.
	 */
	public static final int UNDERTOW_POUR = 12, UNDERTOW_PULL = 30, UNDERTOW_RECOVER = 30;
	public static final double UNDERTOW_RADIUS = 11, UNDERTOW_DRAG = 0.06, UNDERTOW_DRAG_CRACKED = 0.08, CRUSH_RADIUS = 3.5, CRUSH_RADIUS_CRACKED = 4.5, CRUSH_DAMAGE = TIER.hit(22);
	/**
	 * Cracking its bell: its tell, its rest after; how far the blast reaches and what it does; the great ring after, how fast and how
	 * far it rolls and what it does. Get clear of the glow, then jump the ring.
	 */
	public static final int CRACK_TELL = 36, CRACK_RECOVER = 30;
	public static final double CRACK_RADIUS = 6, CRACK_DAMAGE = TIER.hit(20), CRACK_WAVE_SPEED = 0.7, CRACK_WAVE_REACH = 16, CRACK_WAVE_DAMAGE = TIER.hit(14);
	/** The backstep: how long it lasts, how fast it goes, and the least time between two. */
	public static final int BACKSTEP_TICKS = 5, BACKSTEP_REST = 40;
	public static final double BACKSTEP_SPEED = 0.6;
	/** The shell: how long it stays under, the least time between two, and how much hurt, fading each tick by {@link #STING_FADE}, sends it under. */
	public static final int SHELL_TICKS = 40, SHELL_REST = 220;
	public static final double SHELL_AFTER = 45, STING_FADE = 0.97;
	/** Ticks between its moves, fewer once cracked; most moves it makes in one breath once cracked, and the rest after such a pair. */
	public static final int GAP = 10, GAP_CRACKED = 5, CHAIN_MOST = 2, CHAIN_REST = 10;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case SWING -> "swing";
			case HAMMER -> "hammer";
			case SLAM -> "slam";
			case CHARGE -> "charge";
			case BACKSTEP -> "backstep";
			case RECOVER -> "recover";
			case SHELL -> "shell";
			case TOLL -> "toll";
			case LASH -> "lash";
			case COVER -> "cover";
			case PEAL -> "peal";
			case UNDERTOW -> "undertow";
			case STUCK -> "stuck";
			case DAZED -> "dazed";
			case CRACK -> "crack";
			default -> "watch";
		};
	}

	/** The least time between two of {@code move}, a third shorter once cracked. */
	public static long rest(int move, boolean cracked) {
		long r = switch (move) {
			case SWING -> 50;
			case HAMMER -> 40;
			case SLAM -> 120;
			case CHARGE -> 140;
			case TOLL -> 160;
			case LASH -> 90;
			case COVER -> 150;
			case PEAL -> 220;
			case UNDERTOW -> 260;
			default -> 0;
		};
		return cracked ? r * 2 / 3 : r;
	}

	/**
	 * How much it wants each of {@link #PICKS} with its prey {@code distance} off, {@code behind} it or not, {@code height} above its
	 * feet, {@code since} ticks after it last made each move: its clapper and bell for anyone close, the whirl most of all for anyone
	 * at its back; its chain, the floor and its charge for anyone keeping away; the floor and its chain for anyone up out of reach.
	 * Nothing it made too lately.
	 */
	public static double[] weights(double distance, boolean behind, double height, long[] since, boolean cracked) {
		double[] w = new double[PICKS.length];
		boolean up = height > 2.5;
		double d = distance;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m, cracked)) continue;
			w[i] = switch (m) {
				case SWING -> up || d > SWING_REACH - 0.5 ? 0 : behind ? 4.5 : 3;
				case HAMMER -> up || behind || d < 1.5 || d > 6 ? 0 : 2.5;
				case SLAM -> up ? 3 : d < 3 || d > 14 ? 0 : d >= 7 ? 2 : 1.5;
				case CHARGE -> up || d < 6 || d > 16 ? 0 : (d >= 9 ? 3 : 2.5) * (behind ? 0.4 : 1);
				case TOLL -> up ? 0.3 : d <= 12 ? 1.2 : 0;
				case LASH -> up ? 1.5 : d >= 5 && d <= 11 ? 2.5 : 0;
				case COVER -> up || behind || d < 2 || d > 6 ? 0 : 2;
				case PEAL -> behind ? 0 : up ? 1 : d >= 3 && d <= 10 ? 2 : 0;
				case UNDERTOW -> up ? 1 : d >= 4 && d <= 12 ? 1.5 : 0;
				default -> 0;
			};
		}
		return w;
	}
	/** Its pick from {@link #PICKS}, {@code roll} 0-1, by {@link #weights} and what it made lately; -1 to keep closing. */
	public static int choose(MoveDeck deck, double distance, boolean behind, double height, long[] since, boolean cracked, double roll) {
		return deck.pick(PICKS, weights(distance, behind, height, since, cracked), roll);
	}
	/** Once cracked, what it follows {@code move} with in the same breath, its prey {@code distance} off, {@code roll} 0-1; -1 for nothing. */
	public static int chain(int move, double distance, double roll) {
		if (roll >= 0.55) return -1;
		return switch (move) {
			case SWING -> distance <= 6 ? HAMMER : LASH;
			case LASH -> distance <= 5 ? HAMMER : -1;
			case TOLL -> distance >= 6 ? CHARGE : -1;
			case SLAM -> TOLL;
			case PEAL -> SLAM;
			case UNDERTOW -> distance <= 5 ? SWING : -1;
			case CHARGE -> distance >= 6 ? LASH : -1;
			default -> -1;
		};
	}
	/** Whether, at {@code health} of {@code max} and not yet cracked, it tears its bell open. */
	public static boolean cracks(float health, float max, boolean cracked) {
		return !cracked && wounded(health, max);
	}

	/**
	 * Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim:
	 * the hammer's narrow line, the chain's long one past arm's length, its bowed bell running, its fists on the floor.
	 */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case HAMMER -> forward > 0 && forward <= HAMMER_REACH && Math.abs(side) <= HAMMER_HALF && height > -1.5 && height < 4.0;
			case LASH -> forward >= LASH_NEAR && forward <= LASH_REACH && Math.abs(side) <= LASH_HALF && height > -1.5 && height < 4.0;
			case CHARGE -> forward > -1.5 && forward <= 2.8 && Math.abs(side) <= 2.0 && height > -1.5 && height < 4.5;
			case SLAM -> forward > 0 && forward <= 3.5 && Math.abs(side) <= 2.5 && height > -1.5 && height < 2.0;
			default -> false;
		};
	}
	/** Whether the whirl reaches someone {@code distance} off and {@code height} above its feet. */
	public static boolean whirls(double distance, double height) {
		return distance <= SWING_REACH && height > -1.5 && height < 3.5;
	}
	/**
	 * Whether the clapper, {@code el} ticks into a spin, has swept past someone {@code offset} degrees round from where it started
	 * (counting the way it turns); the last tick of the spin takes in whatever is left.
	 */
	public static boolean swept(double offset, long el) {
		double off = ((offset % 360) + 360) % 360, from = (el - 1) * 360.0 / SWING_SPIN, to = el * 360.0 / SWING_SPIN;
		return el >= 1 && off >= from && (off < to || el >= SWING_SPIN);
	}
	/** How far ahead the split in the floor has run {@code el} ticks after the hammer lands. */
	public static double split(long el) {
		return HAMMER_REACH + (SPLIT_REACH - HAMMER_REACH) * Math.max(0, Math.min(1, el / (double) SPLIT_TICKS));
	}
	/** Whether the split, running on from {@code from} to {@code to} ahead, catches someone {@code forward} ahead, {@code side} across and {@code height} up. */
	public static boolean splits(double forward, double side, double height, double from, double to) {
		return forward > from && forward <= to && Math.abs(side) <= SPLIT_HALF && height > -1.5 && height < 1.5;
	}
	/** Whether a spout catches someone {@code dx},{@code dz} from where it bursts and {@code height} above the floor there. */
	public static boolean spouts(double dx, double dz, double height) {
		return dx * dx + dz * dz <= SPOUT_RADIUS * SPOUT_RADIUS && height > -1 && height < SPOUT_HEIGHT;
	}
	/** Whether a ring rolling from {@code from} out to {@code to} this tick passes someone {@code distance} off and {@code height} up without them jumping it. */
	public static boolean crosses(double distance, double from, double to, double height) {
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < WAVE_HEIGHT;
	}
	/** How far ahead its bell comes down on prey {@code distance} off. */
	public static double drop(double distance) {
		return Math.max(COVER_NEAR, Math.min(COVER_FAR, distance));
	}
	/** Whether its bell, come down, covers someone {@code dx},{@code dz} from its middle and {@code height} above the floor. */
	public static boolean covers(double dx, double dz, double height) {
		return dx * dx + dz * dz <= COVER_RADIUS * COVER_RADIUS && height > -1.5 && height < 3.0;
	}
	/** Whether the peal reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet. */
	public static boolean peals(double forward, double side, double height, boolean cracked) {
		return forward > 0 && forward <= PEAL_REACH && Math.toDegrees(Math.atan2(Math.abs(side), forward)) <= (cracked ? PEAL_ANGLE_CRACKED : PEAL_ANGLE)
			&& height > -2 && height < 5;
	}
	/** How hard the undertow drags someone {@code distance} off toward it each tick. */
	public static double drag(double distance, boolean cracked) {
		return distance > 1.5 && distance <= UNDERTOW_RADIUS ? (cracked ? UNDERTOW_DRAG_CRACKED : UNDERTOW_DRAG) : 0;
	}
	/** Whether its fists, come down at the end of the undertow, crush someone {@code distance} off and {@code height} up. */
	public static boolean crushes(double distance, double height, boolean cracked) {
		return distance <= (cracked ? CRUSH_RADIUS_CRACKED : CRUSH_RADIUS) && height > -1.5 && height < 2.5;
	}
	/** How hard the chain drags someone caught {@code distance} off back toward its feet. */
	public static double tug(double distance) {
		return Math.max(0, Math.min(LASH_TUG_MOST, (distance - 2) * LASH_TUG));
	}
	/** Whether its bell cracking open blasts someone {@code distance} off and {@code height} up. */
	public static boolean blasts(double distance, double height) {
		return distance <= CRACK_RADIUS && height > -1.5 && height < 4.0;
	}

	/**
	 * What its bell and hide leave of a blow. Facing you, the bell turns some of it; its back, side on, takes it whole. Spent, its
	 * chest is bare; stuck or dazed, more so; its head bare while its bell is down takes it all and more from anywhere, and so does its
	 * back while it peals; tearing its bell open it is half shut in; under its bell, almost nothing gets through.
	 */
	public static double armour(int move, boolean frontal, boolean open) {
		if (move == SHELL) return 0.15;
		if (move == STUCK || move == DAZED) return 1.5;
		if (move == RECOVER) return 1.3;
		if (move == CRACK) return 0.5;
		if (move == COVER && open) return 1.5;
		if (!frontal) return move == PEAL ? 1.3 : 1.0;
		return 0.65;
	}
	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 50;
	}
	/** Whether someone {@code angle} degrees off its facing is at its back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= 125;
	}
	/** {@code angle} folded into -180 to 180. */
	public static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether it steps back out of a blow: only a frontal one, only while it watches, not too often, and only some of the time. */
	public static boolean dodges(int move, boolean frontal, long sinceDodge, double roll) {
		return move == WATCH && frontal && sinceDodge >= BACKSTEP_REST && roll < 0.4;
	}
	/** Whether the hurt it has taken lately, {@code stung}, sends it under its bell, {@code sinceShell} ticks after it last went under. */
	public static boolean shells(int move, double stung, long sinceShell) {
		return (move == WATCH || move == RECOVER) && stung >= SHELL_AFTER && sinceShell >= SHELL_REST;
	}
	/**
	 * The Bell Shard, touched once the Warden is dead: a blow that leaves you at {@link #KNELL_BELOW} of your health or under makes it
	 * toll, once every {@link #KNELL_RECHARGE} ticks: everything hostile within {@link #KNELL_RADIUS} thrown back {@link #KNELL_SHOVE},
	 * and Absorption for {@link #KNELL_SHIELD} ticks and Resistance for {@link #KNELL_GUARD_TICKS}.
	 */
	public static final float KNELL_BELOW = 0.3F;
	public static final int KNELL_RECHARGE = 3600, KNELL_SHIELD = 160, KNELL_GUARD_TICKS = 60;
	public static final double KNELL_RADIUS = 6, KNELL_SHOVE = 1.6;
	/** A living Warden this close keeps anyone from the shard. */
	public static final double SHARD_GUARD = 48;

	/** Whether the shard tolls for someone left at {@code health} of {@code max}, {@code since} ticks after it last did. */
	public static boolean knells(float health, float max, long since) {
		return health > 0 && health <= max * KNELL_BELOW && since >= KNELL_RECHARGE;
	}
	/** Whether it is wounded: at half health or below, when it cracks its bell. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a hall from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from its vigil is close enough to wake it (and inside the hall, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == KNEEL;
	}

	/** How far up it is, 0 kneeling to 1 standing, {@code elapsed} ticks into a rise (or a kneel, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case KNEEL -> 1 - t;
			default -> 1;
		};
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
