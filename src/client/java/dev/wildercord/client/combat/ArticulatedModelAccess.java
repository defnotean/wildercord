package dev.wildercord.client.combat;

/** A renderer-local rig; resource reload creates a fresh owner and all of its CPU-only geometry. */
public interface ArticulatedModelAccess {
	ArticulatedRig wildercord$rig();
	ArticulatedViewModel wildercord$viewModel();
}
