package dev.wildercord.client.wildlife;

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
 * A skyray: a flat diamond of a body with a rounded head lobe and two curling horn fins, broad wings in three hinged
 * sheets (cut by their texture into a manta's swept outline, painted above and below) over a thick root at the body,
 * and a long thin whip of a tail. In flight a slow wave runs out along each wing, root to tip, now barely a ripple as
 * it glides and now a full stroke as it climbs; it leans into its turns and tips its nose with its climb and dive, and
 * its tail undulates behind. UVs match {@code skyray_skin} in tools/wildlife_art.py.
 */
public final class SkyrayModel extends EntityModel<WildlifeRenderState> {
	/** Flat sheets (wings, moss) are two faces a hair apart, so neither flickers through the other. */
	private static final CubeDeformation SHEET = new CubeDeformation(0.001F);

	private final ModelPart body, leftHorn, rightHorn, tail, tailTip;
	private final ModelPart[] left, right;

	/** How far each segment's tip curls up at rest, root to tip. */
	private static final float[] CURL = {0.04F, 0.07F, 0.14F};

	public SkyrayModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		leftHorn = body.getChild("left_horn");
		rightHorn = body.getChild("right_horn");
		tail = body.getChild("tail");
		tailTip = tail.getChild("tail_tip");
		ModelPart lw = body.getChild("left_wing");
		ModelPart rw = body.getChild("right_wing");
		left = new ModelPart[] {lw, lw.getChild("left_wing_mid"), lw.getChild("left_wing_mid").getChild("left_wing_tip")};
		right = new ModelPart[] {rw, rw.getChild("right_wing_mid"), rw.getChild("right_wing_mid").getChild("right_wing_tip")};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4, -2, -7, 8, 3, 14)
			.texOffs(44, 0).addBox(-3, -1.5F, -10, 6, 2, 3), PartPose.offset(0, 20, 0));
		body.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(62, 0).addBox(-0.5F, -0.5F, -4, 1, 1, 4),
			PartPose.offsetAndRotation(2.6F, 0, -9.5F, 0.35F, -0.15F, 0));
		body.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(62, 0).mirror().addBox(-0.5F, -0.5F, -4, 1, 1, 4),
			PartPose.offsetAndRotation(-2.6F, 0, -9.5F, 0.35F, 0.15F, 0));
		wing(body, "left", 1);
		wing(body, "right", -1);
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 30).addBox(-0.5F, -0.5F, 0, 1, 1, 10),
			PartPose.offset(0, -0.5F, 7));
		tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(22, 30).addBox(-0.5F, -0.5F, 0, 1, 1, 8), PartPose.offset(0, 0, 10));
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** One wing: a thick root and an inner sheet, a middle sheet hinged on it, and a tip hinged on that. */
	private static void wing(PartDefinition body, String side, int out) {
		boolean mirror = out < 0;
		CubeListBuilder inner = CubeListBuilder.create().texOffs(0, 17);
		CubeListBuilder root = CubeListBuilder.create().texOffs(30, 17);
		CubeListBuilder mid = CubeListBuilder.create().texOffs(54, 17);
		CubeListBuilder tip = CubeListBuilder.create().texOffs(78, 17);
		if (mirror) {
			inner.mirror();
			root.mirror();
			mid.mirror();
			tip.mirror();
		}
		inner.addBox(mirror ? -9 : 0, 0, -5, 9, 0, 11, SHEET);
		root.addBox(mirror ? -3 : 0, -1, -4.5F, 3, 2, 9);
		PartDefinition wing = body.addOrReplaceChild(side + "_wing", inner, PartPose.offset(out * 4, -0.5F, -1));
		wing.addOrReplaceChild(side + "_wing_root", root, PartPose.ZERO);
		PartDefinition middle = wing.addOrReplaceChild(side + "_wing_mid", mid.addBox(mirror ? -8 : 0, 0, -4, 8, 0, 8, SHEET),
			PartPose.offset(out * 9, 0, 0.5F));
		middle.addOrReplaceChild(side + "_wing_tip", tip.addBox(mirror ? -7 : 0, 0, -2, 7, 0, 4, SHEET), PartPose.offset(out * 8, 0, 1));
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		body.zRot = state.bank;
		body.xRot = state.pitch;

		// A wave out along each wing: barely a ripple in a glide, a full stroke when it climbs.
		float amplitude = 0.07F + 0.42F * state.flapStrength;
		for (int i = 0; i < 3; i++) {
			float wave = Mth.sin(state.flapPhase - i * 0.75F) * amplitude * (1 - i * 0.12F);
			left[i].zRot = -CURL[i] - wave;
			right[i].zRot = CURL[i] + wave;
		}
		// The horns curl and uncurl lazily; the tail undulates like a ribbon.
		leftHorn.xRot = 0.35F + Mth.sin(t * 0.05F) * 0.08F;
		rightHorn.xRot = 0.35F + Mth.sin(t * 0.05F + 0.4F) * 0.08F;
		tail.yRot = Mth.sin(t * 0.07F) * 0.16F - state.bank * 0.3F;
		tail.xRot = -state.pitch * 0.5F + Mth.sin(t * 0.05F) * 0.05F;
		tailTip.yRot = Mth.sin(t * 0.07F - 0.9F) * 0.28F;
	}
}
