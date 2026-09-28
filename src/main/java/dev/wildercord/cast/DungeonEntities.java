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

	private static final ResourceKey<EntityType<?>> STAR_EATER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("star_eater"));

	/** The Astral Observatory's keeper: a floating knot of void, its eye ringed with star shards. */
	public static final EntityType<StarEater> STAR_EATER = Registry.register(BuiltInRegistries.ENTITY_TYPE, STAR_EATER_KEY,
		EntityType.Builder.<StarEater>of(StarEater::new, MobCategory.MONSTER)
			.sized(1.4F, 1.6F)
			.eyeHeight(0.8F)
			.clientTrackingRange(10)
			.fireImmune()
			.build(STAR_EATER_KEY));

	private static final ResourceKey<EntityType<?>> TIDE_SCRIBE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("tide_scribe"));

	/** The Drowned Scriptorium's keeper: a drowned sorcerer trailing ink, who floods its arena and drains it again. */
	public static final EntityType<TideScribe> TIDE_SCRIBE = Registry.register(BuiltInRegistries.ENTITY_TYPE, TIDE_SCRIBE_KEY,
		EntityType.Builder.<TideScribe>of(TideScribe::new, MobCategory.MONSTER)
			.sized(0.9F, 2.3F)
			.eyeHeight(1.95F)
			.clientTrackingRange(10)
			.build(TIDE_SCRIBE_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(CINDER_WARDEN, CinderWarden.createAttributes());
		FabricDefaultAttributeRegistry.register(STAR_EATER, StarEater.createAttributes());
		FabricDefaultAttributeRegistry.register(TIDE_SCRIBE, TideScribe.createAttributes());
	}
}
