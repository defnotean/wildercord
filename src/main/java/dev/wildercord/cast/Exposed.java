package dev.wildercord.cast;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Exposed mark: arcane's small seam for "this creature's weak point is showing". Harm leaves it for
 * {@link #HARM_TICKS}, Reveal for its whole glow. It counts as one mark for Unweave and Prismatic Burst
 * (and is used up by them), and the sky strikers (Starfall, Cometfall) look for exposed enemies first.
 * Nothing here deals damage; a rune only asks {@link #mark}, {@link #has}, {@link #clear} or {@link #first}.
 */
public final class Exposed {
	private Exposed() {}

	/** How long Harm leaves a creature exposed: 3 seconds. */
	public static final int HARM_TICKS = 60;

	/** Leaves {@code target} exposed for {@code ticks} (a longer mark is never shortened). */
	public static void mark(Entity target, int ticks) {
		Reactions.mark(target, Reactions.Mark.EXPOSED, ticks);
	}

	public static boolean has(Entity target) {
		return Reactions.has(target, Reactions.Mark.EXPOSED);
	}

	/** Takes the mark off {@code target}. */
	public static void clear(Entity target) {
		Reactions.clear(target, Reactions.Mark.EXPOSED);
	}

	/** Exposed creatures first, then the rest, each nearest {@code from} first (for strikes that choose their targets). */
	public static List<LivingEntity> first(List<LivingEntity> targets, Vec3 from) {
		List<LivingEntity> sorted = new ArrayList<>(targets);
		sorted.sort(Comparator.<LivingEntity>comparingInt(t -> has(t) ? 0 : 1).thenComparingDouble(t -> t.distanceToSqr(from)));
		return sorted;
	}
}
