package dev.wildercord.world.dungeons;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.config.WildercordConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/** Pure negative controls and direct handler checks; also runs before the native battlefield fixture. */
public final class BattlefieldsGenerationProbeChecks {
 private BattlefieldsGenerationProbeChecks() {}
 public static void main(String[] args) {
  verify();
  // Raw handler checks run only outside Mixin. Native execution must never load a mixin class directly.
  var world=new Object();
  try(var session=new BattlefieldsGenerationProbe.Session(721,line -> {})) {
   session.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
   try(var command=session.command(world,0,0,0)) {
    var scope=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,world,721,0,0);
    try {forwardingChecks();}finally {scope.finish(true);}
   }
  }
  System.out.println("BattlefieldsGenerationProbeChecks standalone handler forwarding passed");
 }
 static void verify() {
  var rows=new ArrayList<String>();var world=new Object();long seed=721;
  var session=new BattlefieldsGenerationProbe.Session(seed,rows::add);
  session.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
  BattlefieldsGenerationProbe.Candidate candidate;
  try(session) {
   try {session.command(world,0,48,0);throw new AssertionError("off-grid candidate accepted");}
   catch(IllegalStateException expected) { }
   candidate=session.command(world,0,0,0);
   try(candidate) {
    check(BattlefieldsGenerationProbe.matches(candidate,BattlefieldsGenerationProbe.SUITE,world,seed,0,0),"matching command admitted");
    check(!BattlefieldsGenerationProbe.matches(candidate,"other.Test",world,seed,0,0),"wrong scene refused");
    check(!BattlefieldsGenerationProbe.matches(candidate,BattlefieldsGenerationProbe.SUITE,new Object(),seed,0,0),"wrong world identity refused");
    check(!BattlefieldsGenerationProbe.matches(candidate,BattlefieldsGenerationProbe.SUITE,world,seed+1,0,0),"wrong seed refused");
    check(!BattlefieldsGenerationProbe.matches(candidate,BattlefieldsGenerationProbe.SUITE,world,seed,1,0),"wrong candidate chunk refused");
    try {session.command(world,1,48,0);throw new AssertionError("nested command accepted");}
    catch(IllegalStateException expected) { }
    var scope=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,world,seed,0,0);
    try {
     BattlefieldsGenerationProbe.configured(true);
     BattlefieldsGenerationProbe.height(new BlockPos(15,0,15),Heightmap.Types.WORLD_SURFACE_WG,70);
     BattlefieldsGenerationProbe.footing(70,70,new int[]{70,70,70,70},63,true);
     // Wrong-world nesting masks the active locate and cannot overwrite its observed config.
     var nested=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,new Object(),seed,0,0);
     try {BattlefieldsGenerationProbe.configured(false);}finally {nested.finish(false);}
     var worker=Thread.ofPlatform().start(() -> {
      var otherThread=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,world,seed,0,0);
      try {BattlefieldsGenerationProbe.configured(false);}finally {otherThread.finish(false);}
     });
     try {worker.join();}catch(InterruptedException error) {Thread.currentThread().interrupt();throw new AssertionError(error);}
    } finally {scope.finish(true);}
    BattlefieldsGenerationProbe.configured(false); // Return already removed the locate scope.
    candidate.callback(false,0);candidate.returned();
   }
   candidate.scanned(new BlockPos(15,71,15));
   check(candidate.outcome().equals("command_failed_after_admission"),"authentic scan cannot turn command failure into success");
   check(rows.stream().anyMatch(row -> row.contains("COMMAND attempt=0") && row.contains("configEnabled=true") && row.contains("locateAdmitted=true")),"nested, unrelated-thread and after-return calls leave admission unchanged");
   check(rows.stream().anyMatch(row -> row.contains("heightCalls=1") && row.contains("15/0/15:WORLD_SURFACE_WG=70")),"height return captured without resampling");
   check(!BattlefieldsGenerationProbe.matches(candidate,BattlefieldsGenerationProbe.SUITE,world,seed,0,0),"closed command refused");
   for(int i=1;i<16;i++) {
    try(var next=session.command(world,i,(i%4)*48,(i/4)*48)) {
     var scope=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,world,seed,(i%4)*3,(i/4)*3);
     try {BattlefieldsGenerationProbe.configured(false);}finally {scope.finish(false);}
     next.callback(false,0);next.returned();next.scanned(null);
     check(next.outcome().equals("disabled_config"),"disabled admission distinguished");
    }
   }
   try {session.command(world,16,0,192);throw new AssertionError("17th candidate accepted");}
   catch(IllegalStateException expected) { }
  }
  int before=rows.size();session.emit("late");check(rows.size()==before,"closed fixture emits nothing");
  var failed=new BattlefieldsGenerationProbe.Session(seed,rows::add);failed.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
  var originalFailure=new IllegalStateException("original operation failure");
  try(failed;var command=failed.command(world,0,0,0)) {
   var scope=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,world,seed,0,0);
   try {throw originalFailure;}finally {scope.finish(null);}
  } catch(IllegalStateException caught) {check(caught==originalFailure,"original exception identity retained");}
  check(rows.stream().anyMatch(row -> row.contains("outcome=command_threw")),"exception records incomplete command distinctly");
  var afterFailure=new BattlefieldsGenerationProbe.Session(seed,rows::add);afterFailure.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
  try(afterFailure;var command=afterFailure.command(world,0,0,0)) {
   command.returned();check(command.outcome().equals("command_callback_count_0"),"missing callback is not false or success");
   command.callback(true,1);command.scanned(null);
   check(command.outcome().equals("command_succeeded_no_authentic_marker"),"successful command still requires authentic scan");
  }
  try(var unknown=new BattlefieldsGenerationProbe.Session(seed,rows::add)) {
   unknown.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
   try(var command=unknown.command(world,0,0,0)) {
    command.callback(true,1);command.returned();command.scanned(new BlockPos(15,71,15));
    check(command.outcome().equals("command_succeeded_authentic_marker_location_unobserved"),"missing generation evidence never asserts a nearby marker");
   }
   try(var command=unknown.command(world,1,48,0)) {
    command.callback(false,0);command.returned();command.scanned(null);
    check(command.outcome().equals("command_failed_locate_unobserved"),"missing locate observation is distinct from actual admission rejection");
   }
  }
  classificationChecks(world,seed);
  var cappedRows=new ArrayList<String>();var capped=new BattlefieldsGenerationProbe.Session(seed,cappedRows::add);
  try(capped) {for(int i=0;i<100;i++)capped.emit("synthetic");}
  check(cappedRows.size()==BattlefieldsGenerationProbe.MAX_LINES+1,"receipt line count is bounded including final summary");
  check(cappedRows.getLast().contains("omittedLines=61"),"truncation is explicit");
  var brokenSink=new BattlefieldsGenerationProbe.Session(seed,line -> {throw new IllegalStateException("sink failure");});
  brokenSink.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
  try(brokenSink;var command=brokenSink.command(world,0,0,0)) {command.callback(false,0);command.returned();}
  check(brokenSink.summary().contains("sinkFailures=4"),"sink failure is counted without replacing original result or cleanup");
  System.out.println("BATTLEFIELD_GENERATION CHECKS completed=scene_world_seed_chunk_thread_scope,nested_masking,original16_bound,return_exception_cleanup,sink_failure,line_cap,command_result_distinction; syntheticControlsOnly=true");
 }
 private static void classificationChecks(Object world,long seed) {
  var rows=new ArrayList<String>();
  try(var session=new BattlefieldsGenerationProbe.Session(seed,rows::add)) {
   session.habitat(BlockPos.ZERO,"synthetic","synthetic",true);
   for(int i=0;i<3;i++) {
    try(var command=session.command(world,i,i*48,0)) {
     var scope=BattlefieldsGenerationProbe.locate(BattlefieldsGenerationProbe.SUITE,world,seed,i*3,0);
     try {
      BattlefieldsGenerationProbe.configured(true);
      BattlefieldsGenerationProbe.height(new BlockPos(i*48+15,0,15),Heightmap.Types.WORLD_SURFACE_WG,70);
      var heights=new int[]{70,70,70,70};
      BattlefieldsGenerationProbe.footing(70,70,heights,63,i!=0);
      heights[0]=999;
     } finally {scope.finish(i!=0);}
     command.callback(i!=0,i==0 ? 0 : 1);command.returned();
     for(int j=0;j<30;j++)command.marker(new BlockPos(j,71,15),false);
     command.scanned(i==0 ? null : new BlockPos(i*48+15+(i==2 ? 1 : 0),71,15));
     String expected=switch(i) {case 0 -> "footing_rejected";case 1 -> "command_succeeded_expected_authentic_marker";default -> "command_succeeded_nearby_authentic_marker";};
     check(command.outcome().equals(expected),"actual footing and expected marker classification: "+expected);
    }
   }
  }
  check(rows.stream().filter(row -> row.contains("omittedMarkers=14")).count()==3,"marker retention bound records omissions");
  check(rows.stream().filter(row -> row.contains("neighbours=[70, 70, 70, 70]")).count()==3,"mutable original neighbour arrays are copied");
 }
 private static void forwardingChecks() {
  int[] calls={0};
  Operation<Boolean> configured=args -> {calls[0]++;check(args[0]==WildercordConfig.AuraWorldSettings.DEFAULTS,"same config receiver");return true;};
  check((Boolean)invoke("wildercord$config",WildercordConfig.AuraWorldSettings.DEFAULTS,configured),"config original result returned");
  var random=RandomSource.create(92);var reference=RandomSource.create(92);
  var expected=Direction.Plane.HORIZONTAL.getRandomDirection(reference);
  Operation<Direction> direction=args -> {calls[0]++;check(args[0]==Direction.Plane.HORIZONTAL && args[1]==random,"same direction receiver and RNG");return Direction.Plane.HORIZONTAL.getRandomDirection((RandomSource)args[1]);};
  check(invoke("wildercord$direction",Direction.Plane.HORIZONTAL,random,direction)==expected,"original direction identity returned");
  check(random.nextLong()==reference.nextLong(),"RNG stream has exactly the original draw");
  var at=new BlockPos.MutableBlockPos(15,0,15);
  Operation<Integer> height=args -> {calls[0]++;check(args[0]==null && args[1]==at && args[2]==Heightmap.Types.WORLD_SURFACE_WG,"same height arguments");return 70;};
  check((Integer)invoke("wildercord$height",null,at,Heightmap.Types.WORLD_SURFACE_WG,height)==70,"height original value returned");
  at.set(99,99,99); // Retained evidence copied the position before any later mutation.
  var neighbours=new int[]{70,70,70,70};
  Operation<Boolean> footing=args -> {calls[0]++;check(args[2]==neighbours,"same original neighbour array");return false;};
  check(!(Boolean)invoke("wildercord$footing",70,70,neighbours,63,footing),"actual false footing return preserved rather than recomputed");
  neighbours[0]=200;
  check(calls[0]==4,"each original handler operation called exactly once");
  var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(null,null,null,null,null,null,null,null,
    721,new net.minecraft.world.level.ChunkPos(0,0),net.minecraft.world.level.LevelHeightAccessor.create(0,256),biome -> true);
  var result=java.util.Optional.of(new net.minecraft.world.level.levelgen.structure.Structure.GenerationStub(BlockPos.ZERO,builder -> {}));
  Operation<java.util.Optional<net.minecraft.world.level.levelgen.structure.Structure.GenerationStub>> locate=args -> {
   calls[0]++;check(args[0]==context,"same locate context");return result;
  };
  check(invoke("wildercord$locate",context,locate)==result,"locate returns the original Optional identity");
  check(calls[0]==5,"locate original executes exactly once");
  var failure=new IllegalStateException("original handler failure");
  Operation<Boolean> throwing=args -> {throw failure;};
  try {invoke("wildercord$footing",70,70,neighbours,63,throwing);throw new AssertionError("original exception swallowed");}
  catch(IllegalStateException caught) {check(caught==failure,"handler original exception identity preserved");}
 }
 private static Object invoke(String name,Object... args) {
  try {
   var method=java.util.Arrays.stream(Class.forName("dev.wildercord.gametest.mixin.BattlefieldsGenerationProbeMixin").getDeclaredMethods()).filter(value -> value.getName().equals(name)).findFirst().orElseThrow();
   method.setAccessible(true);return method.invoke(null,args);
  } catch(InvocationTargetException error) {
   if(error.getCause() instanceof RuntimeException cause)throw cause;
   if(error.getCause() instanceof Error cause)throw cause;
   throw new AssertionError(error.getCause());
  } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
 }
 private static void check(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
}
