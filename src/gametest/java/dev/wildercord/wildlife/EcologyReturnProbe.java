package dev.wildercord.wildlife;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

/** Bounded reads of actual native paths and goal decisions. Never requests a diagnostic path. */
public final class EcologyReturnProbe {
 static final String REED="dev.wildercord.wildlife.ReedRefugeTest",BANK="dev.wildercord.wildlife.SiltcrestBankReturnTest";
 static final int LIMIT=32,NODE_LIMIT=32;
 private static volatile Session active;
 private static boolean registered;
 private EcologyReturnProbe() {}
 static synchronized Session begin(String suite,ServerLevel level,Mob actor){
  if(!registered){registered=true;ServerTickEvents.END_SERVER_TICK.register(server->{var s=active;if(s!=null&&s.world instanceof ServerLevel l&&l.getServer()==server&&s.actor instanceof Mob mob)observeBody(mob);});}
  var s=install(suite,level,actor,Thread.currentThread(),line->System.out.println("ECOLOGY_RETURN "+line));observeBody(actor);return s;
 }
 static synchronized Session install(String suite,Object world,Object actor,Thread owner,Consumer<String> sink){var s=new Session(suite,world,actor,owner,sink,active);active=s;return s;}
 static Session selected(String suite,Object world,Object actor,Thread thread){var s=active;return s!=null&&!s.closed&&(REED.equals(suite)||BANK.equals(suite))&&s.suite.equals(suite)&&s.world==world&&s.actor==actor&&s.owner==thread?s:null;}
 private static String suite(){var test=FabricClientGameTestRunner.currentlyRunningGameTest;return test==null?"none":test.getDefinition();}
 private static Session owner(){var s=active;return s!=null&&!s.closed&&s.owner==Thread.currentThread()&&s.suite.equals(suite())?s:null;}
 private static Mob actor(Object goal){return (Mob)read(goal,goal.getClass().getName().endsWith("$Shelter")?"this$0":"bird");}

