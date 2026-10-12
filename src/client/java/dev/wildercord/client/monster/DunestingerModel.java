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
 * The Dunestinger: a broad, low carapace with a blunt head plate and two eyes up on stalks; eight jointed legs, four a
 * side, fanned out and bent down at the knee; two heavy pincers on bent arms, each with a lower jaw that opens; and a
 * tail of four segments curled up and over its back to a bulbed sting pointing forward over its head.
 *
 * <p>Buried ({@code veil}) or gone under mid-fight ({@code alt}) it sinks below the sand, its tail laid flat behind it with
 * only the tip of the sting bent up out of the sand, its eyes pushed up on their stalks to look out. Walking, its legs
 * step in staggered pairs. Snapping ({@code guard}) its pincers thrust out and the jaws bang shut. Its sting is drawn back
 * over its back, the front of it lifting ({@code windup}), and then driven down and forward ({@code acting}).</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/dunestinger_art.py}):</p>
 * <pre>
 *   carapace (0,0) 8x4x9    head (36,0) 6x3x3      eye (56,0) 1x2x1
 *   tail1 (0,14) 4x3x4      tail2 (16,14) 3x3x4    tail3 (30,14) 3x3x4    tail4 (44,14) 2x2x4
 *   sting (0,22) 2x2x3      barb (10,22) 1x1x2
 *   arm (0,28) 2x2x4        forearm (12,28) 2x2x3  claw (22,28) 3x2x4     jaw (36,28) 2x1x3
 *   leg (0,36) 5x1x1        shin (12,36) 1x6x1
 * </pre>
 */
public class DunestingerModel extends EntityModel<MonsterRenderState> {
	/** How far it sinks, buried: all of it below the sand but its eye stalks and the tip of its sting. */
	private static final float SINK = 7.5F;
	/** The tail's curl at rest, segment by segment, and laid flat under the sand. */
	private static final float[] CURL = {0.9F, 0.75F, 0.7F, 0.65F, 0.5F};
	private static final float[] FLAT = {0.0F, 0.1F, 0.2F, 0.3F, 0.9F};
	/** The legs' fan, front to back, on the right side (the left mirrors it). */
	private static final float[] FAN = {-0.6F, -0.2F, 0.2F, 0.6F};
	private static final float[] LEG_Z = {-3.5F, -1.5F, 0.5F, 2.5F};

	private final ModelPart body;
	private final ModelPart rightEye;
	private final ModelPart leftEye;
	private final ModelPart[] tail = new ModelPart[5];
	private final ModelPart rightArm;
	private final ModelPart rightForearm;
	private final ModelPart rightJaw;
	private final ModelPart leftArm;
	private final ModelPart leftForearm;
	private final ModelPart leftJaw;
	private final ModelPart[] rightLegs = new ModelPart[4];
	private final ModelPart[] leftLegs = new ModelPart[4];

