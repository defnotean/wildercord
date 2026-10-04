package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
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
 public static final Item EGG=net.minecraft.core.Registry.register(BuiltInRegistries.ITEM,EGG_KEY,new SpawnEggItem(new Item.Properties().setId(EGG_KEY).spawnEgg(DORMOUSE)));
 static boolean room(ServerLevel l,BlockPos at){var peers=new ArrayList<MossveilDormouse>(3);l.getEntities(DORMOUSE,new AABB(at).inflate(24),e->true,peers,3);return peers.size()<3&&peers.stream().filter(Entity::isAlive).count()<2;}
 public static void init(){
  FabricDefaultAttributeRegistry.register(DORMOUSE,MossveilDormouse.attributes());MossveilParticles.init();MossveilFilterCue.init();MossveilCowl.init();
  SpawnPlacements.register(DORMOUSE,SpawnPlacementTypes.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,p,r)->{
   if(reason==EntitySpawnReason.CHUNK_GENERATION)return false;
   if(reason!=EntitySpawnReason.NATURAL)return true;
   // Chunk-generation accessors cannot safely enumerate live actor populations.
   if(!(l instanceof ServerLevel actual)||!actual.getServer().isSameThread())return false;
   var cfg=Config.get().wildlife();return cfg.spawns("mossveil_dormouse")&&r.nextDouble()<Math.min(1,.08*cfg.spawnMultiplier())&&MossveilHome.natural(actual,p)&&room(actual,p);
  });
  BiomeModifications.create(Wildercord.id("mossveil_caves")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.LUSH_CAVES)),c->{var cfg=Config.get().wildlife();if(cfg.spawns("mossveil_dormouse"))c.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(DORMOUSE,net.minecraft.util.valueproviders.UniformInt.of(1,1)),Math.max(1,(int)Math.ceil(2*cfg.spawnMultiplier())));});
  FieldGuide.add(new FieldGuide.Entry("wildercord:mossveil_dormouse",FieldGuide.Group.WILDLIFE,0x8C9870));
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o->o.accept(EGG));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->{o.accept(EGG);o.accept(MossveilCowl.ITEM);});
 }
}
