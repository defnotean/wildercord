package dev.wildercord.world.cartographer;

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

public final class SurveyAstrolabeBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 3, 14), box(6, 3, 6, 10, 10, 10), box(3, 10, 3, 13, 16, 13));

	public SurveyAstrolabeBlock(BlockBehaviour.Properties properties) { super(properties); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SurveyAstrolabeEntity(pos, state); }

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof SurveyAstrolabeEntity astrolabe) return astrolabe.ring(serverPlayer);
		return InteractionResult.CONSUME;
	}
	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return useWithoutItem(state, level, pos, player, hit);
	}
}
