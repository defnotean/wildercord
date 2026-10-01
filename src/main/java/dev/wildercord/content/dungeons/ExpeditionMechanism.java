package dev.wildercord.content.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Dungeon-only controls. Physical controls and spells share the same persisted puzzle state. */
public final class ExpeditionMechanism extends Block implements EntityBlock {
	public enum Kind { CLOCK, GARDEN, SKY }
	public final Kind kind;
	public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,4);
	public ExpeditionMechanism(Kind kind,Properties properties) { super(properties);this.kind=kind;registerDefaultState(defaultBlockState().setValue(FACING,Direction.SOUTH).setValue(STAGE,0)); }
	@Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ExpeditionMechanismEntity(p,s);}
	@Override @SuppressWarnings("unchecked") public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type) {
		return level.isClientSide()||type!=DungeonBlocks.MECHANISM_ENTITY?null:(BlockEntityTicker<T>)(BlockEntityTicker<ExpeditionMechanismEntity>)ExpeditionMechanismEntity::tick;
	}
	@Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
		if(level.getBlockEntity(pos) instanceof ExpeditionMechanismEntity mechanism)mechanism.use(player,ItemStack.EMPTY);
		return InteractionResult.SUCCESS;
	}
	@Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
		if(level.getBlockEntity(pos) instanceof ExpeditionMechanismEntity mechanism)mechanism.use(player,stack);
		return InteractionResult.SUCCESS;
	}
	@Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
	@Override protected BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,STAGE);}
}
