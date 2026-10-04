package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
/** Draft native competing-consumer, finite meal and full-restart verification; no timer or meal calls. */
public final class RootmoltEcologyTest implements FabricClientGameTest {
 private RootmoltStrider a,b;private final BlockPos cap=new BlockPos(2,30,0);
 public void runTest(ClientGameTestContext c) {
  UUID saved;long meal;TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}l.setBlock(cap.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(cap,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE);s.getPlayerList().getPlayers().getFirst().teleportTo(l,0,30,3,java.util.Set.<net.minecraft.world.entity.Relative>of(),0,0,false);a=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);a.snapTo(-.5,30,.5,0,0);l.addFreshEntity(a);b=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);b.snapTo(4.5,30,.5,0,0);l.addFreshEntity(b);});
   boolean consumed=false;for(int i=0;i<150;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> s.overworld().getBlockState(cap).getValue(GlowcapBlock.AGE)==0)) {consumed=true;break;}}
   check(consumed,"Actual competing consumers navigate and complete one uninterrupted meal");
   check(w.getServer().computeOnServer(s -> (a.mealReady()>0)!=(b.mealReady()>0)),"Exactly one consumer spends the mature cap; a competing visitor cannot double-spend it");
   var winner=w.getServer().computeOnServer(s -> a.mealReady()>0?a:b);saved=winner.getUUID();meal=winner.mealReady();
   w.getServer().runOnServer(s -> {check(s.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(cap).inflate(8)).isEmpty(),"Consumption creates no material reward or drops");(winner==a?b:a).discard();s.overworld().setBlock(cap,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);});
   c.waitTicks(180);check(w.getServer().computeOnServer(s -> winner.mealReady()==meal && s.overworld().getBlockState(cap).getValue(GlowcapBlock.AGE)==2),"Another ripe cap cannot shorten or renew the saved meal rest");save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(30);w.getServer().runOnServer(s -> {var winner=(RootmoltStrider)s.overworld().getEntity(saved);check(winner!=null && winner.mealReady()==meal,"Full restart preserves the exact finite meal deadline");check(s.overworld().getBlockState(cap).getValue(GlowcapBlock.AGE)==2,"A restart cannot grant an early second meal");});}
 }
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
