package dev.wildercord.chorus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Chorus casting, as plain rules (no Minecraft types, so they're unit-tested): casters who cast
 * the same shape within a second of each other, close together and at the same place or foe,
 * sing one spell between them. Each voice after the first adds half again to its power (and a
 * Widen to a shape with a size), up to three voices. Only allies sing together, and only spells of
 * the same kind (harmful with harmful, helpful with helpful).
 */
public final class ChorusRules {
	private ChorusRules() {}

	/** Voices must come within a second of the last one. */
	public static final int WINDOW = 20;
	/** How close the casters must stand to each other. */
	public static final double RANGE = 12.0;
	/** How close their aim must be: the same foe, or points this near. */
	public static final double AIM = 5.0;
	/** Power added by each voice after the first. */
	public static final double BONUS_PER_VOICE = 0.5;
	/** Voices after the first that still add anything: a chorus of three is as big as it gets. */
	public static final int MAX_EXTRA = 2;

	/** Shapes that can't be sung together: they only ever touch their own caster, or what set them off. */
	public static final Set<String> SOLO = Set.of("wildercord:self", "wildercord:trigger");

	/** What a spell's effects do, for matching voices: {@link #HARMFUL}, {@link #HELPFUL}, {@link #MIXED} or {@link #OTHER}. */
	public static final String HARMFUL = "harmful";
	public static final String HELPFUL = "helpful";
	public static final String MIXED = "mixed";
	public static final String OTHER = "other";

	/** The kind of a spell with harmful and/or helpful effects in it. */
	public static String kind(boolean harmful, boolean helpful) {
		return harmful && helpful ? MIXED : harmful ? HARMFUL : helpful ? HELPFUL : OTHER;
	}

	/**
	 * One cast offered to the choir: who, which shape, when, where the caster stood, where they
	 * aimed (or the foe they aimed at, null for none), in which dimension, and what kind of spell it
	 * is ({@link #kind}).
	 */
	public record Voice(UUID caster, String shape, long time, String dimension, double x, double y, double z,
			double aimX, double aimY, double aimZ, UUID target, String kind) {
		double distanceTo(Voice other) {
			return Math.sqrt(square(x - other.x) + square(y - other.y) + square(z - other.z));
		}

		double aimDistanceTo(Voice other) {
			return Math.sqrt(square(aimX - other.aimX) + square(aimY - other.aimY) + square(aimZ - other.aimZ));
		}

		private static double square(double d) {
			return d * d;
		}
	}

	/** Power of a chorus of {@code voices}: +50% for each voice after the first, up to x2. */
	public static double power(int voices) {
		return 1 + BONUS_PER_VOICE * extra(voices);
	}

	/** Voices after the first that count, and the Widens a sized shape gains: 0 to {@link #MAX_EXTRA}. */
	public static int extra(int voices) {
		return Math.max(0, Math.min(MAX_EXTRA, voices - 1));
	}

	/** Whether {@code later} joins the voice before it in a chorus. */
	public static boolean joins(Voice earlier, Voice later) {
		if (earlier.caster().equals(later.caster()) || SOLO.contains(later.shape()) || !earlier.shape().equals(later.shape())
				|| !earlier.dimension().equals(later.dimension())) {
			return false;
		}
		// A harmful spell and a healing one don't sing together, nor two that do both.
		if (!earlier.kind().equals(later.kind()) || MIXED.equals(later.kind())) {
			return false;
		}
		long gap = later.time() - earlier.time();
		if (gap < 0 || gap > WINDOW || earlier.distanceTo(later) > RANGE) {
			return false;
		}
		// A voice aimed at another member of the choir isn't singing with them.
		if (later.caster().equals(earlier.target()) || earlier.caster().equals(later.target())) {
			return false;
		}
		if (earlier.target() != null && earlier.target().equals(later.target())) {
			return true;
		}
		return earlier.aimDistanceTo(later) <= AIM;
	}

	/**
	 * The voices heard in the last second, gathered into choirs. {@link #offer} adds a cast and
	 * says who it's singing with. Not thread-safe; the server keeps one.
	 */
	public static final class Choir {
		private final List<List<Voice>> groups = new ArrayList<>();

		/** {@link #offer(Voice, BiPredicate)} with everyone allied. */
		public List<Voice> offer(Voice voice) {
			return offer(voice, (a, b) -> true);
		}

		/**
		 * Adds a voice. Returns everyone now singing together, this voice last: just this voice
		 * when it starts a new chorus, or the chorus it joined. A voice only joins a chorus whose
		 * every singer is {@code allied} with it.
		 */
		public List<Voice> offer(Voice voice, BiPredicate<UUID, UUID> allied) {
			forget(voice.time());
			for (List<Voice> group : groups) {
				Voice last = group.getLast();
				boolean already = group.stream().anyMatch(v -> v.caster().equals(voice.caster()));
				if (!already && joins(last, voice) && group.stream().allMatch(v -> allied.test(v.caster(), voice.caster()))) {
					group.add(voice);
					return List.copyOf(group);
				}
			}
			List<Voice> group = new ArrayList<>();
			group.add(voice);
			groups.add(group);
			return List.of(voice);
		}

		/** Drops choirs whose last voice has faded. */
		public void forget(long now) {
			Iterator<List<Voice>> it = groups.iterator();
			while (it.hasNext()) {
				if (now - it.next().getLast().time() > WINDOW) {
					it.remove();
				}
			}
		}

		public void clear() {
			groups.clear();
		}

		public int size() {
			return groups.size();
		}
	}
}
