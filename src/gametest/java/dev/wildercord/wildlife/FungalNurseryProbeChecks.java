package dev.wildercord.wildlife;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;

/** Standalone negative controls for optional native evidence; does not create a world or alter AI. */
public final class FungalNurseryProbeChecks {
 public static void main(String[] arguments) throws Exception {
  var plant=new BlockPos(4,30,0);var floor=new BlockPos(4,30,2);
  var approach=FungalNurseryTest.nurseryApproachCells(plant,floor);
  check(approach.size()==12&&new java.util.HashSet<>(approach).size()==12,"North entrance is deduplicated with the original nine plant cells");
  var originalCells=new ArrayList<BlockPos>();for(var at:BlockPos.betweenClosed(plant.offset(-1,0,-1),plant.offset(1,0,1)))originalCells.add(at.immutable());
  check(approach.subList(0,9).equals(originalCells),"Existing real pruning scan order is preserved");
  for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)check(approach.contains(floor.relative(direction)),"Every cardinal entrance is considered");
  check(!approach.contains(floor.offset(-1,0,1))&&!approach.contains(floor.offset(1,0,1))&&!approach.contains(floor),"No diagonal garden expansion or canopy-floor replacement enters pruning");
  var absent=FungalNurseryProbe.visit(null,true);
  check(!absent.present()&&!absent.running()&&absent.destination()==null,"An absent goal cannot become an active arrival");
  var goal=new InactiveVisit();
  var inactive=FungalNurseryProbe.visit(goal,false);
  check(inactive.present()&&!inactive.running()&&inactive.destination()==null&&inactive.chew()==0,"An inactive goal with no destination remains explicitly inactive");
  check(goal.left==0&&goal.searchAt==120&&goal.scanAt==7&&goal.retries==3&&goal.sweep==null,"Observation preserves existing goal state");
  check(FungalNurseryProbe.observe(()->{throw new IllegalStateException("missing field");}).contains("observationError=IllegalStateException"),"Read failure is evidence, not a changed native result");
  check(FungalNurseryProbe.observe(()->{throw new AssertionError("diagnostic only");}).contains("observationError=AssertionError"),"Diagnostic assertion cannot replace the native assertion");
  check(FungalNurseryProbe.observe(()->null)==null,"Intentionally skipped rows stay silent");
  var rows=new ArrayList<String>();var probe=new FungalNurseryProbe(rows::add);
  for(int i=0;i<150;i++)probe.sample(null,null,i,false);
  check(rows.size()==FungalNurseryProbe.MAX_ROWS,"Null visitor observations cannot grow output without bound");
  probe.sample(null,null,150,true);
  check(rows.size()==FungalNurseryProbe.MAX_ROWS+1&&rows.getLast().equals("visitor=none"),"Final evidence survives an exhausted ordinary output budget");
  var brokenOutput=new FungalNurseryProbe(row->{throw new IllegalStateException("logger unavailable");});
  brokenOutput.sample(null,null,0,false);brokenOutput.sample(null,null,150,true);
  AssertionError original=new AssertionError("Rested repeat visitor finds actual nursery");
  try{brokenOutput.sample(null,null,150,true);throw original;}catch(AssertionError failure){check(failure==original,"Original native failure remains the same assertion");}
  System.out.println("FungalNurseryProbeChecks: negative controls passed; no world, motion or RNG used");
 }
 private static final class InactiveVisit {
  private final BlockPos destination=null;
  private final boolean shelter=true,nursery=true;
  private final int chew=0,left=0,searchAt=120,scanAt=7,retries=3;
  private final HabitatSweep sweep=null;
 }
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
