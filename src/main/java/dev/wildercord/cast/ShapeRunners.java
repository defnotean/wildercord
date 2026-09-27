package dev.wildercord.cast;

import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The shapes that sweep an area, travel, or last a while: everything beyond the simple hits in CastEngine. */
final class ShapeRunners {
	private ShapeRunners() {}

	/** Cone: everything within a 60-degree cone in front of the origin. */
	static void cone(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double length = SpellNumbers.coneLength(g);
		double cosHalf = Math.cos(Math.toRadians(30));
		Vec3 aim = dir.normalize();
		List<Entity> hits = new ArrayList<>();
		for (Entity e : cast.level.getEntities(cast.caster, new AABB(origin, origin).inflate(length),
				e -> e instanceof LivingEntity && e.isAlive())) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
			double distance = to.length();
			if (distance <= length + e.getBbWidth() / 2 && (distance < 0.8 || to.normalize().dot(aim) >= cosHalf)) {
				hits.add(e);
			}
		}
		Vfx.cone(cast.level, origin, aim, length, theme);
		CastEngine.onHit(cast, g, new Cast.Hit(hits, origin.add(aim.scale(length * 0.6)), aim, origin, null, null, false), anchored);
	}

	/**
	 * Trail: for a while, the caster's footsteps lay glowing patches; anything standing on a
	 * patch is hit every half second. Patches fade after 3 seconds.
	 */
	static void trail(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vfx.Theme theme) {
		int total = SpellNumbers.trailSeconds(g) * 20;
		Deque<Vec3> patches = new ArrayDeque<>();
		Deque<Long> laid = new ArrayDeque<>();
		Map<UUID, Long> lastHit = new HashMap<>();
		ServerPlayer caster = cast.caster;
		for (int t = 0; t <= total + 60; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				long now = cast.level.getGameTime();
				while (!laid.isEmpty() && now - laid.peekFirst() > 60) {
					laid.pollFirst();
					patches.pollFirst();
				}
				if (tick <= total && caster.onGround() && (patches.isEmpty() || patches.peekLast().distanceToSqr(caster.position()) > 0.5)) {
					patches.addLast(caster.position());
					laid.addLast(now);
				}
				for (Vec3 patch : patches) {
					Vfx.trailPatch(cast.level, patch, theme, tick);
				}
				if (tick % 10 != 0 || patches.isEmpty()) {
					return;
				}
				Set<Entity> hits = new LinkedHashSet<>();
				for (Vec3 patch : patches) {
					for (Entity e : cast.level.getEntities((Entity) null, new AABB(patch, patch).inflate(0.9, 1.2, 0.9),
							e -> e instanceof LivingEntity && e.isAlive())) {
						Long last = lastHit.get(e.getUUID());
						if (last == null || now - last >= 10) {
							hits.add(e);
						}
					}
				}
				hits.forEach(e -> lastHit.put(e.getUUID(), now));
				if (!hits.isEmpty()) {
					Vec3 at = hits.iterator().next().position();
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(new ArrayList<>(hits), at, caster.getLookAngle(), at, null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, caster.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.4F);
	}

	/** Wall: a line across the caster's aim at a point; anything crossing it is hit on each pulse. */
	static void wall(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 center, Vec3 look, Vfx.Theme theme) {
		double width = SpellNumbers.wallWidth(g);
		int total = SpellNumbers.wallSeconds(g) * 20;
		int interval = SpellNumbers.wallInterval(g);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		Vec3 side = (flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize()).cross(new Vec3(0, 1, 0)).normalize();
		Vec3 a = center.add(side.scale(-width / 2));
		Vec3 b = center.add(side.scale(width / 2));
		AABB box = new AABB(a, b.add(0, 3.0, 0)).inflate(0.6, 0.0, 0.6);
		for (int t = 0; t <= total; t += 4) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.wall(cast.level, a, b, theme, tick);
				if (tick % interval != 0) {
					return;
				}
				List<Entity> hits = new ArrayList<>();
				for (Entity e : cast.level.getEntities((Entity) null, box, e -> e instanceof LivingEntity && e.isAlive())) {
					// Only what is actually close to the line, not the whole box around a diagonal wall.
					if (distanceToSegment(e.position(), a, b) <= 1.0) {
						hits.add(e);
					}
				}
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(hits, center, look, center, null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, center, net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND, 0.7F, 1.4F);
	}

	/** Orbit: orbs circle the caster; each creature an orb touches is hit, at most once a second. */
	static void orbit(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vfx.Theme theme) {
		int orbs = SpellNumbers.orbs(g);
		int total = SpellNumbers.orbitSeconds(g) * 20;
		Map<UUID, Long> lastHit = new HashMap<>();
		ServerPlayer caster = cast.caster;
		for (int t = 0; t <= total; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				long now = cast.level.getGameTime();
				List<Entity> hits = new ArrayList<>();
				Vec3 lastOrb = caster.position();
				for (int i = 0; i < orbs; i++) {
					double angle = tick * 0.22 + Math.PI * 2 * i / orbs;
					Vec3 orb = caster.position().add(Math.cos(angle) * 2.2, 1.0 + Math.sin(tick * 0.1 + i) * 0.25, Math.sin(angle) * 2.2);
					lastOrb = orb;
					Vfx.orb(cast.level, orb, theme, tick);
					for (Entity e : cast.level.getEntities(caster, new AABB(orb, orb).inflate(0.8), e -> e instanceof LivingEntity && e.isAlive())) {
						Long last = lastHit.get(e.getUUID());
						if (!hits.contains(e) && (last == null || now - last >= 20)) {
							hits.add(e);
							lastHit.put(e.getUUID(), now);
						}
					}
				}
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(hits, lastOrb, caster.getLookAngle(), caster.position(), null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, caster.position(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.5F, 1.8F);
	}

	/** Ring: a circle expanding from the origin; each creature is hit once, as the ring passes it. */
	static void ring(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vfx.Theme theme) {
		double radius = SpellNumbers.ringRadius(g);
		int steps = 10;
		Set<UUID> hit = new java.util.HashSet<>();
		for (int t = 0; t < steps; t++) {
			double r = radius * (t + 1) / steps;
			double inner = radius * t / steps - 0.5;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.ringFront(cast.level, origin, r, theme);
				List<Entity> hits = new ArrayList<>();
				for (Entity e : cast.level.getEntities(cast.caster, new AABB(origin, origin).inflate(r + 1, 2.5, r + 1),
						e -> e instanceof LivingEntity && e.isAlive() && !hit.contains(e.getUUID()))) {
					double d = Math.hypot(e.getX() - origin.x, e.getZ() - origin.z);
					if (d <= r + 0.6 && d >= inner) {
						hits.add(e);
						hit.add(e.getUUID());
					}
				}
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.child(), g, new Cast.Hit(hits, origin, cast.caster.getLookAngle(), origin, null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, origin, net.minecraft.sounds.SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 0.8F);
	}

	/** Pillar: a column erupting from the ground at the point. */
	static void pillar(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 base, Vfx.Theme theme) {
		double radius = SpellNumbers.pillarRadius(g);
		List<Entity> hits = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(base, base).inflate(radius, 0, radius).expandTowards(0, 6, 0),
				e -> e instanceof LivingEntity && e.isAlive())) {
			if (Math.hypot(e.getX() - base.x, e.getZ() - base.z) <= radius + e.getBbWidth() / 2) {
				hits.add(e);
			}
		}
		Vfx.pillar(cast.level, base, radius, theme);
		CastEngine.onHit(cast, g, new Cast.Hit(hits, base.add(0, 1, 0), new Vec3(0, 1, 0), base, null, null, false), anchored);
	}

	/** Wave: a line rolling forward along the ground; each creature is hit once as it passes. */
	static void wave(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 start, Vec3 look, Vfx.Theme theme) {
		double width = SpellNumbers.waveWidth(g);
		double speed = SpellNumbers.waveSpeed(g);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
		Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize();
		int steps = (int) Math.ceil(14 / speed);
		Set<UUID> hit = new java.util.HashSet<>();
		for (int t = 0; t < steps; t++) {
			double d = 1.0 + speed * (t + 1);
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vec3 front = CastEngine.ground(cast.level, start.add(dir.scale(d)).add(0, 1.5, 0));
				Vfx.waveFront(cast.level, front, side, width, theme);
				List<Entity> hits = new ArrayList<>();
				for (Entity e : cast.level.getEntities(cast.caster, new AABB(front, front).inflate(width / 2 + 0.5, 2.0, width / 2 + 0.5),
						e -> e instanceof LivingEntity && e.isAlive() && !hit.contains(e.getUUID()))) {
					hits.add(e);
					hit.add(e.getUUID());
				}
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.child(), g, new Cast.Hit(hits, front, dir, front.subtract(dir), null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, start, net.minecraft.sounds.SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.6F, 1.2F);
	}

	/** Mine: a hidden rune that waits up to 30 seconds for an enemy to step near, then fires once. */
	static void mine(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 point, Vfx.Theme theme) {
		double radius = SpellNumbers.mineRadius(g);
		boolean[] fired = {false};
		Vfx.mineArm(cast.level, point, theme);
		for (int t = 0; t <= 600; t += 5) {
			int tick = t;
			Scheduler.later(t + 5, () -> {
				if (fired[0] || !cast.alive()) {
					return;
				}
				if (tick % 40 == 0) {
					Vfx.mineIdle(cast.level, point, theme);
				}
				boolean enemyNear = !cast.level.getEntities(cast.caster, new AABB(point, point).inflate(1.8, 2.0, 1.8),
					e -> Targets.canHarm(cast.caster, e)).isEmpty();
				if (!enemyNear) {
					return;
				}
				fired[0] = true;
				Vfx.burst(cast.level, point.add(0, 0.5, 0), radius, theme);
				CastEngine.onHit(cast.child(), g, new Cast.Hit(CastEngine.inRadius(cast, point.add(0, 0.5, 0), radius), point, new Vec3(0, 1, 0), point, null, null, false), anchored);
			});
		}
	}

	/** Totem: a floating totem that pulses its effects on everything nearby for a while. */
	static void totem(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 base, Vfx.Theme theme) {
		double radius = SpellNumbers.totemRadius(g);
		int total = SpellNumbers.totemSeconds(g) * 20;
		int interval = SpellNumbers.totemInterval(g);
		Vec3 top = base.add(0, 1.6, 0);
		for (int t = 0; t <= total; t += 5) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.totem(cast.level, top, theme, tick);
				if (tick % interval != 0) {
					return;
				}
				Vfx.totemPulse(cast.level, base, radius, theme);
				List<Entity> hits = CastEngine.inRadius(cast, top, radius);
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(hits, top, new Vec3(0, 1, 0), top, null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, base, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.7F, 1.4F);
	}

	// ------------------------------------------------------------------ batch 4

	/**
	 * Stand: a guardian spirit hovers behind your shoulder and, every interval, strikes the
	 * nearest enemy within reach with the group's effects.
	 */
	static void stand(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vfx.Theme theme) {
		int total = SpellNumbers.standSeconds(g) * 20;
		// Kept up as a passive, a Stand strikes far less often: it never stops, so it mustn't shred.
		int interval = SpellNumbers.standInterval(g) * (cast.passive ? dev.wildercord.spell.Passives.STAND_SLOWDOWN : 1);
		double reach = SpellNumbers.standReach(g);
		ServerPlayer caster = cast.caster;
		if (!cast.passive) {
			TechniqueVfx.standRise(cast.level, standBody(caster), theme);
		}
		for (int t = 0; t <= total; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vec3 body = standBody(caster);
				TechniqueVfx.standFigure(cast.level, body, caster.getLookAngle(), theme, tick);
				if (tick == 0 || tick % interval >= 2) {
					return;
				}
				LivingEntity target = nearestEnemy(cast, caster.position().add(0, 1, 0), reach, caster.getLastHurtMob());
				if (target == null) {
					return;
				}
				Vec3 at = target.getBoundingBox().getCenter();
				TechniqueVfx.standStrike(cast.level, body, at, theme);
				CastEngine.onHit(cast.pulse(), g, new Cast.Hit(List.of(target), at, at.subtract(body).normalize(), body, null, null, false), anchored);
			});
		}
		Scheduler.later(total + 3, () -> {
			if (cast.alive() && !cast.passive) {
				TechniqueVfx.standFade(cast.level, standBody(caster), theme);
			}
		});
	}

	/**
	 * Where a Stand floats: behind and to the right of the caster's shoulder, far enough from
	 * their eyes (over 1.1 blocks) that the camera-clearance rule in {@link Fx#send} keeps it visible.
	 */
	private static Vec3 standBody(ServerPlayer caster) {
		Vec3 look = caster.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 right = flat.cross(new Vec3(0, 1, 0)).normalize();
		double bob = Math.sin(caster.level().getGameTime() * 0.15) * 0.06;
		return caster.position().add(flat.scale(-0.95)).add(right.scale(0.95)).add(0, 1.3 + bob, 0);
	}

	/** The enemy nearest {@code from} within {@code range}; {@code preferred} wins if it is in range. */
	static LivingEntity nearestEnemy(Cast cast, Vec3 from, double range, LivingEntity preferred) {
		if (preferred != null && preferred.isAlive() && Targets.canHarm(cast.caster, preferred)
				&& preferred.getBoundingBox().getCenter().distanceTo(from) <= range) {
			return preferred;
		}
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Entity e : cast.level.getEntities(cast.caster, new AABB(from, from).inflate(range), e -> Targets.canHarm(cast.caster, e))) {
			double d = e.getBoundingBox().getCenter().distanceTo(from);
			if (d <= range && d < bestDistance && cast.caster.hasLineOfSight(e)) {
				best = (LivingEntity) e;
				bestDistance = d;
			}
		}
		return best;
	}

	/**
	 * Domain: a dome around the point for a while. Every interval everything inside is struck,
	 * and enemies inside are slowed so they struggle to leave.
	 */
	static void domain(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 center, Vfx.Theme theme) {
		double radius = SpellNumbers.domainRadius(g);
		int total = SpellNumbers.domainSeconds(g) * 20;
		int interval = SpellNumbers.domainInterval(g);
		TechniqueVfx.domainOpen(cast.level, center, radius, theme);
		for (int t = 10; t <= total + 10; t += 5) {
			int tick = t;
			Scheduler.later(t, () -> {
				if (!cast.alive()) {
					return;
				}
				TechniqueVfx.domainShell(cast.level, center, radius, theme, tick);
				if ((tick - 10) % interval >= 5) {
					return;
				}
				List<Entity> inside = CastEngine.inRadius(cast, center.add(0, 1, 0), radius);
				for (Entity e : inside) {
					if (Targets.canHarm(cast.caster, e)) {
						((LivingEntity) e).addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, interval + 10, 1, false, false));
					}
				}
				TechniqueVfx.domainStrike(cast.level, center, radius, theme);
				if (!inside.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(inside, center, new Vec3(0, 1, 0), center, null, null, false), anchored);
				}
			});
		}
		Scheduler.later(total + 16, () -> TechniqueVfx.domainClose(cast.level, center, radius, theme));
	}

	/** Crescent: a wide slash flying forward at chest height, cutting each creature once. */
	static void crescent(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double width = SpellNumbers.crescentWidth(g);
		double speed = SpellNumbers.crescentSpeed(g);
		Vec3 aim = dir.normalize();
		Vec3 side = aim.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 sideF = side;
		int steps = (int) Math.ceil(16 / speed);
		Set<UUID> hit = new java.util.HashSet<>();
		boolean[] stopped = {false};
		for (int t = 0; t < steps; t++) {
			double d = 1.0 + speed * (t + 1);
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (stopped[0] || !cast.alive()) {
					return;
				}
				Vec3 front = origin.add(aim.scale(d));
				net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(front);
				if (!cast.level.getBlockState(pos).getCollisionShape(cast.level, pos).isEmpty()) {
					stopped[0] = true;
					Vfx.impact(cast.level, front.subtract(aim.scale(0.5)), theme, 0.8);
					return;
				}
				TechniqueVfx.crescent(cast.level, front, aim, sideF, width, theme, tick);
				List<Entity> hits = new ArrayList<>();
				for (Entity e : cast.level.getEntities(cast.caster, new AABB(front, front).inflate(width / 2 + 1, 2.0, width / 2 + 1),
						e -> e instanceof LivingEntity && e.isAlive() && !hit.contains(e.getUUID()))) {
					Vec3 rel = e.getBoundingBox().getCenter().subtract(front);
					if (Math.abs(rel.dot(aim)) <= speed / 2 + 0.8 && Math.abs(rel.dot(sideF)) <= width / 2 + e.getBbWidth() / 2 && Math.abs(rel.y) <= 1.6) {
						hits.add(e);
						hit.add(e.getUUID());
					}
				}
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.child(), g, new Cast.Hit(hits, front, aim, front.subtract(aim), null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, origin, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
		Fx.sound(cast.level, origin, theme.cast(), 0.6F, 1.3F);
	}

	/** Barrage: a flurry of blows over one second on whatever is right in front of you. */
	static void barrage(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		int blows = SpellNumbers.barrageBlows(g);
		ServerPlayer caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		for (int i = 0; i < blows; i++) {
			int blow = i;
			Scheduler.later(1 + (int) Math.round(i * 20.0 / blows), () -> {
				if (!cast.alive()) {
					return;
				}
				List<Entity> hits;
				Vec3 origin;
				Vec3 aim;
				if (fromCaster) {
					origin = caster.getEyePosition().subtract(0, 0.2, 0);
					aim = caster.getLookAngle();
					hits = coneTargets(cast, origin, aim, 3.6, 40);
				} else {
					origin = at.pos();
					aim = at.dir();
					hits = at.entity() != null && at.entity().isAlive() && at.entity() != caster
						? List.of(at.entity()) : CastEngine.inRadius(cast, at.pos(), 1.5);
				}
				Vec3 front = hits.isEmpty() ? origin.add(aim.scale(1.8)) : hits.getFirst().getBoundingBox().getCenter();
				TechniqueVfx.barrageBlow(cast.level, origin, front, theme, blow, blow == blows - 1);
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(hits, front, aim, origin, null, null, false), anchored);
				}
			});
		}
	}

	/** Every living creature within {@code length} of the origin and {@code halfAngle} degrees of the aim. */
	static List<Entity> coneTargets(Cast cast, Vec3 origin, Vec3 aim, double length, double halfAngle) {
		double cosHalf = Math.cos(Math.toRadians(halfAngle));
		Vec3 dir = aim.normalize();
		List<Entity> hits = new ArrayList<>();
		for (Entity e : cast.level.getEntities(cast.caster, new AABB(origin, origin).inflate(length), e -> e instanceof LivingEntity && e.isAlive())) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
			double distance = to.length();
			if (distance <= length + e.getBbWidth() / 2 && (distance < 1.0 || to.normalize().dot(dir) >= cosHalf)) {
				hits.add(e);
			}
		}
		hits.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
		return hits;
	}

	/**
	 * Orb: a slow, heavy sphere that drifts forward through creatures, striking everything near
	 * it at most once a second, and bursting when it meets a wall.
	 */
	static void orb(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double radius = SpellNumbers.orbRadius(g);
		double speed = SpellNumbers.orbSpeed(g);
		Vec3 aim = dir.normalize();
		int steps = (int) Math.ceil(20 / speed);
		Map<UUID, Long> lastHit = new HashMap<>();
		boolean[] done = {false};
		for (int t = 0; t <= steps; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (done[0] || !cast.alive()) {
					return;
				}
				Vec3 pos = origin.add(aim.scale(speed * tick));
				net.minecraft.core.BlockPos block = net.minecraft.core.BlockPos.containing(pos);
				boolean wall = !cast.level.getBlockState(block).getCollisionShape(cast.level, block).isEmpty();
				if (wall || tick == steps) {
					done[0] = true;
					Vfx.burst(cast.level, pos, radius * 1.2, theme);
					CastEngine.onHit(cast.child(), g, new Cast.Hit(CastEngine.inRadius(cast, pos, radius * 1.2), pos, aim, pos, null, null, false), anchored);
					return;
				}
				TechniqueVfx.orb(cast.level, pos, radius, theme, tick);
				long now = cast.level.getGameTime();
				List<Entity> hits = new ArrayList<>();
				for (Entity e : CastEngine.inRadius(cast, pos, radius)) {
					if (e == cast.caster) {
						continue;
					}
					Long last = lastHit.get(e.getUUID());
					if (last == null || now - last >= 20) {
						hits.add(e);
						lastHit.put(e.getUUID(), now);
					}
				}
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(hits, pos, aim, pos.subtract(aim), null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, origin, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.8F, 0.6F);
		Fx.sound(cast.level, origin, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 0.5F, 1.6F);
	}

	/** Blitz: the caster flashes forward up to 8 blocks and strikes everything along the way. */
	static void blitz(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vfx.Theme theme) {
		ServerPlayer caster = cast.caster;
		Vec3 look = caster.getLookAngle();
		Vec3 dir = new Vec3(look.x, Math.max(-0.35, Math.min(0.35, look.y)), look.z);
		dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
		Vec3 start = caster.position();
		Vec3 end = start;
		for (double d = 0.4; d <= 8.0; d += 0.4) {
			Vec3 spot = start.add(dir.scale(d));
			if (!cast.level.noCollision(caster, caster.getDimensions(caster.getPose()).makeBoundingBox(spot))) {
				break;
			}
			end = spot;
		}
		double width = SpellNumbers.blitzWidth(g);
		Vec3 a = start.add(0, 0.9, 0);
		Vec3 b = end.add(0, 0.9, 0);
		List<Entity> hits = new ArrayList<>();
		for (Entity e : cast.level.getEntities(caster, new AABB(a, b).inflate(width + 1.0), e -> e instanceof LivingEntity && e.isAlive())) {
			if (distanceToLine(e.getBoundingBox().getCenter(), a, b) <= width + e.getBbWidth() / 2) {
				hits.add(e);
			}
		}
		TechniqueVfx.blitz(cast.level, a, b, theme);
		if (end != start) {
			caster.teleportTo(cast.level, end.x, end.y, end.z, Set.<net.minecraft.world.entity.Relative>of(), caster.getYRot(), caster.getXRot(), false);
			caster.resetFallDistance();
		}
		CastEngine.onHit(cast, g, new Cast.Hit(hits, b, dir, a, null, null, false), anchored);
	}

	/** Distance from a point to the segment a-b, in 3D. */
	private static double distanceToLine(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr())));
		return p.distanceTo(a.add(ab.scale(t)));
	}

	private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = new Vec3(b.x - a.x, 0, b.z - a.z);
		Vec3 ap = new Vec3(p.x - a.x, 0, p.z - a.z);
		double t = Math.max(0, Math.min(1, ap.dot(ab) / Math.max(1.0E-6, ab.lengthSqr())));
		return ap.subtract(ab.scale(t)).length();
	}
}
