package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Passive, finite receipts for the original lantern scene. Never calls AI, RNG, or chunk acquisition. */
public final class GlimmerwingLanternProbe {
	static final int MAX_ROWS = 256;
	private static final ThreadLocal<Session> ACTIVE = new ThreadLocal<>();
	private static final ThreadLocal<Search> SEARCH = new ThreadLocal<>();
	private GlimmerwingLanternProbe() {}

	/** Admission uses observations of production calls; proximity alone cannot establish attraction. */
	static final class Arrival {
		Object activeLure;
		int firstAcquisition = -1, activeAcquisition = -1, luredTarget = -1, firstLuredTarget = -1, arrived = -1;
		void acquisition(int tick, Object lure, boolean eligible) {
			if (!eligible || lure == null) { activeLure = null; activeAcquisition = -1; luredTarget = -1; return; }
			if (!Objects.equals(activeLure, lure)) {
				activeLure = lure; activeAcquisition = tick; luredTarget = -1;
				if (firstAcquisition < 0) firstAcquisition = tick;
			}
		}
		void target(int tick, Object lure, boolean luredBranch) {
			if (luredBranch && activeLure != null && Objects.equals(activeLure, lure) && tick >= activeAcquisition && luredTarget < 0) {
				luredTarget = tick;
				if (firstLuredTarget < 0) firstLuredTarget = tick;
			}
		}
		void flight(int tick, Object lure, boolean eligibleBody, double distance) {
			if (arrived >= 0) return;
			if (!eligibleBody || !Objects.equals(activeLure, lure)) {
				activeLure = null; activeAcquisition = -1; luredTarget = -1; return;
			}
			if (activeLure != null
					&& luredTarget >= 0 && tick > luredTarget && distance <= 3.2) arrived = tick;
		}
	}

	public static final class Session implements AutoCloseable {
		final Object world, actor, scene;
		final Session previous;
		final Consumer<String> output;
		final List<String> rows = new ArrayList<>();
		BlockPos lamp;
		long started;
		int startTick, searches, lightReads, maxSearchLightReads, summarizedLightReads, aiReturns, samples, dropped, errors;
		final int[] lightHistogram = new int[16];
		final Arrival arrival = new Arrival();
		double tickMinimum = Double.POSITIVE_INFINITY, sampledMinimum = Double.POSITIVE_INFINITY;
		int firstNearTick = -1;
		boolean frozen, closed;
		String terminal = "outcome=interrupted";

		Session(Object world, Object actor, Object scene, Consumer<String> output) {
			this.world = world; this.actor = actor; this.scene = scene; this.output = output;
			previous = ACTIVE.get(); ACTIVE.set(this);
		}
		boolean accepts(Object world, Object actor, Object scene) {
			return !closed && !frozen && ACTIVE.get() == this && this.world == world && this.actor == actor && this.scene == scene;
		}
		void record(Supplier<String> read) {
			if (closed || frozen) return;
			if (rows.size() >= MAX_ROWS) { dropped++; return; }
			try { rows.add(read.get()); } catch (Exception | AssertionError | LinkageError failure) { errors++; }
		}
		void decide(String outcome) { terminal = outcome; frozen = true; }
		@Override public void close() {
			if (closed) return;
			closed = true; frozen = true;
			// Restore scope before optional logging, including when its sink fails or reenters.
			if (ACTIVE.get() == this) {
				Session restore = previous;
				while (restore != null && restore.closed) restore = restore.previous;
				if (restore == null) ACTIVE.remove(); else ACTIVE.set(restore);
			}
			try {
				output.accept("event=summary scene=wildlife-lantern " + terminal + " searches=" + searches + " lightReads=" + lightReads
					+ " aiReturns=" + aiReturns + " originalSamples=" + samples + " sampledMinimum=" + sampledMinimum
						+ " tickMinimum=" + tickMinimum + " firstNearEntityTick=" + firstNearTick
						+ " firstAcquisitionEntityTick=" + arrival.firstAcquisition + " firstLuredTargetEntityTick=" + arrival.firstLuredTarget
						+ " firstQualifiedArrivalEntityTick=" + arrival.arrived + " maxSearchLightReads=" + maxSearchLightReads
						+ " summarizedLightReads=" + summarizedLightReads + " lightHistogram=" + java.util.Arrays.toString(lightHistogram)
					+ " rows=" + rows.size() + " dropped=" + dropped + " observationErrors=" + errors);
				for (String row : rows) output.accept(row);
			} catch (Exception | AssertionError | LinkageError failure) { errors++; }
		}
	}

