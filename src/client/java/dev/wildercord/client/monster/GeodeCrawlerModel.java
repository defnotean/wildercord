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
 * The Geode Crawler: a broad, low beetle, its dark chitin shell grown through with amethyst points of every size, a blunt
 * head with pincer mandibles and twitching feelers, and six arched legs. It scuttles; struck, it curls into a ball of
 * crystal-studded shell; rattling (the tell) the ball shivers in place; then it rolls; dazed, it lies tipped on its back,
 * legs kicking.
 *
 * <p>The crawler and the ball are two parts, shown in turn as it curls: a beetle can't fold into a sphere in boxes.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code geode_crawler} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   body (0,0) 12x5x14    head (52,0) 8x4x5     mandible (78,0) 2x1x4   crystal (90,0) 3x7x3 / (102,0) 2x5x2 / (110,0) 2x3x2
 *   feeler (118,0) 1x1x4  leg (0,20) 7x2x2      shin (18,20) 2x6x2      shell (40,20) 13x2x12   ball (0,36) 12x12x12
 * </pre>
 */
public class GeodeCrawlerModel extends EntityModel<MonsterRenderState> {
	/** The ball's middle: it sits on the ground, as wide as the beetle. */
	private static final float BALL_Y = 18.0F;
	/** The crystals on its back: x, z, size (0 big, 1 middling, 2 small), tilt forward, tilt sideways. */
	private static final float[][] CRYSTALS = {
		{-2.0F, 0.5F, 0, 0.15F, -0.3F}, {2.2F, -1.5F, 0, -0.1F, 0.35F}, {0.0F, 3.5F, 0, 0.35F, 0.05F},
		{-4.2F, -3.0F, 1, -0.2F, -0.5F}, {4.0F, 2.8F, 1, 0.25F, 0.55F}, {-3.5F, 4.5F, 1, 0.45F, -0.35F}, {1.0F, -4.5F, 1, -0.45F, 0.1F},
		{-0.8F, -2.0F, 2, -0.1F, -0.15F}, {4.6F, -2.6F, 2, -0.2F, 0.7F}, {-4.8F, 1.2F, 2, 0.05F, -0.75F}, {2.4F, 5.2F, 2, 0.6F, 0.3F}};
	private static final float[][] BALL_CRYSTALS = {
		{-2.0F, -1.0F, 0, 0.1F, -0.3F}, {2.0F, 1.5F, 0, -0.2F, 0.35F}, {0.0F, -3.5F, 1, -0.5F, 0.0F}, {-3.5F, 2.5F, 1, 0.4F, -0.5F},
		{3.5F, -2.0F, 2, -0.3F, 0.6F}, {0.5F, 3.5F, 2, 0.55F, 0.1F}};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightFeeler;
	private final ModelPart leftFeeler;
	private final ModelPart rightMandible;
	private final ModelPart leftMandible;
	private final ModelPart[] legs = new ModelPart[6];
	private final ModelPart[] shins = new ModelPart[6];
	private final ModelPart ball;

