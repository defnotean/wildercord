package dev.wildercord.cast;

/**
 * How the Archivist fights, kept free of the game so it can be tested. Between its spells it has
 * tricks of its own, each played from its tome: it never makes the same one twice running, and it
 * leans toward what suits where its foe stands (a clap of its tome for anyone close, a line of ink
 * or a volley of books for anyone keeping away, a mark that follows anyone up out of reach). Each
 * trick has a tell of its own and a way out of it, and three leave it open after: its tome shut,
 * its pages wiped blank, or leafing through them for its place. Each rewrite of its Cord teaches it
 * more tricks; in the last it plays them in pairs, rests less between them and harder.
 * <p>
 * Its own tricks and its phase's spells share one deck (see {@link MoveDeck}), so a spell can follow
 * a trick and a trick a spell, but never the same twice running.
 */
public final class ArchivistRules {
	private ArchivistRules() {
	}

	/** Free: casting, choosing or resting. */
	public static final int NONE = 0;
	/**
	 * Books lift from the stacks and wheel about it as it raises its arms, then fly up and out; a
	 * mark opens on the floor under each foe and a few more about them, and the books come down on
	 * the marks a moment later. Step off the marks.
	 */
	public static final int TOMES = 1;
	/**
	 * Its quill drawn back over its shoulder while a line of ink runs across the floor after its foe;
	 * the line turns gold as its aim settles, and it slashes the line out across the room. Sidestep.
	 * From its second Cord it writes three lines in a fan.
	 */
	public static final int QUILL = 2;
	/**
	 * Its tome raised high over its head, the words gathering; then it reads aloud and a ring of
	 * script rolls out along the floor from its feet. Jump the ring. In its last Cord a second ring,
	 * faster, follows the first.
	 */
	public static final int RECITE = 3;
	/**
	 * Its tome thrust out before it and opened wide, the light drawn in toward the pages and a circle
	 * marked about it; then the tome claps shut, throwing everyone in the circle away. Back out of the
	 * circle. In its last Cord the open pages pull at anyone near. Its tome stays shut a while after:
	 * the opening.
	 */
	public static final int SNAP = 4;
	/**
	 * Its hand drawn across its body, a pale cone marked on the floor before it; it sweeps the hand
	 * out and wipes the cone clean, burning whoever is in it and stripping one of their blessings.
	 * Get behind it, or right in under it. Its own pages are wiped blank a while after: the opening.
	 */
	public static final int ERASE = 5;
	/**
	 * Chains of script run from its hands to everyone it can see; when it pulls them taut, whoever it
	 * can still see is bound, burned and slowed. Break its sight: get behind a pillar, or far enough
	 * off. In its last Cord the chains also drag the bound toward it.
	 */
	public static final int BIND = 6;
	/**
	 * Its hand raised straight up while a gold mark opens under its foe and creeps after them; the
	 * mark stops, and it steps through the page onto it in a burst. Keep moving and the mark can't
	 * keep up. It lands leafing through its pages for its place: the opening.
	 */
	public static final int BOOKMARK = 7;
	/**
	 * A finger raised to its hood; a seal hangs over everyone near, and when it closes it calls for
	 * quiet. Anyone who walks while the hush holds is struck down. Stand still, and strike it from
	 * where you stand while it holds the room.
	 */
	public static final int HUSH = 8;
	/** Its tome clapped shut and sagging, after a {@link #SNAP}: it takes more from every blow. */
	public static final int SHUT = 9;
	/** Its own pages wiped blank, after an {@link #ERASE}: it takes the most from every blow. */
	public static final int BLANK = 10;
	/** Leafing through its pages for its place, after a {@link #BOOKMARK}: it takes more from every blow. */
	public static final int LEAFING = 11;
	/** How many move ids there are, nothing included, for the client's per-move blends. */
	public static final int MOVES = 12;

