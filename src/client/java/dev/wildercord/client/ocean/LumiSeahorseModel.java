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
 * 1:1 360-degree high-voxel rig for Lumi-Seahorse (128x128 sheet, 0.5x sub-voxel scale):
 * Curved S-profile emerald seahorse with 5-spike golden coral reef coronet, translucent cheek gill
 * fins, tubular snout, puffed ribbed emerald/mint chest, glowing 5-ray translucent dorsal fan fin,
 * swept pectoral fins, 8-segment spiraled curled tail, and detailed leather/brass saddle with stirrups.
 */
public final class LumiSeahorseModel extends EntityModel<OceanRenderStates.LumiSeahorseState> {
	private final ModelPart chest;
	private final ModelPart head;
	private final ModelPart dorsalFin;
	private final ModelPart tailA;
	private final ModelPart tailB;
	private final ModelPart tailC;
	private final ModelPart saddle;

	public LumiSeahorseModel(ModelPart root) {
		super(root);
		this.chest = root.getChild("chest");
		this.head = this.chest.getChild("head");
		this.dorsalFin = this.chest.getChild("dorsal_fin");
		this.tailA = this.chest.getChild("tail_a");
		this.tailB = this.tailA.getChild("tail_b");
		this.tailC = this.tailB.getChild("tail_c");
		this.saddle = this.chest.getChild("saddle");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Puffed ribbed chest + arched neck segments + swept pectoral blades
		PartDefinition chest = root.addOrReplaceChild("chest",
			CubeListBuilder.create()
				.texOffs(42, 22).addBox(-6.0F, -10.0F, -6.0F, 12, 14, 12)
				// Ribbed emerald/mint belly armor plates
				.texOffs(0, 50).addBox(-5.0F, -7.0F, -7.2F, 10, 3, 13)
				.texOffs(0, 50).addBox(-5.0F, -2.5F, -7.5F, 10, 3, 13)
				.texOffs(128, 50).addBox(-4.5F, 2.0F, -6.5F, 9, 3, 13)
				// Arched neck rings rising into head
				.texOffs(0, 22).addBox(-5.0F, -17.0F, -4.0F, 10, 8, 10)
				// Swept glowing pectoral cheek fins on chest shoulders
				.texOffs(86, 50).addBox(5.5F, -8.0F, 0.0F, 2, 8, 9)
				.texOffs(86, 50).mirror().addBox(-7.5F, -8.0F, 0.0F, 2, 8, 9),
			PartPose.offset(0.0F, 10.0F, 0.0F));

		// Glowing 5-ray translucent sea-mint Dorsal Fan Fin
		PartDefinition dorsal = chest.addOrReplaceChild("dorsal_fin",
			CubeListBuilder.create().texOffs(48, 50).addBox(-1.0F, -9.0F, 4.0F, 2, 14, 12),
			PartPose.offset(0.0F, -3.0F, 3.0F));
		for (int i = 0; i < 4; i++) {
			dorsal.addOrReplaceChild("dray_" + i,
				CubeListBuilder.create().texOffs(78, 50).addBox(-0.5F, -2.0F, -1.0F, 1, 16, 2),
				PartPose.offsetAndRotation(0.0F, -2.0F, 6.0F, 0.55F + i * 0.25F, 0.0F, 0.0F));
		}

		// Sculpted Head + Tubular Flared Snout + Cheek Gill Fins + 5-Spike Golden Reef Coronet
		chest.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -10.0F, -6.0F, 10, 10, 10)
				.texOffs(42, 0).addBox(-2.5F, -6.0F, -16.0F, 5, 5, 10)
				.texOffs(74, 0).addBox(-2.0F, -5.5F, -17.5F, 4, 4, 3)
				// 5-Spike Golden Coral Coronet
				.texOffs(90, 0).addBox(-1.0F, -17.0F, -4.0F, 2, 7, 2)
				.texOffs(90, 0).addBox(-1.0F, -16.0F, -1.0F, 2, 7, 2)
				.texOffs(90, 0).addBox(-1.0F, -14.5F, 2.0F, 2, 7, 2)
				.texOffs(128, 0).addBox(-3.5F, -14.0F, -2.0F, 2, 6, 2)
				.texOffs(128, 0).mirror().addBox(1.5F, -14.0F, -2.0F, 2, 6, 2)
				// Translucent sea-mint cheek gill fins
				.texOffs(100, 0).addBox(4.8F, -8.0F, -2.0F, 2, 5, 6)
				.texOffs(100, 0).mirror().addBox(-6.8F, -8.0F, -2.0F, 2, 5, 6),
			PartPose.offset(0.0F, -16.0F, -1.0F));

		// Leather & Brass Saddle + Arch + Straps + Stirrup Loops (hidden when unsaddled)
		chest.addOrReplaceChild("saddle",
			CubeListBuilder.create()
				.texOffs(0, 96).addBox(-7.0F, -4.0F, -7.0F, 14, 4, 14)
				.texOffs(58, 96).addBox(-6.0F, -7.0F, 3.0F, 12, 4, 4)
				.texOffs(58, 96).addBox(-6.0F, -6.0F, -7.0F, 12, 4, 4)
				.texOffs(92, 96).addBox(-8.2F, 0.0F, -1.5F, 2, 10, 3)
				.texOffs(92, 96).mirror().addBox(6.2F, 0.0F, -1.5F, 2, 10, 3)
				.texOffs(104, 96).addBox(-8.8F, 9.0F, -0.5F, 3, 3, 1)
				.texOffs(104, 96).mirror().addBox(5.8F, 9.0F, -0.5F, 3, 3, 1),
			PartPose.offset(0.0F, -2.0F, 0.0F));

		// Multi-segment Curled Prehensile Tail coiling through ~270 deg
		PartDefinition tailA = chest.addOrReplaceChild("tail_a",
			CubeListBuilder.create()
				.texOffs(0, 78).addBox(-4.0F, 0.0F, -4.0F, 8, 8, 8)
				.texOffs(140, 0).addBox(-3.5F, 7.0F, -2.5F, 7, 7, 8),
			PartPose.offset(0.0F, 4.0F, 1.0F));
		PartDefinition tailB = tailA.addOrReplaceChild("tail_b",
			CubeListBuilder.create()
				.texOffs(34, 78).addBox(-3.0F, 0.0F, -3.0F, 6, 7, 6)
				.texOffs(172, 0).addBox(-2.5F, 5.5F, -5.0F, 5, 6, 6),
			PartPose.offsetAndRotation(0.0F, 13.0F, 1.5F, -0.78F, 0.0F, 0.0F));
		tailB.addOrReplaceChild("tail_c",
			CubeListBuilder.create()
				.texOffs(60, 78).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4)
				.texOffs(196, 0).addBox(-1.5F, 4.0F, -4.5F, 3, 5, 4),
			PartPose.offsetAndRotation(0.0F, 9.5F, -2.5F, -1.05F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.LumiSeahorseState state) {
		super.setupAnim(state);
		float bob = Mth.sin(state.ageInTicks * 0.16F);
		float dash = state.dashTicks > 0 ? 1.0F : 0.0F;
		chest.y = 10.0F + bob * 1.8F;
		chest.xRot = state.xRot * Mth.DEG_TO_RAD * 0.45F + dash * 0.35F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.65F;
		dorsalFin.yRot = Mth.sin(state.ageInTicks * 0.42F) * (0.28F + dash * 0.35F);
		tailA.xRot = 0.25F + 0.14F * bob + dash * 0.3F;
		tailB.xRot = -0.78F + 0.16F * bob;
		tailC.xRot = -1.05F + 0.18F * bob;
		saddle.visible = state.saddled;
	}
}
