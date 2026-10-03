package dev.wildercord.client.fx;

import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.LightOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Flight-only authored fire bodies. Coordinates follow the actual server projectile velocity. */
final class FireFlights {
 private FireFlights() {}
 static final List<String> RUNES=dev.wildercord.cast.FlightBodies.FIRE;
 static boolean supports(String id){return dev.wildercord.cast.FlightBodies.supportsFire(id);}
 static void draw(String id,int age,double scale,Vec3 head,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  draw(id,age,scale,1,head,velocity,minimal,emit);
 }
 static void draw(String id,int age,double scale,double length,Vec3 head,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  if(!supports(id) || !Double.isFinite(velocity.lengthSqr()))return;
  if(FieldFusionForms.flight(id,age,scale,length,head,velocity,minimal,emit))return;
  var forward=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();
  var right=forward.cross(new Vec3(0,1,0));if(right.lengthSqr()<.0001)right=new Vec3(1,0,0);else right=right.normalize();
  var p=new Pen(head,right,right.cross(forward).normalize(),forward.scale(Math.clamp(length,1,2)),Math.clamp(scale,.4,2),emit);
  double t=age*.28;
  switch(id.substring(11)) {
   case "ember" -> { // A compact coal with three independently drifting sparks.
    p.dot(MaterialOption.EMBER,0xFFB34D,0,0,0,.11);
    for(int i=0;i<(minimal?1:3);i++)p.dot(MaterialOption.EMBER,0xD96627,Math.sin(t+i*2)*.08,Math.cos(t+i)*.07,-.18-i*.16,.04);
   }
   case "fire" -> { // A broad flame nose and split licking tongue behind it.
    p.line(0xFFE49B,0,0,.17,0,0,-.35,.075);
    for(int side:new int[]{-1,1})p.dot(MaterialOption.EMBER,0xFF752B,side*.12,Math.sin(t+side)*.12,-.28,.09);
    p.dot(MaterialOption.EMBER,0xFFC557,0,.06,-.08,.13);
   }
   case "firestorm" -> { // Two rotating wind rails carry a flame jet, rather than a coloured corkscrew alone.
    p.line(0xFFD58A,0,0,.12,0,0,-.6,.05);
    for(int i=0;i<(minimal?2:4);i++){double a=t+i*Math.PI/2;p.dot(MaterialOption.WIND,0xAFEAD7,Math.cos(a)*.23,Math.sin(a)*.23,-i*.14,.09);p.dot(MaterialOption.EMBER,0xFF863D,Math.cos(a+.7)*.12,Math.sin(a+.7)*.12,-i*.13,.07);}
   }
   case "steam" -> { // Water bead in front; expanding vapor follows the heated wake.
    p.dot(MaterialOption.WATER,0x90D9EB,0,0,.12,.13);
    for(int i=0;i<(minimal?2:3);i++)p.dot(MaterialOption.VAPOUR,0xDFE5DF,Math.sin(t+i)*.12,.04+i*.05,-.13-i*.23,.12+i*.04);
    p.dot(MaterialOption.EMBER,0xFFC586,0,-.05,-.13,.045);
   }
   case "meteor" -> { // An angular stone nucleus sheds hot chips from its leading mantle.
    p.dot(MaterialOption.STONE,0x876456,0,0,0,.2);p.dot(MaterialOption.EMBER,0xFF9B4A,0,0,.14,.15);
    for(int i=0;i<(minimal?1:3);i++)p.dot(MaterialOption.STONE,0xC87844,Math.sin(t+i*2)*.15,Math.cos(t+i*2)*.15,-.25-i*.16,.065);
    p.line(0xFFBF70,0,0,-.1,0,0,-.7,.06);
   }
   case "soulfire" -> { // A hollow blue lantern flame with two separated spectral tails.
    p.dot(MaterialOption.VOID,0x254C65,0,0,0,.13);
    for(int side:new int[]{-1,1}){p.line(0x75E9F0,side*.13,0,.08,side*.08,Math.sin(t+side)*.14,-.45,.03);p.dot(MaterialOption.EMBER,0x64DDE8,side*.1,.08,-.17,.07);}
   }
   case "starfire" -> { // A turning four-point star, with incandescent central fire and a broken stellar wake.
    p.dot(MaterialOption.EMBER,0xFFF1B0,0,0,0,.09);
    for(int i=0;i<4;i++){double a=t*.5+i*Math.PI/2;p.line(0xECC5FF,0,0,0,Math.cos(a)*.24,Math.sin(a)*.24,-.04,.025);}
    p.dot(MaterialOption.ARCANE,0xD9A5FF,Math.sin(t)*.09,Math.cos(t)*.09,-.4,.065);
   }
   case "phoenix_pyre" -> { // Swept feather blades beat around a forward beak, trailing living embers.
    p.line(0xFFF0A4,0,0,.21,0,0,-.26,.035);
    for(int side:new int[]{-1,1})for(int i=0;i<(minimal?1:2);i++)p.line(0xFFB34F,side*.08,0,-.05,side*(.28+i*.1),.08+Math.sin(t)*.1,-.2-i*.16,.028);
    p.dot(MaterialOption.PETAL,0xF5B879,0,.04,-.33,.075);p.dot(MaterialOption.EMBER,0xFF823C,0,-.02,-.12,.08);
   }
   case "flashfire" -> { // A flat heat shutter travels edge first, opening on alternate beats.
    double gap=.06+.03*Math.sin(t);for(int side:new int[]{-1,1})p.line(0xFFF0BD,-.24,side*gap,0,.24,side*gap,-.08,.025);
    p.dot(MaterialOption.EMBER,0xFFDA8A,0,0,.08,.085);
   }
   case "explode" -> { // Four pressure ribs hold a compact charged center.
    for(int i=0;i<4;i++){double a=t*.3+i*Math.PI/2;p.line(0xFFB370,Math.cos(a)*.17,Math.sin(a)*.17,-.18,0,0,.13,.025);}
    p.dot(MaterialOption.EMBER,0xFFD59B,0,0,0,.12);
   }
   case "inferno" -> { // A five-tooth burning crown rides ahead of a broad furnace wake.
    for(int i=-2;i<=2;i++)p.dot(MaterialOption.EMBER,0xFF7A2E,i*.095,.04*Math.cos(t+i),-.08-Math.abs(i)*.09,.09);
    p.line(0xFFC583,-.2,0,-.24,.2,0,-.24,.035);
   }
   case "primer" -> { // The dark charge carries a short pink hooked fuse.
    p.dot(MaterialOption.STONE,0x583044,0,0,0,.13);p.line(0xFF88C9,0,.11,0,.14,.2,-.08,.02);
    p.dot(MaterialOption.EMBER,0xFFADC8,.14,.2,-.08-.025*Math.sin(t),.045);
   }
   case "kindling" -> { // Crossed wood splinters carry one small ignition point.
    p.line(0xA67B4D,-.16,-.1,-.17,.16,.1,.08,.035);p.line(0xA67B4D,.16,-.1,-.17,-.16,.1,.08,.035);
    p.dot(MaterialOption.STONE,0x715238,0,0,-.05,.09);p.dot(MaterialOption.EMBER,0xF3B34F,0,.04*Math.sin(t),.06,.06);
   }
   case "sunscorch" -> { // A solar lens carries short opposed rays that rotate slowly.
    p.dot(MaterialOption.EMBER,0xFFEAB0,0,0,0,.13);
    for(int i=0;i<3;i++){double a=t*.2+i*Math.PI/3;p.line(0xFFE7A7,-Math.cos(a)*.24,-Math.sin(a)*.24,0,Math.cos(a)*.24,Math.sin(a)*.24,0,.018);}
   }
   case "blazecall" -> { // Three detached flame darts orbit a forward calling needle.
    for(int i=0;i<3;i++){double a=t+i*Math.PI*2/3;p.dot(MaterialOption.EMBER,0xFF9540,Math.cos(a)*.18,Math.sin(a)*.18,-.1,.07);}
    p.line(0xFFD08A,0,0,.18,0,0,-.25,.025);
   }
   case "cinderbrand" -> { // An angular stamp remains readable around a glowing central score.
    p.line(0xFF914C,-.16,-.15,0,-.16,.15,0,.018);p.line(0xFF914C,-.16,.15,0,.15,.12,0,.018);p.line(0xFF914C,.15,.12,0,.15,-.15,0,.018);
    p.dot(MaterialOption.EMBER,0xFFBB75,0,0,.03,.055+.01*Math.sin(t));
   }
   case "ashen_veil" -> { // Split ash curtains roll behind an open seam.
    for(int side:new int[]{-1,1})p.dot(MaterialOption.VAPOUR,0x89827B,side*.15,.08*Math.sin(t+side),-.12,.14);
    p.dot(MaterialOption.EMBER,0xD6986A,0,-.06,0,.045);
   }
   case "cinderheart" -> { // Two pulse lobes shelter a heart's forward point.
    for(int side:new int[]{-1,1})p.dot(MaterialOption.EMBER,0xE76C38,side*(.09+.015*Math.sin(t)),.055,0,.1);
    p.line(0xFFCC91,-.09,.055,0,0,-.13,.1,.025);p.line(0xFFCC91,.09,.055,0,0,-.13,.1,.025);
   }
   case "searing_edge" -> { // An incandescent blade bevel stays aligned to the projectile.
    p.line(0xFFDFA7,0,0,.25,0,0,-.42,.025);p.line(0xFF923D,.055,.02,.12,.055,.02,-.34,.018);
    p.dot(MaterialOption.EMBER,0xE57931,-.04,.03*Math.sin(t),-.2,.045);
   }
   case "fireward" -> { // Shield shoulders close toward a traveling lower point.
    p.line(0xFFCF85,-.18,.12,0,0,-.2,0,.02);p.line(0xFFCF85,.18,.12,0,0,-.2,0,.02);p.line(0xFFCF85,-.18,.12,0,.18,.12,0,.02);
    p.dot(MaterialOption.EMBER,0xF4B269,0,.02,.02,.07);
   }
   case "smelt" -> { // A molten droplet moves through a short hot grate.
    for(int i=-1;i<=1;i++)p.line(0xDC8C44,-.15,i*.08,-.05,.15,i*.08,-.05,.015);
    p.dot(MaterialOption.STONE,0xFFC685,0,.015*Math.sin(t),.08,.1);
   }
   case "hellmouth" -> { // Separated burning jaw teeth flank a hollow dark throat.
    p.dot(MaterialOption.VOID,0x392039,0,0,-.1,.16);
    for(int side:new int[]{-1,1})for(int i=0;i<(minimal?1:2);i++)p.dot(MaterialOption.EMBER,0xCA4F63,side*.16,i*.12-.06,.03,.05);
   }
   case "everburn" -> { // The fire follows a broken time fork, turning back on its own wake.
    p.line(0xE2BB73,-.13,.16,0,.13,-.16,-.26,.018);p.line(0xE2BB73,.13,.16,0,-.13,-.16,-.26,.018);
    p.dot(MaterialOption.TIME,0xD9B55B,0,0,-.15,.055);p.dot(MaterialOption.EMBER,0xFF9F47,0,.06*Math.sin(t),.04,.09);
   }
   case "conflagration" -> { // Three linked ignition fronts surround a stronger center.
    for(int i=0;i<3;i++){double a=t*.4+i*Math.PI*2/3;p.dot(MaterialOption.EMBER,0xF96A35,Math.cos(a)*.18,Math.sin(a)*.18,-.12,.08);}
    p.dot(MaterialOption.EMBER,0xFFD8A1,0,0,.05,.14);
   }
   case "seethe" -> { // A water envelope carries boiling heat with escaping vapor at its sides.
    p.dot(MaterialOption.WATER,0x64BADD,0,0,.04,.17);p.dot(MaterialOption.EMBER,0xFFB474,0,0,.1,.065);
    for(int side:new int[]{-1,1})p.dot(MaterialOption.VAPOUR,0xCEE3DF,side*.16,.06*Math.sin(t+side),-.15,.1);
   }
   case "skyburst" -> { // A rising wind fork supports three falling-fire teeth.
    p.dot(MaterialOption.WIND,0xD8EAC4,0,.13,-.12,.13);
    for(int i=-1;i<=1;i++)p.line(0xFFD093,i*.12,.17,0,i*.1,-.09,.12,.02);
    p.dot(MaterialOption.EMBER,0xFF9D3C,0,0,.05,.09);
   }
   case "cinder_bulwark" -> { // Staggered stone courses travel with an ember mortar joint.
    for(int i=-1;i<=1;i++)p.dot(MaterialOption.STONE,0x825D4B,i*.13,i==0?.06:-.03,0,.095);
    p.line(0xFFAE67,-.19,-.03,.04,.19,-.03,.04,.02);p.dot(MaterialOption.EMBER,0xE8944E,0,.05*Math.sin(t),.08,.045);
   }
   case "boiling_surge" -> { // A wet crest curls over the heated leading lip.
    p.dot(MaterialOption.WATER,0x70BFD7,-.1,.08,0,.13);p.dot(MaterialOption.WATER,0x70BFD7,.06,.15,-.1,.11);
    p.dot(MaterialOption.VAPOUR,0xE9D4B1,.15,.12,-.16,.12);p.line(0xFFD59E,-.12,.02,.08,.16,.12,.02,.022);
   }
   default -> { }
  }
 }
 private record Pen(Vec3 head,Vec3 right,Vec3 up,Vec3 forward,double scale,BiConsumer<ParticleOptions,Vec3> emit) {
  Vec3 at(double x,double y,double z){return head.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void dot(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)(size*scale),5),at(x,y,z));}
  void line(int color,double x,double y,double z,double a,double b,double c,double width){var from=at(x,y,z);var d=at(a,b,c).subtract(from);emit.accept(new LightOption(LightOption.RAY,color,(float)d.x,(float)d.y,(float)d.z,(float)(width*scale),0,0,0,5),from);}
 }
}
