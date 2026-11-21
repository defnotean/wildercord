package dev.wildercord.compat;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The newer {@code dropFromLootTable} overloads that name the table, and optionally take each stack
 * instead of dropping it. 1.21.8 only rolls the entity's own table, so these build the same loot
 * context vanilla uses and hand it to the table-taking method. Mixed into {@code LivingEntity}, where
 * the protected method it calls is reachable.
 */
public interface LivingEntityCompat {
	/** Rolls {@code table} for this death and gives every stack to {@code onDrop}. */
	default void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled,
			ResourceKey<LootTable> table, Consumer<ItemStack> onDrop) {
		LivingEntity self = (LivingEntity) (Object) this;
		dropFromLootTable(level, table, builder -> builder
			.withParameter(LootContextParams.THIS_ENTITY, self)
			.withParameter(LootContextParams.ORIGIN, self.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, source)
			.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
			.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
			.create(LootContextParamSets.ENTITY),
			(where, stack) -> onDrop.accept(stack));
	}

	/** Rolls {@code table} for this death and drops the stacks where the entity lies. */
	default void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled,
			ResourceKey<LootTable> table) {
		dropFromLootTable(level, source, playerKilled, table, stack -> spawnAtLocation(level, stack));
	}
}
