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
 * Floating Japanese chochin paper-lantern spirit with wooden top/bottom caps, pleated ember drum,
 * dangling cord tassel, and fluttering side sleeves (`64x64` layout painted by {@code tools/shrine_cat_art.py}).
 */
public class PaperLanternSpiritModel extends EntityModel<MonsterRenderState> {
	private final ModelPart drum;
	private final ModelPart tassel;
	private final ModelPart leftSleeve;
	private final ModelPart rightSleeve;

	public PaperLanternSpiritModel(ModelPart root) {
		super(root);
		this.drum = root.getChild("drum");
		this.tassel = this.drum.getChild("tassel");
		this.leftSleeve = this.drum.getChild("left_sleeve");
		this.rightSleeve = this.drum.getChild("right_sleeve");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition drum = root.addOrReplaceChild("drum",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 2.0F, 8.0F)
				.texOffs(0, 14).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 12.0F, 10.0F)
				.texOffs(0, 40).addBox(-4.0F, 6.0F, -4.0F, 8.0F, 2.0F, 8.0F),
			PartPose.offset(0.0F, 12.0F, 0.0F));
		drum.addOrReplaceChild("tassel",
			CubeListBuilder.create().texOffs(44, 14).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(0.0F, 8.0F, 0.0F));
		drum.addOrReplaceChild("left_sleeve",
			CubeListBuilder.create().texOffs(34, 40).mirror().addBox(0.0F, -2.0F, -2.0F, 2.0F, 8.0F, 4.0F),
			PartPose.offset(5.0F, -1.0F, 0.0F));
		drum.addOrReplaceChild("right_sleeve",
			CubeListBuilder.create().texOffs(34, 40).addBox(-2.0F, -2.0F, -2.0F, 2.0F, 8.0F, 4.0F),
			PartPose.offset(-5.0F, -1.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		drum.y = 12.0F + Mth.sin(t * 0.14F) * 1.4F;
		drum.yRot = state.yRot * Mth.DEG_TO_RAD;
		float swell = 1.0F + state.windup * 0.14F + state.acting * 0.12F;
		drum.xScale = swell;
		drum.zScale = swell;
		tassel.xRot = Mth.sin(t * 0.16F) * 0.22F;
		tassel.zRot = Mth.cos(t * 0.14F) * 0.18F;
		float flap = 0.28F + state.windup * 0.55F + Mth.sin(t * 0.18F) * 0.2F;
		leftSleeve.zRot = -flap;
		rightSleeve.zRot = flap;
	}
}
