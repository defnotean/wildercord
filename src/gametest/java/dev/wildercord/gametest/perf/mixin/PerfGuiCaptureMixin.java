package dev.wildercord.gametest.perf.mixin;

import dev.wildercord.gametest.perf.HudCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands each drawn GUI rectangle to {@link HudCapture} (only while a layout check is listening).
 * 1.21.5 has no gui render state to extract from, so it hooks the immediate-mode draws instead.
 */
@Mixin(GuiGraphics.class)
public abstract class PerfGuiCaptureMixin {
	@Inject(method = "fill(IIIII)V", at = @At("HEAD"))
	private void perf$fill(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) { if (HudCapture.on) HudCapture.add(new ScreenRectangle(x1, y1, x2 - x1, y2 - y1)); }

	@Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I", at = @At("HEAD"))
	private void perf$text(Font font, String text, int x, int y, int color, CallbackInfoReturnable<Integer> ci) { if (HudCapture.on) HudCapture.add(new ScreenRectangle(x, y, font.width(text), font.lineHeight)); }

	@Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", at = @At("HEAD"))
	private void perf$componentText(Font font, Component text, int x, int y, int color, CallbackInfoReturnable<Integer> ci) { if (HudCapture.on) HudCapture.add(new ScreenRectangle(x, y, font.width(text), font.lineHeight)); }

	@Inject(method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
	private void perf$item(ItemStack stack, int x, int y, CallbackInfo ci) { if (HudCapture.on) HudCapture.add(new ScreenRectangle(x, y, 16, 16)); }
}
