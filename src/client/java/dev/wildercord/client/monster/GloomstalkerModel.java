package dev.wildercord.client.monster;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The Gloomstalker: a long, low panther of shadow, deep chest tapering to lean hips, a small wedge of a head with swept-back
 * ears, a long tail that curls at the tip and trails wisps of dark, and a crest of smoky fur along its spine. It trots
 * lightly; crouching to pounce it sinks low on folded legs, head down, ears flat, tail lashing; mid-pounce it stretches out
 * full length, forelegs reaching; a missed pounce leaves it sprawled flat, legs splayed.
 *
 * <p>Drawn translucent (its renderer sets how much shows), so the default render type is one that blends.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code gloomstalker} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   chest (0,0) 7x7x8     hips (0,16) 6x6x9     head (48,0) 6x5x6     snout (72,0) 4x3x3    ear (86,0) 2x3x1
 *   jaw (92,0) 4x1x3      leg (48,12) 3x9x3     paw (60,12) 4x2x4     tail (76,12) 2x2x10   tip (100,12) 2x2x8
 *   crest (0,32) 0x3x14   wisp (30,32) 0x4x5
 * </pre>
 */
public class GloomstalkerModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart tip;
	private final ModelPart frontRight;
	private final ModelPart frontLeft;
	private final ModelPart backRight;
	private final ModelPart backLeft;

	public GloomstalkerModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.jaw = head.getChild("jaw");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.tail = body.getChild("tail");
		this.tip = tail.getChild("tip");
		this.frontRight = root.getChild("front_right");
		this.frontLeft = root.getChild("front_left");
		this.backRight = root.getChild("back_right");
		this.backLeft = root.getChild("back_left");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// A deep chest and lean hips, one piece, pivoting at the shoulders' back.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5F, -4.0F, -8.0F, 7.0F, 7.0F, 8.0F)
				.texOffs(0, 16).addBox(-3.0F, -3.5F, 0.0F, 6.0F, 6.0F, 9.0F),
			PartPose.offset(0.0F, 12.5F, 0.0F));
		body.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 14.0F),
			PartPose.offset(0.0F, -3.9F, -7.0F));

		// A small wedge of a head, carried low.
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 0).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 6.0F)
				.texOffs(72, 0).addBox(-2.0F, -0.5F, -8.0F, 4.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, -2.0F, -8.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(92, 0).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 1.0F, 3.0F),
			PartPose.offset(0.0F, 2.5F, -5.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(86, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(-2.0F, -2.5F, -1.5F, -0.35F, 0.0F, -0.2F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(86, 0).mirror().addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
			PartPose.offsetAndRotation(2.0F, -2.5F, -1.5F, -0.35F, 0.0F, 0.2F));

		// A long tail, rising, curling at the tip into wisps of dark.
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(76, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 10.0F),
			PartPose.offsetAndRotation(0.0F, -2.0F, 8.5F, -0.5F, 0.0F, 0.0F));
		PartDefinition tip = tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(100, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F),
			PartPose.offsetAndRotation(0.0F, 0.0F, 9.5F, 0.55F, 0.0F, 0.0F));
		tip.addOrReplaceChild("wisp", CubeListBuilder.create().texOffs(30, 32).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 5.0F),
			PartPose.offset(0.0F, 0.0F, 7.0F));

		leg(root, "front_right", -2.2F, -5.0F, false);
		leg(root, "front_left", 2.2F, -5.0F, true);
		leg(root, "back_right", -2.2F, 6.0F, false);
		leg(root, "back_left", 2.2F, 6.0F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder leg = CubeListBuilder.create().texOffs(48, 12);
		PartDefinition part = root.addOrReplaceChild(name, (mirror ? leg.mirror() : leg).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F),
			PartPose.offset(x, 15.0F, z));
		CubeListBuilder paw = CubeListBuilder.create().texOffs(60, 12);
		part.addOrReplaceChild("paw", (mirror ? paw.mirror() : paw).addBox(-2.0F, 0.0F, -2.6F, 4.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 7.0F, 0.0F));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos * 0.75F;
		float crouch = state.windup;
		float leap = state.acting;
		float sprawl = state.stunned;
		float slink = state.alt;

		// A light trot: diagonal pairs together, a little bob.
		float stride = Mth.cos(pos) * 0.9F * walk;
		frontRight.xRot = stride;
		backLeft.xRot = stride;
		frontLeft.xRot = -stride;
		backRight.xRot = -stride;
		body.y = 12.5F - Math.abs(Mth.sin(pos)) * 0.6F * walk;
		body.xRot = 0.0F;

		// Crouched to pounce: low on folded legs, hindquarters wiggling.
		float sink = 3.2F * crouch;
		body.y += sink;
		body.zRot = crouch * Mth.sin(t * 0.9F) * 0.04F;
		foldLegs(crouch, -0.95F, 0.95F);
		// Mid-pounce: stretched out full length, forelegs reaching, hind legs kicked back.
		body.xRot -= 0.15F * leap;
		frontRight.xRot = Mth.lerp(leap, frontRight.xRot, -1.35F);
		frontLeft.xRot = Mth.lerp(leap, frontLeft.xRot, -1.25F);
		backRight.xRot = Mth.lerp(leap, backRight.xRot, 1.2F);
		backLeft.xRot = Mth.lerp(leap, backLeft.xRot, 1.3F);
		// Sprawled after a miss: flat on its belly, legs splayed.
		body.y += 4.0F * sprawl;
		frontRight.zRot = 1.0F * sprawl;
		backRight.zRot = 1.1F * sprawl;
		frontLeft.zRot = -1.0F * sprawl;
		backLeft.zRot = -1.1F * sprawl;
		for (ModelPart leg : new ModelPart[] {frontRight, frontLeft, backRight, backLeft}) {
			leg.y = 15.0F + sink + 3.5F * sprawl;
		}

		// The head turns to look; low and level while it crouches, dropped on the ground when sprawled.
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - sprawl);
		head.xRot = Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD + 0.25F * crouch - 0.25F * leap + 0.35F * sprawl;
		jaw.xRot = 0.5F * crouch * (0.6F + 0.4F * Mth.sin(t * 0.5F)) + 0.6F * leap;
		// Ears flatten as it crouches, slinks off or leaps.
		float flat = Math.max(crouch, Math.max(slink, leap));
		rightEar.xRot = -0.35F - 0.9F * flat;
		leftEar.xRot = -0.35F - 0.9F * flat;

		// The tail: a slow sway at rest, a lash while it crouches, straight out behind in a leap, low as it slinks away.
		float lash = Mth.sin(t * (0.12F + 0.8F * crouch)) * (0.25F + 0.35F * crouch);
		tail.yRot = lash * (1 - leap);
		tail.xRot = -0.5F + 0.45F * leap + 0.5F * slink + 0.45F * sprawl + Mth.sin(t * 0.07F) * 0.05F;
		tip.xRot = 0.55F - 0.45F * leap + Mth.sin(t * 0.1F + 1.2F) * 0.12F;
		tip.yRot = lash * 0.8F * (1 - leap);
	}

	private void foldLegs(float amount, float front, float back) {
		frontRight.xRot = Mth.lerp(amount, frontRight.xRot, front);
		frontLeft.xRot = Mth.lerp(amount, frontLeft.xRot, front);
		backRight.xRot = Mth.lerp(amount, backRight.xRot, back);
		backLeft.xRot = Mth.lerp(amount, backLeft.xRot, back);
	}
}
