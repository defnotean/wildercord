package dev.wildercord.runesmith;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.content.WildercordComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.Random;

/**
 * {@code wildercord:random_rune}: makes a rune item any rune from {@code min_tier} to
 * {@code max_tier} (never an innate one). The Runesmith's shelf and the wandering trader use it,
 * so every trader stocks a different selection.
 */
public record RandomRuneFunction(int minTier, int maxTier) implements LootItemFunction {
	public static final MapCodec<RandomRuneFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.intRange(1, 4).optionalFieldOf("min_tier", 1).forGetter(RandomRuneFunction::minTier),
		Codec.intRange(1, 4).optionalFieldOf("max_tier", 1).forGetter(RandomRuneFunction::maxTier)
	).apply(i, RandomRuneFunction::new));

	@Override
	public MapCodec<RandomRuneFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	public ItemStack apply(ItemStack stack, LootContext context) {
		RuneTrades.random(minTier, Math.max(minTier, maxTier), new Random(context.getRandom().nextLong()))
			.ifPresent(rune -> stack.set(WildercordComponents.RUNE, rune.id()));
		return stack;
	}
}
