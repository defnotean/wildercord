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
 * 1:1 360-degree Chubby Abyssal Seal-Pup rig for the Pearl-Pup (256x256 sheet, 0.5x scale):
 * Snow-aqua &amp; cream spotted seal body with fluffy chest puff, winged fore-flippers, V-flared
 * hind tail flippers, flared cheek frills, cute muzzle, glossy 3x3 catchlight puppy eyes, and a
 * high-arching bioluminescent Pearl Lure Stalk rising well ABOVE the brow so the face stays clear.
 */
public final class PearlPupModel extends EntityModel<OceanRenderStates.PearlPupState> {
	private final ModelPart torso;
	private final ModelPart head;
	private final ModelPart lureBase;
	private final ModelPart lureTip;
	private final ModelPart rightFlipper;
	private final ModelPart leftFlipper;
	private final ModelPart tailBase;

	public PearlPupModel(ModelPart root) {
		super(root);
		this.torso = root.getChild("torso");
		this.head = this.torso.getChild("head");
		this.lureBase = this.head.getChild("lure_base");
		this.lureTip = this.lureBase.getChild("lure_tip");
		this.rightFlipper = this.torso.getChild("right_flipper");
		this.leftFlipper = this.torso.getChild("left_flipper");
		this.tailBase = this.torso.getChild("tail_base");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Plump Seal-Pup Torso resting naturally on seafloor at Y=18.5 (bottom at Y=24.0 * 0.5 -> ground Y=0)
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.5F, -5.5F, -8.0F, 13, 11, 16)
				.texOffs(102, 0).addBox(-5.5F, -3.5F, -9.5F, 11, 8, 7),
			PartPose.offset(0.0F, 18.5F, 0.0F));

		// Rounded Seal Head + Soft Muzzle + Flared Axolotl-Seal Cheek Frills
		PartDefinition head = torso.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(59, 0).addBox(-5.5F, -8.5F, -9.5F, 11, 10, 10)
				.texOffs(9, 28).addBox(-3.0F, -3.0F, -12.5F, 6, 4, 4),
			PartPose.offset(0.0F, -2.5F, -7.5F));
		head.addOrReplaceChild("cheek_frill_r",
			CubeListBuilder.create().texOffs(47, 28).addBox(-4.0F, -2.5F, -1.0F, 4, 5, 2),
			PartPose.offsetAndRotation(-5.0F, -3.0F, -4.5F, 0.0F, -0.35F, -0.18F));
		head.addOrReplaceChild("cheek_frill_l",
			CubeListBuilder.create().texOffs(47, 28).mirror().addBox(0.0F, -2.5F, -1.0F, 4, 5, 2),
			PartPose.offsetAndRotation(5.0F, -3.0F, -4.5F, 0.0F, 0.35F, 0.18F));

		// High-Arching Pearl Lure Stalk rising upward above brow (-Z forward arch over top of forehead)
		PartDefinition lureBase = head.addOrReplaceChild("lure_base",
			CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2),
			PartPose.offsetAndRotation(0.0F, -8.0F, -3.5F, -0.15F, 0.0F, 0.0F));
		PartDefinition lureTip = lureBase.addOrReplaceChild("lure_tip",
			CubeListBuilder.create()
				.texOffs(60, 28).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2)
				.texOffs(30, 28).addBox(-2.0F, -8.0F, -3.0F, 4, 4, 4)
				.texOffs(172, 0).addBox(-2.5F, -8.5F, -3.5F, 5, 5, 5),
			PartPose.offsetAndRotation(0.0F, -6.5F, 0.0F, 0.72F, 0.0F, 0.0F));

		// Swept Fore-Flippers
		torso.addOrReplaceChild("right_flipper",
			CubeListBuilder.create().texOffs(193, 0).addBox(-9.0F, -1.5F, -3.0F, 9, 3, 6),
			PartPose.offsetAndRotation(-6.0F, 3.0F, -3.5F, 0.05F, 0.32F, -0.16F));
		torso.addOrReplaceChild("left_flipper",
			CubeListBuilder.create().texOffs(193, 0).mirror().addBox(0.0F, -1.5F, -3.0F, 9, 3, 6),
			PartPose.offsetAndRotation(6.0F, 3.0F, -3.5F, 0.05F, -0.32F, 0.16F));

		// Chubby Rear Seal Tail & V-Flared Hind Flippers
		PartDefinition tailBase = torso.addOrReplaceChild("tail_base",
			CubeListBuilder.create().texOffs(139, 0).addBox(-4.0F, -3.0F, 0.0F, 8, 6, 8),
			PartPose.offset(0.0F, 1.5F, 7.5F));
		tailBase.addOrReplaceChild("hind_flipper_r",
			CubeListBuilder.create().texOffs(224, 0).addBox(-4.5F, -1.0F, 0.0F, 5, 2, 7),
			PartPose.offsetAndRotation(-1.5F, 1.0F, 7.0F, 0.08F, -0.38F, -0.06F));
		tailBase.addOrReplaceChild("hind_flipper_l",
			CubeListBuilder.create().texOffs(224, 0).mirror().addBox(-0.5F, -1.0F, 0.0F, 5, 2, 7),
			PartPose.offsetAndRotation(1.5F, 1.0F, 7.0F, 0.08F, 0.38F, 0.06F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.PearlPupState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float waddle = Mth.cos(state.walkAnimationPos * 0.85F) * 0.45F * state.walkAnimationSpeed;
		torso.zRot = waddle * 0.25F;

		float sniffBob = state.sniffing ? Mth.sin(t * 0.55F) * 0.14F + 0.12F : 0.0F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F + sniffBob;

		lureBase.xRot = -0.15F + Mth.sin(t * 0.12F) * 0.08F;
		lureTip.xRot = 0.72F + Mth.cos(t * 0.14F) * 0.10F;

		rightFlipper.yRot = 0.32F + waddle;
		leftFlipper.yRot = -0.32F + waddle;
		tailBase.yRot = Mth.sin(t * 0.20F) * (0.14F + 0.22F * state.walkAnimationSpeed);
	}
}
