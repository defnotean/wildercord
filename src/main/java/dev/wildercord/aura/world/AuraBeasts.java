package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Two rare highland animals with grounded habitats, local crowd caps and useful acquisition paths. */
public final class AuraBeasts {
	private AuraBeasts() {}
	public static final EntityType<Stonehorn> STONEHORN=entity("stonehorn",EntityType.Builder.of(Stonehorn::new,MobCategory.CREATURE).sized(1.35F,1.45F).eyeHeight(1.2F).clientTrackingRange(10));
	public static final EntityType<Galeclaw> GALECLAW=entity("galeclaw",EntityType.Builder.of(Galeclaw::new,MobCategory.CREATURE).sized(.95F,1.35F).eyeHeight(1.1F).clientTrackingRange(10));
	public static final Item STONEHORN_PLATE=item("stonehorn_plate",false);
	public static final Item GALECLAW_PLUME=item("galeclaw_plume",false);
	public static final Item BASTION_POULTICE=item("bastion_poultice",true);
	public static final Item RIDGE_WHISTLE=item("ridge_whistle",true);
	private static final Item STONE_EGG=egg("stonehorn",STONEHORN), GALE_EGG=egg("galeclaw",GALECLAW);
	private static <T extends Entity> EntityType<T> entity(String id,EntityType.Builder<T> builder) {
		var key=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id(id)); return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,builder.build(key));
	}
	private static Item egg(String id,EntityType<? extends Mob> type) {
		var key=ResourceKey.create(Registries.ITEM,Wildercord.id(id+"_spawn_egg")); return Registry.register(BuiltInRegistries.ITEM,key,dev.wildercord.compat.McSpawnEgg.create(type, new Item.Properties().map(s->ResourceKey.create(Registries.BIOME,ResourceLocation.parse(s))).collect(java.util.stream.Collectors.toSet());
		BiomeModifications.create(Wildercord.id("aura_beasts/"+id)).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(biomes),c->{
			if(Config.get().auraWorld().auraBeasts()) dev.wildercord.compat.McWorldgen.addSpawn(c.getSpawnSettings(), MobCategory.CREATURE, type, 1, 1, weight);
		});
	}
	public static void init() {
		FabricDefaultAttributeRegistry.register(STONEHORN,AuraBeast.attributes(false)); FabricDefaultAttributeRegistry.register(GALECLAW,AuraBeast.attributes(true));
		FieldGuide.add(new FieldGuide.Entry("wildercord:stonehorn",FieldGuide.Group.WILDLIFE,0xA6AE8B));
		FieldGuide.add(new FieldGuide.Entry("wildercord:galeclaw",FieldGuide.Group.MONSTER,0x8FAAB8));
		spawn(STONEHORN,"stonehorn",3,"meadow","windswept_hills","windswept_gravelly_hills","stony_peaks","wildercord:windscar_highlands");
		spawn(GALECLAW,"galeclaw",2,"windswept_hills","windswept_forest","jagged_peaks","frozen_peaks","snowy_slopes","grove","wildercord:windscar_highlands");
		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->List.of(STONEHORN_PLATE,GALECLAW_PLUME,BASTION_POULTICE,RIDGE_WHISTLE,STONE_EGG,GALE_EGG).forEach(o::accept));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(o->{o.accept(STONE_EGG);o.accept(GALE_EGG);});
	}
}
