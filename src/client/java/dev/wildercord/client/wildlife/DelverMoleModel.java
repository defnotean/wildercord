package dev.wildercord.client.wildlife;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Delver Mole: a plump velvet body, a small head running out into a long snout tipped with a pink star of a nose,
 * stout forearms ending in broad spade claws turned outward, short hind legs and a stub of a tail. It waddles on its
 * forearms; digging, its claws churn in turn and its head dips into the work, its star twitching. Drawn larger by its
 * renderer. UVs match tools/mount_art.py.
 */
public final class DelverMoleModel extends EntityModel<WildlifeRenderState> {
	/** How far its forearms splay out from its sides. */
	private static final float SPLAY = 0.25F;

	private final ModelPart body, head, star, leftArm, rightArm, leftLeg, rightLeg, tail;

	public DelverMoleModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		star = head.getChild("star");
		leftArm = root.getChild("left_arm");
		rightArm = root.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
		tail = body.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7, -7, -9, 14, 10, 18),
			PartPose.offset(0, 17, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(64, 0).addBox(-4, -3, -7, 8, 6, 7)
			.texOffs(94, 0).addBox(-2, 0, -11, 4, 3, 4), PartPose.offset(0, -1, -9));
		head.addOrReplaceChild("star", CubeListBuilder.create().texOffs(110, 0).addBox(-3, -2.5F, -1, 6, 5, 1), PartPose.offset(0, 1.5F, -11));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(72, 28).addBox(-1, -1, 0, 2, 2, 6),
			PartPose.offsetAndRotation(0, -3, 9, -0.3F, 0, 0));
		arm(root, "left_arm", 7.5F, SPLAY, false);
		arm(root, "right_arm", -7.5F, -SPLAY, true);
		leg(root, "left_leg", 4.5F, false);
		leg(root, "right_leg", -4.5F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void arm(PartDefinition root, String name, float x, float splay, boolean mirror) {
		PartDefinition arm = root.addOrReplaceChild(name, cubes(0, 28, mirror).addBox(-2, 0, -2, 4, 6, 4),
			PartPose.offsetAndRotation(x, 16.2F, -5, 0, 0, -splay));
		arm.addOrReplaceChild("claw", cubes(16, 28, mirror).addBox(-3, 6, -4, 6, 2, 6), PartPose.rotation(0, 0, splay));
	}

	private static void leg(PartDefinition root, String name, float x, boolean mirror) {
		PartDefinition leg = root.addOrReplaceChild(name, cubes(40, 28, mirror).addBox(-1.5F, 0, -2, 3, 4, 4), PartPose.offset(x, 19, 6));
		leg.addOrReplaceChild("foot", cubes(54, 28, mirror).addBox(-2, 4, -3, 4, 1, 5), PartPose.ZERO);
	}

	private static CubeListBuilder cubes(int u, int v, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
		return mirror ? cube.mirror() : cube;
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed);
		float walk = state.walkAnimationPos * 0.8F;
		float dig = state.dig;

		// A waddle: forearms and hind legs in diagonal pairs, the body rolling side to side over them.
		float swing = Mth.cos(walk) * 0.7F * speed * (1 - dig);
		leftArm.xRot = swing;
		rightLeg.xRot = swing;
		rightArm.xRot = -swing;
		leftLeg.xRot = -swing;
		body.zRot = Mth.sin(walk) * 0.07F * speed;
		body.y = 17 - Mth.abs(Mth.cos(walk)) * 0.4F * speed;

		// Digging, the claws churn in turn, reaching forward and raking back.
		if (dig > 0) {
			float churn = t * 1.1F;
			leftArm.xRot = Mth.lerp(dig, leftArm.xRot, -0.9F + Mth.sin(churn) * 0.8F);
			rightArm.xRot = Mth.lerp(dig, rightArm.xRot, -0.9F - Mth.sin(churn) * 0.8F);
		}

		head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD + dig * 0.35F;
		// The star never quite stops twitching, and quivers when it digs or sniffs.
		float sniff = Mth.clamp(Mth.sin(t * 0.09F) * 3 - 2, 0, 1);
		star.xRot = Mth.sin(t * 1.7F) * (0.04F + 0.12F * Math.max(sniff, dig));
		star.yRot = Mth.sin(t * 1.3F + 1) * 0.05F;
		tail.yRot = Mth.sin(t * 0.15F + walk) * 0.25F;
	}
}
