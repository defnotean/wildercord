package dev.wildercord.gametest;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.mixin.ArchiveTerrainProbeMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/** Standalone synthetic controls only. They do not claim a native generation outcome. */
public final class ArchivePlacementProbeChecks {
	private static final BlockPos ORIGIN = new BlockPos(337, 62, -67);
	private static final ArchivePlacementProbe.Resident LECTERN = new ArchivePlacementProbe.Resident(true, true, "lectern[awake=false]", true);
	private static final ArchivePlacementProbe.Resident AIR = new ArchivePlacementProbe.Resident(true, true, "air", false);
	private static final ArchivePlacementProbe.Resident UNLOADED = new ArchivePlacementProbe.Resident(false, false, "unloaded", false);
	private ArchivePlacementProbeChecks() {}

	public static void main(String[] args) throws InterruptedException {
		scopeChecks();
		classificationChecks();
		forwardingChecks();
		System.out.println("ARCHIVE_PLACEMENT CHECKS completed=scene_world_seed_structure_origin_thread_scope,nested_masking,one_command_bound,return_exception_cleanup,closed_scope,sink_failure,line_cap,classification,original_handler_forwarding; syntheticControlsOnly=true");
	}

	private static void scopeChecks() throws InterruptedException {
		var world = new Object(); var rows = new ArrayList<String>();
		var session = new ArchivePlacementProbe.Session(world, 721, -64, ORIGIN, rows::add);
		check(session.commandText().equals("place structure wildercord:archive 337 62 -67"), "Original command text retained");
		try (session; var command = session.command()) {
			check(ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN), "Exact command admitted");
			check(!ArchivePlacementProbe.matches(session, "other.Test", world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN), "Wrong suite refused");
			check(!ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, new Object(), 721, ArchivePlacementProbe.STRUCTURE, ORIGIN), "Wrong world refused");
			check(!ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, world, 722, ArchivePlacementProbe.STRUCTURE, ORIGIN), "Wrong seed refused");
			check(!ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, world, 721, "minecraft:village", ORIGIN), "Wrong structure refused");
			check(!ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN.above()), "Wrong exact origin refused");
			try { session.command(); throw new AssertionError("Nested command accepted"); } catch (IllegalStateException expected) {}
			var scope = ArchivePlacementProbe.placement(ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN);
			try {
				ArchivePlacementProbe.terrain(421, -60, Heightmap.Types.WORLD_SURFACE_WG, 99);
				var nested = ArchivePlacementProbe.placement("other.Test", world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN);
				try { ArchivePlacementProbe.terrain(1, 2, Heightmap.Types.OCEAN_FLOOR_WG, 777); } finally { nested.finish(1, null); }
				ArchivePlacementProbe.terrain(421, -60, Heightmap.Types.WORLD_SURFACE_WG, 99);
				var threadFailure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
				var other = Thread.ofPlatform().start(() -> {
					try {
					check(!ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN), "Wrong thread refused");
					var unrelated = ArchivePlacementProbe.placement(ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN);
					try { ArchivePlacementProbe.terrain(3, 4, Heightmap.Types.OCEAN_FLOOR_WG, 888); } finally { unrelated.finish(1, null); }
					} catch (Throwable failure) { threadFailure.set(failure); }
				});
				other.join();
				if (threadFailure.get() != null) throw new AssertionError("Other-thread control failed", threadFailure.get());
			} finally { scope.finish(1, null); }
			ArchivePlacementProbe.terrain(5, 6, Heightmap.Types.OCEAN_FLOOR_WG, 999);
			check(session.generation().contains("terrainCalls=2") && session.generation().contains("421/99/-60:WORLD_SURFACE_WG"), "Nested masking restores its parent; unrelated observations never replace the first original terrain return");
		}
		check(!ArchivePlacementProbe.matches(session, ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN), "Closed session refused");
		int count = rows.size(); session.emit("late"); session.observeScan(ORIGIN, AIR);
		check(rows.size() == count, "Closed session cannot emit or accept scans");

		var originalFailure = new IllegalStateException("original placement failure");
		try (var failed = new ArchivePlacementProbe.Session(world, 721, -64, ORIGIN, rows::add); var command = failed.command()) {
			var scope = ArchivePlacementProbe.placement(ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN);
			try { throw originalFailure; } finally { scope.finish(null, null); }
		} catch (IllegalStateException caught) { check(caught == originalFailure, "Original exception identity retained"); }
		try (var next = new ArchivePlacementProbe.Session(world, 721, -64, ORIGIN, rows::add)) {
			try (var command = next.command()) {}
			try { next.command(); throw new AssertionError("Second command accepted"); } catch (IllegalStateException expected) {}
			var unscoped = ArchivePlacementProbe.placement(ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN);
			try { ArchivePlacementProbe.terrain(1, 2, Heightmap.Types.OCEAN_FLOOR_WG, 777); } finally { unscoped.finish(1, null); }
			check(next.generation().contains("terrainCalls=0"), "No callback leaks after command return or previous exception");
		}
		var cappedRows = new ArrayList<String>();
		try (var capped = new ArchivePlacementProbe.Session(world, 721, -64, ORIGIN, cappedRows::add)) { for (int i = 0; i < 100; i++) capped.emit("synthetic"); }
		check(cappedRows.size() == ArchivePlacementProbe.MAX_LINES + 1 && cappedRows.getLast().contains("omittedLines=93"), "Line cap includes explicit omissions and final summary");
		try (var broken = new ArchivePlacementProbe.Session(world, 721, -64, ORIGIN, line -> { throw new IllegalStateException("sink"); }); var command = broken.command()) {
			var scope = ArchivePlacementProbe.placement(ArchivePlacementProbe.SUITE, world, 721, ArchivePlacementProbe.STRUCTURE, ORIGIN);
			try { ArchivePlacementProbe.terrain(421, -60, Heightmap.Types.WORLD_SURFACE_WG, 99); } finally { scope.finish(1, null); }
		}
		try (var clean = new ArchivePlacementProbe.Session(world, 721, -64, ORIGIN, rows::add); var command = clean.command()) { check(true, "Sink failure cleaned command scope"); }
	}

	private static void classificationChecks() {
		classify(71, LECTERN, LECTERN, new BlockPos(350, 71, -60), "original_scan_found_expected_lectern");
		classify(72, LECTERN, LECTERN, null, "authentic_lectern_outside_original_y");
		classify(71, LECTERN, UNLOADED, null, "lectern_chunk_unloaded_at_scan");
		classify(71, UNLOADED, LECTERN, null, "lectern_chunk_unloaded_after_command");
		classify(71, AIR, AIR, null, "lectern_absent_after_successful_command");
		classify(71, LECTERN, AIR, null, "lectern_changed_after_command");
		classify(71, AIR, LECTERN, null, "lectern_appeared_after_command");
		classify(71, LECTERN, LECTERN, null, "authentic_lectern_present_but_scan_missed");
		try (var missing = new ArchivePlacementProbe.Session(new Object(), 721, -64, ORIGIN, line -> {})) {
			missing.commandReturned = true; missing.placementCalls = 1; missing.commandResult = 1;
			missing.observeScan(ORIGIN, LECTERN);
			check(missing.outcome().equals("generated_piece_unresolved"), "An incidental lectern cannot substitute for missing generation evidence");
			missing.commandResult = null;
			check(missing.outcome().equals("placement_threw"), "Thrown placement cannot be turned into success by a scan");
		}
	}

	private static void classify(int y, ArchivePlacementProbe.Resident after, ArchivePlacementProbe.Resident scan, BlockPos found, String expectedOutcome) {
		try (var session = new ArchivePlacementProbe.Session(new Object(), 721, -64, ORIGIN, line -> {})) {
			session.commandReturned = true; session.placementCalls = 1; session.commandResult = 1;
			session.startCalls = 1; session.valid = true; session.archivePieces = 1;
			var mutable = new BlockPos.MutableBlockPos(350, y, -60);
			session.piece("west", "synthetic", new BlockPos(421, y - 3, -60), mutable);
			mutable.set(999, 999, 999);
			session.afterCommand = after; session.observeScan(found, scan);
			check(session.outcome().equals(expectedOutcome), "Classification: " + expectedOutcome + "; actual=" + session.outcome());
			check(session.changed() == (after.resident() && scan.resident() && !after.state().equals(scan.state())), "Change requires two resident observations");
			check(session.insideXZ(), "Expected immutable position is copied");
		}
	}

	private static void forwardingChecks() {
		int[] calls = {0};
		var handler = new ArchiveTerrainProbeMixin() {};
		Operation<Integer> height = args -> {
			calls[0]++;
			check(args.length == 6 && args[0] == null && (int) args[1] == 421 && (int) args[2] == -60
				&& args[3] == Heightmap.Types.WORLD_SURFACE_WG && args[4] == null && args[5] == null, "Original height arguments including random state forwarded unchanged");
			return 99;
		};
		check((int) invoke(ArchiveTerrainProbeMixin.class, handler, "wildercord$height", null, 421, -60, Heightmap.Types.WORLD_SURFACE_WG, null, null, height) == 99, "Original terrain return preserved");
		Operation<Boolean> valid = args -> { calls[0]++; check(args.length == 1 && args[0] == StructureStart.INVALID_START, "Actual start identity forwarded"); return false; };
		check(!(boolean) invoke(dev.wildercord.gametest.mixin.ArchivePlacementProbeMixin.class, null, "wildercord$start", StructureStart.INVALID_START, valid), "Original invalid-start result preserved");
		check(calls[0] == 2, "Each original handler operation runs once");
		var failure = new IllegalStateException("original height failure");
		Operation<Integer> throwing = args -> { throw failure; };
		try { invoke(ArchiveTerrainProbeMixin.class, handler, "wildercord$height", null, 421, -60, Heightmap.Types.WORLD_SURFACE_WG, null, null, throwing); throw new AssertionError("Original exception swallowed"); }
		catch (IllegalStateException caught) { check(caught == failure, "Original handler exception preserved"); }
	}

	private static Object invoke(Class<?> type, Object receiver, String name, Object... args) {
		try {
			var method = java.util.Arrays.stream(type.getDeclaredMethods()).filter(value -> value.getName().equals(name)).findFirst().orElseThrow();
			method.setAccessible(true); return method.invoke(receiver, args);
		} catch (InvocationTargetException error) {
			if (error.getCause() instanceof RuntimeException cause) throw cause;
			if (error.getCause() instanceof Error cause) throw cause;
			throw new AssertionError(error.getCause());
		} catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
