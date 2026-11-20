package dev.wildercord.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;

/**
 * Bridges 26.2's {@code AbstractCubeMob} health/attack hooks onto 26.1.2's {@link Slime}:
 * size changes arrive through {@code setSize(int, boolean)} instead of {@code setcubeMobHealth(int)},
 * and the package-private slime attack goal is built reflectively so ManaOoze keeps the native
 * hop-and-bite cadence.
 */
public abstract class CompatCubeMob extends Slime {
	protected CompatCubeMob(EntityType<? extends CompatCubeMob> type, Level level) {
		super(type, level);
	}

	@Override
	public void setSize(int size, boolean heal) {
		super.setSize(size, heal);
		// 26.1.2 has no Entity#isConstructing; Slime's own constructor call happens after
		// LivingEntity initialised the attribute map, so the hook is safe to run immediately.
		applyCubeMobHealth(size);
	}

	protected Goal cubeAttackGoal() {
		try {
			var attackGoal = Class.forName("net.minecraft.world.entity.monster.Slime$SlimeAttackGoal")
				.getDeclaredConstructor(Slime.class);
			attackGoal.setAccessible(true);
			return (Goal) attackGoal.newInstance(this);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Slime attack goal missing on this Minecraft version", e);
		}
	}

	protected abstract void applyCubeMobHealth(int size);
}
