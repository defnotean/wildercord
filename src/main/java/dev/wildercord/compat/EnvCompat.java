package dev.wildercord.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 1.21.10 has no environment attributes (they arrive with 1.21.11): the two the casts ask about are
 * worked out from what the version does have. Water "evaporates" where the biome's base temperature is
 * desert-hot, and the moon phase comes from the day time the way vanilla's own getMoonPhase computes it.
 */
public final class EnvCompat {
	private EnvCompat() {}

	public static boolean waterEvaporates(Level level, BlockPos pos) {
		return level.getBiome(pos).value().getBaseTemperature() >= 1.0F;
	}

	/** The moon phase (0..7) at the given place; the phase is world-wide, so the position is only kept for call shape. */
	public static int moonPhase(Level level, BlockPos pos) {
		return (int) ((level.getDayTime() / 24000L + 180L) % 24000L / 24000L * 8.0);
	}
}
