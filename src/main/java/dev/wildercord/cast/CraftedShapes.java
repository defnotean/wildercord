package dev.wildercord.cast;

import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The shapes and links of the second batch of new runes: Glaive (out and back, striking on both passes),
 * Imprint (erupts where you stood, a moment later) and Latch (a thread that holds on to one creature and
 * strikes it again and again); On Reaction and On Weakness, which watch the group before them as On Hit
 * does and fire the rest only at the creatures it set a reaction off on or struck where they're weak.
 * {@link CastEngine} hands them here; their numbers are in {@link SpellNumbers}.
 */
final class CraftedShapes {
	private CraftedShapes() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	static boolean handles(String shape) {
		return shape.equals(Runes.GLAIVE.id()) || shape.equals(Runes.IMPRINT.id()) || shape.equals(Runes.LATCH.id());
	}

	static void deliver(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		String shape = g.shape.id();
		int copies = SpellNumbers.copies(g);
		if (shape.equals(Runes.GLAIVE.id())) {
			Vec3 origin = fromCaster ? caster.getEyePosition().add(caster.getLookAngle().scale(0.8)).subtract(0, 0.3, 0) : at.pos();
			Vec3 aim = fromCaster ? caster.getLookAngle() : at.dir();
			// Thrown by you it comes back to you, wherever you've got to; set off by a link, back to where it was.
			Vec3 home = fromCaster ? null : at.pos();
			for (Vec3 dir : CastEngine.fan(aim, copies)) {
				glaive(cast, g, anchored, origin, dir, home, theme);
			}
		} else if (shape.equals(Runes.IMPRINT.id())) {
			Vec3 feet = CastEngine.ground(cast.level, fromCaster ? caster.position().add(0, 0.2, 0) : at.pos());
			double radius = SpellNumbers.imprintRadius(g);
			for (Vec3 spot : CastEngine.spread(feet, copies, radius)) {
				imprint(cast, g, anchored, spot, theme);
			}
		} else if (shape.equals(Runes.LATCH.id())) {
			latch(cast, g, anchored, at, theme);
		}
	}

	// ------------------------------------------------------------------ Glaive

	/**
	 * A glaive flies out along {@code dir} to {@link SpellNumbers#GLAIVE_RANGE} (or a wall), then comes back
	 * to {@code home} (the caster when null), striking each creature once on the way out and once more on
	 * the way back: a pass of its own, with its own creature budget. A wall on the way back ends it.
	 */
	private static void glaive(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 origin, Vec3 dir, Vec3 home, Vfx.Theme theme) {
		double speed = SpellNumbers.glaiveSpeed(g);
		double width = SpellNumbers.glaiveWidth(g);
		Vec3 aim = dir.normalize();
		Vec3[] pos = {origin};
		double[] travelled = {0};
		boolean[] back = {false};
		Cast[] returning = {cast};
		Set<UUID> out = new HashSet<>();
		Set<UUID> in = new HashSet<>();
		int ticks = (int) Math.ceil(SpellNumbers.GLAIVE_RANGE / speed) + SpellNumbers.GLAIVE_RETURN_TICKS;
		CraftedVfx.glaiveLaunch(cast.level, origin, theme);
		ShapeRunners.each(cast, ticks, tick -> {
			Vec3 from = pos[0];
			Vec3 to;
			boolean turn = false;
			boolean end = false;
			if (!back[0]) {
				to = from.add(aim.scale(Math.min(speed, SpellNumbers.GLAIVE_RANGE - travelled[0])));
				BlockHitResult block = ShapeRunners.clip(cast, from, to);
				if (block.getType() != HitResult.Type.MISS) {
					// It rings off the wall (a world effect lands there) and turns back a little short of it.
					to = block.getLocation().subtract(aim.scale(0.3));
					CastEngine.onHit(cast, g, new Cast.Hit(List.of(), block.getLocation(), aim, block.getLocation(), block.getBlockPos(), block.getDirection(),
						false), anchored);
					turn = true;
				}
				travelled[0] += from.distanceTo(to);
				turn |= travelled[0] >= SpellNumbers.GLAIVE_RANGE - 1.0E-6;
			} else {
				Vec3 target = home != null ? home : cast.caster.getBoundingBox().getCenter();
				Vec3 toward = target.subtract(from);
				if (toward.length() <= speed + 0.5) {
					to = target;
					end = true;
				} else {
					to = from.add(toward.normalize().scale(speed));
				}
				if (!cast.level.hasChunkAt(BlockPos.containing(to))) {
					// Never on into ground that isn't loaded (a caster far away by now).
					CraftedVfx.glaiveEnd(cast.level, from, theme);
					return false;
				}
				BlockHitResult block = ShapeRunners.clip(cast, from, to);
				if (block.getType() != HitResult.Type.MISS) {
					to = block.getLocation();
					end = true;
				}
			}
			Set<UUID> struck = back[0] ? in : out;
			List<Entity> hits = ShapeRunners.along(cast, from, to, width, struck);
			if (!hits.isEmpty()) {
				hits.forEach(e -> struck.add(e.getUUID()));
				Vec3 way = to.subtract(from).lengthSqr() < 1.0E-8 ? aim : to.subtract(from).normalize();
				CastEngine.onHit(returning[0], g, new Cast.Hit(hits, hits.getFirst().getBoundingBox().getCenter(), way, from, null, null, false), anchored);
			}
			if (to.distanceToSqr(cast.caster.getEyePosition()) > 2.25) {
				CraftedVfx.glaiveTick(cast.level, to, width, theme, tick);
			}
			pos[0] = to;
			if (end) {
				CraftedVfx.glaiveEnd(cast.level, to, theme);
				return false;
			}
			if (turn) {
				back[0] = true;
				// The way back is a second strike: a fresh creature budget, as a Zone's pulses have.
				returning[0] = cast.pulse();
				CraftedVfx.glaiveTurn(cast.level, to, theme);
			}
			return true;
		});
	}

