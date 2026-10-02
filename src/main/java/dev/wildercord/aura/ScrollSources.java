package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.TechniqueScrollItem;
import dev.wildercord.config.WildercordConfig;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Where technique scrolls turn up: old places where swords were drawn in earnest, each favouring the parts that fit it (a pool on its
 * loot table as the tables load, its chance scaled by the server's {@code aura.technique_scroll_chance}), a fallen knight's armour
 * now and then, and sources in code that hand parts out through {@link #byId} or the loot function
 * {@code wildercord:random_technique_part}. A scroll carries a part a scroll can carry ({@link TechniqueRules#scrollable}): never one every
 * blade knows, nor a Way's own.
 *
 * <p>{@code sword_tomb} is waiting for the sword tombs: a source in code favouring the parts hardest to find elsewhere, for a tomb's loot
 * table to draw from with {@code {"function": "wildercord:random_technique_part", "source": "sword_tomb"}}.</p>
 */
public final class ScrollSources {
	private ScrollSources() {}

	/**
	 * @param id        the source's id ("ancient_city", "sword_tomb")
	 * @param lootTable the loot table it adds a scroll to ("minecraft:chests/ancient_city"), or "" for a source in code
	 * @param chance    the chance out of 100 a chest (or a creature) of it holds one (ignored for a source in code)
	 * @param weights   how likely each part is, by part id (a part absent isn't given by it)
	 */
	public record Source(String id, String lootTable, int chance, Map<String, Integer> weights) {
		public Source {
			lootTable = lootTable == null ? "" : lootTable;
			chance = Math.max(0, Math.min(100, chance));
			weights = Collections.unmodifiableMap(new LinkedHashMap<>(weights));
		}

		/** A part drawn by weight, or empty when the source gives none. */
		public Optional<String> draw(Random random) {
			return TechniqueRules.draw(weights, java.util.Set.of(), random.nextDouble());
		}
	}

	/** Weights over every part a scroll carries, the favoured ones three times as likely. */
	public static Map<String, Integer> favour(String... favoured) {
		Map<String, Integer> weights = new LinkedHashMap<>();
		List<String> fav = List.of(favoured);
		for (String part : TechniqueRules.scrollParts()) {
			weights.put(part, fav.contains(part) ? 3 : 1);
		}
		return weights;
	}

	private static final Map<String, Source> SOURCES = Collections.synchronizedMap(new LinkedHashMap<>());

	static {
		// The world's own places of old fighting.
		register(new Source("trial_chambers", "minecraft:chests/trial_chambers/reward_rare", 12,
			favour(TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.PIERCE)));
		register(new Source("trial_chambers_ominous", "minecraft:chests/trial_chambers/reward_ominous_rare", 25,
			favour(TechniqueRules.FALLING, TechniqueRules.BURST, TechniqueRules.SUNDER)));
		register(new Source("ancient_city", "minecraft:chests/ancient_city", 14,
			favour(TechniqueRules.SPIN, TechniqueRules.AFTERIMAGE, TechniqueRules.ECHO)));
		register(new Source("stronghold", "minecraft:chests/stronghold_library", 10,
			favour(TechniqueRules.FALLING, TechniqueRules.ECHO, TechniqueRules.SUNDER)));
		register(new Source("bastion", "minecraft:chests/bastion_treasure", 16,
			favour(TechniqueRules.FALLING, TechniqueRules.BURST, TechniqueRules.SUNDER)));
		register(new Source("jungle_temple", "minecraft:chests/jungle_temple", 10,
			favour(TechniqueRules.RISING, TechniqueRules.BIND, TechniqueRules.AFTERIMAGE)));
		register(new Source("desert_pyramid", "minecraft:chests/desert_pyramid", 8,
			favour(TechniqueRules.SWEEP, TechniqueRules.WAVE, TechniqueRules.BIND)));
		register(new Source("woodland_mansion", "minecraft:chests/woodland_mansion", 15,
			favour(TechniqueRules.SPIN, TechniqueRules.ECHO, TechniqueRules.AFTERIMAGE)));
		register(new Source("pillager_outpost", "minecraft:chests/pillager_outpost", 8,
			favour(TechniqueRules.SWEEP, TechniqueRules.RISING, TechniqueRules.WAVE)));
		// The mod's own dungeons: each expedition's vault the parts that fit its element.
		register(new Source("ember_sanctum", "wildercord:chests/ember_sanctum_vault", 20,
			favour(TechniqueRules.FALLING, TechniqueRules.WAVE, TechniqueRules.SUNDER)));
		register(new Source("storm_spire", "wildercord:chests/storm_spire_vault", 20,
			favour(TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.PIERCE)));
		register(new Source("drowned_scriptorium", "wildercord:chests/drowned_scriptorium_vault", 20,
			favour(TechniqueRules.SWEEP, TechniqueRules.BIND, TechniqueRules.ECHO)));
		register(new Source("living_greenhouse", "wildercord:chests/living_greenhouse_vault", 20,
			favour(TechniqueRules.RISING, TechniqueRules.BIND, TechniqueRules.AFTERIMAGE)));
		register(new Source("astral_observatory", "wildercord:chests/astral_observatory_vault", 20,
			favour(TechniqueRules.THRUST, TechniqueRules.BURST, TechniqueRules.ECHO)));
		register(new Source("clockwork_crypt", "wildercord:chests/clockwork_crypt_vault", 20,
			favour(TechniqueRules.SPIN, TechniqueRules.AFTERIMAGE, TechniqueRules.ECHO)));
		register(new Source("rootbound_maze", "wildercord:chests/rootbound_maze_vault", 20,
			favour(TechniqueRules.SWEEP, TechniqueRules.BIND, TechniqueRules.SUNDER)));
		register(new Source("moving_sky_ruin", "wildercord:chests/moving_sky_ruin_vault", 20,
			favour(TechniqueRules.RISING, TechniqueRules.WAVE, TechniqueRules.PIERCE)));
		register(new Source("archive", "wildercord:chests/archive_vault", 20,
			favour(TechniqueRules.THRUST, TechniqueRules.ECHO, TechniqueRules.AFTERIMAGE)));
		// A fallen knight was a swordsman once: now and then a scroll is still in its armour.
		register(new Source("fallen_knight", "wildercord:entities/fallen_knight", 6, favour()));
		// For the sword tombs (aura overhaul step 11): the parts hardest to find elsewhere, as a source in code.
		register(new Source("sword_tomb", "", 0, favour(TechniqueRules.SPIN, TechniqueRules.BURST, TechniqueRules.AFTERIMAGE, TechniqueRules.SUNDER,
			TechniqueRules.ECHO)));
	}

	/** Adds a source, or replaces the one with its id. */
	public static Source register(Source source) {
		SOURCES.put(source.id(), source);
		return source;
	}

	public static Optional<Source> byId(String id) {
		return Optional.ofNullable(id == null ? null : SOURCES.get(id));
	}

	/** Every source, in the order registered. */
	public static List<Source> all() {
		synchronized (SOURCES) {
			return List.copyOf(new ArrayList<>(SOURCES.values()));
		}
	}

	/** The sources that add a scroll to {@code lootTable}. */
	public static List<Source> forLootTable(String lootTable) {
		List<Source> out = new ArrayList<>();
		for (Source source : all()) {
			if (!source.lootTable().isEmpty() && source.lootTable().equals(lootTable)) {
				out.add(source);
			}
		}
		return out;
	}

	/**
	 * {@code wildercord:random_technique_part}: makes a technique scroll carry a part drawn from {@code source}'s weights (a
	 * {@link ScrollSources} id), or any part a scroll can carry when the source is missing.
	 */
	public record RandomPart(String source) implements LootItemFunction {
		public static final MapCodec<RandomPart> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("source", "").forGetter(RandomPart::source)
		).apply(i, RandomPart::new));

		@Override
		public MapCodec<RandomPart> codec() {
			return MAP_CODEC;
		}

		@Override
		public ItemStack apply(ItemStack stack, LootContext context) {
			Random random = new Random(context.getRandom().nextLong());
			List<String> any = TechniqueRules.scrollParts();
			String part = byId(source).flatMap(s -> s.draw(random)).orElseGet(() -> any.get(random.nextInt(any.size())));
			stack.set(TechniqueScrollItem.PART, part);
			return stack;
		}
	}

	/** A pool holding a scroll {@code chance} times in 100, its part drawn by the source's weights. */
	static LootPool.Builder pool(Source source, int chance) {
		LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
		int total = source.weights().values().stream().mapToInt(w -> Math.max(0, w)).sum();
		if (total <= 0) {
			return builder;
		}
		for (Map.Entry<String, Integer> e : source.weights().entrySet()) {
			if (e.getValue() <= 0) {
				continue;
			}
			int weight = Math.max(1, Math.round(chance * 10F * e.getValue() / total));
			builder.add(LootItem.lootTableItem(TechniqueScrollItem.SCROLL).setWeight(weight)
				.apply(SetComponentsFunction.setComponent(TechniqueScrollItem.PART, e.getKey())));
		}
		if (chance < 100) {
			builder.add(EmptyLootItem.emptyItem().setWeight((100 - chance) * 10));
		}
		return builder;
	}

	/** The chance out of 100 a source's table holds a scroll on this server (its own chance, times {@code aura.technique_scroll_chance}). */
	static int chance(Source source, WildercordConfig config) {
		return (int) Math.round(Math.max(0, Math.min(100, source.chance() * config.aura().techniques().scrollChance())));
	}

	static void init() {
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Wildercord.id("random_technique_part"), RandomPart.MAP_CODEC);
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			WildercordConfig config = dev.wildercord.config.Config.get();
			for (Source found : forLootTable(key.identifier().toString())) {
				int chance = chance(found, config);
				if (chance > 0) {
					table.withPool(pool(found, chance));
				}
			}
		});
	}
}
