package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.compat.WorldRenderCompat;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires the world render hooks where fabric's AFTER_ENTITIES would, right after vanilla submits entities. */
@Mixin(LevelRenderer.class)
public abstract class WorldRenderCompatMixin {
	@Inject(method = "submitEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
			at = @At("RETURN"))
	private void wildercord$afterEntities(PoseStack pose, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
		WorldRenderCompat.fireAfterEntities(pose, state, collector);
	}
}
