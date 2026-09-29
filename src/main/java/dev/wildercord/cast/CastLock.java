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

	/** Locks {@code who} out of casting for {@code ticks}, and cuts whatever they were casting short. */
	public static void lock(LivingEntity who, int ticks) {
		long until = who.level().getGameTime() + ticks;
		UNTIL.merge(who.getUUID(), until, Math::max);
		interrupt(who, ticks);
	}

	/** Whether {@code who} may not cast right now. */
	public static boolean locked(LivingEntity who) {
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
		} else if (who instanceof Mob mob) {
			Runebound.interrupt(mob, delay);
		}
	}

	static void clear() {
		UNTIL.clear();
	}
}
