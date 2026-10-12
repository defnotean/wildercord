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
 * The Dripfang Lurker: a long low cave crawler with a ridged stony shell that tapers back through its abdomen into a
 * dripstone spike, a blunt head with two hooked fangs, and six thin legs.
 *
 * <p>Hanging from the roof ({@code guard}) it turns head up, tail down, its legs folded tight back along the shell, and
 * passes for one more stalactite; now and then a leg twitches ({@code windup}). Dropping ({@code acting}) it falls spike
 * first with its legs flung out and its fangs open. Climbing back up ({@code alt}) it goes head first, legs reaching.
 * Flipped on its back ({@code stunned}) it lies belly up with its legs waving.</p>
 *
 * <p>Laid out on a 64x32 skin (see {@code tools/dripfang_art.py}):</p>
 * <pre>
 *   body (0,0) 5x3x8       head (28,0) 4x3x3     fang (44,0) 1x2x1     ridge (50,0) 1x1x2
 *   abdomen (0,12) 4x3x5   spike (20,12) 2x2x4   tip (34,12) 1x1x2     leg (0,22) 7x1x1
 * </pre>
 */
public class DripfangLurkerModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 20.5F;
	/** Where its body hangs, head to the roof and spike down. */
	private static final float HANG_Y = 18.0F;
	private static final float[] LEG_Z = {-2.5F, 0.0F, 2.5F};
	/** Each pair's fan on the ground: the front pair forward, the back pair back. */
	private static final float[] LEG_FAN = {0.5F, 0.0F, -0.5F};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightFang;
	private final ModelPart leftFang;
	private final ModelPart abdomen;
	private final ModelPart spike;
	private final ModelPart[] rightLegs = new ModelPart[3];
	private final ModelPart[] leftLegs = new ModelPart[3];

	public DripfangLurkerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightFang = head.getChild("right_fang");
		this.leftFang = head.getChild("left_fang");
		this.abdomen = body.getChild("abdomen");
		this.spike = abdomen.getChild("spike");
		for (int i = 0; i < 3; i++) {
			rightLegs[i] = body.getChild("right_leg_" + i);
			leftLegs[i] = body.getChild("left_leg_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-2.5F, -1.5F, -4.0F, 5.0F, 3.0F, 8.0F)
				.texOffs(50, 0).addBox(-0.5F, -2.5F, -3.0F, 1.0F, 1.0F, 2.0F)
				.texOffs(50, 0).addBox(-0.5F, -2.5F, 1.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, -4.0F));
		head.addOrReplaceChild("right_fang", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offset(-1.2F, 1.0F, -2.5F));
		head.addOrReplaceChild("left_fang", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offset(1.2F, 1.0F, -2.5F));

		PartDefinition abdomen = body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 12).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 5.0F),
			PartPose.offset(0.0F, -0.2F, 4.0F));
		PartDefinition spike = abdomen.addOrReplaceChild("spike", CubeListBuilder.create().texOffs(20, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, 5.0F));
		spike.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(34, 12).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F),
			PartPose.offset(0.0F, 0.0F, 4.0F));

		for (int i = 0; i < 3; i++) {
			body.addOrReplaceChild("right_leg_" + i, CubeListBuilder.create().texOffs(0, 22).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
				PartPose.offset(-2.5F, 0.5F, LEG_Z[i]));
			body.addOrReplaceChild("left_leg_" + i, CubeListBuilder.create().texOffs(0, 22).mirror().addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
				PartPose.offset(2.5F, 0.5F, LEG_Z[i]));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float hang = state.guard;
		float fall = state.acting;
		float climb = state.alt;
		float flip = state.stunned;
		float twitch = state.windup;
		// Upright: head to the roof, tail down, whether hanging, falling or climbing.
		float upright = Math.min(1, hang + fall + climb);
		float ground = 1 - upright;
		float walk = state.walkAnimationPos * 1.6F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.5F) * ground * (1 - flip);

		body.y = Mth.lerp(upright, BODY_Y, HANG_Y) - 0.8F * flip + Mth.sin(walk * 2) * 0.3F * stride;
		body.xRot = -Mth.HALF_PI * upright;
		body.zRot = Mth.PI * flip * ground + Mth.sin(t * 0.6F) * 0.08F * flip;
		body.yRot = Mth.clamp(state.yRot, -30, 30) * Mth.DEG_TO_RAD * ground * (1 - flip) * 0.3F;

		// The head looks about on the ground, keeps still as stone on the roof.
		float settled = ground * (1 - flip);
		head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD * 0.7F * settled;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * settled - 0.3F * fall;
		// Fangs: closed tight on the roof, open to fall and to bite, chattering when flipped.
		float open = 0.25F * settled + 0.6F * fall + Math.max(0, Mth.sin(t * 0.9F)) * 0.3F * flip + 0.15F * twitch;
		rightFang.zRot = open;
		leftFang.zRot = -open;
		rightFang.xRot = leftFang.xRot = -0.3F * open;

		// The tail hangs straight as a stalactite; on the ground it curls up a little behind it and sways.
		abdomen.xRot = -0.12F * ground + Mth.sin(t * 0.08F) * 0.04F * ground;
		spike.xRot = -0.18F * ground;
		abdomen.yRot = Mth.sin(walk) * 0.12F * stride;

		for (int i = 0; i < 3; i++) {
			float phase = walk + i * Mth.PI * 0.66F;
			float lift = Mth.cos(phase) * 0.35F * stride;
			float sweep = Mth.sin(phase) * 0.35F * stride;
			// Ground: fanned out, bent down to the ground, stepping.
			float yGround = LEG_FAN[i] + sweep;
			float zGround = 0.55F + Math.max(0, lift);
			// Hanging: folded tight back along the shell.
			float yHang = -1.25F + 0.1F * i;
			float zHang = 0.1F;
			// Falling: flung out wide. Climbing: reaching up and gripping, in turn.
			float reach = Mth.sin(t * 0.5F + i * 2.1F);
			float yOut = LEG_FAN[i] * 1.4F + 0.35F * reach * climb;
			float zOut = -0.25F * fall + 0.35F * climb;
			// Flipped: waving in the air (the body's roll turns them skyward).
			float wave = Mth.sin(t * 0.9F + i * 1.7F) * 0.5F;

			float y = yGround * ground + yHang * hang + yOut * (fall + climb);
			float z = zGround * ground * (1 - flip) + (0.3F + wave) * flip * ground + zHang * hang + zOut;
			// Its one tell on the roof: the front leg on the right side twitches out and back.
			float tell = i == 0 ? twitch * (0.5F + 0.3F * Mth.sin(t * 2.4F)) : 0;
			leftLegs[i].yRot = y;
			leftLegs[i].zRot = z;
			// The right side steps in turn against the left.
			rightLegs[i].yRot = -(y - 2 * sweep * ground) + tell;
			rightLegs[i].zRot = -(z - Math.max(0, lift) * ground + Math.max(0, -lift) * ground) - tell * 0.4F;
		}
	}
}
