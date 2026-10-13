package dev.wildercord.cast;

/**
 * How the Storm Conductor fights, kept free of the game so it can be tested: its moves, what it wants of each and where each lands.
 *
 * <p>It never leaves its podium. It conducts the storm with its baton, and the storm answers on the beat: a bolt falls where the
 * baton comes down, a ring rolls out as the baton circles, sparks pop along a line in time with its flicks. Its three rods are
 * the fight's heart. A rod draws down any bolt meant for someone standing at its foot, so the rods are shelter; but it blows
 * people off them, freezes them short of them, and in its second phase runs its chord between the rods that are not sparking,
 * so the shelter can turn on you. Its ball lightning hunts one of its prey, and led into the sparking rod it is swallowed and
 * grounds the Conductor itself. Close in at its feet and it discharges round them.</p>
 *
 * <p>Each move has a tell of its own and a way out of it. Two leave it open: winded after its held note, spent after it
 * discharges. From its second phase it gains the ball, the chord and the rolling thunder; in its third it plays counterpoint
 * across the whole floor, plays most moves twice or bigger, rests less and runs one move straight on into another.</p>
 */
public final class StormConductorRules {
	private StormConductorRules() {
	}

	/** Its baton raised high and brought down: a mark under each of its prey that follows them, then holds, and a bolt onto it. Step off the mark, or stand at a rod's foot. */
	public static final int DOWNBEAT = 1;
	/** Its baton circling faster and faster, then a low ring of lightning rolling out along the floor from its podium: jump it. */
	public static final int CRESCENDO = 2;
	/** Quick flicks of its baton, a line of sparks marked toward its prey, then the sparks popping along it on the beat: step off the line. */
	public static final int STACCATO = 3;
	/** Its baton held up, still, and the hall hums: lightning finds whoever moves while the note is held. Stand still. */
	public static final int FERMATA = 4;
	/** Its baton swept back, then across: a cone of wind that blows whoever is in it off the rods. Get behind it, or crouch into it. */
	public static final int GALE = 5;
	/** Its core crackling and sparks running round its feet, then a burst round its podium: back off when it crackles. */
	public static final int DISCHARGE = 6;
	/** A ball of lightning drawn off its baton that drifts after one of its prey: outrun it, or lead it into the sparking rod. */
	public static final int BALL = 7;
	/** Its baton pointed at each quiet rod in turn, lines marked between them and it, then arcs along every line: stand clear of the lines. */
	public static final int CHORD = 8;
	/** Its baton drawn back and rolled forward: waves of thunder roll in from the walls to its podium one after another. Jump each. */
	public static final int ROLL = 9;
	/** Its baton beating left and right: half the floor marked in squares and struck, then the other half. Step onto the dark squares, then back. */
	public static final int COUNTERPOINT = 10;
	/** Winded after its held note, its baton drooping: the opening after {@link #FERMATA}. */
	public static final int WINDED = 11;
	/** Spent after it discharges, its core dark and its coils cold: the opening after {@link #DISCHARGE}. */
	public static final int SPENT = 12;
	/** How many moves of its own it has, by id. */
	public static final int MOVES = 12;

	/** The moves it picks from when it is free to choose. */
	public static final int[] PICKS = {DOWNBEAT, CRESCENDO, STACCATO, FERMATA, GALE, DISCHARGE, BALL, CHORD, ROLL, COUNTERPOINT};

