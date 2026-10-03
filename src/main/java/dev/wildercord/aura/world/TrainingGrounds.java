package dev.wildercord.aura.world;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraExperience;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Bounded habitat sampling, only while actually breathing. No chunk loads; samples expire after two seconds or movement. */
public final class TrainingGrounds {
	private TrainingGrounds() {}

	private record Sample(ServerLevel level, BlockPos feet, long at, TrainingRules.Ground ground) {}
	private static final Map<ServerPlayer, Sample> SAMPLES = new WeakHashMap<>();

	public static void broken(ServerPlayer player) { SAMPLES.remove(player); }

	public static TrainingRules.Ground ground(ServerPlayer player) {
		if (!Config.get().auraWorld().trainingGrounds() || !player.onGround() || player.isInWater() || player.isInLava())
			return TrainingRules.Ground.NONE;
		var old = SAMPLES.get(player);
		long now = player.level().getGameTime();
		if (old != null && old.level() == player.level() && old.feet().equals(player.blockPosition())
			&& now >= old.at() && now - old.at() < TrainingRules.CHECK_TICKS) return old.ground();
		var ground = inspect(player);
		SAMPLES.put(player, new Sample(player.level(), player.blockPosition(), now, ground));
		return ground;
	}

	private static TrainingRules.Ground inspect(ServerPlayer player) {
		if (!Config.get().auraWorld().trainingGrounds() || !player.onGround() || player.isInWater() || player.isInLava())
			return TrainingRules.Ground.NONE;
		var level = player.level();
		BlockPos feet = player.blockPosition();
		// A six-block falling column close to the dry bank; a source pool never qualifies.
		for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
			if (x * x + z * z > 9) continue;
			BlockPos base = feet.offset(x, 0, z);
			if (!level.hasChunkAt(base)) continue;
			int falling = 0;
			for (int y = 0; y < TrainingRules.WATERFALL_HEIGHT; y++) {
				var water = level.getFluidState(base.above(y));
				if (!water.is(FluidTags.WATER) || !water.getValue(FlowingFluid.FALLING)) break;
				falling++;
			}
			if (TrainingRules.waterfall(falling, true, true, true)) return TrainingRules.Ground.WATERFALL;
		}
		if (!level.getBiome(feet).is(BiomeTags.IS_MOUNTAIN) || !level.canSeeSky(feet.above())) return TrainingRules.Ground.NONE;
		int[] heights = new int[8];
		int i = 0;
		for (int radius : new int[]{6, 12}) for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			BlockPos sample = feet.offset(direction[0] * radius, 0, direction[1] * radius);
			if (!level.hasChunkAt(sample)) return TrainingRules.Ground.NONE;
			heights[i++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample.getX(), sample.getZ());
		}
		return TrainingRules.summit(feet.getY(), level.getSeaLevel(), true, true, true, heights)
			? TrainingRules.Ground.SUMMIT : TrainingRules.Ground.NONE;
	}

	/** Ordinary breathing recovery plus a bonus and bounded practice; called once per full breath. */
	public static void breathe(ServerPlayer player) {
		var ground = ground(player);
		if (ground == TrainingRules.Ground.NONE) return;
		var settings = Config.get().auraWorld();
		Aura.gain(player, AuraRules.BREATH_GAIN * (AuraRules.BREATH_PERIOD / 20.0) * (settings.trainingGain() - 1), "training_ground");
		// earn(practice=true) applies PRACTICE_RATE and the shared PRACTICE_CAP; compensate only that rate.
		AuraExperience.earn(player, settings.trainingPractice() / AuraRules.PRACTICE_RATE, true);
		if (player.level().getGameTime() % 200 < TrainingRules.CHECK_TICKS) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.training_" + ground.name().toLowerCase(java.util.Locale.ROOT))
				.withColor(0xFF000000 | Aura.color(player)));
		}
	}
}
