package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.ScreenEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera shake from big magic, applied where the view already bobs when you're hurt. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow @Final public net.minecraft.client.renderer.ItemInHandRenderer itemInHandRenderer;

	@Inject(method = "extract", at = @At("HEAD"))
	private void wildercord$presentationFrame(CallbackInfo ci) { dev.wildercord.client.CombatPresentation.beginFrame(); }
	@Inject(method = "extract", at = @At("TAIL"))
	private void wildercord$extractHandsAvatar(net.minecraft.client.DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (renderLevel && mc.isGameLoadFinished() && mc.level != null && mc.player != null
				&& mc.options.getCameraType().isFirstPerson() && !mc.player.isSleeping() && !mc.gui.hud.isHidden()
				&& mc.gameMode != null && mc.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.SPECTATOR) {
			float partial = delta.getGameTimeDeltaPartialTick(false);
			((dev.wildercord.client.FirstPersonAvatarState) this.itemInHandRenderer).wildercord$prepareHandsAvatar(mc.player, partial);
		} else {
			((dev.wildercord.client.FirstPersonAvatarState) this.itemInHandRenderer).wildercord$clearHandsAvatar();
		}
	}
	@Inject(method = "render", at = @At("RETURN"))
	private void wildercord$presentationFrameDone(CallbackInfo ci) {
		((dev.wildercord.client.FirstPersonAvatarState) this.itemInHandRenderer).wildercord$clearHandsAvatar();
		dev.wildercord.client.CombatPresentation.endFrame();
	}

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void wildercord$shake(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
		if (dev.wildercord.client.combat.ArticulatedCombat.stableCamera()) { ci.cancel(); return; }
		Matrix4f shake = new Matrix4f();
		ScreenEffects.applyShake(shake, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
		pose.mulPose(shake);
	}
	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void wildercord$stableView(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
		if (dev.wildercord.client.combat.ArticulatedCombat.stableCamera()) ci.cancel();
	}
}
