package dev.wildercord.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Cinder Warden: a hulking, hunched figure of black iron plates over magma, near three blocks
 * tall, a small helm sunk between great pauldrons, arms hanging almost to its knees in heavy
 * gauntlets, and a length of chain swinging from each wrist. Its core burns in the cracks of its
 * chest. Walking, it lumbers; casting, it brings its fists together before its core; winding up a
 * blow, both arms go up over its head; between phases it throws them wide, head back, and shakes;
 * dying, it sinks to its knees and slumps forward.
 *
 * <p>Laid out on a 128x128 skin (see {@code cinder_warden_texture} in {@code tools/dungeon_art.py}):</p>
 * <pre>
 *   torso (0,0) 18x18x11     helm (60,0) 9x9x9         arm (98,0) 7x16x7
 *   pauldron (60,18) 9x6x10  leg (0,30) 8x20x8         belt (32,34) 17x5x10
 *   gauntlet (96,34) 8x7x8   chain (0,60) 2x12x2
 * </pre>
 */
public class CinderWardenModel extends EntityModel<DungeonBossRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightChain;
	private final ModelPart leftChain;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public CinderWardenModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightChain = rightArm.getChild("chain");
		this.leftChain = leftArm.getChild("chain");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// The torso, pivoting at the hips, with its belt; a small helm sunk low and forward between the shoulders.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-9.0F, -18.0F, -5.5F, 18.0F, 18.0F, 11.0F)
				.texOffs(32, 34).addBox(-8.5F, -2.0F, -5.0F, 17.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, 5.0F, 0.0F));
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(60, 0).addBox(-4.5F, -8.0F, -4.5F, 9.0F, 9.0F, 9.0F),
			PartPose.offset(0.0F, -17.0F, -3.0F));
		body.addOrReplaceChild("right_pauldron", CubeListBuilder.create().texOffs(60, 18).addBox(-4.5F, -3.0F, -5.0F, 9.0F, 6.0F, 10.0F),
			PartPose.offsetAndRotation(-9.5F, -17.0F, 0.0F, 0.0F, 0.0F, 0.25F));
		body.addOrReplaceChild("left_pauldron", CubeListBuilder.create().texOffs(60, 18).mirror().addBox(-4.5F, -3.0F, -5.0F, 9.0F, 6.0F, 10.0F),
			PartPose.offsetAndRotation(9.5F, -17.0F, 0.0F, 0.0F, 0.0F, -0.25F));

		// Long arms in heavy gauntlets, a chain hanging from each wrist.
		PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(98, 0).addBox(-3.5F, -1.0F, -3.5F, 7.0F, 16.0F, 7.0F)
				.texOffs(96, 34).addBox(-4.0F, 14.0F, -4.0F, 8.0F, 7.0F, 8.0F),
			PartPose.offset(-12.0F, -14.0F, 0.0F));
		rightArm.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(0, 60).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
			PartPose.offset(0.0F, 20.0F, 2.5F));
		PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(98, 0).mirror().addBox(-3.5F, -1.0F, -3.5F, 7.0F, 16.0F, 7.0F)
				.texOffs(96, 34).mirror().addBox(-4.0F, 14.0F, -4.0F, 8.0F, 7.0F, 8.0F),
			PartPose.offset(12.0F, -14.0F, 0.0F));
		leftArm.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(0, 60).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
			PartPose.offset(0.0F, 20.0F, 2.5F));

		// Thick legs, planted wide.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 20.0F, 8.0F),
			PartPose.offset(-4.5F, 5.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-4.0F, -1.0F, -4.0F, 8.0F, 20.0F, 8.0F),
			PartPose.offset(4.5F, 5.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(DungeonBossRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float s = state.slamming;
		float sh = state.shifting;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos;
		float die = Math.min(1, state.deathTime / 30.0F);
		float slump = Math.max(0, state.deathTime - 30) / 30.0F;

		// A heavy, rolling gait; it breathes, slowly, even standing.
		float breathe = Mth.sin(t * 0.06F) * 0.03F;
		rightLeg.xRot = Mth.cos(pos * 0.45F) * 0.55F * walk;
		leftLeg.xRot = -Mth.cos(pos * 0.45F) * 0.55F * walk;
		body.zRot = Mth.cos(pos * 0.45F) * 0.05F * walk;
		// Hunched forward; leaning back to swing both fists up; shaking between phases; sinking to its knees as it dies.
		body.xRot = 0.18F + breathe - 0.35F * s + sh * Mth.sin(t * 1.3F) * 0.04F + die * 0.25F + slump * 0.45F;
		body.y = 5.0F + die * 7.0F;
		rightLeg.y = 5.0F + die * 7.0F;
		leftLeg.y = 5.0F + die * 7.0F;
		if (die > 0) {
			rightLeg.xRot = -1.3F * die;
			leftLeg.xRot = -1.3F * die;
		}

		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - sh);
		head.xRot = Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD * (1 - sh) - 0.5F * sh - 0.2F * s;

		// Arms: swinging with the gait; casting, fists brought together before its core; a blow wound up over its head;
		// thrown wide between phases; hanging limp as it dies.
		float swing = Mth.cos(pos * 0.45F) * 0.45F * walk;
		poseArm(rightArm, 1, -swing, c, s, sh, t, die);
		poseArm(leftArm, -1, swing, c, s, sh, t + 11, die);

		// The chains swing after the arms, and with the step.
		rightChain.xRot = -rightArm.xRot * 0.6F + Mth.sin(t * 0.12F) * 0.15F + Mth.sin(pos * 0.45F) * 0.25F * walk;
		leftChain.xRot = -leftArm.xRot * 0.6F + Mth.sin(t * 0.12F + 1.7F) * 0.15F - Mth.sin(pos * 0.45F) * 0.25F * walk;
		rightChain.zRot = Mth.sin(t * 0.09F) * 0.1F;
		leftChain.zRot = -Mth.sin(t * 0.09F + 0.8F) * 0.1F;
	}

	private static void poseArm(ModelPart arm, int side, float swing, float c, float s, float sh, float t, float die) {
		float idle = Mth.sin(t * 0.05F) * 0.04F;
		float xRot = Mth.lerp(c, swing + idle, -1.25F + Mth.sin(t * 0.4F) * 0.05F);
		float zRot = Mth.lerp(c, 0.08F * side, -0.45F * side);
		xRot = Mth.lerp(s, xRot, -2.9F);
		zRot = Mth.lerp(s, zRot, -0.15F * side);
		xRot = Mth.lerp(sh, xRot, -0.6F + Mth.sin(t * 1.1F) * 0.08F);
		zRot = Mth.lerp(sh, zRot, 1.5F * side);
		arm.xRot = Mth.lerp(die, xRot, 0.2F);
		arm.zRot = Mth.lerp(die, zRot, 0.05F * side);
		arm.yRot = 0;
	}
}
