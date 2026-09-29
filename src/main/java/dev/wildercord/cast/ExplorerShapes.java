package dev.wildercord.cast;

import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The shapes and condition links of the runes of the world: Vortex (ominous vaults), Snare (jungle
 * temples) and Constellation (the Astral Observatory); If Wounded, If Outnumbered and If Wet.
 * {@link CastEngine} hands them here; their numbers are in {@link SpellNumbers}.
 */
final class ExplorerShapes {
	private ExplorerShapes() {}

	static void deliver(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored, Vfx.Theme theme) {
		String shape = g.shape.id();
		if (shape.equals(Runes.VORTEX.id())) {
			vortex(cast, g, anchored, CastEngine.aimPoint(cast, at), at.fromCaster(cast.caster) ? cast.caster.getLookAngle() : at.dir(), theme);
		} else if (shape.equals(Runes.SNARE.id())) {
			snare(cast, g, anchored, at, theme);
		} else if (shape.equals(Runes.CONSTELLATION.id())) {
			constellation(cast, g, anchored, at, theme);
		}
	}

	// ------------------------------------------------------------------ Vortex

	/** A whirling vortex: drags creatures toward its eye and strikes everything in the eye twice a second. */
	private static void vortex(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 point, Vec3 dir, Vfx.Theme theme) {
		double radius = SpellNumbers.vortexRadius(g);
		double eye = SpellNumbers.vortexEye(g);
		int ticks = SpellNumbers.vortexSeconds(g) * 20;
		Vec3 centre = point;
		ExplorerVfx.vortexOpen(cast.level, centre, radius, theme, ticks);
		ShapeRunners.steps(cast, 1, 2, ticks - 1, tick -> {
			boolean strike = tick % SpellNumbers.VORTEX_INTERVAL == 0 && tick > 0;
			Cast child = strike ? cast.pulse() : cast;
			if (!child.alive()) {
				return;
			}
			ExplorerVfx.vortex(child.level, centre, radius, eye, theme, tick);
			Vec3 heart = centre.add(0, 1.0, 0);
			// Drag: only what the caster may harm, and never a boss.
			for (Entity e : child.level.getEntities((Entity) null, new AABB(heart, heart).inflate(radius), e -> Targets.canHarm(child.caster, e))) {
				if (Spirits.isBoss(e)) {
					continue;
				}
				Vec3 in = heart.subtract(e.getBoundingBox().getCenter());
				double d = in.length();
				if (d <= radius && d > eye * 0.5) {
					Vec3 swirl = new Vec3(-in.z, 0, in.x).normalize().scale(0.12);
					Effects.push((LivingEntity) e, in.normalize().scale(Math.min(0.35, 0.08 + d * 0.04)).add(swirl).add(0, 0.04, 0));
					Reactions.mark(e, Reactions.Mark.PULLED);
				}
			}
			if (strike) {
				CastEngine.onHit(child, g, new Cast.Hit(CastEngine.inRadius(child, heart, eye), heart, dir, centre, null, null, false), anchored);
			}
		});
	}

	// ------------------------------------------------------------------ Snare

