package dev.wildercord.wildlife;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Classifier and fixed-budget adversarial controls, not a replacement for a real client run. */
public final class SiltcrestEcologyWitnessChecks {
 private SiltcrestEcologyWitnessChecks() {}
 public static void main(String[] args){verify();System.out.println("SiltcrestEcologyWitnessChecks passed");}
 public static void verify() {
  var one=caught(0);var two=escaped(1);var three=escaped(2);
  var outcomes=group().inspect(feeds(one,two,three));
  check(SiltcrestEcologyWitness.winner(outcomes)==0&&SiltcrestEcologyWitness.resolved(outcomes),"One exact real-call shape and two fully classified refusals qualify the fixed batch");
  check(SiltcrestEcologyWitness.winner(group().inspect(feeds(escaped(0),two,three)))<0,"Three lawful refusals never pass the positive gate");
  var pending=two.subList(0,1);
  check(!SiltcrestEcologyWitness.resolved(group().inspect(feeds(one,pending,three))),"A valid winner cannot hide an unresolved other-cohort commitment at daylight transition");
  for(String owner:List.of("damageSourceUuid","damageDirectUuid")) {
   var wrong=caught(1);wrong.get(2).addProperty(owner,"foreign");
   var policy=group();expectFailure(()->policy.inspect(feeds(one,wrong,three)),"Foreign "+owner+" in another source fails despite a valid first cohort");
   expectFailure(()->policy.inspect(feeds(one,caught(1),three)),"A later valid result cannot erase the latched batch failure");
  }
  var swollen=caught(0);for(var row:swollen)row.addProperty("pool",row.get("pool").getAsInt()+1);
  expectFailure(()->group().inspect(feeds(swollen,two,three)),"Four-to-three is not the promised exact three-to-two population proof");
  var missing=caught(1);missing.remove(2);
  expectFailure(()->group().inspect(feeds(one,missing,three)),"A dead fish without its damage invocation fails the entire batch");
  var foreign=caught(1);foreign.get(3).addProperty("damageSourceMatchesLast",false);
  expectFailure(()->group().inspect(feeds(one,foreign,three)),"A foreign final damage owner is never accepted");
  var range=escaped(1);for(int i=1;i<range.size();i++){var fish=range.get(i).getAsJsonObject("admittedFish");fish.addProperty("committedDistanceSqr",.31);fish.addProperty("sourceDistanceSqr",4.86);}
  check(group().inspect(feeds(one,range,three)).get(1).outcome()==SiltcrestPresentationWitness.Outcome.ESCAPED,"Ordinary reach-only final refusal is classified without demanding a fabricated locked-center escape");
  var sight=escaped(1);for(int i=1;i<sight.size();i++){var fish=sight.get(i).getAsJsonObject("admittedFish");fish.addProperty("committedDistanceSqr",.1);fish.addProperty("sourceDistanceSqr",2);fish.addProperty("loadedSight",false);}
  check(group().inspect(feeds(one,sight,three)).get(1).outcome()==SiltcrestPresentationWitness.Outcome.ESCAPED,"An observed healthy moving quarry can lawfully lose sight before damage");
  var unexplained=escaped(1);for(int i=1;i<unexplained.size();i++){var fish=unexplained.get(i).getAsJsonObject("admittedFish");fish.addProperty("committedDistanceSqr",.1);fish.addProperty("sourceDistanceSqr",2);}
  expectFailure(()->group().inspect(feeds(one,unexplained,three)),"A refusal without any native failed predicate cannot be silently skipped");
  var injured=escaped(1);for(int i=1;i<injured.size();i++)injured.get(i).getAsJsonObject("admittedFish").addProperty("health",2);
  expectFailure(()->group().inspect(feeds(one,injured,three)),"A hurt quarry cannot masquerade as an untouched lawful refusal");
  var noSight=caught(0);noSight.get(2).getAsJsonObject("admittedFish").addProperty("loadedSight",false);
  expectFailure(()->group().inspect(feeds(noSight,two,three)),"Ecology sight refusals do not weaken successful-hit admission");
  expectFailure(()->group().inspect(List.of(feed(one),feed(two))),"Missing third cohort is not a smaller fallback batch");
  expectFailure(()->group().inspect(List.of(feed(one),new SiltcrestEcologyWitness.Feed(two,1,0),feed(three))),"Other-cohort observation error fails the whole batch");
  var success=outcomes.getFirst();var victim=success.quarry();
  check(SiltcrestEcologyWitness.auditFish(new SiltcrestEcologyWitness.Fish(victim,false,false,0,true,"source_0","source_0"),success,"source_0"),"A complete native receipt owns its actual dead fish");
  expectFailure(()->SiltcrestEcologyWitness.auditFish(new SiltcrestEcologyWitness.Fish(victim,false,false,0,true,"source_0","source_0"),null,"source_0"),"Actual fish death without a complete native receipt fails the independent population audit");
  expectFailure(()->SiltcrestEcologyWitness.auditFish(new SiltcrestEcologyWitness.Fish("other_fish",false,false,0,true,"source_0","source_0"),success,"source_0"),"Another fish cannot be substituted for the recorded recipient");
  expectFailure(()->SiltcrestEcologyWitness.auditFish(new SiltcrestEcologyWitness.Fish(victim,false,false,0,true,"foreign","source_0"),success,"source_0"),"Later population audit also rejects a changed final owner");
  expectFailure(()->SiltcrestEcologyWitness.auditFish(new SiltcrestEcologyWitness.Fish("other_fish",true,false,2,true,"foreign","foreign"),success,"source_0"),"Nonfatal foreign harm elsewhere is a failure too");
  SiltcrestEcologyWitness.auditRetainedDeath(new SiltcrestEcologyWitness.Fish(victim,false,true,0,false,null,null),false);
  expectFailure(()->SiltcrestEcologyWitness.auditRetainedDeath(new SiltcrestEcologyWitness.Fish(victim,false,false,0,true,"foreign","foreign"),false),"A previously proven corpse cannot acquire a different damage object");
  expectFailure(()->SiltcrestEcologyWitness.auditRetainedDeath(new SiltcrestEcologyWitness.Fish(victim,true,false,3,false,null,null),false),"A proven victim cannot be revived or substituted");
  scopes();
  budgets();
 }
 private static void scopes() {
  var world=new Object();var birds=List.of(new Object(),new Object(),new Object());var thread=Thread.currentThread();
  var sessions=SiltcrestPresentationProbe.installEcology(world,birds,thread,line->{});
  try {
   for(int i=0;i<3;i++)check(SiltcrestPresentationProbe.selected(SiltcrestPresentationProbe.ECOLOGY_SUITE,world,birds.get(i),thread)==sessions.get(i),"All three exact source scopes remain simultaneously observable");
   check(SiltcrestPresentationProbe.selected(SiltcrestPresentationProbe.SUITE,world,birds.getFirst(),thread)==null,"Ecology scope cannot consume presentation receipts");
   check(SiltcrestPresentationProbe.selected(SiltcrestPresentationProbe.ECOLOGY_SUITE,new Object(),birds.getFirst(),thread)==null,"Foreign world is refused");
   check(SiltcrestPresentationProbe.selected(SiltcrestPresentationProbe.ECOLOGY_SUITE,world,birds.getFirst(),new Thread())==null,"Foreign thread is refused");
   expectFailure(()->SiltcrestPresentationProbe.installEcology(world,birds,thread,line->{}),"An overlapping cohort scope cannot replace the current observation");
  }finally{SiltcrestPresentationProbe.endEcology(sessions);}
  for(var bird:birds)check(SiltcrestPresentationProbe.selected(SiltcrestPresentationProbe.ECOLOGY_SUITE,world,bird,thread)==null,"Every closed cohort stops observing");
  expectFailure(()->SiltcrestPresentationProbe.installEcology(world,birds.subList(0,2),thread,line->{}),"Scope cannot silently reduce the fixed cohort count");
 }
 private static void budgets() {
  int hunt=SiltcrestPresentationWitness.Budget.HUNT;
  var shared=new SiltcrestEcologyWitness.Budget(40);shared.observe(40,-1);shared.advance();shared.observe(51,51);
  check(shared.deadline()==51+hunt&&shared.firstCoil()==51,"One absolute deadline begins at the earliest native coil, before capture work");
  shared.observe(57,51);check(shared.deadline()==51+hunt,"Six server ticks of capture work consume the original allowance without renewal");
  expectFailure(()->shared.observe(70,66),"A later cohort cannot supply a new first-coil deadline");
  var late=new SiltcrestEcologyWitness.Budget(40);late.observe(51,51);late.observe(51+hunt,51);
  expectFailure(late::advance,"No server tick beyond the hunt allowance even if another pond has not succeeded");
  expectFailure(()->late.observe(52+hunt,51),"Late positive receipts cannot extend the shared server deadline");
  var frozen=new SiltcrestEcologyWitness.Budget(40);frozen.observe(51,51);
  for(int i=0;i<hunt;i++){frozen.advance();frozen.observe(51,51);}
  check(frozen.advances()==hunt,"All cohorts share exactly the hunt allowance of controlled advances even when source time is stalled");
  expectFailure(frozen::advance,"No harness advance beyond the hunt allowance");
  var absent=new SiltcrestEcologyWitness.Budget(40);absent.observe(40+hunt,-1);expectFailure(absent::advance,"Original initial hunt-allowance admission cap also remains bounded");
  var backwards=new SiltcrestEcologyWitness.Budget(40);backwards.observe(51,51);expectFailure(()->backwards.observe(50,51),"Server time cannot run backward to renew the shared window");
 }
 private static List<JsonObject> caught(int index){return scope(SiltcrestPresentationWitnessChecks.successful(SiltcrestPresentationWitnessChecks.recorded()),index);}
 private static List<JsonObject> escaped(int index){return scope(SiltcrestPresentationWitnessChecks.recorded(),index);}
 private static List<JsonObject> scope(List<JsonObject> input,int index) {
  var rows=new ArrayList<JsonObject>();
  for(var old:input){var row=old.deepCopy();row.addProperty("suite",SiltcrestPresentationProbe.ECOLOGY_SUITE);row.addProperty("trial","cohort_"+(index+1));row.addProperty("sourceUuid","source_"+index);
   if(row.has("damageAmount")){row.addProperty("damageSourceUuid","source_"+index);row.addProperty("damageDirectUuid","source_"+index);}rows.add(row);}
  return rows;
 }
 private static SiltcrestEcologyWitness group(){return new SiltcrestEcologyWitness(List.of("source_0","source_1","source_2"));}
 private static SiltcrestEcologyWitness.Feed feed(List<JsonObject> rows){return new SiltcrestEcologyWitness.Feed(rows,0,0);}
 private static List<SiltcrestEcologyWitness.Feed> feeds(List<JsonObject> one,List<JsonObject> two,List<JsonObject> three){return List.of(feed(one),feed(two),feed(three));}
 private static void expectFailure(Runnable task,String why){boolean failed=false;try{task.run();}catch(AssertionError expected){failed=true;}check(failed,"Invalid evidence must fail: "+why);}
 private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
}
