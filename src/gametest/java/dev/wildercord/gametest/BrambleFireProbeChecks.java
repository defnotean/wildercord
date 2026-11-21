package dev.wildercord.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Negative scope, exact forwarding, bounded reads and cleanup checks; does not create or move any entity. */
public final class BrambleFireProbeChecks {
	private BrambleFireProbeChecks() {}

	public static void main(String[] args) {
		verify();
		System.out.println("BrambleFireProbeChecks passed");
	}

	public static void verify() {
		Object world = new Object(), actor = new Object(), player = new Object();
		Thread owner = Thread.currentThread();
		List<String> rows = new ArrayList<>();
		String suite = BrambleFireProbe.SUITE;
		var outer = BrambleFireProbe.install(suite, world, actor, player, owner, 100, rows::add);
		try {
			check(BrambleFireProbe.selected(suite, world, actor, owner, 100) == outer, "exact scope admitted");
			check(BrambleFireProbe.selected("other.Test", world, actor, owner, 100) == null, "other suite refused");
			check(BrambleFireProbe.selected(suite, new Object(), actor, owner, 100) == null, "other world refused");
			check(BrambleFireProbe.selected(suite, world, new Object(), owner, 100) == null, "other entity refused");
			check(BrambleFireProbe.selected(suite, world, actor, new Thread(), 100) == null, "other thread refused");
			check(BrambleFireProbe.selected(suite, world, actor, owner, 99) == null, "before ignition refused");
			check(BrambleFireProbe.selected(suite, world, actor, owner, 140) == outer, "existing deadline admitted");
			check(BrambleFireProbe.selected(suite, world, actor, owner, 141) == null, "after deadline refused");
			check(!outer.admitTick(100), "ignition is not an additional native tick");
			for (int i = 101; i <= 140; i++) {
				check(outer.admitTick(i), "native tick admitted");
				check(!outer.admitTick(i), "duplicate tick refused");
			}
			check(outer.ticks == 40 && !outer.admitTick(141), "forty native samples, never an extended deadline");
			var marker = new IllegalStateException("native failure");
			try {
				try (var inner = BrambleFireProbe.install(suite, new Object(), new Object(), player, owner, 100, line -> {})) {
					check(BrambleFireProbe.selected(suite, world, actor, owner, 100) == null, "nested scope masks outer");
					throw marker;
				}
			} catch (IllegalStateException failure) { check(failure == marker, "original exception identity"); }
			check(BrambleFireProbe.selected(suite, world, actor, owner, 100) == outer, "finally restores live outer scope");
			for (int i = 0; i < 1000; i++) outer.event("row=" + i);
			check(outer.events.size() == 128 && outer.eventCount == 1000 && rows.isEmpty(), "bounded buffered events");
			BrambleFireProbe.finish(outer);
			check(BrambleFireProbe.selected(suite, world, actor, owner, 100) == null, "measurement disarms immediately");
			outer.event("late");
			check(outer.eventCount == 1000, "no post-measurement event");
		} finally { BrambleFireProbe.close(outer); }
		check(rows.size() == 129 && rows.getLast().contains("omittedEvents=872"), "bounded flush with loss accounting");
		check(BrambleFireProbe.selected(suite, world, actor, owner, 100) == null, "cleanup removes scope");
		outer.close();
		check(rows.size() == 129 && outer.events.isEmpty() && outer.bodies.isEmpty(), "idempotent close releases rows");
		var stale = BrambleFireProbe.install(suite, world, actor, player, owner, 100, line -> {});
		var nested = BrambleFireProbe.install(suite, new Object(), new Object(), player, owner, 100, line -> {});
		stale.close(); nested.close();
		check(BrambleFireProbe.selected(suite, world, actor, owner, 100) == null, "out-of-order close never restores a closed scope");
		var sink = BrambleFireProbe.install(suite, world, actor, player, owner, 100, line -> { throw new IllegalStateException("sink"); });
		sink.event("buffered"); sink.close();
		check(sink.sinkFailures == 2 && BrambleFireProbe.selected(suite, world, actor, owner, 100) == null,
			"sink failure cannot escape cleanup");
		forwarding();
		paths();
		terrain();
	}

