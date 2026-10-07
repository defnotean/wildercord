package dev.wildercord.aura.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.config.Config;
import dev.wildercord.wildlife.HighlandRules;
import dev.wildercord.wildlife.HighlandShelterGoal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** GameTest-only observation of the unchanged scavenger fixture. No queries, goal calls, path requests or RNG. */
public final class ScavengerFoodProbe {
 static final String SUITE="dev.wildercord.aura.world.AuraBeastsTest";
 static final int FIRST=16,LAST=48,MAX_EVENTS=12,MAX_ITEMS=13,MAX_GOALS=16,MAX_NODES=16;
 private static final ThreadLocal<Session> SESSION=new ThreadLocal<>();
 private static final ThreadLocal<Frame> TICK=new ThreadLocal<>(),ACT=new ThreadLocal<>();
 private static final ThreadLocal<Query> QUERY=new ThreadLocal<>(),RAW=new ThreadLocal<>();
 private ScavengerFoodProbe(){}

 static String suite(){var test=FabricClientGameTestRunner.currentlyRunningGameTest;return test==null?"none":test.getDefinition();}
 static Session begin(Galeclaw actor,ItemEntity food){
  var session=install(SUITE,actor.level(),actor,food,Thread.currentThread(),line->System.out.println("SCAVENGER_FOOD "+line));
  session.capture("birth");return session;
 }
 static Session install(String suite,Object world,Object actor,Object food,Thread owner,Consumer<String> sink){
  var session=new Session(suite,world,actor,food,owner,sink,SESSION.get());SESSION.set(session);return session;
 }
 static Session selected(String suite,Object world,Object actor,Object food,Thread owner){
  var s=SESSION.get();return s!=null&&!s.closed&&SUITE.equals(suite)&&s.suite.equals(suite)&&s.world==world&&s.actor==actor&&s.food==food&&s.owner==owner&&owner==Thread.currentThread()?s:null;
 }
 static boolean transientScopesClear(){return TICK.get()==null&&ACT.get()==null&&QUERY.get()==null&&RAW.get()==null;}
 static boolean scopesClear(){return SESSION.get()==null&&transientScopesClear();}
 private static Session selected(Object world,Object actor){var s=SESSION.get();if(s==null||!(world instanceof ServerLevel level)||!level.getServer().isSameThread())return null;return selected(suite(),world,actor,s.food,Thread.currentThread());}
 private static Frame frame(Object actor){var f=TICK.get();return f!=null&&!f.session.closed&&f.session.actor==actor&&f.session.owner==Thread.currentThread()&&(!(actor instanceof Entity entity)||entity.level()==f.session.world)?f:null;}
 private static Frame frame(Object actor,Object world){var f=frame(actor);return f!=null&&f.session.world==world?f:null;}
 private static void restore(ThreadLocal<Frame> local,Frame old){if(old==null||old.session.closed)local.remove();else local.set(old);}
 private static void restoreQuery(ThreadLocal<Query> local,Query old){if(old==null||old.frame.session.closed)local.remove();else local.set(old);}

