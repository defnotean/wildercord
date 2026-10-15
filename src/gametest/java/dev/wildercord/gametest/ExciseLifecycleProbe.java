package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.ExciseCasting;
import dev.wildercord.cast.ExciseState;
import net.minecraft.server.level.ServerPlayer;

/** Bounded, test-only evidence of cancellation versus serialization order; never changes the player's state. */
public final class ExciseLifecycleProbe {
	private ExciseLifecycleProbe() {}
	private static final java.util.concurrent.atomic.AtomicInteger RECORDS = new java.util.concurrent.atomic.AtomicInteger();

	public static void record(String phase, ServerPlayer player) {
		if (!player.hasAttached(ExciseState.REST) && !player.hasAttached(ExciseState.RECOVERY) && !ExciseCasting.pending(player)) return;
		if (RECORDS.getAndIncrement() >= 128) return;
		Wildercord.LOGGER.info("EXCISE_LIFECYCLE phase={} body={} tick={} pending={} recovery={} rest={} serverThread={} thread={}",
			phase, System.identityHashCode(player), ExciseCasting.now(player), ExciseCasting.pending(player),
			player.getAttachedOrElse(ExciseState.RECOVERY, 0L), player.getAttachedOrElse(ExciseState.REST, 0L),
			player.level().getServer().isSameThread(), Thread.currentThread().getName());
	}
}
