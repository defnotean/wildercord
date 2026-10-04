package dev.wildercord.cast;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
final class SupportSignatureFx {
 private SupportSignatureFx(){}
 private static void dot(ServerLevel l,ParticleOptions p,Vec3 at){Fx.send(l,p,at,1,0,0);}
 private static MaterialOption m(int style,int color,float size){return new MaterialOption(style,color,size,12);}
 private static void voice(ServerLevel l,Vec3 at,String id){Feels.sound(l,at,"nextsignature_"+id+"_impact",.35F,1);}
 static void escrow(ServerLevel l,LivingEntity from,LivingEntity to,float gift){
  var a=from.position().add(0,.7,0);var b=to.position().add(.3,.9,0);var delta=b.subtract(a);
  for(int i=0;i<7;i++){double beat=i/6.;dot(l,m(MaterialOption.BLOOD,0xA55B52,.06F),a.add(delta.scale(beat)).add(0,Math.sin(beat*Math.PI)*.2,0));}
  for(int segment=0;segment<Math.ceil(gift);segment++)dot(l,m(MaterialOption.BLOOD,0xC77869,.08F),b.add(0,segment*.09,0));
  for(int side:new int[]{-1,1})for(int i=0;i<3;i++)dot(l,m(MaterialOption.ARCANE,0xC9B6AF,.04F),b.add(side*.14,i*.1,0));
  voice(l,b,"blood_escrow");
 }
 static void molt(ServerLevel l,LivingEntity t,boolean broken){
  var at=t.position().add(.22,.85,0);
  if(broken){for(int i=0;i<7;i++)dot(l,m(MaterialOption.FROST,0xAFCBBF,.05F),at.add((i-3)*.07,(i%3)*.08,0));voice(l,at,"frost_molt");}
  else{
   for(int i=0;i<4;i++)dot(l,m(MaterialOption.FROST,0xA9C5C1,.075F),at.add((i%2)*.1,(i/2)*.15,0));
   for(int i=0;i<3;i++)dot(l,new LifeOption(LifeOption.VINE,0x99AC7E,.055F,12,new Vec3(.001,-.002,0),.01F),at.add(.04,i*.1,.03));
  }
 }
 static void ferry(ServerLevel l,Vec3 from,LivingEntity to,float taken,int phase){
  if(!(taken>0)||!Float.isFinite(taken)||to.isRemoved()||!to.isAlive()||to.level()!=l)return;
  var destination=to.position().add(0,.8,0);var delta=destination.subtract(from);
  // The previous visit endpoint is a historical snapshot; never trace an unloaded source chunk.
  if(l.isLoaded(net.minecraft.core.BlockPos.containing(from)))for(int i=0;i<7;i++){double f=i/6.;var at=from.add(delta.scale(f)).add(0,.2*Math.sin(f*Math.PI),0);
   dot(l,new LifeOption(LifeOption.SAP,0xA6BE78,.08F,12,delta.normalize().scale(.025),.01F),at);
   if(i==0 || i==6)dot(l,m(MaterialOption.TIME,0xC6B786,.04F),at.add(0,.1,0));
  }
  // Actual positive-heal landing belongs to LifeOwnerEvents/LifeOutcomes; keep this parcel path and sole voice.
  voice(l,destination,"pulse_ferry");
 }
 static void lantern(ServerLevel l,Vec3 at,boolean returning){
  at=at.add(0,.15,0);
  for(int side:new int[]{-1,1})for(int i=0;i<3;i++)dot(l,m(MaterialOption.ARCANE,0xC9BA90,.045F),at.add(side*.14,i*.09,0));
  dot(l,m(MaterialOption.ARCANE,0xDCD0AC,.045F),at.add(0,.34,0));
  for(int i=0;i<3;i++)dot(l,m(MaterialOption.TIME,0xC2A771,.035F),at.add(0,returning?.04+i*.08:.25-i*.08,0));
  if(returning)voice(l,at,"last_lantern");
 }
}
