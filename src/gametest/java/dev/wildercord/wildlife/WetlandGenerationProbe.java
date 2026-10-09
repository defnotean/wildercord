package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.gametest.mixin.WetlandFeatureIndexAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

/** Passive receipts for one bounded fixture. Retains coordinates/counters, never worlds or RNGs. */
public final class WetlandGenerationProbe {
 static final long SEED=-7620530482425397421L;
 static final String FEATURE="wildercord:moonreed_patch";
 static final Scope ABSENCE=new Scope("absence",SEED,-100,-92,-52,-44);
 private static final int MAX_LINES=4096;
 private static volatile Session active;
 private static final ThreadLocal<Source> PLACEMENT=new ThreadLocal<>();
 private static final ThreadLocal<Invocation> INVOCATION=new ThreadLocal<>();
 private WetlandGenerationProbe() {}

 public static Session beginFixture() {return begin(ABSENCE);}
 static Session begin(Scope scope) {return begin(scope,line -> Wildercord.LOGGER.info(line));}
 static Session begin(Consumer<String> sink) {return begin(ABSENCE,sink);}
 static synchronized Session begin(Scope scope,Consumer<String> sink) {
  if(active!=null)throw new IllegalStateException("Wetland diagnostic fixture already active");
  var session=new Session(scope,sink);active=session;
  session.emit("BEGIN fixture="+scope.name+", seed="+scope.seed+", examined="+scope.minX+".."+scope.maxX+"/"+scope.minZ+".."+scope.maxZ);
  return session;
 }

 static boolean inScope(Session session,boolean region,boolean overworld,long seed,int x,int z) {
  return session!=null && active==session && !session.closed && region && overworld && seed==session.scope.seed
    && session.scope.inHalo(x,z);
 }
 private static boolean inScope(Session session,WorldGenLevel level) {
  if(session==null || session.closed || active!=session || !(level instanceof WorldGenRegion region))return false;
  var source=region.getCenter();
  return inScope(session,true,region.getLevel().dimension()==Level.OVERWORLD,region.getSeed(),source.x(),source.z());
 }
 record Scope(String name,long seed,int minX,int maxX,int minZ,int maxZ) {
  Scope {if(minX>maxX || minZ>maxZ || maxX-minX>8 || maxZ-minZ>8)throw new IllegalArgumentException("At most a 9 by 9 chunk fixture");}
  boolean inside(int x,int z) {return x>=minX && x<=maxX && z>=minZ && z<=maxZ;}
  boolean inHalo(int x,int z) {return x>=minX-1 && x<=maxX+1 && z>=minZ-1 && z<=maxZ+1;}
  int chunks() {return (maxX-minX+1)*(maxZ-minZ+1);}
 }

 public static Source beginPlacement(WorldGenLevel level,ChunkGenerator generator,PlacedFeature feature,boolean biomeCheck) {
  var session=active;
  if(session==null || !biomeCheck || !(feature.feature().value() instanceof MoonreedFeature) || !inScope(session,level))return null;
  String placedKey=String.valueOf(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getKey(feature));
  if(!FEATURE.equals(placedKey))return null;
  var center=((WorldGenRegion)level).getCenter();
  // Native applyBiomeDecoration already evaluated this memoized supplier before calling FeaturePlacer.
  var steps=((WetlandFeatureIndexAccessor)generator).wildercord$featuresPerStep().get();
  int stage=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
  var data=steps.get(stage);int index=data.indexMapping().applyAsInt(feature);
  var source=session.claim(center.x(),center.z());
  if(source==null)return null;
  session.index(index,data.features().size());
  PLACEMENT.set(source);
  String featureKey=feature.feature().unwrapKey().map(key -> key.identifier().toString()).orElse("unregistered");
  session.emit("SCHEDULE "+source.label()+", seed="+session.scope.seed+", stage="+stage+", index="+index
    +", stepFeatures="+data.features().size()+", placedKey="+placedKey+", featureKey="+featureKey);
  return source;
 }

 public static void endPlacement(Source source,Boolean result) {
  if(source==null)return;
  try {
   source.session.count(result==null ? "placementExceptions" : "placementsReturned");
   source.session.emit("PLACEMENT_RETURN "+source.label()+", result="+result);
  } finally {PLACEMENT.remove();INVOCATION.remove();}
 }

 public static Source modifierSource() {var source=PLACEMENT.get();return live(source) ? source : null;}
 private static boolean live(Source source) {return source!=null && active==source.session && !source.session.closed;}

