package dev.wildercord.gametest;

import dev.wildercord.aura.MastersArtAnimation;

/** Pure fail-closed policy for the pair's native Classic rendered-pose receipt. */
public final class BraceNullPhaseContract {
	private BraceNullPhaseContract() {}
	public record Identity(String run, String screenshot, long sequence, String ownerUuid, int owner, int move,
		long activation, boolean left, boolean firstPerson, int windup, int recovery, String requestedPhase) {}
	public record Sample(Identity identity, long stateIdentity, int actor, int move, long activation,
		boolean left, float renderedAge, MastersArtAnimation.Pose pose, float bladeTilt) {}

	public static void require(Identity expected, Sample observed) {
		check(expected != null && observed != null && expected.equals(observed.identity()), "capture identity changed");
		check(expected.run() != null && !expected.run().isBlank() && expected.sequence() > 0 && expected.ownerUuid() != null && !expected.ownerUuid().isBlank()
			&& (expected.move() == 24 || expected.move() == 25), "invalid pair identity");
		String art = expected.move() == 24 ? "unmoved" : "null_parry";
		String view = (expected.left() ? "left_turn_" : "") + (expected.firstPerson() ? "first" : "third_back");
		check(expected.screenshot().equals("masters_style_" + art + "_" + view + "_" + expected.requestedPhase()), "screenshot art/hand/view/phase identity differs");
		check(expected.windup() == (expected.move() == 24 ? 6 : 4) && expected.recovery() == (expected.move() == 24 ? 16 : 14), "pair window changed");
		check(observed.stateIdentity() > 0 && observed.actor() == expected.owner() && observed.move() == expected.move()
			&& observed.activation() == expected.activation() && observed.left() == expected.left(), "rendered actor/art/hand/action changed");
		float age = observed.renderedAge();
		check(Float.isFinite(age) && switch (expected.requestedPhase()) {
			case "windup" -> age >= 0 && age < expected.windup();
			case "active" -> age >= expected.windup() && age < expected.windup() + 1;
			case "recovery" -> age >= expected.windup() + expected.recovery() / 2F && age < expected.windup() + expected.recovery();
			default -> false;
		}, "actual post-HitStop phase differs from screenshot");
		var source = MastersArtAnimation.sample(expected.move(), age, expected.windup(), expected.recovery());
		check(source.weight() > 0 && source.equals(observed.pose()), "renderer did not consume the exact age's immutable pose");
		check(Float.floatToIntBits(observed.bladeTilt()) == Float.floatToIntBits(
			MastersArtAnimation.bladeTilt(expected.move(), age, expected.windup(), source.weight())), "rendered hilt tilt changed");
	}
	private static void check(boolean pass, String message) { if (!pass) throw new AssertionError("Brace/Null native phase: " + message); }
}
