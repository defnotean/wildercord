package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
/** Finite physical backstep through reachable cells; no teleport, retaliation or pursuit of players. */
final class BitternRetreatGoal extends Goal {
 private final SiltcrestBittern bird;
 BitternRetreatGoal(SiltcrestBittern bird){this.bird=bird;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 public boolean canUse(){return bird.pose()==SiltcrestBittern.RETREATING;}public boolean canContinueToUse(){return canUse();}
 public void start(){var away=bird.getLookAngle().multiply(-1,0,-1).normalize();for(int k=0;k<2;k++){var p=BlockPos.containing(bird.position().add(away.scale(2+k)));if(!bird.level().hasChunkAt(p)||!bird.level().getBlockState(p.below()).isSolidRender()||!bird.level().getBlockState(p).getCollisionShape(bird.level(),p).isEmpty())continue;var route=bird.getNavigation().createPath(p,0);if(route!=null&&route.canReach()){bird.getNavigation().moveTo(route,1.1);return;}}}
 public void stop(){bird.getNavigation().stop();}
}
