package dev.wildercord.wildlife;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finite, read-only fixture observation. Never requests a path, invokes goal lifecycle, or retains a world. */
final class FungalNurseryProbe {
 static final int MAX_ROWS=64;
 private final Consumer<String> output;
 private int rows;
 private String previous="";

 FungalNurseryProbe(){this(row->dev.wildercord.Wildercord.LOGGER.info("FUNGAL_NURSERY_PROBE {}",row));}
 FungalNurseryProbe(Consumer<String> output){this.output=output;}

 /** Call in the existing server observation, with no additional waits. Final evidence has its own slot. */
 void sample(SporebackSnail snail,MinecraftServer server,int observation,boolean terminal){
  if(!terminal&&rows>=MAX_ROWS)return;
  String row=observe(()->{
   if(snail==null)return "visitor=none";
   var wrapped=snail.getGoalSelector().getAvailableGoals().stream()
    .filter(g->g.getGoal().getClass().getName().equals(SporebackSnail.class.getName()+"$Visit")).findFirst().orElse(null);
   Object goal=wrapped==null?null:wrapped.getGoal();
   var visit=visit(goal,wrapped!=null&&wrapped.isRunning());
   String key=visit.running()+":"+visit.destination()+":"+visit.shelter()+":"+visit.nursery()+":"+snail.blockPosition()+":"+(visit.chew()>0);
   boolean changed=!key.equals(previous);previous=key;
   if(!terminal&&!changed&&observation%4!=0)return null;
   var player=server==null?null:server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
   return snapshot(snail,player,goal,visit,observation,terminal);
  });
  if(row!=null)emit(row,terminal);
 }

 /** Exactly one local map after the earned canopy has been placed and its temporary support removed. */
 static void placed(ServerLevel level,SporebackSnail snail,BlockPos roof){
  var probe=new FungalNurseryProbe();
  probe.emit(observe(()->{
   var area=new AABB(Vec3.atLowerCornerOf(roof.offset(-1,-2,-1)),Vec3.atLowerCornerOf(roof.offset(2,2,2)));
   if(!resident(level,area))return "phase=placed geometry=unavailable-not-resident";
   return "phase=placed now="+level.getGameTime()+" visitor="+(snail==null?"none":snail.getUUID())
    +" forageReady="+(snail==null?"none":snail.forageReady())+" nurseryReady="+(snail==null?"none":snail.nurseryReady())
    +" cells="+cells(level,area,null);
  }),false);
 }

 record VisitState(boolean present,boolean running,BlockPos destination,boolean shelter,boolean nursery,int chew,String fields){}
 static VisitState visit(Object goal,boolean running) throws ReflectiveOperationException {
  if(goal==null)return new VisitState(false,false,null,false,false,0,"goal=none");
  var destination=(BlockPos)read(goal,"destination");
  boolean shelter=(boolean)read(goal,"shelter"),nursery=(boolean)read(goal,"nursery");
  int chew=(int)read(goal,"chew");var sweep=(HabitatSweep)read(goal,"sweep");
  String fields="running="+running+" destination="+destination+" shelter="+shelter+" nursery="+nursery
   +" left="+read(goal,"left")+" chew="+chew+" retries="+read(goal,"retries")+" searchAt="+read(goal,"searchAt")+" scanAt="+read(goal,"scanAt")
   +" sweep="+(sweep==null?"none":sweep.x()+","+sweep.y()+","+sweep.z()+" cursor="+read(sweep,"cursor"));
  return new VisitState(true,running,destination,shelter,nursery,chew,fields);
 }

