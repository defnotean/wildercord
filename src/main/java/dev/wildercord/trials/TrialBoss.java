package dev.wildercord.trials;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/** Shared server contract for Sovereign Co-Op Trial bosses. */
public interface TrialBoss {
	void configureTrial(BlockPos center, BlockPos gate, BlockPos treasury, int partyCount, boolean apex);

	boolean isAlive();

	UUID getUUID();
}
