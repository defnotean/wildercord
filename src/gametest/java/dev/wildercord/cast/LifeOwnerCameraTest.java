package dev.wildercord.cast;

import dev.wildercord.client.fx.LifeParticle;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.LifeOption;
import dev.wildercord.spell.Runes;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Real paid connected-owner casts. Each view is the native player camera; no camera stand-in.
 * Extraction checks cover surviving particles during a view switch without re-emission.
 */
public final class LifeOwnerCameraTest implements FabricClientGameTest {
 private volatile LifeOwnerEvents.Event outcome;
 @Override public void runTest(ClientGameTestContext c){
  var camera=c.computeOnClient(mc->mc.options.getCameraType());
  var own=c.computeOnClient(mc->MagicQuality.own);var others=c.computeOnClient(mc->MagicQuality.others);
  boolean flash=c.computeOnClient(mc->MagicQuality.reducedFlash),hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  try{
   scenario(c,"first",CameraType.FIRST_PERSON,false,false,false,false);
   scenario(c,"front",CameraType.THIRD_PERSON_FRONT,false,false,false,true);
   scenario(c,"back",CameraType.THIRD_PERSON_BACK,false,false,false,false);
   scenario(c,"minimal",CameraType.THIRD_PERSON_FRONT,true,false,false,false);
   scenario(c,"reduced_flash",CameraType.THIRD_PERSON_FRONT,false,true,false,false);
   scenario(c,"clipped",CameraType.THIRD_PERSON_FRONT,false,false,true,false);
  }finally{
   LifeOwnerEvents.clear();outcome=null;
   c.runOnClient(mc->{mc.setCameraEntity(mc.player);mc.options.setCameraType(camera);
    MagicQuality.own=own;MagicQuality.others=others;MagicQuality.reducedFlash=flash;
    if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});
  }
 }
 private void scenario(ClientGameTestContext c,String name,CameraType view,boolean minimal,boolean reduced,boolean clipped,boolean transitions){
  outcome=null;
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);var server=w.getServer();UUID connected=c.computeOnClient(mc->mc.player.getUUID());
   server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_health_regeneration false");
   server.runCommand("fill -8 100 -8 8 100 8 polished_deepslate");
   if(clipped)server.runCommand("fill -3 101 1 3 105 1 stone");
   server.runOnServer(s->{var p=connected(s,connected);NextSignatureNative.teach(p);NextSignatureNative.pose(p,.5,.5,0);p.setHealth(4);
    LifeOwnerEvents.observe(e->{if(e.rune().equals("heal")){
     check(outcome==null,"One actual successful owner Heal event");
     check(e.source().equals(connected)&&e.recipient().equals(connected)&&e.moment()==LifeOwnerEvents.Moment.APPLY&&e.delta()>0,"Exact source and recipient own actual healing");
     check(p.position().distanceTo(new Vec3(.5,101,.5))<.01&&p.getYRot()==0&&p.getXRot()==0,"Payment pose remains intact through delayed release");outcome=e;
    }});
   });
   c.runOnClient(mc->{mc.setCameraEntity(mc.player);mc.options.setCameraType(view);
    MagicQuality.own=minimal?MagicQuality.Level.MINIMAL:MagicQuality.Level.FULL;
    MagicQuality.others=minimal?MagicQuality.Level.FULL:MagicQuality.Level.MINIMAL;
    MagicQuality.reducedFlash=reduced;if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   c.waitTicks(3);c.runOnClient(mc->mc.particleEngine.clearParticles());
   server.runOnServer(s->NextSignatureNative.cast(connected(s,connected),Runes.SELF,Runes.HEAL));
   c.waitTicks(6); // Includes native ParticleEngine insertion after the receiver's END-tick emission.
   server.runOnServer(s->{var p=connected(s,connected);check(p.getHealth()>4&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Exact connected Survival body actually healed");});
   check(outcome!=null,"Paid release published an actual outcome");
   if(!view.isFirstPerson()&&!clipped)c.waitFor(mc->!owned(mc,connected,false).isEmpty(),5);
   int[] expected={0};LifeOutcomes.draw(new LifeOutcomes.Observation(outcome.rune(),LifeOutcomes.Moment.APPLY,outcome.anchor(),null,outcome.units(),outcome.delta(),0,outcome.normal(),outcome.standoff()),minimal,(option,at)->expected[0]++);
   var original=c.computeOnClient(mc->{
    check(mc.player.getUUID().equals(connected)&&mc.getCameraEntity()==mc.player&&mc.gameRenderer.mainCamera().entity()==mc.player,"Rendered camera belongs to exact current connected player");
    var camera=mc.gameRenderer.mainCamera();var eye=mc.player.getEyePosition();
    var live=owned(mc,connected);
    if(view.isFirstPerson()||clipped){
     check(live.isEmpty(),"First-person/near-clipped native camera admits no face-obscuring self Heal pieces");
     if(clipped)check(camera.isDetached()&&camera.position().distanceTo(eye)<1.1,"Real wall compresses native third-person camera inside clearance");
    }else{
     check(camera.isDetached()&&camera.position().distanceTo(eye)>2.5,"Actual front/back native camera is detached at useful body distance");
     check(mc.level.clip(new ClipContext(camera.position(),eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS,"Native camera-to-owner ray is unobstructed");
     check(live.size()==expected[0],"Source-own quality retains complete paid self recipe: "+live.size()+"/"+expected[0]);
     check(extracted(mc,live)>0,"Live paid self material submits quads in actual native third person");
     for(var p:live){
      check(p.getLifetime()==10,"Actual Life outcome lifetime stays ten ticks");
      int age=(Integer)field(p,Particle.class,"age");float alpha=(Float)field(p,SingleQuadParticle.class,"alpha");
      float authored=Math.min(1,age*.8F)*Math.min(1,(1-age/10F)*3);
      check(Math.abs(alpha-authored*(reduced?.72F:1))<.0001,"Reduced flash retains authored alpha scaling");
     }
    }
    return live;
   });
   c.takeScreenshot(TestScreenshotOptions.of("life_owner_camera_"+name).disableCounterPrefix());
   if(transitions){
    c.runOnClient(mc->{
     mc.options.setCameraType(CameraType.FIRST_PERSON);
     check(extracted(mc,original)==0,"Immediate third-to-first switch suppresses existing quads before the next camera update");
     check(owned(mc,connected).equals(original),"View switch neither replaces nor re-emits particles");
    });c.waitTicks(1);
    c.runOnClient(mc->{check(!mc.gameRenderer.mainCamera().isDetached()&&extracted(mc,original)==0,"Actual first-person camera keeps the same live particles out of the face/HUD");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
    c.waitTicks(1);c.runOnClient(mc->{
     check(extracted(mc,original)>0&&owned(mc,connected).equals(original),"Return to actual third person uses the same unextended live particles");
     mc.player.setYRot(mc.player.getYRot()+90); // After actual release only: native look input changes the camera, not cast delivery.
    });c.waitTicks(1);
    c.runOnClient(mc->{check(mc.getCameraEntity()==mc.player&&mc.gameRenderer.mainCamera().isDetached(),"Free-look stays on native owner camera");check(extracted(mc,original)>0,"Changed native camera angle uses its actual current position");});
   }
   c.waitTicks(12);c.runOnClient(mc->check(owned(mc,connected).isEmpty(),"Authored self particles expire without camera-change renewal"));
  }finally{LifeOwnerEvents.clear();outcome=null;}
 }
 private static ServerPlayer connected(net.minecraft.server.MinecraftServer server,UUID id){
  var p=server.getPlayerList().getPlayer(id);check(p!=null&&p.connection!=null&&p.connection.player==p&&p.connection.hasClientLoaded(),"Exact loaded PlayerList/connection caster required");return p;
 }
 private static List<Particle> owned(Minecraft mc,UUID id){return owned(mc,id,true);}
 private static List<Particle> owned(Minecraft mc,UUID id,boolean queued){return particles(mc.particleEngine,queued).stream().filter(p->p.isAlive()&&p instanceof LifeParticle&&id.equals(field(p,LifeParticle.class,"outcomeSource"))).toList();}
 private static int extracted(Minecraft mc,List<Particle> particles){int count=0;for(var p:particles)if(p.isAlive()){
  var state=new QuadParticleRenderState();((SingleQuadParticle)p).extract(state,mc.gameRenderer.mainCamera(),1);if(!state.isEmpty())count++;
 }return count;}
 private static List<Particle> particles(ParticleEngine engine,boolean queued){var out=new ArrayList<Particle>();
  for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);
  if(queued)for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;
 }
 private static Object field(Object o,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
