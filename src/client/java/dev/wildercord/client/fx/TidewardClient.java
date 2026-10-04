package dev.wildercord.client.fx;

import dev.wildercord.net.TidewardCue;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Owner-only physical warning notches and a braided, finite footing observation. */
public final class TidewardClient {
 private static final class View {
  final TidewardCue cue;final ClientLevel world;final TidewardPlayback playback;long checked=-1;boolean routeValid;
  View(TidewardCue cue,ClientLevel world,long local){this.cue=cue;this.world=world;playback=new TidewardPlayback(local,cue.kind()==TidewardCue.WARNING?12:100);}
  TidewardCue cue(){return cue;}ClientLevel world(){return world;}
 }
 private static View warning,route,pendingRoute;
 private static long pendingUntil;
 private static long warningAccepted,routeAccepted,routeCancelled,playbackTick,routeBursts,routePieces,routeTerrainReads;
 private TidewardClient(){}
 public static long warningAccepted(){return warningAccepted;}public static long routeAccepted(){return routeAccepted;}public static long routeCancelled(){return routeCancelled;}
 public static long routeBursts(){return routeBursts;}public static long routePieces(){return routePieces;}public static long routeTerrainReads(){return routeTerrainReads;}
 public static void init(){
  ClientPlayNetworking.registerGlobalReceiver(TidewardCue.TYPE,(cue,context)->{
   var mc=context.client();if(mc.level==null || mc.player==null || !mc.level.dimension().identifier().toString().equals(cue.world()))return;
   if(cue.kind()==TidewardCue.CLEAR){if(pendingRoute!=null&&pendingRoute.cue().nonce()==cue.nonce())pendingRoute=null;if(route!=null && route.cue().nonce()==cue.nonce()){route=null;routeCancelled++;}return;}
   if(Math.abs(mc.level.getGameTime()-cue.nonce())>40)return;
   var view=new View(cue,mc.level,playbackTick);
   if(cue.kind()==TidewardCue.WARNING){warning=view;if(!validWarning(mc,view)){warning=null;return;}warningAccepted++;notches(mc,view);}
   else {pendingRoute=view;pendingUntil=playbackTick+5;admitPending(mc);}
  });
  ClientTickEvents.END_CLIENT_TICK.register(TidewardClient::tick);
 }
 private static boolean current(Minecraft mc,View v,int life){return v!=null && mc.player!=null && mc.level==v.world() && mc.player.isAlive() && !mc.player.isSpectator()
  && v.playback.alive(playbackTick);}
 private static ReedbackCrab crab(Minecraft mc,View v){return mc.level!=null && mc.level.getEntity(v.cue().entity()) instanceof ReedbackCrab c && c.getUUID().equals(v.cue().identity())?c:null;}
 private static boolean validWarning(Minecraft mc,View v){
  if(!current(mc,v,12) || !mc.player.getItemBySlot(EquipmentSlot.HEAD).is(TidewardEquipment.SPECTACLES) || !mc.player.isShiftKeyDown())return false;
  var c=crab(mc,v);return c!=null && c.isAlive() && !c.isRemoved() && !c.isInvisibleTo(mc.player) && c.pose()==ReedbackCrab.WARNING
   && mc.player.getLookAngle().dot(c.getBoundingBox().getCenter().subtract(mc.player.getEyePosition()).normalize())>=.5
   && c.distanceToSqr(mc.player)<=64 && mc.level.hasChunkAt(c.blockPosition()) && mc.player.hasLineOfSight(c);
 }
 private static boolean validRoute(Minecraft mc,View v){
  if(v==null||!current(mc,v,100)||!mc.player.getMainHandItem().is(TidewardSurvey.ITEM)&&!mc.player.getOffhandItem().is(TidewardSurvey.ITEM))return false;
  if(v.checked==playbackTick)return v.routeValid;v.checked=playbackTick;
  return v.routeValid=routeBody(mc,v);
 }
 private static boolean routeBody(Minecraft mc,View v){
  if(!current(mc,v,100) || !mc.player.getMainHandItem().is(TidewardSurvey.ITEM) && !mc.player.getOffhandItem().is(TidewardSurvey.ITEM))return false;
  routeTerrainReads++;for(var floor:v.cue().feet()){
   var feet=floor.above();var head=feet.above();if(!mc.level.hasChunkAt(floor) || !mc.level.hasChunkAt(feet) || !mc.level.hasChunkAt(head))return false;
   if(!mc.level.getBlockState(floor).isCollisionShapeFullBlock(mc.level,floor)||!mc.level.getFluidState(floor).isEmpty()
    || !mc.level.getBlockState(feet).getCollisionShape(mc.level,feet).isEmpty() || !mc.level.getBlockState(head).getCollisionShape(mc.level,head).isEmpty()
    || !mc.level.getFluidState(head).isEmpty() || !mc.level.getFluidState(feet).isEmpty() && !mc.level.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER))return false;
  }
  return true;
 }
 private static void admitPending(Minecraft mc){
  var v=pendingRoute;if(v==null)return;
  if(playbackTick>=pendingUntil||!current(mc,v,100)||!mc.player.getMainHandItem().is(TidewardSurvey.ITEM)&&!mc.player.getOffhandItem().is(TidewardSurvey.ITEM)){pendingRoute=null;return;}
  // A paid private cue can precede vanilla chunk/block updates. Never render an invalid prefix.
  if(!validRoute(mc,v))return;
  pendingRoute=null;route=v;routeAccepted++;
  if(!mc.isPaused()&&v.playback.firstBurst(playbackTick))braid(mc,v);
 }
 private static void tick(Minecraft mc){
  if(!mc.isPaused())playbackTick++;
  admitPending(mc);
  if(!validWarning(mc,warning))warning=null;
  if(route!=null && !validRoute(mc,route)){route=null;routeCancelled++;}
  if(route!=null && !mc.isPaused() && route.playback.burst(playbackTick))braid(mc,route);
 }
 private static void notches(Minecraft mc,View v){
  int count=MagicQuality.own==MagicQuality.Level.MINIMAL?2:3;
  for(int i=0;i<count;i++)mc.particleEngine.add(new Piece(mc.level,Vec3.ZERO,"tideward_notch",.16F,12,i,v,()->warning==v && validWarning(mc,v)));
 }
 private static Vec3 surface(Minecraft mc,BlockPos floor){double water=mc.level.getFluidState(floor.above()).getHeight(mc.level,floor.above());return Vec3.atBottomCenterOf(floor).add(0,1.09+water,0);}
 private static void braid(Minecraft mc,View v){
  int count=MagicQuality.own==MagicQuality.Level.MINIMAL?8:24;var points=v.cue().feet();
  routeBursts++;routePieces+=count+2;BooleanSupplier valid=()->route==v && validRoute(mc,v);
  for(int i=0;i<count;i++){
   double along=i/(double)(count-1)*(points.size()-1);int index=Math.min(points.size()-2,(int)along);
   var at=surface(mc,points.get(index)).lerp(surface(mc,points.get(index+1)),along-index);
   mc.particleEngine.add(new Piece(mc.level,at,"tideward_braid",.095F,12,i,null,valid));
  }
  for(var floor:new BlockPos[]{points.getFirst(),points.getLast()})mc.particleEngine.add(new Piece(mc.level,surface(mc,floor).add(0,.11,0),"tideward_peg",.19F,12,0,null,valid));
 }
 private static final class Piece extends SingleQuadParticle implements SigilGroup.Extent {
  private final float size;private final int index;private final View following;private final BooleanSupplier valid;
  Piece(ClientLevel level,Vec3 at,String sprite,float size,int life,int index,View following,BooleanSupplier valid){
   super(level,at.x,at.y,at.z,SpellCircleParticle.particleSprite(sprite));this.size=size;this.index=index;this.following=following;this.valid=valid;
   lifetime=life;quadSize=size;hasPhysics=false;alpha=MagicQuality.reducedFlash?.75F:1;roll=following==null?0:(index-1)*.45F;oRoll=roll;
   if(following!=null){position();xo=x;yo=y;zo=z;}
  }
  private void position(){var mc=Minecraft.getInstance();var c=crab(mc,following);if(c==null)return;
   var forward=c.getLookAngle().multiply(1,0,1).normalize();var side=new Vec3(-forward.z,0,forward.x);
   var at=c.position().add(0,c.getBbHeight()+.13,0).add(side.scale((index-1)*(.12+age*.008))).add(forward.scale(.22));x=at.x;y=at.y;z=at.z;
  }
  @Override public void tick(){xo=x;yo=y;zo=z;oRoll=roll;if(age++>=lifetime || !valid.getAsBoolean()){remove();return;}
   if(following!=null)position();quadSize=size*(.8F+Math.min(age,3)*.07F);alpha=Math.min(1,(lifetime-age)/3F)*(MagicQuality.reducedFlash?.75F:1);
  }
  @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}@Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
  @Override public int getLightCoords(float partial){return super.getLightCoords(partial);}
  @Override public double centreX(){return x;}@Override public double centreY(){return y;}@Override public double centreZ(){return z;}@Override public double reach(){return size*1.8;}
 }
}
