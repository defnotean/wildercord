package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.feel.MarkHalos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * How the two wind statuses look (the seams of {@link Statuses}): <b>silenced</b> is a dull ring over the head with a slash
 * through it, that beats once a second while it lasts; <b>airborne</b> is a lift of pale rings and sparkles at the feet when
 * it takes hold, and a small burst of light on the target whenever a spell gets its bonus. Both are sparing: a handful of
 * particles, never more than one cue per creature per second.
 */
final class StatusVfx {
	private StatusVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	/** Silence: the grey-green of held breath. */
	private static final int MUTED = 0x8FA69C;
	private static final int SKY = 0xBFE3FF;

	/** A creature falls silent: a muted ring closes over its head, a slash through it, and the sound is smothered. */
	static void silenced(ServerLevel level, Entity target) {
		Vec3 head = target.position().add(0, target.getBbHeight() + 0.45, 0);
		ElementFx.ring(level, head, UP, MUTED, 0.7, 0.3, 0.04, 9);
		Light.slash(level, head, new Vec3(1, 0.4, 0).normalize(), new Vec3(0, 0, 1), 0xFFFFFF, 0.3, 1.6, 0.05, 1, 8);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, head, 2, 0.15, 0.0);
		Feels.sound(level, target.position(), "wind_muffle", 0.8F, 1.0F);
	}

	/** One beat of a lasting silence: the shared crown halo (see {@link MarkHalos}) in the muted colour. */
	static void silencedBeat(ServerLevel level, Entity target) {
		MarkHalos.halo(level, target, MUTED, MarkHalos.Style.CROWN);
	}

	/**
	 * A creature is lifted (the airborne mark takes hold): the mark's own halo is drawn by {@link MarkHalos} (pale motes rising
	 * off it while it lasts), so this is just the lift: one ring off the feet and the sound.
	 */
	static void lifted(ServerLevel level, Entity target) {
		Vec3 feet = target.position();
		ElementFx.groundRing(level, feet, SKY, 0.2, Math.max(0.5, target.getBbWidth()) + 0.7, 0.05, 9);
		Feels.sound(level, feet, "wind_lift", 0.5F, 1.0F);
	}

	/** A spell landing on something airborne: a small burst of pale light that says "harder". */
	static void airborneBite(ServerLevel level, LivingEntity target) {
		if (!Statuses.claim(target, "airbite", 10)) {
			return;
		}
		Vec3 c = target.getBoundingBox().getCenter();
		ElementFx.ring(level, c, UP, SKY, 0.2, Math.max(0.7, target.getBbWidth() * 0.9), 0.03, 6);
		Vfx.emit(level, ParticleTypes.END_ROD, c, 3, 0.25, 0.08);
	}
}
