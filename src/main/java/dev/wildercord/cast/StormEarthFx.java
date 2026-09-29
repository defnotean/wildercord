package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.BooleanSupplier;

/**
 * The storm and earth runes' own looks that no other rune shares: the tells their mechanics need (a stun ring, the
 * ionised glow, a charged stance, a spot about to be struck) and the lasting cues of timed effects (an aura that stays as
 * long as the buff, a pop when it ends). Each is one or two small light primitives every few ticks, never a per-tick emitter.
 */
public final class StormEarthFx {
	private StormEarthFx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	public static final int STORM = ElementFx.STORM.primary();
	public static final int STORM_WHITE = ElementFx.STORM.secondary();
	public static final int STORM_BLUE = ElementFx.STORM.accent();
	public static final int EARTH = ElementFx.EARTH.primary();
	public static final int EARTH_PALE = ElementFx.EARTH.secondary();
	/** Plasma's colour: a white-hot core in a magenta-violet sheath (not fire orange, not Riftbolt's black). */
	public static final int PLASMA = 0xD070FF;
	public static final int PLASMA_CORE = 0xFFE8FF;
	/** Deepslate for Stoneform, sandstone for Stoneskin: the two stances read apart. */
	public static final int SLATE = 0x98A2BE;
	public static final int ROOT = 0xA07A48;

	/**
	 * Runs {@code step} every {@code every} ticks for {@code ticks}, while {@code alive} holds, then {@code end} once (unless
	 * it stopped early because {@code alive} failed).
	 */
	static void every(int ticks, int every, BooleanSupplier alive, Runnable step, Runnable end) {
		for (int t = every; t < ticks; t += every) {
			Scheduler.later(t, () -> {
				if (alive.getAsBoolean()) {
					step.run();
				}
			});
		}
		Scheduler.later(ticks, () -> {
			if (alive.getAsBoolean()) {
				end.run();
			}
		});
	}

	private static boolean here(Entity t, ServerLevel level) {
		return t.isAlive() && !t.isRemoved() && t.level() == level;
	}

	// ------------------------------------------------------------------ storm

	/** Jolt: four tiny arcs turn in a ring over the head for as long as the stun holds, then clamp shut. */
	static void stunRing(ServerLevel level, LivingEntity t, int ticks) {
		double r = Math.max(0.35, t.getBbWidth() * 0.6);
		every(ticks, 4, () -> here(t, level), () -> {
			Vec3 head = t.position().add(0, t.getBbHeight() + 0.25, 0);
			double a = level.getGameTime() * 0.5;
			Vec3 p = head.add(Math.cos(a) * r, 0, Math.sin(a) * r);
			Vec3 q = head.add(Math.cos(a + Math.PI) * r, 0, Math.sin(a + Math.PI) * r);
			ElementFx.arc(level, p, q, STORM_BLUE, 0.03, 1, true, 4);
		}, () -> {
			Vec3 head = t.position().add(0, t.getBbHeight() + 0.25, 0);
			ElementFx.ring(level, head, UP, STORM_WHITE, r * 1.4, 0.05, 0.04, 5);
		});
	}

	/** Shock: the arc stays a moment as a crackling tether, a small node at each end. */
	static void tether(ServerLevel level, Vec3 from, Vec3 to) {
		ElementFx.arc(level, from, to, STORM_WHITE, 0.035, 1, false, 6);
		Sigils.flash(level, to, STORM_WHITE, 0.5F);
	}

