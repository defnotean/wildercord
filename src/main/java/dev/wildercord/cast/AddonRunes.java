package dev.wildercord.cast;

import dev.wildercord.api.EffectBehaviour;
import dev.wildercord.api.EffectContext;
import dev.wildercord.api.ElementReaction;
import dev.wildercord.api.LinkBehaviour;
import dev.wildercord.api.LinkContext;
import dev.wildercord.api.ShapeBehaviour;
import dev.wildercord.api.ShapeContext;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The runtime side of the add-on API ({@code dev.wildercord.api}): the behaviours add-on runes
 * registered, the reactions and marks they use, and the contexts they're handed. The engine calls in
 * here only for runes it doesn't know itself, so built-in runes never pass through.
 */
public final class AddonRunes {
	private AddonRunes() {}

	private static final Map<String, EffectBehaviour> EFFECTS = new ConcurrentHashMap<>();
	private static final Map<String, ShapeBehaviour> SHAPES = new ConcurrentHashMap<>();
	private static final Map<String, LinkBehaviour> LINKS = new ConcurrentHashMap<>();
	private record Reaction(String id, String element, ElementReaction reaction) {}
	private static final List<Reaction> REACTIONS = new java.util.concurrent.CopyOnWriteArrayList<>();
	/** Add-on marks: creature, then mark key, then the game time it wears off. */
	private static final Map<UUID, Map<String, Long>> MARKS = new ConcurrentHashMap<>();

	public static void effect(String runeId, EffectBehaviour behaviour) {
		EFFECTS.put(runeId, behaviour);
	}

	public static void shape(String runeId, ShapeBehaviour behaviour) {
		SHAPES.put(runeId, behaviour);
	}

	public static void link(String runeId, LinkBehaviour behaviour) {
		LINKS.put(runeId, behaviour);
	}

	public static void reaction(String id, String element, ElementReaction reaction) {
		REACTIONS.removeIf(r -> r.id().equals(id));
		REACTIONS.add(new Reaction(id, element, reaction));
	}

	// ------------------------------------------------------------------ marks

	public static void mark(Entity target, String key, int ticks) {
		long until = target.level().getGameTime() + ticks;
		MARKS.computeIfAbsent(target.getUUID(), k -> new ConcurrentHashMap<>()).merge(key, until, Math::max);
	}

	public static boolean hasMark(Entity target, String key) {
		Map<String, Long> marks = MARKS.get(target.getUUID());
		Long until = marks == null ? null : marks.get(key);
		return until != null && until >= target.level().getGameTime();
	}

	public static void clearMark(Entity target, String key) {
		Map<String, Long> marks = MARKS.get(target.getUUID());
		if (marks != null) {
			marks.remove(key);
		}
	}

	/** Forgets marks that have worn off. */
	static void sweep(long now) {
		MARKS.values().forEach(marks -> marks.values().removeIf(until -> until < now));
		MARKS.values().removeIf(Map::isEmpty);
	}

