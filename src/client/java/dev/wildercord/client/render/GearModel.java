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
 * The leather that holds casting gear on the wearer, in the body's space (y down, the front at -z): a belt
 * round the waist for the tome to hang from ({@link #belt}, in a snug and a roomy size, so it lies over the
 * skin or over a chestplate), a tie that straps a staff to the back ({@link #tie}) and a small ring for the
 * tome's strap ({@link #loop}). Their texture is one sheet of leather with a gold buckle.
 */
public final class GearModel extends Model<Unit> {
	private GearModel(ModelPart root) {
		super(root, RenderTypes::entityCutout);
	}

	public static GearModel of(ModelPart root) {
		return new GearModel(root);
	}

	/** Belt height and where it sits on the torso (body space, pixels). */
	public static final float BELT_TOP = 9.2F;
	public static final float BELT_HEIGHT = 1.3F;

	/** A belt lying just over the skin and its jacket layer. */
	public static LayerDefinition createBelt() {
		return belt(0.42F);
	}

	/** The same, standing clear of a chestplate. */
	public static LayerDefinition createArmoredBelt() {
		return belt(1.1F);
	}

	private static LayerDefinition belt(float deformation) {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("belt", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, BELT_TOP, -2.0F, 8.0F, BELT_HEIGHT, 4.0F, new CubeDeformation(deformation)),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 16);
	}

	/** A band that goes round a staff's shaft, one pixel across and about three long, one pixel deep (stretched to reach the back). */
	public static LayerDefinition createTie() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("tie", CubeListBuilder.create()
				.texOffs(0, 10).addBox(-1.8F, -0.6F, -0.5F, 3.6F, 1.2F, 1.0F),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 16);
	}

	/** The tome's strap: a short leather band, its top on the belt. */
	public static LayerDefinition createLoop() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("loop", CubeListBuilder.create()
				.texOffs(0, 10).addBox(-0.6F, 0.0F, -0.5F, 1.2F, 3.0F, 1.0F),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 16);
	}

	/** One speck of light, centred on its spot (for a focus's glimmer). */
	public static LayerDefinition createMote() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("mote", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 8, 8);
	}
}
