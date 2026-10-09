package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.SiltcrestNative.check;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.fish.AbstractFish;

/** Passive full-cohort audit. Failures are latched for the harness, never thrown through native AI. */
final class SiltcrestEcologyProbe implements AutoCloseable {
 record Cohort(int index,int x,int z,SiltcrestBittern bird,List<AbstractFish> fish) {}
 record State(long clock,long firstCoil,List<SiltcrestPresentationWitness.Candidate> candidates) {}
 private static volatile SiltcrestEcologyProbe active;
 private static boolean registered;
 private final MinecraftServer server;
 final List<Cohort> cohorts;
 private final List<SiltcrestPresentationProbe.Session> sessions;
 private final SiltcrestEcologyWitness witness;
 private final Map<UUID,Integer> entityTicks=new HashMap<>();
 private final Map<UUID,DamageSource> verifiedDeaths=new HashMap<>();
 private final ArrayDeque<String> recent=new ArrayDeque<>();
 private final List<String> first=new ArrayList<>();
 private List<SiltcrestPresentationWitness.Candidate> candidates;
 private AssertionError failure;
 private long firstCoil=-1,lastTick=-1;
 private int ticks;
 private boolean closed;

 SiltcrestEcologyProbe(MinecraftServer server,List<Cohort> cohorts) {
  check(active==null&&cohorts.size()==SiltcrestEcologyWitness.COHORTS,"One fixed three-cohort ecology scope");
  this.server=server;this.cohorts=List.copyOf(cohorts);
  sessions=SiltcrestPresentationProbe.beginEcology(server.overworld(),cohorts.stream().map(Cohort::bird).toList());
  witness=new SiltcrestEcologyWitness(cohorts.stream().map(c->c.bird().getUUID().toString()).toList());
  register();active=this;
 }
 private static synchronized void register() {
  if(registered)return;registered=true;
  ServerTickEvents.END_SERVER_TICK.register(server->{var probe=active;if(probe!=null&&probe.server==server)probe.endTick();});
 }
 private void endTick() {
  if(closed)return;
  try {
   var level=server.overworld();long now=level.getGameTime();
   check(lastTick<0||now==lastTick+1,"Every native server tick in the shared observation is consecutive");lastTick=now;ticks++;
   qualify();audit(level);
   String row="clock="+now+" firstCoil="+firstCoil+" cohorts="+cohorts.stream().map(c->"index="+c.index()+" source="+c.bird().getUUID()+" body="+c.bird().position()+" pose="+c.bird().pose()+" ready="+c.bird().huntReady()+" pool="+c.bird().preyPool(level).size()+" fish="+c.fish().stream().map(f->f.getUUID()+"@"+f.position()+" health="+f.getHealth()+" ticks="+f.tickCount).toList()).toList();
   if(first.size()<24)first.add(row);if(recent.size()==24)recent.removeFirst();recent.addLast(row);
  }catch(Throwable invalid){if(failure==null)failure=invalid instanceof AssertionError a?a:new AssertionError("Native ecology observation failed",invalid);}
 }
 private void qualify() {
  if(failure!=null)throw failure;
  candidates=witness.inspect(sessions.stream().map(s->new SiltcrestEcologyWitness.Feed(s.records,s.errors,s.omitted)).toList());
  // Read all retained admissions: a later candidate cannot replace the first-coil deadline.
  for(var session:sessions)for(var row:session.records)if(row.get("event").getAsString().equals("coil_admitted")){
   long tick=row.get("clock").getAsLong();if(firstCoil<0||tick<firstCoil)firstCoil=tick;
  }
 }
 private void audit(ServerLevel level) {
  for(int i=0;i<cohorts.size();i++) {
   var cohort=cohorts.get(i);var bird=cohort.bird();var candidate=candidates.get(i);
   check(bird.isAlive()&&!bird.isRemoved()&&!bird.isNoAi()&&bird.getHealth()==12,"Every original healthy bird retains ordinary AI");resident(level,bird);
   var raw=level.getEntitiesOfClass(AbstractFish.class,bird.getBoundingBox().inflate(6));
   check(raw.stream().allMatch(cohort.fish()::contains),"The production raw inflate(6) query cannot see another cohort's prey");
   int alive=0;
   for(var fish:cohort.fish()) {
    var damage=fish.getLastDamageSource();
    var population=new SiltcrestEcologyWitness.Fish(fish.getUUID().toString(),fish.isAlive(),fish.isRemoved(),fish.getHealth(),damage!=null,damage==null||damage.getEntity()==null?null:damage.getEntity().getUUID().toString(),damage==null||damage.getDirectEntity()==null?null:damage.getDirectEntity().getUUID().toString());
    boolean earned=verifiedDeaths.containsKey(fish.getUUID());
    if(earned)SiltcrestEcologyWitness.auditRetainedDeath(population,damage==verifiedDeaths.get(fish.getUUID()));
    else {
     earned=SiltcrestEcologyWitness.auditFish(population,candidate,bird.getUUID().toString());
     if(earned)verifiedDeaths.put(fish.getUUID(),damage);
    }
    if(!earned) {
     check(SiltcrestBittern.wildFish(fish,level)&&!fish.isNoAi(),"Each living unowned fish retains ordinary water AI");resident(level,fish);alive++;
     var box=fish.getBoundingBox();
     check(box.minX>=cohort.x()+1-.001&&box.maxX<=cohort.x()+5+.001&&box.minZ>=cohort.z()-.001&&box.maxZ<=cohort.z()+4+.001,"Ordinary living prey remains physically inside its original four-by-four pond");
    }
   }
   check(alive==(candidate!=null&&candidate.outcome()==SiltcrestPresentationWitness.Outcome.CAUGHT?2:3),"Exactly three healthy fish become exactly two only after a complete native kill receipt");
  }
  for(int a=0;a<cohorts.size();a++)for(int b=a+1;b<cohorts.size();b++)for(var f:cohorts.get(a).fish())for(var g:cohorts.get(b).fish())
   if(f.isAlive()&&g.isAlive())check(!f.getBoundingBox().inflate(8).intersects(g.getBoundingBox()),"Native schooling inflate(8) remains isolated across every living fish pair");
 }
 private void resident(ServerLevel level,Entity entity) {
  check(entity.level()==level&&level.getEntity(entity.getUUID())==entity&&level.hasChunkAt(entity.blockPosition()),"Original actor is loaded and registered in the same world");
  Integer before=entityTicks.put(entity.getUUID(),entity.tickCount);
  check(entity.tickCount>0&&(before==null||entity.tickCount==before+1),"Every living observed actor actually ticks once per native server tick");
 }
 State state() {
  check(!closed,"Only the active native hunt supplies observations");qualify();
  return new State(server.overworld().getGameTime(),firstCoil,candidates);
 }
 static int epoch(SiltcrestBittern bird) {
  try{var field=SiltcrestBittern.class.getDeclaredField("epoch");field.setAccessible(true);return field.getInt(bird);}
  catch(ReflectiveOperationException failure){throw new AssertionError("Exact native capture epoch is available",failure);}
 }
 void finish() {
  var state=state();check(SiltcrestEcologyWitness.winner(state.candidates())>=0&&SiltcrestEcologyWitness.resolved(state.candidates()),"A complete real kill and every other commitment must resolve before hunt observation ends");close();
 }
 @Override public void close() {
  if(closed)return;closed=true;if(active==this)active=null;
  SiltcrestPresentationProbe.endEcology(sessions);
  System.out.println("SILTCREST_ECOLOGY_SUMMARY cohorts=3 sharedPostCoilTicks=500 firstCoil="+firstCoil+" observedTicks="+ticks+" finalCandidates="+candidates+" failure="+failure);
  for(var row:first)System.out.println("SILTCREST_ECOLOGY_FIRST "+row);
  for(var row:recent)System.out.println("SILTCREST_ECOLOGY_LAST "+row);
 }
}
