package dev.wildercord.cast;

/**
 * How the Tide Scribe fights, kept free of the game so it can be tested: its moves, what it wants of each and where each lands.
 *
 * <p>It is two creatures by turns. While its pit is dry it is a writer: it flings quills from its scroll, writes words on the
 * floor that burst, and tears out pages that wheel round it. While the pit is flooded it swims: it stirs a whirlpool, sends a
 * riptide down a lane of foam, and dives under its own ink to come up beneath you. Some of what it does wants the tide, some of it
 * the dry floor, and a little either. Each move has a tell of its own and a way out of it, and several leave it open: bowed over
 * its writing, spun dizzy by its own whirlpool, or hanging on the surface after a dive, out of the water's wrapping and lying in
 * open water for a storm spell to find.</p>
 *
 * <p>From its second phase it gains a cloud of ink, its pages, a breaker of water and the dive; in its third it calls the tide in
 * whenever it likes, makes most of its moves bigger, rests less and follows one move straight on with another.</p>
 */
public final class TideScribeRules {
	private TideScribeRules() {
	}

	/** Its quill drawn back, a fan of lanes glinting on the floor, and a volley of quills down each: stand between the lanes, or under it. Dry only. */
	public static final int QUILLS = 1;
	/** Bent over its scroll, it writes, and a word scrawls itself on the floor under each of its prey and round about, bursting a moment later: step off the marks. Dry only. */
	public static final int SCRIPT = 2;
	/** Arms out, it spins, and the flood turns into a whirlpool round it, dragging everything wading in toward its churning heart: wade out, or stand on ice. Flooded only. */
	public static final int WHIRLPOOL = 3;
	/** Both arms drawn back, a lane of foam marked across the water, then a current surging down it that sweeps away whoever is in it: step out of the lane. Flooded only. */
	public static final int RIPTIDE = 4;
	/** Its head thrown back and swelling, a cone of ink spat across its front, blinding: get behind it or out to its side. */
	public static final int INK = 5;
	/** It tears pages from its scroll and they wheel round it in a band, cutting: get in close under it, or out past the band. Dry only. */
	public static final int PAGES = 6;
	/** It rises with its arms high and brings them down, and a breaker rolls out from it across the floor: jump it. */
	public static final int BREAKER = 7;
	/** It sinks under its own ink and a trail of bubbles hunts you; it bursts up under the spot when it settles: be off the spot. Flooded only. */
	public static final int DIVE = 8;
	/** Its scroll raised high, it reads the tide in early: a wave rolls in from the pit's rim toward it, and the flood follows. Dry only. */
	public static final int TIDE_CALL = 9;
	/** Bowed over the words it wrote, its back bare: the opening after {@link #SCRIPT}. */
	public static final int BOWED = 10;
	/** Spun dizzy by its own whirlpool, up out of the water's wrapping: the opening after {@link #WHIRLPOOL}. */
	public static final int SPUN = 11;
	/** Hanging on the surface after a dive, dazed and unwrapped in open water: the long opening after {@link #DIVE}. */
	public static final int SURFACED = 12;
	/** How many moves of its own it has, by id. */
	public static final int MOVES = 12;

	/** The moves it picks from when it is free to choose. */
	public static final int[] PICKS = {QUILLS, SCRIPT, WHIRLPOOL, RIPTIDE, INK, PAGES, BREAKER, DIVE, TIDE_CALL};