 public static void tick(Mob actor,Operation<Void> original){tick(selected(actor.level(),actor),original);}
 static void tick(Session session,Operation<Void> original){
  var previous=TICK.get();var current=session==null||session.closed||previous!=null?null:new Frame(session);TICK.set(current);
  if(current!=null)current.snapshot("before");boolean returned=false;
  try{original.call();returned=true;}
  finally{try{if(current!=null){try{current.snapshot("after");current.row.addProperty("returned",returned);session.retain(current.row);}catch(Throwable failure){session.error(failure);}}}finally{restore(TICK,previous);}}
 }
 public static void ai(Object actor,ServerLevel level,Operation<Void> original){
  var f=frame(actor,level);if(f!=null)f.event("custom_ai_enter",()->snapshot(f.session));boolean returned=false;
  try{original.call(level);returned=true;}finally{if(f!=null){boolean result=returned;f.event("custom_ai_exit",()->{var row=snapshot(f.session);row.addProperty("returned",result);return row;});}}
 }
 public static void act(Object actor,ServerLevel level,Operation<Void> original){
  var previous=ACT.get();var f=previous==null?frame(actor,level):null;ACT.set(f);if(f!=null)f.event("act_enter",()->snapshot(f.session));boolean returned=false;
  try{original.call(level);returned=true;}finally{try{if(f!=null){boolean result=returned;f.event("act_exit",()->{var row=snapshot(f.session);row.addProperty("returned",result);return row;});}}finally{restore(ACT,previous);}}
 }
 public static <T extends Entity> List<T> forage(Object actor,ServerLevel level,Class<T> type,AABB bounds,Predicate<T> eligible,Operation<List<T>> original){
  var previous=QUERY.get();var f=ACT.get();var query=previous==null&&f!=null&&f==frame(actor)&&f.session.world==level&&type==ItemEntity.class?new Query(f,level,type,bounds,eligible):null;QUERY.set(query);
  List<T> result=null;boolean returned=false;
  try{result=original.call(level,type,bounds,eligible);returned=true;return result;}
  finally{try{if(query!=null){var returnedList=result;boolean normal=returned;query.frame.event("forage_query",()->{var row=new JsonObject();row.addProperty("returned",normal);row.addProperty("bounds",bounds.toString());row.addProperty("rawCount",query.rawCount);row.add("rawOrder",query.raw);row.addProperty("rawRefused",query.rawCount>BeastRules.VICTIMS);row.add("eligibleOrder",items(query.frame.session,returnedList));row.addProperty("eligibleCount",returnedList==null?-1:returnedList.size());row.addProperty("returnedListIdentity",returnedList==null?0:System.identityHashCode(returnedList));return row;});}}finally{restoreQuery(QUERY,previous);}}
 }
 /** Mask reentrant/unrelated complete() calls. Only the exact original four-argument query may claim its raw pool. */
 public static <T extends Entity> List<T> complete(ServerLevel level,Class<T> type,AABB bounds,Entity source,Predicate<T> eligible,Operation<List<T>> original){
  var previous=RAW.get();var query=QUERY.get();boolean match=query!=null&&!query.entered&&query.world==level&&query.type==type&&query.bounds==bounds&&query.eligible==eligible&&source==null;
  if(match)query.entered=true;RAW.set(match?query:null);
  try{return original.call(level,type,bounds,source,eligible);}finally{restoreQuery(RAW,previous);}
 }
 public static <T extends Entity> void raw(ServerLevel level,EntityTypeTest<Entity,T> type,AABB bounds,Predicate<? super T> eligible,List<? super T> found,int limit,Operation<Void> original){
  original.call(level,type,bounds,eligible,found,limit);
  var query=RAW.get();if(query!=null&&query.world==level&&query.bounds==bounds)try{query.rawCount=found.size();query.raw=items(query.frame.session,found);}catch(Throwable failure){query.frame.session.error(failure);}
 }
 public static boolean move(Object actor,PathNavigation navigation,Entity food,double speed,Operation<Boolean> original){
  var f=ACT.get();if(f!=frame(actor))f=null;var observed=f;Boolean result=null;
  try{result=original.call(navigation,food,speed);return result;}finally{if(observed!=null){var returned=result;observed.event("food_move",()->{var row=new JsonObject();row.add("selectedFood",entity(observed.session,food));row.addProperty("speed",speed);row.addProperty("result",returned);row.addProperty("threw",returned==null);row.add("navigation",navigation(navigation));return row;});}}
 }
 public static double distance(Object actor,Entity food,Operation<Double> original){
  double result=original.call(actor,food);var f=ACT.get();if(f!=null&&f==frame(actor))f.event("food_distance",()->{var row=new JsonObject();row.add("selectedFood",entity(f.session,food));row.addProperty("distanceSqr",result);row.addProperty("belowTwo",result<2);return row;});return result;
 }

