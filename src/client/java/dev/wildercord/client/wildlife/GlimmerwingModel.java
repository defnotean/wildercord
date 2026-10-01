package dev.wildercord.client.wildlife;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A glimmerwing: a furry thorax and a tapering abdomen hanging below it, a small head with two feathery antennae, and
 * four broad wings (each a flat sheet, painted above and below). The wings beat fast, the hind pair a moment after the
 * fore, with now and then a glide on raised wings; the body bobs with every beat. Its wings are see-through at the
 * edges, so it's drawn translucent. UVs match {@code glimmerwing_skin} in tools/wildlife_art.py.
 */
public final class GlimmerwingModel extends EntityModel<WildlifeRenderState> {
	/** Flat sheets (wings, moss) are two faces a hair apart, so neither flickers through the other. */
	private static final CubeDeformation SHEET = new CubeDeformation(0.001F);

	private final ModelPart body, abdomen, head, leftFore, rightFore, leftHind, rightHind, leftAntenna, rightAntenna;

	public GlimmerwingModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		body = root.getChild("body");
		abdomen = body.getChild("abdomen");
		head = body.getChild("head");
		leftAntenna = head.getChild("left_antenna");
		rightAntenna = head.getChild("right_antenna");
		leftFore = body.getChild("left_forewing");
		rightFore = body.getChild("right_forewing");
		leftHind = body.getChild("left_hindwing");
		rightHind = body.getChild("right_hindwing");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.offset(0, 20, 0));
		body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(8, 0).addBox(-1, -1, 0, 2, 2, 4), PartPose.offsetAndRotation(0, 0, 1, -0.3F, 0, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 0).addBox(-1, -1, -2, 2, 2, 2),
			PartPose.offset(0, -0.2F, -1));
		head.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(28, 0).addBox(0, -3, -3, 0, 3, 3, SHEET),
			PartPose.offsetAndRotation(0.6F, -0.9F, -1.6F, 0.25F, -0.35F, 0));
		head.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0, -3, -3, 0, 3, 3, SHEET),
			PartPose.offsetAndRotation(-0.6F, -0.9F, -1.6F, 0.25F, 0.35F, 0));
		body.addOrReplaceChild("left_forewing", CubeListBuilder.create().texOffs(0, 6).addBox(0, 0, -3, 7, 0, 5, SHEET), PartPose.offset(1, -0.9F, -0.6F));
		body.addOrReplaceChild("right_forewing", CubeListBuilder.create().texOffs(0, 6).mirror().addBox(-7, 0, -3, 7, 0, 5, SHEET), PartPose.offset(-1, -0.9F, -0.6F));
		body.addOrReplaceChild("left_hindwing", CubeListBuilder.create().texOffs(0, 11).addBox(0, 0, -1, 5, 0, 4, SHEET), PartPose.offset(1, -0.7F, 0.8F));
		body.addOrReplaceChild("right_hindwing", CubeListBuilder.create().texOffs(0, 11).mirror().addBox(-5, 0, -1, 5, 0, 4, SHEET), PartPose.offset(-1, -0.7F, 0.8F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float phase = state.flapPhase;
		float strength = state.flapStrength;
		// A beat: wings flung up and swept down, a glide holds them raised in a shallow V and lets them quiver.
		float beat = Mth.sin(phase);
		float fore = 0.25F + beat * 0.95F * strength + (1 - strength) * (0.55F + Mth.sin(phase * 0.5F) * 0.06F);
		float hind = 0.2F + Mth.sin(phase - 0.6F) * 0.8F * strength + (1 - strength) * 0.45F;
		leftFore.zRot = -fore;
		rightFore.zRot = fore;
		leftHind.zRot = -hind;
		rightHind.zRot = hind;
		// The fore pair sweeps a little forward on the downbeat.
		leftFore.yRot = -beat * 0.18F * strength;
		rightFore.yRot = beat * 0.18F * strength;
		body.y = 20 - beat * 0.7F * strength;
		body.xRot = -0.12F + Mth.sin(phase + 0.8F) * 0.06F * strength;
		abdomen.xRot = -0.3F + Mth.sin(phase + 1.6F) * 0.12F;
		head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD;
		leftAntenna.yRot = -0.35F + Mth.sin(state.ageInTicks * 0.2F + state.seed) * 0.1F;
		rightAntenna.yRot = 0.35F - Mth.sin(state.ageInTicks * 0.2F + state.seed + 1) * 0.1F;
	}
}
