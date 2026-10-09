package dev.wildercord.cast;

import dev.wildercord.content.LifeOption;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.RuneFamily;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** native roster and material trace gate; separate from real delivery acceptance. */
public final class LifeOutcomeRecipeTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  var dedicated=new HashSet<>(LifeOutcomes.RUNES);dedicated.add("root_carry");
  check(LifeOutcomes.RUNES.size()==30&&dedicated.size()==31,"Thirty observed Life owners plus one dedicated Root Carry owner");
  check(dev.wildercord.cast.feel.Signatures.get("wildercord:root_carry")!=null&&dev.wildercord.cast.feel.Signatures.get("wildercord:root_carry").ownsOutcomeBody(),"Root Carry uses its actual dedicated outcome signature");
  check(dev.wildercord.client.fx.LifeRuntimePartitionChecks.genericPaths().equals(dedicated),"Thirty observed Life owners plus Root Carry form generic31; separate Excise ordinary/presentation acceptance remains mandatory");
  var identities=new HashSet<String>();
  for(var rune:LifeOutcomes.RUNES) {
   var identity=new ArrayList<String>();
   for(boolean minimal:new boolean[]{false,true})for(var moment:List.of(LifeOutcomes.Moment.APPLY,LifeOutcomes.Moment.PULSE,LifeOutcomes.Moment.TRIGGER,LifeOutcomes.Moment.END)) {
    var trace=new ArrayList<String>();
    LifeOutcomes.draw(new LifeOutcomes.Observation(rune,moment,Vec3.ZERO,new Vec3(0,0,4),3,2,6),minimal,(option,at)->{
     check(!(option instanceof LightOption),"Life outcomes never use generic luminous shapes: "+rune);
     check(Double.isFinite(at.lengthSqr())&&at.length()<2,"Finite bounded local geometry: "+rune);
     check(option instanceof LifeOption||option instanceof MaterialOption,"Original Life/supporting material option: "+rune);
     trace.add(option.toString()+"@"+at);
    });boolean actualPhase=!rune.equals("pulse_ferry")||moment==LifeOutcomes.Moment.PULSE;check((actualPhase?!trace.isEmpty():trace.isEmpty())&&trace.size()<64,"Only owned phases have bounded material: "+rune);identity.add(String.join(";",trace));
   }
   check(identities.add(String.join("/",identity)),"Individually authored outcome identity: "+rune);
   var refused=new ArrayList<String>();LifeOutcomes.draw(new LifeOutcomes.Observation(rune,LifeOutcomes.Moment.REFUSED,Vec3.ZERO,null,0,0,0),false,(option,at)->refused.add(option.toString()));check(refused.isEmpty(),"Refusal cannot pretend successful mutation: "+rune);
  }
  for(String rune:List.of("grow","harvest","glimmer","ancient_seed","glowvine","root_bulwark")) {
   var unchanged=new ArrayList<String>();LifeOutcomes.draw(new LifeOutcomes.Observation(rune,LifeOutcomes.Moment.APPLY,Vec3.ZERO,null,0,0,0),false,(option,at)->unchanged.add(option.toString()));check(unchanged.isEmpty(),"Unchanged world has no successful outcome material: "+rune);
  }
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
