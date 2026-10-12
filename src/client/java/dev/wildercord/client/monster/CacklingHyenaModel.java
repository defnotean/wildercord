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
 * The Cackling Hyena: a deep chest and a smaller, lower rump, so its back slopes down from high shoulders to low hips;
 * long forelegs and short hind legs; a bristling mane down its neck and back; a broad head with round ears, a blunt dark
 * muzzle and a jaw that drops; and a short tail ending in a dark tuft.
 *
 * <p>It lopes with its head low. Crouched to dart ({@code windup}) its forequarters drop, its rump rises, its mane stands
 * up and its ears go flat; darting ({@code acting}) it stretches out long, jaws wide, and snaps them shut. Stumbling from a
 * miss ({@code stunned}) it lurches nose-down. Falling back ({@code alt}) or slinking off ({@code guard}) its tail is tucked
 * and its head low, and slinking off it cringes lower still. Every so often it lifts its head, opens its jaws and cackles,
 * its shoulders shaking.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/hyena_art.py}):</p>
 * <pre>
 *   chest (0,0) 7x8x7       rump (28,0) 6x6x7
 *   head (0,16) 6x6x5       muzzle (22,16) 3x2x4    jaw (36,16) 3x1x4    ear (50,16) 2x3x1
 *   mane (0,28) 2x2x12      tail (28,28) 1x5x1      tuft (32,28) 2x3x2
 *   foreleg (0,44) 2x10x2   hind leg (8,44) 2x8x2
 * </pre>
 */
