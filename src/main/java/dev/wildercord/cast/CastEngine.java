package dev.wildercord.cast;

import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Runs a compiled spell in the world. */
public final class CastEngine {
	private CastEngine() {}

	public static final double BEAM_RANGE = 24.0;
	public static final double AIM_RANGE = 24.0;
	private static final int MAX_TRIGGERS_PER_HIT = 8;

	public static void cast(LivingEntity caster, SpellPlan.Segment root) {
		cast(caster, root, 1, dev.wildercord.player.Heart.Bonuses.NONE, false, null);
	}

	/**
	 * @param castNumber how many times this spell has now been cast, for Combo
	 * @param bonuses    the caster's Heart Circles and Cord enchantments
	 * @param passive    a passive renewing itself
	 */
	public static void cast(LivingEntity caster, SpellPlan.Segment root, int castNumber, dev.wildercord.player.Heart.Bonuses bonuses, boolean passive,
			java.util.function.BooleanSupplier wanted) {
		cast(new Cast(caster, castNumber, bonuses, passive, wanted, new Cast.Info(root, 0, "")), root);
	}

	/** Runs a spell for a cast that's already set up. */
	public static void cast(Cast cast, SpellPlan.Segment root) {
		runSegment(cast, root, Cast.Trigger.self(cast.caster));
	}

	static void runSegment(Cast cast, SpellPlan.Segment seg, Cast.Trigger at) {
		if (seg == null || !cast.alive() || !cast.takeSegment()) {
			return;
		}
		for (SpellPlan.Group group : seg.groups) {
			SpellPlan.Link anchored = seg.link != null && seg.link.anchor == group ? seg.link : null;
			deliver(cast, group, at, anchored);
		}
		SpellPlan.Link link = seg.link;
		if (link == null) {
			return;
		}
		LivingEntity caster = cast.caster;
		String id = link.link.id();
		if (id.equals(Runes.DELAY.id())) {
			Cast child = cast.child();
			Scheduler.later(SpellNumbers.delayTicks(link), () -> runSegment(child, link.next, Cast.Trigger.self(caster)));
		} else if (id.equals(Runes.ON_LAND.id())) {
			Cast child = cast.child();
			Scheduler.onLand(caster, 200, pos -> {
				// Not for a caster who has changed dimension since (the watch follows them there).
				if (!child.alive()) {
					return;
				}
				Vfx.shockwave(child.level, pos, 2.0, Vfx.theme(""), 4);
				Fx.sound(child.level, pos, net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND, 0.7F, 1.3F);
				runSegment(child, link.next, new Cast.Trigger(pos, caster.getLookAngle(), caster, null, null));
			});
		} else if (id.equals(Runes.PULSE.id())) {
			// After On Hit or On Kill, only the first hit's Pulse goes off: it was paid for once.
			if (!link.firstOnly || cast.firstRepeat(link)) {
				int interval = SpellNumbers.pulseInterval(link);
				for (int i = 0; i < SpellNumbers.PULSES; i++) {
					Cast child = cast.repeat();
					Scheduler.later(1 + i * interval, () -> runSegment(child, link.next, Cast.Trigger.self(caster)));
				}
			}
		} else if (id.equals(Runes.ON_HURT.id())) {
			Cast child = cast.child();
			Scheduler.onHurt(caster, 300, attacker -> {
				// Not for a caster who has died of it, or changed dimension since.
				if (!child.alive()) {
					return;
				}
				Vfx.shockwave(child.level, caster.position(), 1.8, Vfx.theme(""), 3);
				if (attacker != null && attacker.isAlive()) {
					Vec3 dir = attacker.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize();
					runSegment(child, link.next, new Cast.Trigger(attacker.getBoundingBox().getCenter(), dir, attacker, null, null));
				} else {
					runSegment(child, link.next, new Cast.Trigger(caster.position(), caster.getLookAngle(), null, null, null));
				}
			});
		} else if (id.equals(Runes.IF_SNEAKING.id())) {
			if (caster.isShiftKeyDown()) {
				runSegment(cast, link.next, at);
			} else {
				refund(cast, link);
			}
		} else if (id.equals(Runes.IF_AIRBORNE.id())) {
			if (!caster.onGround() && !caster.isInWater()) {
				TechniqueVfx.airborne(cast.level, caster);
				runSegment(cast, link.next, at);
			} else {
				refund(cast, link);
			}
		} else if (id.equals(Runes.COMBO.id())) {
			if (cast.castNumber % 3 == 0) {
				TechniqueVfx.combo(cast.level, caster);
				Reactions.callout(cast, "combo", 0xF0C440);
				runSegment(cast, link.next, at);
			} else {
				refund(cast, link);
			}
		} else if (ExplorerShapes.isCondition(id)) {
			// If Wounded, If Outnumbered, If Wet: the runes of the world's conditions.
			if (ExplorerShapes.conditionMet(cast, id)) {
				ExplorerVfx.condition(cast.level, caster, id);
				runSegment(cast, link.next, at);
			} else {
				refund(cast, link);
			}
		} else if (id.equals(Runes.ON_LOW_HEALTH.id())) {
			Cast child = cast.child();
			Scheduler.onLowHealth(caster, 600, () -> {
				if (!child.alive()) {
					return;
				}
				Vfx.shockwave(child.level, caster.position(), 2.4, Vfx.theme("life"), 4);
				runSegment(child, link.next, new Cast.Trigger(caster.position().add(0, 1, 0), caster.getLookAngle(), caster, null, null));
			});
		} else if (id.equals(Runes.ECHO.id())) {
			// After On Hit or On Kill, only the first hit's Echo goes off: it was paid for once.
			if (link.echoPrefix != null && (!link.firstOnly || cast.firstRepeat(link))) {
				Cast child = cast.repeat();
				// In a stored spell it repeats what was stored where the release was set off (it used to land on the
				// caster, so a stored Fire's echo burned nothing); otherwise the whole spell again, from you.
				boolean stored = link.echoPrefix.implicitShape.is(Runes.TRIGGER.id()) && cast.origin() != null;
				Cast.Trigger from = stored ? cast.origin() : Cast.Trigger.self(caster);
				Scheduler.later(10, () -> runSegment(child, link.echoPrefix, from));
			}
			runSegment(cast, link.next, at);
		} else {
			// A link from an add-on (dev.wildercord.api) decides when the rest fires.
			AddonRunes.link(cast, link, at);
		}
		// On Hit, On Kill, On Reaction and On Weakness fire from onHit(), through their anchor group.
	}