	public GeodeCrawlerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightFeeler = head.getChild("right_feeler");
		this.leftFeeler = head.getChild("left_feeler");
		this.rightMandible = head.getChild("right_mandible");
		this.leftMandible = head.getChild("left_mandible");
		for (int i = 0; i < 6; i++) {
			legs[i] = body.getChild("leg_" + i);
			shins[i] = legs[i].getChild("shin");
		}
		this.ball = root.getChild("ball");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -2.5F, -7.0F, 12.0F, 5.0F, 14.0F)
				.texOffs(40, 20).addBox(-6.5F, -4.5F, -5.5F, 13.0F, 2.0F, 12.0F),
			PartPose.offset(0.0F, 18.5F, 0.0F));
		for (int i = 0; i < CRYSTALS.length; i++) {
			crystal(body, "crystal_" + i, CRYSTALS[i], -4.3F);
		}
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(52, 0).addBox(-4.0F, -2.0F, -5.0F, 8.0F, 4.0F, 5.0F),
			PartPose.offset(0.0F, 0.5F, -7.0F));
		head.addOrReplaceChild("right_mandible", CubeListBuilder.create().texOffs(78, 0).addBox(-1.0F, -0.5F, -4.0F, 2.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(-2.3F, 1.2F, -4.5F, 0.0F, 0.35F, 0.0F));
		head.addOrReplaceChild("left_mandible", CubeListBuilder.create().texOffs(78, 0).mirror().addBox(-1.0F, -0.5F, -4.0F, 2.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(2.3F, 1.2F, -4.5F, 0.0F, -0.35F, 0.0F));
		head.addOrReplaceChild("right_feeler", CubeListBuilder.create().texOffs(118, 0).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(-2.0F, -1.8F, -4.5F, -0.6F, 0.4F, 0.0F));
		head.addOrReplaceChild("left_feeler", CubeListBuilder.create().texOffs(118, 0).mirror().addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
			PartPose.offsetAndRotation(2.0F, -1.8F, -4.5F, -0.6F, -0.4F, 0.0F));
		// Six arched legs: knees up, feet out on the ground.
		float[] z = {-4.5F, 0.0F, 4.5F};
		for (int i = 0; i < 6; i++) {
			boolean left = i >= 3;
			float s = left ? 1 : -1;
			CubeListBuilder upper = CubeListBuilder.create().texOffs(0, 20);
			PartDefinition leg = body.addOrReplaceChild("leg_" + i, (left ? upper.mirror() : upper).addBox(left ? 0.0F : -7.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F),
				PartPose.offsetAndRotation(s * 5.5F, 1.0F, z[i % 3], 0.0F, -s * (i % 3 - 1) * 0.35F, -s * 0.3F));
			CubeListBuilder lower = CubeListBuilder.create().texOffs(18, 20);
			leg.addOrReplaceChild("shin", (left ? lower.mirror() : lower).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(s * 7.0F, 0.0F, 0.0F, 0.0F, 0.0F, -s * 0.1F));
		}

		// Curled up: a ball of shell studded with crystal.
		PartDefinition ball = root.addOrReplaceChild("ball", CubeListBuilder.create().texOffs(0, 36).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F),
			PartPose.offset(0.0F, BALL_Y, 0.0F));
		for (int i = 0; i < BALL_CRYSTALS.length; i++) {
			crystal(ball, "crystal_" + i, BALL_CRYSTALS[i], -5.6F);
		}
		// And round its sides, leaning out.
		float[][] sides = {{-5.6F, 0.5F, -1.0F, 0, -1.25F}, {5.6F, -0.5F, 1.5F, 1, 1.3F}, {0.0F, 0.0F, 5.6F, 1, 0.0F}, {-1.5F, 1.0F, -5.6F, 2, 0.0F},
			{3.5F, 2.0F, -3.5F, 2, 0.9F}};
		for (int i = 0; i < sides.length; i++) {
			float[] c = sides[i];
			CubeListBuilder cube = CubeListBuilder.create();
			switch ((int) c[3]) {
				case 0 -> cube.texOffs(90, 0).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F);
				case 1 -> cube.texOffs(102, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F);
				default -> cube.texOffs(110, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F);
			}
			float xRot = c[2] > 5 ? 1.3F : c[2] < -5 ? -1.3F : 0.0F;
			ball.addOrReplaceChild("side_" + i, cube, PartPose.offsetAndRotation(c[0], c[1], c[2], xRot, 0.0F, c[4]));
		}
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void crystal(PartDefinition parent, String name, float[] c, float y) {
		int size = (int) c[2];
		CubeListBuilder cube = CubeListBuilder.create();
		switch (size) {
			case 0 -> cube.texOffs(90, 0).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F);
			case 1 -> cube.texOffs(102, 0).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F);
			default -> cube.texOffs(110, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F);
		}
		parent.addOrReplaceChild(name, cube, PartPose.offsetAndRotation(c[0], y + 0.6F, c[1], c[3], 0.0F, c[4]));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos;
		float curl = state.guard;
		float rattle = state.windup;
		float roll = state.acting;
		float dazed = state.stunned;

		// Curled: the beetle shows as a ball (the switch is quick, half way through the curl).
		boolean balled = curl > 0.5F;
		body.visible = !balled;
		ball.visible = balled;
		float squash = balled ? 0.8F + 0.2F * Math.min(1, (curl - 0.5F) * 2) : 1 - curl * 0.35F;
		if (balled) {
			ball.xScale = squash;
			ball.yScale = squash;
			ball.zScale = squash;
			ball.y = BALL_Y + (1 - squash) * 6;
			// Rattling, it shivers; rolling, it spins forward.
			ball.x = Mth.sin(t * 3.1F) * 0.35F * rattle;
			ball.zRot = Mth.sin(t * 2.7F) * 0.08F * rattle;
			ball.xRot = roll > 0 ? t * 0.75F : 0.0F;
			return;
		}
		body.yScale = squash;

		// Scuttling: the legs step in two tripods, each foot lifting as it swings.
		for (int i = 0; i < 6; i++) {
			boolean left = i >= 3;
			float s = left ? 1 : -1;
			float phase = (i % 2 == 0) == left ? 0 : Mth.PI;
			float step = Mth.sin(pos * 0.9F + phase);
			legs[i].yRot = -s * (i % 3 - 1) * 0.35F + step * 0.4F * walk;
			legs[i].zRot = -s * 0.3F - s * Math.max(0, Mth.cos(pos * 0.9F + phase)) * 0.25F * walk;
			// Tipped on its back, the legs kick.
			legs[i].zRot += s * dazed * (0.6F + Mth.sin(t * 1.3F + i * 1.7F) * 0.5F);
		}
		// A little sway as it walks; tipped over, it rocks on its shell.
		body.zRot = Mth.sin(pos * 0.9F) * 0.04F * walk + dazed * (Mth.PI * 0.82F + Mth.sin(t * 0.4F) * 0.1F);
		body.y = 18.5F - dazed * 3.5F;

		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
		head.xRot = Mth.clamp(state.xRot, -20, 20) * Mth.DEG_TO_RAD;
		float snap = Mth.sin(t * 0.25F) * 0.12F;
		rightMandible.yRot = 0.35F + snap;
		leftMandible.yRot = -0.35F - snap;
		rightFeeler.xRot = -0.6F + Mth.sin(t * 0.31F) * 0.15F;
		leftFeeler.xRot = -0.6F + Mth.sin(t * 0.27F + 1.1F) * 0.15F;
		rightFeeler.yRot = 0.4F + Mth.sin(t * 0.19F) * 0.12F;
		leftFeeler.yRot = -0.4F - Mth.sin(t * 0.23F + 0.6F) * 0.12F;
	}
}