public class CacklingHyenaModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 11.0F;
	/** The slope of its back, high shoulders to low hips. */
	private static final float SLOPE = -0.18F;

	private final ModelPart body;
	private final ModelPart mane;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart rightFore;
	private final ModelPart leftFore;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public CacklingHyenaModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.mane = body.getChild("mane");
		this.head = body.getChild("head");
		this.jaw = head.getChild("jaw");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.tail = body.getChild("tail");
		this.rightFore = root.getChild("right_foreleg");
		this.leftFore = root.getChild("left_foreleg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -4.0F, -7.0F, 7.0F, 8.0F, 7.0F)
				.texOffs(28, 0).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 7.0F),
			PartPose.offsetAndRotation(0.0F, BODY_Y, 0.0F, SLOPE, 0.0F, 0.0F));
		body.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, -2.0F, -8.0F, 2.0F, 2.0F, 12.0F),
			PartPose.offset(0.0F, -3.5F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 6.0F, 5.0F)
				.texOffs(22, 16).addBox(-1.5F, -0.5F, -9.0F, 3.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, -1.5F, -6.5F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, 1.5F, -5.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(50, 16).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(-2.2F, -2.5F, -1.5F, 0.0F, 0.0F, -0.3F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(50, 16).mirror().addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(2.2F, -2.5F, -1.5F, 0.0F, 0.0F, 0.3F));

		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 28).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F)
				.texOffs(32, 28).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 3.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -2.0F, 6.5F, 0.5F, 0.0F, 0.0F));

		root.addOrReplaceChild("right_foreleg", CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
			PartPose.offset(-2.2F, 14.0F, -4.5F));
		root.addOrReplaceChild("left_foreleg", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
			PartPose.offset(2.2F, 14.0F, -4.5F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(8, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(-1.8F, 16.0F, 5.0F));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(8, 44).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(1.8F, 16.0F, 5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos * 0.9F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.6F);
		float crouch = state.windup;
		float dart = state.acting;
		float stumble = state.stunned;
		float slink = Math.max(state.alt * 0.6F, state.guard);

		// The lope: fore and hind legs in diagonal pairs, the body rocking over them.
		float swing = Mth.cos(walk) * 0.9F * stride;
		rightFore.xRot = swing;
		leftHind.xRot = swing * 0.8F;
		leftFore.xRot = -swing;
		rightHind.xRot = -swing * 0.8F;
		body.y = BODY_Y + Math.abs(Mth.sin(walk)) * 0.6F * stride;
		body.xRot = SLOPE + Mth.sin(walk * 2) * 0.03F * stride;
		body.zRot = 0;
		rightFore.y = leftFore.y = 14.0F;
		rightHind.y = leftHind.y = 16.0F;

		// Its head carried low, looking about; the jaw a little slack.
		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD + 0.2F;
		head.zRot = 0;
		jaw.xRot = 0.08F + Mth.sin(t * 0.1F) * 0.04F;
		rightEar.xRot = leftEar.xRot = 0;
		rightEar.zRot = -0.3F + Mth.sin(t * 0.07F) * 0.05F;
		leftEar.zRot = 0.3F - Mth.sin(t * 0.09F + 1) * 0.05F;
		mane.y = -3.5F;
		mane.yScale = 1;
		tail.xRot = 0.5F + Mth.sin(t * 0.12F) * 0.08F + stride * 0.3F;
		tail.zRot = Mth.sin(t * 0.1F) * 0.15F;

		// Now and then it lifts its head and cackles, its shoulders shaking.
		float laugh = Mth.clamp(Mth.sin(t * 0.031F) * 8 - 7, 0, 1) * (1 - crouch) * (1 - dart) * (1 - slink);
		if (laugh > 0) {
			float shake = Mth.sin(t * 2.2F);
			head.xRot = Mth.lerp(laugh, head.xRot, -0.35F + shake * 0.08F);
			jaw.xRot = Mth.lerp(laugh, jaw.xRot, 0.45F + Math.abs(shake) * 0.25F);
			body.y += shake * 0.25F * laugh;
		}

		if (crouch > 0) {
			// Forequarters down, rump up, mane bristling, ears flat, eyes fixed.
			body.y += 1.5F * crouch;
			body.xRot += 0.28F * crouch;
			rightFore.xRot = Mth.lerp(crouch, rightFore.xRot, -0.5F);
			leftFore.xRot = Mth.lerp(crouch, leftFore.xRot, -0.5F);
			rightFore.y = leftFore.y = 14.0F + 1.0F * crouch;
			rightHind.xRot = Mth.lerp(crouch, rightHind.xRot, 0.35F);
			leftHind.xRot = Mth.lerp(crouch, leftHind.xRot, 0.35F);
			head.xRot = Mth.lerp(crouch, head.xRot, -0.15F);
			mane.y = -3.5F - 0.8F * crouch;
			mane.yScale = 1 + 0.6F * crouch;
			rightEar.xRot = leftEar.xRot = -0.9F * crouch;
			jaw.xRot = Mth.lerp(crouch, jaw.xRot, 0.2F + Mth.sin(t * 1.5F) * 0.05F);
			tail.xRot = Mth.lerp(crouch, tail.xRot, 1.3F + Mth.sin(t * 0.8F) * 0.2F);
		}

		if (dart > 0) {
			// Stretched out long, forelegs reaching, hind legs driving, jaws wide.
			rightFore.xRot = leftFore.xRot = Mth.lerp(dart, rightFore.xRot, -1.1F);
			rightHind.xRot = leftHind.xRot = Mth.lerp(dart, rightHind.xRot, 1.0F);
			body.xRot = Mth.lerp(dart, body.xRot, SLOPE + 0.05F);
			head.xRot = Mth.lerp(dart, head.xRot, 0.1F);
			jaw.xRot = Mth.lerp(dart, jaw.xRot, 0.85F);
			mane.yScale = 1 + 0.6F * dart;
			mane.y = -3.5F - 0.8F * dart;
			rightEar.xRot = leftEar.xRot = -0.9F * dart;
			tail.xRot = Mth.lerp(dart, tail.xRot, 1.5F);
		}

		if (stumble > 0) {
			// A lurch, nose to the ground, a foreleg buckled.
			body.xRot += 0.35F * stumble;
			body.zRot = Mth.sin(t * 0.9F) * 0.12F * stumble;
			body.y += 1.2F * stumble;
			rightFore.xRot = Mth.lerp(stumble, rightFore.xRot, -0.9F);
			head.xRot = Mth.lerp(stumble, head.xRot, 0.7F);
			head.zRot = 0.25F * stumble;
			jaw.xRot = Mth.lerp(stumble, jaw.xRot, 0.3F);
		}

		if (slink > 0) {
			// Tail tucked between its legs, head and ears low, cringing.
			body.y += 1.0F * state.guard;
			body.xRot += 0.08F * slink;
			head.xRot = Mth.lerp(slink, head.xRot, 0.55F);
			rightEar.xRot = leftEar.xRot = -1.0F * slink;
			rightEar.zRot = Mth.lerp(slink, rightEar.zRot, -0.9F);
			leftEar.zRot = Mth.lerp(slink, leftEar.zRot, 0.9F);
			mane.yScale = 1 - 0.4F * slink;
			tail.xRot = Mth.lerp(slink, tail.xRot, -0.35F);
			tail.zRot *= 1 - slink;
		}
	}
}
