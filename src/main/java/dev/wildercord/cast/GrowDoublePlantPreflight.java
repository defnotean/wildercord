package dev.wildercord.cast;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Preflight for known adjacent vanilla bonemeal writes.
 * Preflights the exact two writes of vanilla SHORT_GRASS/FERN bonemeal only.
 * Other growth features retain their existing path; tree adjacency is not solved here.
 */
final class GrowDoublePlantPreflight {
 private GrowDoublePlantPreflight() {}
 static boolean handles(BlockState state){return state.is(Blocks.SHORT_GRASS)||state.is(Blocks.FERN);}
 static boolean reserve(Cast cast,BlockPos lower,BlockState source){
  if(!handles(source)||!cast.alive()||cast.caster.level()!=cast.level||!Casters.mayBuild(cast.caster))return false;
  var upper=lower.above();var level=cast.level;
  if(!level.isLoaded(lower)||!level.isLoaded(upper)||!level.isInsideBuildHeight(lower)||!level.isInsideBuildHeight(upper)
    ||!level.getWorldBorder().isWithinBounds(lower)||!level.getWorldBorder().isWithinBounds(upper))return false;
  // Both destinations are checked before reserving or allowing vanilla's first lower write.
  if(!Casters.mayEdit(cast.caster,level,lower)||!Casters.mayEdit(cast.caster,level,upper))return false;
  if(!cast.alive()||!level.getBlockState(lower).equals(source))return false;
  var grass=(TallGrassBlock)source.getBlock();
  if(!grass.isValidBonemealTarget(level,lower,source,BonemealSource.INTERACTION))return false;
  // Atomic shared Cast reservation: one remaining block cannot pay for half a plant.
  return cast.takeBlocks(2);
 }
}
