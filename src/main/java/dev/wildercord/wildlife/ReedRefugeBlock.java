package dev.wildercord.wildlife;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.*;
/** A raised woven roof with four open sides. No block entity or ticking habitat simulation. */
public final class ReedRefugeBlock extends Block implements SimpleWaterloggedBlock {
 private static final VoxelShape SHAPE=Shapes.or(Block.box(0,12,0,16,16,16),Block.box(0,0,0,2,12,2),Block.box(14,0,0,16,12,2),Block.box(0,0,14,2,12,16),Block.box(14,0,14,16,12,16));
 public ReedRefugeBlock(Properties p) {super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(BlockStateProperties.WATERLOGGED);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c) {return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));}
 @Override protected FluidState getFluidState(BlockState s) {return s.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r) {if(s.getValue(BlockStateProperties.WATERLOGGED))t.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,t,p,d,np,ns,r);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return SHAPE;}
 @Override protected boolean isPathfindable(BlockState s,PathComputationType type) {// Amphibious navigation classifies LAND accessibility before testing the fluid.
  return type!=PathComputationType.AIR && s.getValue(BlockStateProperties.WATERLOGGED);}
}
