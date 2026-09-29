package dev.wildercord.cast.feel;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Vfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * What frost and wind leave behind (the AFTERMATH hooks of {@link FrostFeels} and {@link WindFeels}): a small patch of frost
 * that sits a moment, wet drips on what water touched, and leaves and dust settling after a gust. Kept small on purpose: an
 * aftermath is a signature, not a second explosion (a handful of particles, one seal).
 */
final class FrostWindFx {
	private FrostWindFx() {}

	/** Frost: a small frost seal on the ground under the impact and a few flakes drifting down through it. */
	static void frostPatch(FeelCtx ctx) {
		double r = 0.6 + 0.35 * Math.min(2.0, ctx.feel().scale());
		ElementFx.frostCreep(ctx.level(), ctx.at().add(0, -0.15, 0), r, 30);
		Motes.clouds(ctx.level(), ctx.at().add(0, 0.6, 0), 2, 0.3, 0xEAF8FF, 0.35, 26, new Vec3(0, -0.02, 0), 0.01, 0.25);
	}

	/** Water: drips falling off the spot, as off something soaked. */
	static void wetSheen(FeelCtx ctx) {
		Vfx.emit(ctx.level(), ParticleTypes.DRIPPING_WATER, ctx.at().add(0, 0.5, 0), 4, 0.35, 0.0);
		Vfx.emit(ctx.level(), ParticleTypes.FALLING_WATER, ctx.at().add(0, 0.7, 0), 2, 0.3, 0.0);
	}

	/** Wind: dust and leaves settling where a gust passed. */
	static void settle(FeelCtx ctx) {
		Motes.clouds(ctx.level(), ctx.at().add(0, 0.3, 0), 3, 0.5, 0xD8E4D0, 0.45, 28, new Vec3(0, -0.015, 0), 0.02, 0.2);
		Vfx.emit(ctx.level(), ParticleTypes.SMALL_GUST, ctx.at().add(0, 0.2, 0), 2, 0.4, 0.0);
	}
}
