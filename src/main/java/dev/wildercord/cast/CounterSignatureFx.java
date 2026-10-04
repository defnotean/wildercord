package dev.wildercord.cast;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.VoidOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Successful state changes have discrete original material arrangements, with recipient-budgeted emissions. */
final class CounterSignatureFx {
 private CounterSignatureFx(){}
 private static void dot(ServerLevel l,int style,int color,Vec3 at,float size){Fx.send(l,new MaterialOption(style,color,size,12),at,1,0,0);}
 private static void sound(ServerLevel l,Vec3 at,String id){Feels.sound(l,at,"nextsignature_"+id+"_impact",.35F,1);}
 private static void fragment(ServerLevel l,int style,int color,Vec3 at,float size,Vec3 drift){Fx.send(l,new VoidOption(style,color,size,12,drift,.04F),at,1,0,0);}
 static void open(ServerLevel l,LivingEntity t,boolean quiet){
  var forward=t.getLookAngle().multiply(1,0,1);if(forward.lengthSqr()<.001)forward=new Vec3(0,0,1);else forward=forward.normalize();
  var right=forward.cross(new Vec3(0,1,0)).normalize();var at=t.getEyePosition().add(forward.scale(.4));
  if(quiet){
   // Three displaced stamps close around a blank cast slot; sand flows counter to the next stamp.
   for(int i=0;i<3;i++){
    var stamp=at.add(right.scale((i-1)*.18)).add(0,.1-i*.07,0);
    dot(l,MaterialOption.ARCANE,0xBFAACF,stamp,.065F);
    dot(l,MaterialOption.TIME,0xBCAE78,stamp.add(0,.13,0),.035F);
   }
  }else{
   // Reflective teeth frame an empty front aperture; the black intake never fills its center.
   for(int side:new int[]{-1,1})for(int i=0;i<3;i++){
    var edge=at.add(right.scale(side*(.23+i*.025))).add(0,(i-1)*.16,0);
    fragment(l,VoidOption.SHARD,0xBDCCD2,edge,.075F,right.scale(-side*.004));
    fragment(l,VoidOption.FOLD,0x51445E,edge.add(forward.scale(-.1)),.055F,forward.scale(-.004));
    if(i==1)dot(l,MaterialOption.ARCANE,0xBBC6D0,edge.add(0,.025,0),.03F);
   }
  }
 }
 static void release(ServerLevel l,LivingEntity t,boolean quiet){
  var at=t.getEyePosition();
  if(quiet)for(int i=0;i<4;i++)dot(l,MaterialOption.TIME,0xBCA477,at.add((i-1.5)*.05,-.12-i*.06,0),.035F);
  else for(int side:new int[]{-1,1})dot(l,MaterialOption.ARCANE,0xA1AAB3,at.add(side*.32,-.2,0),.06F);
 }
 static void capture(ServerLevel l,LivingEntity t,Vec3 at,boolean quiet){
  if(quiet){
   for(int i=0;i<5;i++)dot(l,MaterialOption.ARCANE,0xBDACCC,at.add((i-2)*.04,0,0),.07F);
   for(int i=0;i<4;i++)dot(l,MaterialOption.TIME,0xB7A778,at.add(0,-i*.045,0),.03F);
  }else{
   var delta=t.getEyePosition().subtract(at);
   for(int i=0;i<7;i++){var p=at.add(delta.scale(i/6.));fragment(l,VoidOption.FOLD,0x5B4C67,p,.08F-i*.006F,delta.normalize().scale(.01));
    if(i%2==0)dot(l,MaterialOption.ARCANE,0xC1CCD1,p.add(.03,.05,0),.045F);}
  }sound(l,at,quiet?"quietus":"nullcatch");
 }
 static void bell(ServerLevel l,LivingEntity t,int beat){
  var at=t.position().add(0,.08,0);
  // One electrode visibly empties at each committed discharge.
  for(int leg=beat;leg<2;leg++)for(int i=0;i<3;i++)dot(l,MaterialOption.STORM,0xC4B98E,at.add(leg==0?-.22:.22,i*.09,0),.045F);
  if(beat>0){for(int i=0;i<5;i++)dot(l,MaterialOption.STORM,0xE7D8A5,at.add((i-2)*.11,.2+(i%2)*.05,0),.06F);sound(l,at,"second_bell");}
  for(int i=0;i<3;i++)dot(l,MaterialOption.TIME,0xADA077,at.add(.03,-.015,.12+i*.04),.03F);
 }
 static void cancelBell(ServerLevel l,Vec3 at){for(int side:new int[]{-1,1})dot(l,MaterialOption.STONE,0xA58158,at.add(side*.25,.08,0),.065F);}
 static void gates(ServerLevel l,LivingEntity t,int remaining){
  var at=t.position().add(0,.65,0);
  for(int gate=0;gate<remaining;gate++){
   double y=gate*.15;
   dot(l,MaterialOption.BLOOD,0x9D4D4C,at.add(-.2,y,0),.055F);
   dot(l,MaterialOption.BLOOD,0xC67969,at.add(.17,y,.05),.05F);
   dot(l,MaterialOption.ARCANE,0xBEB09D,at.add(.22,y,.06),.035F);
  }
 }
 static void cut(ServerLevel l,LivingEntity t,int remaining){
  var at=t.position().add(0,.75,0);
  for(int i=0;i<5;i++)dot(l,MaterialOption.BLOOD,0xB35C53,at.add((i-2)*.1,(i-2)*.05,0),.05F);
  gates(l,t,remaining);sound(l,at,"red_ledger");
 }
}
