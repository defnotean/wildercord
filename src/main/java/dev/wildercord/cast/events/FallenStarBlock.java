package dev.wildercord.cast.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Fallen Star: a lump of glowing starstone left in the crater where a star came down. It can't
 * be mined; once its guards are beaten, use it to break it open for the rune and the Mana Crystal
 * inside. It crumbles once looted (or, left alone, fades after twenty minutes), and the crater it
 * made fills back in.
 */
public class FallenStarBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 11, 14);

	public FallenStarBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			FallenStars.open(server, pos, serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FallenStarBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != EventContent.FALLEN_STAR_ENTITY ? null
			: (BlockEntityTicker<T>) (BlockEntityTicker<FallenStarBlockEntity>) FallenStarBlockEntity::serverTick;
	}
}
