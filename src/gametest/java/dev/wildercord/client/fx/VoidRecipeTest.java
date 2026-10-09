package dev.wildercord.client.fx;

import dev.wildercord.content.VoidOption;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildercord.content.MaterialOption.*;

/** Exact runtime roster and bounded changing recipe acceptance. */
public final class VoidRecipeTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  check(EverydayRunes.combatPaths("void").equals(new HashSet<>(VoidForms.RUNES)),"Exact runtime Void roster");
  var uniquePrep=new HashSet<String>();var uniqueFlight=new HashSet<String>();
  for(String rune:VoidForms.RUNES)for(boolean minimal:new boolean[]{false,true}) {
   var prep=new ArrayList<String>();var flight=new ArrayList<String>();
   for(int phase:new int[]{1,2}) {
    var trace=new ArrayList<String>();var support=new HashSet<Integer>();final int[] count={0};
    VoidForms.prepare("wildercord:"+rune,phase,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(o,p)->{
     check(Double.isFinite(p.lengthSqr()) && p.length()<1.8,"Bounded preparation "+rune);check(!(o instanceof LightOption),"Living materials, no light line "+rune);
     if(o instanceof VoidOption){count[0]++;}if(o instanceof MaterialOption m)support.add(m.style());trace.add(o.toString()+"@"+p);
    });
    check(count[0]>0 && trace.size()<40,"Dedicated bounded Void preparation "+rune);check(support.containsAll(support(rune)),"Preparation ingredients "+rune);prep.add(String.join(";",trace));
   }
   for(int age:new int[]{4,8,12}) {
    var trace=new ArrayList<String>();var support=new HashSet<Integer>();final int[] count={0};
    VoidFlights.draw("wildercord:"+rune,age,1,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{
     check(Double.isFinite(p.lengthSqr()) && p.length()<1,"Bounded moving body "+rune);check(!(o instanceof LightOption),"No luminous travelling template "+rune);
     if(o instanceof VoidOption m){count[0]++;check(m.lifetime()==5,"Short lived body "+rune);}if(o instanceof MaterialOption m)support.add(m.style());trace.add(o.toString()+"@"+p);
    });
    check(count[0]>0 && trace.size()<16,"Dedicated bounded Void flight "+rune);check(support.containsAll(support(rune)),"Flight ingredients "+rune);flight.add(String.join(";",trace));
   }
   check(new HashSet<>(prep).size()>1,"Changing preparation "+rune);check(new HashSet<>(flight).size()>1,"Changing travel "+rune);
   if(!minimal){check(uniquePrep.add(prep.getFirst()),"Unique preparation "+rune);check(uniqueFlight.add(flight.getFirst()),"Unique flight "+rune);}
  }
  check(!VoidForms.supports("other:heal") && !VoidForms.supports("wildercord:fire"),"Exact namespace and family");
  for(Vec3 v:List.of(Vec3.ZERO,new Vec3(0,1,0),new Vec3(0,-1,0))){final int[] emitted={0};VoidFlights.draw("wildercord:pull",8,2,2,Vec3.ZERO,v,true,(o,p)->{emitted[0]++;check(Double.isFinite(p.lengthSqr()) && p.length()<2,"Vertical/stationary bounded");});check(emitted[0]>0,"Vertical/stationary check must render a supported rune");}
 }
 private static Set<Integer> support(String rune){return switch(rune){case "blackflame"->Set.of(EMBER);case "warp"->Set.of(WIND);case "entropy"->Set.of(TIME);case "devour"->Set.of(BLOOD);case "malison","nullcatch","night_seam"->Set.of(ARCANE);default->Set.of();};}

 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
