package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Rimecoat Wolverine: a low, broad body arched at the back, a broad head with a blunt muzzle and small round ears,
 * four short thick legs, and a bushy tail hanging low.
 *
 * <p>It lopes with its back rolling. Growling before a snatch ({@code windup}) it crouches with its head low and its ears
 * flat; the snatch or a bite ({@code acting}) snaps its head forward. Carrying ({@code alt}) it runs with its head high and
 * its tail up; eating ({@code veil}) its head goes down to the snow, chewing. Cornered ({@code guard}) its back arches, its
 * tail bristles and its head stays low.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/wolverine_art.py}):</p>
 * <pre>
 *   body (0,0) 8x7x14
 *   head (0,22) 6x5x5        muzzle (22,22) 4x3x3     ear (36,22) 2x2x1
 *   leg (0,32) 3x6x3         tail (14,32) 3x3x6
 * </pre>
 */
public class RimecoatWolverineModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 15.0F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart muzzle;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart rightFront;
	private final ModelPart leftFront;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public RimecoatWolverineModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.muzzle = head.getChild("muzzle");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.tail = body.getChild("tail");
		this.rightFront = root.getChild("right_front_leg");
		this.leftFront = root.getChild("left_front_leg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.5F, -7.0F, 8.0F, 7.0F, 14.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 22).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, -1.0F, -6.5F));
		head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(22, 22).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 0.5F, -5.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(36, 22).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(-2.2F, -2.5F, -2.0F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(36, 22).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(2.2F, -2.5F, -2.0F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(14, 32).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F),
			PartPose.offsetAndRotation(0.0F, -2.0F, 6.5F, -0.5F, 0.0F, 0.0F));

		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
			PartPose.offset(-2.5F, 18.0F, -4.5F));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
			PartPose.offset(2.5F, 18.0F, -4.5F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
			PartPose.offset(-2.5F, 18.0F, 5.0F));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
			PartPose.offset(2.5F, 18.0F, 5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos * 0.9F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float growl = state.windup;
		float snap = state.acting;
		float carry = state.alt;
		float eat = state.veil;
		float fury = state.guard;

		// A rolling lope, the back rising and falling; crouched to growl, arched when cornered.
		body.y = BODY_Y + Mth.sin(walk * 2) * 0.4F * stride + 1.2F * growl - 0.4F * fury;
		body.xRot = Mth.sin(walk) * 0.05F * stride + 0.08F * growl - 0.06F * fury;
		body.zRot = Mth.sin(walk) * 0.03F * stride;

		// The head: looking about, low to growl, snapping forward, high when it carries, down to the snow to eat.
		float chew = eat * Mth.sin(t * 0.9F) * 0.12F;
		head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD * (1 - eat);
		head.xRot = state.xRot * Mth.DEG_TO_RAD * (1 - eat) + 0.35F * growl - 0.25F * snap - 0.3F * carry + 0.8F * eat + chew + 0.15F * fury;
		head.z = -6.5F - 1.2F * snap;
		head.y = -1.0F + 0.8F * growl;

		// Ears: twitching, flattened back to growl or fight.
		float flat = Math.max(growl, fury);
		rightEar.xRot = leftEar.xRot = 0.6F * flat;
		rightEar.zRot = -0.1F - 0.3F * flat + Math.max(0, Mth.sin(t * 0.21F) - 0.9F) * 2;
		leftEar.zRot = 0.1F + 0.3F * flat - Math.max(0, Mth.sin(t * 0.19F + 1.1F) - 0.9F) * 2;

		// The muzzle drops open to growl and shuts on the snap.
		muzzle.xRot = -0.15F * growl + 0.1F * snap;

		// The tail: hanging low and swaying, up when it runs off with something, bristled out when cornered.
		tail.xRot = -0.5F + 0.35F * carry + 0.4F * fury + Mth.sin(walk) * 0.15F * stride;
		tail.yRot = Mth.sin(t * 0.07F) * 0.15F * (1 - fury);
		float bristle = 1 + 0.35F * fury;
		tail.xScale = tail.yScale = bristle;

		// Legs: a trot, splayed and braced to growl.
		float swing = Mth.cos(walk) * 0.8F * stride;
		rightFront.xRot = swing - 0.3F * growl;
		leftHind.xRot = swing;
		leftFront.xRot = -swing - 0.3F * growl;
		rightHind.xRot = -swing;
		float splay = 0.15F * growl + 0.1F * fury;
		rightFront.zRot = rightHind.zRot = splay;
		leftFront.zRot = leftHind.zRot = -splay;
	}

	/** Moves {@code poseStack} to between its jaws, where what it carries is held. */
	public void toMouth(PoseStack poseStack) {
		root().translateAndRotate(poseStack);
		body.translateAndRotate(poseStack);
		head.translateAndRotate(poseStack);
		muzzle.translateAndRotate(poseStack);
		poseStack.translate(0, 1.6F / 16F, -2.6F / 16F);
	}
}
