package dev.wildercord.wildlife;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Adversarial controls for the same passive receipt used by the real blocked swimmer. */
public final class NewtBlockedTimeoutProbeChecks {
	private NewtBlockedTimeoutProbeChecks() {}
	public static void main(String[] args) { verify(); System.out.println("NewtBlockedTimeoutProbeChecks passed"); }
	public static void verify() {
		validTimeoutAndExpiry();
		validDisplacementStuck();
		for (String boundary : new String[]{"entry", "return"}) {
			for (String field : new String[]{"world", "body", "navigation", "goal", "admittedPath", "destination", "enclosure",
				"arrived", "resting", "beneath", "settled", "refugeReward", "pearlReward", "responseReward", "browseReward", "pearls", "health",
				"resetDeadline", "extendedDeadline", "prematureDeadline"}) {
				rejectReceipt(boundary, field, state -> state.mutate(field));
			}
		}
		for (String field : new String[]{"foreignPath", "ordinaryCompletion", "zeroLimit", "negativeLimit", "nanLimit", "infiniteLimit", "atThreshold", "expiredTimer"})
			rejectReceipt("entry", field, state -> state.mutate(field));
		for (String field : new String[]{"foreignPath", "notStopped", "following", "stuck", "unresetTimer", "unresetLimit", "unresetNode", "differentTick"})
			rejectReceipt("return", field, state -> state.mutate(field));
		expiryAloneAndDeadlineChanges();
		forwardingAndObserverFailures();
		staleAndForeignScope();
	}

	private static void validTimeoutAndExpiry() {
		var state = new State();
		var session = state.open();
		try {
			state.entry();
			int[] calls = {0};
			NewtBlockedTimeoutProbe.timeout(state.navigation, args -> { check(args.length == 0, "Original private method has no arguments"); calls[0]++; state.returned(); return null; });
			check(calls[0] == 1 && session.timeoutCount() == 1 && !state.stuck, "Native waypoint timeout qualifies without displacement-stuck");
			check(!session.sample(), "Native stop alone does not expire Shelter");
			expectFailure(session::verify, "Timeout alone cannot pass without original finite expiry");
			state.expired(); check(session.sample(), "Original 240-tick journey expires"); session.verify();
		} finally { session.close(); }
		session.close(); check(session.cleared(), "Close is idempotent and clears all retained body/world/path/readers");
		expectFailure(session::verify, "Closed session cannot be reused");
	}

	private static void validDisplacementStuck() {
		var state = new State();
		try (var session = state.open()) {
			state.entry(); state.returned(); state.stuck = true;
			check(!session.sample() && session.timeoutCount() == 0, "Existing displacement-stuck branch is independent of waypoint timeout");
			state.expired(); session.sample(); session.verify();
		}
	}

	private static void rejectReceipt(String boundary, String label, Consumer<State> mutate) {
		var state = new State();
		try (var session = state.open()) {
			state.entry();
			if (boundary.equals("entry")) mutate.accept(state);
			int[] calls = {0};
			NewtBlockedTimeoutProbe.timeout(state.originalNavigation, args -> {
				calls[0]++; state.normalIdentity(); state.returned();
				if (boundary.equals("return")) mutate.accept(state);
				return null;
			});
			check(calls[0] == 1 && session.timeoutCount() == 0, "Refuse " + boundary + " " + label + " while forwarding once");
			state.normalIdentity(); state.expired(); session.sample();
			expectFailure(session::verify, "Rejected " + label + " plus generic expiry is still insufficient");
		}
	}

	private static void expiryAloneAndDeadlineChanges() {
		var state = new State();
		try (var session = state.open()) {
			state.entry(); state.returned(); session.sample(); state.expired(); session.sample();
			expectFailure(session::verify, "Generic navigation stop and expiry-only do not prove native blocked termination");
		}
		for (int delta : new int[]{-1, 1, 100}) {
			state = new State();
			try (var session = state.open()) {
				state.entry(); state.remaining += delta;
				expectFailure(session::sample, "Live original deadline cannot shorten, extend or reset");
			}
		}
		for (int tick : new int[]{301, 305}) {
			state = new State();
			try (var session = state.open()) {
				state.expired(); state.tick = tick;
				expectFailure(session::sample, "First stop cannot precede or extend the original deadline");
			}
		}
	}

