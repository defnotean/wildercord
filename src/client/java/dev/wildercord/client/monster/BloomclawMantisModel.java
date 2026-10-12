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
 * The Bloomclaw Mantis: a broad flat abdomen, folded wings lying over it, a long thorax reared up from it, a small
 * triangular head with great eyes at its corners, two raptorial forelegs folded as if in prayer, and four thin walking legs
 * each with a petal-shaped lobe.
 *
 * <p>Perched ({@code guard}) it rocks slowly side to side as a blossom does in the breeze. The tell ({@code windup}) is its
 * threat display: thorax reared high, forelegs raised and opened, wings flared up and out. Striking ({@code acting}) it
 * pitches forward, forelegs flung straight out. Holding on ({@code alt}) its forelegs close, working as it nibbles.
 * Reeling ({@code stunned}) it lurches, forelegs dangling, wings askew.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/mantis_art.py}):</p>
 * <pre>
 *   abdomen (0,0) 5x3x8   thorax (30,0) 2x2x7    head (0,14) 4x3x2      eye (14,14) 1x1x1
 *   crest (20,14) 2x1x1   wing (30,12) 3x0x7     femur (0,22) 1x5x1     tibia (6,22) 1x4x1
 *   leg (0,30) 1x7x1      lobe (6,30) 3x2x1
 * </pre>
 */
public class BloomclawMantisModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 18.0F;

	private final ModelPart body;
	private final ModelPart thorax;
	private final ModelPart head;
	private final ModelPart[] wings = new ModelPart[2];
	private final ModelPart[] femurs = new ModelPart[2];
	private final ModelPart[] tibias = new ModelPart[2];
	private final ModelPart[] legs = new ModelPart[4];

	public BloomclawMantisModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.thorax = body.getChild("thorax");
		this.head = thorax.getChild("head");
		for (int i = 0; i < 2; i++) {
			wings[i] = body.getChild("wing_" + i);
			femurs[i] = thorax.getChild("femur_" + i);
			tibias[i] = femurs[i].getChild("tibia");
		}
		for (int i = 0; i < 4; i++) {
			legs[i] = body.getChild("leg_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 8.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));

		PartDefinition thorax = body.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, -1.0F, -7.0F, 2.0F, 2.0F, 7.0F),
			PartPose.offset(0.0F, -0.5F, 0.5F));
		thorax.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 14).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 3.0F, 2.0F)
				.texOffs(14, 14).addBox(-2.6F, -2.6F, -1.6F, 1.0F, 1.0F, 1.0F)
				.texOffs(14, 14).mirror().addBox(1.6F, -2.6F, -1.6F, 1.0F, 1.0F, 1.0F).mirror(false)
				.texOffs(20, 14).addBox(-1.0F, 1.0F, -1.8F, 2.0F, 1.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, -7.0F));

		for (int i = 0; i < 2; i++) {
			int side = i == 0 ? -1 : 1;
			body.addOrReplaceChild("wing_" + i, CubeListBuilder.create().texOffs(30, 12).mirror(side > 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 0.0F, 7.0F),
				PartPose.offset(side * 1.0F, -1.6F, 0.5F));
			PartDefinition femur = thorax.addOrReplaceChild("femur_" + i, CubeListBuilder.create().texOffs(0, 22).mirror(side > 0)
					.addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offset(side * 1.3F, 0.5F, -5.5F));
			femur.addOrReplaceChild("tibia", CubeListBuilder.create().texOffs(6, 22).mirror(side > 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offset(0.0F, 4.5F, 0.0F));
		}

		for (int i = 0; i < 4; i++) {
			int side = i % 2 == 0 ? -1 : 1;
			float z = i < 2 ? -0.5F : 1.5F;
			body.addOrReplaceChild("leg_" + i, CubeListBuilder.create()
					.texOffs(0, 30).mirror(side > 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F)
					.texOffs(6, 30).mirror(side > 0).addBox(side > 0 ? -0.5F : -2.5F, 1.5F, -0.5F, 3.0F, 2.0F, 1.0F),
				PartPose.offset(side * 1.5F, 0.5F, z));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float perch = state.guard;
		float tell = state.windup;
		float strike = state.acting;
		float hold = state.alt;
		float reel = state.stunned;
		float walk = state.walkAnimationPos * 1.6F;
		float stride = Math.min(1, state.walkAnimationSpeed * 2.0F) * (1 - perch);

		// Rocking side to side like a blossom in the breeze, slow and wide perched, a little always; lurching when it reels.
		float rock = Mth.sin(t * 0.06F) * (0.04F + 0.1F * perch) + Mth.sin(t * 0.7F) * 0.12F * reel;
		body.y = BODY_Y - 0.6F * tell + 0.5F * reel + Math.abs(Mth.sin(walk)) * 0.2F * stride;
		body.z = -1.2F * strike + 0.6F * tell;
		body.xRot = 0.08F * tell - 0.12F * strike + 0.1F * reel;
		body.zRot = rock;
		body.yRot = 0;

		// The thorax reared up: higher for the display, pitched forward in the strike, drooping as it reels.
		thorax.xRot = -0.8F - 0.4F * tell + 0.5F * strike + 0.1F * hold + 0.45F * reel;
		thorax.zRot = -rock * 0.5F;

		// The head held level, turning to look; tilting curiously when perched.
		float level = -thorax.xRot - body.xRot;
		float settled = (1 - tell) * (1 - strike) * (1 - reel);
		head.xRot = level + state.xRot * Mth.DEG_TO_RAD * 0.6F * settled + 0.25F * hold;
		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD * 0.8F * settled;
		head.zRot = Mth.sin(t * 0.045F) * 0.2F * perch + Mth.sin(t * 0.6F) * 0.25F * reel;

		for (int i = 0; i < 2; i++) {
			int side = i == 0 ? -1 : 1;
			// The forelegs: folded as if in prayer; raised and opened in the display; flung out in the strike; closed tight,
			// working, holding on; dangling as it reels.
			float nibble = Math.max(0, Mth.sin(t * 0.63F)) * 0.25F * hold;
			femurs[i].xRot = -0.35F - 1.0F * tell - 1.3F * strike - 0.9F * hold + 0.45F * reel + Mth.sin(t * 0.06F) * 0.05F * perch;
			femurs[i].zRot = -side * (0.08F + 0.35F * tell - 0.1F * hold);
			femurs[i].yRot = side * 0.15F * tell;
			tibias[i].xRot = -2.6F + 1.5F * tell + 2.3F * strike + 0.5F * hold + nibble + 1.9F * reel;

			// The wings: lying flat over the abdomen; flared up and out in the display; lifted a little as it holds on;
			// knocked askew as it reels.
			wings[i].xRot = -0.05F - 0.7F * tell - 0.15F * hold - 0.2F * reel;
			wings[i].yRot = side * (0.08F + 0.5F * tell + 0.2F * reel);
			wings[i].zRot = side * (0.02F + 0.9F * tell + 0.1F * hold) + Mth.sin(t * 1.9F) * 0.08F * tell;
		}

		for (int i = 0; i < 4; i++) {
			int side = i % 2 == 0 ? -1 : 1;
			boolean front = i < 2;
			float phase = walk + ((i == 0 || i == 3) ? 0 : Mth.PI);
			legs[i].xRot = (front ? -0.35F : 0.45F) + Mth.sin(phase) * 0.5F * stride;
			legs[i].zRot = side * (0.65F + 0.15F * reel) - side * Math.max(0, Mth.cos(phase)) * 0.2F * stride;
			legs[i].yRot = 0;
		}
	}
}
