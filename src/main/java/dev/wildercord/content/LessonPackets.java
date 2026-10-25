package dev.wildercord.content;

import dev.wildercord.net.PacketThrottle;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * One allowance shared by the four lesson books' page turns and reopenings (Relay, Reweave, Excise, Pack). Each
 * reopening reads the traveller's studies and sends the book back, so a modified client flooding them is cut off
 * past a burst no reader comes near; closing a book is never held back.
 */
public final class LessonPackets {
	/** Packets a reader may send at once. */
	public static final int BURST = 8;
	/** Ticks for one more to come back. */
	public static final int TICKS_PER_PACKET = 5;

	private static final PacketThrottle THROTTLE = new PacketThrottle(BURST, TICKS_PER_PACKET);

	private LessonPackets() {
	}

	public static void init() {
		dev.wildercord.net.ServerDisconnects.register((handler, server) -> THROTTLE.forget(handler.player.getUUID()));
	}

	/** Whether {@code player} may turn or reopen a lesson now (and counts it if so). */
	public static boolean admit(ServerPlayer player) {
		return allow(player.getUUID(), player.level().getServer().getTickCount());
	}

	/** Whether {@code player} has any allowance left, without spending it. */
	public static boolean hasAllowance(ServerPlayer player) {
		return THROTTLE.wouldAllow(player.getUUID(), player.level().getServer().getTickCount());
	}

	static boolean allow(UUID player, long tick) {
		return THROTTLE.allow(player, tick);
	}
}
