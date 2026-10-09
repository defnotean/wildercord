package dev.wildercord.wildlife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/** Post-refusal reads inside the existing witness poll. Never executes a goal, requests a path, or draws RNG. */
final class SiltcrestPresentationPathProbe implements AutoCloseable {
 private static final int MAX_GOALS=16,MAX_PATH_NODES=16,MAX_PREY=SiltcrestBittern.PREY_CAP+1;
 private final SiltcrestPresentationProbe.Session session;
 private final Log log=new Log(line->System.out.println("SILTCREST_PRESENTATION_PATH "+line));
 private boolean enabled,closed;
 private long refusalTick=-1;
 private int refusalEpoch=-1,calls,scopeRefusals,pathSequence;
 private Object previousPath;
 SiltcrestPresentationPathProbe(SiltcrestPresentationProbe.Session session){this.session=session;}

 void sample(SiltcrestPresentationWitness.Candidate candidate) {
  if(closed)return;
  try {
   if(!enabled){if(candidate==null||candidate.outcome()!=SiltcrestPresentationWitness.Outcome.ESCAPED)return;enabled=true;refusalTick=candidate.strikeTick();refusalEpoch=candidate.epoch();}
   calls++;
   var bird=(SiltcrestBittern)session.bird;var level=(ServerLevel)session.world;
   if(SiltcrestPresentationProbe.selected(SiltcrestPresentationProbe.SUITE,bird.level(),bird,Thread.currentThread())!=session){scopeRefusals++;return;}
   long now=SiltcrestBittern.clock(level);var path=bird.getNavigation().getPath();if(path!=previousPath){pathSequence++;previousPath=path;}
   var key=new JsonObject();key.addProperty("pose",bird.pose());key.addProperty("phase",bird.phase());key.addProperty("epoch",integer(bird,"epoch"));
   key.addProperty("ground",bird.onGround());key.addProperty("water",bird.isInWater());key.addProperty("bodyCell",bird.blockPosition().toString());
   key.addProperty("standingBank",BitternHabitat.standingBank(bird));key.addProperty("hungry",bird.hungry());key.addProperty("forageReady",bird.forageReady());
   key.addProperty("pathSequence",pathSequence);key.addProperty("navigationDone",bird.getNavigation().isDone());key.addProperty("pathNext",path==null?-1:path.getNextNodeIndex());
   var goals=new JsonArray();
   for(var wrapped:bird.getGoalSelector().getAvailableGoals().stream().limit(MAX_GOALS).toList()){
    var goal=wrapped.getGoal();var row=new JsonObject();row.addProperty("goal",goal.getClass().getSimpleName());row.addProperty("running",wrapped.isRunning());
    if(goal instanceof BitternHuntGoal||goal instanceof BitternBankReturnGoal){
     row.addProperty("origin",String.valueOf(read(goal,"origin")));row.addProperty("bank",String.valueOf(read(goal,"bank")));
     row.addProperty("cursor",integer(goal,"cursor"));row.addProperty("journeyExpired",integer(goal,"left")<=0);
     row.addProperty("scanReady",bird.tickCount>=integer(goal,"scanAt"));
     row.addProperty("searchReady",bird.tickCount>=integer(goal,goal instanceof BitternHuntGoal?"searchAt":"next"));
     if(goal instanceof BitternHuntGoal){row.addProperty("quarry",identity((Entity)read(goal,"quarry")));row.addProperty("retries",integer(goal,"retries"));}
    }
    goals.add(row);
   }
   key.add("goals",goals);
   log.observe(now,key.toString(),()->snapshot(level,bird,candidate,now,goals));
  }catch(Throwable invalid){log.error(invalid);}
 }
 private JsonObject snapshot(ServerLevel level,SiltcrestBittern bird,SiltcrestPresentationWitness.Candidate candidate,long now,JsonArray transitionGoals) {
  var out=new JsonObject();out.addProperty("event","post_refusal_path");out.addProperty("suite",session.suite);out.addProperty("trial",session.trial);
  out.addProperty("clock",now);out.addProperty("entityTick",bird.tickCount);out.addProperty("sourceUuid",bird.getUUID().toString());out.addProperty("sourceId",bird.getId());
  out.addProperty("world",level.dimension().identifier().toString());out.addProperty("sameWorld",bird.level()==level);out.addProperty("tracked",level.getEntity(bird.getUUID())==bird);
  out.addProperty("firstRefusalTick",refusalTick);out.addProperty("firstRefusalEpoch",refusalEpoch);out.addProperty("latestOutcome",candidate==null?null:candidate.outcome().name());
  out.add("position",vector(bird.position()));out.add("velocity",vector(bird.getDeltaMovement()));out.addProperty("pose",bird.pose());out.addProperty("phase",bird.phase());
  out.addProperty("epoch",integer(bird,"epoch"));out.addProperty("left",integer(bird,"left"));out.addProperty("alive",bird.isAlive());out.addProperty("removed",bird.isRemoved());out.addProperty("health",bird.getHealth());out.addProperty("noAi",bird.isNoAi());
  out.addProperty("ground",bird.onGround());out.addProperty("water",bird.isInWater());out.addProperty("horizontalCollision",bird.horizontalCollision);out.addProperty("verticalCollision",bird.verticalCollision);
  out.addProperty("standingBank",BitternHabitat.standingBank(bird));out.add("currentBankCell",bank(level,bird.blockPosition()));
  out.addProperty("forageReady",bird.forageReady());out.addProperty("hungry",bird.hungry());out.addProperty("huntReady",bird.huntReady());out.addProperty("huntRemaining",bird.huntReady()-now);
  out.addProperty("frightenedUntil",number(bird,"frightenedUntil"));out.addProperty("night",WetlandRules.night(level.getOverworldClockTime()));out.addProperty("raining",level.isRaining());out.addProperty("disturbed",bird.disturbed(level));
  out.addProperty("vanillaTarget",identity(bird.getTarget()));out.addProperty("committedQuarry",identity((Entity)read(bird,"quarry")));out.add("locked",vector((Vec3)read(bird,"committed")));
  out.addProperty("pathSequence",pathSequence);out.add("navigation",navigation(bird));
  var move=bird.getMoveControl();var control=new JsonObject();control.addProperty("hasWanted",move.hasWanted());control.add("wanted",vector(new Vec3(move.getWantedX(),move.getWantedY(),move.getWantedZ())));control.addProperty("speedModifier",move.getSpeedModifier());control.addProperty("entitySpeed",bird.getSpeed());out.add("moveControl",control);
  out.add("goalTransitions",transitionGoals.deepCopy());
  var detailed=new JsonArray();int count=0;
  for(var wrapped:bird.getGoalSelector().getAvailableGoals()){
   count++;if(detailed.size()>=MAX_GOALS)continue;var goal=wrapped.getGoal();var row=new JsonObject();row.addProperty("goal",goal.getClass().getSimpleName());row.addProperty("running",wrapped.isRunning());
   if(goal instanceof BitternHuntGoal||goal instanceof BitternBankReturnGoal){
    var origin=(BlockPos)read(goal,"origin");var target=(BlockPos)read(goal,"bank");
    row.addProperty("origin",String.valueOf(origin));row.add("bank",target==null?null:bank(level,target));
    row.addProperty("bankDistanceSqr",target==null?null:bird.distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5));
    for(var field:List.of("left","cursor","scanAt"))row.addProperty(field,integer(goal,field));
    row.addProperty("searchAt",integer(goal,goal instanceof BitternHuntGoal?"searchAt":"next"));
    if(goal instanceof BitternHuntGoal){row.addProperty("retries",integer(goal,"retries"));row.add("quarry",fish(level,bird,(AbstractFish)read(goal,"quarry")));
     var banks=new JsonArray();if(origin!=null)for(var offset:new int[][]{{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}})for(int y=-1;y<=1;y++)banks.add(bank(level,origin.offset(offset[0],y,offset[1])));row.add("originalHuntBankCandidates",banks);
    }
   }
   detailed.add(row);
  }
  out.add("goals",detailed);out.addProperty("goalsOmitted",Math.max(0,count-MAX_GOALS));
  var pool=bird.preyPool(level);var eligible=new JsonArray();for(var f:pool)eligible.add(f.getUUID().toString());out.add("eligiblePreyOrder",eligible);
  var raw=new ArrayList<AbstractFish>();level.getEntities(EntityTypeTest.<Entity,AbstractFish>forClass(AbstractFish.class),bird.getBoundingBox().inflate(6),f->true,raw,MAX_PREY);
  var prey=new JsonArray();for(var f:raw)prey.add(fish(level,bird,f));out.add("rawPrey",prey);out.addProperty("rawPreyAtCap",raw.size()==MAX_PREY);
  out.add("lastAdmittedFish",fish(level,bird,session.admitted));return out;
 }
 private static JsonObject bank(ServerLevel level,BlockPos at) {
  var out=new JsonObject();out.addProperty("position",at.toString());boolean loaded=level.hasChunkAt(at)&&level.hasChunkAt(at.above())&&level.hasChunkAt(at.below());out.addProperty("loaded",loaded);if(!loaded)return out;
  var feet=level.getBlockState(at);var below=level.getBlockState(at.below());var head=level.getBlockState(at.above());
  out.addProperty("nativeBank",BitternHabitat.bank(level,at));out.addProperty("fluidEmpty",level.getFluidState(at).isEmpty());out.addProperty("dryBank",BitternHabitat.bank(level,at)&&level.getFluidState(at).isEmpty());
  out.addProperty("feetBlock",feet.toString());out.addProperty("feetCollisionEmpty",feet.getCollisionShape(level,at).isEmpty());out.addProperty("supportBlock",below.toString());out.addProperty("supportSolid",below.isSolidRender());
  out.addProperty("headCollisionEmpty",head.getCollisionShape(level,at.above()).isEmpty());out.addProperty("headFluidEmpty",level.getFluidState(at.above()).isEmpty());return out;
 }
 private static JsonObject fish(ServerLevel level,SiltcrestBittern bird,AbstractFish fish) {
  if(fish==null)return null;var out=new JsonObject();out.addProperty("uuid",fish.getUUID().toString());out.add("position",vector(fish.position()));out.add("velocity",vector(fish.getDeltaMovement()));
  out.addProperty("entityTick",fish.tickCount);out.addProperty("sameWorld",fish.level()==level);out.addProperty("alive",fish.isAlive());out.addProperty("removed",fish.isRemoved());out.addProperty("health",fish.getHealth());out.addProperty("noAi",fish.isNoAi());
  out.addProperty("wild",SiltcrestBittern.wildFish(fish,level));out.addProperty("water",fish.isInWater());out.addProperty("sourceDistanceSqr",bird.distanceToSqr(fish));out.addProperty("loadedSight",fish.level()==level&&bird.loadedSight(fish));out.add("navigation",navigation(fish));return out;
 }
 private static JsonObject navigation(Mob mob) {
  var out=new JsonObject();var navigation=mob.getNavigation();var path=navigation.getPath();out.addProperty("done",navigation.isDone());out.addProperty("stuck",navigation.isStuck());out.addProperty("requestedTarget",String.valueOf(navigation.getTargetPos()));out.addProperty("pathPresent",path!=null);if(path==null)return out;
  out.addProperty("pathDone",path.isDone());out.addProperty("canReach",path.canReach());out.addProperty("target",path.getTarget().toString());out.addProperty("next",path.getNextNodeIndex());out.addProperty("nodeCount",path.getNodeCount());
  var nodes=new JsonArray();int first=Math.max(0,path.getNextNodeIndex());for(int i=first;i<Math.min(path.getNodeCount(),first+MAX_PATH_NODES);i++)nodes.add(path.getNodePos(i).toString());out.add("remainingNodes",nodes);out.addProperty("remainingNodesOmitted",Math.max(0,path.getNodeCount()-first-MAX_PATH_NODES));
  if(first<path.getNodeCount())out.add("nextWaypoint",vector(path.getNextEntityPos(mob)));return out;
 }
 private static Object read(Object source,String name){try{var field=source.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(source);}catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}}
 private static int integer(Object source,String name){return (Integer)read(source,name);}
 private static long number(Object source,String name){return (Long)read(source,name);}
 private static String identity(Entity entity){return entity==null?null:entity.getUUID().toString();}
 private static JsonArray vector(Vec3 v){if(v==null)return null;var out=new JsonArray();out.add(v.x);out.add(v.y);out.add(v.z);return out;}
 @Override public void close() {
  if(closed)return;closed=true;
  try {
   if(!enabled&&log.errors==0)return;
   var summary=new JsonObject();summary.addProperty("suite",session.suite);summary.addProperty("trial",session.trial);summary.addProperty("source",session.bird instanceof SiltcrestBittern bird?bird.getUUID().toString():null);
   summary.addProperty("firstRefusalTick",refusalTick);summary.addProperty("firstRefusalEpoch",refusalEpoch);summary.addProperty("sampleCalls",calls);summary.addProperty("scopeRefusals",scopeRefusals);summary.addProperty("maxGoals",MAX_GOALS);summary.addProperty("maxRemainingPathNodes",MAX_PATH_NODES);summary.addProperty("maxRawPrey",MAX_PREY);summary.addProperty("maxHuntBankCandidates",24);log.close(summary);
  }catch(Throwable failure){log.error(failure);log.close(null);}
  finally{previousPath=null;}
 }
 /** First sixteen and latest forty-eight selected rows; observation/sink errors never escape. */
 static final class Log {
  static final int INTERVAL=20,FIRST=16,LAST=48;
  final List<JsonObject> first=new ArrayList<>();final ArrayDeque<JsonObject> last=new ArrayDeque<>();
  final Consumer<String> sink;
  long nextClock=Long.MIN_VALUE;String previousKey,firstError,lastError;
  int polls,selected,omitted,errors,sinkFailures;boolean closed;
  Log(Consumer<String> sink){this.sink=sink;}
  void observe(long clock,String key,Supplier<JsonObject> capture) {
   if(closed)return;polls++;
   try {
    boolean transition=!key.equals(previousKey),cadence=clock>=nextClock||polls%INTERVAL==1;previousKey=key;
    if(!transition&&!cadence)return;
    var row=capture.get();row.addProperty("diagnosticPoll",polls);row.addProperty("transition",transition);row.addProperty("cadence",cadence);selected++;nextClock=clock+INTERVAL;
    if(first.size()<FIRST)first.add(row);else{if(last.size()==LAST){last.removeFirst();omitted++;}last.addLast(row);}
   }catch(Throwable failure){error(failure);}
  }
  void error(Throwable failure){
   errors++;String text="unprintable diagnostic error";
   try{var rendered=String.valueOf(failure);if(rendered!=null)text=rendered.length()>256?rendered.substring(0,256):rendered;}catch(Throwable ignored){}
   if(firstError==null)firstError=text;lastError=text;
  }
  private void send(JsonObject row){try{sink.accept(row.toString());}catch(Throwable failure){sinkFailures++;}}
  void close(JsonObject summary) {
   if(closed)return;closed=true;
   try {
    for(var row:first)send(row);for(var row:last)send(row);
    summary.addProperty("event","path_summary");summary.addProperty("polls",polls);summary.addProperty("selected",selected);summary.addProperty("retained",first.size()+last.size());summary.addProperty("omitted",omitted);summary.addProperty("observationErrors",errors);summary.addProperty("firstError",firstError);summary.addProperty("lastError",lastError);summary.addProperty("sinkFailures",sinkFailures);summary.addProperty("cadenceTicks",INTERVAL);summary.addProperty("firstCap",FIRST);summary.addProperty("lastCap",LAST);send(summary);
   }catch(Throwable failure){
    error(failure);
    try{var fallback=new JsonObject();fallback.addProperty("event","path_summary_error");fallback.addProperty("observationErrors",errors);fallback.addProperty("firstError",firstError);fallback.addProperty("lastError",lastError);fallback.addProperty("sinkFailures",sinkFailures);send(fallback);}catch(Throwable ignored){}
   }finally{first.clear();last.clear();previousKey=null;}
  }
 }
}
