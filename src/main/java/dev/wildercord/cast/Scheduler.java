package dev.wildercord.cast;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Runs spell parts later: delays, zone pulses, rain strikes and On Land watchers. */
public final class Scheduler {
	private Scheduler() {}

	private static final class Task {
		int ticksLeft;
		final Runnable action;

		Task(int ticksLeft, Runnable action) {
			this.ticksLeft = ticksLeft;
			this.action = action;
		}
	}

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
	private static final List<Task> TASKS = new ArrayList<>();
	private static final List<LandWatch> LAND = new ArrayList<>();

	public static void later(int ticks, Runnable action) {
		TASKS.add(new Task(Math.max(1, ticks), Effects.carryContext(action)));
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
		if (!TASKS.isEmpty()) {
			List<Task> due = new ArrayList<>();
			for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
				Task task = it.next();
				if (--task.ticksLeft <= 0) {
					it.remove();
					due.add(task);
				}
			}
			// Run after the sweep: actions may schedule more tasks.
			for (Task task : due) {
				// One failing part of a spell (or an add-on's) mustn't take the whole server tick down.
				try {
					task.action.run();
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
				watch.action.accept(watch.player.position());
			}
		}
	}
}
