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
 * The Cragcleaver Ram: a great deep shaggy body with a beard of wool hanging under its chest, a long dark head with a
 * blunt snout, two ridged horns that sweep back from its brow, down past its ears and forward round its jaw, four long legs
 * and a stub of a tail.
 *
 * <p>Pawing for a charge ({@code windup}) it rocks back with its head low and one forehoof scraping the ground; charging
 * ({@code acting}) its head goes right down, horns first, and it gallops leaning forward. Dazed against the rock
 * ({@code stunned}) it sways where it stands, its head lolling.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code tools/ram_art.py}):</p>
 * <pre>
 *   body (0,0) 12x11x18      head (64,0) 7x7x8        snout (96,0) 5x4x3
 *   horn (64,16) 3x3x5       curl (82,16) 3x5x3       tip (96,16) 2x2x3     ear (106,16) 3x1x2
 *   leg (0,32) 4x10x4        tail (20,32) 3x4x2       beard (32,32) 10x6x4
 * </pre>
 */
public class CragcleaverRamModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 12.0F;
	private static final float LEG_Y = 14.0F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart beard;
	private final ModelPart tail;
	private final ModelPart rightFront;
	private final ModelPart leftFront;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public CragcleaverRamModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.beard = body.getChild("beard");
		this.tail = body.getChild("tail");
		this.rightFront = root.getChild("right_front_leg");
		this.leftFront = root.getChild("left_front_leg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -5.5F, -9.0F, 12.0F, 11.0F, 18.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));
		body.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(32, 32).addBox(-5.0F, 0.0F, -2.0F, 10.0F, 6.0F, 4.0F),
			PartPose.offset(0.0F, 2.5F, -7.0F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 32).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 4.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -4.5F, 8.5F, 0.3F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 0).addBox(-3.5F, -5.0F, -7.0F, 7.0F, 7.0F, 8.0F),
			PartPose.offset(0.0F, -3.0F, -9.0F));
		head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(96, 0).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, -7.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(106, 16).addBox(-3.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
			PartPose.offset(-3.5F, -2.5F, -1.5F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(106, 16).mirror().addBox(0.0F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
			PartPose.offset(3.5F, -2.5F, -1.5F));
		horn(head, "right", -1, false);
		horn(head, "left", 1, true);

		leg(root, "right_front_leg", -3.5F, -6.0F, false);
		leg(root, "left_front_leg", 3.5F, -6.0F, true);
		leg(root, "right_hind_leg", -3.5F, 6.0F, false);
		leg(root, "left_hind_leg", 3.5F, 6.0F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** A horn: back from the brow, down behind the ear, and forward round the jaw, flaring outward. */
	private static void horn(PartDefinition head, String side, int sign, boolean mirror) {
		CubeListBuilder base = CubeListBuilder.create().texOffs(64, 16);
		CubeListBuilder curl = CubeListBuilder.create().texOffs(82, 16);
		CubeListBuilder tip = CubeListBuilder.create().texOffs(96, 16);
		if (mirror) {
			base.mirror();
			curl.mirror();
			tip.mirror();
		}
		PartDefinition root = head.addOrReplaceChild(side + "_horn", base.addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
			PartPose.offsetAndRotation(sign * 4.0F, -4.0F, -4.0F, -0.2F, sign * 0.3F, 0.0F));
		PartDefinition bend = root.addOrReplaceChild(side + "_horn_curl", curl.addBox(-1.5F, 0.0F, -3.0F, 3.0F, 5.0F, 3.0F),
			PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.2F, 0.0F, 0.0F));
		bend.addOrReplaceChild(side + "_horn_tip", tip.addBox(-1.0F, -2.0F, -3.0F, 2.0F, 2.0F, 3.0F),
			PartPose.offsetAndRotation(0.0F, 5.0F, -3.0F, -0.3F, sign * 0.35F, 0.0F));
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 32);
		if (mirror) {
			cubes.mirror();
		}
		root.addOrReplaceChild(name, cubes.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F), PartPose.offset(x, LEG_Y, z));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float charge = state.acting;
		float paw = state.windup;
		float daze = state.stunned;
		float walk = state.walkAnimationPos * (0.6F + 0.3F * charge);
		float stride = Math.min(1, state.walkAnimationSpeed * 1.4F + charge);

		// The body: rocking back to paw, leaning into the charge, swaying when dazed.
		body.y = BODY_Y + Mth.sin(walk * 2) * 0.5F * stride + 0.6F * paw;
		body.xRot = -0.06F * paw + 0.1F * charge + Mth.sin(walk) * 0.03F * stride;
		body.zRot = Mth.sin(t * 0.15F) * 0.08F * daze;

		// The head: looking about, low to paw, right down horns first to charge, lolling when dazed.
		float settled = 1 - Math.max(paw, Math.max(charge, daze));
		head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD * settled + Mth.sin(t * 0.3F) * 0.3F * daze;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * settled + 0.55F * paw + 0.9F * charge + 0.35F * daze
			+ Mth.sin(t * 0.5F) * 0.06F * paw;
		head.zRot = Mth.sin(t * 0.22F) * 0.15F * daze;

		// Ears out to the sides, flicking now and then; flattened back to charge.
		rightEar.zRot = -0.25F - 0.4F * charge + Math.max(0, Mth.sin(t * 0.2F) - 0.92F) * 3 + 0.4F * daze;
		leftEar.zRot = 0.25F + 0.4F * charge - Math.max(0, Mth.sin(t * 0.17F + 1.3F) - 0.92F) * 3 - 0.4F * daze;
		rightEar.yRot = 0.3F * charge;
		leftEar.yRot = -0.3F * charge;

		// The beard swings as it moves, the tail flicks.
		beard.xRot = -Mth.sin(walk * 2) * 0.12F * stride - 0.25F * charge;
		tail.xRot = 0.3F + 0.5F * charge + Mth.sin(t * 0.3F) * 0.1F;

		// Legs: a long walk or a gallop; one forehoof scraping back and back again to paw; splayed and wobbly when dazed.
		float swing = Mth.cos(walk) * (0.7F + 0.3F * charge) * stride;
		float gallop = charge * 0.6F;
		rightFront.xRot = swing * (1 - paw) + paw * (0.1F + Mth.sin(t * 0.8F) * 0.55F);
		leftFront.xRot = -swing * (1 - gallop) + swing * gallop - 0.25F * paw;
		rightHind.xRot = -swing + 0.15F * paw;
		leftHind.xRot = swing * (1 - gallop) - swing * gallop + 0.15F * paw;
		float splay = 0.12F * daze + Mth.sin(t * 0.15F) * 0.05F * daze;
		rightFront.zRot = rightHind.zRot = splay;
		leftFront.zRot = leftHind.zRot = -splay;
	}
}
