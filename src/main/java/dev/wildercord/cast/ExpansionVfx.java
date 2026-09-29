package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Visuals for the batch 6 runes: sparks, energy balls and beams drawn in shaped light (see
 * {@link Light}), and the new protection, mining and element spells. Like {@link Vfx}, particles
 * go out through {@link Fx} (never in front of a player's own eyes) and magic circles through
 * {@link Sigils}.
 */
final class ExpansionVfx {
	private ExpansionVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int WHITE = 0xFFFFFF;
	private static final int FIRE = 0xF06E32;
	private static final int FROST = 0x8CDCFF;
	private static final int STORM = 0xFFE650;
	private static final int EARTH = 0xB48C5A;
	private static final int LIFE = 0x6EDC64;
	private static final int VOID = 0xB45AF0;
	private static final int TIME = 0xF2D98A;
	private static final int BLOOD = 0xD2283C;

	private static void dot(ServerLevel level, net.minecraft.core.particles.ParticleOptions p, Vec3 at) {
		Vfx.emit(level, p, at, 1, 0.0, 0.0);
	}

	private static void glow(ServerLevel level, int color, Vec3 at, double size) {
		Vfx.emit(level, SigilOption.glow(color, (float) size), at, 1, 0.0, 0.0);
	}

	/** A unit vector across {@code dir}, level with the ground where it can be. */
	private static Vec3 across(Vec3 dir) {
		Vec3 side = dir.cross(UP);
		return side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
	}

	private static Vec3 unit(Vec3 v) {
		return v.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : v.normalize();
	}

	private static Vec3 normalOf(Direction face) {
		return Vec3.atLowerCornerOf(face.getUnitVec3i());
	}

	/** A sphere of light: three shockwave rings on their own tilts racing out together. */
	private static void shell(ServerLevel level, Vec3 at, int color, int secondary, double from, double to, double width, int lifetime) {
		double spin = level.getRandom().nextDouble() * Math.PI;
		for (int i = 0; i < 3; i++) {
			double a = spin + i * Math.PI / 3;
			Vec3 normal = new Vec3(Math.cos(a) * 0.85, 0.5, Math.sin(a) * 0.85).normalize();
			Light.ring(level, at, normal, i == 1 ? secondary : color, from, to, width, lifetime);
		}
	}

	// ------------------------------------------------------------------ Spark

	static void sparkLaunch(ServerLevel level, Vec3 origin, Vec3 aim, Vfx.Theme theme) {
		glow(level, theme.primary(), origin.add(aim.scale(0.6)), 0.5);
		Fx.sound(level, origin, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 1.8F);
		// (the element's cast sound already played once, with the cast circle)
	}

	/** A spark in flight: a bright mote and a thin streak of light behind it. */
	static void sparkTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int tick) {
		glow(level, theme.primary(), to, 0.55);
		Light.ray(level, from, to, theme.primary(), 0.05, 4);
		Light.ray(level, from.lerp(to, 0.5), to, WHITE, 0.02, 3);
		if (tick % 2 == 0) {
			Vfx.emit(level, theme.spark(), to, 1, 0.05, 0.02);
		}
	}

	static void sparkHit(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 0.9F);
		Light.ring(level, at, UP, theme.primary(), 0.1, 0.75, 0.03, 6);
		Vfx.radial(level, theme.spark(), at, 5, 0.12);
		Fx.sound(level, at, theme.impact(), 0.4F, 1.0F);
	}

	// ------------------------------------------------------------------ Ray

	/** A short ray: a thin beam with a white core, fired through a small circle at the hand. */
	static void ray(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme) {
		Vec3 dir = unit(to.subtract(from));
		Light.ray(level, from, to, theme.primary(), 0.1, 8);
		Light.ray(level, from, to, WHITE, 0.035, 6);
		Sigils.layer(level, from, dir, SigilOption.CIRCLE, theme.primary(), 0.28F, 8, 0.3F);
		Sigils.layer(level, from.add(dir.scale(0.03)), dir, SigilOption.RING, theme.secondary(), 0.38F, 8, -0.35F);
		Fx.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 0.45F, 2.0F);
		Fx.sound(level, from, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.7F);
	}

	static void rayHit(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 1.0F);
		Light.ring(level, at, UP, theme.secondary(), 0.1, 0.9, 0.035, 7);
		Vfx.radial(level, theme.spark(), at, 6, 0.15);
		Fx.sound(level, at, theme.impact(), 0.5F, 1.0F);
	}

	// ------------------------------------------------------------------ Nova

	/** Nova: a flare at your heart, a shell of light racing out, and a star burned into the ground. */
	static void nova(ServerLevel level, Vec3 center, double radius, Vfx.Theme theme) {
		Sigils.flash(level, center, theme.primary(), (float) Math.min(4.5, radius * 1.3));
		shell(level, center, theme.primary(), theme.secondary(), 0.2, radius, 0.06, 8);
		Vec3 feet = CastEngine.ground(level, center);
		Light.groundRing(level, feet, theme.primary(), 0.3, radius * 1.1, 0.09, 10);
		Sigils.send(level, SigilOption.flat(SigilOption.STAR, theme.secondary(), (float) (radius * 0.7), 12, 0.25F), feet.add(0, 0.07, 0));
		Vfx.radial(level, theme.mote(), center, 14, 0.2);
		Fx.sound(level, center, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.3F);
		Fx.sound(level, center, theme.impact(), 0.6F, 1.0F);
	}

	// ------------------------------------------------------------------ Wisp

	static void wispRelease(ServerLevel level, Vec3 origin, Vfx.Theme theme) {
		Light.orb(level, origin, theme.primary(), 0.14, 8);
		Fx.sound(level, origin, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 0.6F, 1.6F);
	}

	/** A wisp in flight: a small orb of light with a mote circling it and a faint trail. */
	static void wispTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int tick) {
		if (tick % 2 == 0) {
			Light.orb(level, to, theme.primary(), 0.14, 3);
		}
		glow(level, theme.secondary(), to, 0.5);
		Vec3 dir = unit(to.subtract(from));
		Vec3 side = across(dir);
		Vec3 up = side.cross(dir).normalize();
		double a = tick * 0.9;
		dot(level, new DustParticleOptions(theme.secondary(), 0.7F), to.add(side.scale(Math.cos(a) * 0.28)).add(up.scale(Math.sin(a) * 0.28)));
		Light.ray(level, from, to, theme.primary(), 0.03, 6);
		if (tick % 4 == 0) {
			Vfx.emit(level, theme.mote(), to, 1, 0.05, 0.0);
		}
	}

	/** The wisp finds its prey: a faint thread of light to it, and a chime. */
	static void wispSeek(ServerLevel level, Vec3 wisp, Entity prey, Vfx.Theme theme) {
		Light.ray(level, wisp, prey.getBoundingBox().getCenter(), theme.secondary(), 0.015, 5);
		Fx.sound(level, wisp, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 2.0F);
	}

	static void wispStrike(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 1.2F);
		Light.ring(level, at, UP, theme.primary(), 0.1, 1.0, 0.04, 7);
		Light.ring(level, at, new Vec3(0.7, 0.3, 0.6).normalize(), theme.secondary(), 0.1, 0.7, 0.03, 6);
		Vfx.radial(level, theme.spark(), at, 8, 0.15);
		Fx.sound(level, at, theme.impact(), 0.6F, 1.0F);
	}

	static void wispFade(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Light.ring(level, at, UP, theme.secondary(), 0.5, 0.05, 0.03, 8);
		Vfx.radial(level, theme.mote(), at, 6, 0.05);
	}

	// ------------------------------------------------------------------ Comet

	static void cometLaunch(ServerLevel level, Vec3 origin, Vec3 aim, Vfx.Theme theme) {
		Sigils.layer(level, origin, aim, SigilOption.CIRCLE, theme.primary(), 0.5F, 10, 0.25F);
		Sigils.layer(level, origin.add(aim.scale(0.03)), aim, SigilOption.RING, theme.secondary(), 0.7F, 10, -0.3F);
		Fx.sound(level, origin, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 0.8F);
		// (the element's cast sound already played once, with the cast circle)
	}

	/** A comet in flight: a heavy orb of light wrapped in rings, burning a thick streak behind it. */
	static void cometTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int tick) {
		Light.orb(level, to, theme.primary(), 0.32, 3);
		glow(level, theme.secondary(), to, 1.2);
		Light.ray(level, from.subtract(to.subtract(from)), to, theme.primary(), 0.2, 5);
		if (tick % 2 == 0) {
			Vfx.fling(level, theme.spark(), to, from.subtract(to), 0.15);
			Vfx.emit(level, theme.mote(), to, 1, 0.15, 0.01);
		}
		if (tick % 8 == 0) {
			Fx.sound(level, to, SoundEvents.BEACON_AMBIENT, 0.5F, 1.8F);
		}
	}

	/** A comet bursts: a big flare, a shell of light, a shockwave along the ground and sparks. */
	static void cometBurst(ServerLevel level, Vec3 at, double radius, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), (float) Math.min(6, radius * 1.5));
		shell(level, at, theme.primary(), theme.secondary(), 0.3, radius, 0.08, 10);
		Light.groundRing(level, CastEngine.ground(level, at), theme.primary(), 0.4, radius * 1.15, 0.1, 12);
		Vfx.radial(level, theme.spark(), at, 14, 0.35);
		Vfx.radial(level, theme.mote(), at, 18, 0.2);
		Vfx.emit(level, theme.sparkle(), at, 6, radius * 0.3, 0.0);
		Fx.sound(level, at, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.5F);
		Fx.sound(level, at, theme.impact(), 0.8F, 1.0F);
	}

	// ------------------------------------------------------------------ Ricochet

	static void ricochetLaunch(ServerLevel level, Vec3 origin, Vfx.Theme theme) {
		Fx.sound(level, origin, SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.2F);
		// (the element's cast sound already played once, with the cast circle)
	}

	static void ricochetTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int tick) {
		if (tick % 2 == 0) {
			Light.orb(level, to, theme.primary(), 0.18, 3);
		}
		glow(level, theme.primary(), to, 0.6);
		Light.ray(level, from, to, theme.secondary(), 0.07, 5);
	}

	/** A bounce: a ring of light flat against the surface, a flare and a bright tick. */
	static void ricochetBounce(ServerLevel level, Vec3 at, Vec3 normal, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 0.8F);
		Light.ring(level, at.add(normal.scale(0.05)), normal, theme.primary(), 0.1, 1.0, 0.05, 7);
		Vfx.radial(level, theme.spark(), at, 4, 0.12);
		Fx.sound(level, at, SoundEvents.SLIME_BLOCK_HIT, 0.6F, 1.6F);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.7F, 1.5F);
	}

	static void ricochetHit(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 1.0F);
		Vfx.radial(level, theme.spark(), at, 6, 0.15);
		Fx.sound(level, at, theme.impact(), 0.5F, 1.0F);
	}

	static void ricochetEnd(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 1.2F);
		Light.ring(level, at, UP, theme.secondary(), 0.8, 0.05, 0.04, 8);
		Vfx.radial(level, theme.mote(), at, 8, 0.1);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6F, 1.6F);
	}

	// ------------------------------------------------------------------ Cluster

	/** A cluster in flight: an orb with five shards of light turning inside it. */
	static void clusterTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int tick) {
		Light.orb(level, to, theme.primary(), 0.26, 3);
		Light.ray(level, from, to, theme.primary(), 0.1, 4);
		Vec3 dir = unit(to.subtract(from));
		Vec3 side = across(dir);
		Vec3 up = side.cross(dir).normalize();
		for (int i = 0; i < 5; i++) {
			double a = tick * 0.6 + Math.PI * 2 * i / 5;
			dot(level, new DustParticleOptions(theme.secondary(), 0.6F), to.add(side.scale(Math.cos(a) * 0.3)).add(up.scale(Math.sin(a) * 0.3)));
		}
	}

	/** The cluster breaks: a flare and a crack of light. */
	static void clusterBreak(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), 1.8F);
		shell(level, at, theme.primary(), theme.secondary(), 0.2, 1.4, 0.05, 7);
		Vfx.radial(level, theme.spark(), at, 10, 0.25);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.1F);
		Fx.sound(level, at, SoundEvents.GLASS_BREAK, 0.5F, 1.5F);
	}

	static void shardTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme) {
		glow(level, theme.primary(), to, 0.4);
		Light.ray(level, from, to, theme.primary(), 0.04, 3);
	}

	static void shardLand(ServerLevel level, Vec3 at, double radius, Vfx.Theme theme) {
		Sigils.flash(level, at, theme.primary(), (float) (radius * 0.9));
		Light.groundRing(level, at, theme.secondary(), 0.1, radius, 0.05, 8);
		Vfx.radial(level, theme.spark(), at, 4, 0.12);
		Fx.sound(level, at, theme.impact(), 0.35F, 1.0F);
	}

	// ------------------------------------------------------------------ Lance

	/** Lance: a thick beam with a white-hot core, a triple circle at the hand, rings running down it. */
	static void lance(ServerLevel level, Vec3 from, Vec3 to, double width, Vfx.Theme theme) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		Vec3 dir = unit(delta);
		Light.ray(level, from, to, theme.primary(), width * 0.55, 14);
		Light.ray(level, from, to, WHITE, width * 0.18, 10);
		Sigils.layer(level, from, dir, SigilOption.CIRCLE, theme.primary(), 0.55F, 14, 0.3F);
		Sigils.layer(level, from.add(dir.scale(0.03)), dir, SigilOption.RING, theme.secondary(), 0.78F, 14, -0.35F);
		Sigils.layer(level, from.add(dir.scale(0.06)), dir, SigilOption.STAR, theme.secondary(), 0.36F, 14, 0.5F);
		for (double d = 1.6; d < length - 0.4; d += 1.6) {
			Vec3 p = from.add(dir.scale(d));
			Scheduler.later(1 + (int) (d / 6), () -> Light.ring(level, p, dir, theme.secondary(), width * 1.5, width * 0.5, 0.04, 8));
		}
		Sigils.flash(level, to, theme.primary(), 1.6F);
		Light.ring(level, to, dir, theme.primary(), 0.2, 1.4, 0.06, 9);
		Fx.sound(level, from, SoundEvents.TRIDENT_THROW, 0.8F, 0.7F);
		Fx.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.3F);
	}

	// ------------------------------------------------------------------ Sweep

	/** A Sweep begins: a circle at the hand, and an arc of light tracing the path the beam will swing. */
	static void sweepStart(ServerLevel level, Vec3 origin, Vec3 base, double length, int ticks, Vfx.Theme theme) {
		Vec3 flat = new Vec3(base.x, 0, base.z);
		Vec3 toward = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Sigils.layer(level, origin.add(base.scale(0.8)), base, SigilOption.CIRCLE, theme.primary(), 0.3F, 8, 0.3F);
		Light.slash(level, origin, UP, toward, theme.secondary(), length * 0.96, Math.toRadians(100), 0.18, ticks, ticks + 6);
		Fx.sound(level, origin, SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.5F);
		Fx.sound(level, origin, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 0.6F);
	}

	static void sweepTick(ServerLevel level, Vec3 from, Vec3 to, Vfx.Theme theme, int step) {
		Light.ray(level, from, to, theme.primary(), 0.12, 3);
		Light.ray(level, from, to, WHITE, 0.04, 2);
		glow(level, theme.primary(), to, 0.8);
		if (step % 2 == 0) {
			Vfx.emit(level, theme.spark(), to, 1, 0.1, 0.05);
		}
	}

	// ------------------------------------------------------------------ Prism

	/**
	 * Prism: the beam to where it split, a turning star there, and three rays fanning on (in the
	 * spell's two colours and white). With no rays, it simply landed.
	 */
	static void prism(ServerLevel level, Vec3 from, Vec3 split, Vec3 aim, List<Vec3> ends, Vfx.Theme theme) {
		Light.ray(level, from, split, theme.primary(), 0.14, 10);
		Light.ray(level, from, split, WHITE, 0.05, 8);
		Sigils.layer(level, from, aim, SigilOption.CIRCLE, theme.primary(), 0.36F, 10, 0.3F);
		Fx.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 0.5F, 1.8F);
		if (ends.isEmpty()) {
			Sigils.flash(level, split, theme.primary(), 1.2F);
			return;
		}
		Sigils.layer(level, split, aim, SigilOption.STAR, theme.secondary(), 0.55F, 14, 0.45F);
		Sigils.flash(level, split, WHITE, 1.4F);
		int[] colors = {theme.primary(), WHITE, theme.secondary()};
		for (int i = 0; i < ends.size(); i++) {
			Vec3 end = ends.get(i);
			int color = colors[i % colors.length];
			Scheduler.later(1, () -> {
				Light.ray(level, split, end, color, 0.08, 10);
				Sigils.flash(level, end, color, 0.9F);
			});
		}
		Fx.sound(level, split, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.7F);
	}

	// ------------------------------------------------------------------ Stream

	static void streamStart(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Fx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.9F);
		// (the element's cast sound already played once, with the cast circle)
	}

	/** One tick of a Stream: the beam as it is now, thicker on a strike, and a spray where it lands. */
	static void streamTick(ServerLevel level, Vec3 from, Vec3 to, Vec3 aim, Vfx.Theme theme, int tick, boolean strike) {
		Light.ray(level, from, to, theme.primary(), strike ? 0.15 : 0.09, 3);
		if (strike) {
			Light.ray(level, from, to, WHITE, 0.05, 2);
			Sigils.flash(level, to, theme.primary(), 0.9F);
			Vfx.radial(level, theme.spark(), to, 3, 0.12);
			Fx.sound(level, to, theme.impact(), 0.3F, 1.0F);
		}
		// The circle only as it opens: held in the middle of the view for a whole second, it would hide the target.
		if (tick == 0) {
			Sigils.layer(level, from, aim, SigilOption.CIRCLE, theme.primary(), 0.26F, 8, 0.4F);
		}
		if (tick % 6 == 0) {
			Fx.sound(level, from, SoundEvents.BEACON_AMBIENT, 0.4F, 2.0F);
		}
	}

	// ------------------------------------------------------------------ protection

	/** Barrier: bands of light closing in around the body and a hexagram under the feet. */
	static void barrier(ServerLevel level, Entity t, Vfx.Theme theme) {
		Vec3 feet = t.position();
		double r = Math.max(0.6, t.getBbWidth() * 0.9);
		double h = t.getBbHeight();
		for (int i = 0; i < 3; i++) {
			double y = 0.1 + i * h * 0.27;
			int k = i;
			Scheduler.later(1 + i, () -> Light.ring(level, feet.add(0, y, 0), UP, k == 1 ? theme.secondary() : theme.primary(), r + 0.8, r, 0.04, 14));
		}
		Sigils.send(level, SigilOption.flat(SigilOption.RING, theme.primary(), (float) (r + 0.3), 20, 0.1F), feet.add(0, 0.07, 0));
		Sigils.flash(level, t.getBoundingBox().getCenter(), theme.primary(), 1.2F);
		dev.wildercord.cast.feel.Feels.sound(level, feet, "arcane_hex", 0.8F, 1.0F);
	}

	/** Brace: a ring slams down around the feet and a shockwave runs out from it. */
	static void brace(ServerLevel level, Entity t, Vfx.Theme theme) {
		Vec3 feet = t.position();
		double r = Math.max(0.6, t.getBbWidth());
		Light.ring(level, feet.add(0, 1.2, 0), UP, theme.primary(), r + 0.4, r, 0.08, 6);
		Scheduler.later(2, () -> {
			Light.groundRing(level, feet, theme.primary(), r, r + 1.6, 0.1, 9);
			Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), feet.add(0, 0.2, 0), 10, 0.15);
		});
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, theme.secondary(), (float) (r + 0.2), 40, 0.0F), feet.add(0, 0.07, 0));
		dev.wildercord.cast.feel.Feels.sound(level, feet, "earth_clamp", 0.9F, 1.26F);
	}

	/** Braced too recently: a dull clink. */
	static void braceSpent(ServerLevel level, Entity t) {
		Vfx.emit(level, ParticleTypes.SMOKE, t.position().add(0, 0.3, 0), 4, 0.25, 0.01);
		Fx.sound(level, t.position(), SoundEvents.STONE_HIT, 0.6F, 0.7F);
	}

	/** Anchor: a circle under the feet and chains of light running from it up to the waist. */
	static void anchor(ServerLevel level, Entity t, Vfx.Theme theme) {
		Vec3 feet = t.position();
		double r = Math.max(0.7, t.getBbWidth() + 0.3);
		Sigils.ground(level, feet, theme.primary(), theme.secondary(), (float) r, 30);
		Vec3 waist = feet.add(0, t.getBbHeight() * 0.45, 0);
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 4 + Math.PI / 2 * i;
			Vec3 base = feet.add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
			Light.ray(level, base, waist, theme.primary(), 0.05, 16);
		}
		Light.ring(level, waist, UP, theme.secondary(), 0.9, 0.4, 0.04, 12);
		Fx.sound(level, feet, SoundEvents.CHAIN_PLACE, 1.0F, 0.7F);
		Fx.sound(level, feet, SoundEvents.HEAVY_CORE_PLACE, 0.6F, 0.9F);
	}

	/** Bramble: thorny green arcs curling around the body. */
	static void bramble(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		double r = Math.max(0.5, t.getBbWidth() * 0.8);
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3;
			Vec3 normal = new Vec3(Math.cos(a) * 0.4, 1, Math.sin(a) * 0.4).normalize();
			Vec3 toward = across(normal).scale(i % 2 == 0 ? 1 : -1);
			Light.slash(level, c.add(0, (i - 1) * t.getBbHeight() * 0.25, 0), normal, toward, i == 1 ? 0x3E7A34 : LIFE, r, Math.PI * 1.4, 0.07, 3, 10);
		}
		Vfx.emit(level, new DustParticleOptions(0x3E7A34, 1.1F), c, 8, r * 0.8, 0.0);
		// Four thorns you can count, circling the body: each hit taken spends one.
		ElementFx.orbit(level, c, r + 0.2, dev.wildercord.cast.Effects.BRAMBLE_THORNS, 200, 0x3E7A34, LIFE);
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_thorn_grow", 0.9F, 1.0F);
	}

	/** Bramble answers: a thorn of light lashes out at the attacker. */
	static void brambleStrike(ServerLevel level, Entity t, Entity attacker) {
		Vec3 from = t.getBoundingBox().getCenter();
		Vec3 to = attacker.getBoundingBox().getCenter();
		Light.ray(level, from, to, LIFE, 0.06, 5);
		Sigils.flash(level, to, LIFE, 1.1F);
		Vfx.radial(level, new DustParticleOptions(0x3E7A34, 1.0F), to, 8, 0.15);
		dev.wildercord.cast.feel.Feels.sound(level, to, "life_thorn", 0.9F, 1.0F);
	}

	/** Frostward: pale rings of warmth climbing the body, snowflakes melting away from it. */
	static void frostward(ServerLevel level, Entity t, Vfx.Theme theme) {
		Vec3 feet = t.position();
		double r = Math.max(0.6, t.getBbWidth() * 0.9);
		for (int i = 0; i < 3; i++) {
			double y = 0.1 + i * t.getBbHeight() * 0.28;
			Scheduler.later(1 + i * 2, () -> Light.ring(level, feet.add(0, y, 0), UP, theme.secondary(), r, r + 0.5, 0.04, 10));
		}
		Light.groundRing(level, feet, 0xFFD8A0, 0.2, r + 1.2, 0.06, 12);
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, t.getBoundingBox().getCenter(), 10, 0.12);
		Feels.sound(level, feet, "frost_ward", 0.8F, 1.0F);
	}

	/** Frostward while it lasts: a faint warm ring at the feet now and then (gone with the ward). */
	static void frostwardIdle(ServerLevel level, Entity t) {
		Light.groundRing(level, t.position(), 0xFFD8A0, 0.3, 0.9, 0.03, 12);
	}

	/** Frostward wearing off: the warmth drains out of the ring and a last snowflake settles. */
	static void frostwardEnd(ServerLevel level, Entity t) {
		Light.groundRing(level, t.position(), 0xCFEFFF, 0.9, 0.2, 0.03, 10);
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, t.getBoundingBox().getCenter(), 4, 0.3, 0.01);
		Feels.sound(level, t.position(), "frost_tick", 0.35F, 0.7F);
	}

	/** Cushion while it lasts: a faint ring of air at the feet now and then. */
	static void cushionIdle(ServerLevel level, Entity t, Vfx.Theme theme) {
		Light.groundRing(level, t.position(), theme.primary(), 0.3, 0.9, 0.03, 12);
	}

	/** Cushion wearing off: the ring folds back into the feet. */
	static void cushionEnd(ServerLevel level, Entity t, Vfx.Theme theme) {
		Light.groundRing(level, t.position(), theme.secondary(), 0.9, 0.2, 0.03, 10);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, t.position().add(0, 0.2, 0), 2, 0.3, 0.0);
	}

	/** Cushion: a soft ring of air around the feet. */
	static void cushion(ServerLevel level, Entity t, Vfx.Theme theme) {
		Vec3 feet = t.position();
		Light.groundRing(level, feet, theme.primary(), 0.3, 1.3, 0.07, 12);
		Light.ring(level, feet.add(0, 0.3, 0), UP, theme.secondary(), 1.2, 0.5, 0.04, 10);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, feet.add(0, 0.2, 0), 4, 0.4, 0.0);
		Feels.sound(level, feet, "wind_feather", 0.6F, 0.7F);
	}

	/** A cushioned landing: a gust bursting out along the ground. */
	static void cushionLand(ServerLevel level, Vec3 feet, double radius, Vfx.Theme theme, double fallen) {
		Light.groundRing(level, feet, theme.primary(), 0.4, radius, 0.12, 10);
		Light.groundRing(level, feet, theme.secondary(), 0.2, radius * 0.7, 0.06, 8);
		// The landing grows with the fall: one more ring for every 3 blocks past the first 4.
		int extra = (int) Math.min(4, Math.max(0, (fallen - 4) / 3));
		for (int i = 0; i < extra; i++) {
			Light.groundRing(level, feet.add(0, 0.05 * (i + 1), 0), theme.secondary(), 0.3, radius * (1.0 + 0.25 * (i + 1)), 0.05, 9 + i);
		}
		dot(level, ParticleTypes.GUST, feet.add(0, 0.3, 0));
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			Vfx.fling(level, ParticleTypes.SMALL_GUST, feet.add(0, 0.2, 0), new Vec3(Math.cos(a), 0.1, Math.sin(a)), 0.3);
		}
		Feels.sound(level, feet, "wind_thump", 0.9F, 1.0F);
	}

	/** Deflect: a ring of wind rising around the body. */
	static void deflect(ServerLevel level, Entity t, Vfx.Theme theme) {
		Vec3 c = t.getBoundingBox().getCenter();
		double r = Math.max(1.0, t.getBbWidth() + 0.6);
		Light.ring(level, t.position().add(0, 0.2, 0), UP, theme.primary(), 0.4, r + 0.4, 0.06, 10);
		Light.ring(level, c, new Vec3(0.3, 1, 0.2).normalize(), theme.secondary(), r, r, 0.04, 12);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, c, 6, r * 0.6, 0.0);
		Feels.sound(level, c, "wind_deflect", 0.5F, 0.8F);
	}

	/** Every few ticks of a Deflect: gusts circling the body. */
	static void deflectSpin(ServerLevel level, Entity t, Vfx.Theme theme, long time) {
		double r = Math.max(1.0, t.getBbWidth() + 0.6);
		for (int i = 0; i < 2; i++) {
			double a = time * 0.5 + Math.PI * i;
			Vec3 p = t.position().add(Math.cos(a) * r, 0.4 + t.getBbHeight() * 0.3 * i, Math.sin(a) * r);
			dot(level, ParticleTypes.SMALL_GUST, p);
		}
	}

	/** A projectile turned aside: a slash of wind where it was knocked away. */
	static void deflectHit(ServerLevel level, Vec3 at, Vec3 away, Vfx.Theme theme) {
		Vec3 normal = across(away).cross(away).normalize();
		Light.slash(level, at.subtract(away.scale(0.4)), normal.lengthSqr() < 1.0E-4 ? UP : normal, away, theme.primary(), 0.6, 2.0, 0.08, 1, 5);
		Sigils.flash(level, at, theme.primary(), 0.8F);
		Feels.sound(level, at, "wind_deflect", 1.0F, 1.0F);
	}

	/** Haven opens: a circle on the ground, a flare, and the dome rising ring by ring. */
	static void havenOpen(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme, int lifetime) {
		Sigils.ground(level, c, theme.primary(), theme.secondary(), (float) radius, lifetime);
		Sigils.flash(level, c.add(0, 1, 0), theme.primary(), (float) Math.min(5, radius));
		for (int t = 0; t < 4; t++) {
			double lat = (t + 1) / 5.0;
			Scheduler.later(2 + t * 2, () -> Light.ring(level, c.add(0, radius * Math.sin(lat * Math.PI / 2), 0), UP, theme.primary(),
				radius * Math.cos(lat * Math.PI / 2) * 0.6, radius * Math.cos(lat * Math.PI / 2), 0.05, 14));
		}
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_dome", 1.0F, 1.0F);
	}

	/** Haven holds: the dome's meridians and parallels, renewed every second. */
	static void havenShell(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme, int tick) {
		double spin = tick * 0.02;
		for (int m = 0; m < 3; m++) {
			double a = spin + Math.PI * m / 3;
			Light.ring(level, c, new Vec3(Math.cos(a), 0, Math.sin(a)), theme.primary(), radius, radius, 0.035, 22);
		}
		for (double lat : new double[] {0.35, 0.7}) {
			Light.ring(level, c.add(0, radius * Math.sin(lat * Math.PI / 2), 0), UP, lat > 0.5 ? theme.secondary() : theme.primary(),
				radius * Math.cos(lat * Math.PI / 2), radius * Math.cos(lat * Math.PI / 2), 0.035, 22);
		}
		Light.groundRing(level, c, theme.primary(), radius, radius, 0.06, 22);
		Vfx.emit(level, theme.mote(), c.add(0, 0.5, 0), 3, radius * 0.5, 0.0);
	}

	/** A projectile glances off the dome: a ripple of light on the shell where it struck. */
	static void havenGlance(ServerLevel level, Vec3 at, Vec3 normal, Vfx.Theme theme) {
		Light.ring(level, at, normal, theme.secondary(), 0.1, 0.9, 0.04, 7);
		Sigils.flash(level, at, theme.primary(), 0.9F);
		Fx.sound(level, at, SoundEvents.SHIELD_BLOCK, 0.7F, 1.4F);
	}

	static void havenClose(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme) {
		for (int m = 0; m < 3; m++) {
			double a = Math.PI * m / 3;
			Light.ring(level, c, new Vec3(Math.cos(a), 0, Math.sin(a)), theme.primary(), radius, radius * 0.1, 0.035, 10);
		}
		Fx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 0.7F, 1.3F);
	}

	// ------------------------------------------------------------------ mining and building

	private static Vec3 faceCentre(BlockPos pos, Direction face) {
		return Vec3.atCenterOf(pos).add(normalOf(face).scale(0.51));
	}

	/** Chisel: a small ring of light closing on the face that was struck. */
	static void chisel(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
		Direction f = face == null ? Direction.UP : face;
		Vec3 at = faceCentre(pos, f);
		Light.ring(level, at, normalOf(f), EARTH, 0.7, 0.1, 0.05, 7);
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at, 8, 0.15);
		Vfx.emit(level, ParticleTypes.CRIT, at, 4, 0.2, 0.1);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 1.3F);
	}

	/** Tunnel: a circle opens on the wall and the stone gives way behind it. */
	static void tunnel(ServerLevel level, Vec3 start, Direction into, Vfx.Theme theme) {
		Vec3 out = normalOf(into.getOpposite());
		Vec3 at = start.add(out.scale(0.53));
		Sigils.circle(level, at, out, theme.primary(), theme.secondary(), 0.9F, 18);
		Fx.sound(level, at, SoundEvents.HEAVY_CORE_PLACE, 0.8F, 0.7F);
		Fx.sound(level, at, SoundEvents.MACE_SMASH_GROUND, 0.6F, 0.8F);
	}

	static void bore(ServerLevel level, BlockPos pos, BlockState state, Direction into) {
		Vec3 at = Vec3.atCenterOf(pos);
		Light.ring(level, at, normalOf(into), EARTH, 0.8, 0.2, 0.05, 6);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at, 6, 0.3, 0.05);
	}

	/** Vein: a thread of light running from ore to ore as each one breaks. */
	static void vein(ServerLevel level, BlockPos from, BlockPos pos, BlockState state) {
		Vec3 a = Vec3.atCenterOf(from);
		Vec3 b = Vec3.atCenterOf(pos);
		if (!from.equals(pos)) {
			Light.ray(level, a, b, 0xFFE6A0, 0.06, 8);
		}
		Sigils.flash(level, b, 0xFFE6A0, 1.1F);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), b, 6, 0.3, 0.05);
		int step = Math.floorMod(pos.getX() * 7 + pos.getY() * 13 + pos.getZ() * 3, 6);
		Fx.sound(level, b, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, new float[] {1.0F, 1.122F, 1.26F, 1.498F, 1.682F, 2.0F}[step]);
	}

	/** Smelt: the block goes up in a flash of furnace heat. */
	static void smelt(ServerLevel level, BlockPos pos, BlockState state) {
		Vec3 at = Vec3.atCenterOf(pos);
		Sigils.flash(level, at, FIRE, 1.6F);
		Light.ring(level, at, UP, FIRE, 0.2, 1.1, 0.06, 8);
		Vfx.radial(level, ParticleTypes.FLAME, at, 12, 0.08);
		Vfx.emit(level, ParticleTypes.LAVA, at, 2, 0.2, 0.0);
		Vfx.emit(level, ParticleTypes.SMOKE, at, 4, 0.3, 0.02);
		Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, 0.5F, 1.5F);
		Fx.sound(level, at, SoundEvents.FURNACE_FIRE_CRACKLE, 1.0F, 1.0F);
	}

	/** Fell: a sweeping cut of light across the trunk. */
	static void fellStart(ServerLevel level, BlockPos start, Vfx.Theme theme) {
		Vec3 at = Vec3.atCenterOf(start);
		Light.slash(level, at, UP, new Vec3(1, 0, 0), theme.primary(), 0.9, Math.PI * 1.6, 0.14, 3, 8);
		Sigils.flash(level, at, theme.primary(), 1.2F);
		Fx.sound(level, at, SoundEvents.AXE_STRIP, 1.0F, 0.8F);
		dev.wildercord.cast.feel.Feels.sound(level, at, "earth_dig", 0.8F, 0.75F);
	}

	static void fell(ServerLevel level, BlockPos pos, BlockState state) {
		Vec3 at = Vec3.atCenterOf(pos);
		Light.ring(level, at, UP, EARTH, 0.2, 0.8, 0.04, 6);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at, 3, 0.3, 0.05);
	}

	/** Glimmer: each new patch of lichen lights up with a soft glow. */
	static void glimmer(ServerLevel level, List<BlockPos> grown, Direction face) {
		for (int i = 0; i < grown.size(); i++) {
			Vec3 at = Vec3.atCenterOf(grown.get(i)).subtract(normalOf(face).scale(0.4));
			Scheduler.later(1 + i, () -> {
				glow(level, 0x7CFFE0, at, 0.8);
				Vfx.emit(level, ParticleTypes.GLOW, at, 3, 0.25, 0.01);
			});
		}
		if (!grown.isEmpty()) {
			dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(grown.getFirst()), "life_glimmer", 0.7F, 1.0F);
		}
	}

	/** Prune: a gust spreading out through the leaves. */
	static void prune(ServerLevel level, Vec3 at, double radius, boolean cleared) {
		// A flat scythe of air sweeping a full circle over four ticks, low over the brush.
		for (int i = 0; i < 4; i++) {
			double a = Math.PI * 0.5 * i;
			int color = i % 2 == 0 ? 0xC8F0DC : WHITE;
			Scheduler.later(i, () -> Light.slash(level, at.add(0, 0.4, 0), UP, new Vec3(Math.cos(a), 0, Math.sin(a)), color, radius * 0.9, 1.6, 0.08, 1, 5));
		}
		Vfx.emit(level, ParticleTypes.SMALL_GUST, at.add(0, 0.3, 0), 4, radius * 0.5, 0.0);
		Feels.sound(level, at, "wind_slash", 0.6F, 1.4F);
		if (cleared) {
			Fx.sound(level, at, SoundEvents.GRASS_BREAK, 1.0F, 1.1F);
		}
	}

	static void spanStart(ServerLevel level, Vec3 feet, Vec3 dir, Vfx.Theme theme) {
		Sigils.send(level, SigilOption.flat(SigilOption.CIRCLE, theme.primary(), 0.8F, 20, 0.15F), feet.add(dir.scale(0.8)).add(0, 0.05, 0));
		dev.wildercord.cast.feel.Feels.sound(level, feet, "arcane_glassrun", 0.9F, 1.0F);
	}

	/** Each block of a Span: a flat ring of light running out across its top. */
	static void spanBlock(ServerLevel level, BlockPos pos, Vfx.Theme theme) {
		Vec3 top = Vec3.atCenterOf(pos).add(0, 0.52, 0);
		Light.ring(level, top, UP, theme.primary(), 0.1, 0.7, 0.04, 6);
		glow(level, theme.secondary(), top, 0.6);

	}

	static void spanShatter(ServerLevel level, BlockPos pos, Vfx.Theme theme) {
		Vfx.emit(level, theme.sparkle(), Vec3.atCenterOf(pos), 2, 0.3, 0.0);
		// The glass goes in a cascade: a crash every few blocks.
		if (Math.floorMod(pos.getX() + pos.getZ(), 4) == 0) {
			dev.wildercord.cast.feel.Feels.sound(level, Vec3.atCenterOf(pos), "arcane_glassshatter", 0.5F, 1.0F);
		}
	}

	// ------------------------------------------------------------------ damage and control

	/** Ember: flames licking up around the target and a warm flare. */
	static void ember(ServerLevel level, Entity t) {
		Vec3 base = t.position();
		double r = Math.max(0.4, t.getBbWidth() * 0.6);
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			Vfx.fling(level, ParticleTypes.FLAME, base.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), new Vec3(-Math.cos(a) * 0.2, 1, -Math.sin(a) * 0.2), 0.08);
		}
		glow(level, FIRE, t.getBoundingBox().getCenter(), 1.2);
		Light.groundRing(level, base, FIRE, 0.2, r + 0.6, 0.05, 8);
		Fx.sound(level, base, SoundEvents.FIRECHARGE_USE, 0.4F, 1.7F);
	}

	/** Icicle: ice bursting off the target; a slowed one cracks with a sharper ring. */
	static void icicle(ServerLevel level, Entity t, boolean slowed) {
		Vec3 c = t.getBoundingBox().getCenter();
		// A vertical needle of ice light falling from three blocks up (unlike every radial burst); a slowed target also gets a star.
		ElementFx.ray(level, c.add(0, 3.0, 0), c, WHITE, 0.09, 4);
		ElementFx.ray(level, c.add(0, 3.0, 0), c, FROST, 0.2, 5);
		Sigils.flash(level, c, FROST, slowed ? 1.2F : 0.8F);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), c, 6, 0.14);
		if (slowed) {
			Light.ring(level, c, UP, FROST, 0.1, 1.1, 0.04, 7);
			Light.ring(level, c, new Vec3(0.6, 0.5, 0.6).normalize(), WHITE, 0.1, 1.0, 0.03, 6);
			Feels.sound(level, c, "frost_break", 0.4F, 1.6F);
		}
		Feels.sound(level, c, "frost_needle", 0.9F, 1.0F);
	}

	/** Pelt: stones cracking against the target. */
	static void pelt(ServerLevel level, Entity t, Vec3 away) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), c, 10, 0.15);
		Light.ring(level, c.subtract(away.scale(0.3)), away, EARTH, 0.1, 0.8, 0.05, 6);
		Vfx.emit(level, ParticleTypes.CRIT, c, 5, 0.25, 0.2);
		dev.wildercord.cast.feel.Feels.sound(level, c, "earth_rattle", 0.9F, 1.0F);
	}

	/** Windcut: two blades of wind crossing through the target. */
	static void windcut(ServerLevel level, Entity t, Vec3 away) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vec3 side = across(away);
		double r = Math.max(0.6, t.getBbHeight() * 0.5);
		// A cut, not a burst: one long white-hot line through the target with a mint halo round it.
		Vec3 n = side.add(UP.scale(0.35)).normalize();
		Light.slash(level, c.subtract(away.scale(r * 0.5)), n, away, 0xC8F0DC, r * 1.5, 1.5, 0.16, 1, 5);
		Light.slash(level, c.subtract(away.scale(r * 0.5)), n, away, WHITE, r * 1.5, 1.5, 0.05, 1, 4);
		Feels.sound(level, c, "wind_slash", 0.9F, 1.0F);
	}

	/** Leech: crimson motes drawn from the target into the caster. */
	static void leech(ServerLevel level, Entity t, Entity caster) {
		Vec3 c = t.getBoundingBox().getCenter();
		glow(level, BLOOD, c, 1.1);
		Vfx.stream(level, c, caster.getBoundingBox().getCenter(), Vfx.theme("blood"), 8);
		Vfx.emit(level, new DustParticleOptions(0x8A0A1A, 1.0F), c, 6, 0.25, 0.0);
		Fx.sound(level, c, SoundEvents.GENERIC_DRINK, 0.5F, 0.7F);
	}

	/** Hex: a turning star of void light over the target's head, and a ring binding its body. */
	static void hex(ServerLevel level, Entity t, int ticks) {
		Vec3 head = t.position().add(0, t.getBbHeight() + 0.45, 0);
		Sigils.layer(level, head, UP, SigilOption.STAR, VOID, 0.45F, Math.min(ticks, 60), 0.12F);
		Light.ring(level, t.getBoundingBox().getCenter(), UP, VOID, Math.max(0.9, t.getBbWidth() + 0.5), 0.3, 0.05, 10);
		Vfx.emit(level, ParticleTypes.WITCH, head, 6, 0.2, 0.0);
		Fx.sound(level, head, SoundEvents.EVOKER_CAST_SPELL, 0.6F, 1.4F);
	}

	/** A hexed target takes its hexer's spell: a little bite of violet. */
	static void hexBite(ServerLevel level, Entity t) {
		glow(level, VOID, t.getBoundingBox().getCenter(), 0.8);
		Vfx.emit(level, ParticleTypes.WITCH, t.getBoundingBox().getCenter(), 2, 0.25, 0.0);
	}

	/** Rend: two crimson slashes crossing over the chest and scraps of armour flying. */
	static void rend(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		double r = Math.max(0.5, t.getBbHeight() * 0.4);
		Vec3 look = unit(new Vec3(-Math.sin(Math.toRadians(t.getYRot())), 0, Math.cos(Math.toRadians(t.getYRot()))));
		Vec3 side = across(look);
		Light.slash(level, c.add(look.scale(0.4)), look, side.add(UP).normalize(), BLOOD, r, 1.6, 0.08, 1, 6);
		Light.slash(level, c.add(look.scale(0.4)), look, side.subtract(UP).normalize(), 0xFF8090, r, 1.6, 0.06, 1, 6);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET), c, 6, 0.15);
		Fx.sound(level, c, SoundEvents.ITEM_BREAK, 0.6F, 1.3F);
	}

	/** Countdown: a clock of light over the head, shrinking with each tick. */
	static void countdown(ServerLevel level, Entity t, int beat) {
		Vec3 head = t.position().add(0, t.getBbHeight() + 0.5, 0);
		float size = 0.9F - beat * 0.25F;
		Sigils.layer(level, head, UP, SigilOption.CIRCLE, TIME, size, 11, 0.2F);
		Light.ring(level, head, UP, WHITE, size + 0.3, size * 0.6, 0.03, 8);
		Fx.sound(level, head, SoundEvents.NOTE_BLOCK_HAT, 0.8F, 1.2F + beat * 0.3F);
	}

	/** The moment catches up: a bolt of gold light falls on the target. */
	static void countdownStrike(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		Light.ray(level, c.add(0, 4, 0), c, TIME, 0.18, 8);
		Sigils.flash(level, c, TIME, 2.0F);
		Light.ring(level, c, UP, TIME, 0.2, 1.6, 0.06, 9);
		Vfx.radial(level, ParticleTypes.END_ROD, c, 10, 0.18);
		Fx.sound(level, c, SoundEvents.BELL_BLOCK, 0.8F, 1.5F);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.8F);
	}

	/** Jolt: arcs crackling over the target and a ring pinning it. */
	static void jolt(ServerLevel level, Entity t) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vfx.shockArc(level, c.add(0.5, 0.9, 0.2), c);
		Vfx.shockArc(level, c.add(-0.4, 0.8, -0.3), c.subtract(0, 0.3, 0));
		Sigils.flash(level, c, STORM, 1.4F);
		Light.ring(level, t.position().add(0, 0.1, 0), UP, STORM, Math.max(0.8, t.getBbWidth() + 0.4), 0.3, 0.05, 8);
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, c, 6, 0.3);
		dev.wildercord.cast.feel.Feels.sound(level, c, "storm_clamp", 0.9F, 1.0F);
	}

	/** Bleed: the opening cut, then drops falling with every wound. */
	static void bleed(ServerLevel level, Entity t, boolean cut) {
		Vec3 c = t.getBoundingBox().getCenter();
		if (cut) {
			Vec3 look = unit(new Vec3(-Math.sin(Math.toRadians(t.getYRot())), 0, Math.cos(Math.toRadians(t.getYRot()))));
			Light.slash(level, c.add(look.scale(0.4)), look, across(look).add(0, -0.6, 0).normalize(), BLOOD, Math.max(0.5, t.getBbHeight() * 0.4), 1.8, 0.07, 1, 6);
			Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.5F);
		}
		for (int i = 0; i < 2; i++) {
			Vec3 p = c.add((level.getRandom().nextDouble() - 0.5) * t.getBbWidth(), (level.getRandom().nextDouble() - 0.5) * t.getBbHeight() * 0.6,
				(level.getRandom().nextDouble() - 0.5) * t.getBbWidth());
			Vfx.fling(level, new DustParticleOptions(0x8A0A1A, 1.0F), p, new Vec3(0, -1, 0), 0.05);
		}
	}

	/** Coldsnap: frost spreading across the ground in a ring of white light. */
	static void coldsnap(ServerLevel level, Vec3 point, double radius) {
		Vec3 ground = CastEngine.ground(level, point.add(0, 0.5, 0));
		Sigils.ground(level, ground, FROST, WHITE, (float) radius, 20);
		// The snap races out over a few ticks: a white ring first, then the frost ring after it, then the crust.
		Light.groundRing(level, ground, WHITE, 0.3, radius, 0.1, 6);
		Scheduler.later(2, () -> Light.groundRing(level, ground, FROST, 0.2, radius * 0.9, 0.06, 8));
		Scheduler.later(4, () -> Light.groundRing(level, ground, WHITE, 0.4, radius * 0.6, 0.04, 8));
		Sigils.flash(level, ground.add(0, 0.8, 0), FROST, (float) Math.min(3, radius));
		Vfx.radial(level, ParticleTypes.SNOWFLAKE, ground.add(0, 0.6, 0), 20, 0.25);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.ICE), ground.add(0, 0.4, 0), 10, 0.2);
		Feels.sound(level, ground, "frost_crust", 1.0F, 0.8F);
		Feels.sound(level, ground, "frost_whump", 0.5F, 1.3F);
	}

	static void chilled(ServerLevel level, Entity t) {
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, t.getBoundingBox().getCenter(), 6, 0.3, 0.01);
		Light.ring(level, t.position().add(0, 0.1, 0), UP, FROST, 0.2, Math.max(0.7, t.getBbWidth() + 0.2), 0.04, 8);
	}

	/** Flashfire: a flare of heat, a shell of fire racing out and flames thrown in every direction. */
	static void flashfire(ServerLevel level, Vec3 point, double radius) {
		Sigils.flash(level, point, FIRE, (float) Math.min(5, radius * 1.4));
		shell(level, point, FIRE, 0xFFD060, 0.3, radius, 0.07, 9);
		Light.groundRing(level, CastEngine.ground(level, point.add(0, 0.5, 0)), FIRE, 0.4, radius * 1.1, 0.1, 11);
		Vfx.radial(level, ParticleTypes.FLAME, point, 24, 0.3);
		Motes.clouds(level, point, 6, radius * 0.3, Motes.SMOKE, 1.3, 40, new Vec3(0, 0.03, 0), 0.07, 0.4);
		Fx.sound(level, point, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
		Fx.sound(level, point, SoundEvents.BLAZE_SHOOT, 0.6F, 0.7F);
	}

	/** Banish: the target folds away into a point of void light and unfolds where it lands. */
	static void banish(ServerLevel level, Entity t, Vec3 from, Vec3 to) {
		Vec3 a = from.add(0, t.getBbHeight() * 0.5, 0);
		Vec3 b = to.add(0, t.getBbHeight() * 0.5, 0);
		Light.ring(level, a, UP, VOID, Math.max(1.0, t.getBbWidth() + 0.6), 0.05, 0.06, 8);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, a, 16, 0.3, 0.05);
		Light.ray(level, a, b, VOID, 0.04, 5);
		Sigils.flash(level, b, VOID, 1.8F);
		Light.ring(level, b, UP, VOID, 0.1, Math.max(1.0, t.getBbWidth() + 0.6), 0.06, 8);
		Vfx.radial(level, ParticleTypes.PORTAL, b, 14, 0.3);
		Fx.sound(level, a, SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.8F);
		Fx.sound(level, b, SoundEvents.CHORUS_FRUIT_TELEPORT, 0.6F, 1.2F);
	}

	static void banishResisted(ServerLevel level, Entity t) {
		Light.ring(level, t.getBoundingBox().getCenter(), UP, VOID, 0.3, Math.max(1.0, t.getBbWidth()), 0.04, 6);
		Fx.sound(level, t.position(), SoundEvents.SHULKER_BULLET_HIT, 0.6F, 0.7F);
	}

	static void cycloneRise(ServerLevel level, Vec3 centre, double radius) {
		Light.groundRing(level, centre, 0xC8F0DC, 0.3, radius, 0.08, 12);
		Feels.sound(level, centre, "wind_whirl", 0.9F, 1.0F);
	}

	/** A Cyclone turning: arcs of wind whirling around it at three heights, gusts thrown off them. */
	static void cyclone(ServerLevel level, Vec3 centre, double radius, int tick) {
		if (tick % 2 == 0) {
			for (int k = 0; k < 3; k++) {
				double a = tick * 0.45 + k * 2.1;
				Vec3 toward = new Vec3(Math.cos(a), 0, Math.sin(a));
				double r = radius * (0.55 + 0.2 * k);
				Light.slash(level, centre.add(0, 0.3 + k * 0.9, 0), UP, toward, k == 1 ? WHITE : 0xC8F0DC, r, 1.5, 0.09, 1, 4);
			}
		}
		if (tick % 3 == 0) {
			double a = tick * 0.7;
			Vfx.fling(level, ParticleTypes.SMALL_GUST, centre.add(Math.cos(a) * radius, 0.5 + (tick % 5) * 0.4, Math.sin(a) * radius),
				new Vec3(-Math.sin(a), 0.3, Math.cos(a)), 0.3);
		}
		if (tick % 10 == 0) {
		}
	}

	/** The Cyclone lets go: a burst of wind racing out along the ground. */
	static void cycloneFling(ServerLevel level, Vec3 centre, double radius) {
		Light.groundRing(level, centre, 0xC8F0DC, 0.5, radius * 1.6, 0.12, 10);
		Light.ring(level, centre.add(0, 1, 0), UP, WHITE, 0.4, radius * 1.3, 0.06, 9);
		Vfx.radial(level, ParticleTypes.GUST, centre.add(0, 1, 0), 4, 0.3);
		Vfx.radial(level, ParticleTypes.SMALL_GUST, centre.add(0, 1, 0), 14, 0.5);
		Feels.sound(level, centre, "wind_thump", 1.0F, 0.8F);
	}
}
