package dev.wildercord.client.fx;
import dev.wildercord.content.CampOption;
import dev.wildercord.cast.CampConcordFx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BiConsumer;
/** Individually authored notebook and mana-comb bodies, not the Arcane orb scaffold. */
final class CampConcordForms {
 private CampConcordForms(){}
 static final List<String> RUNES=List.of("watchweft","manabraid");
 static boolean supports(String id){return id.startsWith("wildercord:")&&RUNES.contains(id.substring(11));}
 private static final int PAPER=0xDECDA6,COPPER=0xBE906D,FIBER=0xBDB0D1,COMB=0xE0CCA4;
 private static final class Pen {
  final Vec3 at,r,u,f;final double scale;final int lifetime,maximum;final BiConsumer<ParticleOptions,Vec3> out;int spent;
  Pen(Vec3 at,Vec3 r,Vec3 u,Vec3 f,double scale,int life,boolean minimal,BiConsumer<ParticleOptions,Vec3> out){this.at=at;this.r=r;this.u=u;this.f=f;this.scale=Math.clamp(scale,.35,1.5);lifetime=life;maximum=minimal?6:14;this.out=out;}
  void put(int material,int color,double x,double y,double z,double size,Vec3 drift,double spin){if(spent++>=maximum)return;var p=at.add(r.scale(x*scale)).add(u.scale(y*scale)).add(f.scale(z*scale));var velocity=r.scale(drift.x).add(u.scale(drift.y)).add(f.scale(drift.z));out.accept(new CampOption(material,color,(float)Math.clamp(size*scale,.025,.6),lifetime,velocity,(float)spin),p);}
 }
 static boolean prepare(String id,int beat,double scale,Vec3 at,Vec3 r,Vec3 u,Vec3 f,boolean minimal,BiConsumer<ParticleOptions,Vec3> out){if(!supports(id))return false;double t=Math.clamp(beat/8.,0,1);var p=new Pen(at,r,u,f,scale,12,minimal,out);
  if(id.endsWith(":watchweft")){
   // Separate pages hinge inward on different beats; staple pins arrive only after folds align.
   p.put(CampOption.PAPER,PAPER,-.36*(1-t),.14-.06*t,-.2+.2*t,.24,new Vec3(.006,-.002,.002),.11*(1-t));
   p.put(CampOption.PAPER,0xC4AE85,.31*(1-t),-.12+.09*t,-.08,.19,new Vec3(-.004,.003,0),-.07*(1-t));
   p.put(CampOption.PAPER,0xF0DDB0,.08,.28*(1-t),.12*t,.13,new Vec3(0,-.006,.002),.05);
   if(t>.35)p.put(CampOption.STAPLE,COPPER,-.14,.08,.06,.065,Vec3.ZERO,0);
   if(t>.7)p.put(CampOption.STAPLE,0xDAAF7F,.14,-.06,.08,.055,Vec3.ZERO,0);
  }else{
   // Comb teeth stay apart; unequal fibres are drawn through individual open slots.
   for(int i=0;i<3;i++){double a=t*(3+i*.8)+i*2.1;p.put(CampOption.FIBER,FIBER,(i-1)*.16+.035*Math.sin(a),.3*(1-t)-.11*i,-.25+.31*t,.15,new Vec3(0,-.003,.006),.025*Math.sin(a));}
   p.put(CampOption.COMB,COMB,-.26+.12*t,-.21,.01,.16,Vec3.ZERO,.03);p.put(CampOption.COMB,0xAA91B4,.25-.10*t,.23,.03,.14,Vec3.ZERO,-.025);
   if(t>.6)p.put(CampOption.KNOT,0xD3B8D5,.02,-.03,.11,.07,new Vec3(0,0,.006),.05);
  }return true;
 }
 static boolean flight(String id,int age,double scale,double length,Vec3 at,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> out){if(!supports(id)||!Double.isFinite(velocity.lengthSqr()))return false;var f=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();var r=f.cross(new Vec3(0,1,0));r=r.lengthSqr()<.001?new Vec3(1,0,0):r.normalize();var p=new Pen(at,r,r.cross(f).normalize(),f,scale*.65,5,minimal,out);
  if(id.endsWith(":watchweft")){double fold=.045*Math.sin(age*.9);p.put(CampOption.PAPER,PAPER,-.06,.08+fold,.11,.22,new Vec3(0,0,-.008),.12);p.put(CampOption.PAPER,0xBCA179,.06,-.07-fold,-.04,.16,new Vec3(0,-.002,-.012),-.07);p.put(CampOption.STAPLE,COPPER,-.08,.01,.18,.06,Vec3.ZERO,0);p.put(CampOption.TEAR,0xD4BC91,.03,-.07,-.26*Math.clamp(length,1,2),.09,new Vec3(0,-.003,-.012),.04);
  }else{for(int i=0;i<3;i++){double a=age*.7+i*2.094;p.put(CampOption.FIBER,FIBER,Math.cos(a)*.095,Math.sin(a)*.095,-i*.075,.16,new Vec3(0,0,-.018),.04*Math.sin(a));}p.put(CampOption.COMB,COMB,0,0,.2,.105,Vec3.ZERO,0);p.put(CampOption.KNOT,0xD0BCD8,.02,.03,-.27*Math.clamp(length,1,2),.065,new Vec3(0,0,-.01),.07);}
  return true;
 }
 static void outcome(CampConcordFx.Event e,int age,boolean minimal,BiConsumer<ParticleOptions,Vec3> out){
  if(e.phase()<=CampConcordFx.END){var p=new Pen(e.from(),new Vec3(1,0,0),new Vec3(0,0,1),new Vec3(0,1,0),1,10,minimal,out);double close=e.phase()==CampConcordFx.END?1-age/12.:1;
   if(e.phase()==CampConcordFx.WARN){var hint=e.to().subtract(e.from());if(hint.lengthSqr()>0){var h=new Pen(e.to().add(0,.08,0),new Vec3(1,0,0),new Vec3(0,0,1),new Vec3(0,1,0),1,8,minimal,out);h.put(CampOption.STAPLE,COPPER,0,0,0,.08,Vec3.ZERO,0);}p.put(CampOption.TEAR,PAPER,-.19,.04,.18,.23,new Vec3(-.006,0,.003),-.12);p.put(CampOption.STAPLE,COPPER,.17,.03,.20,.12,new Vec3(.005,0,.003),.1);p.put(CampOption.PAPER,0xB79E73,0,-.12,.12,.17,Vec3.ZERO,.21);return;}
   if(e.phase()==CampConcordFx.OBSCURED){p.put(CampOption.PAPER,0x9E8D76,0,0,.1,.2,Vec3.ZERO,.22);return;}
   p.put(CampOption.PAPER,PAPER,-.28*close,0,.08,.24,Vec3.ZERO,.04*Math.sin(age*.7));p.put(CampOption.PAPER,0xC6AE84,.21*close,-.15,.1,.18,Vec3.ZERO,-.07);p.put(CampOption.PAPER,0xF1DDB1,.10,.25*close,.11,.13,Vec3.ZERO,.06);p.put(CampOption.STAPLE,COPPER,-.07,-.08,.13,.07,Vec3.ZERO,0);return;
  }
  var d=e.to().subtract(e.from());var f=d.lengthSqr()<.0001?new Vec3(0,0,1):d.normalize();var r=f.cross(new Vec3(0,1,0));r=r.lengthSqr()<.001?new Vec3(1,0,0):r.normalize();var u=r.cross(f).normalize();var end=e.to().subtract(f.scale(.42));
  if(e.phase()==CampConcordFx.OFFER||e.phase()==CampConcordFx.DECLINE){var p=new Pen(end,r,u,f,1,10,minimal,out);double spread=e.phase()==CampConcordFx.DECLINE?.23+age*.009:.18;p.put(CampOption.COMB,COMB,-spread,0,0,.15,Vec3.ZERO,-.07);p.put(CampOption.COMB,0xAD94B7,spread,.04,0,.14,Vec3.ZERO,.08);if(!minimal)p.put(CampOption.FIBER,0x82718D,0,-.15,-.01,.09,Vec3.ZERO,.015);return;}
  // Only the actual admitted credit produces travelling knots and a closed clasp.
  double t=Math.clamp(age/12.,0,1);var start=e.from().add(f.scale(.4));var center=start.lerp(end,t);var p=new Pen(center,r,u,f,1,8,minimal,out);
  for(int i=0;i<3;i++){double a=age*.6+i*2.094;p.put(CampOption.FIBER,FIBER,.05*Math.cos(a),.05*Math.sin(a),-.06*i,.16,Vec3.ZERO,.03);}
  p.put(CampOption.KNOT,0xD7C1DB,0,0,.09,.06+e.gain()*.003,Vec3.ZERO,.06);
  if(age>=10)p.put(CampOption.COMB,COMB,0,-.05,.12,.18,Vec3.ZERO,0);
  if(!minimal&&age<=4){var loss=new Pen(start,r,u,f,1,8,false,out);loss.put(CampOption.TEAR,0x918298,-.11,-.14,0,.08,new Vec3(-.006,-.01,0),.1);}
 }
}