	/**
	 * A condition that doesn't hold (Combo on its first two casts, If Sneaking while standing...) gives back the mana its branch
	 * cost: it was paid with the rest of the spell, and a finisher bought three times for one firing was a tax, not a choice.
	 * Only for a spell cast from a Cord and paid in mana, once per link per payment.
	 */
	private static void refund(Cast cast, SpellPlan.Link link) {
		if (!(cast.caster instanceof ServerPlayer player) || cast.passive || cast.origin() != null || player.isCreative() || link.next == null
				|| cast.info.root() == null || cast.info.spell().isEmpty()) {
			return;
		}
		dev.wildercord.spell.SpellCompiler.Compiled compiled = dev.wildercord.spell.SpellCompiler.compile(cast.info.spell());
		if (compiled.paysInHealth() || compiled.cost() <= 0 || !cast.once("refund:" + System.identityHashCode(link))) {
			return;
		}
		double share = dev.wildercord.spell.SpellCompiler.segmentCost(link.next) / compiled.cost();
		int paid = dev.wildercord.player.Heart.manaCost(player, compiled, 1.0);
		float back = (float) Math.min(paid, paid * share);
		dev.wildercord.player.Spellbooks.setMana(player, Math.min(dev.wildercord.player.Mana.max(player), dev.wildercord.player.Spellbooks.mana(player) + back));
	}

	// ------------------------------------------------------------------ shapes

