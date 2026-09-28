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

	private static final ResourceKey<EntityType<?>> ARCHIVIST_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("archivist"));
	private static final ResourceKey<EntityType<?>> DUMMY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("training_dummy"));

	/** The Archive's keeper: an illager-shaped boss, a head taller than an evoker. */
	public static final EntityType<Archivist> ARCHIVIST = Registry.register(BuiltInRegistries.ENTITY_TYPE, ARCHIVIST_KEY,
		EntityType.Builder.<Archivist>of(Archivist::new, MobCategory.MONSTER)
			.sized(0.75F, 2.4F)
			.eyeHeight(2.05F)
			.clientTrackingRange(10)
			.fireImmune()
			.build(ARCHIVIST_KEY));

	public static final EntityType<TrainingDummy> TRAINING_DUMMY = Registry.register(BuiltInRegistries.ENTITY_TYPE, DUMMY_KEY,
		EntityType.Builder.<TrainingDummy>of(TrainingDummy::new, MobCategory.MISC)
			.sized(0.8F, 1.95F)
			.eyeHeight(1.6F)
			.clientTrackingRange(10)
			.build(DUMMY_KEY));

	public static void init() {
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ARCHIVIST, Archivist.createAttributes());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(TRAINING_DUMMY, TrainingDummy.createAttributes());
	}
}
