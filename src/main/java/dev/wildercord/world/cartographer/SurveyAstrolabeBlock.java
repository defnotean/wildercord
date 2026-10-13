package dev.wildercord.world.cartographer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class SurveyAstrolabeBlock extends BaseEntityBlock {
	public static final MapCodec<SurveyAstrolabeBlock> CODEC = simpleCodec(SurveyAstrolabeBlock::new);
	private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 3, 14), box(6, 3, 6, 10, 10, 10), box(3, 10, 3, 13, 16, 13));

	public SurveyAstrolabeBlock(Properties properties) { super(properties); }
	@Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
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
