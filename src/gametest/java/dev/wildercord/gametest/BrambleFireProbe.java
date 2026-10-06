package dev.wildercord.gametest;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.monster.Bramblewalker;
import dev.wildercord.monster.WildMonster;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** GameTest-only, bounded observations of the original fire escape. Never asks for a path or consumes randomness. */
public final class BrambleFireProbe {
	static final String SUITE = "dev.wildercord.gametest.WildercordMonstersTest";
	static final int TICKS = 40, BODY_LIMIT = TICKS + 2, EVENT_LIMIT = 128, NODE_LIMIT = 32;
	private static volatile Session active;
	private static boolean registered;

	private BrambleFireProbe() {}

	/** Called immediately after the original ignition, on the server thread. Failure cannot alter the fixture. */
	public static synchronized Session begin(ServerLevel level, Bramblewalker walker, ServerPlayer player) {
		Session session = null;
		try {
			if (!SUITE.equals(suite()) || !level.getServer().isSameThread()) return null;
			if (!registered) {
				ServerTickEvents.END_SERVER_TICK.register(server -> {
					Session s = active;
					if (s != null && s.world instanceof ServerLevel world && world.getServer() == server
							&& s.actor instanceof Bramblewalker actor) tick(actor);
				});
				registered = true;
			}
			session = install(SUITE, level, walker, player, Thread.currentThread(), level.getGameTime(),
				line -> System.out.println("BRAMBLE_FIRE_RECEIPT " + line));
			capture(session, walker, "ignited", true);
		} catch (Throwable ignored) {
			if (session != null) session.errors++;
		}
		return session;
	}

	static synchronized Session install(String suite, Object world, Object actor, Object player, Thread owner,
			long start, Consumer<String> sink) {
		Session session = new Session(suite, world, actor, player, owner, start, sink, active);
		active = session;
		return session;
	}

	static Session selected(String suite, Object world, Object actor, Thread thread, long now) {
		Session s = active;
		return s != null && !s.closed && !s.finished && SUITE.equals(suite) && s.suite.equals(suite)
			&& s.world == world && s.actor == actor && s.owner == thread && now >= s.start && now - s.start <= TICKS ? s : null;
	}

	private static String suite() {
		var test = FabricClientGameTestRunner.currentlyRunningGameTest;
		return test == null ? "none" : test.getDefinition();
	}

	private static Session selected(PathfinderMob actor) {
		return actor == null ? null : selected(suite(), actor.level(), actor, Thread.currentThread(), actor.level().getGameTime());
	}

	/** Exactly one call with the original arguments; the returned destination and native exception are preserved. */
	public static Vec3 pick(PathfinderMob actor, int horizontal, int vertical, Vec3 away, Operation<Vec3> original) {
		Vec3 result = null;
		boolean returned = false;
		try {
			result = original.call(actor, horizontal, vertical, away);
			returned = true;
			return result;
		} finally {
			if (active != null) try {
				Session s = selected(actor);
				if (s != null) {
					s.picks++;
					s.event("event=pick now=" + actor.level().getGameTime() + " horizontal=" + horizontal
						+ " vertical=" + vertical + " away=" + away + " destination=" + result + " returned=" + returned);
				}
			} catch (Throwable ignored) { observationError(); }
		}
	}

	/** Wraps the original coordinate moveTo call, never a second request or a diagnostic route. */
	public static boolean move(Bramblewalker actor, PathNavigation navigation, double x, double y, double z, double speed,
			Operation<Boolean> original) {
		Boolean result = null;
		try {
			result = original.call(navigation, x, y, z, speed);
			return result;
		} finally {
			if (active != null) try {
				Session s = selected(actor);
				if (s != null) {
					s.moves++;
					s.event("event=move now=" + actor.level().getGameTime() + " destination=" + new Vec3(x, y, z)
						+ " speed=" + speed + " returned=" + (result != null) + " result=" + result
						+ " nativePath=" + path(navigation.getPath()));
				}
			} catch (Throwable ignored) { observationError(); }
		}
	}

