package dev.wildercord.aura.world;

import com.mojang.serialization.MapCodec;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethods;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * {@code wildercord:knight_method}: a loot function that gives a manual page (or a manual) the method of the knight (or
 * duelist) that dropped it, read from the loot's own creature; any built-in method when there isn't one (a data pack's chest).
 * The fallen knight's loot table uses it, so the table stays data a pack can change.
 */
public record KnightLoot() implements LootItemFunction {
	public static final MapCodec<KnightLoot> MAP_CODEC = MapCodec.unit(new KnightLoot());

	@Override
	public MapCodec<KnightLoot> codec() {
		return MAP_CODEC;
	}

	@Override
	public ItemStack apply(ItemStack stack, LootContext context) {
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
		String method = entity instanceof AuraFighter fighter ? fighter.method().id()
			: BreathingMethods.BUILT_IN.get(context.getRandom().nextInt(BreathingMethods.BUILT_IN.size())).id();
		stack.set(BreathingManualItem.METHOD, method);
		return stack;
	}
}
