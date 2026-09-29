package dev.wildercord.cast;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pure rules behind fire's blasts and blood's cuts, kept apart from the effects so the unit tests can reach them: how
 * blasts of one cast are merged and never stack on one enemy, how far a Cleave swings, how a Sanguine Rite's price grows, when
 * a Bleed counts as moving, and what Overdrive's pain adds.
 */
public final class FireBloodRules {
	private FireBloodRules() {}

	/** Blasts one cast may set off at once, and how close (in radii) two blast centres must be to count as one. */
	public static final int MAX_BLASTS = 3;
	public static final int MAX_METEORS = 2;
	public static final double BLAST_MERGE = 0.8;

	/** The blast centres to use: each point in turn unless it is within {@code gap} of one already kept, at most {@code max}. */
	public static List<Vec3> clusterCentres(List<Vec3> points, double gap, int max) {
		List<Vec3> kept = new ArrayList<>();
		for (Vec3 p : points) {
			if (kept.size() >= max) {
				break;
			}
			boolean near = false;
			for (Vec3 k : kept) {
				if (k.distanceTo(p) < gap) {
					near = true;
					break;
				}
			}
			if (!near) {
				kept.add(p);
			}
		}
		return kept;
	}

	/**
	 * What a blast of {@code amount} adds for {@code id} given what this cast's blasts have already dealt it ({@code landed}):
	 * only what it has over the strongest so far, and it records itself. 0 when an earlier blast was at least as strong.
	 */
	public static double unstacked(Map<UUID, Double> landed, UUID id, double amount) {
		double extra = amount - landed.getOrDefault(id, 0.0);
		if (extra > 0) {
			landed.put(id, amount);
		}
		return Math.max(0, extra);
	}

	/** Cleave's damage before power: 6 plus 10% of the target's max health, 20 more at most. */
	public static double cleaveDamage(double maxHealth) {
		return 6 + Math.min(20, 0.10 * maxHealth);
	}

	/** Sanguine Rite's health price: 3, plus 1 per Amplify, 2 per Overcharge, and 1 for every four victims after the first. */
	public static int sanguinePrice(int amplify, int overcharge, int victims) {
		return 3 + amplify + 2 * overcharge + Math.max(0, victims - 1) / 4;
	}

	/** Blocks a Bleed's bearer must have moved in half a second for its wound to tear wider. */
	public static final double BLEED_MOVING = 0.6;

	/** Overdrive's Strength amplifier: 1 (II) plus Amplify, plus 1 below half health and 2 below a quarter, 3 (IV) at most. */
	public static int painStrength(float health, float max, int amplify) {
		float fraction = health / max;
		int pain = fraction < 0.25F ? 2 : fraction < 0.5F ? 1 : 0;
		return Math.min(3, 1 + amplify + pain);
	}
}
