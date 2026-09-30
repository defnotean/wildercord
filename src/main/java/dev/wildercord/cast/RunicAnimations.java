package dev.wildercord.cast;

import dev.wildercord.spell.AnimationSignature;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A small, rune-specific animation layered over the existing shape and effect art. Every rune gets
 * its own illustrated emblem and stable three-beat stroke pattern; bespoke effects keep all of their
 * authored VFX. Both server and nearby clients see the same sequence, including secondary effects
 * and the two effects inside a woven rune.
 */
public final class RunicAnimations {
	private RunicAnimations() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A release for the shape and every effect in a group, including effects after the first. */
	public static void release(Cast cast, SpellPlan.Group group, Cast.Trigger trigger) {
		if (cast.passive || Fx.muted() || !cast.once("rune-release:" + System.identityHashCode(group))) {
			return;
		}
		List<RuneDef> runes = new ArrayList<>();
		runes.add(group.shape);
		for (SpellPlan.EffectNode node : group.effects) {
			runes.add(node.effect);
		}
		Vec3 dir = safe(trigger.dir());
		Vec3 side = side(dir);
		Vec3 high = side.cross(dir).normalize();
		Vec3 base = trigger.fromCaster(cast.caster)
			? cast.caster.getEyePosition().add(dir.scale(1.65)).add(0, -0.35, 0)
			: trigger.pos().add(0, 0.7, 0);
		int count = runes.size();
		for (int i = 0; i < count; i++) {
			RuneDef rune = runes.get(i);
			double spread = (i - (count - 1) * 0.5) * Math.min(0.46, 3.0 / count);
			Vec3 at = base.add(side.scale(spread)).add(high.scale((i % 2 == 0 ? 1 : -1) * 0.12));
			show(cast.level, rune, at, dir, 0.46, false, cast);
		}
	}

	/** The first place this effect lands in a cast. Later pulses retain their normal, lighter effect VFX. */
	public static void land(Cast cast, RuneDef rune, Cast.Hit hit) {
		if (cast.passive || Fx.muted() || !cast.once("rune-animation:" + rune.id())) {
			return;
		}
		show(cast.level, rune, hit.point().add(0, hit.self() ? 0.08 : 0.22, 0), safe(hit.dir()), 0.68, true, cast);
	}

	// Package access lets the client gallery capture this exact production animation for each rune.
	static void show(ServerLevel level, RuneDef rune, Vec3 at, Vec3 normal, double scale, boolean landing, Cast cast) {
		AnimationSignature signature = AnimationSignature.of(rune);
		int base = RuneColors.of(rune);
		int color = tint(base, signature.accent());
		int lifetime = 10 + signature.beat();
		Sigils.spell(level, at, normal, List.of(rune), color, (float) scale, lifetime);
		stroke(level, at, normal, color, signature, scale, 0, landing);
		Scheduler.later(signature.beat(), () -> {
			if (cast.alive()) {
				stroke(level, at, normal, color, signature, scale, 1, landing);
			}
		});
		Scheduler.later(signature.beat() * 2, () -> {
			if (cast.alive()) {
				stroke(level, at, normal, color, signature, scale, 2, landing);
			}
		});
	}

	private static void stroke(ServerLevel level, Vec3 at, Vec3 normal, int color, AnimationSignature sig,
			double scale, int phase, boolean landing) {
		Vec3 side = side(normal);
		Vec3 high = side.cross(normal).normalize();
		double cadence = 0.42 + ((sig.fingerprint() >>> 27) & 15) * 0.05;
		double a = Math.PI * 2 * sig.turn() / 16.0 + phase * (sig.fingerprint() < 0 ? -cadence : cadence);
		Vec3 spoke = side.scale(Math.cos(a)).add(high.scale(Math.sin(a)));
		double radius = scale * (0.49 + 0.16 * phase + (sig.arms() - 3) * 0.05
			+ ((sig.fingerprint() >>> 43) & 7) * 0.012);
		int bright = phase == 2 ? tint(color, 13) : color;
		int life = 6 + sig.beat();
		switch (sig.motif()) {
			case 0 -> { // a breathing halo with one named spoke
				Light.ring(level, at, normal, bright, radius * 0.4, radius, 0.028, life);
				Light.ray(level, at, at.add(spoke.scale(radius)), color, 0.022, life);
			}
			case 1 -> { // three turning crescent cuts
				Light.slash(level, at, normal, spoke, bright, radius, 1.0 + sig.arms() * 0.18, 0.034, 4, life);
			}
			case 2 -> { // a collapsing pair of rings
				Light.ring(level, at, normal, color, radius * 1.2, radius * 0.3, 0.03, life);
				Light.ring(level, at.add(normal.scale(0.04)), normal, bright, radius * 0.8, radius * 0.18, 0.02, life + 2);
			}
			case 3 -> { // an orbit that darts forward
				Vec3 orb = at.add(spoke.scale(radius * 0.7));
				Light.orb(level, orb, bright, radius * 0.26, life);
				Light.ray(level, orb, orb.add(normal.scale(0.35 + phase * 0.2)), color, 0.025, life);
			}
			case 4 -> { // forking prongs
				for (int i = -1; i <= 1; i++) {
					Vec3 fork = side.scale(Math.cos(a + i * 0.5)).add(high.scale(Math.sin(a + i * 0.5)));
					Light.ray(level, at.add(normal.scale(0.05)), at.add(fork.scale(radius)), i == 0 ? bright : color, 0.023, life);
				}
			}
			case 5 -> { // crossing blades
				Light.ray(level, at.add(spoke.scale(-radius)), at.add(spoke.scale(radius)), color, 0.027, life);
				Vec3 cross = normal.cross(spoke).normalize();
				Light.ray(level, at.add(cross.scale(-radius * 0.6)), at.add(cross.scale(radius * 0.6)), bright, 0.02, life);
			}
			case 6 -> { // a short fan of lances
				for (int i = 0; i < sig.arms(); i++) {
					double b = a + Math.PI * 2 * i / sig.arms();
					Vec3 ray = side.scale(Math.cos(b)).add(high.scale(Math.sin(b)));
					Light.ray(level, at.add(ray.scale(radius * 0.25)), at.add(ray.scale(radius)), i % 2 == 0 ? bright : color, 0.018, life);
				}
			}
			default -> { // a clock-like arc, opening then closing
				Light.slash(level, at, normal, spoke, color, radius, Math.PI * (phase == 2 ? 1.75 : 0.7), 0.025, 5, life);
				Light.orb(level, at, bright, radius * 0.16, life);
			}
		}
		if (landing && phase == 2) {
			Vfx.emit(level, new DustParticleOptions(bright, 0.72F), at, Math.min(7, sig.arms() + 1), radius * 0.35, 0.02);
		}
	}

	private static Vec3 safe(Vec3 direction) {
		return direction == null || direction.lengthSqr() < 1.0E-5 ? new Vec3(0, 0, 1) : direction.normalize();
	}

	private static Vec3 side(Vec3 normal) {
		Vec3 side = normal.cross(UP);
		return side.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : side.normalize();
	}

	private static int tint(int rgb, int accent) {
		double towardWhite = 0.06 + accent * 0.024;
		int r = (rgb >> 16) & 255;
		int g = (rgb >> 8) & 255;
		int b = rgb & 255;
		return ((int) Math.round(r + (255 - r) * towardWhite) << 16)
			| ((int) Math.round(g + (255 - g) * towardWhite) << 8)
			| (int) Math.round(b + (255 - b) * towardWhite);
	}
}
