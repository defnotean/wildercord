package dev.wildercord.client.ocean;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree box rig for Pearl-Pup (Lantern Seal, 64x32 sheet):
 * Plump spotted body [8x7x12], front/rear flippers, spherical head [7x6x7] with snout [3x2x2],
 * and bobbing emissive antenna lure [1x5x1 + 3x3x3 pearl] on the brow.
 */
public final class PearlPupModel extends EntityModel<OceanRenderStates.PearlPupState> {
	private final ModelPart head;
	private final ModelPart lureStalk;
	private final ModelPart leftFlipper;
	private final ModelPart rightFlipper;
	private final ModelPart rump;

	public PearlPupModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.lureStalk = this.head.getChild("lure_stalk");
		this.leftFlipper = root.getChild("front_flipper_left");
		this.rightFlipper = root.getChild("front_flipper_right");
		this.rump = root.getChild("rump");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("torso",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.5F, -6.0F, 8, 7, 12),
			PartPose.offset(0.0F, 20.5F, 0.0F));

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 19).addBox(-3.5F, -5.0F, -6.0F, 7, 6, 7)
				.texOffs(50, 19).addBox(-1.5F, -2.0F, -8.0F, 3, 2, 2),
			PartPose.offset(0.0F, 19.5F, -6.0F));

		PartDefinition lure = head.addOrReplaceChild("lure_stalk",
			CubeListBuilder.create().texOffs(60, 19).addBox(-0.5F, -5.0F, -0.5F, 1, 5, 1),
			PartPose.offsetAndRotation(0.0F, -5.0F, -2.5F, 0.35F, 0.0F, 0.0F));
		lure.addOrReplaceChild("lure_pearl",
			CubeListBuilder.create().texOffs(50, 23).addBox(-1.5F, -3.0F, -1.5F, 3, 3, 3),
			PartPose.offset(0.0F, -5.0F, -0.5F));

		root.addOrReplaceChild("front_flipper_left",
			CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, -1.0F, -2.0F, 5, 2, 4),
			PartPose.offset(3.5F, 22.0F, -3.0F));
		root.addOrReplaceChild("front_flipper_right",
			CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-5.0F, -1.0F, -2.0F, 5, 2, 4),
			PartPose.offset(-3.5F, 22.0F, -3.0F));

		PartDefinition rump = root.addOrReplaceChild("rump",
			CubeListBuilder.create().texOffs(28, 19).addBox(-3.0F, -2.0F, 0.0F, 6, 4, 5),
			PartPose.offset(0.0F, 21.0F, 6.0F));
		rump.addOrReplaceChild("rear_flipper_left",
			CubeListBuilder.create().texOffs(40, 6).addBox(0.0F, -1.0F, 0.0F, 4, 2, 5),
			PartPose.offsetAndRotation(1.0F, 0.5F, 4.5F, 0.0F, 0.2F, 0.0F));
		rump.addOrReplaceChild("rear_flipper_right",
			CubeListBuilder.create().texOffs(40, 6).mirror().addBox(-4.0F, -1.0F, 0.0F, 4, 2, 5),
			PartPose.offsetAndRotation(-1.0F, 0.5F, 4.5F, 0.0F, -0.2F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(OceanRenderStates.PearlPupState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		float waddle = Mth.sin(state.walkAnimationPos * 1.1F) * state.walkAnimationSpeed;
		this.leftFlipper.zRot = waddle * 0.45F;
		this.rightFlipper.zRot = -waddle * 0.45F;
		this.rump.yRot = Mth.sin(state.ageInTicks * 0.22F) * 0.16F;

		float bobSpeed = state.sniffing ? 0.35F : 0.12F;
		this.lureStalk.xRot = 0.35F + Mth.sin(state.ageInTicks * bobSpeed) * (state.sniffing ? 0.24F : 0.09F);
	}
}