	// ------------------------------------------------------------------ Imprint

	/** An imprint on the ground at {@code feet} that waits, then erupts on everything within its radius. */
	private static void imprint(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Vec3 feet, Vfx.Theme theme) {
		double radius = SpellNumbers.imprintRadius(g);
		int delay = SpellNumbers.imprintDelay(g);
		CraftedVfx.imprintSet(cast.level, feet, radius, theme, delay);
		if (delay > 10) {
			ShapeRunners.steps(cast, 10, 10, delay - 11, t -> {
				if (cast.alive()) {
					CraftedVfx.imprintTick(cast.level, feet, radius, theme, delay - 10 - t);
				}
			});
		}
		Scheduler.later(delay, () -> {
			if (!cast.alive()) {
				return;
			}
			Vec3 heart = feet.add(0, 1.0, 0);
			CraftedVfx.imprintErupt(cast.level, feet, radius, theme);
			BlockPos below = BlockPos.containing(feet.x, feet.y - 0.5, feet.z);
			CastEngine.onHit(cast, g, new Cast.Hit(CastEngine.inRadius(cast, heart, radius), heart, UP, feet, below, Direction.UP, false), anchored);
		});
	}

	// ------------------------------------------------------------------ Latch

	/**
	 * A thread from the caster's hand to the first creature along the aim; after a link, from where the link
	 * was set off to the creature that set it off (or the first one along from there), so it tethers that
	 * creature to the spot it was struck. It strikes again and again, each strike with its own budget,
	 * until its strikes are spent or the thread snaps (the creature dies, leaves, strays past
	 * {@link SpellNumbers#LATCH_HOLD} or out of sight). With nothing to hold, it lands on the block it
	 * reached, as a beam would.
	 */
	private static void latch(Cast cast, SpellPlan.Group g, SpellPlan.Link anchored, Cast.Trigger at, Vfx.Theme theme) {
		LivingEntity caster = cast.caster;
		boolean fromCaster = at.fromCaster(caster);
		Vec3 start = fromCaster ? caster.getEyePosition() : at.pos();
		Vec3 aim = (fromCaster ? caster.getLookAngle() : at.dir()).normalize();
		LivingEntity target = null;
		if (!fromCaster && at.entity() instanceof LivingEntity struck && struck.isAlive() && struck != caster) {
			target = struck;
		}
		BlockHitResult block = ShapeRunners.clip(cast, start, start.add(aim.scale(SpellNumbers.LATCH_RANGE)));
		Vec3 end = block.getType() == HitResult.Type.MISS ? start.add(aim.scale(SpellNumbers.LATCH_RANGE)) : block.getLocation();
		if (target == null) {
			for (Entity e : ShapeRunners.along(cast, start, end, 0.4, Set.of())) {
				if (e instanceof LivingEntity living) {
					target = living;
					break;
				}
			}
		}
		if (target == null) {
			CraftedVfx.latchSnap(cast.level, end, theme);
			if (block.getType() != HitResult.Type.MISS) {
				CastEngine.onHit(cast, g, new Cast.Hit(List.of(), end, aim, start, block.getBlockPos(), block.getDirection(), false), anchored);
			}
			return;
		}
		LivingEntity held = target;
		int strikes = SpellNumbers.latchStrikes(g);
		int interval = SpellNumbers.latchInterval(g);
		int[] struck = {0};
		CraftedVfx.latchOn(cast.level, start, held, theme);
		ShapeRunners.each(cast, (strikes - 1) * interval + 1, tick -> {
			Vec3 from = fromCaster ? caster.getEyePosition().subtract(0, 0.3, 0) : start;
			Vec3 c = held.getBoundingBox().getCenter();
			if (!held.isAlive() || held.isRemoved() || held.level() != cast.level || c.distanceTo(from) > SpellNumbers.LATCH_HOLD
					|| !(fromCaster ? caster.hasLineOfSight(held) : inSight(cast, from, held))) {
				CraftedVfx.latchSnap(cast.level, from.lerp(c, 0.5), theme);
				return false;
			}
			if (tick % 2 == 0) {
				CraftedVfx.latchThread(cast.level, from.add(c.subtract(from).normalize().scale(0.8)), c, theme, tick);
			}
			if (tick % interval == 0) {
				struck[0]++;
				CraftedVfx.latchStrike(cast.level, from.add(c.subtract(from).normalize().scale(0.8)), c, theme);
				CastEngine.onHit(cast.pulse(), g, new Cast.Hit(List.of(held), c, c.subtract(from).normalize(), from, null, null, false), anchored);
			}
			return struck[0] < strikes;
		});
	}

