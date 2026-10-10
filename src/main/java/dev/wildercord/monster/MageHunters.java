package dev.wildercord.monster;

import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Mage-hunters coming for casters by night. See {@link MageHunterRules}. */
public final class MageHunters {
	private MageHunters() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % MageHunterRules.CHECK_TICKS != 0) return;
			ServerLevel level = server.overworld();
			if (level.getDifficulty() == Difficulty.PEACEFUL || level.isBrightOutside()) return;
			RandomSource random = level.getRandom();
			for (ServerPlayer player : level.players()) {
				if (player.isAlive() && !player.isSpectator() && !player.isCreative() && MageHunterRules.hunted(Heart.circles(player))
					&& random.nextDouble() < MageHunterRules.CHANCE) {
					send(level, player);
				}
			}
		});
	}

	/** Sends a band after {@code player}; returns the hunters, empty with nowhere to stand or a hunter already close. */
	public static List<MageHunter> send(ServerLevel level, ServerPlayer player) {
		List<MageHunter> band = new ArrayList<>();
		if (!level.getEntitiesOfClass(MageHunter.class, player.getBoundingBox().inflate(MageHunterRules.APART)).isEmpty()) return band;
		RandomSource random = level.getRandom();
		int count = MageHunterRules.band(Heart.circles(player), random.nextDouble());
		for (int attempt = 0; attempt < 10 && band.isEmpty(); attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = MageHunterRules.SPAWN_NEAR + random.nextDouble() * (MageHunterRules.SPAWN_FAR - MageHunterRules.SPAWN_NEAR);
			BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
			if (!level.isPositionEntityTicking(column)) continue;
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) continue;
			for (int i = 0; i < count; i++) {
				MageHunter hunter = MonsterContent.MAGE_HUNTER.create(level, EntitySpawnReason.EVENT);
				if (hunter == null) break;
				hunter.snapTo(ground.getX() + 0.5 + i * 1.5, ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360, 0);
				if (!level.noCollision(hunter)) hunter.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0, 0);
				if (!level.noCollision(hunter)) continue;
				hunter.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
				hunter.setTarget(player);
				level.addFreshEntity(hunter);
				band.add(hunter);
			}
		}
		if (!band.isEmpty()) {
			player.sendSystemMessage(Component.translatable(band.size() > 1 ? "message.wildercord.mage_hunter.band" : "message.wildercord.mage_hunter.one")
				.withStyle(ChatFormatting.DARK_RED));
		}
		return band;
	}
}
