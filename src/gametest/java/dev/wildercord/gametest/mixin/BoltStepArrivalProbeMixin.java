package dev.wildercord.gametest.mixin;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.gametest.BoltStepArrivalProbe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the stable direct-cut overload, including scheduled cuts whose originating art is captured in Hits. */
@Mixin(ArtKit.Hits.class)
public abstract class BoltStepArrivalProbeMixin {
	@Shadow(remap = false) @Final private ServerPlayer player;
	@Shadow(remap = false) @Final private AuraApi.StringArt art;

	@Inject(method = "strike(Lnet/minecraft/world/entity/LivingEntity;D)F", at = @At("RETURN"), remap = false)
	private void wildercord$boltStepArrival(LivingEntity target, double factor, CallbackInfoReturnable<Float> cir) {
		BoltStepArrivalProbe.struck(player, art, target, cir.getReturnValueF());
	}
}