	public DunestingerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.rightEye = body.getChild("right_eye");
		this.leftEye = body.getChild("left_eye");
		ModelPart part = body;
		for (int i = 0; i < tail.length; i++) {
			part = part.getChild(i < 4 ? "tail" + (i + 1) : "sting");
			tail[i] = part;
		}
		this.rightArm = body.getChild("right_arm");
		this.rightForearm = rightArm.getChild("forearm");
		this.rightJaw = rightForearm.getChild("claw").getChild("jaw");
		this.leftArm = body.getChild("left_arm");
		this.leftForearm = leftArm.getChild("forearm");
		this.leftJaw = leftForearm.getChild("claw").getChild("jaw");
		for (int i = 0; i < 4; i++) {
			rightLegs[i] = body.getChild("right_leg" + i);
			leftLegs[i] = body.getChild("left_leg" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.0F, -5.0F, 8.0F, 4.0F, 9.0F)
				.texOffs(36, 0).addBox(-3.0F, -1.0F, -8.0F, 6.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 19.5F, 0.0F));
		body.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offset(-1.5F, -1.0F, -7.0F));
		body.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offset(1.5F, -1.0F, -7.0F));

		PartDefinition tail1 = body.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 4.0F),
			PartPose.offset(0.0F, -1.0F, 4.0F));
		PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(16, 14).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, 4.0F));
		PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(30, 14).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, 4.0F));
		PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(44, 14).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, 4.0F));
		tail4.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F)
				.texOffs(10, 22).addBox(-0.5F, -0.5F, 3.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(0.0F, 0.0F, 3.5F));

		pincer(body, "right_arm", -1);
		pincer(body, "left_arm", 1);

		for (int i = 0; i < 4; i++) {
			body.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(0, 36).addBox(-5.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F)
					.texOffs(12, 36).addBox(-5.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offset(-4.0F, 0.5F, LEG_Z[i]));
			body.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(0, 36).mirror().addBox(0.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F)
					.texOffs(12, 36).mirror().addBox(4.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offset(4.0F, 0.5F, LEG_Z[i]));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** An arm, a forearm, a claw and the claw's lower jaw, on one side ({@code side} -1 right, 1 left). */
	private static void pincer(PartDefinition body, String name, int side) {
		boolean left = side > 0;
		PartDefinition arm = body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 28).mirror(left).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
			PartPose.offset(3.0F * side, 0.5F, -6.0F));
		PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(12, 28).mirror(left).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, -4.0F));
		PartDefinition claw = forearm.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(22, 28).mirror(left).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, -3.0F));
		claw.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(36, 28).mirror(left).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 1.0F, 3.0F),
			PartPose.offset(0.0F, 0.5F, -0.5F));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float under = Math.max(state.veil, state.alt);
		float up = 1 - under;
		float walk = state.walkAnimationPos;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.5F) * up;
		float wind = state.windup;
		float strike = state.acting;
		float snap = state.guard;

		// Sunk into the sand; gone under mid-fight, it churns.
		body.y = 19.5F + SINK * under;
		body.zRot = Mth.sin(t * 1.9F) * 0.08F * state.alt;
		body.xRot = -0.18F * wind + 0.22F * strike;
		body.yRot = Mth.sin(t * 0.05F) * 0.05F * up;
		// Its eyes pushed up on their stalks to look out of the sand.
		rightEye.y = leftEye.y = -1.0F - 2.0F * under;
		rightEye.zRot = -0.15F + Mth.sin(t * 0.13F) * 0.1F;
		leftEye.zRot = 0.15F + Mth.sin(t * 0.11F + 1) * 0.1F;

		// The tail: curled over its back, swaying, laid flat when buried; drawn back further, then driven down.
		float sway = Mth.sin(t * 0.09F) * 0.06F;
		for (int i = 0; i < tail.length; i++) {
			tail[i].xRot = Mth.lerp(under, CURL[i], FLAT[i]) + sway * up;
			tail[i].yRot = Mth.sin(t * 0.07F + i * 0.6F) * 0.05F * up;
		}
		// Buried, the tip of the sting twitches now and then.
		tail[4].xRot += under * Math.max(0, Mth.sin(t * 0.23F) - 0.85F) * 2.0F;
		tail[0].xRot += 0.35F * wind - 0.55F * strike;
		tail[1].xRot += 0.25F * wind - 0.3F * strike;
		tail[2].xRot += 0.1F * wind - 0.1F * strike;
		tail[4].xRot += -0.2F * wind + 0.7F * strike;

		// The pincers: held out ahead, the jaws idly working; thrust out and banged shut in a snap; raised for the sting.
		float idle = Mth.sin(t * 0.15F) * 0.08F;
		float arm = 0.55F - 0.35F * snap;
		float fore = -1.0F + 0.55F * snap;
		rightArm.yRot = arm + idle * up;
		leftArm.yRot = -arm - idle * up;
		rightArm.xRot = leftArm.xRot = -0.15F - 0.25F * snap - 0.35F * wind + 0.25F * under;
		rightForearm.yRot = fore;
		leftForearm.yRot = -fore;
		float open = (0.3F + Mth.sin(t * 0.21F) * 0.15F) * up * (1 - snap) + 0.6F * wind;
		rightJaw.xRot = open;
		leftJaw.xRot = Math.max(0, open + Mth.sin(t * 0.21F + 1.3F) * 0.1F * up);

		// The legs, in staggered pairs; folded in under the sand.
		for (int i = 0; i < 4; i++) {
			float phase = walk * 1.3F + i * Mth.HALF_PI;
			float swing = Mth.cos(phase) * 0.4F * stride;
			float lift = Math.max(0, Mth.sin(phase)) * 0.35F * stride;
			float liftL = Math.max(0, -Mth.sin(phase)) * 0.35F * stride;
			rightLegs[i].yRot = FAN[i] * (1 - 0.4F * under) + swing;
			leftLegs[i].yRot = -FAN[i] * (1 - 0.4F * under) + swing;
			rightLegs[i].zRot = 0.25F + lift + 0.5F * under;
			leftLegs[i].zRot = -0.25F - liftL - 0.5F * under;
		}
	}
}
