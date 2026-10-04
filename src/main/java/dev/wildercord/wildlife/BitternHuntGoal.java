package dev.wildercord.wildlife;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import java.util.*;
/** Complete prey pool; dry footing avoids a swimming target that cannot ground a strike; retained bank cursor, at most two native path requests per search pulse. */
final class BitternHuntGoal extends Goal {
 private final SiltcrestBittern bird;private AbstractFish quarry;private BlockPos origin,bank;private int searchAt,scanAt,left,cursor,retries;
 private static final int[][] OFFSETS={{0,1},{1,0},{0,-1},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
 BitternHuntGoal(SiltcrestBittern bird){this.bird=bird;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 public boolean canUse(){return bird.tickCount>=searchAt&&bird.hungry();}
 public void start(){bird.stalking(true);left=SiltcrestBittern.JOURNEY;cursor=0;bank=null;scanAt=bird.tickCount;retries=0;var pool=bird.preyPool((ServerLevel)bird.level());quarry=pool.size()>=3?pool.getFirst():null;origin=quarry==null?null:quarry.blockPosition();}
 public boolean canContinueToUse(){return left>0&&bird.pose()==SiltcrestBittern.STALKING&&quarry!=null&&SiltcrestBittern.wildFish(quarry,(ServerLevel)bird.level())&&WetlandRules.night(bird.level().getOverworldClockTime())&&!bird.level().isRaining();}
 public boolean requiresUpdateEveryTick(){return true;}
 public void tick(){left--;var l=(ServerLevel)bird.level();if(quarry==null)return;if(bird.disturbed(l)){bird.retreat();left=0;return;}bird.getLookControl().setLookAt(quarry,30,30);
  if(bank==null){if(bird.tickCount<scanAt)return;scanAt=bird.tickCount+20;int paths=0;while(cursor<24&&paths<2){var o=OFFSETS[cursor/3];int y=(cursor++%3)-1;var at=origin.offset(o[0],y,o[1]);if(!dryBank(l,at))continue;if(bird.distanceToSqr(at.getX()+.5,at.getY(),at.getZ()+.5)<=.36){bank=at;break;}paths++;var route=bird.getNavigation().createPath(at,0);if(route!=null&&route.canReach()){bank=at;bird.getNavigation().moveTo(route,.7);break;}}if(cursor>=24&&bank==null)left=0;return;}
  if(!dryBank(l,bank)){left=0;return;}
  if(bird.onGround()&&bird.distanceToSqr(bank.getX()+.5,bank.getY(),bank.getZ()+.5)<=.36){bird.getNavigation().stop();if(bird.beginCoil(l,quarry))left=0;else if(bird.tickCount%20==0){bank=null;if(retries++>=3)left=0;}}
  else if(bird.getNavigation().isDone()&&bird.tickCount%20==0&&retries++<3){var route=bird.getNavigation().createPath(bank,0);if(route!=null&&route.canReach())bird.getNavigation().moveTo(route,.7);else bank=null;}
 }
 private static boolean dryBank(ServerLevel l,BlockPos at){return BitternHabitat.bank(l,at)&&l.getFluidState(at).isEmpty();}
 public void stop(){bird.stalking(false);bird.getNavigation().stop();quarry=null;bank=null;origin=null;searchAt=bird.tickCount+100;}
}
