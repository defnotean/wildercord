package dev.wildercord.aura;

import dev.wildercord.cast.Scheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Test-only replay of idle recovery between presentation captures. Instead of idling out each art's rest in real time,
 * the capture waits only until the previous art has fully finished, then ends the owner's server rests at once.
 */
public final class MastersCaptureRest {
	private MastersCaptureRest() {}

	/** Nothing of the previous art is still running: no commitment, no scheduled part, no stray entity, no flame. */
	public static String busy(ServerPlayer player, AABB stage) {
		if (MastersArts.committed(player)) return "committed";
		if (Scheduler.pending() > 0) return "scheduled=" + Scheduler.pending();
		if (player.isOnFire()) return "burning";
		// Drops and orbs are inert and outlive any idle the capture ever took; they never held a capture back.
		var stray = player.level().getEntities(player, stage, entity -> !(entity instanceof Player
			|| entity instanceof net.minecraft.world.entity.item.ItemEntity || entity instanceof net.minecraft.world.entity.ExperienceOrb));
		return stray.isEmpty() ? null : "entities=" + stray;
	}

	/** Ends the move and art rests, the server's swing ledger and request burst, exactly as a long idle would. */
	public static void restNow(ServerPlayer player) {
		if (MastersArts.committed(player)) throw new AssertionError("Rests are only ended after the previous art finished");
		MastersArts.endRests(player);
		player.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		SwordStrings.forget(player.getUUID());
	}
}
