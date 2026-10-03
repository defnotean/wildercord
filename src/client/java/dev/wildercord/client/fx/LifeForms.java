package dev.wildercord.client.fx;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BiConsumer;
import static dev.wildercord.content.LifeOption.*;
/** Independently authored living preparations and moving bodies; no light geometry. */
final class LifeForms {
 private LifeForms() {}
 static final List<String> RUNES=List.of("heal","grow","regrowth","cleanse","venom","nourish","harvest","reversal","restore","bramble","haven","glimmer","fortune","bloom","soulbond","second_wind","lifebloom","root_bulwark","bloomstep","stitchtime","vinelash","remedy","ancient_seed","moonpetal","sporebloom","glowvine","rootsnare","drowse","ashen_mercy");
 static boolean supports(String id){return id.startsWith("wildercord:")&&RUNES.contains(id.substring(11));}
 static Set<String> ingredients(List<String> ids){var out=new HashSet<String>();for(String id:ids)if(supports(id)){out.add("life");switch(id.substring(11)){case "bloom","root_bulwark"->out.add("earth");case "soulbond"->out.add("arcane");case "second_wind","stitchtime"->out.add("time");case "bloomstep"->out.add("void");case "ashen_mercy"->out.add("fire");default->{}}}return out;}
 static void prepare(String id,int beat,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
 if(!supports(id))return;double t=beat*.5;var p=new Pen(anchor,right,up,forward,Math.clamp(scale,.4,2),8,minimal,emit);
 switch(id.substring(11)){
 case "heal" -> { for(int s:new int[]{-1,1})p.part(TISSUE,0xD5DEC3,s*(.3-.2*t),0,.02,.21,-s*.014,0,0,s*.03);p.part(SAP,0xB9D28B,0,.14*t,.04,.09,0,.005,0,0); }
 case "grow" -> { p.part(SEED,0xC4B07C,0,-.25,0,.16,0,0,0,.02);for(int s:new int[]{-1,1})p.part(LEAF,0xA8CF78,s*.2*t,.05*t,.04,.2,s*.005,.009,0,s*.08);p.part(VINE,0x7DAB63,0,-.1+.2*t,0,.17,0,.007,0,0); }
 case "regrowth" -> { for(int i=0;i<3;i++)p.part(VINE,0x9BBE7B,(i-1)*.2,.15*t-i*.12,i*.035,.2,0,.01,0,0);p.part(LEAF,0xBFDB9A,.2,.3*t,.05,.17,.002,.004,0,.04); }
 case "cleanse" -> { for(int i=0;i<4;i++)p.part(PETAL,0xECEDD8,(i-1.5)*(.1+.1*t),.24-i*.12,.04,.14,(i-1.5)*.015,.009,0,i*.04);p.part(SAP,0xDDE8C9,0,-.2,0,.12,0,-.02,0,0); }
 case "venom" -> { p.part(SAP,0xABB765,.1,.18-.08*t,.04,.23,0,-.01,0,.03);p.part(THORN,0xD2D190,-.16,-.12*t,.07,.2,-.002,-.008,0,-.07);p.part(SAP,0x829C54,.12,-.25,0,.08,0,-.026,0,0); }
 case "nourish" -> { for(int i=0;i<3;i++)p.part(SEED,0xE0C791,(i-1)*.2*(1-.3*t),-.13,.02,.16,0,.009,0,i*.12);p.part(LEAF,0xB8C885,0,.14,0,.23,0,.005,0,.03); }
 case "harvest" -> { for(int i=0;i<3;i++){p.part(VINE,0xC7B986,(i-1)*.16,0,.01,.23,0,.006,0,0);p.part(SEED,0xDDD0A0,(i-1)*.16,.24*t,.05,.1,0,-.006,0,i*.06);} }
 case "reversal" -> { p.part(SEED,0xD0D09B,0,.05,.02,.27,0,.003,0,.03*t);for(int s:new int[]{-1,1})p.part(PETAL,0xD7C8DC,s*(.28-.1*t),-.1,.07,.21,-s*.006,.008,0,s*.1); }
 case "restore" -> { for(int i=0;i<3;i++)p.part(TISSUE,0xD2DCC0,(i-1)*.23*(1-.4*t),i*.04-.06,0,.16,0,.005,0,i*.03);p.part(VINE,0xA6BB8D,0,.19,.03,.24,0,-.002,0,0); }
 case "bramble" -> { for(int i=0;i<3;i++)p.part(THORN,0xA8B788,(i-1)*.29,.1-Math.abs(i-1)*.1,0,.22,(i-1)*.005,.002,0,(i-1)*.12);p.part(VINE,0x7C9C61,0,-.2+.08*t,.04,.25,0,.004,0,0); }
 case "haven" -> { for(int i=0;i<3;i++)p.part(LEAF,0xB5D99C,(i-1)*(.33-.05*t),.35-Math.abs(i-1)*.14,0,.23,0,-.002,0,(i-1)*.04);p.part(VINE,0xA3C589,-.4,-.11,.04,.2,0,.01,0,0);p.part(VINE,0xA3C589,.4,-.11,.04,.2,0,.01,0,0); }
 case "glimmer" -> { for(int i=0;i<3;i++)p.part(TISSUE,0xDAE0AA,(i-1)*.17,.1*Math.sin(i+t),.03,.13,0,.002,0,i*.02);p.part(SPORE,0xEDF0C9,.12,-.17*t,0,.08,.006,.01,0,0); }
 case "fortune" -> { for(int i=0;i<3;i++)p.part(LEAF,0xB6D598,(i-1)*.13,.18-Math.abs(i-1)*.13,.02,.17,0,.003,0,i*.14);p.part(SEED,0xE1D3A0,.26*t,-.17,0,.09,.011,.009,0,.19); }
 case "bloom" -> { p.part(SEED,0xD2BB88,0,-.17,0,.15,0,.004,0,.06);for(int s:new int[]{-1,1})p.part(PETAL,0xD9B6CA,s*.26*t,.1,.03,.2,s*.004,.008,0,s*.08);p.support(MaterialOption.STONE,0xA89474,0,-.3,.01); }
 case "soulbond" -> { for(int s:new int[]{-1,1}){p.part(TISSUE,0xC7D9AF,s*.29,.02,0,.17,-s*.005,0,0,s*.04);p.part(VINE,0xACC990,s*(.19-.08*t),-.06,.03,.2,-s*.007,.003,0,0);}p.support(MaterialOption.ARCANE,0xB7D8D3,0,.16,0); }
 case "second_wind" -> { p.part(SEED,0xD2DAB1,0,-.16,0,.18,0,.02,0,.1);p.part(LEAF,0xB2D6A1,-.24,.05+.14*t,.02,.2,.012,.018,0,.18);p.part(LEAF,0xB2D6A1,.24,.18+.08*t,.05,.15,.02,.015,0,-.16);p.support(MaterialOption.TIME,0xD6CEA5,0,.32,.03); }
 case "lifebloom" -> { p.part(TISSUE,0xC0D9AA,0,0,0,.22+.04*t,0,.004,0,0);for(int i=0;i<3;i++)p.part(PETAL,0xE1C9D1,(i-1)*.2*t,.16-Math.abs(i-1)*.18,.04,.16,(i-1)*.005,.005,0,i*.09); }
 case "root_bulwark" -> { for(int i=0;i<3;i++)p.part(VINE,0x9BB987,(i-1)*.24,-.08+.12*t,.02,.27,0,.01,0,0);p.part(LEAF,0xB9D4A0,.24,.26*t,.04,.14,0,.006,0,.06);p.support(MaterialOption.STONE,0xAB9875,0,-.3,0); }
 case "bloomstep" -> { for(int s:new int[]{-1,1})p.part(PETAL,0xD6B7D6,s*(.42-.09*t),.13,.03,.21,-s*.008,.002,0,s*.15);p.part(LEAF,0xA7CA91,-.15,-.26,0,.16,.01,.009,0,.08);p.support(MaterialOption.VOID,0xAD9DBB,0,.09,.06); }
 case "stitchtime" -> { for(int i=0;i<3;i++)p.part(VINE,0xBAC8A0,(i-1)*.23,.11*(i%2==0?1:-1)*t,.02,.18,0,.004,0,0);p.part(TISSUE,0xD5DFC5,0,-.05,0,.15,0,0,0,0);p.support(MaterialOption.TIME,0xD9CAA1,.23,.2,.04); }
 case "vinelash" -> { for(int i=0;i<3;i++)p.part(VINE,0x91B575,(i-1)*.27,.11*Math.sin(i+t*2),.03,.19,.012,.005,0,0);p.part(THORN,0xCAD29B,.33,.19*t,.05,.16,.01,0,0,.1); }
 case "remedy" -> { p.part(LEAF,0xB3CB94,-.17,.13,.03,.26,.002,-.005,0,.04);p.part(SAP,0xD4DDB4,.11,-.13*t,.02,.14,0,-.014,0,0);p.part(PETAL,0xE9DFCA,.24,.16,0,.1,.004,.003,0,.09); }
 case "ancient_seed" -> { p.part(SEED,0xC5AB7B,0,-.18,0,.23,0,0,0,.02);p.part(VINE,0x8DBD78,0,.16*t,0,.19,0,.008,0,0);p.part(LEAF,0xBED697,-.23*t,.17*t,.05,.22,-.005,.006,0,-.04);p.part(PETAL,0xDEC891,.15,.29*t,.03,.13,0,.004,0,.03); }
 case "moonpetal" -> { for(int i=0;i<3;i++)p.part(PETAL,0xD5D7E3,(i-1)*.24,.16*Math.sin(i+t),i*.04,.2,(i-1)*.01,.005,0,(i-1)*.17);p.part(SAP,0xC0C8DB,0,-.24,0,.07,0,.004,0,0); }
 case "sporebloom" -> { p.part(TISSUE,0xCCBAA8,0,.11*t,0,.24,0,.003,0,0);for(int i=0;i<3;i++)p.part(SPORE,0xC8CD9C,(i-1)*.23*t,-.17,.02,.11,(i-1)*.01,.013,0,0); }
 case "glowvine" -> { for(int i=0;i<3;i++){p.part(VINE,0x90BB79,.06*Math.sin(i+t),.3-i*.22,0,.17,0,-.005,0,0);if(i<2)p.part(SAP,0xE5CE9B,.15,.23-i*.27,.05,.11,0,-.002,0,0);} }
 case "rootsnare" -> { for(int s:new int[]{-1,1})p.part(VINE,0x9FAD7C,s*(.36-.17*t),-.05+.13*t,.02,.24,-s*.01,.01,0,0);p.part(THORN,0xC6BC95,0,-.27,.03,.14,0,.004,0,.06); }
 case "drowse" -> { for(int i=0;i<3;i++)p.part(SPORE,0xDED7BC,(i-1)*.2,.15-i*.13,.04,.12,(i-1)*.002,-.004,0,0);p.part(PETAL,0xD6CBD9,.25-.08*t,.2,.02,.18,-.006,-.003,0,.025); }
 case "ashen_mercy" -> {
  // Scorched leaf scraps close across a living patch; spent affliction falls away as sap.
  for(int sign:new int[]{-1,1})p.part(LEAF,0xB8A58D,sign*(.31-.16*t),.12,.04,.18,-sign*.008,.003,0,sign*.08);
  p.part(TISSUE,0xD8DCC0,0,-.04+.05*t,.06,.21,0,.003,0,0);
  p.part(SAP,0xBFCB96,.09,-.27,.02,.08,0,-.016,0,0);
  p.support(MaterialOption.EMBER,0xD4A177,0,.23,.03);
 }
 }
 }
 static void fly(String id,int age,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
 if(!supports(id))return;double t=age*.55,w=Math.sin(t);var p=new Pen(anchor,right,up,forward,scale,5,minimal,emit);
 switch(id.substring(11)){
 case "heal" -> { for(int s:new int[]{-1,1})p.part(TISSUE,0xD5DEC3,s*(.1+.018*w),0,.09,.17,-s*.002,0,0,s*.02);p.part(SAP,0xB9D28B,0,.07,-.2,.07,0,-.003,-.006,0); }
 case "grow" -> { p.part(SEED,0xC4B07C,0,-.09,.12,.15,0,-.003,0,.04);p.part(VINE,0x7DAB63,0,.025*w,-.1,.15,0,.004,0,0);for(int s:new int[]{-1,1})p.part(LEAF,0xA8CF78,s*.17,.1+.02*w,-.21,.14,s*.004,.003,-.005,s*.08); }
 case "regrowth" -> { for(int i=0;i<3;i++)p.part(VINE,0x9BBE7B,.08*Math.sin(t-i),.12-i*.08,.13-i*.17,.15,0,.004,0,0);p.part(LEAF,0xBFDB9A,.18,.12,-.24,.12,.002,.003,0,.04); }
 case "cleanse" -> { for(int i=0;i<3;i++)p.part(PETAL,0xECEDD8,(i-1)*(.14+.02*w),.06*i-.05,.12-i*.13,.13,(i-1)*.01,.005,-.004,i*.05);p.part(SAP,0xDDE8C9,0,-.14,-.3,.08,0,-.014,0,0); }
 case "venom" -> { p.part(THORN,0xD2D190,0,-.025,.19,.17,0,0,0,.015);p.part(SAP,0xABB765,.08,.06,-.08,.17,0,-.006,0,.02);p.part(SAP,0x829C54,.07,-.1-.018*w,-.3,.075,0,-.018,0,0); }
 case "nourish" -> { for(int i=0;i<3;i++)p.part(SEED,0xE0C791,(i-1)*.12,.045*Math.sin(t+i),.13-i*.13,.13,0,-.002,0,i*.11);p.part(LEAF,0xB8C885,0,-.13,-.27,.17,0,.003,0,.04); }
 case "harvest" -> { for(int i=0;i<3;i++){p.part(VINE,0xC7B986,(i-1)*.09,-.06,.02-i*.09,.17,0,0,0,0);p.part(SEED,0xDDD0A0,(i-1)*.09,.08+.012*w,.08-i*.09,.08,0,-.002,0,i*.07);} }
 case "reversal" -> { p.part(SEED,0xD0D09B,0,.018*w,.07,.21,0,.002,0,.03);for(int s:new int[]{-1,1})p.part(PETAL,0xD7C8DC,s*.16,-.06,-.16,.15,-s*.002,.002,0,s*.06); }
 case "restore" -> { for(int i=0;i<3;i++)p.part(TISSUE,0xD2DCC0,(i-1)*.1,.025*w,.14-i*.15,.12,0,0,0,i*.02);p.part(VINE,0xA6BB8D,.16,.07,-.14,.17,-.002,0,0,0); }
 case "bramble" -> { for(int s:new int[]{-1,1})p.part(THORN,0xA8B788,s*.19,.018*w,.13,.18,s*.001,0,0,s*.09);p.part(VINE,0x7C9C61,0,-.1,-.14,.21,0,0,0,0); }
 case "haven" -> { for(int i=0;i<3;i++)p.part(LEAF,0xB5D99C,(i-1)*.22,.23-Math.abs(i-1)*.09+.01*w,.03-i*.04,.2,0,-.001,0,(i-1)*.025);p.part(VINE,0xA3C589,0,-.11,-.2,.21,0,.003,0,0); }
 case "glimmer" -> { for(int i=0;i<3;i++)p.part(TISSUE,0xDAE0AA,(i-1)*.12,.045*Math.sin(t+i),.12-i*.14,.1,0,.001,0,i*.015);p.part(SPORE,0xEDF0C9,.17,.12,-.3,.065,.003,.007,0,0); }
 case "fortune" -> { for(int i=0;i<3;i++)p.part(LEAF,0xB6D598,(i-1)*.09,.1-Math.abs(i-1)*.09,.07,.13,0,.002,0,i*.13);p.part(SEED,0xE1D3A0,.18+.02*w,-.09,-.29,.075,.009,.005,0,.16); }
 case "bloom" -> { p.part(SEED,0xD2BB88,0,-.12,.1,.11,0,.002,0,.04);for(int s:new int[]{-1,1})p.part(PETAL,0xD9B6CA,s*(.15+.025*w),.08,-.1,.17,s*.003,.003,0,s*.07);p.support(MaterialOption.STONE,0xA89474,0,-.17,-.27); }
 case "soulbond" -> { for(int s:new int[]{-1,1}){p.part(TISSUE,0xC7D9AF,s*.21,.015*w,.02,.14,-s*.002,0,0,s*.02);p.part(VINE,0xACC990,s*.1,-.07,-.15,.16,0,.002,0,0);}p.support(MaterialOption.ARCANE,0xB7D8D3,0,.12,-.28); }
 case "second_wind" -> { p.part(SEED,0xD2DAB1,0,-.06,.15,.14,0,.007,0,.07);for(int i=0;i<2;i++)p.part(LEAF,0xB2D6A1,-.16+i*.3,.09+.025*Math.sin(t-i),-.12-i*.18,.16,.014,.008,0,(i-1)*.15);p.support(MaterialOption.TIME,0xD6CEA5,0,.23,-.13); }
 case "lifebloom" -> { p.part(TISSUE,0xC0D9AA,0,.02*w,.04,.19,0,.002,0,0);for(int i=0;i<3;i++)p.part(PETAL,0xE1C9D1,(i-1)*.15,.13-Math.abs(i-1)*.16,-.16,.13,(i-1)*.003,.003,0,i*.06); }
 case "root_bulwark" -> { for(int i=0;i<3;i++)p.part(VINE,0x9BB987,(i-1)*.17,.017*w,.03-i*.05,.23,0,.002,0,0);p.part(LEAF,0xB9D4A0,.17,.18,-.12,.12,0,.002,0,.04);p.support(MaterialOption.STONE,0xAB9875,0,-.22,-.22); }
 case "bloomstep" -> { for(int s:new int[]{-1,1})p.part(PETAL,0xD6B7D6,s*.25,.06+.025*w,-.05,.17,-s*.003,0,0,s*.11);p.part(LEAF,0xA7CA91,-.13,-.18,-.26,.13,.005,.003,0,.05);p.support(MaterialOption.VOID,0xAD9DBB,0,.04,.15); }
 case "stitchtime" -> { for(int i=0;i<3;i++)p.part(VINE,0xBAC8A0,(i-1)*.14,.06*Math.sin(t+i),.08-i*.12,.15,0,.001,0,0);p.part(TISSUE,0xD5DFC5,0,-.09,-.07,.13,0,0,0,0);p.support(MaterialOption.TIME,0xD9CAA1,.15,.13,-.3); }
 case "vinelash" -> { for(int i=0;i<3;i++)p.part(VINE,0x91B575,.09*Math.sin(t-i),.04*Math.cos(t-i),.18-i*.19,.16,.007,.002,0,0);p.part(THORN,0xCAD29B,.07*Math.sin(t),.07,.28,.13,.004,0,0,.07); }
 case "remedy" -> { p.part(LEAF,0xB3CB94,-.1,.08,.04,.19,0,-.002,0,.025);p.part(SAP,0xD4DDB4,.1,-.07-.015*w,-.12,.11,0,-.01,0,0);p.part(PETAL,0xE9DFCA,.21,.07,-.3,.09,.002,.002,0,.06); }
 case "ancient_seed" -> { p.part(SEED,0xC5AB7B,0,-.08,.16,.19,0,-.001,0,.02);p.part(VINE,0x8DBD78,-.07,.06+.02*w,-.04,.14,0,.003,0,0);p.part(LEAF,0xBED697,-.19,.12,-.13,.17,-.002,.002,0,-.04);p.part(PETAL,0xDEC891,.1,.19,-.26,.1,0,.002,0,.02); }
 case "moonpetal" -> { for(int i=0;i<3;i++)p.part(PETAL,0xD5D7E3,(i-1)*.17,.09*Math.sin(t+i),.13-i*.17,.16,(i-1)*.004,.003,0,(i-1)*.12);p.part(SAP,0xC0C8DB,0,-.18,-.29,.065,0,.002,0,0); }
 case "sporebloom" -> { p.part(TISSUE,0xCCBAA8,0,.08+.02*w,.06,.19,0,.001,0,0);for(int i=0;i<3;i++)p.part(SPORE,0xC8CD9C,(i-1)*.16,-.08,-.13-i*.06,.085,(i-1)*.004,.008,0,0); }
 case "glowvine" -> { for(int i=0;i<3;i++)p.part(VINE,0x90BB79,.04*Math.sin(t-i),.17-i*.13,.08-i*.13,.13,0,-.002,0,0);for(int i=0;i<2;i++)p.part(SAP,0xE5CE9B,.12,.12-i*.2,-.06-i*.16,.095,0,-.002,0,0); }
 case "rootsnare" -> { for(int s:new int[]{-1,1})p.part(VINE,0x9FAD7C,s*(.18+.015*w),-.03,.08,.19,-s*.004,.002,0,0);p.part(THORN,0xC6BC95,0,-.18,-.23,.12,0,.002,0,.035); }
 case "drowse" -> { for(int i=0;i<3;i++)p.part(SPORE,0xDED7BC,(i-1)*.14,.08-i*.07,.12-i*.16,.1,(i-1)*.001,-.002,0,0);p.part(PETAL,0xD6CBD9,.17,.14+.01*w,-.27,.13,-.003,-.002,0,.015); }
 case "ashen_mercy" -> {
  // A closed living patch carries a cooled leaf lid and a separate residual heat seam.
  p.part(TISSUE,0xD8DCC0,0,-.025+.014*w,.1,.18,0,.001,0,0);
  p.part(LEAF,0xB8A58D,-.13,.09,-.06,.15,.003,-.001,0,.03);
  p.part(LEAF,0xA69781,.16,.06+.015*w,-.19,.11,-.002,-.001,0,-.02);
  p.part(SAP,0xBFCB96,.03,-.12,-.31,.07,0,-.008,0,0);
  p.support(MaterialOption.EMBER,0xD4A177,-.06,.16,-.13);
 }
 }
 }
 private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,int life,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
 Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
 void part(int style,int color,double x,double y,double z,double size,double dx,double dy,double dz,double spin){emit.accept(new LifeOption(style,color,(float)Math.clamp(size*scale,.025,.6),life,right.scale(dx*scale).add(up.scale(dy*scale)).add(forward.scale(dz*scale)),(float)spin),at(x,y,z));}
 void support(int style,int color,double x,double y,double z){emit.accept(new MaterialOption(style,color,(float)Math.clamp(.07*scale,.02,.8),life),at(x,y,z));}
 }
}
