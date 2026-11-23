package dev.wildercord.client.ocean;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree armored translucent rig for the Lumi-Seahorse mount (256x256 sheet, 0.5x scale):
 * Segmented bone-plated S-curved body, flared trumpet snout, 3-prong coral coronet crown,
 * glowing amber-cyan brood pouch, translucent gossamer cheek &amp; fluttering back wing fins,
 * saddle + brass stirrup hardware when tamed, and a 7-segment ~300-degree inward spiral tail.
 */
public final class LumiSeahorseModel extends EntityModel<OceanRenderStates.LumiSeahorseState> {
	private final ModelPart chest;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart dorsalFin;
	private final ModelPart saddle;
	private final ModelPart[] tailSegs = new ModelPart[7];

	public LumiSeahorseModel(ModelPart root) {
		super(root, RenderType::entityTranslucent);
		this.chest = root.getChild("chest");
		this.neck = this.chest.getChild("neck");
		this.head = this.neck.getChild("head");
		this.dorsalFin = this.chest.getChild("dorsal_fin");
		this.saddle = this.chest.getChild("saddle");
		ModelPart cur = this.chest;
		for (int i = 0; i < 7; i++) {
			cur = cur.getChild("tail_" + (i + 1));
			this.tailSegs[i] = cur;
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Arched Armored Seahorse Chest + 3 Raised Armor Belt Ridges + Bioluminescent Pouch + Conditional Saddle
		PartDefinition chest = root.addOrReplaceChild("chest",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12, 16, 12)
				.texOffs(152, 0).addBox(-5.5F, -5.0F, -6.5F, 11, 3, 13)
				.texOffs(152, 0).addBox(-5.5F, 0.5F, -6.5F, 11, 3, 13)
				.texOffs(152, 0).addBox(-5.5F, 5.5F, -6.5F, 11, 3, 13)
				.texOffs(56, 29).addBox(-4.0F, -1.0F, -8.2F, 8, 9, 4),
			PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.12F, 0.0F, 0.0F));

		chest.addOrReplaceChild("dorsal_fin",
			CubeListBuilder.create().texOffs(49, 0).addBox(-0.5F, -7.0F, 0.0F, 1, 14, 10),
			PartPose.offset(0.0F, -1.0F, 6.0F));

		chest.addOrReplaceChild("saddle",
			CubeListBuilder.create()
				.texOffs(201, 0).addBox(-6.5F, -7.0F, -5.0F, 13, 3, 12)
				.texOffs(174, 29).addBox(-7.5F, -4.0F, -1.5F, 2, 7, 3)
				.texOffs(174, 29).mirror().addBox(5.5F, -4.0F, -1.5F, 2, 7, 3),
			PartPose.ZERO);

		// Arched S-Curved Neck -> Crowned Head + Flared Trumpet Snout + Translucent Cheek Fins
		PartDefinition neck = chest.addOrReplaceChild("neck",
			CubeListBuilder.create().texOffs(113, 0).addBox(-4.5F, -9.5F, -5.0F, 9, 10, 10),
			PartPose.offsetAndRotation(0.0F, -7.5F, 0.5F, 0.25F, 0.0F, 0.0F));