	/** Whether nothing solid stands between {@code from} and {@code e}'s middle. */
	private static boolean inSight(Cast cast, Vec3 from, Entity e) {
		return cast.level.clip(new ClipContext(from, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType()
			== HitResult.Type.MISS;
	}

	// ------------------------------------------------------------------ On Reaction, On Weakness

	/**
	 * Before a group watched by On Reaction or On Weakness lands: how many reactions (or weak strikes) each
	 * creature it hit has had so far, so only what this group sets off counts. Null for any other link.
	 */
	static Map<Entity, Integer> watch(SpellPlan.Link anchored, List<Entity> entities) {
		if (anchored == null) {
			return null;
		}
		boolean reaction = anchored.link.is(Runes.ON_REACTION.id());
		if (!reaction && !anchored.link.is(Runes.ON_WEAKNESS.id())) {
			return null;
		}
		Map<Entity, Integer> before = new LinkedHashMap<>();
		for (Entity e : entities) {
			before.put(e, reaction ? Reactions.count(e) : Affinities.weakStrikes(e));
		}
		return before;
	}

	/** After the group landed: fires the rest at each creature it set a reaction off on (or struck where it's weak), at most {@code most}. */
	static void fire(Cast cast, SpellPlan.Link anchored, Cast.Hit hit, Map<Entity, Integer> before, int most) {
		boolean reaction = anchored.link.is(Runes.ON_REACTION.id());
		List<Entity> sprung = new ArrayList<>();
		for (Map.Entry<Entity, Integer> entry : before.entrySet()) {
			Entity e = entry.getKey();
			int now = reaction ? Reactions.count(e) : Affinities.weakStrikes(e);
			if (now > entry.getValue() && sprung.size() < most) {
				sprung.add(e);
			}
		}
		for (Entity e : sprung) {
			CraftedVfx.linkSprung(cast.level, e, reaction);
			// A creature that died of it is gone: the rest fires where it fell, as after On Kill.
			Entity at = e.isAlive() ? e : null;
			CastEngine.runSegment(cast.child(), anchored.next, new Cast.Trigger(e.getBoundingBox().getCenter(), hit.dir(), at, null, null));
		}
	}
}
