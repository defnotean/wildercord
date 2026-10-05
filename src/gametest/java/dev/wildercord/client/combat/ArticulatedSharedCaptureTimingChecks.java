package dev.wildercord.client.combat;

import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArtRules;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotOptionsImpl;

/** Pinned Fabric option/phase projection regression; no renderer, playback, or native coverage claim. */
public final class ArticulatedSharedCaptureTimingChecks {
	private ArticulatedSharedCaptureTimingChecks() {}

	public static void main(String[] args) {
		verify();
		System.out.println("Shared capture API/phase projection checks passed; native receipt coverage still required");
	}

	static void verify() {
		String name = "articulated_shared_timing_regression_requested_active";
		var defaults = (TestScreenshotOptionsImpl) TestScreenshotOptions.of(name);
		var capture = (TestScreenshotOptionsImpl) ArticulatedSharedPlayerChecks.screenshotOptions(name);
		check(defaults.deltaTicks == 1F, "Pinned Fabric default interpolation changed; re-audit capture timing");
		check(capture.deltaTicks == .5F && capture.deltaTicks == ArticulatedSharedPlayerChecks.CAPTURE_DELTA_TICKS,
			"Screenshot API must retain the exact pre-capture interpolation");
		check(capture.name.equals(name) && !capture.counterPrefix && capture.size == null && capture.destinationDir == null,
			"Interpolation correction retains the existing name, prefix, size and destination options");
		for (int move : new int[] {ArticulatedCombatPose.RISING_BREAK, ArticulatedCombatPose.DRIVING_CUT}) {
			var rule = MastersArtRules.move(move);
			for (boolean left : new boolean[] {false, true}) {
				for (int tick = 0; tick < rule.windup() + rule.recovery(); tick++) {
					var expected = tick < rule.windup() ? ArticulatedCombatPose.Phase.WINDUP
						: tick == rule.windup() ? ArticulatedCombatPose.Phase.ACTIVE : ArticulatedCombatPose.Phase.RECOVERY;
					var projected = ArticulatedCombatPose.samplePlayer(move, tick + capture.deltaTicks, rule.windup(), rule.recovery(), left);
					check(projected.phase() == expected, "Configured screenshot projection must retain every accepted tick's phase");
				}
				check(ArticulatedCombatPose.samplePlayer(move, rule.windup() + defaults.deltaTicks, rule.windup(), rule.recovery(), left).phase()
					== ArticulatedCombatPose.Phase.RECOVERY, "Control reproduces the measured ACTIVE-to-RECOVERY default-option miss");
				check(ArticulatedCombatPose.samplePlayer(move, rule.windup() + rule.recovery(), rule.windup(), rule.recovery(), left)
					== ArticulatedCombatPose.NONE, "Capture interpolation cannot extend the accepted expiry");
			}
		}
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
