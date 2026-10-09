package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.wildlife.NewtBlockedTimeoutProbe;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;

/** Observes the original private native timeout body only in the armed GameTest journey. */
@Mixin(value = PathNavigation.class, remap = false)
public abstract class NewtBlockedTimeoutProbeMixin {
	@WrapMethod(method = "timeoutPath()V", require = 1, expect = 1, allow = 1)
	private void wildercord$blockedTimeout(Operation<Void> original) {
		NewtBlockedTimeoutProbe.timeout(this, original);
	}
}
