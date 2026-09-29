package dev.wildercord.cast;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * "When it comes down": runs a callback at the spot a thrown creature lands, for the runes that throw first and strike
 * again where the creature falls (Tempest's second bolt, Monolith's slam). Checked every second tick, gives up after
 * {@code maxTicks} (it lands wherever it is then) or when the creature is gone.
 */
final class Landings {
	private Landings() {}

	/** Ticks a thrown creature is given to come back down. */
	static final int MAX_TICKS = 50;

	static void after(Cast cast, LivingEntity t, int maxTicks, Consumer<Vec3> then) {
		boolean[] rose = {false};
		int[] age = {0};
		Runnable[] step = new Runnable[1];
		step[0] = Effects.carryContext(() -> {
			if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
				return;
			}
			age[0] += 2;
			if (!t.onGround()) {
				rose[0] = true;
			}
			boolean down = t.onGround() && (rose[0] || age[0] >= 6);
			if (down || age[0] >= maxTicks) {
				then.accept(t.position());
				return;
			}
			Scheduler.later(2, step[0]);
		});
		Scheduler.later(2, step[0]);
	}
}
