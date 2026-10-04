package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.SiltcrestNative.*;
import dev.wildercord.config.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Registered spawn predicate and real rain/physical retreat, supplied habitat; not natural scheduler distribution. */
public final class SiltcrestHabitatTest implements FabricClientGameTest {
 private static final BlockPos SITE=new BlockPos(0,101,0),ROOF=new BlockPos(-2,103,-2);
 private SiltcrestBittern actor;
 public void runTest(ClientGameTestContext c){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");var original=w.getServer().computeOnServer(s->Config.get());
   try{
    w.getServer().runOnServer(s->{var l=s.overworld();floor(l);observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.8,.5));for(int y=102;y<=l.getMaxY();y++)l.setBlock(new BlockPos(0,y,0),Blocks.AIR.defaultBlockState(),2);set(original,Map.of("enabled",true,"siltcrestBittern",true,"spawnMultiplier",1.0));
     var registry=l.registryAccess().lookupOrThrow(Registries.BIOME);for(var key:List.of(Biomes.SWAMP,Biomes.MANGROVE_SWAMP))check(registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().anyMatch(e->e.value().type()==SiltcrestContent.BITTERN),"Actual loaded wetland spawn pool includes the bird: "+key);check(registry.getOrThrow(Biomes.PLAINS).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY).getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().noneMatch(e->e.value().type()==SiltcrestContent.BITTERN),"Ordinary plains pool excludes the wetland bird");
    });
    await(c,w,60,s->s.overworld().canSeeSky(SITE.above()),"Actual cleared candidate column receives real sky visibility");
    // Retain the actual ServerLevel reference only; the off-thread refusal must occur before habitat/entity queries.
    var actualLevel=w.getServer().computeOnServer(s->{check(s.isSameThread(),"Positive fixture runs on the actual owning server thread");return s.overworld();});
    check(!actualLevel.getServer().isSameThread(),"Refusal fixture is the actual native test thread, not a scheduled server callback");
    for(int n=0;n<16;n++){
     check(!rule(actualLevel,EntitySpawnReason.NATURAL,n),"Actual off-thread ServerLevel NATURAL admission refuses before live storage/entity access");
     check(!rule(actualLevel,EntitySpawnReason.CHUNK_GENERATION,n),"Actual off-thread CHUNK_GENERATION remains conservatively refused");
    }
    check(rule(actualLevel,EntitySpawnReason.COMMAND,951)&&rule(actualLevel,EntitySpawnReason.SPAWN_ITEM_USE,951),"Explicit command/egg predicate bypass stays independent of thread/natural admission");
    w.getServer().runOnServer(s->{var l=s.overworld();check(BitternHabitat.bank(l,SITE)&&l.getFluidState(SITE).isEmpty(),"Actual dry bank is physically supported and water-adjacent");admit(l,"Original1.0 probability admits bounded96 real natural trials");for(int n=0;n<16;n++)check(!rule(l,EntitySpawnReason.CHUNK_GENERATION,n),"Even actual eligible server-thread habitat explicitly refuses CHUNK_GENERATION; no original-generation population claim");
     var a=bird(l,-3.5,.5);var b=bird(l,-4.5,.5);refuse(l,"Two actual live local birds reach the unchanged cap");b.discard();check(SiltcrestContent.room(l,SITE),"Actual discard opens the threshold-two population query");admit(l,"One live local bird reopens ordinary random admission");a.discard();
     l.setBlock(SITE.above(),Blocks.WATER.defaultBlockState(),2);refuse(l,"Actual water at head height refuses habitat");l.setBlock(SITE.above(),Blocks.AIR.defaultBlockState(),3);check(l.getFluidState(SITE.above()).isEmpty(),"Actual water control is drained before further checks");
     var nether=s.getLevel(Level.NETHER);check(nether!=null,"Native Nether level exists");floor(nether);refuse(nether,"Eligible supplied bank in Nether still refuses natural and chunk generation");check(rule(nether,EntitySpawnReason.COMMAND,951),"Command predicate bypass remains intact");
     for(var values:List.of(Map.<String,Object>of("enabled",false,"siltcrestBittern",true,"spawnMultiplier",1.0),Map.<String,Object>of("enabled",true,"siltcrestBittern",false,"spawnMultiplier",1.0),Map.<String,Object>of("enabled",true,"siltcrestBittern",true,"spawnMultiplier",0.0))){set(original,values);refuse(l,"Actual master/species/zero-multiplier switch refuses random spawning");check(rule(l,EntitySpawnReason.COMMAND,951),"Natural configuration does not alter command predicate");}set(original,Map.of("enabled",true,"siltcrestBittern",true,"spawnMultiplier",1.0));
     l.setBlock(SITE.above(2),Blocks.STONE.defaultBlockState(),3);
    });
    await(c,w,60,s->!s.overworld().canSeeSky(SITE.above()),"Actual overhead stone blocks sky");w.getServer().runOnServer(s->{refuse(s.overworld(),"Actually covered candidate refuses natural spawning");s.overworld().setBlock(SITE.above(2),Blocks.AIR.defaultBlockState(),3);});await(c,w,60,s->s.overworld().canSeeSky(SITE.above()),"Removing actual roof restores sky");w.getServer().runOnServer(s->admit(s.overworld(),"Removing roof restores ordinary positive control"));
   }finally{w.getServer().runOnServer(s->current(original));}
  }
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather rain");
   w.getServer().runOnServer(s->{var l=s.overworld();floor(l);l.setBlock(ROOF,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);l.setBlock(new BlockPos(-1,100,-2),Blocks.WATER.defaultBlockState(),2);observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(-1.5,101.8,-1.5));actor=bird(l,.5,.5);check(actor.huntReady()==0&&BitternHabitat.bank(l,actor.blockPosition())&&l.getFluidState(actor.blockPosition()).isEmpty(),"Actual initial dry bank, hungry creature and ordinary AI");});
   await(c,w,420,s->s.overworld().isRaining()&&actor.pose()==SiltcrestBittern.SHELTERING&&actor.onGround()&&actor.shelter()!=null&&BitternHabitat.shelter(s.overworld(),actor.shelter())&&actor.distanceToSqr(actor.shelter().getX()+.5,actor.shelter().getY(),actor.shelter().getZ()+.5)<=.36,"Real rain causes ordinary navigation to a grounded covered bank");
   w.getServer().runOnServer(s->{check(actor.huntReady()==0,"Rain shelter cannot invent an earned fish meal");var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();place(p,actor.getX(),101,actor.getZ()-1.7,actor.getBoundingBox().getCenter());check(actor.getHealth()==12,"Actual living target begins with original full health");});c.waitTicks(5);int id=w.getServer().computeOnServer(s->actor.getId());c.runOnClient(mc->mc.gameMode.attack(mc.player,mc.level.getEntity(id)));
   await(c,w,20,s->actor.getHealth()<12&&actor.getHealth()>0&&actor.pose()==SiltcrestBittern.RETREATING,"Real Survival attack packet admits physical injury and readable retreat");
   w.getServer().runOnServer(s->{long now=SiltcrestBittern.clock(s.overworld());check(actor.huntReady()>now&&actor.huntReady()<=now+200&&actor.pose()!=SiltcrestBittern.SHELTERING,"Physical fright stops active rest and grants only its original finite200-tick hunt pause, not a6000-tick fish meal");});
  }
 }
 private static boolean rule(net.minecraft.server.level.ServerLevel l,EntitySpawnReason why,long seed){return SpawnPlacements.checkSpawnRules(SiltcrestContent.BITTERN,l,why,SITE,RandomSource.create(seed));}
 private static void admit(net.minecraft.server.level.ServerLevel l,String why){var r=RandomSource.create(951);boolean found=false;for(int i=0;i<96;i++)found|=SpawnPlacements.checkSpawnRules(SiltcrestContent.BITTERN,l,EntitySpawnReason.NATURAL,SITE,r);check(found,why);}
 private static void refuse(net.minecraft.server.level.ServerLevel l,String why){for(int i=0;i<16;i++)check(!rule(l,EntitySpawnReason.NATURAL,i)&&!rule(l,EntitySpawnReason.CHUNK_GENERATION,i),why);}
 private static void set(WildercordConfig old,Map<String,Object> change){current(copy(old,Map.of("wildlife",copy(old.wildlife(),change))));}
 private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=changes.containsKey(parts[i].getName())?changes.get(parts[i].getName()):parts[i].getAccessor().invoke(record);}return (T)record.getClass().getDeclaredConstructor(types).newInstance(values);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig value){try{var field=Config.class.getDeclaredField("current");field.setAccessible(true);field.set(null,value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
