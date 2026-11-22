package dev.wildercord.mixin;

import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Parks a move control the way 1.21.6's {@code setWait} does; 1.21.5 keeps the field private. */
@Mixin(MoveControl.class)
public interface MoveControlWaitAccessor {

	@Accessor("operation")
	void wildercord$setOperation(MoveControl.Operation operation);
}
