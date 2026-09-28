package dev.wildercord.runesmith;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * What the Runesmith pays and picks, as plain rules (no Minecraft types, so they're unit-tested):
 * the random runes on its shelf, what it pays for a rune you already know, and the reroll that
 * turns two known runes of a tier into one of that tier you haven't learned yet.
 */
public final class RuneTrades {
	private RuneTrades() {}

	/** Buyback offers shown at once, at most. */
	public static final int MAX_BUYBACKS = 6;

	/** Emeralds the Runesmith pays for a rune you already know: 1, 2, 5, 10 by tier. */
	public static int buybackPrice(int tier) {
		return switch (tier) {
			case 1 -> 1;
			case 2 -> 2;
			case 3 -> 5;
			default -> 10;
		};
	}

	/** Every rune of a tier a Runesmith deals in: never an innate, fused or found-only rune. */
	public static List<RuneDef> pool(int tier) {
		List<RuneDef> pool = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (rune.tier() == tier && Runes.common(rune)) {
				pool.add(rune);
			}
		}
		return pool;
	}

	/** One rune of a tier from {@code min} to {@code max}, evenly over every rune in that range. */
	public static Optional<RuneDef> random(int min, int max, Random random) {
		List<RuneDef> pool = new ArrayList<>();
		for (int tier = Math.max(1, min); tier <= Math.min(4, max); tier++) {
			pool.addAll(pool(tier));
		}
		return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(random.nextInt(pool.size())));
	}

	/**
	 * The rune a reroll of this tier gives: one you don't know yet, picked by {@code seed} (the
	 * same seed always picks the same rune, so the offer doesn't change each time it's opened).
	 */
	public static Optional<RuneDef> reroll(int tier, Set<String> known, long seed) {
		List<RuneDef> unknown = new ArrayList<>();
		for (RuneDef rune : pool(tier)) {
			if (!known.contains(rune.id())) {
				unknown.add(rune);
			}
		}
		return unknown.isEmpty() ? Optional.empty() : Optional.of(unknown.get(new Random(seed).nextInt(unknown.size())));
	}

	/** A reroll's price: two known runes of one tier (the same rune twice, or two different ones). */
	public record Pair(String first, String second, int tier) {}

	/**
	 * The reroll each tier can offer, from the known runes in an inventory (rune id to how many):
	 * a rune held twice pays for itself, otherwise the two most plentiful runes of that tier.
	 */
	public static List<Pair> rerolls(Map<String, Integer> knownHeld) {
		List<Pair> pairs = new ArrayList<>();
		for (int tier = 1; tier <= 4; tier++) {
			List<Map.Entry<String, Integer>> ofTier = new ArrayList<>();
			for (Map.Entry<String, Integer> held : knownHeld.entrySet()) {
				int t = tier;
				if (held.getValue() > 0 && Runes.get(held.getKey()).filter(r -> r.tier() == t && !Runes.innate(r)).isPresent()) {
					ofTier.add(held);
				}
			}
			ofTier.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
			if (ofTier.isEmpty()) {
				continue;
			}
			Map.Entry<String, Integer> most = ofTier.getFirst();
			if (most.getValue() >= 2) {
				pairs.add(new Pair(most.getKey(), most.getKey(), tier));
			} else if (ofTier.size() >= 2) {
				pairs.add(new Pair(most.getKey(), ofTier.get(1).getKey(), tier));
			}
		}
		return pairs;
	}

	/** The known runes in an inventory the Runesmith offers to buy back: the rarest first, {@link #MAX_BUYBACKS} at most. */
	public static List<String> buybacks(Map<String, Integer> knownHeld) {
		List<RuneDef> held = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : knownHeld.entrySet()) {
			if (entry.getValue() > 0) {
				Runes.get(entry.getKey()).filter(r -> !Runes.innate(r)).ifPresent(held::add);
			}
		}
		held.sort(Comparator.comparingInt(RuneDef::tier).reversed().thenComparing(RuneDef::id));
		return held.stream().limit(MAX_BUYBACKS).map(RuneDef::id).toList();
	}
}
