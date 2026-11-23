package dev.wildercord.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.client.fx.CustomMobFireFx;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Suppresses vanilla's oversized planar fire billboard on burning {@code wildercord} custom mobs so their
 * detailed models and attack tells stay legible while {@link CustomMobFireFx} draws clean perimeter embers.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererCustomFireMixin {
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z"))
	private boolean wildercord$replaceVanillaFireBillboard(Entity entity, Operation<Boolean> original) {
		if (CustomMobFireFx.isCustomMob(entity)) {
			return false;
		}
		return original.call(entity);
	}
}
