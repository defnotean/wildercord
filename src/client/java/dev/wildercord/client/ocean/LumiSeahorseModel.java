package dev.wildercord.client.ocean;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree box rig for Lumi-Seahorse Mount (64x64 sheet, entityTranslucent):
 * Crowned crest & tubular snout, curved neck & torso, fluttering sea-mint dorsal fin,
 * 3-joint curled spiral tail, and toggleable brass-and-aqua riding saddle + stirrups.
 */
public final class LumiSeahorseModel extends EntityModel<OceanRenderStates.LumiSeahorseState> {
	private final ModelPart torso;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart dorsalFin;
	private final ModelPart saddle;
	private final ModelPart tail0;
	private final ModelPart tail1;
	private final ModelPart tail2;

	public LumiSeahorseModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.torso = root.getChild("torso");
		this.neck = this.torso.getChild("neck");
		this.head = this.neck.getChild("head");
		this.dorsalFin = this.torso.getChild("dorsal_fin");
		this.saddle = this.torso.getChild("saddle");
		this.tail0 = this.torso.getChild("tail_0");
		this.tail1 = this.tail0.getChild("tail_1");
		this.tail2 = this.tail1.getChild("tail_2");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -5.0F, -3.0F, 6, 10, 6),
			PartPose.offset(0.0F, 12.0F, 0.0F));

		torso.addOrReplaceChild("dorsal_fin",
			CubeListBuilder.create().texOffs(0, 28).addBox(-0.5F, -3.0F, 0.0F, 1, 6, 6),
			PartPose.offset(0.0F, -1.0F, 3.0F));

		torso.addOrReplaceChild("saddle",
			CubeListBuilder.create()
				.texOffs(0, 40).addBox(-3.5F, -3.0F, -3.5F, 7, 4, 7)
				.texOffs(28, 40).addBox(3.2F, 1.0F, -0.5F, 1, 4, 1)
				.texOffs(28, 40).mirror().addBox(-4.2F, 1.0F, -0.5F, 1, 4, 1),
			PartPose.ZERO);

		PartDefinition neck = torso.addOrReplaceChild("neck",
			CubeListBuilder.create().texOffs(24, 0).addBox(-2.5F, -6.0F, -2.5F, 5, 6, 5),
			PartPose.offsetAndRotation(0.0F, -5.0F, -0.5F, 0.25F, 0.0F, 0.0F));

		neck.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 16).addBox(-3.0F, -6.0F, -3.5F, 6, 6, 6)
				.texOffs(24, 16).addBox(-1.0F, -3.0F, -8.5F, 2, 2, 5)
				.texOffs(38, 16).addBox(-0.5F, -10.0F, -2.5F, 1, 4, 5)
				.texOffs(44, 0).addBox(3.0F, -4.0F, -0.5F, 2, 3, 1)
				.texOffs(44, 0).mirror().addBox(-5.0F, -4.0F, -0.5F, 2, 3, 1),
			PartPose.offset(0.0F, -5.5F, 0.0F));

		PartDefinition tail0 = torso.addOrReplaceChild("tail_0",
			CubeListBuilder.create().texOffs(14, 28).addBox(-2.0F, 0.0F, -2.0F, 4, 4, 4),
			PartPose.offsetAndRotation(0.0F, 5.0F, 0.5F, 0.45F, 0.0F, 0.0F));
		PartDefinition tail1 = tail0.addOrReplaceChild("tail_1",
			CubeListBuilder.create().texOffs(30, 28).addBox(-1.5F, 0.0F, -2.0F, 3, 3, 4),
			PartPose.offsetAndRotation(0.0F, 3.8F, 0.0F, 0.65F, 0.0F, 0.0F));
		tail1.addOrReplaceChild("tail_2",
			CubeListBuilder.create().texOffs(44, 28).addBox(-1.0F, 0.0F, -1.5F, 2, 2, 3),
			PartPose.offsetAndRotation(0.0F, 2.8F, -0.5F, 0.75F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(OceanRenderStates.LumiSeahorseState state) {
		super.setupAnim(state);
		this.saddle.visible = state.saddled;
		float bob = Mth.sin(state.ageInTicks * 0.14F);
		this.torso.y = 12.0F + bob * 1.1F;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.45F - 0.15F;

		float finRate = state.dashTicks > 0 ? 0.9F : 0.45F;
		this.dorsalFin.yRot = Mth.sin(state.ageInTicks * finRate) * 0.45F;
		this.tail0.xRot = 0.45F + Mth.sin(state.ageInTicks * 0.2F) * 0.16F;
		this.tail1.xRot = 0.65F + Mth.sin(state.ageInTicks * 0.2F - 0.6F) * 0.18F;
		this.tail2.xRot = 0.75F + Mth.sin(state.ageInTicks * 0.2F - 1.2F) * 0.20F;
	}
}