	private static void forwarding() {
		int[] picks = {0}, moves = {0};
		Vec3 away = new Vec3(1, 2, 3), destination = new Vec3(4, 5, 6);
		for (Vec3 result : new Vec3[] {null, destination}) {
			Vec3 actual = BrambleFireProbe.pick(null, 16, 6, away, args -> {
				picks[0]++;
				check(args.length == 4 && args[0] == null && (int) args[1] == 16 && (int) args[2] == 6 && args[3] == away,
					"random selection arguments and threat identity unchanged");
				return result;
			});
			check(actual == result, "null or chosen destination identity unchanged");
		}
		for (boolean result : new boolean[] {false, true}) {
			boolean actual = BrambleFireProbe.move(null, null, 4, 5, 6, 1.6, args -> {
				moves[0]++;
				check(args.length == 5 && args[0] == null && (double) args[1] == 4 && (double) args[2] == 5
					&& (double) args[3] == 6 && (double) args[4] == 1.6, "navigation receiver and arguments unchanged");
				return result;
			});
			check(actual == result, "move result unchanged");
		}
		var marker = new IllegalStateException("native call failed");
		try {
			BrambleFireProbe.pick(null, 16, 6, away, args -> { picks[0]++; throw marker; });
			throw new AssertionError("selection exception swallowed");
		} catch (IllegalStateException failure) { check(failure == marker, "selection exception unchanged"); }
		try {
			BrambleFireProbe.move(null, null, 4, 5, 6, 1.6, args -> { moves[0]++; throw marker; });
			throw new AssertionError("navigation exception swallowed");
		} catch (IllegalStateException failure) { check(failure == marker, "navigation exception unchanged"); }
		check(picks[0] == 3 && moves[0] == 3, "each original invoked exactly once, including failed calls");
	}

	private static void paths() {
		List<Node> nodes = new ArrayList<>(List.of(new Node(1, 101, 3), new Node(2, 102, 3), new Node(3, 101, 3)));
		BlockPos target = new BlockPos(9, 101, 3);
		Path path = new Path(nodes, target, false);
		path.setNextNodeIndex(1);
		String row = BrambleFireProbe.path(path);
		check(path.getNextNodeIndex() == 1 && path.getNodeCount() == 3 && path.getTarget() == target && !path.canReach(),
			"partial path and native cursor preserved");
		for (int i = 0; i < nodes.size(); i++) check(path.getNode(i) == nodes.get(i), "node identity preserved");
		check(row.contains("canReach=false") && row.contains("index=1") && row.contains("2,102,3"), "actual path progression retained");
		path.setNextNodeIndex(3);
		check(BrambleFireProbe.path(path).contains("next=none"), "completed path has no invalid next-node read");
		List<Node> longNodes = new ArrayList<>();
		for (int i = 0; i < 100; i++) longNodes.add(new Node(i, 101, 3));
		check(BrambleFireProbe.path(new Path(longNodes, target, true)).contains("omittedNodes=68"), "path serialization bounded");
	}

	private static void terrain() {
		int[] lookups = {0}, reads = {0};
		String absent = BrambleFireProbe.terrain(16, -60, 16, new BrambleFireProbe.ResidentReader() {
			public Object chunkNow(int x, int z) { lookups[0]++; return null; }
			public String state(Object chunk, int x, int y, int z) { reads[0]++; throw new AssertionError("unloaded state read"); }
		});
		check(lookups[0] == 9 && reads[0] == 0 && absent.contains("missingColumns=9"), "absent chunks never inspected or loaded");
		lookups[0] = 0;
		BrambleFireProbe.terrain(16, -60, 16, new BrambleFireProbe.ResidentReader() {
			public Object chunkNow(int x, int z) { lookups[0]++; return new int[] {x, z}; }
			public String state(Object chunk, int x, int y, int z) {
				reads[0]++;
				int[] coordinates = (int[]) chunk;
				check(coordinates[0] == x >> 4 && coordinates[1] == z >> 4, "read uses the exact resident chunk across boundaries");
				check(y >= -61 && y <= -57, "bounded collision neighborhood");
				return "block";
			}
		});
		check(lookups[0] == 9 && reads[0] == 45, "resident terrain read bound");
	}

	private static void check(boolean okay, String why) {
		if (!okay) throw new AssertionError(why);
	}
}