	/** The secret spell's place in its deck and among its rests. */
	public static final int SUNFALL = 12;
	/** How many places its rests have: every move id, and the secret spell. */
	public static final int RESTS = 13;
	/** Spell {@code i} of its phase is {@code SPELL + i} in its deck. */
	public static final int SPELL = 100;
	/** How many of its last picks it remembers, so as not to fall back into a habit. */
	public static final int MEMORY = 4;
	/** The tricks it picks from when it is free to choose, alongside its spells. */
	public static final int[] PICKS = {TOMES, QUILL, RECITE, SNAP, ERASE, BIND, BOOKMARK, HUSH};

	/** Where it puts anything it lays down: within this of the Archive's heart, across the floor. */
	public static final double ARENA = 11;
	/** How high above its feet a foe counts as up out of reach (on a pillar, say). */
	public static final double UP = 2.5;
	/** The furthest its tricks reach for a foe; beyond, it casts or walks closer. */
	public static final double REACH = 20;
	/** How far round from its front a foe counts as behind it, in degrees. */
	public static final double BEHIND = 125;
	/** How near it a foe counts toward a crowd. */
	public static final double CROWD = 6;

	/**
	 * The volley of books: its tell, the beat after, how long the marks wait before the books land,
	 * when the second volley of its last Cord is laid; how wide a mark is, how far the scattered ones
	 * fall from a foe, what a book does and how high it throws.
	 */
	public static final int TOMES_TELL = 16, TOMES_TELL_CHAINED = 10, TOMES_AFTER = 8, TOMES_FALL = 14, TOMES_AGAIN = 12;
	public static final double TOMES_RADIUS = 1.4, TOMES_NEAR = 1.5, TOMES_FAR = 4.5, TOMES_DAMAGE = 6, TOMES_LIFT = 0.35;
	/**
	 * The line of ink: its tell, when its aim locks, the beat after; how far the line runs and how
	 * fast, how wide it cuts, what it does, and how far apart the fanned lines of its later Cords are.
	 */
	public static final int QUILL_TELL = 14, QUILL_TELL_CHAINED = 9, QUILL_LOCK = 4, QUILL_AFTER = 8, QUILL_GROW = 5;
	public static final double QUILL_REACH = 12, QUILL_HALF = 0.9, QUILL_DAMAGE = 7, QUILL_SPREAD = 28;
	/**
	 * Reading aloud: its tell, the beat after, when its last Cord's second ring follows; how fast each
	 * ring rolls, how far, how high off the floor it reaches, and what it does.
	 */
	public static final int RECITE_TELL = 14, RECITE_TELL_CHAINED = 10, RECITE_AFTER = 12, RECITE_AGAIN = 10;
	public static final double RECITE_SPEED = 0.5, RECITE_SPEED_AGAIN = 0.75, RECITE_REACH = 13, RECITE_HEIGHT = 0.9, RECITE_DAMAGE = 5;
	/**
	 * The clap: its tell, the beat after, how long its tome stays shut; how wide the circle is, what
	 * the clap does and how hard it throws, and its last Cord's pull while the tome is open.
	 */
	public static final int SNAP_TELL = 14, SNAP_TELL_CHAINED = 9, SNAP_AFTER = 4, SHUT_TICKS = 30;
	public static final double SNAP_RADIUS = 4.5, SNAP_DAMAGE = 8, SNAP_PUSH = 1.1, SNAP_LIFT = 0.3, SNAP_PULL = 0.04, SNAP_PULL_RANGE = 7;
	/**
	 * The wiping: its tell, when its aim locks, the beat after, how long its pages stay blank; how
	 * near its feet is safe, how far the cone reaches, how wide (and how wide in its last Cord), and
	 * what it does.
	 */
	public static final int ERASE_TELL = 16, ERASE_TELL_CHAINED = 11, ERASE_LOCK = 4, ERASE_AFTER = 6, BLANK_TICKS = 40;
	public static final double ERASE_NEAR = 1.2, ERASE_REACH = 9, ERASE_HALF = 30, ERASE_HALF_LATE = 40, ERASE_DAMAGE = 7;
	/**
	 * The chains: its tell, the beat after, how long the bound are slowed; how far it binds, what a
	 * chain does, and how hard its last Cord's chains drag.
	 */
	public static final int BIND_TELL = 18, BIND_TELL_CHAINED = 12, BIND_AFTER = 8, BIND_SLOW = 40;
	public static final double BIND_RANGE = 16, BIND_DAMAGE = 4, BIND_DRAW = 0.6;
	/**
	 * The bookmark: its tell, when the mark stops, the beat after, how long it leafs for its place;
	 * how wide the mark is, how fast it creeps, what the landing does and how high it throws.
	 */
	public static final int BOOKMARK_TELL = 16, BOOKMARK_TELL_CHAINED = 11, BOOKMARK_LOCK = 6, BOOKMARK_AFTER = 2, LEAFING_TICKS = 30;
	public static final double BOOKMARK_RADIUS = 1.8, BOOKMARK_CREEP = 0.1, BOOKMARK_DAMAGE = 9, BOOKMARK_LIFT = 0.5;
	/**
	 * The hush: its tell, how long the quiet holds; how far it reaches, how far a foe may drift
	 * before it counts as walking, and what breaking the quiet does.
	 */
	public static final int HUSH_TELL = 16, HUSH_TELL_CHAINED = 11, HUSH_HOLD = 40;
	public static final double HUSH_RANGE = 16, HUSH_STEP = 0.9, HUSH_DAMAGE = 8;
	/** The secret spell's rest, between one Sunfall and the next. */
	public static final int SUNFALL_REST = 200;