 private static JsonObject snapshot(Session session){
  var actor=(Galeclaw)session.actor;var level=(ServerLevel)session.world;var row=new JsonObject();
  row.addProperty("serverThread",level.getServer().isSameThread());row.addProperty("gameTime",level.getGameTime());row.addProperty("overworldClockTime",level.getOverworldClockTime());row.addProperty("dimension",level.dimension().identifier().toString());
  row.addProperty("raining",level.isRaining());row.addProperty("difficulty",level.getDifficulty().toString());row.addProperty("beastsEnabled",Config.get().auraWorld().auraBeasts());
  row.add("actor",entity(session,actor));row.add("fixtureFood",entity(session,(Entity)session.food));row.addProperty("fixtureDistanceSqr",actor.position().distanceToSqr(((Entity)session.food).position()));
  row.addProperty("pose",actor.pose());row.addProperty("left",actor.left);row.addProperty("calm",actor.calm);row.addProperty("aggression",actor.aggression);row.addProperty("fedUntil",actor.fedUntil());row.addProperty("hungry",HighlandRules.hungry(level.getGameTime(),actor.fedUntil()));
  row.addProperty("forageModulo",Math.floorMod(actor.tickCount,40));row.addProperty("home",String.valueOf(actor.home()));row.addProperty("noAi",actor.isNoAi());row.add("target",entity(session,actor.getTarget()));
  row.addProperty("wantsCover",HighlandRules.wantsCover(true,level.getOverworldClockTime(),level.isRaining()));
  row.add("navigation",navigation(actor.getNavigation()));var goals=new JsonArray();int count=0;
  for(var wrapped:actor.getGoalSelector().getAvailableGoals()){
   if(count++>=MAX_GOALS)continue;var goal=wrapped.getGoal();var item=new JsonObject();item.addProperty("class",goal.getClass().getName());item.addProperty("priority",wrapped.getPriority());item.addProperty("running",wrapped.isRunning());if(goal instanceof HighlandShelterGoal shelter)item.addProperty("shelterRunning",shelter.running());goals.add(item);
  }
  row.add("goals",goals);row.addProperty("goalsOmitted",Math.max(0,count-MAX_GOALS));return row;
 }
 private static JsonObject entity(Session session,Entity entity){
  if(entity==null)return null;var row=new JsonObject();row.addProperty("identity",System.identityHashCode(entity));row.addProperty("id",entity.getId());row.addProperty("uuid",entity.getUUID().toString());row.addProperty("fixtureFood",entity==session.food);row.addProperty("sameWorld",entity.level()==session.world);row.addProperty("tickCount",entity.tickCount);row.addProperty("alive",entity.isAlive());row.addProperty("removed",entity.isRemoved());row.addProperty("removalReason",String.valueOf(entity.getRemovalReason()));row.add("position",vector(entity.position()));row.add("velocity",vector(entity.getDeltaMovement()));row.addProperty("onGround",entity.onGround());row.addProperty("horizontalCollision",entity.horizontalCollision);row.addProperty("verticalCollision",entity.verticalCollision);
  if(entity instanceof ItemEntity item){row.addProperty("item",item.getItem().getItem().toString());row.addProperty("count",item.getItem().getCount());}return row;
 }
 private static JsonArray items(Session session,List<?> items){if(items==null)return null;var array=new JsonArray();for(int i=0;i<Math.min(items.size(),MAX_ITEMS);i++)array.add(entity(session,(Entity)items.get(i)));return array;}
 static JsonObject path(Path path){
  var row=new JsonObject();row.addProperty("present",path!=null);if(path==null)return row;
  row.addProperty("identity",System.identityHashCode(path));row.addProperty("canReach",path.canReach());row.addProperty("done",path.isDone());row.addProperty("target",path.getTarget().toString());row.addProperty("next",path.getNextNodeIndex());row.addProperty("nodeCount",path.getNodeCount());var nodes=new JsonArray();int start=Math.max(0,path.getNextNodeIndex());for(int i=start;i<Math.min(path.getNodeCount(),start+MAX_NODES);i++)nodes.add(path.getNodePos(i).toString());row.add("remainingNodes",nodes);row.addProperty("nodesOmitted",Math.max(0,path.getNodeCount()-start-MAX_NODES));return row;
 }
 private static JsonObject navigation(PathNavigation navigation){var row=new JsonObject();row.addProperty("done",navigation.isDone());row.addProperty("stuck",navigation.isStuck());row.addProperty("target",String.valueOf(navigation.getTargetPos()));row.add("path",path(navigation.getPath()));return row;}
 private static JsonArray vector(Vec3 vector){var row=new JsonArray();row.add(vector.x);row.add(vector.y);row.add(vector.z);return row;}

