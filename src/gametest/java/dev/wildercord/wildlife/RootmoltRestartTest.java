package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Actual live contact before full shutdown; control is transient while attack/rest clocks remain saved. */
public final class RootmoltRestartTest implements FabricClientGameTest {
 private RootmoltStrider root;private SporebackSnail visible,blocked,distant;
 public void runTest(ClientGameTestContext c) {
  TestWorldSave saved;UUID id,snailId;long deadline,threat,hidden;long[] economy;boolean dew;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.STONE.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),y==33?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,2,30,.5,Set.<Relative>of(),90,0,false);root=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);root.snapTo(0,30,.5,0,0);l.addFreshEntity(root);root.setTarget(p);visible=snail(l,0,2);blocked=snail(l,0,-2);distant=snail(l,6,.5);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(0,y,-1),Blocks.STONE.defaultBlockState(),3);});
   long[] originalEconomy=w.getServer().computeOnServer(s -> clocks(visible));
   await(c,w,s -> root.pose()==RootmoltStrider.WARNING && visible.threatReady()>0,"Actual WARNING proximity and line of sight retract the nearby Sporeback");
   w.getServer().runOnServer(s -> {check(visible.pose()==2 && visible.hiddenUntil()>s.overworld().getGameTime(),"Actual physical alarm creates a finite hide");check(blocked.threatReady()==0 && distant.threatReady()==0,"Blocked and out-of-range snails refuse the actual alarm");check(Arrays.equals(originalEconomy,clocks(visible)) && !visible.dew(),"Physical danger changes no gather/forage/magic-response/nursery clock or dew reserve");long ready=visible.threatReady(),hide=visible.hiddenUntil();RootmoltContent.alarm(root);RootmoltContent.alarm(root);check(!visible.answerThreat() && visible.threatReady()==ready && visible.hiddenUntil()==hide,"Repeated actual warning and hidden response cannot renew physical clocks");});
   boolean grabbed=false;for(int i=0;i<100;i++){c.waitTicks(1);if(w.getServer().computeOnServer(s -> root.holding(s.getPlayerList().getPlayers().getFirst()))){grabbed=true;break;}}check(grabbed,"Actual living source owns control before shutdown");id=root.getUUID();deadline=root.attackReady();snailId=visible.getUUID();threat=visible.threatReady();hidden=visible.hiddenUntil();economy=w.getServer().computeOnServer(s -> clocks(visible));dew=visible.dew();saved=w.getWorldSave();
  }
  try(var w=saved.open()) {c.waitTicks(3);w.getServer().runOnServer(s -> {var source=(RootmoltStrider)s.overworld().getEntity(id);var p=s.getPlayerList().getPlayers().getFirst();check(source!=null && source.attackReady()==deadline,"Exact saved attack rest survives full shutdown");check(source.pose()!=RootmoltStrider.HOLDING && !p.hasEffect(RootmoltContent.TETHER) && p.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Saved player effects cannot restore a transient source grab after restart");
    visible=(SporebackSnail)s.overworld().getEntity(snailId);check(visible!=null && visible.threatReady()==threat && visible.hiddenUntil()==hidden,"Full reopen preserves exact physical threat and hide timestamps");check(Arrays.equals(economy,clocks(visible)) && visible.dew()==dew,"Restart preserves independent reserve clocks and dew");check(!visible.answerThreat() && visible.threatReady()==threat,"Saved hidden deadline refuses even before the first active AI pose update");source.discard();RootmoltContent.alarm(source);check(visible.threatReady()==threat && visible.hiddenUntil()==hidden,"Removed source cannot renew its former physical alarm");visible.setNoAi(false);visible.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);});c.waitTicks(3);
   w.getServer().runOnServer(s -> check(visible.pose()==2,"Ordinary resumed AI respects the saved hide"));
   await(c,w,s -> s.overworld().getGameTime()>hidden && visible.pose()!=2,"Physical hide expires through normal elapsed AI ticks after source removal");
   w.getServer().runOnServer(s -> {check(s.overworld().getGameTime()<threat && !visible.answerThreat(),"Independent threat rest still refuses after the shorter hide expires");check(visible.threatReady()==threat && visible.hiddenUntil()==hidden && Arrays.equals(economy,clocks(visible)) && visible.dew()==dew,"Expiry/refusal does not renew deadlines or mint resources");});
  }finally{root=null;visible=null;blocked=null;distant=null;}

 }
 private static SporebackSnail snail(net.minecraft.server.level.ServerLevel l,double x,double z){var snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);check(snail!=null,"Actual Sporeback fixture");snail.setNoAi(true);snail.setPersistenceRequired();snail.snapTo(x,30,z,0,0);l.addFreshEntity(snail);return snail;}
 private static long[] clocks(SporebackSnail snail){return new long[]{snail.gatherReady(),snail.forageReady(),snail.responseReady(),snail.nurseryReady()};}
 private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<net.minecraft.server.MinecraftServer> yes,String why){for(int i=0;i<240;i++){c.waitTicks(1);if(w.getServer().computeOnServer(yes::test))return;}throw new AssertionError(why);}
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
