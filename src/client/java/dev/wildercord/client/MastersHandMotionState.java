package dev.wildercord.client;

/** Extraction-time provenance: a real equip/use transition must retain its native hand motion. */
public interface MastersHandMotionState {
	boolean wildercord$mainHandEquipping();
	void wildercord$mainHandEquipping(boolean value);
}
