package dev.wildercord.client.monster;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Custom 128x128 Paper Lantern Spirit ({@code Chochin-Obake}) rig matching the turnaround &amp; pose
 * sheet: stepped dark-cedar roof canopy with brass ring, ribbed crimson-and-gold pleated lantern
 * drum, hinged lower jaw revealing a spinning inner flame crystal core, 2-segment fluttering white
 * {@code ofuda} talisman wings, and a 2-segment braided silk tassel ending in a bronze bell.
 */
public class PaperLanternSpiritModel extends EntityModel<MonsterRenderState> {
	private final ModelPart drum;
	private final ModelPart innerCore;
	private final ModelPart lowerJaw;
	private final ModelPart tassel;
	private final ModelPart tasselBell;
	private final ModelPart leftSleeve;
	private final ModelPart leftSleeveTip;
	private final ModelPart rightSleeve;
	private final ModelPart rightSleeveTip;

	public PaperLanternSpiritModel(ModelPart root) {
		super(root);
		this.drum = root.getChild("drum");
		this.innerCore = this.drum.getChild("inner_core");
		this.lowerJaw = this.drum.getChild("lower_jaw");
		this.tassel = this.lowerJaw.getChild("tassel");
		this.tasselBell = this.tassel.getChild("tassel_bell");
		this.leftSleeve = this.drum.getChild("left_sleeve");
		this.leftSleeveTip = this.leftSleeve.getChild("left_sleeve_tip");
		this.rightSleeve = this.drum.getChild("right_sleeve");
		this.rightSleeveTip = this.rightSleeve.getChild("right_sleeve_tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Stepped dark-cedar canopy, brass top loop & upper pleated lantern drum.
		PartDefinition drum = root.addOrReplaceChild("drum",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0F, -7.0F, -6.0F, 12, 2, 12)
				.texOffs(50, 0).addBox(-4.0F, -9.0F, -4.0F, 8, 2, 8)
				.texOffs(84, 0).addBox(-2.0F, -12.0F, -1.0F, 4, 3, 2)
				.texOffs(0, 18).addBox(-5.0F, -5.0F, -5.0F, 10, 7, 10),
			PartPose.offset(0.0F, 11.5F, 0.0F));

		// Spinning blazing crystal ember core suspended inside the lower mouth cavity.
		drum.addOrReplaceChild("inner_core",
			CubeListBuilder.create().texOffs(42, 18).addBox(-2.5F, -2.5F, -2.5F, 5, 5, 5, new CubeDeformation(-0.15F)),
			PartPose.offsetAndRotation(0.0F, 2.8F, -0.5F, 0.785F, 0.785F, 0.0F));

		// Articulated lower lantern rim/jaw hinged at rear edge (+Z) to drop open like a Chochin-Obake mouth.
		PartDefinition lowerJaw = drum.addOrReplaceChild("lower_jaw",
			CubeListBuilder.create()
				.texOffs(0, 38).addBox(-5.0F, 0.0F, -9.5F, 10, 4, 10)
				.texOffs(42, 38).addBox(-4.5F, 4.0F, -9.0F, 9, 2, 9),
			PartPose.offset(0.0F, 2.0F, 4.5F));

		// 2-segment braided silk tassel & bronze chime bell hanging below the lower rim.
		PartDefinition tassel = lowerJaw.addOrReplaceChild("tassel",
			CubeListBuilder.create().texOffs(80, 38).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
			PartPose.offset(0.0F, 6.0F, -4.5F));
		tassel.addOrReplaceChild("tassel_bell",
			CubeListBuilder.create()
				.texOffs(90, 38).addBox(-1.0F, 0.0F, -1.0F, 2, 3, 2)
				.texOffs(100, 38).addBox(-1.5F, 2.5F, -1.5F, 3, 2, 3),
			PartPose.offset(0.0F, 3.8F, 0.0F));

		// 2-segment fluttering white ofuda calligraphy talisman wings.
		PartDefinition leftSleeve = drum.addOrReplaceChild("left_sleeve",
			CubeListBuilder.create().texOffs(0, 56).mirror().addBox(0.0F, -2.0F, -2.0F, 1, 5, 4),
			PartPose.offset(5.0F, -1.0F, 0.0F));
		leftSleeve.addOrReplaceChild("left_sleeve_tip",
			CubeListBuilder.create().texOffs(12, 56).mirror().addBox(0.0F, 0.0F, -2.0F, 1, 4, 4),
			PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition rightSleeve = drum.addOrReplaceChild("right_sleeve",
			CubeListBuilder.create().texOffs(0, 56).addBox(-1.0F, -2.0F, -2.0F, 1, 5, 4),
			PartPose.offset(-5.0F, -1.0F, 0.0F));
		rightSleeve.addOrReplaceChild("right_sleeve_tip",
			CubeListBuilder.create().texOffs(12, 56).addBox(-1.0F, 0.0F, -2.0F, 1, 4, 4),
			PartPose.offset(0.0F, 3.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;

		// Figure-8 hover bob & gentle tilt matching Strip (d).
		drum.y = 11.5F + Mth.sin(t * 0.14F) * 1.4F;
		drum.x = Mth.sin(t * 0.07F) * 0.9F;
		drum.yRot = state.yRot * Mth.DEG_TO_RAD;
		drum.zRot = Mth.cos(t * 0.07F) * 0.08F;

		// Accordion breathing & 360° burst swell.
		float swell = 1.0F + state.windup * 0.12F + state.acting * 0.22F;
		drum.xScale = swell;
		drum.zScale = swell;
		drum.yScale = 1.0F - state.acting * 0.08F + state.windup * 0.08F;

		// Hinged jaw unhinges during windup/attack (with slight ominous creep while floating).
		float jawOpen = 0.06F + Mth.sin(t * 0.12F) * 0.04F + state.windup * 0.72F + state.acting * 0.88F;
		lowerJaw.xRot = jawOpen;

		// Blazing inner ember crystal core spins & pulses visibly when jaw opens.
		innerCore.yRot = t * (0.08F + state.windup * 0.24F + state.acting * 0.35F);
		innerCore.xRot = 0.785F + Mth.sin(t * 0.15F) * 0.15F;
		float coreScale = 0.75F + state.windup * 0.45F + state.acting * 0.65F;
		innerCore.xScale = innerCore.yScale = innerCore.zScale = coreScale;

		// Phase-delayed ofuda wing flare & silk tassel whip.
		float baseFlap = 0.32F + state.windup * 0.58F + state.acting * 0.75F + Mth.sin(t * 0.18F) * 0.22F;
		float tipLag = 0.18F + Mth.sin(t * 0.18F - 0.85F) * 0.28F + state.acting * 0.35F;
		leftSleeve.zRot = -baseFlap;
		leftSleeveTip.zRot = -tipLag;
		rightSleeve.zRot = baseFlap;
		rightSleeveTip.zRot = tipLag;

		tassel.xRot = -jawOpen * 0.45F + Mth.sin(t * 0.16F) * 0.20F + state.acting * 0.35F;
		tassel.zRot = Mth.cos(t * 0.14F) * 0.18F;
		tasselBell.xRot = Mth.sin(t * 0.16F - 0.9F) * 0.26F;
		tasselBell.zRot = Mth.cos(t * 0.14F - 0.9F) * 0.24F;
	}
}
