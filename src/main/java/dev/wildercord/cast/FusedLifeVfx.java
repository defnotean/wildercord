package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static dev.wildercord.cast.ElementFx.ARCANE;
import static dev.wildercord.cast.ElementFx.BLOOD;
import static dev.wildercord.cast.ElementFx.LIFE;
import static dev.wildercord.cast.ElementFx.TIME;
import static dev.wildercord.cast.ElementFx.WIND;

/**
 * How the fused effects of life and blood look ({@link FusedLife}). Like every fused effect, each draws on both of
 * its elements' visual languages ({@link ElementFx}):
 * <ul>
 *   <li><b>Zephyr</b> (wind, life): a warm breeze turning round the spot in gold and spring green, petals of light
 *       carried round on it, and butterflies of light lifting off everyone it touches.</li>
 *   <li><b>Crimson Mist</b> (wind, blood): a slow red haze, billows drifting round on a slow wind, a heartbeat
 *       running through the ground once a second.</li>
 *   <li><b>Soulbond</b> (life, arcane): a tether of pale arcane light between the two, a pink strand and a green
 *       one twining round it, star seals under both; a bright bead runs along it with every shared wound.</li>
 *   <li><b>Second Wind</b> (life, time): an hourglass of green light rimmed in gold round the ally, sand of light
 *       running through its neck; it shatters into green and golden shards when it saves them.</li>
 *   <li><b>Transfusion</b> (life, blood): a cut on the giver, and a stream of blood-light arcing to the ally that
 *       turns green as it goes, blooming where it lands.</li>
 *   <li><b>Lifebloom</b> (life, life): a flower seal of petals of light opening under the ally, ring by ring,
 *       pulsing each second, and bursting wide open when it fades.</li>
 *   <li><b>Bonespur</b> (earth, blood): the ground cracks under each enemy and pale spurs of bone burst up round
 *       it, leaning in, blood running down from their points.</li>
 *   <li><b>Sanguine Rite</b> (blood, blood): a blood sigil opens under the caster as they pay, and a crimson lance
 *       drives into each target, an upright sigil and crossed cuts where it lands.</li>
 * </ul>
 */
