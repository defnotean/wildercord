package dev.wildercord.ocean;

import dev.wildercord.Wildercord;
import dev.wildercord.compat.McWorldgen;
import dev.wildercord.spell.FieldGuide;
import java.util.List;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Unified entry point for the Deep Ocean Expansion: initializes blocks, items, entities,
 * spawn eggs, ocean biome spawns, and Grimoire Field Guide entries.
 */
public final class OceanContent {
	private OceanContent() {}

	public static final Item BARNACLE_HULK_SPAWN_EGG = egg("barnacle_hulk_dredger", OceanEntities.BARNACLE_HULK_DREDGER);
	public static final Item SIREN_JELLY_SPAWN_EGG = egg("siren_veil_jelly", OceanEntities.SIREN_VEIL_JELLY);
	public static final Item CORAL_MAKO_SPAWN_EGG = egg("coral_spine_mako", OceanEntities.CORAL_SPINE_MAKO);
	public static final Item DREAD_NAUTILUS_SPAWN_EGG = egg("dread_nautilus_sentinel", OceanEntities.DREAD_NAUTILUS_SENTINEL);
	public static final Item THALASSOR_SPAWN_EGG = egg("thalassor", OceanEntities.THALASSOR);
	public static final Item BRINE_MERCHANT_SPAWN_EGG = egg("brine_merchant", OceanEntities.BRINE_MERCHANT);
	public static final Item LUMI_SEAHORSE_SPAWN_EGG = egg("lumi_seahorse", OceanEntities.LUMI_SEAHORSE);
	public static final Item PEARL_PUP_SPAWN_EGG = egg("pearl_pup", OceanEntities.PEARL_PUP);
	public static final Item SINGING_MOON_WHALE_SPAWN_EGG = egg("singing_moon_whale", OceanEntities.SINGING_MOON_WHALE);

	private static Item egg(String id, EntityType<? extends Mob> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id + "_spawn_egg"));
		return Registry.register(BuiltInRegistries.ITEM, key, dev.wildercord.compat.McSpawnEgg.create(type, new Item.Properties()));
	}

	public static void init() {
		OceanBlocks.init();
		OceanEntities.init();
		OceanItems.init();

		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(out -> List.of(
				BARNACLE_HULK_SPAWN_EGG,
				SIREN_JELLY_SPAWN_EGG,
				CORAL_MAKO_SPAWN_EGG,
				DREAD_NAUTILUS_SPAWN_EGG,
				THALASSOR_SPAWN_EGG,
				BRINE_MERCHANT_SPAWN_EGG,
				LUMI_SEAHORSE_SPAWN_EGG,
				PEARL_PUP_SPAWN_EGG,
				SINGING_MOON_WHALE_SPAWN_EGG
			).forEach(out::accept));

		BiomeModifications.create(Wildercord.id("deep_ocean_expansion_spawns"))
			.add(ModificationPhase.ADDITIONS, BiomeSelectors.tag(BiomeTags.IS_OCEAN), context -> {
				var spawns = context.getSpawnSettings();
				McWorldgen.addSpawn(spawns, MobCategory.MONSTER, OceanEntities.BARNACLE_HULK_DREDGER, 1, 1, 5);
				McWorldgen.addSpawn(spawns, MobCategory.MONSTER, OceanEntities.SIREN_VEIL_JELLY, 1, 3, 14);
				McWorldgen.addSpawn(spawns, MobCategory.MONSTER, OceanEntities.CORAL_SPINE_MAKO, 1, 2, 12);
				McWorldgen.addSpawn(spawns, MobCategory.MONSTER, OceanEntities.DREAD_NAUTILUS_SENTINEL, 1, 1, 4);
				McWorldgen.addSpawn(spawns, MobCategory.WATER_CREATURE, OceanEntities.LUMI_SEAHORSE, 1, 2, 10);
				McWorldgen.addSpawn(spawns, MobCategory.WATER_CREATURE, OceanEntities.PEARL_PUP, 1, 2, 8);
				McWorldgen.addSpawn(spawns, MobCategory.WATER_CREATURE, OceanEntities.SINGING_MOON_WHALE, 1, 1, 3);
			});

		FieldGuide.add(new FieldGuide.Entry("wildercord:barnacle_hulk_dredger", FieldGuide.Group.MONSTER, 0xD87090));
		FieldGuide.add(new FieldGuide.Entry("wildercord:siren_veil_jelly", FieldGuide.Group.MONSTER, 0xC070FF));
		FieldGuide.add(new FieldGuide.Entry("wildercord:coral_spine_mako", FieldGuide.Group.MONSTER, 0x4EA0F0));
		FieldGuide.add(new FieldGuide.Entry("wildercord:dread_nautilus_sentinel", FieldGuide.Group.MONSTER, 0xC89850));
		FieldGuide.add(new FieldGuide.Entry("wildercord:thalassor", FieldGuide.Group.MONSTER, 0x48F0E0));
		FieldGuide.add(new FieldGuide.Entry("wildercord:brine_merchant", FieldGuide.Group.WANDERER, 0x70E0C8));
		FieldGuide.add(new FieldGuide.Entry("wildercord:lumi_seahorse", FieldGuide.Group.WILDLIFE, 0x60FFE0));
		FieldGuide.add(new FieldGuide.Entry("wildercord:pearl_pup", FieldGuide.Group.WILDLIFE, 0xF0F8FF));
		FieldGuide.add(new FieldGuide.Entry("wildercord:singing_moon_whale", FieldGuide.Group.WILDLIFE, 0x90B8FF));
	}
}