 static final class Frame {
  final Session session;final JsonObject row=new JsonObject();final JsonArray events=new JsonArray();int count;
  Frame(Session session){this.session=session;row.addProperty("event","tick");row.add("events",events);}
  void snapshot(String phase){try{row.add(phase,ScavengerFoodProbe.snapshot(session));}catch(Throwable failure){session.error(failure);}}
  void event(String phase,Supplier<JsonObject> capture){if(session.closed)return;if(count++>=MAX_EVENTS){session.eventsOmitted++;return;}try{var detail=capture.get();detail.addProperty("event",phase);events.add(detail);}catch(Throwable failure){session.error(failure);}}
 }
 private static final class Query {
  final Frame frame;final Object world,type,bounds,eligible;boolean entered;int rawCount=-1;JsonArray raw;
  Query(Frame frame,Object world,Object type,Object bounds,Object eligible){this.frame=frame;this.world=world;this.type=type;this.bounds=bounds;this.eligible=eligible;}
 }
 static final class Session implements AutoCloseable {
  final String suite;Object world,actor,food;Thread owner;Session previous;Consumer<String> sink;volatile boolean closed;
  final List<JsonObject> first=new ArrayList<>();final ArrayDeque<JsonObject> last=new ArrayDeque<>();int count,omitted,errors,sinkFailures,eventsOmitted;String firstError;
  Session(String suite,Object world,Object actor,Object food,Thread owner,Consumer<String> sink,Session previous){this.suite=suite;this.world=world;this.actor=actor;this.food=food;this.owner=owner;this.sink=sink;this.previous=previous;}
  void capture(String event){if(closed)return;try{var row=snapshot(this);row.addProperty("event",event);retain(row);}catch(Throwable failure){error(failure);}}
  void retain(JsonObject row){if(closed)return;count++;if(first.size()<FIRST)first.add(row);else{if(last.size()==LAST){last.removeFirst();omitted++;}last.addLast(row);}}
  void error(Throwable failure){errors++;if(firstError==null)try{firstError=failure.getClass().getName();}catch(Throwable ignored){firstError="diagnostic failure";}}
  void send(JsonObject row){try{sink.accept(row.toString());}catch(Throwable failure){sinkFailures++;}}
  @Override public void close(){
   if(closed)return;
   try{if(actor instanceof Galeclaw)capture("deadline");}catch(Throwable failure){error(failure);}finally{
    closed=true;
    try{
     for(var row:first)send(row);for(var row:last)send(row);var summary=new JsonObject();summary.addProperty("event","summary");summary.addProperty("suite",suite);summary.addProperty("waitTicks",45);summary.addProperty("rows",count);summary.addProperty("retained",first.size()+last.size());summary.addProperty("omitted",omitted);summary.addProperty("eventsOmitted",eventsOmitted);summary.addProperty("errors",errors);summary.addProperty("firstError",firstError);summary.addProperty("sinkFailures",sinkFailures);summary.addProperty("maxRows",FIRST+LAST);summary.addProperty("maxEventsPerTick",MAX_EVENTS);summary.addProperty("maxItemsPerQuery",MAX_ITEMS);summary.addProperty("maxGoals",MAX_GOALS);summary.addProperty("maxPathNodes",MAX_NODES);send(summary);
    }catch(Throwable failure){error(failure);}finally{
     if(SESSION.get()==this){if(previous==null||previous.closed)SESSION.remove();else SESSION.set(previous);}
     if(TICK.get()!=null&&TICK.get().session==this)TICK.remove();if(ACT.get()!=null&&ACT.get().session==this)ACT.remove();if(QUERY.get()!=null&&QUERY.get().frame.session==this)QUERY.remove();if(RAW.get()!=null&&RAW.get().frame.session==this)RAW.remove();
     first.clear();last.clear();world=null;actor=null;food=null;owner=null;previous=null;sink=null;
    }
   }
  }
 }
}
