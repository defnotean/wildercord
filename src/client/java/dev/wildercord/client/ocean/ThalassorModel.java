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
 * 1:1 360-degree Sea-Sovereign Leviathan rig for Thalassor (256x256 sheet, 0.72x render scale):
 * Abyssal navy-obsidian armored scales with glowing geometric cyan runes, translucent teal
 * sea-fan cheek fins, pectoral wings, dorsal crest sail &amp; crescent caudal fan, golden-cyan crown,
 * 2-tier sculpted belly scutes, articulated jaw with fangs, and 5-segment serpentine tail.
 */
public final class ThalassorModel extends EntityModel<OceanRenderStates.ThalassorState> {
	private final ModelPart chest;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightFin;
	private final ModelPart leftFin;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart tail1;
	private final ModelPart tail2;
	private final ModelPart tail3;
	private final ModelPart tail4;
	private final ModelPart tailFan;

	public ThalassorModel(ModelPart root) {
		super(root, RenderType::entityTranslucent);
		this.chest = root.getChild("chest");
		this.neck = this.chest.getChild("neck");
		this.head = this.neck.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.rightFin = this.chest.getChild("right_fin");
		this.leftFin = this.chest.getChild("left_fin");
		this.rightWing = this.rightFin.getChild("right_wing");
		this.leftWing = this.leftFin.getChild("left_wing");
		this.tail1 = this.chest.getChild("tail_1");
		this.tail2 = this.tail1.getChild("tail_2");
		this.tail3 = this.tail2.getChild("tail_3");
		this.tail4 = this.tail3.getChild("tail_4");
		this.tailFan = this.tail4.getChild("tail_fan");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Broad Leviathan Chest + 2-Tier Stepped Golden-Ivory Plastron Belly Scutes + Translucent Dorsal Sail
		PartDefinition chest = root.addOrReplaceChild("chest",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-11.0F, -10.0F, -12.0F, 22, 20, 24)
				.texOffs(61, 82).addBox(-8.0F, 8.5F, -11.0F, 16, 4, 22)
				.texOffs(63, 112).addBox(-6.0F, 11.5F, -10.0F, 12, 3, 20),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		chest.addOrReplaceChild("dorsal_sail",
			CubeListBuilder.create().texOffs(138, 0).addBox(-1.0F, -14.0F, -12.0F, 2, 14, 24),
			PartPose.offset(0.0F, -9.5F, 1.0F));
		chest.addOrReplaceChild("pelvic_r",
			CubeListBuilder.create().texOffs(175, 138).addBox(-9.0F, -1.0F, -4.0F, 9, 2, 8),
			PartPose.offsetAndRotation(-8.0F, 8.0F, 8.0F, 0.0F, 0.25F, -0.35F));
		chest.addOrReplaceChild("pelvic_l",
			CubeListBuilder.create().texOffs(175, 138).mirror().addBox(0.0F, -1.0F, -4.0F, 9, 2, 8),
			PartPose.offsetAndRotation(8.0F, 8.0F, 8.0F, 0.0F, -0.25F, 0.35F));

		// Pectoral Wing-Fins (armored fore-arm + huge translucent veined wing membrane)
		PartDefinition rightFin = chest.addOrReplaceChild("right_fin",
			CubeListBuilder.create().texOffs(138, 138).addBox(-10.0F, -2.0F, -4.0F, 10, 4, 8),
			PartPose.offsetAndRotation(-11.0F, 2.0F, -5.0F, 0.08F, 0.25F, -0.18F));
		rightFin.addOrReplaceChild("right_wing",
			CubeListBuilder.create().texOffs(15, 138).addBox(-18.0F, -1.0F, -6.0F, 18, 2, 16),
			PartPose.offsetAndRotation(-9.0F, 0.0F, 1.0F, 0.0F, 0.28F, -0.12F));

		PartDefinition leftFin = chest.addOrReplaceChild("left_fin",
			CubeListBuilder.create().texOffs(138, 138).mirror().addBox(0.0F, -2.0F, -4.0F, 10, 4, 8),
			PartPose.offsetAndRotation(11.0F, 2.0F, -5.0F, 0.08F, -0.25F, 0.18F));
		leftFin.addOrReplaceChild("left_wing",
			CubeListBuilder.create().texOffs(15, 138).mirror().addBox(0.0F, -1.0F, -6.0F, 18, 2, 16),
			PartPose.offsetAndRotation(9.0F, 0.0F, 1.0F, 0.0F, -0.28F, 0.12F));

		// Armored Neck + Translucent Neck Crest
		PartDefinition neck = chest.addOrReplaceChild("neck",
			CubeListBuilder.create().texOffs(150, 45).addBox(-8.0F, -8.0F, -17.0F, 16, 16, 18),
			PartPose.offset(0.0F, -2.0F, -11.0F));
		neck.addOrReplaceChild("neck_fin",
			CubeListBuilder.create().texOffs(191, 82).addBox(-1.0F, -12.0F, -14.0F, 2, 12, 14),
			PartPose.offset(0.0F, -7.5F, -2.0F));

		// Crowned Leviathan Skull, Brow Plates, Translucent Cheek Fans, Swept Temple Horns & Jaw
		PartDefinition head = neck.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 45).addBox(-8.0F, -8.0F, -20.0F, 16, 14, 22)
				.texOffs(198, 112).addBox(-6.0F, -6.5F, -29.0F, 12, 10, 10)
				.texOffs(84, 138).addBox(-8.5F, -9.2F, -18.0F, 4, 3, 14)
				.texOffs(84, 138).mirror().addBox(4.5F, -9.2F, -18.0F, 4, 3, 14),
			PartPose.offset(0.0F, -2.0F, -16.0F));

		head.addOrReplaceChild("crown_spire",
			CubeListBuilder.create().texOffs(121, 138).addBox(-2.0F, -11.0F, -2.0F, 4, 11, 4),
			PartPose.offsetAndRotation(0.0F, -7.5F, -10.0F, -0.45F, 0.0F, 0.0F));
		head.addOrReplaceChild("horn_r",
			CubeListBuilder.create().texOffs(0, 138).addBox(-1.5F, -16.0F, -2.0F, 3, 16, 4),
			PartPose.offsetAndRotation(-6.0F, -7.5F, -5.0F, -0.68F, 0.0F, -0.32F));
		head.addOrReplaceChild("horn_l",
			CubeListBuilder.create().texOffs(0, 138).mirror().addBox(-1.5F, -16.0F, -2.0F, 3, 16, 4),
			PartPose.offsetAndRotation(6.0F, -7.5F, -5.0F, -0.68F, 0.0F, 0.32F));
		head.addOrReplaceChild("cheek_fan_r",
			CubeListBuilder.create().texOffs(128, 112).addBox(-0.5F, -5.0F, -2.0F, 1, 10, 12),
			PartPose.offsetAndRotation(-8.5F, 0.0F, -6.0F, 0.0F, -0.42F, 0.0F));
		head.addOrReplaceChild("cheek_fan_l",
			CubeListBuilder.create().texOffs(128, 112).mirror().addBox(-0.5F, -5.0F, -2.0F, 1, 10, 12),
			PartPose.offsetAndRotation(8.5F, 0.0F, -6.0F, 0.0F, 0.42F, 0.0F));

		// Hinged Mandible + 6 Fangs (4 front/mid, 2 rear tusks)
		PartDefinition jaw = head.addOrReplaceChild("jaw",
			CubeListBuilder.create()
				.texOffs(0, 112).addBox(-5.5F, 0.0F, -20.0F, 11, 5, 20)
				.texOffs(210, 138).addBox(-4.5F, -3.5F, -19.0F, 2, 4, 2)
				.texOffs(210, 138).addBox(2.5F, -3.5F, -19.0F, 2, 4, 2)
				.texOffs(210, 138).addBox(-5.0F, -3.0F, -12.5F, 2, 4, 2)
				.texOffs(210, 138).addBox(3.0F, -3.0F, -12.5F, 2, 4, 2)
				.texOffs(210, 138).addBox(-5.2F, -3.2F, -6.5F, 2, 4, 2)
				.texOffs(210, 138).addBox(3.2F, -3.2F, -6.5F, 2, 4, 2),
			PartPose.offset(0.0F, 4.5F, -7.0F));

		// 5-Segment Undulating Serpent Tail + Monumental Translucent Caudal Fan
		PartDefinition tail1 = chest.addOrReplaceChild("tail_1",
			CubeListBuilder.create().texOffs(77, 45).addBox(-9.0F, -8.0F, 0.0F, 18, 16, 18),
			PartPose.offset(0.0F, 0.5F, 11.5F));
		PartDefinition tail2 = tail1.addOrReplaceChild("tail_2",
			CubeListBuilder.create().texOffs(0, 82).addBox(-7.0F, -6.5F, 0.0F, 14, 13, 16),
			PartPose.offset(0.0F, 0.5F, 17.0F));
		PartDefinition tail3 = tail2.addOrReplaceChild("tail_3",
			CubeListBuilder.create().texOffs(138, 82).addBox(-5.0F, -5.0F, 0.0F, 10, 10, 16),
			PartPose.offset(0.0F, 0.5F, 15.0F));
		PartDefinition tail4 = tail3.addOrReplaceChild("tail_4",
			CubeListBuilder.create().texOffs(155, 112).addBox(-3.5F, -3.5F, 0.0F, 7, 7, 14),
			PartPose.offset(0.0F, 0.0F, 15.0F));
		tail4.addOrReplaceChild("tail_fan",
			CubeListBuilder.create().texOffs(93, 0).addBox(-1.0F, -12.0F, -1.0F, 2, 24, 20),
			PartPose.offset(0.0F, 0.0F, 13.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.ThalassorState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float wave = Mth.sin(t * 0.14F);
		chest.y = 6.0F + Mth.sin(t * 0.11F) * 2.0F;
		chest.yRot = wave * 0.08F;

		neck.yRot = -wave * 0.16F + state.yRot * Mth.DEG_TO_RAD * 0.45F;
		head.yRot = -wave * 0.12F + state.yRot * Mth.DEG_TO_RAD * 0.40F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.50F + Mth.sin(t * 0.10F) * 0.06F;

		jaw.xRot = Math.max(0.06F, state.jawOpen * 0.65F) + Mth.sin(t * 0.18F) * 0.04F;

		float wingFlap = Mth.sin(t * 0.14F) * 0.22F;
		rightFin.zRot = -0.18F + wingFlap;
		leftFin.zRot = 0.18F - wingFlap;
		rightWing.zRot = -0.12F + wingFlap * 0.65F;
		leftWing.zRot = 0.12F - wingFlap * 0.65F;

		tail1.yRot = wave * 0.24F;
		tail2.yRot = Mth.sin(t * 0.14F - 0.7F) * 0.30F;
		tail3.yRot = Mth.sin(t * 0.14F - 1.4F) * 0.36F;
		tail4.yRot = Mth.sin(t * 0.14F - 2.1F) * 0.40F;
		tailFan.yRot = Mth.sin(t * 0.14F - 2.8F) * 0.45F;
	}
}
