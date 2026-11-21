package dev.wildercord.client.mixin;

import dev.wildercord.client.cosmetic.CordStyleLook;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries a player's Cord style into their render state, where the Cord layer draws it. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererStyleMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$cordStyle(net.minecraft.client.player.AbstractClientPlayer avatar, PlayerRenderState state, float partial, CallbackInfo ci) {
		CordStyleLook.extract(avatar, state);
	}
}
