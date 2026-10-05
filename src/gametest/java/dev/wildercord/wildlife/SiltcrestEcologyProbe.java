package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.SiltcrestNative.check;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.phys.Vec3;

/** Test-only reads around unchanged native AI and delegate-once vanilla damage, including screenshot waits. */
final class SiltcrestEcologyProbe implements AutoCloseable {
 private static final int LIMIT=32;
 private static volatile SiltcrestEcologyProbe active;
 private static boolean registered;
 private final List<Cod> fish=new ArrayList<>();
 private final List<String> first=new ArrayList<>(),firstTransitions=new ArrayList<>(),firstCoil=new ArrayList<>();
 private final ArrayDeque<String> last=new ArrayDeque<>(),lastTransitions=new ArrayDeque<>(),lastCoil=new ArrayDeque<>();
 private final List<String> receipts=new ArrayList<>(),screenshots=new ArrayList<>();
 private MinecraftServer server;
 private SiltcrestBittern bird;
 private String previous="";
 private int ticks,hurtCalls,transitionCount,coilSamples,receiptCount;

 SiltcrestEcologyProbe(){register();}
 private static synchronized void register(){
  if(registered)return;registered=true;
  // Match the existing performance probe's scoped listener pattern; retain no world after close.
  ServerTickEvents.END_SERVER_TICK.register(s->{var p=active;if(p!=null&&p.server==s)p.endTick();});
 }
 void begin(MinecraftServer s){check(active==null,"Only this ecology observation is active");server=s;active=this;}
 void bind(SiltcrestBittern b){bird=b;}
 void fish(ServerLevel l,int i){
  // EntityType.create(Level, COMMAND) checks canSpawn and invokes the Cod constructor.
  // Keep that check, registered COD attributes and the original initial placement.
  check(EntityTypes.COD.canSpawn(l),"Actual vanilla prey factory eligibility");
  var f=new WitnessCod(l);f.snapTo(1.5+i%4,100.1,.5+(i/4)%4,0,0);l.addFreshEntity(f);fish.add(f);
 }
 private final class WitnessCod extends Cod {
  WitnessCod(ServerLevel l){super(EntityTypes.COD,l);}
  @Override public void tick(){
   super.tick();
   if(active!=SiltcrestEcologyProbe.this||bird==null||bird.pose()!=SiltcrestBittern.COILING||read(bird,"quarry")!=this)return;
   // Fish are inserted before the bird. This records native fish movement while
   // the commitment is still active, before the bird can resolve its final coil tick.
   String row=state("after_quarry_tick");coilSamples++;retain(firstCoil,lastCoil,row);
  }
  @Override public boolean hurtServer(ServerLevel l,DamageSource damage,float amount){
   if(active!=SiltcrestEcologyProbe.this)return super.hurtServer(l,damage,amount);
   int call=++hurtCalls;String source=identity(damage.getEntity());
   receipt("hurt_before call="+call+" fish="+getUUID()+" damage="+damage+" source="+source+" amount="+amount+" "+state("before_hurt"));
   boolean returned=false,hit=false;
   try{hit=super.hurtServer(l,damage,amount);returned=true;return hit;}
   finally{receipt("hurt_after call="+call+" returned="+returned+" hit="+hit+" sameDamageSource="+(getLastDamageSource()==damage)+" finalDamage="+damageState(getLastDamageSource())+" "+state("after_hurt"));}
  }
 }
 private void endTick(){
  if(bird==null)return;
  String row=state("end_server_tick");ticks++;retain(first,last,row);
  String key=bird.pose()+":"+read(bird,"epoch")+":"+bird.huntReady()+":"+identity((Entity)read(bird,"quarry"))+":"+fish.stream().map(f->f.getUUID()+":"+f.getHealth()+":"+f.isAlive()+":"+f.isRemoved()).toList();
  if(!key.equals(previous)){transitionCount++;retain(firstTransitions,lastTransitions,row);previous=key;}
 }
 private void receipt(String row){receiptCount++;if(receipts.size()<LIMIT)receipts.add(row);}
 private static void retain(List<String> first,ArrayDeque<String> last,String row){
  if(first.size()<LIMIT)first.add(row);if(last.size()==LIMIT)last.removeFirst();last.addLast(row);
 }
 void screenshot(ClientGameTestContext c,TestSingleplayerContext w,String name){
  long before=w.getServer().computeOnServer(s->s.overworld().getGameTime());
  boolean completed=false;
  try{c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());completed=true;}
  finally{
   long after=w.getServer().computeOnServer(s->s.overworld().getGameTime());
   String row="name="+name+" before="+before+" after="+after+" elapsed="+(after-before)+" completed="+completed;
   if(screenshots.size()<LIMIT)screenshots.add(row);System.out.println("SILTCREST_ECOLOGY_SCREENSHOT "+row);
  }
 }
 private String state(String event){
  var l=server.overworld();var committed=(Vec3)read(bird,"committed");
  return "event="+event+" now="+l.getGameTime()+" bird="+identity(bird)+" body="+bird.position()
   +" alive="+bird.isAlive()+" removed="+bird.isRemoved()+" sameLevel="+(bird.level()==l)
   +" pose="+bird.pose()+" phase="+bird.phase()+" left="+read(bird,"left")+" epoch="+read(bird,"epoch")
   +" ready="+bird.huntReady()+" remaining="+(bird.huntReady()-l.getGameTime())
   +" ground="+bird.onGround()+" standingBank="+BitternHabitat.standingBank(bird)+" water="+bird.isInWater()
   +" disturbed="+bird.disturbed(l)+" night="+WetlandRules.night(l.getOverworldClockTime())+" rain="+l.isRaining()
   +" pool="+bird.preyPool(l).size()+" quarry="+identity((Entity)read(bird,"quarry"))+" committed="+committed
   +" pendingPreen="+read(bird,"pendingPreen")+" shelter="+bird.shelter()+" shelterUntil="+bird.shelterUntil()
   +" navigation="+route(bird)+" hunt="+hunt()+" fish="+fish.stream().map(f->fishState(l,f,committed)).toList();
 }
 private String fishState(ServerLevel l,Cod f,Vec3 committed){
  return "id="+f.getUUID()+" body="+f.position()+" center="+f.getBoundingBox().getCenter()+" hp="+f.getHealth()
   +" alive="+f.isAlive()+" removed="+f.isRemoved()+" wild="+SiltcrestBittern.wildFish(f,l)+" noAI="+f.isNoAi()
   +" sameLevel="+(f.level()==l)+" loaded="+l.hasChunkAt(f.blockPosition())+" named="+f.hasCustomName()
   +" bucket="+f.fromBucket()+" persistent="+f.isPersistenceRequired()+" water="+f.isInWater()
   +" distanceSqr="+bird.distanceToSqr(f)+" committedDistanceSqr="+(committed==null?"none":f.getBoundingBox().getCenter().distanceToSqr(committed))
   +" sight="+bird.loadedSight(f)+" damage="+damageState(f.getLastDamageSource())+" navigation="+route(f);
 }
 private String hunt(){
  return bird.getGoalSelector().getAvailableGoals().stream().filter(w->w.getGoal() instanceof BitternHuntGoal).map(w->{var g=w.getGoal();return "running="+w.isRunning()+" quarry="+identity((Entity)read(g,"quarry"))+" origin="+read(g,"origin")+" bank="+read(g,"bank")+" cursor="+read(g,"cursor")+" retries="+read(g,"retries")+" left="+read(g,"left")+" scanAt="+read(g,"scanAt")+" searchAt="+read(g,"searchAt");}).toList().toString();
 }
 private static String route(net.minecraft.world.entity.Mob mob){
  var n=mob.getNavigation();var p=n.getPath();
  return "done="+n.isDone()+(p==null?" path=none":" pathDone="+p.isDone()+" canReach="+p.canReach()+" next="+p.getNextNodeIndex()+" nodes="+p.getNodeCount()+" target="+p.getTarget());
 }
 private static String identity(Entity e){return e==null?"none":e.getUUID().toString();}
 private static String damageState(DamageSource d){return d==null?"none":d+" source="+identity(d.getEntity())+" direct="+identity(d.getDirectEntity());}
 private static Object read(Object o,String name){
  try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
  catch(ReflectiveOperationException e){throw new AssertionError("Native ecology diagnostic field missing: "+name,e);}
 }
 @Override public void close(){
  if(active==this)active=null;
  System.out.println("SILTCREST_ECOLOGY_SUMMARY ticks="+ticks+" hurtCalls="+hurtCalls+" transitions="+transitionCount+" coilSamples="+coilSamples+" receiptCount="+receiptCount+" screenshots="+screenshots);
  print("FIRST",first);print("LAST",last);print("TRANSITION_FIRST",firstTransitions);print("TRANSITION_LAST",lastTransitions);print("COIL_FIRST",firstCoil);print("COIL_LAST",lastCoil);print("HURT",receipts);
  server=null;bird=null;fish.clear();
 }
 private static void print(String label,Iterable<String> rows){for(String row:rows)System.out.println("SILTCREST_ECOLOGY_"+label+" "+row);}
}
