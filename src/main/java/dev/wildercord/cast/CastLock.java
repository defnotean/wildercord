package dev.wildercord.cast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A minimal "can't cast for a while" state: what Silence and Manaburn do to a caster. A player under it can't
 * cast or charge (and a charge in hand is cancelled); a Runebound's telegraphed cast is cancelled and its
 * next one delayed. Small seam on purpose: if a shared Silenced state lands elsewhere, {@link #lock},
 * {@link #locked} and {@link #interrupt} are the only three calls to redirect.
 */
public final class CastLock {
	private CastLock() {}

	private static final Map<UUID, Long> UNTIL = new HashMap<>();
	private record PlayerWindow(long until, long readyAt) {}
	private static final Map<ServerPlayer, PlayerWindow> PLAYER_WINDOWS = new java.util.WeakHashMap<>();
	/** Players get a two-second maximum cast lock followed by two seconds to respond. */
	public static final int PLAYER_LOCK_CAP = 40, PLAYER_RECOVERY = 40;

	/** Locks {@code who} out of casting for {@code ticks}, and cuts whatever they were casting short. */
	public static void lock(LivingEntity who, int ticks) {
		if (who instanceof dev.wildercord.aura.world.SwordMaster master && !master.acceptsInfluence(Effects.applying())) return;
		if (who instanceof ServerPlayer player) {
			long now = who.level().getGameTime();
			var previous = PLAYER_WINDOWS.get(player);
			if (previous != null && now < previous.readyAt) return;
			int duration = Math.clamp(ticks, 1, PLAYER_LOCK_CAP);
			PLAYER_WINDOWS.put(player, new PlayerWindow(now + duration, now + duration + PLAYER_RECOVERY));
			interrupt(who, duration);
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.control_lock"));
			return;
		}
		long until = who.level().getGameTime() + ticks;
		UNTIL.merge(who.getUUID(), until, Math::max);
		interrupt(who, ticks);
	}

	/**
	 * Dazes a player who let their own channel tear loose: no casting or charging for {@code ticks} (at
	 * most {@link #PLAYER_LOCK_CAP}). Quietly, since the backfire says what happened, and with no
	 * recovery window: it was their own doing, not a foe's seal, so it shouldn't shield them from one.
	 */
	public static void daze(ServerPlayer player, int ticks) {
		if (ticks <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		long until = now + Math.min(ticks, PLAYER_LOCK_CAP);
		var previous = PLAYER_WINDOWS.get(player);
		if (previous != null && previous.until >= until) {
			return;
		}
		PLAYER_WINDOWS.put(player, new PlayerWindow(until, previous == null ? until : Math.max(until, previous.readyAt)));
	}

	/** Whether {@code who} may not cast right now. */
	public static boolean locked(LivingEntity who) {
		if (who instanceof ServerPlayer player) {
			var window = PLAYER_WINDOWS.get(player);
			if (window == null) return false;
			long now = who.level().getGameTime();
			if (now >= window.readyAt) PLAYER_WINDOWS.remove(player);
			return now < window.until;
		}
		Long until = UNTIL.get(who.getUUID());
		if (until == null) {
			return false;
		}
		if (until < who.level().getGameTime()) {
			UNTIL.remove(who.getUUID(), until);
			return false;
		}
		return true;
	}

	/** Cancels a charge in hand or a telegraphed cast; a monster's next cast waits {@code delay} more ticks. */
	public static void interrupt(LivingEntity who, int delay) {
		if (who instanceof ServerPlayer player) {
			Charging.interrupt(player);
		} else if (who instanceof dev.wildercord.aura.world.SwordMaster) {
			Statuses.interrupt(who);
		} else if (who instanceof Mob mob) {
			Runebound.interrupt(mob, delay);
		}
	}

	static void clear() {
		UNTIL.clear();
		PLAYER_WINDOWS.clear();
	}
}
