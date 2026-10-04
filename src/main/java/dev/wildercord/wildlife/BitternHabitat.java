package dev.wildercord.wildlife;
import net.minecraft.core.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
/** Physical feet, shallow water and overhead cover; bounded reads never generate chunks. */
final class BitternHabitat {
 private BitternHabitat(){}
 static boolean bank(LevelReader l,BlockPos p){if(!l.hasChunkAt(p)||!l.getBlockState(p.below()).isSolidRender()||!l.getBlockState(p.above()).getCollisionShape(l,p.above()).isEmpty()||!l.getFluidState(p.above()).isEmpty())return false;if(l.getFluidState(p).is(FluidTags.WATER))return l.getBlockState(p).getCollisionShape(l,p).isEmpty();if(!l.getBlockState(p).getCollisionShape(l,p).isEmpty()||!l.getFluidState(p).isEmpty())return false;for(var d:Direction.Plane.HORIZONTAL){var at=p.relative(d);if(l.hasChunkAt(at)&&(l.getFluidState(at).is(FluidTags.WATER)||l.getFluidState(at.below()).is(FluidTags.WATER)))return true;}return false;}
 /** Mud's actual top lies inside its block cell. Normalize only proven dry partial footing. */
 static boolean standingBank(SiltcrestBittern bird){
  var l=bird.level();var p=bird.blockPosition();if(bank(l,p))return true;
  if(!bird.onGround()||!l.hasChunkAt(p)||!l.getFluidState(p).isEmpty())return false;
  var shape=l.getBlockState(p).getCollisionShape(l,p);if(shape.isEmpty())return false;
  double top=shape.max(Direction.Axis.Y);if(top<=0||top>=1||Math.abs(bird.getY()-(p.getY()+top))>.03)return false;
  var body=bird.getBoundingBox();for(int ix=0;ix<2;ix++)for(int iz=0;iz<2;iz++)if(!l.hasChunkAt(BlockPos.containing(ix==0?body.minX:body.maxX,body.minY,iz==0?body.minZ:body.maxZ)))return false;
  return l.noCollision(bird,body)&&bank(l,p.above());
 }
 static boolean shelter(LevelReader l,BlockPos p){if(!bank(l,p)||!l.getFluidState(p).isEmpty()||!l.getBlockState(p).getCollisionShape(l,p).isEmpty())return false;var roof=p.above(2);return l.hasChunkAt(roof)&&!l.getBlockState(roof).getCollisionShape(l,roof).isEmpty();}
}
