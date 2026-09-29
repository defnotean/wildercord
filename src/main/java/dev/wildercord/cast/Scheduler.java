package dev.wildercord.cast;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Runs spell parts later: delays, zone pulses, rain strikes and On Land watchers. */
public final class Scheduler {
	private Scheduler() {}

	private static final class LandWatch {
		final LivingEntity player;
		int ticksLeft;
		boolean airborne;
		final Consumer<Vec3> action;

		LandWatch(LivingEntity player, int ticksLeft, Consumer<Vec3> action) {
			this.player = player;
			this.ticksLeft = ticksLeft;
			this.action = action;
		}
	}

	private static final class HurtWatch {
		final LivingEntity player;
		int ticksLeft;
		final Consumer<net.minecraft.world.entity.LivingEntity> action;
		/** Only fire once health is below 30%. */
		final boolean lowHealth;

		HurtWatch(LivingEntity player, int ticksLeft, Consumer<net.minecraft.world.entity.LivingEntity> action, boolean lowHealth) {
			this.player = player;
			this.ticksLeft = ticksLeft;
			this.action = action;
			this.lowHealth = lowHealth;
		}
	}

	private static final List<HurtWatch> HURT = new ArrayList<>();
	/**
	 * Tasks by the tick they're due on the scheduler's own clock, each tick's in the order they were
	 * added. Spells schedule many of their parts ahead (an effect's every pulse; a lasting shape books
	 * one step at a time, see {@code ShapeRunners.steps}), so thousands can be waiting: kept by due tick,
	 * a server tick only touches the ones due now, where a single list was walked (and shifted, for every
	 * task taken out of it) in full every tick.
	 */
	private static final Map<Long, List<Runnable>> TASKS = new HashMap<>();
	/** Ticks the scheduler has run: {@link #later} counts from here. */
	private static long clock;
	private static final List<LandWatch> LAND = new ArrayList<>();

	public static void later(int ticks, Runnable action) {
		TASKS.computeIfAbsent(clock + Math.max(1, ticks), k -> new ArrayList<>()).add(Effects.carryContext(action));
	}

	/** Spell parts waiting to run, for the tests: a lasting shape keeps one waiting at a time, however long it lasts. */
	public static int pending() {
		int n = 0;
		for (List<Runnable> due : TASKS.values()) {
			n += due.size();
		}
		return n;
	}

	/** Fires once the player has left the ground and touched it again, within {@code timeout} ticks. */
	public static void onLand(LivingEntity player, int timeout, Consumer<Vec3> action) {
		LandWatch watch = new LandWatch(player, timeout, action);
		watch.airborne = !player.onGround();
		LAND.add(watch);
	}

	/** Fires once, when the player's health first drops below 30% within {@code timeout} ticks. */
	public static void onLowHealth(LivingEntity player, int timeout, Runnable action) {
		HURT.add(new HurtWatch(player, timeout, attacker -> action.run(), true));
	}

	/** Fires once, the next time the player takes damage within {@code timeout} ticks; gets the attacker (or null). */
	public static void onHurt(LivingEntity player, int timeout, Consumer<net.minecraft.world.entity.LivingEntity> action) {
		HURT.add(new HurtWatch(player, timeout, action, false));
	}

	public static void init() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (HURT.isEmpty() || !(entity instanceof LivingEntity player)) {
				return;
			}
			net.minecraft.world.entity.LivingEntity attacker = source.getEntity() instanceof net.minecraft.world.entity.LivingEntity living && living != player ? living : null;
			List<HurtWatch> fired = new ArrayList<>();
			boolean low = player.getHealth() < player.getMaxHealth() * 0.3F;
			HURT.removeIf(watch -> {
				if (watch.player == player && (!watch.lowHealth || low)) {
					fired.add(watch);
					return true;
				}
				return false;
			});
			// Run after the damage settles, so a counter-spell never re-enters the damage code.
			fired.forEach(watch -> later(1, () -> watch.action.accept(attacker)));
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TASKS.clear();
			LAND.clear();
			HURT.clear();
		});
	}

	private static void tick() {
		HURT.removeIf(watch -> watch.player.isRemoved() || !watch.player.isAlive() || --watch.ticksLeft <= 0);
		// Taken out before any runs: actions may schedule more tasks (never for this same tick).
		List<Runnable> due = TASKS.remove(++clock);
		if (due != null) {
			for (Runnable action : due) {
				// One failing part of a spell (or an add-on's) mustn't take the whole server tick down.
				try {
					action.run();
				} catch (RuntimeException e) {
					dev.wildercord.Wildercord.LOGGER.error("A scheduled spell part failed", e);
				}
			}
		}
		if (!LAND.isEmpty()) {
			List<LandWatch> landed = new ArrayList<>();
			for (Iterator<LandWatch> it = LAND.iterator(); it.hasNext(); ) {
				LandWatch watch = it.next();
				if (watch.player.isRemoved() || !watch.player.isAlive() || --watch.ticksLeft <= 0) {
					it.remove();
					continue;
				}
				if (!watch.player.onGround()) {
					watch.airborne = true;
				} else if (watch.airborne) {
					it.remove();
					landed.add(watch);
				}
			}
			for (LandWatch watch : landed) {
				// As for tasks: one failing part of a spell mustn't take the whole server tick down.
				try {
					watch.action.accept(watch.player.position());
				} catch (RuntimeException e) {
					dev.wildercord.Wildercord.LOGGER.error("A spell part set off by landing failed", e);
				}
			}
		}
	}
}
