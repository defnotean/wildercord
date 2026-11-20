package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.SakuraVillager;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Custom Japanese kimono NPC model with wide haori sleeves, obi sash, woven amigasa tea-hat
 * (for the Tea Master) or floral kanzashi hairpin (for the Shrine Maiden), and bowing greeting.
 */
public class SakuraVillagerModel extends EntityModel<SakuraVillagerRenderState> {
	private final ModelPart head;
	private final ModelPart hat;
	private final ModelPart kanzashi;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public SakuraVillagerModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.hat = this.head.getChild("hat");
		this.kanzashi = this.head.getChild("kanzashi");
		this.body = root.getChild("body");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, 2.0F, 0.0F));
		head.addOrReplaceChild("hat",
			CubeListBuilder.create()
				.texOffs(0, 40).addBox(-6.0F, -9.5F, -6.0F, 12.0F, 2.0F, 12.0F)
				.texOffs(50, 40).addBox(-3.0F, -11.5F, -3.0F, 6.0F, 2.0F, 6.0F),
			PartPose.ZERO);
		head.addOrReplaceChild("kanzashi",
			CubeListBuilder.create().texOffs(80, 0).addBox(2.5F, -7.5F, -2.0F, 3.0F, 3.0F, 2.0F),
			PartPose.ZERO);

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
			PartPose.offset(0.0F, 2.0F, 0.0F));
		body.addOrReplaceChild("obi",
			CubeListBuilder.create().texOffs(36, 0).addBox(-4.2F, 6.0F, -2.2F, 8.0F, 4.0F, 4.0F),
			PartPose.ZERO);

		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(36, 16).addBox(-3.0F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(-5.0F, 3.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(36, 16).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(5.0F, 3.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(56, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(-2.0F, 14.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(56, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(2.0F, 14.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(SakuraVillagerRenderState state) {
		super.setupAnim(state);
		boolean maiden = state.role == SakuraVillager.SHRINE_MAIDEN;
		hat.visible = !maiden;
		kanzashi.visible = maiden;

		float bow = state.bowTick > 0 ? Mth.sin(Math.min(1.0F, state.bowTick / 26.0F) * Mth.PI) * 0.42F : 0.0F;
		body.xRot = bow * 0.55F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD + bow * 0.85F;

		float walk = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.55F * state.walkAnimationSpeed;
		rightLeg.xRot = walk;
		leftLeg.xRot = -walk;
		rightArm.xRot = -walk * 0.55F - 0.15F + bow * 0.35F;
		leftArm.xRot = walk * 0.55F - 0.15F + bow * 0.35F;
	}
}
