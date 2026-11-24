package dev.wildercord.ocean;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Centerpiece Sovereign Awakening Altar in the Abyssal Trench Citadel Colosseum. Tracks boss state
 * and automatically respawns Thalassor after defeat, or lets players awaken it early with pearls.
 */
public final class SovereignAltarBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

	public SovereignAltarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean wet = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
		return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, wet);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		Direction dir,
		BlockState neighborState,
		net.minecraft.world.level.LevelAccessor level,
		BlockPos pos,
		BlockPos neighborPos
	) {
		if (state.getValue(BlockStateProperties.WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SovereignAltarBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != OceanBlocks.SOVEREIGN_ALTAR_ENTITY
			? null
			: (BlockEntityTicker<T>) (BlockEntityTicker<SovereignAltarBlockEntity>) SovereignAltarBlockEntity::serverTick;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof SovereignAltarBlockEntity altar) {
			return altar.interact(player, ItemStack.EMPTY);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hit
	) {
		if (level.getBlockEntity(pos) instanceof SovereignAltarBlockEntity altar) {
			return altar.interact(player, stack);
		}
		return InteractionResult.SUCCESS;
	}
}
