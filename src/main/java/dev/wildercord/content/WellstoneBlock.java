package dev.wildercord.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The Wellstone: set it on a ley line and it wakes, drawing the line's mana up into a slowly
 * turning circle. Everyone within 12 blocks of an awake Wellstone regenerates mana half again
 * as fast. Off a ley line it only glows faintly.
 */
public class WellstoneBlock extends Block implements EntityBlock {
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public WellstoneBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WellstoneBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != WildercordBlocks.WELLSTONE_ENTITY ? null
			: (BlockEntityTicker<T>) (BlockEntityTicker<WellstoneBlockEntity>) WellstoneBlockEntity::serverTick;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}
}
