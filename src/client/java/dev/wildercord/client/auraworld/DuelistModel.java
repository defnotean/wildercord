package dev.wildercord.client.auraworld;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A wandering duelist: a player's shape under a deep travelling hood, a short mantle over the shoulders and a long cloak down
 * the back that swings as it walks, and a scabbard slung at the left hip with its hilt jutting forward, empty once the blade
 * is drawn. Its poses: a ready stance with the blade drawn; both arms raised high behind its head for a slash (the tell); the
 * blade held across the body in guard; thrown back when caught out; a bow; down on one knee when it yields; sitting by its
 * fire with its forearms on its knees; a low crouch before a dash.
 *
 * <p>Laid out on a 64x64 skin (see {@code duelist} in {@code tools/aura_world_art.py}), the humanoid parts where a player's
 * are:</p>
 * <pre>
 *   head (0,0) 8x8x8     hood (32,0) 8x8x8 +0.6   body (16,16) 8x12x4    arm (40,16) 4x12x4    leg (0,16) 4x12x4
 *   cloak (0,32) 10x17x1  mantle (22,32) 10x4x6 +0.3   scabbard (54,32) 1x12x2   grip (60,32) 1x4x1   guard (54,46) 1x1x3
 *   tassel (22,42) 2x5x0
 * </pre>
 */
public class DuelistModel extends HumanoidModel<AuraFighterRenderState> {
	private final ModelPart cloak;
	private final ModelPart scabbard;
	private final ModelPart hilt;

