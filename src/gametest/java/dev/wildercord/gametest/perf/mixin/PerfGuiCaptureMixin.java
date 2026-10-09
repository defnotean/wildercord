package dev.wildercord.gametest.perf.mixin;

import dev.wildercord.gametest.perf.HudCapture;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands each extracted GUI rectangle to {@link HudCapture} (only while a layout check is listening). */
@Mixin(GuiRenderState.class)
public abstract class PerfGuiCaptureMixin {
	@Inject(method = "addGuiElement", at = @At("HEAD"))
	private void perf$element(GuiElementRenderState state, CallbackInfo ci) { if (HudCapture.on) HudCapture.add(state.bounds()); }

	@Inject(method = "addBlitToCurrentLayer", at = @At("HEAD"))
	private void perf$blit(net.minecraft.client.renderer.state.gui.BlitRenderState state, CallbackInfo ci) { if (HudCapture.on) HudCapture.add(state.bounds()); }

	@Inject(method = "addText", at = @At("HEAD"))
	private void perf$text(GuiTextRenderState state, CallbackInfo ci) { if (HudCapture.on) HudCapture.add(state.bounds()); }

	@Inject(method = "addItem", at = @At("HEAD"))
	private void perf$item(GuiItemRenderState state, CallbackInfo ci) { if (HudCapture.on) HudCapture.add(state.bounds()); }
}