	public static void goal(Bramblewalker actor, String event, Boolean result) {
		if (active == null) return;
		try {
			Session s = selected(actor);
			if (s != null) s.event("event=" + event + " now=" + actor.level().getGameTime() + " result=" + result);
		} catch (Throwable ignored) { observationError(); }
	}

	private static void tick(Bramblewalker actor) {
		try {
			Session s = selected(actor);
			if (s == null || !s.admitTick(actor.level().getGameTime())) return;
			Vec3 position = actor.position();
			boolean stopped = s.previousPosition != null && position.distanceToSqr(s.previousPosition) < 0.0001;
			boolean terrain = (stopped || actor.horizontalCollision || actor.verticalCollision) && s.ticks % 5 == 0;
			capture(s, actor, "tick", terrain);
			s.previousPosition = position;
		} catch (Throwable ignored) { observationError(); }
	}

	/** Final snapshot at the existing assertion's server callback, without extending the observation window. */
	public static void finish(Session session) {
		if (session == null || session.finished || session.closed) return;
		try {
			if (session.actor instanceof Bramblewalker actor && active == session && SUITE.equals(suite())
					&& session.world == actor.level() && session.owner == Thread.currentThread()) {
				session.measurementTime = actor.level().getGameTime();
				capture(session, actor, "measured", true);
				session.measured = true;
			}
		} catch (Throwable ignored) { session.errors++; }
		finally { session.finished = true; }
	}

	/** Closing is safe on the test thread, including when native execution threw; it reads no game state. */
	public static void close(Session session) {
		if (session != null) session.close();
	}

	private static void observationError() {
		Session s = active;
		if (s != null) s.errors++;
	}

	private static void capture(Session s, Bramblewalker actor, String event, boolean terrain) {
		if (s.bodies.size() >= BODY_LIMIT) { s.omittedBodies++; return; }
		ServerPlayer player = (ServerPlayer) s.player;
		var move = actor.getMoveControl();
		var goals = actor.getGoalSelector().getAvailableGoals().stream().filter(g -> g.isRunning())
			.map(g -> g.getGoal().getClass().getSimpleName()).toList();
		s.lastTime = actor.level().getGameTime();
		s.bodies.add("event=" + event + " now=" + s.lastTime + " elapsed=" + (s.lastTime - s.start)
			+ " tick=" + actor.tickCount + " alive=" + actor.isAlive() + " noAi=" + actor.isNoAi()
			+ " player=" + player.position() + " body=" + actor.position() + " distance=" + actor.distanceTo(player)
			+ " box=" + actor.getBoundingBox() + " delta=" + actor.getDeltaMovement()
			+ " fireTicks=" + actor.getRemainingFireTicks() + " fleeing=" + actor.fleeing()
			+ " windingUp=" + actor.windingUp() + " acting=" + actor.state(WildMonster.ACTING)
			+ " ground=" + actor.onGround() + " water=" + actor.isInWater()
			+ " horizontalCollision=" + actor.horizontalCollision + " verticalCollision=" + actor.verticalCollision
			+ " movementAttribute=" + actor.getAttributeValue(Attributes.MOVEMENT_SPEED) + " speed=" + actor.getSpeed()
			+ " input=" + new Vec3(actor.xxa, actor.yya, actor.zza) + " goals=" + goals
			+ " navigationDone=" + actor.getNavigation().isDone() + " nativePath=" + path(actor.getNavigation().getPath())
			+ " moveControl={wanted=" + move.hasWanted() + ",x=" + move.getWantedX() + ",y=" + move.getWantedY()
			+ ",z=" + move.getWantedZ() + ",speed=" + move.getSpeedModifier() + "}"
			+ (terrain ? " nearbyResidentStates=" + terrain((ServerLevel) actor.level(), actor.blockPosition()) : ""));
	}

	static String path(Path path) {
		if (path == null) return "none";
		List<String> nodes = new ArrayList<>();
		for (int i = 0; i < Math.min(path.getNodeCount(), NODE_LIMIT); i++) {
			var n = path.getNode(i);
			nodes.add(i + ":" + n.x + "," + n.y + "," + n.z + ":" + n.type + ":malus=" + n.costMalus);
		}
		return "{identity=" + System.identityHashCode(path) + ",canReach=" + path.canReach() + ",done=" + path.isDone()
			+ ",index=" + path.getNextNodeIndex() + ",count=" + path.getNodeCount() + ",target=" + path.getTarget()
			+ ",next=" + (path.isDone() ? "none" : path.getNextNodePos()) + ",nodes=" + nodes
			+ ",omittedNodes=" + Math.max(0, path.getNodeCount() - NODE_LIMIT) + "}";
	}

