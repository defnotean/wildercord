package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.EnumSet;
/** Nearest retained bounded sweep; actual arrival plus40 uninterrupted browse ticks, no remote settlement. */
public final class EmberHabitatGoal extends Goal {
 private final CinderBailiff mob;private HabitatSweep sweep;private BlockPos fern;private int searchAt,scanAt,left,chew,retries;
 public EmberHabitatGoal(CinderBailiff mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 @Override public boolean canUse(){return mob.tickCount>=searchAt&&mob.mayBrowse();}
 @Override public void start(){if(sweep==null)sweep=new HabitatSweep(6);var p=mob.blockPosition();sweep.anchor(p.getX(),p.getY(),p.getZ());fern=null;scanAt=mob.tickCount;left=EmberRules.JOURNEY;chew=0;retries=0;}
 @Override public boolean canContinueToUse(){return left>0&&chew<EmberRules.BROWSE&&mob.getTarget()==null&&(mob.pose()==CinderBailiff.IDLE||mob.pose()==CinderBailiff.BROWSING)&&(fern==null||CinderBailiff.mature(mob.level(),fern));}
 private void seek(){if(mob.tickCount<scanAt)return;scanAt=mob.tickCount+100;int paths=0;
  for(int k=0;k<5;k++){var offset=sweep.next();if(offset==null){left=0;return;}for(int dy=-1;dy<=1;dy++){var p=new BlockPos(sweep.x()+offset.x(),sweep.y()+dy,sweep.z()+offset.z());if(!CinderBailiff.mature(mob.level(),p))continue;if(reached(p)){fern=p;mob.getNavigation().stop();return;}if(paths++>=2)return;var path=mob.getNavigation().createPath(p,0);if(path!=null&&path.canReach()){fern=p;mob.getNavigation().moveTo(path,.65);return;}}}
 }
 private boolean reached(BlockPos p){return mob.distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)<1.3&&mob.level().clip(new ClipContext(mob.getEyePosition(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob)).getType()==HitResult.Type.MISS;}
 @Override public void tick(){left--;if(fern==null){seek();return;}boolean near=reached(fern);
  if(near){mob.getNavigation().stop();mob.browsing(true);if(++chew==EmberRules.BROWSE&&mob.level() instanceof ServerLevel l)mob.meal(l,fern);}
  else{chew=0;mob.browsing(false);if(mob.getNavigation().isDone()&&mob.tickCount%20==0&&retries++<3){var path=mob.getNavigation().createPath(fern,0);if(path!=null&&path.canReach())mob.getNavigation().moveTo(path,.65);}}
 }
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void stop(){mob.browsing(false);mob.getNavigation().stop();fern=null;searchAt=mob.tickCount+100;}
}
