package dev.wildercord.client.fx;

import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.LightOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Ice grows edges; water flows. Every built-in frost effect has its own authored preparation. */
final class FrostFormations {
 private FrostFormations() {}
 static final List<String> RUNES=List.of("basinfill","absolute_zero","avalanche","black_ice","blizzard","bubble","chill",
  "coldsnap","cryostasis","current","drowning_word","flash_freeze","freeze","frost","frostbite","frostbloom",
  "frostward","glacier","hail","hoarfrost","icepath","icicle","mirrorfrost","rime_causeway","rime_seal",
  "tidal_lift","tidebreath","tidecall","tidehook","tidewrit","undertow");
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
   case "basinfill" -> { // Four pouring threads descend into a little horizontal vessel.
    for(int i=0;i<4;i++){double x=(i-1.5)*.18;p.path(MaterialOption.WATER,0x55BDDD,new double[][]{{x,.35,0},{x+.06*Math.sin(i+t),.08-.12*t,.08},{x,-.3,.16}});}
    p.path(MaterialOption.WATER,0xB9F3F4,new double[][]{{-.38,-.3,-.12},{.38,-.3,-.12},{.38,-.3,.28},{-.38,-.3,.28},{-.38,-.3,-.12}});
   }
   case "chill" -> { // A breath curls low; a second fine crust gathers along its lower edge.
    p.path(MaterialOption.VAPOUR,0xD8F3FF,new double[][]{{-.48,-.15,0},{-.22,.02,.06*t},{.1,.02,.15*t},{.42,-.08,.2*t}});
    for(int i=0;i<3;i++)p.dot(MaterialOption.FROST,0xB1DFFF,i*.2-.2,-.19-.04*t,0,.055);
   }
   case "frost" -> { // A branching crust grows outward from the contact spine.
    p.edge(0xA9E6FF,0,-.4,0,0,.42*t,0);
    for(int side:new int[]{-1,1})for(int i=0;i<3;i++)p.path(MaterialOption.FROST,0xC7EEFF,new double[][]{{0,i*.2-.2,0},{side*.16*t,i*.2-.08,0},{side*.3*t,i*.2-.02,.04}});
   }
   case "freeze" -> { // Opposed ice plates slide inward and latch around a hollow center.
    double x=.52-.2*t;
    for(int side:new int[]{-1,1})p.path(MaterialOption.FROST,0x86D8FF,new double[][]{{side*x,-.45,-.1},{side*x,.4,-.1},{side*(x-.12),.5,0},{side*(x-.12),-.5,0}});
    p.edge(0xE0FAFF,-x,-.42,0,x,-.42,0);p.edge(0xE0FAFF,-x,.42,0,x,.42,0);
   }
   case "tidebreath" -> { // Two watery lung lobes expand around a central rising breath.
    for(int side:new int[]{-1,1})p.path(MaterialOption.WATER,0x62C9EA,new double[][]{{side*.08,.3,0},{side*.3*t,.15,0},{side*.36*t,-.15,0},{side*.16,-.3,0},{side*.08,.05,0}});
    p.path(MaterialOption.VAPOUR,0xE9FBFF,new double[][]{{0,-.12,0},{0,.16,0},{.08,.36+.1*t,0}});
   }
   case "icepath" -> { // Three flat stepping lozenges crystallize in the direction of travel.
    for(int i=0;i<3;i++){double z=i*.3-.3,y=-.25+i*.025;p.path(MaterialOption.FROST,0xA8E5F3,new double[][]{{-.22,y,z},{0,y,z-.16*t},{.22,y,z},{0,y,z+.16*t},{-.22,y,z}});}
   }
   case "bubble" -> { // A wet membrane rounds out in two perpendicular seams, leaving air inside.
    double r=.24+.18*t;p.arc(MaterialOption.WATER,0x65CBE8,r,0,Math.PI*2,0);
    for(int i=0;i<6;i++){double a=i*Math.PI/3;p.dot(MaterialOption.WATER,0xDDFBFF,0,Math.sin(a)*r,Math.cos(a)*r,.055);}
    p.dot(MaterialOption.VAPOUR,0xF4FFFF,-r*.4,r*.35,-.05,.065);
   }
   case "frostward" -> { // A warm opening is sheltered by an icy hood, never sealed shut.
    p.path(MaterialOption.FROST,0xACE8FF,new double[][]{{-.4,-.28,0},{-.42,.12,0},{-.2,.4,0},{.2,.4,0},{.42,.12,0},{.4,-.28,0}});
    p.path(MaterialOption.VAPOUR,0xFFF2C7,new double[][]{{-.22,-.3,0},{0,-.15+.08*t,0},{.22,-.3,0}});
   }
   case "icicle" -> { // Three facets weld a long forward needle with a dripping base.
    for(int i=0;i<3;i++){double a=i*Math.PI*2/3;p.edge(0xD4F6FF,Math.cos(a)*.18,Math.sin(a)*.18,-.32,0,0,.35+.22*t);}
    p.dot(MaterialOption.FROST,0xA9DFFF,0,0,.35+.22*t,.085);p.dot(MaterialOption.WATER,0x5DCBFF,0,-.18,-.25,.065);
   }
   case "coldsnap" -> { // Two brittle fans spring apart across a sharp central fracture.
    for(int side:new int[]{-1,1})for(int i=-1;i<=1;i++)p.path(MaterialOption.FROST,0xDBF5FF,new double[][]{{0,-.08,0},{side*.2*t,i*.2,0},{side*.5*t,i*.35,.08}});
    p.edge(0x76D7FF,0,-.4,0,0,.4,0);
   }
   case "mirrorfrost" -> { // Four glints plane a mirror; an incoming ray turns back off its face.
    p.path(MaterialOption.FROST,0xB6D4FF,new double[][]{{0,.45,0},{.34,0,0},{0,-.45,0},{-.34,0,0},{0,.45,0}});
    p.edge(0xFFFFFF,-.55,.22,-.25,0,0,0);p.edge(0x89DEFF,0,0,0,-.3-.2*t,-.22,-.25);
    p.dot(MaterialOption.FROST,0xFFFFFF,.12,.16,0,.09);
   }
   case "hail" -> { // Five pellets queue above the release, descending on staggered beats.
    for(int i=0;i<5;i++){double x=(i-2)*.2,y=.42-.1*(i%2)-.18*t;p.dot(MaterialOption.FROST,0xE4F4FF,x,y,0,.09);p.edge(0x9ED8FF,x,y+.12,0,x,y-.12*t,.06);}
    p.path(MaterialOption.STORM,0xBBE9FF,new double[][]{{-.42,.1,0},{-.16,.18,.04},{0,.04,0},{.18,.16,.04},{.42,.1,0}});
   }
   case "glacier" -> { // Three columns lock together, suggesting ice spreading to nearby foes.
    for(int i=-1;i<=1;i++){double x=i*.26,h=.25+(1-Math.abs(i))*.25*t;p.path(MaterialOption.FROST,0x83C5EB,new double[][]{{x-.08,-.35,0},{x-.08,h,0},{x+.08,h+.08,0},{x+.08,-.35,0}});}
    p.edge(0xD5F2FF,-.4,-.2,0,.4,-.2,0);
    for(int i=-1;i<=1;i++)p.dot(MaterialOption.STONE,0x7C929E,i*.26,-.36,0,.1);
   }
   case "blizzard" -> { // Snow lanes spiral through a crosswind, flowing instead of closing a seal.
    for(int lane=0;lane<2;lane++)for(int i=0;i<6;i++){double a=i*.8+t+lane*Math.PI,y=i*.13-.35;p.dot(MaterialOption.FROST,0xEAF9FF,Math.cos(a)*.4,y,Math.sin(a)*.15,.065);}
    p.path(MaterialOption.WIND,0xA4CDDE,new double[][]{{-.55,-.2,-.1},{-.2,.2,0},{.25,.1,.08},{.55,-.15,.2*t}});
   }
   case "frostbloom" -> { // Ice petals unfurl around a living green bud.
    for(int i=0;i<5;i++){double a=i*Math.PI*2/5,x=Math.cos(a),y=Math.sin(a);p.path(MaterialOption.FROST,0xA5E7FF,new double[][]{{0,0,0},{x*.2,y*.2,-.1},{x*(.3+.12*t),y*(.3+.12*t),0},{x*.2,y*.2,.1},{0,0,0}});}
    p.dot(MaterialOption.PETAL,0x93D892,0,0,.08,.12);
   }
   case "black_ice" -> { // Dark angular facets crack along a pale cutting edge.
    p.path(MaterialOption.VOID,0x302846,new double[][]{{-.4,-.3,0},{-.3,.32,0},{.04,.46,0},{.36,.12,0},{.3,-.4,0},{-.4,-.3,0}});
    p.edge(0xADB9FF,-.26,-.25,0,.2,.3,0);p.path(MaterialOption.FROST,0xA3C6EF,new double[][]{{.04,.46,0},{.18+.1*t,.12,0},{.3,-.4,0}});
   }
   case "rime_seal" -> { // A floor lattice grows forked teeth around a waiting center.
    for(int i=0;i<4;i++){double a=i*Math.PI/2,x=Math.cos(a),z=Math.sin(a);p.path(MaterialOption.FROST,0xC6EEFF,new double[][]{{x*.12,-.3,z*.12},{x*.45*t,-.3,z*.45*t},{x*.4-z*.12,-.3,z*.4+x*.12}});}
    p.edge(0x89D0ED,-.28,-.3,-.28,.28,-.3,.28);p.edge(0x89D0ED,.28,-.3,-.28,-.28,-.3,.28);
    p.dot(MaterialOption.ARCANE,0xC9B4FF,0,-.28,.04*t,.07);
   }
   case "cryostasis" -> { // A protective capsule closes around a mending pulse and paused time.
    for(int side:new int[]{-1,1})p.path(MaterialOption.FROST,0xBCDFFF,new double[][]{{0,.5,0},{side*(.4-.12*t),.3,0},{side*(.4-.12*t),-.3,0},{0,-.5,0}});
    p.edge(0xEACB87,-.12,-.14,0,-.12,.14,0);p.edge(0xEACB87,.12,-.14,0,.12,.14,0);p.dot(MaterialOption.PETAL,0xA9DBAC,0,-.28,0,.07+.04*t);
    p.dot(MaterialOption.TIME,0xEACB87,-.12,.2-.12*t,0,.05);p.dot(MaterialOption.TIME,0xEACB87,.12,-.2+.12*t,0,.05);
   }
   case "frostbite" -> { // Opposed teeth creep inward in three staggered pairs.
    for(int i=0;i<3;i++)for(int side:new int[]{-1,1})p.path(MaterialOption.FROST,0x90C4EF,new double[][]{{side*.4,i*.22-.22,0},{side*(.22-.07*t),i*.22-.14,0},{side*.4,i*.22-.06,0}});
    p.dot(MaterialOption.BLOOD,0xB24465,0,.12*t,0,.08);p.dot(MaterialOption.FROST,0xF5CFDC,0,-.12*t,0,.06);
   }
   case "absolute_zero" -> { // Four cold fronts pinch a dark, motionless point.
    for(int i=0;i<4;i++){double a=i*Math.PI/2,x=Math.cos(a),y=Math.sin(a);p.path(MaterialOption.FROST,0x567FE5,new double[][]{{x*.6,y*.6,0},{x*.32,y*.32,0},{x*(.23-.12*t),y*(.23-.12*t),0}});}
    p.dot(MaterialOption.FROST,0x182B5E,0,0,0,.1);p.edge(0xD5E8FF,-.16,.16,0,.16,-.16,0);
   }
   case "tidal_lift" -> { // Three borrowed source beads rise along a curved aqueduct.
    p.path(MaterialOption.WATER,0x69BAE8,new double[][]{{-.6,-.35,0},{-.35,.2,0},{0,.45,.1},{.35,.2,.2},{.6,-.35,.3}});
    for(int i=0;i<3;i++){double x=i*.23-.3+.14*t;p.dot(MaterialOption.WATER,0xD0F7FF,x,.35-x*x,0,.11);}
   }
   case "rime_causeway" -> { // Wind supports three ascending courses of temporary ice.
    for(int i=0;i<3;i++)p.path(MaterialOption.FROST,0xADDCED,new double[][]{{-.3,i*.18-.35,i*.25-.3},{.3,i*.18-.35,i*.25-.3},{.3,i*.18-.3,i*.25-.12},{-.3,i*.18-.3,i*.25-.12}});
    p.path(MaterialOption.WIND,0xD6F5F9,new double[][]{{-.45,-.45,0},{0,-.3+.08*t,.1},{.45,-.15,.3}});
   }
   case "avalanche" -> { // Hanging slabs break into snow and rock before descending.
    for(int i=-1;i<=1;i++){double y=.5-Math.abs(i)*.12-.22*t;p.dot(MaterialOption.STONE,0x778999,i*.3,y,0,.13);p.path(MaterialOption.FROST,0xF1F8FF,new double[][]{{i*.3-.12,y+.08,0},{i*.3,y+.18,0},{i*.3+.12,y+.08,0}});}
    p.path(MaterialOption.VAPOUR,0xD8E5EA,new double[][]{{-.45,-.2,0},{0,-.1-.12*t,0},{.45,-.2,0}});
   }
   case "tidecall" -> { // Opposed breakers curl inward to bunch the crowd at their meeting.
    for(int side:new int[]{-1,1})p.path(MaterialOption.WATER,0x56B9E0,new double[][]{{side*.6,-.3,0},{side*.48,.16,0},{side*.24,.4,0},{side*(.2-.12*t),.1,.12},{0,-.16,.2}});
    p.dot(MaterialOption.VAPOUR,0xE7FBFF,0,.1,.1,.09);
   }
   case "undertow" -> { // A downward current twists into dark sediment at its foot.
    for(int i=0;i<8;i++){double a=i*.65+t;p.dot(MaterialOption.WATER,0x4BA0D0,Math.cos(a)*.3,.45-i*.1,Math.sin(a)*.14,.07);}
    p.path(MaterialOption.STONE,0x8A8376,new double[][]{{-.3,-.4,0},{0,-.48,0},{.3,-.4,0}});
   }
   case "hoarfrost" -> { // A fern of creeping rime grows forks toward its delayed freeze.
    p.path(MaterialOption.FROST,0xD7ECEF,new double[][]{{-.35,-.4,0},{-.1,-.15,0},{.08,.1,0},{.25,.32+.1*t,0}});
    for(int i=0;i<3;i++){double x=i*.18-.22,y=i*.22-.2;p.edge(0xA8DDE9,x,y,0,x-.18*t,y+.12,0);p.dot(MaterialOption.FROST,0xF5FEFF,x+.13*t,y-.03,0,.06);}
   }
   case "drowning_word" -> { // A broken breath divides into inward-filling water channels.
    for(int side:new int[]{-1,1})p.path(MaterialOption.WATER,0x267FBC,new double[][]{{side*.38,.3,0},{side*.22,.1,0},{side*.22,-.3,0},{side*.08,-.15,0}});
    for(int i=0;i<3;i++)p.dot(MaterialOption.WATER,0x76CBEF,0,.28-i*.2-.12*t,0,.065);p.edge(0xB7D7EB,-.15,.45,0,.15,.45,0);
   }
   case "tidewrit" -> { // Three water courses rise into a travelling wall with a folding lip.
    for(int row=0;row<3;row++)p.path(MaterialOption.WATER,0x4EBDD9,new double[][]{{-.6,row*.22-.3,0},{-.2,row*.22-.28,.06*t},{.2,row*.22-.28,.12*t},{.6,row*.22-.3,.18*t}});
    p.path(MaterialOption.VAPOUR,0xDEF5FF,new double[][]{{-.6,.4,0},{0,.5,.1},{.6,.4,.18*t}});
   }
   case "tidehook" -> { // A wet hook casts outward while its tether coils behind it.
    p.path(MaterialOption.WATER,0x55BEDD,new double[][]{{-.5,-.25,-.15},{-.15,-.05,0},{.3,.28,.15*t},{.45,.1,.22*t},{.28,-.1,.22*t},{.14,.04,.15*t}});
    p.arc(MaterialOption.WATER,0xA0E1EE,.16,Math.PI/2,Math.PI*1.8,-.25);
   }
   case "current" -> { // Three flowing lanes align into a shared forward thrust.
    for(int i=-1;i<=1;i++)p.path(MaterialOption.WATER,0x7EDCE9,new double[][]{{i*.24,-.2,-.5},{i*.22,-.08,-.1},{i*.18,0,.28*t}});
    p.edge(0xE0F8FF,-.22,0,.1,0,.12,.35*t);p.edge(0xE0F8FF,.22,0,.1,0,.12,.35*t);
   }
   case "flash_freeze" -> { // Wet beads close between opposed frost blades and become crystal.
    for(int i=-1;i<=1;i++)p.dot(beat==1?MaterialOption.WATER:MaterialOption.FROST,beat==1?0x60C5E9:0xD6F6FF,i*.2,0,0,.09);
    double y=.4-.18*t;p.edge(0xA9E8FF,-.4,-y,0,.4,-y,0);p.edge(0xA9E8FF,-.4,y,0,.4,y,0);
   }
   default -> { }
  }
 }
 private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void dot(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)Math.clamp(size*scale,.02,.8),8),at(x,y,z));}
  void edge(int color,double x,double y,double z,double a,double b,double c){var start=at(x,y,z);var d=at(a,b,c).subtract(start);emit.accept(new LightOption(LightOption.RAY,color,(float)d.x,(float)d.y,(float)d.z,.012F,0,0,0,8),start);}
  void path(int style,int color,double[][] points){for(int i=0;i<points.length;i++){var q=points[i];dot(style,color,q[0],q[1],q[2],.075);if(!minimal && i>0){var prev=points[i-1];dot(style,color,(prev[0]+q[0])*.5,(prev[1]+q[1])*.5,(prev[2]+q[2])*.5,.055);}}}
  void arc(int style,int color,double radius,double begin,double end,double z){int n=minimal?6:12;for(int i=0;i<n;i++){double a=begin+(end-begin)*i/n;dot(style,color,Math.cos(a)*radius,Math.sin(a)*radius,z,.06);}}
 }
}
