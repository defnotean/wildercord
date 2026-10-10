package dev.wildercord.monster;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The magical monsters of the wilds, registered: their entity types (none allowed on Peaceful), what they drop, and a
 * spawn egg for each. Each drops from its loot table, {@code wildercord:entities/<id>}, written by tools/monster_art.py.
 */
public final class MonsterContent {
	private MonsterContent() {}

	/** A hunched walking thicket, a head taller than a player. */
	public static final EntityType<Bramblewalker> BRAMBLEWALKER = monster("bramblewalker",
		EntityType.Builder.of(Bramblewalker::new, MobCategory.MONSTER).sized(0.95F, 2.15F).eyeHeight(1.8F));
	/** A long, low shadow panther. */
	public static final EntityType<Gloomstalker> GLOOMSTALKER = monster("gloomstalker",
		EntityType.Builder.of(Gloomstalker::new, MobCategory.MONSTER).sized(0.9F, 0.95F).eyeHeight(0.72F));
	/** A winged hunter of the peaks. */
	public static final EntityType<ThunderwingHarpy> THUNDERWING_HARPY = monster("thunderwing_harpy",
		EntityType.Builder.of(ThunderwingHarpy::new, MobCategory.MONSTER).sized(0.8F, 1.3F).eyeHeight(1.1F));
	/** A broad crystal-backed beetle. */
	public static final EntityType<GeodeCrawler> GEODE_CRAWLER = monster("geode_crawler",
		EntityType.Builder.of(GeodeCrawler::new, MobCategory.MONSTER).sized(1.2F, 0.75F).eyeHeight(0.5F));
	/** A frog the size of a cow. */
	public static final EntityType<BogWitchFrog> BOG_WITCH_FROG = monster("bog_witch_frog",
		EntityType.Builder.of(BogWitchFrog::new, MobCategory.MONSTER).sized(1.35F, 1.1F).eyeHeight(0.9F));
	/** A slime of clear jelly, sized like a slime (this is its smallest). */
	public static final EntityType<ManaOoze> MANA_OOZE = monster("mana_ooze",
		EntityType.Builder.of(ManaOoze::new, MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F).spawnDimensionsScale(4.0F));

	/** A hooded illager who hunts casters (0.13). */
	public static final EntityType<MageHunter> MAGE_HUNTER = monster("mage_hunter",
		EntityType.Builder.of(MageHunter::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.62F));

	/** A Bog Witch-Frog's bubble of poison, in flight. */
	public static final EntityType<BogBubble> BOG_BUBBLE = register("bog_bubble",
		EntityType.Builder.<BogBubble>of(BogBubble::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(2));
	/** A thrown Living Bramble. */
	public static final EntityType<ThrownBramble> THROWN_BRAMBLE = register("thrown_bramble",
		EntityType.Builder.<ThrownBramble>of(ThrownBramble::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4)
			.updateInterval(10));

	// ------------------------------------------------------------------ what they drop

	public static final Item LIVING_BRAMBLE = item("living_bramble", p -> new MonsterDropItem.LivingBramble(p.stacksTo(16)
		// It rots down well.
		.compostable(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COMPOSTABLE_MEDIUM)));
	public static final Item SHADOW_PELT = item("shadow_pelt", p -> new MonsterDropItem("shadow_pelt", p.rarity(Rarity.UNCOMMON)));
	public static final Item STORM_FEATHER = item("storm_feather", p -> new MonsterDropItem.StormFeather(p.stacksTo(16)));
	public static final Item BOG_GLAND = item("bog_gland", p -> new MonsterDropItem("bog_gland", p));
	public static final Item MANA_GEL = item("mana_gel", p -> new MonsterDropItem.ManaGel(p.stacksTo(16)
		.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.0F).animation(ItemUseAnimation.EAT)
			.sound(SoundEvents.HONEY_DRINK).hasConsumeParticles(false).build())));

	/** The spawn eggs, in the order the monsters are listed. */
	public static final List<Item> SPAWN_EGGS = new ArrayList<>();
	public static final Item BRAMBLEWALKER_EGG = egg("bramblewalker", BRAMBLEWALKER);
	public static final Item GLOOMSTALKER_EGG = egg("gloomstalker", GLOOMSTALKER);
	public static final Item THUNDERWING_HARPY_EGG = egg("thunderwing_harpy", THUNDERWING_HARPY);
	public static final Item GEODE_CRAWLER_EGG = egg("geode_crawler", GEODE_CRAWLER);
	public static final Item BOG_WITCH_FROG_EGG = egg("bog_witch_frog", BOG_WITCH_FROG);
	public static final Item MANA_OOZE_EGG = egg("mana_ooze", MANA_OOZE);
	public static final Item MAGE_HUNTER_EGG = egg("mage_hunter", MAGE_HUNTER);

	/** Every monster's type, in the order of {@link MonsterRules.Kind}. */
	public static List<EntityType<? extends Mob>> types() {
		return List.of(BRAMBLEWALKER, GLOOMSTALKER, THUNDERWING_HARPY, GEODE_CRAWLER, BOG_WITCH_FROG, MANA_OOZE);
	}

	/** The type for one of the six. */
	public static EntityType<? extends Mob> type(MonsterRules.Kind kind) {
		return types().get(kind.ordinal());
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> monster(String id, EntityType.Builder<T> builder) {
		return register(id, builder.clientTrackingRange(8).notInPeaceful());
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	private static Item egg(String id, EntityType<? extends Mob> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id + "_spawn_egg"));
		Item egg = Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
		SPAWN_EGGS.add(egg);
		return egg;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(BRAMBLEWALKER, Bramblewalker.createAttributes());
		FabricDefaultAttributeRegistry.register(GLOOMSTALKER, Gloomstalker.createAttributes());
		FabricDefaultAttributeRegistry.register(THUNDERWING_HARPY, ThunderwingHarpy.createAttributes());
		FabricDefaultAttributeRegistry.register(GEODE_CRAWLER, GeodeCrawler.createAttributes());
		FabricDefaultAttributeRegistry.register(BOG_WITCH_FROG, BogWitchFrog.createAttributes());
		FabricDefaultAttributeRegistry.register(MANA_OOZE, ManaOoze.createAttributes());
		FabricDefaultAttributeRegistry.register(MAGE_HUNTER, MageHunter.createAttributes());
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
			output.accept(LIVING_BRAMBLE);
			output.accept(SHADOW_PELT);
			output.accept(STORM_FEATHER);
			output.accept(BOG_GLAND);
			output.accept(MANA_GEL);
			SPAWN_EGGS.forEach(output::accept);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
	}
}
