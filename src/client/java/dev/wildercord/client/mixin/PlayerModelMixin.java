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
		if (dev.wildercord.client.MastersArtPose.apply((PlayerModel) (Object) this, state)) return;
		CastingPose pose = (CastingPose) state;
		PlayerModel model = (PlayerModel) (Object) this;
		ModelPart right = model.rightArm;
		ModelPart left = model.leftArm;
		float pitch = model.head.xRot;
		float forward = pitch - Mth.HALF_PI;
		// Arms that point where you're looking follow the head's turn, like drawing a bow.
		float turn = model.head.yRot;
		if (pose.wildercord$charging()) {
			// Each motion is held its own way while it charges (see dev.wildercord.cast.feel.Motion).
			float breathe = Mth.sin(state.ageInTicks * 0.3F) * 0.03F;
			String held = pose.wildercord$shape();
			switch (held.isEmpty() ? dev.wildercord.cast.feel.Motion.HURL : dev.wildercord.cast.feel.Motion.of(held)) {
				case FLICK -> {
					// One hand forward, the other tucked in: a finger ready to flick.
					set(right, forward + breathe, turn - 0.1F, 0, 1);
					set(left, 0.4F, 0, 0.2F, 1);
				}
				case HURL -> {
					// Drawing a bow: the casting arm back, the other forward.
					set(right, forward - 0.9F + breathe, turn - 0.35F, 0, 1);
					set(left, forward - breathe, turn + 0.2F, 0, 1);
				}
				case SLASH -> {
					// The sword arm back and across.
					set(right, -1.9F + breathe, turn + 0.9F, -0.5F, 1);
					set(left, 0.2F, 0, 0.1F, 1);
				}
				case BLAST -> {
					// Fists together at the chest, gathering.
					set(right, -1.2F + breathe, turn + 0.55F, 0, 1);
					set(left, -1.2F - breathe, turn - 0.55F, 0, 1);
				}
				case SEAL -> {
					// Both palms pressed down toward the ground.
					set(right, -0.6F + breathe, -0.1F, 0.25F, 1);
					set(left, -0.6F - breathe, 0.1F, -0.25F, 1);
				}
				case CALL -> {
					// Both arms raised to the sky.
					set(right, -2.8F + breathe, 0, 0.35F, 1);
					set(left, -2.8F - breathe, 0, -0.35F, 1);
				}
				case AURA -> {
					// Arms open, palms out.
					set(right, -0.5F + breathe, 0, 0.9F, 1);
					set(left, -0.5F - breathe, 0, -0.9F, 1);
				}
				default -> {
					// BEAM: both hands locked forward, pushing the circle open.
					set(right, forward + breathe, turn - 0.32F, 0, 1);
					set(left, forward - breathe, turn + 0.32F, 0, 1);
				}
			}
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
		// Shapes with a motion of their own below; the rest move by their motion's gesture.
		dev.wildercord.cast.feel.Motion motion = dev.wildercord.cast.feel.Motion.of(shape);
		if (path.equals("spark") || path.equals("ray") || path.equals("touch")) {
			// A quick snap of the wrist.
			float snap = p < 0.2F ? p / 0.2F : 1;
			set(right, forward - 0.4F + 0.4F * snap, turn - 0.05F, 0, w);
			set(left, 0.35F, 0, 0, w);
			return;
		}
		if (motion == dev.wildercord.cast.feel.Motion.CALL) {
			// Hands opening skyward.
			set(right, -2.9F, 0, 0.3F + 0.5F * e, w);
			set(left, -2.9F, 0, -0.3F - 0.5F * e, w);
			return;
		}
		if (path.equals("nova")) {
			// A shove outward, arms wide.
			set(right, -0.8F, 0, 1.6F * e, w);
			set(left, -0.8F, 0, -1.6F * e, w);
			return;
		}
		if (motion == dev.wildercord.cast.feel.Motion.BEAM && !path.equals("beam")) {
			// Held steady down the line.
			set(right, forward, turn - 0.15F, 0, w);
			set(left, forward, turn + 0.15F, 0, w);
			return;
		}
		switch (path) {
			case "crescent", "glaive" -> {
				set(right, forward, turn + Mth.lerp(e, 1.3F, -1.2F), -0.3F, w);
				set(left, 0.3F, 0, 0, w);
			}
			case "barrage" -> {
				boolean rightTurn = ((int) (p * 8)) % 2 == 0;
				set(rightTurn ? right : left, forward, turn + (rightTurn ? -0.15F : 0.15F), 0, w);
				set(rightTurn ? left : right, 0.35F, 0, 0, w);
			}
			case "zone", "ring", "burst", "totem", "mine", "vortex", "snare" -> {
				set(right, -0.25F, 0, 2.4F * e, w);
				set(left, -0.25F, 0, -2.4F * e, w);
			}
			case "domain" -> {
				if (p < 0.55F) {
					set(right, -0.2F, 0, 1.7F, w);
					set(left, -0.2F, 0, -1.7F, w);
				} else {
					set(right, forward, turn - 0.5F, 0, w);
					set(left, forward, turn + 0.5F, 0, w);
				}
			}
			case "pillar" -> {
				float lift = p < 0.35F ? -2.9F : Mth.lerp((p - 0.35F) / 0.3F, -2.9F, -0.5F);
				set(right, lift, -0.2F, 0, w);
				set(left, lift, 0.2F, 0, w);
			}
			case "wall", "wave", "orb" -> {
				set(right, forward, turn - 0.2F, 0, w);
				set(left, forward, turn + 0.2F, 0, w);
			}
			case "blitz" -> {
				set(right, 0.9F, 0, 0.3F, w);
				set(left, 0.9F, 0, -0.3F, w);
			}
			case "self", "orbit", "trail", "imprint" -> {
				set(right, -0.35F, 0, 0.6F, w);
				set(left, -0.35F, 0, -0.6F, w);
			}
			default -> {
				// Bolt, Beam, Arc, Touch, Cone and anything new: a thrust of the casting hand.
				set(right, forward, turn - 0.1F, 0, w);
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
