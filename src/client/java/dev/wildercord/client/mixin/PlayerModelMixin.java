package dev.wildercord.client.mixin;

import dev.wildercord.client.CastingPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Casting poses. Charging: both hands held out in front, pushing the circle open. After a cast, a
 * motion for the shape: a thrust for bolts and beams, a sweep for a Crescent, alternating blows for
 * a Barrage, arms flung up for the great circles (Zone, Domain, Rain...), a push for a Wall or
 * Wave, arms swept back for a Blitz, open hands for Self. Each eases in and back out.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void wildercord$pose(AvatarRenderState state, CallbackInfo ci) {
		CastingPose pose = (CastingPose) state;
		PlayerModel model = (PlayerModel) (Object) this;
		ModelPart right = model.rightArm;
		ModelPart left = model.leftArm;
		float pitch = model.head.xRot;
		float forward = pitch - Mth.HALF_PI;
		if (pose.wildercord$charging()) {
			float breathe = Mth.sin(state.ageInTicks * 0.3F) * 0.03F;
			set(right, forward + breathe, -0.32F, 0, 1);
			set(left, forward - breathe, 0.32F, 0, 1);
			return;
		}
		float p = pose.wildercord$progress();
		if (p < 0) {
			return;
		}
		// In fast, held, then eased back to rest.
		float w = p < 0.15F ? p / 0.15F : p < 0.6F ? 1 : 1 - (p - 0.6F) / 0.4F;
		float e = 1 - (1 - p) * (1 - p) * (1 - p);
		String shape = pose.wildercord$shape();
		String path = shape.substring(shape.indexOf(':') + 1);
		switch (path) {
			case "crescent" -> {
				set(right, forward, Mth.lerp(e, 1.3F, -1.2F), -0.3F, w);
				set(left, 0.3F, 0, 0, w);
			}
			case "barrage" -> {
				boolean rightTurn = ((int) (p * 8)) % 2 == 0;
				set(rightTurn ? right : left, forward, rightTurn ? -0.15F : 0.15F, 0, w);
				set(rightTurn ? left : right, 0.35F, 0, 0, w);
			}
			case "zone", "rain", "ring", "burst", "totem", "mine" -> {
				set(right, -0.25F, 0, 2.4F * e, w);
				set(left, -0.25F, 0, -2.4F * e, w);
			}
			case "domain" -> {
				if (p < 0.55F) {
					set(right, -0.2F, 0, 1.7F, w);
					set(left, -0.2F, 0, -1.7F, w);
				} else {
					set(right, forward, -0.5F, 0, w);
					set(left, forward, 0.5F, 0, w);
				}
			}
			case "pillar" -> {
				float lift = p < 0.35F ? -2.9F : Mth.lerp((p - 0.35F) / 0.3F, -2.9F, -0.5F);
				set(right, lift, -0.2F, 0, w);
				set(left, lift, 0.2F, 0, w);
			}
			case "wall", "wave", "orb" -> {
				set(right, forward, -0.2F, 0, w);
				set(left, forward, 0.2F, 0, w);
			}
			case "blitz" -> {
				set(right, 0.9F, 0, 0.3F, w);
				set(left, 0.9F, 0, -0.3F, w);
			}
			case "self", "orbit", "trail" -> {
				set(right, -0.35F, 0, 0.6F, w);
				set(left, -0.35F, 0, -0.6F, w);
			}
			default -> {
				// Bolt, Beam, Arc, Touch, Cone and anything new: a thrust of the casting hand.
				set(right, forward, -0.1F, 0, w);
				set(left, 0.35F, 0, 0, w);
			}
		}
	}

	/** Moves an arm {@code w} of the way from where the vanilla animation left it to the pose. */
	private static void set(ModelPart arm, float xRot, float yRot, float zRot, float w) {
		arm.xRot = Mth.lerp(w, arm.xRot, xRot);
		arm.yRot = Mth.lerp(w, arm.yRot, yRot);
		arm.zRot = Mth.lerp(w, arm.zRot, zRot);
	}
}
