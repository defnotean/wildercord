package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four authored lift poses, followed by the empty stone. Only active draws schedule ticks. */
public final class SleepingBladeStone extends Block implements EntityBlock {
	public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 4);
	public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
	public SleepingBladeStone(Properties p) {
		super(p); registerDefaultState(defaultBlockState().setValue(PHASE, 0).setValue(FACING, Direction.NORTH));
	}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PHASE, FACING); }
	@Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new SleepingBladeEntity(p, s); }
	@Override protected VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter level, BlockPos p, CollisionContext c) {
		return s.getValue(PHASE) == 4 ? Block.box(1, 0, 1, 15, 10, 15) : Block.box(1, 0, 1, 15, 16, 15);
	}
	@Override protected void tick(BlockState s, ServerLevel level, BlockPos p, RandomSource random) {
		if (!(level.getBlockEntity(p) instanceof SleepingBladeEntity stone)) return;
		if (stone.claimedBy() != null) stone.phase(4);
		else if (!SleepingBlades.drawingAt(level, p)) stone.phase(0);
		else level.scheduleTick(p, this, 20);
	}
}
