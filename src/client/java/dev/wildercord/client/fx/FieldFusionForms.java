package dev.wildercord.client.fx;

import dev.wildercord.content.EarthOption;
import dev.wildercord.content.AirflowOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Five field recipes. Geometry helpers are shared; silhouettes, constituent materials and motion are authored individually. */
final class FieldFusionForms {
 private FieldFusionForms(){}
 static final List<String> RUNES=List.of("springbed","cinder_sieve","clockroot","skylatch","thresherwind");
 static boolean supports(String id){return id.startsWith("wildercord:") && RUNES.contains(id.substring(11));}
 static java.util.Set<String> ingredients(List<String> ids){var out=new java.util.HashSet<String>();for(var id:ids)if(supports(id))switch(id.substring(11)){
  case "springbed"->{out.add("frost");out.add("life");}case "cinder_sieve"->{out.add("fire");out.add("void");}case "clockroot"->{out.add("earth");out.add("time");}case "skylatch"->{out.add("wind");out.add("void");}case "thresherwind"->{out.add("wind");out.add("life");}default->{} }return out;}
 static boolean prepare(String id,int beat,double scale,Vec3 at,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id))return false;draw(id.substring(11),beat*.5,false,new Pen(at,right,up,forward,Math.clamp(scale,.4,2),12,minimal,emit));return true;
 }
 static boolean flight(String id,int age,double scale,double length,Vec3 at,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id)||!Double.isFinite(velocity.lengthSqr()))return false;
  var f=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();var r=f.cross(new Vec3(0,1,0));r=r.lengthSqr()<.0001?new Vec3(1,0,0):r.normalize();
  draw(id.substring(11),age*.23,true,new Pen(at,r,r.cross(f).normalize(),f.scale(Math.clamp(length,1,2)),Math.clamp(scale,.4,2)*.65,5,minimal,emit));return true;
 }
 static boolean travel(String id,int age,double scale,Vec3 at,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id))return false;draw(id.substring(11),age*.23,true,new Pen(at,right,up,forward,scale*.65,5,minimal,emit));return true;
 }
 private static void draw(String id,double t,boolean flight,Pen p){
  switch(id){
   case "springbed" -> {
    if(!flight){
     // Water tips in before the seed roots unfold underneath its vessel. No icy star or ordinary green burst.
     for(int i=0;i<3;i++){double x=(i-1)*.22;p.path(MaterialOption.WATER,0x9FDCD0,new double[][]{{x,.42-.18*t,-.08},{x+.05*Math.sin(t+i),.1,.02},{x,-.2,.06}});}
     for(int s:new int[]{-1,1})p.root(new double[][]{{0,-.18,.06},{s*.12*t,-.29,.09},{s*.31*t,-.32,.16}},0x7F9059);
     p.dot(MaterialOption.PETAL,0xABC784,0,-.17,.04,.1);
    }else{
     // A droplet is cradled by a living fork; the lower fork trails and flexes with the moving parcel.
     p.dot(MaterialOption.WATER,0xA2E3D4,0,.06,.13,.16);
     for(int s:new int[]{-1,1})p.root(new double[][]{{s*.07,-.02,.13},{s*.16,-.12,-.08},{s*(.25+.025*Math.sin(t)),-.16,-.38}},0x87965E);
     p.dot(MaterialOption.WATER,0xC5F2DE,0,-.09-(t%1)*.09,-.28,.055);
     p.dot(MaterialOption.PETAL,0xABC784,0,-.06,-.07,.055);
    }
   }
   case "cinder_sieve" -> {
    // A perforated kiln basket closes from four ribs; void draws through the holes rather than replacing fire.
    double span=flight?.18:.4-.13*t;
    for(int i=0;i<4;i++){double a=i*Math.PI/2;double x=Math.cos(a)*span,y=Math.sin(a)*span;
     if(flight)p.dot(MaterialOption.EMBER,0xCB804A,x*.7,y*.7,.12,.07);
     else p.path(MaterialOption.EMBER,0xCB804A,new double[][]{{x,y,-.29},{x*.7,y*.7,.12},{0,0,.23}});
     p.dot(MaterialOption.STONE,0x615647,x,y,-.22,.09);
    }
    for(int i=0;i<(flight||p.minimal?2:4);i++){double a=t+i*Math.PI/2;p.dot(MaterialOption.VOID,0x635467,Math.cos(a)*span*.55,Math.sin(a)*span*.55,-.12-i*.04,.065);}
    if(flight){p.dot(MaterialOption.EMBER,0xF1B36B,0,0,.16,.12);p.dot(MaterialOption.VOID,0x776079,0,0,-.42-.05*Math.sin(t),.09);}
   }
   case "clockroot" -> {
    if(!flight){
     // Unequal root prongs pin a sand hourglass. Sand changes direction on the last preparation beat.
     for(int s:new int[]{-1,1})p.root(new double[][]{{s*.36,-.32,0},{s*.24,.12*t,.03},{s*.12,.32,.02}},0x8D7050);
     for(int i=0;i<5;i++){double y=.28-i*.13,x=(i%2==0?1:-1)*Math.abs(y)*.45;p.grit(x,y,0,0,t<.75?-.024:.024,0);}
     p.dot(MaterialOption.TIME,0xC9B780,0,.04-.04*t,.02,.035);
    }else{
     // Forked root needle travels nose first; individual grains slide backwards along its stitched spine.
     for(int s:new int[]{-1,1})p.root(new double[][]{{s*.2,-.08,-.4},{s*.11,.04,-.1},{0,0,.2}},0x967653);
     for(int i=0;i<4;i++){double z=.15-((t+i*.2)%1)*.55;p.grit((i%2==0?.06:-.06),.02,z,0,0,-.02);}
     p.dot(MaterialOption.TIME,0xC9B780,0,.03,-.2,.03);
    }
   }
   case "skylatch" -> {
    if(!flight){
     // An open updraft catches a suspended knot. Its two ends remain loose so it reads as a temporary latch.
     for(int s:new int[]{-1,1})p.path(MaterialOption.WIND,0xCADBD0,new double[][]{{s*.4,-.37,0},{s*(.21-.07*t),-.03,.08},{s*.17,.35,.01}});
     p.path(MaterialOption.VOID,0x68586F,new double[][]{{-.14,.27,0},{.12,.4,.04},{.14,.22,.08},{-.12,.38,.04},{-.14,.27,0},{-.22,.08,.02}});
    }else{
     // Open brackets flow past the nose; a dark cross-knot pulses at a slower frequency than the air rails.
     for(int s:new int[]{-1,1})p.path(MaterialOption.WIND,0xC1D9CE,new double[][]{{s*.22,-.22,-.2},{s*.28,.01,.12},{s*.14,.2,-.05}});
     p.path(MaterialOption.VOID,0x63546D,new double[][]{{-.1,.02,-.12},{.1,.08,-.18},{-.08,.1,-.18},{.1,.02,-.12}});
     p.dot(MaterialOption.WIND,0xDDE8DA,0,-.2-.04*Math.sin(t*.5),-.34,.08);
    }
   }
   case "thresherwind" -> {
    p.dot(MaterialOption.WIND,0xD7DEBC,0,.03,-.13,.055);
    // Three separate blade lanes advance in order, sever stalk fibres, then collect actual grain behind the shear.
    int lanes=p.minimal?2:3;
    for(int lane=0;lane<lanes;lane++){
     double delay=lane*.24,sweep=flight?Math.sin(t-delay)*.08:Math.clamp(t-delay,0,1)*.25;
     p.path(MaterialOption.WIND,0xCED9B3,new double[][]{{-.4,lane*.12-.16,-.08},{0,lane*.12-.03,.12+sweep},{.42,lane*.12-.13,-.08}});
     p.root(new double[][]{{(lane-1)*.15,-.22,-.1},{(lane-1)*.15+.08*sweep,-.34,-.14}},0xAB9A5C);
     p.dot(MaterialOption.PETAL,0x8F9B58,(lane-1)*.15,-.18,-.13,.065);
     if(flight||t>delay+.2)p.grit((lane-1)*.12,-.2-(t%1)*.08,-.28-lane*.07,0,-.014,-.02);
    }
   }
  }
 }
 private record Pen(Vec3 at,Vec3 right,Vec3 up,Vec3 forward,double scale,int lifetime,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  Vec3 pos(double x,double y,double z){return at.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void dot(int material,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(material,color,(float)Math.clamp(size*scale,.025,.6),lifetime),pos(x,y,z));}
  void path(int material,int color,double[][] points){
   if(material==MaterialOption.WIND){for(int i=0;i+2<points.length;i+=2){var a=points[i];var b=points[i+1];var c=points[i+2];var start=pos(a[0],a[1],a[2]);emit.accept(new AirflowOption(color,pos(b[0],b[1],b[2]).subtract(start),pos(c[0],c[1],c[2]).subtract(start),(float)Math.clamp(.065*scale,.02,.25),lifetime,minimal),start);}return;}
   for(int i=1;i<points.length;i++)for(int step=0;step<(minimal?2:4);step++){double t=step/(minimal?1.0:3.0);var a=points[i-1];var b=points[i];dot(material,color,a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,a[2]+(b[2]-a[2])*t,.035);}
  }
  void root(double[][] points,int color){for(var q:points)emit.accept(new EarthOption(EarthOption.ROOT,color,(float)Math.clamp(.11*scale,.03,.3),lifetime,new Vec3(0,-.002,0),.015F),pos(q[0],q[1],q[2]));}
  void grit(double x,double y,double z,double dx,double dy,double dz){emit.accept(new EarthOption(EarthOption.GRIT,0xB5A476,(float)Math.clamp(.045*scale,.025,.15),lifetime,new Vec3(dx,dy,dz),.02F),pos(x,y,z));}
 }
}
