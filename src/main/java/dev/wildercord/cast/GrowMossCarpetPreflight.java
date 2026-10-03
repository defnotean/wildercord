package dev.wildercord.cast;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Known single adjacent destination; no feature planning or rollback. */
final class GrowMossCarpetPreflight {
 private GrowMossCarpetPreflight(){}
 static boolean handles(BlockState source){return source.is(Blocks.PALE_MOSS_CARPET);}
 static boolean reserve(Cast cast,BlockPos base,BlockState source){
  if(!handles(source)||!cast.alive()||cast.caster.level()!=cast.level||!Casters.mayBuild(cast.caster))return false;
  var upper=base.above();var level=cast.level;
  if(!level.isLoaded(base)||!level.isLoaded(upper)||!level.isInsideBuildHeight(base)||!level.isInsideBuildHeight(upper)
   ||!level.getWorldBorder().isWithinBounds(base)||!level.getWorldBorder().isWithinBounds(upper))return false;
  if(!Casters.mayEdit(cast.caster,level,base)||!Casters.mayEdit(cast.caster,level,upper))return false;
  if(!cast.alive()||!level.getBlockState(base).equals(source))return false;
  if(!((MossyCarpetBlock)source.getBlock()).isValidBonemealTarget(level,base,source,BonemealSource.INTERACTION))return false;
  // performBonemeal writes upper only; reserve the actual single destination, not base again.
  return cast.takeBlocks(1);
 }
}