 /** Calls the modifier once; original output objects reach its original consumer immediately. */
 public static void modifier(Source source,String modifier,BlockPos input,Consumer<Consumer<BlockPos>> original,Consumer<BlockPos> output) {
  if(!live(source)) {original.accept(output);return;}
  var positions=new ArrayList<BlockPos>(1);int[] count={0};boolean[] returned={false};
  try {
   original.accept(at -> {
    // Copy before forwarding because the consumer may mutate a mutable position; never replace its object.
    var copy=at.immutable();count[0]++;
    if(positions.size()<24)positions.add(copy);
    output.accept(at);
   });
   returned[0]=true;
  } finally {
   source.session.count("modifierCalls");
   if(returned[0] && modifier.equals("RarityFilter"))source.session.count(count[0]==0 ? "rarityRejected" : "rarityAdmitted");
   if(returned[0] && modifier.equals("BiomeFilter"))source.session.count(count[0]==0 ? "biomeRejected" : "biomeAdmitted");
   source.session.emit("MODIFIER "+source.label()+", modifier="+modifier+", input="+position(input)
     +", outputCount="+count[0]+", outputs="+positions.stream().map(WetlandGenerationProbe::position).toList()
     +", returned="+returned[0]+", omitted="+Math.max(0,count[0]-positions.size()));
  }
 }

 public static Invocation beginFeature(WorldGenLevel level,BlockPos origin) {
  var source=PLACEMENT.get();
  if(!live(source) || !inScope(source.session,level))return null;
  var center=((WorldGenRegion)level).getCenter();
  if(center.x()!=source.x || center.z()!=source.z)return null;
  return beginFeature(source,origin);
 }
 static Invocation beginFeature(Source source,BlockPos origin) {
  if(!live(source))return null;
  // The registered placement has one output. Bound evidence if that contract ever changes.
  if(++source.featureCalls>1) {source.session.count("extraFeatureCalls");return null;}
  var invocation=new Invocation(source,origin.immutable());INVOCATION.set(invocation);
  source.session.count("featureCalls");
  source.session.emit("FEATURE_HEAD "+source.label()+", origin="+position(origin));return invocation;
 }
 public static void candidate(int x,int y,int z) {
  var invocation=INVOCATION.get();if(invocation==null || !live(invocation.source))return;
  if(invocation.ordinal>0 && !invocation.terminal)invocation.source.session.count("unterminatedAttempts");
  invocation.ordinal++;invocation.at=new BlockPos(x,y,z);invocation.gates.setLength(0);invocation.terminal=false;
  if(invocation.ordinal<=24)invocation.source.session.count("attempts");
  else invocation.source.session.count("extraAttempts");
 }
 public static void gate(String gate,boolean result) {
  var invocation=INVOCATION.get();if(invocation==null || !live(invocation.source) || invocation.ordinal>24)return;
  if(!invocation.gates.isEmpty())invocation.gates.append('/');
  invocation.gates.append(gate).append('=').append(result);
  if(!result)terminal(invocation,"reject="+gate,"reject_"+gate);
 }
 public static void write(boolean result) {
  var invocation=INVOCATION.get();if(invocation==null || !live(invocation.source) || invocation.ordinal>24)return;
  terminal(invocation,"writeReturn="+result,result ? "writesTrue" : "writesFalse");
 }
 private static void terminal(Invocation invocation,String outcome,String counter) {
  if(invocation.terminal) {invocation.source.session.count("duplicateTerminals");return;}
  invocation.terminal=true;invocation.source.session.count("terminals");invocation.source.session.count(counter);
  String receipt="source="+invocation.source.x+"/"+invocation.source.z+", origin="+position(invocation.origin)
    +", ordinal="+invocation.ordinal+", at="+position(invocation.at)+", gates="+invocation.gates+", "+outcome;
  invocation.source.session.attempt(receipt,counter.equals("writesTrue") ? invocation.at : null);
  invocation.source.session.emit("ATTEMPT "+receipt);
 }
 public static void endFeature(Invocation invocation,Boolean result) {
  if(invocation==null)return;
  try {
   if(invocation.ordinal>0 && !invocation.terminal)invocation.source.session.count("unterminatedAttempts");
   if(invocation.ordinal!=24)invocation.source.session.count("non24Invocations");
   invocation.source.session.count(result==null ? "featureExceptions" : "featuresReturned");
   invocation.source.session.emit("FEATURE_RETURN "+invocation.source.label()+", origin="+position(invocation.origin)
     +", attempts="+invocation.ordinal+", result="+result);
  } finally {INVOCATION.remove();}
 }
 public static void afterRequestedChunk(ChunkPos chunk) {
  var session=active;if(session!=null && session.scope.inside(chunk.x(),chunk.z()))session.checkpoint(chunk);
 }
 private static String position(BlockPos pos) {return pos.getX()+"/"+pos.getY()+"/"+pos.getZ();}

