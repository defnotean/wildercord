package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Ordinary old and new swamp banks can admit this solitary bird; no feature/structure or population generator. */
public final class SiltcrestContent {
 private SiltcrestContent(){}
 private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("siltcrest_bittern"));
 public static final EntityType<SiltcrestBittern> BITTERN=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,EntityType.Builder.of(SiltcrestBittern::new,MobCategory.CREATURE).sized(.65F,1.6F).eyeHeight(1.30F).clientTrackingRange(8).build(KEY));
 private static final ResourceKey<Item> EGG_KEY=ResourceKey.create(Registries.ITEM,Wildercord.id("siltcrest_bittern_spawn_egg"));
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,EGG_KEY,dev.wildercord.compat.McSpawnEgg.create(BITTERN, new Item.Properties());ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->o.accept(EGG));
 }
}
