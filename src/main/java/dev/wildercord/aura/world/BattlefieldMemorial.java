package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A weathered marker with buried crossed steel. Only generated markers retain a memory. */
public final class BattlefieldMemorial extends Block implements EntityBlock {
	public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 2);
	public BattlefieldMemorial(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(KIND,0)); }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(KIND); }
	@Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new BattlefieldMemoryEntity(pos,state); }
	@Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context) {
		return Block.box(2,0,2,14,14,14);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
		if (player instanceof net.minecraft.server.level.ServerPlayer p) Battlefields.begin(p,pos);
		return InteractionResult.SUCCESS;
	}
	@Override protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,
		Player player,net.minecraft.world.InteractionHand hand,BlockHitResult hit) { return useWithoutItem(state,level,pos,player,hit); }
}
