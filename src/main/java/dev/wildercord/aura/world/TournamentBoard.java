package dev.wildercord.aura.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** A visible registration stand; event and reward data live in its block entity. */
public final class TournamentBoard extends Block implements EntityBlock {
	public TournamentBoard(Properties p){super(p);}
	@Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TournamentBoardEntity(p,s);}
	@Override @SuppressWarnings("unchecked") public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide() || t!=VillageTournaments.BOARD_ENTITY?null:(BlockEntityTicker<T>)(BlockEntityTicker<TournamentBoardEntity>)TournamentBoardEntity::tick;}
}
