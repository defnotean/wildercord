package dev.wildercord.wildlife;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.*;

/** Native amphibious routes, with aquatic approaches for waterlogged refuges. */
final class NewtPathNavigation extends AmphibiousPathNavigation {
 private boolean aquaticSearch,refugeRoute;

 NewtPathNavigation(LanternNewt newt,Level level) {super(newt,level);}

 boolean followingRefuge() {return refugeRoute && !isDone();}

 private boolean waterloggedRefuge(BlockPos at) {
  if(!level.hasChunkAt(at))return false;
  var state=level.getBlockState(at);
  return state.is(WetlandShelters.REFUGE) && state.getValue(BlockStateProperties.WATERLOGGED);
 }

 @Override protected PathFinder createPathFinder(int maxVisitedNodes) {
  nodeEvaluator=new AmphibiousNodeEvaluator(false) {
   @Override public Node getStart() {
    var start=super.getStart();
    if(aquaticSearch && mob.isInWater()) {
     var rounded=new BlockPos(start.x,start.y,start.z);
     var feet=new BlockPos(start.x,net.minecraft.util.Mth.floor(mob.getY()),start.z);
     // Vanilla rounds swimming feet upward by .5, which can put a shallow
     // swimmer's start in dry air. Keep its native horizontal anchor.
     if(!currentContext.getBlockState(rounded).getFluidState().is(FluidTags.WATER)
       && currentContext.getBlockState(feet).getFluidState().is(FluidTags.WATER))return getStartNode(feet);
    }
    return start;
   }
   @Override public PathType getPathType(PathfindingContext context,int x,int y,int z) {
    // Shelter cannot continue out of water in clear weather. Do not admit a dry
    // stepping stone even when vanilla classifies it as WATER_BORDER.
    if(aquaticSearch && !context.getBlockState(new BlockPos(x,y,z)).getFluidState().is(FluidTags.WATER))return PathType.BLOCKED;
    return super.getPathType(context,x,y,z);
   }
  };
  return new PathFinder(nodeEvaluator,maxVisitedNodes);
 }

 @Override protected Path createPath(Set<BlockPos> targets,int padding,boolean above,int accuracy,float range) {
  boolean previous=aquaticSearch;
  aquaticSearch=targets.size()==1 && waterloggedRefuge(targets.iterator().next());
  try{return super.createPath(targets,padding,above,accuracy,range);}
  finally{aquaticSearch=previous;}
 }

 @Override public boolean moveTo(Path route,double speed) {
  boolean accepted=super.moveTo(route,speed);
  refugeRoute=accepted && path!=null && waterloggedRefuge(path.getTarget());
  return accepted;
 }

 @Override public void tick() {
  if(refugeRoute && (path==null || !waterloggedRefuge(path.getTarget()))) {stop();return;}
  super.tick();
 }

 @Override public void recomputePath() {
  if(refugeRoute && (getTargetPos()==null || !waterloggedRefuge(getTargetPos()))) {stop();return;}
  super.recomputePath();
 }

 @Override public boolean canCutCorner(PathType type) {
  // The inherited point ray can skip a boundary waypoint before the body clears
  // its obstacle. Keep normal proximity advancement for these short approaches.
  return !refugeRoute && super.canCutCorner(type);
 }

 @Override protected void followThePath() {
  if(refugeRoute && path.getNextNodeIndex()==path.getNodeCount()-1
    && !((LanternNewt)mob).beneathRefuge(path.getTarget())) {
   // Vanilla's .45 horizontal tolerance ends swimming before Shelter's .28
   // arrival. Keep this native last waypoint live, including its stuck timeout.
   doStuckDetection(getTempMobPos());return;
  }
  super.followThePath();
 }

 @Override public void stop() {
  // A deferred native recompute must not revive a cancelled shelter journey.
  if(refugeRoute)hasDelayedRecomputation=false;
  super.stop();refugeRoute=false;
 }
}
