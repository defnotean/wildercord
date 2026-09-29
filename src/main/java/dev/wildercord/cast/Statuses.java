package dev.wildercord.cast;

import dev.wildercord.cast.feel.MarkHalos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small reusable seams shared by any rune, so no rune builds a private version:
 * <ul>
 *   <li><b>Silenced</b>: {@link #silence}, {@link #silenced}. A silenced player can't cast (a tap, or a charge
 *       begun), a silenced Runebound's telegraphed cast is cancelled and it can't start another. Ends by itself.</li>
 *   <li><b>Airborne</b>: {@link #airborne}. A creature marked airborne takes {@link #AIRBORNE_BONUS} times the damage
 *       of any spell while it is off the ground (applied in {@link Reactions#hit}, so every element gets it). It ends
 *       when the mark runs out or the creature lands.</li>
 *   <li><b>Wind mass</b>: {@link #windMass}, {@link #windPush}. Wind moves light creatures farther and heavy ones less.</li>
 *   <li><b>Interrupt</b>: {@link #interrupt}. Breaks what a creature is winding up: a player's charge, a bow draw,
 *       a creeper's fuse, a Runebound's telegraph (at most once every {@link #INTERRUPT_GAP} ticks per creature).</li>
 * </ul>
 */
public final class Statuses {
	private Statuses() {}

	/** Damage multiplier from any spell on a creature marked airborne that's off the ground. */
	public static final double AIRBORNE_BONUS = 1.2;
	/** The soonest the same creature can be interrupted again (8 seconds). */
	public static final int INTERRUPT_GAP = 160;

	private static final Map<UUID, Long> SILENCED = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> INTERRUPTED = new ConcurrentHashMap<>();

	// ------------------------------------------------------------------ silenced

	/** Silences a creature for {@code ticks}; a longer silence already on it stays. */
	public static void silence(LivingEntity target, int ticks) {
		boolean fresh = !silenced(target);
		long until = target.level().getGameTime() + Math.max(1, ticks);
		SILENCED.merge(target.getUUID(), until, Math::max);
		if (fresh && target.level() instanceof ServerLevel level) {
			StatusVfx.silenced(level, target);
			// A crown halo every halo period while it lasts, so the state can be read on the creature (at most 16 beats).
			for (int beat = MarkHalos.PERIOD; beat <= Math.min(ticks, 160); beat += MarkHalos.PERIOD) {
				Scheduler.later(beat, () -> {
					if (target.isAlive() && silenced(target)) {
						StatusVfx.silencedBeat(level, target);
					}
				});
			}
		}
		if (target instanceof ServerPlayer player) {
			Charging.interrupt(player);
		} else if (target instanceof Mob mob) {
			Runebound.interrupt(mob);
		}
	}

	/** Whether {@code entity} can't cast right now. */
	public static boolean silenced(Entity entity) {
		Long until = SILENCED.get(entity.getUUID());
		if (until == null) {
			return false;
		}
		if (until < entity.level().getGameTime()) {
			SILENCED.remove(entity.getUUID(), until);
			return false;
		}
		return true;
	}

	// ------------------------------------------------------------------ airborne

	/** Marks {@code target} airborne for up to {@code ticks} (see {@link #AIRBORNE_BONUS}). */
	public static void airborne(Entity target, int ticks) {
		boolean fresh = !Reactions.has(target, Reactions.Mark.AIRBORNE);
		Reactions.mark(target, Reactions.Mark.AIRBORNE, ticks);
		if (fresh && target.level() instanceof ServerLevel level) {
			StatusVfx.lifted(level, target);
		}
	}

	/** The airborne factor for a hit on {@code target}: {@link #AIRBORNE_BONUS} while marked and off the ground, else 1. */
	public static double airborneFactor(Entity target) {
		return Reactions.has(target, Reactions.Mark.AIRBORNE) && !target.onGround() ? AIRBORNE_BONUS : 1.0;
	}

	// ------------------------------------------------------------------ wind mass

	/** How far wind carries {@code e} compared with a person: 1.5 for the lightest, 0.5 for the heaviest, by its size. */
	public static double windMass(Entity e) {
		return windMass(e.getBbWidth(), e.getBbHeight());
	}

	/** {@link #windMass(Entity)} for a body of this size (a person is 0.6 by 1.8 and moves 1.0). */
	public static double windMass(double width, double height) {
		double size = Math.max(0.05, width * width * height);
		return Math.max(0.5, Math.min(1.5, Math.sqrt(0.65 / size)));
	}

	/** Pushes {@code target} by a wind's impulse, scaled by its mass. */
	public static void windPush(LivingEntity target, Vec3 impulse) {
		Effects.push(target, impulse.scale(windMass(target)));
	}

	// ------------------------------------------------------------------ once-in-a-while guards

	private static final Map<String, Long> CLAIMS = new ConcurrentHashMap<>();

	/**
	 * A per-creature, per-{@code key} guard for effects that would otherwise stack under a repeating shape (a Zone, a
	 * Linger, an Echo): true the first time, and again only once {@code gap} ticks have passed since it was last granted.
	 */
	public static boolean claim(Entity entity, String key, int gap) {
		long now = entity.level().getGameTime();
		String id = key + ":" + entity.getUUID();
		Long last = CLAIMS.get(id);
		if (last != null && last <= now && now - last < gap) {
			return false;
		}
		if (CLAIMS.size() > 512) {
			CLAIMS.values().removeIf(at -> now - at > 400 || at > now);
		}
		CLAIMS.put(id, now);
		return true;
	}

	/** Whether {@link #claim} with this key would still be refused (it was granted less than {@code gap} ticks ago). */
	public static boolean claimed(Entity entity, String key, int gap) {
		Long last = CLAIMS.get(key + ":" + entity.getUUID());
		long now = entity.level().getGameTime();
		return last != null && last <= now && now - last < gap;
	}

	/** A flinch: {@code target} can't act for {@code ticks} (a player is only slowed). Bosses are never staggered. */
	public static void stagger(LivingEntity target, int ticks) {
		if (!Spirits.isBoss(target)) {
			Spirits.hold(target, ticks);
		}
	}

	// ------------------------------------------------------------------ interrupt

	/**
	 * Breaks what {@code target} is winding up. Returns whether it had anything to break (a creature that was
	 * interrupted in the last {@link #INTERRUPT_GAP} ticks is left alone, so it can't be locked out).
	 */
	public static boolean interrupt(LivingEntity target) {
		long now = target.level().getGameTime();
		Long last = INTERRUPTED.get(target.getUUID());
		if (last != null && last <= now && now - last < INTERRUPT_GAP) {
			return false;
		}
		boolean broke = false;
		if (target instanceof ServerPlayer player) {
			broke = Charging.interrupt(player);
		} else if (target instanceof Mob mob) {
			broke = Runebound.interrupt(mob);
			if (target instanceof Creeper creeper && creeper.getSwelling(1.0F) > 0) {
				creeper.setSwellDir(-1);
				broke = true;
			}
		}
		if (target.isUsingItem()) {
			target.stopUsingItem();
			broke = true;
		}
		if (broke) {
			INTERRUPTED.put(target.getUUID(), now);
		}
		return broke;
	}

	/** Forgets everything (the server stopped). */
	static void clear() {
		SILENCED.clear();
		INTERRUPTED.clear();
		CLAIMS.clear();
	}

	/** Drops finished entries so the tables can't grow for ever. */
	static void sweep(long now) {
		SILENCED.values().removeIf(until -> until < now);
		INTERRUPTED.values().removeIf(at -> now - at > INTERRUPT_GAP || at > now);
		CLAIMS.values().removeIf(at -> now - at > 600 || at > now);
	}
}
