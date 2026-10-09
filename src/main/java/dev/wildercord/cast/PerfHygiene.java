package dev.wildercord.cast;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Drops what spells kept about a player once they leave (see docs/testing/performance.md). */
public final class PerfHygiene {
	private PerfHygiene() {}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			WayfarerEffects.forget(handler.player.getUUID(), server.overworld().getGameTime()));
	}
}