	/** In its last Cord, the chance one trick runs straight into another; it never plays more than a pair. */
	public static final double CHAIN_CHANCE = 0.6;
	public static final int CHAIN_MOST = 1;
	/** The extra rest it takes after a pair. */
	public static final int CHAIN_REST = 10;

	/** Whether {@code move} is among its tricks in {@code phase}: four from the start, two more with each rewrite. */
	public static boolean unlocked(int move, int phase) {
		return switch (move) {
			case TOMES, QUILL, RECITE, SNAP -> true;
			case ERASE, BIND -> phase >= 2;
			case BOOKMARK, HUSH -> phase >= 3;
			default -> false;
		};
	}

	/** The lang key's last part for {@code move} (see {@code boss.wildercord.archivist.move.*}), or null for none. */
	public static String key(int move) {
		return switch (move) {
			case TOMES -> "tomes";
			case QUILL -> "quill";
			case RECITE -> "recite";
			case SNAP -> "snap";
			case ERASE -> "erase";
			case BIND -> "bind";
			case BOOKMARK -> "bookmark";
			case HUSH -> "hush";
			case SHUT -> "shut";
			case BLANK -> "blank";
			case LEAFING -> "leafing";
			default -> null;
		};
	}

	/** How long after making {@code move} (or the secret spell) before it makes it again, in {@code phase}; a third less in its last. */
	public static int rest(int move, int phase) {
		int rest = switch (move) {
			case TOMES -> 60;
			case QUILL -> 40;
			case RECITE -> 80;
			case SNAP -> 60;
			case ERASE -> 100;
			case BIND -> 200;
			case BOOKMARK -> 120;
			case HUSH -> 300;
			case SUNFALL -> SUNFALL_REST;
			default -> 0;
		};
		return phase >= 3 ? rest * 2 / 3 : rest;
	}

