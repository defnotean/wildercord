package dev.wildercord.client.fx;

import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.LightOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Authored fire preparations. Geometry utilities are shared; each rune chooses its own sequence. */
final class FireFormations {
 private FireFormations() {}
 static final List<String> RUNES=List.of("ember","fire","flashfire","explode","meteor","inferno","primer","kindling",
  "firestorm","steam","sunscorch","soulfire","blazecall","cinderbrand","ashen_veil","cinderheart","searing_edge","fireward",
  "smelt","hellmouth","starfire","everburn","conflagration","phoenix_pyre","bloodboil","seethe","skyburst","cinder_bulwark","boiling_surge","cinder_sieve");
 // Bloodboil belongs to blood in the runtime roster; its heat preparation is also authored here.
 static boolean supports(String id) { return id.startsWith("wildercord:") && RUNES.contains(id.substring(11)); }
 static boolean draw(SpellFormations.Canvas c,int beat) {
  boolean authored=false;
  for(String id:c.event.runes()) if(supports(id)) {
   authored=true;
   draw(id.substring(11),beat,c.event.scale(),c.assembly(),c.right,c.up,c.forward,
    c.quality==MagicQuality.Level.MINIMAL,c::emit);
  }
  return authored;
 }
 /** Emit local geometry through the existing bounded canvas. No effects or sounds execute here. */
 static void draw(String rune,int beat,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,
                  boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  if(FieldFusionForms.prepare("wildercord:"+rune,beat,scale,anchor,right,up,forward,minimal,emit))return;
  double t=beat/2.0;
  var p=new Pen(anchor,right,up,forward,scale*(1.16-.16*t),minimal,emit);
  switch(rune) {
   case "ember" -> { // A hooked spark draws in and lodges as one bright cinder.
    p.curve(MaterialOption.EMBER,0xFFB540,new double[][]{{-.62,.25,-.25},{-.35,.45,-.15},{-.1,.2,0},{0,0,.12*t}});
    p.dot(MaterialOption.EMBER,0xFFD060,0,0,.12*t,.16);
   }
   case "fire" -> { // Three tongues climb, then lean into the direction of release.
    for(int i=-1;i<=1;i++) p.curve(MaterialOption.EMBER,0xFF7930,new double[][]{{i*.22,-.4,0},{i*.26,0,.08*t},{i*.12,.42,.22*t}});
   }
   case "flashfire" -> { // A flat shutter snaps inward into a white-hot slit.
    double q=.62*(1.3-t);p.stroke(0xFFF3D0,-q,-.16,0,q,-.16,0);p.stroke(0xFFD060,-q,.16,0,q,.16,0);
    p.dot(MaterialOption.EMBER,0xFFFFFF,0,0,0,.08+.06*t);
   }
   case "explode" -> { // Four sparks converge on a compressed heart; pressure ribs close.
    for(int i=0;i<4;i++){double a=i*Math.PI/2;p.stroke(0xFFAF40,Math.cos(a)*.55,Math.sin(a)*.55,-.15,0,0,.15);}
    p.arc(MaterialOption.EMBER,0xFF5A20,.28*(1.4-t),0,Math.PI*2,0);p.dot(MaterialOption.EMBER,0xFFFFFF,0,0,.15,.13);
   }
   case "meteor" -> { // Fragments weld into a rock with a descending diagonal tail.
    for(int i=0;i<5;i++){double a=i*2.4;p.dot(MaterialOption.STONE,0x5A4234,Math.cos(a)*.2,Math.sin(a)*.2,.08,.13);}
    p.curve(MaterialOption.EMBER,0xFF6524,new double[][]{{-.7,.85,-.45},{-.45,.55,-.3},{-.2,.25,-.1},{0,0,.1}});
   }
   case "inferno" -> { // A serrated crown rises, then closes its burning teeth.
    for(int i=-2;i<=2;i++)p.curve(MaterialOption.EMBER,0xFF7A20,new double[][]{{i*.23,-.3,0},{i*.23,.28+(.12*(2-Math.abs(i))),0},{i*.12,.12,.2*t}});
   }
   case "primer" -> { // A hooked pink fuse shortens toward a suspended charge.
    p.curve(MaterialOption.EMBER,0xFF6EC7,new double[][]{{-.6,.45,0},{-.3,.45,0},{-.3,.12,0},{0,.12,0}});
    p.dot(MaterialOption.EMBER,0xFFD060,-.6+.3*t,.45,0,.1);p.dot(MaterialOption.STONE,0x583044,0,0,0,.16);
   }
   case "kindling" -> { // Crossed splinters catch at their intersection.
    p.curve(MaterialOption.STONE,0x856347,new double[][]{{-.3,-.25,0},{.3,.2,0}});
    p.curve(MaterialOption.STONE,0x856347,new double[][]{{.3,-.25,0},{-.3,.2,0}});
    p.curve(MaterialOption.EMBER,0xFFD060,new double[][]{{0,-.1,0},{-.08,.15,0},{.05,.35*t,0}});
   }
   case "firestorm" -> { // Wind ribbons braid around a rising flame column.
    p.helix(MaterialOption.WIND,0xD7EFFF,.44,.85,t*1.1);
    p.curve(MaterialOption.EMBER,0xFF7030,new double[][]{{0,-.45,0},{-.08,0,.05},{.08,.4,.12}});
   }
   case "steam" -> { // Two wet lobes close and vent vapor through their seam.
    for(int side:new int[]{-1,1})p.curve(MaterialOption.WATER,0x8BCBDD,new double[][]{{side*.42,-.2,0},{side*.25,.15,0},{side*.08,.2,0}});
    p.curve(MaterialOption.VAPOUR,0xE6FAFF,new double[][]{{0,.1,0},{-.08,.32,0},{.12,.6,.1*t}});
   }
   case "sunscorch" -> { // Short rays fold toward a small solar core.
    for(int i=0;i<6;i++){double a=i*Math.PI/3;p.stroke(0xFFF3D0,Math.cos(a)*.65,Math.sin(a)*.65,0,Math.cos(a)*.3,Math.sin(a)*.3,0);}
    p.dot(MaterialOption.EMBER,0xFFE7A0,0,0,0,.18);
   }
   case "soulfire" -> { // Blue wisps twist against a contracting dark wake.
    p.curve(MaterialOption.EMBER,0x5AD8E6,new double[][]{{-.2,-.45,0},{.1,-.15,.08},{-.12,.18,0},{0,.5,.12}});
    p.curve(MaterialOption.VOID,0x3B284E,new double[][]{{.3,-.3,-.1},{.22,.05,-.1},{0,.28,0}});
   }
   case "blazecall" -> { // Three hooked tongues gather around a spear of flame.
    for(int i=-1;i<=1;i++)p.curve(MaterialOption.EMBER,0xFF8030,new double[][]{{i*.4,-.32,-.2},{i*.25,.2,0},{0,.05,.4*t}});
   }
   case "cinderbrand" -> { // An open angular brand closes into a hot mark.
    p.curve(MaterialOption.EMBER,0xFF7040,new double[][]{{-.35,-.3,0},{-.35,.3,0},{.2,.3,0},{.35,0,0},{.1,-.3,0},{-.35,-.3,0}});
    p.stroke(0xFFD060,0,-.18,0,0,.18,0);
   }
   case "ashen_veil" -> { // Two drifting curtains overlap, leaving the aim line open.
    for(int side:new int[]{-1,1})p.curve(MaterialOption.VAPOUR,0x8A8480,new double[][]{{side*.6,.45,0},{side*.42,.12,.08},{side*.55,-.3,.15}});
   }
   case "cinderheart" -> { // Paired ember lobes tighten into a beating heart.
    p.curve(MaterialOption.EMBER,0xFF7A20,new double[][]{{0,-.4,0},{-.36,.05,0},{-.24,.28,0},{0,.08,.1},{.24,.28,0},{.36,.05,0},{0,-.4,0}});
    p.dot(MaterialOption.EMBER,0xFFF0A0,0,0,0,.08+.06*t);
   }
   case "searing_edge" -> { // A narrow heated blade grows a bright bevel.
    p.stroke(0xFFF3D0,-.42,-.42,0,.42,.42,.12*t);
    p.curve(MaterialOption.EMBER,0xFFB040,new double[][]{{-.4,-.32,0},{0,.1,.05},{.36,.48,.1}});
   }
   case "fireward" -> { // A shield's two shoulders lock against a lower point.
    p.curve(MaterialOption.EMBER,0xFFC060,new double[][]{{-.4,.32,0},{0,.45,0},{.4,.32,0},{.32,-.15,0},{0,-.48,0},{-.32,-.15,0},{-.4,.32,0}});
    p.stroke(0xFFE5A0,-.22,.1,0,.22,.1,0);
   }
   case "smelt" -> { // A heated grate gathers a descending metallic droplet.
    for(int i=-1;i<=1;i++)p.stroke(0xFFB050,-.42,i*.2,0,.42,i*.2,0);
    p.dot(MaterialOption.STONE,0xFFE080,0,.5-.25*t,0,.14);
   }
   case "hellmouth" -> { // A black jaw opens between opposed burning teeth.
    p.arc(MaterialOption.VOID,0x31152F,.46,0,Math.PI*2,0);
    for(int i=-2;i<=2;i++){p.dot(MaterialOption.EMBER,0xB94162,i*.16,.35,0,.08);p.dot(MaterialOption.EMBER,0xFF6428,i*.16,-.35,0,.08);}
   }
   case "starfire" -> { // Five incandescent points draw a star, pink sparks along its edges.
    for(int i=0;i<5;i++){double a=i*Math.PI*2/5-Math.PI/2,b=(i+2)*Math.PI*2/5-Math.PI/2;p.stroke(0xFF80C0,Math.cos(a)*.48,Math.sin(a)*.48,0,Math.cos(b)*.48,Math.sin(b)*.48,0);}
    p.dot(MaterialOption.EMBER,0xFFFFFF,0,0,0,.1);
   }
   case "everburn" -> { // A flame loops through a broken hourglass that never closes.
    p.curve(MaterialOption.TIME,0xFFD060,new double[][]{{-.3,.4,0},{.3,.4,0},{-.3,-.4,0},{.3,-.4,0}});
    p.curve(MaterialOption.EMBER,0xFF8B30,new double[][]{{0,-.32,0},{-.09,0,.06},{.08,.32,.12}});
   }
   case "conflagration" -> { // Five separate fronts rush inward before the white ignition.
    for(int i=0;i<5;i++){double a=i*Math.PI*2/5;p.curve(MaterialOption.EMBER,0xFF6930,new double[][]{{Math.cos(a)*.7,Math.sin(a)*.7,-.2},{Math.cos(a)*.4,Math.sin(a)*.4,-.1},{0,0,.18*t}});}
    p.dot(MaterialOption.EMBER,0xFFFFFF,0,0,.18*t,.15);
   }
   case "phoenix_pyre" -> { // Feathered wings lift beside an ember body; green renewal follows.
    for(int side:new int[]{-1,1})for(int i=0;i<3;i++)p.curve(MaterialOption.EMBER,0xFFA340,new double[][]{{side*.12,-.1,0},{side*(.35+i*.14),.1+i*.13,0},{side*(.48+i*.14),.35+i*.13,.1*t}});
    p.curve(MaterialOption.PETAL,0x7AD060,new double[][]{{0,-.4,0},{0,-.12,0},{0,.16,0}});
   }
   case "bloodboil" -> { // Red drops climb around two opposed hot needles.
    for(int i=-1;i<=1;i++)p.dot(MaterialOption.BLOOD,0xA82228,i*.22,-.2+.18*t,0,.12);
    p.stroke(0xFF7A30,-.35,-.4,0,-.12,.3,0);p.stroke(0xFF7A30,.35,-.4,0,.12,.3,0);
   }
   case "seethe" -> { // Heated water beads rise and rupture into vapor.
    for(int i=-1;i<=1;i++){p.dot(MaterialOption.WATER,0x4AA8FF,i*.25,-.2+.2*t,0,.12);p.dot(MaterialOption.VAPOUR,0xC5E8EA,i*.25,.25+.15*t,0,.13);}
    p.stroke(0xFF7540,-.35,-.35,0,.35,-.35,0);
   }
   case "skyburst" -> { // A high comb of falling fire forms above three release lanes.
    for(int i=-1;i<=1;i++)p.curve(MaterialOption.EMBER,0xFFD060,new double[][]{{i*.3,.75,-.2},{i*.28,.45,-.1},{i*.2,.15,.1*t}});
    p.stroke(0xFF7930,-.5,.8,-.2,.5,.8,-.2);
   }
   case "cinder_bulwark" -> { // Staggered stone courses knit together with ember mortar.
    for(int row=0;row<2;row++)for(int col=0;col<3;col++)p.dot(MaterialOption.STONE,0x8B5F48,(col-1)*.3+(row==0?.08:0),row*.3-.15,0,.15);
    p.stroke(0xFFA26A,-.5,0,0,.5,0,0);p.dot(MaterialOption.EMBER,0xFFD090,0,.32,0,.09);
   }
   case "boiling_surge" -> { // A curling wet crest bears hot vapor along its leading edge.
    p.curve(MaterialOption.WATER,0x66B9D4,new double[][]{{-.55,-.3,0},{-.2,-.15,0},{.1,.25,0},{.35,.4,0},{.48,.2,.15*t}});
    p.curve(MaterialOption.VAPOUR,0xFFCF9A,new double[][]{{-.3,.1,0},{0,.45,0},{.35,.55,.1*t}});
   }
   default -> { }
  }
 }
 private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void dot(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)Math.clamp(size*scale,.02,.8),8),at(x,y,z));}
  void stroke(int color,double x,double y,double z,double a,double b,double c){Vec3 start=at(x,y,z),d=at(a,b,c).subtract(start);emit.accept(new LightOption(LightOption.RAY,color,(float)d.x,(float)d.y,(float)d.z,.016F,0,0,0,8),start);}
  void curve(int style,int color,double[][] points){for(int i=0;i<points.length;i++){var q=points[i];dot(style,color,q[0],q[1],q[2],.09);if(!minimal && i>0){var prev=points[i-1];dot(style,color,(prev[0]+q[0])*.5,(prev[1]+q[1])*.5,(prev[2]+q[2])*.5,.07);}}}
  void arc(int style,int color,double radius,double begin,double end,double z){int count=minimal?6:12;for(int i=0;i<count;i++){double a=begin+(end-begin)*i/count;dot(style,color,Math.cos(a)*radius,Math.sin(a)*radius,z,.08);}}
  void helix(int style,int color,double radius,double height,double phase){int count=minimal?6:12;for(int i=0;i<count;i++){double t=i/(double)(count-1),a=t*Math.PI*3+phase;dot(style,color,Math.cos(a)*radius,(t-.5)*height,Math.sin(a)*radius,.08);}}
 }
}
