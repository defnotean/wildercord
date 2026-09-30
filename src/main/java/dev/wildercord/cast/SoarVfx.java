package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.LightOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Soar's look ({@link Soar}). From the server: the wind gathering round whoever it lifts, the wingbeat of a
 * take-off, wisps peeling away as it fades, feathers as it sets them down, the wind torn away by a grounding
 * hit, and a ward stilling it. The wings themselves, and the wake behind a flier on the move, are drawn by
 * every client for every flier ({@code client.fx.SoarWings}), so they keep up with the body they're on; their
 * shape is {@link #wing}, shared with the take-off's wingbeat here.
 */
public final class SoarVfx {
	private SoarVfx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	/** The pale sky blue of the wind runes that lift (Levitate, Updraft, Feather Fall), and the ward's violet. */
	private static final int SKY = 0xBFE3FF;
	private static final int WARD = 0xB48CFF;
	/** A wing's strokes, top to bottom: a near-white leading edge, then sky, then the mint of wind itself. */
	private static final int[] STROKES = {0xE8F8FF, SKY, 0x7FE0C0};

	/** One stroke of a wing: the centre of the circle it's an arc of, and the light to draw there. */
	public record Stroke(Vec3 centre, LightOption light) {}

	/**
	 * A wing of wind growing from {@code root} (between the shoulder blades) out to one side ({@code side} -1
	 * left, 1 right) of a body facing {@code forward}, in the plane of its back: up to three strokes fanned
	 * out from the shoulder, each bowed upward like a feather; the top one long and rising (the leading edge),
	 * the others shorter and lower, the last hanging a little. Folded, two short strokes hang down along the
	 * back. {@code beat} (radians) tips the whole wing up or down.
	 */
	public static List<Stroke> wing(Vec3 root, Vec3 forward, int side, double beat, boolean folded, int strokes, int sweep, int lifetime) {
		Vec3 out = new Vec3(-forward.z, 0, forward.x).scale(side);
		Vec3 shoulder = root.add(out.scale(0.12));
		List<Stroke> wing = new ArrayList<>();
		for (int k = 0; k < Math.min(strokes, folded ? 2 : 3); k++) {
			double angle = (folded ? -1.05 - 0.25 * k : 0.7 - 0.46 * k) + beat * (1 + 0.3 * k);
			double length = folded ? 0.62 - 0.12 * k : 1.3 - 0.2 * k;
			Vec3 tip = shoulder.add(out.scale(Math.cos(angle) * length)).add(0, Math.sin(angle) * length, 0);
			// The stroke is an arc from the shoulder to its tip, bowed upward: its circle's centre lies below the chord.
			Vec3 chord = tip.subtract(shoulder);
			double half = chord.length() / 2;
			Vec3 along = chord.normalize();
			double outward = along.dot(out);
			Vec3 bow = out.scale(-along.y).add(0, outward, 0).normalize();
			double sag = half * 1.8;
			double radius = Math.sqrt(half * half + sag * sag);
			Vec3 centre = shoulder.add(chord.scale(0.5)).subtract(bow.scale(sag));
			double span = 2 * Math.asin(half / radius);
			wing.add(new Stroke(centre, ElementFx.slashOption(forward, bow, STROKES[k], radius, span, 0.06 - 0.006 * k, sweep, lifetime)));
		}
		return wing;
	}

	/** The way a creature's body faces, level. */
	private static Vec3 facing(Entity e) {
		double yaw = Math.toRadians(e instanceof net.minecraft.world.entity.LivingEntity living ? living.yBodyRot : e.getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	/** Between the shoulder blades, a little behind the back: where the wings grow from. */
	public static Vec3 root(Entity e) {
		return e.position().add(0, e.getBbHeight() * 0.72, 0).subtract(facing(e).scale(0.28));
	}

	/** Both wings at once, as one beat: {@code beat} radians above their resting spread (below it for a downbeat). */
	private static void wingbeat(ServerLevel level, Entity e, double beat, int sweep, int lifetime) {
		for (int side = -1; side <= 1; side += 2) {
			for (Stroke stroke : wing(root(e), facing(e), side, beat, false, 3, sweep, lifetime)) {
				Fx.send(level, stroke.light(), stroke.centre().x, stroke.centre().y, stroke.centre().z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** The wind gathers: a spiral of crescents climbs round the target, a ring runs out under it and wings unfurl at its back. */
	static void lift(ServerLevel level, Entity target) {
		Vec3 feet = target.position();
		double r = Math.max(0.6, target.getBbWidth() * 0.9);
		ElementFx.swirl(level, feet.add(0, 0.1, 0), r, target.getBbHeight() + 0.3, 4, SKY, ElementFx.WIND.secondary());
		ElementFx.gustRing(level, feet, 1.6);
		Scheduler.later(4, () -> {
			if (target.isAlive() && target.level() == level) {
				wingbeat(level, target, 0.25, 5, 14);
			}
		});
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			Vfx.fling(level, ParticleTypes.END_ROD, feet.add(Math.cos(a) * r, 0.15, Math.sin(a) * r), UP, 0.08);
		}
		Feels.sound(level, feet, "wind_lift", 0.5F, 1.0F);
	}

	/** Take-off: a downbeat of both wings, a ring of air slammed out below and a puff of cloud under the feet. */
	static void takeOff(ServerLevel level, Entity flier) {
		Vec3 feet = flier.position();
		wingbeat(level, flier, -0.5, 3, 9);
		ElementFx.gustRing(level, feet.add(0, -0.4, 0), 2.2);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 6; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			Vfx.fling(level, ParticleTypes.CLOUD, feet.add(0, 0.1, 0), new Vec3(Math.cos(a) * 0.6, -0.5, Math.sin(a) * 0.6), 0.12);
		}
		Feels.sound(level, feet, "wind_soar_takeoff", 0.7F, 1.0F);
	}

	/** Fading: wisps of wind peel off the wings and drift down behind, and a few feathers fall away. */
	static void fading(ServerLevel level, Entity flier) {
		Vec3 root = root(flier);
		Vec3 forward = facing(flier);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 4; i++) {
			Vec3 at = root.add((r.nextDouble() - 0.5) * 1.6, (r.nextDouble() - 0.3) * 0.6, (r.nextDouble() - 0.5) * 0.4).subtract(forward.scale(0.2));
			Vec3 normal = ElementFx.randomDir(r);
			ElementFx.slash(level, at, normal, ElementFx.inPlane(normal, r.nextDouble() * Math.PI * 2), i % 2 == 0 ? SKY : ElementFx.WIND.primary(),
				0.3 + 0.15 * r.nextDouble(), 2.0, 0.045, 3, 12);
		}
		for (int i = 0; i < 4; i++) {
			Vfx.fling(level, new ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER), root.add((r.nextDouble() - 0.5) * 1.2, 0, (r.nextDouble() - 0.5) * 0.6),
				new Vec3(0, -0.3, 0), 0.04);
		}
		Feels.sound(level, root, "wind_soar_fade", 0.6F, 1.0F);
	}

	/** Set down: the wings fold away into a last slow ring round the waist, and feathers drift down with the flier. */
	static void setDown(ServerLevel level, Entity flier) {
		Vec3 waist = flier.position().add(0, flier.getBbHeight() * 0.5, 0);
		ElementFx.ring(level, waist, UP, SKY, 1.3, 0.4, 0.04, 16);
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * 2 * i / 5;
			Vfx.fling(level, new ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER), waist.add(Math.cos(a) * 0.7, 0.6, Math.sin(a) * 0.7),
				new Vec3(0, -0.2, 0), 0.04);
		}
		Feels.sound(level, waist, "wind_feather", 0.6F, 0.9F);
	}

	/** Coming down: now and then a feather drifts down beside the faller, so a slow fall reads as Soar's. */
	static void drifting(ServerLevel level, Entity faller) {
		RandomSource r = level.getRandom();
		Vec3 at = faller.position().add((r.nextDouble() - 0.5) * 1.2, faller.getBbHeight() * (0.4 + 0.5 * r.nextDouble()), (r.nextDouble() - 0.5) * 1.2);
		Vfx.fling(level, new ItemParticleOption(ParticleTypes.ITEM, Items.FEATHER), at, new Vec3(0, -0.2, 0), 0.03);
	}

	/** Grounded: the wind is torn away: a ring snaps in round the flier, a crescent slams down over it and air bursts out. */
	static void grounded(ServerLevel level, Entity flier) {
		Vec3 waist = flier.position().add(0, flier.getBbHeight() * 0.5, 0);
		ElementFx.ring(level, waist, UP, ElementFx.WIND.accent(), 1.6, 0.2, 0.05, 7);
		Vec3 forward = facing(flier);
		ElementFx.slash(level, waist.add(0, 0.4, 0), new Vec3(-forward.z, 0, forward.x), new Vec3(0, -1, 0), 0x9FB8B0, 1.0, 1.8, 0.09, 2, 7);
		Vfx.radial(level, ParticleTypes.CLOUD, waist, 6, 0.12);
		Feels.sound(level, waist, "wind_thump", 0.7F, 0.8F);
	}

	/** Stilled by a ward: a violet ring closes round the flier and the wind falls quiet with a chime of amethyst. */
	static void stilled(ServerLevel level, Entity flier) {
		Vec3 waist = flier.position().add(0, flier.getBbHeight() * 0.5, 0);
		ElementFx.ring(level, waist, UP, WARD, 1.4, 0.3, 0.04, 12);
		Vfx.emit(level, ParticleTypes.ENCHANT, waist, 10, 0.4, 0.3);
		Fx.sound(level, waist, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
	}
}