	public DuelistModel(ModelPart root) {
		super(root);
		this.cloak = body.getChild("cloak");
		this.scabbard = body.getChild("scabbard");
		this.hilt = scabbard.getChild("hilt");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.getChild("head");
		// The hood, deeper than a hat, its face open.
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.6F)),
			PartPose.ZERO);
		PartDefinition body = root.getChild("body");
		// A long cloak down the back, hung from the shoulders.
		body.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 17.0F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 0.2F, 2.3F, 0.06F, 0.0F, 0.0F));
		// A short mantle over the shoulders.
		body.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(22, 32).addBox(-5.0F, -0.4F, -3.0F, 10.0F, 4.0F, 6.0F, new CubeDeformation(0.3F)),
			PartPose.ZERO);
		// The scabbard at the left hip, slung back, its hilt forward and up.
		PartDefinition scabbard = body.addOrReplaceChild("scabbard", CubeListBuilder.create().texOffs(54, 32).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 12.0F, 2.0F),
			PartPose.offsetAndRotation(4.7F, 10.0F, 0.2F, -0.95F, 0.0F, -0.2F));
		PartDefinition hilt = scabbard.addOrReplaceChild("hilt", CubeListBuilder.create().texOffs(60, 32).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
			PartPose.ZERO);
		hilt.addOrReplaceChild("guard", CubeListBuilder.create().texOffs(54, 46).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, -0.3F, 0.0F));
		scabbard.addOrReplaceChild("tassel", CubeListBuilder.create().texOffs(22, 42).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.001F)),
			PartPose.offsetAndRotation(0.0F, 0.5F, -1.05F, 0.2F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AuraFighterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float drawn = state.drawn;
		float wind = state.windup;
		float guard = state.guard;
		float stagger = state.stagger;
		float bow = state.bow;
		float kneel = state.yield;
		float sit = state.sit;
		float dash = state.dash;

		// The cloak swings out behind as it walks, and stirs standing still.
		cloak.xRot = 0.06F + walk * 0.45F + Mth.sin(t * 0.07F) * 0.03F + dash * 0.5F;
		hilt.visible = drawn < 0.5F;

		// Blade drawn: a ready stance, the blade forward.
		float ready = drawn * (1 - wind) * (1 - guard) * (1 - bow) * (1 - kneel);
		rightArm.xRot = Mth.lerp(ready, rightArm.xRot, rightArm.xRot * 0.4F - 0.5F);
		rightArm.yRot = Mth.lerp(ready, rightArm.yRot, -0.15F);

		// The tell: both arms raised high behind its head, leaning back.
		rightArm.xRot = Mth.lerp(wind, rightArm.xRot, -2.85F);
		rightArm.yRot = Mth.lerp(wind, rightArm.yRot, -0.3F);
		rightArm.zRot = Mth.lerp(wind, rightArm.zRot, 0.15F);
		leftArm.xRot = Mth.lerp(wind, leftArm.xRot, -2.7F);
		leftArm.yRot = Mth.lerp(wind, leftArm.yRot, 0.55F);
		leftArm.zRot = Mth.lerp(wind, leftArm.zRot, -0.1F);
		body.xRot -= 0.14F * wind;
		head.xRot -= 0.18F * wind;

		// Guard: the blade across the body, the off hand bracing it.
		rightArm.xRot = Mth.lerp(guard, rightArm.xRot, -1.3F);
		rightArm.yRot = Mth.lerp(guard, rightArm.yRot, -0.85F);
		rightArm.zRot = Mth.lerp(guard, rightArm.zRot, 0.0F);
		leftArm.xRot = Mth.lerp(guard, leftArm.xRot, -1.15F);
		leftArm.yRot = Mth.lerp(guard, leftArm.yRot, 0.75F);

		// Caught out: thrown back, arms flung wide.
		body.xRot -= 0.22F * stagger;
		head.xRot -= 0.3F * stagger;
		rightArm.zRot += 0.7F * stagger;
		leftArm.zRot -= 0.7F * stagger;
		rightArm.xRot = Mth.lerp(stagger, rightArm.xRot, -0.4F);
		leftArm.xRot = Mth.lerp(stagger, leftArm.xRot, -0.3F);

		// A bow: bent forward from the waist, one hand to the chest, the other behind.
		lean(0.6F * bow + 0.28F * kneel + 0.42F * dash);
		head.xRot += 0.45F * bow + 0.4F * kneel;
		rightArm.xRot = Mth.lerp(bow, rightArm.xRot, -1.05F);
		rightArm.yRot = Mth.lerp(bow, rightArm.yRot, -0.8F);
		rightArm.zRot = Mth.lerp(bow, rightArm.zRot, 0.05F);
		leftArm.xRot = Mth.lerp(bow, leftArm.xRot, 0.55F);
		leftArm.zRot = Mth.lerp(bow, leftArm.zRot, -0.15F);

		// Yielding: down on the right knee, the left foot planted, the blade's point to the ground.
		rightLeg.xRot = Mth.lerp(kneel, rightLeg.xRot, 0.2F);
		leftLeg.xRot = Mth.lerp(kneel, leftLeg.xRot, -1.45F);
		rightArm.xRot = Mth.lerp(kneel, rightArm.xRot, -0.35F);
		leftArm.xRot = Mth.lerp(kneel, leftArm.xRot, -0.95F);
		leftArm.yRot = Mth.lerp(kneel, leftArm.yRot, 0.2F);

		// A dash's crouch: knees bent, low and forward.
		rightLeg.xRot = Mth.lerp(dash, rightLeg.xRot, -0.55F);
		leftLeg.xRot = Mth.lerp(dash, leftLeg.xRot, 0.35F);

		// Sitting by its fire, forearms on its knees.
		rightLeg.xRot = Mth.lerp(sit, rightLeg.xRot, -1.41F);
		rightLeg.yRot = Mth.lerp(sit, rightLeg.yRot, 0.3F);
		leftLeg.xRot = Mth.lerp(sit, leftLeg.xRot, -1.41F);
		leftLeg.yRot = Mth.lerp(sit, leftLeg.yRot, -0.3F);
		rightArm.xRot = Mth.lerp(sit, rightArm.xRot, -0.75F);
		leftArm.xRot = Mth.lerp(sit, leftArm.xRot, -0.75F);
		rightArm.yRot = Mth.lerp(sit, rightArm.yRot, 0.15F);
		leftArm.yRot = Mth.lerp(sit, leftArm.yRot, -0.15F);
		cloak.xRot = Mth.lerp(sit, cloak.xRot, 1.15F);

		// Idle: the breath, the weight and the head each on their own period, so the stance never visibly loops.
		float still = (1 - walk) * (1 - sit) * (1 - wind) * (1 - stagger);
		body.xRot += Mth.sin(t * 0.11F) * 0.012F * still;
		body.zRot += Mth.sin(t * 0.047F + 0.8F) * 0.018F * still;
		head.xRot += Mth.sin(t * 0.083F + 2.0F) * 0.016F * still;
		head.zRot += Mth.sin(t * 0.039F) * 0.022F * still;
		leftArm.zRot -= Mth.sin(t * 0.09F + 1.0F) * 0.02F * still;
		rightArm.zRot += Mth.sin(t * 0.09F) * 0.02F * still;

		// Cloth and tassel trail the body on a lag: the cloak a beat behind the stride and behind a turn of the head, the
		// tassel a beat behind the cloak, and the whole cloak shudders in the tell.
		float lagged = Mth.sin(state.walkAnimationPos * 0.6662F - 1.1F) * 0.09F * walk;
		cloak.xRot += lagged + wind * Mth.sin(t * 1.7F) * 0.03F;
		cloak.zRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * -0.006F + Mth.sin(t * 0.045F + 1.3F) * 0.03F * (1 - walk);
		ModelPart tassel = scabbard.getChild("tassel");
		tassel.xRot = 0.2F + Mth.sin(state.walkAnimationPos * 0.6662F - 2.3F) * 0.35F * walk + Mth.sin(t * 0.13F) * 0.05F + 0.4F * dash;
		tassel.zRot = Mth.sin(t * 0.08F + 0.5F) * 0.08F;
		scabbard.xRot += Mth.sin(state.walkAnimationPos * 0.6662F - 0.6F) * 0.05F * walk;

		// Hurt: a snap away from the blow that rebounds, the cloak flung forward over it.
		float hit = state.hurt;
		if (hit > 0 && state.deathTime <= 0) {
			float k = (float) (hit * Math.cos((1 - hit) * 6.0F));
			body.xRot -= 0.22F * k;
			head.xRot -= 0.32F * k;
			head.zRot += 0.1F * k;
			rightArm.zRot += 0.28F * k;
			leftArm.zRot -= 0.28F * k;
			cloak.xRot += 0.35F * k;
			tassel.xRot += 0.5F * k;
		}

		// Staged death: the head is thrown back, the knees fold one after the other, the arms go slack, the cloak settles.
		if (state.deathTime > 0) {
			float d = state.deathTime;
			float recoil = Mth.clamp(d / 3.0F, 0.0F, 1.0F), foldR = Mth.clamp((d - 3.0F) / 8.0F, 0.0F, 1.0F);
			float foldL = Mth.clamp((d - 5.0F) / 8.0F, 0.0F, 1.0F), slump = Mth.clamp((d - 8.0F) / 8.0F, 0.0F, 1.0F);
			body.xRot += -0.3F * recoil * (1 - slump) + 0.3F * slump;
			head.xRot += -0.4F * recoil * (1 - slump) + 0.5F * slump;
			rightLeg.xRot += -0.8F * foldR;
			leftLeg.xRot += -0.45F * foldL;
			rightArm.xRot = Mth.lerp(slump, rightArm.xRot, 0.3F);
			leftArm.xRot = Mth.lerp(slump, leftArm.xRot, 0.2F);
			rightArm.zRot += 0.35F * slump;
			leftArm.zRot -= 0.35F * slump;
			cloak.xRot += 0.4F * foldR;
		}
	}

	/**
	 * Bends forward by {@code angle}, the way the game crouches a player: the body tips about the neck, the shoulders and head
	 * come down with it and the legs step back under the hips.
	 */
	private void lean(float angle) {
		if (angle <= 0.001F) {
			return;
		}
		float s = angle / 0.5F;
		body.xRot += angle;
		body.y += 3.2F * s;
		head.y += 4.2F * s;
		rightArm.y += 3.2F * s;
		leftArm.y += 3.2F * s;
		rightLeg.z += 4.0F * s;
		leftLeg.z += 4.0F * s;
		rightArm.xRot += angle * 0.8F;
		leftArm.xRot += angle * 0.8F;
	}
}
