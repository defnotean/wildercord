package dev.wildercord.wildlife;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.function.BiConsumer;
/** Hand-authored earth clod cupped by fine roots; original folded-frond silhouette. */
public final class RootCarryPictures {
 private RootCarryPictures(){}
 public static boolean supports(String id){return "wildercord:root_carry".equals(id);}
 public static boolean prepare(String id,int beat,double scale,Vec3 at,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> sink){if(!supports(id))return false;body(at,right,up,forward,Math.clamp(scale,.4,2),Math.clamp(beat,0,2)/2.0,8,minimal,sink);return true;}
 public static boolean fly(String id,int age,double scale,Vec3 at,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> sink){if(!supports(id))return false;body(at,right,up,forward,Math.clamp(scale,.4,2),.6+.1*Math.sin(age*.35),5,minimal,sink);return true;}
 private static void body(Vec3 at,Vec3 right,Vec3 up,Vec3 forward,double scale,double fold,int lifetime,boolean minimal,BiConsumer<ParticleOptions,Vec3> sink){
  sink.accept(new MaterialOption(MaterialOption.STONE,0x6C5540,(float)(.13*scale),lifetime),at);
  sink.accept(new LifeOption(LifeOption.VINE,0xBFA47C,(float)(.16*scale),lifetime,up.scale(-.006),.04F),at.add(up.scale((-.11+.04*fold)*scale)));
  if(minimal)return;
  for(int side:new int[]{-1,1}){
   var root=at.add(right.scale(side*(.18-.055*fold)*scale)).add(up.scale(-.08*scale)).add(forward.scale(.04*scale));
   sink.accept(new LifeOption(LifeOption.VINE,0x9C845F,(float)(.13*scale),lifetime,right.scale(-side*.008).add(up.scale(.003)),side*.035F),root);
   sink.accept(new LifeOption(LifeOption.LEAF,0x99AC75,(float)(.11*scale),lifetime,right.scale(-side*.005),side*.05F),at.add(right.scale(side*(.14-.08*fold)*scale)).add(up.scale((.17-.055*fold)*scale)));
  }
 }
 public static void transfer(RootCarryFx.Event e,int beat,boolean minimal,BiConsumer<ParticleOptions,Vec3> sink){
  if(beat<0||beat>2)throw new IllegalArgumentException("Invalid root picture beat");double t=beat/2.0;
  Vec3 a=new Vec3(e.from().getX()+.5,e.from().getY()+.18,e.from().getZ()+.5),b=new Vec3(e.to().getX()+.5,e.to().getY()+.18,e.to().getZ()+.5),direction=b.subtract(a);Vec3 forward=direction.lengthSqr()<.01?new Vec3(0,0,1):direction.normalize();Vec3 right=forward.cross(new Vec3(0,1,0));if(right.lengthSqr()<.01)right=new Vec3(1,0,0);else right=right.normalize();
  Vec3 at=e.moved()?a.add(direction.scale(t)).add(0,Math.sin(t*Math.PI)*.65,0):a;
  body(at,right,new Vec3(0,1,0),forward,1,e.moved()?t:1,8,minimal,sink);
 }
}
