package dev.wildercord.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Cap competition and owned physical control; observations are facts, never item rewards. */
public final class RootmoltContent {
 private RootmoltContent() {}
 public static final UUID NO_OWNER=new UUID(0,0);
 public static final AttachmentType<UUID> GRAB_OWNER=AttachmentRegistry.create(Wildercord.id("root_grab_owner"),b -> b.initializer(() -> NO_OWNER));
 public static final Holder<MobEffect> TETHER=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Wildercord.id("root_tether"),new RootTetherEffect());
 private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Wildercord.id("rootmolt_strider"));
 public static final EntityType<RootmoltStrider> STRIDER=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,EntityType.Builder.of(RootmoltStrider::new,MobCategory.MONSTER).sized(1.05F,.8F).eyeHeight(.48F).clientTrackingRange(8).build(KEY));
 private static final ResourceKey<Item> EGG_KEY=ResourceKey.create(Registries.ITEM,Wildercord.id("rootmolt_strider_spawn_egg"));
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,EGG_KEY,new SpawnEggItem(new Item.Properties().setId(EGG_KEY).spawnEgg(STRIDER)));
 @FunctionalInterface public interface RootResistance {boolean resist(LivingEntity victim,RootmoltStrider source);}
 /** Providers must admit and spend their finite equipment preparation atomically, after actual contact. */
 public static final Event<RootResistance> ROOT_RESISTANCE=EventFactory.createArrayBacked(RootResistance.class,callbacks -> (victim,source) -> {for(var callback:callbacks){if(!contact(victim,source))return false;if(callback.resist(victim,source))return true;}return false;});
 private static boolean contact(LivingEntity victim,RootmoltStrider source) {return victim.isAlive() && !victim.isRemoved() && source.isAlive() && !source.isRemoved() && victim.level()==source.level() && source.pose()==RootmoltStrider.RAKING && source.getTarget()==victim && source.distanceToSqr(victim)<16 && source.hasLineOfSight(victim) && source.onLine(victim.position()) && victim.getAttachedOrElse(GRAB_OWNER,NO_OWNER).equals(NO_OWNER) && !(victim instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()));}
 static boolean resistRoot(LivingEntity victim,RootmoltStrider source) {return contact(victim,source) && ROOT_RESISTANCE.invoker().resist(victim,source);}
 private static boolean observer(ServerPlayer p,RootmoltStrider source) {return p.isAlive() && !p.isSpectator() && !p.isCreative() && p.level()==source.level() && p.distanceToSqr(source)<=36 && source.hasLineOfSight(p);}
 static void observedMeal(RootmoltStrider source,BlockPos cap) {if(!(source.level() instanceof ServerLevel l))return;for(var p:l.getEntitiesOfClass(ServerPlayer.class,source.getBoundingBox().inflate(6),p -> observer(p,source)))FungalInvestigation.remember(p,"field:rootmolt_meal");}
 static void countered(ServerPlayer p,RootmoltStrider source) {if(observer(p,source) && FungalInvestigation.knows(p,"field:rootmolt_meal"))FungalInvestigation.remember(p,"field:rootmolt_counter");}
 static final int ALARM_INSPECT=32;
 /** A full small pool permits deterministic ordering; a truncated pool never biases the receivers. */
 static List<SporebackSnail> alarmCandidates(RootmoltStrider source) {
  if(!(source.level() instanceof ServerLevel l))return List.of();
  var nearby=new ArrayList<SporebackSnail>(ALARM_INSPECT+1);
  // Include hidden/dead candidates before admission so rejected prefixes cannot defeat the cap.
  l.getEntities(EntityTypeTest.<Entity,SporebackSnail>forClass(SporebackSnail.class),source.getBoundingBox().inflate(4),e -> true,nearby,ALARM_INSPECT+1);
  if(nearby.size()>ALARM_INSPECT)return List.of();
  nearby.removeIf(e -> !e.isAlive() || e.isRemoved() || e.pose()==2 || source.distanceToSqr(e)>16);
  nearby.sort(Comparator.<SporebackSnail>comparingDouble(source::distanceToSqr).thenComparing(e -> e.getUUID().toString()));
  return List.copyOf(nearby);
 }
 static void alarm(RootmoltStrider source) {
  if(!(source.level() instanceof ServerLevel) || !source.isAlive() || source.isRemoved() || source.pose()!=RootmoltStrider.WARNING)return;
  for(var snail:alarmCandidates(source))if(loadedAlarmSight(source,snail))snail.answerThreat();
 }
 private static boolean loadedAlarmSight(RootmoltStrider source,SporebackSnail snail) {
  // At four blocks of range the eye ray intersects at most four horizontal chunks.
  var a=BlockPos.containing(source.getEyePosition());var b=BlockPos.containing(snail.getEyePosition());
  for(int x=Math.min(a.getX(),b.getX())>>4;x<=(Math.max(a.getX(),b.getX())>>4);x++)
   for(int z=Math.min(a.getZ(),b.getZ())>>4;z<=(Math.max(a.getZ(),b.getZ())>>4);z++)
    if(!source.level().hasChunkAt(new BlockPos(x<<4,a.getY(),z<<4)))return false;
  return source.hasLineOfSight(snail);
 }
 /** Natural admission needs only the two live local peers which exhaust its population allowance. */
 static boolean localRoom(ServerLevel l,EntityType<RootmoltStrider> type,BlockPos p) {
  var peers=new ArrayList<RootmoltStrider>(RootmoltRules.LOCAL_CAP);
  l.getEntities(type,new AABB(p).inflate(24),Entity::isAlive,peers,RootmoltRules.LOCAL_CAP);
  return peers.size()<RootmoltRules.LOCAL_CAP;
 }
 public static void init() {
  FabricDefaultAttributeRegistry.register(STRIDER,RootmoltStrider.attributes());
  SpawnPlacements.register(STRIDER,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,l,reason,p,r) -> {
   if(reason!=EntitySpawnReason.NATURAL && reason!=EntitySpawnReason.CHUNK_GENERATION)return true;
   if(!l.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return false;
   var settings=Config.get().wildlife();if(l.getDifficulty()==Difficulty.PEACEFUL || !settings.spawns("rootmolt_strider") || r.nextDouble()>=Math.min(1,.18*settings.spawnMultiplier()) || p.getY()>=48 || l.canSeeSky(p) || l.getMaxLocalRawBrightness(p)>8 || !l.getFluidState(p).isEmpty())return false;
   var floor=l.getBlockState(p.below());if(!(floor.is(Blocks.MOSS_BLOCK) || floor.is(Blocks.CLAY)) || !localRoom(l.getLevel(),type,p))return false;
   for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=1;y++) {var at=p.offset(x,y,z);if(!l.hasChunkAt(at))continue;var state=l.getBlockState(at);if(state.is(FungalGarden.GLOWCAP) && state.getValue(GlowcapBlock.AGE)==2)return true;}return false;
  });
  BiomeModifications.create(Wildercord.id("rootmolt_caves")).add(ModificationPhase.ADDITIONS,BiomeSelectors.includeByKey(Set.of(Biomes.LUSH_CAVES,Biomes.DRIPSTONE_CAVES)),c -> {var s=Config.get().wildlife();if(s.spawns("rootmolt_strider"))c.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(STRIDER,net.minecraft.util.valueproviders.UniformInt.of(1,1)),Math.max(1,(int)(3*Math.max(1,s.spawnMultiplier()))));});
  FieldGuide.add(new FieldGuide.Entry("wildercord:rootmolt_strider",FieldGuide.Group.WILDLIFE,0xAB926B));
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Wildercord.id("wildercord"))).register(o -> o.accept(EGG));CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(o -> o.accept(EGG));
 }
}
