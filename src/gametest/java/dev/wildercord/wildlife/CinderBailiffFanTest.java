package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
/** Real retaliation owns Warning/Fan; finite physical health outcome and locked geometry. */
public final class CinderBailiffFanTest implements FabricClientGameTest {
 private CinderBailiff mob;private Zombie target;
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");w.getServer().runCommand("time set 18000");
  w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlock(new BlockPos(x,29,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=30;y<34;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE);mob=EmberContent.BAILIFF.create(l,EntitySpawnReason.COMMAND);mob.snapTo(.5,30,.5,0,0);l.addFreshEntity(mob);target=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);target.setNoAi(true);target.getAttribute(Attributes.ARMOR).setBaseValue(0);target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);target.snapTo(.5,30,2.5,0,0);l.addFreshEntity(target);check(mob.hurtServer(l,target.damageSources().mobAttack(target),1),"Genuine creature attack primes retaliation");diagnostic(s,"initial actual damage");});
  boolean warning=false;for(int n=0;n<20;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->{diagnostic(s,"bounded warning poll");return mob.pose()==CinderBailiff.WARNING;})){warning=true;break;}}if(!warning)w.getServer().runOnServer(s->diagnostic(s,"strict40tick warning refusal"));check(warning,"Actual retaliation starts a real fixed fan warning");
  boolean recovery=false;for(int n=0;n<40;n++){c.waitTicks(2);if(w.getServer().computeOnServer(s->mob.pose()==CinderBailiff.RECOVERING)){recovery=true;break;}}check(recovery,"Finite three-vent action reaches recovery");
  w.getServer().runOnServer(s->{check(Math.abs(target.getHealth()-16)<.001,"One actual unarmored target receives exactly4 physical health across all vents");check(mob.attackReady()>s.overworld().getGameTime(),"Finite attack rest prevents immediate repeat");check(!mob.isOnFire()&&!target.isOnFire(),"Physical ash fan adds no persistent burn");for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)check(!s.overworld().getBlockState(new BlockPos(x,30,z)).is(Blocks.FIRE),"Fan never ignites terrain");});
  c.waitTicks(45);w.getServer().runOnServer(s->check(target.getHealth()==16,"Recovery adds no DoT or repeated target harm"));
  w.getServer().runCommand("difficulty peaceful");c.waitTicks(25);w.getServer().runOnServer(s->check(s.overworld().getEntity(mob.getUUID())==mob && mob.isAlive() && mob.pose()!=CinderBailiff.WARNING && mob.pose()!=CinderBailiff.FANNING,"Actual existing neutral-taxonomy woodland Bailiff survives Peaceful with aggression cancelled"));
 }}
 /** At most initial+20 original polls+one failure snapshot; no added waits or gameplay writes. */
 private void diagnostic(net.minecraft.server.MinecraftServer s,String phase){
  var stored=mob.getTarget();dev.wildercord.Wildercord.LOGGER.info("EMBER_FAN_START phase={} time={} actorTick={} pose={} actor={} motion={} grounded={} alive={} removed={} noAI={} actorHealth={} expectedTarget={} expectedTargetHealth={} expectedTargetAlive={} expectedTargetRemoved={} storedTarget={} storedMatchesExpected={} distanceSquared={} lineOfSight={} attackReady={} mealReady={} restUntil={} home={} navigationEnd={}",phase,s.overworld().getGameTime(),mob.tickCount,mob.pose(),mob.position(),mob.getDeltaMovement(),mob.onGround(),mob.isAlive(),mob.isRemoved(),mob.isNoAi(),mob.getHealth(),target.position(),target.getHealth(),target.isAlive(),target.isRemoved(),stored==null?"none":stored.getUUID(),stored==target,mob.distanceToSqr(target),mob.hasLineOfSight(target),mob.attackReady(),mob.mealReady(),mob.restUntil(),mob.home(),mob.getNavigation().getPath()==null?"none":mob.getNavigation().getPath().getEndNode());
 }
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