	/** How long {@code move}'s tell is, shorter when it follows straight on from another. */
	public static int tell(int move, boolean chained) {
		return switch (move) {
			case TOMES -> chained ? TOMES_TELL_CHAINED : TOMES_TELL;
			case QUILL -> chained ? QUILL_TELL_CHAINED : QUILL_TELL;
			case RECITE -> chained ? RECITE_TELL_CHAINED : RECITE_TELL;
			case SNAP -> chained ? SNAP_TELL_CHAINED : SNAP_TELL;
			case ERASE -> chained ? ERASE_TELL_CHAINED : ERASE_TELL;
			case BIND -> chained ? BIND_TELL_CHAINED : BIND_TELL;
			case BOOKMARK -> chained ? BOOKMARK_TELL_CHAINED : BOOKMARK_TELL;
			case HUSH -> chained ? HUSH_TELL_CHAINED : HUSH_TELL;
			default -> 0;
		};
	}

	/** How long {@code move} goes on after its tell: the beat after it lands, or the hush's hold. */
	public static int after(int move) {
		return switch (move) {
			case TOMES -> TOMES_AFTER;
			case QUILL -> QUILL_AFTER;
			case RECITE -> RECITE_AFTER;
			case SNAP -> SNAP_AFTER;
			case ERASE -> ERASE_AFTER;
			case BIND -> BIND_AFTER;
			case BOOKMARK -> BOOKMARK_AFTER;
			case HUSH -> HUSH_HOLD;
			default -> 0;
		};
	}

	/** How many ticks before {@code move} lands its aim stops following its foe (nothing for those that don't aim). */
	public static int lock(int move) {
		return switch (move) {
			case QUILL -> QUILL_LOCK;
			case ERASE -> ERASE_LOCK;
			case BOOKMARK -> BOOKMARK_LOCK;
			default -> 0;
		};
	}

	/** The opening {@code move} leaves it in once it lands, or nothing. */
	public static int opening(int move) {
		return switch (move) {
			case SNAP -> SHUT;
			case ERASE -> BLANK;
			case BOOKMARK -> LEAFING;
			default -> NONE;
		};
	}

	/** How long the opening {@code state} lasts. */
	public static int openFor(int state) {
		return switch (state) {
			case SHUT -> SHUT_TICKS;
			case BLANK -> BLANK_TICKS;
			case LEAFING -> LEAFING_TICKS;
			default -> 0;
		};
	}

	/** What every blow does to it while it is in {@code move}, as a share: more while it is open. */
	public static float taken(int move) {
		return switch (move) {
			case SHUT -> 1.35F;
			case BLANK -> 1.5F;
			case LEAFING -> 1.3F;
			default -> 1.0F;
		};
	}

	/** Whether {@code move} is one of its openings, where it stands spent rather than doing anything. */
	public static boolean open(int move) {
		return move == SHUT || move == BLANK || move == LEAFING;
	}

	/** How long it waits after {@code made} (one of its tricks, a spell or the secret) before choosing again, {@code roll} being 0-1. */
	public static int pause(int made, int phase, double roll) {
		double r = Math.max(0, Math.min(1, roll));
		if (made >= SPELL || made == SUNFALL) {
			return (phase >= 3 ? 30 : phase == 2 ? 40 : 48) + (int) (24 * r);
		}
		return phase >= 3 ? 12 + (int) (12 * r) : phase == 2 ? 20 + (int) (16 * r) : 26 + (int) (20 * r);
	}

	/** How many marks a volley of books scatters about its foes, besides one under each. */
	public static int scatter(int phase) {
		return phase >= 2 ? 5 : 3;
	}

	/** How many lines its quill writes at once. */
	public static int quillLines(int phase) {
		return phase >= 2 ? 3 : 1;
	}

	/**
	 * What it runs straight into after {@code move} in its last Cord, its foe {@code distance} off,
	 * {@code roll} being 0-1; -1 for nothing. A line of ink reaches for a volley at range or a clap up
	 * close, reading aloud for a line, a volley for its bookmark, and the hush ends in a volley on
	 * whoever stood still for it. The tricks that leave it open never chain.
	 */
	public static int chain(int move, double distance, boolean sight, double roll) {
		if (roll >= CHAIN_CHANCE) {
			return -1;
		}
		return switch (move) {
			case QUILL -> distance >= 6 ? TOMES : distance <= 4 ? SNAP : -1;
			case RECITE -> sight ? QUILL : TOMES;
			case TOMES -> BOOKMARK;
			case HUSH -> TOMES;
			default -> -1;
		};
	}