 public static void goal(Object goal,String event,Boolean decision){
  var s=active;if(s==null)return;
  try{if(owner()!=s)return;
   var mob=actor(goal);if(selected(suite(),mob.level(),mob,Thread.currentThread())!=s)return;
   if(event.equals("continue")){s.lastContinue=decision;if(Boolean.TRUE.equals(decision))return;}
   if(event.equals("admission")&&!Boolean.TRUE.equals(decision))return;
   if(event.equals("start"))s.lastContinue=null;
   String fields=goalFields(goal);
   String key=fields.replaceAll("travelLeft=-?\\d+","travelLeft=*").replaceAll("left=-?\\d+","left=*")+" "+routeKey(mob.getNavigation().getPath());
   if(event.equals("tick")&&key.equals(s.goalKey)&&mob.tickCount%20!=0)return;
   s.goalKey=key;
   String row="event="+event+" decision="+decision+" "+body(mob)+" goal="+goal.getClass().getSimpleName()+" "+fields
    +(event.equals("stop")?" lastContinue="+s.lastContinue+" stopPredicates="+stopPredicates(mob,goal):"");
   if(event.equals("tick"))s.retain(row);else s.critical(row);
  }catch(Throwable ignored){s.errors++;}
 }
 /** Wraps only the original goal's moveTo call: identical receiver/arguments, result and exception. */
 public static boolean move(Object goal,PathNavigation navigation,Path route,double speed,Operation<Boolean> original){
  moveReceipt(goal,"move_call",route,speed,null);
  Boolean result=null;
  try{result=original.call(navigation,route,speed);return result;}
  finally{moveReceipt(goal,"move_return",route,speed,result);}
 }
 private static void moveReceipt(Object goal,String event,Path route,double speed,Boolean result){
  var s=active;if(s==null)return;
  try{if(owner()!=s)return;var mob=actor(goal);if(selected(suite(),mob.level(),mob,Thread.currentThread())!=s)return;
   if(event.equals("move_call"))s.moveCalls++;else s.moveReturns++;
   s.critical("event="+event+" result="+result+" threw="+(event.equals("move_return")&&result==null)+" speed="+speed+" acceptedPath="+path(route)+" "+goalFields(goal)+" "+body(mob));
  }catch(Throwable ignored){s.errors++;}
 }
 private static void observeBody(Mob mob){
  var s=active;if(s==null)return;
  try{if(owner()!=s)return;if(selected(suite(),mob.level(),mob,Thread.currentThread())!=s)return;
   boolean expired=mob instanceof SiltcrestBittern b&&b.huntReady()>0&&b.level().getGameTime()>=b.huntReady();
   String goals=mob.getGoalSelector().getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getGoal().getClass().getSimpleName()).toList().toString();
   String key=goals+" "+routeKey(mob.getNavigation().getPath())+" "+(mob instanceof LanternNewt n?n.resting():((SiltcrestBittern)mob).pose());
   boolean expiry=expired&&!s.expired;s.expired=expired;
   if(!expiry&&key.equals(s.bodyKey)&&mob.tickCount%20!=0)return;s.bodyKey=key;
   String row="event="+(expiry?"rest_expiry":"body")+" goals="+goals+" "+body(mob);if(expiry)s.critical(row);else s.retain(row);
  }catch(Throwable ignored){s.errors++;}
 }
 private static String stopPredicates(Mob mob,Object goal){
  if(mob instanceof SiltcrestBittern b)return "{remainingCounterNonPositive="+((int)read(goal,"left")<=0)+",forageReady="+b.forageReady()+",water="+b.isInWater()+",standingBank="+BitternHabitat.standingBank(b)+",navigationDone="+b.getNavigation().isDone()+",cursor="+read(goal,"cursor")+",cause=observed_predicates_only}";
  var n=(LanternNewt)mob;return "{remainingCounterNonPositive="+((int)read(goal,"travelLeft")<=0)+",settled="+read(goal,"settled")+",roof="+read(goal,"roof")+",frightened="+n.frightened()+",response="+n.response()+",cause=observed_predicates_only}";
 }
 private static String goalFields(Object goal){
  if(goal.getClass().getName().endsWith("$Shelter"))return "roof="+read(goal,"roof")+" travelLeft="+read(goal,"travelLeft")+" settled="+read(goal,"settled")+" retries="+read(goal,"retries")+" scanAt="+read(goal,"scanAt")+" arrived="+read(goal,"arrived")+" admittedRoute="+path((Path)read(goal,"route"));
  return "origin="+read(goal,"origin")+" bank="+read(goal,"bank")+" cursor="+read(goal,"cursor")+" left="+read(goal,"left")+" scanAt="+read(goal,"scanAt")+" next="+read(goal,"next");
 }
 static String body(Mob mob){
  var move=mob.getMoveControl();
  String extra=mob instanceof LanternNewt n?" resting="+n.resting()+" refugeReady="+n.refugeReady()+" pitch="+n.getXRot()+" movementInputs={x="+n.xxa+",y="+n.yya+",z="+n.zza+",speed="+n.getSpeed()+"}"+" lookControl={type="+n.getLookControl().getClass().getName()+",target="+n.getLookControl().isLookingAtTarget()+"}":" pose="+((SiltcrestBittern)mob).pose()+" rest="+((SiltcrestBittern)mob).huntReady()+" forageReady="+((SiltcrestBittern)mob).forageReady()+" standingBank="+BitternHabitat.standingBank((SiltcrestBittern)mob);
  return "now="+mob.level().getGameTime()+" tick="+mob.tickCount+" actor="+mob.getUUID()+" body="+mob.position()+" box="+mob.getBoundingBox()+" delta="+mob.getDeltaMovement()+" ground="+mob.onGround()+" water="+mob.isInWater()+" horizontalCollision="+mob.horizontalCollision+" verticalCollision="+mob.verticalCollision+" bodyCollision="+!mob.level().noCollision(mob,mob.getBoundingBox())+" touchCollision="+!mob.level().noCollision(mob,mob.getBoundingBox().inflate(.001))+extra+" navigationDone="+mob.getNavigation().isDone()+" nativePath="+path(mob.getNavigation().getPath())+" moveControl={type="+move.getClass().getSimpleName()+",operation="+read(move,"operation")+",wanted="+move.hasWanted()+",x="+move.getWantedX()+",y="+move.getWantedY()+",z="+move.getWantedZ()+",speed="+move.getSpeedModifier()+"}";
 }
 static String path(Path path){
  if(path==null)return "none";var nodes=new ArrayList<String>();for(int i=0;i<Math.min(path.getNodeCount(),NODE_LIMIT);i++){var n=path.getNode(i);nodes.add(i+":"+n.x+","+n.y+","+n.z+":"+n.type+":malus="+n.costMalus);}
  return "{identity="+System.identityHashCode(path)+",canReach="+path.canReach()+",done="+path.isDone()+",index="+path.getNextNodeIndex()+",count="+path.getNodeCount()+",target="+path.getTarget()+",nodes="+nodes+",omittedNodes="+Math.max(0,path.getNodeCount()-NODE_LIMIT)+"}";
 }
 private static String routeKey(Path path){return path==null?"none":System.identityHashCode(path)+":"+path.getNextNodeIndex()+":"+path.isDone();}
 private static Object read(Object object,String name){
  for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
  throw new IllegalStateException("Missing diagnostic field "+name);
 }
 static final class Session implements AutoCloseable {
  final String suite;final Object world,actor;final Thread owner;final Consumer<String> sink;final Session previous;
  final List<String> first=new ArrayList<>(),milestones=new ArrayList<>();final ArrayDeque<String> last=new ArrayDeque<>();
  volatile boolean closed;int count,criticalCount,errors,sinkFailures,moveCalls,moveReturns;boolean expired;Boolean lastContinue;String goalKey="",bodyKey="";
  Session(String suite,Object world,Object actor,Thread owner,Consumer<String> sink,Session previous){this.suite=suite;this.world=world;this.actor=actor;this.owner=owner;this.sink=sink;this.previous=previous;}
  synchronized void critical(String row){if(closed)return;criticalCount++;if(milestones.size()<LIMIT)milestones.add(row);}
  synchronized void retain(String row){if(closed)return;count++;if(first.size()<LIMIT)first.add(row);else{if(last.size()==LIMIT)last.removeFirst();last.addLast(row);}}
  void send(String row){try{sink.accept("suite="+suite+" "+row);}catch(Throwable ignored){sinkFailures++;}}
  @Override public synchronized void close(){synchronized(EcologyReturnProbe.class){if(closed)return;closed=true;if(active==this){active=previous;while(active!=null&&active.closed)active=active.previous;}}
   try{milestones.forEach(this::send);first.forEach(this::send);last.forEach(this::send);send("event=summary milestones="+criticalCount+" retainedMilestones="+milestones.size()+" observed="+count+" retained="+(first.size()+last.size())+" omitted="+Math.max(0,count-first.size()-last.size())+" observationErrors="+errors+" moveCalls="+moveCalls+" moveReturns="+moveReturns+" sinkFailures="+sinkFailures);}finally{first.clear();last.clear();milestones.clear();}
  }
 }
}
