package dev.wildercord.compat;

import dev.wildercord.compat.McTime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 1.21.11 has no {@code dev.wildercord.compat.McTime.overworldClock(Level)}: the overworld's clock is read from the server's
 * overworld directly. Everywhere else the level's own day time is the clock.
 */
public final class McTime {
	private McTime() {}

	/** The overworld's day time, which the mod's day/night and moon-phase rules all key off. */
	public static long overworldClock(Level level) {
		if (level instanceof ServerLevel server) {
			ServerLevel overworld = server.getServer() == null ? null : server.getServer().overworld();
			if (overworld != null) {
				return overworld.getDayTime();
			}
		}
		return level.getDayTime();
	}
}
