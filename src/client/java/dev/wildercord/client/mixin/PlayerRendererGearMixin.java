package dev.wildercord.client.mixin;

import dev.wildercord.client.render.GearLook;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carries the casting gear a player wears (and the aura on their blade) into their render state, where the gear layer (and the blade's) draws it. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererGearMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$gear(net.minecraft.client.player.AbstractClientPlayer avatar, PlayerRenderState state, float partial, CallbackInfo ci) {
		GearLook.extract(avatar, state);
		// The aura on the weapon in their main hand.
		dev.wildercord.client.fx.AuraBlade.extract(avatar, state);
		// Aura armour's shell round them, and a step's afterimages.
		dev.wildercord.client.render.AuraShellLayer.extract(avatar, state, partial);
		// The body's aura by stage (a shimmer, a haze, a mantle, a corona and burning eyes), calm at rest and flaring in a fight.
		dev.wildercord.client.render.AuraBodyLayer.extract(avatar, state, partial);
	}
}
