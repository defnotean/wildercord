package dev.wildercord.wildlife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Passive GameTest receipts. No AI, path, RNG, damage, pose or clock operation is supplied here. */
public final class SiltcrestPresentationProbe {
 static final String SUITE="dev.wildercord.wildlife.SiltcrestPresentationTest";
 static final String ECOLOGY_SUITE="dev.wildercord.wildlife.SiltcrestEcologyTest";
 static final int MAX_RECORDS=64;
 private static volatile Session active;
 private static volatile List<Session> ecology=List.of();
 private SiltcrestPresentationProbe() {}

 static Session begin(ServerLevel level,SiltcrestBittern bird,String trial) {
  return install(SUITE,level,bird,trial,Thread.currentThread(),line -> System.out.println("SILTCREST_PRESENTATION "+line));
 }
 static synchronized List<Session> beginEcology(ServerLevel level,List<SiltcrestBittern> birds) {
  return installEcology(level,birds,Thread.currentThread(),line->System.out.println("SILTCREST_ECOLOGY_NATIVE "+line));
 }
 static synchronized List<Session> installEcology(Object level,List<?> birds,Thread owner,Consumer<String> sink) {
  if(active!=null||!ecology.isEmpty()||birds.size()!=3||birds.stream().distinct().count()!=3)throw new AssertionError("Exactly three isolated ecology sources own one observation scope");
  var sessions=new ArrayList<Session>();
  for(int i=0;i<birds.size();i++)sessions.add(new Session(ECOLOGY_SUITE,level,birds.get(i),"cohort_"+(i+1),owner,sink,null));
  ecology=List.copyOf(sessions);return ecology;
 }
 static synchronized void endEcology(List<Session> sessions) {
  if(ecology!=sessions)throw new AssertionError("Only the owning ecology group may close its observation scope");
  ecology=List.of();for(var session:sessions)session.close();
 }
 static synchronized Session install(String suite,Object world,Object bird,String trial,Thread owner,Consumer<String> sink) {
  var session=new Session(suite,world,bird,trial,owner,sink,active);active=session;return session;
 }
 static boolean matches(Session s,String suite,Object world,Object bird,Thread thread) {
  return s!=null&&!s.closed&&(SUITE.equals(suite)||ECOLOGY_SUITE.equals(suite))&&suite.equals(s.suite)&&s.world==world&&s.bird==bird&&s.owner==thread;
 }
 static Session selected(String suite,Object world,Object bird,Thread thread) {
  if(ECOLOGY_SUITE.equals(suite))return ecology.stream().filter(s->matches(s,suite,world,bird,thread)).findFirst().orElse(null);
  var s=active;return matches(s,suite,world,bird,thread)?s:null;
 }
 private static String currentSuite() {
  var test=FabricClientGameTestRunner.currentlyRunningGameTest;return test==null?"none":test.getDefinition();
 }

