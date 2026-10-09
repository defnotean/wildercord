package dev.wildercord.client.fx;

import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Camera safety belongs to each extraction, including particles admitted before a view switch.
 * The scoped tag preserves vanilla particle admission and never changes a particle's lifetime.
 */
final class LifeOwnerClearance {
 private record Emission(UUID source,boolean owner){}
 private static final ThreadLocal<Emission> SOURCE=new ThreadLocal<>();
 private static final double CLEARANCE_SQUARED=1.1*1.1;
 private LifeOwnerClearance(){}
 static UUID source(){var emission=SOURCE.get();return emission==null?null:emission.source();}
 static boolean owner(){var emission=SOURCE.get();return emission!=null&&emission.owner();}
 static void emit(UUID source,boolean owner,Runnable spawn){
  Emission previous=SOURCE.get();SOURCE.set(new Emission(source,owner));
  try{spawn.run();}finally{if(previous==null)SOURCE.remove();else SOURCE.set(previous);}
 }
 static boolean clear(boolean nativeOwnerThirdPerson,Vec3 at,Vec3 eye,Vec3 camera){
  return at.distanceToSqr(camera)>=CLEARANCE_SQUARED
   &&(nativeOwnerThirdPerson||at.distanceToSqr(eye)>=CLEARANCE_SQUARED);
 }
 static boolean clear(Minecraft mc,Camera camera,Vec3 at,boolean own){
  if(mc.player==null||!camera.isInitialized())return false;
  boolean nativeOwnerThirdPerson=own&&!mc.options.getCameraType().isFirstPerson()
   &&mc.getCameraEntity()==mc.player&&camera.entity()==mc.player&&camera.isDetached();
  return clear(nativeOwnerThirdPerson,at,mc.player.getEyePosition(),camera.position());
 }
 static boolean visible(UUID source,ClientLevel level,Camera camera,float partial,
                        double xo,double yo,double zo,double x,double y,double z){
  if(source==null)return true; // Other particle sources retain their established renderer.
  var mc=Minecraft.getInstance();
  if(mc.level!=level||mc.player==null||!source.equals(mc.player.getUUID()))return false;
  Vec3 at=new Vec3(xo+(x-xo)*partial,yo+(y-yo)*partial,zo+(z-zo)*partial);
  return clear(mc,camera,at,true);
 }
}