	private static void deliver(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored) {
		LivingEntity caster = cast.caster;
		String shape = g.shape.id();
		int copies = SpellNumbers.copies(g);
		int color = colorOf(g);
		Vfx.Theme theme = cast.theme(g);

		if (shape.equals(Runes.SELF.id())) {
			Vfx.self(caster, theme);
			onHit(cast, g, new Cast.Hit(List.of(caster), caster.position(), caster.getLookAngle(), caster.position(), null, null, true), anchored);
		} else if (shape.equals(Runes.TRIGGER.id())) {
			Entity entity = at.entity() != null && at.entity().isAlive() ? at.entity() : null;
			List<Entity> entities = entity == null ? List.of() : List.of(entity);
			onHit(cast, g, new Cast.Hit(entities, at.pos(), at.dir(), at.pos(), at.block(), at.face(), entity == caster), anchored);
		} else if (shape.equals(Runes.TOUCH.id())) {
			touch(cast, g, at, anchored, theme);
		} else if (shape.equals(Runes.BOLT.id()) || shape.equals(Runes.ARC.id())) {
			boolean arc = shape.equals(Runes.ARC.id());
			volley(cast, g, () -> {
				boolean fromCaster = at.fromCaster(caster);
				Vec3 origin = fromCaster ? caster.getEyePosition().add(caster.getLookAngle().scale(0.6)) : at.pos();
				Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
				for (Vec3 dir : fan(aim, copies)) {
					RuneBolt.launch(cast, g, anchored, origin, arc ? dir.add(0, 0.28, 0).normalize() : dir, arc);
				}
			});
		} else if (shape.equals(Runes.BEAM.id())) {
			volley(cast, g, () -> {
				boolean fromCaster = at.fromCaster(caster);
				Vec3 origin = fromCaster ? caster.getEyePosition() : at.pos();
				Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
				for (Vec3 dir : fan(aim, copies)) {
					beam(cast, g, anchored, origin, dir, theme);
				}
			});
		} else if (shape.equals(Runes.CONE.id())) {
			Vec3 origin = at.fromCaster(caster) ? caster.getEyePosition().subtract(0, 0.2, 0) : at.pos();
			ShapeRunners.cone(cast, g, anchored, origin, at.fromCaster(caster) ? caster.getLookAngle() : at.dir(), theme);
		} else if (shape.equals(Runes.TRAIL.id())) {
			ShapeRunners.trail(cast, g, anchored, theme);
		} else if (shape.equals(Runes.WALL.id())) {
			ShapeRunners.wall(cast, g, anchored, aimPoint(cast, at), at.fromCaster(caster) ? caster.getLookAngle() : at.dir(), theme);
		} else if (shape.equals(Runes.ORBIT.id())) {
			ShapeRunners.orbit(cast, g, anchored, theme);
		} else if (shape.equals(Runes.RING.id())) {
			ShapeRunners.ring(cast, g, anchored, at.fromCaster(caster) ? caster.position().add(0, 0.2, 0) : at.pos(), theme);
		} else if (shape.equals(Runes.PILLAR.id())) {
			double radius = SpellNumbers.pillarRadius(g);
			for (Vec3 base : spread(aimPoint(cast, at), copies, radius)) {
				ShapeRunners.pillar(cast, g, anchored, base, theme);
			}
		} else if (shape.equals(Runes.WAVE.id())) {
			ShapeRunners.wave(cast, g, anchored, at.fromCaster(caster) ? caster.position() : at.pos(), at.fromCaster(caster) ? caster.getLookAngle() : at.dir(), theme);
		} else if (shape.equals(Runes.MINE.id())) {
			for (Vec3 point : spread(aimPoint(cast, at), copies, 2.0)) {
				ShapeRunners.mine(cast, g, anchored, point, theme);
			}
		} else if (shape.equals(Runes.TOTEM.id())) {
			ShapeRunners.totem(cast, g, anchored, aimPoint(cast, at), theme);
		} else if (shape.equals(Runes.DOMAIN.id())) {
			ShapeRunners.domain(cast, g, anchored, at.fromCaster(caster) ? caster.position() : ground(cast.level, at.pos()), theme);
		} else if (shape.equals(Runes.CRESCENT.id())) {
			volley(cast, g, () -> {
				boolean fromCaster = at.fromCaster(caster);
				Vec3 origin = fromCaster ? caster.getEyePosition().subtract(0, 0.3, 0) : at.pos();
				Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
				for (Vec3 dir : fan(aim, copies)) {
					ShapeRunners.crescent(cast, g, anchored, origin, dir, theme);
				}
			});
		} else if (shape.equals(Runes.BARRAGE.id())) {
			ShapeRunners.barrage(cast, g, anchored, at, theme);
		} else if (shape.equals(Runes.ORB.id())) {
			boolean fromCaster = at.fromCaster(caster);
			Vec3 origin = fromCaster ? caster.getEyePosition().add(caster.getLookAngle().scale(1.2)) : at.pos();
			Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
			for (Vec3 dir : fan(aim, copies)) {
				ShapeRunners.orb(cast, g, anchored, origin, dir, theme);
			}
		} else if (shape.equals(Runes.BLITZ.id())) {
			ShapeRunners.blitz(cast, g, anchored, theme);
		} else if (shape.equals(Runes.SPARK.id()) || shape.equals(Runes.COMET.id())) {
			boolean comet = shape.equals(Runes.COMET.id());
			volley(cast, g, () -> {
				boolean fromCaster = at.fromCaster(caster);
				Vec3 origin = fromCaster ? caster.getEyePosition().add(caster.getLookAngle().scale(comet ? 1.2 : 0.6)) : at.pos();
				Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
				for (Vec3 dir : fan(aim, copies)) {
					if (comet) {
						ShapeRunners.comet(cast, g, anchored, origin, dir, theme);
					} else {
						ShapeRunners.spark(cast, g, anchored, origin, dir, theme);
					}
				}
			});
		} else if (shape.equals(Runes.WISP.id()) || shape.equals(Runes.RICOCHET.id()) || shape.equals(Runes.CLUSTER.id())) {
			boolean fromCaster = at.fromCaster(caster);
			Vec3 origin = fromCaster ? caster.getEyePosition().add(caster.getLookAngle().scale(1.0)).subtract(0, 0.2, 0) : at.pos();
			Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
			for (Vec3 dir : fan(aim, copies)) {
				if (shape.equals(Runes.WISP.id())) {
					ShapeRunners.wisp(cast, g, anchored, origin, dir, theme);
				} else if (shape.equals(Runes.RICOCHET.id())) {
					ShapeRunners.ricochet(cast, g, anchored, origin, dir, theme);
				} else {
					ShapeRunners.cluster(cast, g, anchored, origin, dir, theme);
				}
			}
		} else if (shape.equals(Runes.RAY.id()) || shape.equals(Runes.LANCE.id()) || shape.equals(Runes.PRISM.id())) {
			boolean fromCaster = at.fromCaster(caster);
			Vec3 origin = fromCaster ? caster.getEyePosition() : at.pos();
			Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
			for (Vec3 dir : fan(aim, copies)) {
				if (shape.equals(Runes.RAY.id())) {
					ShapeRunners.ray(cast, g, anchored, origin, dir, theme);
				} else if (shape.equals(Runes.LANCE.id())) {
					ShapeRunners.lance(cast, g, anchored, origin, dir, theme);
				} else {
					ShapeRunners.prism(cast, g, anchored, origin, dir, theme);
				}
			}
		} else if (shape.equals(Runes.SWEEP.id())) {
			ShapeRunners.sweep(cast, g, anchored, at, theme);
		} else if (shape.equals(Runes.STREAM.id())) {
			ShapeRunners.stream(cast, g, anchored, at, theme);
		} else if (shape.equals(Runes.NOVA.id())) {
			double radius = SpellNumbers.novaRadius(g);
			Vec3 center = at.fromCaster(caster) ? caster.position().add(0, 1, 0) : at.pos();
			ExpansionVfx.nova(cast.level, center, radius, theme);
			onHit(cast, g, new Cast.Hit(inRadius(cast, center, radius), center, at.dir(), center, null, null, false), anchored);
		} else if (shape.equals(Runes.BURST.id())) {
			double radius = SpellNumbers.burstRadius(g);
			Vec3 base = at.fromCaster(caster) ? caster.position().add(0, 1, 0) : at.pos();
			for (Vec3 center : spread(base, copies, radius)) {
				Vfx.burst(cast.level, center, radius, theme);
				onHit(cast, g, new Cast.Hit(inRadius(cast, center, radius), center, at.dir(), center, null, null, false), anchored);
			}
		} else if (shape.equals(Runes.ZONE.id())) {
			double radius = SpellNumbers.zoneRadius(g);
			int pulses = SpellNumbers.zonePulses(g);
			int interval = SpellNumbers.zoneInterval(g);
			for (Vec3 center : spread(aimPoint(cast, at), copies, radius)) {
				Vfx.zoneOpen(cast.level, center, radius, theme, pulses * interval + 12);
				ShapeRunners.steps(cast, 1, interval, (pulses - 1) * interval, t -> {
					Cast child = cast.pulse();
					if (!child.alive()) {
						return;
					}
					Vfx.zonePulse(child.level, center, radius, theme, t / interval);
					onHit(child, g, new Cast.Hit(inRadius(child, center.add(0, 1, 0), radius), center, at.dir(), center, null, null, false), anchored);
				});
			}
		} else if (shape.equals(Runes.RAIN.id())) {
			double radius = SpellNumbers.rainRadius(g);
			Vec3 center = aimPoint(cast, at);
			int strikes = 5 * copies;
			Vfx.rainCloud(cast.level, center, radius, theme, 56);
			// The sky looks for enemies: most strikes fall on one in the area (any, at random), the rest at random points.
			List<Entity> enemies = inRadius(cast, center.add(0, 1, 0), radius).stream().filter(e -> Targets.canHarm(caster, e)).toList();
			for (int i = 0; i < strikes; i++) {
				Cast child = cast.pulse();
				double a = cast.level.getRandom().nextDouble() * Math.PI * 2;
				double r = Math.sqrt(cast.level.getRandom().nextDouble()) * radius;
				Vec3 target = ground(cast.level, center.add(Math.cos(a) * r, 2, Math.sin(a) * r));
				if (!enemies.isEmpty() && cast.level.getRandom().nextFloat() < SpellNumbers.RAIN_SEEK) {
					target = ground(cast.level, enemies.get(cast.level.getRandom().nextInt(enemies.size())).position().add(0, 2, 0));
				}
				Vec3 strike = target;
				int delay = 1 + i * 40 / strikes;
				Scheduler.later(delay, () -> {
					if (child.alive()) {
						Vfx.rainStrike(child.level, strike, theme);
					}
				});
				// The hit lands when the streak does.
				Scheduler.later(delay + 6, () -> {
					if (!child.alive()) {
						return;
					}
					BlockPos below = BlockPos.containing(strike.x, strike.y - 0.5, strike.z);
					onHit(child, g, new Cast.Hit(inRadius(child, strike.add(0, 1, 0), SpellNumbers.RAIN_STRIKE), strike, new Vec3(0, -1, 0), strike, below, net.minecraft.core.Direction.UP, false), anchored);
				});
			}
		} else if (CraftedShapes.handles(shape)) {
			// New runes (batch 2): Glaive, Imprint and Latch.
			CraftedShapes.deliver(cast, g, at, anchored, theme);
		} else {
			// A shape from an add-on (dev.wildercord.api) finds its own hits; otherwise the shapes of the world: Vortex, Snare and Constellation.
			if (!AddonRunes.shape(cast, g, at, anchored)) {
				ExplorerShapes.deliver(cast, g, at, anchored, theme);
			}
		}
	}

