package dev.wildercord.cast;

import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Native observer framing only: the real paid Survival caster remains separate from the camera. */
final class LifeOutcomeGallery implements AutoCloseable {
 private final ClientGameTestContext context;
 private final CameraType camera;
 private final MagicQuality.Level quality,others;
 private final boolean hidden;
 private final Set<String> captured=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private final String prefix;
 private ServerPlayer viewer;
 private volatile String pending;
 LifeOutcomeGallery(ClientGameTestContext c,String prefix){
  context=c;this.prefix=prefix;
  camera=c.computeOnClient(mc->mc.options.getCameraType());
  quality=c.computeOnClient(mc->MagicQuality.own);others=c.computeOnClient(mc->MagicQuality.others);
  hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);MagicQuality.own=MagicQuality.Level.FULL;MagicQuality.others=MagicQuality.Level.FULL;
   if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});
 }
 ServerPlayer actor(net.minecraft.server.MinecraftServer s){
  viewer=s.getPlayerList().getPlayers().getFirst();viewer.setGameMode(GameType.SPECTATOR);
  viewer.teleportTo(s.overworld(),.5,102,-4.5,Set.<Relative>of(),0,15,false);
  var caster=NextSignatureNative.guest(viewer,"LifeCaster",false);
  caster.snapTo(.5,101,.5,0,0);NextSignatureNative.ground(caster);return caster;
 }
 void observe(LifeOwnerEvents.Event event){
  if(event.moment()==LifeOwnerEvents.Moment.REFUSED||viewer==null)return;
  if(event.moment()==LifeOwnerEvents.Moment.END&&!(event.rune().equals("stitchtime")&&event.delta()>0))return;
  boolean reactive=Set.of("reversal","bramble","fortune","soulbond","second_wind").contains(event.rune());
  if(reactive&&event.moment()!=LifeOwnerEvents.Moment.TRIGGER)return;
  String key=event.rune().equals("pulse_ferry")?event.rune()+"_phase"+event.units():event.rune();
  if(pending!=null||captured.contains(key))return;
  var at=event.anchor().add(event.normal().scale(event.standoff()));
  // Server eye clearance is retained. The actual viewer receives the genuine packet from outside the body.
  var eye=clearEye(event,at);var direction=at.subtract(eye);
  float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
  float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
  viewer.teleportTo(event.level(),eye.x,eye.y-viewer.getEyeHeight(),eye.z,Set.<Relative>of(),yaw,pitch,false);
  pending=key;
 }
 /** At most sixteen real loaded-world rays; a hit on the actual final solid cell is permitted. */
 private net.minecraft.world.phys.Vec3 clearEye(LifeOwnerEvents.Event event,net.minecraft.world.phys.Vec3 at){
  var level=event.level();
  var target=net.minecraft.core.BlockPos.containing(event.anchor());
  var n=event.normal();var flat=new net.minecraft.world.phys.Vec3(n.x,0,n.z);
  var facing=flat.lengthSqr()<1e-6?new net.minecraft.world.phys.Vec3(0,0,1):flat.normalize();
  boolean solid=!level.getBlockState(target).getCollisionShape(level,target).isEmpty();
  net.minecraft.world.phys.Vec3 fallback=null;
  for(double lift:new double[]{1.2,.25})for(double angle:new double[]{0,Math.PI/4,-Math.PI/4,Math.PI/2,-Math.PI/2,3*Math.PI/4,-3*Math.PI/4,Math.PI}){
   double horizontal=Math.sqrt(3.2*3.2-lift*lift);
   var side=facing.yRot((float)angle);var eye=at.add(side.x*horizontal,lift,side.z*horizontal);
   boolean loaded=true;for(int i=0;i<=16;i++)if(!level.isLoaded(net.minecraft.core.BlockPos.containing(eye.lerp(at,i/16.)))){loaded=false;break;}
   if(!loaded)continue;if(fallback==null)fallback=eye;
   // The camera must see the victim past other bodies, including the real caster.
   boolean bodyBlocked=level.getEntities(viewer,new net.minecraft.world.phys.AABB(eye,at).inflate(.05),
    body->body instanceof net.minecraft.world.entity.LivingEntity && body.isAlive() && !body.isRemoved() && !body.isSpectator()
     && !body.getUUID().equals(event.recipient())).stream().anyMatch(body->body.getBoundingBox().inflate(.03).contains(eye)||body.getBoundingBox().inflate(.03).clip(eye,at).isPresent());
   if(bodyBlocked)continue;
   var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,at,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,viewer));
   if(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS||solid&&hit.getBlockPos().equals(target))return eye;
  }
  // A fallback is explicit evidence of an obstructed fixture, not a guarantee of visible material.
  if(fallback==null)throw new AssertionError("No loaded observer ray for actual outcome anchor "+at);
  dev.wildercord.Wildercord.LOGGER.info("LIFE_GALLERY_OCCLUDED rune={} cell={} materialFocus={}",event.rune(),target,at);
  return fallback;
 }
 void waitTicks(int ticks){
  for(int i=0;i<ticks;i++){
   context.waitTicks(1);String key=pending;
   if(key!=null&&i+2<ticks){
    // These two presentation ticks are part of the requested gameplay wait, never appended to it.
    context.waitTicks(2);i+=2;
    context.takeScreenshot(TestScreenshotOptions.of(prefix+"_"+key).disableCounterPrefix());
    captured.add(key);pending=null;
   }
  }
 }
 boolean captured(String rune){return captured.contains(rune);}
 @Override public void close(){
  context.runOnClient(mc->{mc.options.setCameraType(camera);MagicQuality.own=quality;MagicQuality.others=others;
   if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});
  pending=null;viewer=null;captured.clear();
 }
}