	/**
	 * The volley of quills: its tell, when its aim locks, the gap before a second volley (third phase), and its rest; how near and
	 * far a lane runs, how wide it is, how many lanes fan out and how far apart (degrees) in each phase, and what a quill does.
	 * The second volley flies down the gaps the first left. Stand between the lanes, or close in under it.
	 */
	public static final int QUILL_TELL = 14, QUILL_LOCK = 4, QUILL_AGAIN = 12, QUILL_REST = 22;
	public static final double QUILL_NEAR = 2.0, QUILL_REACH = 16, QUILL_HALF = 0.6, QUILL_DAMAGE = 6;
	public static final int[] QUILL_LANES = {0, 3, 4, 5};
	public static final double[] QUILL_SPREAD = {0, 22, 18, 18};
	/**
	 * The written floor: its tell, how long the words glow before they burst, and the bowed opening after; how wide a word is, how
	 * many more fall scattered round each of its prey (by phase) and how far, and what a burst does. In its third phase the words
	 * burst in two waves, the second where its prey then stand. Step off the marks.
	 */
	public static final int SCRIPT_TELL = 16, SCRIPT_FUSE = 16, SCRIPT_AGAIN = 14, BOWED_TICKS = 34;
	public static final double GLYPH_RADIUS = 1.4, GLYPH_SPREAD = 4.0, GLYPH_DAMAGE = 7, GLYPH_LIFT = 0.7;
	public static final int[] GLYPH_SCATTER = {0, 2, 3, 3};
	/**
	 * The whirlpool: its tell, how long it spins and how often its heart churns, and the dizzy opening after; how far the pull
	 * reaches, how hard it drags (harder from the second phase), how wide its churning heart is and what a churn does; in its third
	 * phase it ends by flinging everything within {@link #WHIRL_FLING} of its heart back out, for {@link #WHIRL_FLING_DAMAGE}. Wade out against it, or stand on ice where the current can't take you.
	 */
	public static final int WHIRL_TELL = 16, WHIRL_TICKS = 44, WHIRL_EVERY = 10, SPUN_TICKS = 36;
	public static final double WHIRL_RADIUS = 9, WHIRL_DRAG = 0.05, WHIRL_DRAG_HARD = 0.07, WHIRL_CORE = 2.2, WHIRL_DAMAGE = 3,
		WHIRL_FLING = 4.0, WHIRL_FLING_DAMAGE = 5;
	/**
	 * The riptide: its tell, when its aim locks, how long the current runs, the gap before a second one back across (third phase),
	 * and its rest; how far the lane runs and how wide, what the current does, and how hard it sweeps whoever is in it along the lane.
	 * Step out of the lane.
	 */
	public static final int RIP_TELL = 14, RIP_LOCK = 4, RIP_TICKS = 10, RIP_AGAIN = 10, RIP_REST = 20;
	public static final double RIP_REACH = 15, RIP_HALF = 1.6, RIP_DAMAGE = 6, RIP_SWEEP = 0.35;
	/**
	 * The cloud of ink: its tell, when its aim locks, and its rest; how far the ink carries and how wide it spreads either side
	 * (degrees, wider in the third phase), what it does, and how long it blinds and slows. Get behind it, or out to its side.
	 */
	public static final int INK_TELL = 14, INK_LOCK = 4, INK_REST = 20, INK_BLIND = 50, INK_SLOW = 60;
	public static final double INK_REACH = 9, INK_ANGLE = 35, INK_ANGLE_WIDE = 50, INK_DAMAGE = 4;
	/**
	 * The pages: its tell, how long they wheel and how often they cut, and its rest; how near and far from it the band runs, how
	 * high it reaches, and what a cut does. In its third phase the band flies out at the end as a ring along the floor, how fast and
	 * how far. Get in close, or out past the band; then jump the ring.
	 */
	public static final int PAGES_TELL = 14, PAGES_TICKS = 40, PAGES_EVERY = 10, PAGES_REST = 20;
	public static final double PAGE_INNER = 3.0, PAGE_OUTER = 6.0, PAGE_HEIGHT = 3.0, PAGE_DAMAGE = 4, PAGE_FLY_SPEED = 0.6, PAGE_FLY_REACH = 11;
	/**
	 * The breaker: its tell, and its rest; how fast it rolls and how far, how high off the floor (or the water) it reaches, what it
	 * does, and how hard it throws whoever it catches. In its third phase a second follows {@link #BREAKER_AGAIN} ticks behind. Jump it.
	 */
	public static final int BREAKER_TELL = 16, BREAKER_AGAIN = 12, BREAKER_REST = 20;
	public static final double BREAKER_SPEED = 0.55, BREAKER_REACH = 13, WAVE_HEIGHT = 0.9, BREAKER_DAMAGE = 6, BREAKER_SHOVE = 0.9;
	/**
	 * The dive: its tell, how long it hunts under the water, when the spot stops following (before it bursts), how long the burst
	 * lasts; how fast its bubbles follow, how wide the burst is and what it does; then the long dazed opening on the surface. In its
	 * third phase it dives once more before it surfaces for good. Keep moving, and be off the spot when it settles.
	 */
	public static final int DIVE_TELL = 12, DIVE_HUNT = 40, DIVE_LOCK = 16, DIVE_BURST = 6, SURFACED_TICKS = 44;
	public static final double DIVE_FOLLOW = 0.3, DIVE_RADIUS = 1.8, DIVE_DAMAGE = 8, DIVE_LIFT = 1.0;
	/**
	 * The tide called in: its tell, and its rest; the wave rolling in from the pit's rim toward it, how fast, how high and what it
	 * does, sweeping whoever it catches in toward the deep middle; then the flood comes in behind it. Jump the wave.
	 */
	public static final int CALL_TELL = 18, CALL_REST = 24;
	public static final double CALL_SPEED = 0.5, CALL_DAMAGE = 6, CALL_SWEEP = 0.8;
	/** How far out from its pit's heart the called wave starts rolling in. */
	public static final double CALL_FROM = 12.0;
	/** How long the tide must still be out for it to bother calling it in. */
	public static final int CALL_LEAST = 60;

