package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Visuals for the batch 4 runes: their shapes in shaped light, like {@link Vfx}'s, and their
 * effects in their element's language ({@link ElementFx}). Time is pale gold clock faces, blood is
 * crimson cuts and heartbeats, and the black of Blackspark and Blackflame is darkness edged with
 * crimson or violet light, so it still reads at night.
 */
final class TechniqueVfx {
	private TechniqueVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int TIME = 0xF2D98A;
	private static final int BLOOD = 0xD2283C;
	private static final int BLACK = 0x0C0810;

	private static DustParticleOptions dust(int color, float scale) {
		return new DustParticleOptions(color, scale);
	}

	private static SigilOption flash(int color) {
		return SigilOption.glow(color, 2.2F);
	}

	private static void dot(ServerLevel level, ParticleOptions p, Vec3 at) {
		Vfx.emit(level, p, at, 1, 0.0, 0.0);
	}

	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	/** A small standing silhouette (head, shoulders, body) facing {@code forward}. */
	private static void silhouette(ServerLevel level, ParticleOptions p, Vec3 feet, Vec3 forward, double scale) {
		Vec3 right = flat(forward).cross(UP).normalize();
		Vec3 head = feet.add(0, 1.65 * scale, 0);
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			dot(level, p, head.add(right.scale(Math.cos(a) * 0.16 * scale)).add(0, Math.sin(a) * 0.16 * scale, 0));
		}
		for (int k = 0; k < 6; k++) {
			double y = (1.35 - k * 0.22) * scale;
			double w = (k == 0 ? 0.28 : 0.16) * scale;
			dot(level, p, feet.add(0, y, 0).add(right.scale(w)));
			dot(level, p, feet.add(0, y, 0).add(right.scale(-w)));
		}
	}

	// ------------------------------------------------------------------ shapes

	/** Domain particles are seen from afar (the far side of a big dome is often 30+ blocks away). */
	private static void far(ServerLevel level, ParticleOptions p, Vec3 at) {
		Fx.sendFar(level, p, at);
	}

	/** A horizontal ring of far-seen particles, one about every {@code spacing} blocks. */
	private static void farRing(ServerLevel level, ParticleOptions p, Vec3 c, double r, double spacing, double spin) {
		int points = (int) Math.max(12, Math.min(260, 2 * Math.PI * r / spacing));
		for (int i = 0; i < points; i++) {
			double a = spin + Math.PI * 2 * i / points;
			far(level, p, c.add(Math.cos(a) * r, 0, Math.sin(a) * r));
		}
	}

	/** Dust for a Domain frame: bigger for bigger domains, so the far side still reads. */
	private static float frameSize(double radius) {
		return (float) Math.min(3.0, 1.0 + radius / 12.0);
	}

	/**
	 * Domain opens: the spell's own magic circle spreads across the whole floor of it (readable like
	 * any other), a flare, and the dome rising in rings.
	 */
	static void domainOpen(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme, java.util.List<dev.wildercord.spell.RuneDef> spell, int lifetime) {
		ScreenFx.shake(level, c, 0.45F, radius * 2.5);
		if (!spell.isEmpty()) {
			Sigils.spell(level, c.add(0, 0.07, 0), UP, spell, theme.primary(), (float) radius, lifetime);
		} else {
			Sigils.ground(level, c, theme.primary(), theme.secondary(), (float) radius, lifetime);
		}
		Sigils.flash(level, c.add(0, 1, 0), theme.primary(), (float) Math.min(8, radius));
		for (int t = 0; t < 5; t++) {
			double lat = (t + 1) / 6.0;
			Scheduler.later(2 + t * 2, () -> Light.ring(level, c.add(0, radius * Math.sin(lat * Math.PI / 2), 0), UP, theme.primary(),
				radius * Math.cos(lat * Math.PI / 2) * 0.6, radius * Math.cos(lat * Math.PI / 2), domainLine(radius), 14));
		}
		Fx.sound(level, c, dev.wildercord.content.WildercordSounds.DOMAIN_OPEN, 1.0F, 1.0F);
	}

	/** How wide a domain's lines are: wider for bigger domains, so the far side still reads. */
	private static double domainLine(double radius) {
		return 0.06 + radius * 0.006;
	}

	/** Domain: a dome of light (meridians and parallels) breathing over the circle, and a dim interior. */
	static void domainShell(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme, int tick) {
		if (tick % 20 == 0) {
			double w = domainLine(radius);
			double spin = tick * 0.01;
			for (int m = 0; m < 4; m++) {
				double a = spin + Math.PI * m / 4;
				// A meridian: a vertical great circle (its lower half is under the floor, out of sight).
				Light.ring(level, c, new Vec3(Math.cos(a), 0, Math.sin(a)), theme.primary(), radius, radius, w, 30);
			}
			for (double lat : new double[] {0.3, 0.6, 0.85}) {
				Light.ring(level, c.add(0, radius * Math.sin(lat * Math.PI / 2), 0), UP, lat == 0.6 ? theme.secondary() : theme.primary(),
					radius * Math.cos(lat * Math.PI / 2), radius * Math.cos(lat * Math.PI / 2), w, 30);
			}
			Light.groundRing(level, c, theme.primary(), radius, radius, w * 1.5, 30);
		}
		if (tick % 10 == 0) {
			for (int i = 0; i < 6; i++) {
				double a = tick * 0.03 + Math.PI * 2 * i / 6;
				Vfx.fling(level, ParticleTypes.ENCHANT, c.add(Math.cos(a) * radius * 0.82, 0.12, Math.sin(a) * radius * 0.82), UP, 0.4);
			}
		}
		for (int i = 0; i < 4; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double rr = Math.sqrt(level.getRandom().nextDouble()) * radius * 0.9;
			dot(level, dust(0x2A1A40, 0.9F), c.add(Math.cos(a) * rr, 0.3 + level.getRandom().nextDouble() * radius * 0.6, Math.sin(a) * rr));
		}
	}

	/** Domain strikes: a wave of light across the floor, and light pouring down from the crown of the dome. */
	static void domainStrike(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme) {
		Light.groundRing(level, c, theme.primary(), radius * 0.1, radius * 0.98, domainLine(radius) * 1.6, 12);
		Light.ray(level, c.add(0, radius, 0), c.add(0, 0.2, 0), theme.secondary(), 0.12 + radius * 0.01, 8);
		Sigils.flash(level, c.add(0, radius, 0), theme.primary(), 2.5F);
		Vfx.emit(level, theme.sparkle(), c.add(0, 1.5, 0), 8, radius * 0.4, 0.0);
		Fx.sound(level, c, theme.impact(), 0.8F, 1.0F);
	}

	/** Domain closes: the dome falls in on itself and the floor cracks. */
	static void domainClose(ServerLevel level, Vec3 c, double radius, Vfx.Theme theme) {
		double w = domainLine(radius);
		for (int m = 0; m < 4; m++) {
			double a = Math.PI * m / 4;
			Light.ring(level, c, new Vec3(Math.cos(a), 0, Math.sin(a)), theme.primary(), radius, radius * 0.1, w, 10);
		}
		Light.groundRing(level, c, theme.secondary(), radius, radius * 0.05, w * 1.5, 10);
		Sigils.send(level, SigilOption.flat(SigilOption.CRACKED, theme.primary(), (float) radius * 0.8F, 30, 0.0F), c.add(0, 0.09, 0));
		Sigils.flash(level, c.add(0, 1, 0), theme.primary(), (float) Math.min(8, radius));
		Fx.sound(level, c, dev.wildercord.content.WildercordSounds.DOMAIN_CLOSE, 1.0F, 1.0F);
	}

	/** Crescent: a blade of light, white at its edge, sweeping forward with a paler echo behind it. */
	static void crescent(ServerLevel level, Vec3 front, Vec3 aim, Vec3 side, double width, Vfx.Theme theme, int tick) {
		double radius = width * 0.62;
		double span = 2 * Math.asin(Math.min(0.99, width / 2 / radius));
		// A slight tilt, like a real swing.
		Vec3 normal = UP.add(side.scale(0.18)).normalize();
		Vec3 centre = front.subtract(aim.scale(radius * 0.8));
		Light.slash(level, centre, normal, aim, theme.primary(), radius, span, tick == 0 ? 0.42 : 0.34, tick == 0 ? 2 : 1, 4);
		if (tick % 2 == 0) {
			Light.slash(level, centre.subtract(aim.scale(0.6)), normal, aim, theme.secondary(), radius * 0.95, span * 0.9, 0.16, 1, 4);
		}
		Vfx.emit(level, theme.mote(), front, 2, width * 0.2, 0.01);
	}

	/** One blow of a Barrage: a quick arc of light and a flare where it lands; the last lands with a bang. */
	static void barrageBlow(ServerLevel level, Vec3 origin, Vec3 front, Vfx.Theme theme, int blow, boolean last) {
		Vec3 at = front.add((level.getRandom().nextDouble() - 0.5) * 0.8, (level.getRandom().nextDouble() - 0.5) * 0.8,
			(level.getRandom().nextDouble() - 0.5) * 0.8);
		Vec3 dir = at.subtract(origin).normalize();
		// Each blow swings on its own tilt around the line of the punch.
		double tilt = level.getRandom().nextDouble() * Math.PI;
		Vec3 side = dir.cross(UP);
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		Vec3 normal = up.scale(Math.cos(tilt)).add(side.scale(Math.sin(tilt)));
		Light.slash(level, at.subtract(dir.scale(0.5)), normal, dir, blow % 2 == 0 ? theme.primary() : theme.secondary(), 0.55, 2.3, 0.14, 1, 4);
		Vfx.emit(level, SigilOption.glow(theme.primary(), 0.8F), at, 1, 0.0, 0.0);
		Vfx.emit(level, ParticleTypes.CRIT, at, 4, 0.1, 0.25);
		Fx.sound(level, at, blow % 2 == 0 ? SoundEvents.PLAYER_ATTACK_WEAK : SoundEvents.PLAYER_ATTACK_STRONG, 0.55F, 0.9F + blow * 0.05F);
		if (last) {
			Sigils.flash(level, front, theme.primary(), 2.2F);
			Light.ring(level, front, dir, theme.primary(), 0.2, 1.8, 0.07, 10);
			Light.groundRing(level, front.subtract(0, 0.5, 0), theme.secondary(), 0.3, 1.8, 0.06, 10);
			Vfx.radial(level, theme.spark(), front, 16, 0.3);
			Fx.sound(level, front, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
		}
	}

	/** Orb: a glowing core wrapped in three turning rings, drifting forward. */
	static void orb(ServerLevel level, Vec3 pos, double radius, Vfx.Theme theme, int tick) {
		Light.orb(level, pos, theme.primary(), radius * 0.4, 2);
		Vfx.emit(level, SigilOption.glow(theme.secondary(), (float) (radius * 1.1)), pos, 1, 0.0, 0.0);
		if (tick % 3 == 0) {
			Vfx.emit(level, theme.mote(), pos, 2, radius * 0.4, 0.02);
		}
		if (tick % 10 == 0) {
			Fx.sound(level, pos, dev.wildercord.content.WildercordSounds.ORB_HUM, 0.7F, 1.0F);
		}
	}

	/** Blitz: a streak of light where you were, afterimages along it, and a cross of slashes where you land. */
	static void blitz(ServerLevel level, Vec3 a, Vec3 b, Vfx.Theme theme) {
		Vec3 d = b.subtract(a);
		double length = d.length();
		Sigils.flash(level, a, theme.primary(), 1.6F);
		if (length > 0.1) {
			Light.ray(level, a, b, theme.primary(), 0.22, 10);
			for (int k = 1; k <= 3; k++) {
				Vec3 feet = a.add(d.scale(k / 4.0)).subtract(0, 0.9, 0);
				silhouette(level, dust(theme.secondary(), 0.7F), feet, d, 1.0);
			}
			for (double s = 0; s < length; s += 1.5) {
				Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, a.add(d.scale(s / length)), 1, 0.2, 0.05);
			}
			Vec3 dir = d.normalize();
			Vec3 side = dir.cross(UP);
			side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
			Vec3 up = side.cross(dir).normalize();
			Light.slash(level, b, up.add(side).normalize(), dir, theme.primary(), 0.9, 2.2, 0.16, 1, 5);
			Light.slash(level, b, up.subtract(side).normalize(), dir, theme.secondary(), 0.9, 2.2, 0.16, 1, 5);
		}
		Sigils.flash(level, b, theme.primary(), 1.8F);
		Fx.sound(level, a, SoundEvents.TRIDENT_RIPTIDE_1, 1.0F, 1.4F);
		// (the element's cast sound already played once, with the cast circle)
	}

	// ------------------------------------------------------------------ damage

	/** Cleave: a great crimson cut swept down across the target, a heartbeat pulsing out of it and blood falling. */
	static void cleave(ServerLevel level, Entity target, Vec3 look) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 fwd = flat(look);
		Vec3 side = fwd.cross(UP).normalize();
		double size = Math.max(0.9, target.getBbHeight() * 0.6);
		// In the plane facing the caster, bulging up and to one side: a diagonal cut through the middle.
		Vec3 bulge = side.add(0, 1, 0).normalize();
		double r = size * 1.4;
		ElementFx.cut(level, c.subtract(bulge.scale(r)), fwd, bulge, r, 0.32);
		ElementFx.pulse(level, c, fwd, 1.0 + size * 0.5);
		Sigils.flash(level, c, BLOOD, 1.6F);
		dot(level, ParticleTypes.SWEEP_ATTACK, c);
		ElementFx.drip(level, c, 0.3, 6);
		Feels.sound(level, c, "blood_cleave", 1.0F, 1.0F);
	}

	/** Blackspark: on a true hit, black lightning edged in crimson strikes into the target and the air falls in on it. */
	static void blackspark(ServerLevel level, Entity target, boolean spark) {
		Vec3 c = target.getBoundingBox().getCenter();
		if (!spark) {
			ElementFx.ring(level, c, ElementFx.tilted(0.8, level.getRandom().nextDouble() * Math.PI * 2), ElementFx.dark(ElementFx.VOID.accent()), 0.9, 0.1,
				0.07, 6);
			Vfx.radial(level, ParticleTypes.CRIT, c, 6, 0.25);
			dev.wildercord.cast.feel.Feels.sound(level, c, "void_spark_plain", 0.9F, 1.0F);
			return;
		}
		dot(level, flash(BLOOD), c);
		RandomSource random = level.getRandom();
		for (int bolt = 0; bolt < 3; bolt++) {
			Vec3 from = c.add((random.nextDouble() - 0.5) * 2.4, 1.6 + random.nextDouble(), (random.nextDouble() - 0.5) * 2.4);
			ElementFx.bolt(level, from, c, 0.035, 1, 2, BLOOD, ElementFx.dark(BLACK));
		}
		ElementFx.implode(level, c, 1.4, 7);
		ElementFx.pulse(level, c, UP, 1.6);
		ElementFx.sparks(level, c, 12, 0.5);
		ElementFx.drip(level, c, 0.25, 4);
		dev.wildercord.cast.feel.Feels.sound(level, c, "void_spark_crit", 1.0F, 1.0F);
	}

	/** Aftershock: a hit, and half a second later the ground under the target cracks and heaves. */
	/** Aftershock's echo: the ground struck again on the spot of the first blow (not where the target went). */
	static void aftershockSpot(ServerLevel level, Vec3 spot) {
		Vfx.Theme earth = Vfx.theme("earth");
		dot(level, flash(0xFFE0B0), spot.add(0, 0.6, 0));
		ElementFx.crack(level, spot, 1.6, 20);
		Vfx.shockwave(level, spot, 1.8, earth, 4);
		Scheduler.later(2, () -> Vfx.shockwave(level, spot, 2.6, earth, 4));
		ElementFx.stoneShards(level, spot.add(0, 0.4, 0), ElementFx.groundBlock(level, spot), 10, 0.3);
		dev.wildercord.cast.feel.Feels.sound(level, spot, "earth_quake", 1.0F, 0.84F);
	}
	
	static void aftershock(ServerLevel level, Entity target, boolean second) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.Theme earth = Vfx.theme("earth");
		if (!second) {
			ElementFx.earthImpact(level, c, 0.8);
			dev.wildercord.cast.feel.Feels.sound(level, c, "earth_stomp", 0.9F, 1.0F);
			return;
		}
		dot(level, flash(0xFFE0B0), c);
		ElementFx.crack(level, target.position(), 1.6, 20);
		Vfx.shockwave(level, target.position(), 1.8, earth, 4);
		Scheduler.later(2, () -> Vfx.shockwave(level, target.position(), 2.6, earth, 4));
		ElementFx.stoneShards(level, c, ElementFx.groundBlock(level, target.position()), 10, 0.3);
		Fx.sound(level, c, SoundEvents.MACE_SMASH_GROUND, 1.0F, 1.1F);
	}

	private static final int CURSE = 0xC0306A;

	/** Resonance: a nail rings out and threads of cursed light run to every marked enemy. */
	static void resonance(ServerLevel level, Entity target, List<? extends Entity> linked) {
		Vec3 c = target.getBoundingBox().getCenter();
		ItemParticleOption nail = new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET);
		Sigils.flash(level, c, CURSE, 1.4F);
		ElementFx.ring(level, c, UP, ElementFx.ARCANE.primary(), 0.2, 1.1, 0.045, 9);
		ElementFx.ring(level, c, ElementFx.tilted(1.2, level.getRandom().nextDouble() * Math.PI * 2), CURSE, 0.15, 0.8, 0.035, 11);
		Vfx.radial(level, nail, c, 6, 0.15);
		for (Entity other : linked) {
			Vec3 o = other.getBoundingBox().getCenter();
			ElementFx.ray(level, c, o, CURSE, 0.035, 10);
			dot(level, SigilOption.glow(ElementFx.ARCANE.primary(), 1.2F), o);
			ElementFx.ring(level, o, UP, CURSE, 0.15, 0.7, 0.035, 8);
			Vfx.radial(level, nail, o, 3, 0.12);
		}
		dev.wildercord.cast.feel.Feels.sound(level, c, linked.isEmpty() ? "arcane_resonate" : "arcane_resonate_echo", 0.9F, 1.0F);
	}

	/** Ripple: rings of golden sunlight run out through the target, each on its own tilt. */
	static void ripple(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double a = level.getRandom().nextDouble() * Math.PI * 2;
		dot(level, SigilOption.glow(0xFFD050, 1.4F), c);
		for (int t = 0; t < 3; t++) {
			int k = t;
			Scheduler.later(t + 1, () -> ElementFx.ring(level, target.getBoundingBox().getCenter(), ElementFx.tilted(0.5, a + k * 2.1),
				k % 2 == 0 ? 0xFFD050 : 0xFF9A30, 0.2, 0.8 + k * 0.35, 0.05, 7));
		}
		Vfx.emit(level, ParticleTypes.WAX_ON, c, 6, 0.4, 0.0);
		dev.wildercord.cast.feel.Feels.sound(level, c, "storm_sun", 0.8F, 1.0F);
	}

