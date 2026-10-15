package dev.wildercord.world.kilnwidow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class KilnHeartstoneBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 4, 14), box(4, 4, 4, 12, 11, 12), box(3, 11, 3, 13, 16, 13));

	public KilnHeartstoneBlock(BlockBehaviour.Properties properties) { super(properties); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new KilnHeartstoneEntity(pos, state); }

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof KilnHeartstoneEntity heartstone) return heartstone.stoke(serverPlayer);
		return InteractionResult.CONSUME;
	}
	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return useWithoutItem(state, level, pos, player, hit);
	}
}
