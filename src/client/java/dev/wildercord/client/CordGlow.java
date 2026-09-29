package dev.wildercord.client;

import net.minecraft.world.entity.Avatar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How brightly a Cord's beads glow while its wearer isn't casting: fully for the first couple of
 * seconds after it's put on, then fading out, so the glow says "just equipped" instead of being on all
 * the time (charging and casting light the beads up again, see AvatarRendererMixin). Only a Cord seen
 * going on counts: a player who walks into view already wearing one, or your own Cord as you join,
 * doesn't flare.
 */
public final class CordGlow {
	private CordGlow() {}

	/** Full glow for this long after the Cord goes on, in ticks. */
	private static final int HOLD = 40;
	/** Then it fades out over this long. */
	private static final int FADE = 20;
	/**
	 * A body younger than this (respawned, or just come into view) may be drawn before its Cord's look
	 * arrives: the server sends looks twice a second, and doesn't keep one through a respawn.
	 */
	private static final int SETTLE = 20;

	private record Seen(String tier, long since) {}

	private static final Map<UUID, Seen> SEEN = new HashMap<>();

	/** 0 to 1: the idle glow of {@code avatar}'s Cord of {@code tier} ("" when it wears none). */
	public static float idle(Avatar avatar, String tier, float partial) {
		long now = avatar.level().getGameTime();
		Seen seen = SEEN.get(avatar.getUUID());
		if (seen == null) {
			// First sight: whatever it wears was on before we could see it go on.
			SEEN.put(avatar.getUUID(), new Seen(tier, Long.MIN_VALUE / 2));
			return 0;
		}
		if (!seen.tier().equals(tier)) {
			// A Cord put on, or swapped for another: it glows from now. Taken off: nothing to glow. A Cord
			// that shows up on a body only just made was worn all along, its look just late (see SETTLE).
			boolean late = seen.tier().isEmpty() && avatar.tickCount < SETTLE;
			seen = new Seen(tier, tier.isEmpty() || late ? Long.MIN_VALUE / 2 : now);
			SEEN.put(avatar.getUUID(), seen);
		}
		float age = now - seen.since() + partial;
		if (age < HOLD) {
			return 1;
		}
		return age < HOLD + FADE ? 1 - (age - HOLD) / FADE : 0;
	}

	/** Forgets everyone (on leaving a world), so nobody flares on the next one. */
	public static void clear() {
		SEEN.clear();
	}
}
