package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** Genuine retaliation, bounded withdrawal, earned home and committed warning; supplied habitat only. */
public final class CinderBailiffTerritoryTest implements FabricClientGameTest {
 private CinderBailiff mob;
 private Zombie attacker;
 private Vec3 direction;
 private BlockPos preparedFern;
 private long meal, attack, rest;

 @Override public void runTest(ClientGameTestContext c){
  withdrawal(c,false);
  withdrawal(c,true);
  earnedHome(c);
 }

 private void withdrawal(ClientGameTestContext c,boolean wall){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);configure(w.getServer());
   w.getServer().runOnServer(s->{arena(s);spawn(s);});
   c.waitTicks(5); // Actual grounding before genuine injury; no pose or velocity writes.
   w.getServer().runOnServer(s->{
    var l=s.overworld();attacker=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);attacker.setNoAi(true);
    // Named supplied actors bypass the unrelated random Runebound load-time health bonus.
    attacker.setCustomName(net.minecraft.network.chat.Component.literal("Cinder Bailiff withdrawal witness"));
    attacker.snapTo(mob.getX(),30,mob.getZ()+2,0,0);l.addFreshEntity(attacker);
    check(attacker.getHealth()==20&&attacker.getMaxHealth()==20,"Actual admitted withdrawal witness starts with exactly twenty health: health="+attacker.getHealth()+" max="+attacker.getMaxHealth());
    check(mob.hurtServer(l,attacker.damageSources().mobAttack(attacker),1),"Real physical injury retains actual aggressor");
    check(mob.getTarget()==attacker&&mob.pose()==CinderBailiff.IDLE,"Actual injury starts idle target ownership, not synthetic warning");
    meal=mob.mealReady();attack=mob.attackReady();rest=mob.restUntil();
    if(wall){
     attacker.snapTo(mob.getX(),30,mob.getZ()+2.5,0,0);
     int z=mob.blockPosition().getZ()+1;
     for(int x=mob.blockPosition().getX()-2;x<=mob.blockPosition().getX()+2;x++)for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState(),2);
     check(mob.distanceToSqr(attacker)<16&&!mob.hasLineOfSight(attacker),"Real stone wall obstructs an otherwise reachable attacker");
    }else{
     attacker.snapTo(mob.getX(),30,mob.getZ()+5.5,0,0);
     check(mob.distanceToSqr(attacker)>16&&mob.distanceToSqr(attacker)<64&&mob.hasLineOfSight(attacker),"Actual retreat leaves four-block warning reach but remains a valid nearby actor");
    }
   });
   boolean released=false;
   for(int n=0;n<6;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->mob.getTarget()==null)){released=true;break;}}
   check(released,"Idle unreachable target is released by the original ten-tick admission interval");
   w.getServer().runOnServer(s->{
    check(mob.pose()!=CinderBailiff.WARNING&&mob.pose()!=CinderBailiff.FANNING,"Withdrawal does not create an attack phase");
    check(attacker.getHealth()==20,"Release deals no physical or periodic damage: health="+attacker.getHealth()+" max="+attacker.getMaxHealth());
    check(mob.mealReady()==meal&&mob.attackReady()==attack&&mob.restUntil()==rest,"Mere release creates no saved rest deadline");
    check(mob.mayBrowse(),"Target release restores genuine habitat admission");
    attacker.discard();
    if(wall){int z=mob.blockPosition().getZ()+1;for(int x=-6;x<=6;x++)for(int y=30;y<=32;y++)s.overworld().setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
    preparedFern=mob.blockPosition().offset(2,0,0);s.overworld().setBlock(preparedFern,EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,2),2);
   });
   boolean fed=false;for(int n=0;n<150;n++){c.waitTicks(5);if(w.getServer().computeOnServer(s->mob.mealReady()>meal)){fed=true;break;}}
   check(fed,"Same released actor resumes actual navigation and completes a real fern meal");
   w.getServer().runOnServer(s->{check(preparedFern.equals(mob.home())&&s.overworld().getBlockState(preparedFern).is(EmberContent.FERN)&&s.overworld().getBlockState(preparedFern).getValue(CinderFernBlock.AGE)==1,"Actual resumed meal consumes mature growth once, preserves root and earns that real home");check(mob.mealReady()>s.overworld().getGameTime()&&mob.mealReady()<=s.overworld().getGameTime()+EmberRules.MEAL_REST,"Actual resumed meal grants the original finite rest");});
  }
 }

 private void earnedHome(ClientGameTestContext c){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);configure(w.getServer());
   w.getServer().runOnServer(s->{arena(s);spawn(s);s.overworld().setBlock(new BlockPos(2,30,0),EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,2),2);});
   boolean fed=false;for(int n=0;n<150;n++){c.waitTicks(5);if(w.getServer().computeOnServer(s->mob.home()!=null)){fed=true;break;}}
   check(fed,"Home is earned only by actual forty-tick fern browse and real age consumption");
   boolean idle=false;for(int n=0;n<90;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->mob.pose()==CinderBailiff.IDLE)){idle=true;break;}}
   check(idle,"Earned finite rest expires through ordinary ticks");
   w.getServer().runOnServer(s->{
    var p=s.getPlayerList().getPlayers().getFirst();var h=Vec3.atCenterOf(mob.home());
    var outward=mob.position().subtract(h).multiply(1,0,1).normalize();
    check(outward.lengthSqr()>.5,"Earned visitor stands at a real offset from its home");
    var far=h.subtract(outward.scale(3.8));p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),far.x,30,far.z,Set.<Relative>of(),0,0,false);
    check(p.position().distanceToSqr(h)<=16&&mob.distanceToSqr(p)>16&&mob.distanceToSqr(p)<64,"Real eligible guest is inside home radius but outside animal warning reach");
    check(mob.hasLineOfSight(p),"Out-of-range guest has genuine unobstructed visibility");meal=mob.mealReady();attack=mob.attackReady();rest=mob.restUntil();
   });
   c.waitTicks(12);
   w.getServer().runOnServer(s->{
    check(mob.getTarget()==null&&mob.pose()==CinderBailiff.IDLE,"Home selection refuses an out-of-warning-reach guest instead of repeatedly retaining it");
    check(mob.mealReady()==meal&&mob.attackReady()==attack&&mob.restUntil()==rest,"Out-of-range territorial guest renews no saved deadline");
    var p=s.getPlayerList().getPlayers().getFirst();var toward=Vec3.atCenterOf(mob.home()).subtract(mob.position()).multiply(1,0,1).normalize();
    var near=mob.position().add(toward.scale(2));p.teleportTo(s.overworld(),near.x,30,near.z,Set.<Relative>of(),0,0,false);
    check(mob.distanceToSqr(p)<16&&p.position().distanceToSqr(Vec3.atCenterOf(mob.home()))<=16,"Actual guest approaches inside both territorial and warning reach");
   });
   boolean warning=false;for(int n=0;n<6;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->mob.pose()==CinderBailiff.WARNING)){warning=true;break;}}
   check(warning,"Eligible close guest produces a genuine native territorial warning");
   w.getServer().runOnServer(s->{
    var p=s.getPlayerList().getPlayers().getFirst();direction=p.position().subtract(mob.position()).multiply(1,0,1).normalize();
    check(mob.inLane(mob.position().add(direction.scale(2)),1),"Actual warning locks its original central direction");attack=mob.attackReady();meal=mob.mealReady();rest=mob.restUntil();
    var side=new Vec3(-direction.z,0,direction.x);var retreat=mob.position().add(side.scale(5.5));
    p.teleportTo(s.overworld(),retreat.x,30,retreat.z,Set.<Relative>of(),0,0,false);
    check(mob.distanceToSqr(p)>16&&mob.distanceToSqr(p)<64,"Actual committed-warning retreat remains inside existing validity radius");
   });
   c.waitTicks(2);
   w.getServer().runOnServer(s->{
    check(mob.pose()==CinderBailiff.WARNING&&mob.getTarget()==s.getPlayerList().getPlayers().getFirst(),"Idle target release does not cancel a committed real warning");
    check(mob.mealReady()==meal&&mob.attackReady()==attack&&mob.restUntil()==rest,"Retreat during commitment creates no extra saved deadline");
    check(mob.inLane(mob.position().add(direction.scale(2)),1),"Committed original lane remains fixed after retreat");
    var side=new Vec3(-direction.z,0,direction.x);check(!mob.inLane(mob.position().add(side.scale(2)),1),"Fan does not rotate its central lane to track retreating player");
   });
  }
 }

 private void spawn(MinecraftServer s){mob=EmberContent.BAILIFF.create(s.overworld(),EntitySpawnReason.COMMAND);mob.snapTo(.5,30,.5,0,0);s.overworld().addFreshEntity(mob);}
 private void arena(MinecraftServer s){var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=30;y<=34;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(l,7,30,7,Set.<Relative>of(),0,0,false);}
 private void configure(TestServerContext server){server.runCommand("difficulty normal");server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule random_tick_speed 0");server.runCommand("gamerule natural_health_regeneration false");server.runCommand("time set 18000");}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
