package dev.wildercord.world.sites.trials;

import net.minecraft.core.BlockPos;

/** Shared contract for all Co-Op Trial Sanctum structure pieces. */
public interface TrialArenaPiece {
	/** Short trial identifier such as {@code "leyline"}, {@code "crucible"}, or {@code "eclipse"}. */
	String trialKind();

	/** Registered site structure path such as {@code "trial_leyline_sanctum"}. */
	String structureId();

	BlockPos gatePosition();

	BlockPos arenaCenter();

	BlockPos treasuryPosition();
}
