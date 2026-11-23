package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Rare loaded fungal cave residents; existing tamed animals remain usable when natural admission is disabled. */
public final class MossveilContent{
 private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("mossveil_dormouse"));
 public static final EntityType<MossveilDormouse> DORMOUSE=net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,EntityType.Builder.of(MossveilDormouse::new,MobCategory.CREATURE).sized(.55F,.60F).eyeHeight(.37F).clientTrackingRange(8).build(KEY));
 private static final ResourceKey<Item> EGG_KEY=ResourceKey.create(Registries.ITEM,Wildercord.id("mossveil_dormouse_spawn_egg"));
 public static final Item EGG=net.minecraft.core.Registry.register(BuiltInRegistries.ITEM,EGG_KEY,dev.wildercord.compat.McSpawnEgg.create(DORMOUSE, new Item.Properties().register(o->o.accept(EGG));
  ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->{o.accept(EGG);o.accept(MossveilCowl.ITEM);});
 }
}
