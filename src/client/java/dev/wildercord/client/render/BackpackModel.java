package dev.wildercord.client.render;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

/**
 * A worn backpack, in the body's space (y down, the back at +z), at the Runewoven Backpack's full size; the
 * smaller ones are the same shape drawn smaller (see {@link GearLayer}). Its origin is the middle of the top of
 * the face that rests against the back. Parts a backpack doesn't have (the Runewoven's side pouches, the
 * Reinforced Backpack's bedroll) are left transparent in its texture.
 *
 * <p>Every backpack's textures share one 64x32 layout, drawn by {@code tools/backpack_art.py}: keep the two in
 * step. The straps are a model of their own, since they lie on the chest, wherever the front is.</p>
 */
public final class BackpackModel extends Model<Unit> {
	private BackpackModel(ModelPart root) {
		super(root, RenderTypes::entityCutout);
	}

	public static BackpackModel of(ModelPart root) {
		return new BackpackModel(root);
	}

	/** How far the pack reaches out from the back (the flap and pocket's outer faces), at full size, in pixels. */
	public static final float DEPTH = 5.0F;
	/** How far the lid rises above the origin, at full size. */
	public static final float LID = 1.0F;

	public static LayerDefinition createPack() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		// The bag, and its lid, a little bigger, over the top.
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 10.0F, 4.0F), PartPose.ZERO);
		root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 14).addBox(-4.5F, -1.0F, 0.0F, 9.0F, 2.0F, 5.0F), PartPose.ZERO);
		// The flap hanging from the lid down the outside, and a pocket below it.
		root.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(28, 14).addBox(-3.5F, 1.0F, 4.0F, 7.0F, 4.0F, 1.0F), PartPose.ZERO);
		root.addOrReplaceChild("pocket", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, 6.0F, 4.0F, 6.0F, 3.0F, 1.0F), PartPose.ZERO);
		// A pouch on each side, and a rolled bedroll strapped underneath.
		root.addOrReplaceChild("right_pouch", CubeListBuilder.create().texOffs(40, 0).addBox(-5.0F, 3.0F, 0.5F, 1.0F, 5.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("left_pouch", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(4.0F, 3.0F, 0.5F, 1.0F, 5.0F, 3.0F), PartPose.ZERO);
		root.addOrReplaceChild("bedroll", CubeListBuilder.create().texOffs(0, 21).addBox(-4.5F, 10.0F, 0.5F, 9.0F, 2.0F, 3.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	/** One shoulder strap as it runs down the chest: its back face at z 0, its top at the top of the chest. */
	public static LayerDefinition createStrap() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("strap", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 1.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}
}
