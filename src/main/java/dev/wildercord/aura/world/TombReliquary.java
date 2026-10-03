package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** The buried keeper's challenge and finite reward. Authentic provenance lives in the entity. */
public final class TombReliquary extends Block implements EntityBlock {
	public static final IntegerProperty STATE=IntegerProperty.create("state",0,2);
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
	public TombReliquary(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(STATE,0).setValue(FACING,net.minecraft.core.Direction.NORTH));}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(STATE,FACING);}
	@Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TombReliquaryEntity(p,s);}
	@Override @SuppressWarnings("unchecked") public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level l,BlockState s,BlockEntityType<T> t){
		return l.isClientSide() || t!=SwordTombs.RELIQUARY_ENTITY?null:(BlockEntityTicker<T>)(BlockEntityTicker<TombReliquaryEntity>)TombReliquaryEntity::tick;
	}
}
