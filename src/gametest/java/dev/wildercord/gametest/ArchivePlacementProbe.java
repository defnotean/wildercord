package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.gametest.mixin.ArchivePiecePositionAccess;
import dev.wildercord.world.ArchivePiece;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.function.Consumer;

/** Observes only the tour's original placement. Never generates, loads, retries, or changes a result. */
public final class ArchivePlacementProbe {
	static final String SUITE = "dev.wildercord.gametest.WildercordFeatureTour";
	static final String STRUCTURE = "wildercord:archive";
	static final int MAX_LINES = 8;
	private static final ThreadLocal<Session> COMMAND = new ThreadLocal<>();
	private static final ThreadLocal<Session> PLACEMENT = new ThreadLocal<>();
	private ArchivePlacementProbe() {}

	static String currentSuite() {
		var test = FabricClientGameTestRunner.currentlyRunningGameTest;
		return test == null ? "none" : test.getDefinition();
	}

	static Session begin(ServerLevel level, BlockPos origin) {
		Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"" + SUITE + "\",\"seed\":\"" + level.getSeed() + "\"}");
		return new Session(level, level.getSeed(), level.getMinY(), origin, line -> Wildercord.LOGGER.info(line));
	}

	static boolean matches(Session session, String suite, Object world, long seed, String structure, BlockPos origin) {
		return session != null && !session.closed && session.owner == Thread.currentThread()
			&& SUITE.equals(suite) && session.world == world && session.seed == seed
			&& STRUCTURE.equals(structure) && session.origin.equals(origin);
	}

	public static PlacementScope placement(ServerLevel level, String structure, BlockPos origin) {
		return placement(currentSuite(), level, level.getSeed(), structure, origin);
	}

	static PlacementScope placement(String suite, Object world, long seed, String structure, BlockPos origin) {
		Session previous = PLACEMENT.get(), session = COMMAND.get();
		if (!matches(session, suite, world, seed, structure, origin) || session.placementCalls++ != 0) session = null;
		// A nested unrelated placement must not write to its caller's receipt.
		PLACEMENT.set(session);
		return new PlacementScope(previous, session);
	}

	private static Session observing() {
		Session session = PLACEMENT.get();
		return session != null && session == COMMAND.get() && !session.closed && session.owner == Thread.currentThread() ? session : null;
	}

	public static void terrain(int x, int z, Heightmap.Types map, int height) {
		var session = observing();
		if (session != null) session.terrain(x, z, map.toString(), height);
	}

	public static void generated(StructureStart start, boolean valid) {
		var session = observing();
		if (session == null) return;
		try {
			session.startCalls++;
			if (session.startCalls != 1) return;
			session.valid = valid;
			session.pieceCount = start.getPieces().size();
			if (!valid) return;
			session.startBounds = start.getBoundingBox().toString();
			for (var piece : start.getPieces()) {
				if (!(piece instanceof ArchivePiece archive)) continue;
				session.archivePieces++;
				if (session.archivePieces != 1) continue;
				var position = (ArchivePiecePositionAccess) archive;
				session.piece(archive.getOrientation().toString(), archive.getBoundingBox().toString(),
					archive.entrance(), position.wildercord$worldPosition(20, 3, 77));
			}
		} catch (RuntimeException failure) { session.observationFailures++; }
	}

	public static final class PlacementScope {
		private final Session previous, session;
		private PlacementScope(Session previous, Session session) { this.previous = previous; this.session = session; }
		public void finish(Integer result, ServerLevel level) {
			try {
				if (session != null) {
					session.commandResult = result;
					session.afterCommand = session.resident(level);
					session.emit("COMMAND " + session.generation() + ", result=" + result + ", resident=" + session.afterCommand);
				}
			} finally { if (previous == null) PLACEMENT.remove(); else PLACEMENT.set(previous); }
		}
	}

	static final class CommandScope implements AutoCloseable {
		private final Session session;
		private CommandScope(Session session) { this.session = session; }
		@Override public void close() {
			if (COMMAND.get() == session) COMMAND.remove();
			if (PLACEMENT.get() == session) PLACEMENT.remove();
		}
	}

	/** Strings and immutable positions only; neither a start nor a piece is retained after the callback. */
	static final class Session implements AutoCloseable {
		private Object world;
		private Thread owner = Thread.currentThread();
		final long seed;
		final int minY;
		private final BlockPos origin;
		private final Consumer<String> sink;
		private volatile boolean closed;
		boolean commandReturned;
		private boolean scanned;
		Boolean valid;
		Integer commandResult;
		int placementCalls, startCalls, archivePieces;
		private int commands, pieceCount, terrainCalls, lines, omitted, sinkFailures, observationFailures;
		private String startBounds = "unobserved", direction = "unobserved", bounds = "unobserved", terrain = "unobserved";
		private BlockPos entrance, expected, found;
		Resident afterCommand;
		private Resident atScan;

		Session(Object world, long seed, int minY, BlockPos origin, Consumer<String> sink) {
			this.world = world; this.seed = seed; this.minY = minY; this.origin = origin.immutable(); this.sink = sink;
			emit("BEGIN seed=" + seed + ", origin=" + position(origin) + ", commandLimit=1, candidates=12, waits=100/20/40"
				+ ", seedPolicy=unchanged_random_normal_world, biomePolicy=vanilla_place_command_unfiltered, scan=" + scanBounds());
		}

		BlockPos origin() { return origin; }
		String commandText() { return "place structure wildercord:archive " + origin.getX() + " " + origin.getY() + " " + origin.getZ(); }

		CommandScope command() {
			if (closed || owner != Thread.currentThread() || COMMAND.get() != null || commands != 0)
				throw new IllegalStateException("Archive observation must enclose exactly the original server-thread command");
			commands++;
			COMMAND.set(this);
			return new CommandScope(this);
		}

		void runCommand(MinecraftServer server) {
			// This is TestServerContextImpl.runCommand's original implementation, with no new callback/source options.
			try (var ignored = command()) {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), commandText());
				commandReturned = true;
			}
		}

		void terrain(int x, int z, String map, int height) {
			if (terrainCalls++ == 0) terrain = x + "/" + height + "/" + z + ":" + map;
		}

		void piece(String direction, String bounds, BlockPos entrance, BlockPos expected) {
			this.direction = direction; this.bounds = bounds; this.entrance = entrance.immutable(); this.expected = expected.immutable();
		}

		private Resident resident(ServerLevel level) {
			if (closed || world != level || owner != Thread.currentThread() || expected == null) return null;
			try {
				boolean scanLoaded = level.isLoaded(expected);
				var chunk = level.getChunkSource().getChunkNow(expected.getX() >> 4, expected.getZ() >> 4);
				if (chunk == null) return new Resident(scanLoaded, false, "unloaded", false);
				var state = chunk.getBlockState(expected);
				return new Resident(scanLoaded, true, state.toString(), state.is(WildercordBlocks.ARCHIVE_LECTERN));
			} catch (RuntimeException failure) { observationFailures++; return null; }
		}

		BlockPos scanned(ServerLevel level, BlockPos originalResult) {
			if (closed || world != level || owner != Thread.currentThread()) return originalResult;
			observeScan(originalResult, resident(level));
			return originalResult;
		}

		void observeScan(BlockPos originalResult, Resident resident) {
			if (closed || scanned) return;
			scanned = true; found = originalResult == null ? null : originalResult.immutable(); atScan = resident;
			emit("SCAN found=" + position(found) + ", expected=" + position(expected) + ", insideXZ=" + insideXZ() + ", insideY=" + insideY()
				+ ", resident=" + atScan + ", stateChanged=" + changed() + ", outcome=" + outcome() + ", originalBounds=" + scanBounds());
		}

		boolean insideXZ() { return expected != null && Math.abs((long) expected.getX() - origin.getX()) <= 100 && Math.abs((long) expected.getZ() - origin.getZ()) <= 100; }
		boolean insideY() { return expected != null && expected.getY() >= minY + 1 && expected.getY() < origin.getY() + 10; }
		boolean changed() { return afterCommand != null && atScan != null && afterCommand.resident && atScan.resident && !afterCommand.state.equals(atScan.state); }
		String outcome() {
			if (!commandReturned) return "command_did_not_return";
			if (placementCalls != 1) return "placement_call_count_" + placementCalls;
			if (commandResult == null) return "placement_threw";
			if (commandResult != 1) return "placement_result_" + commandResult;
			if (startCalls != 1 || !Boolean.TRUE.equals(valid) || archivePieces != 1 || expected == null) return "generated_piece_unresolved";
			if (!scanned) return "original_scan_pending";
			if (afterCommand == null || atScan == null) return "resident_observation_unavailable";
			if (!atScan.resident || !atScan.scanLoaded) return "lectern_chunk_unloaded_at_scan";
			if (!afterCommand.resident) return "lectern_chunk_unloaded_after_command";
			if (afterCommand.lectern && !atScan.lectern) return "lectern_changed_after_command";
			if (!afterCommand.lectern && !atScan.lectern) return "lectern_absent_after_successful_command";
			if (!afterCommand.lectern) return "lectern_appeared_after_command";
			if (!insideY()) return "authentic_lectern_outside_original_y";
			if (!insideXZ()) return "authentic_lectern_outside_original_xz";
			if (found == null) return "authentic_lectern_present_but_scan_missed";
			return found.equals(expected) ? "original_scan_found_expected_lectern" : "original_scan_found_other_lectern";
		}
		String generation() {
			return "seed=" + seed + ", origin=" + position(origin) + ", startCalls=" + startCalls + ", valid=" + valid
				+ ", pieces=" + pieceCount + ", archivePieces=" + archivePieces + ", startBounds=" + startBounds + ", facing=" + direction
				+ ", pieceBounds=" + bounds + ", entranceAtPieceFloor=" + position(entrance) + ", terrainCalls=" + terrainCalls
				+ ", originalEntranceTerrain=" + terrain + ", expectedLectern=" + position(expected);
		}
		String scanBounds() { return (origin.getX() - 100) + ".." + (origin.getX() + 100) + "/" + (minY + 1) + "..<" + (origin.getY() + 10) + "/" + (origin.getZ() - 100) + ".." + (origin.getZ() + 100); }
		void emit(String line) {
			if (closed) return;
			if (lines++ >= MAX_LINES) { omitted++; return; }
			send("ARCHIVE_PLACEMENT " + line);
		}
		private void send(String line) { try { sink.accept(line); } catch (Throwable ignored) { sinkFailures++; } }
		@Override public void close() {
			if (closed) return;
			try { send("ARCHIVE_PLACEMENT END outcome=" + outcome() + ", commands=" + commands + ", placementCalls=" + placementCalls
				+ ", scanned=" + scanned + ", omittedLines=" + omitted + ", sinkFailures=" + sinkFailures + ", observationFailures=" + observationFailures); }
			finally { closed = true; world = null; owner = null; }
		}
	}

	static record Resident(boolean scanLoaded, boolean resident, String state, boolean lectern) {}
	static String position(BlockPos pos) { return pos == null ? "unobserved" : pos.getX() + "/" + pos.getY() + "/" + pos.getZ(); }
}
