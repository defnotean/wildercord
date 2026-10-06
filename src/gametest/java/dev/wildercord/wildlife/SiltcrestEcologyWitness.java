package dev.wildercord.wildlife;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Three simultaneous opportunities, with one failure latch and one unchanged post-coil allowance. */
final class SiltcrestEcologyWitness {
 static final int COHORTS=3;
 record Feed(List<JsonObject> rows,int errors,int omitted) {}
 record Fish(String uuid,boolean alive,boolean removed,float health,boolean damaged,String owner,String direct) {}
 private final List<SiltcrestPresentationWitness> policies;
 private AssertionError failure;
 SiltcrestEcologyWitness(List<String> sources) {
  require(sources.size()==COHORTS&&sources.stream().distinct().count()==COHORTS,"Exactly three different native sources own this fixed cohort");
  var result=new ArrayList<SiltcrestPresentationWitness>();
  for(int i=0;i<COHORTS;i++)result.add(new SiltcrestPresentationWitness(sources.get(i),"cohort_"+(i+1),true));policies=List.copyOf(result);
 }
 List<SiltcrestPresentationWitness.Candidate> inspect(List<Feed> feeds) {
  if(failure!=null)throw failure;
  try {
   require(feeds.size()==COHORTS,"No cohort may disappear from qualification");
   var next=new ArrayList<SiltcrestPresentationWitness.Candidate>();
   // Qualify every source before publishing any positive outcome.
   for(int i=0;i<COHORTS;i++){var feed=feeds.get(i);next.add(policies.get(i).inspect(feed.rows(),feed.errors(),feed.omitted()));}
   return java.util.Collections.unmodifiableList(next);
  }catch(AssertionError invalid){failure=invalid;throw invalid;}
 }
 static int winner(List<SiltcrestPresentationWitness.Candidate> candidates) {
  require(candidates.size()==COHORTS,"All three qualified outcomes are required before selection");
  for(int i=0;i<COHORTS;i++){var candidate=candidates.get(i);if(candidate!=null&&candidate.outcome()==SiltcrestPresentationWitness.Outcome.CAUGHT)return i;}
  return -1;
 }
 static boolean resolved(List<SiltcrestPresentationWitness.Candidate> candidates) {
  require(candidates.size()==COHORTS,"All three outcomes are required at hunt closure");
  return candidates.stream().noneMatch(c->c!=null&&c.outcome()==SiltcrestPresentationWitness.Outcome.PENDING);
 }
 static boolean auditFish(Fish fish,SiltcrestPresentationWitness.Candidate candidate,String source) {
  boolean earned=candidate!=null&&candidate.outcome()==SiltcrestPresentationWitness.Outcome.CAUGHT&&fish.uuid().equals(candidate.quarry());
  if(earned)require(!fish.alive()&&fish.health()<=0&&fish.damaged()&&source.equals(fish.owner())&&source.equals(fish.direct()),"Only the fully qualified native recipient may die, with the same source and direct owner");
  else require(fish.alive()&&!fish.removed()&&fish.health()==3&&!fish.damaged(),"Every other fish stays healthy; foreign damage/death cannot be hidden by a catch elsewhere");
  return earned;
 }
 static void auditRetainedDeath(Fish fish,boolean sameDamageObject) {
  // Vanilla clears getLastDamageSource after forty ticks and removes the corpse normally.
  require(!fish.alive()&&fish.health()<=0&&(!fish.damaged()||sameDamageObject),"A previously proven death permits normal corpse cleanup, never revival or another damage owner");
 }
 static final class Budget {
  private final SiltcrestPresentationWitness.Budget admission;
  private SiltcrestPresentationWitness.Budget hunt;
  private long firstCoil=-1;
  Budget(long started){admission=new SiltcrestPresentationWitness.Budget(started);}
  void observe(long now,long earliestCoil) {
   if(hunt==null){
    admission.observe(now);
    if(earliestCoil>=0){require(earliestCoil<=now,"An actual observed commitment starts the hunt clock");firstCoil=earliestCoil;hunt=new SiltcrestPresentationWitness.Budget(firstCoil);}
   }
   if(hunt!=null){require(earliestCoil==firstCoil,"Later cohorts and refusals cannot replace the first native coil");hunt.observe(now);}
  }
  void advance(){(hunt==null?admission:hunt).advance(null);}
  long deadline(){return (hunt==null?admission:hunt).deadline;}
  int advances(){return (hunt==null?admission:hunt).advances();}
  long firstCoil(){return firstCoil;}
 }
 private static void require(boolean condition,String why){if(!condition)throw new AssertionError(why);}
}
