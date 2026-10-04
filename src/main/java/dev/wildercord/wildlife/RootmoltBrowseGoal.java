package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import java.util.EnumSet;
/** Retained local sweep/path and a close uninterrupted meal; competing consumers cannot double-spend a cap. */
public final class RootmoltBrowseGoal extends Goal {
 private final RootmoltStrider mob;private HabitatSweep sweep;private BlockPos cap;private int searchAt,scanAt,left,chew,retries;
 public RootmoltBrowseGoal(RootmoltStrider mob) {this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 private boolean mature(BlockPos p) {var l=mob.level();return l.hasChunkAt(p) && l.getBlockState(p).is(FungalGarden.GLOWCAP) && l.getBlockState(p).getValue(GlowcapBlock.AGE)==2;}
 @Override public boolean canUse() {return mob.tickCount>=searchAt && mob.canBrowse();}
 @Override public void start() {sweep=new HabitatSweep(RootmoltRules.SEARCH_RADIUS);var p=mob.blockPosition();sweep.anchor(p.getX(),p.getY(),p.getZ());cap=null;scanAt=mob.tickCount;left=RootmoltRules.JOURNEY;chew=0;retries=0;}
 @Override public boolean canContinueToUse() {return left>0 && chew<RootmoltRules.BROWSE && mob.getTarget()==null && (mob.pose()==RootmoltStrider.IDLE || mob.pose()==RootmoltStrider.BROWSING) && (cap==null || mature(cap));}
 private void seek() {if(mob.tickCount<scanAt)return;scanAt=mob.tickCount+20;int paths=0;for(int k=0;k<RootmoltRules.SEARCH_COLUMNS;k++) {var offset=sweep.next();if(offset==null){left=0;return;}for(int dy=-1;dy<=1;dy++){var p=new BlockPos(sweep.x()+offset.x(),sweep.y()+dy,sweep.z()+offset.z());if(!mature(p))continue;if(paths>=RootmoltRules.PATH_BUDGET)return;paths++;var path=mob.getNavigation().createPath(p,0);if(path!=null && path.canReach()){cap=p;left=RootmoltRules.JOURNEY;mob.getNavigation().moveTo(path,RootmoltRules.VISIT_SPEED);return;}}}}
 @Override public void tick() {
  left--;if(cap==null){seek();return;}
  boolean near=mob.distanceToSqr(cap.getX()+.5,cap.getY(),cap.getZ()+.5)<.85 && mob.level().clip(new ClipContext(mob.getEyePosition(),Vec3.atCenterOf(cap),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob)).getType()==HitResult.Type.MISS;
  if(near) {mob.getNavigation().stop();mob.beginBrowse(cap);if(++chew==RootmoltRules.BROWSE && mature(cap) && mob.level() instanceof ServerLevel l) {mob.tryMeal(l,cap);}}
  else {chew=0;mob.stopBrowse();if(mob.getNavigation().isDone() && mob.tickCount%20==0 && retries++<3){var path=mob.getNavigation().createPath(cap,0);if(path!=null && path.canReach())mob.getNavigation().moveTo(path,RootmoltRules.VISIT_SPEED);}}
 }
 @Override public boolean requiresUpdateEveryTick() {return true;}
 @Override public void stop() {mob.stopBrowse();mob.getNavigation().stop();cap=null;sweep=null;searchAt=mob.tickCount+120;}
}
