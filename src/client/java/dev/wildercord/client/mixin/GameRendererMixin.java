package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.ScreenEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera shake from big magic, applied where the view already bobs when you're hurt. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"))
	private void wildercord$shake(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
		Matrix4f shake = new Matrix4f();
		ScreenEffects.applyShake(shake, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		pose.mulPose(shake);
	}
}
