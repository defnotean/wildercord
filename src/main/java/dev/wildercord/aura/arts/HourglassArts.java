package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.TimeFx;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hourglass Breath's arts: echoes, rewinds, and moments held still. An Hourglass swordsman fights a moment ahead and a moment
 * behind: a cut struck again by an afterimage, a leap that snaps back to where it began, a foe held still in time, the world
 * dragging round a rush, and at the last time stopped while the cuts gather. A hold on another player is held to
 * {@link ArtRules#PVP_HOLD_TICKS} (and not again within {@link ArtRules#PVP_HOLD_REST}) as every art's is; what was gathered on
 * them still lands when time moves. Its light is gold and pale sand, its voice clockwork.
 * <ul>
 * <li><b>Echo Cut</b> (I): a cut, and a golden afterimage left where you stood that strikes it again a moment later.</li>
 * <li><b>Rewind Leap</b> (II): a falling cut that drags its foes in time, then time snaps you back to where you leapt from.</li>
 * <li><b>Stopped Moment</b> (III): whoever struck held still in time a while, then flung back by the moment's snap as it starts
 * again; the foes near dragged.</li>
 * <li><b>Blur</b> (IV): a rush faster than any other, the foes in the way cut; for three seconds time drags round you: foes slowed,
 * projectiles slowed to a third, you quickened.</li>
 * <li><b>Thousand Moments</b> (V): time stopped round you: every foe near held still while your cuts gather on them, all landing at
 * once when time moves again, with a share of every blow you struck them meanwhile.</li>
 * </ul>
 */
public final class HourglassArts {
	private HourglassArts() {}

	public static final String METHOD = "hourglass";
	public static final String ECHO_CUT = "echo_cut";
	public static final String REWIND_LEAP = "rewind_leap";
	public static final String STOPPED_MOMENT = "stopped_moment";
	public static final String BLUR = "blur";
	public static final String THOUSAND_MOMENTS = "thousand_moments";

	/** The kinds of field Hourglass leaves (for the tests). */
	public static final String DRAG = "hourglass_drag";
	public static final String STOPPED = "hourglass_stopped";

	/** The time palette's pale sand and deep gold, and white. */
	private static final int SAND = 0xFFF8E0;
	private static final int DEEP = 0xC8962E;
	private static final int WHITE = 0xFFFFFF;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, ECHO_CUT, HourglassArts::echoCut),
			MethodArts.art(AuraApi.ArtSlot.SECOND, REWIND_LEAP, HourglassArts::rewindLeap),
			MethodArts.art(AuraApi.ArtSlot.THIRD, STOPPED_MOMENT, HourglassArts::stoppedMoment),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, BLUR, HourglassArts::blur),
			MethodArts.art(AuraApi.ArtSlot.FINAL, THOUSAND_MOMENTS, HourglassArts::thousandMoments));
	}

	// ------------------------------------------------------------------ the look they share

	/**
	 * The gold Hourglass's arts are drawn in: its aura's own colour taken halfway to deep gold. (Its aura burns nearly white at the
	 * high stages, and pale light alone washes out against a bright sky: the arts' shapes keep their gold.)
	 */
	static int gold(ServerPlayer player) {
		return ArtKit.mix(ArtKit.color(player), DEEP, 0.5);
	}

	/**
	 * A clock face of light at {@code centre} facing {@code normal}, standing still: a gold rim and a pale one inside it, the twelve
	 * hours ticked in deep gold, two hands stopped at {@code hand} radians, a bright heart. Drawn rimmed by day, so it reads against
	 * the sky. Holds {@code life} ticks.
	 */
	static void clockFace(ArtLight light, Vec3 centre, Vec3 normal, double radius, double hand, int color, int life) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? ArtKit.UP : normal.normalize();
		if (Math.abs(n.y) > 0.95) {
			AuraFx.groundScar(light.level(), centre, radius, life, 2);
			return;
		}
		Vec3 u = ElementFx.perp(n);
		Vec3 v = n.cross(u);
		light.ring(centre, n, color, radius, radius, 0.09, life);
		light.bare().ring(centre.add(n.scale(0.01)), n, SAND, radius * 0.86, radius * 0.86, 0.03, life);
		for (int h = 0; h < 12; h++) {
			double a = Math.PI * 2 * h / 12;
			Vec3 dir = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
			double in = h % 3 == 0 ? 0.72 : 0.8;
			light.ray(centre.add(dir.scale(radius * in)), centre.add(dir.scale(radius * 0.94)), h % 3 == 0 ? color : DEEP, h % 3 == 0 ? 0.07 : 0.045, life);
		}
		Vec3 longHand = u.scale(Math.cos(hand)).add(v.scale(Math.sin(hand)));
		Vec3 shortHand = u.scale(Math.cos(hand + 2.1)).add(v.scale(Math.sin(hand + 2.1)));
		light.ray(centre, centre.add(longHand.scale(radius * 0.78)), color, 0.07, life);
		light.ray(centre, centre.add(shortHand.scale(radius * 0.5)), DEEP, 0.09, life);
		light.bare().orb(centre, WHITE, Math.max(0.05, radius * 0.07), Math.min(4, life));
	}

	/** Time moving again where {@code foe} was held: a white flash, a gold ring snapping out, sparks of light and its toll. */
	static void resume(ServerPlayer player, LivingEntity foe, int color) {
		ServerLevel level = player.level();
		Vec3 c = foe.getBoundingBox().getCenter();
		ArtLight world = ArtLight.world(player);
		world.flash(c, WHITE, 2.0F);
		world.ring(c, ArtKit.UP, color, 0.3, 2.2, 0.08, 8);
		world.groundRing(foe.position(), DEEP, 0.4, 2.0, 0.08, 9);
		Vfx.radial(level, ParticleTypes.END_ROD, c, 10, 0.3);
		ElementFx.goldenTicks(level, c, 0.4, 6);
		Feels.sound(level, c, "time_resume", 0.9F, 1.0F);
	}

	/**
	 * A cut of gold in front of {@code feet} facing {@code look}: low and flat over the ground for everyone (your own view keeps it
	 * under the middle), and an upright crescent across the front seen from outside.
	 */
	static void goldCut(ServerPlayer player, Vec3 feet, Vec3 look, int color, boolean mirror, double reach, float strength) {
		ArtLight world = ArtLight.world(player);
		Vec3 low = feet.add(0, 0.5, 0).add(look.scale(0.6));
		world.slash(low, ArtKit.UP, look, color, reach * 0.85, 2.3, 0.3 * strength, 1, 9);
		world.bare().slash(low.add(0, 0.02, 0), ArtKit.UP, look, SAND, reach * 0.82, 2.0, 0.08 * strength, 1, 8);
		ArtLight show = ArtLight.spectacle(player);
		Vec3 centre = feet.add(0, 0.9, 0).add(look.scale(1.2));
		Vec3 side = ArtKit.bladeSide(player, look).scale(mirror ? -0.35 : 0.35);
		Vec3 normal = ArtKit.UP.add(side).normalize();
		show.slash(centre, normal, look, color, 1.9, 2.4, 0.38 * strength, 1, 9);
		show.bare().slash(centre.add(0, 0.03, 0), normal, look, WHITE, 1.84, 2.1, 0.09 * strength, 1, 8);
	}

	/** A blow stored on a foe Thousand Moments holds (asked by {@link ArtWards}): a glint frozen where it struck. */
	static void storedLook(ServerPlayer player, LivingEntity foe) {
		ServerLevel level = player.level();
		TimeFx.stasisShard(level, foe, 30);
		Feels.sound(level, foe.position(), "time_tick", 0.4F, 1.5F);
	}

	// ------------------------------------------------------------------ I. Echo Cut

	static boolean echoCut(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		var continuation = dev.wildercord.aura.MastersArts.continuation(player);
		int color = gold(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_echo_cut", 1.0F, 1.0F);
		goldCut(player, feet, look, color, false, ArtRules.ECHO_REACH, 1.0F);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.ECHO_REACH, ArtRules.ECHO_DEGREES, ArtRules.ECHO_TARGETS)) {
			hits.strike(foe, ArtRules.ECHO_FACTOR);
			// Held a moment in time where it stood, so its echo finds it there.
			ArtKit.steady(foe);
			ElementFx.goldenTicks(level, foe.getBoundingBox().getCenter(), 0.3, 3);
		}
		// The afterimage, left where you stood, and a small face ticking at its feet.
		AuraStep.afterimages(player, feet, feet, look, color);
		AuraPhysicalFx.clock(level, feet.add(0, 0.06, 0), ArtKit.UP, 0.85, ArtRules.ECHO_DELAY, false);
		ElementFx.goldenTicks(level, feet.add(0, 1.0, 0), 0.4, 5);
		// A moment later it strikes again, from where you stood, the way you faced.
		Scheduler.later(ArtRules.ECHO_DELAY, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			// The released afterimage still strikes; a stopped physical performance does not swing the live body again.
			if (continuation.getAsBoolean()) hits.fx().trail(AuraFxRules.Stroke.CUT, true, 1.2F);
			goldCut(player, feet, look, ArtKit.mix(color, SAND, 0.35), true, ArtRules.ECHO_REACH, 0.8F);
			Feels.sound(level, feet.add(0, 1, 0), "time_reecho", 0.8F, 1.1F);
			for (LivingEntity foe : ArtKit.arcFrom(player, feet, look, ArtRules.ECHO_REPEAT_REACH, ArtRules.ECHO_DEGREES, ArtRules.ECHO_TARGETS)) {
				hits.strike(foe, ArtRules.ECHO_REPEAT, AuraFxRules.Weight.FULL);
				AuraPhysicalFx.timeImpact(level, foe.getBoundingBox().getCenter(), 0.7);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ II. Rewind Leap

	static boolean rewindLeap(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		Vec3 leapt = ArtWards.leapedFrom(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_rewind_leap", 1.0F, 1.0F);
		ArtLight world = ArtLight.world(player);
		Vec3 ahead = feet.add(look.scale(1.8));
		world.groundRing(ahead, color, 0.3, 2.2, 0.14, 10);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.REWIND_REACH, ArtRules.REWIND_DEGREES, ArtRules.REWIND_TARGETS)) {
			hits.strike(foe, ArtRules.REWIND_FACTOR);
			if (foe.isAlive()) {
				// Dragged in time: heavy-footed, a still face behind it, sand trickling off.
				ArtKit.slow(player, foe, ArtRules.REWIND_DRAG, 2);
				Vec3 c = foe.getBoundingBox().getCenter();
				Vec3 face = TimeFx.toward(c, feet);
				clockFace(world, c.subtract(face.scale(0.5)), face, Math.max(0.6, foe.getBbHeight() * 0.42), 1.2, color, ArtRules.REWIND_DRAG / 2);
				Vfx.emit(level, ParticleTypes.WAX_ON, c, 5, 0.3, 0.0);
			}
		}
		// Then time snaps you back to where you left the ground, if you may stand there still.
		if (leapt != null && leapt.distanceTo(feet) <= ArtRules.REWIND_MAX && leapt.distanceTo(feet) > 0.8) {
			boolean warded = DungeonWards.warded(level, BlockPos.containing(feet));
			Scheduler.later(ArtRules.REWIND_DELAY, () -> {
				if (!player.isAlive() || player.level() != level || !ArtKit.fits(player, leapt, warded)) {
					return;
				}
				Vec3 from = player.position();
				Vec3 dir = leapt.subtract(from);
				dir = new Vec3(dir.x, 0, dir.z);
				dir = dir.lengthSqr() < 1.0E-4 ? look.scale(-1) : dir.normalize();
				AuraStep.afterimages(player, from, leapt, dir, color);
				ArtKit.blink(player, leapt);
				TimeFx.rewindPath(level, from, leapt);
				ElementFx.goldenTicks(level, leapt.add(0, 1.0, 0), 0.5, 8);
				ArtLight w = ArtLight.world(player);
				// A thread of gold back along the way time ran, and a face on the ground where you come to stand, its hands turned back.
				w.ray(from.add(0, 0.1, 0), leapt.add(0, 0.1, 0), color, 0.16, 14);
				w.bare().ray(from.add(0, 0.12, 0), leapt.add(0, 0.12, 0), SAND, 0.05, 12);
				clockFace(w, leapt.add(0, 0.07, 0), ArtKit.UP, 1.0, -1.9, color, 16);
				w.groundRing(leapt, color, 1.6, 0.2, 0.08, 9);
				Feels.sound(level, leapt, "time_rewind", 0.9F, 1.1F);
			});
		}
		return true;
	}

	// ------------------------------------------------------------------ III. Stopped Moment

	static boolean stoppedMoment(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 feet = player.position();
		LivingEntity foe = ArtKit.attacker(player, context, 4.0);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_stopped_moment", 1.0F, 1.0F);
		AuraFx.burst(level, player, ArtKit.hand(player), ArtKit.flat(player), color, 1.3F, AuraFx.Burst.RING | AuraFx.Burst.FLASH);
		// The foes near dragged in time a moment.
		for (LivingEntity other : ArtKit.around(player, feet, ArtRules.STOPPED_DRAG_REACH, 1.0, 2.5, 6)) {
			if (other != foe) {
				ArtKit.slow(player, other, ArtRules.STOPPED_DRAG, 1);
				ElementFx.goldenTicks(level, other.getBoundingBox().getCenter(), 0.3, 3);
			}
		}
		if (foe == null) {
			return true;
		}
		hits.strike(foe, ArtRules.STOPPED_FACTOR);
		if (!foe.isAlive()) {
			return true;
		}
		// Held still in time: a still face behind it, a column of sand standing round it, motes of gold hanging in the air.
		boolean held = ArtKit.hold(player, foe, ArtRules.STOPPED_HOLD);
		int ticks = held ? (foe instanceof Player ? ArtRules.PVP_HOLD_TICKS : ArtKit.boss(foe) ? 0 : ArtRules.STOPPED_HOLD) : 0;
		if (ticks > 0) {
			Vec3 c = foe.getBoundingBox().getCenter();
			Vec3 face = TimeFx.toward(c, feet);
			// A great still face standing behind it, a smaller one on the ground under it, both stopped.
			ArtLight world = ArtLight.world(player);
			clockFace(world, c.subtract(face.scale(0.55)).add(0, 0.25, 0), face, Math.max(1.0, foe.getBbHeight() * 0.62), 2.2, color, ticks);
			clockFace(world, foe.position().add(0, 0.07, 0), ArtKit.UP, Math.max(0.7, foe.getBbWidth() + 0.3), 0.4, color, ticks);
			TimeFx.stasisColumn(level, foe, ticks);
			RandomSource r = level.getRandom();
			for (int i = 0; i < 10; i++) {
				Vec3 at = c.add((r.nextDouble() - 0.5) * 2.2, (r.nextDouble() - 0.5) * 1.8, (r.nextDouble() - 0.5) * 2.2);
				Motes.glow(level, at, i % 2 == 0 ? SAND : color, 0.06, ticks, Vec3.ZERO, 0.0);
			}
			if (foe instanceof ServerPlayer other) {
				ScreenFx.tint(other, color, ticks);
			}
		}
		// As time starts again, the moment snaps: flung back, and struck by it.
		Scheduler.later(Math.max(2, ticks), () -> {
			if (!foe.isAlive() || !player.isAlive() || foe.level() != level) {
				return;
			}
			hits.strike(foe, ArtRules.STOPPED_SNAP, AuraFxRules.Weight.FULL);
			ArtKit.knock(foe, player.position(), ArtRules.STOPPED_THROW, 0.25);
			resume(player, foe, color);
			ArtLight.world(player).ring(foe.getBoundingBox().getCenter(), TimeFx.toward(foe.position(), player.position()), color, 0.3, 1.8, 0.08, 8);
		});
		return true;
	}

	// ------------------------------------------------------------------ IV. Blur

	static boolean blur(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.BLUR_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, BLUR);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		// So fast it leaves more of itself behind than a step does.
		AuraStep.afterimages(player, from, from.lerp(to, 0.5), dir, color);
		AuraStep.afterimages(player, from.lerp(to, 0.5), to, dir, ArtKit.mix(color, SAND, 0.4));
		Feels.sound(level, from.add(0, 1, 0), "aura_art_blur", 1.05F, 1.0F);
		ArtLight world = ArtLight.world(player);
		Vec3 right = ArtKit.right(dir);
		ArtKit.dash(player, path, ArtRules.BLUR_TICKS, (a, b, step, last) -> {
			// Streaks of gold either side and through the middle: the world blurred past.
			for (int k = -1; k <= 1; k++) {
				Vec3 off = right.scale(0.45 * k).add(0, 0.5 + 0.35 * (k + 1), 0);
				world.ray(a.add(off), b.add(off), k == 0 ? color : SAND, k == 0 ? 0.2 : 0.06, 8 + k);
			}
			ElementFx.goldenTicks(level, b.add(0, 0.8, 0), 0.5, 4);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, ArtRules.BLUR_WIDTH / 2 + 0.3, 2.2,
					ArtRules.BLUR_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= ArtRules.BLUR_TARGETS) {
					continue;
				}
				hits.strike(foe, ArtRules.BLUR_FACTOR);
				ArtKit.slow(player, foe, 30, 1);
				AuraPhysicalFx.timeImpact(level, foe.getBoundingBox().getCenter(), 0.7);
			}
		});
		// Time dragging round you a while: foes slowed, projectiles slowed to a fraction as they cross into it, you quickened.
		Set<UUID> slowed = new HashSet<>();
		ArtFields.open(player, DRAG, ArtFields.disc(player::position, ArtRules.BLUR_RADIUS, 3.0), ArtRules.BLUR_FIELD + ArtRules.BLUR_TICKS, 1,
			(field, owner, age) -> drag(owner, field, age, slowed));
		return true;
	}

	/** One tick of Blur's drag round its swordsman. */
	private static void drag(ServerPlayer owner, ArtFields.Field field, long age, Set<UUID> slowed) {
		ServerLevel level = field.level();
		Vec3 at = owner.position();
		int color = gold(owner);
		for (Entity e : level.getEntities(owner, new AABB(at, at).inflate(ArtRules.BLUR_RADIUS, 3.0, ArtRules.BLUR_RADIUS),
				e -> e instanceof Projectile p && !slowed.contains(e.getUUID()) && !(p.getOwner() != null && ArtKit.helpable(owner, p.getOwner())))) {
			if (e.position().distanceToSqr(at.add(0, 1, 0)) > ArtRules.BLUR_RADIUS * ArtRules.BLUR_RADIUS) {
				continue;
			}
			slowed.add(e.getUUID());
			e.setDeltaMovement(e.getDeltaMovement().scale(ArtRules.BLUR_ARROW));
			e.needsSync = true;
			ArtLight.world(owner).ring(e.position(), e.getDeltaMovement().lengthSqr() < 1.0E-6 ? ArtKit.UP : e.getDeltaMovement(), color, 0.1, 0.6, 0.04, 6);
			Feels.sound(level, e.position(), "time_slow_hour", 0.5F, 1.4F);
		}
		if (age % 5 == 0) {
			for (LivingEntity foe : field.foes(owner)) {
				ArtKit.slow(owner, foe, 12, 1);
			}
		}
		if (age % 10 == 0) {
			owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 15, 0, false, false, true));
			ArtLight world = ArtLight.world(owner);
			world.groundRing(at, color, ArtRules.BLUR_RADIUS, ArtRules.BLUR_RADIUS * 0.85, 0.05, 12);
			RandomSource r = level.getRandom();
			for (int i = 0; i < 4; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				double d = 1.5 + r.nextDouble() * (ArtRules.BLUR_RADIUS - 1.5);
				Motes.glow(level, at.add(Math.cos(a) * d, 0.3 + r.nextDouble() * 2.0, Math.sin(a) * d), i % 2 == 0 ? SAND : color, 0.06, 26,
					new Vec3(0, 0.003, 0), 0.0);
			}
			// The bubble of slowed time, seen from outside: rings round you on three tilts.
			ArtLight show = ArtLight.spectacle(owner);
			for (int k = 0; k < 3; k++) {
				show.ring(at.add(0, 1.0, 0), ElementFx.tilted(k == 0 ? 0 : 1.2, age * 0.05 + k * 2.1), k == 1 ? SAND : color, ArtRules.BLUR_RADIUS * 0.95,
					ArtRules.BLUR_RADIUS * 0.95, 0.045, 11);
			}
		}
		if (age % 20 == 0) {
			Feels.sound(level, at, "time_tock", 0.35F, 0.7F);
		}
	}

	// ------------------------------------------------------------------ V. Thousand Moments

	static boolean thousandMoments(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = gold(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_thousand_moments", 1.3F, 1.0F);
		long now = level.getGameTime();
		// Time stops: a great face on the ground whose hands sweep once and stand still, motes of gold hanging in the air, everything
		// near held where it stands.
		AuraPhysicalFx.clock(level, feet.add(0, 0.06, 0), ArtKit.UP, ArtRules.THOUSAND_RADIUS, 8, false);
		ArtLight world = ArtLight.world(player);
		Scheduler.later(8, () -> clockFace(world, feet.add(0, 0.07, 0), ArtKit.UP, ArtRules.THOUSAND_RADIUS * 0.92, 1.1, color, ArtRules.THOUSAND_HOLD - 6));
		world.groundRing(feet, color, 0.5, ArtRules.THOUSAND_RADIUS * 1.1, 0.3, 12);
		world.groundRing(feet, SAND, 0.4, ArtRules.THOUSAND_RADIUS, 0.08, 10);
		world.ground(feet, SigilOption.BAND, DEEP, ArtRules.THOUSAND_RADIUS * 0.6, ArtRules.THOUSAND_HOLD, 0.0);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 24; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = 1.4 + r.nextDouble() * (ArtRules.THOUSAND_RADIUS - 1.4);
			Motes.glow(level, feet.add(Math.cos(a) * d, 0.3 + r.nextDouble() * 2.4, Math.sin(a) * d), i % 3 == 0 ? WHITE : i % 3 == 1 ? SAND : color, 0.06,
				ArtRules.THOUSAND_HOLD, Vec3.ZERO, 0.0);
		}
		// Seen from outside: an hourglass of light standing over its swordsman.
		ArtLight.spectacle(player).bare().ray(feet.add(0, 2.4, 0), feet.add(0, 3.6, 0), SAND, 0.05, ArtRules.THOUSAND_HOLD);
		List<LivingEntity> foes = ArtKit.around(player, feet, ArtRules.THOUSAND_RADIUS, 1.5, 3.5, ArtRules.THOUSAND_TARGETS);
		for (LivingEntity foe : foes) {
			boolean held = ArtKit.hold(player, foe, ArtRules.THOUSAND_HOLD);
			ArtWards.stop(player, foe, now + ArtRules.THOUSAND_HOLD);
			if (held) {
				TimeFx.stasisColumn(level, foe, foe instanceof Player ? ArtRules.PVP_HOLD_TICKS : ArtRules.THOUSAND_HOLD);
			}
			if (foe instanceof ServerPlayer other) {
				ScreenFx.tint(other, color, ArtRules.PVP_HOLD_TICKS);
			}
		}
		// The cuts gathering: a crescent of gold appearing round each one and hanging there, then another, and another.
		int gap = Math.max(1, ArtRules.THOUSAND_HOLD / ArtRules.THOUSAND_CUTS);
		List<List<Vec3[]>> cuts = new ArrayList<>();
		for (int i = 0; i < foes.size(); i++) {
			cuts.add(new ArrayList<>());
		}
		for (int k = 0; k < ArtRules.THOUSAND_CUTS; k++) {
			int cut = k;
			Scheduler.later(2 + k * gap, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				ArtLight w = ArtLight.world(player);
				RandomSource rr = level.getRandom();
				for (int i = 0; i < foes.size(); i++) {
					LivingEntity foe = foes.get(i);
					if (!foe.isAlive()) {
						continue;
					}
					Vec3 c = foe.getBoundingBox().getCenter().add((rr.nextDouble() - 0.5) * 0.3, (rr.nextDouble() - 0.5) * 0.5, (rr.nextDouble() - 0.5) * 0.3);
					Vec3 normal = ElementFx.randomDir(rr);
					Vec3 toward = ElementFx.inPlane(normal, rr.nextDouble() * Math.PI * 2);
					int life = ArtRules.THOUSAND_HOLD - 2 - cut * gap + 2;
					w.slash(c, normal, toward, cut % 2 == 0 ? color : DEEP, 0.75 + 0.1 * rr.nextDouble(), 2.0, 0.07, 1, Math.max(3, life));
					cuts.get(i).add(new Vec3[] {c, normal, toward});
				}
				if (cut % 2 == 0) {
					Feels.sound(level, feet, "time_tick", 0.45F, 1.2F + 0.04F * cut);
				}
			});
		}
		// Time moves again: every gathered cut lands at once.
		Scheduler.later(ArtRules.THOUSAND_HOLD, () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			ArtLight w = ArtLight.world(player);
			Feels.sound(level, feet.add(0, 1, 0), "aura_art_thousand_moments_release", 1.3F, 1.0F);
			AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.75F);
			ScreenFx.shake(level, feet, 0.35F, 14);
			w.groundRing(feet, WHITE, 0.4, ArtRules.THOUSAND_RADIUS * 1.2, 0.12, 10);
			hits.fx().trail(AuraFxRules.Stroke.SPIN, true, 1.7F);
			for (int i = 0; i < foes.size(); i++) {
				LivingEntity foe = foes.get(i);
				for (Vec3[] cut : cuts.get(i)) {
					w.bare().slash(cut[0], cut[1], cut[2], WHITE, 0.85, 2.3, 0.1, 1, 5);
				}
				double stored = ArtWards.release(player, foe);
				if (!foe.isAlive()) {
					continue;
				}
				hits.strike(foe, ArtRules.THOUSAND_FACTOR, AuraFxRules.Weight.GRAND);
				if (stored > 0) {
					hits.raw(foe, stored, AuraFxRules.Weight.FULL);
				}
				resume(player, foe, color);
			}
		});
		return true;
	}
}
