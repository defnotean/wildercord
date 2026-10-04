package dev.wildercord.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
/** Re-find a local dry bank after ordinary roaming. Never follows a committed fish or changes a strike. */
final class BitternBankReturnGoal extends Goal {
 private final SiltcrestBittern bird;private int next,left,scanAt,cursor;private BlockPos origin,bank;
 private static final int WIDTH=13,CELLS=WIDTH*WIDTH*3,READS=48;
 BitternBankReturnGoal(SiltcrestBittern bird){this.bird=bird;setFlags(EnumSet.of(Flag.MOVE));}
 public boolean canUse(){return bird.tickCount>=next&&bird.forageReady()&&!bird.isInWater()&&bird.onGround()&&!BitternHabitat.standingBank(bird)&&!bird.disturbed((ServerLevel)bird.level());}
 public void start(){origin=bird.blockPosition();left=SiltcrestBittern.JOURNEY;cursor=0;bank=null;scanAt=bird.tickCount;}
 public boolean canContinueToUse(){return left>0&&bird.forageReady()&&!bird.isInWater()&&!BitternHabitat.standingBank(bird);}
 public boolean requiresUpdateEveryTick(){return true;}
 public void tick(){
  left--;var l=(ServerLevel)bird.level();if(bird.disturbed(l)){left=0;return;}
  if(bank!=null){if(!dry(l,bank)){bank=null;bird.getNavigation().stop();}else if(bird.getNavigation().isDone()){left=0;return;}else return;}
  if(bird.tickCount<scanAt)return;scanAt=bird.tickCount+20;int reads=0,paths=0;
  // Fixed local window, retained cursor, <=48 habitat candidates and <=2 native path requests per20ticks.
  while(cursor<CELLS&&reads<READS&&paths<2){int index=cursor++;int y=index%3-1;int column=index/3;int x=column%WIDTH-6,z=column/WIDTH-6;var at=origin.offset(x,y,z);reads++;
   if(!dry(l,at)||!l.hasChunkAt(at.above()))continue;
   var box=bird.getBoundingBox().move(at.getX()+.5-bird.getX(),at.getY()-bird.getY(),at.getZ()+.5-bird.getZ());boolean loaded=true;for(int ix=0;ix<2;ix++)for(int iz=0;iz<2;iz++)loaded&=l.hasChunkAt(BlockPos.containing(ix==0?box.minX:box.maxX,box.minY,iz==0?box.minZ:box.maxZ));if(!loaded||!l.noCollision(bird,box))continue;
   paths++;var route=bird.getNavigation().createPath(at,0);if(route!=null&&route.canReach()){bank=at;bird.getNavigation().moveTo(route,.7);return;}
  }
  if(cursor>=CELLS)left=0;
 }
 private static boolean dry(ServerLevel l,BlockPos at){return l.hasChunkAt(at)&&BitternHabitat.bank(l,at)&&l.getFluidState(at).isEmpty();}
 public void stop(){bird.getNavigation().stop();origin=null;bank=null;left=0;next=bird.tickCount+100;}
}