	private static void forwardingAndObserverFailures() {
		int[] calls = {0};
		NewtBlockedTimeoutProbe.timeout(new Object(), args -> { calls[0]++; return null; });
		check(calls[0] == 1, "Unarmed native invocation is unchanged");
		for (boolean failOnReturn : new boolean[]{false, true}) {
			var state = new State();
			var marker = new AssertionError("observer failure");
			boolean[] fail = {false};
			Supplier<NewtBlockedTimeoutProbe.Snapshot> reader = () -> { if (fail[0]) throw marker; return state.snapshot(); };
			try (var session = NewtBlockedTimeoutProbe.install(state.navigation, reader, () -> true)) {
				state.enclosed = true; session.arm(); state.entry(); fail[0] = !failOnReturn;
				int before = calls[0];
				NewtBlockedTimeoutProbe.timeout(state.navigation, args -> { calls[0]++; state.returned(); fail[0] = true; return null; });
				check(calls[0] == before + 1 && session.timeoutCount() == 0, "Observer failures never interrupt or repeat native physics");
				fail[0] = false;
				try { session.sample(); throw new IllegalStateException("Observer error not asserted"); }
				catch (AssertionError observed) { check(observed.getCause() == marker, "Original observer failure is latched until fixture assertion"); }
			}
		}
		var state = new State();
		try (var session = state.open()) {
			state.entry(); var marker = new IllegalStateException("native timeout failed"); int before = calls[0];
			try { NewtBlockedTimeoutProbe.timeout(state.navigation, args -> { calls[0]++; throw marker; }); throw new AssertionError("Native failure swallowed"); }
			catch (IllegalStateException failure) { check(failure == marker, "Original native throwable identity is preserved"); }
			check(calls[0] == before + 1 && session.timeoutCount() == 0, "Exceptional native call runs once and cannot qualify");
			expectFailure(session::sample, "Native exception cannot be converted into success");
		}
	}

	private static void staleAndForeignScope() {
		var state = new State();
		try (var session = state.open()) {
			state.entry(); int[] calls = {0};
			NewtBlockedTimeoutProbe.timeout(new Object(), args -> { calls[0]++; return null; });
			check(calls[0] == 1 && session.timeoutCount() == 0, "Foreign navigation forwards without receiving credit");
			var thrown = new AtomicReference<Throwable>();
			var threadState = state;
			var thread = new Thread(() -> { try { NewtBlockedTimeoutProbe.timeout(threadState.navigation, args -> { calls[0]++; return null; }); } catch (Throwable failure) { thrown.set(failure); } });
			thread.start(); try { thread.join(); } catch (InterruptedException failure) { throw new AssertionError(failure); }
			check(thrown.get() == null && calls[0] == 2 && session.timeoutCount() == 0, "Foreign thread forwards without observing");
		}
		state = new State();
		try (var session = NewtBlockedTimeoutProbe.install(state.navigation, state::snapshot, () -> false)) {
			state.enclosed = true; session.arm(); state.entry(); var current = state;
			NewtBlockedTimeoutProbe.timeout(state.navigation, args -> { current.returned(); return null; });
			check(session.timeoutCount() == 0, "Foreign suite cannot earn a receipt");
		}
		var oldState = new State(); var old = oldState.open(); var nextState = new State();
		NewtBlockedTimeoutProbe.Session[] next = {null};
		try {
			oldState.entry();
			NewtBlockedTimeoutProbe.timeout(oldState.navigation, args -> { old.close(); next[0] = nextState.open(); oldState.returned(); return null; });
			check(old.cleared() && old.timeoutCount() == 0 && next[0].timeoutCount() == 0, "Late return cannot credit a stale or replacement session");
		} finally { old.close(); if (next[0] != null) next[0].close(); }
	}

