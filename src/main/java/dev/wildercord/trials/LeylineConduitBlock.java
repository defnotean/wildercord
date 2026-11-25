package dev.wildercord.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Interactive pylon inside the Leyline Sanctum; party members dampen it during Bell Toll Conduit. */
public final class LeylineConduitBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public LeylineConduitBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		interactConduit(level, pos, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hit
	) {
		interactConduit(level, pos, player);
		return InteractionResult.SUCCESS;
	}

	public static boolean interactConduit(Level level, BlockPos pos, Player player) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
			return false;
		}
		if (!pos.closerToCenterThan(sp.position(), 5.5)) {
			return false;
		}
		for (VaelkhorBoss boss : server.getEntitiesOfClass(VaelkhorBoss.class, new AABB(pos).inflate(28.0))) {
			if (boss.tryDampenConduit(pos, sp)) {
				return true;
			}
		}
		sp.sendOverlayMessage(Component.translatable("message.wildercord.trial.conduit_idle"));
		return false;
	}
}