 /** Every value is read at the real native boundary, before cancellation clears the commitment. */
 public static void observe(SiltcrestBittern bird,String event,AbstractFish quarry,Vec3 committed,int epoch,int left,
                            boolean pendingPreen,Integer pause,AbstractFish damaged,DamageSource damage,Float amount,Boolean result,boolean threw) {
  if(active==null&&ecology.isEmpty())return;
  var s=selected(currentSuite(),bird.level(),bird,Thread.currentThread());if(s==null)return;
  try {
   if(event.equals("coil_admitted")){s.admitted=quarry;s.locked=committed;s.admittedEpoch=epoch;s.admittedTick=SiltcrestBittern.clock(bird.level());}
   if(event.equals("damage_call"))s.damageCalls++;
   if(event.equals("damage_return"))s.damageReturns++;
   if(event.equals("cancel_enter"))s.cancellations++;
   if(s.records.size()>=MAX_RECORDS){s.omitted++;return;}
   var level=(ServerLevel)bird.level();var out=new JsonObject();
   out.addProperty("event",event);out.addProperty("suite",s.suite);out.addProperty("trial",s.trial);
   out.addProperty("clock",SiltcrestBittern.clock(level));out.addProperty("entityTick",bird.tickCount);
   out.addProperty("world",level.dimension().identifier().toString());out.addProperty("sourceId",bird.getId());
   out.addProperty("sourceUuid",bird.getUUID().toString());out.add("sourcePosition",vector(bird.position()));
   out.addProperty("sourceAlive",bird.isAlive());out.addProperty("sourceRemoved",bird.isRemoved());out.addProperty("sourceHealth",bird.getHealth());
   out.addProperty("sourceTracked",level.getEntity(bird.getUUID())==bird);out.addProperty("ground",bird.onGround());out.addProperty("water",bird.isInWater());
   out.addProperty("pose",bird.pose());out.addProperty("phase",bird.phase());out.addProperty("epoch",epoch);out.addProperty("left",left);
   out.addProperty("pendingPreen",pendingPreen);out.addProperty("huntReady",bird.huntReady());out.addProperty("controlReady",bird.controlReady());
   out.addProperty("night",WetlandRules.night(level.getOverworldClockTime()));out.addProperty("raining",level.isRaining());
   out.addProperty("disturbed",bird.disturbed(level));out.addProperty("pool",bird.preyPool(level).size());
   out.addProperty("quarryUuid",quarry==null?null:quarry.getUUID().toString());out.add("committed",vector(committed));
   out.addProperty("admittedEpoch",s.admittedEpoch);out.addProperty("admittedTick",s.admittedTick);out.add("admittedLocked",vector(s.locked));
   out.addProperty("sameAdmittedQuarry",quarry!=null&&quarry==s.admitted);out.add("admittedFish",fish(bird,level,s.admitted,s.locked));
   if(quarry!=s.admitted)out.add("currentFish",fish(bird,level,quarry,committed));
   if(pause!=null)out.addProperty("cancelPause",pause);
   if(damaged!=null){
    out.addProperty("damageTargetUuid",damaged.getUUID().toString());out.addProperty("damageTargetIsAdmitted",damaged==s.admitted);
    out.addProperty("damageAmount",amount);out.addProperty("damageResult",result);out.addProperty("damageThrew",threw);
    out.addProperty("damageSourceUuid",damage.getEntity()==null?null:damage.getEntity().getUUID().toString());
    out.addProperty("damageDirectUuid",damage.getDirectEntity()==null?null:damage.getDirectEntity().getUUID().toString());
    out.addProperty("damageSourceMatchesLast",damaged.getLastDamageSource()==damage);out.add("damageTarget",fish(bird,level,damaged,s.locked));
   }
   out.addProperty("threw",threw);s.retain(out);
  }catch(Throwable ignored){s.errors++;} // Diagnostics never replace the native result or fixture failure.
 }
 private static JsonObject fish(SiltcrestBittern bird,ServerLevel level,AbstractFish fish,Vec3 locked) {
  if(fish==null)return null;
  var out=new JsonObject();out.addProperty("id",fish.getId());out.addProperty("uuid",fish.getUUID().toString());
  out.add("position",vector(fish.position()));out.add("center",vector(fish.getBoundingBox().getCenter()));out.add("velocity",vector(fish.getDeltaMovement()));
  out.addProperty("sameWorld",fish.level()==level);out.addProperty("alive",fish.isAlive());out.addProperty("removed",fish.isRemoved());
  out.addProperty("health",fish.getHealth());out.addProperty("water",fish.isInWater());out.addProperty("fromBucket",fish.fromBucket());
  out.addProperty("named",fish.hasCustomName());out.addProperty("persistent",fish.isPersistenceRequired());out.addProperty("noAi",fish.isNoAi());
  out.addProperty("loaded",fish.level().hasChunkAt(fish.blockPosition()));out.addProperty("wild",SiltcrestBittern.wildFish(fish,level));
  out.addProperty("sourceDistanceSqr",bird.distanceToSqr(fish));out.addProperty("loadedSight",fish.level()==level&&bird.loadedSight(fish));
  if(locked!=null)out.addProperty("committedDistanceSqr",fish.getBoundingBox().getCenter().distanceToSqr(locked));
  var prior=fish.getLastDamageSource();out.addProperty("lastDamage",prior==null?null:prior.toString());return out;
 }
 private static JsonArray vector(Vec3 at) {
  if(at==null)return null;var out=new JsonArray();out.add(at.x);out.add(at.y);out.add(at.z);return out;
 }

 static final class Session implements AutoCloseable {
  final String suite,trial;final Object world,bird;final Thread owner;final Consumer<String> sink;final Session previous;
  final List<JsonObject> records=new ArrayList<>();
  volatile boolean closed;int omitted,errors,damageCalls,damageReturns,cancellations,sinkFailures,admittedEpoch;
  long admittedTick;AbstractFish admitted;Vec3 locked;
  private Session(String suite,Object world,Object bird,String trial,Thread owner,Consumer<String> sink,Session previous) {
   this.suite=suite;this.world=world;this.bird=bird;this.trial=trial;this.owner=owner;this.sink=sink;this.previous=previous;
  }
  void retain(JsonObject receipt){if(records.size()<MAX_RECORDS)records.add(receipt);else omitted++;}
  private void send(JsonObject row){try{sink.accept(row.toString());}catch(Throwable ignored){sinkFailures++;}}
  @Override public void close() {
   synchronized(SiltcrestPresentationProbe.class){
    if(closed)return;closed=true;
    // The original fixture's finally can restore scope even if its server task fails.
    if(active==this){active=previous;while(active!=null&&active.closed)active=active.previous;}
   }
   try {
    for(var row:records)send(row);
    var summary=new JsonObject();summary.addProperty("event","summary");summary.addProperty("suite",suite);summary.addProperty("trial",trial);
    summary.addProperty("records",records.size());summary.addProperty("omitted",omitted);summary.addProperty("observationErrors",errors);
    summary.addProperty("damageCalls",damageCalls);summary.addProperty("damageReturns",damageReturns);summary.addProperty("cancellations",cancellations);
    summary.addProperty("sinkFailures",sinkFailures);send(summary);
   }finally{records.clear();admitted=null;locked=null;}
  }
 }
}