/** Blackflame: tongues of black fire licking up the target, a few of them violet at the edge. */
	static void blackflame(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double w = Math.max(0.35, target.getBbWidth() * 0.6);
		ElementFx.tongues(level, base, w, target.getBbHeight(), 5, ElementFx.dark(BLACK), ElementFx.VOID.primary(), 2, 9);
		for (int i = 0; i < 3; i++) {
			double a = Math.PI * 2 * i / 3 + level.getRandom().nextDouble();
			Vfx.fling(level, ParticleTypes.SQUID_INK, base.add(Math.cos(a) * w, 0.2 + level.getRandom().nextDouble() * target.getBbHeight(), Math.sin(a) * w), UP,
				0.08);
		}
		Motes.smoke(level, target.getBoundingBox().getCenter(), 1, w);
		VoidFx.blackHeart(level, target.getBoundingBox().getCenter());
		dev.wildercord.cast.feel.Feels.sound(level, base, "void_black_fire", 0.7F, 1.0F);
	}

	private static final int RED = 0xFF3030;
	private static final int BLUE = 0x3050FF;

	/** Hollow: a red and a blue orb spiral together, then the gap collapses into a hole in the world that bursts violet. */
	static void hollow(ServerLevel level, Vec3 c, double radius) {
		Vec3 side = new Vec3(1, 0, 0);
		// The creature is cut out of the world for the wind-up: a dark sphere with an inverted rim stands on it.
		for (int t = 0; t < 7; t += 3) {
			Scheduler.later(t, () -> VoidFx.cutout(level, c, 0.75));
		}
		// The creature is cut out of the world for the wind-up: a dark sphere with an inverted rim stands on it.
		for (int t = 0; t < 7; t += 3) {
			Scheduler.later(t, () -> VoidFx.cutout(level, c, 0.75));
		}
		for (int t = 0; t < 6; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double k = 1 - (tick + 1) / 6.0;
				double before = 1 - tick / 6.0;
				Vec3 offset = side.yRot((float) (tick * 0.6)).scale(1.8 * k).add(0, 0.2 * k, 0);
				Vec3 last = side.yRot((float) ((tick - 1) * 0.6)).scale(1.8 * before).add(0, 0.2 * before, 0);
				ElementFx.orb(level, c.add(offset), RED, 0.3, 2);
				ElementFx.orb(level, c.subtract(offset), BLUE, 0.3, 2);
				ElementFx.ray(level, c.add(last), c.add(offset), RED, 0.12, 5);
				ElementFx.ray(level, c.subtract(last), c.subtract(offset), BLUE, 0.12, 5);
			});
		}
		Scheduler.later(7, () -> {
			dot(level, flash(0xB45AF0), c);
			dot(level, ParticleTypes.SONIC_BOOM, c);
			ElementFx.blackCore(level, c, 0.6, 14);
			ElementFx.implode(level, c, radius, 8);
			for (int i = 0; i < 3; i++) {
				ElementFx.ring(level, c, ElementFx.tilted(1.2, i * Math.PI / 3), i == 1 ? ElementFx.VOID.secondary() : ElementFx.VOID.primary(), 0.4, radius * 0.9,
					0.06, 10);
			}
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, c, 20, 0.6);
			Vfx.shockwave(level, c.subtract(0, 0.8, 0), radius, Vfx.theme("void"), 5);
		});
		Scheduler.later(7, () -> dev.wildercord.cast.feel.Feels.sound(level, c, "void_snap", 1.2F, 0.8F));
	}

	private static final int REPEL = ElementFx.WIND.accent();
	private static final int REPEL_LIGHT = ElementFx.WIND.secondary();

	/** Repel: a shell of wind bursting outward, blades of air whirling out with it. */
	static void repel(ServerLevel level, Vec3 c, double radius) {
		dot(level, flash(REPEL), c);
		RandomSource random = level.getRandom();
		// A dome: a hemisphere of level rings stacked up from the ground, each a beat after the one below.
		double[] rise = {0.0, 0.3, 0.55, 0.75};
		for (int i = 0; i < rise.length; i++) {
			double phi = rise[i];
			Scheduler.later(i, () -> ElementFx.ring(level, c.add(0, radius * 0.6 * Math.sin(phi), 0), UP, phi > 0.5 ? REPEL_LIGHT : REPEL,
				0.3, radius * Math.cos(phi), 0.07, 8));
		}
		Vec3 floor = ElementFx.floor(level, c, radius);
		if (floor != null) {
			ElementFx.groundRing(level, floor, REPEL, 0.3, radius * 1.2, 0.08, 10);
		}
		Vfx.radial(level, ParticleTypes.GUST, c, 3, 0.3);
		Vfx.radial(level, ParticleTypes.SMALL_GUST, c, 8, 0.5);
		Feels.sound(level, c, "wind_thump", 1.0F, 0.7F);
	}

	/** Collapse (Repel meeting a pull): the two forces annihilate, darkness falling in as violet light bursts out. */
	static void collapse(ServerLevel level, Vec3 c) {
		dot(level, flash(0xB45AF0), c);
		dot(level, ParticleTypes.SONIC_BOOM, c);
		ElementFx.implode(level, c, 2.2, 6);
		ElementFx.blackCore(level, c, 0.4, 10);
		for (int i = 0; i < 3; i++) {
			ElementFx.ring(level, c, ElementFx.tilted(i == 0 ? 0 : 1.2, i * Math.PI * 2 / 3), i == 0 ? ElementFx.VOID.secondary() : ElementFx.VOID.primary(), 0.3,
				2.6, 0.07, 9);
		}
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, c, 20, 0.5);
		Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 1.2F);
	}

	// ------------------------------------------------------------------ control

	private static final int DECREE = 0x8A1030;

	/** Decree: the words leave the caster's mouth as rings of force, from a little way out so they clear the caster's own view. */
	static void decreeSpoken(ServerLevel level, LivingEntity caster) {
		Vec3 mouth = caster.getEyePosition().subtract(0, 0.15, 0);
		Vec3 f = caster.getLookAngle();
		for (int t = 0; t < 3; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> ElementFx.ring(level, mouth.add(f.scale(1.6 + tick * 0.8)), f, tick == 1 ? CURSE : DECREE, 0.15, 0.4 + tick * 0.2, 0.03, 6));
		}
		dev.wildercord.cast.feel.Feels.sound(level, mouth, "arcane_gong", 0.9F, 1.0F);
	}

	/** The command lands: a thread of crimson to the target, and a seal hanging over its head while it's held. */
	static void decree(ServerLevel level, Vec3 from, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 d = c.subtract(from);
		double length = d.length();
		if (length > 2.0) {
			ElementFx.ray(level, from.add(d.scale(1.5 / length)), c, DECREE, 0.035, 8);
		}
		Vec3 head = target.position().add(0, target.getBbHeight() + 0.3, 0);
		ElementFx.ring(level, head, UP, CURSE, 0.8, 0.4, 0.04, 10);
		ElementFx.sigil(level, head, UP, SigilOption.CIRCLE, DECREE, 0.35, 40, 0.1);
		ElementFx.shimmer(level, c, 0.3, 8);
	}

	/** Weigh: dust raining down on the target and the ground pressed in round its feet. */
	static void weigh(ServerLevel level, Entity target, boolean start) {
		Vec3 top = target.position().add(0, target.getBbHeight() + 0.4, 0);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.ANVIL.defaultBlockState()), top, 3, target.getBbWidth() * 0.5, 0.0);
		ElementFx.groundRing(level, target.position(), ElementFx.EARTH.primary(), 0.9, 0.45, 0.05, 6);
		if (start) {
			ElementFx.crack(level, target.position(), 0.9, 30);
			ElementFx.ring(level, top, UP, ElementFx.EARTH.secondary(), 1.0, 0.3, 0.05, 8);
			dev.wildercord.cast.feel.Feels.sound(level, target.position(), "earth_press", 0.9F, 1.0F);
		}
	}

	private static final int STEEL = 0xA8A8B4;

	/** Shackle: a chain of grey light from where the target stood to where it is now. */
	static void chain(ServerLevel level, Vec3 anchor, Entity target, boolean bind) {
		Vec3 end = target.getBoundingBox().getCenter();
		Vec3 start = anchor.add(0, 0.1, 0);
		ElementFx.ray(level, start, end, STEEL, 0.035, 3);
		Vec3 d = end.subtract(start);
		int n = (int) Math.min(12, Math.max(2, d.length() / 0.5));
		for (int i = 1; i < n; i++) {
			dot(level, dust(i % 2 == 0 ? 0x5A5A64 : 0xC8C8D4, 0.7F), start.add(d.scale(i / (double) n)));
		}
		if (bind) {
			ElementFx.groundRing(level, anchor, STEEL, 0.9, 0.5, 0.05, 10);
			ElementFx.flatSigil(level, anchor, SigilOption.CIRCLE, STEEL, 0.5, 20, 0.05);
			Fx.sound(level, end, SoundEvents.CHAIN_PLACE, 1.0F, 0.8F);
		}
	}

	private static final int BUBBLE = 0xCFEFFF;

	/** Bubble: a shimmering sphere of water round the target, two great circles turning over it and a glint on top. */
	static void bubble(ServerLevel level, Entity target, boolean straining) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.65 + 0.15;
		if (straining) {
			// About to pop: the sphere swells and shivers, tiny bubbles breaking off it.
			r *= 1.0 + 0.08 * Math.sin(level.getGameTime() * 1.7);
			Vfx.emit(level, ParticleTypes.BUBBLE_POP, c, 3, r * 0.5, 0.02);
		}
		double spin = level.getGameTime() * 0.2;
		ElementFx.ring(level, c, ElementFx.tilted(1.1, spin), BUBBLE, r, r, 0.025, 4);
		ElementFx.ring(level, c, ElementFx.tilted(0.5, -spin * 1.3), BUBBLE, r, r, 0.02, 4);
		dot(level, dust(0xFFFFFF, 0.9F), c.add(-r * 0.4, r * 0.5, -r * 0.4));
	}

	static void bubblePop(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.65 + 0.15;
		dot(level, SigilOption.glow(BUBBLE, (float) (r * 1.8)), c);
		for (int i = 0; i < 3; i++) {
			ElementFx.ring(level, c, ElementFx.tilted(i == 0 ? 0 : 1.2, i * 2.1), i == 0 ? 0xFFFFFF : BUBBLE, r, r * 2.2, 0.04, 7);
		}
		Vfx.radial(level, ParticleTypes.SPLASH, c, 16, 0.3);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, c, 10, 0.5, 0.05);
		Feels.sound(level, c, "frost_bubble_pop", 1.0F, 1.0F);
	}

	// ------------------------------------------------------------------ support

	private static final int INFINITY = 0xE8E0FF;

	/** Infinity forms: a sphere of pale light settles round the target, three great circles on their own tilts. */
	static void infinityStart(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		for (int i = 0; i < 3; i++) {
			// The bubble's whole reach (Zeno's 5 blocks) draws in to the shell: glass-clear ripples that tighten inward.
			ElementFx.ring(level, c, ElementFx.tilted(i == 0 ? 0 : 1.1, i * Math.PI * 2 / 3), i == 1 ? ElementFx.VOID.secondary() : INFINITY, 4.6, 1.7, 0.03, 18);
		}
		// Under its feet too, where one warding themselves sees it.
		ElementFx.groundRing(level, target.position(), INFINITY, 2.4, 1.7, 0.04, 16);
		dot(level, SigilOption.glow(INFINITY, 1.6F), c);
		dev.wildercord.cast.feel.Feels.sound(level, target.position(), "void_zeno_beat", 1.0F, 1.0F);
	}

	/** Infinity holds: two faint great circles breathing round the target. */
	static void infinityShell(ServerLevel level, Vec3 c) {
		double spin = level.getGameTime() * 0.05;
		ElementFx.ring(level, c, ElementFx.tilted(1.1, spin), INFINITY, 1.7, 1.7, 0.02, 10);
		ElementFx.ring(level, c, ElementFx.tilted(1.1, spin + Math.PI / 2), ElementFx.VOID.secondary(), 1.7, 1.7, 0.015, 10);
		// A ripple drawn in from the edge of the bubble, and a slow heartbeat (time thickening).
		ElementFx.ring(level, c, ElementFx.tilted(0.2, spin), 0xFFFFFF, 4.4, 1.9, 0.015, 16);
		if (level.getGameTime() % 48 < 8) {
			dev.wildercord.cast.feel.Feels.sound(level, c, "void_zeno_beat", 0.35F, 1.0F);
		}
	}

	/** Where Infinity catches a projectile: a small ring of light closing on it. */
	static void infinityHalt(ServerLevel level, Vec3 at) {
		ElementFx.ring(level, at, ElementFx.randomDir(level.getRandom()), INFINITY, 0.5, 0.15, 0.03, 8);
		dot(level, SigilOption.glow(INFINITY, 0.5F), at);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 1.8F);
	}

	static void reversalMark(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		ElementFx.leafSpiral(level, base, 0.55, target.getBbHeight() + 0.3, 5);
		ElementFx.flatSigil(level, base, SigilOption.STAR, ElementFx.LIFE.primary(), 0.8, 24, 0.06);
		Vfx.emit(level, ParticleTypes.TOTEM_OF_UNDYING, target.getBoundingBox().getCenter(), 6, 0.3, 0.1);
		dev.wildercord.cast.feel.Feels.sound(level, target.position(), "life_ripen", 0.7F, 0.75F);
	}

	private static final int TOTEM = 0xF5D86A;

	/** A killing blow reversed: a great green bloom, rings of life rising, a leaf spiral and a burst of totem light. */
	static void reversal(ServerLevel level, Entity target) {
		LifeArcaneFx.reversalPayoff(level, target);	}

	/** Reflect: a faceted shell of mirror light round the target and a star seal under it. */
	static void reflectMark(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(0.8, target.getBbHeight() * 0.55);
		double a = level.getRandom().nextDouble() * Math.PI;
		for (int i = 0; i < 3; i++) {
			double b = a + i * Math.PI / 3;
			ElementFx.ring(level, c, new Vec3(Math.cos(b), 0, Math.sin(b)), i == 1 ? ElementFx.ARCANE.secondary() : ElementFx.ARCANE.accent(), r * 1.3, r, 0.03, 12);
		}
		ElementFx.starSeal(level, target.position().add(0, 0.07, 0), UP, 0.7, 14);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE), c, 8, 0.15);
		dev.wildercord.cast.feel.Feels.sound(level, c, "arcane_mirror", 0.9F, 1.0F);
	}

	/** The hurt thrown back: a beam of light to the attacker and glass breaking round it. */
	static void reflect(ServerLevel level, Entity from, Entity attacker) {
		Vec3 a = from.getBoundingBox().getCenter();
		Vec3 b = attacker.getBoundingBox().getCenter();
		Vec3 d = b.subtract(a);
		double length = d.length();
		// From a little way out, so the one reflecting sees it leave.
		Vec3 start = length > 1.8 ? a.add(d.scale(1.2 / length)) : a;
		ElementFx.ray(level, start, b, ElementFx.ARCANE.primary(), 0.06, 8);
		ElementFx.ray(level, start, b, ElementFx.ARCANE.secondary(), 0.025, 7);
		dot(level, SigilOption.glow(ElementFx.ARCANE.primary(), 1.4F), b);
		ElementFx.ring(level, b, d, ElementFx.ARCANE.secondary(), 0.2, 1.0, 0.04, 7);
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE), b, 6, 0.3, 0.1);
		dev.wildercord.cast.feel.Feels.sound(level, b, "arcane_mirror_crack", 0.9F, 1.0F);
	}

	static void foresightMark(ServerLevel level, Entity target, int charges) {
		// A halo over the head with a pip for every blow it will sidestep (nothing at the eyes: you would not see it).
		TimeFx.halo(level, target, charges, 30);
		dev.wildercord.cast.feel.Feels.sound(level, target.position().add(0, target.getBbHeight() + 0.25, 0), "time_tick", 0.7F, 1.0F);
	}

	/** Foresight: a golden afterimage where the blow was meant to land, its clock stopped, and a streak to where you stepped. */
	static void dodge(ServerLevel level, Entity entity, Vec3 from, Vec3 to) {
		silhouette(level, dust(TIME, 0.8F), from, entity.getLookAngle(), entity.getBbHeight() / 1.8);
		TimeFx.stoppedFace(level, from.add(0, entity.getBbHeight() * 0.55, 0), entity.getLookAngle(), 0.5, level.getRandom().nextDouble() * Math.PI * 2, 10);
		TimeFx.pipSpent(level, entity);
		if (from.distanceToSqr(to) > 0.01) {
			Vec3 a = from.add(0, 1, 0);
			Vec3 b = to.add(0, 1, 0);
			double length = b.distanceTo(a);
			if (length > 0.8) {
				ElementFx.ray(level, a, b.subtract(b.subtract(a).scale(0.6 / length)), TIME, 0.05, 8);
			}
			Fx.send(level, new net.minecraft.core.particles.TrailParticleOption(b, TIME, 6), from.x, from.y + 1, from.z, 3, 0.2, 0.3, 0.2, 0);
		}
		dev.wildercord.cast.feel.Feels.sound(level, to, "time_dodge", 0.9F, 1.0F);
	}

	static void restore(ServerLevel level, Entity target, boolean mended) {
		LifeArcaneFx.restore(level, target, mended);	}

	/** Accelerate: a clock face round the target with its hands racing, another hand sweeping on a tilt, and gold at its feet. */
	static void accelerate(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		ElementFx.clock(level, c, UP, 0.85, 5, false);
		Vec3 tilt = ElementFx.tilted(1.0, level.getRandom().nextDouble() * Math.PI * 2);
		ElementFx.slash(level, c, tilt, ElementFx.inPlane(tilt, 0), ElementFx.TIME.secondary(), 0.6, Math.PI * 1.9, 0.05, 3, 6);
		ElementFx.groundRing(level, target.position(), TIME, 0.2, 1.5, 0.05, 8);
		Vfx.emit(level, ParticleTypes.END_ROD, c, 6, 0.5, 0.05);
		// Speed lines streaming back from whoever is hurried (a tempo buff, not a potion).
		Vec3 back = flat(target.getLookAngle()).scale(-1);
		for (int i = 0; i < 4; i++) {
			double y = 0.3 + level.getRandom().nextDouble() * (target.getBbHeight() - 0.4);
			Vec3 a = target.position().add(0, y, 0).add(back.scale(0.3));
			ElementFx.ray(level, a, a.add(back.scale(1.2 + level.getRandom().nextDouble())), (i & 1) == 0 ? TIME : 0xFFFFFF, 0.03, 6);
		}
		dev.wildercord.cast.feel.Feels.sound(level, c, "time_run", 0.9F, 1.0F);
	}

	// ------------------------------------------------------------------ movement

	static void swap(ServerLevel level, Vec3 a, Vec3 b) {
		LifeArcaneFx.swap(level, a, b);	}

	private static final int ZIP = 0xFFE070;

	/** Zipper: a seam of gold light down the wall, two rows of teeth opening either side of it, and closing behind you. */
	static void zipper(ServerLevel level, Vec3 entry, Vec3 exit, Vec3 dir) {
		Vec3 side = flat(dir).cross(UP).normalize();
		for (Vec3 at : List.of(entry, exit)) {
			ElementFx.ray(level, at.add(0, 0.95, 0), at.add(0, -0.95, 0), ZIP, 0.05, 12);
			for (int i = -6; i <= 6; i++) {
				Vec3 p = at.add(0, i * 0.14, 0);
				double open = 0.08 + (6 - Math.abs(i)) * 0.02;
				dot(level, dust(i % 2 == 0 ? 0xD8B040 : 0x404048, 0.7F), p.add(side.scale(open)));
				dot(level, dust(i % 2 == 0 ? 0x404048 : 0xD8B040, 0.7F), p.add(side.scale(-open)));
			}
			dot(level, SigilOption.glow(ZIP, 0.6F), at.add(0, 0.9, 0));
		}
		// The teeth are open for a moment and then close behind you, from the ends toward the middle.
		Scheduler.later(8, () -> {
			VoidFx.zipShut(level, entry, dir, 0xD8B040, 0x404048);
			VoidFx.zipShut(level, exit, dir, 0xD8B040, 0x404048);
		});
		dev.wildercord.cast.feel.Feels.sound(level, entry, "void_unzip", 1.0F, 1.0F);
	}

	/** Shadowstep: darkness implodes where you were and where you arrive, and a black afterimage is left behind. */
	static void shadowstep(ServerLevel level, Vec3 from, Vec3 to) {
		// A stretched smear, not Blink's implosion: black dust from where you were, thick at the start, and ink where you land.
		VoidFx.smear(level, from, to);
		ElementFx.blackCore(level, to.add(0, 1, 0), 0.25, 6);
		ElementFx.groundRing(level, to, ElementFx.dark(ElementFx.VOID.accent()), 1.4, 0.2, 0.1, 10);
		silhouette(level, dust(BLACK, 1.0F), from, to.subtract(from), 1.0);
	}

	/** Stasis: the hands of a clock race round the target and stop; time closes in on it. */
	static void stasisStart(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(0.7, target.getBbWidth() * 0.9);
		dot(level, flash(0xFFFFFF), c);
		ElementFx.ring(level, c, UP, 0xFFFFFF, r * 2.2, r, 0.05, 8);
	}

	/** The hold: the column of frozen sand is drawn once (it lasts the hold); a few grains fall every so often. */
	static void stasisTick(ServerLevel level, Entity target, int ticksLeft) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.emit(level, dust(0xB8B8C4, 0.8F), c, 1, target.getBbWidth() * 0.6, 0.0);
	}

	/**
	 * A hit held in Stasis: a frozen crack of light where it struck, hanging in the air, and a
	 * pale ring that tightens with every hit waiting to land.
	 */
	static void stasisStore(ServerLevel level, Entity target, int hits) {
		// Six held blows a tick are drawn as shards; the rest still count but cost nothing on the wire.
		if (!TimeFx.allow(level, "stasis_shard", 6)) {
			return;
		}
		TimeFx.stasisShard(level, target, 100);
		dev.wildercord.cast.feel.Feels.sound(level, target.position(), "time_tick", 0.6F, dev.wildercord.cast.feel.Feels.step(Math.min(5, hits / 2)));
		if (true) {
			return;
		}
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 at = c.add((level.getRandom().nextDouble() - 0.5) * target.getBbWidth(), (level.getRandom().nextDouble() - 0.5) * target.getBbHeight() * 0.8,
			(level.getRandom().nextDouble() - 0.5) * target.getBbWidth());
		for (int i = 0; i < 4; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 dir = new Vec3(Math.cos(a), (level.getRandom().nextDouble() - 0.5) * 1.4, Math.sin(a)).normalize().scale(0.35);
			ElementFx.ray(level, at, at.add(dir), 0xFFFFFF, 0.025, 30);
		}
		dot(level, SigilOption.glow(TIME, 0.5F), at);
		double r = Math.max(0.5, 1.4 - hits * 0.08);
		ElementFx.ring(level, c, UP, 0xE8E4FF, r + 0.3, r, 0.03, 10);
		Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.7F, 1.4F + Math.min(0.6F, hits * 0.05F));
	}

	/** Time moves again: the hands spin once and everything held lands at once. */
	static void timeResumes(ServerLevel level, Entity target, float stored) {
		// The column shatters outward and everything held lands as one flash (time_resume is the sound).
		TimeFx.stasisRelease(level, target, stored);
		Vfx.radial(level, ParticleTypes.CRIT, target.getBoundingBox().getCenter(), (int) Math.min(30, 6 + stored), 0.5);
		Vfx.shockwave(level, target.position(), 1.6, Vfx.theme("time"), 4);
	}

	/** Rewind: golden streaks run back to where you were, and a clock's hands turn backward there. */
	static void rewind(ServerLevel level, Vec3 from, Vec3 to) {
		double length = to.distanceTo(from);
		if (length > 0.5) {
			// The way back lights up dot by dot (time running backward), with an afterimage where you stood.
			TimeFx.rewindPath(level, from, to);
			silhouette(level, dust(TIME, 0.8F), from, to.subtract(from), 1.0);
			dev.wildercord.cast.feel.Feels.sound(level, to, "time_rewind", 1.0F, 1.0F);
		} else {
			TimeFx.face(level, to.add(0, 1, 0), new Vec3(0, 0, 1), 0.9, 10, true);
			ElementFx.goldenTicks(level, to.add(0, 1, 0), 0.4, 6);
		}
	}

	private static final int SKIP = 0xB02030;

	/** Time Skip: a crimson afterimage stays behind for a moment with a clock stopped in it; you're already elsewhere. */
	static void timeSkip(ServerLevel level, Vec3 from, Vec3 to) {
		// Gold frames flickering out of the film: nothing crimson (that is blood's colour).
		TimeFx.skip(level, from, to);
		dev.wildercord.cast.feel.Feels.sound(level, from, "time_stutter", 1.0F, 1.0F);
	}

	// ------------------------------------------------------------------ summons and links

	static void shadeRise(ServerLevel level, Vec3 at) {
		ElementFx.groundRing(level, at, ElementFx.dark(ElementFx.VOID.accent()), 0.2, 1.2, 0.12, 14);
		ElementFx.flatSigil(level, at, SigilOption.CIRCLE, ElementFx.VOID.primary(), 0.9, 20, -0.06);
		ElementFx.implode(level, at.add(0, 0.5, 0), 1.0, 8);
		Vfx.radial(level, ParticleTypes.SQUID_INK, at.add(0, 0.4, 0), 10, 0.15);
		Motes.clouds(level, at.add(0, 0.5, 0), 3, 0.3, 0x3A3040, 1.0, 30, new Vec3(0, 0.02, 0), 0.03, 0.4);
		dev.wildercord.cast.feel.Feels.sound(level, at, "void_hound_growl", 0.9F, 1.0F);
	}

	static void shadeAura(ServerLevel level, Entity wolf) {
		ElementFx.groundRing(level, wolf.position(), ElementFx.dark(ElementFx.VOID.accent()), 0.7, 0.2, 0.08, 10);
		Vfx.emit(level, ParticleTypes.SQUID_INK, wolf.getBoundingBox().getCenter(), 1, 0.25, 0.01);
	}

	/** The Thunderbird: a bright body, crescent wings of light beating and a tail, heading round its circle. */
	static void thunderbird(ServerLevel level, Vec3 bird, double angle, int tick) {
		Vec3 heading = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
		Vec3 wing = new Vec3(Math.cos(angle), 0, Math.sin(angle));
		double flap = Math.sin(tick * 0.9);
		ElementFx.orb(level, bird, ElementFx.STORM.primary(), 0.16, 3);
		for (int side = -1; side <= 1; side += 2) {
			// An arch over each wing, in the plane across the heading: its tip rises and falls with the beat.
			Vec3 shoulder = bird.add(wing.scale(side * 0.3)).add(0, flap * 0.08 - 0.15, 0);
			Vec3 toward = UP.add(wing.scale(side * flap * 0.5)).normalize();
			ElementFx.slash(level, shoulder, heading, toward, ElementFx.STORM.primary(), 0.38, 1.8, 0.07, 1, 3);
		}
		ElementFx.ray(level, bird.subtract(heading.scale(0.1)), bird.subtract(heading.scale(0.6)).add(0, 0.05, 0), ElementFx.STORM.secondary(), 0.05, 3);
		if (tick % 12 == 0) {
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, bird, 2, 0.2, 0.05);
		}
		if (tick % 40 == 0) {
			dev.wildercord.cast.feel.Feels.sound(level, bird, "storm_hum", 0.35F, 1.0F);
		}
	}

	static void birdStrike(ServerLevel level, Vec3 from, Vec3 to) {
		ElementFx.bolt(level, from, to, 0.05, 0, 2);
		ElementFx.groundRing(level, to.subtract(0, 0.9, 0), ElementFx.STORM.primary(), 0.2, 1.4, 0.05, 6);
		ElementFx.sparks(level, to, 6, 0.3);
		dev.wildercord.cast.feel.Feels.sound(level, to, "storm_zap", 0.8F, 1.26F);
	}

	static void birdFade(ServerLevel level, Vec3 at) {
		dot(level, SigilOption.glow(ElementFx.STORM.primary(), 1.4F), at);
		ElementFx.ring(level, at, UP, ElementFx.STORM.primary(), 0.2, 1.2, 0.04, 8);
		Vfx.emit(level, ParticleTypes.POOF, at, 4, 0.2, 0.02);
		ElementFx.sparks(level, at, 10, 0.3);
	}

	/** If Airborne fired: a ring of wind under your feet. */
	static void airborne(ServerLevel level, LivingEntity caster) {
		ElementFx.ring(level, caster.position().add(0, -0.05, 0), UP, ElementFx.WIND.secondary(), 0.3, 1.2, 0.04, 8);
		ElementFx.ring(level, caster.position().add(0, -0.05, 0), UP, ElementFx.WIND.accent(), 0.2, 0.8, 0.03, 10);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, caster.position(), 2, 0.3, 0.02);
	}

	/** Twin Star, armed: two stars trace orbits round the caster, and a star seal turns at its feet. */
	static void twinStarMark(ServerLevel level, LivingEntity caster) {
		ElementFx.orbit(level, caster.position().add(0, 1.1, 0), 0.8, 2, 12, 0xFFE8FF, ElementFx.ARCANE.primary());
		ElementFx.starSeal(level, caster.position().add(0, 0.07, 0), UP, 0.6, 20);
		dev.wildercord.cast.feel.Feels.sound(level, caster.position(), "arcane_twin", 0.8F, 1.0F);
	}

	/** Twin Star's second cast: a flash of starlight at the hands and a star under your feet. */
	static void twinStar(ServerLevel level, LivingEntity caster) {
		Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.8)).add(0, -0.3, 0);
		dot(level, flash(0xE678DC), hand);
		ElementFx.ring(level, hand, caster.getLookAngle(), ElementFx.ARCANE.secondary(), 0.1, 0.7, 0.03, 7);
		ElementFx.groundRing(level, caster.position(), ElementFx.ARCANE.primary(), 0.6, 0.2, 0.04, 12);
		Vfx.radial(level, ParticleTypes.END_ROD, hand, 8, 0.12);
		dev.wildercord.cast.feel.Feels.sound(level, hand, "arcane_twin_echo", 0.8F, 1.0F);
	}

	private static final int BEAT = 0xF5D56A;

	/** On the beat: notes rise and a gold ring pulses out along the ground, one note per step of the chain. */
	static void rhythm(ServerLevel level, LivingEntity caster, int stacks) {
		Vec3 c = caster.position().add(0, 2.1, 0);
		for (int i = 0; i < stacks; i++) {
			double a = Math.PI * 2 * i / stacks;
			Fx.send(level, ParticleTypes.NOTE, c.x + Math.cos(a) * 0.5, c.y, c.z + Math.sin(a) * 0.5, 0, (0.2 + 0.25 * i) / 1.0, 0, 0, 1.0);
		}
		ElementFx.groundRing(level, caster.position(), BEAT, 0.3, 0.9 + 0.3 * stacks, 0.04 + 0.01 * stacks, 10);
	}

	private static final int GOLD = 0xF0C440;

	/** Combo fired: a golden burst round you. */
	static void combo(ServerLevel level, LivingEntity caster) {
		Vec3 c = caster.position().add(0, 1, 0);
		ElementFx.groundRing(level, caster.position(), GOLD, 0.3, 1.6, 0.06, 10);
		ElementFx.groundRing(level, caster.position(), 0xFFF4C0, 0.2, 1.1, 0.035, 12);
		dot(level, SigilOption.glow(GOLD, 1.6F), c);
		Vfx.radial(level, ParticleTypes.WAX_ON, c, 12, 0.3);
		Fx.sound(level, c, SoundEvents.PLAYER_LEVELUP, 0.35F, 1.8F);
	}
}