	/**
	 * The downbeat: its tell, when the marks stop following, the gap before a second beat where its prey then stand (third phase),
	 * and its rest; how wide a mark is and what the bolt does. A rod draws down a bolt meant for anyone within {@link #ROD_DRAW} of it.
	 */
	public static final int DOWN_TELL = 16, DOWN_LOCK = 8, DOWN_AGAIN = 12, DOWN_REST = 18;
	public static final double DOWN_RADIUS = 1.5, DOWN_DAMAGE = 6;
	/** How near a rod someone must stand for it to draw down a bolt meant for them. */
	public static final double ROD_DRAW = 2.0;
	/**
	 * The crescendo: its tell, the gap before a second ring (third phase), and its rest; how fast the ring rolls and how far, how high
	 * off the floor it reaches, what it does and how hard it throws whoever it catches. Jump it.
	 */
	public static final int CRESC_TELL = 16, CRESC_AGAIN = 12, CRESC_REST = 20;
	public static final double CRESC_SPEED = 0.5, CRESC_REACH = 11, WAVE_HEIGHT = 0.9, CRESC_DAMAGE = 5, CRESC_SHOVE = 0.6;
	/**
	 * The staccato: its tell, when its aim locks, the ticks between beats, and its rest; how far apart the sparks sit along a line,
	 * how many, how wide each pops and what it does; how many lines it marks in each phase and how far apart (degrees).
	 */
	public static final int STAC_TELL = 12, STAC_LOCK = 4, STAC_EVERY = 2, STAC_COUNT = 10, STAC_REST = 18;
	public static final double STAC_STEP = 1.2, STAC_RADIUS = 0.9, STAC_DAMAGE = 5, STAC_SPREAD = 30;
	public static final int[] STAC_LINES = {0, 1, 1, 3};
	/**
	 * The held note: its tell, how long the note is held, how often it listens for movement, and its rest; how far someone may drift
	 * between two listens and still be still, and what the lightning does. Then it is winded for {@link #WINDED_TICKS}.
	 */
	public static final int FERM_TELL = 14, FERM_HOLD = 30, FERM_EVERY = 5, FERM_REST = 16, WINDED_TICKS = 40;
	public static final double FERM_MOVED = 0.5, FERM_DAMAGE = 6;
	/**
	 * The gale: its tell, when its aim locks, and its rest; how far the wind carries and how wide it spreads either side (degrees,
	 * wider in the third phase), what it does, how hard it blows, and how much of that someone crouching still takes.
	 */
	public static final int GALE_TELL = 14, GALE_LOCK = 4, GALE_REST = 18;
	public static final double GALE_REACH = 9, GALE_ANGLE = 50, GALE_ANGLE_WIDE = 70, GALE_DAMAGE = 3, GALE_SHOVE = 1.1, GALE_CROUCH = 0.35;
	/**
	 * The discharge: its tell, and its rest; how far the burst reaches round its podium (farther in the third phase), what it does
	 * and how hard it throws. Then it is spent for {@link #SPENT_TICKS}.
	 */
	public static final int DIS_TELL = 14, DIS_REST = 16, SPENT_TICKS = 36;
	public static final double DIS_RADIUS = 3.5, DIS_RADIUS_WIDE = 4.0, DIS_DAMAGE = 7, DIS_SHOVE = 1.0;
	/**
	 * The ball lightning: its tell, how long it hunts, how long before it bursts it stops and swells, and its rest; how fast it
	 * drifts (faster in the third phase, but never as fast as someone walking), how near it must come to strike, how near the
	 * sparking rod to be swallowed, how wide its last burst is, what it does, and how high it floats.
	 */
	public static final int BALL_TELL = 16, BALL_LIFE = 90, BALL_STOP = 12, BALL_REST = 20;
	public static final double BALL_FOLLOW = 0.15, BALL_FOLLOW_FAST = 0.19, BALL_TOUCH = 1.0, BALL_SWALLOW = 1.5, BALL_POP = 1.8,
		BALL_DAMAGE = 7, BALL_HEIGHT = 1.2;
	/**
	 * The chord: its tell, how long the arcs run, the gap before they run again (third phase), and its rest; how wide a line is
	 * and what an arc does.
	 */
	public static final int CHORD_TELL = 16, CHORD_TICKS = 6, CHORD_AGAIN = 12, CHORD_REST = 18;
	public static final double CHORD_HALF = 0.9, CHORD_DAMAGE = 6;
	/**
	 * The rolling thunder: its tell, the ticks between waves, and its rest; how far out the waves start, how fast they roll in,
	 * what each does, and how many roll in each phase.
	 */
	public static final int ROLL_TELL = 14, ROLL_GAP = 10, ROLL_REST = 20;
	public static final double ROLL_FROM = 12, ROLL_SPEED = 0.6, ROLL_DAMAGE = 4;
	public static final int[] ROLL_WAVES = {0, 3, 3, 4};
	/**
	 * The counterpoint: its tell, the gap before the other half of the floor is struck, and its rest; how big a square is, how far
	 * out from it the squares reach, and what a strike does.
	 */
	public static final int COUNTER_TELL = 16, COUNTER_AGAIN = 12, COUNTER_REST = 20;
	public static final double COUNTER_TILE = 2.0, COUNTER_REACH = 10, COUNTER_DAMAGE = 6;

