package dev.wildercord.cast;

import dev.wildercord.spell.RuneChoreography;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
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

    /** Cosmetic-only paid Reweave inscription. No Cast is copied and its closed damage lifetime is never reopened. */
    static void reweave(net.minecraft.server.level.ServerPlayer caster, java.util.function.BooleanSupplier releasedOwner) {
        if (Fx.muted() || !releasedOwner.getAsBoolean()) return;
        ServerLevel level = caster.level();
        var sequence = RuneChoreography.of(Runes.REWEAVE);
        var beats = List.of(sequence.opening(), sequence.middle(), sequence.finish());
        for (int i = 0; i < beats.size(); i++) {
            int phase = i;
            Runnable stroke = () -> {
                if (Fx.muted() || !releasedOwner.getAsBoolean() || caster.level() != level) return;
                Vec3 dir = safe(caster.getLookAngle());
                if (!Double.isFinite(dir.x) || !Double.isFinite(dir.y) || !Double.isFinite(dir.z)) return;
                // Like ordinary rune release, the small glyph stays behind the first-person sightline.
                Vec3 at = caster.getEyePosition().subtract(dir.scale(1.8)).add(0, -.35, 0);
                draw(level, at, dir, RuneColors.of(Runes.REWEAVE), beats.get(phase), .46, phase, false, 3);
            };
            if (i == 0) stroke.run(); else Scheduler.later(i * 3, stroke);
        }
    }

	/** A release for the shape and every effect in a group, including effects after the first. */
	public static void release(Cast cast, SpellPlan.Group group, Cast.Trigger trigger) {
		if (cast.passive || Fx.muted() || !cast.once("rune-release:" + System.identityHashCode(group))) {
			return;
		}
		List<RuneDef> runes = new ArrayList<>();
		// Linked effects use an internal Target placeholder, not a castable shape with artwork.
		if (!group.shape.is(Runes.TRIGGER.id())) runes.add(group.shape);
		for (SpellPlan.EffectNode node : group.effects) {
			runes.add(node.effect);
		}
		Vec3 dir = safe(trigger.dir());
		RuneDef discipline=dev.wildercord.spell.CircleDisciplines.selected(group.shapeMods);
		if(discipline!=null && !trigger.fromCaster(cast.caster)) {
			List<RuneDef> circuit=new ArrayList<>();
			circuit.add(group.shape.is(Runes.TRIGGER.id())?Runes.SELF:group.shape);
			circuit.add(discipline);group.effects.forEach(node->circuit.add(node.effect));
			float yaw=(float)Math.toDegrees(Math.atan2(-dir.x,dir.z)),pitch=(float)-Math.toDegrees(Math.asin(Math.clamp(dir.y,-1,1)));
			Fx.particle(cast.level,new dev.wildercord.content.SpellCircleOption(circuit.stream().map(RuneDef::id).limit(16).toList(),
				cast.theme(group).primary(),.7F,yaw,pitch,18),trigger.pos().add(0,.4,0),1,0,0);
		}
		Vec3 side = side(dir);
		Vec3 high = side.cross(dir).normalize();
		// The cast's rune panels belong with the rear formation circle. In front of
		// the caster they sat directly over the first-person aim just as the spell fired.
		Vec3 base = trigger.fromCaster(cast.caster)
			? cast.caster.getEyePosition().subtract(dir.scale(1.7)).add(0, -0.35, 0)
			: trigger.pos().add(0, 0.7, 0);
		int count = runes.size();
		for (int i = 0; i < count; i++) {
			RuneDef rune = runes.get(i);
			double spread = (i - (count - 1) * 0.5) * Math.min(0.46, 3.0 / count);
			if(trigger.fromCaster(cast.caster)){
				// The client's tracking formation already draws every rune's emblem. A second,
				// fixed world circle stayed at the old rear position after a sudden turn.
				casterBeats(cast,rune,spread,i%2==0?1:-1);
				continue;
			}
			Vec3 at = base.add(side.scale(spread)).add(high.scale((i % 2 == 0 ? 1 : -1) * 0.12));
			show(cast.level, rune, at, dir, 0.46, false, cast);
		}
	}
	private static void casterBeats(Cast cast,RuneDef rune,double spread,int sign){
		var sequence=RuneChoreography.of(rune);
		var beats=List.of(sequence.opening(),sequence.middle(),sequence.finish());
		for(int i=0;i<3;i++){
			int phase=i;
			Runnable beat=()->{
				if(!cast.alive())return;
				Vec3 dir=safe(cast.caster.getLookAngle()),side=side(dir),high=side.cross(dir).normalize();
				Vec3 at=cast.caster.getEyePosition().subtract(dir.scale(1.8)).add(0,-.35,0).add(side.scale(spread)).add(high.scale(sign*.12));
				draw(cast.level,at,dir,RuneColors.of(rune),beats.get(phase),.46,phase,false,3);
			};
			if(i==0)beat.run();else Scheduler.later(i*3,beat);
		}
	}

	/** The first place this effect lands in a cast. Later pulses retain their normal, lighter effect VFX. */
	public static void land(Cast cast, RuneDef rune, Cast.Hit hit) {
		// Authored outcomes follow actual owner changes; a collision must not invent a successful landing.
		var signature = dev.wildercord.cast.feel.Signatures.get(rune.id());
		if (signature != null && signature.ownsOutcomeBody()) return;
		// A pair fusion choreographs its own landing in dev.wildercord.pairs.
		if (dev.wildercord.spell.PairRunes.isPair(rune)) return;
		if (cast.passive || Fx.muted() || !cast.once("rune-animation:" + rune.id())) {
			return;
		}
		show(cast.level, rune, hit.point().add(0, hit.self() ? 0.08 : 0.22, 0), safe(hit.dir()), 0.68, true, cast);
	}

	// Package access lets the client gallery capture this exact production animation for each rune.
	static void show(ServerLevel level, RuneDef rune, Vec3 at, Vec3 normal, double scale, boolean landing, Cast cast) {
		RuneChoreography.Sequence sequence = RuneChoreography.of(rune);
		int color = RuneColors.of(rune);
		Sigils.spell(level, at, normal, List.of(rune), color, (float) (scale * 0.42), 11);
		draw(level, at, normal, color, sequence.opening(), scale, 0, landing);
		Scheduler.later(3, () -> {
			if (cast.alive()) {
				draw(level, at, normal, color, sequence.middle(), scale, 1, landing);
			}
		});
		Scheduler.later(6, () -> {
			if (cast.alive()) {
				draw(level, at, normal, color, sequence.finish(), scale, 2, landing);
			}
		});
	}

	/** Each beat draws a specific object or motion; the sequence belongs to the named rune. */
	private static void draw(ServerLevel level, Vec3 at, Vec3 normal, int color,
			RuneChoreography.Gesture gesture, double scale, int phase, boolean landing) {
		draw(level,at,normal,color,gesture,scale,phase,landing,9);
	}
	private static void draw(ServerLevel level, Vec3 at, Vec3 normal, int color,
			RuneChoreography.Gesture gesture, double scale, int phase, boolean landing,int life) {
		int bright = phase == 2 ? tint(color, 11) : color;
		Canvas c = new Canvas(level, at.add(normal.scale(phase * 0.045)), normal, bright,
			scale * (1.08 + phase * 0.08), life);
		switch (gesture) {
			case SEAL -> { c.ring(0.75, 0.75); c.ring(0.33, 0.33); c.line(-0.24, 0, 0.24, 0); }
			case HALO -> { c.ring(0.43, 0.92); c.orb(0, 0.94, 0.13); }
			case ORBIT -> { c.ring(0.72, 0.72); c.orb(-0.66, 0.28, 0.16); c.orb(0.63, 0.30, 0.16); c.orb(0, -0.73, 0.16); }
			case LANCE -> { c.line(0, -0.86, 0, 0.86); c.line(-0.23, 0.5, 0, 0.86); c.line(0.23, 0.5, 0, 0.86); }
			case FAN -> { for (int i = -2; i <= 2; i++) c.line(0, -0.65, i * 0.28, 0.72); }
			case CRESCENT -> { c.arc(-0.26, 0, 0.92, 2.5); c.orb(0.52, 0, 0.09); }
			case CROSS -> { c.line(-0.72, -0.72, 0.72, 0.72); c.line(-0.72, 0.72, 0.72, -0.72); }
			case FORK -> { c.line(0, -0.75, 0, 0.04); c.line(0, 0.04, -0.53, 0.75); c.line(0, 0.04, 0.53, 0.75); }
			case BURST -> { for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; c.line(Math.cos(a) * 0.24, Math.sin(a) * 0.24, Math.cos(a) * 0.9, Math.sin(a) * 0.9); } }
			case CRACK -> { c.line(-0.36, 0.88, 0.12, 0.27); c.line(0.12, 0.27, -0.09, -0.11); c.line(-0.09, -0.11, 0.43, -0.85); c.line(0.1, 0.28, 0.51, 0.49); }
			case DROPLET -> { c.orb(0, -0.21, 0.39); c.line(-0.29, 0.02, 0, 0.85); c.line(0.29, 0.02, 0, 0.85); }
			case SHARD -> { c.line(-0.5, -0.7, 0, 0.85); c.line(0, 0.85, 0.53, -0.52); c.line(0.53, -0.52, -0.5, -0.7); c.line(0, 0.85, 0.13, -0.55); }
			case SPIRAL -> { c.arc(0.18, -0.15, 0.8, 3.9); c.arc(-0.12, 0.11, 0.49, 3.4); c.orb(0, 0, 0.11); }
			case VINE -> { c.arc(-0.4, -0.24, 0.72, 1.9); c.line(-0.25, -0.24, 0.11, 0.1); c.orb(0.47, 0.53, 0.14); }
			case PETAL -> { for (int i = 0; i < 4; i++) { double a = i * Math.PI / 2; c.orb(Math.cos(a) * 0.52, Math.sin(a) * 0.52, 0.28); } c.orb(0, 0, 0.18); }
			case ROOTS -> { c.line(0, 0.7, 0, -0.24); c.line(0, -0.24, -0.68, -0.75); c.line(0, -0.24, 0.63, -0.75); c.line(-0.29, -0.47, -0.4, -0.88); }
			case WING -> { c.arc(-0.38, 0.24, 0.65, 1.8); c.arc(0.38, 0.24, 0.65, 1.8); c.line(0, -0.62, 0, 0.4); }
			case FLAME -> { c.arc(0, -0.24, 0.76, 2.2); c.line(-0.43, -0.52, -0.12, 0.78); c.line(0.3, -0.55, 0.12, 0.39); c.orb(0, -0.28, 0.18); }
			case RAIN -> { for (int i = -2; i <= 2; i++) c.line(i * 0.32, 0.78 - (i & 1) * 0.25, i * 0.32 - 0.12, -0.73 + (i & 1) * 0.19); }
			case GEAR -> { c.ring(0.64, 0.64); for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; c.line(Math.cos(a) * 0.65, Math.sin(a) * 0.65, Math.cos(a) * 0.87, Math.sin(a) * 0.87); } }
			case CLOCK -> { c.ring(0.76, 0.76); c.line(0, 0, 0, 0.55); c.line(0, 0, 0.43, -0.2); }
			case GATE -> { c.line(-0.61, -0.8, -0.61, 0.58); c.line(0.61, -0.8, 0.61, 0.58); c.arc(0, 0.44, 0.63, 2.7); }
			case MIRROR -> { c.line(0, -0.87, 0, 0.87); c.line(-0.69, -0.43, -0.2, 0.33); c.line(0.69, -0.43, 0.2, 0.33); c.orb(0, 0, 0.11); }
			case CHAIN -> { c.orb(-0.46, 0.43, 0.3); c.orb(0.45, -0.4, 0.3); c.line(-0.26, 0.22, 0.25, -0.2); }
			case CROWN -> { c.line(-0.77, -0.58, -0.64, 0.49); c.line(-0.64, 0.49, -0.24, 0.02); c.line(-0.24, 0.02, 0, 0.82); c.line(0, 0.82, 0.24, 0.02); c.line(0.24, 0.02, 0.64, 0.49); c.line(0.64, 0.49, 0.77, -0.58); c.line(-0.77, -0.58, 0.77, -0.58); }
			case SHELL -> { c.arc(0, 0, 0.87, 3.2); c.arc(0, -0.08, 0.56, 3.0); c.line(-0.8, -0.3, 0.8, -0.3); }
			case STAR -> { for (int i = 0; i < 5; i++) { double a = Math.PI / 2 + i * Math.PI * 4 / 5; double b = Math.PI / 2 + (i + 1) * Math.PI * 4 / 5; c.line(Math.cos(a) * 0.85, Math.sin(a) * 0.85, Math.cos(b) * 0.85, Math.sin(b) * 0.85); } }
			case WAVE -> { c.arc(-0.4, -0.08, 0.55, 2.0); c.arc(0.25, 0.18, 0.62, 2.0); c.line(-0.8, -0.55, 0.8, -0.55); }
			case PILLAR -> { c.line(-0.36, -0.84, -0.36, 0.84); c.line(0.36, -0.84, 0.36, 0.84); c.line(-0.59, -0.84, 0.59, -0.84); c.line(-0.59, 0.84, 0.59, 0.84); }
			case SWARM -> { c.orb(-0.65, -0.12, 0.17); c.orb(-0.2, 0.59, 0.13); c.orb(0.27, -0.53, 0.2); c.orb(0.72, 0.35, 0.12); }
			case MIST -> { c.ring(0.28, 0.84); c.orb(-0.54, 0.32, 0.2); c.orb(0.53, -0.3, 0.16); }
			case FLARE -> { c.orb(0, 0, 0.25); for (int i = 0; i < 6; i++) { double a = i * Math.PI / 3; c.line(Math.cos(a) * 0.42, Math.sin(a) * 0.42, Math.cos(a) * 0.9, Math.sin(a) * 0.9); } }
			case NEEDLE -> { c.line(-0.09, -0.77, 0.09, 0.83); c.line(0.09, 0.83, 0.33, 0.4); c.orb(-0.09, -0.77, 0.08); }
			case DIAMOND -> { c.line(0, 0.86, 0.63, 0); c.line(0.63, 0, 0, -0.86); c.line(0, -0.86, -0.63, 0); c.line(-0.63, 0, 0, 0.86); }
			case TIDE -> { c.arc(-0.4, -0.28, 0.54, 2.3); c.arc(0.19, 0.06, 0.62, 2.3); c.arc(0.57, -0.43, 0.29, 1.9); }
			case CLAW -> { for (int i = -1; i <= 1; i++) c.arc(i * 0.34, i * 0.08, 0.6, 1.15); }
			case HEART -> { c.orb(-0.32, 0.3, 0.35); c.orb(0.32, 0.3, 0.35); c.line(-0.62, 0.12, 0, -0.81); c.line(0.62, 0.12, 0, -0.81); }
			case EYE -> { c.arc(0, 0.26, 0.8, 2.1); c.arc(0, -0.26, 0.8, 2.1); c.orb(0, 0, 0.24); }
			case STEP -> { c.orb(-0.42, -0.4, 0.19); c.orb(0.2, 0.28, 0.19); c.line(-0.3, -0.22, 0.07, 0.12); }
			case TETHER -> { c.orb(-0.73, 0.35, 0.14); c.orb(0.73, -0.35, 0.14); c.line(-0.6, 0.28, 0.6, -0.28); }
			case CLOUD -> { c.orb(-0.47, 0.03, 0.36); c.orb(0.04, 0.36, 0.42); c.orb(0.49, 0.02, 0.34); c.line(-0.72, -0.27, 0.73, -0.27); }
			case FOAM -> { c.orb(-0.59, -0.37, 0.15); c.orb(-0.21, 0.16, 0.27); c.orb(0.35, -0.13, 0.22); c.orb(0.67, 0.48, 0.11); }
			case LEDGER -> {
				// One narrow account, with the original four obligations ruled across it.
				c.line(-0.16, -0.88, -0.16, 0.88); c.line(0.16, -0.88, 0.16, 0.88);
				for (int i = 0; i < 4; i++) { double y = -0.66 + i * 0.44; c.line(-0.16, y, 0.16, y); }
			}
		}
		if (landing && phase == 2) {
			Vfx.emit(level, new DustParticleOptions(bright, 0.72F), at, 5, scale * 0.3, 0.02);
		}
	}

	private record Canvas(ServerLevel level, Vec3 centre, Vec3 normal, int color, double size, int life) {
		Vec3 x() { return side(normal); }
		Vec3 y() { return x().cross(normal).normalize(); }
		Vec3 point(double u, double v) { return centre.add(x().scale(u * size)).add(y().scale(v * size)); }
		void line(double u1, double v1, double u2, double v2) {
			Light.ray(level, point(u1, v1), point(u2, v2), color, 0.024, life);
		}
		void ring(double from, double to) {
			Light.ring(level, centre, normal, color, from * size, to * size, 0.028, life);
		}
		void orb(double u, double v, double radius) {
			Light.orb(level, point(u, v), color, radius * size, life);
		}
		void arc(double u, double v, double radius, double span) {
			Light.slash(level, point(u, v), normal, y(), color, radius * size, span, 0.028, 4, life);
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
