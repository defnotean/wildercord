package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** The dimension dungeons' bosses. Each drops from its loot table, {@code wildercord:entities/<id>}. */
public final class DungeonEntities {
	private DungeonEntities() {}

	private static final ResourceKey<EntityType<?>> CINDER_WARDEN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("cinder_warden"));

	/** The Ember Sanctum's keeper: a hulking figure of magma and chains, nearly three blocks tall. */
	public static final EntityType<CinderWarden> CINDER_WARDEN = Registry.register(BuiltInRegistries.ENTITY_TYPE, CINDER_WARDEN_KEY,
		EntityType.Builder.<CinderWarden>of(CinderWarden::new, MobCategory.MONSTER)
			.sized(1.4F, 2.9F)
			.eyeHeight(2.45F)
			.clientTrackingRange(10)
			.build(CINDER_WARDEN_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(CINDER_WARDEN, CinderWarden.createAttributes());
	}
}