		PartDefinition head = neck.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(72, 0).addBox(-5.0F, -9.0F, -6.0F, 10, 10, 10)
				.texOffs(29, 29).addBox(-2.0F, -4.0F, -15.0F, 4, 4, 9)
				.texOffs(209, 29).addBox(-2.5F, -4.5F, -16.8F, 5, 5, 2)
				.texOffs(142, 29).addBox(-1.5F, -15.5F, -2.5F, 3, 7, 4),
			PartPose.offsetAndRotation(0.0F, -9.0F, -1.5F, -0.32F, 0.0F, 0.0F));
		head.addOrReplaceChild("crown_left",
			CubeListBuilder.create().texOffs(198, 29).addBox(-1.0F, -5.0F, -1.5F, 2, 5, 3),
			PartPose.offsetAndRotation(3.0F, -11.5F, -1.0F, -0.15F, 0.0F, 0.45F));
		head.addOrReplaceChild("crown_right",
			CubeListBuilder.create().texOffs(198, 29).mirror().addBox(-1.0F, -5.0F, -1.5F, 2, 5, 3),
			PartPose.offsetAndRotation(-3.0F, -11.5F, -1.0F, -0.15F, 0.0F, -0.45F));
		head.addOrReplaceChild("cheek_fin_r",
			CubeListBuilder.create().texOffs(106, 29).addBox(-0.5F, -3.5F, 0.0F, 1, 7, 6),
			PartPose.offsetAndRotation(-5.2F, -4.0F, -1.0F, 0.0F, -0.48F, 0.0F));
		head.addOrReplaceChild("cheek_fin_l",
			CubeListBuilder.create().texOffs(106, 29).mirror().addBox(-0.5F, -3.5F, 0.0F, 1, 7, 6),
			PartPose.offsetAndRotation(5.2F, -4.0F, -1.0F, 0.0F, 0.48F, 0.0F));

		// 7-Segment Tapered ~300-degree Prehensile Spiral Tail
		PartDefinition t1 = chest.addOrReplaceChild("tail_1",
			CubeListBuilder.create().texOffs(0, 29).addBox(-3.5F, 0.0F, -3.5F, 7, 8, 7),
			PartPose.offsetAndRotation(0.0F, 7.5F, 0.5F, 0.35F, 0.0F, 0.0F));
		PartDefinition t2 = t1.addOrReplaceChild("tail_2",
			CubeListBuilder.create().texOffs(81, 29).addBox(-3.0F, 0.0F, -3.0F, 6, 7, 6),
			PartPose.offsetAndRotation(0.0F, 7.2F, 0.0F, 0.55F, 0.0F, 0.0F));
		PartDefinition t3 = t2.addOrReplaceChild("tail_3",
			CubeListBuilder.create().texOffs(121, 29).addBox(-2.5F, 0.0F, -2.5F, 5, 6, 5),
			PartPose.offsetAndRotation(0.0F, 6.2F, 0.0F, 0.65F, 0.0F, 0.0F));
		PartDefinition t4 = t3.addOrReplaceChild("tail_4",
			CubeListBuilder.create().texOffs(157, 29).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offsetAndRotation(0.0F, 5.4F, 0.0F, 0.72F, 0.0F, 0.0F));
		PartDefinition t5 = t4.addOrReplaceChild("tail_5",
			CubeListBuilder.create().texOffs(185, 29).addBox(-1.5F, 0.0F, -1.5F, 3, 5, 3),
			PartPose.offsetAndRotation(0.0F, 5.2F, 0.0F, 0.75F, 0.0F, 0.0F));
		PartDefinition t6 = t5.addOrReplaceChild("tail_6",
			CubeListBuilder.create().texOffs(224, 29).addBox(-1.5F, 0.0F, -1.5F, 3, 4, 3),
			PartPose.offsetAndRotation(0.0F, 4.4F, 0.0F, 0.78F, 0.0F, 0.0F));
		t6.addOrReplaceChild("tail_7",
			CubeListBuilder.create().texOffs(237, 29).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
			PartPose.offsetAndRotation(0.0F, 3.6F, 0.0F, 0.80F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.LumiSeahorseState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float dash = state.dashTicks > 0 ? 1.5F : 1.0F;
		chest.y = 0.0F + Mth.sin(t * 0.16F) * 1.4F;
		chest.xRot = 0.12F + (state.dashTicks > 0 ? 0.28F : state.walkAnimationSpeed * 0.18F);

		neck.xRot = 0.25F + Mth.sin(t * 0.14F) * 0.05F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
		head.xRot = -0.32F + state.xRot * Mth.DEG_TO_RAD * 0.5F;

		dorsalFin.yRot = Mth.sin(t * 0.55F * dash) * 0.48F;
		saddle.visible = state.saddled;

		float uncurl = state.dashTicks > 0 ? -0.18F : Mth.sin(t * 0.12F) * 0.04F;
		float[] baseX = {0.35F, 0.55F, 0.65F, 0.72F, 0.75F, 0.78F, 0.80F};
		for (int i = 0; i < 7; i++) {
			tailSegs[i].xRot = baseX[i] + uncurl * (1.0F + i * 0.12F);
		}
	}
}
