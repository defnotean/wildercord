package dev.wildercord.wildlife;
import dev.wildercord.config.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Actual registered placement predicate; does not claim natural scheduler/entity admission. */
public final class RootmoltPlacementTest implements FabricClientGameTest {
 private static final BlockPos SITE=new BlockPos(0,30,0),CAP=SITE.east(2);
 @Override public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
  WildercordConfig original=w.getServer().computeOnServer(s->Config.get());
  try{
   w.getServer().runOnServer(s->{var l=s.overworld();habitat(l,SITE);set(original,Map.of("enabled",true,"rootmoltStrider",true,"spawnMultiplier",1.0));});
   c.waitTicks(15);
   w.getServer().runOnServer(s->{var l=s.overworld();System.out.println("ROOTMOLT_PLACEMENT initial light: skyVisible="+l.canSeeSky(SITE)+" raw="+l.getMaxLocalRawBrightness(SITE)+" sky="+l.getBrightness(net.minecraft.world.level.LightLayer.SKY,SITE)+" block="+l.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,SITE));check(!l.canSeeSky(SITE)&&l.getMaxLocalRawBrightness(SITE)<=8,"Real fixture is covered and actually dark");
    boolean some=false;var random=RandomSource.create(951);for(int i=0;i<100;i++)some|=SpawnPlacements.checkSpawnRules(RootmoltContent.STRIDER,l,EntitySpawnReason.NATURAL,SITE,random);check(some,"Registered ordinary random predicate admits eligible natural trials");
    set(original,Map.of("enabled",true,"rootmoltStrider",true,"spawnMultiplier",6.0));
    check(rule(l,SITE,EntitySpawnReason.NATURAL),"Actual probability clamp admits eligible natural site");check(!rule(l,SITE,EntitySpawnReason.CHUNK_GENERATION),"Eligible actual server-thread habitat conservatively refuses chunk generation; ordinary NATURAL positive is retained");
    l.setBlock(SITE.below(),Blocks.CLAY.defaultBlockState(),2);check(rule(l,SITE,EntitySpawnReason.NATURAL),"Clay is an actual alternative floor");
    l.setBlock(SITE.below(),Blocks.STONE.defaultBlockState(),2);refuse(l,SITE,"Ordinary stone floor");l.setBlock(SITE.below(),Blocks.MOSS_BLOCK.defaultBlockState(),2);
    l.setBlock(CAP,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,1),2);refuse(l,SITE,"Only an actually mature cap qualifies");l.removeBlock(CAP,false);refuse(l,SITE,"No cap cannot admit a spawn");l.setBlock(CAP,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);
    l.setBlock(SITE,Blocks.WATER.defaultBlockState(),2);refuse(l,SITE,"Wet candidate foot");l.setBlock(SITE,Blocks.AIR.defaultBlockState(),3);check(l.getFluidState(SITE).isEmpty(),"Actually drain the water fixture before later dry-foot admission");
    BlockPos high=SITE.above(18);habitat(l,high);refuse(l,high,"Height48 boundary");
    RootmoltStrider a=spawn(l,SITE.east(5)),b=spawn(l,SITE.west(5));refuse(l,SITE,"Two alive local Rootmolts reach actual cap");b.discard();diagnose(l,a,b);check(RootmoltContent.localRoom(l,RootmoltContent.STRIDER,SITE),"Actual one-peer native population query reopens after discard");check(rule(l,SITE,EntitySpawnReason.NATURAL),"Removing one actual local animal reopens capacity");a.discard();
    var nether=s.getLevel(Level.NETHER);check(nether!=null,"Actual Nether level exists");habitat(nether,SITE);refuse(nether,SITE,"Covered eligible non-Overworld habitat");
    check(rule(nether,SITE,EntitySpawnReason.COMMAND)&&rule(nether,SITE,EntitySpawnReason.SPAWN_ITEM_USE),"Explicit command and egg predicate bypass remains intact");
    for(var values:List.of(Map.<String,Object>of("enabled",false,"rootmoltStrider",true,"spawnMultiplier",6.0),Map.<String,Object>of("enabled",true,"rootmoltStrider",false,"spawnMultiplier",6.0),Map.<String,Object>of("enabled",true,"rootmoltStrider",true,"spawnMultiplier",0.0))){set(original,values);refuse(l,SITE,"Live wildlife master/creature/zero-multiplier switch");check(rule(l,SITE,EntitySpawnReason.COMMAND),"Natural switches do not block command branch");}
    set(original,Map.of("enabled",true,"rootmoltStrider",true,"spawnMultiplier",6.0));
   });
   w.getServer().runOnServer(s->s.overworld().setBlock(SITE.above(),Blocks.GLOWSTONE.defaultBlockState(),2));c.waitTicks(15);
   w.getServer().runOnServer(s->{check(s.overworld().getMaxLocalRawBrightness(SITE)>8,"Actual propagated block light reaches bright refusal fixture");refuse(s.overworld(),SITE,"Actually bright covered candidate");s.overworld().removeBlock(SITE.above(),false);});c.waitTicks(15);
   w.getServer().runOnServer(s->check(rule(s.overworld(),SITE,EntitySpawnReason.NATURAL),"Removing actual light reopens otherwise eligible habitat"));
   w.getServer().runOnServer(s->{var l=s.overworld();for(int y=SITE.getY()+3;y<=l.getMaxY();y++)l.removeBlock(new BlockPos(SITE.getX(),y,SITE.getZ()),false);});
   // The actual light engine propagates the opened column between server ticks.
   boolean exposed=false;for(int i=0;i<30;i++){c.waitTicks(3);if(w.getServer().computeOnServer(s->s.overworld().canSeeSky(SITE))){exposed=true;break;}}
   check(exposed,"Sky refusal fixture exposes actual candidate column through propagated skylight");
   w.getServer().runOnServer(s->{var l=s.overworld();System.out.println("ROOTMOLT_PLACEMENT sky exposure: skyVisible="+l.canSeeSky(SITE)+" raw="+l.getMaxLocalRawBrightness(SITE));refuse(l,SITE,"Sky-exposed candidate");l.setBlock(SITE.above(3),Blocks.STONE.defaultBlockState(),2);});
   w.getServer().runCommand("difficulty peaceful");w.getServer().runOnServer(s->refuse(s.overworld(),SITE,"Actual Peaceful difficulty"));
  }finally{w.getServer().runOnServer(s->current(original));}
 }}
 private static void habitat(ServerLevel l,BlockPos site){l.getChunkAt(site);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){var p=site.offset(x,0,z);l.setBlock(p.below(),Blocks.MOSS_BLOCK.defaultBlockState(),2);boolean perimeter=Math.abs(x)==4 || Math.abs(z)==4;for(int y=0;y<3;y++)l.setBlock(p.above(y),(perimeter?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);l.setBlock(p.above(3),Blocks.STONE.defaultBlockState(),2);}l.setBlock(site.east(2),FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);}
 private static boolean rule(ServerLevel l,BlockPos p,EntitySpawnReason reason){return SpawnPlacements.checkSpawnRules(RootmoltContent.STRIDER,l,reason,p,RandomSource.create(951));}
 private static void diagnose(ServerLevel l,RootmoltStrider a,RootmoltStrider b){
  var peers=new ArrayList<RootmoltStrider>();l.getEntities(RootmoltContent.STRIDER,new net.minecraft.world.phys.AABB(SITE).inflate(24),e->e.isAlive(),peers,4);
  System.out.println("ROOTMOLT_PLACEMENT reopen: room="+RootmoltContent.localRoom(l,RootmoltContent.STRIDER,SITE)+" peers="+peers.stream().map(e->e.getUUID()+":"+e.position()+":alive="+e.isAlive()+":removed="+e.isRemoved()).toList()+" firstAlive="+a.isAlive()+" firstRemoved="+a.isRemoved()+" discardedAlive="+b.isAlive()+" discardedRemoved="+b.isRemoved()+" config="+Config.get().wildlife()+" cap="+l.getBlockState(CAP)+" capFloor="+l.getBlockState(CAP.below())+" floor="+l.getBlockState(SITE.below())+" fluid="+l.getFluidState(SITE)+" raw="+l.getMaxLocalRawBrightness(SITE)+" skyVisible="+l.canSeeSky(SITE)+" sky="+l.getBrightness(net.minecraft.world.level.LightLayer.SKY,SITE)+" block="+l.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,SITE));
 }
 private static void refuse(ServerLevel l,BlockPos p,String why){check(!rule(l,p,EntitySpawnReason.NATURAL)&&!rule(l,p,EntitySpawnReason.CHUNK_GENERATION),why);}
 private static RootmoltStrider spawn(ServerLevel l,BlockPos p){var e=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Actual local-cap fixture");e.setNoAi(true);e.snapTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);l.addFreshEntity(e);return e;}
 private static void set(WildercordConfig original,Map<String,Object> changes){var wildlife=copy(original.wildlife(),changes);current(copy(original,Map.of("wildlife",wildlife)));}
 private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();Class<?>[] types=new Class<?>[parts.length];Object[] args=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();args[i]=changes.containsKey(parts[i].getName())?changes.get(parts[i].getName()):parts[i].getAccessor().invoke(record);}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig config){try{var field=Config.class.getDeclaredField("current");field.setAccessible(true);field.set(null,config);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