 private static String snapshot(SporebackSnail snail,ServerPlayer player,Object goal,VisitState visit,int observation,boolean terminal) throws ReflectiveOperationException {
  if(!(snail.level() instanceof ServerLevel level))return "visitor="+snail.getUUID()+" world=not-server";
  var feet=snail.blockPosition();var body=snail.getBoundingBox();var control=snail.getMoveControl();
  String basic="phase="+(terminal?"final":"wait")+" observation="+observation+" now="+level.getGameTime()+" tick="+snail.tickCount
   +" visitor="+snail.getUUID()+" alive="+snail.isAlive()+" removed="+snail.isRemoved()+" position="+snail.position()+" body="+body
   +" pose="+snail.pose()+" dew="+snail.dew()+" forageReady="+snail.forageReady()+" nurseryReady="+snail.nurseryReady()+" hiddenUntil="+snail.hiddenUntil()
   +" velocity="+snail.getDeltaMovement()+" ground="+snail.onGround()+" horizontalCollision="+snail.horizontalCollision+" verticalCollision="+snail.verticalCollision
   +" controlWanted="+control.hasWanted()+" controlTarget="+new Vec3(control.getWantedX(),control.getWantedY(),control.getWantedZ())
   +" controlOperation="+read(control,MoveControl.class,"operation")+" visit={"+visit.fields()+"} path={"+path(snail)+"}"
   +" activeGoals="+snail.getGoalSelector().getAvailableGoals().stream().filter(WrappedGoal::isRunning).limit(8).map(g->g.getGoal().getClass().getSimpleName()).toList();
  // The margin covers cover(p.above(1..3)), the nursery occupancy AABB, collider neighbors,
  // and the entire ray. getChunkNow observes completed FULL residency without requesting it.
  var destination=visit.destination();
  var area=new AABB(Vec3.atLowerCornerOf(feet.offset(-3,-2,-3)),Vec3.atLowerCornerOf(feet.offset(4,5,4)));
  if(destination!=null){
   if(Math.abs((long)destination.getX()-feet.getX())>16||Math.abs((long)destination.getY()-feet.getY())>16||Math.abs((long)destination.getZ()-feet.getZ())>16)
    return basic+" worldReads=skipped-outside-bounded-destination";
   area=area.minmax(new AABB(destination).inflate(3));
  }
  if(!resident(level,area))return basic+" worldReads=skipped-not-resident";
  String predicates="goal=none";
  if(visit.present()){
   boolean cover=predicate(goal,"cover",feet),nursery=predicate(goal,"nursery",feet);
   boolean shelterGate=!visit.shelter()||(cover&&(!visit.nursery()||nursery));
   predicates="currentCover="+cover+" currentNursery="+nursery+" shelterGate="+shelterGate;
   if(destination!=null){
    double distance=snail.distanceToSqr(destination.getX()+.5,destination.getY(),destination.getZ()+.5);
    var clip=level.clip(new ClipContext(snail.getEyePosition(),Vec3.atCenterOf(destination),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,snail));
    boolean distanceGate=distance<.7,rayGate=clip.getType()==net.minecraft.world.phys.HitResult.Type.MISS;
    predicates+=" destinationHabitat="+predicate(goal,"habitat",destination)+" distanceSquared="+distance+" distanceGate="+distanceGate
     +" rayGate="+rayGate+" rayType="+clip.getType()+" rayBlock="+clip.getBlockPos()+" rayPoint="+clip.getLocation()+" rayInside="+clip.isInside()
     +" arrival="+(shelterGate&&distanceGate&&rayGate);
   }else predicates+=" destination=none arrival=not-evaluated";
  }
  var physical=new AABB(Vec3.atLowerCornerOf(feet.offset(-1,-1,-1)),Vec3.atLowerCornerOf(feet.offset(2,3,2)));
  return basic+" predicates={"+predicates+"} collision={"+cells(level,physical,body)+"}"
   +" player="+(player==null?"none":entity(player)+" sameLevel="+(player.level()==level))
   +" nearby="+level.getEntities(snail,body.inflate(2),Entity::isPushable).stream().limit(8).map(FungalNurseryProbe::entity).toList();
 }

