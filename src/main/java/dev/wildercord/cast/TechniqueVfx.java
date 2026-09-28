package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Visuals for the batch 4 runes, built from the same vanilla-particle primitives as {@link Vfx}.
 * Colours: time is pale gold, blood is crimson, and the black of Blackspark and Blackflame is a
 * near-black dust edged with its element's glow so it still reads at night.
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

	/** Dots along a line, {@code step} blocks apart (at most 60). */
	private static void line(ServerLevel level, ParticleOptions p, Vec3 a, Vec3 b, double step) {
		Vec3 d = b.subtract(a);
		double length = d.length();
		int n = (int) Math.min(60, Math.max(1, length / step));
		for (int i = 0; i <= n; i++) {
			dot(level, p, a.add(d.scale(i / (double) n)));
		}
	}

	/** Points spread evenly over a sphere (or its upper half), turned by {@code spin}. */
	private static void sphere(ServerLevel level, ParticleOptions p, Vec3 c, double r, int points, double spin, boolean upperHalf) {
		for (int i = 0; i < points; i++) {
			double y = upperHalf ? 1 - (i + 0.5) / points : 1 - (i + 0.5) * 2.0 / points;
			double rr = Math.sqrt(Math.max(0, 1 - y * y));
			double a = i * 2.39996323 + spin;
			dot(level, p, c.add(Math.cos(a) * rr * r, y * r, Math.sin(a) * rr * r));
		}
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
		Fx.sound(level, c, SoundEvents.END_PORTAL_SPAWN, 0.45F, 1.3F);
		Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.7F);
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
		Fx.sound(level, c, theme.impact(), 0.8F, 0.7F);
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
		Fx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.8F);
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
			Fx.sound(level, pos, SoundEvents.BEACON_AMBIENT, 0.8F, 1.4F);
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
		Fx.sound(level, b, theme.cast(), 0.5F, 1.4F);
	}

	// ------------------------------------------------------------------ damage

	static void cleave(ServerLevel level, Entity target, Vec3 look) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 side = flat(look).cross(UP).normalize();
		double size = Math.max(0.9, target.getBbHeight() * 0.6);
		for (int i = -10; i <= 10; i++) {
			double s = i / 10.0;
			Vec3 p = c.add(side.scale(s * size)).add(0, -s * size * 0.8, 0);
			dot(level, dust(BLOOD, 1.5F - 0.8F * (float) Math.abs(s)), p);
		}
		dot(level, ParticleTypes.SWEEP_ATTACK, c);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.REDSTONE), c, 4, 0.15);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 0.7F);
	}

	static void dismantle(ServerLevel level, Entity target, int slash) {
		Vec3 c = target.getBoundingBox().getCenter();
		double a = level.getRandom().nextDouble() * Math.PI;
		Vec3 dir = new Vec3(Math.cos(a), Math.sin(a) * 0.8, Math.sin(a + 1.3)).normalize();
		double size = Math.max(0.7, target.getBbHeight() * 0.55);
		line(level, dust(0xFFE0E4, 0.6F), c.subtract(dir.scale(size)), c.add(dir.scale(size)), 0.12);
		line(level, dust(BLOOD, 0.8F), c.subtract(dir.scale(size * 0.7)), c.add(dir.scale(size * 0.7)), 0.2);
		Vfx.emit(level, dust(0x8A0A1A, 0.9F), c, 3, 0.2, 0.0);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.6F + slash * 0.15F);
	}

	/** Blackspark: on a true hit, black lightning edged in crimson strikes into the target. */
	static void blackspark(ServerLevel level, Entity target, boolean spark) {
		Vec3 c = target.getBoundingBox().getCenter();
		if (!spark) {
			Vfx.radial(level, ParticleTypes.CRIT, c, 8, 0.25);
			Vfx.emit(level, dust(0x3A1060, 1.2F), c, 6, 0.25, 0.0);
			Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 0.8F);
			return;
		}
		dot(level, flash(BLOOD), c);
		for (int bolt = 0; bolt < 3; bolt++) {
			Vec3 p = c.add((level.getRandom().nextDouble() - 0.5) * 2.4, 1.6 + level.getRandom().nextDouble(), (level.getRandom().nextDouble() - 0.5) * 2.4);
			for (int seg = 0; seg < 5; seg++) {
				Vec3 next = seg == 4 ? c : p.add(c.subtract(p).scale(0.3)).add((level.getRandom().nextDouble() - 0.5) * 0.5, 0,
					(level.getRandom().nextDouble() - 0.5) * 0.5);
				line(level, dust(BLACK, 1.3F), p, next, 0.12);
				line(level, dust(BLOOD, 0.6F), p.add(0.06, 0.06, 0), next.add(0.06, 0.06, 0), 0.25);
				p = next;
			}
		}
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, c, 24, 0.5);
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.REDSTONE), c, 4, 0.25);
		Fx.sound(level, c, SoundEvents.TRIDENT_THUNDER, 0.6F, 1.7F);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.6F);
	}

	static void aftershock(ServerLevel level, Entity target, boolean second) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.Theme earth = Vfx.theme("earth");
		if (!second) {
			dot(level, earth.flash(), c);
			Vfx.radial(level, ParticleTypes.CRIT, c, 10, 0.3);
			Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_STRONG, 0.9F, 0.7F);
			return;
		}
		dot(level, flash(0xFFE0B0), c);
		Vfx.shockwave(level, target.position(), 1.8, earth, 4);
		Scheduler.later(2, () -> Vfx.shockwave(level, target.position(), 2.6, earth, 4));
		Vfx.radial(level, earth.spark(), c, 14, 0.3);
		Fx.sound(level, c, SoundEvents.MACE_SMASH_GROUND, 1.0F, 1.1F);
	}

	/** Resonance: a nail rings out and threads of cursed energy run to every marked enemy. */
	static void resonance(ServerLevel level, Entity target, List<? extends Entity> linked) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.Theme arcane = Vfx.theme("arcane");
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET), c, 8, 0.15);
		Vfx.ring(level, arcane.dust(1.1F), c, 0.6, 12);
		for (Entity other : linked) {
			Vec3 o = other.getBoundingBox().getCenter();
			line(level, dust(0x8A1848, 0.6F), c, o, 0.4);
			dot(level, arcane.flash(), o);
			Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET), o, 5, 0.12);
		}
		Fx.sound(level, c, SoundEvents.ANVIL_LAND, 0.3F, 1.9F);
		Fx.sound(level, c, SoundEvents.BELL_RESONATE, 0.5F, 1.6F);
	}

	/** Ripple: golden rings of sunlight run out across the target. */
	static void ripple(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		for (int t = 0; t < 3; t++) {
			double r = 0.4 + t * 0.35;
			Scheduler.later(t + 1, () -> Vfx.ring(level, new DustColorTransitionOptions(0xFFD050, 0xFF7A20, 1.2F), c, r, 14));
		}
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, c, 12, 0.3);
		Vfx.emit(level, ParticleTypes.WAX_ON, c, 8, 0.4, 0.0);
		Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.8F);
	}

	static void primed(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		dot(level, flash(0xFF6EC7), c);
		Vfx.ring(level, dust(0xFF6EC7, 1.2F), c, 0.7, 14);
		Vfx.emit(level, ParticleTypes.SMOKE, c, 6, 0.3, 0.01);
	}

	static void primerTick(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.emit(level, dust(0xFF6EC7, 0.9F), c, 4, 0.35, 0.0);
		Vfx.emit(level, ParticleTypes.SMOKE, target.position().add(0, target.getBbHeight() + 0.2, 0), 2, 0.05, 0.01);
		Fx.sound(level, c, SoundEvents.TRIPWIRE_CLICK_ON, 0.6F, 2.0F);
	}

	/** Blackflame: black fire licking up the target, edged with violet. */
	static void blackflame(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double w = target.getBbWidth() * 0.6;
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + level.getRandom().nextDouble();
			Vec3 p = base.add(Math.cos(a) * w, 0.1 + level.getRandom().nextDouble() * target.getBbHeight(), Math.sin(a) * w);
			dot(level, dust(BLACK, 1.6F), p);
			Vfx.fling(level, ParticleTypes.SQUID_INK, p, UP, 0.08);
		}
		Vfx.emit(level, dust(0x5A1A8A, 0.9F), target.getBoundingBox().getCenter(), 4, w, 0.0);
		Vfx.emit(level, ParticleTypes.LARGE_SMOKE, target.getBoundingBox().getCenter(), 2, w, 0.02);
		Fx.sound(level, base, SoundEvents.FIRE_AMBIENT, 0.6F, 0.6F);
	}

	/** Hollow: a red and a blue orb spiral together, then the gap collapses in a violet flash. */
	static void hollow(ServerLevel level, Vec3 c, double radius) {
		Vec3 side = new Vec3(1, 0, 0);
		for (int t = 0; t < 6; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double k = 1 - (tick + 1) / 6.0;
				double a = tick * 0.6;
				Vec3 offset = side.yRot((float) a).scale(1.8 * k).add(0, 0.2 * k, 0);
				Vfx.emit(level, dust(0xFF3030, 2.0F), c.add(offset), 5, 0.12, 0.0);
				Vfx.emit(level, dust(0x3050FF, 2.0F), c.subtract(offset), 5, 0.12, 0.0);
			});
		}
		Scheduler.later(7, () -> {
			dot(level, flash(0xB45AF0), c);
			dot(level, ParticleTypes.SONIC_BOOM, c);
			sphere(level, dust(0x9A3AF0, 1.8F), c, radius * 0.6, 40, 0.0, false);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, c, 30, 0.6);
			Vfx.shockwave(level, c.subtract(0, 0.8, 0), radius, Vfx.theme("void"), 5);
		});
		Fx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 1.0F, 0.5F);
		Scheduler.later(7, () -> Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.6F));
	}

	/** Repel: a red shell bursting outward with gusts. */
	static void repel(ServerLevel level, Vec3 c, double radius) {
		dot(level, flash(0xFF5050), c);
		for (int t = 0; t < 3; t++) {
			double r = radius * (t + 1) / 3;
			Scheduler.later(t + 1, () -> sphere(level, new DustColorTransitionOptions(0xFF4040, 0xFFB0A0, 1.3F), c, r, (int) Math.max(20, r * r * 3), 0.0, false));
		}
		Vfx.radial(level, ParticleTypes.GUST, c, 6, 0.3);
		Vfx.radial(level, ParticleTypes.SMALL_GUST, c, 14, 0.5);
		Fx.sound(level, c, SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 0.7F);
		Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.4F);
	}

	/** Collapse (Repel meeting a pull): the two forces annihilate in a violet burst. */
	static void collapse(ServerLevel level, Vec3 c) {
		dot(level, flash(0xB45AF0), c);
		dot(level, ParticleTypes.SONIC_BOOM, c);
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, c, 24, 0.5);
		sphere(level, dust(0x9A3AF0, 1.5F), c, 1.2, 24, 0.0, false);
		Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 1.2F);
	}

	// ------------------------------------------------------------------ control

	/** Decree: the words leave the caster's mouth as rings of force. */
	static void decreeSpoken(ServerLevel level, LivingEntity caster) {
		Vec3 mouth = caster.getEyePosition().subtract(0, 0.15, 0);
		Vec3 f = caster.getLookAngle();
		for (int t = 0; t < 3; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 at = mouth.add(f.scale(0.8 + tick * 0.6));
				Vec3 r = flat(f).cross(UP).normalize();
				for (int i = 0; i < 10; i++) {
					double a = Math.PI * 2 * i / 10;
					double rad = 0.25 + tick * 0.15;
					dot(level, dust(0x8A1030, 0.8F), at.add(r.scale(Math.cos(a) * rad)).add(0, Math.sin(a) * rad, 0));
				}
			});
		}
		Fx.sound(level, mouth, SoundEvents.ELDER_GUARDIAN_CURSE, 0.35F, 1.8F);
	}

	static void decree(ServerLevel level, Vec3 from, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Fx.send(level, Vfx.theme("arcane").trail(c, 8), from.x, from.y, from.z, 4, 0.1, 0.1, 0.1, 0);
		Vfx.ring(level, dust(0x8A1030, 1.0F), target.position().add(0, target.getBbHeight() + 0.3, 0), 0.4, 12);
		Vfx.emit(level, ParticleTypes.ENCHANT, c, 12, 0.4, 0.3);
	}

	/** Weigh: dust raining down on the target and a heavy ring at its feet. */
	static void weigh(ServerLevel level, Entity target, boolean start) {
		Vec3 top = target.position().add(0, target.getBbHeight() + 0.4, 0);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.ANVIL.defaultBlockState()), top, 4, target.getBbWidth() * 0.5, 0.0);
		Vfx.ring(level, dust(0x4A3A2A, 1.2F), target.position().add(0, 0.1, 0), 0.6, 10);
		if (start) {
			Vfx.shockwave(level, target.position(), 1.4, Vfx.theme("earth"), 3);
			Fx.sound(level, target.position(), SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
		}
	}

	/** Shackle: a chain from where the target stood to where it is now. */
	static void chain(ServerLevel level, Vec3 anchor, Entity target, boolean bind) {
		Vec3 end = target.getBoundingBox().getCenter();
		Vec3 d = end.subtract(anchor.add(0, 0.1, 0));
		int n = (int) Math.min(30, Math.max(3, d.length() / 0.25));
		for (int i = 0; i <= n; i++) {
			dot(level, dust(i % 2 == 0 ? 0x5A5A64 : 0xA8A8B4, 0.7F), anchor.add(0, 0.1, 0).add(d.scale(i / (double) n)));
		}
		if (bind) {
			Vfx.ring(level, dust(0x7A7A86, 1.0F), anchor.add(0, 0.08, 0), 0.5, 10);
			Fx.sound(level, end, SoundEvents.CHAIN_PLACE, 1.0F, 0.8F);
		}
	}

	static void bubble(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		double r = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.65 + 0.15;
		sphere(level, dust(0xCFEFFF, 0.7F), c, r, 18, level.getGameTime() * 0.2, false);
		dot(level, dust(0xFFFFFF, 0.9F), c.add(-r * 0.4, r * 0.5, -r * 0.4));
	}

	static void bubblePop(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.radial(level, ParticleTypes.SPLASH, c, 20, 0.3);
		Vfx.emit(level, ParticleTypes.BUBBLE_POP, c, 12, 0.5, 0.05);
		Fx.sound(level, c, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0F, 1.0F);
		Fx.sound(level, c, SoundEvents.PLAYER_SPLASH, 0.5F, 1.6F);
	}

	// ------------------------------------------------------------------ support

	static void infinityStart(ServerLevel level, Entity target) {
		sphere(level, dust(0xE8E0FF, 0.8F), target.getBoundingBox().getCenter(), 1.7, 30, 0.0, false);
		Fx.sound(level, target.position(), SoundEvents.BEACON_ACTIVATE, 0.6F, 1.9F);
	}

	static void infinityShell(ServerLevel level, Vec3 c) {
		sphere(level, dust(0xE8E0FF, 0.55F), c, 1.7, 14, level.getGameTime() * 0.1, false);
	}

	/** Where Infinity catches a projectile: a small ring in the air. */
	static void infinityHalt(ServerLevel level, Vec3 at) {
		Vfx.ring(level, dust(0xE8E0FF, 0.7F), at, 0.3, 8);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 1.8F);
	}

	static void reversalMark(ServerLevel level, Entity target) {
		Vfx.helix(level, target.position(), 0.5, target.getBbHeight() + 0.3, Vfx.theme("life"), 8);
		Vfx.emit(level, ParticleTypes.TOTEM_OF_UNDYING, target.getBoundingBox().getCenter(), 8, 0.3, 0.1);
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.2F);
	}

	static void reversal(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		dot(level, flash(0x6EDC64), c);
		Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, c, 40, 0.5);
		Vfx.helix(level, target.position(), 0.7, target.getBbHeight() + 0.6, Vfx.theme("life"), 10);
		Fx.sound(level, c, SoundEvents.TOTEM_USE, 0.9F, 1.1F);
	}

	static void reflectMark(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE), c, 10, 0.15);
		Vfx.ring(level, Vfx.theme("arcane").dust(1.0F), c, 0.8, 14);
		Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.4F);
	}

	static void reflect(ServerLevel level, Entity from, Entity attacker) {
		Vec3 a = from.getBoundingBox().getCenter();
		Vec3 b = attacker.getBoundingBox().getCenter();
		Fx.send(level, Vfx.theme("arcane").trail(b, 6), a.x, a.y, a.z, 4, 0.1, 0.1, 0.1, 0);
		Vfx.emit(level, new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE), b, 6, 0.3, 0.1);
		Fx.sound(level, b, SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.6F);
	}

	static void overdrive(ServerLevel level, Entity target, boolean start) {
		Vec3 c = target.getBoundingBox().getCenter();
		if (start) {
			Vfx.shockwave(level, target.position(), 1.3, Vfx.theme("blood"), 3);
			Vfx.emit(level, dust(BLOOD, 1.2F), c, 10, 0.4, 0.0);
			Fx.sound(level, c, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.4F);
		} else {
			Vfx.emit(level, ParticleTypes.DAMAGE_INDICATOR, c, 2, 0.2, 0.05);
			Vfx.emit(level, dust(BLOOD, 1.0F), c, 4, 0.3, 0.0);
		}
	}

	static void foresightMark(ServerLevel level, Entity target) {
		Vec3 head = target.position().add(0, target.getBbHeight() + 0.25, 0);
		Vfx.ring(level, dust(TIME, 1.0F), head, 0.45, 14);
		Vfx.emit(level, ParticleTypes.END_ROD, head, 6, 0.3, 0.02);
		Fx.sound(level, head, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.6F, 1.6F);
	}

	/** Foresight: a golden afterimage where the blow was meant to land. */
	static void dodge(ServerLevel level, Entity entity, Vec3 from, Vec3 to) {
		silhouette(level, dust(TIME, 0.8F), from, entity.getLookAngle(), entity.getBbHeight() / 1.8);
		if (from.distanceToSqr(to) > 0.01) {
			Fx.send(level, new net.minecraft.core.particles.TrailParticleOption(to.add(0, 1, 0), TIME, 6), from.x, from.y + 1, from.z, 4, 0.2, 0.3, 0.2, 0);
		}
		Fx.sound(level, to, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.4F);
	}

	static void restore(ServerLevel level, Entity target, boolean mended) {
		Vfx.helix(level, target.position(), 0.55, target.getBbHeight() + 0.4, Vfx.theme("life"), 8);
		Vfx.emit(level, ParticleTypes.HEART, target.position().add(0, target.getBbHeight() + 0.3, 0), 2, 0.3, 0.0);
		if (mended) {
			Vfx.emit(level, ParticleTypes.WAX_ON, target.getBoundingBox().getCenter(), 10, 0.4, 0.0);
			Fx.sound(level, target.position(), SoundEvents.SMITHING_TABLE_USE, 0.6F, 1.4F);
		}
		Fx.sound(level, target.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.2F);
	}

	static void accelerate(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		for (int t = 0; t < 6; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				double a = tick * 1.1;
				for (int i = 0; i < 12; i++) {
					double b = a + Math.PI * 2 * i / 12;
					dot(level, dust(i == 0 ? 0xFFFFFF : TIME, i == 0 ? 1.2F : 0.8F), c.add(Math.cos(b) * 0.8, 0, Math.sin(b) * 0.8));
				}
			});
		}
		Vfx.emit(level, ParticleTypes.END_ROD, c, 10, 0.5, 0.05);
		Fx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 0.7F, 2.0F);
	}

	// ------------------------------------------------------------------ movement

	static void swap(ServerLevel level, Vec3 a, Vec3 b) {
		Vfx.Theme arcane = Vfx.theme("arcane");
		for (Vec3 p : List.of(a, b)) {
			dot(level, arcane.flash(), p.add(0, 1, 0));
			Vfx.ring(level, arcane.dust(1.2F), p.add(0, 0.1, 0), 0.7, 14);
			Vfx.emit(level, arcane.sparkle(), p.add(0, 1, 0), 8, 0.3, 0.0);
		}
		Fx.sound(level, a, SoundEvents.NOTE_BLOCK_SNARE, 1.0F, 1.2F);
		Fx.sound(level, b, SoundEvents.ENDERMAN_TELEPORT, 0.4F, 1.6F);
	}

	/** Zipper: two rows of teeth opening on the wall, and closing behind you. */
	static void zipper(ServerLevel level, Vec3 entry, Vec3 exit, Vec3 dir) {
		Vec3 side = flat(dir).cross(UP).normalize();
		for (Vec3 at : List.of(entry, exit)) {
			for (int i = -6; i <= 6; i++) {
				Vec3 p = at.add(0, i * 0.14, 0);
				double open = 0.08 + (6 - Math.abs(i)) * 0.02;
				dot(level, dust(i % 2 == 0 ? 0xD8B040 : 0x404048, 0.7F), p.add(side.scale(open)));
				dot(level, dust(i % 2 == 0 ? 0x404048 : 0xD8B040, 0.7F), p.add(side.scale(-open)));
			}
			dot(level, dust(0xFFE070, 1.2F), at.add(0, 0.9, 0));
		}
		Fx.sound(level, entry, SoundEvents.CHAIN_BREAK, 0.8F, 1.7F);
		Fx.sound(level, exit, SoundEvents.CHAIN_PLACE, 0.8F, 1.9F);
	}

	static void shadowstep(ServerLevel level, Vec3 from, Vec3 to) {
		for (Vec3 p : List.of(from, to)) {
			Vfx.radial(level, ParticleTypes.SQUID_INK, p.add(0, 1, 0), 14, 0.15);
			Vfx.emit(level, ParticleTypes.LARGE_SMOKE, p.add(0, 1, 0), 6, 0.3, 0.02);
		}
		silhouette(level, dust(BLACK, 1.0F), from, to.subtract(from), 1.0);
		Fx.sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.5F);
	}

	/** Stasis: a clock face stops around the target. */
	static void stasisStart(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		dot(level, flash(0xFFFFFF), c);
		clock(level, c, Math.max(0.7, target.getBbWidth() * 0.9), 0.0);
	}

	private static void clock(ServerLevel level, Vec3 c, double r, double hand) {
		Vfx.ring(level, dust(TIME, 0.9F), c, r, 24);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			dot(level, dust(0xFFFFFF, i % 3 == 0 ? 1.2F : 0.7F), c.add(Math.cos(a) * r * 1.15, 0, Math.sin(a) * r * 1.15));
		}
		for (double s = 0; s <= 1.0; s += 0.2) {
			dot(level, dust(0xFFF4D0, 0.8F), c.add(Math.cos(hand) * r * 0.8 * s, 0, Math.sin(hand) * r * 0.8 * s));
			dot(level, dust(0xFFF4D0, 0.8F), c.add(Math.cos(hand + 2.1) * r * 0.55 * s, 0, Math.sin(hand + 2.1) * r * 0.55 * s));
		}
	}

	static void stasisTick(ServerLevel level, Entity target, int ticksLeft) {
		Vec3 c = target.getBoundingBox().getCenter();
		clock(level, c, Math.max(0.7, target.getBbWidth() * 0.9), 0.0);
		Vfx.emit(level, dust(0xB8B8C4, 0.8F), c, 4, target.getBbWidth() * 0.6, 0.0);
	}

	/**
	 * A hit held in Stasis: a frozen crack of light where it struck, hanging in the air, and a
	 * pale ring that tightens with every hit waiting to land.
	 */
	static void stasisStore(ServerLevel level, Entity target, int hits) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 at = c.add((level.getRandom().nextDouble() - 0.5) * target.getBbWidth(), (level.getRandom().nextDouble() - 0.5) * target.getBbHeight() * 0.8,
			(level.getRandom().nextDouble() - 0.5) * target.getBbWidth());
		for (int i = 0; i < 4; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 dir = new Vec3(Math.cos(a), (level.getRandom().nextDouble() - 0.5) * 1.4, Math.sin(a)).normalize().scale(0.35);
			line(level, dust(0xFFFFFF, 0.55F), at, at.add(dir), 0.08);
		}
		dot(level, dust(TIME, 1.2F), at);
		Vfx.ring(level, dust(0xE8E4FF, 0.7F), c, Math.max(0.5, 1.4 - hits * 0.08), 16);
		Fx.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.7F, 1.4F + Math.min(0.6F, hits * 0.05F));
	}

	/** Time moves again: everything held lands at once. */
	static void timeResumes(ServerLevel level, Entity target, float stored) {
		Vec3 c = target.getBoundingBox().getCenter();
		dot(level, flash(0xFFFFFF), c);
		Vfx.radial(level, ParticleTypes.CRIT, c, (int) Math.min(40, 8 + stored), 0.5);
		Vfx.shockwave(level, target.position(), 1.6, Vfx.theme("time"), 4);
		Fx.sound(level, c, SoundEvents.BELL_BLOCK, 0.8F, 1.4F);
		if (stored > 0) {
			Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
		}
	}

	/** Rewind: golden streaks run backward to where you were, and the clock turns back. */
	static void rewind(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 a = from.add(0, 1, 0);
		Fx.send(level, new net.minecraft.core.particles.TrailParticleOption(to.add(0, 1, 0), TIME, 12), a.x, a.y, a.z, 14, 0.3, 0.5, 0.3, 0);
		silhouette(level, dust(TIME, 0.8F), from, to.subtract(from), 1.0);
		for (int t = 0; t < 8; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> clock(level, to.add(0, 1, 0), 0.8, -tick * 0.8));
		}
		Fx.sound(level, to, SoundEvents.TRIDENT_RETURN, 0.9F, 0.8F);
		Fx.sound(level, to, SoundEvents.BELL_RESONATE, 0.5F, 0.6F);
	}

	/** Time Skip: a crimson afterimage stays behind for a moment; you're already elsewhere. */
	static void timeSkip(ServerLevel level, Vec3 from, Vec3 to) {
		for (int t = 0; t < 6; t += 2) {
			Scheduler.later(t + 1, () -> silhouette(level, dust(0xB02030, 0.9F), from, to.subtract(from), 1.0));
		}
		dot(level, flash(0xB02030), to.add(0, 1, 0));
		Vfx.emit(level, dust(0xB02030, 1.2F), to.add(0, 1, 0), 10, 0.4, 0.0);
		Fx.sound(level, from, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.9F, 0.5F);
		Fx.sound(level, to, SoundEvents.BELL_BLOCK, 0.4F, 0.5F);
	}

	// ------------------------------------------------------------------ summons and links

	static void shadeRise(ServerLevel level, Vec3 at) {
		Vfx.radial(level, ParticleTypes.SQUID_INK, at.add(0, 0.4, 0), 16, 0.15);
		Vfx.emit(level, ParticleTypes.LARGE_SMOKE, at.add(0, 0.5, 0), 8, 0.3, 0.02);
		Vfx.ring(level, dust(BLACK, 1.4F), at.add(0, 0.08, 0), 0.9, 14);
	}

	static void shadeAura(ServerLevel level, Entity wolf) {
		Vfx.emit(level, ParticleTypes.SQUID_INK, wolf.getBoundingBox().getCenter(), 1, 0.25, 0.01);
		Vfx.emit(level, dust(0x2A1040, 1.0F), wolf.getBoundingBox().getCenter(), 2, 0.3, 0.0);
	}

	/** The Thunderbird: a bright body, flapping wings and a tail, heading along its circle. */
	static void thunderbird(ServerLevel level, Vec3 bird, double angle, int tick) {
		Vec3 heading = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
		Vec3 wing = new Vec3(Math.cos(angle), 0, Math.sin(angle));
		double flap = Math.sin(tick * 0.9) * 0.3;
		dot(level, dust(0xFFE650, 1.9F), bird);
		dot(level, dust(0xFFE650, 1.4F), bird.add(heading.scale(0.22)));
		dot(level, dust(0xFFFFFF, 1.0F), bird.add(heading.scale(0.42)).add(0, 0.06, 0));
		for (int side : new int[] {1, -1}) {
			for (int k = 1; k <= 4; k++) {
				Vec3 feather = bird.add(wing.scale(side * k * 0.26)).add(0, flap * k / 4.0, 0).subtract(heading.scale(k * 0.05));
				dot(level, dust(k == 4 ? 0xFFFFFF : 0xFFE650, k == 4 ? 0.9F : 1.2F), feather);
			}
		}
		dot(level, dust(0xE0C030, 1.0F), bird.subtract(heading.scale(0.35)));
		dot(level, dust(0xFFFFFF, 0.8F), bird.subtract(heading.scale(0.55)).add(0, 0.05, 0));
		if (tick % 6 == 0) {
			Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, bird, 2, 0.2, 0.05);
		}
		if (tick % 20 == 0) {
			Fx.sound(level, bird, SoundEvents.PHANTOM_FLAP, 0.4F, 1.6F);
		}
	}

	static void birdStrike(ServerLevel level, Vec3 from, Vec3 to) {
		Vfx.shockArc(level, from, to);
		dot(level, flash(0xFFF4C0), to);
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, to, 14, 0.4);
		Fx.sound(level, to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.6F);
	}

	static void birdFade(ServerLevel level, Vec3 at) {
		Vfx.emit(level, ParticleTypes.POOF, at, 6, 0.2, 0.02);
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, at, 10, 0.3);
	}

	/** If Airborne fired: a gust ring under your feet. */
	static void airborne(ServerLevel level, LivingEntity caster) {
		Vfx.ring(level, Vfx.theme("wind").dust(1.0F), caster.position().add(0, -0.1, 0), 0.8, 12);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, caster.position(), 3, 0.3, 0.02);
	}

	/** Combo fired: a golden burst around you. */
	/** Twin Star, armed: two small stars circle the caster for a moment. */
	static void twinStarMark(ServerLevel level, LivingEntity caster) {
		for (int t = 0; t < 12; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				Vec3 c = caster.position().add(0, 1.1, 0);
				for (int s = 0; s < 2; s++) {
					double a = tick * 0.55 + s * Math.PI;
					dot(level, dust(s == 0 ? 0xFFE8FF : 0xE678DC, 1.2F), c.add(Math.cos(a) * 0.8, Math.sin(tick * 0.3) * 0.2, Math.sin(a) * 0.8));
				}
			});
		}
		Fx.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.8F);
	}

	/** Twin Star's second cast: a flash of starlight at the hands. */
	static void twinStar(ServerLevel level, LivingEntity caster) {
		Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.8)).add(0, -0.3, 0);
		dot(level, flash(0xE678DC), hand);
		Vfx.radial(level, ParticleTypes.END_ROD, hand, 10, 0.12);
		Fx.sound(level, hand, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.9F);
	}

	/** On the beat: notes rise and a gold ring pulses out, one note per step of the chain. */
	static void rhythm(ServerLevel level, LivingEntity caster, int stacks) {
		Vec3 c = caster.position().add(0, 2.1, 0);
		for (int i = 0; i < stacks; i++) {
			double a = Math.PI * 2 * i / stacks;
			Fx.send(level, ParticleTypes.NOTE, c.x + Math.cos(a) * 0.5, c.y, c.z + Math.sin(a) * 0.5, 0, (0.2 + 0.25 * i) / 1.0, 0, 0, 1.0);
		}
		Vfx.ring(level, dust(0xF5D56A, 0.9F), caster.position().add(0, 0.1, 0), 0.9 + 0.3 * stacks, 18 + 4 * stacks);
	}

	static void combo(ServerLevel level, LivingEntity caster) {
		Vec3 c = caster.position().add(0, 1, 0);
		Vfx.radial(level, ParticleTypes.WAX_ON, c, 16, 0.3);
		Vfx.ring(level, dust(0xF0C440, 1.3F), caster.position().add(0, 0.1, 0), 1.1, 18);
		Fx.sound(level, c, SoundEvents.PLAYER_LEVELUP, 0.35F, 1.8F);
	}
}
