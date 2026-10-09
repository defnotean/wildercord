package dev.wildercord.gametest.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Apply after Fabric's priority-1000 mixin; use its full phase/task pump, without copying semaphore logic. */
@Mixin(value = Minecraft.class, priority = 900)
public interface NativeClientPhaseAccess {
	@Invoker(value = "postRunTasks", remap = false)
	void wildercord$runFabricPhase();
}
