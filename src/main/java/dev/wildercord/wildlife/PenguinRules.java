package dev.wildercord.wildlife;

/**
 * The floe penguin's numbers and decisions, kept pure for its tests: when it flops onto its belly and toboggans, when it
 * goes fishing and what it catches, who gets the fish, when the colony huddles for the night, and how long a fish buys its
 * trust.
 */
public final class PenguinRules {
	private PenguinRules() {}

	/** Sliding on its belly over ice and snow it goes this much faster; it slides once hurrying at least this much. */
	public static final float SLIDE_BOOST = 1.9F;
	public static final double SLIDE_HURRY = 1.15;
	/** How far it looks for open water, how long it swims about after a fish, and when it gives a trip up. */
	public static final int FISH_RANGE = 10;
	public static final int FISH_MIN = 60, FISH_MAX = 140;
	public static final int TRIP_TICKS = 600;
	/** Between trips: alone now and then; for a friend, eagerly. */
	public static final int FISH_COOLDOWN = 1400;
	public static final int GIFT_COOLDOWN = 360;
	/** How near the one it brings a fish to it has to be to hand it over. */
	public static final double HAND_RANGE = 1.6;
	/** How far it looks for a chick to feed, and for the rest of its colony to huddle with. */
	public static final double CHICK_RANGE = 12;
	public static final double HUDDLE_RANGE = 14;
	/** In the huddle: this close to the heart of it, it settles. */
	public static final double HUDDLE_CLOSE = 1.3;
	/** How long a fish buys its trust. */
	public static final int TRUST_TICKS = 4800;
	public static final double FOLLOW_FAR = 6.0, FOLLOW_NEAR = 3.0;

	/** What it carries in its beak. */
	public static final int NOTHING = 0, COD = 1, SALMON = 2;
	/** Who a caught fish goes to. */
	public enum Mouth { FRIEND, CHICK, SELF }

	/** Whether it flops down and slides: on ice or snow, on the ground, and in a hurry. */
	public static boolean slides(boolean slick, boolean grounded, double hurry) {
		return slick && grounded && hurry >= SLIDE_HURRY;
	}

	/** Its speed, sliding or not. */
	public static float speed(float speed, boolean sliding) {
		return sliding ? speed * SLIDE_BOOST : speed;
	}

	/** Whether it sets off fishing: grown, idle, empty-beaked, and its last trip long enough ago (sooner with a friend). */
	public static boolean fishes(boolean grown, boolean idle, int carrying, long sinceLast, boolean friend) {
		return grown && idle && carrying == NOTHING && (sinceLast < 0 || sinceLast >= (friend ? GIFT_COOLDOWN : FISH_COOLDOWN));
	}

	/** How long it swims about before it comes up with a fish, from a 0 to 1 roll. */
	public static int fishLength(double roll) {
		double r = Math.max(0, Math.min(1, roll));
		return (int) Math.round(FISH_MIN + (FISH_MAX - FISH_MIN) * r);
	}

	/** What it comes up with, from a 0 to 1 roll: mostly cod, now and then a salmon. */
	public static int catchOf(double roll) {
		return roll < 0.7 ? COD : SALMON;
	}

	/** Who gets the fish: its friend first, then a hungry chick, else itself. */
	public static Mouth mouth(boolean friend, boolean chick) {
		return friend ? Mouth.FRIEND : chick ? Mouth.CHICK : Mouth.SELF;
	}

	/** Whether it goes to huddle: at night, grown, with others of its colony about, and no friend to follow. */
	public static boolean huddles(boolean night, boolean grown, int others, boolean friend) {
		return night && grown && others > 0 && !friend;
	}
}
