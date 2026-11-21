package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.wildercord.gametest.MastersCaptureProbe;
import dev.wildercord.client.compat.GuiGraphicsExtractor;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads the native HUD sprite coordinates; never changes extraction or rendering. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class MastersHudRenderProbeMixin {
	@Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V", at = @At("HEAD"))
	private void wildercord$hudSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, CallbackInfo ci) {
		var graphics = (GuiGraphicsExtractor) (Object) this;
		MastersCaptureProbe.hudSprite(sprite, x, y, width, height, graphics.guiWidth(), graphics.guiHeight());
	}
}
