package dev.wildercord.api;

import dev.wildercord.spell.RuneDef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * What an add-on link sees, and how it fires the rest of the spell.
 *
 * @since 1.0
 */
public interface LinkContext {
	LivingEntity caster();

	ServerLevel level();

	/** The link rune itself. */
	RuneDef link();

	/** How many of a modifier rune are attached to the link. */
	int count(RuneDef modifier);

	/** Where the segment before the link started. */
	Vec3 position();

	Vec3 direction();

	/** Fires the rest of the spell where the segment before it started. */
	void fire();

	/** Fires the rest of the spell at a point, aimed along {@code direction}, set off by {@code entity} (or null). */
	void fireAt(Vec3 position, Vec3 direction, Entity entity);

	/** False once the caster has left, died or the cast was cut short. */
	boolean alive();

	/** Runs {@code task} on the server after {@code ticks}. */
	void later(int ticks, Runnable task);
}
