package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Fixed small supported canopy snapshot; no scans generate chunks or edit a garden. */
final class MossveilHome{
 record Home(ServerLevel world,BlockPos canopy,Map<BlockPos,BlockState> cells){
  boolean current(){for(var e:cells.entrySet())if(!world.hasChunkAt(e.getKey())||world.getBlockState(e.getKey())!=e.getValue())return false;return supported(world,canopy);}
  boolean allowed(ServerPlayer p){for(var at:cells.keySet())if(!TidewardReadAdmission.allows(p,world,at))return false;return p.level()==world&&current();}
 }
 static boolean supported(ServerLevel l,BlockPos canopy){
  var feet=canopy.below();var floor=canopy.below(2);
  return l.hasChunkAt(canopy)&&l.hasChunkAt(feet)&&l.hasChunkAt(floor)&&l.getBlockState(canopy).is(FungalGarden.NURSERY)
   &&l.getBlockState(feet).getCollisionShape(l,feet).isEmpty()&&l.getFluidState(feet).isEmpty()
   &&l.getBlockState(floor).isCollisionShapeFullBlock(l,floor)&&l.getFluidState(floor).isEmpty();
 }
 static Home find(ServerLevel l,BlockPos center){
  // At most196 loaded candidate cells, only when beginning preparation; no per-tick cube scan.
  for(var at:BlockPos.betweenClosed(center.offset(-3,1,-3),center.offset(3,4,3)))if(supported(l,at)){
   var copy=at.immutable();var cells=new LinkedHashMap<BlockPos,BlockState>();for(var p:List.of(copy,copy.below(),copy.below(2)))cells.put(p,l.getBlockState(p));return new Home(l,copy,Collections.unmodifiableMap(cells));
  }return null;
 }
 /** Complete bounded search chooses an actually jointly usable home, rather than starving behind an earlier distant site. */
 static Home find(ServerLevel l,BlockPos center,net.minecraft.world.phys.Vec3 payer,net.minecraft.world.phys.Vec3 pet){
  if(payer==null||pet==null||!Double.isFinite(payer.lengthSqr())||!Double.isFinite(pet.lengthSqr()))return null;
  BlockPos best=null;double bestScore=Double.POSITIVE_INFINITY;
  for(var at:BlockPos.betweenClosed(center.offset(-3,1,-3),center.offset(3,4,3)))if(supported(l,at)){
   var anchor=net.minecraft.world.phys.Vec3.atCenterOf(at);double payerDistance=anchor.distanceToSqr(payer),petDistance=anchor.distanceToSqr(pet);
   if(payerDistance>9||petDistance>9)continue;double score=payerDistance+petDistance;
   if(score<bestScore){best=at.immutable();bestScore=score;}
  }
  if(best==null)return null;var cells=new LinkedHashMap<BlockPos,BlockState>();for(var p:List.of(best,best.below(),best.below(2)))cells.put(p,l.getBlockState(p));return new Home(l,best,Collections.unmodifiableMap(cells));
 }
 static boolean natural(ServerLevel l,BlockPos feet){
  if(!l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||l.isOutsideBuildHeight(feet)||l.isOutsideBuildHeight(feet.above())||!l.getWorldBorder().isWithinBounds(feet)||!l.hasChunkAt(feet)||!l.hasChunkAt(feet.below())||!l.getFluidState(feet).isEmpty()||!l.getFluidState(feet.above()).isEmpty()
   ||!l.getBlockState(feet).getCollisionShape(l,feet).isEmpty()||!l.getBlockState(feet.above()).getCollisionShape(l,feet.above()).isEmpty()
   ||!(l.getBlockState(feet.below()).is(Blocks.MOSS_BLOCK)||l.getBlockState(feet.below()).is(Blocks.CLAY))||l.getMaxLocalRawBrightness(feet)>8)return false;
  boolean cover=false,mature=false;
  for(int i=1;i<=4;i++){var at=feet.above(i);if(l.hasChunkAt(at)&&(l.getBlockState(at).isSolidRender()||l.getBlockState(at).is(FungalGarden.NURSERY)))cover=true;}
  for(var at:BlockPos.betweenClosed(feet.offset(-3,-1,-3),feet.offset(3,1,3)))if(l.hasChunkAt(at)){var state=l.getBlockState(at);if(state.is(FungalGarden.GLOWCAP)&&state.getValue(GlowcapBlock.AGE)==2&&GlowcapBlock.conditions(l,at))mature=true;}
  return cover&&mature;
 }
}
