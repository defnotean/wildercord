package dev.wildercord.client.fx;
import dev.wildercord.content.VoidOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BiConsumer;
import static dev.wildercord.content.VoidOption.*;
/** Individually authored dark material assemblies and velocity-oriented travelling bodies. */
final class VoidForms {
 private VoidForms() {}
 static final List<String> RUNES=List.of("pull","blink","sonic_boom","wither","dragon_breath","veil","gravity_well","blind","grapple","collect","blackspark","blackflame","hollow","infinity","zipper","shadowstep","shades","anchor","hex","banish","phantom","warp","entropy","devour","singularity","malison","echolocate","resonant_shriek","shulkershell","portalfall","hush","warp_step","eclipse","starmaw","riftcall","umbra","nullcatch","night_seam");
 static boolean supports(String id){return id.startsWith("wildercord:")&&RUNES.contains(id.substring(11));}
 static Set<String> ingredients(List<String> ids){var out=new HashSet<String>();for(String id:ids)if(supports(id)){out.add("void");switch(id.substring(11)){case "warp"->out.add("wind");case "entropy"->out.add("time");case "devour"->out.add("blood");case "malison"->out.add("arcane");case "blackflame"->out.add("fire");case "nullcatch","night_seam"->out.add("arcane");default->{}}}return out;}
 static void prepare(String id,int beat,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
 if(!supports(id))return;if(NextSignatureForms.prepare(id,beat,scale,anchor,right,up,forward,minimal,emit))return;double t=beat*.5;var p=new Pen(anchor,right,up,forward,Math.clamp(scale,.4,2),8,minimal,emit);
 switch(id.substring(11)){
 case "pull" -> { for(int s:new int[]{-1,1})p.f(CLOTH,s*(.38-.16*t),.12*s,.03,.21,-s*.012,-s*.002,0,s*.07);p.f(FOLD,0,-.12,.05,.18,0,.002,0,0); }
 case "blink" -> { p.f(FOLD,-.27+.09*t,.11,.06,.24,.006,0,0,-.025);p.f(FOLD,.28-.06*t,-.1,-.04,.21,-.004,0,0,.02);p.f(REMNANT,.02,.18*t,.03,.12,0,.006,0,.04); }
 case "sonic_boom" -> { for(int i=0;i<3;i++)p.f(PRESSURE,0,.035*i,-.32+i*.21*t,.2-i*.035,0,0,.012,0);p.f(SCULK,.2,-.1,.12,.1,0,0,.003,.03); }
 case "wither" -> { for(int i=0;i<3;i++)p.f(TOOTH,(i-1)*.19,.1-i*.08,.02,.21-i*.03,0,-.004*t,0,i*.04);p.f(SHARD,.12,-.3*t,.01,.07,.005,-.016,0,.13); }
 case "dragon_breath" -> { for(int i=0;i<(minimal?3:5);i++)p.f(HAZE,(i%2==0?-1:1)*.18,.1*Math.sin(i+t),-.25+i*.11,.19+i*.016,.004,.003,-.008,0);p.f(JAW,0,.18,.14,.19,0,.001,0,.02); }
 case "veil" -> { for(int i=0;i<3;i++)p.f(CLOTH,(i-1)*(.26-.08*t),.07-i*.07,.05-i*.045,.23,0,-.001,0,(i-1)*.035);p.f(FOLD,.04,.14,0,.14,0,.003,0,0); }
 case "gravity_well" -> { for(int i=0;i<3;i++)p.f(SHARD,(i-1)*(.29-.1*t),.22-i*.11,.02,.13,(1-i)*.01,-.015,0,i*.08);p.f(FOLD,0,-.18,.03,.25,0,-.002,0,0); }
 case "blind" -> { p.f(SHELL,0,.15-.13*t,.03,.29,0,-.01,0,.01);p.f(FOLD,0,-.12,.07,.22,0,0,0,0);p.f(HAZE,.2,-.12*t,0,.14,.003,0,0,0); }
 case "grapple" -> { p.f(TOOTH,.08,.14*t,.08,.26,.003,.008,0,.02);for(int i=0;i<3;i++)p.f(CLOTH,-.06-i*.06,-.14-i*.09,.02,.13,0,.003,0,0); }
 case "collect" -> { for(int i=0;i<3;i++)p.f(SHARD,(i-1)*(.37-.2*t),.21-i*.14,.04,.1,(1-i)*.016,-.006,0,i*.1);p.f(CLOTH,0,-.18,.02,.25,0,-.001,0,.02); }
 case "blackspark" -> { for(int s:new int[]{-1,1})p.f(SCULK,s*.25,.1*s,.02,.23,-s*.008,0,0,s*.055);p.f(SHARD,0,.13*t,.07,.18,0,.004,0,.15); }
 case "blackflame" -> { for(int i=0;i<3;i++)p.f(CLOTH,(i-1)*.18,.08+i*.055*t,.02,.19,0,.007,0,(i-1)*.06);p.f(SHARD,.1,-.21,.04,.09,0,-.014,0,.13);p.support(MaterialOption.EMBER,0x9A857E,0,.23,.07); }
 case "hollow" -> { for(int s:new int[]{-1,1})p.f(JAW,s*(.35-.14*t),.09*s,.02,.27,-s*.01,0,0,s*.035);p.f(REMNANT,0,-.21,.06,.14,0,-.006,0,.08); }
 case "infinity" -> { for(int i=0;i<3;i++)p.f(SHELL,(i-1)*(.38-.035*t),.11-i*.08,.07*i,.19,(1-i)*.002,0,0,0);p.f(FOLD,0,-.24,.02,.17,0,0,0,0); }
 case "zipper" -> { for(int s:new int[]{-1,1})for(int i=0;i<2;i++)p.f(TOOTH,s*(.16+.08*t),.21-i*.28,.02,.15,s*.005,0,0,s*.09);p.f(CLOTH,0,-.2,.04,.2,0,-.003,0,.03); }
 case "shadowstep" -> { p.f(REMNANT,-.13,.1,.02,.29,-.006,0,0,-.035);p.f(SHELL,.14,-.22+.08*t,.04,.18,.01,.004,0,.015);p.f(CLOTH,-.22,-.25,.02,.12,-.004,0,0,.06); }
 case "shades" -> { for(int s:new int[]{-1,1}){p.f(HOUND,s*.31,.1*t,.03,.22,s*.004,.008,0,s*.05);p.f(TOOTH,s*.35,.14,-.02,.12,s*.002,.005,0,s*.02);}p.f(HAZE,0,-.26,0,.2,0,.003,0,0); }
 case "anchor" -> { p.f(SHELL,0,-.22+.04*t,.02,.29,0,-.003,0,0);p.f(TOOTH,.02,.16,.05,.21,0,-.008,0,.01);p.f(SHARD,-.22,-.26,0,.1,0,-.006,0,.02); }
 case "hex" -> { for(int i=0;i<3;i++)p.f(TOOTH,(i-1)*.24,.18-Math.abs(i-1)*.22,.02,.19,(1-i)*.004,-.002,0,(i-1)*.09);p.f(SCULK,.05,-.13*t,.04,.15,0,.002,0,.02); }
 case "banish" -> { for(int s:new int[]{-1,1})p.f(FOLD,s*(.19+.17*t),.04*s,.02,.22,s*.018,0,0,s*.025);p.f(SHELL,0,.11-.07*t,0,.23,0,-.005,0,0); }
 case "phantom" -> { p.f(REMNANT,-.13,.08,.04,.3,-.004,.003,0,.025);p.f(REMNANT,.16+.11*t,.03,-.06,.2,.007,.002,0,-.04);p.f(CLOTH,-.1,-.29,.03,.13,0,-.007,0,.09); }
 case "warp" -> { p.f(FOLD,-.26+.12*t,.11,.08,.24,.01,0,0,-.025);p.f(FOLD,.25-.09*t,-.12,-.09,.2,-.008,0,0,.03);p.support(MaterialOption.WIND,0xB1B9C3,-.13,-.27,.03); }
 case "entropy" -> { for(int i=0;i<3;i++)p.f(SHARD,(i-1)*.23,.1-i*.1*t,.03,.19-i*.025,0,-.007*t,0,i*.12);p.support(MaterialOption.TIME,0xBDB7A5,.22,.2,.06); }
 case "devour" -> { for(int s:new int[]{-1,1})p.f(JAW,s*(.28-.07*t),.09*s,.03,.23,-s*.006,0,0,s*.03);p.f(TOOTH,.05,.2*t,.06,.17,0,.006,0,.025);p.support(MaterialOption.BLOOD,0x956975,0,-.21,.03); }
 case "singularity" -> { p.f(SHELL,0,0,.04,.27+.025*t,0,0,0,0);for(int i=0;i<3;i++)p.f(SHARD,(i-1)*(.36-.12*t),.28-i*.24,.01,.09,(1-i)*.018,(i-1)*.012,0,i*.17); }
 case "malison" -> { for(int i=0;i<3;i++)p.f(SCULK,(i-1)*.21,.16-i*.12*t,.03,.18,0,.003,0,(i-1)*.06);p.f(TOOTH,.19,-.15,.07,.11,.005,-.004,0,.07);p.support(MaterialOption.ARCANE,0xA9B9BD,-.19,.21,.05); }
 case "echolocate" -> { p.f(PRESSURE,0,.03,.03,.19,0,0,.008,0);for(int i=0;i<3;i++)p.f(SHARD,(i-1)*.2,.08*i,-.16-i*.1*t,.095,0,0,-.006,i*.035); }
 case "resonant_shriek" -> { for(int i=0;i<3;i++)p.f(SCULK,(i-1)*.19,.17+.05*Math.sin(i+t*3),.02,.22,0,.004,0,(i-1)*.03);p.f(PRESSURE,0,-.16,-.06,.27,0,-.002,0,0); }
 case "shulkershell" -> { for(int s:new int[]{-1,1})p.f(SHELL,s*(.29-.12*t),.04,.02,.23,-s*.008,0,0,0);p.f(SHARD,0,.23,.04,.13,0,-.004,0,.02);p.f(SHARD,0,-.23,0,.1,0,.004,0,-.02); }
 case "portalfall" -> { p.f(FOLD,0,.3-.06*t,.04,.24,0,-.007,0,.02);p.f(FOLD,.05,-.3+.05*t,0,.2,0,.003,0,-.015);p.f(SHARD,0,.12-.25*t,.08,.12,0,-.028,0,.06); }
 case "hush" -> { for(int i=0;i<3;i++)p.f(SHELL,(i-1)*.21*(1-.15*t),.09-i*.05,.02,.21,0,0,0,0);p.f(CLOTH,.04,-.25,.04,.18,0,-.001,0,.025); }
 case "warp_step" -> { p.f(FOLD,-.2-.04*t,.12,.08,.23,-.004,.002,0,-.015);p.f(TOOTH,.16,.03-.1*t,-.07,.12,.003,-.003,0,.03);p.f(CLOTH,0,-.2,-.04,.17,0,0,0,.04); }
 case "eclipse" -> { p.f(SHELL,-.11+.13*t,.1,.07,.31,.01,-.002,0,0);p.f(FOLD,.18,-.02,.01,.26,-.001,.002,0,.015);p.f(REMNANT,.2,-.23,0,.12,.004,-.006,0,.03); }
 case "starmaw" -> { for(int s:new int[]{-1,1})p.f(JAW,s*(.4-.11*t),.08*s,.02,.29,-s*.012,0,0,s*.025);p.f(SHARD,-.04,.18-.08*t,.05,.1,.003,-.007,0,.08);p.f(REMNANT,.07,-.16+.06*t,.03,.13,-.003,.004,0,.06); }
 case "riftcall" -> { for(int s:new int[]{-1,1}){p.f(FOLD,s*(.19+.07*t),.09*s,.03,.23,s*.006,0,0,s*.02);p.f(CLOTH,s*.36,-.16*s,.02,.13,-s*.011,s*.003,0,s*.07);} }
 case "umbra" -> { p.f(TOOTH,.07,.05,.1,.24,.004,0,0,.025);p.f(CLOTH,-.11,-.13+.03*t,-.05,.19,-.003,.002,0,-.04);p.f(HAZE,.15,-.2,.02,.12,.002,-.001,0,0); }
 }
 }
 static void fly(String id,int age,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
 if(!supports(id))return;if(NextSignatureForms.travel(id,age,scale,anchor,right,up,forward,minimal,emit))return;double t=age*.55,w=Math.sin(t);var p=new Pen(anchor,right,up,forward,scale,5,minimal,emit);
 switch(id.substring(11)){
 case "pull" -> { for(int s:new int[]{-1,1})p.f(CLOTH,s*(.15+.018*w),s*.07,-.16,.15,-s*.004,-s*.001,-.004,s*.05);p.f(FOLD,0,-.06,.14,.16,0,.001,0,0); }
 case "blink" -> { p.f(FOLD,-.15,.05+.017*w,.12,.18,.003,0,0,-.02);p.f(FOLD,.16,-.05,-.14,.16,-.003,0,0,.015);p.f(REMNANT,.02,.12,-.28,.09,0,.003,0,.03); }
 case "sonic_boom" -> { for(int i=0;i<3;i++)p.f(PRESSURE,0,.02*Math.sin(t-i),.18-i*.2,.17-i*.025,0,0,.006,0);p.f(SCULK,.13,-.07,-.3,.075,0,0,.002,.02); }
 case "wither" -> { for(int i=0;i<3;i++)p.f(TOOTH,(i-1)*.11,.05-i*.07+.015*w,.13-i*.14,.15-i*.02,0,-.003,0,i*.025);p.f(SHARD,.1,-.17,-.3,.06,.003,-.012,0,.12); }
 case "dragon_breath" -> { for(int i=0;i<(minimal?3:5);i++)p.f(HAZE,(i%2==0?-1:1)*.12,.055*Math.sin(t-i),.13-i*.13,.17+i*.01,.002,.002,-.005,0);p.f(JAW,0,.1,.18,.15,0,.001,0,.01); }
 case "veil" -> { for(int i=0;i<3;i++)p.f(CLOTH,(i-1)*.15,.04*Math.sin(t-i),.1-i*.14,.17,0,-.001,0,(i-1)*.025);p.f(FOLD,.03,.1,-.23,.11,0,.001,0,0); }
 case "gravity_well" -> { p.f(FOLD,0,-.09,.08,.21,0,-.001,0,0);for(int i=0;i<3;i++)p.f(SHARD,(i-1)*(.16+.015*w),.14-i*.1,-.07-i*.09,.1,(1-i)*.006,-.011,0,i*.065); }
 case "blind" -> { p.f(SHELL,0,.07+.012*w,.09,.23,0,-.004,0,.01);p.f(FOLD,0,-.1,-.1,.17,0,0,0,0);p.f(HAZE,.11,-.08,-.28,.13,.002,0,-.004,0); }
 case "grapple" -> { p.f(TOOTH,.035,.045*w,.2,.2,.001,.002,0,.015);for(int i=0;i<3;i++)p.f(CLOTH,-.035-i*.04,-.08,.04-i*.15,.09,0,.001,-.003,0); }
 case "collect" -> { p.f(CLOTH,0,-.08,.09,.2,0,-.001,0,.01);for(int i=0;i<3;i++)p.f(SHARD,(i-1)*(.1+.01*w),.075-i*.04,-.11-i*.08,.075,(1-i)*.006,-.002,0,i*.07); }
 case "blackspark" -> { for(int sign:new int[]{-1,1})p.f(SCULK,sign*.16,.055*Math.sin(t+sign),.08,.18,-sign*.003,0,0,sign*.045);p.f(SHARD,0,.03,-.15,.13,0,.002,0,.12); }
 case "blackflame" -> { for(int i=0;i<3;i++)p.f(CLOTH,(i-1)*.11,.07+.025*Math.sin(t-i),.08-i*.09,.15,0,.005,0,(i-1)*.04);p.f(SHARD,.08,-.13,-.26,.065,0,-.01,0,.1);p.support(MaterialOption.EMBER,0x9A857E,0,.16,-.11); }
 case "hollow" -> { for(int sign:new int[]{-1,1})p.f(JAW,sign*(.2+.012*w),.05*sign,.12,.22,-sign*.004,0,0,sign*.025);p.f(REMNANT,0,-.13,-.2,.11,0,-.004,0,.065); }
 case "infinity" -> { for(int i=0;i<3;i++)p.f(SHELL,(i-1)*.2,.06-i*.05+.008*w,.12-i*.12,.15,(1-i)*.001,0,0,0);p.f(FOLD,0,-.15,-.2,.13,0,0,0,0); }
 case "zipper" -> { for(int sign:new int[]{-1,1})for(int i=0;i<2;i++)p.f(TOOTH,sign*(.13+.01*w),.09-i*.16,.1-i*.12,.11,sign*.001,0,0,sign*.065);p.f(CLOTH,0,-.12,-.22,.15,0,-.002,0,.025); }
 case "shadowstep" -> { p.f(SHELL,.06,-.12,.16,.16,.002,.001,0,.01);p.f(REMNANT,-.12,.04+.017*w,-.04,.2,-.003,0,0,-.025);p.f(CLOTH,-.16,-.15,-.3,.095,-.002,0,0,.04); }
 case "shades" -> { for(int sign:new int[]{-1,1}){p.f(HOUND,sign*.2,.055*Math.sin(t-sign),.07-sign*.08,.24,sign*.002,.003,0,sign*.035);p.f(TOOTH,sign*.22,.1,-.08-sign*.07,.095,sign*.001,.002,0,sign*.015);}p.f(HAZE,0,-.15,-.25,.14,0,.002,0,0); }
 case "anchor" -> { p.f(SHELL,0,-.1+.008*w,.04,.23,0,-.001,0,0);p.f(TOOTH,.02,.13,.1,.16,0,-.003,0,.01);p.f(SHARD,-.16,-.14,-.25,.075,0,-.005,0,.02); }
 case "hex" -> { for(int i=0;i<3;i++)p.f(TOOTH,(i-1)*.15,.12-Math.abs(i-1)*.14+.017*Math.sin(t+i),.06,.145,(1-i)*.002,-.001,0,(i-1)*.06);p.f(SCULK,.035,-.08,-.2,.12,0,.001,0,.015); }
 case "banish" -> { p.f(FOLD,-.2-.014*w,.03,.16,.17,-.006,0,0,-.015);p.f(FOLD,.2+.018*w,-.04,-.12,.15,.005,0,0,.025);p.f(SHELL,0,.03,-.22,.16,0,-.002,0,0); }
 case "phantom" -> { p.f(REMNANT,-.1,.06+.015*w,.1,.23,-.002,.001,0,.02);p.f(REMNANT,.15,.025*Math.sin(t-1),-.15,.16,.005,.001,0,-.03);p.f(CLOTH,-.075,-.18,-.27,.1,0,-.004,0,.065); }
 case "warp" -> { p.f(FOLD,-.16,.06+.03*w,.14,.18,.003,0,0,-.02);p.f(FOLD,.15,-.07-.025*w,-.13,.16,-.002,0,0,.025);p.support(MaterialOption.WIND,0xB1B9C3,-.09,-.17,-.28); }
 case "entropy" -> { for(int i=0;i<3;i++)p.f(SHARD,(i-1)*.12,.07-i*.05-.013*w*i,.13-i*.18,.14-i*.02,0,-.005,0,i*.09);p.support(MaterialOption.TIME,0xBDB7A5,.17,.13,-.18); }
 case "devour" -> { p.f(JAW,-.14,.06,.1,.19,.003,0,0,-.02);p.f(JAW,.15,-.05+.016*w,-.1,.17,-.002,0,0,.025);p.f(TOOTH,.035,.13,.23,.12,0,.002,0,.02);p.support(MaterialOption.BLOOD,0x956975,0,-.14,-.21); }
 case "singularity" -> { p.f(SHELL,0,.008*w,.05,.22,0,0,0,0);for(int i=0;i<3;i++)p.f(SHARD,(i-1)*.21,.17-i*.13,.02-i*.12,.075,(1-i)*.009,(i-1)*.007,.004,i*.13); }
 case "malison" -> { for(int i=0;i<3;i++)p.f(SCULK,(i-1)*.12,.09-i*.08+.025*Math.sin(t+i),.08-i*.09,.145,0,.001,0,(i-1)*.04);p.f(TOOTH,.14,-.1,-.16,.085,.003,-.002,0,.05);p.support(MaterialOption.ARCANE,0xA9B9BD,-.14,.16,-.26); }
 case "echolocate" -> { p.f(PRESSURE,0,.015*w,.16,.15,0,0,.004,0);for(int i=0;i<3;i++)p.f(SHARD,(i-1)*.12,.04*i,-.08-i*.14,.07,0,0,-.003,i*.025); }
 case "resonant_shriek" -> { for(int i=0;i<3;i++)p.f(SCULK,(i-1)*.13,.1+.035*Math.sin(t*1.5+i),.06-i*.04,.17,0,.002,0,(i-1)*.025);p.f(PRESSURE,0,-.09,-.21,.2,0,-.001,0,0); }
 case "shulkershell" -> { for(int sign:new int[]{-1,1})p.f(SHELL,sign*.15,.03+.01*w,.06,.18,-sign*.002,0,0,0);p.f(SHARD,0,.15,-.16,.095,0,-.002,0,.015);p.f(SHARD,0,-.14,-.23,.075,0,.002,0,-.015); }
 case "portalfall" -> { p.f(FOLD,0,.18+.018*w,.08,.19,0,-.003,0,.015);p.f(FOLD,.035,-.18,-.09,.15,0,.001,0,-.01);p.f(SHARD,0,.055*w,-.21,.095,0,-.018,0,.045); }
 case "hush" -> { for(int i=0;i<3;i++)p.f(SHELL,(i-1)*.13,.04-i*.035+.008*w,.12-i*.1,.16,0,0,0,0);p.f(CLOTH,.03,-.16,-.3,.13,0,-.001,0,.02); }
 case "warp_step" -> { p.f(FOLD,-.14,.08+.014*w,.16,.18,-.002,.001,0,-.01);p.f(TOOTH,.12,-.01-.025*w,-.15,.1,.001,-.001,0,.025);p.f(CLOTH,0,-.13,-.3,.12,0,0,0,.025); }
 case "eclipse" -> { p.f(SHELL,-.075+.018*w,.07,.1,.25,.003,-.001,0,0);p.f(FOLD,.12,-.015,-.08,.2,-.001,.001,0,.01);p.f(REMNANT,.14,-.15,-.24,.095,.002,-.004,0,.02); }
 case "starmaw" -> { for(int sign:new int[]{-1,1})p.f(JAW,sign*(.25+.015*w),.045*sign,.1,.23,-sign*.005,0,0,sign*.02);p.f(SHARD,-.03,.12,-.09,.075,.002,-.004,.003,.055);p.f(REMNANT,.05,-.105,-.25,.1,-.002,.003,.004,.04); }
 case "riftcall" -> { for(int sign:new int[]{-1,1}){p.f(FOLD,sign*(.15+.015*Math.sin(t+sign)),.055*sign,.1,.18,sign*.002,0,0,sign*.015);p.f(CLOTH,sign*.23,-.09*sign,-.19,.105,-sign*.007,sign*.002,.002,sign*.05);} }
 case "umbra" -> { p.f(TOOTH,.045,.03+.012*w,.2,.19,.002,0,0,.02);p.f(CLOTH,-.08,-.09,-.07,.15,-.002,.001,0,-.025);p.f(HAZE,.11,-.14,-.27,.095,.001,-.001,0,0); }
 }
 }
 private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,int life,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void f(int style,double x,double y,double z,double size,double dx,double dy,double dz,double spin){
   int color=style==SCULK?0xABD0D1:style==PRESSURE?0xB0C4C7:0xD0CDDE;
   emit.accept(new VoidOption(style,color,(float)Math.clamp(size*scale,.025,.6),life,right.scale(dx*scale).add(up.scale(dy*scale)).add(forward.scale(dz*scale)),(float)spin),at(x,y,z));
  }
  void support(int style,int color,double x,double y,double z){emit.accept(new MaterialOption(style,color,(float)Math.clamp(.07*scale,.02,.8),life),at(x,y,z));}
 }
}
