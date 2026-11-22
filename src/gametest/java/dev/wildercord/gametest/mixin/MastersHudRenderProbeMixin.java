package dev.wildercord.gametest.mixin;

import dev.wildercord.gametest.MastersCaptureProbe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads the native HUD sprite coordinates; never changes extraction or rendering. */
@Mixin(GuiGraphics.class)
public abstract class MastersHudRenderProbeMixin {
	@Inject(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", at = @At("HEAD"))
	private void wildercord$hudSprite(ResourceLocation sprite, int x, int y, int width, int height, CallbackInfo ci) {
		var graphics = (GuiGraphics) (Object) this;
		MastersCaptureProbe.hudSprite(sprite, x, y, width, height, graphics.guiWidth(), graphics.guiHeight());
	}
}