	/** A tripwire from the caster's feet to the point; the first enemy across it springs it. */
	private static void snare(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		Vec3 a = (fromCaster ? caster.position() : CastEngine.ground(cast.level, at.pos())).add(0, 0.35, 0);
		Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
		Vec3 flat = Effects.horizontal(aim, aim);
		Vec3 end = a.add(flat.scale(SpellNumbers.SNARE_LENGTH));
		BlockHitResult wall = cast.level.clip(new ClipContext(a, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 b = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation().subtract(flat.scale(0.2));
		double radius = SpellNumbers.snareRadius(g);
		ExplorerVfx.snareSet(cast.level, a, b, theme);
		int ticks = SpellNumbers.SNARE_SECONDS * 20;
		boolean[] sprung = {false};
		ShapeRunners.steps(cast, 4, 4, ticks - 5, t -> {
			int tick = t + 4;
			if (sprung[0] || !cast.alive()) {
				return;
			}
			if (tick % 40 == 0) {
				ExplorerVfx.snareIdle(cast.level, a, b, theme);
			}
			LivingEntity crossing = crossing(cast, a, b);
			if (crossing == null) {
				return;
			}
			sprung[0] = true;
			Vec3 spot = crossing.getBoundingBox().getCenter();
			ExplorerVfx.snareSpring(cast.level, a, b, spot, radius, theme);
			CastEngine.onHit(cast.pulse(), g, new Cast.Hit(CastEngine.inRadius(cast, spot, radius), spot, flat, a, null, null, false), anchored);
		});
	}

	/** The first enemy whose feet are within half a block of the wire, or null. */
	private static LivingEntity crossing(Cast cast, Vec3 a, Vec3 b) {
		AABB box = new AABB(a, b).inflate(0.8, 1.2, 0.8);
		Vec3 ab = b.subtract(a);
		double length2 = Math.max(1.0E-6, ab.lengthSqr());
		for (Entity e : cast.level.getEntities((Entity) null, box, e -> Targets.canHarm(cast.caster, e))) {
			Vec3 p = e.position().add(0, 0.35, 0);
			double s = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / length2));
			if (p.distanceTo(a.add(ab.scale(s))) <= 0.5 + e.getBbWidth() / 2) {
				return (LivingEntity) e;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ Constellation

	/** Up to five enemies near the caster (that it can see: never through a wall), joined in a constellation and struck together. */
	private static void constellation(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		Vec3 centre = fromCaster ? caster.getEyePosition() : at.pos();
		double range = SpellNumbers.constellationRange(g);
		List<LivingEntity> stars = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(centre, centre).inflate(range), e -> Targets.canHarm(caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(centre) <= range && (fromCaster ? caster.hasLineOfSight(e) : inSight(cast, centre, e))) {
				stars.add((LivingEntity) e);
			}
		}
		stars.sort(Comparator.comparingDouble(e -> e.distanceToSqr(centre)));
		if (stars.size() > SpellNumbers.CONSTELLATION_STARS) {
			stars = new ArrayList<>(stars.subList(0, SpellNumbers.CONSTELLATION_STARS));
		}
		if (stars.isEmpty()) {
			Casters.tell(caster, net.minecraft.network.chat.Component.translatable("message.wildercord.no_targets"));
			return;
		}
		List<Vec3> points = new ArrayList<>();
		stars.forEach(s -> points.add(s.getBoundingBox().getCenter()));
		ExplorerVfx.constellation(cast.level, points, theme);
		List<Entity> hit = new ArrayList<>(stars);
		CastEngine.onHit(cast, g, new Cast.Hit(hit, points.getFirst(), at.dir(), centre, null, null, false), anchored);
	}

	/** Whether nothing solid stands between {@code from} and {@code e}'s eyes or middle. */
	private static boolean inSight(Cast cast, Vec3 from, Entity e) {
		for (Vec3 to : new Vec3[] {e.getEyePosition(), e.getBoundingBox().getCenter()}) {
			if (cast.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ conditions

	static boolean isCondition(String id) {
		return id.equals(Runes.IF_WOUNDED.id()) || id.equals(Runes.IF_OUTNUMBERED.id()) || id.equals(Runes.IF_WET.id());
	}

	static boolean conditionMet(Cast cast, String id) {
		LivingEntity caster = cast.caster;
		if (id.equals(Runes.IF_WOUNDED.id())) {
			return caster.getHealth() < caster.getMaxHealth() * 0.5F;
		}
		if (id.equals(Runes.IF_WET.id())) {
			return caster.isInWaterOrRain();
		}
		if (id.equals(Runes.IF_OUTNUMBERED.id())) {
			Vec3 at = caster.position();
			return cast.level.getEntities(caster, new AABB(at, at).inflate(8.0),
				e -> Targets.canHarm(caster, e) && e.distanceTo(caster) <= 8.0 && foe(caster, e)).size() >= 3;
		}
		return false;
	}

	/**
	 * Who counts toward If Outnumbered: a monster, another player (one the caster may harm), whatever
	 * the caster is fighting, or anything hunting the caster. A field of cows or villagers doesn't.
	 */
	private static boolean foe(LivingEntity caster, Entity e) {
		return e instanceof net.minecraft.world.entity.monster.Enemy
			|| e instanceof net.minecraft.world.entity.player.Player
			|| caster instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == e
			|| caster.getLastHurtMob() == e
			|| e instanceof net.minecraft.world.entity.Mob hunter && hunter.getTarget() == caster;
	}
}
