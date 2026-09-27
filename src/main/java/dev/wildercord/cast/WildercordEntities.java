package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class WildercordEntities {
	private WildercordEntities() {}

	private static final ResourceKey<EntityType<?>> RUNE_BOLT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("rune_bolt"));

	public static final EntityType<RuneBolt> RUNE_BOLT = Registry.register(BuiltInRegistries.ENTITY_TYPE, RUNE_BOLT_KEY,
		EntityType.Builder.<RuneBolt>of(RuneBolt::new, MobCategory.MISC)
			.noLootTable()
			.noSave()
			.noSummon()
			.sized(0.3F, 0.3F)
			.clientTrackingRange(8)
			.updateInterval(1)
			.build(RUNE_BOLT_KEY));

	public static void init() {}
}
