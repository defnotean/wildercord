package dev.wildercord.wildlife;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Passive, single-journey proof of vanilla's separate waypoint-timeout termination. */
public final class NewtBlockedTimeoutProbe {
	private static volatile Session active;
	private NewtBlockedTimeoutProbe() {}

	static Session begin(LanternNewt actor, BlockPos destination, IntSupplier pearls) {
		var capture = new NativeCapture(actor, destination, pearls);
		return install(actor.getNavigation(), capture, () -> {
			var test = FabricClientGameTestRunner.currentlyRunningGameTest;
			return test != null && EcologyReturnProbe.REED.equals(test.getDefinition());
		});
	}

	static synchronized Session install(Object navigation, Supplier<Snapshot> capture, BooleanSupplier selected) {
		if (active != null) throw new AssertionError("A blocked-newt journey is already being observed");
		var session = new Session(navigation, capture, selected);
		active = session;
		return session;
	}

	/** WrapMethod supplies no receiver argument. The native body always runs once, even if observation fails. */
	public static void timeout(Object navigation, Operation<Void> original) {
		var session = active;
		Snapshot entry = null;
		if (session != null) {
			try {
				if (session.accepts(navigation)) entry = session.capture.get();
			} catch (Throwable failure) { session.latch(failure); }
		}
		boolean returned = false;
		try {
			original.call();
			returned = true;
		} finally {
			if (entry != null) {
				try {
					if (session.accepts(navigation)) {
						if (returned) session.receipt(entry, session.capture.get());
						else session.nativeFailures++;
					}
				} catch (Throwable failure) { session.latch(failure); }
			}
		}
	}

	/** Immutable reads let negative controls use the exact receipt and lifetime predicates. */
	record Snapshot(Object world, Object actor, Object navigation, Object goal, Object path,
		Object admittedPath, Object destination, int tick, int remaining, boolean running,
		boolean enclosed, boolean arrived, boolean resting, boolean beneath, int settled,
		long refugeReady, long pearlReady, long responseReady, long browseReady, int pearls, float health,
		boolean done, boolean following, boolean stuck, long timer, double limit, boolean cacheReset) {}

	static final class Session implements AutoCloseable {
		private Object navigation;
		private Supplier<Snapshot> capture;
		private BooleanSupplier selected;
		private Thread owner;
		private Snapshot initial;
		private Throwable observationFailure;
		final int originalTick, originalRemaining, deadline;
		private boolean armed, closed, expired, displacementStuck;
		private int timeouts, nativeFailures;
		private String timeoutReceipt = "none";

		Session(Object navigation, Supplier<Snapshot> capture, BooleanSupplier selected) {
			this.navigation = navigation;
			this.capture = capture;
			this.selected = selected;
			owner = Thread.currentThread();
			initial = capture.get();
			originalTick = initial.tick();
			originalRemaining = initial.remaining();
			deadline = Math.addExact(originalTick, originalRemaining);
			check(initial.navigation() == navigation && initial.running() && initial.path() != null
				&& initial.path() == initial.admittedPath() && initial.destination() != null
				&& initial.following() && !initial.done() && originalRemaining > 0 && originalRemaining <= 240
				&& !initial.arrived() && !initial.resting() && !initial.beneath() && initial.settled() == 0,
				"Capture the first live original Shelter route before enclosing it");
		}

		void arm(BlockPos cage) {
			if (!(capture instanceof NativeCapture nativeCapture)) throw new AssertionError("Native enclosure required");
			nativeCapture.enclose(cage);
			arm();
		}

		void arm() {
			check(!armed && !closed && active == this, "Only the current journey may be armed");
			var now = capture.get();
			check(liveJourney(now) && unchanged(now) && now.enclosed() && now.path() == initial.path()
				&& now.following() && !now.done(), "Enclosure preserves the admitted body, path and original deadline");
			armed = true;
		}

		private boolean accepts(Object candidate) {
			return active == this && !closed && armed && owner == Thread.currentThread()
				&& candidate == navigation && selected.getAsBoolean();
		}

		private boolean sameBody(Snapshot now) {
			return now.world() == initial.world() && now.actor() == initial.actor()
				&& now.navigation() == navigation && now.goal() == initial.goal();
		}

		private boolean liveJourney(Snapshot now) {
			return sameBody(now) && now.running() && now.admittedPath() == initial.admittedPath()
				&& Objects.equals(now.destination(), initial.destination()) && now.tick() >= originalTick
				&& (long) now.tick() + now.remaining() == deadline;
		}

		private boolean unchanged(Snapshot now) {
			return !now.arrived() && !now.resting() && !now.beneath() && now.settled() == 0
				&& now.refugeReady() == initial.refugeReady() && now.pearlReady() == initial.pearlReady()
				&& now.responseReady() == initial.responseReady() && now.browseReady() == initial.browseReady()
				&& now.pearls() == initial.pearls() && Float.compare(now.health(), initial.health()) == 0;
		}

		private void receipt(Snapshot before, Snapshot after) {
			if (!liveJourney(before) || !unchanged(before) || !before.enclosed()
				|| before.path() != initial.path() || before.done() || !before.following()
				|| before.remaining() <= 0 || before.tick() >= deadline
				|| !Double.isFinite(before.limit()) || before.limit() <= 0 || before.timer() <= 3 * before.limit()
				|| !liveJourney(after) || !unchanged(after) || !after.enclosed()
				|| after.tick() != before.tick() || after.remaining() != before.remaining()
				|| after.path() != null || !after.done() || after.following() || after.stuck()
				|| after.timer() != 0 || after.limit() != 0 || !after.cacheReset()) return;
			timeouts++;
			timeoutReceipt = "tick=" + before.tick() + " timer=" + before.timer() + " limit=" + before.limit()
				+ " originalDeadline=" + deadline + " remaining=" + before.remaining()
				+ " nativeReset=true nativeStop=true";
		}

