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
import java.util.function.Consumer;

/** Passive receipts for one exact fixture. Retains coordinates/counters, never worlds or RNGs. */
public final class WetlandGenerationProbe {
 static final long SEED=-7620530482425397421L;
 static final String FEATURE="wildercord:moonreed_patch";
 private static final int MAX_LINES=4096;
 private static volatile Session active;
 private static final ThreadLocal<Source> PLACEMENT=new ThreadLocal<>();
 private static final ThreadLocal<Invocation> INVOCATION=new ThreadLocal<>();
 private WetlandGenerationProbe() {}

 public static Session beginFixture() {return begin(line -> Wildercord.LOGGER.info(line));}
 static synchronized Session begin(Consumer<String> sink) {
  if(active!=null)throw new IllegalStateException("Wetland diagnostic fixture already active");
  var session=new Session(sink);active=session;
  session.emit("BEGIN seed="+SEED+", habitat=-1536/64/-768, examined=-100..-92/-52..-44, sourceHalo=-101..-91/-53..-43");
  return session;
 }

 static boolean inScope(Session session,boolean region,boolean overworld,long seed,int x,int z) {
  return session!=null && active==session && !session.closed && region && overworld && seed==SEED
    && x>=-101 && x<=-91 && z>=-53 && z<=-43;
 }
 private static boolean inScope(Session session,WorldGenLevel level) {
  if(session==null || session.closed || active!=session || !(level instanceof WorldGenRegion region))return false;
  var source=region.getCenter();
  return inScope(session,true,region.getLevel().dimension()==Level.OVERWORLD,region.getSeed(),source.x(),source.z());
 }
 private static boolean inside(int x,int z) {return x>=-100 && x<=-92 && z>=-52 && z<=-44;}

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
  PLACEMENT.set(source);
  String featureKey=feature.feature().unwrapKey().map(key -> key.identifier().toString()).orElse("unregistered");
  session.emit("SCHEDULE "+source.label()+", seed="+SEED+", stage="+stage+", index="+index
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
  invocation.source.session.emit("ATTEMPT "+invocation.source.label()+", origin="+position(invocation.origin)
    +", ordinal="+invocation.ordinal+", at="+position(invocation.at)+", gates="+invocation.gates+", "+outcome);
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
  var session=active;if(session!=null && inside(chunk.x(),chunk.z()))session.checkpoint(chunk);
 }
 private static String position(BlockPos pos) {return pos.getX()+"/"+pos.getY()+"/"+pos.getZ();}

 public static final class Session implements AutoCloseable {
  private final Consumer<String> sink;
  private final Map<Long,Source> sources=new LinkedHashMap<>();
  private final Map<String,Integer> counters=new LinkedHashMap<>();
  private volatile boolean closed;
  private int lines,omitted,checkpoints,sinkFailures;
  private Session(Consumer<String> sink) {this.sink=sink;}
  synchronized Source claim(int x,int z) {
   if(!inScope(this,true,true,SEED,x,z))return null;
   long key=((long)x<<32)^(z&0xffffffffL);
   if(sources.containsKey(key)) {count("duplicateSources");return null;}
   var source=new Source(this,x,z);sources.put(key,source);return source;
  }
  synchronized void count(String name) {if(!closed)counters.merge(name,1,Integer::sum);}
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
   if(closed || checkpoints>=81)return;
   checkpoints++;emit("CHECKPOINT requestedChunk="+chunk+", afterRequestedChunkReturn=true, "+summary());
  }
  synchronized String summary() {
   long inner=sources.values().stream().filter(source -> inside(source.x,source.z)).count();
   return "scheduledInside="+inner+", scheduledHalo="+(sources.size()-inner)+", counters="+counters
     +", checkpoints="+checkpoints+", omittedLines="+omitted+", sinkFailures="+sinkFailures;
  }
  int retainedSources() {return sources.size();}
  @Override public void close() {
   try {
    synchronized(this) {
     if(closed)return;
     try {send("WETLAND_GENERATION END "+summary());}
     finally {closed=true;sources.clear();counters.clear();}
    }
   } finally {
    synchronized(WetlandGenerationProbe.class) {if(active==this)active=null;}
    PLACEMENT.remove();INVOCATION.remove();
   }
  }
 }
 public static final class Source {
  private final Session session;
  private final int x,z;
  private int featureCalls;
  private Source(Session session,int x,int z) {this.session=session;this.x=x;this.z=z;}
  private String label() {return "source="+x+"/"+z+", scope="+(inside(x,z) ? "inside81" : "halo");}
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