 public static final class Session implements AutoCloseable {
  private final Scope scope;
  private final Consumer<String> sink;
  private final Map<Long,Source> sources=new LinkedHashMap<>();
  private final Map<String,Integer> counters=new LinkedHashMap<>();
  private final List<String> attempts=new ArrayList<>();
  private final List<BlockPos> writes=new ArrayList<>();
  private final Set<Integer> indexes=new LinkedHashSet<>(),stepSizes=new LinkedHashSet<>();
  private volatile boolean closed;
  private int lines,omitted,checkpoints,sinkFailures;
  private Session(Scope scope,Consumer<String> sink) {this.scope=scope;this.sink=sink;}
  synchronized Source claim(int x,int z) {
   if(!inScope(this,true,true,scope.seed,x,z))return null;
   long key=((long)x<<32)^(z&0xffffffffL);
   if(sources.containsKey(key)) {count("duplicateSources");return null;}
   var source=new Source(this,x,z);sources.put(key,source);return source;
  }
  synchronized void count(String name) {if(!closed)counters.merge(name,1,Integer::sum);}
  synchronized void index(int index,int size) {if(!closed) {indexes.add(index);stepSizes.add(size);}}
  synchronized void attempt(String receipt,BlockPos written) {
   if(closed)return;
   if(attempts.size()>=121*24) {count("omittedAttempts");return;}
   attempts.add(receipt);if(written!=null)writes.add(written.immutable());
  }
  synchronized Receipt receipt() {
   return new Receipt(Map.copyOf(counters),List.copyOf(attempts),List.copyOf(writes),Set.copyOf(indexes),Set.copyOf(stepSizes),checkpoints,omitted,sinkFailures);
  }
  synchronized void emit(String line) {
   if(closed)return;
   if(lines>=MAX_LINES) {
    if(omitted++==0)send("WETLAND_GENERATION TRUNCATED eventLineLimit="+MAX_LINES);
    return;
   }
   lines++;send("WETLAND_GENERATION "+line);
  }
  private void send(String line) {
   // A diagnostic sink failure must not replace a native return/exception or strand the session.
   try {sink.accept(line);}catch(RuntimeException ignored) {sinkFailures++;}
  }
  synchronized void checkpoint(ChunkPos chunk) {
   if(closed || checkpoints>=scope.chunks())return;
   checkpoints++;emit("CHECKPOINT requestedChunk="+chunk+", afterRequestedChunkReturn=true, "+summary());
  }
  synchronized String summary() {
   long inner=sources.values().stream().filter(source -> scope.inside(source.x,source.z)).count();
   return "scheduledInside="+inner+", scheduledHalo="+(sources.size()-inner)+", counters="+counters
     +", checkpoints="+checkpoints+", omittedLines="+omitted+", sinkFailures="+sinkFailures;
  }
  int retainedSources() {return sources.size();}
  @Override public void close() {
   try {
    synchronized(this) {
     if(closed)return;
     try {send("WETLAND_GENERATION END "+summary());}
     finally {closed=true;sources.clear();counters.clear();attempts.clear();writes.clear();indexes.clear();stepSizes.clear();}
    }
   } finally {
    synchronized(WetlandGenerationProbe.class) {if(active==this)active=null;}
    PLACEMENT.remove();INVOCATION.remove();
   }
  }
 }
 record Receipt(Map<String,Integer> counters,List<String> attempts,List<BlockPos> writes,Set<Integer> indexes,Set<Integer> stepSizes,int checkpoints,int omittedLines,int sinkFailures) {
  int count(String key) {return counters.getOrDefault(key,0);}
 }
 public static final class Source {
  private final Session session;
  private final int x,z;
  private int featureCalls;
  private Source(Session session,int x,int z) {this.session=session;this.x=x;this.z=z;}
  private String label() {return "source="+x+"/"+z+", scope="+(session.scope.inside(x,z) ? "inside" : "halo");}
 }
 public static final class Invocation {
  private final Source source;
  private final BlockPos origin;
  private final StringBuilder gates=new StringBuilder();
  private BlockPos at;
  private int ordinal;
  private boolean terminal;
  private Invocation(Source source,BlockPos origin) {this.source=source;this.origin=origin;}
 }
}
