package dev.wildercord.client.monster;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Clockwork wooden-and-brass shrine automaton model with a rotating chest gear, kabuto crest,
 * and sweeping wind-up arms (`64x64` layout painted by {@code tools/shrine_cat_art.py}).
 */
public class KarakuriSentinelModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart gear;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public KarakuriSentinelModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.gear = this.body.getChild("gear");
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 10.0F, 6.0F),
			PartPose.offset(0.0F, 4.0F, 0.0F));
		body.addOrReplaceChild("gear",
			CubeListBuilder.create().texOffs(30, 0).addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F),
			PartPose.offset(0.0F, 5.0F, -3.4F));

		root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 18).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				.texOffs(30, 8).addBox(-5.0F, -10.0F, -2.0F, 10.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, 4.0F, 0.0F));
		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(34, 18).addBox(-3.0F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(-5.0F, 5.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(34, 18).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(5.0F, 5.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(-2.0F, 14.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(0, 38).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(2.0F, 14.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float stride = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.65F * state.walkAnimationSpeed;
		rightLeg.xRot = stride;
		leftLeg.xRot = -stride;

		// Chest gear ticks continuously and revs up during windup.
		gear.zRot = t * (0.06F + state.windup * 0.35F);
		if (state.windup > 0.05F) {
			float raise = -1.45F * state.windup;
			rightArm.xRot = raise;
			leftArm.xRot = raise;
			body.yRot = -0.35F * state.windup;
		} else if (state.acting > 0.05F) {
			float swing = 1.15F * state.acting;
			rightArm.xRot = swing;
			leftArm.xRot = -0.35F;
			body.yRot = 0.55F * state.acting;
		} else {
			body.yRot = 0.0F;
			rightArm.xRot = -stride * 0.7F;
			leftArm.xRot = stride * 0.7F;
		}
	}
}
