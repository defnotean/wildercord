package dev.wildercord.wildlife;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Minecraft-free negative controls for the real probe's scope, forwarding, bounds, and cleanup core. */
public final class GlimmerwingLanternProbeChecks {
	private static int checks;
	public static void main(String[] args) {
		arrivalControls();
		Object world = new Object(), actor = new Object(), scene = new Object();
		var output = new ArrayList<String>();
		var calls = new AtomicInteger();
		var observed = new AtomicInteger();
		var outer = GlimmerwingLanternProbe.open(world, actor, scene, output::add);
		try {
			check(outer.accepts(world, actor, scene), "exact world, actor, scene admitted");
			check(!outer.accepts(new Object(), actor, scene), "other world excluded");
			check(!outer.accepts(world, new Object(), scene), "other actor excluded");
			check(!outer.accepts(world, actor, new Object()), "other scene excluded");
			int returned = GlimmerwingLanternProbe.aroundSearch(world, actor, () -> {
				for (int i = 0; i < 5; i++) {
					int expected = i;
					int value = GlimmerwingLanternProbe.forwardLight(world, actor, () -> { calls.incrementAndGet(); return expected; }, light -> {
						observed.incrementAndGet(); return "light=" + light;
					});
					check(value == expected, "original light value unchanged");
				}
				return 42;
			});
			check(returned == 42 && calls.get() == 5 && observed.get() == 5 && outer.lightReads == 5, "five real reads, exactly once each");
			GlimmerwingLanternProbe.aroundSearch(world, actor, () -> {
				Object stranger = new Object(), otherWorld = new Object();
				GlimmerwingLanternProbe.aroundSearch(world, stranger, () -> {
					forward(world, stranger, calls, observed); forward(world, actor, calls, observed); return null;
				});
				GlimmerwingLanternProbe.aroundSearch(otherWorld, actor, () -> { forward(otherWorld, actor, calls, observed); return null; });
				check(observed.get() == 5 && calls.get() == 8, "unrelated nested worlds and actors forward without receipts");
				try (var inner = GlimmerwingLanternProbe.open(world, actor, new Object(), row -> {})) {
					check(!outer.accepts(world, actor, scene), "outer scene suspended");
					forward(world, actor, calls, observed);
					check(observed.get() == 5, "new scene cannot borrow outer search frame");
					GlimmerwingLanternProbe.aroundSearch(world, actor, () -> { forward(world, actor, calls, observed); return null; });
					check(inner.lightReads == 1 && outer.lightReads == 5, "inner receipt isolated");
				}
				forward(world, actor, calls, observed);
				check(outer.lightReads == 6 && observed.get() == 7 && calls.get() == 11, "nested close restores original search and scene");
				var boom = new IllegalStateException("original search exception");
				try {
					GlimmerwingLanternProbe.aroundSearch(world, new Object(), () -> { calls.incrementAndGet(); throw boom; });
					throw new AssertionError("missing exception");
				} catch (IllegalStateException actual) { check(actual == boom, "same original exception escapes"); }
				forward(world, actor, calls, observed);
				check(outer.lightReads == 7 && calls.get() == 13, "exception restores outer search and original ran once");
				return null;
			});
			forward(world, actor, calls, observed);
			check(outer.lightReads == 7 && calls.get() == 14, "search frame removed after return");
			var boom = new IllegalArgumentException("original brightness exception");
			try {
				GlimmerwingLanternProbe.aroundSearch(world, actor, () -> GlimmerwingLanternProbe.forwardLight(world, actor,
					() -> { calls.incrementAndGet(); throw boom; }, light -> { throw new AssertionError("must not observe failed original"); }));
				throw new AssertionError("missing exception");
			} catch (IllegalArgumentException actual) { check(actual == boom && calls.get() == 15, "brightness exception unchanged and exactly one call"); }
			int originalValue = GlimmerwingLanternProbe.aroundSearch(world, actor, () -> GlimmerwingLanternProbe.forwardLight(world, actor,
				() -> { calls.incrementAndGet(); return 73; }, light -> { throw new LinkageError("broken optional observer"); }));
			check(originalValue == 73 && calls.get() == 16 && outer.errors == 1, "observer failure preserves original result and count");
			outer.decide("arrived=false finalAttempt=25");
			GlimmerwingLanternProbe.aroundSearch(world, actor, () -> { forward(world, actor, calls, observed); return null; });
			check(outer.lightReads == 8 && calls.get() == 17, "decision freezes receipts while calls keep forwarding");
		} finally { outer.close(); }
		int flushed = output.size(); outer.close();
		check(flushed == output.size() && flushed > 1 && !outer.accepts(world, actor, scene), "cleanup is terminal and idempotent");
		var bound = GlimmerwingLanternProbe.open(world, actor, scene, row -> {});
		for (int i = 0; i < GlimmerwingLanternProbe.MAX_ROWS + 7; i++) bound.record(() -> "bounded row");
		check(bound.rows.size() == GlimmerwingLanternProbe.MAX_ROWS && bound.dropped == 7, "finite buffer and exact drop count");
		try {
			try (var broken = GlimmerwingLanternProbe.open(world, actor, new Object(), row -> { throw new AssertionError("broken sink"); })) {
				throw new IllegalStateException("scene body failure");
			}
		} catch (IllegalStateException expected) { check(bound.accepts(world, actor, scene), "exception and sink failure restore outer scene"); }
		bound.close();
		GlimmerwingLanternProbe.aroundSearch(world, actor, () -> { forward(world, actor, calls, observed); return null; });
		check(calls.get() == 18 && observed.get() == 8, "outside every scene forwards once without observations");
		try (var budget = GlimmerwingLanternProbe.open(world, actor, scene, row -> {})) {
			GlimmerwingLanternProbe.aroundSearch(world, actor, () -> {
				for (int i = 0; i < 55; i++) GlimmerwingLanternProbe.forwardLight(world, actor, () -> 12, value -> "light=" + value);
				return null;
			});
			check(budget.lightReads == 55 && budget.maxSearchLightReads == 55 && budget.lightHistogram[12] == 55,
					"Every real read contributes to exact aggregate budget evidence");
			check(budget.rows.size() == 5 && budget.summarizedLightReads == 50 && budget.dropped == 0,
					"Bounded detail summarizes all other light reads without dropped evidence");
			budget.arrival.arrived = 3;
			check(GlimmerwingLanternProbe.arrived(budget), "Qualified receipt with a valid read budget admitted");
			budget.maxSearchLightReads = 56;
			check(!GlimmerwingLanternProbe.arrived(budget), "Read-budget violation cannot certify the native scene");
		}
		System.out.println("GlimmerwingLanternProbeChecks: " + checks + " controls passed");
	}
	private static void arrivalControls() {
		var proof = new GlimmerwingLanternProbe.Arrival();
		Object lantern = new Object(), other = new Object();
		proof.flight(25, null, true, 2.58);
		check(proof.arrived < 0, "The real baseline's incidental near pass cannot establish attraction");
		proof.acquisition(30, lantern, false); proof.target(31, lantern, true); proof.flight(32, lantern, true, 2);
		check(proof.arrived < 0, "Dark or ineligible light supplies no acquisition even with close flight");
		proof.acquisition(40, lantern, true); proof.flight(41, lantern, true, 2);
		check(proof.arrived < 0, "Acquisition still requires a real lured target selection");
		proof.target(42, lantern, false); proof.flight(43, lantern, true, 2);
		check(proof.arrived < 0, "Flower or wandering target branch cannot qualify");
		proof.target(44, lantern, true); proof.flight(44, lantern, true, 2);
		check(proof.arrived < 0, "Same-tick target selection is not later natural arrival");
		proof.flight(45, other, true, 2); proof.flight(45, lantern, false, 2); proof.flight(45, lantern, true, 3.21);
		check(proof.arrived < 0, "Wrong lure, invalid body, and outside-radius samples are excluded");
		proof.acquisition(46, other, true); proof.flight(47, other, true, 2);
		check(proof.arrived < 0, "A replaced lure must produce its own flight target");
		proof.target(48, other, true); proof.acquisition(49, other, false); proof.flight(50, other, true, 2);
		check(proof.arrived < 0, "Lost acquisition cannot borrow an earlier lured target");
		proof.acquisition(51, lantern, true); proof.target(52, lantern, true); proof.flight(53, lantern, true, 3.2);
		check(proof.arrived == 53 && proof.firstAcquisition == 40 && proof.firstLuredTarget == 44,
				"Only complete acquisition, production target, and later arrival evidence qualifies");
		proof = new GlimmerwingLanternProbe.Arrival();
		proof.acquisition(1, lantern, true); proof.target(2, lantern, true); proof.flight(3, lantern, false, 5);
		proof.acquisition(4, lantern, true); proof.flight(5, lantern, true, 2);
		check(proof.arrived < 0 && proof.luredTarget < 0, "Expiry followed by identical-coordinate reacquisition cannot reuse the old target receipt");
		proof.target(6, lantern, true); proof.flight(7, lantern, true, 2); proof.flight(8, null, false, 9);
		check(proof.arrived == 7, "Fresh target permits later arrival and subsequent expiry preserves the completed receipt");
	}
	private static void forward(Object world, Object actor, AtomicInteger calls, AtomicInteger observed) {
		int value = GlimmerwingLanternProbe.forwardLight(world, actor, () -> { calls.incrementAndGet(); return 11; }, light -> {
			observed.incrementAndGet(); return "light=" + light;
		});
		check(value == 11, "forwarded value");
	}
	private static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
}
