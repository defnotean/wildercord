package dev.wildercord.cast;

import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Native owner framing: the exact connected Survival caster remains the camera entity. */
final class LifeOutcomeGallery implements AutoCloseable {
 private record Capture(String key,LifeOwnerEvents.Event event){}
 private final ClientGameTestContext context;
 private final CameraType camera;
 private final MagicQuality.Level quality,others;
 private final boolean hidden;
 private final Set<String> captured=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private final Set<String> queued=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private final Queue<Capture> pending=new ConcurrentLinkedQueue<>();
 private final String prefix;
 private UUID connected;
 private net.minecraft.client.multiplayer.ClientLevel clientWorld;
 private ServerPlayer owner;
 private CameraType prepared=CameraType.THIRD_PERSON_FRONT;
 private float yaw=-90,pitch=-15;
 LifeOutcomeGallery(ClientGameTestContext c,String prefix){
  context=c;this.prefix=prefix;
  camera=c.computeOnClient(mc->mc.options.getCameraType());
  quality=c.computeOnClient(mc->MagicQuality.own);others=c.computeOnClient(mc->MagicQuality.others);
  hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  c.runOnClient(mc->{MagicQuality.own=MagicQuality.Level.FULL;MagicQuality.others=MagicQuality.Level.FULL;
   if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});
 }
 /** Test-thread setup, before payment. Native camera mode never supplies a substitute body. */
 void prepare(CameraType type,float yaw,float pitch){
  this.prepared=type;this.yaw=yaw;this.pitch=pitch;
  connected=context.computeOnClient(mc->{check(mc.player!=null,"Connected owner exists before paid fixture setup");
   if(clientWorld!=mc.level){pending.clear();queued.clear();clientWorld=mc.level;}
   mc.setCameraEntity(mc.player);mc.options.setCameraType(type);mc.player.setYRot(yaw);mc.player.setXRot(pitch);
   return mc.player.getUUID();
  });
 }
 ServerPlayer actor(MinecraftServer s){
  check(connected!=null,"Native owner camera is prepared before resolving the caster");
  owner=s.getPlayerList().getPlayer(connected);checkOwner(owner);
  NextSignatureNative.teach(owner);
  owner.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),yaw,pitch,false);
  return owner;
 }
 /** Only pre-payment aim setup; neither capture nor waiting ever restores a player's position. */
 void beforePayment(ServerPlayer p){
  checkOwner(p);check(p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Connected owner pays in ordinary Survival");
  p.setYRot(yaw);p.setXRot(pitch);
  NextSignatureNative.ground(p);
  if(prepared!=CameraType.FIRST_PERSON){
   var eye=p.getEyePosition();var offset=p.getLookAngle().scale(prepared==CameraType.THIRD_PERSON_FRONT?4:-4);
   check(clearRay(p,eye.add(offset),eye),"Pre-payment native owner camera has a loaded unobstructed ray");
  }
 }
 private void checkOwner(ServerPlayer p){
  check(p!=null&&p.getUUID().equals(connected)&&p.level().getServer().getPlayerList().getPlayer(connected)==p
   &&p.connection!=null&&p.connection.player==p,"Exact connected client UUID resolves to its current PlayerList connection body");
 }
 void observe(LifeOwnerEvents.Event event){
  if(event.moment()==LifeOwnerEvents.Moment.REFUSED||owner==null)return;
  if(event.moment()==LifeOwnerEvents.Moment.END&&!(event.rune().equals("stitchtime")&&event.delta()>0))return;
  boolean reactive=Set.of("reversal","bramble","fortune","soulbond","second_wind").contains(event.rune());
  if(reactive&&event.moment()!=LifeOwnerEvents.Moment.TRIGGER)return;
  checkOwner(owner);check(owner.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Actual outcome retains the connected Survival owner");
  check(connected.equals(event.source()),"Queued actual outcome belongs to the exact connected caster");
  String key=event.rune().equals("pulse_ferry")?event.rune()+"_phase"+event.units():event.rune();
  if(captured.contains(key)||!queued.add(key))return;
  // Preserve this exact original stage/recipient. No observer movement, re-aim, or later pulse substitution.
  pending.add(new Capture(key,event));
 }
 void waitTicks(int ticks){
  for(int i=0;i<ticks;i++){
   context.waitTicks(1);var next=pending.peek();
   if(next!=null&&context.computeOnClient(mc->mc.level!=clientWorld)){pending.clear();queued.clear();next=null;}
   if(next!=null&&i+2<ticks){
    // These two presentation ticks stay inside the existing wait. Recheck native arrival view
    // as the actual teleport reaches the client, without changing its position or cast aim.
    for(int presentation=0;presentation<2;presentation++){prepareCapture(next.event());context.waitTicks(1);i++;}
    verifyCapture(next.event());
    context.takeScreenshot(TestScreenshotOptions.of(prefix+"_"+next.key()).disableCounterPrefix());
    captured.add(next.key());pending.remove(next);
   }
  }
 }
 private void prepareCapture(LifeOwnerEvents.Event event){
  context.runOnClient(mc->{check(mc.player!=null&&mc.player.getUUID().equals(connected)&&mc.getCameraEntity()==mc.player,
    "The rendered camera remains the actual connected paid owner");
   if(event.rune().equals("bloomstep")){
    // Arrival is genuine gameplay movement. Keep its actual position and cast aim; only choose a native view.
    var eye=mc.player.getEyePosition();var look=mc.player.getLookAngle().scale(4);
    if(clearRay(mc.player,eye.add(look),eye))mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    else{check(clearRay(mc.player,eye.subtract(look),eye),"Real Bloomstep arrival has an unobstructed native rear camera");
     mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
   }
  });
 }
 private void verifyCapture(LifeOwnerEvents.Event event){
  context.runOnClient(mc->{var active=mc.gameRenderer.mainCamera();
   check(mc.player!=null&&mc.player.getUUID().equals(connected)&&mc.getCameraEntity()==mc.player&&active.entity()==mc.player,
    "Native camera renders the same connected owner that paid for the captured outcome");
   check(mc.level==clientWorld&&event.level()==owner.level(),"Queued capture belongs to the same actual client/server world pair");
   long age=mc.level.getGameTime()-event.tick();check(age>=0&&age<=5,"Original owner event is captured within its actual material lifetime");
   check(clearRay(mc.player,active.position(),mc.player.getEyePosition()),"Rendered native camera-to-owner ray is loaded and unobstructed");
   if(event.rune().equals("bloomstep"))check(mc.player.position().distanceTo(event.anchor().add(0,-.42,0))<.05,
    "Rendered connected owner remains at the original actual Bloomstep arrival");
   if(event.rune().equals("sporebloom")){
    var focus=event.anchor().add(event.normal().scale(event.standoff())).add(0,event.normal().y>.999?.15:0,0);
    check(clearRay(mc.player,active.position(),focus),"Native owner camera sees the actual supported fruit or airborne body-spore focus");
    boolean blocked=mc.level.getEntities(mc.player,new net.minecraft.world.phys.AABB(active.position(),focus).inflate(.03),
     body->body instanceof net.minecraft.world.entity.LivingEntity&&body.isAlive()&&!body.isRemoved()&&!body.isSpectator()).stream()
     .anyMatch(body->body.getBoundingBox().contains(active.position())||body.getBoundingBox().clip(active.position(),focus).isPresent());
    check(!blocked,"Actual recipient and nearby bodies leave the native owner Spore material ray clear");
   }
   if(mc.options.getCameraType()!=CameraType.FIRST_PERSON){
    double distance=active.position().distanceTo(mc.player.getEyePosition());
    check(active.isDetached()&&distance>2.5&&distance<=4.2,"Actual native third-person owner camera has a useful bounded distance");
   }
  });
 }
 private static boolean clearRay(net.minecraft.world.entity.LivingEntity p,Vec3 eye,Vec3 focus){
  for(int i=0;i<=16;i++)if(!p.level().isLoaded(BlockPos.containing(eye.lerp(focus,i/16.))))return false;
  return p.level().clip(new ClipContext(eye,focus,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
 }
 boolean captured(String rune){return captured.contains(rune);}
 @Override public void close(){
  context.runOnClient(mc->{mc.setCameraEntity(mc.player);mc.options.setCameraType(camera);MagicQuality.own=quality;MagicQuality.others=others;
   if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});
  pending.clear();queued.clear();owner=null;connected=null;clientWorld=null;captured.clear();
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
