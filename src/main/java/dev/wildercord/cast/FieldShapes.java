package dev.wildercord.cast;

import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.FieldShapeGeometry;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The world side of the shapes pack (see {@link FieldShapeGeometry} for the pure side): field shapes strike
 * a pattern of blocks and whatever stands on them; kin shapes strike a kind of creature.
 *
 * <p>Each cast lands as ONE hit through {@link CastEngine#onHit}: every creature the shape picked out, at
 * its anchor (the first block of the field), so links, On Hit and On Kill fire once as for any area shape.
 * The rest of the field's blocks then take only the group's WORLD effects, each one through
 * {@link Effects#apply}, which spends the cast's block budget ({@link Cast#MAX_BLOCKS}) and checks who may
 * edit there; the creature budget ({@link Cast#MAX_ENTITIES}) is spent in {@code onHit}. Harmful effects
 * still only harm foes and helpful ones only help allies: {@link Effects} sorts the creatures by
 * {@link Targets}, and the kin shapes that are only for friends or only for foes choose by it as well.
 */
final class FieldShapes {
	private FieldShapes() {}

	/** How far a field may be aimed (and Bobber cast, and Tether reach a creature). */
	static final double AIM_RANGE = 24.0;

	static boolean handles(String shape) {
		return FieldShapeGeometry.handles(FieldShapeGeometry.path(shape));
	}

	static void deliver(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored, Vfx.Theme theme) {
		String path = FieldShapeGeometry.path(g.shape.id());
		FieldShapeGeometry.Spec spec = FieldShapeGeometry.spec(path);
		if (spec == null || !cast.alive()) {
			return;
		}
		if (spec.anchor() == FieldShapeGeometry.Anchor.SELF) {
			kin(cast, g, at, anchored, theme, path);
		} else {
			field(cast, g, at, anchored, theme, path, spec.anchor());
		}
	}

	// ------------------------------------------------------------------ field shapes

	private static void field(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored, Vfx.Theme theme, String path,
			FieldShapeGeometry.Anchor anchor) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		boolean fromCaster = at.fromCaster(caster);
		Vec3 look = fromCaster ? caster.getLookAngle() : at.dir();
		int[] facing = FieldShapeGeometry.cardinal(look.x, look.z);
		Direction face = Direction.UP;
		BlockPos base;
		switch (anchor) {
			case AIM_BLOCK -> {
				BlockHitResult hit = aimBlock(cast, at);
				if (hit != null) {
					base = hit.getBlockPos();
					face = hit.getDirection();
				} else {
					base = below(CastEngine.aimPoint(cast, at));
				}
			}
			case SELF_GROUND -> base = fromCaster ? below(CastEngine.ground(level, caster.position().add(0, 0.2, 0))) : groundBlock(cast, at);
			case WATER -> {
				base = water(cast, at);
				if (base == null) {
					FieldShapesVfx.fizzle(level, CastEngine.aimPoint(cast, at), theme);
					return;
				}
			}
			default -> base = groundBlock(cast, at);
		}
		List<BlockPos> cells = new ArrayList<>();
		int limit = FieldShapeGeometry.limit(path);
		for (FieldShapeGeometry.Cell c : FieldShapeGeometry.cells(path, facing[0], facing[1], SpellNumbers.shapeRadius(g))) {
			BlockPos p = base.offset(c.x(), c.y(), c.z());
			if (!level.hasChunkAt(p) || !keeps(level, path, p)) {
				continue;
			}
			cells.add(p);
			if (cells.size() >= limit) {
				break;
			}
		}
		Vec3 origin = fromCaster ? caster.position() : at.pos();
		if (cells.isEmpty()) {
			// Lodeseek with no ore near, a bank with no water: the shape shows it found nothing and strikes nothing.
			FieldShapesVfx.fizzle(level, Vec3.atCenterOf(base), theme);
			return;
		}
		List<Entity> standing = standing(cast, path, cells);
		BlockPos first = cells.getFirst();
		Vec3 point = Vec3.atCenterOf(first).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		Vec3 dir = new Vec3(facing[0], 0, facing[1]);
		FieldShapesVfx.field(level, cells, face, theme, path);
		CastEngine.onHit(cast, g, new Cast.Hit(standing, point, dir, origin, first, face, false), anchored);
		worldRest(cast, g, cells.subList(1, cells.size()), face, dir, origin);
	}

	/** The rest of the field: every WORLD effect of the group on each block after the first (onHit gave the first). */
	private static void worldRest(Cast cast, SpellPlan.Group g, List<BlockPos> rest, Direction face, Vec3 dir, Vec3 origin) {
		if (rest.isEmpty() || !cast.alive() || blocked(g)) {
			return;
		}
		double groupPower = SpellNumbers.groupPower(g);
		for (SpellPlan.EffectNode node : g.effects) {
			if (node.effect.kind() != EffectKind.WORLD) {
				continue;
			}
			Cast effectCast = cast.circleEffect(g, EffectKind.WORLD);
			for (BlockPos p : rest) {
				if (!cast.alive()) {
					return;
				}
				Vec3 point = Vec3.atCenterOf(p).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
				Effects.apply(effectCast, node, new Cast.Hit(List.of(), point, dir, origin, p, face, false), groupPower);
			}
		}
	}

	/** The same refusal {@link CastEngine#onHit} makes: lesson-pack and Excise effects are never spread across a field. */
	private static boolean blocked(SpellPlan.Group g) {
		return g.effects.stream().anyMatch(e -> e.effect.is(dev.wildercord.spell.ExciseRules.ID)
			|| dev.wildercord.spell.LessonPackRules.byRune(e.effect.id()) != null);
	}

	/** Which of a field's candidate blocks it keeps: Lodeseek only ores, Bobber only water, Shoreline water and its banks. */
	private static boolean keeps(ServerLevel level, String path, BlockPos p) {
		return switch (path) {
			case "lodeseek" -> ore(level.getBlockState(p));
			case "bobber" -> level.getFluidState(p).is(FluidTags.WATER);
			case "shoreline" -> level.getFluidState(p).is(FluidTags.WATER) && !level.getFluidState(p.above()).is(FluidTags.WATER)
				|| !level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && besideWater(level, p);
			default -> true;
		};
	}

	private static boolean ore(BlockState state) {
		return state.is(BlockTags.ORES) || state.is(ConventionalBlockTags.ORES);
	}

	private static boolean besideWater(ServerLevel level, BlockPos p) {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (level.getFluidState(p.relative(d)).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The creatures a field strikes: whatever stands in one of its block columns, on it or up to two blocks
	 * above it (Collapse: anything up to six blocks under its slab). Never the caster, except on Footing.
	 */
	private static List<Entity> standing(Cast cast, String path, List<BlockPos> cells) {
		Map<Long, int[]> columns = new HashMap<>();
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		boolean under = path.equals("collapse");
		for (BlockPos p : cells) {
			int lo = under ? p.getY() - 6 : p.getY();
			int hi = under ? p.getY() : p.getY() + 3;
			columns.merge(BlockPos.asLong(p.getX(), 0, p.getZ()), new int[] {lo, hi}, (a, b) -> new int[] {Math.min(a[0], b[0]), Math.max(a[1], b[1])});
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, lo);
			minZ = Math.min(minZ, p.getZ());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, hi);
			maxZ = Math.max(maxZ, p.getZ());
		}
		boolean self = path.equals("footing");
		AABB box = new AABB(minX - 1, minY, minZ - 1, maxX + 2, maxY + 1, maxZ + 2);
		List<Entity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
			if (e == cast.caster && !self) {
				continue;
			}
			int[] span = columns.get(BlockPos.asLong(e.getBlockX(), 0, e.getBlockZ()));
			if (span != null && e.getY() >= span[0] && e.getY() <= span[1] + 0.01) {
				out.add(e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(Vec3.atCenterOf(cells.getFirst()))));
		return out;
	}

	private static BlockPos below(Vec3 groundPoint) {
		return BlockPos.containing(groundPoint.x, groundPoint.y - 0.5, groundPoint.z);
	}

	/** The ground block a field lies on: where you look, or a link's block (or the ground under it). */
	private static BlockPos groundBlock(Cast cast, Cast.Trigger at) {
		if (!at.fromCaster(cast.caster) && at.block() != null) {
			return at.block();
		}
		return below(CastEngine.aimPoint(cast, at));
	}

	/** The block you look at within {@link #AIM_RANGE} (a link's own block after one), or null. */
	private static BlockHitResult aimBlock(Cast cast, Cast.Trigger at) {
		LivingEntity caster = cast.caster;
		if (!at.fromCaster(caster)) {
			return at.block() == null ? null : new BlockHitResult(at.pos(), at.face() == null ? Direction.UP : at.face(), at.block(), false);
		}
		Vec3 from = caster.getEyePosition();
		Vec3 to = from.add(caster.getLookAngle().scale(AIM_RANGE * Mastery.range(cast)));
		BlockHitResult hit = cast.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
		return hit.getType() == HitResult.Type.MISS ? null : hit;
	}

	/** Bobber: the first water along your look, within {@link #AIM_RANGE} (or at, or just under, a link's point). */
	private static BlockPos water(Cast cast, Cast.Trigger at) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		if (!at.fromCaster(caster)) {
			BlockPos p = BlockPos.containing(at.pos());
			for (int i = 0; i < 4; i++, p = p.below()) {
				if (level.hasChunkAt(p) && level.getFluidState(p).is(FluidTags.WATER)) {
					return p;
				}
			}
			return null;
		}
		Vec3 from = caster.getEyePosition();
		Vec3 to = from.add(caster.getLookAngle().scale(AIM_RANGE * Mastery.range(cast)));
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, caster));
		if (hit.getType() == HitResult.Type.MISS) {
			return null;
		}
		BlockPos p = hit.getBlockPos();
		return level.getFluidState(p).is(FluidTags.WATER) ? p : null;
	}

	// ------------------------------------------------------------------ kin shapes

	private static void kin(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored, Vfx.Theme theme, String path) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		boolean fromCaster = at.fromCaster(caster);
		Vec3 center = fromCaster ? caster.getBoundingBox().getCenter() : at.pos();
		double reach = FieldShapeGeometry.reach(path, SpellNumbers.shapeRadius(g));
		Vec3 dir = fromCaster ? caster.getLookAngle() : at.dir();
		List<Entity> chosen = switch (path) {
			case "herd" -> around(cast, center, reach, e -> e instanceof Animal && (!Targets.playerPet(e) || Targets.isAlly(caster, e)));
			case "fellowship" -> around(cast, center, reach, e -> Targets.canHelp(caster, e));
			case "saddle" -> riders(caster);
			case "packbond" -> around(cast, center, reach, e -> e != caster && e instanceof OwnableEntity own && own.getRootOwner() == caster);
			case "nursery" -> around(cast, center, reach, e -> e instanceof AgeableMob young && young.isBaby());
			case "shoal" -> around(cast, center, reach, e -> e != caster && e.isInWater());
			case "rearguard" -> {
				Vec3 flat = new Vec3(dir.x, 0, dir.z);
				Vec3 from = center;
				Vec3 back = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, -1) : flat.normalize();
				yield around(cast, center, reach, e -> {
					Vec3 to = e.position().subtract(from.x, e.getY(), from.z);
					return e != caster && to.lengthSqr() > 1.0E-4 && to.normalize().dot(back) < -0.2;
				});
			}
			case "grudge" -> cap(around(cast, center, reach, e -> e != caster && (e instanceof Mob mob && mob.getTarget() == caster
				|| e == caster.getLastHurtByMob() || caster instanceof Mob self && self.getTarget() == e)), FieldShapeGeometry.GRUDGE_MAX);
			case "sentinel" -> cap(around(cast, center, reach, e -> e instanceof Mob mob && mob.getTarget() != null
				&& Targets.canHelp(caster, mob.getTarget()) && Targets.canHarm(caster, e)), FieldShapeGeometry.SENTINEL_MAX);
			case "aureole" -> around(cast, center, reach, e -> true);
			case "tether" -> {
				Entity held = tethered(cast, at);
				if (held == null) {
					yield List.of();
				}
				center = held.getBoundingBox().getCenter();
				Vec3 c = center;
				yield around(cast, c, reach, e -> e != caster || e == held);
			}
			case "flock" -> around(cast, center, reach, e -> e != caster && flies(e));
			default -> List.of();
		};
		if (chosen.isEmpty()) {
			FieldShapesVfx.fizzle(level, center, theme);
			return;
		}
		Vec3 point = center;
		BlockPos ground = below(CastEngine.ground(level, center));
		FieldShapesVfx.kin(level, center, chosen, theme);
		CastEngine.onHit(cast, g, new Cast.Hit(chosen, point, dir, center, ground, Direction.UP, false), anchored);
		// World effects: the ground under each creature chosen (the first under the shape's own centre went with the hit).
		Set<BlockPos> feet = new LinkedHashSet<>();
		for (Entity e : chosen) {
			BlockPos p = e.getBlockPosBelowThatAffectsMyMovement();
			if (!p.equals(ground) && level.hasChunkAt(p)) {
				feet.add(p);
			}
			if (feet.size() >= FieldShapeGeometry.MAX_CELLS - 1) {
				break;
			}
		}
		worldRest(cast, g, List.copyOf(feet), Direction.UP, dir, center);
	}

	/** Every living creature within {@code reach} of {@code center} that {@code which} chooses, nearest first. */
	private static List<Entity> around(Cast cast, Vec3 center, double reach, Predicate<Entity> which) {
		List<Entity> out = new ArrayList<>(CastEngine.inRadius(cast, center, reach).stream()
			.filter(e -> !e.isSpectator() && which.test(e)).toList());
		out.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(center)));
		return out;
	}

	private static List<Entity> cap(List<Entity> list, int max) {
		return list.size() > max ? List.copyOf(list.subList(0, max)) : list;
	}

	/** Saddle: you, what you ride, and everything else riding it. */
	private static List<Entity> riders(LivingEntity caster) {
		Set<Entity> out = new LinkedHashSet<>();
		out.add(caster);
		Entity root = caster.getRootVehicle();
		if (root != caster) {
			out.add(root);
			root.getIndirectPassengers().forEach(out::add);
		}
		return out.stream().filter(e -> e instanceof LivingEntity && e.isAlive()).toList();
	}

	/** Tether: a link's creature, or the first creature along your look within {@link #AIM_RANGE}. */
	private static Entity tethered(Cast cast, Cast.Trigger at) {
		LivingEntity caster = cast.caster;
		if (!at.fromCaster(caster)) {
			return at.entity() instanceof LivingEntity living && living.isAlive() ? living : null;
		}
		Vec3 from = caster.getEyePosition();
		Vec3 to = from.add(caster.getLookAngle().scale(AIM_RANGE * Mastery.range(cast)));
		BlockHitResult wall = cast.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
		Entity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Entity e : cast.level.getEntities(caster, new AABB(from, end).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
			if (e.getBoundingBox().inflate(0.4).clip(from, end).isPresent()) {
				double d = e.distanceToSqr(from);
				if (d < bestDistance) {
					best = e;
					bestDistance = d;
				}
			}
		}
		return best;
	}

	/** Flock: what flies by nature: bats, phantoms, ghasts, blazes, vexes, and every creature that finds its way through the air. */
	private static boolean flies(Entity e) {
		return e instanceof Bat || e instanceof Phantom || e instanceof Ghast || e instanceof Blaze || e instanceof Vex
			|| e instanceof Mob mob && mob.getNavigation() instanceof FlyingPathNavigation;
	}
}
