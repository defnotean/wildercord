package dev.wildercord.aura;

import dev.wildercord.gametest.BraceNullPhaseContract;

/** Pure adversarial policy checks; no native renderer execution is claimed. */
public final class CheckBraceNullPhaseContract {
	private static int checks;
	public static void main(String[] args) {
		var check = new CheckBraceNullPhaseContract();
		check.actualPhaseAndCompletePoseAreRequiredForBothHands();
		check.activeCannotBorrowTheFollowingRecoveryTickOrAFrozenWindup();
		check.actorArtHandActionStateScreenshotAndBladeIdentityCannotBeBorrowed();
		System.out.println("Brace/Null phase-contract CPU checks passed: " + checks + "; native renderer not run");
	}
	private static void assertDoesNotThrow(Runnable action) { action.run(); checks++; }
	private static void assertThrows(Class<AssertionError> type, Runnable action) {
		try { action.run(); } catch (AssertionError expected) { checks++; return; }
		throw new AssertionError("Invalid phase evidence was accepted");
	}
	void actualPhaseAndCompletePoseAreRequiredForBothHands() {
		for (int move : new int[] {24, 25}) for (boolean left : new boolean[] {false, true}) {
			var rule = MastersStyleRules.animation(move);
			for (String phase : new String[] {"windup", "active", "recovery"}) {
				var id = identity(move, left, phase);
				float age = phase.equals("windup") ? rule.windup() * .5F : phase.equals("active") ? rule.windup() + .5F : rule.windup() + rule.recovery() * .6F;
				var sample = sample(id, age);
				assertDoesNotThrow(() -> BraceNullPhaseContract.require(id, sample));
				for (float invalid : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY, rule.windup() + rule.recovery()})
					assertThrows(AssertionError.class, () -> BraceNullPhaseContract.require(id, sample(id, invalid)));
				assertThrows(AssertionError.class, () -> BraceNullPhaseContract.require(id,
					new BraceNullPhaseContract.Sample(id, 1, id.owner(), move, id.activation(), left, age, MastersArtAnimation.NONE, sample.bladeTilt())));
			}
		}
	}
	void activeCannotBorrowTheFollowingRecoveryTickOrAFrozenWindup() {
		for (int move : new int[] {24, 25}) {
			var id = identity(move, false, "active");
			for (float age : new float[] {id.windup() - .001F, id.windup() + 1, id.windup() + 1.5F})
				assertThrows(AssertionError.class, () -> BraceNullPhaseContract.require(id, sample(id, age)));
			assertDoesNotThrow(() -> BraceNullPhaseContract.require(id, sample(id, id.windup())));
			assertDoesNotThrow(() -> BraceNullPhaseContract.require(id, sample(id, Math.nextDown((float) id.windup() + 1))));
		}
	}
	void actorArtHandActionStateScreenshotAndBladeIdentityCannotBeBorrowed() {
		var id = identity(24, false, "active"); var good = sample(id, 6.5F);
		var foreign = new BraceNullPhaseContract.Identity("foreign-run", "other.png", 2, "other-uuid", 99, 25, 101, true, false, 4, 14, "active");
		for (var wrong : new BraceNullPhaseContract.Sample[] {
			new BraceNullPhaseContract.Sample(foreign, 1, 7, 24, 100, false, 6.5F, good.pose(), good.bladeTilt()),
			new BraceNullPhaseContract.Sample(id, 0, 7, 24, 100, false, 6.5F, good.pose(), good.bladeTilt()),
			new BraceNullPhaseContract.Sample(id, 1, 8, 24, 100, false, 6.5F, good.pose(), good.bladeTilt()),
			new BraceNullPhaseContract.Sample(id, 1, 7, 25, 100, false, 6.5F, good.pose(), good.bladeTilt()),
			new BraceNullPhaseContract.Sample(id, 1, 7, 24, 101, false, 6.5F, good.pose(), good.bladeTilt()),
			new BraceNullPhaseContract.Sample(id, 1, 7, 24, 100, true, 6.5F, good.pose(), good.bladeTilt()),
			new BraceNullPhaseContract.Sample(id, 1, 7, 24, 100, false, 6.5F, good.pose(), 0)})
			assertThrows(AssertionError.class, () -> BraceNullPhaseContract.require(id, wrong));
	}
	private static BraceNullPhaseContract.Identity identity(int move, boolean left, String phase) {
		var s = MastersStyleRules.animation(move);
		return new BraceNullPhaseContract.Identity("run", "masters_style_" + s.art() + (left ? "_left_turn_first_" : "_first_") + phase, 1, "owner-uuid", 7, move, 100, left, true, s.windup(), s.recovery(), phase);
	}
	private static BraceNullPhaseContract.Sample sample(BraceNullPhaseContract.Identity id, float age) {
		var pose = MastersArtAnimation.sample(id.move(), age, id.windup(), id.recovery());
		return new BraceNullPhaseContract.Sample(id, 1, id.owner(), id.move(), id.activation(), id.left(), age, pose,
			MastersArtAnimation.bladeTilt(id.move(), age, id.windup(), pose.weight()));
	}
}
