package dev.wildercord.client.fx;

import dev.wildercord.content.EarthOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.BiConsumer;
import static dev.wildercord.content.EarthOption.*;

/** Thirty-two independently authored preparations and physical travelling bodies. No light primitives. */
final class EarthForms {
 private EarthForms() {}
 static final List<String> RUNES=List.of("shield","break","stoneskin","root","tremor","excavate","aftershock","weigh",
  "shackle","rampart","brace","chisel","tunnel","vein","fell","pelt","stoneform","magma","sinkhole","geode",
  "fossilize","bonespur","monolith","strata_rise","thunderquake","infest","sandstorm","tusk_charge","mire",
  "stalactite","basalt_surge","prospect","clockroot");
 static boolean supports(String id){return id.startsWith("wildercord:") && RUNES.contains(id.substring(11));}
 static Set<String> ingredients(List<String> ids){
  var out=new HashSet<String>();
  for(String id:ids)if(supports(id)){
   out.add("earth");
   switch(id.substring(11)){
    case "magma" -> out.add("fire");
    case "sinkhole" -> out.add("void");
    case "geode","prospect" -> out.add("arcane");
    case "fossilize","clockroot" -> out.add("time");
    case "bonespur" -> out.add("blood");
    case "thunderquake" -> out.add("storm");
    case "root","fell" -> out.add("life");
    case "mire" -> out.add("frost");
    default -> { }
   }
  }
  return out;
 }
 static void prepare(String id,int beat,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id))return;
  if(FieldFusionForms.prepare(id,beat,scale,anchor,right,up,forward,minimal,emit))return;
  double t=beat*.5;
  var p=new Pen(anchor,right,up,forward,Math.clamp(scale,.4,2),8,minimal,emit);
  switch(id.substring(11)){
   case "shield" -> { // Weight-bearing plates key into an arch, their seams settling shut.
    for(int s:new int[]{-1,1})p.piece(SLAB,0xC3BCA4,s*(.4-.12*t),.05,.04,.22,-s*.014,0,0,s*.07);
    p.piece(ROCK,0xDBD1B5,0,.31-.07*t,.06,.21,0,-.004,0,0);
    p.piece(FRACTURE,0x897F66,0,-.18,.03,.16,0,0,0,0);
   }
   case "break" -> { // An expanding fault tears a single suspended rock into unequal halves.
    p.piece(ROCK,0xAEA188,-.13-.15*t,.06,.04,.22,-.025,.008,0,-.16);
    p.piece(ROCK,0x8F8978,.15+.08*t,-.09,.02,.18,.018,-.009,0,.12);
    p.piece(FRACTURE,0xD0BFA1,0,.06,0,.28,0,-.003,0,0);p.dust(0,-.27,.01,.17);
   }
   case "stoneskin" -> { // Armour scales overlap downward, closing gaps one course at a time.
    for(int i=0;i<(minimal?3:5);i++){double a=i*1.18;p.piece(SLAB,0xB0B2A0,Math.sin(a)*(.35-.06*t),.28-i*.12,Math.cos(a)*.12,.18,0,-.002,0,i%2==0?.025F:-.025F);}
    p.grit(.08,-.36,0,.08*t);
   }
   case "root" -> { // Woody forks grow toward a clamp while pulled soil trails their tips.
    for(int s:new int[]{-1,1})p.piece(ROOT,0xA0A77B,s*(.35-.12*t),-.03,.02,.25,-s*.006,.01,0,0);
    p.chain(GRIT,0x897654,new double[][]{{-.4,-.3,0},{0,-.4,0},{.4,-.3,0}},.075);
    p.piece(ROOT,0xBCC29A,0,.26*t,.06,.12,0,.01,0,0);
   }
   case "tremor" -> { // The low fault buckles three slabs in alternating heave phases.
    for(int i=-1;i<=1;i++)p.piece(SLAB,0xB2A487,i*.34,-.18+(.11*t*(i==0?1:-1)),i*.07,.21,0,.015*(i==0?1:-1),0,i*.04);
    p.piece(FRACTURE,0x8C8069,0,-.28,.06,.3,0,.009,0,0);p.dust(.3,-.2,0,.12);
   }
   case "excavate" -> { // Nine plugs loosen across a square cutting face, each layer backing out.
    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)if(!minimal || x==0 || y==0)
     p.piece(GRIT,0xB2A68B,x*.24,y*.24,-.15*t*(2-Math.abs(x)),.12,x*.003,y*.003,-.02,.12);
    p.piece(FRACTURE,0xD4C29C,.15,.12,0,.23,0,0,-.012,0);
   }
   case "aftershock" -> { // A first struck tile rises; a separate delayed plate answers beneath it.
    p.piece(SLAB,0xB9A789,-.14,.12*t,.04,.24,-.003,.018,0,-.05);
    p.piece(SLAB,0x887E6B,.2,-.23+.09*(t>.5?1:0),-.14,.21,.006,t>.5?.028:0,0,.09);
    p.piece(FRACTURE,0xD8B98E,0,-.2,.06,.22,0,0,0,0);
   }
   case "weigh" -> { // A dense counterweight sinks from two rock sockets, dragging grit downward.
    p.piece(ROCK,0x8D918C,0,.2-.38*t,.07,.3,0,-.035,0,.01);
    for(int s:new int[]{-1,1})p.piece(SLAB,0xB0B1A2,s*.27,.32,.01,.13,0,-.001,0,0);
    p.chain(GRIT,0x8C8270,new double[][]{{-.1,-.2,0},{0,-.4,0},{.12,-.52,0}},.06);
   }
   case "shackle" -> { // Two earth anchors draw in rough, unequal stone-link tethers.
    for(int s:new int[]{-1,1}){p.piece(ROCK,0x9D9583,s*.46,-.21,0,.14,0,0,0,0);
     p.chain(FRACTURE,0xC6B99A,new double[][]{{s*.43,-.16,0},{s*.28,.04,.07},{s*(.18-.08*t),.18,.05}},.105);}
   }
   case "rampart" -> { // Offset blocks build a parapet, with a compacted dirt bed beneath.
    for(int i=-1;i<=1;i++)p.piece(SLAB,0xAA9980,i*.31,.12*t+Math.abs(i)*.07,0,.21,0,.008,0,0);
    p.piece(ROCK,0x796D54,0,-.24,.04,.24,0,-.003,0,0);p.dust(.3,-.2,0,.12);
   }
   case "brace" -> { // Two diagonal buttresses lean into a keystone instead of enclosing a shield.
    for(int s:new int[]{-1,1})p.piece(SLAB,0xC3B493,s*(.3-.05*t),-.16,.08,.24,-s*.006,.004,0,s*.16);
    p.piece(ROCK,0xDED3B0,0,.06-.04*t,.18,.16,0,-.003,0,0);
   }
   case "chisel" -> { // An off-centre wedge advances through a scored stone plate.
    p.piece(CRYSTAL,0xBFC0AC,-.16+.11*t,.12,.24*t,.21,.008,0,.02,0);
    p.piece(SLAB,0x8A8372,.14,-.08,0,.24,0,0,0,0);
    p.piece(FRACTURE,0xDDD3B5,.13,-.05,.02,.15,0,0,0,0);p.grit(.28,-.19,0,.055*t);
   }
   case "tunnel" -> { // A hollow bore is excavated through three retreating masonry courses.
    for(int i=0;i<(minimal?2:3);i++)for(int s:new int[]{-1,1})p.piece(SLAB,0x9F9480,s*.3,.23-i*.2,-i*.2-.1*t,.15,0,0,-.012,0);
    p.piece(SLAB,0xC1B494,0,.35,-.15*t,.2,0,0,-.015,0);p.dust(0,-.35,-.25,.17);
   }
   case "vein" -> { // A branching mineral seam pulls separate ore grains along its junctions.
    p.chain(CRYSTAL,0xB9B69E,new double[][]{{-.42,-.25,0},{-.15,-.1,0},{0,.1,0},{.17,.32,.1*t}},.105);
    p.chain(GRIT,0x827E68,new double[][]{{0,.1,0},{.28,-.04,0},{.45,.1,0}},.07);
    p.piece(ROCK,0x777B6C,-.14,-.1,.03,.16,.009*t,.005,0,.07);
   }
   case "fell" -> { // Bark collars shear apart while a woody stump falls across the release axis.
    for(int s:new int[]{-1,1})p.piece(ROOT,0x9B906C,s*(.2+.13*t),.1,.02,.23,s*.025,-.006,0,s*.15);
    p.piece(FRACTURE,0xD9C49A,0,-.15,.03,.19,0,0,0,0);p.dust(0,-.32,0,.17);
   }
   case "pelt" -> { // Three different pebbles gather as a staggered throwing hand.
    p.piece(ROCK,0xC0AE8C,-.35+.18*t,.23-.06*t,.14,.19,.014,-.002,.012,-.2);
    p.piece(ROCK,0x8F8974,.32-.1*t,.05,.03,.13,-.009,.002,.01,.23);
    p.piece(GRIT,0xDBCAA1,.03,-.26+.09*t,-.14,.16,0,.012,.016,.16);
   }
   case "stoneform" -> { // Broad body plates close around a massive, cracked spine.
    p.piece(ROCK,0x8C9187,0,.06,.05,.3,0,0,0,0);
    for(int s:new int[]{-1,1})p.piece(SLAB,0xBBC2B0,s*(.38-.09*t),-.08,.04,.25,-s*.005,0,0,s*.02);
    p.piece(FRACTURE,0xDBD8C0,0,.12*t,.09,.19,0,0,0,0);
   }
   case "magma" -> { // A cooled shell splits, squeezing actual molten fire through sediment seams.
    for(int s:new int[]{-1,1})p.piece(ROCK,0x756E5E,s*(.22+.08*t),-.04,0,.25,s*.005,0,0,s*.04);
    p.material(MaterialOption.EMBER,0xEFC578,0,-.08+.2*t,.1,.11);
    p.chain(FRACTURE,0xB29065,new double[][]{{-.16,-.3,0},{.03,-.1,.1},{-.08,.16,0}},.1);
   }
   case "sinkhole" -> { // Sediment drops inward around a hollow void, leaving the top open.
    for(int i=0;i<(minimal?4:7);i++){double a=i*1.1,r=.46-.14*t;p.piece(GRIT,0xA79A80,Math.cos(a)*r,-.08-i*.035,Math.sin(a)*.12,.105,-Math.cos(a)*.022,-.018,0,.13);}
    p.material(MaterialOption.VOID,0x494051,0,-.25-.08*t,0,.15);p.dust(0,-.35,0,.22);
   }
   case "geode" -> { // An asymmetric rock casing opens to expose shaded crystal teeth.
    p.piece(ROCK,0x8F8172,-.25-.08*t,-.05,0,.26,-.006,0,0,-.06);
    p.piece(ROCK,0xB2A496,.25+.1*t,.09,0,.21,.009,0,0,.08);
    for(int i=-1;i<=1;i++)p.piece(CRYSTAL,0xC8BECF,i*.15,.02+.12*t,.1,.17,0,.003,0,0);
    p.material(MaterialOption.ARCANE,0xBFB6DB,0,.24,.03,.055);
   }
   case "fossilize" -> { // A root-shaped fossil is buried course by course; the clock arrests the grit.
    p.piece(ROOT,0xC5B992,-.08,.03,0,.29,0,0,0,0);
    for(int i=0;i<3;i++)p.piece(SLAB,0x958B77,.09,-.3+i*.2+.04*t,0,.2,0,0,0,(1-t)*.1);
    p.material(MaterialOption.TIME,0xC8B384,.26,.23-.06*t,.03,.07);
   }
   case "bonespur" -> { // Unequal ivory spurs extrude from a buried socket with blood at their roots.
    for(int i=-1;i<=1;i++)p.piece(BONE,0xD6D0AA,i*.23,-.08+.18*t-Math.abs(i)*.07,.05,.22,0,.012,0,i*.03);
    p.piece(ROCK,0x827465,0,-.32,0,.22,0,0,0,0);p.material(MaterialOption.BLOOD,0x9E4D45,.12,-.23,.07,.07);
   }
   case "monolith" -> { // Three weighty courses lock vertically, leaving compacted chips at the foot.
    for(int i=0;i<3;i++)p.piece(SLAB,0xB5B29C,0,-.27+i*.24*t,0,.26,0,.007*(i+1),0,0);
    p.grit(-.24,-.38,0,.05*t);p.grit(.26,-.32,0,-.03*t);
   }
   case "strata_rise" -> { // A terrace assembles diagonally in sequential sediment layers.
    for(int i=-1;i<=1;i++)p.piece(SLAB,0xC0AD85,i*.32,-.23+(i+1)*.13*t,i*.09,.23,0,.008*(i+2),0,0);
    p.piece(FRACTURE,0x8A795C,-.14,-.35,.04,.17,0,0,0,0);p.dust(.3,-.3,0,.16);
   }
   case "thunderquake" -> { // Three separate fault lips compress before their timed thunder releases.
    for(int i=-1;i<=1;i++)p.piece(SLAB,0xA2977D,i*(.36-.05*t),-.09+Math.sin(i*1.7+t*2)*.08,0,.22,i*.006,.007,0,i*.04);
    p.piece(FRACTURE,0xD6C299,0,.04,.06,.26,0,0,0,0);p.material(MaterialOption.STORM,0xAEC0CC,.12,.23*t,.06,.07);
   }
   case "infest" -> { // Burrowing stone grubs disturb a crooked seam in independent segments.
    for(int i=0;i<(minimal?4:6);i++)p.piece(GRIT,0xB6AC8F,i*.15-.38,Math.sin(i*1.5+t*3)*.07,-i*.04,.095,.01,0,0,i*.06);
    p.piece(FRACTURE,0x766B55,.1,-.2,.02,.19,0,0,0,0);p.dust(-.32,-.14,0,.15);
   }
   case "sandstorm" -> { // A diagonal granular sheet rolls over a lower turbulent dust bank.
    for(int i=0;i<(minimal?4:8);i++)p.piece(GRIT,0xD6C397,i*.12-.4,Math.sin(i*.9+t*2)*.2,-.07*i,.08,.023,-.004,0,.21);
    p.dust(-.21,-.12,.06,.24);p.dust(.22,.1*t,-.13,.18);
   }
   case "tusk_charge" -> { // Heavy curved tusks lower toward the route, kicking up displaced earth.
    for(int s:new int[]{-1,1})p.piece(BONE,0xD2C5A0,s*.3,-.08-.12*t,.2*t,.3,s*.004,-.004,.026,s*.12);
    p.piece(ROCK,0x94876B,0,-.22,-.1,.17,0,0,.017,.08);p.dust(0,-.36,-.2,.21);
   }
   case "mire" -> { // Wet soil clods slump into an unequal mud bed; moisture leaks from underneath.
    p.piece(ROCK,0x81765E,-.2,-.16-.1*t,0,.25,-.006,-.007,0,.03);
    p.piece(SLAB,0x9F9475,.23,-.22,.08,.23,.003,-.006,0,-.03);
    p.material(MaterialOption.WATER,0x779A9E,.05,-.31-.07*t,.03,.1);p.dust(-.1,-.3,0,.13);
   }
   case "stalactite" -> { // A suspended dripstone cone elongates downward, shedding its crust.
    p.piece(ROCK,0xA99A7D,0,.32,.01,.23,0,0,0,0);
    p.piece(CRYSTAL,0xB7A68B,0,.11-.17*t,.05,.24,0,-.014,0,.02);
    p.grit(-.23,.06-.22*t,0,-.055);p.grit(.2,-.03-.19*t,0,.035);
   }
   case "basalt_surge" -> { // Unequal basalt teeth rise in a forward-stepping pressure chain.
    for(int i=0;i<3;i++)p.piece(SLAB,0x8F9288,i*.27-.27,-.27+.17*t*(3-i),i*.13,.22,0,.012*(3-i),.007,0);
    p.piece(FRACTURE,0xBFBBA4,-.2,-.32,.1,.2,0,0,0,0);
   }
   case "prospect" -> { // Unassuming rock opens a mineral window; ore answers as four separate chips.
    p.piece(ROCK,0x9C977F,0,-.05,0,.29,0,0,0,.025);
    for(int i=0;i<4;i++){double a=i*Math.PI/2;p.piece(CRYSTAL,0xD1CDA9,Math.cos(a)*(.25+.08*t),Math.sin(a)*(.25+.08*t),.1,.1,Math.cos(a)*.007,Math.sin(a)*.007,0,0);}
    p.material(MaterialOption.ARCANE,0xACD0CB,-.1,.1*t,.12,.055);
   }
  }
 }

 static void fly(String id,int age,double scale,Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!supports(id))return;
  if(FieldFusionForms.travel(id,age,scale,anchor,right,up,forward,minimal,emit))return;
  double t=age*.22,sway=Math.sin(t),pulse=Math.cos(t*.7);
  var p=new Pen(anchor,right,up,forward,scale,5,minimal,emit);
  switch(id.substring(11)){
   case "shield" -> { // Joined arch stones meet air edge-first, with an open lower seam.
    for(int s:new int[]{-1,1})p.piece(SLAB,0xC3BCA4,s*.23,.02+.025*sway,.02,.19,0,0,-.015,s*.035);
    p.piece(ROCK,0xDBD1B5,0,.21,.07,.16,0,-.002,0,0);p.dust(0,-.12,-.26,.12);
   }
   case "break" -> { // The split rock tumbles outwards while a bare fracture leads.
    p.piece(ROCK,0xAEA188,-.18,.06*sway,-.04,.17,-.012,.003,-.008,-.2);
    p.piece(ROCK,0x8F8978,.13,-.08*sway,-.15,.13,.011,-.004,-.008,.15);
    p.piece(FRACTURE,0xD0BFA1,0,0,.19,.15,0,0,0,0);
   }
   case "stoneskin" -> { // Overlapping armour scales shed a single chip from the lower course.
    for(int i=0;i<(minimal?3:4);i++)p.piece(SLAB,0xB0B2A0,(i%2==0?-.1:.1),.22-i*.13,.03-i*.09,.15,0,0,0,.02*sway);
    p.grit(.17,-.2,-.36,.04*pulse);
   }
   case "root" -> { // A pointed fork pulls trailing soil behind a continuously curling root.
    p.piece(ROOT,0xA0A77B,-.1,.05*sway,.11,.23,.005,.003,-.005,.02);
    p.piece(ROOT,0xBCC29A,.1,-.05*sway,-.17,.16,0,0,-.012,-.03);
    p.grit(-.08,-.15,-.38,.045);
   }
   case "tremor" -> { // Three fault tiles heave in travelling phase, never becoming a glowing wave.
    for(int i=-1;i<=1;i++)p.piece(SLAB,0xB2A487,i*.22,.06*Math.sin(t+i*1.8),-.07*Math.abs(i),.17,0,.008*Math.cos(t+i),0,i*.05);
    p.piece(FRACTURE,0x8C8069,0,-.12,-.2,.19,0,0,0,0);
   }
   case "excavate" -> { // A rectangular cutting face trails freshly released excavation plugs.
    for(int i=0;i<(minimal?3:5);i++)p.piece(GRIT,0xB2A68B,(i%3-1)*.17,(i/3)*.17-.08,-.08-i*.075,.095,0,0,-.014,.12+sway*.03);
    p.piece(SLAB,0xD4C29C,0,.12,.12,.18,0,0,0,0);
   }
   case "aftershock" -> { // Unequal paired plates travel with a visibly delayed rear rebound.
    p.piece(SLAB,0xB9A789,-.1,.06*sway,.06,.19,0,.008*pulse,0,-.04);
    p.piece(SLAB,0x887E6B,.13,.05*Math.sin(t-1.5),-.3,.17,0,-.007*pulse,-.01,.05);
    p.dust(.04,-.14,-.45,.12);
   }
   case "weigh" -> { // A dense suspended weight drags two sediment falls along its underside.
    p.piece(ROCK,0x8D918C,0,-.04-.045*sway,.06,.24,0,-.01,0,.012);
    for(int s:new int[]{-1,1})p.piece(GRIT,0x8C8270,s*.15,-.17,-.25,.07,0,-.02,-.012,s*.09);
   }
   case "shackle" -> { // Two anchor jaws carry a loose jagged tether behind their closing bite.
    for(int s:new int[]{-1,1})p.piece(ROCK,0x9D9583,s*(.23-.035*sway),-.03,.11,.15,-s*.006,0,0,s*.04);
    p.chain(FRACTURE,0xC6B99A,new double[][]{{-.16,-.1,-.1},{.04,-.14,-.23},{.19,-.08,-.41}},.08);
   }
   case "rampart" -> { // A crenellated brick stack advances behind a compacted earthen foot.
    for(int i=-1;i<=1;i++)p.piece(SLAB,0xAA9980,i*.21,.1+.025*pulse,0,.16,0,0,0,0);
    p.piece(ROCK,0x796D54,0,-.14,-.1,.18,0,-.002,0,.025);p.dust(.1,-.22,-.33,.14);
   }
   case "brace" -> { // A tilted triangular buttress leans forward, dust sheltered in its wake.
    p.piece(SLAB,0xC3B493,-.15,-.09,.02,.21,.003,0,0,.1+.025*sway);
    p.piece(ROCK,0xDED3B0,.11,.05,.15,.14,0,0,0,-.04);p.dust(-.08,-.13,-.28,.11);
   }
   case "chisel" -> { // A dense wedge strikes point-first and scrapes stone chips off its trailing edge.
    p.piece(CRYSTAL,0xBFC0AC,-.025,.05,.2,.18,0,0,.005,.015*sway);
    p.piece(FRACTURE,0xDDD3B5,.04,-.06,-.08,.13,0,0,-.006,0);
    p.grit(.17,-.12,-.31,.04*pulse);
   }
   case "tunnel" -> { // Hollow shoulders keep a clear bore through two trailing dust collars.
    for(int s:new int[]{-1,1})p.piece(SLAB,0x9F9480,s*.22,.04+.02*sway,.06,.19,0,0,-.003,0);
    p.piece(SLAB,0xC1B494,0,.22,.02,.17,0,0,0,0);p.dust(0,-.1,-.36,.22);
   }
   case "vein" -> { // An ore seam forks from a central nugget into two travelling mineral chips.
    p.piece(ROCK,0x777B6C,0,-.02,0,.19,0,0,0,.04*sway);
    p.piece(CRYSTAL,0xB9B69E,-.17,.14,.14,.13,-.004,.003,0,.015);
    p.piece(CRYSTAL,0xB9B69E,.19,-.13,-.22,.1,.006,-.003,-.003,-.015);
    p.grit(.06,.02,-.4,.03);
   }
   case "fell" -> { // Two broken woody segments cant sideways from the stump they just split.
    for(int s:new int[]{-1,1})p.piece(ROOT,0x9B906C,s*.16,.04*sway,-.07,.18,s*.006,-.002,-.003,s*.12);
    p.piece(FRACTURE,0xD9C49A,.01,-.1,.12,.12,0,0,0,0);p.dust(0,-.12,-.3,.14);
   }
   case "pelt" -> { // Distinct pebbles ricochet in a staggered train, with independent tumble axes.
    p.piece(ROCK,0xC0AE8C,-.09,.075*sway,.16,.18,.006,0,0,-.24);
    p.piece(ROCK,0x8F8974,.15,-.08*pulse,-.13,.12,-.004,0,-.007,.18);
    p.piece(GRIT,0xDBCAA1,-.14,-.06,-.36,.1,.003,-.004,-.012,.28);
   }
   case "stoneform" -> { // A broad, heavy spine drags a fractured shoulder shell through the air.
    p.piece(ROCK,0x8C9187,0,0,.08,.25,0,0,0,.018*sway);
    p.piece(SLAB,0xBBC2B0,-.2,-.04,-.11,.18,0,-.001,0,.015);
    p.piece(FRACTURE,0xDBD8C0,.16,.09,-.1,.15,0,0,-.005,0);
   }
   case "magma" -> { // Molten jets squeeze through a dark rocky shell, leaving cinders below.
    for(int s:new int[]{-1,1})p.piece(ROCK,0x756E5E,s*.16,.015*sway,0,.17,s*.003,0,0,s*.025);
    p.material(MaterialOption.EMBER,0xEFC578,0,.035*sway,.12,.11);
    p.piece(FRACTURE,0xB29065,0,-.09,-.18,.15,0,0,-.005,0);
    p.material(MaterialOption.EMBER,0xB97842,.08,-.12,-.35,.045);
   }
   case "sinkhole" -> { // Inward-running sediment folds into a descending hollow behind the nose.
    for(int i=0;i<(minimal?3:5);i++){double a=t+i*2.1;p.piece(GRIT,0xA79A80,Math.cos(a)*.23,Math.sin(a)*.12,-i*.08,.085,-Math.cos(a)*.014,-.006,0,.12);}
    p.material(MaterialOption.VOID,0x494051,0,-.08,-.14,.12);p.dust(0,-.22,-.32,.16);
   }
   case "geode" -> { // An exposed crystal fan travels inside the gap in a split mineral shell.
    for(int s:new int[]{-1,1})p.piece(ROCK,0x8F8172,s*.2,-.01,0,.19,0,0,-.003,s*.035);
    p.piece(CRYSTAL,0xC8BECF,0,.1+.02*pulse,.1,.18,0,.001,0,.01);
    p.material(MaterialOption.ARCANE,0xBFB6DB,0,.08,-.26,.055);
   }
   case "fossilize" -> { // A fossil remains still while a slower mineral rind closes around it.
    p.piece(ROOT,0xC5B992,0,.03,.07,.2,0,0,0,0);
    p.piece(SLAB,0x958B77,-.12,.09,-.08,.18,0,0,0,.018*pulse);
    p.piece(SLAB,0x958B77,.13,-.1,-.17,.16,0,0,0,-.018*pulse);
    p.material(MaterialOption.TIME,0xC8B384,.16,.16,-.28,.055);
   }
   case "bonespur" -> { // Three ivory barbs lead a dense socket; small blood drops cling behind.
    for(int i=-1;i<=1;i++)p.piece(BONE,0xD6D0AA,i*.14,.045*sway-Math.abs(i)*.06,.13-Math.abs(i)*.08,.17,0,.003,0,i*.018);
    p.piece(ROCK,0x827465,0,-.16,-.16,.15,0,-.002,0,.02);
    p.material(MaterialOption.BLOOD,0x9E4D45,.12,-.12,-.3,.065);
   }
   case "monolith" -> { // A solid vertical stone stack surges as one mass, crumbs peeling from its base.
    for(int i=0;i<3;i++)p.piece(SLAB,0xB5B29C,0,-.21+i*.2,.04,.19,0,0,0,.01*sway);
    p.grit(-.19,-.26,-.18,.045);p.grit(.18,-.23,-.3,-.045);
   }
   case "strata_rise" -> { // A miniature terrace advances course-by-course in a repeating rising sequence.
    for(int i=0;i<3;i++)p.piece(SLAB,0xC0AD85,i*.17-.17,-.15+.035*Math.sin(t-i),.1-i*.16,.17,0,.003*Math.cos(t-i),0,0);
    p.dust(-.13,-.26,-.35,.12);
   }
   case "thunderquake" -> { // Displaced fault jaws answer each other, each carrying a separate small discharge.
    for(int i=0;i<3;i++)p.piece(SLAB,0xA2977D,i*.18-.18,.08*Math.sin(t-i*1.4),-.1*i,.17,0,.007*Math.cos(t-i),0,(i-1)*.05);
    p.material(MaterialOption.STORM,0xAEC0CC,0,.17,-.1,.065);p.piece(FRACTURE,0xD6C299,.12,-.1,-.23,.11,0,0,0,0);
   }
   case "infest" -> { // A burrowing segmented stone worm wriggles along a broken seam.
    for(int i=0;i<(minimal?3:5);i++)p.piece(GRIT,0xB6AC8F,.055*Math.sin(t-i),.045*Math.cos(t-i),.14-i*.14,.09,0,0,-.008,i*.035);
    p.piece(FRACTURE,0x766B55,-.15,-.08,-.2,.13,0,0,0,0);
   }
   case "sandstorm" -> { // A broad grain front sweeps sideways above two differently drifting dust banks.
    for(int i=0;i<(minimal?3:6);i++)p.piece(GRIT,0xD6C397,i*.1-.25,.1*Math.sin(t+i),-.035*i,.065,.018,-.002,0,.2);
    p.dust(-.17,-.08,-.18,.21);p.dust(.17,.06*pulse,-.36,.17);
   }
   case "tusk_charge" -> { // Paired heavy tusks rake ahead of a low clod; displaced grit rolls away below.
    for(int s:new int[]{-1,1})p.piece(BONE,0xD2C5A0,s*.21,-.02+.025*pulse,.13,.25,s*.002,0,.003,s*.04);
    p.piece(ROCK,0x94876B,0,-.16,-.11,.14,0,0,0,.018);p.dust(0,-.25,-.3,.18);
   }
   case "mire" -> { // A saturated mud clod droops into its wake, shedding water from the low side.
    p.piece(ROCK,0x81765E,-.1,-.045+.018*sway,.08,.23,0,-.007,0,.03);
    p.piece(SLAB,0x9F9475,.16,-.12,-.15,.17,0,-.005,-.002,-.02);
    p.material(MaterialOption.WATER,0x779A9E,.03,-.18,-.3,.075);
   }
   case "stalactite" -> { // A cracked dripstone point travels ahead of tumbling crust chips.
    p.piece(CRYSTAL,0xB7A68B,0,-.035+.012*sway,.19,.21,0,-.003,0,.01);
    p.piece(ROCK,0xA99A7D,0,.09,-.03,.17,0,0,0,-.025);
    p.grit(-.16,-.15,-.31,.05);p.grit(.16,-.05,-.42,-.04);
   }
   case "basalt_surge" -> { // Three upright basalt teeth leap in a forward-timed chain.
    for(int i=0;i<3;i++)p.piece(SLAB,0x8F9288,i*.12-.12,-.03+.085*Math.sin(t-i*1.2),.12-i*.17,.18,0,.006*Math.cos(t-i),0,0);
    p.piece(FRACTURE,0xBFBBA4,-.1,-.18,-.28,.15,0,0,0,0);
   }
   case "prospect" -> { // A struck ore nodule carries a separate answering mineral echo behind it.
    p.piece(ROCK,0x9C977F,0,-.04,.06,.21,0,0,0,.03*sway);
    p.piece(CRYSTAL,0xD1CDA9,.1,.11,.16,.12,0,0,0,.015);
    p.piece(CRYSTAL,0xD1CDA9,-.15,.06*pulse,-.27,.09,0,.002,-.003,-.018);
    p.material(MaterialOption.ARCANE,0xACD0CB,-.08,.1,-.37,.055);
   }
  }
 }
 private record Pen(Vec3 anchor,Vec3 right,Vec3 up,Vec3 forward,double scale,int life,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  Vec3 at(double x,double y,double z){return anchor.add(right.scale(x*scale)).add(up.scale(y*scale)).add(forward.scale(z*scale));}
  void piece(int style,int color,double x,double y,double z,double size,double dx,double dy,double dz,double spin){
   var drift=right.scale(dx*scale).add(up.scale(dy*scale)).add(forward.scale(dz*scale));
   emit.accept(new EarthOption(style,color,(float)Math.clamp(size*scale,.025,.6),life,drift,(float)spin),at(x,y,z));
  }
  void grit(double x,double y,double z,double dx){piece(GRIT,0xB3A383,x,y,z,.075,dx*.3,-.006,-.005,.16);}
  void dust(double x,double y,double z,double size){piece(DUST,0xB6A78A,x,y,z,size,.004,-.001,-.006,0);}
  void material(int style,int color,double x,double y,double z,double size){emit.accept(new MaterialOption(style,color,(float)Math.clamp(size*scale,.02,.8),life),at(x,y,z));}
  void chain(int style,int color,double[][] points,double size){
   for(int i=0;i<points.length;i++){var q=points[i];piece(style,color,q[0],q[1],q[2],size,0,0,0,.02);
    if(!minimal && i>0){var old=points[i-1];piece(style,color,(q[0]+old[0])*.5,(q[1]+old[1])*.5,(q[2]+old[2])*.5,size*.7,0,0,0,-.01);}}
  }
 }
}
