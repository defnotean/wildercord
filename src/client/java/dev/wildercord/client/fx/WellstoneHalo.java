package dev.wildercord.client.fx;

import dev.wildercord.content.WellstoneBlock;
import dev.wildercord.content.WellstoneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An awake Wellstone's halo: a ring of pale light hanging over the stone, leaning and slowly
 * swinging round, with three beads of light running along it. Drawn on the client for every awake
 * Wellstone in view (the circles on the ground come from the server).
 */
public final class WellstoneHalo {
	private WellstoneHalo() {}

	private static final int LIFE = 240;
	private static final double RANGE = 40;

	/** Awake Wellstones in view, and the tick each one's halo ends. */
	private static final Map<BlockPos, Integer> HALOS = new HashMap<>();
	private static List<BlockPos> awake = List.of();
	private static @Nullable ClientLevel seen;
	private static int clock;

	public static void tick(Minecraft mc) {
		clock++;
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level != seen) {
			seen = level;
			HALOS.clear();
			awake = List.of();
		}
		if (level == null || player == null) {
			return;
		}
		if (clock % 20 == 0) {
			awake = find(level, player);
			HALOS.keySet().retainAll(awake);
		}
		for (BlockPos pos : awake) {
			Integer ends = HALOS.get(pos);
			if (ends != null && clock < ends - 20) {
				continue;
			}
			Vec3 centre = Vec3.atBottomCenterOf(pos).add(0, 1.85, 0);
			mc.particleEngine.add(new RingGlow(level, centre, 0.34F, 0.02F, LIFE)
				.ring(0.62F, 0.07F, 0xE6DEFF, 0.8F)
				.ring(0.62F, 0.24F, 0xA890F0, 0.28F)
				.beads(3, 0.08F, 0xFFFFFF));
			HALOS.put(pos, clock + LIFE);
		}
	}

	/** Awake Wellstones in the loaded chunks near the player. */
	private static List<BlockPos> find(ClientLevel level, LocalPlayer player) {
		List<BlockPos> found = new ArrayList<>();
		int pcx = player.blockPosition().getX() >> 4;
		int pcz = player.blockPosition().getZ() >> 4;
		for (int cx = pcx - 3; cx <= pcx + 3; cx++) {
			for (int cz = pcz - 3; cz <= pcz + 3; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof WellstoneBlockEntity) || entity.getBlockPos().distToCenterSqr(player.position()) > RANGE * RANGE) {
						continue;
					}
					BlockState state = level.getBlockState(entity.getBlockPos());
					if (state.hasProperty(WellstoneBlock.ACTIVE) && state.getValue(WellstoneBlock.ACTIVE)) {
						found.add(entity.getBlockPos().immutable());
					}
				}
			}
		}
		return found;
	}
}
