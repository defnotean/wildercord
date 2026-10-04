package dev.wildercord.cast;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.VoidOption;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
final class TrailSignatureFx {
 private TrailSignatureFx(){}
 private static void dot(ServerLevel l,ParticleOptions p,Vec3 at){Fx.send(l,p,at,1,0,0);}
 private static MaterialOption m(int style,int color,float size){return new MaterialOption(style,color,size,12);}
 private static VoidOption v(int style,float size,Vec3 drift){return new VoidOption(style,0x61536A,size,12,drift,.025F);}
 private static void voice(ServerLevel l,Vec3 at,String id){Feels.sound(l,at,"nextsignature_"+id+"_impact",.35F,1);}
 static void envelope(ServerLevel l,Vec3 from,Vec3 to,int units){
  var delta=to.subtract(from);for(int i=0;i<6;i++){var at=from.add(delta.scale(i/5.));
   dot(l,m(MaterialOption.WATER,0xA0CFC0,.075F),at.add(0,.08,0));
   dot(l,v(VoidOption.CLOTH,.08F,delta.normalize().scale(.018)),at.add(.06,-.015,0));
  }
  dot(l,v(VoidOption.FOLD,.12F,new Vec3(0,-.004,0)),to);voice(l,to,"pocket_current");
 }
 static void links(ServerLevel l,Vec3 from,Vec3 to){
  var delta=to.subtract(from);for(int segment=1;segment<=3;segment++){
   var at=from.add(delta.scale(segment/4.));
   dot(l,m(MaterialOption.ARCANE,0xC5D0C2,.055F),at.add(-.07,.07,0));
   dot(l,m(MaterialOption.ARCANE,0xC5D0C2,.055F),at.add(.07,-.07,0));
   dot(l,v(VoidOption.CLOTH,.085F,delta.normalize().scale(.006)),at);
  }
 }
 static void anchor(ServerLevel l,Vec3 at){dot(l,v(VoidOption.FOLD,.1F,new Vec3(0,-.002,0)),at);voice(l,at,"wayline");}
 static void seam(ServerLevel l,Vec3 at,Direction direction,int solids,boolean threat,boolean audible){
  var inward=new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());
  // A readable near-face stitch board displays one stitch per probed stone cell; no full-volume x-ray.
  for(int i=0;i<solids;i++){
   var stitch=at.add((i-1)*.15,.12+(i%2)*.09,0);
   dot(l,v(VoidOption.CLOTH,.12F,inward.scale(.003)),stitch);
   dot(l,m(MaterialOption.ARCANE,0xCFC39D,.05F),stitch.add(.06,.03,0));
  }
  dot(l,v(threat?VoidOption.TOOTH:VoidOption.FOLD,.09F,new Vec3(0,.003,0)),at.add(0,.45,0));
  if(audible)voice(l,at,"night_seam");
 }
 static void compass(ServerLevel l,Vec3 from,Vec3 to){
  var delta=to.subtract(from);double length=delta.length();
  for(int i=1;i<=12;i++){double phase=i/12.;var at=from.add(delta.scale(phase));
   dot(l,m(MaterialOption.STONE,0xB99B71,(float)NextSignatureRules.gradedCrumb(length*(1-phase))),at);
   if(i%4==0)dot(l,m(MaterialOption.ARCANE,0xCEC6B0,.035F),at.add(0,.07,0));
  }voice(l,from,"shard_compass");
 }
}
