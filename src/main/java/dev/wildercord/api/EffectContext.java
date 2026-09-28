package dev.wildercord.api;

import dev.wildercord.spell.RuneDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Everything an add-on effect sees when its shape hits, and the ways it may act (which respect the
 * cast's budgets, Shields, PvP and friendly fire).
 *
 * @since 1.0
 */
public interface EffectContext {
	/** Who cast it: a player, or a monster (Runebound and bosses cast runes too). */
	LivingEntity caster();

	ServerLevel level();

	/** The effect rune itself. */
	RuneDef rune();

	/** Creatures this effect may harm: never the caster or their allies, and already past any Shield. */
	List<LivingEntity> harmed();

	/** Creatures this effect may help: the caster and their allies. */
	List<LivingEntity> helped();

	/** The point that was hit. */
	Vec3 point();

	/** The way the spell was travelling. */
	Vec3 direction();

	/** The block that was hit, or null. */
	BlockPos block();

	/** The face of {@link #block()} that was hit, or null. */
	Direction face();

	/** Whether the shape was Self (the caster). */
	boolean self();

	/**
	 * Power multiplier: its modifiers (Amplify, Frugal...), the shape, Heart Circles, Cord enchantments,
	 * the charge, the rhythm chain, elemental leaning and casting gear. Multiply your numbers by it.
	 */
	double power();

	/** Duration multiplier (Extend, Persistence...). */
	double duration();

	/** How many of a modifier rune are attached to this effect (for your own modifiers). */
	int count(RuneDef modifier);

	/**
	 * Damages a creature as spell damage: skips invulnerability frames, applies Execute, element
	 * reactions, Unison and PvP scaling, meets Shields, and counts spell kills.
	 */
	void hurt(LivingEntity target, DamageSource source, double amount);

	/** Magic damage from the caster, for {@link #hurt}. */
	DamageSource magic();

	/**
	 * Whether this spell may change the block at {@code pos}: the server lets spells edit blocks, the
	 * caster is a player who may build there (claims and spawn protection agree), and the cast's block
	 * budget has room. A true answer uses one block of the budget, so only ask for blocks you'll change.
	 */
	boolean mayEdit(BlockPos pos);

	/** False once the caster has left, died or the cast was cut short: scheduled work should stop. */
	boolean alive();

	/** Runs {@code task} on the server after {@code ticks}. */
	void later(int ticks, Runnable task);
}
