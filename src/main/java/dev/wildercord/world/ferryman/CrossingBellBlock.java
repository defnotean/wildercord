package dev.wildercord.world.ferryman;

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

public final class CrossingBellBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1,0,2,4,16,14), Block.box(12,0,2,15,16,14),
		Block.box(1,14,2,15,16,14), Block.box(5,5,4,11,13,12));
	public CrossingBellBlock(BlockBehaviour.Properties properties) { super(properties); }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrossingBellEntity(pos, state); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof CrossingBellEntity bell) bell.use(server);
		return InteractionResult.SUCCESS;
	}
	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return useWithoutItem(state, level, pos, player, hit);
	}
}
