package dev.wildercord.cast;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Preflight for the exact vanilla underwater two-cell mutation.
 * Checks destinations/reserves the pair, then leaves vanilla placement and fluid physics intact.
 */
final class GrowSeagrassPreflight {
 private GrowSeagrassPreflight(){}
 static boolean handles(BlockState state){return state.is(Blocks.SEAGRASS);}
 static boolean reserve(Cast cast,BlockPos lower,BlockState source){
  if(!handles(source)||!cast.alive()||cast.caster.level()!=cast.level||!Casters.mayBuild(cast.caster))return false;
  var upper=lower.above();var level=cast.level;
  if(!level.isLoaded(lower)||!level.isLoaded(upper)||!(lower.getY() >= level.getMinBuildHeight() && lower.getY() <= level.getMaxBuildHeight())||!(upper.getY() >= level.getMinBuildHeight() && upper.getY() <= level.getMaxBuildHeight())
   ||!level.getWorldBorder().isWithinBounds(lower)||!level.getWorldBorder().isWithinBounds(upper))return false;
  if(!Casters.mayEdit(cast.caster,level,lower)||!Casters.mayEdit(cast.caster,level,upper))return false;
  if(!cast.alive()||!level.getBlockState(lower).equals(source))return false;
  if(!dev.wildercord.compat.McBlocksAndItems.isValidBonemealTarget((SeagrassBlock)source.getBlock(),level,lower,source))return false;
  return cast.takeBlocks(2);
 }
}