 private static String path(SporebackSnail snail){
  var navigation=snail.getNavigation();var path=navigation.getPath();
  if(path==null)return "none done="+navigation.isDone()+" stuck="+navigation.isStuck();
  return "done="+navigation.isDone()+" stuck="+navigation.isStuck()+" canReach="+path.canReach()+" target="+path.getTarget()
   +" nextIndex="+path.getNextNodeIndex()+" count="+path.getNodeCount()+" next="+(path.isDone()?"none":path.getNextNode())+" end="+path.getEndNode();
 }
 private static String entity(Entity entity){return entity.getUUID()+" "+entity.getType()+" position="+entity.position()+" body="+entity.getBoundingBox()+" velocity="+entity.getDeltaMovement();}

 /** Terrain reads occur only after the complete bounded area and its neighboring chunks are resident. */
 private static boolean resident(ServerLevel level,AABB area){
  int minX=Math.floorDiv((int)Math.floor(area.minX)-1,16),maxX=Math.floorDiv((int)Math.ceil(area.maxX)+1,16);
  int minZ=Math.floorDiv((int)Math.floor(area.minZ)-1,16),maxZ=Math.floorDiv((int)Math.ceil(area.maxZ)+1,16);
  if((long)(maxX-minX+1)*(maxZ-minZ+1)>16)return false;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
  return true;
 }
 private static String cells(ServerLevel level,AABB area,AABB body){
  var rows=new ArrayList<String>();int colliders=0;
  var support=body==null?null:new AABB(body.minX,body.minY-.025,body.minZ,body.maxX,body.minY+.00001,body.maxZ);
  for(var at:BlockPos.betweenClosed(BlockPos.containing(area.minX,area.minY,area.minZ),BlockPos.containing(area.maxX-1,area.maxY-1,area.maxZ-1))){
   // Read the existing completed chunk directly; do not broaden this to Level.getChunk.
   var chunk=level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);
   if(chunk==null)return "unavailable-residency-changed";
   var state=chunk.getBlockState(at);var shape=state.getCollisionShape(level,at);
   if(shape.isEmpty())continue;colliders++;
   if(rows.size()>=24)continue;
   var boxes=shape.toAabbs().stream().map(box->box.move(at.getX(),at.getY(),at.getZ())).toList();
   rows.add(at.immutable()+"="+state+" boxes="+boxes+" bodyOverlap="+(body!=null&&boxes.stream().anyMatch(body::intersects))
    +" supportCandidate="+(support!=null&&boxes.stream().anyMatch(support::intersects)));
  }
  return "colliderCount="+colliders+" omitted="+Math.max(0,colliders-rows.size())+" cells="+rows;
 }
 private static boolean predicate(Object goal,String name,BlockPos at) throws ReflectiveOperationException {
  Method method=goal.getClass().getDeclaredMethod(name,BlockPos.class);method.setAccessible(true);return (boolean)method.invoke(goal,at);
 }
 private static Object read(Object object,String name) throws ReflectiveOperationException {return read(object,object.getClass(),name);}
 private static Object read(Object object,Class<?> type,String name) throws ReflectiveOperationException {var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}
 @FunctionalInterface interface Observation {String read() throws Exception;}
 /** Optional evidence must not replace a native assertion or interrupt the original wait. */
 static String observe(Observation observation){
  try{return observation.read();}
  catch(Exception|AssertionError|LinkageError failure){return "observationError="+failure.getClass().getSimpleName()+" detail="+String.valueOf(failure.getMessage()).replace('\n',' ').substring(0,Math.min(180,String.valueOf(failure.getMessage()).replace('\n',' ').length()));}
 }
 private void emit(String row,boolean terminal){
  if(row==null||(!terminal&&rows>=MAX_ROWS))return;
  if(!terminal)rows++;
  // A broken optional logger/sink is also observation failure, not native test failure.
  observe(()->{output.accept(row);return null;});
 }
}
