package dev.wildercord.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.Level;

/**
 * Bridges {@code AbstractCubeMob#setcubeMobHealth(int)} (26.2) vs {@code setCubeMobHealth(int)} (26.3).
 */
public abstract class CompatCubeMob extends AbstractCubeMob {
	protected CompatCubeMob(EntityType<? extends AbstractCubeMob> type, Level level) {
		super(type, level);
	}

	@Override
	protected void setcubeMobHealth(int size) {
		applyCubeMobHealth(size);
	}

	protected abstract void applyCubeMobHealth(int size);
}
