package dev.wildercord.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Smoldering basalt slag vent native to Cinder Calderas; yields Caldera Slag Cores. */
public final class CalderaSlagVentBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

	public CalderaSlagVentBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMOKE,
				pos.getX() + 0.35 + random.nextDouble() * 0.3,
				pos.getY() + 0.92,
				pos.getZ() + 0.35 + random.nextDouble() * 0.3,
				0.0, 0.04, 0.0);
		}
		if (random.nextInt(6) == 0) {
			level.addParticle(ParticleTypes.FLAME,
				pos.getX() + 0.4 + random.nextDouble() * 0.2,
				pos.getY() + 0.92,
				pos.getZ() + 0.4 + random.nextDouble() * 0.2,
				0.0, 0.03, 0.0);
		}
	}
}
