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
 * The Pitchback Beetle: a broad thorax and a long abdomen under two ridged wing cases; a blunt head with hooked mandibles
 * and long feelers; six jointed legs; and swelling out behind the wing cases, a round sac of resin.
 *
 * <p>It scuttles on its six legs in two tripods, feelers waving. Rearing up to lob ({@code windup}) its front lifts, its
 * wing cases part, and the sac swells and swings up over its back; the lob ({@code acting}) snaps the sac forward and the
 * mandibles shut. With its sac burst ({@code alt}) the sac hangs shrunken and slack.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/pitchback_art.py}):</p>
 * <pre>
 *   thorax (0,0) 8x5x5      head (26,0) 6x4x4       mandible (46,0) 1x1x4    feeler (56,0) 1x7x1
 *   abdomen (0,10) 10x5x10  sac (40,10) 6x5x6
 *   wing case (0,25) 5x3x11 leg (32,25) 6x1x1       shin (46,25) 1x8x1
 * </pre>
 */
public class PitchbackBeetleModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 17.0F;
	/** The legs, front to back: where they join and how they fan out (right side; the left mirrors). */
	private static final float[] LEG_Z = {-3.0F, 1.5F, 6.0F};
	private static final float[] FAN = {-0.55F, 0.0F, 0.55F};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightMandible;
	private final ModelPart leftMandible;
	private final ModelPart rightFeeler;
	private final ModelPart leftFeeler;
	private final ModelPart rightCase;
	private final ModelPart leftCase;
	private final ModelPart sac;
	private final ModelPart[] rightLegs = new ModelPart[3];
	private final ModelPart[] leftLegs = new ModelPart[3];

	public PitchbackBeetleModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightMandible = head.getChild("right_mandible");
		this.leftMandible = head.getChild("left_mandible");
		this.rightFeeler = head.getChild("right_feeler");
		this.leftFeeler = head.getChild("left_feeler");
		this.rightCase = body.getChild("right_case");
		this.leftCase = body.getChild("left_case");
		this.sac = body.getChild("sac");
		for (int i = 0; i < 3; i++) {
			rightLegs[i] = body.getChild("right_leg" + i);
			leftLegs[i] = body.getChild("left_leg" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.5F, -5.0F, 8.0F, 5.0F, 5.0F)
				.texOffs(0, 10).addBox(-5.0F, -2.5F, 0.0F, 10.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, -5.0F));
		head.addOrReplaceChild("right_mandible", CubeListBuilder.create().texOffs(46, 0).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(-2.0F, 1.0F, -3.5F, 0.0F, -0.35F, 0.0F));
		head.addOrReplaceChild("left_mandible", CubeListBuilder.create().texOffs(46, 0).mirror().addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(2.0F, 1.0F, -3.5F, 0.0F, 0.35F, 0.0F));
		head.addOrReplaceChild("right_feeler", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(-1.5F, -1.5F, -3.5F, 0.9F, 0.0F, -0.35F));
		head.addOrReplaceChild("left_feeler", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(1.5F, -1.5F, -3.5F, 0.9F, 0.0F, 0.35F));

		body.addOrReplaceChild("right_case", CubeListBuilder.create().texOffs(0, 25).addBox(-5.0F, -1.0F, 0.0F, 5.0F, 3.0F, 11.0F),
			PartPose.offset(0.0F, -4.0F, -0.5F));
		body.addOrReplaceChild("left_case", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(0.0F, -1.0F, 0.0F, 5.0F, 3.0F, 11.0F),
			PartPose.offset(0.0F, -4.0F, -0.5F));
		body.addOrReplaceChild("sac", CubeListBuilder.create().texOffs(40, 10).addBox(-3.0F, -5.0F, 0.0F, 6.0F, 5.0F, 6.0F),
			PartPose.offset(0.0F, -1.0F, 8.5F));

		for (int i = 0; i < 3; i++) {
			body.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(32, 25).addBox(-6.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F)
					.texOffs(46, 25).addBox(-6.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F),
				PartPose.offset(-4.0F, 1.5F, LEG_Z[i]));
			body.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(32, 25).mirror().addBox(0.0F, -0.5F, -0.5F, 6.0F, 1.0F, 1.0F)
					.texOffs(46, 25).mirror().addBox(5.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F),
				PartPose.offset(4.0F, 1.5F, LEG_Z[i]));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos * 1.4F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.8F);
		float rear = state.windup;
		float snap = state.acting;
		float empty = state.alt;

		body.y = BODY_Y + Mth.sin(walk * 2) * 0.25F * stride;
		body.xRot = -0.35F * rear + 0.1F * snap;
		body.zRot = Mth.sin(walk) * 0.04F * stride;

		head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F + 0.35F * rear - 0.15F * snap;
		head.z = -5.0F - 0.8F * snap;

		// Mandibles: twitching idly, wide while it rears, shut on the snap.
		float open = 0.1F + Math.max(0, Mth.sin(t * 0.17F)) * 0.15F + 0.4F * rear - 0.3F * snap;
		rightMandible.yRot = -0.35F + open;
		leftMandible.yRot = 0.35F - open;

		// Feelers waving, each to its own beat; swept back while it rears.
		rightFeeler.xRot = 0.9F + Mth.sin(t * 0.13F) * 0.2F - 0.6F * rear;
		leftFeeler.xRot = 0.9F + Mth.sin(t * 0.11F + 1.7F) * 0.2F - 0.6F * rear;
		rightFeeler.zRot = -0.35F + Mth.sin(t * 0.09F) * 0.12F;
		leftFeeler.zRot = 0.35F - Mth.sin(t * 0.1F + 0.6F) * 0.12F;

		// The wing cases part as it rears, to let the sac swing up between them.
		float part = 0.45F * rear + 0.15F * snap;
		rightCase.zRot = part;
		leftCase.zRot = -part;
		rightCase.xRot = leftCase.xRot = 0.12F * rear;

		// The sac: breathing slowly, swelling and swinging up over its back to throw; slack and shrunken once burst.
		float breathe = 1 + Mth.sin(t * 0.08F) * 0.04F;
		float swell = breathe * (1 + 0.35F * rear - 0.15F * snap) * (1 - 0.5F * empty);
		sac.xScale = sac.zScale = swell;
		sac.yScale = swell * (1 - 0.2F * empty);
		sac.xRot = 1.1F * rear - 0.4F * snap + 0.25F * empty;
		sac.y = -1.0F + 1.2F * empty;

		// Six legs in two tripods; braced wide while it rears.
		for (int i = 0; i < 3; i++) {
			float phase = walk + (i % 2 == 0 ? 0 : Mth.PI);
			float swing = Mth.cos(phase) * 0.45F * stride;
			float lift = Math.max(0, Mth.sin(phase)) * 0.4F * stride;
			float liftL = Math.max(0, -Mth.sin(phase)) * 0.4F * stride;
			float brace = 0.2F * rear;
			rightLegs[i].yRot = FAN[i] + swing;
			leftLegs[i].yRot = -FAN[i] + swing;
			rightLegs[i].zRot = 0.3F + lift - brace;
			leftLegs[i].zRot = -0.3F - liftL + brace;
		}
	}
}
