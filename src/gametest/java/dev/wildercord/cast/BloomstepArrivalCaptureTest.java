package dev.wildercord.cast;

import dev.wildercord.client.fx.LifeParticle;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.LifeOption;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.particle.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Connected Survival player, paid production Bolt/Bloomstep and actual arrival materials.
 * Neither the event, teleport, client position nor a replacement actor is fabricated.
 */
public final class BloomstepArrivalCaptureTest implements FabricClientGameTest {
 private volatile LifeOwnerEvents.Event arrival;
 private long observedAt;
 private UUID connected;
 private Vec3 departure;
 @Override public void runTest(ClientGameTestContext c){
  var camera=c.computeOnClient(mc->mc.options.getCameraType());
  var own=c.computeOnClient(mc->MagicQuality.own);var others=c.computeOnClient(mc->MagicQuality.others);
  boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  try{
   int full=scenario(c,false);int minimal=scenario(c,true);
   check(full>minimal,"Actual same-source own Full keeps more arrival petals than own Minimal with opposing others preference: "+full+"/"+minimal);
  }finally{
   LifeOwnerEvents.clear();arrival=null;connected=null;departure=null;
   c.runOnClient(mc->{mc.options.setCameraType(camera);MagicQuality.own=own;MagicQuality.others=others;
    if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});
  }
 }
 private int scenario(ClientGameTestContext c,boolean minimal){
  arrival=null;
  try(var w=c.worldBuilder().create()){
   Entity[] previousCamera={null};
   try{
   c.waitTicks(30);var server=w.getServer();server.runCommand("gamerule spawn_mobs false");
   server.runCommand("gamerule natural_health_regeneration false");
   server.runCommand("fill -10 100 -5 10 100 22 polished_deepslate");
   server.runCommand("fill -4 101 14 4 106 14 stone");
   server.runOnServer(s->{
    ServerPlayer p=s.getPlayerList().getPlayers().getFirst();NextSignatureNative.teach(p);
    NextSignatureNative.pose(p,.5,.5,0);connected=p.getUUID();departure=p.position();
    check(p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Actual connected caster stays Survival");
    LifeOwnerEvents.observe(e->{if(e.rune().equals("bloomstep")&&e.moment()==LifeOwnerEvents.Moment.APPLY){
     check(e.recipient().equals(connected)&&e.source().equals(connected),"Actual connected caster owns arrival, not observer/FakePlayer");
     check(e.secondary()!=null&&e.secondary().distanceTo(departure)<.01,"Actual outcome records original departure");
     check(e.anchor().distanceTo(p.position().add(0,.42,0))<.001,"Actual paid arrival anchors material at caster's live feet");
     arrival=e;observedAt=s.overworld().getGameTime();
    }});
   });c.waitTicks(5);
   c.runOnClient(mc->{check(mc.player!=null&&mc.player.getUUID().equals(connected),"The rendered client is the actual connected caster");
    previousCamera[0]=mc.getCameraEntity();mc.setCameraEntity(mc.player);
    MagicQuality.own=minimal?MagicQuality.Level.MINIMAL:MagicQuality.Level.FULL;
    MagicQuality.others=minimal?MagicQuality.Level.FULL:MagicQuality.Level.MINIMAL;
    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();
    mc.particleEngine.clearParticles();
   });
   server.runOnServer(s->NextSignatureNative.cast(s.getPlayerList().getPlayers().getFirst(),Runes.BOLT,Runes.get("wildercord:bloomstep").orElseThrow()));
   for(int i=0;i<50&&arrival==null;i++)c.waitTicks(1);
   check(arrival!=null,"Actual paid Bolt reaches wall and produces bounded successful Bloomstep teleport");
   Vec3 actual=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
    check(p.getUUID().equals(connected)&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Same actual Survival player after genuine teleport");
    check(p.position().distanceTo(departure)>3&&p.position().distanceTo(departure)<=32,"Real safe destination obeys Bloomstep range");return p.position();
   });
   for(int i=0;i<3&&!c.computeOnClient(mc->mc.player!=null&&mc.player.position().distanceTo(actual)<.05);i++)c.waitTicks(1);
   // Native local-player extraction requires the camera entity to be that player.
   // Preserve the actual cast orientation; use the rear view when the wall blocks front.
   boolean rear=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
    boolean front=clearCameraRay(p,p.getEyePosition().add(p.getLookAngle().scale(4)),p.getEyePosition());
    if(!front)check(clearCameraRay(p,p.getEyePosition().subtract(p.getLookAngle().scale(4)),p.getEyePosition()),"Actual native rear camera ray is loaded and unobstructed");
    return !front;
   });
   c.runOnClient(mc->{check(mc.player.position().distanceTo(actual)<.05,"Client already received actual server teleport");
    mc.setCameraEntity(mc.player);mc.options.setCameraType(rear?CameraType.THIRD_PERSON_BACK:CameraType.THIRD_PERSON_FRONT);
   });
   c.waitTicks(2);
   server.runOnServer(s->check(s.overworld().getGameTime()-observedAt<=5,"Capture remains within real ten-tick material lifetime"));
   int count=c.computeOnClient(mc->{
    var camera=mc.gameRenderer.mainCamera();
    check(mc.player.getUUID().equals(connected)&&mc.getCameraEntity()==mc.player&&camera.entity()==mc.player&&camera.isDetached(),"Native detached camera renders the actual connected arriving player");
    var eye=mc.player.getEyePosition();var cameraEye=camera.position();
    check(cameraEye.distanceTo(eye)>2.5&&cameraEye.distanceTo(eye)<=4.2,"Actual native third-person camera has a useful bounded actor distance");
    var ray=mc.level.clip(new net.minecraft.world.level.ClipContext(cameraEye,eye,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
    check(ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS,"Actual rendered camera-to-player eye ray is unobstructed");
    check(mc.player.position().distanceTo(actual)<.05,"Rendered client remains at actual admitted server arrival");
    return footPetals(mc.particleEngine,actual);
   });
   check(count>0&&count<=24,"Actual live arrival-foot Life petals, excluding higher bolt/preparation bodies: "+count);
   c.takeScreenshot(TestScreenshotOptions.of("life_bloomstep_connected_"+(minimal?"minimal":"full")).disableCounterPrefix());
   return count;
   }finally{c.runOnClient(mc->mc.setCameraEntity(previousCamera[0]!=null?previousCamera[0]:mc.player));}
  }finally{LifeOwnerEvents.clear();arrival=null;}
 }
 private static boolean clearCameraRay(ServerPlayer p,Vec3 eye,Vec3 focus){
  for(int i=0;i<=16;i++)if(!p.level().isLoaded(net.minecraft.core.BlockPos.containing(eye.lerp(focus,i/16.))))return false;
  return p.level().clip(new net.minecraft.world.level.ClipContext(eye,focus,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
 }
 private static int footPetals(ParticleEngine engine,Vec3 feet){
  var found=new ArrayList<Particle>();
  for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))found.add((Particle)p);
  for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))found.add((Particle)p);
  int count=0;
  for(var p:found)if(p.isAlive()&&p instanceof LifeParticle){
   var material=(LifeOption)field(p,LifeParticle.class,"material");
   var at=new Vec3((double)field(p,Particle.class,"x"),(double)field(p,Particle.class,"y"),(double)field(p,Particle.class,"z"));
   if(material.style()==LifeOption.PETAL&&Math.abs(at.x-feet.x)<1&&Math.abs(at.z-feet.z)<1&&at.y>=feet.y-.1&&at.y<feet.y+.6)count++;
  }
  return count;
 }
 private static Object field(Object o,Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
