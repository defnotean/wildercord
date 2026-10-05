package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerModel.class)
public abstract class ArticulatedSharedPlayerPaletteMixin {
	/** After the entire original method, including production TAIL hooks that install the palette. */
	@WrapMethod(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$palette(AvatarRenderState state, Operation<Void> original) {
		original.call(state);
		ArticulatedSharedRenderProbe.bodyPalette((PlayerModel) (Object) this, state);
	}
}
