package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.client.fx.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Supplied dry-bank equipment scene; route uses actual owner packets and warning uses real crab AI. */
public final class TidewardPresentationTest implements FabricClientGameTest {
 private static final BlockPos A=new BlockPos(0,100,0),B=new BlockPos(4,100,0);
 private ReedbackCrab crab;
 @Override public void runTest(ClientGameTestContext c){
  try(var settings=new TidewardNative(c)){
   c.runOnClient(mc->{mc.getWindow().setWindowed(1920,1080);mc.resizeGui();clean(mc);});
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))try(var w=c.worldBuilder().create()){
    String prefix="tideward_"+quality.name().toLowerCase(Locale.ROOT);
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("difficulty normal");
    w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
     p.setItemSlot(EquipmentSlot.FEET,new ItemStack(TidewardEquipment.WADERS));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TidewardEquipment.SPECTACLES));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardSurvey.ITEM));
     p.teleportTo(p.level(),2.5,101,3.5,Set.<Relative>of(),180,0,false);
    });c.waitTicks(5);
    c.runOnClient(mc->{MagicQuality.own=quality;MagicQuality.others=MagicQuality.Level.MINIMAL;MagicQuality.reducedFlash=false;clean(mc);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
     check(mc.player.getItemBySlot(EquipmentSlot.FEET).is(TidewardEquipment.WADERS)&&mc.player.getItemBySlot(EquipmentSlot.HEAD).is(TidewardEquipment.SPECTACLES)&&mc.player.getMainHandItem().is(TidewardSurvey.ITEM),"Actual connected worn feet/head and held forked spool are present in dry-bank scene");
    });c.waitTicks(3);shot(c,prefix+"_supplied_gear_dry_front");
    c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);look(mc,new Vec3(2.5,100.99,.5));clean(mc);});c.waitTicks(2);c.runOnClient(mc->{if(mc.gui.hud.isHidden())mc.gui.hud.toggle();check(!mc.gui.hud.isHidden()&&mc.options.getCameraType()==CameraType.FIRST_PERSON&&mc.player.getMainHandItem().is(TidewardSurvey.ITEM),"Actual first-person native hand rendering requires visible HUD and the real held spool");});shot(c,prefix+"_supplied_forked_spool_hand");c.runOnClient(TidewardPresentationTest::clean);
    long routeBefore=c.computeOnClient(mc->TidewardClient.routeAccepted());long burstBefore=c.computeOnClient(mc->TidewardClient.routeBursts());
    use(c,A);use(c,B);
    c.waitFor(mc->TidewardClient.routeAccepted()==routeBefore+1&&!pieces(mc,false).isEmpty(),40);
    w.getServer().runOnServer(s->{var p=player(s);check(p.getMainHandItem().getDamageValue()==2,"Two ordinary real bank-use packets pay exactly two spool wear once");check(p.getAttachedOrElse(TidewardSurvey.READY,0L)>TidewardEquipment.clock(p),"Real route packet starts actual finite shared rest");});
    c.waitTicks(1);
    paired(c,prefix+"_actual_route_braid",false,quality==MagicQuality.Level.MINIMAL?10:26);
    // Keep the real held spool and route unchanged until its existing five-second receiver deadline.
    c.waitFor(mc->field(null,TidewardClient.class,"route")==null,110);
    check(c.computeOnClient(mc->TidewardClient.routeBursts())<=burstBefore+10,"Unchanged genuine five-second route cannot renew beyond its finite scheduled bursts");
    c.waitTicks(13);c.runOnClient(mc->check(pieces(mc,false).isEmpty(),"Expired route leaves no live authored braid/peg material"));shot(c,prefix+"_actual_route_expired");
    w.getServer().runOnServer(s->{var p=player(s);p.teleportTo(p.level(),.5,101,.5,Set.<Relative>of(),0,26,false);});c.waitTicks(3);
    c.runOnClient(mc->{clean(mc);mc.options.keyShift.setDown(true);mc.particleEngine.clearParticles();});c.waitTicks(3);
    long warningBefore=c.computeOnClient(mc->TidewardClient.warningAccepted());
    w.getServer().runOnServer(s->{crab=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);crab.snapTo(.5,101,2.5,180,0);crab.setPersistenceRequired();s.overworld().addFreshEntity(crab);crab.setTarget(player(s));});
    await(c,w,s->crab.pose()==ReedbackCrab.WARNING,30,"Actual living territorial AI starts ordinary forty-tick warning");
    c.waitFor(mc->TidewardClient.warningAccepted()==warningBefore+1&&!pieces(mc,true).isEmpty(),25);
    w.getServer().runOnServer(s->{check(crab.isAlive()&&crab.pose()==ReedbackCrab.WARNING,"Actual cue is observed while original living warning remains active");check(player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1&&player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)>TidewardEquipment.clock(player(s)),"Real crouched spectacle observation spends one worn lens and actual saved rest");});
    c.waitTicks(1);
    paired(c,prefix+"_actual_spectacles_crab_warning",true,quality==MagicQuality.Level.MINIMAL?2:3);
    c.runOnClient(mc->mc.options.keyShift.setDown(false));w.getServer().runOnServer(s->crab.discard());
   }
  }
 }
 private static void use(ClientGameTestContext c,BlockPos at){c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at).add(0,.49,0),Direction.UP,at,false)));c.waitTicks(2);}
 private static void clean(Minecraft mc){mc.gui.setScreen(null);mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();}
 private static void look(Minecraft mc,Vec3 at){var d=at.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));}
 private static void paired(ClientGameTestContext c,String name,boolean warning,int minimum){
  var done=c.computeOnClient(mc->{clean(mc);var live=pieces(mc,warning);check(live.size()>=minimum,"Actual owner-delivered material count respects chosen Full/Minimal contract: "+name+" count="+live.size());
   int visible=0;for(var p:live){var at=new Vec3(number(p,"x"),number(p,"y"),number(p,"z"));var direction=at.subtract(mc.player.getEyePosition());
    check(direction.lengthSqr()<64,"Actual authored physical material remains within eight-block review camera reach");
    if(direction.normalize().dot(mc.player.getLookAngle())>.75)visible++;
    if(!warning)check(at.y>=101.08&&at.y<=101.25&&at.x>=.3&&at.x<=4.7&&Math.abs(at.z-.5)<.2,"Actual dry-bank braid/peg positions lie on paid five-cell route, without magic circles");
   }check(visible>=2,"Actual retained physical materials project inside review view cone");
   var drawn=pixels(mc,name);mc.particleEngine.clearParticles();var background=pixels(mc,name+"_background");
   return drawn.thenCombine(background,(a,b)->{check(a.length==b.length,"Matched actual framebuffer dimensions");int changed=0;for(int i=0;i<a.length;i++){int x=a[i],y=b[i];int d=Math.abs((x>>16&255)-(y>>16&255))+Math.abs((x>>8&255)-(y>>8&255))+Math.abs((x&255)-(y&255));if(d>20)changed++;}check(changed>10,"Actual owner-authored material changes native pixels: "+name+" changed="+changed);return (Void)null;});
  });c.waitFor(mc->done.isDone());done.join();
 }
 private static List<Particle> pieces(Minecraft mc,boolean warning){return particles(mc.particleEngine).stream().filter(p->p.isAlive()&&p.getClass().getName().equals("dev.wildercord.client.fx.TidewardClient$Piece")&&(field(p,p.getClass(),"following")!=null)==warning).toList();}
 private static double number(Particle p,String name){return ((Number)field(p,Particle.class,name)).doubleValue();}
 private static Object field(Object o,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static List<Particle> particles(ParticleEngine e){var out=new ArrayList<Particle>();for(var group:((Map<?,?>)field(e,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);return out;}
 private static CompletableFuture<int[]> pixels(Minecraft mc,String name){var result=new CompletableFuture<int[]>();mc.gameRenderer.update(DeltaTracker.ONE);mc.gameRenderer.extract(DeltaTracker.ONE,true);mc.gameRenderer.render();com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var path=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(path.getParent());image.writeToFile(path);result.complete(image.getPixels());}catch(Throwable e){result.completeExceptionally(e);}});return result;}
}
