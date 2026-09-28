package dev.wildercord.client.mixin;

import dev.wildercord.client.cosmetic.CordStyleLook;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries a player's Cord style into their render state, where the Cord layer draws it. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererStyleMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$cordStyle(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
		CordStyleLook.extract(avatar, state);
	}
}
