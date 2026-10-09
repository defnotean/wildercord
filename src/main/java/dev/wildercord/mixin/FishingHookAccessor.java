package dev.wildercord.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The fishing pack's view of a bobber: how long until a fish comes, and whether one is biting now. */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {
	@Accessor("nibble") int wildercord$nibble();
	@Accessor("timeUntilLured") int wildercord$timeUntilLured();
	@Accessor("timeUntilLured") void wildercord$setTimeUntilLured(int ticks);
	@Accessor("timeUntilHooked") int wildercord$timeUntilHooked();
	@Accessor("timeUntilHooked") void wildercord$setTimeUntilHooked(int ticks);
}
