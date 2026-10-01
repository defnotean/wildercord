package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
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

import java.util.Map;
import java.util.Random;

/**
 * Where breathing manuals are found (the sources are {@link MethodSources}): a pool on each source's loot table, as the
 * tables load, its chance scaled by the server's rune loot multiplier; and {@code wildercord:random_breathing_method}, the
 * loot function the master weaponsmith's and cleric's trades use to pick which manual they hand down.
 */
public final class AuraLoot {
	private AuraLoot() {}

	/**
	 * {@code wildercord:random_breathing_method}: makes a manual teach a method drawn from {@code source}'s weights (a
	 * {@link MethodSources} id), or any built-in method when the source is missing.
	 */
	public record RandomMethod(String source) implements LootItemFunction {
		public static final MapCodec<RandomMethod> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("source", "").forGetter(RandomMethod::source)
		).apply(i, RandomMethod::new));

		@Override
		public MapCodec<RandomMethod> codec() {
			return MAP_CODEC;
		}

		@Override
		public ItemStack apply(ItemStack stack, LootContext context) {
			Random random = new Random(context.getRandom().nextLong());
			String method = MethodSources.byId(source).flatMap(s -> s.draw(random))
				.orElseGet(() -> BreathingMethods.BUILT_IN.get(random.nextInt(BreathingMethods.BUILT_IN.size())).id());
			stack.set(BreathingManualItem.METHOD, method);
			return stack;
		}
	}

	static void init() {
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Wildercord.id("random_breathing_method"), RandomMethod.MAP_CODEC);
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			WildercordConfig config = dev.wildercord.config.Config.get();
			for (MethodSources.Source found : MethodSources.forLootTable(key.identifier().toString())) {
				int chance = WildercordConfig.scaledChance(found.chance(), config.runeLootChance());
				if (chance > 0) {
					table.withPool(pool(found, chance));
				}
			}
		});
	}

	/** A pool holding a manual {@code chance} times in 100, its method drawn by the source's weights. */
	static LootPool.Builder pool(MethodSources.Source source, int chance) {
		LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
		int total = source.weights().values().stream().mapToInt(w -> Math.max(0, w)).sum();
		if (total <= 0) {
			return builder;
		}
		for (Map.Entry<String, Integer> e : source.weights().entrySet()) {
			if (e.getValue() <= 0) {
				continue;
			}
			// Weights out of 1000 between them: each method's share of the chance.
			int weight = Math.max(1, Math.round(chance * 10F * e.getValue() / total));
			builder.add(LootItem.lootTableItem(BreathingManualItem.MANUAL).setWeight(weight)
				.apply(SetComponentsFunction.setComponent(BreathingManualItem.METHOD, e.getKey())));
		}
		if (chance < 100) {
			builder.add(EmptyLootItem.emptyItem().setWeight((100 - chance) * 10));
		}
		return builder;
	}
}