	/** How much it wants any one spell of its phase. */
	public static final double SPELL = 0.9;
	/** How near the sparking rod its prey must be for it to treat them as about to ground it. */
	public static final double NEAR_ROD = 3.5;

	/** The name {@code move} goes by, for its boss bar line; null for none. */
	public static String key(int move) {
		return switch (move) {
			case DOWNBEAT -> "downbeat";
			case CRESCENDO -> "crescendo";
			case STACCATO -> "staccato";
			case FERMATA -> "fermata";
			case GALE -> "gale";
			case DISCHARGE -> "discharge";
			case BALL -> "ball";
			case CHORD -> "chord";
			case ROLL -> "roll";
			case COUNTERPOINT -> "counterpoint";
			case WINDED -> "winded";
			case SPENT -> "spent";
			default -> null;
		};
	}

	/** The phase {@code move} is first made in. */
	public static int unlocks(int move) {
		return switch (move) {
			case DOWNBEAT, CRESCENDO, STACCATO, FERMATA, GALE, DISCHARGE -> 1;
			case BALL, CHORD, ROLL -> 2;
			case COUNTERPOINT -> 3;
			default -> 99;
		};
	}

	/** Whether {@code move} can be made at all now, in {@code phase}, with {@code quiet} rods not sparking: a chord wants two. */
	public static boolean open(int move, int phase, int quiet) {
		return phase >= unlocks(move) && (move != CHORD || quiet >= 2);
	}

	/** Whether {@code move} leaves it open to harm. */
	public static boolean opening(int move) {
		return move == WINDED || move == SPENT;
	}

	/** Whether the shape of {@code move} is aimed at its prey (and so locks its aim a few ticks before it lands). */
	public static boolean aimed(int move) {
		return move == DOWNBEAT || move == STACCATO || move == GALE;
	}

	/** How many ticks before {@code move} lands its aim stops following its prey (none for a move that isn't aimed). */
	public static int lock(int move) {
		return switch (move) {
			case DOWNBEAT -> DOWN_LOCK;
			case STACCATO -> STAC_LOCK;
			case GALE -> GALE_LOCK;
			default -> 0;
		};
	}

	/** The least time between two of {@code move}, a third shorter in its third phase. */
	public static long cooldown(int move, int phase) {
		long r = switch (move) {
			case DOWNBEAT -> 80;
			case CRESCENDO -> 140;
			case STACCATO -> 90;
			case FERMATA -> 200;
			case GALE -> 100;
			case DISCHARGE -> 120;
			case BALL -> 300;
			case CHORD -> 160;
			case ROLL -> 180;
			case COUNTERPOINT -> 160;
			default -> 0;
		};
		return phase >= 3 ? r * 2 / 3 : r;
	}

	/** Ticks it rests after {@code move} before it picks again: less in its second phase, half in its third. */
	public static int rest(int move, int phase) {
		int r = switch (move) {
			case DOWNBEAT -> DOWN_REST;
			case CRESCENDO -> CRESC_REST;
			case STACCATO -> STAC_REST;
			case FERMATA -> FERM_REST;
			case GALE -> GALE_REST;
			case DISCHARGE -> DIS_REST;
			case BALL -> BALL_REST;
			case CHORD -> CHORD_REST;
			case ROLL -> ROLL_REST;
			case COUNTERPOINT -> COUNTER_REST;
			default -> 12;
		};
		return phase >= 3 ? r / 2 : phase == 2 ? r * 3 / 4 : r;
	}

