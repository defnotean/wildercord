package dev.wildercord.client.render;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

/**
 * The worn Cord, in the right arm's space: a band of cord wound twice round the wrist ({@link #band})
 * or one glowing bead ({@link #bead}, drawn once per rune at a spot along the band).
 */
public final class CordModel extends Model<Unit> {
	private CordModel(ModelPart root) {
		super(root, RenderTypes::entityCutout);
	}

	public static CordModel band(ModelPart root) {
		return new CordModel(root);
	}

	public static CordModel bead(ModelPart root) {
		return new CordModel(root);
	}

	/** Two turns of cord round the wrist, just proud of the arm (and its sleeve), like a bracelet. */
	public static LayerDefinition createBand() {
		return band(-3.0F, 4.0F);
	}

	/** The same, round a slim (three-pixel) arm. */
	public static LayerDefinition createSlimBand() {
		return band(-2.0F, 3.0F);
	}

	private static LayerDefinition band(float x, float width) {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("band", CubeListBuilder.create()
				.texOffs(0, 0).addBox(x, 7.9F, -2.0F, width, 0.7F, 4.0F, new CubeDeformation(0.3F))
				.texOffs(0, 6).addBox(x, 8.9F, -2.0F, width, 0.5F, 4.0F, new CubeDeformation(0.28F)),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 16);
	}

	/** One bead, centred on its spot. */
	public static LayerDefinition createBead() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("bead", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 8, 8);
	}
}
