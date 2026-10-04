package dev.wildercord.wildlife;

import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Supplied-load measurement, not natural-spawn acceptance or a machine-independent speed gate. */
public final class MixedWetlandPerformanceTest implements FabricClientGameTest {
 private static final int WARMUP=100,SAMPLES=400;
 private static volatile Window active;
 private static boolean listenersRegistered;
 private static final AABB NEWT_BOX=new AABB(-31,99,-7,-17,107,7);
 private static final AABB CRAB_BOX=new AABB(-7,100,-7,7,107,7);
 private static final AABB PREY_BOX=new AABB(41,99,-7,55,107,7);

 private static synchronized void register(){
  if(listenersRegistered)return;listenersRegistered=true;
  // These static listeners retain no world when active is null. Fabric has no event unregistration.
  ServerTickEvents.START_SERVER_TICK.register(s->{var a=active;if(a!=null&&a.server==s)a.start(s);});
  ServerTickEvents.END_SERVER_TICK.register(s->{var a=active;if(a!=null&&a.server==s)a.end(s);});
 }
 @Override public void runTest(ClientGameTestContext c){
  register();var results=new ArrayList<Report>();
  try{
   for(boolean pressure:new boolean[]{false,true}){
    try(var w=c.worldBuilder().create()){
     c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");
     w.getServer().runCommand("gamerule natural_health_regeneration false");
     w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
     var scene=w.getServer().computeOnServer(s->setup(s,pressure));
     int begin=w.getServer().computeOnServer(MinecraftServer::getTickCount);
     for(int n=0;n<800&&w.getServer().computeOnServer(MinecraftServer::getTickCount)-begin<WARMUP;n++)c.waitTicks(1);
     check(w.getServer().computeOnServer(MinecraftServer::getTickCount)-begin>=WARMUP,"One hundred actual warmup server ticks elapsed");
     w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getY()>=100,"Actual supplied observer remains above the wetland platform after warmup: body="+p.position()+" ground="+p.onGround()+" flying="+p.getAbilities().flying+" actualFish="+scene.fish.stream().filter(f->f.isAlive()&&!f.isRemoved()).count());});
     Window window=w.getServer().computeOnServer(s->{check(active==null,"No overlapping measurement world");var a=new Window(s,scene);active=a;return a;});
     try{
      for(int n=0;n<2400&&w.getServer().computeOnServer(s->window.used)<SAMPLES;n++)c.waitTicks(1);
      results.add(w.getServer().computeOnServer(s->{
       check(window.used==SAMPLES,"Exactly400 paired actual server ticks recorded");
       check(window.error==null,"Execution window authority: "+window.error);
       check(window.movingActorTicks>0,"Unpaused supplied actors exhibited actual physical motion");
       for(var e:scene.actors){check(e.isAlive()&&!e.isRemoved()&&e.level()==scene.level&&scene.level.getEntity(e.getUUID())==e,"Supplied creature remains alive, registered and in its world");check(!e.isNoAi()&&e.tickCount-window.actorTicks.get(e.getUUID())>=SAMPLES,"Every supplied creature executed400 actual unpaused entity ticks");}
       check(window.minCreatures==scene.actors.size()&&window.maxCreatures==scene.actors.size(),"Fixed supplied creature population remains bounded");
       if(pressure)check(window.newtSaturated>0&&window.crabSaturated>0&&window.preySaturated>0&&window.bitternRefused>0,"Actual moving dense pools and actual bittern query reached conservative saturation, never an arbitrary eligible prefix"+" "+window.diagnostic());
       else check(window.newtSaturated==0&&window.crabSaturated==0&&window.preySaturated==0,"Sparse typed pools remained genuinely complete");
       return window.report();
      }));
     }finally{w.getServer().runOnServer(s->{if(active==window)active=null;});}
    }
   }
   var payload=new LinkedHashMap<String,Object>();payload.put("schema",1);
   payload.put("scope","Independent fresh integrated-server worlds; supplied geometry and COMMAND actors; 100 actual warmup ticks then400 START_SERVER_TICK to END_SERVER_TICK execution intervals. Includes other simulated server workloads and listeners between these callbacks; excludes intentional tick wait, client rendering, setup and this fixture's END observation loop. Not TPS, natural populations, per-creature attribution or universal performance.");
   payload.put("percentiles","Nearest rank: sorted nanos[ceil(p*N)-1]; milliseconds=nanos/1e6. No machine-independent time threshold.");payload.put("scenes",results);
   String json=new GsonBuilder().setPrettyPrinting().create().toJson(payload);
   Files.writeString(Path.of("mixed-wetland-performance.json"),json,StandardCharsets.UTF_8);
   for(var r:results)System.out.println("MIXED_WETLAND_PERFORMANCE "+new GsonBuilder().create().toJson(r));
  }catch(java.io.IOException e){throw new AssertionError("Raw measurement export failed",e);}
  finally{active=null;}
 }

 private record Scene(ServerLevel level,boolean pressure,List<Mob> actors,List<AbstractFish> fish,List<BlockPos> reeds){}
 private static Scene setup(MinecraftServer s,boolean pressure){
  var l=s.overworld();var player=s.getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.CREATIVE);
  player.teleportTo(l,26.5,101,8.5,Set.<Relative>of(),0,0,false);
  check(!s.isPaused()&&!s.tickRateManager().isFrozen()&&!s.tickRateManager().isSprinting(),"Ordinary nonpaused nonsprinting server");
  // Clay is explicit support and containment, not water placed over a generated air gap.
  for(int x=-34;x<=58;x++)for(int z=-10;z<=10;z++){
   l.setBlock(new BlockPos(x,99,z),Blocks.CLAY.defaultBlockState(),2);
   l.setBlock(new BlockPos(x,100,z),Blocks.MUD.defaultBlockState(),2);
   for(int y=101;y<=108;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
  }
  for(int center:new int[]{-24,48}){
   for(int x=center-4;x<=center+4;x++)for(int z=-4;z<=4;z++)l.setBlock(new BlockPos(x,100,z),Blocks.WATER.defaultBlockState(),2);
   // Ordinary low pen prevents swimming population departure; AI and physics continue inside.
   for(int i=-6;i<=6;i++)for(int y=101;y<=102;y++){
    l.setBlock(new BlockPos(center+i,y,-6),Blocks.CLAY.defaultBlockState(),2);l.setBlock(new BlockPos(center+i,y,6),Blocks.CLAY.defaultBlockState(),2);
    l.setBlock(new BlockPos(center-6,y,i),Blocks.CLAY.defaultBlockState(),2);l.setBlock(new BlockPos(center+6,y,i),Blocks.CLAY.defaultBlockState(),2);
   }
  }
  for(int i=-6;i<=6;i++)for(int y=101;y<=102;y++){
   l.setBlock(new BlockPos(i,y,-6),Blocks.CLAY.defaultBlockState(),2);l.setBlock(new BlockPos(i,y,6),Blocks.CLAY.defaultBlockState(),2);
   l.setBlock(new BlockPos(-6,y,i),Blocks.CLAY.defaultBlockState(),2);l.setBlock(new BlockPos(6,y,i),Blocks.CLAY.defaultBlockState(),2);
  }
  l.setBlock(new BlockPos(-24,100,-3),WetlandShelters.REFUGE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),2);
  for(int x=-26;x<=-22;x++)l.setBlock(new BlockPos(x,100,2),Blocks.SEAGRASS.defaultBlockState(),2);
  var reeds=new ArrayList<BlockPos>();
  for(int i=0;i<6;i++){
   var p=new BlockPos(22+i%3*2,101,-2+i/3*4);reeds.add(p);
   l.setBlock(p.below(),Blocks.MUD.defaultBlockState(),2);l.setBlock(p.below().east(),Blocks.WATER.defaultBlockState(),2);
   for(int y=p.getY();y<l.getMaxY();y++)l.setBlock(new BlockPos(p.getX(),y,p.getZ()),Blocks.AIR.defaultBlockState(),2);
   l.setBlock(p,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,1),2);
   check(l.getBlockState(p).canSurvive(l,p)&&MoonreedBlock.canBloom(l,p,l.getOverworldClockTime()),"Supplied ordinary moist exposed night reed is valid");
  }
  var actors=new ArrayList<Mob>();var fish=new ArrayList<AbstractFish>();
  for(int i=0;i<(pressure?13:3);i++)track(l,WetlandContent.NEWT.create(l,EntitySpawnReason.COMMAND),-27.5+i%7,100.1,-1.5+i/7,actors);
  for(int i=0;i<(pressure?13:2);i++)track(l,ReedbackContent.CRAB.create(l,EntitySpawnReason.COMMAND),-3.5+i%7,101,-1.5+i/7,actors);
  for(int i=0;i<(pressure?12:2);i++)track(l,Wildlife.GLIMMERWING.create(l,EntitySpawnReason.COMMAND),22.5+i%4,102,-1.5+i/4,actors);
  for(int i=0;i<(pressure?2:1);i++)track(l,SiltcrestContent.BITTERN.create(l,EntitySpawnReason.COMMAND),43.5,101,-1.5+i*3,actors);
  for(int i=0;i<(pressure?16:3);i++){
   var f=(pressure&&i<13?EntityTypes.TROPICAL_FISH:EntityTypes.COD).create(l,EntitySpawnReason.COMMAND);
   check(f!=null,"Actual ordinary fish factory");f.snapTo(45.5+i%6,100.1,-2.5+i/6,0,0);
   // Wild Cod must remain unowned/nonpersistent; actual predation may change its population.
   check(!f.isNoAi()&&l.addFreshEntity(f),"Actual swimming fish tracked without paused AI");fish.add(f);
  }
  check(WetlandQueries.scan(l,LanternNewt.class,NEWT_BOX,null).saturated()==pressure,"Actual initial complete/saturated newt query");
  check(WetlandQueries.scan(l,ReedbackCrab.class,CRAB_BOX,null).saturated()==pressure,"Actual initial complete/saturated crab query");
  check(l.getBlockState(player.blockPosition().below()).isSolidRender()&&l.noCollision(player,player.getBoundingBox()),"Actual Creative observer has solid supplied support and ordinary collision-free standing room");
  for(var f:fish)check(player.distanceToSqr(f)<1024,"Every real wild fish starts inside32 blocks of the actual supported observer, without persistence/AI changes");
  return new Scene(l,pressure,List.copyOf(actors),List.copyOf(fish),List.copyOf(reeds));
 }
 private static void track(ServerLevel l,Mob e,double x,double y,double z,List<Mob> actors){
  check(e!=null,"Actual registered wildlife factory");e.snapTo(x,y,z,0,0);e.setPersistenceRequired();
  check(!e.isNoAi()&&l.addFreshEntity(e),"Real tracked wildlife keeps ordinary full AI");actors.add(e);
 }
 private record Report(String profile,int warmupTicks,int sampleCount,List<Integer> serverTicks,List<Long> executionNanos,
  double averageMs,double p95Ms,double p99Ms,double maximumMs,int initialCreatures,int minCreatures,int maxCreatures,
  int initialFish,int finalFish,long worldTimeStart,long worldTimeEnd,long movingActorTicks,Map<String,Long> behaviorActorTicks,
  int newtSaturatedTicks,int crabSaturatedTicks,int preySaturatedTicks,int actualBitternSaturatedRefusals,int openedReeds,Map<String,Integer> creatureCounts){}
 private static final class Window{
  final MinecraftServer server;final Scene scene;final long[] nanos=new long[SAMPLES];final int[] ticks=new int[SAMPLES];
  final Map<UUID,Integer> actorTicks=new HashMap<>();final Map<UUID,Vec3> lastPositions=new HashMap<>();
  final Map<String,Long> behaviors=new TreeMap<>();final long timeStart;
  int used,startTick=-1,lastTick=-1,minCreatures=Integer.MAX_VALUE,maxCreatures,newtSaturated,crabSaturated,preySaturated,bitternRefused;
  long startNanos,movingActorTicks,timeEnd;String error;
  Window(MinecraftServer s,Scene scene){server=s;this.scene=scene;timeStart=scene.level.getGameTime();for(var e:scene.actors){actorTicks.put(e.getUUID(),e.tickCount);lastPositions.put(e.getUUID(),e.position());}}
  void start(MinecraftServer s){if(used>=SAMPLES)return;startTick=s.getTickCount();startNanos=System.nanoTime();}
  void end(MinecraftServer s){
   if(used>=SAMPLES||startTick<0)return;
   long elapsed=System.nanoTime()-startNanos;int now=s.getTickCount();
   if(now!=startTick||(lastTick>=0&&now!=lastTick+1)||elapsed<0)error="Nonconsecutive or unpaired execution sample";
   if(s.isPaused()||s.tickRateManager().isFrozen()||s.tickRateManager().isSprinting())error="Server clock paused/frozen/sprinting";
   ticks[used]=now;nanos[used++]=elapsed;lastTick=now;startTick=-1;
   // Below this line is outside the reported execution interval. Fixed owned lists, never a global entity scan.
   int live=0;for(var e:scene.actors){
    if(e.isAlive()&&!e.isRemoved()&&e.level()==scene.level)live++;
    if(e.isNoAi())error="Supplied creature AI became disabled";
    var old=lastPositions.put(e.getUUID(),e.position());if(old.distanceToSqr(e.position())>1e-8)movingActorTicks++;
    String state=e instanceof LanternNewt n?"newt:"+(n.resting()?"resting":n.browsing()?"browsing":"moving"):
     e instanceof ReedbackCrab crab?"crab:pose"+crab.pose():e instanceof SiltcrestBittern bird?"bittern:pose"+bird.pose():"glimmerwing:alive";
    behaviors.merge(state,1L,Long::sum);
   }
   minCreatures=Math.min(minCreatures,live);maxCreatures=Math.max(maxCreatures,live);timeEnd=scene.level.getGameTime();
   var np=WetlandQueries.scan(scene.level,LanternNewt.class,NEWT_BOX,null);
   var cp=WetlandQueries.scan(scene.level,ReedbackCrab.class,CRAB_BOX,null);
   var fp=WetlandQueries.scan(scene.level,AbstractFish.class,PREY_BOX,null);
   if(np.saturated())newtSaturated++;if(cp.saturated())crabSaturated++;
   // Bittern's native raw bound is8, unlike the wetland generic12 helper: inspect at most9 actual fish.
   var rawFish=new ArrayList<AbstractFish>(9);
   scene.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.<Entity,AbstractFish>forClass(AbstractFish.class),PREY_BOX,f->true,rawFish,9);
   if(rawFish.size()==9)preySaturated++;
   for(var e:scene.actors)if(e instanceof SiltcrestBittern bird){
    var local=new ArrayList<AbstractFish>(9);
    scene.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.<Entity,AbstractFish>forClass(AbstractFish.class),bird.getBoundingBox().inflate(6),f->true,local,9);
    if(local.size()==9){if(!bird.preyPool(scene.level).isEmpty())error="Actual Bittern exposed an eligible prefix under raw fish saturation";bitternRefused++;}
   }
   if((np.saturated()&&(!np.entities().isEmpty()||np.enumerated()!=13))||(cp.saturated()&&(!cp.entities().isEmpty()||cp.enumerated()!=13))||(fp.saturated()&&(!fp.entities().isEmpty()||fp.enumerated()!=13)))error="Saturated query exposed a truncated prefix or exceeded13";
  }
  String diagnostic(){
   var np=WetlandQueries.scan(scene.level,LanternNewt.class,NEWT_BOX,null);var cp=WetlandQueries.scan(scene.level,ReedbackCrab.class,CRAB_BOX,null);var fp=WetlandQueries.scan(scene.level,AbstractFish.class,PREY_BOX,null);
   var birds=new ArrayList<String>();for(var e:scene.actors)if(e instanceof SiltcrestBittern b){var raw=new ArrayList<AbstractFish>(9);scene.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.<Entity,AbstractFish>forClass(AbstractFish.class),b.getBoundingBox().inflate(6),f->true,raw,9);birds.add("uuid="+b.getUUID()+" body="+b.position()+" pose="+b.pose()+" ground="+b.onGround()+" water="+b.isInWater()+" raw="+raw.size()+" actualEligible="+b.preyPool(scene.level).size()+" huntReady="+b.huntReady());}
   var observer=server.getPlayerList().getPlayers().getFirst();
   return "observerBody="+observer.position()+" observerGround="+observer.onGround()+" observerDelta="+observer.getDeltaMovement()+" observerSupport="+scene.level.getBlockState(observer.blockPosition().below())+" used="+used+" counterNewt="+newtSaturated+" counterCrab="+crabSaturated+" counterFish9="+preySaturated+" counterActualBirdRefusal="+bitternRefused+" trackedMinMax="+minCreatures+"/"+maxCreatures+" moving="+movingActorTicks+" error="+error+" finalNewt="+np.enumerated()+"/"+np.saturated()+" finalCrab="+cp.enumerated()+"/"+cp.saturated()+" finalFish12="+fp.enumerated()+"/"+fp.saturated()+" actualBirds="+birds+" trackedActors="+scene.actors.stream().map(e->e.getType()+":"+e.getUUID()+":"+e.position()+":alive="+e.isAlive()+":removed="+e.isRemoved()+":tick="+e.tickCount+":water="+e.isInWater()).toList()+" trackedFish="+scene.fish.stream().map(e->e.getUUID()+":"+e.position()+":alive="+e.isAlive()+":removed="+e.isRemoved()+":water="+e.isInWater()+":observerDistanceSquared="+observer.distanceToSqr(e)).toList();
  }
  Report report(){
   check(timeEnd-timeStart>=SAMPLES,"Measured world executed400 actual simulation ticks");
   long[] sorted=nanos.clone();Arrays.sort(sorted);double total=0;var raw=new ArrayList<Long>();var ids=new ArrayList<Integer>();
   for(int i=0;i<SAMPLES;i++){raw.add(nanos[i]);ids.add(ticks[i]);total+=nanos[i];}
   var counts=new TreeMap<String,Integer>();for(var e:scene.actors)counts.merge(e.getType().toString(),1,Integer::sum);
   int finalFish=(int)scene.fish.stream().filter(e->e.isAlive()&&!e.isRemoved()).count();
   int opened=(int)scene.reeds.stream().filter(p->scene.level.getBlockState(p).is(WetlandGarden.REED)&&scene.level.getBlockState(p).getValue(MoonreedBlock.AGE)==2).count();
   return new Report(scene.pressure?"pressure":"sparse",WARMUP,SAMPLES,ids,raw,total/SAMPLES/1e6,
    sorted[(int)Math.ceil(.95*SAMPLES)-1]/1e6,sorted[(int)Math.ceil(.99*SAMPLES)-1]/1e6,sorted[SAMPLES-1]/1e6,
    scene.actors.size(),minCreatures,maxCreatures,scene.fish.size(),finalFish,timeStart,timeEnd,movingActorTicks,
    Map.copyOf(behaviors),newtSaturated,crabSaturated,preySaturated,bitternRefused,opened,counts);
  }
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