	/** How much it wants any one spell of its phase, before the tide is weighed in. */
	public static final double SPELL = 1.2;
	/** How much more of a blow it takes while it is open: bowed, spun or surfaced. */
	public static final double OPEN = 1.3;
	/** How far from its pit's heart it ever goes of its own accord, inside its leash. */
	public static final double ROAM = 11.0;

	/** The bar's name for what it is doing, or null for nothing of its own. */
	public static String key(int move) {
		return switch (move) {
			case QUILLS -> "quills";
			case SCRIPT -> "script";
			case WHIRLPOOL -> "whirlpool";
			case RIPTIDE -> "riptide";
			case INK -> "ink";
			case PAGES -> "pages";
			case BREAKER -> "breaker";
			case DIVE -> "dive";
			case TIDE_CALL -> "tide_call";
			case BOWED -> "bowed";
			case SPUN -> "spun";
			case SURFACED -> "surfaced";
			default -> null;
		};
	}

	/** The phase {@code move} is first made in. */
	public static int unlocks(int move) {
		return switch (move) {
			case QUILLS, SCRIPT, WHIRLPOOL, RIPTIDE -> 1;
			case INK, PAGES, BREAKER, DIVE -> 2;
			case TIDE_CALL -> 3;
			default -> 99;
		};
	}

	/** Whether {@code move} wants the flood (true), the dry floor (false), or either (null). */
	public static Boolean wants(int move) {
		return switch (move) {
			case WHIRLPOOL, RIPTIDE, DIVE -> true;
			case QUILLS, SCRIPT, PAGES, TIDE_CALL -> false;
			default -> null;
		};
	}

	/** Whether {@code move} can be made at all now, in {@code phase}, the pit {@code flooded} or not. */
	public static boolean open(int move, int phase, boolean flooded) {
		Boolean wet = wants(move);
		return phase >= unlocks(move) && (wet == null || wet == flooded);
	}

	/** Whether {@code move} leaves it open to harm. */
	public static boolean opening(int move) {
		return move == BOWED || move == SPUN || move == SURFACED;
	}

	/** The least time between two of {@code move}, a third shorter in its third phase. */
	public static long cooldown(int move, int phase) {
		long r = switch (move) {
			case QUILLS -> 60;
			case SCRIPT -> 120;
			case WHIRLPOOL -> 200;
			case RIPTIDE -> 80;
			case INK -> 140;
			case PAGES -> 140;
			case BREAKER -> 120;
			case DIVE -> 180;
			case TIDE_CALL -> 400;
			default -> 0;
		};
		return phase >= 3 ? r * 2 / 3 : r;
	}

	/** Ticks it rests after {@code move} before it picks again: less in its third phase. */
	public static int rest(int move, int phase) {
		int r = switch (move) {
			case QUILLS -> QUILL_REST;
			case RIPTIDE -> RIP_REST;
			case INK -> INK_REST;
			case PAGES -> PAGES_REST;
			case BREAKER -> BREAKER_REST;
			case TIDE_CALL -> CALL_REST;
			default -> 12;
		};
		return phase >= 3 ? r / 2 : phase == 2 ? r * 3 / 4 : r;
	}

