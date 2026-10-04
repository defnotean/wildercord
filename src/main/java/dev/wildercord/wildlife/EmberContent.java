package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.*;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Creature/plant only: no later relic, book or quest registrations. */
public final class EmberContent {
 private EmberContent(){}
 private static final ResourceKey<EntityType<?>> ENTITY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("cinder_bailiff"));
 public static final EntityType<CinderBailiff> BAILIFF=Registry.register(BuiltInRegistries.ENTITY_TYPE,ENTITY,EntityType.Builder.of(CinderBailiff::new,MobCategory.MONSTER).sized(1.15F,1.0F).eyeHeight(.75F).clientTrackingRange(8).build(ENTITY));
 private static final ResourceKey<Block> BLOCK=ResourceKey.create(Registries.BLOCK,Wildercord.id("cinder_fern"));
 public static final CinderFernBlock FERN=Registry.register(BuiltInRegistries.BLOCK,BLOCK,new CinderFernBlock(BlockBehaviour.Properties.of().setId(BLOCK).noCollision().noOcclusion().instabreak().randomTicks().sound(SoundType.AZALEA)));
 private static ResourceKey<Item> item(String n){return ResourceKey.create(Registries.ITEM,Wildercord.id(n));}
 public static final Item FERN_ITEM=Registry.register(BuiltInRegistries.ITEM,item("cinder_fern"),new BlockItem(FERN,new Item.Properties().setId(item("cinder_fern")).useBlockDescriptionPrefix()));
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,item("cinder_bailiff_spawn_egg"),new SpawnEggItem(new Item.Properties().setId(item("cinder_bailiff_spawn_egg")).spawnEgg(BAILIFF)));
 static boolean room(net.minecraft.server.level.ServerLevel l,BlockPos p){var peers=new ArrayList<CinderBailiff>(EmberRules.LOCAL_CAP);l.getEntities(BAILIFF,new AABB(p).inflate(32),Entity::isAlive,peers,EmberRules.LOCAL_CAP);return peers.size()<EmberRules.LOCAL_CAP;}
 public static void init(){Registry.register(BuiltInRegistries.FEATURE_TYPE,Wildercord.id("cinder_fern_patch"),CinderFernFeature.CODEC);FabricDefaultAttributeRegistry.register(BAILIFF,CinderBailiff.attributes());
  SpawnPlacements.register(BAILIFF,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,p,r)->{if(reason==EntitySpawnReason.CHUNK_GENERATION)return false;if(reason!=EntitySpawnReason.NATURAL)return true;if(!(l instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return false;
   // Root integration must add the cinder_bailiff per-species switch before promotion.
   if(!l.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||l.getDifficulty()==Difficulty.PEACEFUL||!Config.get().wildlife().spawns("cinder_bailiff")||r.nextDouble()>=Math.min(1,.12*Config.get().wildlife().spawnMultiplier())||!CinderFernBlock.footing(l.getBlockState(p.below()))||!l.getFluidState(p).isEmpty()||!room(l.getLevel(),p))return false;
   for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=1;y++)if(CinderBailiff.mature(l.getLevel(),p.offset(x,y,z)))return true;return false;
  });
  var biomes=Set.of(Biomes.FOREST,Biomes.WOODED_BADLANDS);BiomeModifications.create(Wildercord.id("ember_bailiffs")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(biomes),c->{var s=Config.get().wildlife();if(s.spawns("cinder_bailiff"))c.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(BAILIFF,net.minecraft.util.valueproviders.UniformInt.of(1,1)),Math.max(1,(int)(2*Math.max(1,s.spawnMultiplier()))));});
  BiomeModifications.addFeature(BiomeSelectors.includeByKey(biomes),GenerationStep.Decoration.VEGETAL_DECORATION,ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("cinder_fern_patch")));
  FieldGuide.add(new FieldGuide.Entry("wildercord:cinder_bailiff",FieldGuide.Group.WILDLIFE,0xA16D48));CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o->{o.accept(FERN_ITEM);o.accept(EGG);});CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o->o.accept(EGG));
 }
 /** Successful actual cooling owns one Warning interruption per shared payment. */
 public static boolean cool(Cast cast,LivingEntity target){if(!cast.alive()||cast.passive||!(cast.caster instanceof net.minecraft.server.level.ServerPlayer player)||player.isCreative()||player.isSpectator()||!(target instanceof CinderBailiff bailiff)||bailiff.pose()!=CinderBailiff.WARNING||target.level()!=cast.level||!target.isAlive()||target.isRemoved()||player.distanceToSqr(target)>=64||!player.hasLineOfSight(target))return false;if(!cast.once("ember_bailiff_cooling"))return false;return bailiff.cool(player);}
 /** Called only by admitted effect owners, not generic appearance events. Shares once/payment+block budgets. */
 public static boolean affectFern(Cast cast,BlockPos p,String element){return affectFern(cast,p,element,EmberPlantAdmission.WORLD);}
 private static boolean plantCaster(Cast cast,BlockPos p){return cast.alive()&&!cast.passive&&cast.caster instanceof net.minecraft.server.level.ServerPlayer player&&!player.isCreative()&&!player.isSpectator()&&cast.level.hasChunkAt(p)&&cast.level.getWorldBorder().isWithinBounds(p)&&Casters.mayBuild(player)&&cast.level.mayInteract(player,p)&&!dev.wildercord.cast.Effects.isTemporary(cast.level,p)&&cast.level.getBlockEntity(p)==null;}
 private static boolean plantClaim(Cast cast,BlockPos p){var player=(net.minecraft.server.level.ServerPlayer)cast.caster;return Casters.probeBreak(cast.level,player,p,cast.level.getBlockState(p),cast.level.getBlockEntity(p));}
 /** A successful boolean writer alone is insufficient: actual retained state and caster authority own success. */
 static boolean affectFern(Cast cast,BlockPos p,String element,EmberPlantAdmission.Writer writer){
  if(!Set.of("fire","water","life").contains(element)||!plantCaster(cast,p))return false;var old=cast.level.getBlockState(p);if(!old.is(FERN)||!old.canSurvive(cast.level,p))return false;var next=old;
  if(element.equals("water"))next=old.setValue(CinderFernBlock.COOLED,true);else if(element.equals("fire"))next=old.setValue(CinderFernBlock.COOLED,false).setValue(CinderFernBlock.AGE,Math.min(2,old.getValue(CinderFernBlock.AGE)+1));else if(old.getValue(CinderFernBlock.COOLED)&&old.getValue(CinderFernBlock.AGE)<2)next=old.setValue(CinderFernBlock.AGE,old.getValue(CinderFernBlock.AGE)+1);
  if(next.equals(old))return false;var body=cast.caster.position();try(var lease=EmberPlantAdmission.open(cast.level,p)){
   if(lease==null||!plantClaim(cast,p)||!plantCaster(cast,p)||!cast.caster.position().equals(body)||!cast.level.getBlockState(p).equals(old)||!old.canSurvive(cast.level,p))return false;
   if(!cast.once("ember_fern_mutation")||!cast.takeBlocks(1))return false;
   if(!writer.set(cast.level,p,next)||!plantCaster(cast,p)||!cast.caster.position().equals(body)||!cast.level.getBlockState(p).equals(next)||!next.canSurvive(cast.level,p))return false;
   return plantClaim(cast,p)&&plantCaster(cast,p)&&cast.caster.position().equals(body)&&cast.level.getBlockState(p).equals(next)&&next.canSurvive(cast.level,p);
  }
 }
}
