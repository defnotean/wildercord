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
 * The Bramblewalker: a hunched trunk of bark on two stumpy root-legs, its head a knot of bramble sunk low between mossy,
 * leafy shoulders, thorned branches reaching up off its back like a crown, and long vine-wrapped arms hanging to its knees
 * in twig claws, with vines trailing off them. It lumbers, swaying; rearing for its lash it leans back with both arms
 * raised high behind it, vines lifting; the lash flings its right arm forward as its body snaps down after it.
 *
 * <p>Laid out on a 128x64 skin (see {@code bramblewalker} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   torso (0,0) 14x15x9    head (48,0) 9x8x8      arm (84,0) 5x17x5     hand (104,0) 6x4x6
 *   leg (0,26) 6x10x6      root (24,26) 8x3x8     leaves (56,22) 8x5x7  branch (88,22) 2x12x2
 *   twig (96,22) 1x5x1     vine (100,22) 4x10x0   tuft (0,44) 6x4x6
 * </pre>
 */
public class BramblewalkerModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart[] branches;
	private final ModelPart[] vines;

	public BramblewalkerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.branches = new ModelPart[] {body.getChild("branch_0"), body.getChild("branch_1"), body.getChild("branch_2"), body.getChild("branch_3"),
			body.getChild("branch_4")};
		this.vines = new ModelPart[] {rightArm.getChild("vine"), leftArm.getChild("vine"), body.getChild("vine_0"), body.getChild("vine_1")};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// The trunk, pivoting at the hips, hunched forward.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -15.0F, -4.5F, 14.0F, 15.0F, 9.0F),
			PartPose.offsetAndRotation(0.0F, 14.0F, 1.0F, 0.32F, 0.0F, 0.0F));
		// Its head, a knot of bramble sunk low and forward between the shoulders.
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 0).addBox(-4.5F, -6.0F, -6.5F, 9.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, -14.0F, -3.0F));
		// Mossy, leafy shoulders and a tuft on its back.
		body.addOrReplaceChild("right_leaves", CubeListBuilder.create().texOffs(56, 22).addBox(-4.0F, -3.0F, -3.5F, 8.0F, 5.0F, 7.0F),
			PartPose.offsetAndRotation(-6.0F, -14.0F, 0.5F, 0.0F, 0.0F, -0.22F));
		body.addOrReplaceChild("left_leaves", CubeListBuilder.create().texOffs(56, 22).mirror().addBox(-4.0F, -3.0F, -3.5F, 8.0F, 5.0F, 7.0F),
			PartPose.offsetAndRotation(6.0F, -14.0F, 0.5F, 0.0F, 0.0F, 0.22F));
		body.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(0, 44).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F),
			PartPose.offsetAndRotation(0.0F, -9.0F, 4.0F, 0.3F, 0.0F, 0.0F));
		// Thorned branches reaching up off its back, each with a twig.
		float[][] crown = {{-5.0F, -13.0F, 2.5F, -0.35F, 0.2F, -0.55F}, {5.0F, -13.0F, 2.5F, -0.35F, -0.2F, 0.55F}, {-1.5F, -14.0F, 3.0F, -0.6F, 0.0F, -0.15F},
			{2.0F, -12.0F, 3.5F, -0.85F, 0.0F, 0.25F}, {-3.0F, -8.0F, 4.0F, -1.05F, 0.3F, -0.4F}};
		for (int i = 0; i < crown.length; i++) {
			float[] c = crown[i];
			PartDefinition branch = body.addOrReplaceChild("branch_" + i, CubeListBuilder.create().texOffs(88, 22).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				PartPose.offsetAndRotation(c[0], c[1], c[2], c[3], c[4], c[5]));
			branch.addOrReplaceChild("twig", CubeListBuilder.create().texOffs(96, 22).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -7.0F - i % 2, 0.0F, 0.0F, 0.0F, i % 2 == 0 ? 0.7F : -0.7F));
		}
		// Vines trailing off its chest.
		body.addOrReplaceChild("vine_0", CubeListBuilder.create().texOffs(100, 22).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 10.0F, 0.0F),
			PartPose.offset(-3.5F, -5.0F, -4.6F));
		body.addOrReplaceChild("vine_1", CubeListBuilder.create().texOffs(100, 22).mirror().addBox(-2.0F, 0.0F, 0.0F, 4.0F, 10.0F, 0.0F),
			PartPose.offset(3.0F, -9.0F, -4.6F));

		// Long, vine-wrapped arms ending in twig claws.
		arm(body, "right_arm", -9.0F, false);
		arm(body, "left_arm", 9.0F, true);

		// Stumpy root-legs, spreading at the foot.
		leg(root, "right_leg", -3.6F, false);
		leg(root, "left_leg", 3.6F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void arm(PartDefinition body, String name, float x, boolean mirror) {
		CubeListBuilder upper = CubeListBuilder.create().texOffs(84, 0);
		PartDefinition arm = body.addOrReplaceChild(name, (mirror ? upper.mirror() : upper).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 17.0F, 5.0F),
			PartPose.offset(x, -12.5F, -0.5F));
		CubeListBuilder palm = CubeListBuilder.create().texOffs(104, 0);
		PartDefinition hand = arm.addOrReplaceChild("hand", (mirror ? palm.mirror() : palm).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F),
			PartPose.offset(0.0F, 15.0F, 0.0F));
		for (int i = 0; i < 3; i++) {
			hand.addOrReplaceChild("claw_" + i, CubeListBuilder.create().texOffs(96, 22).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(-1.8F + i * 1.8F, 3.5F, -2.0F, -0.35F, 0.0F, (i - 1) * 0.25F));
		}
		CubeListBuilder vine = CubeListBuilder.create().texOffs(100, 22);
		arm.addOrReplaceChild("vine", (mirror ? vine.mirror() : vine).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 10.0F, 0.0F),
			PartPose.offset(0.0F, 6.0F, 2.6F));
	}

	private static void leg(PartDefinition root, String name, float x, boolean mirror) {
		CubeListBuilder trunk = CubeListBuilder.create().texOffs(0, 26);
		PartDefinition leg = root.addOrReplaceChild(name, (mirror ? trunk.mirror() : trunk).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F),
			PartPose.offset(x, 14.0F, 1.0F));
		CubeListBuilder roots = CubeListBuilder.create().texOffs(24, 26);
		leg.addOrReplaceChild("roots", (mirror ? roots.mirror() : roots).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 3.0F, 8.0F), PartPose.offset(0.0F, 7.0F, 0.0F));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos;
		float rear = state.windup;
		float lash = state.acting;
		float gait = Mth.cos(pos * 0.5F);

		// A heavy, swaying lumber; it breathes, slowly, standing.
		rightLeg.xRot = gait * 0.6F * walk;
		leftLeg.xRot = -gait * 0.6F * walk;
		float breathe = Mth.sin(t * 0.055F) * 0.03F;
		body.zRot = gait * 0.07F * walk;
		body.yRot = Mth.sin(pos * 0.25F) * 0.05F * walk;
		// Rearing back for the lash; snapping forward after it.
		body.xRot = 0.32F + breathe - 0.55F * rear + 0.38F * lash;

		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
		head.xRot = Mth.clamp(state.xRot, -25, 25) * Mth.DEG_TO_RAD - 0.32F * (1 - rear) * 0.5F - 0.35F * rear;

		// Arms: swinging slow with the gait, hanging heavy; both raised high behind it as it rears; the right flung out in the lash.
		float swing = gait * 0.45F * walk;
		float sway = Mth.sin(t * 0.06F) * 0.05F;
		rightArm.xRot = Mth.lerp(rear, -swing - 0.15F + sway, -2.7F);
		leftArm.xRot = Mth.lerp(rear, swing - 0.15F - sway, -2.5F);
		rightArm.zRot = Mth.lerp(rear, 0.12F, 0.35F);
		leftArm.zRot = Mth.lerp(rear, -0.12F, -0.35F);
		rightArm.xRot = Mth.lerp(lash, rightArm.xRot, -1.35F);
		rightArm.zRot = Mth.lerp(lash, rightArm.zRot, 0.05F);
		leftArm.xRot = Mth.lerp(lash, leftArm.xRot, 0.3F);

		// The crown of branches stirs as if in a wind; it shivers as it rears.
		for (int i = 0; i < branches.length; i++) {
			branches[i].zRot = baseZ(i) + Mth.sin(t * 0.07F + i * 1.3F) * 0.05F + rear * Mth.sin(t * 1.4F + i) * 0.06F;
		}
		// Vines swing after the arms, lifting as it rears.
		for (int i = 0; i < vines.length; i++) {
			vines[i].xRot = Mth.sin(t * 0.09F + i * 0.9F) * 0.12F + Mth.sin(pos * 0.5F + i) * 0.2F * walk - 0.6F * rear;
		}
	}

	/** Each branch's own lean (as built), for the sway to sit on. */
	private static float baseZ(int i) {
		return switch (i) {
			case 0 -> -0.55F;
			case 1 -> 0.55F;
			case 2 -> -0.15F;
			case 3 -> 0.25F;
			default -> -0.4F;
		};
	}
}