	interface ResidentReader {
		Object chunkNow(int x, int z);
		String state(Object chunk, int x, int y, int z);
	}

	private static String terrain(ServerLevel level, BlockPos pos) {
		return terrain(pos.getX(), pos.getY(), pos.getZ(), new ResidentReader() {
			public Object chunkNow(int x, int z) { return level.getChunkSource().getChunkNow(x, z); }
			public String state(Object chunk, int x, int y, int z) {
				if (level.isOutsideBuildHeight(y)) return "outside-build-height";
				var block = ((LevelChunk) chunk).getBlockState(new BlockPos(x, y, z));
				return block.isAir() ? null : block.toString();
			}
		});
	}

	/** At most nine resident lookups and 45 state reads. No shapes, light queries, tickets or chunk loads. */
	static String terrain(int x, int y, int z, ResidentReader reader) {
		List<String> blocks = new ArrayList<>();
		int missing = 0;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
			int bx = x + dx, bz = z + dz;
			Object chunk = reader.chunkNow(bx >> 4, bz >> 4);
			if (chunk == null) { missing++; continue; }
			for (int dy = -1; dy <= 3; dy++) {
				String state = reader.state(chunk, bx, y + dy, bz);
				if (state != null) blocks.add(bx + "," + (y + dy) + "," + bz + ":" + state);
			}
		}
		return "{missingColumns=" + missing + ",states=" + blocks + "}";
	}

	static final class Session implements AutoCloseable {
		final String suite;
		final Object world, actor, player;
		final Thread owner;
		final long start;
		final Consumer<String> sink;
		final Session previous;
		final List<String> events = new ArrayList<>(), bodies = new ArrayList<>();
		volatile boolean closed, finished;
		boolean measured;
		long lastTick = Long.MIN_VALUE, lastTime, measurementTime = Long.MIN_VALUE;
		int ticks, eventCount, picks, moves, errors, sinkFailures, omittedBodies;
		Vec3 previousPosition;

		Session(String suite, Object world, Object actor, Object player, Thread owner, long start, Consumer<String> sink, Session previous) {
			this.suite = suite; this.world = world; this.actor = actor; this.player = player; this.owner = owner;
			this.start = start; this.lastTime = start; this.sink = sink; this.previous = previous;
		}

		boolean admitTick(long now) {
			if (closed || finished || ticks >= TICKS || now <= start || now - start > TICKS || now == lastTick) return false;
			lastTick = now; ticks++; return true;
		}

		void event(String row) {
			if (closed || finished) return;
			eventCount++;
			if (events.size() < EVENT_LIMIT) events.add(row);
		}

		private void send(String row) {
			try { sink.accept("suite=" + suite + " " + row); } catch (Throwable ignored) { sinkFailures++; }
		}

		@Override public synchronized void close() {
			synchronized (BrambleFireProbe.class) {
				if (closed) return;
				closed = true;
				if (active == this) {
					active = previous;
					while (active != null && active.closed) active = active.previous;
				}
			}
			try {
				events.forEach(this::send);
				bodies.forEach(this::send);
				send("event=summary start=" + start + " lastTime=" + lastTime + " elapsed=" + (lastTime - start)
					+ " sampledTicks=" + ticks + " bodyRows=" + bodies.size() + " omittedBodies=" + omittedBodies + " events=" + eventCount
					+ " retainedEvents=" + events.size() + " omittedEvents=" + Math.max(0, eventCount - events.size())
					+ " picks=" + picks + " moves=" + moves + " observationErrors=" + errors + " sinkFailures=" + sinkFailures
					+ " measured=" + measured + " measurementTime=" + measurementTime);
			} finally { events.clear(); bodies.clear(); }
		}
	}
}
