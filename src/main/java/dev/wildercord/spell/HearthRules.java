package dev.wildercord.spell;

/**
 * The pure numbers of the hearth pack (see {@code cast.HearthEffects}): gentle, long-lasting self and ally
 * utility. Kept free of Minecraft so the rules are unit tested. Every timer here refreshes, never stacks.
 */
public final class HearthRules {
	private HearthRules() {}

	// ---- How long each lasts, in seconds (Extend stretches them; a passive's own renewals cap them).
	public static final int LONG = 600, MID = 300, SHORT = 180, BRIEF = 60;
	/** Camp Ward, Ember Rest, Rally Light: a place's magic, one per caster. */
	public static final int WARD_RADIUS = 8, EMBER_RADIUS = 6, RALLY_RADIUS = 10;
	public static final int WAYMARKS = 3, WAYMARK_SIGHT = 160;
	public static final int TRAIL_STEP = 6, TRAIL_CRUMBS = 40;
	public static final int HOMEWARD_CHANNEL = 60, HOMEWARD_REST = 2400, HOMEWARD_RANGE = 2000;
	public static final int POCKET_SLOTS = 9;
	public static final int TINKER_EVERY = 200, TINKER_MAX = 30;
	public static final int DYNAMO_STEP = 24, DYNAMO_MANA = 2, DYNAMO_MAX = 12;
	public static final float STILL_MANA = 0.3f, STILL_MAX = 12;
	public static final int STILL_AFTER = 60;
	public static final double GLIDE_CAP = 1.2, GLIDE_PUSH = 0.04, KEEL_CAP = 0.5, KEEL_PUSH = 0.015;
	public static final int SOFTFOOT_SIGHT = 8, GRUDGE_TICKS = 200;
	public static final float BOND_LOW = 6;
	public static final int BOND_REST = 1200;

	/** A recast's new end: whichever is later, so casting twice never adds the two together. */
	public static long refreshed(long current, long now, int ticks) {
		return Math.max(current, now + Math.max(0, ticks));
	}

	/** Slowburn: half of what hunger took comes back, carried over in fractions. Returns {refund, carry}. */
	public static float[] slowburn(float drop, float carry) {
		if (drop <= 0) {
			return new float[] {0, carry};
		}
		float owed = carry + drop * 0.5f;
		float whole = (float) Math.floor(owed * 4) / 4;
		return new float[] {whole, owed - whole};
	}

	/** Keenkeep: half the wear a tool took is undone; an odd point carries to the next. Returns {kept, carry}. */
	public static int[] keenkeep(int wear, int carry) {
		if (wear <= 0) {
			return new int[] {0, carry};
		}
		int owed = wear + carry;
		return new int[] {owed / 2, owed % 2};
	}

	/** Tarry: a helpful effect gets back half of the 40 ticks it just lost, if it is still long enough to matter. */
	public static int tarry(int remaining, boolean infinite) {
		return infinite || remaining <= 40 ? 0 : 20;
	}

	/** Clot: poison and wither lose an extra second each second (never ended outright by it). */
	public static int clot(int remaining) {
		return Math.max(1, remaining - 20);
	}

	/** Savor: food just eaten gives half its points again as saturation, never past the food level. */
	public static float savor(int gained, int food, float saturation) {
		if (gained <= 0) {
			return saturation;
		}
		return Math.min(food, saturation + gained * 0.5f);
	}

	/** Dynamo Stride: mana for the ground walked, in steps of {@link #DYNAMO_STEP} blocks, up to the cap per cast. */
	public static int dynamo(double walked, int given) {
		int steps = (int) (walked / DYNAMO_STEP);
		return Math.max(0, Math.min(steps * DYNAMO_MANA, DYNAMO_MAX - given));
	}

	/** Stillwell: mana a second once you've stood still long enough, up to the cap per cast. */
	public static float stillwell(int stillTicks, float given) {
		return stillTicks < STILL_AFTER ? 0 : Math.max(0, Math.min(STILL_MANA, STILL_MAX - given));
	}

	/** Glidewind and Keelwind: a push only below the cap, so it keeps speed but never builds it past. */
	public static double push(double speed, double cap, double push) {
		return speed >= cap ? 0 : Math.min(push, cap - speed);
	}

	/** Homeward: a lodestar in this dimension and in range. */
	public static boolean homeward(boolean sameDimension, double distanceSqr) {
		return sameDimension && distanceSqr <= (double) HOMEWARD_RANGE * HOMEWARD_RANGE;
	}

	/** Camp Ward: a monster that only just appeared, nobody named and nothing keeps. */
	public static boolean freshSpawn(int age, boolean persistent, boolean named) {
		return age < 40 && !persistent && !named;
	}

	/** Softfoot: a monster this far off forgets you, unless you struck it lately. */
	public static boolean loses(double distanceSqr, int sinceStruck) {
		return distanceSqr > SOFTFOOT_SIGHT * SOFTFOOT_SIGHT && sinceStruck > GRUDGE_TICKS;
	}

	/** Whether a once-in-a-while thing (a bond's call, a warning) may happen again. */
	public static boolean ready(long last, long now, int gap) {
		return now - last >= gap;
	}

	/** Trailblaze: a crumb every few blocks walked. */
	public static boolean crumb(double movedSqr) {
		return movedSqr >= TRAIL_STEP * TRAIL_STEP;
	}

	/** The moon's phase on a day: 0 full moon to 7. */
	public static int moon(long dayTime) {
		return (int) Math.floorMod(dayTime / 24000L, 8L);
	}

	/** Ticks as whole minutes, rounded up, at least 1. */
	public static int minutes(int ticks) {
		return Math.max(1, (ticks + 1199) / 1200);
	}

	/** The time of day, 0 to 23999, as a clock's hours (tick 0 is 6 o'clock). */
	public static int hour(long dayTime) {
		return (int) ((Math.floorMod(dayTime, 24000L) / 1000 + 6) % 24);
	}

	/** Delvesense: a drop worth a warning. */
	public static boolean drop(int airBelow) {
		return airBelow >= 6;
	}
}
