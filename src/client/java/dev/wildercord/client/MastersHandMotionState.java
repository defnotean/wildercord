package dev.wildercord.client;

import net.minecraft.world.item.ItemStack;

/** Extraction-time provenance: a real equip/use transition must retain its native hand motion. */
public interface MastersHandMotionState {
	boolean wildercord$mainHandEquipping();
	void wildercord$mainHandEquipping(boolean value);
	ItemStack wildercord$mainHandItem();
	float wildercord$mainHandHeight();
	float wildercord$oldMainHandHeight();
}
