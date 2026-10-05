package dev.wildercord.wildlife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Test-only, bounded observations of the final moth's real AI and synchronous bloom admission. */
public final class MoonreedSourceProbe {
 private MoonreedSourceProbe() {}
 private static final int MAX_RECORDS=256;
 private static Session active;
 private static final class Session {
  final ServerLevel level;final BlockPos root;final Glimmerwing moth;final long started;
  final List<JsonObject> records=new ArrayList<>();
  int dropped,errors,searches,acquired,lost,pollinations,successes,leaseGranted,leaseRefused,pollinationDepth;boolean searchHadRoot,frozen;
  Session(ServerLevel level,BlockPos root,Glimmerwing moth){this.level=level;this.root=root.immutable();this.moth=moth;started=level.getGameTime();}
 }
 public static synchronized void begin(ServerLevel level,BlockPos root,Glimmerwing moth){
  try{active=new Session(level,root,moth);record("begin",null,null,null,0,0,null);}catch(Throwable ignored){active=null;}
 }
 public static synchronized void clear(){active=null;}
 public static synchronized void search(Glimmerwing moth,boolean after,BlockPos flower,Vec3 target,Vec3 lure,int retarget,int lureLeft){
  var s=active;if(s==null||s.frozen||s.moth!=moth)return;
  if(!after){s.searches++;s.searchHadRoot=s.root.equals(flower);}
  else {if(s.root.equals(flower))s.acquired++;else if(s.searchHadRoot)s.lost++;}
  record(after?"search-return":"search-head",flower,target,lure,retarget,lureLeft,null);
 }
 public static synchronized void flight(Glimmerwing moth,BlockPos flower,Vec3 target,Vec3 lure,int retarget,int lureLeft){
  var s=active;if(s==null||s.frozen||s.moth!=moth||moth.tickCount%20!=0)return;
  record("ai-return",flower,target,lure,retarget,lureLeft,null);
 }
 public static synchronized void pollination(Glimmerwing moth,ServerLevel level,BlockPos root,Boolean result){
  var s=active;if(s==null||s.frozen||s.moth!=moth||s.level!=level||!s.root.equals(root))return;
  if(result==null){s.pollinations++;s.pollinationDepth++;}
  else {if(result)s.successes++;s.pollinationDepth=Math.max(0,s.pollinationDepth-1);}
  record(result==null?"pollinate-head":"pollinate-return",null,null,null,0,0,result);
 }
 public static synchronized void lease(ServerLevel level,BlockPos root,boolean admitted){
  var s=active;if(s==null||s.frozen||s.level!=level||!s.root.equals(root)||s.pollinationDepth==0)return;
  if(admitted)s.leaseGranted++;else s.leaseRefused++;
  record("lease-return",null,null,null,0,0,admitted);
 }
 /** Preserve the existing predicate and freeze at its actual success/deadline decision on the server thread. */
 public static synchronized boolean waited(boolean arrived,boolean lastAttempt){
  var s=active;if(s!=null&&!s.frozen&&(arrived||lastAttempt)){record("wait-result",null,null,null,0,0,arrived);s.frozen=true;}
  return arrived;
 }
 /** Flush only after the original wait, so log I/O does not pace flight or acquisition. */
 public static synchronized void finish(boolean arrived){
  var s=active;if(s==null)return;active=null;
  try {
   var summary=new JsonObject();summary.addProperty("event","summary");summary.addProperty("searches",s.searches);
   summary.addProperty("arrived",arrived);summary.addProperty("rootAcquisitions",s.acquired);summary.addProperty("rootLossesAtSearch",s.lost);summary.addProperty("pollinationAttempts",s.pollinations);
   summary.addProperty("pollinationSuccesses",s.successes);summary.addProperty("leaseGranted",s.leaseGranted);
   summary.addProperty("leaseRefused",s.leaseRefused);summary.addProperty("records",s.records.size());
   summary.addProperty("dropped",s.dropped);summary.addProperty("observationErrors",s.errors);
   Wildercord.LOGGER.info("WILDERCORD_MOONREED_SOURCE {}",summary);
   for(var record:s.records)Wildercord.LOGGER.info("WILDERCORD_MOONREED_SOURCE {}",record);
  }catch(Throwable ignored){/* Diagnostics must not replace the original assertion. */}
 }
 private static void record(String event,BlockPos flower,Vec3 target,Vec3 lure,int retarget,int lureLeft,Boolean result){
  var s=active;if(s==null||s.frozen)return;
  // Reserve one receipt for the exact final predicate even if ordinary events fill the history.
  if(s.records.size()>=MAX_RECORDS-(event.equals("wait-result")?0:1)){s.dropped++;return;}
  try {
   var moth=s.moth;var level=s.level;var root=s.root;var here=moth.blockPosition();
   var out=new JsonObject();out.addProperty("event",event);out.addProperty("elapsedTicks",level.getGameTime()-s.started);
   out.addProperty("entityTick",moth.tickCount);out.addProperty("id",moth.getId());out.addProperty("uuid",moth.getUUID().toString());
   out.addProperty("alive",moth.isAlive());out.addProperty("removed",moth.isRemoved());out.addProperty("sameWorld",moth.level()==level);
   out.addProperty("world",moth.level().dimension().identifier().toString());out.addProperty("tracked",level.getEntity(moth.getUUID())==moth);
   out.addProperty("sourceChunkLoaded",moth.level().hasChunkAt(here));out.add("position",vector(moth.position()));out.add("velocity",vector(moth.getDeltaMovement()));
   out.addProperty("rootDistanceSquared",moth.position().distanceToSqr(Vec3.atCenterOf(root).add(0,.4,0)));
   out.addProperty("rootInSearchBox",Math.abs(here.getX()-root.getX())<=3&&Math.abs(here.getY()-root.getY())<=1&&Math.abs(here.getZ()-root.getZ())<=3);
   out.addProperty("searchPhase",Math.floorMod(moth.tickCount+moth.getId(),40));out.addProperty("clock",level.getOverworldClockTime());
   out.addProperty("night",WetlandRules.night(level.getOverworldClockTime()));out.addProperty("rootChunkLoaded",level.hasChunkAt(root));
   out.addProperty("rootWithinBorder",level.getWorldBorder().isWithinBounds(root));
   if(level.hasChunkAt(root)){
    var state=level.getBlockState(root);out.addProperty("rootState",state.toString());out.addProperty("rootSurvives",state.canSurvive(level,root));
    out.addProperty("surfaceHeight",MoonreedBlock.surfaceHeight(level,root.getX(),root.getZ()));out.addProperty("openSky",MoonreedBlock.openSky(level,root));
    // Do not create a chunk load solely to inspect moisture at a horizontal neighbor.
    boolean neighborsLoaded=true;for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)neighborsLoaded&=level.hasChunkAt(root.relative(direction));
    out.addProperty("moistureChunksLoaded",neighborsLoaded);if(neighborsLoaded)out.addProperty("moist",MoonreedBlock.moist(level,root));
   }
   boolean casterLure=false;for(var player:level.players())if(!player.isSpectator()&&player.distanceToSqr(moth)<WildlifeRules.CAST_LURE_RANGE*WildlifeRules.CAST_LURE_RANGE&&Wildlife.castRecently(player,WildlifeRules.CAST_LURE_TICKS)){casterLure=true;break;}
   out.addProperty("recentCasterInRange",casterLure);
   if(event.startsWith("search")||event.equals("ai-return")){
    out.addProperty("flower",flower==null?null:flower.toShortString());out.add("target",target==null?null:vector(target));out.add("lure",lure==null?null:vector(lure));
    out.addProperty("retarget",retarget);out.addProperty("lureLeft",lureLeft);
   }
   if(result!=null)out.addProperty("result",result);s.records.add(out);
  }catch(Throwable ignored){s.errors++;}
 }
 private static JsonArray vector(Vec3 at){var out=new JsonArray();out.add(at.x);out.add(at.y);out.add(at.z);return out;}
}
