package dev.wildercord.world.dungeons;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Passive, GameTest-only receipts. Only the existing command's server-thread call may be observed. */
public final class BattlefieldsGenerationProbe {
 static final String SUITE="dev.wildercord.world.dungeons.BattlefieldsTest";
 static final int MAX_CANDIDATES=16,MAX_LINES=40,MAX_MARKERS=16;
 private static final ThreadLocal<Candidate> COMMAND=new ThreadLocal<>();
 private static final ThreadLocal<Candidate> LOCATE=new ThreadLocal<>();
 private BattlefieldsGenerationProbe() {}

 static String currentSuite() {
  var test=FabricClientGameTestRunner.currentlyRunningGameTest;
  return test==null ? "none" : test.getDefinition();
 }
 static Session begin(long seed) {
  Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.world.dungeons.BattlefieldsTest\",\"seed\":\"{}\"}",seed);
  return new Session(seed,line -> Wildercord.LOGGER.info(line));
 }
 static boolean matches(Candidate candidate,String suite,Object world,long seed,int x,int z) {
  return candidate!=null && !candidate.closed && !candidate.session.closed && SUITE.equals(suite)
    && candidate.world==world && candidate.session.seed==seed && candidate.x>>4==x && candidate.z>>4==z;
 }

 public static LocateScope locate(Structure.GenerationContext context) {
  return locate(currentSuite(),context.heightAccessor(),context.seed(),context.chunkPos().x(),context.chunkPos().z());
 }
 static LocateScope locate(String suite,Object world,long seed,int x,int z) {
  Candidate previous=LOCATE.get(),candidate=COMMAND.get();
  if(!matches(candidate,suite,world,seed,x,z) || candidate.locateCalls++!=0)candidate=null;
  // Even an unrelated nested locate must mask the outer observation until it returns.
  LOCATE.set(candidate);
  return new LocateScope(previous,candidate);
 }
 private static Candidate observing() {
  var candidate=LOCATE.get();
  return candidate!=null && candidate==COMMAND.get() && !candidate.closed && !candidate.session.closed ? candidate : null;
 }
 public static void configured(boolean enabled) {var c=observing();if(c!=null)c.enabled=enabled;}
 public static void direction(Direction direction) {var c=observing();if(c!=null)c.direction=direction.toString();}
 public static void height(BlockPos at,Heightmap.Types map,int value) {
  var c=observing();if(c==null)return;
  c.heightCalls++;
  if(c.heights.size()<6)c.heights.add(position(at)+":"+map+"="+value);
  if(c.heightCalls==1)c.centre=at.immutable();
 }
 public static void footing(int surface,int floor,int[] neighbours,int sea,boolean admitted) {
  var c=observing();if(c==null)return;
  c.surface=surface;c.floor=floor;c.sea=sea;c.neighbourCount=neighbours.length;
  c.neighbours=Arrays.copyOf(neighbours,Math.min(4,neighbours.length));c.footing=admitted;
 }
 static String position(BlockPos at) {return at==null ? "none" : at.getX()+"/"+at.getY()+"/"+at.getZ();}

 public static final class LocateScope {
  private final Candidate previous,candidate;
  private LocateScope(Candidate previous,Candidate candidate) {this.previous=previous;this.candidate=candidate;}
  public void finish(Boolean admitted) {
   try {if(candidate!=null)candidate.locateAdmitted=admitted;}
   finally {if(previous==null)LOCATE.remove();else LOCATE.set(previous);}
  }
 }

 static final class Session implements AutoCloseable {
  final long seed;
  private final Consumer<String> sink;
  private final Map<String,Integer> outcomes=new LinkedHashMap<>();
  private boolean closed;
  private int attempts,scans,lines,omitted,sinkFailures;
  private BlockPos habitat;
  Session(long seed,Consumer<String> sink) {
   this.seed=seed;this.sink=sink;
   emit("BEGIN suite="+SUITE+", seed="+seed+", candidates=16, seedPolicy=representative_fixed_normal_world, biomePolicy=vanilla_place_command_unfiltered");
  }
  void habitat(BlockPos at,String biome,String structureClass,boolean allowed) {
   if(habitat!=null)throw new IllegalStateException("Memorial habitat already observed");
   habitat=at.immutable();
   emit("HABITAT position="+position(at)+", sampledBiome="+biome+", structure=wildercord:old_battlefield, class="+structureClass
     +", sampledBiomeInRegisteredSet="+allowed+", surfaceBiomeNotResampled=true");
  }
  Candidate command(Object world,int ordinal,int x,int z) {
   if(closed || attempts>=MAX_CANDIDATES || ordinal!=attempts || COMMAND.get()!=null || habitat==null
      || x!=habitat.getX()+(ordinal%4)*48 || z!=habitat.getZ()+(ordinal/4)*48)
    throw new IllegalStateException("Memorial diagnostic command scope is closed, nested or outside the original candidate bound");
   attempts++;var candidate=new Candidate(this,world,ordinal,x,z);COMMAND.set(candidate);return candidate;
  }
  String summary() {return "seed="+seed+", attempts="+attempts+", completedScans="+scans+", outcomes="+outcomes+", omittedLines="+omitted+", sinkFailures="+sinkFailures;}
  void emit(String line) {
   if(closed)return;
   if(lines++>=MAX_LINES) {omitted++;return;}
   send("BATTLEFIELD_GENERATION "+line);
  }
  private void send(String line) {try {sink.accept(line);}catch(Throwable ignored) {sinkFailures++;}}
  @Override public void close() {
   if(closed)return;
   try {send("BATTLEFIELD_GENERATION END "+summary());}
   finally {closed=true;habitat=null;outcomes.clear();}
  }
 }

 /** Immutable copy of values already observed; this does not invoke generation or read the world. */
 record Admission(Boolean enabled,String direction,BlockPos centre,int heightCalls,Integer surface,Integer floor,
   List<Integer> neighbours,Integer sea,Boolean footing,Boolean locateAdmitted,int locateCalls,
   boolean commandReturned,int callbackCalls,Boolean commandSuccess,Integer commandResult) {}

 static final class Candidate implements AutoCloseable {
  private final Session session;
  private Object world;
  private final int ordinal,x,z;
  private final List<String> heights=new ArrayList<>(6),markers=new ArrayList<>(MAX_MARKERS);
  private String direction="unobserved";
  private Boolean enabled,footing,locateAdmitted,commandSuccess;
  private int locateCalls,heightCalls,surface,floor,sea,neighbourCount,callbackCalls,commandResult,markerCount;
  private int[] neighbours=new int[0];
  private BlockPos centre,found;
  private boolean commandReturned,closed,scanned;
  private Candidate(Session session,Object world,int ordinal,int x,int z) {this.session=session;this.world=world;this.ordinal=ordinal;this.x=x;this.z=z;}
  void callback(boolean success,int result) {callbackCalls++;if(callbackCalls==1) {commandSuccess=success;commandResult=result;}}
  void returned() {commandReturned=true;}
  void marker(BlockPos at,boolean authentic) {
   markerCount++;if(markers.size()<MAX_MARKERS)markers.add(position(at)+":oldGround="+authentic);
  }
  void scanned(BlockPos at) {
   if(scanned || session.closed)return;
   scanned=true;found=at;session.scans++;
   session.outcomes.merge(outcome(),1,Integer::sum);
   session.emit("SCAN attempt="+ordinal+", observedMemorials="+markerCount+", retainedMarkers="+markers+", omittedMarkers="+Math.max(0,markerCount-markers.size())
     +", authenticFound="+position(found)+", scanStoppedAtFirstAuthentic=true, expectedMarker="+position(expectedMarker())+", outcome="+outcome());
  }
  Admission admission() {
   return new Admission(enabled,direction,centre,heightCalls,footing==null ? null : surface,footing==null ? null : floor,
     Arrays.stream(neighbours).boxed().toList(),footing==null ? null : sea,footing,locateAdmitted,locateCalls,
     commandReturned,callbackCalls,commandSuccess,callbackCalls==0 ? null : commandResult);
  }
  private BlockPos expectedMarker() {return centre==null || !Boolean.TRUE.equals(footing) ? null : centre.atY(surface+1);}
  String outcome() {
   if(!commandReturned)return "command_threw";
   if(callbackCalls!=1)return "command_callback_count_"+callbackCalls;
   if(!Boolean.TRUE.equals(commandSuccess)) {
    if(locateCalls==0)return "command_failed_locate_unobserved";
    if(locateCalls!=1)return "command_failed_locate_count_"+locateCalls;
    if(locateAdmitted==null)return "command_failed_locate_incomplete";
    if(Boolean.FALSE.equals(enabled) && Boolean.FALSE.equals(locateAdmitted))return "disabled_config";
    if(Boolean.FALSE.equals(footing) && Boolean.FALSE.equals(locateAdmitted))return "footing_rejected";
    return Boolean.TRUE.equals(locateAdmitted) ? "command_failed_after_admission" : "command_failed_admission_unresolved";
   }
   if(!scanned)return "command_succeeded_scan_pending";
   if(found==null)return "command_succeeded_no_authentic_marker";
   if(expectedMarker()==null)return "command_succeeded_authentic_marker_location_unobserved";
   return found.equals(expectedMarker()) ? "command_succeeded_expected_authentic_marker" : "command_succeeded_nearby_authentic_marker";
  }
  @Override public void close() {
   if(closed)return;
   try {
    session.emit("COMMAND attempt="+ordinal+", command="+x+"/70/"+z+", chunk="+(x>>4)+"/"+(z>>4)
      +", configEnabled="+enabled+", locateCalls="+locateCalls+", direction="+direction+", centre="+position(centre)
      +", heightCalls="+heightCalls+", heights="+heights+", surface="+(footing==null ? "unobserved" : surface)
      +", floor="+(footing==null ? "unobserved" : floor)+", neighbours="+Arrays.toString(neighbours)+", neighbourCount="+neighbourCount
      +", sea="+(footing==null ? "unobserved" : sea)+", footingAdmitted="+footing+", locateAdmitted="+locateAdmitted
      +", commandReturned="+commandReturned+", callbackCalls="+callbackCalls+", commandSuccess="+commandSuccess+", commandResult="+(callbackCalls==0 ? "unobserved" : commandResult)
      +", outcome="+outcome());
   } finally {closed=true;world=null;if(COMMAND.get()==this)COMMAND.remove();if(LOCATE.get()==this)LOCATE.remove();}
  }
 }
}
