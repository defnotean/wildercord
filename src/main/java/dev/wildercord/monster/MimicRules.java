package dev.wildercord.monster;

/**
 * The Coffer Mimic's rules, apart from the game so they can be tested: when it gives itself away, which move it makes
 * from where, how hard a blow lands on it, and where it settles back down.
 */
public final class MimicRules {
	private MimicRules() {}

	/** How close someone must stand, and how long, before it can't keep still any longer. */
	public static final double WAKE_REACH = 1.6;
	public static final int LINGER = 30;
	/** Within this, now and then its lid twitches (the tell). */
	public static final double TWITCH_RANGE = 6.0;
	public static final int TWITCH_MIN = 60;
	public static final int TWITCH_SPREAD = 100;
	public static final int TWITCH_TICKS = 6;

	/** Waking: the lid flies open, then it bites whoever is right there. */
	public static final int WAKE_TICKS = 14;
	public static final int WAKE_BITE_AT = 6;
	public static final double AMBUSH_REACH = 2.2;
	public static final float AMBUSH_DAMAGE = 6.0F;

	public static final double BITE_REACH = 2.0;
	public static final int BITE_TICKS = 8;
	public static final int BITE_AT = 4;
	public static final int BITE_COOLDOWN = 22;

	public static final double LASH_MIN = 2.5;
	public static final double LASH_MAX = 6.0;
	public static final int LASH_TICKS = 12;
	public static final int LASH_AT = 5;
	public static final int LASH_COOLDOWN = 70;
	public static final float LASH_DAMAGE = 2.0F;

	public static final double LUNGE_MIN = 3.5;
	public static final double LUNGE_MAX = 8.0;
	public static final int LUNGE_CROUCH = 16;
	public static final int LUNGE_COOLDOWN = 110;
	public static final float LUNGE_DAMAGE = 7.0F;
	/** A missed lunge leaves it on its back, legs in the air. */
	public static final int FLIPPED = 50;
	public static final float FLIPPED_SCALE = 1.5F;

	/** Hurt badly and quickly, it clamps shut. */
	public static final int CLAMP_WINDOW = 40;
	public static final float CLAMP_SHARE = 0.3F;
	public static final int CLAMP_TICKS = 30;
	public static final int CLAMP_COOLDOWN = 160;
	public static final float CLAMPED_SCALE = 0.25F;

	/** With no one to chase this long, it goes home and shuts itself up again. */
	public static final int SETTLE_AFTER = 200;
	public static final int SETTLE_GIVE_UP = 600;
	public static final double HOME_REACH = 0.6;

	/** Fed a book, a third of the time it takes to whoever fed it. */
	public static final float TAME_CHANCE = 1.0F / 3.0F;
	/** A tamed one's belly holds as much as a chest. */
	public static final int STORAGE_ROWS = 3;
	/** How far off its owner may be while it's open: past this the lid shuts on them. */
	public static final double STORAGE_REACH = 8.0;
	/** Its owner further off than this, it gets up and follows; past the far mark, it skips the walk and turns up beside them. */
	public static final double FOLLOW_START = 6.0;
	public static final double FOLLOW_STOP = 2.5;
	public static final double FOLLOW_BLINK = 16.0;
	/** Beside its owner and left to itself this long, it shuts up as a coffer again. */
	public static final int SETTLE_BESIDE = 40;
	public static final float BOOK_HEAL = 4.0F;

	public enum Move { BITE, LASH, LUNGE, CHASE }

	/** What a tamed one does about its owner: nothing, walk over, or turn up beside them. */
	public enum Heel { STAY, WALK, BLINK }

	/** Whether someone standing this close (flat distance and height apart) for this many ticks wakes it. */
	public static boolean wakes(double flat, double dy, int lingered) {
		return flat <= WAKE_REACH && Math.abs(dy) <= 1.5 && lingered >= LINGER;
	}

	/** What it does next, from how far off its prey is and which moves are ready. */
	public static Move choose(double distance, boolean sight, boolean biteReady, boolean lashReady, boolean lungeReady) {
		if (distance <= BITE_REACH) {
			return biteReady ? Move.BITE : Move.CHASE;
		}
		if (sight && lashReady && distance >= LASH_MIN && distance <= LASH_MAX) {
			return Move.LASH;
		}
		if (sight && lungeReady && distance >= LUNGE_MIN && distance <= LUNGE_MAX) {
			return Move.LUNGE;
		}
		return Move.CHASE;
	}

	/** How much of a blow lands: clamped shut it turns most of it; flipped on its back it's soft. */
	public static float damageScale(boolean clamped, boolean flipped) {
		return clamped ? CLAMPED_SCALE : flipped ? FLIPPED_SCALE : 1.0F;
	}

	/** Whether this much harm inside the window makes it clamp shut. */
	public static boolean clamps(float recent, float maxHealth) {
		return maxHealth > 0 && recent >= maxHealth * CLAMP_SHARE;
	}

	/** A facing snapped to the nearest of the four sides, as a chest stands. */
	public static float snapYaw(float yaw) {
		float wrapped = ((yaw % 360) + 360) % 360;
		return (Math.round(wrapped / 90.0F) % 4) * 90.0F;
	}

	/** Whether a book fed to it with this roll (0 to 1) tames it. */
	public static boolean tames(float roll) {
		return roll < TAME_CHANCE;
	}

	/** What a tamed one does with its owner this far off, if it's been told to stay put, or is busy already. */
	public static Heel heel(double distance, boolean staying, boolean following) {
		if (staying) {
			return Heel.STAY;
		}
		if (distance > FOLLOW_BLINK) {
			return Heel.BLINK;
		}
		return distance > (following ? FOLLOW_STOP : FOLLOW_START) ? Heel.WALK : Heel.STAY;
	}

	/** Whether its belly stays open for someone: only its living self, for its owner, near enough. */
	public static boolean opensFor(boolean alive, boolean owner, double distance) {
		return alive && owner && distance <= STORAGE_REACH;
	}
}
