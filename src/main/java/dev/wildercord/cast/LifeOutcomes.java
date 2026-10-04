package dev.wildercord.cast;

import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Actual effect owners call this after their observed transition succeeds.
 * Geometry is pure: this class cannot heal, damage, place blocks, renew a ward or infer success.
 * An observation's anchor is the live recipient/edited cell/actual destination supplied by that owner.
 */
public final class LifeOutcomes {
 public enum Moment { APPLY, PULSE, TRIGGER, RENEW, END, REFUSED }
 public static final List<String> RUNES=List.of("heal","grow","regrowth","cleanse","venom","nourish","harvest","reversal","restore","bramble","haven","glimmer","fortune","bloom","soulbond","second_wind","lifebloom","root_bulwark","bloomstep","stitchtime","vinelash","remedy","ancient_seed","moonpetal","sporebloom","glowvine","rootsnare","drowse","ashen_mercy","pulse_ferry");
 /** units: actual changed cells/removed conditions/ward charges, bounded for display only.
  * delta: actual health or durability change, never requested power. Secondary: source, partner,
  * old position or projectile strike as documented in the integration map. It is optional.
  */
 public record Observation(String rune,Moment moment,Vec3 anchor,Vec3 secondary,int units,double delta,int age,Vec3 normal,double standoff) {
  public Observation(String rune,Moment moment,Vec3 anchor,Vec3 secondary,int units,double delta,int age){this(rune,moment,anchor,secondary,units,delta,age,new Vec3(0,0,1),0);}
  public Observation {
   if(!RUNES.contains(rune)||moment==null||anchor==null||!Double.isFinite(anchor.lengthSqr())||units<0||!Double.isFinite(delta)||age<0)throw new IllegalArgumentException("Invalid observed Life outcome");
   if(secondary!=null && !Double.isFinite(secondary.lengthSqr()))throw new IllegalArgumentException("Invalid secondary endpoint");
   if(normal==null||!Double.isFinite(normal.lengthSqr())||Math.abs(normal.lengthSqr()-1)>1e-5||!Double.isFinite(standoff)||standoff<0||standoff>4)throw new IllegalArgumentException("Invalid outcome surface frame");
  }
 }
 public static void draw(Observation o,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit) {
  // Refusal is an independent gameplay/UI event; no successful outcome is drawn here.
  if(o.moment()==Moment.REFUSED)return;
  boolean end=o.moment()==Moment.END, trigger=o.moment()==Moment.TRIGGER;
  double t=Math.min(1,o.age()/12.0),q=1-t;
  int n=Math.min(6,o.units());
  Vec3 right=right(o.normal()),up=o.normal().cross(right);
  java.util.function.BiConsumer<ParticleOptions,Vec3> surface=(option,at)->{
   Vec3 local=at.subtract(o.anchor());Vec3 world=o.anchor().add(o.normal().scale(o.standoff())).add(rotate(local,right,up,o.normal()));
   if(option instanceof LifeOption l)option=new LifeOption(l.style(),l.color(),l.size(),l.lifetime(),rotate(l.drift(),right,up,o.normal()),l.spin());
   emit.accept(option,world);
  };
  var p=new Pen(o.anchor(),minimal,surface,o.normal(),o.standoff());
  switch(o.rune()) {
   case "heal" -> { // Two wound lips close; unused healing shows only actual absorption separately.
    for(int side:new int[]{-1,1})p.chain(LifeOption.TISSUE,0xC7D898,new double[][]{{side*.32*q,-.25,0},{side*.18*q,0,.06},{0,.26,.05}},new Vec3(-side*.018,.008,0));
    if(o.delta()>0)p.dot(LifeOption.SAP,0xD9E6A0,0,.18,.08,.12,new Vec3(0,.012,0));
    if(end)p.dot(LifeOption.LEAF,0x839F57,.18,-.18,.03,.1,new Vec3(.01,-.025,0));
   }
   case "grow" -> { // One spine branches at the actual grown cell, then sheds the seed coat.
    if(n==0)return;p.chain(LifeOption.VINE,0x6F9A48,new double[][]{{0,-.38,0},{0,.06+.22*t,0},{-.18,.16+.22*t,.02},{0,.06+.22*t,0},{.24,.24+.22*t,.02}},new Vec3(0,.012,0));
    p.dot(end?LifeOption.SEED:LifeOption.LEAF,0xA8B872,.1,.2,.08,.11,new Vec3(.012,end?-.03:.008,0));
   }
   case "regrowth" -> { // The real regeneration stage determines how many tissue bands open.
    for(int i=0;i<Math.max(1,Math.min(3,n));i++)p.chain(LifeOption.TISSUE,0xB1BF89,new double[][]{{-.24,-.3+i*.21,0},{0,-.24+i*.21,.06},{.2,-.26+i*.21,0}},new Vec3(0,end?-.012:.008,0));
    p.dot(LifeOption.VINE,0x638440,0,-.4,.08,.1,new Vec3(.006,-.015,0));
   }
   case "cleanse" -> { // Removed conditions peel away as dry spent husks, without implying a heal.
    for(int i=0;i<n;i++)p.dot(LifeOption.SEED,0x77745D,(i-2)*.11,.08+(i%2)*.12,.04,.08,new Vec3((i-2)*.012,-.023,.015));
    p.chain(LifeOption.TISSUE,0xDDD7B1,new double[][]{{-.23,-.2,0},{0,-.16,.06},{.19,.03,.04}},new Vec3(.012,.018,0));
   }
   case "venom" -> { // Puncture pair seals around the actual victim; DoT pulses issue only on real ticks.
    for(int side:new int[]{-1,1})p.dot(LifeOption.THORN,0x7E9544,side*(end?.27:.12*q),-.08,.1,.14,new Vec3(side*.012,end?-.018:0,0));
    p.dot(LifeOption.SAP,0xA4B35C,.04,-.12,.13,.09,new Vec3(.004,-.026,0));
    if(trigger)p.dot(LifeOption.TISSUE,0x8A8264,-.08,.06,0,.06,Vec3.ZERO);
   }
   case "nourish" -> { // Seed halves cup a food sap drop at the fed player's or animal's body.
    p.dot(LifeOption.SEED,0xBDA26A,-.14,-.18,.04,.15,new Vec3(-.01,-.02,0));p.dot(LifeOption.SEED,0xBDA26A,.14,-.18,.04,.15,new Vec3(.01,-.02,0));
    p.dot(LifeOption.SAP,0xD7B565,0,-.04,.06,.13,new Vec3(0,end?-.024:.011,0));
   }
   case "harvest" -> { // The real replanted crop receives a cut straw bundle and falling ripe seeds.
    if(n==0)return;for(int i=0;i<3;i++)p.chain(LifeOption.VINE,0xC8B36A,new double[][]{{i*.09-.1,-.28,0},{i*.09-.08,.2,.03}},new Vec3((i-1)*.01,-.016,0));
    for(int i=0;i<Math.min(3,n);i++)p.dot(LifeOption.SEED,0xE0C27F,i*.13-.12,.24,.06,.08,new Vec3((i-1)*.012,-.04,0));
   }
   case "reversal" -> { // A folded sap pocket opens only when an actual stored wound is returned.
    p.chain(LifeOption.TISSUE,0xCCC494,new double[][]{{-.24,-.16,0},{0,-.28,.1},{.24,-.16,0},{0,.14,.12}},new Vec3(0,trigger?.025:-.006,0));
    if(trigger)p.dot(LifeOption.SAP,0xE6C581,0,.2,.14,.16,new Vec3(0,.035,0));
   }
   case "restore" -> { // Repairs appear as stitches beside the actual mended equipment slot.
    for(int i=0;i<Math.max(1,n);i++)p.chain(LifeOption.TISSUE,0xD2BE8A,new double[][]{{-.16,i*.08-.2,0},{.04,i*.08-.13,.03},{.19,i*.08-.22,.02}},new Vec3(.005,-.006,0));
    if(end)p.dot(LifeOption.SEED,0x9C8B65,.22,-.16,0,.06,new Vec3(.018,-.022,0));
   }
   case "bramble" -> { // Remaining real charges become unequal twig teeth; consumed tooth shoots at attacker.
    for(int i=0;i<n;i++)p.chain(LifeOption.THORN,0x826942,new double[][]{{i*.14-.3,-.25,0},{i*.14-.27,.05+(i%2)*.1,.04}},new Vec3(trigger?.025:0,-.004,0));
    if(trigger)p.tow(LifeOption.THORN,0xA68C54,o.secondary(),.16,.12);
   }
   case "haven" -> { // Sparse leaf shutters mark the actual boundary glance, never a filled dome.
    for(int i=0;i<3;i++)p.chain(LifeOption.LEAF,0xB8C685,new double[][]{{-.3+i*.22,-.3,0},{-.25+i*.22,.26-i*.06,.05}},new Vec3(end?.018:-.008,end?-.018:.006,0));
    if(trigger)p.dot(LifeOption.VINE,0x75975C,0,.04,.1,.1,new Vec3(.018,.006,.014));
   }
   case "glimmer" -> { // Pale lichen plates attach at the successfully edited face, not the aim point.
    if(n==0)return;p.chain(LifeOption.LEAF,0xA5C9AC,new double[][]{{-.28,-.12,0},{-.13,.1,0},{.03,-.05,0},{.21,.13,0}},new Vec3(0,-.003,.002));
    p.dot(LifeOption.SPORE,0xC8D9B2,.17,.18,.03,.05,new Vec3(.006,.008,0));
   }
   case "fortune" -> { // A single seed token flips; it is a luck mark, not a loot guarantee.
    p.dot(LifeOption.SEED,0xD4B65B,0,.35,.02,.18,new Vec3(end?.018:0,end?-.03:.004,0));
    p.chain(LifeOption.VINE,0xA59551,new double[][]{{-.17,.21,0},{0,.06,.03},{.16,.23,0}},new Vec3(0,-.008,0));
   }
   case "bloom" -> { // Three offset petals open on direct allies; pollen hops use their actual recipient.
    p.chain(LifeOption.PETAL,0xE0C5AE,new double[][]{{-.27,-.12,0},{-.16,.16,.05},{0,.07,.1},{.22,.2,.04}},new Vec3(.012,end?-.012:.009,0));
    if(trigger)p.dot(LifeOption.SPORE,0xD0D196,.08,.3,.03,.05,new Vec3(.016,.014,0));
   }
   case "soulbond" -> { // Two living endpoint knots; the link is drawn only after a real valid partner bind.
    p.chain(LifeOption.VINE,0xB598AC,new double[][]{{-.17,-.2,0},{.1,-.07,.08},{-.07,.15,.05},{.16,.22,0}},new Vec3(end?.018:0,end?-.012:0,0));
    p.tow(LifeOption.TISSUE,0xD3BACA,o.secondary(),.07,end?-.025:.018);
   }
   case "second_wind" -> { // Time-supported seed closes at admission; death prevention cracks the shell once.
    p.dot(LifeOption.SEED,0xCDB980,-.13,0,.03,.19,new Vec3(trigger?-.035:0,end?-.02:0,0));p.dot(LifeOption.SEED,0xCDB980,.13,0,.03,.19,new Vec3(trigger?.035:0,end?-.02:0,0));
    p.support(MaterialOption.TIME,0xC5A979,0,.2,.04,.07);if(trigger)p.dot(LifeOption.TISSUE,0xDBD69C,0,.05,.1,.19,new Vec3(0,.025,0));
   }
   case "lifebloom" -> { // The actual remaining pulse count closes one petal per heal; burst fans at expiry.
    for(int i=0;i<Math.max(1,n);i++)p.dot(LifeOption.PETAL,0xD6C29E,(i-2)*.1,(i%2)*.12-.1,.03,.11,new Vec3(end?(i-2)*.026:0,end?.018:.004,0));
    p.dot(LifeOption.SAP,0xC4D193,0,-.13,.06,.12,new Vec3(0,end?.02:-.009,0));
   }
   case "root_bulwark" -> { // Root splinters climb each actually placed row, then slough beside removed cells.
    if(n==0)return;p.chain(LifeOption.VINE,0x806745,new double[][]{{-.25,-.35,0},{-.15,.1,.06},{.02,.28,.1},{.2,.4,.03}},new Vec3(.002,end?-.035:.023,0));
    p.dot(LifeOption.LEAF,0x879F5C,.16,.22,.11,.13,new Vec3(.015,end?-.028:.009,0));p.support(MaterialOption.STONE,0xA79474,-.1,-.3,.08,.12);
   }
   case "bloomstep" -> { // Departing scraps stretch toward actual arrival; destination petal tread stays at feet.
    p.chain(LifeOption.PETAL,0xD8B5A6,new double[][]{{-.24,-.38,0},{0,-.28,.12},{.3,-.35,.05}},new Vec3(.02,end?-.014:.012,0));
    p.tow(LifeOption.LEAF,0x92AA65,o.secondary(),.08,.026);p.support(MaterialOption.VOID,0x9E879D,.08,-.25,.04,.06);
   }
   case "stitchtime" -> { // The record's true wound counter fills interrupted stitches before its capped closure.
    for(int i=0;i<Math.max(1,n);i++)p.chain(LifeOption.TISSUE,0xD4C99A,new double[][]{{-.22,i*.1-.3,0},{-.04,i*.1-.2,.06},{.2,i*.1-.29,0}},new Vec3(end?-.012:0,end?.012:0,0));
    p.support(MaterialOption.TIME,0xC0A273,.26,.1,.03,.07);
   }
   case "vinelash" -> { // A bent tendon snaps between the actual caster and struck victim, then frays.
    p.chain(LifeOption.VINE,0x7F905A,new double[][]{{-.32,-.18,0},{-.12,.1,.06},{.14,-.05,.1},{.3,.22,.02}},new Vec3(end?.023:-.016,end?-.02:.003,0));
    p.tow(LifeOption.THORN,0xB5A078,o.secondary(),.1,.018);
   }
   case "remedy" -> { // Removed husks become distinct live sap beads; zombie preparation gets no fake cure.
    for(int i=0;i<n;i++){p.dot(LifeOption.SEED,0x8E8A72,i*.12-.22,-.15,.03,.07,new Vec3(.006,-.024,0));p.dot(LifeOption.SAP,0xCBD493,i*.12-.2,.03,.08,.08,new Vec3(-.006,.022,0));}
    if(o.delta()>0)p.dot(LifeOption.TISSUE,0xC4C6A2,0,.13,.1,.13,new Vec3(0,.008,0));
   }
   case "ancient_seed" -> { // Five real growth sweeps break a stratified seed coat at changed crop cells.
    if(n==0)return;for(int i=0;i<3;i++)p.dot(LifeOption.SEED,0xA18A5B,i*.15-.16,.06+i*.08,.04,.1,new Vec3((i-1)*.012,-.02,0));
    p.chain(LifeOption.VINE,0x879F57,new double[][]{{0,-.3,0},{-.07,.07,.04},{.09,.28,.06}},new Vec3(.003,.018,0));
   }
   case "moonpetal" -> { // One-sided silver cutting petals and warm tissue answer actual harmed/helped targets.
    p.chain(LifeOption.PETAL,0xBFC5D6,new double[][]{{-.38,-.2,0},{-.18,.16,.02},{.12,.27,.04}},new Vec3(.028,end?-.013:.01,0));
    p.dot(o.delta()>0?LifeOption.TISSUE:LifeOption.THORN,o.delta()>0?0xD6CEB4:0x9394B6,.15,-.02,.07,.12,new Vec3(.012,.008,0));
   }
   case "sporebloom" -> { // Ground fruit cracks upward; only actual afflicted/fed recipients get satellite grains.
    p.chain(LifeOption.TISSUE,0xB69A75,new double[][]{{-.22,-.34,0},{0,-.12,.05},{.23,-.35,0}},new Vec3(end?.012:0,end?-.02:.006,0));
    for(int i=0;i<3;i++)p.dot(LifeOption.SPORE,0xC8C095,i*.16-.15,.04+i*.1,.05,.045,new Vec3((i-1)*.012,.018,0));
   }
   case "glowvine" -> { // One berry weight forms at each real placed head/refreshed berry; leaves drape downward.
    if(n==0)return;p.chain(LifeOption.VINE,0x788B55,new double[][]{{0,.35,0},{-.04,.05,.02},{.03,-.22,.04}},new Vec3(0,-.018,0));
    p.dot(LifeOption.SEED,0xCFA764,.06,-.2,.06,.13,new Vec3(end?.018:0,-.024,0));
   }
   case "rootsnare" -> { // Actual held foe gets opposing root forks; each charged step snaps one real thorn.
    for(int side:new int[]{-1,1})p.chain(LifeOption.VINE,0x8A754D,new double[][]{{side*.4,-.42,0},{side*.18,-.05,.07},{side*.27,.13,.04}},new Vec3(side*(trigger?.02:-.008),end?-.015:.004,0));
    if(trigger)p.dot(LifeOption.THORN,0xB2986C,0,-.2,.1,.13,new Vec3(.028,-.018,0));
   }
   case "drowse" -> { // Folded dry leaf curtains descend; an actual wake opens them sideways.
    for(int side:new int[]{-1,1})p.chain(LifeOption.LEAF,0xA6A990,new double[][]{{side*.2,.3,0},{side*.12,.12,.07},{side*.23,-.13,.04}},new Vec3(end?side*.024:0,-.016,0));
    p.dot(LifeOption.SPORE,0xD0CBB8,0,.28,.1,.035,new Vec3(.003,.007,0));
   }
   case "ashen_mercy" -> { // Actually removed ailments give scorched flakes; capped healing makes a tissue seam.
    for(int i=0;i<n;i++)p.dot(LifeOption.LEAF,0x706452,i*.13-.2,.1+(i%2)*.11,.04,.09,new Vec3((i-1)*.01,-.022,.01));
    if(o.delta()>0)p.chain(LifeOption.TISSUE,0xD0BD99,new double[][]{{-.23,-.17,0},{0,.03,.08},{.21,-.13,0}},new Vec3(0,.012,0));
    p.support(MaterialOption.EMBER,0xB18862,0,-.19,.05,.04);
   }
   case "pulse_ferry" -> { // Only actual parcel healing: split seed envelope, sap tongue, clock tooth.
    if(o.moment()!=Moment.PULSE||o.delta()<=0)return;
    int beat=Math.min(2,Math.max(1,n));double split=beat==1?.08:.17;
    for(int side:new int[]{-1,1})p.chain(LifeOption.LEAF,0x859961,new double[][]{{side*.25,-.12,0},{side*split,.06,.07},{side*.19,.21,.04}},new Vec3(side*.013,.006,0));
    p.chain(LifeOption.SAP,0xC1CD8F,new double[][]{{-.04,-.16,.08},{.05,.02,.12},{-.02,.19,.09}},new Vec3(.006,.018,0));
    p.dot(LifeOption.TISSUE,0xA4BF7D,.03,-.09,.14,.10+Math.min(3,o.delta())*.015,new Vec3(0,.009,0));
    p.support(MaterialOption.TIME,0xB4A778,-.21,.13,.06,.045);
    if(beat==2)p.support(MaterialOption.TIME,0xB4A778,.22,.20,.03,.04);
    p.tow(LifeOption.SEED,0x9BAB72,o.secondary(),.05,-.012);
   }
   default -> throw new IllegalArgumentException("Unreviewed Life outcome");
  }
 }
 private static Vec3 right(Vec3 normal){Vec3 r=new Vec3(0,1,0).cross(normal);return r.lengthSqr()<1e-6?new Vec3(1,0,0):r.normalize();}
 private static Vec3 rotate(Vec3 local,Vec3 right,Vec3 up,Vec3 normal){return right.scale(local.x).add(up.scale(local.y)).add(normal.scale(local.z));}
 private record Pen(Vec3 origin,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit,Vec3 normal,double standoff) {
  void dot(int style,int color,double x,double y,double z,double size,Vec3 drift){emit.accept(new LifeOption(style,color,(float)size,10,drift,.08F),origin.add(x,y,z));}
  void chain(int style,int color,double[][] points,Vec3 drift){for(int i=0;i<points.length;i++){var a=points[i];dot(style,color,a[0],a[1],a[2],.075,drift);if(!minimal && i>0){var b=points[i-1];dot(style,color,(a[0]+b[0])*.5,(a[1]+b[1])*.5,(a[2]+b[2])*.5,.05,drift);}}}
  void support(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)size,8),origin.add(x,y,z));}
  void tow(int style,int color,Vec3 other,double size,double speed){if(other==null)return;Vec3 dir=other.subtract(origin.add(normal.scale(standoff)));if(dir.lengthSqr()<.001)return;Vec3 world=dir.normalize().scale(speed),r=right(normal),u=normal.cross(r);Vec3 local=new Vec3(world.dot(r),world.dot(u),world.dot(normal));dot(style,color,0,0,.05,size,local);}
 }
}
