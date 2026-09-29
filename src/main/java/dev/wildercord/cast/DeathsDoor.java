package dev.wildercord.cast;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The rest after cheating death. Reversal, Second Wind and a secret spell each turn a killing blow aside,
 * and each can be cast again long before it runs out; without a rest, a caster who kept one up would never
 * die. So once any of them has saved a creature, none of them saves it again for a minute (and the secret
 * one, stronger, can't be taken up again for three).
 */
public final class DeathsDoor {
	private DeathsDoor() {}

	/** After any save, nothing saves the same creature again for this long, in ticks. */
	public static final int REST = 1200;
	/** After the secret spell's save, it can't be cast on the same creature again for this long. */
	public static final int REBIRTH_REST = 3600;

	private static final Map<UUID, Long> SAVED = new HashMap<>();
	private static final Map<UUID, Long> REBORN = new HashMap<>();

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SAVED.clear();
			REBORN.clear();
		});
	}

	/** {@code entity} was just saved from death (by the secret spell when {@code rebirth}). */
	static void saved(Entity entity, boolean rebirth) {
		long now = entity.level().getGameTime();
		if (SAVED.size() > 256) {
			SAVED.values().removeIf(at -> now - at >= REST || at > now);
			REBORN.values().removeIf(at -> now - at >= REBIRTH_REST || at > now);
		}
		SAVED.put(entity.getUUID(), now);
		if (rebirth) {
			REBORN.put(entity.getUUID(), now);
		}
	}

	/** Seconds before anything may save {@code entity} again (0: it may now). */
	public static int resting(Entity entity) {
		return left(SAVED.get(entity.getUUID()), entity.level().getGameTime(), REST);
	}

	/** Seconds before the secret spell may be cast on {@code entity} again (0: it may now). */
	public static int restingFromRebirth(Entity entity) {
		long now = entity.level().getGameTime();
		return Math.max(left(REBORN.get(entity.getUUID()), now, REBIRTH_REST), left(SAVED.get(entity.getUUID()), now, REST));
	}

	static int left(Long savedAt, long now, int rest) {
		if (savedAt == null || savedAt > now || now - savedAt >= rest) {
			return 0;
		}
		return (int) Math.max(1, (rest - (now - savedAt) + 19) / 20);
	}
}
