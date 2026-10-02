package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gale Breath's arts: reach, the air taken out from under a foe, and the wind at your back. Gale cuts from further than a sword
 * should, throws foes up where its blade (and a mage's spell) bites harder, turns arrows aside, and carries allies along.
 * <ul>
 * <li><b>Cutting Breeze</b> (I): a blade of wind loosed off the cut, flying on far past the sword's reach and pushing what it cuts.</li>
 * <li><b>Updraft</b> (II): the foes in front thrown high and you rising after them; while they're up your blade lands harder.</li>
 * <li><b>Eye of the Storm</b> (III): a spinning cut all round, and for a while projectiles turn aside and foes are blown off you.</li>
 * <li><b>Tailwind</b> (IV): a long dash, the foes in the way shoved aside, your allies near swept along faster.</li>
 * <li><b>Hundred Winds</b> (V): you become a whirlwind of cuts, drawing foes in, and at its end lifting them all.</li>
 * </ul>
 */
public final class GaleArts {
	private GaleArts() {}

	public static final String METHOD = "gale";
	public static final String CUTTING_BREEZE = "cutting_breeze";
	public static final String UPDRAFT = "updraft";
	public static final String EYE_OF_THE_STORM = "eye_of_the_storm";
	public static final String TAILWIND = "tailwind";
	public static final String HUNDRED_WINDS = "hundred_winds";

	public static final String EYE = "gale_eye";
	public static final String WINDS = "gale_winds";

	/** The wind palette's white and teal (cast.ElementFx.WIND). */
	private static final int WHITE = 0xFFFFFF;
	private static final int TEAL = 0x7FE0C0;

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, CUTTING_BREEZE, GaleArts::cuttingBreeze),
			MethodArts.art(AuraApi.ArtSlot.SECOND, UPDRAFT, GaleArts::updraft),
			MethodArts.art(AuraApi.ArtSlot.THIRD, EYE_OF_THE_STORM, GaleArts::eyeOfTheStorm),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, TAILWIND, GaleArts::tailwind),
			MethodArts.art(AuraApi.ArtSlot.FINAL, HUNDRED_WINDS, GaleArts::hundredWinds));
	}

	// ------------------------------------------------------------------ I. Cutting Breeze

	static boolean cuttingBreeze(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 look = player.getViewVector(1.0F);
		Vec3 aim = new Vec3(look.x, look.y * 0.4, look.z).normalize();
		Vec3 cross = aim.cross(ArtKit.UP);
		Vec3 side = cross.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : cross.normalize();
		Vec3 normal = ArtKit.UP.add(side.scale(0.3)).normalize();
		Vec3 origin = player.getEyePosition().subtract(0, 0.7, 0);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_cutting_breeze", 1.0F, 1.0F);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, origin.add(aim.scale(1.2)), 1, 0.0, 0.0);
		int steps = (int) Math.ceil(ArtRules.BREEZE_RANGE / ArtRules.BREEZE_SPEED);
		Set<UUID> cut = new HashSet<>();
		boolean[] done = {false};
		for (int i = 0; i < steps; i++) {
			int step = i;
			Runnable fly = () -> {
				if (done[0] || !player.isAlive() || player.level() != level) {
					return;
				}
				double d = 1.6 + ArtRules.BREEZE_SPEED * step;
				Vec3 front = origin.add(aim.scale(d));
				BlockPos pos = BlockPos.containing(front);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					done[0] = true;
					ElementFx.windImpact(level, front.subtract(aim.scale(0.4)), 1.0);
					return;
				}
				// The blade of wind: a pale crescent, white at its edge, a gust shed behind it.
				ArtLight world = ArtLight.world(player);
				Vec3 centre = front.subtract(aim.scale(0.75));
				world.slash(centre, normal, aim, color, 1.2, 2.4, 0.34, 1, 5);
				world.slash(centre.add(aim.scale(0.08)), normal, aim, WHITE, 1.16, 2.1, 0.1, 1, 4);
				if (step % 2 == 1) {
					Motes.glows(level, front, 2, 0.5, WHITE, 0.08, 10, aim.scale(-0.04), 0.01);
				}
				for (Entity e : level.getEntities(player, new AABB(front, front).inflate(ArtRules.BREEZE_WIDTH / 2 + 1, 1.8, ArtRules.BREEZE_WIDTH / 2 + 1),
						e -> ArtKit.harmable(player, e) && !cut.contains(e.getUUID()))) {
					if (cut.size() >= ArtRules.BREEZE_TARGETS) {
						break;
					}
					Vec3 rel = e.getBoundingBox().getCenter().subtract(front);
					if (Math.abs(rel.dot(aim)) > ArtRules.BREEZE_SPEED / 2 + 0.8 || Math.abs(rel.dot(side)) > ArtRules.BREEZE_WIDTH / 2 + e.getBbWidth() / 2
							|| Math.abs(rel.y) > 1.7) {
						continue;
					}
					LivingEntity foe = (LivingEntity) e;
					cut.add(foe.getUUID());
					hits.strike(foe, ArtRules.BREEZE_FACTOR, AuraFxRules.Weight.FULL);
					Statuses.windPush(foe, new Vec3(aim.x, 0, aim.z).normalize().scale(ArtRules.BREEZE_PUSH).add(0, 0.15, 0));
					Reactions.mark(foe, Reactions.Mark.WINDSWEPT, 50);
					ElementFx.windImpact(level, foe.getBoundingBox().getCenter(), 0.8);
				}
			};
			if (i == 0) {
				fly.run();
			} else {
				Scheduler.later(i, fly);
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Updraft

	static boolean updraft(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.RISING, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_updraft", 1.0F, 1.0F);
		ArtLight world = ArtLight.world(player);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), ArtRules.UPDRAFT_REACH, ArtRules.UPDRAFT_DEGREES, ArtRules.UPDRAFT_TARGETS)) {
			hits.strike(foe, ArtRules.UPDRAFT_FACTOR);
			ArtKit.lift(foe, ArtRules.UPDRAFT_LIFT, ArtRules.UPDRAFT_AIRBORNE);
			ArtWards.juggled(player, foe, ArtRules.UPDRAFT_AIRBORNE);
			// A whirl of wind lifting it, spiralling up its height.
			Vec3 base = foe.position();
			ElementFx.swirl(level, base, Math.max(0.7, foe.getBbWidth()) + 0.3, foe.getBbHeight() + 1.5, 4, color, WHITE);
			world.groundRing(base, WHITE, 0.2, 1.4, 0.05, 8);
			Vfx.emit(level, ParticleTypes.SMALL_GUST, base.add(0, 0.4, 0), 1, 0.0, 0.0);
		}
		// You rise after them, and hang a moment to follow up in the air.
		Vec3 v = player.getDeltaMovement();
		ArtKit.launch(player, new Vec3(v.x * 0.5, Math.max(v.y, ArtRules.UPDRAFT_SELF), v.z * 0.5));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ArtRules.UPDRAFT_FLOAT, 0, false, false, true));
		ElementFx.gustRing(level, player.position(), 2.2);
		for (int i = 0; i < 6; i++) {
			RandomSource r = level.getRandom();
			Motes.fling(level, player.position().add((r.nextDouble() - 0.5) * 1.2, 0.2, (r.nextDouble() - 0.5) * 1.2), new Vec3(0, 1, 0), 0.25,
				i % 2 == 0 ? WHITE : color, 0.08, 16, Vec3.ZERO);
		}
		MethodArts.whenLanded(player, 60, at -> { });
		return true;
	}

	// ------------------------------------------------------------------ III. Eye of the Storm

	static boolean eyeOfTheStorm(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.45F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_eye_of_the_storm", 1.1F, 1.0F);
		for (LivingEntity foe : ArtKit.around(player, feet, ArtRules.EYE_RADIUS, 1.5, 3.0, ArtRules.EYE_TARGETS)) {
			hits.strike(foe, ArtRules.EYE_FACTOR);
			Vec3 away = foe.position().subtract(feet);
			away = new Vec3(away.x, 0, away.z);
			away = away.lengthSqr() < 1.0E-4 ? ArtKit.flat(player) : away.normalize();
			Statuses.windPush(foe, away.scale(ArtRules.EYE_PUSH).add(0, 0.25, 0));
			ElementFx.windImpact(level, foe.getBoundingBox().getCenter(), 0.8);
		}
		// The spin's wind racing out over the ground, and the eye: a ring of wind round the swordsman while it lasts.
		ArtLight world = ArtLight.world(player);
		world.groundRing(feet, color, 0.4, ArtRules.EYE_RADIUS * 1.3, 0.2, 10);
		world.groundRing(feet, WHITE, 0.3, ArtRules.EYE_RADIUS, 0.06, 8);
		ElementFx.gustRing(level, feet, ArtRules.EYE_RADIUS);
		ArtWards.eye(player, ArtRules.EYE_TICKS);
		ArtFields.open(player, EYE, ArtFields.disc(player::position, ArtRules.EYE_GUST_RADIUS, 2.5), ArtRules.EYE_TICKS, 3, (field, owner, age) -> {
			ServerLevel lv = field.level();
			int c = ArtKit.color(owner);
			Vec3 at = owner.position();
			ArtLight show = ArtLight.spectacle(owner);
			// Two crescents of wind wheeling round the body (seen from outside), and a whirl over the ground everyone sees.
			double a = age * 0.55;
			for (int k = 0; k < 2; k++) {
				double angle = a + Math.PI * k;
				Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
				Vec3 tangent = new Vec3(-out.z, 0, out.x);
				show.slash(at.add(0, 0.9 + 0.4 * k, 0), ArtKit.UP.add(out.scale(0.2)).normalize(), tangent, k == 0 ? c : WHITE, 1.6, 1.6, 0.1, 1, 4);
			}
			if (age % 6 == 0) {
				ArtLight.world(owner).groundRing(at, c, ArtRules.EYE_GUST_RADIUS * 1.1, ArtRules.EYE_GUST_RADIUS * 0.6, 0.05, 7);
				Vfx.emit(lv, ParticleTypes.SMALL_GUST, at.add(Math.cos(a) * 2.2, 0.5, Math.sin(a) * 2.2), 1, 0.0, 0.0);
			}
			if (age % 9 == 0) {
				for (LivingEntity foe : field.foes(owner)) {
					Vec3 away = foe.position().subtract(at);
					away = new Vec3(away.x, 0, away.z);
					if (away.lengthSqr() > 1.0E-4) {
						Statuses.windPush(foe, away.normalize().scale(ArtRules.EYE_GUST).add(0, 0.1, 0));
					}
				}
			}
			if (age % 20 == 0) {
				Feels.sound(lv, at, "wind_eddy", 0.4F, 1.1F);
			}
		});
		return true;
	}

	/** A projectile turned aside by the eye: a puff of wind where it was swung round, and its whistle. */
	static void eyeTurns(ServerPlayer player, Vec3 at) {
		ServerLevel level = player.level();
		ElementFx.windImpact(level, at, 0.7);
		Feels.sound(level, at, "wind_deflect", 0.8F, 1.1F);
	}

	// ------------------------------------------------------------------ IV. Tailwind

	static boolean tailwind(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, ArtRules.TAILWIND_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, TAILWIND);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		AuraStep.afterimages(player, from, to, dir, color);
		ElementFx.gustRing(level, from, 2.0);
		Feels.sound(level, from.add(0, 1, 0), "aura_art_tailwind", 1.1F, 1.0F);
		Vec3 right = ArtKit.right(dir);
		ArtLight world = ArtLight.world(player);
		ArtKit.dash(player, path, ArtRules.TAILWIND_TICKS, (a, b, step, last) -> {
			// Two streaks of wind either side of the way, and the wake curling off it.
			world.ray(a.add(right.scale(0.5)).add(0, 1.0, 0), b.add(right.scale(0.5)).add(0, 1.0, 0), WHITE, 0.08, 9);
			world.ray(a.add(right.scale(-0.5)).add(0, 1.0, 0), b.add(right.scale(-0.5)).add(0, 1.0, 0), WHITE, 0.08, 9);
			world.ray(a.add(0, 0.3, 0), b.add(0, 0.3, 0), color, 0.3, 11);
			Vfx.emit(level, ParticleTypes.SMALL_GUST, b.add(0, 0.6, 0), 1, 0.2, 0.0);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, ArtRules.TAILWIND_WIDTH / 2 + 0.3, 2.2,
					ArtRules.TAILWIND_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= ArtRules.TAILWIND_TARGETS) {
					continue;
				}
				hits.strike(foe, ArtRules.TAILWIND_FACTOR, AuraFxRules.Weight.FULL);
				double sideOf = foe.position().subtract(b).dot(right);
				Statuses.windPush(foe, right.scale(sideOf >= 0 ? ArtRules.TAILWIND_SHOVE : -ArtRules.TAILWIND_SHOVE).add(0, 0.2, 0));
				ElementFx.windImpact(level, foe.getBoundingBox().getCenter(), 0.7);
			}
			if (last) {
				ElementFx.gustRing(level, b, 2.4);
				sweep(player, path);
			}
		});
		return true;
	}

	/** The allies near the way, and the swordsman, swept along faster. */
	private static void sweep(ServerPlayer player, List<Vec3> path) {
		ServerLevel level = player.level();
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, ArtRules.TAILWIND_SPEED, 0, false, true, true));
		Set<UUID> swept = new HashSet<>();
		swept.add(player.getUUID());
		for (int i = 0; i < path.size(); i += 5) {
			Vec3 p = path.get(i);
			for (Entity e : level.getEntities(player, new AABB(p, p).inflate(ArtRules.TAILWIND_ALLIES, 2.5, ArtRules.TAILWIND_ALLIES),
					e -> e instanceof LivingEntity && ArtKit.helpable(player, e) && !swept.contains(e.getUUID()))) {
				if (e.position().distanceTo(p) > ArtRules.TAILWIND_ALLIES) {
					continue;
				}
				swept.add(e.getUUID());
				LivingEntity ally = (LivingEntity) e;
				ally.addEffect(new MobEffectInstance(MobEffects.SPEED, ArtRules.TAILWIND_SPEED, 1, false, true, true), player);
				ElementFx.gustRing(level, ally.position(), 1.2);
				ElementFx.swirl(level, ally.position(), 0.7, 1.8, 3);
			}
		}
	}

	// ------------------------------------------------------------------ V. Hundred Winds

	static boolean hundredWinds(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = ArtKit.color(player);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SPIN, false, 1.5F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, player.position().add(0, 1, 0), "aura_art_hundred_winds", 1.3F, 1.0F);
		ElementFx.gustRing(level, player.position(), ArtRules.WINDS_RADIUS);
		ArtFields.open(player, WINDS, ArtFields.disc(player::position, ArtRules.WINDS_RADIUS, 3.0), ArtRules.WINDS_TICKS, ArtRules.WINDS_PERIOD,
			(field, owner, age) -> winds(owner, field, hits, age));
		return true;
	}

	/** One beat of the whirlwind: wind wheeling round the swordsman, foes drawn in and cut; at its end, all of them lifted. */
	private static void winds(ServerPlayer owner, ArtFields.Field field, ArtKit.Hits hits, long age) {
		ServerLevel level = field.level();
		int color = ArtKit.color(owner);
		Vec3 at = owner.position();
		RandomSource r = level.getRandom();
		ArtLight show = ArtLight.spectacle(owner);
		ArtLight world = ArtLight.world(owner);
		// The whirlwind (seen from outside): crescents wheeling up round the body on their own tilts; over the ground, everyone.
		for (int k = 0; k < 5; k++) {
			double angle = age * 0.7 + k * 1.26;
			double radius = 1.4 + (k % 3) * 1.2;
			Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
			Vec3 tangent = new Vec3(-out.z, 0, out.x);
			Vec3 normal = ArtKit.UP.add(out.scale(0.25 + 0.15 * (k % 2))).normalize();
			show.slash(at.add(0, 0.3 + k * 0.45, 0), normal, tangent, k % 2 == 0 ? color : WHITE, radius, 2.0, 0.09 + 0.02 * (k % 3), 1, 5);
		}
		world.groundRing(at, color, ArtRules.WINDS_RADIUS, 1.0, 0.07, 6);
		if (age % 10 == 0) {
			hits.fx().trail(AuraFxRules.Stroke.SPIN, age % 20 == 0, 1.2F);
			world.groundRing(at, TEAL, 0.5, ArtRules.WINDS_RADIUS * 1.05, 0.04, 9);
		}
		for (int i = 0; i < 3; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			Vfx.emit(level, ParticleTypes.CLOUD, at.add(Math.cos(a) * ArtRules.WINDS_RADIUS * 0.8, 0.2 + r.nextDouble() * 2, Math.sin(a) * ArtRules.WINDS_RADIUS * 0.8),
				1, 0.1, 0.02);
		}
		List<LivingEntity> inside = field.foes(owner);
		if (inside.size() > ArtRules.WINDS_TARGETS) {
			inside = inside.subList(0, ArtRules.WINDS_TARGETS);
		}
		boolean end = age >= ArtRules.WINDS_TICKS;
		for (LivingEntity foe : inside) {
			if (end) {
				hits.strike(foe, ArtRules.WINDS_FINISH, AuraFxRules.Weight.GRAND);
				ArtKit.lift(foe, ArtRules.WINDS_LIFT, 40);
				ElementFx.swirl(level, foe.position(), 0.8, foe.getBbHeight() + 2, 4, color, WHITE);
			} else {
				ArtKit.pull(foe, at, ArtRules.WINDS_PULL);
				hits.strike(foe, ArtRules.WINDS_FACTOR, null);
				Vec3 c = foe.getBoundingBox().getCenter();
				Vec3 n = new Vec3(r.nextDouble() - 0.5, 0.5, r.nextDouble() - 0.5).normalize();
				world.slash(c, n, ElementFx.perp(n), WHITE, 0.6, 2.4, 0.06, 1, 3);
				Vfx.emit(level, ParticleTypes.CRIT, c, 2, 0.2, 0.1);
			}
		}
		if (age % 15 == 0 && !end) {
			Feels.sound(level, at, "wind_slash", 0.45F, 1.2F + r.nextFloat() * 0.3F);
		}
		if (end) {
			world.groundRing(at, color, 0.5, ArtRules.WINDS_RADIUS * 1.4, 0.25, 12);
			world.groundRing(at, WHITE, 0.4, ArtRules.WINDS_RADIUS * 1.1, 0.08, 10);
			show.ray(at, at.add(0, 8, 0), WHITE, 0.4, 10);
			ElementFx.swirl(level, at, ArtRules.WINDS_RADIUS * 0.7, 5.0, 6, color, WHITE);
			Feels.sound(level, at, "wind_crash", 1.0F, 1.0F);
			AuraFx.sound(owner, AuraFx.Sound.IMPACT, 0.9F, 0.8F);
		}
	}
}