	/**
	 * How much it wants each of {@link #PICKS} in {@code phase}, its prey {@code distance} off, {@code nearRod} (closing on the
	 * sparking rod) or not, {@code sheltered} at the foot of any rod or not, {@code behind} it or not, with {@code quiet} rods not
	 * sparking, {@code since} ticks after it last made each move (by id). The downbeat for anyone out in the open; the gale and the
	 * chord for anyone sheltering at a rod; the held note for anyone closing on the sparking rod; the discharge only for anyone
	 * at its feet; the ball for anyone keeping off. Nothing locked to a later phase, short of rods, or made too lately.
	 */
	public static double[] weights(int phase, double distance, boolean nearRod, boolean sheltered, boolean behind, int quiet, long[] since) {
		double[] w = new double[PICKS.length];
		double d = distance;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (!open(m, phase, quiet) || since[m] < cooldown(m, phase)) {
				continue;
			}
			w[i] = switch (m) {
				case DOWNBEAT -> sheltered ? 0.6 : 2.2;
				case CRESCENDO -> d <= CRESC_REACH ? 1.8 : 0.3;
				case STACCATO -> d >= 3 && d <= STAC_STEP * STAC_COUNT ? 2.0 : 0.4;
				case FERMATA -> nearRod ? 2.6 : 1.2;
				case GALE -> behind || d > GALE_REACH ? 0 : sheltered || nearRod ? 3.5 : 1.0;
				case DISCHARGE -> d < 4 ? 3.5 : 0;
				case BALL -> nearRod ? 0.6 : d >= 4 ? 2.4 : 1.2;
				case CHORD -> sheltered ? 3.0 : 1.5;
				case ROLL -> d <= ROLL_FROM ? 2.0 : 0.5;
				case COUNTERPOINT -> 2.2;
				default -> 0;
			};
		}
		return w;
	}

	/** Its pick from {@link #PICKS}, {@code roll} 0-1, by {@link #weights} and what it made lately; -1 if nothing will do. */
	public static int choose(MoveDeck deck, int phase, double distance, boolean nearRod, boolean sheltered, boolean behind, int quiet,
		long[] since, double roll) {
		return deck.pick(PICKS, weights(phase, distance, nearRod, sheltered, behind, quiet, since), roll);
	}

	/** How much it wants a spell of its phase: more for prey keeping out past where its own moves reach. */
	public static double spellWeight(double distance) {
		return distance > ROLL_FROM ? SPELL * 1.5 : SPELL;
	}

	/**
	 * In its third phase, what it follows {@code move} with in the same breath, {@code roll} 0-1; -1 for nothing. The held note
	 * keeps its prey still for the downbeat; the gale blows them onto a line of sparks; the ring rolls out and the thunder rolls
	 * back in; the chord gives way to the ball; the counterpoint ends in a crescendo.
	 */
	public static int chain(int move, double roll) {
		if (roll >= 0.5) {
			return -1;
		}
		int next = switch (move) {
			case FERMATA, STACCATO, ROLL -> DOWNBEAT;
			case GALE -> STACCATO;
			case CRESCENDO -> ROLL;
			case CHORD -> BALL;
			case COUNTERPOINT -> CRESCENDO;
			default -> -1;
		};
		return next > 0 && unlocks(next) <= 3 ? next : -1;
	}

	/** The tell of {@code move}: how long it winds up before anything lands (none for an opening). */
	public static int tell(int move) {
		return switch (move) {
			case DOWNBEAT -> DOWN_TELL;
			case CRESCENDO -> CRESC_TELL;
			case STACCATO -> STAC_TELL;
			case FERMATA -> FERM_TELL;
			case GALE -> GALE_TELL;
			case DISCHARGE -> DIS_TELL;
			case BALL -> BALL_TELL;
			case CHORD -> CHORD_TELL;
			case ROLL -> ROLL_TELL;
			case COUNTERPOINT -> COUNTER_TELL;
			default -> 0;
		};
	}

	/** How many waves of thunder roll in during {@link #ROLL} in {@code phase}. */
	public static int waves(int phase) {
		return ROLL_WAVES[Math.max(1, Math.min(3, phase))];
	}

	/** How many lines of sparks it marks during {@link #STACCATO} in {@code phase}. */
	public static int lines(int phase) {
		return STAC_LINES[Math.max(1, Math.min(3, phase))];
	}

	/** How long {@code move} lasts in all in {@code phase}, its tell included. */
	public static int length(int move, int phase) {
		boolean late = phase >= 3;
		return switch (move) {
			case DOWNBEAT -> DOWN_TELL + 4 + (late ? DOWN_AGAIN : 0);
			case CRESCENDO -> CRESC_TELL + rolls(CRESC_REACH, CRESC_SPEED) + 2 + (late ? CRESC_AGAIN : 0);
			case STACCATO -> STAC_TELL + STAC_COUNT * STAC_EVERY + 2;
			case FERMATA -> FERM_TELL + FERM_HOLD + 2;
			case GALE -> GALE_TELL + 6;
			case DISCHARGE -> DIS_TELL + 6;
			case BALL -> BALL_TELL + BALL_LIFE + BALL_STOP;
			case CHORD -> CHORD_TELL + (late ? CHORD_AGAIN : 0) + CHORD_TICKS + 2;
			case ROLL -> ROLL_TELL + (waves(phase) - 1) * ROLL_GAP + rolls(ROLL_FROM, ROLL_SPEED) + 2;
			case COUNTERPOINT -> COUNTER_TELL + COUNTER_AGAIN + 4;
			case WINDED -> WINDED_TICKS;
			case SPENT -> SPENT_TICKS;
			default -> 1;
		};
	}

	/** Ticks a ring takes to roll {@code reach} blocks at {@code speed} a tick. */
	public static int rolls(double reach, double speed) {
		return (int) Math.ceil(reach / speed);
	}

	/** Which way lane {@code i} of {@code n} points, degrees off its aim, {@code spread} apart, shifted by {@code offset}. */
	public static double lane(int i, int n, double spread, double offset) {
		return (i - (n - 1) / 2.0) * spread + offset;
	}

	/** Whether a downbeat's bolt onto its mark catches someone {@code dx},{@code dz} from the mark and {@code height} above it. */
	public static boolean marked(double dx, double dz, double height) {
		return dx * dx + dz * dz <= DOWN_RADIUS * DOWN_RADIUS && height > -1.5 && height < 3.0;
	}

	/** Whether a rod {@code dx},{@code dz} from where a strike is meant to land draws it down into itself instead. */
	public static boolean drawn(double dx, double dz) {
		return dx * dx + dz * dz <= ROD_DRAW * ROD_DRAW;
	}

	/**
	 * Whether a ring rolling along the floor between {@code from} and {@code to} off this tick (either way: out from it or in from
	 * the walls) passes someone {@code distance} off and {@code height} above it without their jumping it.
	 */
	public static boolean crosses(double distance, double from, double to, double height) {
		double lo = Math.min(from, to), hi = Math.max(from, to);
		return distance > lo - 0.5 && distance <= hi + 0.5 && height > -1.5 && height < WAVE_HEIGHT;
	}

	/** How far out along its line spark {@code i} of a staccato sits. */
	public static double spark(int i) {
		return (i + 1) * STAC_STEP;
	}

	/** Which spark of a staccato pops {@code t} ticks after its tell, near to far and one each beat; -1 for none this tick. */
	public static int popping(int t) {
		if (t < 0 || t % STAC_EVERY != 0) {
			return -1;
		}
		int i = t / STAC_EVERY;
		return i < STAC_COUNT ? i : -1;
	}

	/** Whether a spark popping catches someone {@code dx},{@code dz} from it and {@code height} above it. */
	public static boolean sparked(double dx, double dz, double height) {
		return dx * dx + dz * dz <= STAC_RADIUS * STAC_RADIUS && height > -1.5 && height < 2.5;
	}

	/** Whether someone who drifted {@code moved} blocks across the floor between two listens of its held note has kept still. */
	public static boolean still(double moved) {
		return moved <= FERM_MOVED;
	}

	/** Whether its gale catches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, {@code wide} or not. */
	public static boolean blown(double forward, double side, double height, boolean wide) {
		return forward > 0 && forward <= GALE_REACH && Math.toDegrees(Math.atan2(Math.abs(side), forward)) <= (wide ? GALE_ANGLE_WIDE : GALE_ANGLE)
			&& height > -2.5 && height < 3.5;
	}

	/** How hard its gale blows someone, {@code crouching} or not. */
	public static double shove(boolean crouching) {
		return crouching ? GALE_SHOVE * GALE_CROUCH : GALE_SHOVE;
	}

	/** Whether its discharge catches someone {@code distance} off and {@code height} above its feet, {@code wide} (third phase) or not. */
	public static boolean discharged(double distance, double height, boolean wide) {
		return distance <= (wide ? DIS_RADIUS_WIDE : DIS_RADIUS) && height > -2.0 && height < 3.5;
	}

	/** How much of the way {@code dx},{@code dz} to its prey its ball drifts this tick at {@code step} a tick: 1 for all of it. */
	public static double pace(double dx, double dz, double step) {
		double d = Math.sqrt(dx * dx + dz * dz);
		return d <= step ? 1 : step / d;
	}

	/** Whether its ball touches someone {@code dx},{@code dz} from it, the ball {@code above} their feet. */
	public static boolean touches(double dx, double above, double dz) {
		return dx * dx + dz * dz <= BALL_TOUCH * BALL_TOUCH && above > -0.5 && above < 2.3;
	}

	/** Whether the sparking rod {@code dx},{@code dz} from its ball swallows it. */
	public static boolean swallowed(double dx, double dz) {
		return dx * dx + dz * dz <= BALL_SWALLOW * BALL_SWALLOW;
	}

	/** Whether its ball bursting catches someone {@code dx},{@code dz} from it, the ball {@code above} their feet. */
	public static boolean popped(double dx, double above, double dz) {
		return dx * dx + dz * dz <= BALL_POP * BALL_POP && above > -1.5 && above < 3.0;
	}

	/** How far {@code px},{@code pz} lies from the line between {@code ax},{@code az} and {@code bx},{@code bz}, end to end. */
	public static double segment(double px, double pz, double ax, double az, double bx, double bz) {
		double vx = bx - ax, vz = bz - az, len = vx * vx + vz * vz;
		double f = len <= 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / len));
		double cx = ax + vx * f - px, cz = az + vz * f - pz;
		return Math.sqrt(cx * cx + cz * cz);
	}

	/** Whether an arc of its chord catches someone {@code distance} off its line and {@code height} above the floor. */
	public static boolean chorded(double distance, double height) {
		return distance <= CHORD_HALF && height > -1.5 && height < 3.0;
	}

	/** Which square of its counterpoint {@code x} blocks off its podium falls in, along one axis. */
	public static int tile(double x) {
		return (int) Math.floor(x / COUNTER_TILE);
	}

	/** The middle of square {@code i} of its counterpoint along one axis, blocks off its podium. */
	public static double centre(int i) {
		return (i + 0.5) * COUNTER_TILE;
	}

	/** Which half of its counterpoint square {@code i},{@code j} belongs to: 0 struck first, 1 second. */
	public static int half(int i, int j) {
		return (i + j) & 1;
	}

	/** Whether square {@code i},{@code j} of its counterpoint is within its reach (and not its own podium). */
	public static boolean reaches(int i, int j) {
		double x = centre(i), z = centre(j), d = x * x + z * z;
		return d <= COUNTER_REACH * COUNTER_REACH && d > 1.5 * 1.5;
	}

	/** Whether half {@code set} of its counterpoint strikes someone {@code dx},{@code dz} off its podium and {@code height} above it. */
	public static boolean counter(double dx, double dz, int set, double height) {
		int i = tile(dx), j = tile(dz);
		return half(i, j) == set && reaches(i, j) && height > -1.5 && height < 3.0;
	}

	/** Whether someone {@code angle} degrees off its facing is at its back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= 125;
	}

	/** {@code angle} folded into -180 to 180. */
	public static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
}
