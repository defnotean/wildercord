package dev.wildercord.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Where breathing manuals are found: chests in the places where fighting is old (each favouring the methods that fit it),
 * and master weaponsmiths and clerics. Pure data, read by {@code AuraLoot} when loot tables load and by the trades' random
 * manual; an add-on (or wave 2's duelists) registers a source of its own here, through {@code api.AuraApi}.
 *
 * <p>A source is either a loot table (with the chance out of 100 that a chest of it holds a manual) or a named source that
 * hands methods out in code (a trade, a duelist, a quest): its {@code lootTable} is then empty. Either way its weights pick
 * the method: a method absent from the weights isn't given by that source.</p>
 */
public final class MethodSources {
	private MethodSources() {}

	/**
	 * @param id        the source's id ("trial_vault", "weaponsmith")
	 * @param lootTable the loot table it adds a manual to ("minecraft:chests/ancient_city"), or "" for a source in code
	 * @param chance    the chance out of 100 a chest holds one (ignored for a source in code)
	 * @param weights   how likely each method is, by method id
	 */
	public record Source(String id, String lootTable, int chance, Map<String, Integer> weights) {
		public Source {
			lootTable = lootTable == null ? "" : lootTable;
			chance = Math.max(0, Math.min(100, chance));
			weights = Collections.unmodifiableMap(new LinkedHashMap<>(weights));
		}

		/** A method drawn by weight, or empty when the source gives none. */
		public Optional<String> draw(Random random) {
			int total = 0;
			for (Map.Entry<String, Integer> e : weights.entrySet()) {
				total += Math.max(0, e.getValue());
			}
			if (total <= 0) {
				return Optional.empty();
			}
			int pick = random.nextInt(total);
			for (Map.Entry<String, Integer> e : weights.entrySet()) {
				pick -= Math.max(0, e.getValue());
				if (pick < 0) {
					return Optional.of(e.getKey());
				}
			}
			return Optional.empty();
		}
	}

	private static final Map<String, Source> SOURCES = Collections.synchronizedMap(new LinkedHashMap<>());

	/** Weights: the favoured methods three times as likely as the rest, which can still turn up. */
	static Map<String, Integer> favour(String... favoured) {
		Map<String, Integer> weights = new LinkedHashMap<>();
		for (BreathingMethod method : BreathingMethods.BUILT_IN) {
			weights.put(method.id(), 1);
		}
		for (String id : favoured) {
			weights.put(id, 3);
		}
		return weights;
	}

	static {
		// The mod's own dungeons: the Archive's books, and each expedition's vault its element's method.
		register(new Source("archive_library", "wildercord:chests/archive_library", 10, favour("starlit", "hourglass", "hollow")));
		register(new Source("archive_vault", "wildercord:chests/archive_vault", 30, favour("starlit", "hourglass", "hollow")));
		register(new Source("ember_sanctum", "wildercord:chests/ember_sanctum_vault", 25, favour("ember", "crimson")));
		register(new Source("storm_spire", "wildercord:chests/storm_spire_vault", 25, favour("thunder", "gale")));
		register(new Source("drowned_scriptorium", "wildercord:chests/drowned_scriptorium_vault", 25, favour("rime", "hollow")));
		register(new Source("living_greenhouse", "wildercord:chests/living_greenhouse_vault", 25, favour("verdant", "stone")));
		register(new Source("astral_observatory", "wildercord:chests/astral_observatory_vault", 25, favour("starlit", "hollow")));
		register(new Source("clockwork_crypt", "wildercord:chests/clockwork_crypt_vault", 25, favour("hourglass", "stone")));
		register(new Source("rootbound_maze", "wildercord:chests/rootbound_maze_vault", 25, favour("stone", "verdant")));
		register(new Source("moving_sky_ruin", "wildercord:chests/moving_sky_ruin_vault", 25, favour("gale", "thunder")));
		// The world's own places of old battles.
		register(new Source("trial_vault", "minecraft:chests/trial_chambers/reward_rare", 10, favour("thunder", "gale", "stone")));
		register(new Source("ominous_vault", "minecraft:chests/trial_chambers/reward_ominous_rare", 20, favour("thunder", "gale", "crimson")));
		register(new Source("stronghold", "minecraft:chests/stronghold_library", 15, favour("starlit", "hourglass", "rime")));
		register(new Source("ancient_city", "minecraft:chests/ancient_city", 12, favour("hollow", "rime", "crimson")));
		// Masters of their trade, who hand down what they were taught (see the villager_trade data).
		register(new Source("weaponsmith", "", 0, favour("ember", "thunder", "stone", "gale", "crimson")));
		register(new Source("cleric", "", 0, favour("verdant", "starlit", "hourglass", "hollow", "rime")));
	}

	/** Adds a source, or replaces the one with its id. */
	public static Source register(Source source) {
		SOURCES.put(source.id(), source);
		return source;
	}

	public static Optional<Source> byId(String id) {
		return Optional.ofNullable(SOURCES.get(id));
	}

	/** Every source, in the order registered. */
	public static List<Source> all() {
		synchronized (SOURCES) {
			return List.copyOf(new ArrayList<>(SOURCES.values()));
		}
	}

	/** The sources that add a manual to {@code lootTable}. */
	public static List<Source> forLootTable(String lootTable) {
		List<Source> out = new ArrayList<>();
		for (Source source : all()) {
			if (!source.lootTable().isEmpty() && source.lootTable().equals(lootTable)) {
				out.add(source);
			}
		}
		return out;
	}

	// ---- methods-b pack
	static {
		// Echo, Dawn and Venom's manuals, each where its kind of fighting is old: the deep city's silence, the desert's sun-lit
		// tombs, the jungle's serpent temples.
		for (MethodsBPack.Find find : MethodsBPack.FINDS) {
			register(new Source(find.id(), find.lootTable(), find.chance(), Map.of(find.method(), 1)));
		}
	}
}
