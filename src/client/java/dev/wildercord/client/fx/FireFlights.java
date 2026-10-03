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
 static final List<String> RUNES=List.of("ember","fire","firestorm","steam","meteor","soulfire","starfire","phoenix_pyre");
 static boolean supports(String id){return id.startsWith("wildercord:") && RUNES.contains(id.substring(11));}
 static void draw(String id,int age,double scale,Vec3 head,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  if(!supports(id) || !Double.isFinite(velocity.lengthSqr()))return;
  var forward=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();
  var right=forward.cross(new Vec3(0,1,0));if(right.lengthSqr()<.0001)right=new Vec3(1,0,0);else right=right.normalize();
  var p=new Pen(head,right,right.cross(forward).normalize(),forward,Math.clamp(scale,.4,2),emit);
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
    if(!minimal)p.dot(MaterialOption.ARCANE,0xD9A5FF,Math.sin(t)*.09,Math.cos(t)*.09,-.4,.065);
   }
   case "phoenix_pyre" -> { // Swept feather blades beat around a forward beak, trailing living embers.
    p.line(0xFFF0A4,0,0,.21,0,0,-.26,.035);
    for(int side:new int[]{-1,1})for(int i=0;i<(minimal?1:2);i++)p.line(0xFFB34F,side*.08,0,-.05,side*(.28+i*.1),.08+Math.sin(t)*.1,-.2-i*.16,.028);
    p.dot(MaterialOption.PETAL,0xF5B879,0,.04,-.33,.075);p.dot(MaterialOption.EMBER,0xFF823C,0,-.02,-.12,.08);
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
