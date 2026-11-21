package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerModel.class)
public abstract class ArticulatedSharedPlayerPaletteMixin {
	/** After the entire original method, including production TAIL hooks that install the palette. */
	@WrapMethod(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$palette(PlayerRenderState state, Operation<Void> original) {
		original.call(state);
		ArticulatedSharedRenderProbe.bodyPalette((PlayerModel) (Object) this, state);
	}
}
