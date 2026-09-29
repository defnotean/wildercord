package dev.wildercord.client.mixin;

import dev.wildercord.client.CastingPose;
import dev.wildercord.client.CordGlow;
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
 * held out pushing the circle open; for a moment after a cast, the shape's own motion. Also how
 * brightly the Cord's beads glow: only for a moment after it's put on (see CordGlow), brighter as a
 * charge builds, and in a flare that fades after a cast.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$castingPose(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
		CastingPose pose = (CastingPose) state;
		WildercordAttachments.CordLook cord = avatar.getAttachedOrElse(WildercordAttachments.CORD_LOOK, WildercordAttachments.CordLook.NONE);
		pose.wildercord$setCord(cord);
		// At rest the beads glow only for a moment after the Cord goes on.
		float idle = CordGlow.idle(avatar, cord.tier(), partial);
		pose.wildercord$setGlow(idle);
		WildercordAttachments.Charge charge = avatar.getAttached(WildercordAttachments.CHARGE);
		if (charge != null) {
			pose.wildercord$setPose(wildercord$firstShape(charge.runes()), 0, true);
			// The beads burn brighter as the charge builds.
			pose.wildercord$setGlow(1 + Math.min(1, (avatar.level().getGameTime() - charge.start() + partial) / 30F) * 1.5F);
			return;
		}
		WildercordAttachments.CastPose cast = avatar.getAttached(WildercordAttachments.CAST_POSE);
		if (cast != null) {
			float t = (avatar.level().getGameTime() - cast.start() + partial) / WildercordAttachments.CastPose.TICKS;
			if (t >= 0 && t < 1) {
				pose.wildercord$setPose(cast.shape(), t, false);
				// A flare that fades back to the resting glow.
				pose.wildercord$setGlow(Math.max(idle, 3 * (1 - t)));
				return;
			}
		}
		pose.wildercord$setPose("", -1, false);
	}

	/** The first shape rune of a charge (its pose), or Self when a spell starts with its effects. */
	private static String wildercord$firstShape(java.util.List<String> runes) {
		for (String id : runes) {
			var rune = dev.wildercord.spell.Runes.get(id);
			if (rune.isPresent() && rune.get().family() == dev.wildercord.spell.RuneFamily.SHAPE) {
				return id;
			}
		}
		return dev.wildercord.spell.Runes.SELF.id();
	}
}
