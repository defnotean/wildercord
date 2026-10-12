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
 * The Foxfire Kitsune: a slim long body on four long thin legs, a narrow head with a pointed snout and tall ears, and three
 * great fluffy tails fanned out behind, each tipped with a tuft of blue fire.
 *
 * <p>It runs light and bounding, its tails streaming. Its tails sway on their own, never quite together. Fanning them up
 * to throw its foxfire ({@code windup}) they rise and spread wide, its head drops and its ears prick forward; the throw or
 * the bite ({@code acting}) snaps its head forward. Splitting ({@code alt}) it spins on the spot.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/kitsune_art.py}):</p>
 * <pre>
 *   body (0,0) 5x5x10
 *   head (0,16) 6x5x5      snout (22,16) 2x2x3     ear (32,16) 2x3x1
 *   tail (0,28) 4x4x8      tip (24,28) 4x4x3
 *   leg (0,44) 2x8x2
 * </pre>
 */
public class FoxfireKitsuneModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 13.5F;
	private static final float[] SPLAY = {-0.5F, 0, 0.5F};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart[] tails = new ModelPart[3];
	private final ModelPart rightFore;
	private final ModelPart leftFore;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public FoxfireKitsuneModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		for (int i = 0; i < 3; i++) {
			tails[i] = body.getChild("tail_" + i);
		}
		this.rightFore = root.getChild("right_foreleg");
		this.leftFore = root.getChild("left_foreleg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -3.0F, -4.5F, 6.0F, 5.0F, 5.0F)
				.texOffs(22, 16).addBox(-1.0F, -0.5F, -7.5F, 2.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, -2.0F, -5.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(32, 16).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(-2.0F, -3.0F, -2.0F, 0.0F, 0.0F, -0.15F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(2.0F, -3.0F, -2.0F, 0.0F, 0.0F, 0.15F));
		for (int i = 0; i < 3; i++) {
			body.addOrReplaceChild("tail_" + i, CubeListBuilder.create().texOffs(0, 28).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 8.0F)
					.texOffs(24, 28).addBox(-2.0F, -2.0F, 8.0F, 4.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -1.5F, 4.5F, 0.5F, SPLAY[i], 0.0F));
		}
		root.addOrReplaceChild("right_foreleg", CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(-1.5F, 16.0F, -3.5F));
		root.addOrReplaceChild("left_foreleg", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(1.5F, 16.0F, -3.5F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(-1.5F, 16.0F, 3.5F));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(1.5F, 16.0F, 3.5F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float step = state.walkAnimationPos * 0.9F;
		float fan = state.windup;
		float snap = state.acting;

		// A light bounding run: fore and hind pairs together, the body rising and falling.
		rightFore.xRot = leftFore.xRot = Mth.cos(step) * 1.1F * speed;
		rightFore.xRot += Mth.sin(step * 0.5F) * 0.1F * speed;
		rightHind.xRot = leftHind.xRot = Mth.cos(step + Mth.PI) * 1.1F * speed;
		leftHind.xRot += Mth.sin(step * 0.5F) * 0.1F * speed;
		body.y = BODY_Y - Math.abs(Mth.sin(step)) * 1.0F * speed;
		body.xRot = Mth.sin(step) * 0.08F * speed;

		head.xRot = state.xRot * Mth.DEG_TO_RAD + fan * 0.25F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.z = -5.0F - snap * 1.5F;
		rightEar.xRot = leftEar.xRot = -fan * 0.4F;
		float flick = Mth.clamp(Mth.sin(t * 0.06F) * 6 - 5, 0, 1);
		rightEar.zRot = -0.15F - flick * 0.3F * Mth.sin(t * 1.3F);

		// Three tails, each swaying on its own; streaming back as it runs, fanned high and wide to throw.
		for (int i = 0; i < 3; i++) {
			ModelPart tail = tails[i];
			float phase = i * 2.1F;
			tail.xRot = 0.5F + Mth.sin(t * 0.08F + phase) * 0.08F - 0.3F * speed + fan * 0.75F;
			tail.yRot = SPLAY[i] * (1 + fan * 0.7F) + Mth.sin(t * 0.06F + phase) * 0.15F * (1 - speed)
				+ Mth.sin(step + phase) * 0.12F * speed;
			tail.zRot = SPLAY[i] * fan * 0.4F + Mth.sin(t * 0.25F + phase) * 0.05F * fan;
		}

		// The split: a spin on the spot.
		root().yRot = state.alt * t * 0.9F;
	}
}