	private static final class State {
		final Object originalWorld = new Object(), originalActor = new Object(), originalNavigation = new Object(), originalGoal = new Object(), originalPath = new Object(), originalDestination = new Object();
		Object world = originalWorld, actor = originalActor, navigation = originalNavigation, goal = originalGoal, path = originalPath, admittedPath = originalPath, destination = originalDestination;
		int tick = 62, remaining = 240, settled, pearls;
		boolean running = true, enclosed, arrived, resting, beneath, done, following = true, stuck, cacheReset;
		long refugeReady, pearlReady, responseReady, browseReady, timer;
		double limit;
		float health = 10;
		NewtBlockedTimeoutProbe.Session open() {
			var session = NewtBlockedTimeoutProbe.install(navigation, this::snapshot, () -> true);
			enclosed = true; session.arm(); return session;
		}
		void normalIdentity() {
			world = originalWorld; actor = originalActor; navigation = originalNavigation; goal = originalGoal;
			path = originalPath; admittedPath = originalPath; destination = originalDestination;
			enclosed = true; arrived = resting = beneath = stuck = false; settled = pearls = 0;
			refugeReady = pearlReady = responseReady = browseReady = 0; health = 10;
		}
		void entry() { tick = 243; remaining = 59; running = true; path = originalPath; following = true; done = false; timer = 160; limit = 53.0910569715; cacheReset = false; }
		void returned() { tick = 243; remaining = 59; running = true; path = null; done = true; following = false; stuck = false; timer = 0; limit = 0; cacheReset = true; }
		void expired() { tick = 302; remaining = 0; running = false; admittedPath = destination = path = null; done = true; following = false; }
		void mutate(String field) {
			switch (field) {
				case "world" -> world = new Object(); case "body" -> actor = new Object(); case "navigation" -> navigation = new Object();
				case "goal" -> goal = new Object(); case "foreignPath" -> path = new Object(); case "admittedPath" -> admittedPath = new Object();
				case "destination" -> destination = new Object(); case "enclosure" -> enclosed = false;
				case "arrived" -> arrived = true; case "resting" -> resting = true; case "beneath" -> beneath = true; case "settled" -> settled++;
				case "refugeReward" -> refugeReady++; case "pearlReward" -> pearlReady++; case "responseReward" -> responseReady++;
				case "browseReward" -> browseReady++; case "pearls" -> pearls++; case "health" -> health++;
				case "resetDeadline" -> remaining = 240; case "extendedDeadline" -> remaining++; case "prematureDeadline" -> remaining--;
				case "ordinaryCompletion" -> { done = true; following = false; }
				case "zeroLimit" -> limit = 0; case "negativeLimit" -> limit = -1; case "nanLimit" -> limit = Double.NaN;
				case "infiniteLimit" -> limit = Double.POSITIVE_INFINITY; case "atThreshold" -> { limit = 53; timer = 159; }
				case "expiredTimer" -> { tick = 302; remaining = 0; }
				case "notStopped" -> done = false; case "following" -> following = true; case "stuck" -> stuck = true;
				case "unresetTimer" -> timer = 1; case "unresetLimit" -> limit = 1; case "unresetNode" -> cacheReset = false; case "differentTick" -> { tick++; remaining--; }
				default -> throw new AssertionError("Unknown control " + field);
			}
		}
		NewtBlockedTimeoutProbe.Snapshot snapshot() {
			return new NewtBlockedTimeoutProbe.Snapshot(world, actor, navigation, goal, path, admittedPath, destination, tick, remaining,
				running, enclosed, arrived, resting, beneath, settled, refugeReady, pearlReady, responseReady, browseReady, pearls, health,
				done, following, stuck, timer, limit, cacheReset);
		}
	}
	private static void expectFailure(Runnable operation, String why) {
		boolean failed = false; try { operation.run(); } catch (AssertionError expected) { failed = true; }
		check(failed, why);
	}
	private static void check(boolean okay, String why) { if (!okay) throw new AssertionError(why); }
}
