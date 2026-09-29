package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import com.mojang.math.Transformation;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * How the fused effects of flame and stone look ({@link FusedFlame}). Like every fused effect, each draws on both of
 * its elements' visual languages ({@link ElementFx}):
 * <ul>
 *   <li><b>Phoenix Pyre</b>: wings of flame fanning from the ally's back over green feathers of life, a seal of
 *       fire and leaf underfoot, and flame and green light rising from them as they go.</li>
 *   <li><b>Hellmouth</b>: a disc of darkness in the ground with red-hot cracks round it, black and violet flames
 *       licking at its rim, darkness sweeping in from its edge; it caves in with a burst of violet flame.</li>
 *   <li><b>Starfire</b>: a star seal bursting, and five gold stars flying on curving paths, pink and gold streaks
 *       and falling sparks behind them.</li>
 *   <li><b>Everburn</b>: flames inside a clock face of gold whose hands race round, a dial spinning underfoot, a hand
 *       sweeping round with each extra burn; the clock runs backward when it rekindles.</li>
 *   <li><b>Bloodboil</b>: crimson bubbles and red steam rising, a hot heartbeat, and the blood flashing into
 *       crimson flame each time it's hurt.</li>
 *   <li><b>Conflagration</b>: a pillar of flame under a sun seal, a ring of fire racing out, and arcs of flame
 *       leaping to every burning enemy, which go up in columns of fire.</li>
 *   <li><b>Monolith</b>: a real column of stone (block displays: a plinth of the ground itself, three drums of stone
 *       brick, the middle one carved, and a capital) bursting from the cracked ground with seams of gold light up its
 *       edges, then crumbling from the top down.</li>
 *   <li><b>Magnetize</b>: a lodestone seal, loops of field light arching over the target from head to foot, iron
 *       filings drawn in, and lightning where something touches it.</li>
 *   <li><b>Sinkhole</b>: the ground cracks, a hole of darkness opens and widens, slabs of the ground tilt into it and
 *       rings of dust and darkness keep falling in; then the slabs slam down.</li>
 * </ul>
 * The stone is block displays only, never the world's blocks: each is discarded on its own schedule, all of them as
 * the server stops, and one left behind anyway (its chunk unloaded) is removed when it loads ({@link BlockFx}).
 */
final class FusedFlameVfx {
	private FusedFlameVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;
	/** Void's darkness: the black of Hellmouth's fire and Sinkhole's hole. */
	private static final int DARK = ElementFx.dark(ElementFx.VOID.accent());
	private static final int PIT_SMOKE = 0x2A1838;
	private static final int BLOOD_STEAM = 0xE07A80;
	private static final int STAR = 0xFFF2B0;
	private static final int DUST = 0x9A8A70;

	private static final ElementFx.Palette FIRE = ElementFx.FIRE;
	private static final ElementFx.Palette LIFE = ElementFx.LIFE;
	private static final ElementFx.Palette VOID = ElementFx.VOID;
	private static final ElementFx.Palette ARCANE = ElementFx.ARCANE;
	private static final ElementFx.Palette TIME = ElementFx.TIME;
	private static final ElementFx.Palette BLOOD = ElementFx.BLOOD;
	private static final ElementFx.Palette EARTH = ElementFx.EARTH;
	private static final ElementFx.Palette STORM = ElementFx.STORM;

	private static Vec3 centre(Entity t) {
		return t.getBoundingBox().getCenter();
	}

	// ------------------------------------------------------------------ Phoenix Pyre

