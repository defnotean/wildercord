package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import dev.wildercord.client.fx.MagicQuality;
import java.util.*;

/** Initial habitat placement is supplied; movement, wear, observations and use are real native events. */
final class TidewardNative implements AutoCloseable {
 final ClientGameTestContext c;final CameraType camera;final boolean hidden;final boolean[] keys;final int[] window;final int scale;final MagicQuality.Level own,others;final boolean reduced;
 TidewardNative(ClientGameTestContext c){this.c=c;camera=c.computeOnClient(mc -> mc.options.getCameraType());hidden=c.computeOnClient(mc -> mc.gui.hud.isHidden());
  keys=c.computeOnClient(mc -> {var k=keys(mc);var out=new boolean[k.length];for(int i=0;i<k.length;i++)out[i]=k[i].isDown();return out;});
  window=c.computeOnClient(mc -> new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});scale=c.computeOnClient(mc -> mc.options.guiScale().get());own=MagicQuality.own;others=MagicQuality.others;reduced=MagicQuality.reducedFlash;
  c.runOnClient(mc -> {for(var k:keys(mc))k.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);if(mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.getWindow().setWindowed(1024,640);mc.options.guiScale().set(2);mc.resizeGui();});
 }
 private static KeyMapping[] keys(Minecraft mc){return new KeyMapping[]{mc.options.keyUp,mc.options.keyDown,mc.options.keyLeft,mc.options.keyRight,mc.options.keyShift,mc.options.keyJump,mc.options.keyUse,mc.options.keyAttack,mc.options.keySprint};}
 static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 static void place(ServerPlayer p,double x,double y,double z){p.teleportTo(p.level(),x,y,z,Set.<Relative>of(),0,10,false);}
 static void floor(ServerLevel l){for(int x=-5;x<=5;x++)for(int z=-5;z<=24;z++)for(int y=100;y<=104;y++)l.setBlock(new BlockPos(x,y,z),y==100?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
 static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<MinecraftServer> predicate,int ticks,String why){for(int i=0;i<ticks;i++){c.waitTicks(1);if(w.getServer().computeOnServer(predicate::test))return;}throw new AssertionError(why);}
 static void shot(ClientGameTestContext c,String name){c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 @Override public void close(){c.runOnClient(mc -> {var k=keys(mc);for(int i=0;i<k.length;i++)k[i].setDown(keys[i]);mc.options.setCameraType(camera);if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.gui.setScreen(null);mc.getWindow().setWindowed(window[0],window[1]);mc.options.guiScale().set(scale);mc.resizeGui();MagicQuality.own=own;MagicQuality.others=others;MagicQuality.reducedFlash=reduced;});}
}