final class FusedLifeVfx {
	private FusedLifeVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	/** Zephyr's warmth: sunlit gold. */
	private static final int WARM = 0xFFE3A6;
	/** Petals of light. */
	private static final int PETAL = 0xFFB8DA;
	/** Crimson Mist's haze, deep and paler. */
	private static final int HAZE = 0x9B1B30;
	private static final int HAZE_DEEP = 0x6A0E1E;
	private static final int HAZE_LIGHT = 0xE04A5E;
	/** Bone, and its shadow. */
	private static final int BONE = 0xF2EBD9;
	private static final int BONE_SHADE = 0xCFC3A4;
	private static final ColorParticleOption LEAF = ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF000000 | LIFE.primary());

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	/** A colour {@code s} of the way from {@code from} to {@code to}. */
	private static int lerp(int from, int to, double s) {
		int r = (int) Math.round(((from >> 16) & 0xFF) * (1 - s) + ((to >> 16) & 0xFF) * s);
		int g = (int) Math.round(((from >> 8) & 0xFF) * (1 - s) + ((to >> 8) & 0xFF) * s);
		int b = (int) Math.round((from & 0xFF) * (1 - s) + (to & 0xFF) * s);
		return r << 16 | g << 8 | b;
	}

	// ------------------------------------------------------------------ Zephyr

	/** Zephyr: a warm breeze turning round the spot, three waves of it, carrying petals and leaves. */
	static void zephyr(ServerLevel level, Vec3 feet, double radius) {
		ElementFx.groundRing(level, feet, WARM, 0.3, radius, 0.05, 14);
		ElementFx.groundRing(level, feet, LIFE.primary(), 0.2, radius * 0.75, 0.035, 18);
		ElementFx.swirl(level, feet.add(0, 0.15, 0), Math.min(2.4, radius * 0.55), 2.4, 5, WARM, LIFE.secondary());
		Sigils.flash(level, feet.add(0, 1.0, 0), WARM, 1.6F);
		ElementFx.petals(level, feet.add(0, 1.6, 0), radius * 0.4, 8);
		breeze(level, feet, radius, 0);
		Scheduler.later(7, () -> breeze(level, feet, radius, 1));
		Scheduler.later(14, () -> breeze(level, feet, radius, 2));
		Feels.sound(level, feet, "wind_feather", 0.9F, 0.9F);
		Fx.sound(level, feet, SoundEvents.CHERRY_LEAVES_BREAK, 1.0F, 1.2F);
	}

	/** One wave of the breeze: petals of light carried round the circle on the wind, a low warm crescent sweeping after them. */
	private static void breeze(ServerLevel level, Vec3 feet, double radius, int wave) {
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < 9; i++) {
			double a = phase + Math.PI * 2 * i / 9;
			double rr = radius * (0.35 + 0.55 * r.nextDouble());
			Vec3 p = feet.add(Math.cos(a) * rr, 0.3 + r.nextDouble() * 1.5, Math.sin(a) * rr);
			// Round the circle (anticlockwise), lifting a little.
			Vec3 along = new Vec3(-Math.sin(a), 0.2, Math.cos(a));
			int color = i % 3 == 0 ? PETAL : i % 3 == 1 ? WARM : LIFE.secondary();
			Motes.fling(level, p, along, 0.16 + r.nextDouble() * 0.08, color, 0.11, 26, new Vec3(0, 0.006, 0));
		}
		Vfx.emit(level, wave % 2 == 1 ? LEAF : ParticleTypes.CHERRY_LEAVES, feet.add(0, 1.2, 0), 3, radius * 0.45, 0.0);
		double a = phase + wave * 2.1;
		ElementFx.slash(level, feet.add(0, 0.45 + wave * 0.45, 0), ElementFx.tilted(0.2, a), ElementFx.flatDir(a), wave % 2 == 0 ? WARM : WIND.primary(),
			radius * 0.7, 2.6, 0.06, 3, 10);
	}

	/** An ally Zephyr touches: a green-gold gust climbing it, petals, and butterflies of light lifting off. */
	static void zephyrTouch(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		ElementFx.swirl(level, feet.add(0, 0.1, 0), Math.max(0.5, t.getBbWidth() * 0.8), t.getBbHeight() + 0.2, 3, LIFE.primary(), WARM);
		ElementFx.petals(level, feet.add(0, t.getBbHeight() + 0.2, 0), 0.35, 3);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 2; i++) {
			Vec3 at = centre(t).add((r.nextDouble() - 0.5) * 0.8, 0.2 + r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.8);
			Motes.butterfly(level, at, i == 0 ? PETAL : WARM, 0.3, 40, new Vec3((r.nextDouble() - 0.5) * 0.05, 0.035, (r.nextDouble() - 0.5) * 0.05));
		}
	}

	// ------------------------------------------------------------------ Crimson Mist

	/** The mist rising: a heartbeat through the ground, a gust spreading it, and the first red billows. */
	static void crimsonMistOpen(ServerLevel level, Vec3 feet, double radius) {
		ElementFx.groundRing(level, feet, BLOOD.primary(), 0.2, radius, 0.05, 10);
		ElementFx.groundRing(level, feet, HAZE_LIGHT, 0.15, radius * 0.75, 0.035, 14);
		ElementFx.swirl(level, feet.add(0, 0.2, 0), radius * 0.7, 1.4, 4, BLOOD.primary(), HAZE_LIGHT);
		Motes.clouds(level, feet.add(0, 0.6, 0), 8, radius * 0.4, HAZE, 1.4, 60, new Vec3(0, 0.008, 0), 0.03, 0.42);
		ElementFx.drip(level, feet.add(0, 1.4, 0), radius * 0.35, 5);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, feet.add(0, 0.4, 0), 2, radius * 0.3, 0.0);
		Fx.sound(level, feet, SoundEvents.WIND_CHARGE_BURST, 0.5F, 0.6F);
		Fx.sound(level, feet, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
	}

	/** The haze, every few ticks: billows drifting round on a slow wind, now and then a wisp of red wind stirring it. */
	static void crimsonMist(ServerLevel level, Vec3 feet, double radius, int age) {
		RandomSource r = level.getRandom();
		if (age % 10 == 0) {
			for (int i = 0; i < 3; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				double rr = Math.sqrt(r.nextDouble()) * radius * 0.85;
				Vec3 p = feet.add(Math.cos(a) * rr, 0.3 + r.nextDouble() * 0.9, Math.sin(a) * rr);
				// Round the middle, slowly: the wind carrying it.
				Vec3 drift = new Vec3(-Math.sin(a) * 0.012, 0.004, Math.cos(a) * 0.012);
				Motes.clouds(level, p, 1, 0.15, i == 0 ? HAZE_DEEP : HAZE, 1.1 + r.nextDouble() * 0.5, 50, drift, 0.003, 0.36);
			}
		}
		if (age % 20 == 5) {
			double a = r.nextDouble() * Math.PI * 2;
			ElementFx.slash(level, feet.add(0, 0.6 + r.nextDouble() * 0.8, 0), ElementFx.tilted(0.25, a), ElementFx.flatDir(a), HAZE_LIGHT, radius * 0.7, 2.0,
				0.045, 5, 14);
		}
		Vfx.emit(level, ParticleTypes.CRIMSON_SPORE, feet.add(0, 0.9, 0), 2, radius * 0.5, 0.0);
	}

	/** Once a second: a heartbeat running out through the ground under the mist. */
	static void crimsonMistPulse(ServerLevel level, Vec3 feet, double radius) {
		ElementFx.groundRing(level, feet, BLOOD.primary(), 0.2, radius, 0.035, 9);
		Scheduler.later(4, () -> ElementFx.groundRing(level, feet, BLOOD.secondary(), 0.1, radius * 0.7, 0.025, 9));
		Fx.sound(level, feet, SoundEvents.WARDEN_HEARTBEAT, 0.45F, 1.2F);
	}

	/** An enemy bleeding in the mist. */
	static void crimsonMistBleed(ServerLevel level, LivingEntity t) {
		ElementFx.drip(level, centre(t), Math.max(0.2, t.getBbWidth() * 0.4), 3);
		ElementFx.ring(level, centre(t), UP, BLOOD.primary(), 0.2, Math.max(0.6, t.getBbWidth() + 0.2), 0.03, 7);
	}

	/** An ally mending in the mist: warm red motes rising off it. */
	static void crimsonMistMend(ServerLevel level, LivingEntity t) {
		Motes.glows(level, centre(t), 3, Math.max(0.2, t.getBbWidth() * 0.4), 0xFF8A9A, 0.1, 22, new Vec3(0, 0.03, 0), 0.01);
		Vfx.emit(level, ParticleTypes.HEART, t.position().add(0, t.getBbHeight() + 0.3, 0), 1, 0.2, 0.0);
	}

	/** The mist thinning: its last billows blown outward. */
	static void crimsonMistClose(ServerLevel level, Vec3 feet, double radius) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < 4; i++) {
			double a = Math.PI * 2 * i / 4 + r.nextDouble();
			Vec3 out = ElementFx.flatDir(a);
			Motes.clouds(level, feet.add(out.scale(radius * 0.5)).add(0, 0.6, 0), 1, 0.2, HAZE, 1.2, 30, out.scale(0.03).add(0, 0.01, 0), 0.005, 0.3);
		}
		ElementFx.groundRing(level, feet, HAZE_LIGHT, radius * 0.5, radius * 1.2, 0.03, 10);
		Fx.sound(level, feet, SoundEvents.WIND_CHARGE_BURST, 0.3F, 0.8F);
	}

	// ------------------------------------------------------------------ Soulbond

	/** Where the tether meets a body: a little above its middle. */
	private static Vec3 anchor(Entity e) {
		return centre(e).add(0, 0.15, 0);
	}

	/** Two souls bound: a star seal under each, comets of pink and green round them, and the tether reaching across. */
	static void soulbondBind(ServerLevel level, LivingEntity a, LivingEntity b) {
		for (LivingEntity e : new LivingEntity[] {a, b}) {
			double w = Math.max(0.5, e.getBbWidth() * 0.8);
			ElementFx.starSeal(level, e.position().add(0, 0.06, 0), UP, 0.35 + w * 0.5, 30);
			ElementFx.groundRing(level, e.position(), LIFE.primary(), 0.2, 0.8 + w, 0.035, 18);
			ElementFx.orbit(level, centre(e), 0.45 + w * 0.4, 2, 10, ARCANE.primary(), LIFE.primary());
		}
		Sigils.flash(level, anchor(b), ARCANE.secondary(), 1.4F);
		soulbondTether(level, a, b, 0, false);
		Motes.seek(level, anchor(a), anchor(b), ARCANE.secondary(), 0.16, 10, 1.5);
		Motes.seek(level, anchor(b), anchor(a), LIFE.secondary(), 0.16, 10, 1.5);
		dev.wildercord.cast.feel.Feels.sound(level, a.position(), "life_bond", 1.0F, 1.0F);
	}

	/**
	 * The tether, drawn fresh as the last fades: a core of pale arcane light sagging between the two, and a pink
	 * strand and a green one twining round it, pinched in at either end. {@code age} turns the twist, so it
	 * seems to wind as it holds; a {@code flare} is the bright flash of a wound crossing it.
	 */
	static void soulbondTether(ServerLevel level, LivingEntity a, LivingEntity b, int age, boolean flare) {
		Vec3 from = anchor(a);
		Vec3 d = anchor(b).subtract(from);
		double length = d.length();
		if (length < 0.4) {
			return;
		}
		Vec3 dir = d.scale(1 / length);
		Vec3 u = ElementFx.perp(dir);
		Vec3 v = dir.cross(u);
		int n = (int) Math.max(4, Math.min(12, Math.round(length / 1.1)));
		double sag = Math.min(0.9, 0.07 * length);
		double turns = Math.max(1, Math.min(4, length / 3));
		double twist = age * 0.3;
		Vec3[] core = new Vec3[n + 1];
		Vec3[] pink = new Vec3[n + 1];
		Vec3[] green = new Vec3[n + 1];
		for (int k = 0; k <= n; k++) {
			double s = k / (double) n;
			Vec3 p = from.add(d.scale(s)).add(0, -sag * Math.sin(Math.PI * s), 0);
			double w = 0.13 * Math.sin(Math.PI * s);
			double angle = twist + s * Math.PI * 2 * turns;
			Vec3 off = u.scale(Math.cos(angle) * w).add(v.scale(Math.sin(angle) * w));
			core[k] = p;
			pink[k] = p.add(off);
			green[k] = p.subtract(off);
		}
		int life = flare ? 8 : 12;
		for (int k = 1; k <= n; k++) {
			boolean nearA = k <= n / 2;
			if (flare) {
				// Only the core flashes: the strands are still there from the last drawing.
				piece(level, core, k, nearA, 0xFFFFFF, 0.07, life);
				continue;
			}
			piece(level, core, k, nearA, ARCANE.secondary(), 0.045, life);
			piece(level, pink, k, nearA, ARCANE.primary(), 0.022, life);
			piece(level, green, k, nearA, LIFE.primary(), 0.022, life);
		}
		if (!flare && age % 20 == 0) {
			// A mote of light running along it, and a few green flecks drifting down off the middle.
			Motes.seek(level, core[1], core[n - 1], age % 40 == 0 ? ARCANE.secondary() : LIFE.secondary(), 0.1, 16, turns);
			Motes.glows(level, core[n / 2], 2, 0.15, LIFE.secondary(), 0.08, 24, new Vec3(0, -0.01, 0), 0.01);
		}
	}

	/**
	 * One piece of a line of points, sent from its end nearer the middle of the line, so neither end of the
	 * tether sits in front of a bound player's own eyes (see {@link Fx#send}).
	 */
	private static void piece(ServerLevel level, Vec3[] p, int k, boolean nearStart, int color, double width, int life) {
		if (nearStart) {
			ElementFx.ray(level, p[k], p[k - 1], color, width, life);
		} else {
			ElementFx.ray(level, p[k - 1], p[k], color, width, life);
		}
	}

	/** A wound crossing the bond: the tether flares, a bead of light runs along it, a star flashes on the one taking its half. */
	static void soulbondShare(ServerLevel level, LivingEntity from, LivingEntity to, boolean saved) {
		soulbondTether(level, from, to, 0, true);
		Motes.seek(level, anchor(from), anchor(to), 0xFFFFFF, 0.2, 7, 0.5);
		Vec3 c = centre(to);
		double w = Math.max(0.6, to.getBbWidth() + 0.3);
		ElementFx.ring(level, c, UP, ARCANE.primary(), 0.15, w, 0.04, 8);
		ElementFx.shimmer(level, c, 0.25, 3);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.6F);
		if (saved) {
			// The one whose killing blow it halved: a star seal and a bloom where it would have fallen.
			Vec3 s = centre(from);
			ElementFx.starSeal(level, from.position().add(0, 0.06, 0), UP, Math.max(0.8, from.getBbWidth() + 0.5), 20);
			ElementFx.lifeImpact(level, s, 1.0);
			Sigils.flash(level, s, ARCANE.secondary(), 1.8F);
			Fx.sound(level, s, SoundEvents.BELL_RESONATE, 0.6F, 1.6F);
		}
	}

	/** The bond stretched too far: it snaps, each end whipping back to its own. */
	static void soulbondSnap(ServerLevel level, LivingEntity a, LivingEntity b) {
		Vec3 pa = anchor(a);
		Vec3 pb = anchor(b);
		Vec3 d = pb.subtract(pa);
		if (d.lengthSqr() < 1.0E-4) {
			return;
		}
		Vec3 dir = d.normalize();
		ElementFx.ray(level, pa.add(dir.scale(1.6)), pa.add(dir.scale(0.4)), ARCANE.primary(), 0.03, 6);
		ElementFx.ray(level, pb.subtract(dir.scale(1.6)), pb.subtract(dir.scale(0.4)), LIFE.primary(), 0.03, 6);
		ElementFx.shimmer(level, pa.add(dir.scale(1.2)), 0.2, 3);
		ElementFx.shimmer(level, pb.subtract(dir.scale(1.2)), 0.2, 3);
		dev.wildercord.cast.feel.Feels.sound(level, a.position(), "life_bond_snap", 0.9F, 1.0F);
	}

	/** The bond running its time: a last shimmer at either end. */
	static void soulbondFade(ServerLevel level, LivingEntity a, LivingEntity b) {
		for (LivingEntity e : new LivingEntity[] {a, b}) {
			ElementFx.shimmer(level, centre(e), 0.3, 4);
			ElementFx.groundRing(level, e.position(), ARCANE.primary(), Math.max(0.6, e.getBbWidth() + 0.4), 0.1, 0.025, 10);
		}
		Fx.sound(level, a.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4F, 1.6F);
	}

	/** Nobody to bind to: a strand of light reaches out from the caster and frays into nothing. */
	static void soulbondAlone(ServerLevel level, LivingEntity caster) {
		Vec3 from = anchor(caster);
		Vec3 look = Effects.horizontal(caster.getLookAngle(), caster.getLookAngle());
		Vec3 p = from.add(look.scale(1.2));
		for (int k = 0; k < 3; k++) {
			Vec3 q = p.add(look.scale(0.5)).add(0, -0.08 * (k + 1), 0);
			ElementFx.ray(level, q, p, k % 2 == 0 ? ARCANE.primary() : LIFE.primary(), 0.03 - k * 0.007, 8 - k);
			p = q;
		}
		ElementFx.shimmer(level, p, 0.2, 3);
		ElementFx.flatSigil(level, caster.position(), SigilOption.STAR, ARCANE.primary(), 0.5, 14, 0.1);
		Fx.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.7F);
	}

	// ------------------------------------------------------------------ Second Wind

	/**
	 * An hourglass of light standing at {@code feet}, {@code height} tall: gold rims top and bottom (time), and
	 * {@code sides} green lines (life) running from each rim in to a narrow neck, turned by {@code turn}.
	 */
	private static void hourglass(ServerLevel level, Vec3 feet, double height, double radius, double turn, int sides, int lifetime, boolean sand) {
		Vec3 top = feet.add(0, height, 0);
		Vec3 waist = feet.add(0, height / 2, 0);
		ElementFx.ring(level, top, UP, TIME.primary(), radius, radius, 0.03, lifetime);
		ElementFx.ring(level, feet, UP, TIME.primary(), radius, radius, 0.03, lifetime);
		for (int i = 0; i < sides; i++) {
			Vec3 out = ElementFx.flatDir(turn + Math.PI * 2 * i / sides);
			Vec3 neck = waist.add(out.scale(radius * 0.12));
			ElementFx.ray(level, top.add(out.scale(radius)), neck, LIFE.primary(), 0.03, lifetime);
			ElementFx.ray(level, feet.add(out.scale(radius)), neck, LIFE.primary(), 0.03, lifetime);
		}
		if (sand) {
			// Sand of golden light running down through the neck.
			Motes.glows(level, waist.add(0, height * 0.2, 0), 3, 0.04, TIME.secondary(), 0.07, 16, new Vec3(0, -0.035, 0), 0.004);
		}
	}

	/** Second Wind taken: an hourglass of green light rises round the ally and turns, leaves round it and golden flecks. */
	static void secondWind(ServerLevel level, LivingEntity t) {
		double height = t.getBbHeight() + 0.2;
		double radius = Math.max(0.45, t.getBbWidth() * 0.7 + 0.15);
		for (int i = 0; i < 4; i++) {
			double turn = i * 0.3;
			Runnable draw = () -> {
				if (t.isAlive()) {
					hourglass(level, t.position().add(0, 0.05, 0), height, radius, turn, 4, 8, true);
				}
			};
			if (i == 0) {
				draw.run();
			} else {
				Scheduler.later(i * 5, draw);
			}
		}
		ElementFx.leafSpiral(level, t.position(), radius * 0.8, height, 5);
		ElementFx.goldenTicks(level, centre(t), 0.4, 6);
		Sigils.flash(level, centre(t), LIFE.secondary(), 1.2F);
		Fx.sound(level, t.position(), WildercordSounds.impact("time"), 0.6F, 1.3F);
		Fx.sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.5F);
		Fx.sound(level, t.position(), SoundEvents.AZALEA_LEAVES_PLACE, 0.8F, 1.1F);
	}

	/** Every couple of seconds while it waits: a small hourglass turning over the ally's head. */
	static void secondWindMark(ServerLevel level, LivingEntity t, int age) {
		Vec3 feet = t.position().add(0, t.getBbHeight() + 0.3, 0);
		hourglass(level, feet, 0.42, 0.16, age * 0.05, 2, 14, false);
		Motes.glow(level, feet.add(0, 0.3, 0), TIME.secondary(), 0.05, 12, new Vec3(0, -0.02, 0), 0.0);
	}

	/** Second Wind saving someone: the hourglass shatters into green and golden shards, and they rise in a flash of life. */
	static void secondWindSaved(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double height = t.getBbHeight() + 0.2;
		double radius = Math.max(0.45, t.getBbWidth() * 0.7 + 0.15);
		hourglass(level, t.position().add(0, 0.05, 0), height, radius, 0, 4, 4, false);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 14; i++) {
			Vec3 dir = ElementFx.randomDir(r).add(0, 0.2, 0).normalize();
			double length = 0.8 + r.nextDouble() * 0.9;
			int color = i % 3 == 0 ? TIME.primary() : i % 3 == 1 ? LIFE.primary() : LIFE.secondary();
			ElementFx.ray(level, c.add(dir.scale(0.25)), c.add(dir.scale(length)), color, 0.04, 7 + r.nextInt(3));
		}
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STAINED_GLASS.lime().defaultBlockState()), c, 10, 0.25);
		Sigils.flash(level, c, LIFE.secondary(), 2.6F);
		Sigils.flash(level, c, TIME.secondary(), 1.2F);
		ElementFx.ring(level, c, UP, TIME.primary(), 0.3, 2.4, 0.06, 10);
		ElementFx.ring(level, c, ElementFx.tilted(0.7, r.nextDouble() * Math.PI * 2), LIFE.primary(), 0.2, 1.8, 0.04, 12);
		Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, c, 24, 0.45);
		ElementFx.leafSpiral(level, t.position(), Math.max(0.5, t.getBbWidth() * 0.8), height + 0.2, 6);
		ElementFx.gustRing(level, t.position(), 3.0);
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_gasp", 1.0F, 1.0F);
		if (t instanceof ServerPlayer player) {
			ScreenFx.tint(player, LIFE.primary(), 12);
		}
	}

	/**
	 * A Second Wind that won't take (this one was saved by one not long ago): a small cracked hourglass
	 * over them, its sand spilling out, and a dull tick.
	 */
	static void secondWindSpent(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position().add(0, t.getBbHeight() + 0.3, 0);
		hourglass(level, feet, 0.42, 0.16, 0, 2, 10, false);
		ElementFx.ray(level, feet.add(0.12, 0.34, 0), feet.add(-0.1, 0.1, 0), BLOOD.secondary(), 0.02, 10);
		Motes.glows(level, feet.add(0, 0.2, 0), 3, 0.08, TIME.secondary(), 0.06, 18, new Vec3(0, -0.04, 0), 0.02);
		Fx.sound(level, t.position(), SoundEvents.GLASS_HIT, 0.6F, 0.6F);
		Fx.sound(level, t.position(), WildercordSounds.impact("time"), 0.25F, 0.6F);
	}

	/** A Second Wind running out unused: the sand runs out, a gold ring closing in. */
	static void secondWindFade(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.ring(level, c, UP, TIME.primary(), Math.max(0.5, t.getBbWidth() + 0.3), 0.1, 0.025, 10);
		ElementFx.goldenTicks(level, c, 0.3, 4);
		Fx.sound(level, c, WildercordSounds.impact("time"), 0.3F, 1.7F);
	}

	// ------------------------------------------------------------------ Transfusion

	/**
	 * Transfusion: a cut and a heartbeat on the giver, then a stream of blood-light arcing over to the ally,
	 * turning from crimson to green as it goes, and a bloom where it lands.
	 */
	static void transfusion(ServerLevel level, LivingEntity from, LivingEntity to, double given) {
		Vec3 a = centre(from);
		Vec3 b = centre(to);
		Vec3 d = b.subtract(a);
		double length = d.length();
		Vec3 facing = Effects.horizontal(d, from.getLookAngle());
		ElementFx.cut(level, a.add(facing.scale(0.35)), facing, ElementFx.perp(facing).add(0, 0.5, 0).normalize(), 0.35, 0.07);
		ElementFx.pulse(level, a, UP, 0.7);
		ElementFx.drip(level, a, 0.2, 3);
		int n = (int) Math.max(4, Math.min(10, Math.round(length / 0.8)));
		double lift = Math.min(1.0, 0.15 * length);
		Vec3 prev = a;
		for (int k = 1; k <= n; k++) {
			double s = k / (double) n;
			Vec3 p = a.add(d.scale(s)).add(0, lift * Math.sin(Math.PI * s), 0);
			int color = lerp(BLOOD.primary(), LIFE.primary(), s);
			Vec3 start = prev;
			Scheduler.later(1 + k * 6 / n, () -> ElementFx.ray(level, start, p, color, 0.06, 10));
			// Motes of blood along the way, fading green.
			Fx.send(level, new DustColorTransitionOptions(BLOOD.primary(), LIFE.primary(), 1.0F), p, 1, 0.05, 0.0);
			prev = p;
		}
		Motes.seek(level, a, b, BLOOD.secondary(), 0.14, 9, 1.0);
		Motes.seek(level, a, b, LIFE.secondary(), 0.12, 11, 1.5);
		double size = 0.6 + Math.min(0.6, given * 0.1);
		Scheduler.later(9, () -> {
			if (!to.isAlive()) {
				return;
			}
			Vec3 c = centre(to);
			ElementFx.lifeImpact(level, c, size);
			ElementFx.leafSpiral(level, to.position(), Math.max(0.4, to.getBbWidth() * 0.7), to.getBbHeight() + 0.2, 4);
			Vfx.emit(level, ParticleTypes.HEART, to.position().add(0, to.getBbHeight() + 0.3, 0), 2, 0.3, 0.0);
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.3F);
		});
		Fx.sound(level, a, SoundEvents.WARDEN_HEARTBEAT, 0.9F, 1.1F);
		Fx.sound(level, a, SoundEvents.PLAYER_HURT, 0.4F, 0.8F);
	}

	/** Nothing given: a heartbeat falling in at the giver's feet. */
	static void transfusionRefused(ServerLevel level, LivingEntity caster) {
		ElementFx.groundRing(level, caster.position(), BLOOD.primary(), 0.9, 0.1, 0.03, 8);
		ElementFx.drip(level, centre(caster), 0.15, 2);
		Fx.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.6F);
	}

	// ------------------------------------------------------------------ Lifebloom

	/**
	 * A flower of light lying on the ground at {@code feet}: {@code petals} crescents round the middle, each
	 * bulging outward like a petal's tip, sweeping open over a few ticks.
	 */
	private static void flower(ServerLevel level, Vec3 feet, double radius, int petals, double phase, int color, int lifetime) {
		for (int i = 0; i < petals; i++) {
			Vec3 out = ElementFx.flatDir(phase + Math.PI * 2 * i / petals);
			ElementFx.slash(level, feet.add(out.scale(radius * 0.5)).add(0, 0.08, 0), UP, out, color, radius * 0.5, 2.4, 0.05, 3, lifetime);
		}
	}

	/** Lifebloom: a flower seal opening under the ally, ring by ring (pink, green, pale gold), leaves climbing it. */
	static void lifebloomOpen(ServerLevel level, LivingEntity t) {
		double size = Math.max(0.9, t.getBbWidth() + 0.5);
		Vec3 feet = t.position().add(0, 0.02, 0);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, LIFE.accent(), size * 0.3, 40, 0.05);
		flower(level, feet, size * 0.5, 6, 0, LIFE.accent(), 30);
		Scheduler.later(3, () -> flower(level, feet, size * 0.85, 6, Math.PI / 6, LIFE.primary(), 28));
		Scheduler.later(6, () -> flower(level, feet, size * 1.2, 8, 0, LIFE.secondary(), 26));
		Sigils.flash(level, centre(t), LIFE.secondary(), 1.4F);
		ElementFx.leafSpiral(level, t.position(), Math.max(0.45, t.getBbWidth() * 0.75), t.getBbHeight() + 0.3, 6);
		ElementFx.petals(level, centre(t).add(0, t.getBbHeight() * 0.5, 0), 0.5, 6);
		Fx.sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.4F);
		Fx.sound(level, t.position(), SoundEvents.PINK_PETALS_PLACE, 1.0F, 0.9F);
		Fx.sound(level, t.position(), WildercordSounds.impact("life"), 0.5F, 1.0F);
	}

	/** Each second of it: a small flower pulsing at the ally's feet (turning a little each time) and a mote of green rising. */
	/** One step of the scale higher on each beat of a Lifebloom. */
	private static final float[] BEAT_PITCH = {1.0F, 1.122F, 1.26F, 1.498F, 1.682F};

	static void lifebloomPulse(ServerLevel level, LivingEntity t, int beat) {
		double size = Math.max(0.8, t.getBbWidth() + 0.4);
		flower(level, t.position().add(0, 0.02, 0), size * 0.7, 6, beat * 0.3, beat % 2 == 0 ? LIFE.primary() : LIFE.accent(), 14);
		Motes.glows(level, centre(t), 2, 0.3, LIFE.secondary(), 0.1, 22, new Vec3(0, 0.03, 0), 0.01);
		dev.wildercord.cast.feel.Feels.sound(level, t.position(), "life_ripen", 0.6F, BEAT_PITCH[Math.floorMod(beat, BEAT_PITCH.length)]);
	}

	/** The bloom fading: the flower bursts wide open, petals thrown out to everyone it heals. */
	static void lifebloomBurst(ServerLevel level, LivingEntity t, double radius) {
		Vec3 feet = t.position().add(0, 0.02, 0);
		Vec3 c = centre(t);
		flower(level, feet, radius, 8, 0, LIFE.primary(), 16);
		flower(level, feet, radius * 0.6, 8, Math.PI / 8, LIFE.accent(), 18);
		ElementFx.groundRing(level, feet, LIFE.secondary(), 0.3, radius, 0.06, 12);
		Sigils.flash(level, c, LIFE.secondary(), 2.2F);
		Motes.burst(level, c, 14, PETAL, 0.12, 30, 0.25);
		Vfx.emit(level, ParticleTypes.CHERRY_LEAVES, c.add(0, 0.6, 0), 10, radius * 0.4, 0.0);
		Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, c, 10, 0.3);
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_ripen_burst", 1.0F, 1.0F);
	}

	/** An ally the burst heals. */
	static void lifebloomMended(ServerLevel level, LivingEntity t) {
		ElementFx.lifeImpact(level, centre(t), 0.6);
		Vfx.emit(level, ParticleTypes.HEART, t.position().add(0, t.getBbHeight() + 0.3, 0), 1, 0.2, 0.0);
	}

	// ------------------------------------------------------------------ Bonespur

	/** Bonespur's reach: a pale ring and a red one running out over the ground; with nobody in it, a few spurs at the point. */
	static void bonespurField(ServerLevel level, Vec3 ground, double radius, boolean empty) {
		ElementFx.groundRing(level, ground, BONE, 0.3, radius, 0.05, 12);
		ElementFx.groundRing(level, ground, BLOOD.primary(), 0.2, radius * 0.8, 0.03, 14);
		Fx.sound(level, ground, SoundEvents.BONE_BLOCK_PLACE, 0.8F, 0.6F);
		if (empty) {
			ElementFx.crack(level, ground.add(0, 0.05, 0), 0.9, 18);
			spurs(level, ground, 0.5, 1.0, 3);
			Fx.sound(level, ground, SoundEvents.BONE_BLOCK_BREAK, 0.8F, 0.8F);
		}
	}

	/** Where a spur is about to break out: a cracked seal of bone under the enemy, a red ring drawing in. */
	static void bonespurWarn(ServerLevel level, LivingEntity t) {
		double w = Math.max(0.6, t.getBbWidth() + 0.3);
		ElementFx.flatSigil(level, t.position(), SigilOption.CRACKED, BONE, w, 16, 0.0);
		ElementFx.groundRing(level, t.position(), BLOOD.primary(), w + 0.3, 0.1, 0.04, 6);
	}

	/**
	 * Spurs of bone erupting round {@code feet}: {@code count} of them, their bases {@code spread} out, leaning
	 * in to meet over it {@code height} up. A thick pale root, the spur tapering to its point, and blood running
	 * down from the tip.
	 */
	private static void spurs(ServerLevel level, Vec3 feet, double spread, double height, int count) {
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			Vec3 out = ElementFx.flatDir(phase + Math.PI * 2 * i / count + (r.nextDouble() - 0.5) * 0.5);
			Vec3 base = feet.add(out.scale(spread * (0.9 + 0.4 * r.nextDouble()))).add(0, 0.02, 0);
			Vec3 tip = feet.add(out.scale(spread * 0.2)).add(0, height * (0.6 + 0.4 * r.nextDouble()), 0);
			Vec3 along = tip.subtract(base);
			ElementFx.ray(level, base, base.add(along.scale(0.45)), BONE_SHADE, 0.17, 16);
			ElementFx.ray(level, base, tip, BONE, 0.09, 16);
			ElementFx.ray(level, base.add(along.scale(0.55)), tip, BLOOD.primary(), 0.035, 14);
		}
	}

	/** The spurs breaking out under one enemy: the ground cracked open, bone thrown up, blood. */
	static void bonespur(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		double w = Math.max(0.35, t.getBbWidth() * 0.6);
		spurs(level, feet, w, t.getBbHeight(), 5);
		ElementFx.crack(level, feet.add(0, 0.05, 0), Math.max(0.8, w + 0.5), 20);
		ElementFx.stoneShards(level, feet.add(0, 0.3, 0), Blocks.BONE_BLOCK.defaultBlockState(), 10, 0.25);
		ElementFx.drip(level, centre(t), 0.3, 5);
		ElementFx.pulse(level, centre(t), UP, Math.max(0.7, w + 0.4));
		Fx.sound(level, feet, SoundEvents.BONE_BLOCK_BREAK, 1.0F, 0.7F);
		Fx.sound(level, feet, SoundEvents.POINTED_DRIPSTONE_LAND, 0.8F, 1.2F);
		Fx.sound(level, feet, WildercordSounds.impact("earth"), 0.6F, 1.0F);
	}

	// ------------------------------------------------------------------ Sanguine Rite

	/** The price paid: a blood sigil opening under the caster, a heartbeat, and their own blood falling. */
	static void sanguineSigil(ServerLevel level, LivingEntity caster) {
		Vec3 feet = caster.position();
		ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, BLOOD.primary(), 1.1, 30, 0.06);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.RING, BLOOD.secondary(), 1.45, 30, -0.05);
		ElementFx.flatSigil(level, feet.add(0, 0.02, 0), SigilOption.STAR, BLOOD.primary(), 0.6, 30, 0.1);
		ElementFx.groundRing(level, feet, BLOOD.primary(), 0.2, 1.6, 0.04, 9);
		ElementFx.pulse(level, centre(caster), UP, 0.9);
		ElementFx.drip(level, centre(caster), 0.25, 4);
		Vfx.emit(level, ParticleTypes.DAMAGE_INDICATOR, centre(caster), 3, 0.3, 0.1);
		Fx.sound(level, feet, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.9F);
		Fx.sound(level, feet, SoundEvents.PLAYER_HURT, 0.6F, 0.7F);
	}

	/** The lance: a crimson spear of light from the caster into the target, over a stroke of darkness, and a sigil and crossed cuts where it lands. */
	static void sanguineLance(ServerLevel level, LivingEntity caster, LivingEntity t) {
		Vec3 to = centre(t);
		Vec3 from = caster.getEyePosition().add(caster.getLookAngle().scale(0.8)).add(0, -0.35, 0);
		if (from.distanceToSqr(to) > 24 * 24) {
			// Far off: the lance comes out of the air a way short of it.
			from = to.add(from.subtract(to).normalize().scale(12));
		}
		Vec3 dir = to.subtract(from);
		if (dir.lengthSqr() < 1.0E-4) {
			dir = caster.getLookAngle();
		}
		dir = dir.normalize();
		Vec3 start = from;
		ElementFx.ray(level, start, to, ElementFx.dark(BLOOD.accent()), 0.34, 10);
		ElementFx.ray(level, start, to, BLOOD.primary(), 0.16, 9);
		ElementFx.ray(level, start, to, BLOOD.secondary(), 0.05, 7);
		Scheduler.later(2, () -> ElementFx.ray(level, start, to, BLOOD.primary(), 0.1, 6));
		ElementFx.sigil(level, to.subtract(dir.scale(0.3)), dir, SigilOption.CIRCLE, BLOOD.primary(), 0.75, 14, 0.15);
		ElementFx.sigil(level, to.subtract(dir.scale(0.32)), dir, SigilOption.STAR, BLOOD.secondary(), 0.45, 14, -0.2);
		Vec3 side = ElementFx.perp(dir);
		ElementFx.cut(level, to, dir, side.add(0, 0.8, 0).normalize(), 0.55, 0.1);
		ElementFx.cut(level, to, dir, side.scale(-1).add(0, 0.8, 0).normalize(), 0.55, 0.1);
		ElementFx.drip(level, to, 0.3, 6);
		ElementFx.pulse(level, to, dir, 1.0);
		Sigils.flash(level, to, BLOOD.primary(), 1.6F);
		Fx.sound(level, start, SoundEvents.TRIDENT_THROW, 0.8F, 0.7F);
		Fx.sound(level, to, SoundEvents.TRIDENT_HIT, 1.0F, 0.6F);
		Fx.sound(level, to, WildercordSounds.impact("blood"), 0.8F, 1.0F);
	}

	/** Not enough blood for the rite: its sigil cracks and gutters out. */
	static void sanguineRefused(ServerLevel level, LivingEntity caster) {
		ElementFx.flatSigil(level, caster.position(), SigilOption.CRACKED, BLOOD.primary(), 0.9, 14, 0.0);
		ElementFx.groundRing(level, caster.position(), BLOOD.secondary(), 1.0, 0.1, 0.03, 8);
		Fx.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.5F);
	}
}
