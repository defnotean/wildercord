package dev.wildercord.client.familiar;

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
 * A wisp: a bright core with two tiny eyes on its front, inside a soft shell of light, and a tail
 * of three shrinking beads that trails behind and sways as it flies. The whole of it bobs.
 *
 * <p>Laid out on a 64x32 skin (see {@code wisp_texture} in {@code tools/familiar_art.py}):</p>
 * <pre>
 *   core (0,0) 5x5x5    shell (0,10) 7x7x7    tail (30,0) 3x3x3, (30,6) 2x2x2, (30,10) 1x1x2
 * </pre>
 */
public class WispModel extends EntityModel<WispRenderState> {
	private final ModelPart orb;
	private final ModelPart shell;
	private final ModelPart tail;
	private final ModelPart tailMid;
	private final ModelPart tailTip;

	public WispModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.orb = root.getChild("orb");
		this.shell = orb.getChild("shell");
		this.tail = orb.getChild("tail");
		this.tailMid = tail.getChild("mid");
		this.tailTip = tailMid.getChild("tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The orb's middle sits 4 pixels above the entity's feet (the box is 0.45 blocks tall).
		PartDefinition orb = root.addOrReplaceChild("orb", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, 20.0F, 0.0F));
		orb.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 10).addBox(-3.5F, -3.5F, -3.5F, 7.0F, 7.0F, 7.0F), PartPose.ZERO);
		PartDefinition tail = orb.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 0).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, 2.5F));
		PartDefinition mid = tail.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(30, 6).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, 0.0F, 3.0F));
		mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(30, 10).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(0.0F, 0.0F, 2.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WispRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		orb.y += Mth.sin(t * 0.12F) * 0.8F;
		// The shell breathes, and swells for a moment with each flare.
		float breath = 1.0F + 0.05F * Mth.sin(t * 0.2F) + 0.25F * state.flare;
		shell.xScale = breath;
		shell.yScale = breath;
		shell.zScale = breath;
		// The tail trails the way it came, lifting as it climbs, swaying side to side, each bead a little later.
		tail.xRot = Mth.clamp(state.climb * 2.5F, -0.6F, 0.6F) + Mth.sin(t * 0.15F) * 0.15F;
		tail.yRot = Mth.sin(t * 0.17F) * 0.35F;
		tailMid.yRot = Mth.sin(t * 0.17F - 0.8F) * 0.4F;
		tailMid.xRot = Mth.sin(t * 0.15F - 0.8F) * 0.2F;
		tailTip.yRot = Mth.sin(t * 0.17F - 1.6F) * 0.5F;
		// Now and then it blinks: the core squints for two ticks.
		orb.yScale = (int) t % 97 < 2 ? 0.8F : 1.0F;
	}
}
