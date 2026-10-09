package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** Actual registered moth callback identity/body/liveness faults, followed by ordinary unpaused native pollination. */
public final class MoonreedSourceTest implements FabricClientGameTest {
 private static final BlockPos ROOT=new BlockPos(0,30,0);
 private Entity transferred;
 public void runTest(ClientGameTestContext c){
  try(var w=c.worldBuilder().create()){
   try {
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(l,6.5,30,6.5,Set.<Relative>of(),0,0,false);
    for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){l.setBlock(new BlockPos(x,29,z),Blocks.DIRT.defaultBlockState(),2);for(int y=30;y<=34;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setBlock(ROOT.east().below(2),Blocks.DIRT.defaultBlockState(),2);l.setBlock(ROOT.east().below(),Blocks.WATER.defaultBlockState(),2);
    // Expose the actual root column; retain the strict WORLD_SURFACE/open-sky check on any supplied world template.
    for(int y=35;y<=l.getMaxY();y++)l.setBlock(new BlockPos(ROOT.getX(),y,ROOT.getZ()),Blocks.AIR.defaultBlockState(),2);
    var destination=s.getLevel(Level.NETHER);check(destination!=null,"Actual source-transfer destination exists");for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=100;y<=120;y++)destination.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    for(int mode=0;mode<4;mode++){
     bud(l);var moth=moth(l,new Vec3(.5,30.9,.5));var identity=moth.getUUID();final int fault=mode;
     boolean result=MoonreedBlock.pollinate(l,ROOT,moth,(world,at,next)->{
      boolean wrote=world.setBlock(at,next,2);check(wrote,"Native source-fault fixture performs the real bloom write");
      if(fault==0)moth.discard();
      if(fault==1){check(moth.hurtServer(world,world.damageSources().generic(),100),"Actual lethal damage callback admitted");check(!moth.isAlive(),"Actual source died during retained bloom write");}
      if(fault==2)moth.snapTo(.75,30.9,.5,0,0);
      if(fault==3){transferred=moth.teleport(new net.minecraft.world.level.portal.TeleportTransition(destination,new Vec3(.5,110,.5),Vec3.ZERO,0,0,Set.<Relative>of(),net.minecraft.world.level.portal.TeleportTransition.PLACE_PORTAL_TICKET));check(transferred instanceof Glimmerwing&&transferred!=moth&&transferred.getUUID().equals(identity)&&transferred.level()==destination&&transferred.isAlive()&&!transferred.isRemoved(),"Actual teleport returns a live same-UUID pollinator clone in the other world");check(moth.isRemoved()||moth.level()!=world,"Actual old source departed synchronously before bloom outcome admission");}
      return true;
     });
     check(!result,"Removed, dead, body-moved or cross-world actual moth cannot report admitted pollination");
     check(l.getBlockState(ROOT).is(WetlandGarden.REED)&&l.getBlockState(ROOT).getValue(MoonreedBlock.AGE)==2,"Source refusal reports no success; this candidate does not claim mature-state rollback");
     if(fault==2)check(moth.position().distanceToSqr(Vec3.atCenterOf(ROOT).add(0,.4,0))<1,"Body-movement refusal is stricter than merely retaining old contact-range admission");
     moth.discard();
    }
    bud(l);var unregistered=Wildlife.GLIMMERWING.create(l,EntitySpawnReason.COMMAND);check(unregistered!=null,"Actual mapped unregistered factory fixture");unregistered.snapTo(.5,30.9,.5,0,0);int[] writes={0};check(!MoonreedBlock.pollinate(l,ROOT,unregistered,(world,at,next)->{writes[0]++;return world.setBlock(at,next,2);})&&writes[0]==0,"An untracked factory object is not accepted as a live registered pollinator");unregistered.discard();
    bud(l);var valid=moth(l,new Vec3(.5,30.9,.5));check(MoonreedBlock.pollinate(l,ROOT,valid)&&l.getBlockState(ROOT).getValue(MoonreedBlock.AGE)==2,"Unchanged registered source blooms normally after all source faults and lease cleanup");valid.discard();
    bud(l);check(MoonreedBlock.pollinate(l,ROOT,Vec3.atCenterOf(ROOT).add(0,.4,0)),"Original Vec3 compatibility/fault seam remains available and explicitly separate from live source authority");
    bud(l);var flying=moth(l,new Vec3(2.5,31,.5));MoonreedSourceProbe.begin(l,ROOT,flying);
   });
   c.waitTicks(45);w.getServer().runOnServer(s->{check(transferred!=null&&s.getLevel(Level.NETHER).getEntity(transferred.getUUID())==transferred&&transferred.isAlive()&&!transferred.isRemoved(),"Actual portal ticket admits the exact transferred source into destination UUID lookup after ordinary ticks");transferred.discard();transferred=null;});
   boolean arrived=false;for(int n=0;n<80;n++){boolean lastAttempt=n==79;c.waitTicks(5);if(w.getServer().computeOnServer(s->MoonreedSourceProbe.waited(s.overworld().getBlockState(ROOT).is(WetlandGarden.REED)&&s.overworld().getBlockState(ROOT).getValue(MoonreedBlock.AGE)==2,lastAttempt))){arrived=true;break;}}
   boolean observed=MoonreedSourceProbe.finish(arrived);
   check(arrived,"Actual unpaused Glimmerwing finds and reaches the bud through production entity-source overload; no injected arrival pose");
   check(observed,"Passive native receipt observes an initially distant moth acquire this root, enter the production entity overload and succeed through the pollination lease without observation errors");
   }finally{MoonreedSourceProbe.clear();}
  }
 }
 private static Glimmerwing moth(ServerLevel l,Vec3 at){var moth=Wildlife.GLIMMERWING.create(l,EntitySpawnReason.COMMAND);check(moth!=null,"Actual registered moth factory");moth.snapTo(at.x,at.y,at.z,0,0);moth.setPersistenceRequired();check(l.addFreshEntity(moth),"Actual moth is added to source world");check(l.getEntity(moth.getUUID())==moth,"Real tracked source identity");return moth;}
 private static void bud(ServerLevel l){l.setBlock(ROOT,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,1),2);check(MoonreedBlock.canBloom(l,ROOT,l.getOverworldClockTime()),"Source fixture is an actual moist open-sky night bud");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
