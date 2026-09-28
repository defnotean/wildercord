package dev.wildercord.api;

import dev.wildercord.spell.RuneDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What an add-on shape sees when its group fires, and how it hands hits to the group's effects.
 *
 * @since 1.0
 */
public interface ShapeContext {
	LivingEntity caster();

	ServerLevel level();

	/** The shape rune itself. */
	RuneDef shape();

	/** Where the group starts: the caster's eyes, or after a link the thing that set it off. */
	Vec3 origin();

	Vec3 direction();

	/** Whether the group starts from the caster (not from a link's trigger). */
	boolean fromCaster();

	/** Where the caster is looking (the block within 24 blocks, dropped to the ground), or the trigger's point. */
	Vec3 aimPoint();

	/** How many copies Split asks for (1 without Split). */
	int copies();

	/** How many of a modifier rune are attached to this shape (Widen, Quicken, your own...). */
	int count(RuneDef modifier);

	/** Radius multiplier from Widen and Focus (and add-on radius modifiers). */
	double radius();

	/** The group's colour (its first effect's element), for visuals. */
	int color();

	/** Living creatures within {@code radius} of {@code centre}, the caster included. */
	List<LivingEntity> near(Vec3 centre, double radius);

	/**
	 * Applies the group's effects to a hit, then fires any On Hit or On Kill after it. Hits share the
	 * cast's creature budget; for a shape that strikes again and again, use {@link #pulse()} per strike.
	 *
	 * @param block the block hit (for world effects), or null
	 * @param face  the face of that block, or null
	 */
	void hit(List<? extends Entity> entities, Vec3 point, BlockPos block, Direction face);

	/** A context for one more strike of a repeating shape: a fresh creature and block budget. */
	ShapeContext pulse();

	/** False once the caster has left, died or the cast was cut short: scheduled work should stop. */
	boolean alive();

	/** Runs {@code task} on the server after {@code ticks}. */
	void later(int ticks, Runnable task);
}
