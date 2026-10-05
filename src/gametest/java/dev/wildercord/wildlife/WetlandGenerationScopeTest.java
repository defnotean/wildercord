package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;

/** Exercises diagnostic scope and forwarding with scalar receipts, without creating a world. */
public final class WetlandGenerationScopeTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {verify();}
 public static void main(String[] arguments) {verify();System.out.println("WetlandGenerationScopeTest passed");}
 private static void verify() {
  var rows=new ArrayList<String>();
  var session=WetlandGenerationProbe.begin(rows::add);
  try(session) {
   check(WetlandGenerationProbe.inScope(session,true,true,WetlandGenerationProbe.SEED,-96,-48),"exact fixture admitted");
   check(!WetlandGenerationProbe.inScope(session,false,true,WetlandGenerationProbe.SEED,-96,-48),"non-worldgen level refused");
   check(!WetlandGenerationProbe.inScope(session,true,false,WetlandGenerationProbe.SEED,-96,-48),"other dimension refused");
   check(!WetlandGenerationProbe.inScope(session,true,true,WetlandGenerationProbe.SEED+1,-96,-48),"other seed refused");
   for(var point:new int[][]{{-102,-48},{-90,-48},{-96,-54},{-96,-42}})
    check(!WetlandGenerationProbe.inScope(session,true,true,WetlandGenerationProbe.SEED,point[0],point[1]),"outside halo refused");
   var source=session.claim(-96,-48);check(source!=null,"source claimed");
   check(session.claim(-96,-48)==null,"duplicate source bounded");
   check(session.claim(-102,-48)==null,"out of scope source cannot be retained");
   var random=RandomSource.create(921);var reference=RandomSource.create(921);int expected=reference.nextInt(9);
   var mutable=new BlockPos.MutableBlockPos(1,2,3);int[] originalCalls={0},outputCalls={0};
   WetlandGenerationProbe.modifier(source,"Synthetic",new BlockPos(0,0,0),output -> {
    originalCalls[0]++;check(random.nextInt(9)==expected,"same sole random draw");output.accept(mutable);
    check(outputCalls[0]==1,"consumer is forwarded synchronously");
   },at -> {check(at==mutable,"original mutable output identity preserved");outputCalls[0]++;mutable.set(7,8,9);});
   check(originalCalls[0]==1 && outputCalls[0]==1,"original and consumer called exactly once");
   check(random.nextLong()==reference.nextLong(),"RNG stream unchanged by observer");
   check(rows.stream().anyMatch(line -> line.contains("outputs=[1/2/3]")),"receipt snapshots output before downstream mutation");
   WetlandGenerationProbe.modifier(null,"Synthetic",mutable,output -> {originalCalls[0]++;output.accept(mutable);},at -> outputCalls[0]++);
   check(originalCalls[0]==2 && outputCalls[0]==2,"disabled observer forwards unchanged");
   var failure=new RuntimeException("injected modifier failure");
   try {WetlandGenerationProbe.modifier(source,"Synthetic",mutable,output -> {throw failure;},at -> {throw new AssertionError("unexpected output");});
    throw new AssertionError("original exception swallowed");
   } catch(RuntimeException caught) {check(caught==failure,"original exception identity preserved");}
   var invocation=WetlandGenerationProbe.beginFeature(source,new BlockPos(0,64,0));
   try {
    for(int i=0;i<24;i++) {
     WetlandGenerationProbe.candidate(i,64,0);WetlandGenerationProbe.gate("empty",true);
     if(i<23)WetlandGenerationProbe.gate("survive",false);
     else {WetlandGenerationProbe.gate("survive",true);WetlandGenerationProbe.gate("moist",true);WetlandGenerationProbe.gate("openSky",true);WetlandGenerationProbe.write(false);}
    }
   } finally {WetlandGenerationProbe.endFeature(invocation,true);}
   check(rows.stream().filter(line -> line.contains(" ATTEMPT ")).count()==24,"24 attempts have exactly 24 terminals");
   check(rows.stream().anyMatch(line -> line.contains("writeReturn=false")),"failed write remains separate from production result");
   int completedRows=rows.size();WetlandGenerationProbe.candidate(0,64,0);WetlandGenerationProbe.gate("empty",false);
   check(rows.size()==completedRows,"feature return clears ThreadLocal state");
   var interrupted=WetlandGenerationProbe.beginFeature(session.claim(-95,-48),new BlockPos(0,64,0));
   try {WetlandGenerationProbe.candidate(0,64,0);throw failure;}
   catch(RuntimeException caught) {check(caught==failure,"feature failure propagates");}
   finally {WetlandGenerationProbe.endFeature(interrupted,null);}
   int failedRows=rows.size();WetlandGenerationProbe.gate("empty",false);
   check(rows.size()==failedRows,"feature exception clears ThreadLocal state");
   for(int x=-101;x<=-91;x++)for(int z=-53;z<=-43;z++)session.claim(x,z);
   check(session.retainedSources()==121,"source retention bounded to exact halo");
  }
  check(session.retainedSources()==0,"successful teardown clears sources");
  check(!WetlandGenerationProbe.inScope(session,true,true,WetlandGenerationProbe.SEED,-96,-48),"closed fixture refused");
  int closedRows=rows.size();session.emit("late");check(rows.size()==closedRows,"closed session emits nothing");
  var failed=WetlandGenerationProbe.begin(rows::add);
  try(failed) {
   failed.claim(-96,-48);throw new RuntimeException("injected fixture failure");
  } catch(RuntimeException expected) {check(expected.getMessage().equals("injected fixture failure"),"fixture failure propagates");}
  check(failed.retainedSources()==0,"failing teardown clears sources");
  check(!WetlandGenerationProbe.inScope(failed,true,true,WetlandGenerationProbe.SEED,-96,-48),"failed fixture refused");
  try(var next=WetlandGenerationProbe.begin(rows::add)) {check(next.retainedSources()==0,"next fixture starts clean");}
  var cappedRows=new ArrayList<String>();
  try(var capped=WetlandGenerationProbe.begin(cappedRows::add)) {for(int i=0;i<5000;i++)capped.emit("synthetic");}
  check(cappedRows.size()==4098,"event cap includes one truncation receipt and one terminal summary");
  check(cappedRows.getLast().contains("END ") && cappedRows.getLast().contains("omittedLines=905"),"truncation remains explicit at teardown");
  var brokenSink=WetlandGenerationProbe.begin(line -> {throw new IllegalStateException("injected sink failure");});
  try(brokenSink) {brokenSink.claim(-96,-48);brokenSink.emit("native operation still returns");}
  check(brokenSink.retainedSources()==0,"sink failure cannot strand retained sources");
  check(!WetlandGenerationProbe.inScope(brokenSink,true,true,WetlandGenerationProbe.SEED,-96,-48),"sink failure cannot leave fixture active");
  try(var afterBrokenSink=WetlandGenerationProbe.begin(rows::add)) {check(afterBrokenSink.retainedSources()==0,"fixture restarts after sink failure");}
 }
 private static void check(boolean condition,String reason) {if(!condition)throw new AssertionError(reason);}
}
