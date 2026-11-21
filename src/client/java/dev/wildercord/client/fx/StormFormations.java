package dev.wildercord.client.fx;

import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.LightOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Authored storm preparations: conductor paths, clamps, magnetic filings, sunlight and charged constructs. */
final class StormFormations {
 private StormFormations() {}
 static final List<String> RUNES=List.of("frostwire","galvanize","jolt","lightning","magnetize","plasma","riftbolt",
  "ripple","shock","stormclock","stormheart","stormweave","surge","tempest","thunder_tide","thunder_walk",
  "thunderbird","thunderclap","thunderhead","thunderstep");
 static boolean supports(String id){return id.startsWith("wildercord:") && RUNES.contains(id.substring(11));}
 static boolean draw(SpellFormations.Canvas c,int beat) {
  boolean authored=false;
  for(String id:c.event.runes())if(supports(id)) {
   authored=true;draw(id.substring(11),beat,c.event.scale(),c.assembly(),c.right,c.up,c.forward,
    c.quality==MagicQuality.Level.MINIMAL,c::emit);
  }
  return authored;
 }
 static void draw(String rune,int beat,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,
                  boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  double t=beat/2.0;
  var p=new Pen(anchor,right,up,forward,scale,minimal,emit);
  switch(rune) {
   case "shock" -> { // A conductor forks toward two unequal destinations; the nearer node charges first.
    p.path(MaterialOption.STORM,0xFFF6C6,new double[][]{{-.48,-.3,-.1},{-.25,-.04,0},{-.07,-.12,0},{.1,.16,.14*t},{.4,.28,.25*t}});
    p.arc(0xD2ECFF,-.07,-.12,0,.25,-.27,.14*t);p.dot(MaterialOption.STORM,0xFFFFFF,.4,.28,.25*t,.1);
   }
   case "jolt" -> { // A narrow interruption clamp snaps shut, cutting the conducting path.
    double x=.42-.15*t;
    for(int side:new int[]{-1,1})p.path(MaterialOption.STORM,0xB4D5FF,new double[][]{{side*x,.36,0},{side*x,-.26,0},{side*(x-.14),-.26,0}});
    p.arc(0xD6EAFF,-x,.08,0,x,.08,0);p.dot(MaterialOption.STORM,0x8EB6F2,0,-.12*t,0,.08);
   }
   case "lightning" -> { // A descending leader and two rising branches meet at the strike point.
    p.path(MaterialOption.STORM,0xFFFBE0,new double[][]{{.2,.68,0},{-.06,.37,0},{.12,.26,0},{0,-.04-.12*t,.1}});
    for(int side:new int[]{-1,1})p.arc(0xD8E7FF,side*.36,-.4,0,0,-.04-.12*t,.1);
    p.dot(MaterialOption.STORM,0xFFFFFF,0,-.04-.12*t,.1,.12);
   }
   case "thunderclap" -> { // Split pressure jaws widen around the flash that precedes the crack.
    for(int side:new int[]{-1,1})p.path(MaterialOption.STORM,0xD4E4FF,new double[][]{{side*.16,-.42,0},{side*(.24+.15*t),-.15,0},{side*(.3+.15*t),.18,0},{side*.16,.4,0}});
    p.ray(0xFFFBE7,-.22,.08,0,.22,.08,0);p.dot(MaterialOption.STORM,0xFFFFFF,0,0,.12*t,.15);
   }
   case "ripple" -> { // Sunlight opens a small fan; the healing answer rises warm and quiet.
    for(int i=0;i<5;i++){double a=Math.PI*(.1+i*.2),r=.28+.14*t;p.ray(0xFFDC77,Math.cos(a)*.16,Math.sin(a)*.16,0,Math.cos(a)*r,Math.sin(a)*r,0);}
    p.path(MaterialOption.ARCANE,0xFFE6A0,new double[][]{{-.35,-.22,0},{-.1,-.14,0},{.1,-.14,0},{.35,-.22,0}});
    p.dot(MaterialOption.ARCANE,0xFFF4CA,0,.06*t,0,.09);
   }
   case "thunderbird" -> { // A folded bird rises; three feather forks spread from each shoulder.
    for(int side:new int[]{-1,1})for(int i=0;i<3;i++)p.path(MaterialOption.STORM,0xD4E9FF,new double[][]{{side*.08,-.02,0},{side*(.25+i*.12),.16+i*.09,0},{side*(.35+i*.12)*t,.28+i*.08,.08}});
    p.path(MaterialOption.STORM,0xFFF4B8,new double[][]{{0,-.28,0},{0,.08,0},{.06,.26+.08*t,.1}});
    p.arc(0xC7E8FF,-.1,-.23,0,.1,-.23,0);
   }
   case "stormheart" -> { // Opposed heart lobes gather a charge behind a serrated guard.
    p.path(MaterialOption.STORM,0x9DBFFF,new double[][]{{0,-.42,0},{-.34,-.02,0},{-.26,.25,0},{0,.08,0},{.26,.25,0},{.34,-.02,0},{0,-.42,0}});
    p.arc(0xE1EFFF,-.16,.06,0,.16,.06,.1*t);p.dot(MaterialOption.STORM,0xFFFFFF,0,-.12,0,.07+.04*t);
   }
   case "galvanize" -> { // A copper contact receives a red power pulse across three terminal prongs.
    p.path(MaterialOption.STONE,0xB36D4A,new double[][]{{-.3,-.28,0},{.3,-.28,0},{.3,.08,0},{-.3,.08,0},{-.3,-.28,0}});
    for(int i=-1;i<=1;i++)p.path(MaterialOption.STORM,0xFF5B35,new double[][]{{i*.2,-.06,0},{i*.2,.18,0},{i*.2+.08*t,.36,0}});
    p.ray(0xFFB185,-.28,-.08,0,.28,-.08,0);
   }
   case "tempest" -> { // Wind sweeps a charge outward; a bent return leader waits on the far side.
    p.path(MaterialOption.WIND,0xC6F4DE,new double[][]{{-.55,-.25,0},{-.36,.18,0},{0,.35,0},{.36,.18,.12*t},{.55,-.25,.25*t}});
    p.arc(0xE3F5FF,-.22,.38,0,.12,-.3,.08);p.path(MaterialOption.STORM,0xBCE5CF,new double[][]{{.55,-.25,.25*t},{.3,-.08,.12},{.35,.25,.1}});
   }
   case "plasma" -> { // Opposed electrical rails heat a fire core in a violet sheath.
    for(int side:new int[]{-1,1})p.path(MaterialOption.STORM,0xCD93FA,new double[][]{{side*.22,-.4,-.1},{side*(.25-.1*t),0,0},{side*.12,.45,.18*t}});
    p.path(MaterialOption.EMBER,0xFFB4D8,new double[][]{{0,-.32,-.08},{-.06,-.04,0},{.04,.24,.12*t}});
    p.ray(0xFFF0FC,0,-.32,-.08,0,.36,.12*t);
   }
   case "surge" -> { // A living stem carries current to two reaching hands.
    p.path(MaterialOption.PETAL,0x70D684,new double[][]{{0,-.4,0},{0,-.12,0},{-.16,.08,0},{-.3,.3,.1*t}});
    p.path(MaterialOption.STORM,0xBCF58F,new double[][]{{0,-.12,0},{.15,.12,0},{.3,.3,.1*t}});
    for(int side:new int[]{-1,1})p.dot(MaterialOption.STORM,0xF4FFE0,side*.3,.3+.06*t,.1,.08);
   }
   case "magnetize" -> { // Filings draw toward an iron core between opposed magnetic field loops.
    for(int side:new int[]{-1,1})p.path(MaterialOption.STORM,0xE7C678,new double[][]{{side*.16,-.25,0},{side*.44,-.3,0},{side*.53,.04,0},{side*.32,.34,0},{side*.12,.25,0}});
    for(int i=-1;i<=1;i++)p.dot(MaterialOption.STONE,0x9298A1,i*(.35-.2*t),-.04,0,.075);
    p.ray(0xFFE8AB,-.12,-.23,0,.12,.23,0);
   }
   case "riftbolt" -> { // A dark vertical tear parts, its jagged edges carrying the bolt.
    for(int side:new int[]{-1,1})p.path(MaterialOption.VOID,0x51326E,new double[][]{{side*.08,-.48,0},{side*(.1+.14*t),-.12,0},{side*.06,.16,0},{side*.12,.5,0}});
    p.path(MaterialOption.STORM,0xC68AF3,new double[][]{{-.12,-.4,0},{.06,-.17,0},{-.04,.03,0},{.15,.25,.1*t}});
    p.arc(0xE1C5FF,-.4,.02,0,0,.03,.1);
   }
   case "stormweave" -> { // Four marked nodes thread a crossing web, tightening into an electrical stitch.
    double r=.44-.08*t;
    for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2,b=a+Math.PI;p.dot(MaterialOption.ARCANE,0xE99FD9,Math.cos(a)*r,Math.sin(a)*r,0,.09);p.arc(0xEDB6F3,Math.cos(a)*r,Math.sin(a)*r,0,Math.cos(b)*r,Math.sin(b)*r,.1);}
    p.dot(MaterialOption.STORM,0xFFF0FF,0,0,.1,.09);
   }
   case "stormclock" -> { // Three appointment ticks wait while a lightning hand advances toward the next strike.
    for(int i=0;i<3;i++){double a=-Math.PI/2+i*Math.PI*2/3;p.dot(MaterialOption.TIME,0xE9C96F,Math.cos(a)*.4,Math.sin(a)*.4,0,.08);}
    double a=-Math.PI/2+t*Math.PI*2/3;p.arc(0xFFF0B0,0,0,0,Math.cos(a)*.35,Math.sin(a)*.35,0);
    p.path(MaterialOption.STORM,0xD5E2FF,new double[][]{{-.12,-.45,0},{0,-.24,0},{-.05,-.07,0},{0,0,0}});
   }
   case "thunderhead" -> { // Heavy cloud lobes gather over soaked rain lanes and a flickering leader.
    for(int i=-1;i<=1;i++)p.dot(MaterialOption.VAPOUR,i==0?0x687889:0x9BABBB,i*.28,.4+.08*(1-Math.abs(i)),0,.16);
    for(int i=-1;i<=1;i++)p.dot(MaterialOption.WATER,0xA0CDE7,i*.25,.14-.16*t,0,.065);
    p.path(MaterialOption.STORM,0xE9F2FF,new double[][]{{.08,.34,0},{-.12,.15,0},{.06,.03,0},{-.02,-.25*t,0}});
   }
   case "frostwire" -> { // Crystalline conductor nodes are joined by a narrow running current.
    for(int i=0;i<4;i++)p.dot(MaterialOption.FROST,0xA5E5FF,i*.25-.375,(i%2==0?-.12:.12),0,.085);
    p.path(MaterialOption.STORM,0xBEEBFF,new double[][]{{-.375,-.12,0},{-.125,.12,0},{.125,-.12,0},{.375,.12,.08*t}});
    p.arc(0xECFCFF,-.375,-.12,0,-.375+.5*t,-.12,0);
   }
   case "thunderstep" -> { // An empty footfall opens ahead of a broken bolt; arrival gathers at its toe.
    p.path(MaterialOption.STORM,0xD6E8FF,new double[][]{{-.35,.48,-.1},{-.1,.26,0},{-.22,.1,0},{.15,-.12,.18*t}});
    p.path(MaterialOption.STORM,0xFFF5BF,new double[][]{{-.1,-.38,-.15},{.2,-.38,-.15},{.3,-.38,.15},{0,-.38,.15},{-.1,-.38,-.15}});
    p.arc(0xFFFFFF,.15,-.12,.18*t,.15,-.38,.15);
   }
   case "thunder_tide" -> { // Electrical hoops suspend real-water beads before driving them forward.
    for(int i=0;i<3;i++){double x=(i-1)*.28;p.dot(MaterialOption.WATER,0x6FCDE4,x,-.02,.18*t,.12);p.path(MaterialOption.STORM,0xD6F6F4,new double[][]{{x-.1,-.24,0},{x-.13,.16,0},{x+.13,.16,0},{x+.1,-.24,.18*t}});}
    p.arc(0xE5FFFF,-.5,-.32,0,.5,-.32,.18*t);
   }
   case "thunder_walk" -> { // Five copper-lit footings align while wind holds them and charge joins the tops.
    for(int i=0;i<5;i++){double x=(i-2)*.21,y=i*.07-.28,z=i*.12-.2;p.dot(MaterialOption.STONE,0xC98358,x,y,z,.085);p.dot(MaterialOption.STORM,0xFFF2B5,x,y+.08,z+.08*t,.055);}
    p.path(MaterialOption.WIND,0xC5F4DF,new double[][]{{-.48,-.4,-.2},{0,-.25,0},{.48,-.08,.28*t}});
    p.arc(0xE7FFEE,-.42,-.17,-.2,.42,.11,.28*t);
   }
   default -> { }
  }
 }
 private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void dot(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)Math.clamp(size*scale,.02,.8),8),at(x,y,z));}
  void ray(int color,double x,double y,double z,double a,double b,double c){stroke(LightOption.RAY,color,x,y,z,a,b,c);}
  void arc(int color,double x,double y,double z,double a,double b,double c){stroke(LightOption.ARC,color,x,y,z,a,b,c);}
  void stroke(int kind,int color,double x,double y,double z,double a,double b,double c){var start=at(x,y,z);var d=at(a,b,c).subtract(start);emit.accept(new LightOption(kind,color,(float)d.x,(float)d.y,(float)d.z,.014F,kind==LightOption.ARC?1:0,0,kind==LightOption.ARC?.35F:0,8),start);}
  void path(int style,int color,double[][] points){for(int i=0;i<points.length;i++){var q=points[i];dot(style,color,q[0],q[1],q[2],.075);if(!minimal && i>0){var prev=points[i-1];dot(style,color,(prev[0]+q[0])*.5,(prev[1]+q[1])*.5,(prev[2]+q[2])*.5,.055);}}}
 }
}
