package dev.wildercord.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Training Dummy: a straw sack body and a sack head with a painted target, on a pole in a
 * wooden base, with a crossbar through the shoulders. It rocks back when it's hit.
 */
public class DummyModel extends EntityModel<DummyRenderState> {
	private final ModelPart body;

	public DummyModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(32, 16).addBox(-3.0F, 22.0F, -3.0F, 6.0F, 2.0F, 6.0F), PartPose.ZERO);
		// Everything above the base pivots at the base, so a hit rocks the whole dummy.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(28, 0).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F)
				.texOffs(0, 14).addBox(-4.0F, -23.0F, -2.5F, 8.0F, 11.0F, 5.0F)
				.texOffs(0, 32).addBox(-8.0F, -21.5F, -1.0F, 16.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, 22.0F, 0.0F));
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -30.0F, -3.5F, 7.0F, 7.0F, 7.0F), PartPose.ZERO);
		body.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(40, 26).addBox(-10.5F, -22.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
		body.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(40, 26).mirror().addBox(7.5F, -22.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(DummyRenderState state) {
		super.setupAnim(state);
		float t = state.hurt;
		body.xRot = t > 0 ? -Mth.sin(t * 0.9F) * 0.22F * (t / 10F) : 0.0F;
		body.zRot = t > 0 ? Mth.sin(t * 1.3F) * 0.06F * (t / 10F) : 0.0F;
	}
}