	/** Everything it can choose among in {@code phase} with {@code spells} spells: its tricks, the secret, then each spell. */
	public static int[] options(int phase, int spells) {
		int[] options = new int[PICKS.length + 1 + spells];
		System.arraycopy(PICKS, 0, options, 0, PICKS.length);
		options[PICKS.length] = SUNFALL;
		for (int i = 0; i < spells; i++) {
			options[PICKS.length + 1 + i] = SPELL + i;
		}
		return options;
	}

	/**
	 * How much it wants each of {@link #options} right now: its foe {@code distance} off across the
	 * floor and {@code height} above its feet, {@code behind} it or not, in its sight or not, with
	 * {@code crowd} foes about it, and {@code silenced} (a seal on its casting: no spells, though its
	 * tricks still work). {@code since} holds how long since each of its tricks and the secret (by id)
	 * were last made; nothing still resting, or not yet learned, can be chosen.
	 */
	public static double[] weights(int phase, int spells, double distance, boolean behind, double height, boolean sight, int crowd,
			boolean silenced, long[] since) {
		int[] options = options(phase, spells);
		double[] weights = new double[options.length];
		for (int i = 0; i < options.length; i++) {
			int option = options[i];
			if (option >= SPELL) {
				weights[i] = sight && !silenced && distance <= 24 ? (phase == 1 ? 1.1 : 0.9) : 0;
				continue;
			}
			if (since[option] < rest(option, phase)) {
				continue;
			}
			if (option == SUNFALL) {
				weights[i] = phase >= 3 && sight && !silenced ? 0.7 : 0;
				continue;
			}
			if (unlocked(option, phase)) {
				weights[i] = want(option, distance, behind, height, sight, crowd);
			}
		}
		return weights;
	}

	/** How much it wants trick {@code move} where its foe stands (rests and phases aside). */
	static double want(int move, double distance, boolean behind, double height, boolean sight, int crowd) {
		boolean up = height > UP;
		if (move == TOMES) {
			return distance > 24 ? 0 : up ? 2.5 : distance >= 5 ? 2.0 : 0.6;
		}
		if (distance > REACH) {
			return 0;
		}
		return switch (move) {
			case QUILL -> !sight || up ? 0 : distance < 3 ? 0.3 : distance <= 12 ? 2.2 : 1.0;
			case RECITE -> up ? 0 : distance <= 10 ? 1.5 + 0.6 * Math.max(0, crowd - 1) : 0.4;
			case SNAP -> up ? 0 : distance <= 4 ? (behind ? 3.5 : 3.0) : distance <= 6 ? 1.0 : 0;
			case ERASE -> !sight || behind ? 0 : up ? 0.5 : distance >= 2 && distance <= 9 ? 2.5 : 0.5;
			case BIND -> !sight ? 0 : up ? 2.0 : distance >= 6 ? 2.0 + (crowd >= 2 ? 0.5 : 0) : 0.6;
			case BOOKMARK -> up ? 2.0 : distance >= 7 ? 2.5 : 0.5;
			case HUSH -> 1.2 + (behind ? 1.0 : 0) + (distance <= 4 ? 0.5 : 0);
			default -> 0;
		};
	}

	/** Which of {@link #options} it makes next, {@code roll} being 0-1; -1 if it can make none. See {@link #weights}. */
	public static int choose(MoveDeck deck, int phase, int spells, double distance, boolean behind, double height, boolean sight, int crowd,
			boolean silenced, long[] since, double roll) {
		return deck.pick(options(phase, spells), weights(phase, spells, distance, behind, height, sight, crowd, silenced, since), roll);
	}