	private static final class Search {
		final Session session;
		int reads;
		Search(Session session) { this.session = session; }
	}
	static Session open(Object world, Object actor, Object scene, Consumer<String> output) {
		return new Session(world, actor, scene, output);
	}
	private static Session matching(Object world, Object actor) {
		Session session = ACTIVE.get();
		return session != null && session.accepts(world, actor, session.scene) ? session : null;
	}
	/** The original call executes exactly once, even for an unrelated/nested search or an exception. */
	static <T> T aroundSearch(Object world, Object actor, Supplier<T> original) {
		Search previous = SEARCH.get();
		Session session = matching(world, actor);
		Search entered = new Search(session);
		SEARCH.set(entered);
		if (session != null) session.searches++;
		try { return original.get(); }
		finally {
			if (session != null) session.maxSearchLightReads = Math.max(session.maxSearchLightReads, entered.reads);
			if (previous == null) SEARCH.remove(); else SEARCH.set(previous);
		}
	}
	static int forwardLight(Object world, Object actor, IntSupplier original, IntFunction<String> receipt) {
		int result = original.getAsInt();
		Search search = SEARCH.get();
		Session session = matching(world, actor);
		if (session != null && search != null && search.session == session) {
			session.lightReads++;
			search.reads++;
			if (result >= 0 && result < session.lightHistogram.length) session.lightHistogram[result]++;
			try {
				String row = receipt.apply(result);
				if (search.reads <= 5) session.record(() -> row);
				else session.summarizedLightReads++;
			} catch (Exception | AssertionError | LinkageError failure) { session.errors++; }
		}
		return result;
	}

	public static Session begin(ServerLevel level, Glimmerwing moth, BlockPos lamp) {
		Session session = open(level, moth, new Object(), row -> Wildercord.LOGGER.info("WILDERCORD_GLIMMERWING_LANTERN {}", row));
		session.lamp = lamp.immutable(); session.started = level.getGameTime(); session.startTick = moth.tickCount;
		session.record(() -> "event=begin " + body(session, level, moth) + " lamp={" + cell(level, lamp)
			+ "} support={" + cell(level, lamp.below()) + "} noAi=" + moth.isNoAi() + " persistent=" + moth.isPersistenceRequired());
		return session;
	}
	public static void searchCall(ServerLevel level, Glimmerwing moth, Runnable original) {
		aroundSearch(level, moth, () -> { original.run(); return null; });
	}
	public static int lightRead(ServerLevel level, Glimmerwing moth, BlockPos at, IntSupplier original) {
		return forwardLight(level, moth, original, result -> "event=light-sample tick=" + moth.tickCount
			+ " search=" + ACTIVE.get().searches + " at=" + at.toShortString() + " returnedLight=" + result
			+ " cell={" + cell(level, at) + "}");
	}
	public static void search(ServerLevel level, Glimmerwing moth, boolean after, BlockPos flower, Vec3 target,
			Vec3 lure, int retarget, int lureLeft) {
		Session session = matching(level, moth);
		Search search = SEARCH.get();
		if (session == null || search == null || search.session != session) return;
		if (after) {
			try { session.arrival.acquisition(moth.tickCount, lure, lanternLure(session, level, moth, flower, lure, lureLeft)); }
			catch (Exception | AssertionError | LinkageError failure) { session.errors++; }
		}
		session.record(() -> "event=search-" + (after ? "return" : "head") + " search=" + session.searches + " "
			+ body(session, level, moth) + " " + flight(level, flower, target, lure, retarget, lureLeft)
			+ " eligibleCasters=" + level.players().stream().filter(player -> !player.isSpectator()
				&& player.distanceToSqr(moth) < WildlifeRules.CAST_LURE_RANGE * WildlifeRules.CAST_LURE_RANGE
				&& Wildlife.castRecently(player, WildlifeRules.CAST_LURE_TICKS))
					.map(player -> player.getUUID() + "@" + player.getEyePosition().add(0, .6, 0)).toList());
	}
	/** Called only on the real pickTarget return; no fabricated target or invocation of the AI. */
	public static void target(ServerLevel level, Glimmerwing moth, BlockPos flower, Vec3 lure, int lureLeft, Vec3 target) {
		Session session = matching(level, moth);
		if (session == null) return;
		try {
			int before = session.arrival.luredTarget;
			session.arrival.target(moth.tickCount, lure, flower == null && lure != null && lureLeft > 0 && target != null);
			if (before != session.arrival.luredTarget) session.record(() -> "event=lured-target " + body(session, level, moth)
					+ " lure=" + lure + " target=" + target);
		} catch (Exception | AssertionError | LinkageError failure) { session.errors++; }
	}
	public static void flight(ServerLevel level, Glimmerwing moth, BlockPos flower, Vec3 target, Vec3 lure, int retarget, int lureLeft) {
		Session session = matching(level, moth);
		if (session == null) return;
		try {
			session.aiReturns++;
			double distance = moth.position().distanceTo(Vec3.atCenterOf(session.lamp));
			session.tickMinimum = Math.min(session.tickMinimum, distance);
			if (distance <= 3.2 && session.firstNearTick < 0) session.firstNearTick = moth.tickCount;
			int before = session.arrival.arrived;
			session.arrival.flight(moth.tickCount, lure, moth.tickCount - session.startTick <= 500
					&& lanternLure(session, level, moth, flower, lure, lureLeft), distance);
			if (before != session.arrival.arrived) session.record(() -> "event=qualified-arrival " + body(session, level, moth)
					+ " " + flight(level, flower, target, lure, retarget, lureLeft));
			if ((moth.tickCount - session.startTick) % 20 == 0)
				session.record(() -> "event=ai-return " + body(session, level, moth) + " " + flight(level, flower, target, lure, retarget, lureLeft));
		} catch (Exception | AssertionError | LinkageError failure) { session.errors++; }
	}
	/** The original twenty-tick loop reads a latched proof of actual acquisition and subsequent arrival. */
	public static void sampled(Session session, ServerLevel level, Entity found, double distance, int attempt) {
		if (session == null || !session.accepts(level, session.actor, session.scene)) return;
		try {
			session.samples++; session.sampledMinimum = Math.min(session.sampledMinimum, distance);
			session.record(() -> "event=original-sample attempt=" + attempt + " elapsedServerTicks=" + (level.getGameTime() - session.started)
				+ " distance=" + distance + " sameActor=" + (found == session.actor)
				+ (found instanceof Glimmerwing moth ? " " + body(session, level, moth) : " found=" + found));
			if (session.arrival.arrived >= 0 || attempt == 25)
				session.decide("arrived=" + (session.arrival.arrived >= 0) + " finalSample=" + distance + " finalAttempt=" + attempt);
		} catch (Exception | AssertionError | LinkageError failure) { session.errors++; }
	}
	public static boolean arrived(Session session) {
		return session != null && session.arrival.arrived >= 0 && session.errors == 0 && session.maxSearchLightReads <= 55;
	}
	public static void finish(Session session) { if (session != null) session.close(); }

