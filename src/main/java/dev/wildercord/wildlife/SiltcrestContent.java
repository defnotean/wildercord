package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
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
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,EGG_KEY,new SpawnEggItem(new Item.Properties().setId(EGG_KEY).spawnEgg(BITTERN)));
 static boolean room(net.minecraft.server.level.ServerLevel l,BlockPos p){var peers=new ArrayList<SiltcrestBittern>(2);l.getEntities(BITTERN,new AABB(p).inflate(32),Entity::isAlive,peers,2);return peers.size()<2;}
 public static void init(){FabricDefaultAttributeRegistry.register(BITTERN,SiltcrestBittern.attributes());
  SpawnPlacements.register(BITTERN,SpawnPlacementTypes.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,p,r)->{if(reason==EntitySpawnReason.CHUNK_GENERATION)return false;if(reason!=EntitySpawnReason.NATURAL)return true;if(!(l instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return false;var cfg=Config.get().wildlife();return level.dimension().equals(Level.OVERWORLD)&&cfg.spawns("siltcrest_bittern")&&r.nextDouble()<Math.min(1,.12*cfg.spawnMultiplier())&&BitternHabitat.bank(l,p)&&l.canSeeSky(p.above())&&room(level,p);});
  BiomeModifications.create(Wildercord.id("siltcrest_banks")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP)),c->{var cfg=Config.get().wildlife();if(cfg.spawns("siltcrest_bittern"))c.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(BITTERN,net.minecraft.util.valueproviders.UniformInt.of(1,1)),Math.max(1,(int)(2*Math.max(1,cfg.spawnMultiplier()))));});
  FieldGuide.add(new FieldGuide.Entry("wildercord:siltcrest_bittern",FieldGuide.Group.WILDLIFE,0xAD9570));
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o->o.accept(EGG));CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->o.accept(EGG));
 }
}
