package dev.wildercord.cast.feel;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Vfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * What a {@link Hook} is given.
 *
 * @param at     where it happens (the hand for CUE, the projectile for TRAVEL, the impact point, the target's centre for HIT)
 * @param dir    the way the spell is going (a unit vector; the caster's look for CUE)
 * @param target the creature touched (HIT only, else null)
 * @param cast   the cast, or null when there is none (a mob's or a scroll's spell)
 * @param tick   for TRAVEL, the projectile's age in ticks; else 0
 */
public record FeelCtx(ServerLevel level, Feel feel, Vfx.Theme theme, Vec3 at, Vec3 dir, Entity target, Cast cast, int tick) {}
