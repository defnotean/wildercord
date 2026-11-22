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
 * 1:1 360-degree high-voxel rig for Pearl-Pup (Lantern Seal, 128x128 sheet, 0.5x sub-voxel scale):
 * Plump tapering 4-tier spotted snow-seal body resting at ground Y=24, cute round head with slate
 * muzzle, 4 delicate 3D whiskers, 5-segment curved arching forehead angler-stalk holding a glowing
 * cyan-white lure pearl, sweeping 3-segment flippers, and forked twin rear flippers.
 */
public final class PearlPupModel extends EntityModel<OceanRenderStates.PearlPupState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart lureStalk;
	private final ModelPart leftFlipper;
	private final ModelPart rightFlipper;
	private final ModelPart tailHips;
	private final ModelPart leftRear;
	private final ModelPart rightRear;

	public PearlPupModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.lureStalk = this.head.getChild("lure_stalk");
		this.leftFlipper = this.body.getChild("left_flipper");
		this.rightFlipper = this.body.getChild("right_flipper");
		this.tailHips = this.body.getChild("hips");
		this.leftRear = this.tailHips.getChild("left_rear_flipper");
		this.rightRear = this.tailHips.getChild("right_rear_flipper");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Ground Y = 24.0 at bottom of chest (0.5x scale puts belly right on ground)
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8.0F, -6.5F, -8.0F, 16, 13, 16)
				.texOffs(66, 0).addBox(-7.0F, -5.5F, 6.0F, 14, 12, 12),
			PartPose.offset(0.0F, 17.5F, -2.0F));

		PartDefinition head = body.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 31).addBox(-7.0F, -9.0F, -14.0F, 14, 12, 14)
				.texOffs(58, 31).addBox(-5.0F, -3.5F, -18.5F, 10, 6, 6)
				.texOffs(92, 31).addBox(-2.0F, -3.0F, -19.5F, 4, 2, 2)
				// 4 delicate whiskers (2 per cheek)
				.texOffs(58, 45).addBox(4.5F, -2.0F, -16.5F, 5, 1, 1)
				.texOffs(58, 45).addBox(4.5F, 0.0F, -16.0F, 5, 1, 1)
				.texOffs(58, 45).mirror().addBox(-9.5F, -2.0F, -16.5F, 5, 1, 1)
				.texOffs(58, 45).mirror().addBox(-9.5F, 0.0F, -16.0F, 5, 1, 1),
			PartPose.offset(0.0F, -1.0F, -7.0F));

		// Curved 5-segment arching forehead angler stalk holding a glowing cyan-white pearl lure
		PartDefinition stalk = head.addOrReplaceChild("lure_stalk",
			CubeListBuilder.create().texOffs(72, 45).addBox(-1.0F, -8.0F, -1.0F, 2, 8, 2),
			PartPose.offsetAndRotation(0.0F, -8.0F, -7.0F, 0.35F, 0.0F, 0.0F));
		PartDefinition stalkTip = stalk.addOrReplaceChild("stalk_arc",
			CubeListBuilder.create().texOffs(108, 45).addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2),
			PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, 0.85F, 0.0F, 0.0F));
		stalkTip.addOrReplaceChild("lure_pearl",
			CubeListBuilder.create().texOffs(82, 45).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6),
			PartPose.offset(0.0F, -7.5F, -1.5F));

		// Sweeping front flippers
		body.addOrReplaceChild("left_flipper",
			CubeListBuilder.create()
				.texOffs(78, 60).addBox(0.0F, -1.5F, -4.0F, 12, 4, 8)
				.texOffs(40, 81).addBox(10.0F, -1.0F, -3.0F, 8, 3, 6),
			PartPose.offsetAndRotation(7.5F, 3.0F, -3.0F, 0.0F, -0.35F, 0.16F));

		body.addOrReplaceChild("right_flipper",
			CubeListBuilder.create()
				.texOffs(78, 60).mirror().addBox(-12.0F, -1.5F, -4.0F, 12, 4, 8)
				.texOffs(40, 81).mirror().addBox(-18.0F, -1.0F, -3.0F, 8, 3, 6),
			PartPose.offsetAndRotation(-7.5F, 3.0F, -3.0F, 0.0F, 0.35F, -0.16F));

		// Tapering Hips + Tail Base + V-forked Twin Rear Flippers
		PartDefinition hips = body.addOrReplaceChild("hips",
			CubeListBuilder.create()
				.texOffs(0, 60).addBox(-5.5F, -4.0F, 0.0F, 11, 9, 10)
				.texOffs(44, 60).addBox(-4.0F, -3.0F, 8.0F, 8, 7, 8),
			PartPose.offset(0.0F, 1.0F, 17.0F));

		hips.addOrReplaceChild("left_rear_flipper",
			CubeListBuilder.create().texOffs(0, 81).addBox(0.0F, -1.5F, 0.0F, 8, 3, 11),
			PartPose.offsetAndRotation(2.0F, 2.0F, 14.0F, 0.0F, 0.36F, 0.0F));

		hips.addOrReplaceChild("right_rear_flipper",
			CubeListBuilder.create().texOffs(0, 81).mirror().addBox(-8.0F, -1.5F, 0.0F, 8, 3, 11),
			PartPose.offsetAndRotation(-2.0F, 2.0F, 14.0F, 0.0F, -0.36F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.PearlPupState state) {
		super.setupAnim(state);
		float wag = Mth.sin(state.ageInTicks * 0.16F + state.walkAnimationPos * 0.7F);
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.8F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.65F;
		body.yRot = wag * 0.07F;
		tailHips.yRot = -wag * (0.22F + 0.16F * state.walkAnimationSpeed);
		leftRear.yRot = 0.36F + wag * 0.15F;
		rightRear.yRot = -0.36F + wag * 0.15F;
		leftFlipper.zRot = 0.16F + Mth.cos(state.ageInTicks * 0.2F) * 0.14F;
		rightFlipper.zRot = -leftFlipper.zRot;
		lureStalk.xRot = state.sniffing ? (0.48F + 0.24F * Mth.sin(state.ageInTicks * 0.45F)) : (0.35F + 0.06F * wag);
	}
}
