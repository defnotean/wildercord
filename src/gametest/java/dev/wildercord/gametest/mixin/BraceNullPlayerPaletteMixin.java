package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Pair-only native consumption observation; old captures leave the probe disarmed. */
@Mixin(PlayerModel.class)
public abstract class BraceNullPlayerPaletteMixin {
	@WrapMethod(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullConsumed(AvatarRenderState state, Operation<Void> original) {
		original.call(state);
		BraceNullCaptureProbe.bodyConsumed((PlayerModel) (Object) this, state);
	}
}
