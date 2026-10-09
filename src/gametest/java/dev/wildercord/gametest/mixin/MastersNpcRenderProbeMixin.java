package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.auraworld.MasterRenderer;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MasterRenderer.class)
public abstract class MastersNpcRenderProbeMixin {
	@Inject(method = "extractRenderState(Ldev/wildercord/aura/world/SwordMaster;Ldev/wildercord/client/auraworld/AuraFighterRenderState;F)V", at = @At("TAIL"))
	private void wildercord$nativeMaster(SwordMaster master, AuraFighterRenderState state, float partial, CallbackInfo ci) {
		MastersNpcCaptureProbe.extracted(master, state, partial);
	}
}
