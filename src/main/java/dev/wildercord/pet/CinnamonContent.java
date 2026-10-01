package dev.wildercord.pet;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Cinnamon is her own small companion species, with no wolf entity or model involved. */
public final class CinnamonContent {
	private CinnamonContent() {}
	private static final ResourceKey<net.minecraft.world.item.Item> TOY_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("cinnamon_toy"));
	public static final net.minecraft.world.item.Item TOY = Registry.register(BuiltInRegistries.ITEM, TOY_KEY,
		new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().setId(TOY_KEY).stacksTo(1)));

	private static final ResourceKey<net.minecraft.world.item.Item> BOW_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("cinnamon_bow"));
	/** Her "armour": a ribbon bow for her head. Use it on her to put it on; shears take it off again. */
	public static final net.minecraft.world.item.Item BOW = Registry.register(BuiltInRegistries.ITEM, BOW_KEY,
		new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().setId(BOW_KEY).stacksTo(1)));

	private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("cinnamon"));
	public static final EntityType<CinnamonDog> CINNAMON = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
		EntityType.Builder.<CinnamonDog>of(CinnamonDog::new, MobCategory.CREATURE)
			.sized(0.58F, 0.67F).eyeHeight(0.48F).clientTrackingRange(10).fireImmune().build(KEY));

	public static void init() {
		CinnamonState.init();
		FabricDefaultAttributeRegistry.register(CINNAMON, CinnamonDog.createAttributes());
		CinnamonCompanion.init();
	}
}
