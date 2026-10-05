package dev.wildercord.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.function.BooleanSupplier;

/** One native player hit's wound, scoped only until that same hit's AFTER_DAMAGE callbacks finish. */
public final class ManaSkinDamage {
	private ManaSkinDamage() {}
	private static final ThreadLocal<Pending> CURRENT = new ThreadLocal<>();

	public record Wound(float healthBefore, float healthAfter) {}

	private static final class Pending {
		final ServerPlayer player;
		final DamageSource source;
		Wound wound;
		boolean observing, consumed;
		Pending(ServerPlayer player, DamageSource source) { this.player = player; this.source = source; }
	}

	/** Each reentrant hurtServer call gets its own scope, including rejected and same-source hits. */
	public static boolean during(Player player, DamageSource source, BooleanSupplier hurt) {
		if (!(player instanceof ServerPlayer serverPlayer)) return hurt.getAsBoolean();
		Pending previous = CURRENT.get();
		CURRENT.set(new Pending(serverPlayer, source));
		try {
			return hurt.getAsBoolean();
		} finally {
			if (previous == null) CURRENT.remove();
			else CURRENT.set(previous);
		}
	}

	public static final class Observation {
		private final Pending pending;
		private final float health;
		private Observation(Pending pending) { this.pending = pending; health = pending.player.getHealth(); }
	}

	/** Observe only the exact source/target currently inside its own hurtServer invocation. */
	public static Observation begin(Player player, DamageSource source) {
		Pending pending = CURRENT.get();
		if (pending == null || pending.player != player || pending.source != source || pending.observing
				|| pending.wound != null || pending.consumed) return null;
		pending.observing = true;
		return new Observation(pending);
	}

	/** Called only after native damage successfully returns, before death saves or any healing callback. */
	public static void finish(Observation observation) {
		if (observation == null) return;
		Pending pending = observation.pending;
		pending.observing = false;
		pending.wound = new Wound(observation.health, pending.player.getHealth());
	}

	/** One-shot consumption at HeartCircles' existing callback position; never reads a previous hit. */
	public static Wound take(ServerPlayer player, DamageSource source) {
		Pending pending = CURRENT.get();
		if (pending == null || pending.player != player || pending.source != source || pending.consumed) return null;
		pending.consumed = true;
		return pending.wound;
	}
}
