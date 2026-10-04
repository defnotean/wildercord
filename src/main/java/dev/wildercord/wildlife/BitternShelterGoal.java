package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
/** Genuine dry covered bank arrival, not a remote rest. Retained sweep and finite movement deadline. */
final class BitternShelterGoal extends Goal {
 private final SiltcrestBittern bird;private HabitatSweep sweep;private BlockPos at;private int scanAt,searchAt,left,settled,retries;
 BitternShelterGoal(SiltcrestBittern bird){this.bird=bird;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 public boolean canUse(){return bird.tickCount>=searchAt&&bird.pose()==SiltcrestBittern.IDLE&&bird.wantsShelter();}
 public void start(){if(sweep==null)sweep=new HabitatSweep(6);var p=bird.blockPosition();sweep.anchor(p.getX(),p.getY(),p.getZ());at=null;left=260;settled=0;retries=0;scanAt=bird.tickCount;}
 public boolean canContinueToUse(){return left>0&&bird.pose()==SiltcrestBittern.IDLE&&bird.wantsShelter()&&(at==null||BitternHabitat.shelter(bird.level(),at));}
 public boolean requiresUpdateEveryTick(){return true;}
 public void tick(){left--;if(at==null){if(bird.tickCount<scanAt)return;scanAt=bird.tickCount+20;int paths=0;for(int i=0;i<8&&paths<2;i++){var o=sweep.next();if(o==null){left=0;return;}for(int dy=-1;dy<=1&&paths<2;dy++){var p=new BlockPos(sweep.x()+o.x(),sweep.y()+dy,sweep.z()+o.z());if(!BitternHabitat.shelter(bird.level(),p))continue;if(bird.distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)<=.36){at=p;return;}paths++;var route=bird.getNavigation().createPath(p,0);if(route!=null&&route.canReach()){at=p;bird.getNavigation().moveTo(route,.8);return;}}}return;}
  if(bird.onGround()&&bird.distanceToSqr(at.getX()+.5,at.getY(),at.getZ()+.5)<=.36){bird.getNavigation().stop();bird.setSpeed(0);if(++settled>=20&&bird.settle((ServerLevel)bird.level(),at))left=0;}
  else{settled=0;if(bird.getNavigation().isDone()&&bird.tickCount%20==0&&retries++<3){var route=bird.getNavigation().createPath(at,0);if(route!=null&&route.canReach())bird.getNavigation().moveTo(route,.8);}}
 }
 public void stop(){at=null;bird.getNavigation().stop();searchAt=bird.tickCount+100;}
}
