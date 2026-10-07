package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.CounterHitCapture;
import dev.wildercord.aura.arts.ArtKit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Test-only observation. No arguments, return value, timing or native consequence is replaced. */
@Mixin(value = ArtKit.Hits.class, remap = false)
public abstract class CounterHitCaptureMixin {
	@Shadow @Final private ServerPlayer player;
	@Shadow @Final private AuraApi.StringArt art;

	@WrapMethod(method = "raw(Lnet/minecraft/world/entity/LivingEntity;DLdev/wildercord/aura/AuraFxRules$Weight;)F", require = 1, expect = 1, allow = 1)
	private float wildercord$counterHit(LivingEntity target, double damage, AuraFxRules.Weight weight, Operation<Float> original) {
		return CounterHitCapture.observe(this, player, art, target, () -> original.call(target, damage, weight));
	}
}