	/** Phoenix Pyre catches: the wings unfold, a seal of fire and leaf opens underfoot, flame and green light climb the ally. */
	static void phoenixRise(ServerLevel level, LivingEntity t, double radius) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		ElementFx.flatSigil(level, feet, SigilOption.STAR, FIRE.secondary(), 0.9, 34, 0.08);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.RING, LIFE.primary(), 1.35, 34, -0.05);
		ElementFx.groundRing(level, feet, FIRE.primary(), 0.3, radius, 0.07, 14);
		ElementFx.groundRing(level, feet, LIFE.primary(), radius, 0.4, 0.04, 16);
		wings(level, t, 1.0, 18, true);
		double h = t.getBbHeight();
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> Light.ring(level, feet.add(0, 0.2 + k * 0.35 * h, 0), UP, k == 1 ? LIFE.primary() : FIRE.primary(), 0.9, 0.35,
				0.05, 10));
		}
		Motes.glows(level, c, 8, 0.35, LIFE.secondary(), 0.14, 34, new Vec3(0, 0.05, 0), 0.02);
		ElementFx.embers(level, feet.add(0, 0.3, 0), 0.45, 10);
		ElementFx.petals(level, c.add(0, 0.6, 0), 0.5, 6);
		Sigils.flash(level, c, LIFE.secondary(), 1.3F);
		ElementFx.heatFlare(level, c.add(0, 0.2, 0), 0.9);
		Feels.sound(level, feet, "fire_pyre", 1.0F, 1.0F);
	}

	/**
	 * A phoenix's wings on {@code t}'s back: feathers fanning up and out from the shoulders on both sides, each a
	 * flame-orange stroke with a gold quill over a shorter green feather of life. Drawn as beams ({@link Light}), which
	 * open even in front of the ally's own eyes, so they can see their own wings. {@code unfold} spreads six feathers a
	 * side one after another from the lowest; otherwise three flicker at once.
	 */
	private static void wings(ServerLevel level, LivingEntity t, double scale, int lifetime, boolean unfold) {
		double h = t.getBbHeight();
		double s = scale * Math.max(0.55, Math.min(1.6, h / 1.8));
		double yaw = Math.toRadians(t.getYRot());
		Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
		Vec3 side = new Vec3(back.z, 0, -back.x);
		Vec3 root = t.position().add(0, h * 0.7, 0).add(back.scale(0.22 * s));
		int feathers = unfold ? 6 : 3;
		for (int k = 0; k < feathers; k++) {
			int f = unfold ? k : k * 2 + 1;
			double theta = Math.toRadians(6 + f * 15);
			double length = s * (0.8 + 0.65 * Math.sin(Math.PI * (f + 0.5) / 6));
			int green = f % 2 == 0 ? LIFE.primary() : LIFE.secondary();
			for (int sign = -1; sign <= 1; sign += 2) {
				Vec3 from = root.add(side.scale(sign * 0.1 * s));
				Vec3 dir = side.scale(sign * Math.cos(theta)).add(0, Math.sin(theta) * 1.1, 0).add(back.scale(0.45)).normalize();
				Vec3 under = side.scale(sign * Math.cos(theta - 0.12)).add(0, Math.sin(theta - 0.12), 0).add(back.scale(0.3)).normalize();
				Runnable feather = () -> {
					Light.ray(level, from, from.add(dir.scale(length)), FIRE.primary(), 0.1 * s, lifetime);
					Light.ray(level, from, from.add(dir.scale(length * 0.88)), FIRE.secondary(), 0.035 * s, lifetime - 2);
					Light.ray(level, from.add(0, -0.05, 0), from.add(under.scale(length * 0.62)), green, 0.06 * s, lifetime + 2);
				};
				if (unfold && f > 0) {
					Scheduler.later(f, feather);
				} else {
					feather.run();
				}
			}
		}
	}

	/** Each second of the pyre: the wings flicker, a ring of fire runs out to the aura's edge, green light and embers rise. */
	static void phoenixPulse(ServerLevel level, LivingEntity t, double radius, int age) {
		Vec3 feet = t.position();
		ElementFx.groundRing(level, feet, FIRE.primary(), radius * 0.5, radius, 0.06, 12);
		ElementFx.groundRing(level, feet, LIFE.primary(), radius * 0.9, radius * 0.4, 0.035, 14);
		wings(level, t, 0.85, 12, false);
		Motes.glows(level, centre(t), 3, 0.3, LIFE.secondary(), 0.12, 26, new Vec3(0, 0.05, 0), 0.015);
		ElementFx.embers(level, feet.add(0, 0.2, 0), Math.min(0.8, radius * 0.4), 4);
		if (age % 40 == 20) {
			Feels.sound(level, feet, "fire_whump", 0.5F, 1.4F);
		}
	}

	/** Between the pulses: a spark of flame or of green light rising off the ally's feet. */
	static void phoenixSmoulder(ServerLevel level, LivingEntity t) {
		RandomSource r = level.getRandom();
		double a = r.nextDouble() * Math.PI * 2;
		Vec3 p = t.position().add(Math.cos(a) * 0.5, 0.1, Math.sin(a) * 0.5);
		Motes.glow(level, p, r.nextBoolean() ? LIFE.primary() : FIRE.secondary(), 0.1, 22, new Vec3(0, 0.07, 0), 0.01);
		Vfx.emit(level, ParticleTypes.SMALL_FLAME, p, 1, 0.05, 0.01);
	}

	/** The pyre scorching an enemy: a lash of flame with a green heart from the ally, and fire licking up it. */
	static void phoenixScorch(ServerLevel level, LivingEntity ally, LivingEntity enemy) {
		Vec3 from = centre(ally);
		Vec3 to = centre(enemy);
		Light.ray(level, from, to, FIRE.primary(), 0.05, 6);
		Light.ray(level, from, to, LIFE.secondary(), 0.018, 5);
		ElementFx.flames(level, enemy.position(), Math.max(0.35, enemy.getBbWidth() * 0.6), enemy.getBbHeight() * 0.8, 4);
		ElementFx.embers(level, to, 0.25, 3);
		Fx.sound(level, to, SoundEvents.FIRECHARGE_USE, 0.35F, 1.5F);
	}

	/** The pyre burns out: gold sparks rising and a few petals. */
	static void phoenixFade(ServerLevel level, LivingEntity t) {
		Motes.glows(level, centre(t).add(0, 0.5, 0), 5, 0.3, FIRE.secondary(), 0.12, 24, new Vec3(0, 0.06, 0), 0.02);
		ElementFx.petals(level, centre(t), 0.4, 4);
		Feels.sound(level, t.position(), "fire_out", 0.6F, 1.2F);
	}

	// ------------------------------------------------------------------ Hellmouth

	/**
	 * Hellmouth opens: a disc of darkness in the ground, red-hot cracks round it, a violet ring as far as it drags,
	 * its rim glowing, black and violet flame bursting up and the ground thrown out of it.
	 */
	static void hellmouthOpen(ServerLevel level, Vec3 centre, double radius, double core, int ticks) {
		int life = ticks + 12;
		ElementFx.flatSigil(level, centre, SigilOption.CIRCLE, DARK, core, life, 0.03);
		ElementFx.flatSigil(level, centre.add(0, 0.01, 0), SigilOption.CRACKED, FIRE.accent(), core * 1.9, life, -0.01);
		ElementFx.flatSigil(level, centre.add(0, 0.02, 0), SigilOption.RING, VOID.primary(), radius, life, -0.04);
		ElementFx.groundRing(level, centre, FIRE.primary(), core, core, 0.07, life);
		ElementFx.tongues(level, centre, core * 0.8, 1.8, 8, DARK, VOID.primary(), 3, 12);
		ElementFx.implode(level, centre.add(0, 0.4, 0), radius, 10);
		ElementFx.stoneShards(level, centre.add(0, 0.3, 0), ElementFx.groundBlock(level, centre), 12, 0.3);
		Motes.clouds(level, centre.add(0, 0.4, 0), 4, core * 0.5, PIT_SMOKE, 1.3, 40, new Vec3(0, 0.03, 0), 0.01, 0.55);
		ScreenFx.shake(level, centre, 0.25F, 10);
		Feels.sound(level, centre, "fire_pit", 1.0F, 1.0F);
	}

	/** A quarter second of the pit: darkness sweeping in from its edge, flames at its rim, violet and red embers rising; a glow from its depths each second. */
	static void hellmouth(ServerLevel level, Vec3 centre, double radius, double core, int age) {
		ElementFx.ring(level, centre.add(0, 0.12, 0), UP, DARK, radius, core * 0.6, 0.12, 8);
		ElementFx.ring(level, centre.add(0, 0.1, 0), UP, VOID.primary(), radius * 0.95, core, 0.025, 9);
		ElementFx.tongues(level, centre, core * 0.9, 1.2, 3, DARK, VOID.primary(), 2, 8);
		Motes.glows(level, centre.add(0, 0.3, 0), 3, core * 0.45, age % 10 == 0 ? FIRE.accent() : VOID.secondary(), 0.11, 22, new Vec3(0, 0.06, 0), 0.02);
		if (age % 20 == 0) {
			ElementFx.blackCore(level, centre.add(0, 0.35, 0), core * 0.35, 12);
			Sigils.flash(level, centre.add(0, 0.2, 0), FIRE.accent(), (float) (core * 1.4));
			Motes.clouds(level, centre.add(0, 0.4, 0), 2, core * 0.4, PIT_SMOKE, 1.1, 36, new Vec3(0, 0.035, 0), 0.01, 0.5);
			Feels.sound(level, centre, "fire_field", 0.7F, 0.7F);
		} else if (age % 10 == 5) {
			Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, centre.add(0, 0.3, 0), 4, core * 0.4, 0.02);
		}
	}

	/** Something dragged toward the pit: a streak of darkness with a violet thread. */
	static void hellmouthPull(ServerLevel level, Vec3 centre, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.ray(level, c, centre.add(0, 0.3, 0), DARK, 0.07, 5);
		ElementFx.ray(level, c, centre.add(0, 0.3, 0), VOID.primary(), 0.02, 4);
	}

	/** Something burning in the pit's core: black and violet flame up its body. */
	static void hellmouthBurn(ServerLevel level, LivingEntity t) {
		ElementFx.tongues(level, t.position(), Math.max(0.35, t.getBbWidth() * 0.6), t.getBbHeight(), 4, DARK, VOID.primary(), 2, 8);
		ElementFx.embers(level, centre(t), 0.3, 3);
	}

	/** Cast again into its own pit: the flames surge. */
	static void hellmouthStoke(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.implode(level, centre.add(0, 0.4, 0), radius, 8);
		ElementFx.tongues(level, centre, 0.9, 1.6, 5, DARK, VOID.primary(), 2, 10);
		Feels.sound(level, centre, "fire_whump", 0.6F, 0.7F);
	}

	/** Too many pits open: a gutter of darkness and smoke. */
	static void hellmouthFizzle(ServerLevel level, Vec3 centre) {
		ElementFx.implode(level, centre.add(0, 0.4, 0), 0.8, 6);
		Motes.smoke(level, centre.add(0, 0.3, 0), 2, 0.3);
		Feels.sound(level, centre, "fire_out", 0.5F, 0.7F);
	}

	/** The pit caves in: the ground cracks and falls in, the black fire gutters into a burst of violet flame, and dust rolls out. */
	static void hellmouthClose(ServerLevel level, Vec3 centre, double radius, double core) {
		Vec3 mid = centre.add(0, 0.5, 0);
		ElementFx.crack(level, centre, radius * 0.8, 24);
		ElementFx.voidImpact(level, mid, core * 1.4);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 6; i++) {
			Vec3 normal = ElementFx.randomDir(r);
			ElementFx.slash(level, mid, normal, ElementFx.inPlane(normal, r.nextDouble() * Math.PI * 2), i % 2 == 0 ? DARK : VOID.primary(),
				core * (0.8 + 0.4 * r.nextDouble()), 1.8, 0.14, 2, 8);
		}
		Sigils.flash(level, mid, FIRE.accent(), (float) (core * 2.2));
		Motes.clouds(level, centre.add(0, 0.3, 0), 6, radius * 0.4, Motes.SMOKE, 1.4, 44, new Vec3(0, 0.03, 0), 0.03, 0.45);
		ScreenFx.shake(level, centre, 0.45F, 14);
		Feels.sound(level, centre, "fire_meteor_hit", 1.0F, 0.8F);
	}

	// ------------------------------------------------------------------ Starfire

	/** The motes burst out: a star seal, a gold ring and a pink one on a tilt, and a chime. */
	static void starfireBurst(ServerLevel level, Vec3 at) {
		Sigils.flash(level, at, FIRE.secondary(), 1.6F);
		ElementFx.starSeal(level, at, UP, 0.7, 12);
		ElementFx.ring(level, at, UP, FIRE.secondary(), 0.2, 1.4, 0.05, 8);
		ElementFx.ring(level, at, ElementFx.tilted(0.8, level.getRandom().nextDouble() * Math.PI * 2), ARCANE.primary(), 0.2, 1.1, 0.035, 9);
		Feels.sound(level, at, "fire_star", 1.0F, 1.0F);
	}

	/** One tick of a mote's flight: a gold star, a pink or gold streak behind it with a pale core, and a spark falling off the trail. */
	static void starfireMote(ServerLevel level, Vec3 from, Vec3 to, int index, int age) {
		boolean pink = (index + age) % 2 == 0;
		ElementFx.orb(level, to, FIRE.secondary(), 0.16, 3);
		ElementFx.ray(level, from, to, pink ? ARCANE.primary() : FIRE.primary(), 0.06, 6);
		ElementFx.ray(level, from, to, STAR, 0.02, 4);
		Motes.glow(level, from, pink ? ARCANE.secondary() : FIRE.secondary(), 0.07, 16, new Vec3(0, -0.012, 0), 0.02);
		if (age % 3 == 0) {
			Vfx.emit(level, ParticleTypes.END_ROD, from, 1, 0.04, 0.0);
		}
	}

	/** A mote lands: a gold flash, a little star seal, a lick of flame and pink sparks. */
	static void starfireStrike(ServerLevel level, Vec3 at, int index) {
		Sigils.flash(level, at, FIRE.secondary(), 1.1F);
		ElementFx.starSeal(level, at, ElementFx.tilted(0.5, index * 1.3), 0.45, 10);
		ElementFx.flameBurst(level, at, 0.45, 2);
		ElementFx.shimmer(level, at, 0.25, 4);
		Motes.burst(level, at, 6, ARCANE.primary(), 0.08, 18, 0.12);
		Feels.sound(level, at, "fire_flick", 0.8F, FireBloodVfx.LADDER[Math.floorMod(index, FireBloodVfx.LADDER.length)]);
	}

	/** A mote with nothing left to seek winks out. */
	static void starfireFizzle(ServerLevel level, Vec3 at) {
		Motes.burst(level, at, 4, ARCANE.secondary(), 0.06, 14, 0.06);
		Vfx.emit(level, ParticleTypes.END_ROD, at, 2, 0.1, 0.01);
	}

	// ------------------------------------------------------------------ Everburn

	/** Everburn catches: flames inside a gold clock face whose hands race round, and a dial spinning fast underfoot. */
	static void everburnLight(ServerLevel level, LivingEntity t, int ticks) {
		Vec3 c = centre(t);
		double r = Math.max(0.7, t.getBbWidth() + 0.45);
		ElementFx.clock(level, c, UP, r, 8, false);
		ElementFx.flatSigil(level, t.position(), SigilOption.RING, TIME.primary(), r * 1.25, Math.min(ticks, 60), 0.25);
		ElementFx.tongues(level, t.position(), r * 0.8, t.getBbHeight() + 0.3, 6, FIRE.primary(), TIME.primary(), 2, 9);
		ElementFx.heatFlare(level, c, 0.8);
		ElementFx.goldenTicks(level, c, 0.35, 5);
		Feels.sound(level, c, "fire_whump", 0.8F, 1.2F);
	}

	/** One of Everburn's own burns: a gold hand sweeps once round the target and the flames jump. */
	static void everburnTick(ServerLevel level, LivingEntity t, boolean rekindled) {
		Vec3 c = centre(t);
		double r = Math.max(0.6, t.getBbWidth() + 0.35);
		double a = level.getRandom().nextDouble() * Math.PI * 2;
		ElementFx.slash(level, c, UP, ElementFx.flatDir(a), TIME.secondary(), r, Math.PI * 1.8, 0.06, 3, 7);
		ElementFx.tongues(level, t.position(), r * 0.75, t.getBbHeight(), 3, rekindled ? TIME.primary() : FIRE.primary(), FIRE.secondary(), 2, 7);
		ElementFx.goldenTicks(level, c, 0.3, 2);
		Feels.sound(level, c, "fire_clock", 0.45F, 1.4F);
	}

	/** Everburn rekindles: the clock runs backward and the fire it burned comes back. */
	static void everburnRekindle(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		double r = Math.max(0.7, t.getBbWidth() + 0.5);
		ElementFx.clock(level, c, UP, r, 10, true);
		ElementFx.fireImpact(level, c, 1.0);
		ElementFx.ring(level, t.position().add(0, 0.1, 0), UP, TIME.primary(), r * 1.6, 0.3, 0.05, 10);
		ElementFx.goldenTicks(level, c, 0.4, 6);
		Feels.sound(level, c, "fire_clock", 0.9F, 0.8F);
	}

	/** Everburn is spent: the clock stops, a curl of smoke. */
	static void everburnOut(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.stoppedClock(level, c, UP, Math.max(0.6, t.getBbWidth() + 0.35), level.getRandom().nextDouble() * Math.PI * 2, 14);
		Motes.smoke(level, c, 2, 0.3);
		Feels.sound(level, c, "fire_out", 0.5F, 1.0F);
	}

	// ------------------------------------------------------------------ Bloodboil

	/** Bloodboil strikes: a crimson cut and heartbeat, a flash of heat, and the blood starting to boil. */
	static void bloodboil(ServerLevel level, LivingEntity t) {
		Vec3 c = centre(t);
		ElementFx.bloodImpact(level, c, 1.0);
		Sigils.flash(level, c, FIRE.primary(), 1.0F);
		ElementFx.pulse(level, c, UP, Math.max(0.9, t.getBbWidth() + 0.5));
		simmer(level, t, 5);
		Fx.sound(level, c, SoundEvents.LAVA_POP, 0.9F, 0.7F);
		Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0F, 0.6F);
		Fx.sound(level, c, WildercordSounds.impact("blood"), 0.7F, 1.0F);
	}

	/** The boiling, every half second: crimson bubbles rising through the target and a wisp of red steam; a hot heartbeat each second. */
	static void bloodboilSimmer(ServerLevel level, LivingEntity t, int age) {
		simmer(level, t, 2);
		if (age % 20 == 0) {
			Vec3 c = centre(t);
			ElementFx.ring(level, c, UP, BLOOD.primary(), 0.15, Math.max(0.8, t.getBbWidth() + 0.4), 0.05, 7);
			ElementFx.ring(level, c, UP, FIRE.primary(), 0.05, Math.max(0.6, t.getBbWidth() + 0.2), 0.035, 10);
			Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.5F, 0.8F);
		}
	}

	private static void simmer(ServerLevel level, LivingEntity t, int bubbles) {
		Vec3 c = centre(t);
		double w = Math.max(0.25, t.getBbWidth() * 0.45);
		Motes.glows(level, c.add(0, -0.2, 0), bubbles, w, BLOOD.secondary(), 0.1, 18, new Vec3(0, 0.06, 0), 0.02);
		Motes.clouds(level, c.add(0, t.getBbHeight() * 0.35, 0), 1, w * 0.6, BLOOD_STEAM, 0.8, 26, new Vec3(0, 0.05, 0), 0.01, 0.35);
		if (bubbles >= 3) {
			Vfx.emit(level, ParticleTypes.LAVA, c, 1, w * 0.5, 0.0);
		}
	}

	/** Hurt while boiling: the blood flashes into flame, a crimson cut and tongues of crimson and orange fire bursting out. */
	static void bloodboilBurst(ServerLevel level, LivingEntity t, int count) {
		Vec3 c = centre(t);
		RandomSource r = level.getRandom();
		Vec3 normal = ElementFx.randomDir(r);
		ElementFx.cut(level, c, normal, ElementFx.inPlane(normal, r.nextDouble() * Math.PI * 2), 0.55, 0.1);
		ElementFx.tongues(level, t.position(), Math.max(0.35, t.getBbWidth() * 0.6), t.getBbHeight(), 3 + count, BLOOD.primary(), FIRE.primary(), 2, 8);
		ElementFx.drip(level, c, 0.25, 3);
		ElementFx.embers(level, c, 0.3, 3);
		Vfx.emit(level, ParticleTypes.LAVA, c, 1 + count / 2, 0.2, 0.0);
		Fx.sound(level, c, SoundEvents.LAVA_POP, 0.8F, 0.9F + 0.1F * count);
		Fx.sound(level, c, SoundEvents.FIRECHARGE_USE, 0.4F, 1.3F);
	}

	// ------------------------------------------------------------------ Conflagration

	/**
	 * Conflagration: under each target (the first three; the rest a burst of flame) a sun seal and a pillar of flame
	 * three blocks tall, and a ring of fire racing out as far as the flare reaches.
	 */
	static void conflagration(ServerLevel level, List<Vec3> hearts, double radius) {
		int shown = 0;
		for (Vec3 heart : hearts) {
			if (shown++ >= 3) {
				ElementFx.fireImpact(level, heart, 1.0);
				continue;
			}
			Vec3 floor = CastEngine.ground(level, heart);
			ElementFx.flatSigil(level, floor, SigilOption.STAR, FIRE.accent(), 1.6, 24, 0.12);
			ElementFx.flatSigil(level, floor.add(0, 0.01, 0), SigilOption.CIRCLE, FIRE.secondary(), 1.1, 24, -0.08);
			ElementFx.groundRing(level, floor, FIRE.primary(), 0.5, radius, 0.12, 14);
			ElementFx.groundRing(level, floor, FIRE.secondary(), 0.3, radius * 0.8, 0.05, 16);
			ElementFx.tongues(level, floor, 0.8, 3.2, 10, FIRE.primary(), FIRE.secondary(), 3, 12);
			ElementFx.tongues(level, floor, 0.5, 2.4, 6, FIRE.accent(), FIRE.primary(), 2, 10);
			ElementFx.heatFlare(level, heart, 2.0);
			Motes.burst(level, heart, 14, FIRE.secondary(), 0.12, 24, 0.3);
			ElementFx.embers(level, heart, 0.5, 10);
		}
		Vec3 first = hearts.getFirst();
		ScreenFx.shake(level, first, 0.3F, 12);
		Feels.sound(level, first, "fire_blast", 1.0F, 0.8F);
		Feels.sound(level, first, "fire_field", 0.8F, 0.9F);
	}

	/** A burning enemy flares up: an arch of flame leaps to it from the fire's heart, and it goes up in a column of fire. */
	static void flareUp(ServerLevel level, Vec3 heart, LivingEntity t) {
		Vec3 c = centre(t);
		Vec3 d = c.subtract(heart);
		double length = d.length();
		if (length > 0.8) {
			Vec3 across = new Vec3(d.x, 0, d.z);
			Vec3 normal = across.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(-across.z, 0, across.x).normalize();
			Vec3 mid = heart.add(c).scale(0.5).subtract(0, length * 0.2, 0);
			ElementFx.slash(level, mid, normal, UP, FIRE.primary(), length * 0.55, Math.PI * 0.9, 0.1, 3, 8);
			ElementFx.slash(level, mid, normal, UP, FIRE.secondary(), length * 0.55, Math.PI * 0.8, 0.04, 3, 7);
		}
		ElementFx.flameBurst(level, c, 0.6, 3);
		ElementFx.tongues(level, t.position(), Math.max(0.4, t.getBbWidth() * 0.7), t.getBbHeight() + 1.0, 6, FIRE.primary(), FIRE.secondary(), 2, 9);
		ElementFx.heatFlare(level, c, 1.0);
		Feels.sound(level, c, "fire_hop", 0.6F, 1.0F);
	}

	// ------------------------------------------------------------------ stone: block displays, visual only

	/** Every stone display standing now: they all go before the world is saved, and there are never more than {@link #MAX_STONE}. */
	private static final Set<Display.BlockDisplay> STONE = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final int MAX_STONE = 120;
	/** Each creature's Monolith column, so a second one takes the first's place. */
	private static final Map<UUID, Column> COLUMNS = new HashMap<>();
	/** How long a Monolith column stands before it crumbles (4 seconds). */
	static final int MONOLITH_HOLD = 80;

	/** A column: its pieces bottom to top, what each is made of, and its shape (width, height, middle height). */
	private record Column(List<Display.BlockDisplay> parts, BlockState[] states, float[][] shape) {}

	/** Takes every stone display away: the server is stopping. */
	static void clearStone() {
		for (Display.BlockDisplay d : STONE) {
			d.discard();
		}
		STONE.clear();
		COLUMNS.clear();
	}

	private static Display.BlockDisplay stone(ServerLevel level, Vec3 at, BlockState state, Transformation start) {
		STONE.removeIf(Entity::isRemoved);
		if (STONE.size() >= MAX_STONE) {
			return null;
		}
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(at.x, at.y, at.z);
		display.setBlockState(state);
		display.setTransformation(start);
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		STONE.add(display);
		return display;
	}

	private static void tween(Display.BlockDisplay display, Transformation to, int ticks) {
		if (display.isRemoved()) {
			return;
		}
		display.setTransformationInterpolationDelay(0);
		display.setTransformationInterpolationDuration(ticks);
		display.setTransformation(to);
	}

	private static void drop(Display.BlockDisplay display) {
		STONE.remove(display);
		display.discard();
	}

	/** A block {@code w} x {@code h} x {@code d} turned by {@code rot} about its own middle, which sits at {@code centre} (from the display). */
	private static Transformation box(Vector3f centre, Quaternionf rot, float w, float h, float d) {
		Vector3f half = rot.transform(new Vector3f(w / 2, h / 2, d / 2));
		return new Transformation(new Vector3f(centre).sub(half), new Quaternionf(rot), new Vector3f(w, h, d), new Quaternionf());
	}

	/** {@code state} if it's drawn as a plain block (not a chest, water or air), else {@code fallback}. */
	private static BlockState solid(BlockState state, BlockState fallback) {
		return state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity() ? state : fallback;
	}

	// ------------------------------------------------------------------ Monolith

	/**
	 * Monolith: the ground cracks and throws up stone, gold seams of earth-light race up where the column's edges
	 * will be, rings of gold and brown open at its top and middle, and (when there's ground to stand on and room
	 * for more stone) the column itself bursts up.
	 */
	static void monolith(ServerLevel level, LivingEntity t, Vec3 base, double width, boolean column) {
		ElementFx.crack(level, base, width * 1.5, 24);
		ElementFx.stoneShards(level, base.add(0, 0.3, 0), Blocks.STONE.defaultBlockState(), 12, 0.3);
		ElementFx.earthImpact(level, base.add(0, 0.4, 0), 1.2);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, ElementFx.groundBlock(level, base)), base.add(0, 0.2, 0), 10, width * 0.4, 0.1);
		double top = 2.45;
		double hw = width / 2 + 0.03;
		for (int i = 0; i < 4; i++) {
			double x = (i % 2 == 0 ? 1 : -1) * hw;
			double z = (i < 2 ? 1 : -1) * hw;
			Light.ray(level, base.add(x, 0.3, z), base.add(x, top, z), EARTH.secondary(), 0.035, 12);
		}
		Scheduler.later(3, () -> {
			ElementFx.ring(level, base.add(0, top + 0.05, 0), UP, EARTH.secondary(), width * 0.9, width * 1.6, 0.06, 8);
			ElementFx.ring(level, base.add(0, 1.25, 0), UP, EARTH.primary(), width * 0.7, width * 1.25, 0.05, 8);
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()), base.add(0, top, 0), 8, width * 0.3, 0.1);
		});
		if (column) {
			column(level, t.getUUID(), base, (float) width);
		}
		ScreenFx.shake(level, base, 0.35F, 10);
		Fx.sound(level, base, SoundEvents.MACE_SMASH_GROUND_HEAVY, 0.9F, 0.8F);
		Fx.sound(level, base, SoundEvents.STONE_BREAK, 1.0F, 0.6F);
		Fx.sound(level, base, WildercordSounds.impact("earth"), 0.9F, 1.0F);
	}

	/**
	 * The column: a plinth of the ground itself, three drums of stone brick (the middle one carved) narrowing a
	 * little as they go up, and a smooth capital, 2.4 blocks in all. It starts sunk in the ground and bursts up in
	 * three ticks, stands {@link #MONOLITH_HOLD} ticks, then crumbles from the top down.
	 */
	private static void column(ServerLevel level, UUID owner, Vec3 base, float w) {
		Column old = COLUMNS.remove(owner);
		if (old != null) {
			old.parts().forEach(FusedFlameVfx::drop);
		}
		BlockState[] states = {
			solid(ElementFx.groundBlock(level, base), Blocks.COBBLESTONE.defaultBlockState()),
			Blocks.STONE_BRICKS.defaultBlockState(),
			Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
			Blocks.STONE_BRICKS.defaultBlockState(),
			Blocks.SMOOTH_STONE.defaultBlockState()};
		float[][] shape = {
			{w + 0.45F, 0.34F, 0.12F},
			{w, 0.62F, 0.60F},
			{w - 0.04F, 0.62F, 1.22F},
			{w - 0.08F, 0.62F, 1.84F},
			{w + 0.24F, 0.28F, 2.29F}};
		float sink = 2.7F;
		List<Display.BlockDisplay> parts = new ArrayList<>();
		for (int i = 0; i < states.length; i++) {
			Display.BlockDisplay d = stone(level, base, states[i], box(new Vector3f(0, shape[i][2] - sink, 0), new Quaternionf(), shape[i][0], shape[i][1], shape[i][0]));
			if (d == null) {
				break;
			}
			parts.add(d);
		}
		if (parts.isEmpty()) {
			return;
		}
		Column column = new Column(parts, states, shape);
		COLUMNS.put(owner, column);
		Scheduler.later(1, () -> {
			for (int i = 0; i < parts.size(); i++) {
				tween(parts.get(i), box(new Vector3f(0, shape[i][2], 0), new Quaternionf(), shape[i][0], shape[i][1], shape[i][0]), 3);
			}
		});
		Scheduler.later(4 + MONOLITH_HOLD, () -> crumble(level, owner, column, base));
	}

	/** The column crumbles from the capital down, each piece tipping and dropping in a spill of its own stone, then sinks away. */
	private static void crumble(ServerLevel level, UUID owner, Column column, Vec3 base) {
		COLUMNS.remove(owner, column);
		List<Display.BlockDisplay> parts = column.parts();
		if (parts.stream().allMatch(Entity::isRemoved)) {
			parts.forEach(STONE::remove);
			return;
		}
		Fx.sound(level, base, SoundEvents.STONE_BREAK, 1.0F, 0.7F);
		Fx.sound(level, base, SoundEvents.GRAVEL_BREAK, 0.9F, 0.6F);
		int n = parts.size();
		for (int i = n - 1; i >= 0; i--) {
			Display.BlockDisplay d = parts.get(i);
			float[] s = column.shape()[i];
			BlockState state = column.states()[i];
			float drop = i == 0 ? 0.35F : 0.7F;
			Scheduler.later(1 + (n - 1 - i) * 3, () -> {
				if (d.isRemoved()) {
					return;
				}
				RandomSource r = level.getRandom();
				Quaternionf tilt = new Quaternionf().rotateY(r.nextFloat() * 6.28F).rotateX(0.3F + r.nextFloat() * 0.35F);
				tween(d, box(new Vector3f(0, s[2] - drop, 0), tilt, s[0] * 0.85F, s[1] * 0.85F, s[0] * 0.85F), 6);
				Vec3 at = base.add(0, s[2], 0);
				Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at, 10, s[0] * 0.4, 0.05);
				Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, state), at, 4, s[0] * 0.4, 0.0);
				Fx.sound(level, at, SoundEvents.STONE_HIT, 0.6F, 0.8F);
			});
		}
		int sunk = n * 3 + 6;
		Scheduler.later(sunk, () -> {
			for (int i = 0; i < n; i++) {
				float[] s = column.shape()[i];
				tween(parts.get(i), box(new Vector3f(0, s[2] - 2.9F, 0), new Quaternionf(), s[0] * 0.7F, s[1] * 0.7F, s[0] * 0.7F), 8);
			}
			ElementFx.crack(level, base, column.shape()[0][0] * 1.2, 14);
		});
		Scheduler.later(sunk + 9, () -> parts.forEach(FusedFlameVfx::drop));
	}

	// ------------------------------------------------------------------ Magnetize

	/**
	 * Magnetize: a lodestone seal of earth and storm under the target, a ring racing out as far as it pulls and a gold
	 * one falling back in, a crack of lightning, and the first field lines.
	 */
	static void magnetize(ServerLevel level, LivingEntity t, double radius) {
		Vec3 feet = t.position();
		Vec3 c = centre(t);
		ElementFx.flatSigil(level, feet, SigilOption.CIRCLE, EARTH.secondary(), Math.max(0.8, t.getBbWidth() + 0.4), 24, 0.14);
		ElementFx.flatSigil(level, feet.add(0, 0.01, 0), SigilOption.RING, STORM.primary(), Math.max(1.1, t.getBbWidth() + 0.8), 24, -0.1);
		ElementFx.groundRing(level, feet, STORM.primary(), 0.4, radius, 0.05, 10);
		ElementFx.groundRing(level, feet, EARTH.secondary(), radius, 0.5, 0.06, 14);
		ElementFx.stormImpact(level, c, 0.8);
		magnetField(level, t, 0);
		Fx.sound(level, feet, SoundEvents.LODESTONE_PLACE, 1.0F, 0.6F);
		Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4F, 1.8F);
		Fx.sound(level, c, WildercordSounds.impact("storm"), 0.6F, 1.0F);
	}

	/** Field lines: four loops of light arching out from the target's head round to its feet, turning slowly, gold and yellow; iron filings drawn in. */
	static void magnetField(ServerLevel level, LivingEntity t, int age) {
		Vec3 c = centre(t);
		double r = Math.max(0.65, t.getBbHeight() * 0.5);
		for (int i = 0; i < 4; i++) {
			double a = age * 0.12 + i * Math.PI / 2;
			Vec3 out = ElementFx.flatDir(a);
			Vec3 normal = new Vec3(-out.z, 0, out.x);
			ElementFx.slash(level, c.add(out.scale(0.15)), normal, out, i % 2 == 0 ? EARTH.secondary() : STORM.primary(), r, Math.PI * 1.15, 0.04, 3, 10);
		}
		RandomSource rr = level.getRandom();
		ItemParticleOption iron = new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET);
		for (int i = 0; i < 3; i++) {
			Vec3 from = c.add(ElementFx.randomDir(rr).scale(1.6));
			Vfx.fling(level, iron, from, c.subtract(from).normalize(), 0.18);
		}
		ElementFx.sparks(level, c, 2, 0.15);
	}

	/** Something drawn in: a thread of gold light to the magnet, and grit dragged off the ground at its feet. */
	static void magnetPull(ServerLevel level, LivingEntity magnet, LivingEntity other) {
		ElementFx.ray(level, centre(other), centre(magnet), EARTH.secondary(), 0.025, 5);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, ElementFx.groundBlock(level, other.position())), other.position().add(0, 0.1, 0), 2, 0.2, 0.05);
	}

	/** Something touching the magnet is shocked: lightning between them and a clang of lodestone. */
	static void magnetShock(ServerLevel level, LivingEntity magnet, LivingEntity other) {
		Vec3 a = centre(magnet);
		Vec3 b = centre(other);
		ElementFx.bolt(level, a, b, 0.05, 1, 2);
		ElementFx.stormImpact(level, a.add(b).scale(0.5), 0.6);
		Fx.sound(level, b, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.6F);
		Fx.sound(level, b, SoundEvents.LODESTONE_HIT, 0.8F, 0.8F);
	}

	/** The magnet lets go: its field falls in and sparks scatter. */
	static void magnetizeEnd(ServerLevel level, LivingEntity t) {
		ElementFx.groundRing(level, t.position(), EARTH.secondary(), Math.max(1.0, t.getBbWidth() + 0.6), 0.2, 0.04, 10);
		ElementFx.sparks(level, centre(t), 4, 0.2);
		Fx.sound(level, t.position(), SoundEvents.LODESTONE_BREAK, 0.6F, 1.2F);
	}

	/** Too many magnets: a few sparks. */
	static void magnetFizzle(ServerLevel level, LivingEntity t) {
		ElementFx.sparks(level, centre(t), 4, 0.15);
		Fx.sound(level, centre(t), SoundEvents.LODESTONE_HIT, 0.5F, 1.4F);
	}

	// ------------------------------------------------------------------ Sinkhole

	/**
	 * The ground gives way: it cracks across, a hole of darkness opens and widens in three steps, dust and chips of the
	 * ground slide in from the edge, darkness falls in, and slabs of the ground itself tilt into the hole.
	 */
	static void sinkholeOpen(ServerLevel level, Vec3 centre, double radius, int ticks) {
		int life = ticks + 14;
		BlockState ground = ElementFx.groundBlock(level, centre);
		ElementFx.flatSigil(level, centre, SigilOption.CRACKED, EARTH.secondary(), radius * 1.1, life, 0.0);
		for (int i = 0; i < 3; i++) {
			double size = radius * (0.25 + 0.15 * i);
			int wait = i * 3;
			Scheduler.later(1 + wait, () -> ElementFx.flatSigil(level, centre.add(0, 0.012, 0), SigilOption.CIRCLE, DARK, size, life - wait, 0.02));
		}
		ElementFx.ring(level, centre.add(0, 0.1, 0), UP, EARTH.primary(), radius * 1.1, 0.2, 0.12, 12);
		ElementFx.implode(level, centre.add(0, 0.3, 0), radius, 12);
		BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, ground);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 14; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			Vec3 p = centre.add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius);
			Vfx.fling(level, chip, p, new Vec3(-Math.cos(a), -0.2, -Math.sin(a)), 0.25 + r.nextDouble() * 0.1);
		}
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground), centre.add(0, 0.1, 0), 12, radius * 0.5, 0.05);
		slabs(level, centre, radius, solid(ground, Blocks.DIRT.defaultBlockState()), ticks);
		ScreenFx.shake(level, centre, 0.3F, 12);
		Fx.sound(level, centre, SoundEvents.GRAVEL_BREAK, 1.0F, 0.5F);
		Fx.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.5F);
		Fx.sound(level, centre, WildercordSounds.cast("void"), 0.7F, 0.5F);
	}

	/**
	 * Six slabs of the ground round the hole's edge (block displays of the ground's own block): they tilt in as it
	 * opens, slam down into it as it crushes, and sink away.
	 */
	private static void slabs(ServerLevel level, Vec3 centre, double radius, BlockState ground, int ticks) {
		int count = 6;
		double ring = radius * 0.72;
		float w = (float) Math.min(1.5, 2 * Math.PI * ring / count * 0.8);
		float d = (float) Math.min(1.2, radius * 0.45);
		float h = 0.32F;
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = phase + i * Math.PI * 2 / count;
			Vec3 at = centre.add(Math.cos(a) * ring, 0, Math.sin(a) * ring);
			// Turned so its own +Z points at the middle; tipping about its own X dips that inner edge.
			float yaw = (float) Math.atan2(-Math.cos(a), -Math.sin(a));
			Display.BlockDisplay slab = stone(level, at, ground, box(new Vector3f(0, -h / 2 + 0.01F, 0), new Quaternionf().rotateY(yaw), w, h, d));
			if (slab == null) {
				return;
			}
			Vector3f in = new Vector3f((float) -Math.cos(a), 0, (float) -Math.sin(a));
			Quaternionf tilt = new Quaternionf().rotateY(yaw).rotateX(0.42F);
			Quaternionf steep = new Quaternionf().rotateY(yaw).rotateX(0.95F);
			Scheduler.later(1 + i % 2, () -> tween(slab, box(new Vector3f(in).mul(0.1F).add(0, -0.3F, 0), tilt, w, h, d), 6));
			Scheduler.later(ticks + 1, () -> tween(slab, box(new Vector3f(in).mul((float) (ring * 0.45)).add(0, -0.8F, 0), steep, w, h, d), 3));
			Scheduler.later(ticks + 6, () -> tween(slab, box(new Vector3f(in).mul((float) (ring * 0.55)).add(0, -1.8F, 0), steep, w * 0.8F, h, d * 0.8F), 8));
			Scheduler.later(ticks + 15, () -> drop(slab));
		}
	}

	/** While it's open: rings of dust and darkness falling in, grit falling from the edge, and void drawn down. */
	static void sinkhole(ServerLevel level, Vec3 centre, double radius, int age) {
		ElementFx.ring(level, centre.add(0, 0.1, 0), UP, EARTH.primary(), radius, 0.3, 0.07, 9);
		ElementFx.ring(level, centre.add(0, 0.14, 0), UP, DARK, radius * 0.6, 0.15, 0.1, 9);
		Vfx.emit(level, ParticleTypes.PORTAL, centre.add(0, 0.2, 0), 4, 0.1, radius * 0.4);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, ElementFx.groundBlock(level, centre)), centre.add(0, 0.6, 0), 4, radius * 0.5, 0.0);
		if (age % 12 == 0) {
			Fx.sound(level, centre, SoundEvents.SAND_FALL, 0.6F, 0.5F);
		}
	}

	/** Pinned: a ring of darkness closing on the creature's feet and grit clinging to it. */
	static void sinkholePinned(ServerLevel level, LivingEntity t) {
		Vec3 feet = t.position();
		ElementFx.groundRing(level, feet, DARK, Math.max(0.7, t.getBbWidth() + 0.3), 0.1, 0.1, 12);
		ElementFx.stoneShards(level, feet.add(0, 0.2, 0), ElementFx.groundBlock(level, feet), 5, 0.12);
	}

	/** Cast again where its ground has already given way: a rumble and falling grit. */
	static void sinkholeRumble(ServerLevel level, Vec3 centre, double radius) {
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, ElementFx.groundBlock(level, centre)), centre.add(0, 0.5, 0), 6, radius * 0.4, 0.0);
		Fx.sound(level, centre, SoundEvents.GRAVEL_BREAK, 0.6F, 0.5F);
	}

	/** The crush: earth and void slamming together, the ground cracking out, stone thrown up and a roll of dust. */
	static void sinkholeCrush(ServerLevel level, Vec3 centre, double radius) {
		Vec3 mid = centre.add(0, 0.4, 0);
		ElementFx.earthImpact(level, mid, 1.6);
		ElementFx.voidImpact(level, mid, 1.2);
		ElementFx.crack(level, centre, radius, 20);
		ElementFx.stoneShards(level, mid, ElementFx.groundBlock(level, centre), 16, 0.35);
		Motes.clouds(level, centre.add(0, 0.3, 0), 5, radius * 0.5, DUST, 1.3, 40, new Vec3(0, 0.02, 0), 0.03, 0.45);
		ScreenFx.shake(level, centre, 0.5F, 14);
		Fx.sound(level, centre, SoundEvents.ANVIL_LAND, 0.5F, 0.5F);
		Fx.sound(level, centre, SoundEvents.DEEPSLATE_BREAK, 1.0F, 0.5F);
		Fx.sound(level, centre, WildercordSounds.impact("earth"), 1.0F, 0.5F);
	}

	/** A creature crushed: stone breaking over it. */
	static void sinkholeCrushed(ServerLevel level, LivingEntity t) {
		ElementFx.stoneShards(level, centre(t), ElementFx.groundBlock(level, t.position()), 6, 0.2);
		Sigils.flash(level, centre(t), WHITE, 0.6F);
	}
}