	// ------------------------------------------------------------------ where its tricks land

	/**
	 * Whether a line cut straight out from it catches someone {@code forward} along the line and
	 * {@code side} off it, {@code height} above its floor, where the line runs from {@code from} to
	 * {@code to} blocks out.
	 */
	public static boolean lane(double forward, double side, double height, double from, double to) {
		return forward > from - 0.3 && forward <= to + 0.3 && Math.abs(side) <= QUILL_HALF + 0.3 && height > -1.5 && height < 2.5;
	}

	/** How far out a line of ink has run {@code elapsed} ticks after it was slashed. */
	public static double quillFront(int elapsed) {
		return 1 + (QUILL_REACH - 1) * Math.min(1, Math.max(0, elapsed) / (double) QUILL_GROW);
	}

	/** How far a ring read aloud has rolled {@code elapsed} ticks after it set out at {@code speed}. */
	public static double wave(int elapsed, double speed) {
		return Math.min(RECITE_REACH, Math.max(0, elapsed) * speed);
	}

	/** Whether a ring rolling from {@code from} to {@code to} blocks out this tick catches someone {@code distance} off, {@code height} above its floor. */
	public static boolean crosses(double distance, double from, double to, double height) {
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < RECITE_HEIGHT;
	}

	/** Whether someone {@code dx}, {@code dz} from the middle of a mark {@code radius} wide, {@code height} above it, is on it. */
	public static boolean inMark(double dx, double dz, double height, double radius) {
		double r = radius + 0.3;
		return dx * dx + dz * dz <= r * r && height > -1 && height < 3;
	}

	/** Whether the clap of its tome catches someone {@code distance} off across the floor, {@code height} above its feet. */
	public static boolean snapHits(double distance, double height) {
		return distance <= SNAP_RADIUS + 0.3 && height > -1.5 && height < 3;
	}

	/** Whether the wiping catches someone {@code forward} before it and {@code side} off its line, {@code height} above its feet, in {@code phase}. */
	public static boolean cone(double forward, double side, double height, int phase) {
		if (forward <= ERASE_NEAR || height <= -1.5 || height >= 3) {
			return false;
		}
		double half = phase >= 3 ? ERASE_HALF_LATE : ERASE_HALF;
		return forward * forward + side * side <= Math.pow(ERASE_REACH + 0.3, 2) && Math.toDegrees(Math.atan2(Math.abs(side), forward)) <= half;
	}

	/** Whether someone who has gone {@code dx}, {@code dz} across the floor since the hush fell has walked. */
	public static boolean moved(double dx, double dz) {
		return dx * dx + dz * dz > HUSH_STEP * HUSH_STEP;
	}

	/** A point {@code dx}, {@code dz} from the Archive's heart, brought within {@link #ARENA} of it. */
	public static double[] within(double dx, double dz) {
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d <= ARENA) {
			return new double[] {dx, dz};
		}
		return new double[] {dx * ARENA / d, dz * ARENA / d};
	}

	/** Where a bookmark at {@code mx}, {@code mz} has crept to after a tick following its foe at {@code tx}, {@code tz}. */
	public static double[] creep(double mx, double mz, double tx, double tz) {
		double dx = tx - mx;
		double dz = tz - mz;
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d <= BOOKMARK_CREEP) {
			return new double[] {tx, tz};
		}
		return new double[] {mx + dx / d * BOOKMARK_CREEP, mz + dz / d * BOOKMARK_CREEP};
	}

	/** An angle in degrees brought into -180 to 180. */
	public static double wrap(double degrees) {
		return ((degrees % 360) + 540) % 360 - 180;
	}

	/** Whether something at yaw {@code toward} from it is behind it, facing {@code facing}. */
	public static boolean behind(double facing, double toward) {
		return Math.abs(wrap(toward - facing)) >= BEHIND;
	}
}
