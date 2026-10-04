package dev.wildercord.client.fx;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
/** Finite private physical-filter beat; local age cannot replay on a stalled server clock. */
public final class MossveilFilterClient{
 private record View(MossveilFilterCue cue,ClientLevel world,long born){}
 private static View active;private static ClientLevel lastWorld;private static long lastNonce=-1,local,accepted,pieces,retired;private static int emitted=-1;
 public static long accepted(){return accepted;}public static long pieces(){return pieces;}public static long retired(){return retired;}
 public static void init(){net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(MossveilParticles.FIBER,new MossveilFiberParticle.Provider());ClientPlayNetworking.registerGlobalReceiver(MossveilFilterCue.TYPE,(e,context)->{
  var mc=context.client();if(mc.level==null||mc.player==null||!e.wearer().equals(mc.player.getUUID())||!mc.level.dimension().identifier().toString().equals(e.world())||Math.abs(mc.level.getGameTime()-e.tick())>40)return;
  if(lastWorld!=mc.level){lastWorld=mc.level;lastNonce=-1;}if(e.tick()<=lastNonce)return;
  var v=new View(e,mc.level,local);if(!current(mc,v))return;lastNonce=e.tick();active=v;emitted=-1;accepted++;
 });ClientTickEvents.END_CLIENT_TICK.register(MossveilFilterClient::tick);}
 private static boolean current(Minecraft mc,View v){return mc.player!=null&&mc.level==v.world&&mc.player.isAlive()&&!mc.player.isSpectator()&&mc.player.getItemBySlot(EquipmentSlot.HEAD).is(MossveilCowl.ITEM)&&mc.level.getEntity(v.cue.pet())instanceof MossveilDormouse pet&&pet.getUUID().equals(v.cue.source())&&pet.isAlive()&&!pet.isRemoved()&&pet.isOwnedBy(mc.player)&&pet.isInSittingPose()&&mc.player.distanceToSqr(pet)<=9;}
 private static void tick(Minecraft mc){
  if(mc.level==null||mc.player==null){active=null;lastWorld=null;lastNonce=-1;return;}if(lastWorld!=mc.level){if(active!=null)retired++;active=null;lastWorld=mc.level;lastNonce=-1;}if(mc.isPaused())return;local++;
  var v=active;if(v==null)return;long age=local-v.born;
  if(age>=16||age<0||!current(mc,v)){active=null;retired++;return;}
  int beat=age<4?0:1;if(beat==emitted)return;emitted=beat;
  boolean minimal=MagicQuality.own==MagicQuality.Level.MINIMAL;int count=minimal?2:4;
  var p=mc.player;var horizontal=p.getLookAngle().multiply(1,0,1).normalize();var side=new Vec3(-horizontal.z,0,horizontal.x);
  for(int i=0;i<count;i++){
   double sign=i%2==0?-1:1;var at=p.position().add(side.scale(sign*(.62+i*.015))).add(0,.78+beat*.08,0);
   if(at.distanceToSqr(p.getEyePosition())<.49||at.distanceToSqr(mc.gameRenderer.mainCamera().position())<.49)continue;
   var drift=side.scale(sign*.006).add(0,.007,0);mc.particleEngine.add(new MossveilFiberParticle(mc.level,at,drift,i));pieces++;
  }
 }
}
