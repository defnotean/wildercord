package dev.wildercord.client.mixin;

import dev.wildercord.client.SpellHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Subtitles (bottom right) move up past the spell panel when they'd cover its name row. Each line is placed
 * by one translate, whose x gives the box's width: the lift is worked out in {@link SpellHud#subtitleLift}.
 */
@Mixin(SubtitleOverlay.class)
public abstract class SubtitleOverlayMixin {
	@ModifyArgs(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;"))
	private void wildercord$clearSpellHud(Args args) {
		var window = Minecraft.getInstance().getWindow();
		float x = args.get(0);
		float y = args.get(1);
		// Vanilla centres each line at x = width - half - 2; its backdrop reaches half + 1 either side.
		int half = Math.round(window.getGuiScaledWidth() - 2 - x);
		int lift = SpellHud.subtitleLift(window.getGuiScaledHeight(), Math.round(x) - half - 1);
		if (lift > 0) {
			args.set(1, y - lift);
		}
	}
}