	/**
	 * How much it wants each of {@link #PICKS} in {@code phase}, the pit {@code flooded} or not (and the tide fully out and staying
	 * so a while, {@code tideOut}), its prey {@code distance} off, {@code behind} it or not, {@code height} above the pit floor,
	 * {@code wading} in the flood or not, {@code since} ticks after it last made each move (by id). Quills for anyone keeping off,
	 * pages for anyone close, words on the floor for anyone; the whirlpool most for anyone wading, the riptide and the dive for
	 * anyone keeping away; ink for anyone before it. Nothing locked to a later phase, wrong for the tide, or made too lately.
	 */
	public static double[] weights(int phase, boolean flooded, boolean tideOut, double distance, boolean behind, double height,
		boolean wading, long[] since) {
		double[] w = new double[PICKS.length];
		boolean up = height > 2.5;
		double d = distance;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (!open(m, phase, flooded) || since[m] < cooldown(m, phase)) {
				continue;
			}
			w[i] = switch (m) {
				case QUILLS -> d < QUILL_NEAR + 1 ? 0.6 : d <= QUILL_REACH ? 2.5 : 0;
				case SCRIPT -> d <= 18 ? (up ? 1.0 : 2.0) : 0;
				case WHIRLPOOL -> d > WHIRL_RADIUS + 2 ? 0.4 : wading ? 3.5 : 1.0;
				case RIPTIDE -> d >= 3 && d <= RIP_REACH ? (wading ? 3.0 : 2.0) : 0;
				case INK -> behind || d > INK_REACH ? 0 : 2.5;
				case PAGES -> d <= PAGE_OUTER + 1 ? 3.0 : 0.5;
				case BREAKER -> d <= BREAKER_REACH ? (up ? 0.5 : flooded ? 2.0 : 1.6) : 0;
				case DIVE -> d >= 4 ? 2.5 : 1.0;
				case TIDE_CALL -> tideOut ? 3.0 : 0;
				default -> 0;
			};
		}
		return w;
	}

	/** Its pick from {@link #PICKS}, {@code roll} 0-1, by {@link #weights} and what it made lately; -1 if nothing will do. */
	public static int choose(MoveDeck deck, int phase, boolean flooded, boolean tideOut, double distance, boolean behind, double height,
		boolean wading, long[] since, double roll) {
		return deck.pick(PICKS, weights(phase, flooded, tideOut, distance, behind, height, wading, since), roll);
	}

	/**
	 * How much it wants a spell of its phase: a storm spell more while its prey wades in the flood (the water will carry it to
	 * them), a frost spell less while the pit is dry and there is nothing to freeze.
	 */
	public static double spellWeight(boolean storm, boolean frost, boolean flooded, boolean wading) {
		double w = SPELL;
		if (storm && flooded && wading) {
			w *= 1.6;
		}
		if (frost && !flooded) {
			w *= 0.6;
		}
		return w;
	}

	/**
	 * In its third phase, what it follows {@code move} with in the same breath, the pit {@code flooded} or not, its prey
	 * {@code distance} off, {@code roll} 0-1; -1 for nothing. The riptide sweeps you out into the deep and the whirlpool takes you;
	 * a volley drives you onto the floor it then writes on; ink blinds you for the dive.
	 */
	public static int chain(int move, boolean flooded, double distance, double roll) {
		if (roll >= 0.5) {
			return -1;
		}
		int next = switch (move) {
			case RIPTIDE -> WHIRLPOOL;
			case QUILLS -> SCRIPT;
			case INK -> flooded ? DIVE : QUILLS;
			case PAGES -> BREAKER;
			case BREAKER -> flooded ? RIPTIDE : distance > PAGE_OUTER ? QUILLS : PAGES;
			case TIDE_CALL -> BREAKER;
			default -> -1;
		};
		return next > 0 && open(next, 3, flooded) ? next : -1;
	}

	/** The tell of {@code move}: how long it winds up before anything lands (none for an opening). */
	public static int tell(int move) {
		return switch (move) {
			case QUILLS -> QUILL_TELL;
			case SCRIPT -> SCRIPT_TELL;
			case WHIRLPOOL -> WHIRL_TELL;
			case RIPTIDE -> RIP_TELL;
			case INK -> INK_TELL;
			case PAGES -> PAGES_TELL;
			case BREAKER -> BREAKER_TELL;
			case DIVE -> DIVE_TELL;
			case TIDE_CALL -> CALL_TELL;
			default -> 0;
		};
	}

	/** How long {@code move} lasts in all in {@code phase}, its tell included (a second go starts afresh, and isn't counted). */
	public static int length(int move, int phase) {
		return switch (move) {
			case QUILLS -> QUILL_TELL + 6;
			case SCRIPT -> SCRIPT_TELL + SCRIPT_FUSE + 2 + (phase >= 3 ? SCRIPT_AGAIN : 0);
			case WHIRLPOOL -> WHIRL_TELL + WHIRL_TICKS;
			case RIPTIDE -> RIP_TELL + RIP_TICKS + 4;
			case INK -> INK_TELL + 8;
			case PAGES -> PAGES_TELL + PAGES_TICKS + (phase >= 3 ? rolls(PAGE_FLY_REACH - PAGE_OUTER, PAGE_FLY_SPEED) : 0);
			case BREAKER -> BREAKER_TELL + rolls(BREAKER_REACH, BREAKER_SPEED) + 2 + (phase >= 3 ? BREAKER_AGAIN : 0);
			case DIVE -> DIVE_TELL + DIVE_HUNT + DIVE_BURST;
			case TIDE_CALL -> CALL_TELL + rolls(CALL_FROM, CALL_SPEED) + 2;
			case BOWED -> BOWED_TICKS;
			case SPUN -> SPUN_TICKS;
			case SURFACED -> SURFACED_TICKS;
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

	/** Whether a quill flying down a lane catches someone {@code forward} along it, {@code side} across and {@code height} above its floor. */
	public static boolean quilled(double forward, double side, double height) {
		return forward >= QUILL_NEAR && forward <= QUILL_REACH && Math.abs(side) <= QUILL_HALF && height > -1.5 && height < 3.0;
	}

	/** Whether a word bursting catches someone {@code dx},{@code dz} from it and {@code height} above it. */
	public static boolean scribed(double dx, double dz, double height) {
		return dx * dx + dz * dz <= GLYPH_RADIUS * GLYPH_RADIUS && height > -1.0 && height < 2.5;
	}

	/** How hard the whirlpool drags someone wading {@code distance} off toward it each tick (harder from the second phase). */
	public static double drag(double distance, int phase) {
		return distance > 1.0 && distance <= WHIRL_RADIUS ? (phase >= 2 ? WHIRL_DRAG_HARD : WHIRL_DRAG) : 0;
	}

	/** Whether the whirlpool's heart churns someone {@code distance} off and {@code height} above its floor. */
	public static boolean churns(double distance, double height) {
		return distance <= WHIRL_CORE && height > -1.5 && height < 3.0;
	}

	/** Whether the riptide's lane takes someone {@code forward} along it, {@code side} across and {@code height} above the floor. */
	public static boolean swept(double forward, double side, double height) {
		return forward > 0.5 && forward <= RIP_REACH && Math.abs(side) <= RIP_HALF && height > -1.5 && height < 3.0;
	}

	/** Whether its ink catches someone {@code forward} ahead, {@code side} across and {@code height} above its feet. */
	public static boolean inked(double forward, double side, double height, boolean wide) {
		return forward > 0 && forward <= INK_REACH && Math.toDegrees(Math.atan2(Math.abs(side), forward)) <= (wide ? INK_ANGLE_WIDE : INK_ANGLE)
			&& height > -3 && height < 3;
	}

	/** Whether the wheeling pages cut someone {@code distance} off and {@code height} above its floor. */
	public static boolean cut(double distance, double height) {
		return distance >= PAGE_INNER && distance <= PAGE_OUTER && height > -1.5 && height < PAGE_HEIGHT;
	}

	/**
	 * Whether a ring rolling along the floor between {@code from} and {@code to} off this tick (either way: out from it or in from
	 * the rim) passes someone {@code distance} off and {@code height} above it without their jumping it.
	 */
	public static boolean crosses(double distance, double from, double to, double height) {
		double lo = Math.min(from, to), hi = Math.max(from, to);
		return distance > lo - 0.5 && distance <= hi + 0.5 && height > -1.5 && height < WAVE_HEIGHT;
	}

	/** Whether its burst up out of the water catches someone {@code dx},{@code dz} from the spot and {@code height} above the floor. */
	public static boolean burst(double dx, double dz, double height) {
		return dx * dx + dz * dz <= DIVE_RADIUS * DIVE_RADIUS && height > -1.5 && height < 3.5;
	}

	/** Where its trail of bubbles goes next, {@code from} toward its prey {@code to}, one coordinate: no faster than {@link #DIVE_FOLLOW}. */
	public static double follow(double from, double to, double step) {
		double d = to - from;
		return Math.abs(d) <= step ? to : from + Math.signum(d) * step;
	}

	/** How much of the way {@code x},{@code z} off its pit's heart it may go, to stay within {@link #ROAM}: 1 for all of it. */
	public static double keep(double x, double z) {
		double d = Math.sqrt(x * x + z * z);
		return d <= ROAM ? 1 : ROAM / d;
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
