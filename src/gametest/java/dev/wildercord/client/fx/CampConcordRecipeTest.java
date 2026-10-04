package dev.wildercord.client.fx;
import dev.wildercord.cast.*;
import dev.wildercord.content.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Recipe/wire contracts only; native renders and actual owner outcomes are accepted by separate suites. */
public final class CampConcordRecipeTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){var identities=new HashSet<String>();for(String rune:CampConcordForms.RUNES)for(boolean minimal:new boolean[]{true,false}){
  var evolution=new HashSet<String>();for(int beat:new int[]{0,4,8}){var draw=new ArrayList<String>();CampConcordForms.prepare("wildercord:"+rune,beat,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(o,p)->{check(o instanceof CampOption&&p.length()<1&&Double.isFinite(p.lengthSqr()),"Original bounded prep material");draw.add(o+"@"+p);});check(!draw.isEmpty()&&draw.size()<=14,"Bounded nonempty recipe");evolution.add(draw.toString());if(beat==4&&!minimal)check(identities.add(draw.toString()),"Two effect preparations have different material identities");}check(evolution.size()>1,"Actual authored evolution across prep beats");
  var draw=new ArrayList<String>();CampConcordForms.flight("wildercord:"+rune,4,1,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{check(o instanceof CampOption&&((CampOption)o).lifetime()==5&&p.length()<1,"Dedicated finite moving body, no luminous orb");draw.add(o+"@"+p);});check(draw.size()>2&&draw.size()<=14,"Bounded material flight");
 }
 check(!CampConcordForms.supports("other:watchweft")&&!CampConcordForms.supports("wildercord:heal"),"Exact new dispatch cannot replace an existing rune");
 var source=UUID.randomUUID();var target=UUID.randomUUID();for(int phase=0;phase<8;phase++){boolean watch=phase<=CampConcordFx.END;var e=new CampConcordFx.Event(source,watch?source:target,"minecraft:overworld",Vec3.ZERO,watch?Vec3.ZERO:new Vec3(2,0,0),phase,phase==CampConcordFx.TRANSFER?16:0,10);var parts=new ArrayList<CampOption>();CampConcordForms.outcome(e,4,false,(o,p)->parts.add((CampOption)o));check(!parts.isEmpty()&&parts.size()<=14,"Every finite owner phase has original material");if(phase==CampConcordFx.OFFER||phase==CampConcordFx.DECLINE)check(parts.stream().noneMatch(p->p.style()==CampOption.KNOT),"No success knot before actual credit");}
 }
 private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
}
