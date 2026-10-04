package dev.wildercord.client.fx;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.VoidOption;
import dev.wildercord.content.LifeOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BiConsumer;

/** Twelve authored ingredient interactions, used by preparation and moving-body dispatch. */
final class NextSignatureForms {
 private NextSignatureForms(){}
 static final List<String> RUNES=List.of("nullcatch","second_bell","red_ledger","quietus","blood_escrow","frost_molt",
  "pulse_ferry","last_lantern","pocket_current","wayline","night_seam","shard_compass");
 static boolean supports(String id){return id.startsWith("wildercord:") && RUNES.contains(id.substring(11));}
 static Set<String> ingredients(String id){
  if(!supports(id))return Set.of();return switch(id.substring(11)){
   case "nullcatch","wayline","night_seam" -> Set.of("arcane","void");
   case "second_bell" -> Set.of("storm","time");case "red_ledger","blood_escrow" -> Set.of("blood","arcane");
   case "quietus","last_lantern" -> Set.of("arcane","time");case "frost_molt" -> Set.of("frost","life");
   case "pulse_ferry" -> Set.of("life","time");case "pocket_current" -> Set.of("frost","void");
   case "shard_compass" -> Set.of("earth","arcane");default -> Set.of();
  };
 }
 static boolean prepare(String id,int beat,double scale,Vec3 at,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id))return false;draw(id.substring(11),Math.clamp(beat/8.,0,1),false,new Pen(at,right,up,forward,Math.clamp(scale,.4,2),12,minimal,emit));return true;
 }
 static boolean flight(String id,int age,double scale,double length,Vec3 at,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id) || !Double.isFinite(velocity.lengthSqr()))return false;
  var f=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();var r=f.cross(new Vec3(0,1,0));r=r.lengthSqr()<.0001?new Vec3(1,0,0):r.normalize();
  draw(id.substring(11),(age%12)/12.,true,new Pen(at,r,r.cross(f).normalize(),f.scale(Math.clamp(length,1,2)),Math.clamp(scale,.4,2)*.65,5,minimal,emit));return true;
 }
 static boolean travel(String id,int age,double scale,Vec3 at,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id))return false;
  // LifeFlights/VoidFlights already applied the moving body's .65 scale before this delegate.
  draw(id.substring(11),(age%12)/12.,true,new Pen(at,right,up,forward,Math.clamp(scale,.26,1.3),5,minimal,emit));return true;
 }
 private static void draw(String id,double t,boolean flight,Pen p){
  switch(id){
   case "nullcatch" -> {
    // Mirror teeth close around an EMPTY pocket; reflective edge stays ahead of the inward fold.
    for(int side:new int[]{-1,1}){
     p.voidPart(VoidOption.SHARD,side*(flight?.16:.34-.09*t),.13,-.05,.13,side*.01,-.008,0,side*.08,0xBAC7C9);
     p.voidPart(VoidOption.FOLD,side*.13,-.1,-.2-.06*t,.12,-side*.007,0,-.004,side*.02,0x574B66);
     p.material(MaterialOption.ARCANE,0xC3C7D4,side*.23,.22,-.01,.055);
    }
    if(flight)p.voidPart(VoidOption.CLOTH,0,-.21,-.43,.11,0,-.003,-.006,.04,0x66556D);
   }
   case "second_bell" -> {
    // A low copper stem connects unequal chimes; spark arrives at the second AFTER the first swings.
    p.material(MaterialOption.STORM,0xD5B981,-.24,.15+.08*Math.sin(t*5),flight?.13:0,.1);
    p.material(MaterialOption.STORM,0xAA8D63,.23,-.11+.04*Math.sin(t*3-1),-.08,.085);
    p.stroke(MaterialOption.STORM,0xCCAC7B,new double[][]{{-.22,.02,0},{-.05,-.19,-.08},{.19,-.17,-.09}});
    p.material(MaterialOption.TIME,0xC7B285,-.24+.47*t,.04-.14*t,.03,.05);
   }
   case "red_ledger" -> {
    // A measuring needle reaches ahead while three blood knots lag at separate unequal heights.
    p.stroke(MaterialOption.ARCANE,0xD4C6B2,new double[][]{{-.1,-.29,-.2},{.05,.12,.1},{.15,.27,flight?.25:0}});
    for(int i=0;i<3;i++)p.material(MaterialOption.BLOOD,0xA25F53,-.17+.045*i,.16-i*.16,(flight?-.18-i*.11:0)+.035*t,.075);
    p.material(MaterialOption.ARCANE,0xAD9EAF,.16,-.2,.04,.04);
   }
   case "quietus" -> {
    // Three displaced stamps leave a blank slot; sand flows backwards through it before its seal.
    for(int i=0;i<3;i++)p.material(MaterialOption.ARCANE,0xBBAACD,(i-1)*.18,.17-i*.11,flight?-.1-i*.12:.03,.075);
    for(int i=0;i<3;i++)p.material(MaterialOption.TIME,0xC4B17C,.015,.3-((t+i*.26)%1)*.52,flight?-.15:.01,.045);
    p.stroke(MaterialOption.ARCANE,0xA494B1,new double[][]{{-.15,-.26,.03},{.18,-.26,.03},{.18,-.12,.03}});
   }
   case "blood_escrow" -> {
    // Three capillaries feed one chamber from the left; the level rises only during preparation.
    for(int i=0;i<3;i++)p.stroke(MaterialOption.BLOOD,0xA85D53,new double[][]{{-.32,.13-i*.12,-.12},{-.16,.03-i*.09,.03},{.06,-.17+i*.075,.04}});
    p.stroke(MaterialOption.ARCANE,0xC8BDAF,new double[][]{{-.01,.25,.05},{.22,.25,.05},{.26,-.24,.05},{.01,-.24,.05}});
    p.material(MaterialOption.BLOOD,0xCC7B65,.13,-.22+(flight?.22+.03*Math.sin(t*6):.3*t),.06,.12);
   }
   case "frost_molt" -> {
    // Actual frost peels outward first; a living stitch closes the recovered uneven plate.
    for(int i=0;i<3;i++)p.material(MaterialOption.FROST,0xB4CDC4,-.24+.12*i,.21-i*.16,flight?.12:-(1-t)*.15,.105);
    p.life(LifeOption.VINE,0x9CB184,.08,-.21+.21*t,0,.10,0,.005,0,.035);
    p.life(LifeOption.VINE,0x849D72,-.11,.15-.12*t,.03,.08,0,-.003,0,-.025);
   }
   case "pulse_ferry" -> {
    // Two TIME cradles are empty until the one moving sap parcel reaches them; never two free heals.
    for(int side:new int[]{-1,1})p.stroke(MaterialOption.TIME,0xC4B88A,new double[][]{{side*.25-.08,.2,0},{side*.25,-.01,0},{side*.25+.08,.2,0}});
    p.life(LifeOption.SAP,0xA4BC7E,-.25+.5*t,.12*Math.sin(t*Math.PI),flight?.17:0,.14,.015,0,0,.02);
    p.life(LifeOption.LEAF,0x8DAB68,-.21+.35*t,-.25,flight?-.35:0,.10,0,-.006,0,.07);
   }
   case "last_lantern" -> {
    // The bent handle appears first, then the asymmetric chamber fills with reversed sand.
    p.stroke(MaterialOption.ARCANE,0xD1C4A3,new double[][]{{-.16,.25,0},{-.1,.34,0},{.13,.3,0},{.19,.19,0}});
    for(int side:new int[]{-1,1})p.stroke(MaterialOption.ARCANE,0xBEB18C,new double[][]{{side*.17,.14,0},{side*.12,-.25,0},{0,-.27,0}});
    for(int i=0;i<3;i++)p.material(MaterialOption.TIME,0xD7C08F,.015,-.2+((t+i*.23)%1)*.3,flight?-.05:.02,.035);
   }
   case "pocket_current" -> {
    // Water folds over a small parcel; the dark intake is a single OFF-CENTRE seam.
    p.stroke(MaterialOption.WATER,0xA8D2C5,new double[][]{{-.22,.17+.04*t,-.12},{0,.31-.10*t,.04},{.21,.13-.06*t,-.1},{.08,-.2,-.08},{-.22,.17+.04*t,-.12}});
    p.voidPart(VoidOption.CLOTH,.12,-.11,flight?-.32:.015,.12,0,-.003,0,.025,0x69576D);
    p.voidPart(VoidOption.FOLD,.02,-.18,flight?.12:.05,.095,.003,0,.002,-.02,0x514859);
   }
   case "wayline" -> {
    // Three links articulate toward the far clasp; a real dark rope passes through them.
    for(int i=0;i<3;i++){double x=(i-1)*(.27-.06*t),z=flight?.12-i*.2-.03*t:0;
     p.stroke(MaterialOption.ARCANE,0xC3D0C2,new double[][]{{x-.055,.1,z},{x,.18,z},{x+.055,.1,z},{x,.02,z},{x-.055,.1,z}});
     p.voidPart(VoidOption.CLOTH,x,.1,z,.09,.003,0,-.004,.02,0x6E5C77);
    }
   }
   case "night_seam" -> {
    // A precise flat flap unfolds in three stitches; lantern beads ride its far edge in order.
    for(int i=0;i<3;i++){
     p.voidPart(VoidOption.CLOTH,(i-1)*(.2-.025*t),.13-i*.07,flight?-.12-i*.14:0,.12,.002,.002,0,(i-1)*.035,0x695C73);
     p.material(MaterialOption.ARCANE,0xD3C6A4,(i-1)*.18+.07,.17-i*.07+.04*t,.025,.045);
    }
    p.voidPart(VoidOption.FOLD,-.03,-.21,.04,.11,0,-.003,0,.02,0x504858);
   }
   case "shard_compass" -> {
    // Unequal mineral teeth sort by weight; an articulated needle points through their largest gap.
    p.material(MaterialOption.STONE,0xB8A07A,-.23,-.11+.03*t,-.07,.15);
    p.material(MaterialOption.STONE,0xCEB589,.16,.2-.05*t,.025,.11);
    p.material(MaterialOption.STONE,0xA58B64,.23,-.23,.01,.085);
    p.stroke(MaterialOption.ARCANE,0xD4C9AE,new double[][]{{-.14,-.25,flight?-.31:0},{0,.04,.02},{.1,.17,flight?.22:.04}});
   }
   default -> throw new IllegalArgumentException(id);
  }
 }
 private record Pen(Vec3 at,Vec3 r,Vec3 u,Vec3 f,double scale,int life,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  Vec3 point(double x,double y,double z){return at.add(r.scale(x*scale)).add(u.scale(y*scale)).add(f.scale(z*scale));}
  void material(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)Math.clamp(size*scale,.025,.45),life),point(x,y,z));}
  void voidPart(int style,double x,double y,double z,double size,double dx,double dy,double dz,double spin,int color){
   var drift=r.scale(dx).add(u.scale(dy)).add(f.normalize().scale(dz));emit.accept(new VoidOption(style,color,(float)Math.clamp(size*scale,.025,.45),life,drift,(float)spin),point(x,y,z));
  }
  void life(int style,int color,double x,double y,double z,double size,double dx,double dy,double dz,double spin){
   var drift=r.scale(dx).add(u.scale(dy)).add(f.normalize().scale(dz));emit.accept(new LifeOption(style,color,(float)Math.clamp(size*scale,.025,.45),life,drift,(float)spin),point(x,y,z));
  }
  void stroke(int style,int color,double[][] points){
   int step=minimal?2:1;for(int i=0;i<points.length;i+=step){var p=points[i];material(style,color,p[0],p[1],p[2],.045);}
   if(minimal && points.length%2==0){var p=points[points.length-1];material(style,color,p[0],p[1],p[2],.045);}
  }
 }
}
