package dev.wildercord.compat;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The newer {@code dropFromLootTable} overloads that name the table, and optionally take each stack
 * instead of dropping it. 1.21.8 only rolls the entity's own table, so this builds the same loot
 * context vanilla uses for a death and rolls the named table itself.
 */
public final class McLoot {
	private McLoot() {}

	/** Rolls {@code table} for this death and gives every stack to {@code onDrop}. */
	public static void dropFromLootTable(LivingEntity entity, ServerLevel level, DamageSource source,
			boolean playerKilled, ResourceKey<LootTable> table, Consumer<ItemStack> onDrop) {
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.THIS_ENTITY, entity)
			.withParameter(LootContextParams.ORIGIN, entity.position())
			.withParameter(LootContextParams.DAMAGE_SOURCE, source)
			.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
			.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
			.create(LootContextParamSets.ENTITY);
		LootTable loot = level.getServer().reloadableRegistries().getLootTable(table);
		loot.getRandomItems(params, onDrop::accept);
	}

	/** Rolls {@code table} for this death and drops the stacks where the entity lies. */
	public static void dropFromLootTable(LivingEntity entity, ServerLevel level, DamageSource source,
			boolean playerKilled, ResourceKey<LootTable> table) {
		dropFromLootTable(entity, level, source, playerKilled, table, stack -> entity.spawnAtLocation(level, stack));
	}
}
