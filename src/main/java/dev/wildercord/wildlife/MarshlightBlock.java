package dev.wildercord.wildlife;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.util.RandomSource;

/** A pearl in a reed cage: native constant illumination, usable on a submerged bank. */
public final class MarshlightBlock extends Block implements SimpleWaterloggedBlock {
	public MarshlightBlock(Properties p) {super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED,false));}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(BlockStateProperties.WATERLOGGED);}
	@Override public BlockState getStateForPlacement(BlockPlaceContext c) {return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));}
	@Override protected FluidState getFluidState(BlockState s) {return s.getValue(BlockStateProperties.WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
	@Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r) {if(s.getValue(BlockStateProperties.WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,l,ticks,p,d,np,ns,r);}
	@Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return Block.box(3,0,3,13,14,13);}
}
