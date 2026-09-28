package dev.wildercord.client.mixin;

import dev.wildercord.client.CastingPose;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands a player's casting pose to the model (see PlayerModelMixin): while charging, both hands
 * held out pushing the circle open; for a moment after a cast, the shape's own motion.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$castingPose(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
		CastingPose pose = (CastingPose) state;
		pose.wildercord$setCord(avatar.getAttachedOrElse(WildercordAttachments.CORD_LOOK, WildercordAttachments.CordLook.NONE));
		pose.wildercord$setGlow(1);
		WildercordAttachments.Charge charge = avatar.getAttached(WildercordAttachments.CHARGE);
		if (charge != null) {
			pose.wildercord$setPose(charge.runes().isEmpty() ? "" : charge.runes().getFirst(), 0, true);
			// The beads burn brighter as the charge builds.
			pose.wildercord$setGlow(1 + Math.min(1, (avatar.level().getGameTime() - charge.start() + partial) / 30F) * 1.5F);
			return;
		}
		WildercordAttachments.CastPose cast = avatar.getAttached(WildercordAttachments.CAST_POSE);
		if (cast != null) {
			float t = (avatar.level().getGameTime() - cast.start() + partial) / WildercordAttachments.CastPose.TICKS;
			if (t >= 0 && t < 1) {
				pose.wildercord$setPose(cast.shape(), t, false);
				pose.wildercord$setGlow(1 + 2 * (1 - t));
				return;
			}
		}
		pose.wildercord$setPose("", -1, false);
	}
}
