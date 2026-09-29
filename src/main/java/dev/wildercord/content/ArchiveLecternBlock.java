package dev.wildercord.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Archive Lectern, at the heart of the Archive: the Archivist rises from it the first time a
 * player comes near, and it goes quiet afterwards (unless the Archivist goes missing without falling:
 * see {@link ArchiveLecternBlockEntity}).
 */
public class ArchiveLecternBlock extends Block implements EntityBlock {
	public static final BooleanProperty AWAKE = BooleanProperty.create("awake");
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

	public ArchiveLecternBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AWAKE, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ArchiveLecternBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != WildercordBlocks.ARCHIVE_LECTERN_ENTITY ? null
			: (BlockEntityTicker<T>) (BlockEntityTicker<ArchiveLecternBlockEntity>) ArchiveLecternBlockEntity::serverTick;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AWAKE);
	}
}
