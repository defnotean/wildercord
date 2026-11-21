package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Pair-only native consumption observation; old captures leave the probe disarmed. */
@Mixin(PlayerModel.class)
public abstract class BraceNullPlayerPaletteMixin {
	@WrapMethod(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullConsumed(PlayerRenderState state, Operation<Void> original) {
		original.call(state);
		BraceNullCaptureProbe.passive(() -> BraceNullCaptureProbe.bodyConsumed((PlayerModel) (Object) this, state));
	}
}
