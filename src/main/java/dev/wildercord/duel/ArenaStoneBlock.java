package dev.wildercord.duel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** An Arena Stone: use it to step up for a ranked spell duel, sneak-use it to read the season's ladder (see {@link Arena}). */
public class ArenaStoneBlock extends Block {
	public ArenaStoneBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			Arena.use(server, pos, serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}
}
