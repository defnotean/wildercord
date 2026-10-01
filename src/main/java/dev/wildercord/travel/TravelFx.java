package dev.wildercord.travel;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.MoteOption;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * How travel looks, in arcane magic's colours: a circle and rune ring forming at the traveller's
 * feet through the warmup (a star lights in it for the last second), motes rising off its edge, a
 * pillar of light where they leave and a flash, a shockwave and a star seal where they land. A
 * broken warmup cracks its circle. Built only from the mod's own circles, light and motes.
 */
final class TravelFx {
	private TravelFx() {}

	private static final int PRIMARY = ElementFx.ARCANE.primary();
	private static final int SECONDARY = ElementFx.ARCANE.secondary();
	private static final int ACCENT = ElementFx.ARCANE.accent();
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** The circle under a warmup, sent once a second (each lasts a little over a second, so a cancelled one fades at once). */
	static void circle(ServerLevel level, Vec3 feet, long secondsLeft) {
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, PRIMARY, 1.1F, 26, 0.05F), feet.add(0, 0.06, 0));
		Sigils.send(level, SigilOption.flat(SigilOption.RING, SECONDARY, 1.45F, 26, -0.07F), feet.add(0, 0.07, 0));
		if (secondsLeft <= 1) {
			Sigils.send(level, SigilOption.flat(SigilOption.STAR, ACCENT, 0.7F, 24, 0.12F), feet.add(0, 0.08, 0));
		}
		// A ring of light closing in on the circle's edge, once a second: the countdown made visible.
		Light.groundRing(level, feet, SECONDARY, 1.9, 1.15, 0.04, 16);
	}

	/** A mote rising off the edge of a warmup's circle. */
	static void mote(ServerLevel level, Vec3 feet, RandomSource random) {
		double a = random.nextDouble() * Math.PI * 2;
		Motes.glow(level, feet.add(Math.cos(a) * 1.1, 0.1, Math.sin(a) * 1.1), random.nextBoolean() ? SECONDARY : PRIMARY, 0.1, 30,
			new Vec3(0, 0.06, 0), 0.01);
	}

	/** A warmup broken: its circle cracks and fizzles out. */
	static void fizzle(ServerLevel level, Vec3 feet) {
		Sigils.send(level, SigilOption.flat(SigilOption.CRACKED, PRIMARY, 1.1F, 14, 0F), feet.add(0, 0.09, 0));
		Fx.sound(level, feet, WildercordSounds.MAGIC_BREAK, 0.45F, 1.4F);
	}

	/** Where a traveller leaves: a pillar of light, a ring falling in on them, a flash and motes carried upward. */
	static void depart(ServerLevel level, Vec3 feet) {
		Vec3 heart = feet.add(0, 1, 0);
		Light.ray(level, feet, feet.add(0, 5, 0), PRIMARY, 0.3, 9);
		ElementFx.ring(level, heart, UP, SECONDARY, 1.4, 0.1, 0.05, 8);
		Sigils.flash(level, heart, SECONDARY, 2.0F);
		Motes.glows(level, heart, 10, 0.35, SECONDARY, 0.1, 28, new Vec3(0, 0.1, 0), 0.03);
		Fx.sound(level, feet, WildercordSounds.BLINK, 0.7F, 1.15F);
	}

	/** Where a traveller lands: a flash, a shockwave along the ground, a star seal under them and a burst of motes. */
	static void arrive(ServerLevel level, Vec3 feet) {
		Vec3 heart = feet.add(0, 1, 0);
		Sigils.flash(level, heart, SECONDARY, 2.2F);
		Light.groundRing(level, feet, PRIMARY, 0.2, 2.6, 0.08, 12);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, PRIMARY, 1.0, 16, 0.1);
		Motes.burst(level, heart, 14, SECONDARY, 0.1, 26, 0.12);
		Fx.sound(level, feet, WildercordSounds.BLINK, 0.9F, 0.95F);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.2F);
	}

	/** A place marked (a home, warp or waypoint set): a small star seal at the feet and a chime. */
	static void mark(ServerLevel level, Vec3 feet) {
		ElementFx.flatSigil(level, feet, SigilOption.STAR, PRIMARY, 0.6, 20, 0.08);
		Light.groundRing(level, feet, SECONDARY, 0.1, 1.2, 0.04, 10);
		Fx.sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.5F);
	}

	/**
	 * A tracked waypoint's beam, for its tracker alone: a faint shaft of light rising from it and a few
	 * motes drifting up. Sent past the usual 32 blocks, since it's meant to be seen from afar.
	 */
	static void beam(ServerPlayer viewer, Vec3 at, boolean withShaft) {
		ServerLevel level = viewer.level();
		if (withShaft) {
			dev.wildercord.cast.Fx.sendParticles(level, viewer, new LightOption(LightOption.RAY, SECONDARY, 0F, 24F, 0F, 0.06F, 0F, 0F, 0F, 24), true, true,
				at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
		MoteOption mote = new MoteOption(MoteOption.GLOW, PRIMARY, 0.14F, 50, 0F, 0.06F, 0F, 0.01F);
		dev.wildercord.cast.Fx.sendParticles(level, viewer, mote, true, false, at.x, at.y + 0.5, at.z, 2, 0.15, 0.3, 0.15, 0.01);
	}
}
