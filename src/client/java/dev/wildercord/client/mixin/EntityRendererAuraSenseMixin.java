package dev.wildercord.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.client.AuraClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Aura sense (see {@code aura.AuraSense}): a creature the breathing stance has sensed is outlined, for this player alone, in
 * the aura's colour (unless it glows for another reason already, which keeps its own colour). Vanilla's own outline, so it
 * shows through walls and draws under shader packs as glowing does.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class EntityRendererAuraSenseMixin {
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean wildercord$auraSensed(Minecraft minecraft, Entity entity, Operation<Boolean> original) {
		return original.call(minecraft, entity) || AuraClient.sensed(entity);
	}

}
