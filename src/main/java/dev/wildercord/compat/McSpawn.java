package dev.wildercord.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * 1.21.8 has no {@code LevelData.RespawnData} or {@code ServerPlayer.RespawnConfig.respawnData()}:
 * a level's spawn point is its level data and a player's is the plain respawn config they carry.
 */
public final class McSpawn {
	/** Where a player wakes: the world's spawn or the bed they last slept in. */
	public record SpawnPoint(ResourceKey<Level> dimension, BlockPos pos, float angle, boolean forced) {}

	private McSpawn() {}

	/** The world's own spawn point, which compasses and bearings point at. */
	public static SpawnPoint ofLevel(ServerLevel level) {
		return new SpawnPoint(level.dimension(), level.getLevelData().getSpawnPos(), level.getLevelData().getSpawnAngle(), false);
	}

	/** The player's bed or respawn anchor, or null when they have none. */
	public static SpawnPoint ofPlayer(ServerPlayer player) {
		var home = dev.wildercord.compat.McSpawn.ofPlayer(player);
		return home == null ? null : new SpawnPoint(home.dimension(), home.pos(), home.angle(), home.forced());
	}

	/** The world's spawn, which is the overworld's spawn point on 1.21.8. */
	public static SpawnPoint ofServer(net.minecraft.server.MinecraftServer server) {
		return ofLevel(server.overworld());
	}

	/** The level a spawn teleport lands in: the overworld, where 1.21.8 keeps the world spawn. */
	public static ServerLevel spawnLevel(net.minecraft.server.MinecraftServer server) {
		return server.overworld();
	}
}
