package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
/** Actual native lateral input during a committed warning; no post-warning teleport or pose edits. */
public final class RootmoltSidestepTest implements FabricClientGameTest {
 private RootmoltStrider root;
 public void runTest(ClientGameTestContext c) {
  boolean previousLeft=c.computeOnClient(mc -> mc.options.keyLeft.isDown());
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.STONE.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),y==33?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,2,30,.5,Set.<Relative>of(),90,0,false);p.setHealth(20);root=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);root.snapTo(0,30,.5,0,0);l.addFreshEntity(root);root.setTarget(p);});
   boolean warning=false;for(int i=0;i<60;i++){c.waitTicks(1);if(w.getServer().computeOnServer(s -> root.pose()==RootmoltStrider.WARNING)){warning=true;break;}}check(warning,"Real AI raises a committed warning");
   double originZ=w.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getZ());
   c.runOnClient(mc -> mc.options.keyLeft.setDown(true));c.waitTicks(24);c.runOnClient(mc -> mc.options.keyLeft.setDown(previousLeft));c.waitTicks(15);
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();check(Math.abs(p.getZ()-originZ)>.8,"Native lateral input physically clears the committed line");check(p.getHealth()==20 && !p.hasEffect(RootmoltContent.TETHER),"Side-stepping prevents both physical contact and root hold");check(root.pose()==RootmoltStrider.RECOVERING && root.attackReady()>lTime(s),"The missed single rake still owes recovery/rest");});
   c.takeScreenshot(TestScreenshotOptions.of("rootmolt_native_sidestep").disableCounterPrefix());
  }finally {c.runOnClient(mc -> mc.options.keyLeft.setDown(previousLeft));}
 }
 private static long lTime(net.minecraft.server.MinecraftServer s){return s.overworld().getGameTime();}
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