	private static boolean lanternLure(Session session, ServerLevel level, Glimmerwing moth, BlockPos flower, Vec3 lure, int lureLeft) {
		if (flower != null || lure == null || lureLeft <= 0 || !moth.isAlive() || moth.isRemoved()
				|| moth.level() != level || level.getEntity(moth.getUUID()) != moth) return false;
		if (level.players().stream().anyMatch(player -> !player.isSpectator()
				&& player.distanceToSqr(moth) < WildlifeRules.CAST_LURE_RANGE * WildlifeRules.CAST_LURE_RANGE
				&& Wildlife.castRecently(player, WildlifeRules.CAST_LURE_TICKS))) return false;
		BlockPos at = BlockPos.containing(lure);
		if (Math.abs(at.getX() - session.lamp.getX()) + Math.abs(at.getY() - session.lamp.getY())
				+ Math.abs(at.getZ() - session.lamp.getZ()) > 5) return false;
		var lampChunk = level.getChunkSource().getChunkNow(session.lamp.getX() >> 4, session.lamp.getZ() >> 4);
		var lureChunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
		return lampChunk != null && lureChunk != null
				&& lampChunk.getBlockState(session.lamp).is(net.minecraft.world.level.block.Blocks.LANTERN)
				&& lureChunk.getBlockState(at).isAir() && level.getBrightness(LightLayer.BLOCK, at) >= WildlifeRules.LIGHT_LURE;
	}

	private static String body(Session session, ServerLevel level, Glimmerwing moth) {
		return "elapsedServerTicks=" + (level.getGameTime() - session.started) + " entityTick=" + moth.tickCount
			+ " id=" + moth.getId() + " uuid=" + moth.getUUID() + " world=" + moth.level().dimension().identifier()
			+ " sameWorld=" + (moth.level() == level) + " tracked=" + (level.getEntity(moth.getUUID()) == moth)
			+ " alive=" + moth.isAlive() + " removed=" + moth.isRemoved() + " position=" + moth.position()
			+ " distance=" + moth.position().distanceTo(Vec3.atCenterOf(session.lamp)) + " velocity=" + moth.getDeltaMovement()
			+ " onGround=" + moth.onGround() + " horizontalCollision=" + moth.horizontalCollision + " verticalCollision=" + moth.verticalCollision
			+ " body=" + moth.getBoundingBox() + " clock=" + level.getOverworldClockTime()
			+ " cell={" + cell(level, moth.blockPosition()) + "}";
	}
	private static String flight(ServerLevel level, BlockPos flower, Vec3 target, Vec3 lure, int retarget, int lureLeft) {
		return "flower=" + flower + " target=" + target + " lure=" + lure + " retarget=" + retarget + " lureLeft=" + lureLeft
			+ " targetCell={" + (target == null ? "none" : cell(level, BlockPos.containing(target)))
			+ "} lureCell={" + (lure == null ? "none" : cell(level, BlockPos.containing(lure))) + "}";
	}
	/** Extra reads use an already completed chunk; no Level.getBlockState, heightmap, path, or chunk request. */
	private static String cell(ServerLevel level, BlockPos at) {
		var chunk = level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
		if (chunk == null) return "at=" + at.toShortString() + " resident=false";
		var state = chunk.getBlockState(at);
		return "at=" + at.toShortString() + " resident=true state=" + state + " air=" + state.isAir()
			+ " blockLight=" + level.getBrightness(LightLayer.BLOCK, at);
	}
}
