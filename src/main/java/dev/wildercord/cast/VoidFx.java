package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * What the void runes draw beyond {@link ElementFx}'s implosion and black core, so that each one has its own shape: a taut
 * tether (Pull), a wide spiralling disc (Gravity Well), an upright slit (Riftcall), a cut-out sphere (Hollow), a smear
 * (Shadowstep), a dotted rope (Grapple), two spirals that trade colours (Warp), a return sigil (Warp Step), a filled dark disc
 * with a corona (Eclipse), veins (Wither) and the clank of an Anchor refusing a push. Sounds are the kit's
 * ({@code tools/feel/void.py}).
 */
public final class VoidFx {
	private VoidFx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	private static final int VIOLET = ElementFx.VOID.primary();
	private static final int PALE = ElementFx.VOID.secondary();
	private static final int DARK = ElementFx.dark(ElementFx.VOID.accent());

	private static Vec3 unit(Vec3 v, Vec3 fallback) {
		return v.lengthSqr() < 1.0E-6 ? fallback : v.normalize();
	}

	private static Vec3 flat(Vec3 v) {
		return unit(new Vec3(v.x, 0, v.z), new Vec3(0, 0, 1));
	}

	// ------------------------------------------------------------------ Pull: the hook

	/**
	 * Pull: a taut line from where the hook was set to the target's chest, a small dark ring where it bites and a run of dust
	 * along it that slides toward the caster. It ends with a thud of dust where the creature lands.
	 */
	public static void hook(ServerLevel level, Entity target, Vec3 towards) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 d = towards.subtract(c);
		double length = d.length();
		Vec3 n = unit(d, UP);
		ElementFx.ring(level, c, n, DARK, Math.max(0.8, target.getBbWidth() + 0.4), 0.2, 0.08, 6);
		if (length > 1.5) {
			Vec3 end = towards.subtract(n.scale(Math.min(1.0, length * 0.2)));
			ElementFx.ray(level, c, end, VIOLET, 0.045, 8);
			ElementFx.ray(level, c, end, PALE, 0.015, 8);
			int beads = (int) Math.min(8, length);
			for (int i = 0; i < beads; i++) {
				double u = (double) i / beads;
				Vec3 p = c.add(d.scale(u));
				Scheduler.later(i / 2, () -> Vfx.emit(level, new DustParticleOptions(PALE, 0.6F), p, 1, 0.0, 0.0));
			}
		}
		Feels.sound(level, target.position(), "void_hook", 0.8F, 1.0F);
	}

	/** Where a pulled creature lands: a thud of dust. */
	public static void hookLanded(ServerLevel level, Vec3 feet) {
		ElementFx.groundRing(level, feet, DARK, 0.2, 1.1, 0.1, 8);
		Vfx.emit(level, ParticleTypes.POOF, feet.add(0, 0.1, 0), 4, 0.25, 0.02);
	}

	// ------------------------------------------------------------------ Gravity Well: the disc

	/**
	 * One beat of the well's disc (every ten ticks): three spiral arms of dust drawn in along the ground toward the middle,
	 * lifting a little as they go, under a wide ring. The well's 7-block reach finally reads as a place, not a point.
	 */
	public static void disc(ServerLevel level, Vec3 centre, double radius, int tick) {
		Vec3 core = centre.add(0, 0.25, 0);
		double spin = tick * 0.12;
		for (int arm = 0; arm < 3; arm++) {
			double base = spin + Math.PI * 2 * arm / 3;
			for (int k = 0; k < 4; k++) {
				double u = 1.0 - k * 0.22;
				double a = base + (1 - u) * 2.2;
				Vec3 from = centre.add(Math.cos(a) * radius * u, 0.12 + (1 - u) * 0.3, Math.sin(a) * radius * u);
				Fx.send(level, new TrailParticleOption(core, DARK, 14), from.x, from.y, from.z, 1, 0, 0, 0, 0);
			}
		}
		ElementFx.groundRing(level, centre, VIOLET, radius, radius * 0.3, 0.05, 14);
		Vfx.emit(level, ParticleTypes.PORTAL, core, 4, radius * 0.4, 0.4);
	}

	/** The crush: the disc snaps shut. */
	public static void discSnap(ServerLevel level, Vec3 centre, double radius) {
		ElementFx.groundRing(level, centre, DARK, radius, 0.2, 0.15, 6);
		ElementFx.groundRing(level, centre, PALE, radius * 0.7, 0.1, 0.05, 6);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, centre.add(0, 0.5, 0), 12, radius * 0.3, 0.2);
		Feels.sound(level, centre, "void_snap", 1.0F, 1.0F);
	}

	// ------------------------------------------------------------------ Riftcall: an upright slit

	/**
	 * A tear standing in the air: a jagged vertical slit of dark light with a lit rim, {@code gape} blocks wide at its middle
	 * (it opens as the rift feeds). It faces {@code facing}.
	 */
	public static void slit(ServerLevel level, Vec3 centre, Vec3 facing, double height, double gape, int lifetime) {
		Vec3 f = flat(facing);
		Vec3 side = f.cross(UP).normalize();
		int segments = 6;
		Vec3 previousLeft = null;
		Vec3 previousRight = null;
		var rnd = level.getRandom();
		for (int i = 0; i <= segments; i++) {
			double u = (double) i / segments;
			double y = (u - 0.5) * height;
			// Widest at the middle, a point at both ends, jagged in between.
			double w = gape * Math.sin(u * Math.PI) * (0.6 + 0.4 * rnd.nextDouble());
			Vec3 left = centre.add(0, y, 0).add(side.scale(-w));
			Vec3 right = centre.add(0, y, 0).add(side.scale(w));
			if (previousLeft != null) {
				ElementFx.ray(level, previousLeft, left, PALE, 0.03, lifetime);
				ElementFx.ray(level, previousRight, right, PALE, 0.03, lifetime);
			}
			previousLeft = left;
			previousRight = right;
		}
		ElementFx.ray(level, centre.add(0, -height * 0.5, 0), centre.add(0, height * 0.5, 0), DARK, Math.max(0.1, gape * 1.2), lifetime);
		Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, centre, 3, gape, 0.03);
	}

	// ------------------------------------------------------------------ Hollow: a cut-out

	/**
	 * A sphere cut out of the world: a dark orb with a thin pale rim on three tilted rings, and a ring of white shards where the
	 * colours invert. The erased creature stands inside it.
	 */
	public static void cutout(ServerLevel level, Vec3 c, double radius) {
		ElementFx.orb(level, c, DARK, radius, 6);
		double a = level.getRandom().nextDouble() * Math.PI;
		for (int i = 0; i < 3; i++) {
			ElementFx.ring(level, c, ElementFx.tilted(1.2, a + Math.PI * i / 3), i == 0 ? 0xFFFFFF : PALE, radius * 1.05, radius * 1.05, 0.02, 6);
		}
		Vfx.radial(level, ParticleTypes.END_ROD, c, 6, 0.05);
	}

	// ------------------------------------------------------------------ movement

	/**
	 * Shadowstep: a stretched smear from where you were to where you land, thickest at the start, and black dust where you were;
	 * no implosion (that is Blink's).
	 */
	public static void smear(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 a = from.add(0, 1, 0);
		Vec3 b = to.add(0, 1, 0);
		Vec3 d = b.subtract(a);
		double length = d.length();
		int steps = (int) Math.max(4, Math.min(12, length * 1.5));
		for (int i = 0; i < steps; i++) {
			double u = (double) i / steps;
			Vec3 p = a.add(d.scale(u));
			float size = (float) (1.6 - 1.1 * u);
			Vfx.emit(level, new DustParticleOptions(0x0C0810, size), p, 1, 0.12, 0.0);
			if (i % 2 == 0) {
				Vfx.emit(level, ParticleTypes.SQUID_INK, p, 1, 0.1, 0.01);
			}
		}
		ElementFx.ray(level, a, b.subtract(d.scale(length > 1.5 ? 0.8 / length : 0)), DARK, 0.3, 4);
		Vfx.radial(level, ParticleTypes.SQUID_INK, to.add(0, 1, 0), 6, 0.1);
		Feels.sound(level, to, "void_shadow_cut", 0.9F, 1.0F);
	}

	/**
	 * Grapple: a dotted rope from the hand to the anchor that tightens in three beats (the dots draw together and the sag
	 * goes) and is gone as you arrive.
	 */
	public static void rope(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		Vec3 near = length > 2.6 ? from.add(d.scale(1.3 / length)) : from;
		Vec3 span = to.subtract(near);
		int dots = (int) Math.max(4, Math.min(18, length * 1.4));
		for (int beat = 0; beat < 3; beat++) {
			double sag = (2 - beat) * 0.35;
			int b = beat;
			Scheduler.later(beat * 3, () -> {
				for (int i = 1; i < dots; i++) {
					double u = (double) i / dots;
					Vec3 p = near.add(span.scale(u)).add(0, -Math.sin(u * Math.PI) * sag, 0);
					Vfx.emit(level, new DustParticleOptions(b == 2 ? PALE : VIOLET, 0.55F), p, 1, 0.0, 0.0);
				}
			});
		}
		ElementFx.blackCore(level, to, 0.2, 10);
		ElementFx.ring(level, to, d, VIOLET, 0.1, 0.9, 0.04, 8);
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, to, 6, 0.12);
		Feels.sound(level, from, "void_grapple_reel", 0.8F, 1.0F);
	}

	/**
	 * Warp: two spirals turning round the line between the two places, one rising as the other falls, their colours trading
	 * halfway; a small flash at each end.
	 */
	public static void spirals(ServerLevel level, Vec3 a, Vec3 b) {
		Vec3 pa = a.add(0, 1, 0);
		Vec3 pb = b.add(0, 1, 0);
		Vec3 d = pb.subtract(pa);
		double length = d.length();
		Vec3 axis = unit(d, UP);
		Vec3 u = unit(axis.cross(UP), new Vec3(1, 0, 0));
		Vec3 v = axis.cross(u);
		int steps = (int) Math.max(6, Math.min(16, length * 1.5));
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			double ang = t * Math.PI * 2.5;
			double r = 0.45 * Math.sin(t * Math.PI) + 0.05;
			Vec3 c = pa.add(d.scale(t));
			for (int strand = 0; strand < 2; strand++) {
				double s = ang + strand * Math.PI;
				Vec3 p = c.add(u.scale(Math.cos(s) * r)).add(v.scale(Math.sin(s) * r));
				int color = (t < 0.5) == (strand == 0) ? VIOLET : PALE;
				Scheduler.later(i / 3, () -> Vfx.emit(level, new DustParticleOptions(color, 0.7F), p, 1, 0.0, 0.0));
			}
		}
		for (Vec3 end : new Vec3[] {pa, pb}) {
			Vfx.emit(level, SigilOption.glow(VIOLET, 1.6F), end, 1, 0.0, 0.0);
			ElementFx.ring(level, end, UP, PALE, 0.2, 1.0, 0.03, 6);
		}
		Feels.sound(level, a, "void_warp_ping", 0.8F, 1.0F);
	}

	/** Warp Step: a sigil left on the ground where you were, and a ring closing on it over the time you have. */
	public static void returnSigil(ServerLevel level, Vec3 origin, int ticks) {
		ElementFx.flatSigil(level, origin, SigilOption.TARGET, VIOLET, 0.9, ticks, -0.1);
		ElementFx.groundRing(level, origin, PALE, 1.6, 0.4, 0.04, ticks);
	}

	/** Warp Step's snap back: a ring collapsing on you, not a repeat of the leap. */
	public static void returnSnap(ServerLevel level, Vec3 at) {
		ElementFx.ring(level, at.add(0, 1, 0), UP, PALE, 1.4, 0.1, 0.05, 6);
		ElementFx.groundRing(level, at, DARK, 1.0, 0.1, 0.1, 6);
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, at.add(0, 1, 0), 8, 0.12);
	}

	/**
	 * The zipper's seam closing again behind you: the teeth come together from the ends toward the middle over six ticks.
	 */
	public static void zipShut(ServerLevel level, Vec3 at, Vec3 dir, int gold, int grey) {
		Vec3 side = flat(dir).cross(UP).normalize();
		for (int step = 0; step < 4; step++) {
			int s = step;
			Scheduler.later(step * 2, () -> {
				for (int i = -6; i <= 6; i++) {
					// The teeth nearest the ends shut first.
					if (Math.abs(i) < 6 - s * 2 - 1) {
						continue;
					}
					double open = Math.max(0.0, 0.14 - s * 0.04);
					Vec3 p = at.add(0, i * 0.14, 0);
					Vfx.emit(level, new DustParticleOptions(i % 2 == 0 ? gold : grey, 0.6F), p.add(side.scale(open)), 1, 0.0, 0.0);
					Vfx.emit(level, new DustParticleOptions(i % 2 == 0 ? grey : gold, 0.6F), p.add(side.scale(-open)), 1, 0.0, 0.0);
				}
			});
		}
	}

	// ------------------------------------------------------------------ Eclipse: a filled disc with a corona

	/**
	 * A dark disc hung {@code height} blocks up: filled with black dust (a few dozen grains, not a wire), a violet corona round
	 * its edge, and a patch of shadow on the ground beneath.
	 */
	public static void eclipseDisc(ServerLevel level, Vec3 ground, double radius, double height, int lifetime) {
		Vec3 top = ground.add(0, height, 0);
		var rnd = level.getRandom();
		for (int i = 0; i < 28; i++) {
			double a = rnd.nextDouble() * Math.PI * 2;
			double r = Math.sqrt(rnd.nextDouble()) * radius;
			Vfx.emit(level, new DustParticleOptions(0x0C0810, 1.6F), top.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0.0, 0.0);
		}
		ElementFx.ring(level, top, UP, DARK, radius, radius, 0.35, lifetime);
		ElementFx.ring(level, top, UP, PALE, radius * 1.02, radius * 1.02, 0.02, lifetime);
		ElementFx.ring(level, top, UP, VIOLET, radius * 1.18, radius * 1.18, 0.03, lifetime);
		ElementFx.groundRing(level, ground, DARK, radius, radius, 0.5, lifetime);
	}

	/** The light comes back: a bright ring races out where the disc was. */
	public static void eclipseLift(ServerLevel level, Vec3 ground, double radius) {
		ElementFx.groundRing(level, ground, PALE, 0.5, radius * 1.4, 0.06, 10);
		ElementFx.ring(level, ground.add(0, 2.5, 0), UP, 0xFFFFFF, radius * 0.6, radius * 1.4, 0.04, 10);
		Vfx.radial(level, ParticleTypes.END_ROD, ground.add(0, 1.5, 0), 8, 0.15);
		Feels.sound(level, ground, "void_eclipse_lift", 0.8F, 1.0F);
	}

	// ------------------------------------------------------------------ Wither, Blackflame, Anchor

	/** Wither: black veins climbing the target's body and drops of rot falling from it. */
	public static void veins(ServerLevel level, Entity t) {
		Vec3 feet = t.position();
		double w = Math.max(0.3, t.getBbWidth() * 0.5);
		double h = t.getBbHeight();
		var rnd = level.getRandom();
		for (int i = 0; i < 4; i++) {
			double a = rnd.nextDouble() * Math.PI * 2;
			Vec3 base = feet.add(Math.cos(a) * w, 0.1, Math.sin(a) * w);
			Vec3 mid = base.add((rnd.nextDouble() - 0.5) * 0.25, h * 0.4, (rnd.nextDouble() - 0.5) * 0.25);
			Vec3 tip = mid.add((rnd.nextDouble() - 0.5) * 0.25, h * 0.4, (rnd.nextDouble() - 0.5) * 0.25);
			ElementFx.ray(level, base, mid, DARK, 0.05, 24);
			ElementFx.ray(level, mid, tip, DARK, 0.035, 24);
		}
		Vfx.emit(level, ParticleTypes.SQUID_INK, t.getBoundingBox().getCenter(), 3, w, -0.02);
	}

	/** Blackflame: a white-hot heart in the black, ash falling inward. */
	public static void blackHeart(ServerLevel level, Vec3 c) {
		Vfx.emit(level, SigilOption.glow(0xFFFFFF, 0.7F), c, 1, 0.0, 0.0);
		Vfx.emit(level, ParticleTypes.SQUID_INK, c.add(0, 0.3, 0), 2, 0.2, -0.01);
	}

	/** An Anchor refusing a push: a clank of light and a puff of dust at the feet. */
	public static void clank(ServerLevel level, Entity t) {
		Vec3 feet = t.position();
		ElementFx.groundRing(level, feet, PALE, 0.3, 1.4, 0.08, 6);
		ElementFx.ring(level, t.getBoundingBox().getCenter(), UP, VIOLET, 0.7, 0.9, 0.05, 5);
		Vfx.emit(level, ParticleTypes.POOF, feet.add(0, 0.1, 0), 4, 0.25, 0.02);
		if (TimeFx.allow(level, "anchor_clank" + t.getId(), 1)) {
			Feels.sound(level, feet, "void_anchor_clank", 0.7F, 1.0F);
		}
	}

	/** Umbra in the dark: three claws of darkness raked across the target; in the light, one pale flicker. */
	public static void claws(ServerLevel level, Entity t, boolean dim) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vec3 look = flat(t.getLookAngle());
		Vec3 side = look.cross(UP).normalize();
		if (dim) {
			for (int i = -1; i <= 1; i++) {
				Vec3 a = c.add(side.scale(i * 0.22)).add(look.scale(0.4)).add(0, 0.45, 0);
				Vec3 b = c.add(side.scale(i * 0.22 - 0.15)).add(look.scale(0.4)).add(0, -0.45, 0);
				ElementFx.ray(level, a, b, DARK, 0.09, 7);
				ElementFx.ray(level, a, b, PALE, 0.02, 6);
			}
		} else {
			Vfx.emit(level, SigilOption.glow(PALE, 0.8F), c, 1, 0.0, 0.0);
		}
		Feels.sound(level, c, "void_shadow_bite", dim ? 0.9F : 0.6F, dim ? 0.9F : 1.3F);
	}
}
