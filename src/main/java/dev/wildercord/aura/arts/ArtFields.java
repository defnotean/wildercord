package dev.wildercord.aura.arts;

import dev.wildercord.cast.Effects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * What an art leaves on the ground for a while: a line of fire, a burning trail, a ring of fire, a path of ice, a whirlwind. A
 * field is only a shape and a beat: every {@code period} ticks its {@link Pulse} runs (draws its light, does its work on what
 * stands in it), until its time is up or its swordsman is gone (dead, away, in another world). Nothing in the world's blocks
 * changes: what a field looks like is light, and what it does it does to creatures.
 *
 * <p>Bounded: a swordsman keeps at most {@link #PER_PLAYER} fields at once (a new one ends the oldest), and the server at most
 * {@link #MAX} in all.</p>
 */
public final class ArtFields {
	private ArtFields() {}

	public static final int PER_PLAYER = 4;
	public static final int MAX = 256;

	/** Where a field is. */
	public interface Shape {
		/** Whether {@code e} stands in it. */
		boolean contains(Entity e);

		/** Its middle (where to look for creatures). */
		Vec3 centre();

		/** How far from its middle anything in it can be (for the search). */
		double extent();
	}

	/** A strip along {@code points} (a line of fire, a trail, a path), {@code half} wide each side, {@code height} tall. */
	public static Shape strip(List<Vec3> points, double half, double height) {
		List<Vec3> pts = List.copyOf(points);
		Vec3 sum = Vec3.ZERO;
		for (Vec3 p : pts) {
			sum = sum.add(p);
		}
		Vec3 centre = pts.isEmpty() ? Vec3.ZERO : sum.scale(1.0 / pts.size());
		double extent = 0;
		for (Vec3 p : pts) {
			extent = Math.max(extent, p.distanceTo(centre));
		}
		double reach = extent + half + 1;
		return new Shape() {
			@Override
			public boolean contains(Entity e) {
				Vec3 at = e.position();
				double r = half + e.getBbWidth() / 2;
				for (int i = 0; i < pts.size(); i++) {
					Vec3 a = pts.get(i);
					Vec3 b = i + 1 < pts.size() ? pts.get(i + 1) : a;
					if (Math.abs(at.y - (a.y + b.y) / 2) > height) {
						continue;
					}
					if (flatDistance(at, a, b) <= r) {
						return true;
					}
				}
				return false;
			}

			@Override
			public Vec3 centre() {
				return centre;
			}

			@Override
			public double extent() {
				return reach;
			}
		};
	}

	/** A disc round {@code centre} (which may move: a whirlwind round its swordsman), {@code radius} across, {@code height} up and down. */
	public static Shape disc(Supplier<Vec3> centre, double radius, double height) {
		return new Shape() {
			@Override
			public boolean contains(Entity e) {
				Vec3 c = centre.get();
				double dx = e.getX() - c.x;
				double dz = e.getZ() - c.z;
				double r = radius + e.getBbWidth() / 2;
				return dx * dx + dz * dz <= r * r && Math.abs(e.getY() - c.y) <= height;
			}

			@Override
			public Vec3 centre() {
				return centre.get();
			}

			@Override
			public double extent() {
				return radius + 1;
			}
		};
	}

	/** A ring round {@code centre}, from {@code inner} to {@code outer} out. */
	public static Shape ring(Vec3 centre, double inner, double outer, double height) {
		return new Shape() {
			@Override
			public boolean contains(Entity e) {
				double dx = e.getX() - centre.x;
				double dz = e.getZ() - centre.z;
				double d = Math.sqrt(dx * dx + dz * dz);
				double w = e.getBbWidth() / 2;
				return d >= inner - w && d <= outer + w && Math.abs(e.getY() - centre.y) <= height;
			}

			@Override
			public Vec3 centre() {
				return centre;
			}

			@Override
			public double extent() {
				return outer + 1;
			}
		};
	}

	/** The flat distance from {@code p} to the segment {@code a}–{@code b}. */
	static double flatDistance(Vec3 p, Vec3 a, Vec3 b) {
		double abx = b.x - a.x;
		double abz = b.z - a.z;
		double len = abx * abx + abz * abz;
		double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * abx + (p.z - a.z) * abz) / len));
		double cx = a.x + abx * t - p.x;
		double cz = a.z + abz * t - p.z;
		return Math.sqrt(cx * cx + cz * cz);
	}

	/** A field's beat: draw it, and do what it does to what stands in it. */
	@FunctionalInterface
	public interface Pulse {
		void pulse(Field field, ServerPlayer owner, long age);
	}

	/** Told once as a field ends (its time up, or its swordsman gone). */
	@FunctionalInterface
	public interface End {
		void ended(Field field);
	}

	/** A field on the ground. */
	public static final class Field {
		final UUID owner;
		final ServerLevel level;
		final String kind;
		final Shape shape;
		final long start;
		final long until;
		final int period;
		final Pulse pulse;
		End end;
		boolean done;

		Field(UUID owner, ServerLevel level, String kind, Shape shape, long start, long until, int period, Pulse pulse) {
			this.owner = owner;
			this.level = level;
			this.kind = kind;
			this.shape = shape;
			this.start = start;
			this.until = until;
			this.period = Math.max(1, period);
			this.pulse = pulse;
		}

		public String kind() {
			return kind;
		}

		public Shape shape() {
			return shape;
		}

		public ServerLevel level() {
			return level;
		}

		/** Ticks left. */
		public long left() {
			return until - level.getGameTime();
		}

		/** The foes of {@code owner} standing in it. */
		public List<LivingEntity> foes(ServerPlayer owner) {
			return inside(owner, true);
		}

		/** {@code owner} and their allies standing in it. */
		public List<LivingEntity> allies(ServerPlayer owner) {
			return inside(owner, false);
		}

		private List<LivingEntity> inside(ServerPlayer owner, boolean foes) {
			Vec3 c = shape.centre();
			double r = shape.extent();
			List<LivingEntity> out = new ArrayList<>();
			for (Entity e : level.getEntities((Entity) null, new AABB(c, c).inflate(r, 4, r),
					e -> e instanceof LivingEntity living && living.isAlive() && (foes ? ArtKit.harmable(owner, e) : ArtKit.helpable(owner, e)))) {
				if (shape.contains(e)) {
					out.add((LivingEntity) e);
				}
			}
			return out;
		}

		/** Something to do once it's over. */
		public Field onEnd(End end) {
			this.end = end;
			return this;
		}

		/** Ends it now. */
		public void end() {
			done = true;
		}
	}

	private static final List<Field> FIELDS = new ArrayList<>();
	/** Only retained while a pulse is on the server stack; callbacks can retire it before the pulse returns. */
	private static Field pulsing;
	private static ServerPlayer pulseOwner;
	private static Object pulseScope;

	/** A retired pulse cannot keep harming through a target list it collected before a synchronous callback. */
	public static boolean blocksRetiredHarm(Entity source, Entity target) {
		return pulsing != null && pulsing.done && source == pulseOwner && source != target
			&& Effects.applying() == pulseOwner && Effects.applyingCast() == null && Effects.sourceScope() == pulseScope;
	}

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ArtFields::tick);
	}

	/**
	 * Lays a field of {@code owner}'s: {@code shape}, for {@code ticks}, beating every {@code period} ticks (the first beat next
	 * tick). Ends the owner's oldest field of any kind if they already have {@link #PER_PLAYER}.
	 */
	public static Field open(ServerPlayer owner, String kind, Shape shape, int ticks, int period, Pulse pulse) {
		long now = owner.level().getGameTime();
		Field field = new Field(owner.getUUID(), owner.level(), kind, shape, now, now + Math.max(1, ticks), period, pulse);
		FIELDS.add(field);
		// Add first: an end callback may open another field, which must see and respect this replacement too.
		while (true) {
			List<Field> theirs = FIELDS.stream().filter(f -> f.owner.equals(owner.getUUID()) && !f.done).toList();
			if (theirs.size() <= PER_PLAYER) break;
			finish(theirs.getFirst());
		}
		while (FIELDS.size() > MAX) {
			finish(FIELDS.getFirst());
		}
		return field;
	}

	private static void finish(Field field) {
		// Detach before calling user code: a callback can forget or open fields itself.
		FIELDS.remove(field);
		if (field.done && field.end == null) {
			return;
		}
		field.done = true;
		End end = field.end;
		field.end = null;
		if (end != null) {
			try {
				end.ended(field);
			} catch (RuntimeException e) {
				dev.wildercord.Wildercord.LOGGER.warn("An art's field failed to end", e);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		if (FIELDS.isEmpty()) {
			return;
		}
		// Damage/end callbacks may open or remove fields. New fields first beat on a later tick.
		for (Field field : List.copyOf(FIELDS)) {
			ServerPlayer owner = server.getPlayerList().getPlayer(field.owner);
			long now = field.level.getGameTime();
			if (field.done || owner == null || !owner.isAlive() || owner.level() != field.level || now > field.until) {
				finish(field);
				continue;
			}
			long age = now - field.start;
			if (age > 0 && age % field.period == 0) {
				Field outerPulse = pulsing;
				ServerPlayer outerOwner = pulseOwner;
				Object outerScope = pulseScope;
				pulsing = field;
				pulseOwner = owner;
				try {
					Effects.withSource(owner, () -> {
						pulseScope = Effects.sourceScope();
						field.pulse.pulse(field, owner, age);
					});
				} catch (RuntimeException e) {
					dev.wildercord.Wildercord.LOGGER.warn("An art's field failed; ending it", e);
					finish(field);
				} finally {
					pulsing = outerPulse;
					pulseOwner = outerOwner;
					pulseScope = outerScope;
				}
			}
		}
	}

	/** How many fields of {@code kind} {@code owner} has now (for the tests). */
	public static int count(ServerPlayer owner, String kind) {
		int n = 0;
		for (Field f : FIELDS) {
			if (f.owner.equals(owner.getUUID()) && !f.done && f.kind.equals(kind) && owner.level().getGameTime() <= f.until) {
				n++;
			}
		}
		return n;
	}

	/** Whether {@code e} stands in a field of {@code kind} of {@code owner}'s now. */
	public static boolean inside(ServerPlayer owner, String kind, Entity e) {
		for (Field f : FIELDS) {
			if (f.owner.equals(owner.getUUID()) && !f.done && f.kind.equals(kind) && f.level == e.level() && owner.level().getGameTime() <= f.until
					&& f.shape.contains(e)) {
				return true;
			}
		}
		return false;
	}

	static void forget(UUID id) {
		for (Field f : List.copyOf(FIELDS)) {
			if (f.owner.equals(id)) {
				finish(f);
			}
		}
	}

	static void clear() {
		// Preserve silent server-stop cleanup, while retiring a pulse still unwinding on this stack.
		for (Field field : FIELDS) {
			field.done = true;
			field.end = null;
		}
		FIELDS.clear();
	}
}