	/** Keeps the marks tidy: worn-off ones go every 10 seconds, and all of them when the server stops. */
	public static void init() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 200 == 0 && !MARKS.isEmpty()) {
				sweep(server.overworld().getGameTime());
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> MARKS.clear());
	}

	// ------------------------------------------------------------------ hooks the engine calls

	/** Spell damage of {@code element} about to land: every add-on reaction of that element has its say. */
	static double react(Cast cast, LivingEntity target, String element) {
		if (REACTIONS.isEmpty() || element.isEmpty()) {
			return 1.0;
		}
		double multiplier = 1.0;
		for (Reaction r : REACTIONS) {
			if (r.element().equals(element)) {
				try {
					double m = r.reaction().react(cast.caster, target);
					multiplier *= Double.isFinite(m) && m >= 0 ? m : 1.0;
				} catch (RuntimeException e) {
					failed("reaction of " + element, e);
				}
				if (!cast.consequencesValid(target)) return 1.0;
			}
		}
		return multiplier;
	}

	/** An add-on threw: log it once for that behaviour, and carry on as if it did nothing. */
	private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private static void failed(String what, RuntimeException e) {
		if (FAILED.add(what)) {
			dev.wildercord.Wildercord.LOGGER.error("A Wildercord add-on's {} threw; it's being skipped", what, e);
		}
	}

	/** An effect the engine doesn't know: its add-on behaviour, if any (otherwise it does nothing). */
	static void effect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> harmed, List<LivingEntity> helped, double power, double duration) {
		EffectBehaviour behaviour = EFFECTS.get(node.effect.id());
		if (behaviour != null) {
			try {
				behaviour.apply(new Effect(cast, node, hit, harmed, helped, power, duration));
			} catch (RuntimeException e) {
				failed("effect " + node.effect.id(), e);
			}
		}
	}

	/** A shape the engine doesn't know: true if an add-on delivered it. */
	static boolean shape(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored) {
		ShapeBehaviour behaviour = SHAPES.get(g.shape.id());
		if (behaviour == null) {
			return false;
		}
		try {
			behaviour.deliver(new Shape(cast, g, at, anchored));
		} catch (RuntimeException e) {
			failed("shape " + g.shape.id(), e);
		}
		return true;
	}

	/** A link the engine doesn't know: true if an add-on took it. */
	static boolean link(Cast cast, SpellPlan.Link link, Cast.Trigger at) {
		LinkBehaviour behaviour = LINKS.get(link.link.id());
		if (behaviour == null) {
			return false;
		}
		try {
			behaviour.link(new Link(cast.child(), link, at));
		} catch (RuntimeException e) {
			failed("link " + link.link.id(), e);
		}
		return true;
	}

	// ------------------------------------------------------------------ contexts

	private record Effect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> harmed, List<LivingEntity> helped, double power,
			double duration) implements EffectContext {
		Effect {
			harmed = List.copyOf(harmed);
			helped = List.copyOf(helped);
		}

		@Override
		public LivingEntity caster() {
			return cast.caster;
		}

		@Override
		public ServerLevel level() {
			return cast.level;
		}

		@Override
		public RuneDef rune() {
			return node.effect;
		}

		@Override
		public Vec3 point() {
			return hit.point();
		}

		@Override
		public Vec3 direction() {
			return hit.dir();
		}

		@Override
		public BlockPos block() {
			return hit.block();
		}

		@Override
		public Direction face() {
			return hit.face();
		}

		@Override
		public boolean self() {
			return hit.self();
		}

		@Override
		public int count(RuneDef modifier) {
			return node.count(modifier);
		}

		@Override
		public void hurt(LivingEntity target, DamageSource source, double amount) {
			Effects.hurt(cast, target, source, amount);
		}

		@Override
		public DamageSource magic() {
			return cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
		}

		@Override
		public boolean mayEdit(BlockPos pos) {
			return Casters.mayBuild(cast.caster) && !Techniques.isRampart(cast.level, pos) && Casters.mayEdit(cast.caster, cast.level, pos)
				&& cast.takeBlock();
		}

		@Override
		public boolean alive() {
			return cast.alive();
		}

		@Override
		public void later(int ticks, Runnable task) {
			Scheduler.later(ticks, Effects.carryContext(task));
		}
	}

	private record Shape(Cast cast, SpellPlan.Group g, Cast.Trigger at, SpellPlan.Link anchored) implements ShapeContext {
		@Override
		public LivingEntity caster() {
			return cast.caster;
		}

		@Override
		public ServerLevel level() {
			return cast.level;
		}

		@Override
		public RuneDef shape() {
			return g.shape;
		}

		@Override
		public Vec3 origin() {
			return at.fromCaster(cast.caster) ? cast.caster.getEyePosition() : at.pos();
		}

		@Override
		public Vec3 direction() {
			return at.dir();
		}

		@Override
		public boolean fromCaster() {
			return at.fromCaster(cast.caster);
		}

		@Override
		public Vec3 aimPoint() {
			return CastEngine.aimPoint(cast, at);
		}

		@Override
		public int copies() {
			return SpellNumbers.copies(g);
		}

		@Override
		public int count(RuneDef modifier) {
			return g.count(modifier);
		}

		@Override
		public double radius() {
			return SpellNumbers.shapeRadius(g);
		}

		@Override
		public int color() {
			return CastEngine.colorOf(g);
		}

		@Override
		public List<LivingEntity> near(Vec3 centre, double radius) {
			List<LivingEntity> near = new ArrayList<>();
			for (Entity e : CastEngine.inRadius(cast, centre, radius)) {
				near.add((LivingEntity) e);
			}
			return near;
		}

		@Override
		public void hit(List<? extends Entity> entities, Vec3 point, BlockPos block, Direction face) {
			if (!cast.alive()) {
				return;
			}
			Vec3 from = origin();
			Vec3 dir = point.subtract(from).lengthSqr() > 1.0E-6 ? point.subtract(from).normalize() : at.dir();
			CastEngine.onHit(cast, g, new Cast.Hit(List.copyOf(entities), point, dir, from, block, face, false), anchored);
		}

		@Override
		public ShapeContext pulse() {
			return new Shape(cast.pulse(), g, at, anchored);
		}

		@Override
		public boolean alive() {
			return cast.alive();
		}

		@Override
		public void later(int ticks, Runnable task) {
			Scheduler.later(ticks, task);
		}
	}

	private record Link(Cast cast, SpellPlan.Link node, Cast.Trigger at) implements LinkContext {
		@Override
		public LivingEntity caster() {
			return cast.caster;
		}

		@Override
		public ServerLevel level() {
			return cast.level;
		}

		@Override
		public RuneDef link() {
			return node.link;
		}

		@Override
		public int count(RuneDef modifier) {
			return node.count(modifier);
		}

		@Override
		public Vec3 position() {
			return at.pos();
		}

		@Override
		public Vec3 direction() {
			return at.dir();
		}

		@Override
		public void fire() {
			CastEngine.runSegment(cast, node.next, at);
		}

		@Override
		public void fireAt(Vec3 position, Vec3 direction, Entity entity) {
			CastEngine.runSegment(cast, node.next, new Cast.Trigger(position, direction, entity, null, null));
		}

		@Override
		public boolean alive() {
			return cast.alive();
		}

		@Override
		public void later(int ticks, Runnable task) {
			Scheduler.later(ticks, task);
		}
	}
}
