package dev.wildercord.content;

import com.mojang.serialization.MapCodec;
import dev.wildercord.Wildercord;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.RuneTwistRules;
import dev.wildercord.spell.RuneTwistRules.Twist;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Flawed and corrupted runes in dungeon chests. See {@link RuneTwistRules}. */
public final class TwistedRunes {
	private TwistedRunes() {}

	/** The chests that may hold one. */
	private static final Set<ResourceKey<LootTable>> CHESTS = Set.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.ABANDONED_MINESHAFT,
		BuiltInLootTables.STRONGHOLD_CORRIDOR, BuiltInLootTables.ANCIENT_CITY, BuiltInLootTables.BASTION_TREASURE, BuiltInLootTables.WOODLAND_MANSION,
		BuiltInLootTables.DESERT_PYRAMID, BuiltInLootTables.JUNGLE_TEMPLE);

	/** {@code wildercord:twisted_rune}: makes a rune item a random effect rune with a random twist. */
	public record Function() implements LootItemFunction {
		public static final MapCodec<Function> MAP_CODEC = MapCodec.unit(new Function());

		@Override
		public MapCodec<Function> codec() {
			return MAP_CODEC;
		}

		@Override
		public ItemStack apply(ItemStack stack, LootContext context) {
			Random random = new Random(context.getRandom().nextLong());
			Optional<RuneDef> rune = randomEffect(random);
			if (rune.isEmpty()) return ItemStack.EMPTY;
			return stack(rune.get(), RuneTwistRules.roll(random.nextDouble()));
		}
	}

	/** A random effect rune of the found tiers, never an innate one nor one learned from the Grimoire. */
	public static Optional<RuneDef> randomEffect(Random random) {
		for (int i = 0; i < 40; i++) {
			Optional<RuneDef> rune = dev.wildercord.runesmith.RuneTrades.random(RuneTwistRules.MIN_TIER, RuneTwistRules.MAX_TIER, random);
			if (rune.isPresent() && rune.get().family() == RuneFamily.EFFECT && !Runes.innate(rune.get())
				&& !rune.get().is(dev.wildercord.spell.ExciseRules.ID) && dev.wildercord.spell.LessonPackRules.byRune(rune.get().id()) == null) return rune;
		}
		return Optional.empty();
	}

	/** A twisted rune item: {@code rune} with {@code twist} cut into it. */
	public static ItemStack stack(RuneDef rune, Twist twist) {
		ItemStack stack = RuneItem.stack(rune);
		stack.set(WildercordComponents.TWIST, twist.id());
		return stack;
	}

	public static Twist twistOf(ItemStack stack) {
		return Twist.byId(stack.get(WildercordComponents.TWIST));
	}

	public static void init() {
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Wildercord.id("twisted_rune"), Function.MAP_CODEC);
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin() || !CHESTS.contains(key)) return;
			int chance = dev.wildercord.config.WildercordConfig.scaledChance(RuneTwistRules.CHEST_CHANCE, dev.wildercord.config.Config.get().runeLootChance());
			if (chance <= 0) return;
			LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(WildercordItems.RUNE).apply(Function::new).setWeight(chance));
			if (chance < 100) pool.add(EmptyLootItem.emptyItem().setWeight(100 - chance));
			table.withPool(pool);
		});
	}
}
