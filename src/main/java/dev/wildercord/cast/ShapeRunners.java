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
		LivingEntity caster = cast.caster;
		steps(cast, 1, 2, total + 60, tick -> {
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
		// Every tick, so the strikes fall on their own interval (stepping by the shimmer's 4 ticks, a 10-tick interval
		// only ever struck every 20, and one Quicken changed nothing).
		steps(cast, 1, 1, total, tick -> {
			if (!cast.alive()) {
				return;
			}
			if (tick % 4 == 0) {
				Vfx.wall(cast.level, a, b, theme, tick);
			}
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
		Fx.sound(cast.level, center, net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND, 0.7F, 1.4F);
	}

	/** Orbit: orbs circle the caster; each creature an orb touches is hit, at most once a second. */
	static void orbit(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vfx.Theme theme) {
		int orbs = SpellNumbers.orbs(g);
		int total = SpellNumbers.orbitSeconds(g) * 20;
		Map<UUID, Long> lastHit = new HashMap<>();
		LivingEntity caster = cast.caster;
		steps(cast, 1, 1, total, tick -> {
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
		Fx.sound(cast.level, caster.position(), dev.wildercord.content.WildercordSounds.CIRCLE_OPEN, 0.5F, 1.0F);
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
					CastEngine.onHit(cast, g, new Cast.Hit(hits, origin, cast.caster.getLookAngle(), origin, null, null, false), anchored);
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
			// 14 blocks and no further, however quick: the last step used to overshoot by up to a whole step.
			double d = 1.0 + Math.min(14.0, speed * (t + 1));
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
					CastEngine.onHit(cast, g, new Cast.Hit(hits, front, dir, front.subtract(dir), null, null, false), anchored);
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
		steps(cast, 5, 5, 600, tick -> {
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
			CastEngine.onHit(cast, g, new Cast.Hit(CastEngine.inRadius(cast, point.add(0, 0.5, 0), radius), point, new Vec3(0, 1, 0), point, null, null, false), anchored);
		});
	}

	/** Totem: a floating totem that pulses its effects on everything nearby for a while. */
	static void totem(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 base, Vfx.Theme theme) {
		double radius = SpellNumbers.totemRadius(g);
		int total = SpellNumbers.totemSeconds(g) * 20;
		int interval = SpellNumbers.totemInterval(g);
		Vec3 top = base.add(0, 1.6, 0);
		steps(cast, 1, 5, total, tick -> {
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
		Fx.sound(cast.level, base, dev.wildercord.content.WildercordSounds.CIRCLE_OPEN, 0.7F, 1.0F);
	}

	// ------------------------------------------------------------------ batch 4

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
		TechniqueVfx.domainOpen(cast.level, center, radius, theme, cast.info.spell(), total + 20);
		DomainClash.open(cast, g, center, radius, total, theme.primary());
		steps(cast, 10, 5, total, t -> {
			int tick = t + 10;
			if (!cast.alive()) {
				return;
			}
			TechniqueVfx.domainShell(cast.level, center, radius, theme, tick);
			// Everyone inside sees the edges of their view take on the Domain's colour.
			for (net.minecraft.server.level.ServerPlayer player : cast.level.players()) {
				if (player.position().distanceTo(center) <= radius) {
					ScreenFx.tint(player, theme.primary(), 12);
				}
			}
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
		Scheduler.later(total + 16, () -> {
			if (cast.alive()) {
				TechniqueVfx.domainClose(cast.level, center, radius, theme);
			}
		});
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
					CastEngine.onHit(cast, g, new Cast.Hit(hits, front, aim, front.subtract(aim), null, null, false), anchored);
				}
			});
		}
		Fx.sound(cast.level, origin, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
		Fx.sound(cast.level, origin, theme.cast(), 0.6F, 1.0F);
	}

	/** Barrage: a flurry of blows over one second on whatever is right in front of you. */
	static void barrage(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		int blows = SpellNumbers.barrageBlows(g);
		LivingEntity caster = cast.caster;
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
					CastEngine.onHit(cast, g, new Cast.Hit(CastEngine.inRadius(cast, pos, radius * 1.2), pos, aim, pos, null, null, false), anchored);
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
		Fx.sound(cast.level, origin, dev.wildercord.content.WildercordSounds.ORB_HUM, 0.8F, 1.0F);
		Fx.sound(cast.level, origin, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 0.5F, 1.6F);
	}

	/** Blitz: the caster flashes forward up to 8 blocks and strikes everything along the way. */
	static void blitz(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		Vec3 look = caster.getLookAngle();
		// On the ground, a dash runs level: looking a little down shouldn't drive it into the floor.
		double rise = caster.onGround() ? Math.max(0, look.y) : look.y;
		Vec3 dir = new Vec3(look.x, Math.max(-0.35, Math.min(0.35, rise)), look.z);
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

	// ------------------------------------------------------------------ batch 6: sparks, energy balls and beams

	/** Runs {@code step} once a tick (with tick 0, 1, 2...) up to {@code ticks} times, until it returns false or the cast ends. */
	static void each(Cast cast, int ticks, java.util.function.IntPredicate step) {
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			if (!cast.alive() || tick[0] >= ticks) {
				return;
			}
			if (step.test(tick[0]++)) {
				Scheduler.later(1, next[0]);
			}
		};
		Scheduler.later(1, next[0]);
	}

	/**
	 * Runs {@code step} with t = 0, {@code stride}, 2 × {@code stride}... up to {@code last}: the first {@code delay}
	 * ticks from now, then every {@code stride} ticks. One task waits at a time, never one for every step up front
	 * (a shape that Extend has made last an hour would fill the scheduler, which every tick walks through), and none
	 * once the caster is gone for good. Each step checks {@link Cast#alive()} itself, so a caster back from another
	 * world, or a passive picking up again, carries on as before.
	 */
	static void steps(Cast cast, int delay, int stride, int last, java.util.function.IntConsumer step) {
		int[] t = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			if (cast.caster.isRemoved()) {
				return;
			}
			int now = t[0];
			t[0] = now + stride;
			// The next step is booked first, so one that fails doesn't end the rest.
			if (t[0] <= last) {
				Scheduler.later(stride, next[0]);
			}
			step.accept(now);
		};
		Scheduler.later(delay, next[0]);
	}

	/** Every living creature (never the caster) within {@code width} of the segment, nearest first, skipping {@code skip}. */
	static List<Entity> along(Cast cast, Vec3 from, Vec3 to, double width, Set<UUID> skip) {
		List<Entity> hits = new ArrayList<>();
		for (Entity e : cast.level.getEntities(cast.caster, new AABB(from, to).inflate(width + 1.0),
				e -> e instanceof LivingEntity && e.isAlive() && !skip.contains(e.getUUID()))) {
			AABB box = e.getBoundingBox().inflate(width);
			if (box.contains(from) || box.clip(from, to).isPresent()) {
				hits.add(e);
			}
		}
		hits.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(from)));
		return hits;
	}

	/** Where a line from {@code from} stops: the first solid block, or its end. */
	static net.minecraft.world.phys.BlockHitResult clip(Cast cast, Vec3 from, Vec3 to) {
		return cast.level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, cast.caster));
	}

	/** Whether the ground at {@code at} is loaded: a flight that goes on for seconds stops at its edge rather than load more. */
	private static boolean loaded(Cast cast, Vec3 at) {
		return cast.level.hasChunkAt(net.minecraft.core.BlockPos.containing(at));
	}

	private static boolean missed(net.minecraft.world.phys.BlockHitResult block) {
		return block.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	/** What something flying from {@code from} to {@code to} meets first: a creature, then a block, or nothing (null). */
	private record Contact(Entity entity, net.minecraft.world.phys.BlockHitResult block, Vec3 at, boolean parried) {
		Contact(Entity entity, net.minecraft.world.phys.BlockHitResult block, Vec3 at) {
			this(entity, block, at, false);
		}
	}

	/**
	 * What {@code g}, flying from {@code from} to {@code to}, meets first. A Shield raised at the last
	 * moment parries it: it's turned back at its caster as a bolt, and the contact says so ({@code
	 * parried}), so the shape ends there without landing.
	 */
	private static Contact contact(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 from, Vec3 to, double width, Set<UUID> skip) {
		net.minecraft.world.phys.BlockHitResult block = clip(cast, from, to);
		Vec3 end = missed(block) ? to : block.getLocation();
		Shields.Interception shield = Shields.intercept(cast, from, end);
		if (shield != null && !skip.contains(shield.target().getUUID())) {
			if (Shields.harmful(g, anchored) && Shields.parries(cast, shield.target())) {
				Shields.parry(cast, shield.target(), from, false);
				RuneBolt.reflect(cast.reflected(shield.target()), g, anchored, shield.at(), cast.caster);
				return new Contact(shield.target(), null, shield.at(), true);
			}
			// It meets a Shield's circle first.
			return new Contact(shield.target(), null, shield.at());
		}
		List<Entity> hits = along(cast, from, end, width, skip);
		if (!hits.isEmpty()) {
			Entity e = hits.getFirst();
			Vec3 at = e.getBoundingBox().inflate(width).clip(from, end).orElse(e.getBoundingBox().getCenter());
			return new Contact(e, null, at);
		}
		return missed(block) ? null : new Contact(null, block, block.getLocation());
	}

	/** A single-target shape arrives: the creature it touched, or the block it struck. */
	private static void land(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Contact c, Vec3 dir) {
		if (c.entity() != null) {
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(c.entity()), c.at(), dir, cast.caster.position(), null, null, false), anchored);
		} else {
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(), c.at(), dir, c.at(), c.block().getBlockPos(), c.block().getDirection(), false), anchored);
		}
	}

	/** Not right in front of the caster's own eyes, where a first-person view would see it as a smear. */
	private static boolean nearEyes(Cast cast, Vec3 at) {
		return at.distanceToSqr(cast.caster.getEyePosition()) < 2.25;
	}

	/** Whether a step of a flying shape is far enough from the caster's eyes to draw. */
	private static boolean visible(Cast cast, Vec3 from, Vec3 to) {
		return !nearEyes(cast, from) && !nearEyes(cast, to);
	}

	/** Spark: a small, fast mote of energy that hits the first thing in its path. */
	static void spark(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double speed = SpellNumbers.sparkSpeed(g);
		Vec3 aim = dir.normalize();
		Vec3[] pos = {origin};
		ExpansionVfx.sparkLaunch(cast.level, origin, aim, theme);
		each(cast, (int) Math.ceil(SpellNumbers.SPARK_RANGE / speed), tick -> {
			Vec3 from = pos[0];
			Vec3 to = from.add(aim.scale(speed));
			Contact c = contact(cast, g, anchored, from, to, 0.25, Set.of());
			if (c != null && c.parried()) {
				return false;
			}
			Vec3 end = c == null ? to : c.at();
			if (visible(cast, from, end)) {
				ExpansionVfx.sparkTick(cast.level, from, end, theme, tick);
			}
			if (c != null) {
				ExpansionVfx.sparkHit(cast.level, c.at(), theme);
				land(cast, g, anchored, c, aim);
				return false;
			}
			pos[0] = to;
			return true;
		});
	}

	/** Ray: an instant short line, like a Beam that reaches only 10 blocks. */
	static void ray(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 from, Vec3 dir, Vfx.Theme theme) {
		Vec3 aim = dir.normalize();
		net.minecraft.world.phys.BlockHitResult block = clip(cast, from, from.add(aim.scale(SpellNumbers.RAY_RANGE)));
		Vec3 end = missed(block) ? from.add(aim.scale(SpellNumbers.RAY_RANGE)) : block.getLocation();
		List<Entity> hits = along(cast, from, end, 0.3, Set.of());
		int max = 1 + SpellNumbers.pierce(g);
		if (hits.size() > max) {
			hits = new ArrayList<>(hits.subList(0, max));
		}
		Vec3 stop = !hits.isEmpty() && SpellNumbers.pierce(g) == 0 ? hits.getFirst().getBoundingBox().getCenter() : end;
		ExpansionVfx.ray(cast.level, from.add(aim.scale(0.7)), stop, theme);
		if (!hits.isEmpty()) {
			for (Entity e : hits) {
				ExpansionVfx.rayHit(cast.level, e.getBoundingBox().getCenter(), theme);
				CastEngine.onHit(cast, g, new Cast.Hit(List.of(e), e.getBoundingBox().getCenter(), aim, from, null, null, false), anchored);
			}
			CastEngine.chain(cast, g, anchored, hits.getFirst(), theme);
		} else if (!missed(block)) {
			ExpansionVfx.rayHit(cast.level, end, theme);
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(), end, aim, from, block.getBlockPos(), block.getDirection(), false), anchored);
		}
	}

	/** Wisp: a slow mote that chases the nearest enemy, then strikes whatever it touches first. */
	static void wisp(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double speed = SpellNumbers.wispSpeed(g);
		Vec3[] pos = {origin};
		Vec3[] velocity = {dir.normalize().scale(speed)};
		LivingEntity[] prey = {null};
		ExpansionVfx.wispRelease(cast.level, origin, theme);
		each(cast, SpellNumbers.WISP_TICKS, tick -> {
			if (prey[0] == null || !prey[0].isAlive() || tick % 10 == 0) {
				LivingEntity found = nearestEnemy(cast, pos[0], SpellNumbers.WISP_SEEK, prey[0]);
				if (found != null && found != prey[0]) {
					ExpansionVfx.wispSeek(cast.level, pos[0], found, theme);
				}
				prey[0] = found;
			}
			if (prey[0] != null) {
				Vec3 want = prey[0].getBoundingBox().getCenter().subtract(pos[0]).normalize();
				velocity[0] = velocity[0].normalize().lerp(want, 0.22).normalize().scale(speed);
			}
			Vec3 from = pos[0];
			Vec3 to = from.add(velocity[0]);
			if (!loaded(cast, to)) {
				// Never on into ground that isn't loaded: following it there would load the world as it went.
				ExpansionVfx.wispFade(cast.level, from, theme);
				return false;
			}
			Contact c = contact(cast, g, anchored, from, to, 0.35, Set.of());
			if (c != null && c.parried()) {
				return false;
			}
			if (c != null) {
				ExpansionVfx.wispStrike(cast.level, c.at(), theme);
				land(cast, g, anchored, c, velocity[0].normalize());
				return false;
			}
			pos[0] = to;
			if (visible(cast, from, to)) {
				ExpansionVfx.wispTick(cast.level, from, to, theme, tick);
			}
			if (tick == SpellNumbers.WISP_TICKS - 1) {
				ExpansionVfx.wispFade(cast.level, to, theme);
			}
			return true;
		});
	}

	/** Comet: a heavy ball that bursts on the first thing it touches (or at the end of its flight). */
	static void comet(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double speed = SpellNumbers.cometSpeed(g);
		double radius = SpellNumbers.cometRadius(g);
		Vec3 aim = dir.normalize();
		Vec3[] pos = {origin};
		int steps = (int) Math.ceil(SpellNumbers.COMET_RANGE / speed);
		ExpansionVfx.cometLaunch(cast.level, origin, aim, theme);
		each(cast, steps, tick -> {
			Vec3 from = pos[0];
			Vec3 to = from.add(aim.scale(speed));
			Contact c = contact(cast, g, anchored, from, to, 0.45, Set.of());
			if (c != null && c.parried()) {
				return false;
			}
			if (c == null && tick < steps - 1) {
				if (visible(cast, from, to)) {
					ExpansionVfx.cometTick(cast.level, from, to, theme, tick);
				}
				pos[0] = to;
				return true;
			}
			// It bursts a little short of a wall, so the blast isn't half inside it.
			Vec3 at = c == null ? to : c.block() != null ? c.at().subtract(aim.scale(0.4)) : c.at();
			ExpansionVfx.cometBurst(cast.level, at, radius, theme);
			net.minecraft.core.BlockPos block = c != null && c.block() != null ? c.block().getBlockPos() : null;
			net.minecraft.core.Direction face = c != null && c.block() != null ? c.block().getDirection() : null;
			CastEngine.onHit(cast, g, new Cast.Hit(CastEngine.inRadius(cast, at, radius), at, aim, at, block, face, false), anchored);
			return false;
		});
	}

	/**
	 * Ricochet: an orb thrown with a little lift that falls, bounces off whatever it meets and
	 * passes through creatures, hitting each once. It stops after its last bounce.
	 */
	static void ricochet(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		Vec3[] pos = {origin};
		Vec3[] velocity = {dir.normalize().scale(SpellNumbers.ricochetSpeed(g)).add(0, 0.12, 0)};
		int[] bounces = {SpellNumbers.ricochetBounces(g)};
		Set<UUID> hit = new java.util.HashSet<>();
		ExpansionVfx.ricochetLaunch(cast.level, origin, theme);
		each(cast, 140, tick -> {
			velocity[0] = velocity[0].add(0, -0.06, 0);
			Vec3 from = pos[0];
			Vec3 to = from.add(velocity[0]);
			if (!loaded(cast, to)) {
				// As a wisp: it stops where the loaded world does.
				ExpansionVfx.ricochetEnd(cast.level, from, theme);
				return false;
			}
			net.minecraft.world.phys.BlockHitResult block = clip(cast, from, to);
			Vec3 end = missed(block) ? to : block.getLocation();
			List<Entity> passed = along(cast, from, end, 0.45, hit);
			if (!passed.isEmpty()) {
				passed.forEach(e -> hit.add(e.getUUID()));
				Vec3 at = passed.getFirst().getBoundingBox().getCenter();
				ExpansionVfx.ricochetHit(cast.level, at, theme);
				CastEngine.onHit(cast, g, new Cast.Hit(passed, at, velocity[0].normalize(), from, null, null, false), anchored);
			}
			if (visible(cast, from, end)) {
				ExpansionVfx.ricochetTick(cast.level, from, end, theme, tick);
			}
			if (missed(block)) {
				pos[0] = to;
				return true;
			}
			Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getUnitVec3i());
			if (bounces[0]-- <= 0 || velocity[0].length() < 0.2) {
				ExpansionVfx.ricochetEnd(cast.level, end, theme);
				CastEngine.onHit(cast, g, new Cast.Hit(List.of(), end, velocity[0].normalize(), end, block.getBlockPos(), block.getDirection(), false), anchored);
				return false;
			}
			velocity[0] = velocity[0].subtract(normal.scale(2 * velocity[0].dot(normal))).scale(0.82);
			pos[0] = end.add(normal.scale(0.05));
			ExpansionVfx.ricochetBounce(cast.level, end, normal, theme);
			return true;
		});
	}

	/**
	 * Cluster: a ball of energy that strikes what it touches, then breaks into shards that fly out
	 * and strike everything near where each one lands. Nothing is struck twice.
	 */
	static void cluster(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vfx.Theme theme) {
		double speed = SpellNumbers.clusterSpeed(g);
		Vec3 aim = dir.normalize();
		Vec3[] pos = {origin};
		int steps = (int) Math.ceil(SpellNumbers.COMET_RANGE / speed);
		each(cast, steps, tick -> {
			Vec3 from = pos[0];
			Vec3 to = from.add(aim.scale(speed));
			Contact c = contact(cast, g, anchored, from, to, 0.4, Set.of());
			if (c != null && c.parried()) {
				return false;
			}
			if (c == null && tick < steps - 1) {
				if (visible(cast, from, to)) {
					ExpansionVfx.clusterTick(cast.level, from, to, theme, tick);
				}
				pos[0] = to;
				return true;
			}
			Set<UUID> struck = new java.util.HashSet<>();
			Vec3 at = c == null ? to : c.at();
			Vec3 normal = aim.scale(-1);
			if (c != null && c.entity() != null) {
				struck.add(c.entity().getUUID());
				land(cast, g, anchored, c, aim);
			} else if (c != null) {
				normal = Vec3.atLowerCornerOf(c.block().getDirection().getUnitVec3i());
				at = at.add(normal.scale(0.3));
				land(cast, g, anchored, c, aim);
			}
			ExpansionVfx.clusterBreak(cast.level, at, theme);
			shards(cast, g, anchored, at, normal, theme, struck);
			return false;
		});
	}

	/** A Cluster's shards: thrown out around {@code normal}, each falling until it lands and strikes. */
	private static void shards(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 at, Vec3 normal, Vfx.Theme theme, Set<UUID> struck) {
		double radius = SpellNumbers.clusterRadius(g);
		Vec3 n = normal.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : normal.normalize();
		Vec3 side = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : n.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 up = side.cross(n).normalize();
		double spin = cast.level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < SpellNumbers.CLUSTER_SHARDS; i++) {
			double a = spin + Math.PI * 2 * i / SpellNumbers.CLUSTER_SHARDS;
			Vec3 out = n.scale(0.55).add(side.scale(Math.cos(a))).add(up.scale(Math.sin(a))).normalize();
			Vec3[] pos = {at};
			Vec3[] velocity = {out.scale(0.42).add(0, 0.28, 0)};
			each(cast, 30, tick -> {
				velocity[0] = velocity[0].add(0, -0.07, 0);
				Vec3 from = pos[0];
				Vec3 to = from.add(velocity[0]);
				Contact c = contact(cast, g, anchored, from, to, 0.3, struck);
				if (c != null && c.parried()) {
					return false;
				}
				if (c == null && tick < 29) {
					ExpansionVfx.shardTick(cast.level, from, to, theme);
					pos[0] = to;
					return true;
				}
				Vec3 land = c == null ? to : c.at();
				List<Entity> hits = new ArrayList<>();
				for (Entity e : CastEngine.inRadius(cast, land, radius)) {
					if (struck.add(e.getUUID())) {
						hits.add(e);
					}
				}
				ExpansionVfx.shardLand(cast.level, land, radius, theme);
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast, g, new Cast.Hit(hits, land, velocity[0].normalize(), land, null, null, false), anchored);
				}
				return false;
			});
		}
	}

	/** Lance: a thick line of light, 16 blocks, through every creature in it. */
	static void lance(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 from, Vec3 dir, Vfx.Theme theme) {
		Vec3 aim = dir.normalize();
		Vec3 to = from.add(aim.scale(SpellNumbers.LANCE_RANGE));
		net.minecraft.world.phys.BlockHitResult block = clip(cast, from, to);
		Vec3 end = missed(block) ? to : block.getLocation();
		double width = SpellNumbers.lanceWidth(g);
		List<Entity> hits = along(cast, from, end, width, Set.of());
		ExpansionVfx.lance(cast.level, from.add(aim.scale(0.8)), end, width, theme);
		for (Entity e : hits) {
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(e), e.getBoundingBox().getCenter(), aim, from, null, null, false), anchored);
		}
		if (hits.isEmpty() && !missed(block)) {
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(), end, aim, from, block.getBlockPos(), block.getDirection(), false), anchored);
		}
	}

	/** Sweep: a beam swung across in front of the caster like a searchlight, hitting each creature once. */
	static void sweep(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		Vec3 base = (fromCaster ? caster.getLookAngle() : at.dir()).normalize();
		double length = SpellNumbers.sweepLength(g);
		int ticks = SpellNumbers.sweepTicks(g);
		Set<UUID> hit = new java.util.HashSet<>();
		ExpansionVfx.sweepStart(cast.level, fromCaster ? caster.getEyePosition().subtract(0, 0.25, 0) : at.pos(), base, length, ticks, theme);
		for (int i = 0; i <= ticks; i++) {
			int step = i;
			Scheduler.later(1 + i, () -> {
				if (!cast.alive()) {
					return;
				}
				// From 50 degrees to one side across to 50 degrees to the other.
				Vec3 dir = base.yRot((float) Math.toRadians(50 - 100.0 * step / ticks));
				Vec3 origin = fromCaster ? caster.getEyePosition().subtract(0, 0.25, 0) : at.pos();
				net.minecraft.world.phys.BlockHitResult block = clip(cast, origin, origin.add(dir.scale(length)));
				Vec3 end = missed(block) ? origin.add(dir.scale(length)) : block.getLocation();
				ExpansionVfx.sweepTick(cast.level, origin.add(dir.scale(0.8)), end, theme, step);
				List<Entity> hits = along(cast, origin, end, 0.5, hit);
				if (!hits.isEmpty()) {
					hits.forEach(e -> hit.add(e.getUUID()));
					CastEngine.onHit(cast, g, new Cast.Hit(hits, hits.getFirst().getBoundingBox().getCenter(), dir, origin, null, null, false), anchored);
				}
			});
		}
	}

	/** Prism: a beam that splits into three rays at the first creature it hits (or where it ends). */
	static void prism(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 from, Vec3 dir, Vfx.Theme theme) {
		Vec3 aim = dir.normalize();
		Vec3 to = from.add(aim.scale(SpellNumbers.PRISM_RANGE));
		net.minecraft.world.phys.BlockHitResult block = clip(cast, from, to);
		Vec3 end = missed(block) ? to : block.getLocation();
		List<Entity> first = along(cast, from, end, 0.3, Set.of());
		Set<UUID> struck = new java.util.HashSet<>();
		Vec3 split;
		if (!first.isEmpty()) {
			Entity target = first.getFirst();
			struck.add(target.getUUID());
			split = target.getBoundingBox().getCenter();
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(target), split, aim, from, null, null, false), anchored);
		} else if (!missed(block)) {
			// Against a wall there's nothing to split into: it lands like a beam.
			ExpansionVfx.prism(cast.level, from.add(aim.scale(0.8)), end, aim, List.of(), theme);
			CastEngine.onHit(cast, g, new Cast.Hit(List.of(), end, aim, from, block.getBlockPos(), block.getDirection(), false), anchored);
			return;
		} else {
			split = end;
		}
		List<Vec3> ends = new ArrayList<>();
		for (int k = -1; k <= 1; k++) {
			Vec3 rayDir = aim.yRot((float) Math.toRadians(25 * k));
			Vec3 rayTo = split.add(rayDir.scale(SpellNumbers.PRISM_RAY_RANGE));
			net.minecraft.world.phys.BlockHitResult rayBlock = clip(cast, split, rayTo);
			Vec3 rayEnd = missed(rayBlock) ? rayTo : rayBlock.getLocation();
			List<Entity> hits = along(cast, split, rayEnd, 0.3, struck);
			if (!hits.isEmpty()) {
				Entity e = hits.getFirst();
				struck.add(e.getUUID());
				rayEnd = e.getBoundingBox().getCenter();
				CastEngine.onHit(cast, g, new Cast.Hit(List.of(e), rayEnd, rayDir, split, null, null, false), anchored);
			}
			ends.add(rayEnd);
		}
		ExpansionVfx.prism(cast.level, from.add(aim.scale(0.8)), split, aim, ends, theme);
	}

	/** Stream: a steady beam that follows the caster's aim for a second, striking again and again. */
	static void stream(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		int strikes = SpellNumbers.streamStrikes(g);
		int total = SpellNumbers.STREAM_TICKS;
		Set<Integer> strikeTicks = new java.util.HashSet<>();
		for (int i = 0; i < strikes; i++) {
			strikeTicks.add((int) Math.round(i * (double) total / strikes));
		}
		ExpansionVfx.streamStart(cast.level, fromCaster ? caster.position() : at.pos(), theme);
		each(cast, total, tick -> {
			Vec3 origin = fromCaster ? caster.getEyePosition().subtract(0, 0.2, 0) : at.pos();
			Vec3 aim = (fromCaster ? caster.getLookAngle() : at.dir()).normalize();
			net.minecraft.world.phys.BlockHitResult block = clip(cast, origin, origin.add(aim.scale(SpellNumbers.STREAM_RANGE)));
			Vec3 end = missed(block) ? origin.add(aim.scale(SpellNumbers.STREAM_RANGE)) : block.getLocation();
			List<Entity> hits = along(cast, origin, end, 0.3, Set.of());
			Vec3 stop = hits.isEmpty() ? end : hits.getFirst().getBoundingBox().getCenter();
			boolean strike = strikeTicks.contains(tick);
			ExpansionVfx.streamTick(cast.level, origin.add(aim.scale(0.8)), stop, aim, theme, tick, strike);
			if (strike) {
				if (!hits.isEmpty()) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(List.of(hits.getFirst()), stop, aim, origin, null, null, false), anchored);
				} else if (!missed(block)) {
					CastEngine.onHit(cast.pulse(), g, new Cast.Hit(List.of(), end, aim, origin, block.getBlockPos(), block.getDirection(), false), anchored);
				}
			}
			return true;
		});
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
