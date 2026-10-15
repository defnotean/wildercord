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
 * A fallen knight: a suit of old plate with nobody in it, a closed helm with a glowing visor slit and a ragged crest, broad
 * pauldrons, a torn tabard hanging from its belt and the rag of a cloak behind. It walks heavily, a little stooped; raising its
 * blade for a slash it lifts both arms high and leans back (the tell); in guard it holds the blade across its body; caught
 * out it reels back.
 *
 * <p>Laid out on a 64x64 skin (see {@code fallen_knight} in {@code tools/aura_world_art.py}):</p>
 * <pre>
 *   head (0,0) 8x8x8      helm (32,0) 8x8x8 +0.75   body (16,16) 8x12x4 +0.25   arm (40,16) 4x12x4 +0.2   leg (0,16) 4x12x4 +0.15
 *   crest (0,32) 1x4x8    pauldron (18,32) 5x3x5 +0.35   tabard (38,32) 6x10x0   cloak (0,44) 8x14x0
 * </pre>
 */
public class FallenKnightModel extends HumanoidModel<AuraFighterRenderState> {
	private final ModelPart tabard;
	private final ModelPart cloak;

	public FallenKnightModel(ModelPart root) {
		super(root);
		this.tabard = body.getChild("tabard");
		this.cloak = body.getChild("cloak");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		// The helm, bigger than the head inside it.
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F)),
			PartPose.ZERO);
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, -4.0F, -4.0F, 1.0F, 4.0F, 8.0F),
			PartPose.offsetAndRotation(0.0F, -8.4F, 0.5F, -0.12F, 0.0F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
			new CubeDeformation(0.25F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		body.addOrReplaceChild("tabard", CubeListBuilder.create().texOffs(38, 32).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 10.0F, 0.0F, new CubeDeformation(0.001F)),
			PartPose.offset(0.0F, 10.5F, -2.4F));
		body.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 14.0F, 0.0F, new CubeDeformation(0.001F)),
			PartPose.offsetAndRotation(0.0F, 0.4F, 2.4F, 0.08F, 0.0F, 0.0F));
		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
			new CubeDeformation(0.2F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(18, 32).addBox(-3.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.35F)),
			PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.18F));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
			new CubeDeformation(0.2F)), PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(18, 32).mirror().addBox(-1.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F,
			new CubeDeformation(0.35F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.18F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.15F)),
			PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.15F)),
			PartPose.offset(1.9F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AuraFighterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float wind = state.windup;
		float guard = state.guard;
		float stagger = state.stagger;

		// A heavy, stooped walk; the rags stir.
		head.xRot += 0.12F;
		tabard.xRot = -Math.abs(Mth.sin(state.walkAnimationPos * 0.6662F)) * 0.5F * walk + Mth.sin(t * 0.05F) * 0.03F;
		cloak.xRot = 0.08F + walk * 0.4F + Mth.sin(t * 0.06F + 1) * 0.04F;

		// Its blade held ready before it.
		float ready = (1 - wind) * (1 - guard) * (1 - stagger);
		rightArm.xRot = Mth.lerp(ready, rightArm.xRot, rightArm.xRot * 0.4F - 0.55F);

		// The tell: the blade raised high in both hands, leaning back.
		rightArm.xRot = Mth.lerp(wind, rightArm.xRot, -2.9F);
		rightArm.yRot = Mth.lerp(wind, rightArm.yRot, -0.35F);
		leftArm.xRot = Mth.lerp(wind, leftArm.xRot, -2.75F);
		leftArm.yRot = Mth.lerp(wind, leftArm.yRot, 0.6F);
		body.xRot -= 0.12F * wind;
		head.xRot -= 0.25F * wind;

		// Guard: the blade across the body.
		rightArm.xRot = Mth.lerp(guard, rightArm.xRot, -1.25F);
		rightArm.yRot = Mth.lerp(guard, rightArm.yRot, -0.9F);
		leftArm.xRot = Mth.lerp(guard, leftArm.xRot, -1.1F);
		leftArm.yRot = Mth.lerp(guard, leftArm.yRot, 0.8F);

		// Reeling.
		body.xRot -= 0.25F * stagger;
		head.xRot -= 0.35F * stagger;
		rightArm.zRot += 0.6F * stagger;
		leftArm.zRot -= 0.6F * stagger;
		rightArm.xRot = Mth.lerp(stagger, rightArm.xRot, -0.2F);

		// Idle: heavy plate settling, each part on its own slow period; the crest nods a beat behind the helm.
		float still = (1 - walk) * (1 - wind) * (1 - stagger);
		body.xRot += Mth.sin(t * 0.09F) * 0.014F * still;
		body.zRot += Mth.sin(t * 0.041F + 0.6F) * 0.02F * still;
		head.zRot += Mth.sin(t * 0.033F + 2.0F) * 0.03F * still;
		head.yRot += Mth.sin(t * 0.027F) * 0.05F * still;
		rightArm.zRot += Mth.sin(t * 0.07F) * 0.025F * still;
		leftArm.zRot -= Mth.sin(t * 0.07F + 1.4F) * 0.025F * still;
		ModelPart crest = head.getChild("crest");
		crest.xRot = -0.12F + Mth.sin(t * 0.09F - 0.9F) * 0.05F + walk * Mth.sin(state.walkAnimationPos * 0.6662F - 1.4F) * 0.12F - 0.3F * wind;

		// Rags and tabard lag the stride by a beat and swing out sideways when the helm turns.
		cloak.xRot += Mth.sin(state.walkAnimationPos * 0.6662F - 1.2F) * 0.08F * walk;
		cloak.zRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * -0.005F + Mth.sin(t * 0.05F + 0.4F) * 0.03F;
		tabard.zRot = Mth.sin(state.walkAnimationPos * 0.6662F - 0.8F) * 0.12F * walk + Mth.sin(t * 0.06F) * 0.02F;

		// The blow: a heavy lurch (plate does not snap), the pauldrons ringing against it.
		float hit = state.hurt;
		if (hit > 0 && state.deathTime <= 0) {
			float k = (float) (hit * Math.cos((1 - hit) * 5.0F));
			body.xRot -= 0.16F * k;
			head.xRot -= 0.26F * k;
			head.yRot += 0.12F * k;
			rightArm.zRot += 0.22F * k;
			leftArm.zRot -= 0.22F * k;
			cloak.xRot += 0.3F * k;
		}

		// Staged collapse: the helm jerks up, the knees buckle one after the other, the arms hang, the plate slumps forward.
		if (state.deathTime > 0) {
			float d = state.deathTime;
			float jerk = Mth.clamp(d / 3.0F, 0.0F, 1.0F), foldR = Mth.clamp((d - 3.0F) / 7.0F, 0.0F, 1.0F);
			float foldL = Mth.clamp((d - 5.0F) / 7.0F, 0.0F, 1.0F), slump = Mth.clamp((d - 8.0F) / 8.0F, 0.0F, 1.0F);
			head.xRot += -0.4F * jerk * (1 - slump) + 0.55F * slump;
			body.xRot += -0.2F * jerk * (1 - foldR) + 0.45F * slump;
			rightLeg.xRot += -0.9F * foldR;
			leftLeg.xRot += -0.5F * foldL;
			rightArm.xRot = Mth.lerp(slump, rightArm.xRot, 0.35F);
			leftArm.xRot = Mth.lerp(slump, leftArm.xRot, 0.25F);
			rightArm.zRot += 0.3F * slump;
			leftArm.zRot -= 0.3F * slump;
			cloak.xRot += 0.35F * foldR;
		}
	}
}