	private static void touch(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		Vec3 from = at.fromCaster(caster) ? caster.getEyePosition() : at.pos();
		double reach = at.fromCaster(caster) ? Casters.entityReach(caster) : 3.0;
		Vec3 to = from.add(at.dir().scale(Math.max(reach, Casters.blockReach(caster))));
		BlockHitResult block = cast.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
		Vec3 entityTo = from.add(at.dir().scale(reach));
		if (block.getType() != HitResult.Type.MISS && block.getLocation().distanceTo(from) < reach) {
			entityTo = block.getLocation();
		}
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, from, entityTo,
			new AABB(from, entityTo).inflate(1.0), e -> e != caster && e instanceof LivingEntity && e.isAlive(), reach * reach);
		if (entityHit != null) {
			Entity target = entityHit.getEntity();
			Vfx.contact(cast.level, from.add(at.dir().scale(0.5)), entityHit.getLocation(), theme);
			Vfx.impact(cast.level, entityHit.getLocation(), theme, 0.8);
			onHit(cast, g, new Cast.Hit(List.of(target), entityHit.getLocation(), at.dir(), from, null, null, false), anchored);
			chain(cast, g, anchored, target, theme);
		} else if (block.getType() != HitResult.Type.MISS) {
			Vfx.impact(cast.level, block.getLocation(), theme, 0.6);
			onHit(cast, g, new Cast.Hit(List.of(), block.getLocation(), at.dir(), from, block.getBlockPos(), block.getDirection(), false), anchored);
		}
	}

	private static void beam(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 from, Vec3 dir, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		Vec3 to = from.add(dir.scale(BEAM_RANGE));
		BlockHitResult block = cast.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
		List<Entity> along = new ArrayList<>();
		for (Entity e : cast.level.getEntities(caster, new AABB(from, end).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive())) {
			if (e.getBoundingBox().inflate(0.3).clip(from, end).isPresent() || e.getBoundingBox().contains(from)) {
				along.add(e);
			}
		}
		along.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
		int maxHits = 1 + SpellNumbers.pierce(g);
		Vec3 stop = end;
		if (along.size() > maxHits) {
			along = new ArrayList<>(along.subList(0, maxHits));
		}
		if (!along.isEmpty() && along.size() == maxHits && SpellNumbers.pierce(g) == 0) {
			stop = along.getLast().getBoundingBox().getCenter();
		}
		Vfx.beam(cast.level, from.add(dir.scale(0.8)), stop, theme);
		if (!along.isEmpty()) {
			for (Entity e : along) {
				Vfx.impact(cast.level, e.getBoundingBox().getCenter(), theme, 0.8);
				onHit(cast, g, new Cast.Hit(List.of(e), e.getBoundingBox().getCenter(), dir, from, null, null, false), anchored);
			}
			chain(cast, g, anchored, along.getFirst(), theme);
		} else if (block.getType() != HitResult.Type.MISS) {
			Vfx.impact(cast.level, end, theme, 0.7);
			onHit(cast, g, new Cast.Hit(List.of(), end, dir, from, block.getBlockPos(), block.getDirection(), false), anchored);
		}
	}

	/** Chain: from the first creature hit, jump to up to N more enemies within 6 blocks. */
	static void chain(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Entity first, Vfx.Theme theme) {
		int jumps = SpellNumbers.chainJumps(g);
		Entity current = first;
		List<Entity> visited = new ArrayList<>();
		visited.add(first);
		for (int i = 0; i < jumps; i++) {
			Entity from = current;
			Entity next = cast.level.getEntities(cast.caster, from.getBoundingBox().inflate(6.0),
					e -> !visited.contains(e) && Targets.canHarm(cast.caster, e) && e.distanceTo(from) <= 6.0)
				.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
			if (next == null) {
				return;
			}
			visited.add(next);
			Vfx.beam(cast.level, from.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), theme);
			Vfx.impact(cast.level, next.getBoundingBox().getCenter(), theme, 0.6);
			onHit(cast, g, new Cast.Hit(List.of(next), next.getBoundingBox().getCenter(), next.position().subtract(from.position()).normalize(),
				from.position(), null, null, false), anchored);
			current = next;
		}
	}

	// ------------------------------------------------------------------ hits and links

	/** Applies a group's effects to a hit, then fires any On Hit / On Kill link watching it. */
	public static void onHit(Cast cast, SpellPlan.Group g, Cast.Hit hit, SpellPlan.Link anchored) {
		if (!cast.alive()) {
			return;
		}
		List<Entity> entities = hit.entities();
		int granted = cast.takeEntities(entities.size());
		if (granted < entities.size()) {
			entities = entities.subList(0, granted);
			hit = new Cast.Hit(entities, hit.point(), hit.dir(), hit.origin(), hit.block(), hit.face(), hit.self());
		}
		List<LivingEntity> aliveBefore = new ArrayList<>();
		for (Entity e : entities) {
			if (e instanceof LivingEntity living && living.isAlive() && e != cast.caster) {
				aliveBefore.add(living);
			}
		}
		if (!g.effects.isEmpty()) {
			dev.wildercord.api.WildercordEvents.SPELL_HIT.invoker().onHit(cast.caster, entities, hit.point(), g.effects.stream().map(e -> e.effect).toList());
		}
		// On Reaction and On Weakness count only what this group sets off.
		java.util.Map<Entity, Integer> watched = CraftedShapes.watch(anchored, entities);
		double groupPower = SpellNumbers.groupPower(g);
		for (SpellPlan.EffectNode effect : stasisFirst(g.effects)) {
			// Belated: it (and each lingering landing after it) comes a moment late, on whatever it struck that's still there.
			int late = SpellNumbers.belatedTicks(effect);
			if (late == 0) {
				Effects.apply(cast, effect, hit, groupPower);
			} else {
				Cast.Hit first = hit;
				CraftedRunes.belated(cast, hit, late);
				Scheduler.later(late, () -> {
					if (cast.alive()) {
						Effects.apply(cast, effect, still(cast, first), groupPower);
					}
				});
			}
			int again = SpellNumbers.lingerHits(effect);
			for (int i = 1; i <= again; i++) {
				Cast.Hit first = hit;
				// The same cast landing again, not a link: it takes none of the cast's link depth.
				Scheduler.later(late + 20 * i, () -> {
					if (!cast.alive()) {
						return;
					}
					Effects.apply(cast, effect, still(cast, first), groupPower * SpellNumbers.LINGER_POWER);
				});
			}
		}
		cast.siphon(aliveBefore.stream().filter(e -> Targets.canHarm(cast.caster, e) || !e.isAlive()).count());
		if (anchored == null) {
			return;
		}
		String id = anchored.link.id();
		if (id.equals(Runes.IMBUE.id())) {
			// The rest isn't cast: it's stored in the item in hand (Self) or the block this touched.
			Imbuing.imbue(cast, hit);
		} else if (id.equals(Runes.ON_HIT.id())) {
			if (!entities.isEmpty()) {
				int n = 0;
				for (Entity e : entities) {
					if (n++ >= MAX_TRIGGERS_PER_HIT) {
						break;
					}
					// The payload is paid once however many it fires at: each further one is a little weaker.
					runSegment(cast.child(Math.pow(SpellNumbers.TRIGGER_FALLOFF, n - 1)), anchored.next, new Cast.Trigger(e.getBoundingBox().getCenter(), hit.dir(), e, null, null));
				}
			} else if (hit.block() != null) {
				runSegment(cast.child(), anchored.next, new Cast.Trigger(hit.point(), hit.dir(), null, hit.block(), hit.face()));
			}
		} else if (id.equals(Runes.ON_KILL.id())) {
			for (LivingEntity victim : aliveBefore) {
				if (!victim.isAlive() || victim.isDeadOrDying()) {
					Fx.particle(cast.level, ParticleTypes.SOUL, victim.getBoundingBox().getCenter(), 10, 0.3, 0.04);
					Fx.particle(cast.level, ParticleTypes.SCULK_SOUL, victim.getBoundingBox().getCenter(), 6, 0.3, 0.04);
					runSegment(cast.child(), anchored.next, new Cast.Trigger(victim.getBoundingBox().getCenter(), hit.dir(), null, null, null));
				}
			}
		} else if (watched != null) {
			// On Reaction, On Weakness: at each creature this group set a reaction off on, or struck where it's weak.
			CraftedShapes.fire(cast, anchored, hit, watched, MAX_TRIGGERS_PER_HIT);
		}
	}

	/** What a hit struck that's still there to land on again: alive, and not gone to another dimension since (a player keeps being the same entity there). */
	private static Cast.Hit still(Cast cast, Cast.Hit first) {
		List<Entity> still = first.entities().stream().filter(e -> e.isAlive() && e.level() == cast.level).toList();
		return new Cast.Hit(still, first.point(), first.dir(), first.origin(), first.block(), first.face(), first.self());
	}

	/**
	 * Time stops before anything else lands: a group's Stasis is applied first, so every other
	 * effect in it (Sonic Boom, Explode, a Barrage's blows) is held too, whatever order it was threaded in.
	 */
	private static List<SpellPlan.EffectNode> stasisFirst(List<SpellPlan.EffectNode> effects) {
		boolean late = false;
		for (int i = 1; i < effects.size(); i++) {
			late |= effects.get(i).effect.is(Runes.STASIS.id());
		}
		if (!late) {
			return effects;
		}
		List<SpellPlan.EffectNode> ordered = new ArrayList<>(effects.size());
		effects.stream().filter(e -> e.effect.is(Runes.STASIS.id())).forEach(ordered::add);
		effects.stream().filter(e -> !e.effect.is(Runes.STASIS.id())).forEach(ordered::add);
		return ordered;
	}

	/** Runs {@code shot} once, or several times a quarter-second apart with Volley. */
	static void volley(Cast cast, SpellPlan.Group g, Runnable shot) {
		int shots = SpellNumbers.volleyShots(g);
		shot.run();
		for (int i = 1; i < shots; i++) {
			Scheduler.later(i * 5, () -> {
				if (cast.alive()) {
					shot.run();
				}
			});
		}
	}

	// ------------------------------------------------------------------ geometry

	static List<Entity> inRadius(Cast cast, Vec3 center, double radius) {
		return cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius),
			e -> e instanceof LivingEntity && e.isAlive() && e.getBoundingBox().getCenter().distanceTo(center) <= radius + e.getBbWidth() / 2);
	}

	/** Split for bolts and beams: fan the copies out horizontally, 12 degrees apart. */
	static List<Vec3> fan(Vec3 dir, int copies) {
		List<Vec3> dirs = new ArrayList<>(copies);
		for (int i = 0; i < copies; i++) {
			double angle = Math.toRadians((i - (copies - 1) / 2.0) * 12.0);
			dirs.add(dir.yRot((float) angle).normalize());
		}
		return dirs;
	}

	/** Split for areas: the first copy in the middle, the rest in a ring around it. */
	static List<Vec3> spread(Vec3 center, int copies, double radius) {
		List<Vec3> centers = new ArrayList<>(copies);
		centers.add(center);
		for (int i = 1; i < copies; i++) {
			double a = Math.PI * 2 * i / (copies - 1);
			centers.add(center.add(Math.cos(a) * radius * 1.4, 0, Math.sin(a) * radius * 1.4));
		}
		return centers;
	}

	/** Where Zone and Rain land: the block you look at (up to 24 blocks) or, after a link, the trigger. */
	static Vec3 aimPoint(Cast cast, Cast.Trigger at) {
		if (!at.fromCaster(cast.caster)) {
			return ground(cast.level, at.pos());
		}
		LivingEntity caster = cast.caster;
		Vec3 from = caster.getEyePosition();
		Vec3 to = from.add(caster.getLookAngle().scale(AIM_RANGE));
		BlockHitResult hit = cast.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		return ground(cast.level, hit.getType() == HitResult.Type.MISS ? to : hit.getLocation());
	}

	/** Drops a point onto the ground below it (up to 16 blocks). */
	static Vec3 ground(ServerLevel level, Vec3 pos) {
		if (!level.hasChunkAt(BlockPos.containing(pos))) {
			// Never load the world to find the ground (a Rain widened again and again reaches far past it).
			return pos;
		}
		BlockHitResult hit = level.clip(new ClipContext(pos.add(0, 0.5, 0), pos.add(0, -16, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS ? pos : hit.getLocation();
	}

	/** The group's colour: its first effect's element, or the shape colour. */
	static int colorOf(SpellPlan.Group g) {
		return g.effects.isEmpty() ? RuneColors.SHAPE : RuneColors.of(g.effects.getFirst().effect);
	}
}
