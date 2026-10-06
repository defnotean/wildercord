package dev.wildercord.wildlife;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

/** Classifies exact passive receipts. Only a complete, undamaged native escape may be skipped. */
final class SiltcrestPresentationWitness {
 enum Outcome { PENDING,ESCAPED,CAUGHT }
 record Candidate(int epoch,String quarry,long admittedTick,long strikeTick,long huntReady,Outcome outcome) {}
 private final String source,trial,suite;private final boolean ecology;
 private int consumed,step,epoch;private long admittedTick,strikeTick=-1,ready;
 private JsonObject admitted,entered,called,cancelled;
 private Outcome outcome=Outcome.PENDING;
 SiltcrestPresentationWitness(String source,String trial){this(source,trial,false);}
 SiltcrestPresentationWitness(String source,String trial,boolean ecology){this.source=source;this.trial=trial;this.ecology=ecology;suite=ecology?SiltcrestPresentationProbe.ECOLOGY_SUITE:SiltcrestPresentationProbe.SUITE;}

 Candidate inspect(List<JsonObject> rows,int errors,int omitted) {
  require(errors==0&&omitted==0,"Complete error-free native receipts own qualification");
  require(rows.size()>=consumed,"Native receipt history must not reset");
  while(consumed<rows.size())accept(rows.get(consumed++));
  return admitted==null?null:new Candidate(epoch,text(admitted,"quarryUuid"),admittedTick,strikeTick,ready,outcome);
 }
 private void accept(JsonObject row) {
  require(text(row,"suite").equals(suite)&&text(row,"trial").equals(trial)
    &&text(row,"sourceUuid").equals(source)&&!bool(row,"threw"),"Exact source/trial and normal native return own every receipt");
  String event=text(row,"event");
  if(event.equals("coil_admitted")) {
   require(admitted==null||outcome==Outcome.ESCAPED,"A new commitment cannot replace an unresolved or successful hunt");
   if(admitted!=null)require(number(row,"clock")>=ready&&integer(row,"epoch")==epoch+2,"A later natural commitment consumes the real failed-attempt cooldown");
   admitted=row;epoch=integer(row,"epoch");admittedTick=number(row,"clock");strikeTick=-1;ready=0;
   entered=null;called=null;cancelled=null;step=1;outcome=Outcome.PENDING;
   identity(row,true);source(row,3);fish(row,true);
   require(integer(row,"pose")==SiltcrestBittern.COILING&&integer(row,"phase")==0&&integer(row,"left")==SiltcrestBittern.COIL
     &&!bool(row,"pendingPreen")&&admittedTick>=number(row,"huntReady"),"Original ready grounded sixteen-tick coil admission");
   var target=object(row,"admittedFish");
   require(bool(target,"loadedSight")&&decimal(target,"sourceDistanceSqr")<=3.24&&decimal(target,"committedDistanceSqr")==0
     &&value(row,"committed").equals(value(target,"center")),"Admission locks the actual eligible nearby quarry center");
   return;
  }
  require(admitted!=null,"Native action cannot precede its observed commitment");identity(row,false);
  switch(event) {
   case "strike_enter" -> {
    require(step==1&&outcome==Outcome.PENDING,"One real strike follows each commitment");
    strikeTick=number(row,"clock");
    require(strikeTick-admittedTick==SiltcrestBittern.COIL-1,"Original coil resolves on its native final tick");
    liveCommitment(row);source(row,3);fish(row,true);entered=row;step=2;
   }
   case "damage_call" -> {
    require(step==2,"Only one native damage call follows a valid strike gate");nativeStrike(row);source(row,3);fish(row,true);inReach(row);
    require(number(row,"huntReady")==strikeTick+200,"Native strike sets its original failed-attempt rest before damage");
    damage(row);require(value(row,"damageResult").isJsonNull()&&!bool(row,"damageThrew"),"Damage invocation precedes its real result");called=row;step=3;
   }
   case "damage_return" -> {
    require(step==3,"Damage result belongs to the single observed native invocation");nativeStrike(row);source(row,2);fish(row,false);inReach(row);damage(row);
    require(bool(row,"damageResult")&&!bool(row,"damageThrew")&&bool(row,"damageSourceMatchesLast"),"Original native damage result and exact source own the death");
    require(decimal(object(row,"damageTarget"),"health")<decimal(object(called,"damageTarget"),"health")
      &&value(row,"damageTarget").equals(value(row,"admittedFish")),"The same admitted quarry actually loses health and dies");step=4;
   }
   case "cancel_enter" -> {
    require(step==2&&outcome==Outcome.PENDING,"Only a pre-damage final strike refusal may be nonqualifying");
    nativeStrike(row);source(row,3);fish(row,true);
    require(integer(row,"cancelPause")==40&&number(row,"huntReady")==strikeTick+200,"Exact native failed-strike cancellation retains two hundred ticks");
    var target=object(row,"admittedFish");
    require((decimal(target,"committedDistanceSqr")>.36||ecology&&(decimal(target,"sourceDistanceSqr")>3.24||!bool(target,"loadedSight")))
      &&value(row,"admittedFish").equals(value(entered,"admittedFish")),"The live native quarry actually escaped the locked center before any damage");
    cancelled=row;step=5;
   }
   case "strike_exit" -> {
    require(number(row,"clock")==strikeTick&&integer(row,"phase")==0&&value(row,"quarryUuid").isJsonNull()
      &&value(row,"committed").isJsonNull(),"Same-tick native strike exit clears its original commitment");
    if(step==5) {
     source(row,3);fish(row,true);
     require(integer(row,"epoch")==epoch+1&&integer(row,"pose")==SiltcrestBittern.IDLE&&integer(row,"left")==0
       &&!bool(row,"pendingPreen")&&number(row,"huntReady")==strikeTick+200
       &&value(row,"admittedFish").equals(value(cancelled,"admittedFish")),"Observed untouched escape completes its exact cancellation");
     ready=number(row,"huntReady");outcome=Outcome.ESCAPED;
    }else {
     require(step==4,"A strike cannot complete without either the exact legal refusal or the real kill");source(row,2);fish(row,false);inReach(row);
     require(integer(row,"epoch")==epoch&&integer(row,"pose")==SiltcrestBittern.STRIKING&&integer(row,"left")==6
       &&bool(row,"pendingPreen")&&number(row,"huntReady")==strikeTick+SiltcrestBittern.APPETITE,"The actual kill earns appetite and the unchanged six-tick strike");
     ready=number(row,"huntReady");outcome=Outcome.CAUGHT;
    }
    step=6;
   }
   default -> throw new AssertionError("Unexplained native receipt: "+event);
  }
 }
 private void identity(JsonObject row,boolean admission) {
  require(integer(row,"admittedEpoch")==epoch&&number(row,"admittedTick")==admittedTick
    &&value(row,"admittedLocked").equals(value(admitted,"committed"))
    &&text(row,"world").equals(text(admitted,"world"))&&integer(row,"sourceId")==integer(admitted,"sourceId"),"Original commitment/source/world identity survives every receipt");
  require(text(object(row,"admittedFish"),"uuid").equals(text(admitted,"quarryUuid")),"Original naturally selected quarry owns every receipt");
  if(admission)require(bool(row,"sameAdmittedQuarry"),"Admission retains its exact quarry object");
 }
 private void source(JsonObject row,int pool) {
  require(bool(row,"sourceAlive")&&!bool(row,"sourceRemoved")&&bool(row,"sourceTracked")&&bool(row,"ground")&&!bool(row,"water")
    &&bool(row,"night")&&!bool(row,"raining")&&!bool(row,"disturbed")&&(ecology?integer(row,"pool")==pool:integer(row,"pool")>=pool)
    &&decimal(row,"sourceHealth")==decimal(admitted,"sourceHealth")&&value(row,"sourcePosition").equals(value(admitted,"sourcePosition")),"The original healthy supported source remains undisturbed and still through its commitment");
 }
 private void fish(JsonObject row,boolean alive) {
  var fish=object(row,"admittedFish");
  require(bool(fish,"sameWorld")&&!bool(fish,"removed")&&bool(fish,"water")&&!bool(fish,"fromBucket")&&!bool(fish,"named")
    &&!bool(fish,"persistent")&&!bool(fish,"noAi")&&bool(fish,"loaded")&&(ecology||bool(fish,"loadedSight")),"Original unowned loaded ordinary-AI quarry remains in the supplied water; presentation also requires sight");
  require(bool(fish,"alive")==alive&&bool(fish,"wild")==alive
    &&(alive?decimal(fish,"health")==3:decimal(fish,"health")<=0),"Refusal preserves the healthy fish; successful damage owns its death");
 }
 private void liveCommitment(JsonObject row) {
  require(integer(row,"epoch")==epoch&&bool(row,"sameAdmittedQuarry")&&text(row,"quarryUuid").equals(text(admitted,"quarryUuid"))
    &&value(row,"committed").equals(value(admitted,"committed"))&&integer(row,"pose")==SiltcrestBittern.STRIKING
    &&integer(row,"phase")==0&&integer(row,"left")==6&&!bool(row,"pendingPreen"),"Original live commitment enters the native six-tick strike unchanged");
 }
 private void nativeStrike(JsonObject row){liveCommitment(row);require(number(row,"clock")==strikeTick,"Synchronous native strike boundary retains its source clock");}
 private void inReach(JsonObject row){var fish=object(row,"admittedFish");require(bool(fish,"loadedSight")&&decimal(fish,"committedDistanceSqr")<=.36&&decimal(fish,"sourceDistanceSqr")<=3.24,"Actual successful quarry satisfies sight and both native reach gates");}
 private void damage(JsonObject row) {
  require(bool(row,"damageTargetIsAdmitted")&&text(row,"damageTargetUuid").equals(text(admitted,"quarryUuid"))&&decimal(row,"damageAmount")==4,"Exactly the admitted quarry receives the original four-damage native call");
  if(ecology)require(text(row,"damageSourceUuid").equals(source)&&text(row,"damageDirectUuid").equals(source),"The same native bird owns both direct and responsible damage identity");
 }
 static long phaseDeadline(long deadline,long now,int cap){return Math.min(deadline,now+cap);}
 /** Neither a stopped server clock nor another rejected commitment can renew the original allowance. */
 static final class Budget {
  record Limit(long serverTick,int harnessAdvances) {}
  final long deadline;private long clock;private int advances;
  Budget(long started){clock=started;deadline=Math.addExact(started,500);}
  void observe(long now){
   require(now>=clock,"Native source clock must not run backward during the hunt: prior="+clock+" now="+now);
   require(now<=deadline&&advances<=500,"Natural witness stays within both original absolute hunt allowances: now="+now+" serverDeadline="+deadline+" harnessAdvances="+advances);clock=now;
  }
  Limit phase(int cap){require(cap>0,"Positive original phase cap");return new Limit(phaseDeadline(deadline,clock,cap),Math.min(500,advances+cap));}
  boolean within(Limit limit){return limit!=null&&clock<=limit.serverTick()&&advances<=limit.harnessAdvances();}
  void advance(Limit phase){
   require(clock<deadline&&advances<500,"No complete natural witness before the server or controlled-harness hunt allowance expired: clock="+clock+" serverDeadline="+deadline+" harnessAdvances="+advances);
   require(phase==null||clock<phase.serverTick()&&advances<phase.harnessAdvances(),"Original phase allowance expired before its actual witness");
   advances++;
  }
  int advances(){return advances;}
 }
 private static JsonElement value(JsonObject row,String key){require(row!=null&&row.has(key),"Missing native receipt field: "+key);return row.get(key);}
 private static JsonObject object(JsonObject row,String key){var v=value(row,key);require(v.isJsonObject(),"Missing native receipt object: "+key);return v.getAsJsonObject();}
 private static String text(JsonObject row,String key){var v=value(row,key);require(!v.isJsonNull(),"Missing native receipt text: "+key);return v.getAsString();}
 private static boolean bool(JsonObject row,String key){return value(row,key).getAsBoolean();}
 private static int integer(JsonObject row,String key){return value(row,key).getAsInt();}
 private static long number(JsonObject row,String key){return value(row,key).getAsLong();}
 private static double decimal(JsonObject row,String key){return value(row,key).getAsDouble();}
 private static void require(boolean okay,String why){if(!okay)throw new AssertionError(why);}
}