	/** Plasma's ionised mark: a faint violet haze round the target while it conducts (a mote every 10 ticks). */
	static void ionised(ServerLevel level, LivingEntity t, int ticks) {
		DustParticleOptions haze = new DustParticleOptions(PLASMA, 0.8F);
		every(ticks, 10, () -> here(t, level), () -> {
			Vfx.emit(level, haze, t.getBoundingBox().getCenter(), 2, Math.max(0.3, t.getBbWidth() * 0.5), 0.0);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, t.getBoundingBox().getCenter(), 1, 0.3, 0.02);
		}, () -> { });
	}

	/** Surge: a crackle over the ally for as long as the charge lasts (a spark every 8 ticks), and a pop when it ends. */
	static void surgeAura(ServerLevel level, LivingEntity t, int ticks) {
		every(ticks, 8, () -> here(t, level), () -> {
			Vec3 c = t.getBoundingBox().getCenter();
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, c, 2, Math.max(0.3, t.getBbWidth() * 0.6), 0.03);
		}, () -> {
			Feels.sound(level, t.position(), "storm_pip", 0.5F, 0.7F);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, t.getBoundingBox().getCenter(), 6, 0.4, 0.08);
		});
	}

	/** Stormheart: a charged ring orbiting the chest while it is armed, seen by everyone, and a pop when it lapses. */
	static void stormheartStance(ServerLevel level, LivingEntity t, int ticks) {
		every(ticks, 10, () -> here(t, level), () -> {
			Vec3 chest = t.position().add(0, t.getBbHeight() * 0.6, 0);
			ElementFx.ring(level, chest, UP, STORM, Math.max(0.6, t.getBbWidth() + 0.2), Math.max(0.55, t.getBbWidth()), 0.025, 9);
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, chest, 1, 0.4, 0.02);
		}, () -> {
			Feels.sound(level, t.position(), "storm_pip", 0.6F, 0.75F);
			ElementFx.ring(level, t.position().add(0, t.getBbHeight() * 0.6, 0), UP, STORM_WHITE, 0.3, 1.2, 0.03, 6);
		});
	}

	/** A spot about to be struck from above: a closing ring on the ground (Thunderbird's dive, Aftershock's echo). */
	static void mark(ServerLevel level, Vec3 spot, int color, double radius, int ticks) {
		ElementFx.groundRing(level, spot, color, radius, 0.3, 0.05, ticks);
	}

	/** Galvanize's spark running out: a small discharge ring. */
	static void discharge(ServerLevel level, Vec3 at) {
		ElementFx.ring(level, at, UP, STORM, 0.2, 0.9, 0.03, 6);
		Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, at, 6, 0.3, 0.1);
		Feels.sound(level, at, "storm_pip", 0.6F, 0.6F);
	}

	// ------------------------------------------------------------------ earth

	/** Stoneform: slate plates settling on the body (dark rings closing), unlike Stoneskin's sandstone. */
	static void stoneform(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 base = t.position();
		double r = Math.max(0.6, t.getBbWidth() * 0.9);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> ElementFx.ring(level, base.add(0, 0.2 + k * t.getBbHeight() * 0.35, 0), UP, k == 1 ? EARTH_PALE : SLATE,
				r * 1.8, r, 0.08, 10));
		}
		Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
			net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState()), base.add(0, 1, 0), 10, 0.4, 0.1);
		Feels.sound(level, base, "earth_clamp", 0.9F, 0.75F);
		every(ticks, 20, () -> here(t, level), () -> ElementFx.groundRing(level, t.position(), SLATE, r * 1.1, r * 0.9, 0.05, 12),
			() -> {
				Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
					net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState()), t.position().add(0, 0.8, 0), 8, 0.35, 0.05);
				Feels.sound(level, t.position(), "earth_rattle", 0.6F, 0.8F);
			});
	}

	/** Stoneskin: a slow fall of dust off the plates while it holds, and a crumble when it ends. */
	static void stoneskinHold(ServerLevel level, LivingEntity t, int ticks) {
		net.minecraft.core.particles.BlockParticleOption dust = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.FALLING_DUST,
			net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState());
		every(ticks, 20, () -> here(t, level), () -> Vfx.emit(level, dust, t.position().add(0, t.getBbHeight() * 0.8, 0), 2, 0.3, 0.0),
			() -> {
				Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
					net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState()), t.position().add(0, 0.9, 0), 8, 0.35, 0.05);
				Feels.sound(level, t.position(), "earth_rattle", 0.5F, 1.0F);
			});
	}

	/** Mire: the bog stays visible as long as it holds (a mud ring each second), and burps now and then. */
	static void mire(ServerLevel level, LivingEntity t, int ticks) {
		every(ticks, 20, () -> here(t, level), () -> {
			ElementFx.groundRing(level, t.position(), 0x9A7A50, 1.0, 0.7, 0.06, 18);
			if (level.getRandom().nextInt(2) == 0) {
				Feels.sound(level, t.position(), "earth_gloop", 0.4F, 1.0F);
			}
		}, () -> { });
	}

	/** Root: roots that hold visibly for the whole hold, a slow pulse at the feet. */
	static void rootHold(ServerLevel level, LivingEntity t, int ticks) {
		every(ticks, 15, () -> here(t, level), () -> ElementFx.groundRing(level, t.position(), ROOT, Math.max(0.6, t.getBbWidth()), 0.4, 0.06, 14),
			() -> Feels.sound(level, t.position(), "earth_creak", 0.4F, 1.3F));
	}
}