		private void latch(Throwable failure) { if (observationFailure == null) observationFailure = failure; }

		/** Called by the fixture outside the native navigation/physics call stack. */
		boolean sample() {
			check(!closed && armed && active == this && owner == Thread.currentThread(), "Current armed journey required");
			if (observationFailure != null) throw new AssertionError("Blocked-newt observer failed", observationFailure);
			check(nativeFailures == 0, "An exceptional native timeout is not a successful receipt");
			var now = capture.get();
			check(sameBody(now) && now.enclosed() && unchanged(now), "Blocked original body cannot arrive, gain rewards or escape its enclosure");
			if (now.running()) {
				check(liveJourney(now) && now.tick() <= deadline + 2, "The original Shelter deadline cannot reset, extend or run indefinitely");
				if (now.stuck() && now.done() && !now.following() && now.tick() < deadline) displacementStuck = true;
			} else {
				check(now.tick() >= deadline && now.tick() <= deadline + 2 && now.remaining() <= 0
					&& now.destination() == null && now.admittedPath() == null && now.path() == null
					&& now.done() && !now.following(), "First Shelter stop must be its original finite expiry with native route cleanup");
				expired = true;
			}
			return expired;
		}

		void verify() {
			check(!closed && active == this && observationFailure == null && nativeFailures == 0,
				"Only a clean, live observer may verify this journey");
			check(expired && (displacementStuck || timeouts > 0),
				"Blocked journey requires native displacement-stuck or a qualifying original waypoint timeout, and original finite expiry"
					+ ": stuck=" + displacementStuck + ", timeouts=" + timeouts + ", expired=" + expired);
			if (capture instanceof NativeCapture) System.out.println("NEWT_BLOCKED_TIMEOUT " + timeoutReceipt + " displacementStuck=" + displacementStuck + " expired=" + expired);
		}

		int timeoutCount() { return timeouts; }
		boolean cleared() { return closed && navigation == null && capture == null && selected == null && owner == null && initial == null && observationFailure == null; }
		@Override public void close() {
			synchronized (NewtBlockedTimeoutProbe.class) {
				if (closed) return;
				closed = true;
				if (active == this) active = null;
				navigation = null; capture = null; selected = null; owner = null; initial = null; observationFailure = null;
			}
		}
	}

	private static final class NativeCapture implements Supplier<Snapshot> {
		private final LanternNewt actor;
		private final PathNavigation navigation;
		private final Object goal;
		private final BlockPos destination;
		private final IntSupplier pearls;
		private BlockPos cage;
		private BlockState floor;
		NativeCapture(LanternNewt actor, BlockPos destination, IntSupplier pearls) {
			this.actor = actor; navigation = actor.getNavigation(); this.destination = destination.immutable(); this.pearls = pearls;
			goal = actor.getGoalSelector().getAvailableGoals().stream()
				.filter(g -> g.isRunning() && g.getGoal().getClass().getSimpleName().equals("Shelter"))
				.findFirst().orElseThrow().getGoal();
			check(read(navigation, "mob") == actor && destination.equals(read(goal, "roof"))
				&& navigation.getPath() != null && destination.equals(navigation.getPath().getTarget()),
				"Observe this exact newt and its retained refuge destination");
		}
		void enclose(BlockPos cage) { this.cage = cage.immutable(); floor = actor.level().getBlockState(cage.below()); }
		private boolean enclosed() {
			if (cage == null || cage.equals(destination) || !actor.blockPosition().equals(cage)
				|| !actor.level().getBlockState(cage.below()).equals(floor)
				|| !actor.level().getBlockState(cage.above()).is(Blocks.GLASS)
				|| !actor.level().noCollision(actor, actor.getBoundingBox())) return false;
			for (var direction : Direction.Plane.HORIZONTAL)
				if (!actor.level().getBlockState(cage.relative(direction)).is(Blocks.GLASS)) return false;
			var roof = actor.level().getBlockState(destination);
			return roof.is(WetlandShelters.REFUGE) && roof.getValue(BlockStateProperties.WATERLOGGED);
		}
		@Override public Snapshot get() {
			return new Snapshot(actor.level(), read(navigation, "mob"), actor.getNavigation(), goal, navigation.getPath(),
				read(goal, "route"), read(goal, "roof"), actor.tickCount, (int) read(goal, "travelLeft"),
				actor.getGoalSelector().getAvailableGoals().stream().anyMatch(g -> g.getGoal() == goal && g.isRunning()),
				enclosed(), (boolean) read(goal, "arrived"), actor.resting(), actor.beneathRefuge(destination), (int) read(goal, "settled"),
				actor.refugeReady(), actor.pearlReady(), actor.responseReady(), (long) read(actor, "browseReady"), pearls.getAsInt(), actor.getHealth(),
				navigation.isDone(), ((NewtPathNavigation) navigation).followingRefuge(), navigation.isStuck(),
				(long) read(navigation, "timeoutTimer"), (double) read(navigation, "timeoutLimit"), Vec3i.ZERO.equals(read(navigation, "timeoutCachedNode")));
		}
	}

	private static Object read(Object object, String name) {
		for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
			try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(object); }
			catch (NoSuchFieldException ignored) {}
			catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
		}
		throw new IllegalStateException("Missing blocked-newt observation field " + name);
	}
	private static void check(boolean okay, String why) { if (!okay) throw new AssertionError(why); }
}
