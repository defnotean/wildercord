package dev.wildercord.compat;

import dev.wildercord.mixin.MoveControlWaitAccessor;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Parks a mob's move control. 1.21.6 added {@code MoveControl#setWait}; 1.21.5 only has the
 * private operation field, which the accessor mixin reaches.
 */
public final class McMoveControl {

	private McMoveControl() {}

	/** Stops the mob walking anywhere until something gives it a new target. */
	public static void hold(MoveControl control) {
		((MoveControlWaitAccessor) control).wildercord$setOperation(MoveControl.Operation.WAIT);
	}
}
