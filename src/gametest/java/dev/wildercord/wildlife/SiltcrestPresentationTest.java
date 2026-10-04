package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.SiltcrestNative.*;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Actual unpaused model poses. Full/Minimal spell settings do not substitute for this creature's own rig. */
public final class SiltcrestPresentationTest implements FabricClientGameTest {
 private SiltcrestBittern source;private int entityId;
 public void runTest(ClientGameTestContext c){var camera=c.computeOnClient(mc->mc.options.getCameraType());boolean hud=c.computeOnClient(mc->mc.gui.hud.isHidden());int[] size=c.computeOnClient(mc->new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});var own=MagicQuality.own;var others=MagicQuality.others;boolean flash=MagicQuality.reducedFlash;
  try{
   c.runOnClient(mc->{mc.getWindow().setWindowed(1920,1080);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);clean(mc);});
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))try(var w=c.worldBuilder().create()){
    String prefix="siltcrest_"+quality.name().toLowerCase(Locale.ROOT);c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
    w.getServer().runOnServer(s->{var l=s.overworld();floor(l);for(var light:List.of(new BlockPos(0,100,-2),new BlockPos(5,100,2),new BlockPos(-3,100,-2)))l.setBlock(light,Blocks.SEA_LANTERN.defaultBlockState(),2);var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(l,.5,101,-2,Set.<Relative>of(),0,0,false);for(int i=0;i<3;i++)fish(l,i);source=bird(l,.5,.5);entityId=source.getId();check(BitternHabitat.bank(l,source.blockPosition())&&l.getFluidState(source.blockPosition()).isEmpty(),"Actual initial dry supported bank is adjacent to the supplied pond");});
    c.runOnClient(mc->{MagicQuality.own=quality;MagicQuality.others=quality;MagicQuality.reducedFlash=false;clean(mc);});
    phase(c,w,SiltcrestBittern.COILING,"Actual ordinary dry-bank hunt reaches real coiling",500);var coilFrame=capture(c,prefix+"_actual_coil",SiltcrestBittern.COILING);
    phase(c,w,SiltcrestBittern.STRIKING,"Actual admitted wild fish strike reaches its original six-tick phase",80);var strikeFrame=capture(c,prefix+"_actual_strike",SiltcrestBittern.STRIKING);
    phase(c,w,SiltcrestBittern.PREENING,"Original six-tick strike completes into actual preening",25);var preenFrame=capture(c,prefix+"_actual_preen",SiltcrestBittern.PREENING);
    long meal=w.getServer().computeOnServer(s->{check(source.isAlive()&&!source.isRemoved()&&source.huntReady()>SiltcrestBittern.clock(s.overworld())+SiltcrestBittern.APPETITE-100,"Real admitted fish death earns original finite appetite");check(source.preyPool(s.overworld()).size()>=2,"Real hunt leaves at least two live eligible fish");return source.huntReady();});
    // Drain asynchronous GPU readback/PNG work only after the original short live phases.
    finish(c,CompletableFuture.allOf(coilFrame,strikeFrame,preenFrame));
    w.getServer().runOnServer(s->{var l=s.overworld();l.setBlock(new BlockPos(-2,103,-2),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);l.setBlock(new BlockPos(-1,100,-2),Blocks.WATER.defaultBlockState(),2);});w.getServer().runCommand("time set 6000");
    phase(c,w,SiltcrestBittern.SHELTERING,"Actual daytime shelter navigation reaches supplied covered dry bank",420);
    w.getServer().runOnServer(s->check(source.onGround()&&source.shelter()!=null&&BitternHabitat.shelter(s.overworld(),source.shelter())&&source.distanceToSqr(source.shelter().getX()+.5,source.shelter().getY(),source.shelter().getZ()+.5)<=.36&&source.huntReady()==meal,"Actual grounded canopy rest retains earned appetite"));finish(c,capture(c,prefix+"_actual_shelter",SiltcrestBittern.SHELTERING));
   }
  }finally{c.runOnClient(mc->{MagicQuality.own=own;MagicQuality.others=others;MagicQuality.reducedFlash=flash;mc.options.setCameraType(camera);if(mc.gui.hud.isHidden()!=hud)mc.gui.hud.toggle();mc.getWindow().setWindowed(size[0],size[1]);mc.resizeGui();});}
 }
 private void phase(ClientGameTestContext c,TestSingleplayerContext w,int pose,String why,int ticks){try{c.waitFor(mc->{var e=mc.level==null?null:mc.level.getEntity(entityId);if(!(e instanceof SiltcrestBittern b)||!b.isAlive()||b.pose()!=pose)return false;return switch(pose){case SiltcrestBittern.COILING->b.coil>=.3F;case SiltcrestBittern.STRIKING->b.strike>=.5F;case SiltcrestBittern.PREENING->b.preen>=.3F;case SiltcrestBittern.SHELTERING->b.rest>=.3F;default->false;};},ticks);}catch(AssertionError failure){String client=c.computeOnClient(mc->{var e=mc.level==null?null:mc.level.getEntity(entityId);return e instanceof SiltcrestBittern b?"clientBody="+b.position()+" pose="+b.pose()+" phase="+b.phase()+" alive="+b.isAlive()+" coil="+b.coil+" strike="+b.strike+" preen="+b.preen+" rest="+b.rest:"trackedClientMissing";});String server=w.getServer().computeOnServer(s->"serverBody="+source.position()+" pose="+source.pose()+" phase="+source.phase()+" alive="+source.isAlive()+" removed="+source.isRemoved()+" ground="+source.onGround()+" water="+source.isInWater()+" pool="+source.preyPool(s.overworld()).size()+" huntReady="+source.huntReady()+" clock="+SiltcrestBittern.clock(s.overworld()));throw new AssertionError(why+" unchangedWindow="+ticks+" "+client+" "+server,failure);}}
 private CompletableFuture<Void> capture(ClientGameTestContext c,String name,int pose){var done=c.computeOnClient(mc->{var e=mc.level.getEntity(entityId);check(e instanceof SiltcrestBittern,"Original tracked creature owns native capture");var b=(SiltcrestBittern)e;check(b.isAlive()&&!b.isRemoved()&&!b.isInvisible()&&b.pose()==pose,"Actual current living model phase owns paired frame: "+name);clean(mc);
   var target=b.getBoundingBox().getCenter();var d=target.subtract(mc.player.getEyePosition());check(d.lengthSqr()<64,"Actual review camera remains within eight blocks of authored rig");mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
   int width=mc.getWindow().getWidth(),height=mc.getWindow().getHeight();double distance=d.length();int rx=Math.max(16,(int)(width*.15/distance)),ry=Math.max(16,(int)(height*.25/distance));var drawn=pixels(mc,name);CompletableFuture<int[]> background;
   // Client-only visibility is a matched model-render control. No server position, health,
   // AI, phase, pose, animation age or source clock is changed; restore the exact flag immediately.
   boolean invisible=b.isInvisible();try{b.setInvisible(true);background=pixels(mc,name+"_background");}finally{b.setInvisible(invisible);}
   return drawn.thenCombine(background,(a,z)->{check(a.length==z.length&&a.length==width*height,"Matched native rig framebuffer dimensions");int changed=0;for(int py=Math.max(0,height/2-ry);py<Math.min(height,height/2+ry);py++)for(int px=Math.max(0,width/2-rx);px<Math.min(width,width/2+rx);px++){int i=py*width+px,x=a[i],y=z[i];int delta=Math.abs((x>>16&255)-(y>>16&255))+Math.abs((x>>8&255)-(y>>8&255))+Math.abs((x&255)-(y&255));if(delta>20)changed++;}check(changed>100,"Actual authored torso-centered model region changes visible native pixels: "+name+" changed="+changed);return (Void)null;});
  });return done;}
 private static void finish(ClientGameTestContext c,CompletableFuture<Void> done){c.waitFor(mc->done.isDone());done.join();}
 private static void clean(Minecraft mc){mc.gui.setScreen(null);mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();}
 private static CompletableFuture<int[]> pixels(Minecraft mc,String name){var result=new CompletableFuture<int[]>();mc.gameRenderer.update(DeltaTracker.ONE);mc.gameRenderer.extract(DeltaTracker.ONE,true);mc.gameRenderer.render();com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var p=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(p.getParent());image.writeToFile(p);result.complete(image.getPixels());}catch(Throwable error){result.completeExceptionally(error);}});return result;}
}
