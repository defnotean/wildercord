package dev.wildercord.aura;

import dev.wildercord.aura.world.AuraWorldRules;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.WildercordSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Crescents of aura in flight: every Aura Slash, whoever loosed it (a player, a duelist, a fallen knight), flies here, a step
 * a tick, the Crescent shape's flight in the aura's colour. They're kept together so that two meeting in the air clash (both
 * break in a burst, harming nobody), a held guard facing one catches it (it cuts the guard and goes no further), and a perfect
 * guard sends one back at whoever loosed it.
 *
 * <p>Each tick every crescent moves first, then any two of different owners that met on the way clash, then the rest cut what
 * they reached: so the order crescents were loosed in never decides a clash.</p>
 */
public final class Crescents {
	private Crescents() {}

	/** What a crescent does to a creature it reaches: deals its harm (and answers it), returning what it took. */
	@FunctionalInterface
	public interface Cut {
		float cut(Flight flight, LivingEntity target);
	}

	/**
	 * Told once when a crescent's flight is over, however it ended (a wall, a clash, a guard, sent back, or its full flight):
	 * where it ended, and the block it struck if it was a wall (null otherwise).
	 */
	@FunctionalInterface
	public interface End {
		void ended(Flight flight, Vec3 at, BlockPos blocked);
	}

	/** A creature besides a player that can guard against a crescent (a duelist, a knight): whether its guard catches this one. */
	public interface Guarding {
		boolean catches(Flight flight);
	}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** One crescent in flight. */
	public static final class Flight {
		final LivingEntity caster;
		final ServerLevel level;
		final Vec3 origin;
		final Vec3 aim;
		final Vec3 side;
		final int color;
		final double damage;
		final double bonus;
		final double speed;
		final double width;
		final int steps;
		final int targets;
		final boolean weak;
		final Predicate<Entity> mayCut;
		final Cut cut;
		final long launched;
		final Set<UUID> hit = new HashSet<>();
		int step;
		Vec3 prev;
		Vec3 front;
		boolean done;
		/** Told each step it flies (a spell riding it draws its colour on the edge), and once when it's over. */
		java.util.function.Consumer<Flight> onStep;
		End onEnd;
		boolean ended;
		/** Where it ended against a wall, and the block it struck; null while it flies or for any other ending. */
		Vec3 endAt;
		BlockPos blockedAt;
		/** Whether it pierces (the Way of the Blade): a held guard doesn't stop it, and it cuts through a crescent it meets. */
		boolean pierce;
		/** What's left of its harm (a piercing crescent that won a clash flies on weaker). */
		double scale = 1.0;

		Flight(LivingEntity caster, Vec3 origin, Vec3 aim, int color, double damage, double bonus, double speed, double range, double width, int targets,
				boolean weak, Predicate<Entity> mayCut, Cut cut) {
			this.caster = caster;
			this.level = (ServerLevel) caster.level();
			this.origin = origin;
			this.aim = aim;
			Vec3 s = aim.cross(UP);
			this.side = s.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : s.normalize();
			this.color = color;
			this.damage = damage;
			this.bonus = bonus;
			this.speed = speed;
			this.width = width;
			this.steps = (int) Math.ceil(range / speed);
			this.targets = targets;
			this.weak = weak;
			this.mayCut = mayCut;
			this.cut = cut;
			this.launched = level.getServer().getTickCount();
			this.front = origin.add(aim.scale(0.8));
			this.prev = front;
		}

		/** Who loosed it (for a crescent sent back, the guard who sent it). */
		public LivingEntity caster() {
			return caster;
		}

		/** Its harm before the defences of whatever it cuts. */
		public double damage() {
			return damage * scale;
		}

		/** Makes it pierce (the Way of the Blade's slash): a held guard doesn't stop it, and it cuts through a crescent it meets. */
		public Flight pierce() {
			this.pierce = true;
			return this;
		}

		public boolean pierces() {
			return pierce;
		}

		/** What multiplies its harm on top of the element, held to the spell-defence cap against a player (a forged glaive's). */
		public double bonus() {
			return bonus;
		}

		public int color() {
			return color;
		}

		/** Where it is now. */
		public Vec3 front() {
			return front;
		}

		/** Which way it flies. */
		public Vec3 aim() {
			return aim;
		}

		public double width() {
			return width;
		}

		public double speed() {
			return speed;
		}

		/** How far it flies in all. */
		public double range() {
			return steps * speed;
		}

		public int targets() {
			return targets;
		}

		public boolean done() {
			return done;
		}

		/** Stops it where it is (a guard caught it, or it was sent back). */
		public void stop() {
			done = true;
		}

		/** Something to do each step it flies. */
		public Flight onStep(java.util.function.Consumer<Flight> step) {
			this.onStep = step;
			return this;
		}

