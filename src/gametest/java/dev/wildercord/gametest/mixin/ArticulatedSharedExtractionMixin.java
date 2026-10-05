package dev.wildercord.gametest.mixin;

import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The return follows the production dispatcher HitStop hook; this is the actual entity argument. */
@Mixin(LevelExtractor.class)
public abstract class ArticulatedSharedExtractionMixin {
	@Inject(method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"), require = 1)
	private void wildercord$extracted(Entity entity, float partial, CallbackInfoReturnable<EntityRenderState> cir) {
		ArticulatedSharedRenderProbe.extracted(entity, partial, cir.getReturnValue());
	}
}
