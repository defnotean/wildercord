package dev.wildercord.wildlife;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Recorded native refusal plus synthetic classifier controls; these never simulate a Minecraft hunt. */
public final class SiltcrestPresentationWitnessChecks {
 private SiltcrestPresentationWitnessChecks() {}
 public static void main(String[] args){verify();System.out.println("SiltcrestPresentationWitnessChecks passed");}
 public static void verify() {
  var escaped=recorded();var result=inspect(escaped);
  check(result.outcome()==SiltcrestPresentationWitness.Outcome.ESCAPED&&result.strikeTick()==66&&result.huntReady()==266,"Recorded native escape retains its exact two-hundred-tick rest");
  for(int cut=0;cut<escaped.size();cut++)check(policy(escaped).inspect(escaped.subList(0,cut),0,0)==null||policy(escaped).inspect(escaped.subList(0,cut),0,0).outcome()==SiltcrestPresentationWitness.Outcome.PENDING,"Incomplete prefix cannot qualify a refusal");
  rejected(escaped,rows->rows.remove(0),"missing admission");
  rejected(escaped,rows->rows.remove(1),"missing strike entry");
  rejected(escaped,rows->rows.remove(2),"missing cancellation");
  rejected(escaped,rows->rows.get(2).addProperty("cancelPause",200),"wrong cancellation path");
  rejected(escaped,rows->rows.get(3).addProperty("huntReady",106),"shortened native cooldown");
  rejected(escaped,rows->rows.get(1).addProperty("quarryUuid","another fish"),"changed quarry");
  rejected(escaped,rows->rows.get(2).addProperty("sourceUuid","another bird"),"changed source");
  rejected(escaped,rows->rows.get(1).addProperty("world","another world"),"changed world");
  rejected(escaped,rows->rows.get(2).addProperty("ground",false),"unsupported source is not a lawful escape witness");
  rejected(escaped,rows->rows.get(2).getAsJsonObject("admittedFish").addProperty("health",2),"damaged quarry is not an untouched refusal");
  rejected(escaped,rows->rows.get(1).getAsJsonObject("admittedFish").addProperty("noAi",true),"frozen target cannot qualify");
  rejected(escaped,rows->{for(int i=1;i<4;i++)rows.get(i).getAsJsonObject("admittedFish").addProperty("committedDistanceSqr",.1);},"no actual locked-center escape");
  rejected(escaped,rows->rows.get(1).addProperty("threw",true),"thrown native action");
  rejected(escaped,rows->rows.get(2).remove("pool"),"missing predicate receipt");
  expectFailure(()->policy(escaped).inspect(escaped,1,0),"observer error");
  expectFailure(()->policy(escaped).inspect(escaped,0,1),"omitted receipt");

  var caught=successful(escaped);result=inspect(caught);
  check(result.outcome()==SiltcrestPresentationWitness.Outcome.CAUGHT&&result.huntReady()==6066,"Complete same-quarry native-call receipt shape earns the original appetite");
  rejected(caught,rows->rows.remove(2),"missing damage invocation");
  rejected(caught,rows->rows.remove(3),"missing damage return");
  rejected(caught,rows->rows.get(3).addProperty("damageResult",false),"refused real damage");
  rejected(caught,rows->rows.get(3).addProperty("damageSourceMatchesLast",false),"foreign death cannot earn appetite");
  rejected(caught,rows->rows.get(2).addProperty("damageTargetUuid","other"),"different damage recipient");
  rejected(caught,rows->rows.get(2).addProperty("damageAmount",5),"changed damage amount");
  rejected(caught,rows->rows.get(3).getAsJsonObject("admittedFish").addProperty("alive",true),"surviving quarry cannot qualify");
  rejected(caught,rows->rows.get(4).addProperty("left",7),"stretched strike cannot qualify");
  rejected(caught,rows->rows.get(4).addProperty("pendingPreen",false),"missing earned preen");
  rejected(caught,rows->rows.get(4).addProperty("huntReady",266),"failed-attempt rest cannot stand in for appetite");
  var later=shift(caught,215,2,266);var both=copy(escaped);both.addAll(later);
  check(inspect(both).outcome()==SiltcrestPresentationWitness.Outcome.CAUGHT,"A later naturally selected complete hunt may follow the consumed rest");
  var tooSoon=copy(escaped);tooSoon.addAll(shift(caught,214,2,266));expectFailure(()->inspect(tooSoon),"cooldown cannot be reset between candidates");
  var unseen=copy(escaped);unseen.addAll(shift(caught,215,3,266));expectFailure(()->inspect(unseen),"unobserved epoch cannot be skipped");
  check(SiltcrestPresentationWitness.phaseDeadline(500,100,80)==180&&SiltcrestPresentationWitness.phaseDeadline(500,490,80)==500,"Original strike cap clips to the single remaining budget");
  check(SiltcrestPresentationWitness.phaseDeadline(500,100,25)==125&&SiltcrestPresentationWitness.phaseDeadline(500,490,25)==500,"Original preen cap clips to the same remaining budget");
  budgets();
 }
 private static void budgets() {
  var stalled=new SiltcrestPresentationWitness.Budget(40);
  for(int tick=0;tick<500;tick++){stalled.observe(40);stalled.advance(null);}
  stalled.observe(40);check(stalled.advances()==500,"Exactly five hundred controlled advances exhaust even a stopped source clock");
  expectFailure(()->stalled.advance(null),"stalled clock must not permit advance five hundred and one");
  var backward=new SiltcrestPresentationWitness.Budget(40);backward.observe(41);
  expectFailure(()->backward.observe(40),"backward server clock cannot extend the absolute deadline");
  var serverFirst=new SiltcrestPresentationWitness.Budget(40);serverFirst.observe(540);
  expectFailure(()->serverFirst.advance(null),"server allowance can expire before controlled harness allowance");
  expectFailure(()->new SiltcrestPresentationWitness.Budget(40).observe(541),"a server jump beyond the deadline cannot qualify");
  for(int cap:new int[]{80,25}) {
   var stoppedPhase=new SiltcrestPresentationWitness.Budget(0);var limit=stoppedPhase.phase(cap);
   for(int tick=0;tick<cap;tick++){stoppedPhase.advance(limit);stoppedPhase.observe(0);}
   check(stoppedPhase.within(limit),"Final phase predicate may inspect its exact last controlled tick");
   expectFailure(()->stoppedPhase.advance(limit),"a stopped source clock cannot stretch the original "+cap+"-tick phase cap");
  }
  var shared=new SiltcrestPresentationWitness.Budget(32);
  for(int refusal=0;refusal<3;refusal++) {
   var phase=shared.phase(80);
   for(int tick=0;tick<16;tick++){shared.advance(phase);shared.observe(32+shared.advances());}
   if(refusal<2)for(int tick=0;tick<200;tick++){shared.advance(null);shared.observe(32+shared.advances());}
  }
  check(shared.advances()==448&&shared.deadline==532,"Three refused commitments and two complete rests consume one unchanged allowance");
  var remainder=shared.phase(80);
  check(remainder.serverTick()==532&&remainder.harnessAdvances()==500,"Later candidate phase is clipped to both remaining hunt allowances");
  for(int tick=0;tick<52;tick++){shared.advance(remainder);shared.observe(32+shared.advances());}
  expectFailure(()->shared.advance(shared.phase(80)),"another refusal or fresh phase cannot renew either exhausted hunt budget");
 }
 static List<JsonObject> recorded() {
  try(var stream=SiltcrestPresentationWitnessChecks.class.getResourceAsStream("/siltcrest/presentation-refusal.json")) {
   check(stream!=null,"Recorded refusal receipt resource exists");
   var data=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();var rows=new ArrayList<JsonObject>();
   for(var row:data.getAsJsonArray("events"))rows.add(row.getAsJsonObject());return rows;
  }catch(java.io.IOException e){throw new AssertionError(e);}
 }
 private static SiltcrestPresentationWitness policy(List<JsonObject> rows){return new SiltcrestPresentationWitness(rows.getFirst().get("sourceUuid").getAsString(),rows.getFirst().get("trial").getAsString());}
 private static SiltcrestPresentationWitness.Candidate inspect(List<JsonObject> rows){return policy(rows).inspect(rows,0,0);}
 private static List<JsonObject> copy(List<JsonObject> rows){var result=new ArrayList<JsonObject>();for(var row:rows)result.add(row.deepCopy());return result;}
 private static void rejected(List<JsonObject> original,Consumer<List<JsonObject>> change,String why){var rows=copy(original);change.accept(rows);expectFailure(()->policy(original).inspect(rows,0,0),why);}
 private static void expectFailure(Runnable action,String why){boolean failed=false;try{action.run();}catch(AssertionError expected){failed=true;}check(failed,"Invalid proof must fail: "+why);}
 static List<JsonObject> successful(List<JsonObject> nativeEscape) {
  var admission=nativeEscape.getFirst().deepCopy();var enter=nativeEscape.get(1).deepCopy();enter.add("admittedFish",admission.get("admittedFish").deepCopy());
  var call=enter.deepCopy();call.addProperty("event","damage_call");call.addProperty("huntReady",266);
  call.addProperty("damageTargetUuid",call.get("quarryUuid").getAsString());call.addProperty("damageTargetIsAdmitted",true);call.addProperty("damageAmount",4);
  call.add("damageResult",JsonNull.INSTANCE);call.addProperty("damageThrew",false);call.addProperty("damageSourceMatchesLast",false);call.add("damageTarget",call.get("admittedFish").deepCopy());
  var returned=call.deepCopy();returned.addProperty("event","damage_return");returned.addProperty("damageResult",true);returned.addProperty("damageSourceMatchesLast",true);returned.addProperty("pool",2);
  var dead=returned.getAsJsonObject("admittedFish");dead.addProperty("health",0);dead.addProperty("alive",false);dead.addProperty("wild",false);dead.addProperty("lastDamage","classifier-control mob damage");returned.add("damageTarget",dead.deepCopy());
  var exit=enter.deepCopy();exit.addProperty("event","strike_exit");exit.addProperty("huntReady",6066);exit.addProperty("pool",2);exit.addProperty("pendingPreen",true);
  exit.add("quarryUuid",JsonNull.INSTANCE);exit.add("committed",JsonNull.INSTANCE);exit.addProperty("sameAdmittedQuarry",false);exit.add("admittedFish",dead.deepCopy());exit.add("currentFish",JsonNull.INSTANCE);
  return new ArrayList<>(List.of(admission,enter,call,returned,exit));
 }
 private static List<JsonObject> shift(List<JsonObject> original,long ticks,int epochs,long admissionReady) {
  var rows=copy(original);
  for(var row:rows){row.addProperty("clock",row.get("clock").getAsLong()+ticks);row.addProperty("admittedTick",row.get("admittedTick").getAsLong()+ticks);
   row.addProperty("epoch",row.get("epoch").getAsInt()+epochs);row.addProperty("admittedEpoch",row.get("admittedEpoch").getAsInt()+epochs);
   long ready=row.get("huntReady").getAsLong();row.addProperty("huntReady",ready==0?admissionReady:ready+ticks);}
  return rows;
 }
 private static void check(boolean okay,String why){if(!okay)throw new AssertionError(why);}
}