		/** Something to do once its flight is over, however it ends. */
		public Flight onEnd(End end) {
			this.onEnd = end;
			return this;
		}
	}

	private static final List<Flight> FLIGHTS = new ArrayList<>();
	/** The crescent cutting a creature right now (on the server thread), so a guard it meets can answer it. */
	private static Flight cutting;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Crescents::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			FLIGHTS.clear();
			cutting = null;
		});
	}

	/**
	 * Looses a crescent from {@code origin} along {@code aim} (already levelled as it should fly): its first step is next tick.
	 *
	 * @param bonus   a multiplier on its harm counted with its element (1 for none)
	 * @param targets the most creatures it cuts
	 * @param mayCut  what it may harm (it passes through anything else, still counting it as met)
	 */
	public static Flight launch(LivingEntity caster, Vec3 origin, Vec3 aim, int color, double damage, double bonus, double speed, double range, double width,
			int targets, boolean weak, Predicate<Entity> mayCut, Cut cut) {
		Flight flight = new Flight(caster, origin, aim.normalize(), color, damage, bonus, speed, range, width, targets, weak, mayCut, cut);
		FLIGHTS.add(flight);
		return flight;
	}

	/** The crescent cutting a creature right now, or null: asked by a guard it meets. */
	public static Flight cutting() {
		return cutting;
	}

	/** Every crescent in flight now (for the tests). */
	public static List<Flight> inFlight() {
		return List.copyOf(FLIGHTS);
	}

	// ------------------------------------------------------------------ the flight

	private static void tick(MinecraftServer server) {
		if (FLIGHTS.isEmpty()) {
			return;
		}
		List<Flight> moving = new ArrayList<>();
		for (Flight flight : List.copyOf(FLIGHTS)) {
			if (flight.done) {
				continue;
			}
			if (!flight.caster.isAlive() || flight.caster.level() != flight.level || flight.step >= flight.steps) {
				flight.done = true;
				continue;
			}
			// A crescent loosed this tick takes its first step next tick, as it always has.
			if (server.getTickCount() <= flight.launched) {
				continue;
			}
			move(flight);
			if (!flight.done) {
				moving.add(flight);
			}
		}
		// Two crescents of different owners that met on the way this tick: both break, in a burst.
		for (int i = 0; i < moving.size(); i++) {
			Flight a = moving.get(i);
			for (int j = i + 1; j < moving.size() && !a.done; j++) {
				Flight b = moving.get(j);
				if (b.done || a.level != b.level || a.caster == b.caster) {
					continue;
				}
				if (AuraWorldRules.meets(xyz(a.prev), xyz(a.front), xyz(b.prev), xyz(b.front), AuraWorldRules.clashReach(a.width, b.width))) {
					clash(a, b);
				}
			}
		}
		for (Flight flight : moving) {
			if (!flight.done) {
				cutFrom(flight);
			}
		}
		// Every flight that ended this tick, however it ended, is told so once.
		for (Flight flight : List.copyOf(FLIGHTS)) {
			if (flight.done || flight.step >= flight.steps) {
				end(flight);
			}
		}
		FLIGHTS.removeIf(f -> f.done || f.step >= f.steps);
	}

	/** A flight is over: told once, where it ended. */
	private static void end(Flight flight) {
		flight.done = true;
		if (flight.ended) {
			return;
		}
		flight.ended = true;
		if (flight.onEnd != null) {
			try {
				flight.onEnd.ended(flight, flight.endAt != null ? flight.endAt : flight.front, flight.blockedAt);
			} catch (RuntimeException e) {
				dev.wildercord.Wildercord.LOGGER.warn("A crescent's ending failed", e);
			}
		}
	}

	private static double[] xyz(Vec3 v) {
		return new double[] {v.x, v.y, v.z};
	}

	/** One step: on along its line, stopped by a solid block. */
	private static void move(Flight flight) {
		int t = flight.step++;
		double d = 0.8 + flight.speed * (t + 1);
		flight.prev = flight.front;
		flight.front = flight.origin.add(flight.aim.scale(d));
		BlockPos pos = BlockPos.containing(flight.front);
		if (!flight.level.getBlockState(pos).getCollisionShape(flight.level, pos).isEmpty()) {
			flight.done = true;
			flight.endAt = flight.front.subtract(flight.aim.scale(0.4));
			flight.blockedAt = pos;
			AuraVfx.slashEnd(flight.level, flight.endAt, flight.aim, flight.color);
			return;
		}
		AuraVfx.slashStep(flight.level, flight.front, flight.aim, flight.side, flight.color, t, flight.weak);
		if (flight.onStep != null) {
			flight.onStep.accept(flight);
		}
	}

	/** Cuts each creature it reached this step once, the most it may. */
	private static void cutFrom(Flight flight) {
		Vec3 front = flight.front;
		double width = flight.width;
		for (Entity e : flight.level.getEntities(flight.caster, new AABB(front, front).inflate(width / 2 + 1, 2.0, width / 2 + 1),
				e -> e instanceof LivingEntity && e.isAlive() && !flight.hit.contains(e.getUUID()))) {
			if (flight.hit.size() >= flight.targets || flight.done) {
				break;
			}
			Vec3 rel = e.getBoundingBox().getCenter().subtract(front);
			if (Math.abs(rel.dot(flight.aim)) > flight.speed / 2 + 0.8 || Math.abs(rel.dot(flight.side)) > width / 2 + e.getBbWidth() / 2
				|| Math.abs(rel.y) > 1.6) {
				continue;
			}
			flight.hit.add(e.getUUID());
			if (!flight.mayCut.test(e)) {
				continue;
			}
			LivingEntity target = (LivingEntity) e;
			boolean catches = caught(target, flight);
			Flight before = cutting;
			cutting = flight;
			try {
				flight.cut.cut(flight, target);
			} finally {
				cutting = before;
			}
			AuraVfx.slashCut(flight.level, target.getBoundingBox().getCenter(), flight.aim, flight.color);
			if (catches && !flight.done && flight.pierce) {
				// A piercing crescent cuts through the guard (which still took its share) and flies on.
				AuraVfx.slashCut(flight.level, front, flight.aim, AuraVfx.hot(flight.color, 0.5));
			} else if (catches && !flight.done) {
				// A held guard facing it takes the cut and stops it there: nothing behind the guard is reached.
				flight.done = true;
				AuraVfx.slashEnd(flight.level, front.subtract(flight.aim.scale(0.6)), flight.aim, flight.color);
			}
		}
	}

	/** Whether {@code target}'s guard is up and facing the crescent's way. */
	static boolean caught(LivingEntity target, Flight flight) {
		if (target instanceof ServerPlayer player) {
			return AuraGuard.guarding(player) && AuraGuard.faces(player, flight.origin);
		}
		return target instanceof Guarding guarding && guarding.catches(flight);
	}

	// ------------------------------------------------------------------ meeting

	/**
	 * Two crescents meeting in the air: both break in a burst of their colours, shoving creatures back and harming nobody. A piercing
	 * crescent (the Way of the Blade's) meeting one that doesn't cuts it apart and flies on, weaker.
	 */
	static void clash(Flight a, Flight b) {
		Flight winner = a.pierce && !b.pierce ? a : b.pierce && !a.pierce ? b : null;
		a.done = true;
		b.done = true;
		if (winner != null) {
			winner.done = false;
			winner.scale *= WayRules.BLADE_CLASH_CARRY;
		}
		ServerLevel level = a.level;
		Vec3 at = a.front.add(b.front).scale(0.5);
		AuraVfx.clash(level, at, a.aim, a.color, b.color);
		String sound = WildercordSounds.kit("duelist_clash") != null ? "duelist_clash" : "aura_perfect_guard";
		Feels.sound(level, at, sound, 1.2F, 1.0F);
		ScreenFx.shake(level, at, 0.2F, 10);
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(AuraWorldRules.CLASH_RADIUS),
				e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
			dev.wildercord.monster.MonsterMagic.knock((LivingEntity) e, at, AuraWorldRules.CLASH_PUSH);
		}
		for (Flight f : List.of(a, b)) {
			if (f.caster instanceof ServerPlayer player) {
				Grimoire.unlock(player, "aura:clash");
			}
		}
	}

	/**
	 * Sends the crescent cutting {@code by} right now back at whoever loosed it (a perfect guard), as {@code by}'s own: the same
	 * harm, a little faster, in {@code color}. Returns false when no crescent is cutting {@code by} (a Thunder spark, say).
	 */
	public static boolean reflect(LivingEntity by, int color, Predicate<Entity> mayCut, Cut cut) {
		Flight flight = cutting;
		if (flight == null || flight.done || flight.caster == by) {
			return false;
		}
		flight.done = true;
		LivingEntity owner = flight.caster;
		Vec3 origin = by.getEyePosition().subtract(0, 0.45, 0);
		Vec3 aim = owner.isAlive() && owner.level() == by.level() && owner.distanceTo(by) < flight.range() * 1.5
			? owner.getBoundingBox().getCenter().subtract(origin) : by.getViewVector(1.0F);
		if (aim.lengthSqr() < 1.0E-4) {
			aim = by.getViewVector(1.0F);
		}
		launch(by, origin, aim, color, flight.damage(), flight.bonus, Math.min(2.4, flight.speed * 1.15), flight.range(), flight.width, flight.targets, false,
			mayCut, cut);
		AuraVfx.slashStart(flight.level, origin, aim.normalize(), color);
		return true;
	}
}
