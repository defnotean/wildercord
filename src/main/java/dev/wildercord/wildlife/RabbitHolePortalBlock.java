package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Swirling dark burrow portal nestled inside the base of an ancient hollow tree (or waiting at
 * the destination exit). Entering or activating it transports the player to a random cave,
 * a stocked subterranean treasure vault, or all bosses in one go.
 */
public class RabbitHolePortalBlock extends Block implements EntityBlock {
	public RabbitHolePortalBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RabbitHolePortalBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp && !sp.isSpectator()) {
			if (server.getBlockEntity(pos) instanceof RabbitHolePortalBlockEntity portal) {
				portal.enter(server, sp);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			InsideBlockEffectApplier effects, boolean precise) {
		if (level instanceof ServerLevel server && entity instanceof ServerPlayer sp && !sp.isSpectator()) {
			if (server.getBlockEntity(pos) instanceof RabbitHolePortalBlockEntity portal) {
				portal.enter(server, sp);
			}
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 0.5;
		double cz = pos.getZ() + 0.5;
		for (int i = 0; i < 3; i++) {
			double ox = (random.nextDouble() - 0.5) * 0.7;
			double oy = (random.nextDouble() - 0.5) * 0.7;
			double oz = (random.nextDouble() - 0.5) * 0.7;
			level.addParticle(i == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL,
				cx + ox, cy + oy, cz + oz, -ox * 0.4, 0.08, -oz * 0.4);
		}
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.ENCHANT,
				cx + (random.nextDouble() - 0.5) * 0.6,
				cy + 0.5 + random.nextDouble() * 0.4,
				cz + (random.nextDouble() - 0.5) * 0.6,
				0.0, 0.05, 0.0);
		}
	}
}
