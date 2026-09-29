package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.cast.Fishing;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Set;

/**
 * {@code wildercord:fished_rune}: leaves the rune as it is, and tells its angler it came up (see {@link Fishing#caught}):
 * the Grimoire's Reeled In, and, when {@code magic} (a rune magic waters tangled in the line, on top of the catch), a
 * glint at the bobber and a line above the hotbar. Does nothing for loot rolled without a bobber.
 */
public record FishedRuneFunction(boolean magic) implements LootItemFunction {
	public static final MapCodec<FishedRuneFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.BOOL.optionalFieldOf("magic", false).forGetter(FishedRuneFunction::magic)
	).apply(i, FishedRuneFunction::new));

	@Override
	public MapCodec<FishedRuneFunction> codec() {
		return MAP_CODEC;
	}

	@Override
	public Set<ContextKey<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.THIS_ENTITY);
	}

	@Override
	public ItemStack apply(ItemStack stack, LootContext context) {
		if (!stack.isEmpty()) {
			Fishing.caught(context.getOptional(LootContextParams.THIS_ENTITY), magic);
		}
		return stack;
	}
}
