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
 * A stonecrest puffin: a stout upright body, a round head with a great deep bill banded to an orange-red tip, short wings at its
 * sides, a stub of a tail and short orange legs with broad webbed feet. It waddles side to side with its wings held a little out;
 * its wings whirr in a blur aloft and give a quick shake to stretch ({@link WildlifeRenderState#flapStrength}); afloat it sits low
 * and paddles ({@link WildlifeRenderState#swim}); under the water it tips nose down and rows with its wings
 * ({@link WildlifeRenderState#dive}); it preens with its head turned back into its wing ({@link WildlifeRenderState#bow}),
 * bill-taps with its mate, its head bobbing and its bill rattling side to side ({@link WildlifeRenderState#knock}), sleeps with its
 * head tucked back ({@link WildlifeRenderState#settle}) and tips its head back to gulp its fish ({@link WildlifeRenderState#graze}).
 * Fish it carries hang crosswise from its bill ({@link WildlifeRenderState#held}). A puffling has a bigger head. UVs match
 * tools/puffin_art.py.
 */
public final class StonecrestPuffinModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 21, HEAD_Y = -5.5F, WING_REST = 0.12F, FLOAT = 3;

	private final ModelPart body, head, beak, fish, leftWing, rightWing, tail, leftLeg, rightLeg;

	public StonecrestPuffinModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		beak = head.getChild("beak");
		fish = beak.getChild("fish");
		leftWing = body.getChild("left_wing");
		rightWing = body.getChild("right_wing");
		tail = body.getChild("tail");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -6, -3, 5, 6, 6),
			PartPose.offset(0, BODY_Y, 1));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-2, -4, -2, 4, 4, 4),
			PartPose.offset(0, HEAD_Y, -1.5F));
		PartDefinition beak = head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(42, 0).addBox(-1, -1.5F, -2, 2, 3, 2),
			PartPose.offset(0, -1.8F, -2));
		beak.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(42, 6).addBox(-1, -1, -1, 2, 2, 1), PartPose.offset(0, 0.2F, -2));
		beak.addOrReplaceChild("fish", CubeListBuilder.create()
			.texOffs(0, 24).addBox(-3, -0.5F, -0.5F, 6, 1, 1)
			.texOffs(0, 27).addBox(-2.5F, 0.5F, -0.3F, 5, 1, 1), PartPose.offset(0, 1.2F, -1.5F));
		body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 14).addBox(0, 0, -1, 1, 5, 4),
			PartPose.offset(2.5F, -5.5F, -1));
		body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-1, 0, -1, 1, 5, 4),
			PartPose.offset(-2.5F, -5.5F, -1));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(12, 14).addBox(-1.5F, -0.5F, 0, 3, 1, 2),
			PartPose.offsetAndRotation(0, -1, 3, -0.3F, 0, 0));
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 10).addBox(-0.5F, 0, -0.5F, 1, 2, 1),
			PartPose.offset(1.2F, BODY_Y, 1));
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 10).mirror().addBox(-0.5F, 0, -0.5F, 1, 2, 1),
			PartPose.offset(-1.2F, BODY_Y, 1));
		leftLeg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(30, 10).addBox(-1, 2, -2.5F, 2, 1, 3), PartPose.ZERO);
		rightLeg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(30, 10).mirror().addBox(-1, 2, -2.5F, 2, 1, 3), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.6F);
		float walk = state.walkAnimationPos * 1.4F;

		// The waddle: rocking side to side from foot to foot, its wings held a little out for balance.
		body.xRot = 0;
		body.yRot = 0;
		body.zRot = Mth.sin(walk) * 0.18F * speed;
		body.y = BODY_Y - Mth.abs(Mth.sin(walk)) * 0.4F * speed;
		body.z = 1;
		body.yScale = 1;
		leftLeg.xRot = Mth.cos(walk) * 0.9F * speed;
		rightLeg.xRot = -Mth.cos(walk) * 0.9F * speed;
		leftLeg.y = rightLeg.y = BODY_Y;
		leftLeg.visible = rightLeg.visible = true;
		float out = WING_REST + 0.25F * speed;
		leftWing.zRot = -out;
		rightWing.zRot = out;
		leftWing.xRot = rightWing.xRot = 0;
		tail.xRot = -0.3F + Mth.sin(walk) * 0.1F * speed;
		tail.yRot = Mth.sin(t * 0.13F) * 0.08F;

		// The head: where it looks, now and then a quick cock of the head to one side.
		float cock = Mth.clamp(Mth.sin(t * 0.06F) * 6 - 5, 0, 1);
		head.yRot = Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.zRot = cock * 0.3F;
		head.y = HEAD_Y;
		beak.xRot = 0;
		beak.yRot = 0;
		fish.visible = state.held > 0;
		fish.zRot = Mth.sin(t * 0.4F) * 0.08F;

		// Afloat: sitting low on the water, its feet paddling under it, bobbing on the swell.
		float swim = state.swim;
		if (swim > 0) {
			body.y = Mth.lerp(swim, body.y, BODY_Y + FLOAT + Mth.sin(t * 0.12F) * 0.3F);
			body.zRot = Mth.lerp(swim, body.zRot, Mth.sin(t * 0.09F) * 0.05F);
			leftLeg.y = rightLeg.y = Mth.lerp(swim, BODY_Y, BODY_Y + FLOAT - 1);
			leftLeg.xRot = Mth.lerp(swim, leftLeg.xRot, 0.6F + Mth.sin(t * 0.5F) * 0.6F);
			rightLeg.xRot = Mth.lerp(swim, rightLeg.xRot, 0.6F - Mth.sin(t * 0.5F) * 0.6F);
			leftWing.zRot = -WING_REST * 0.5F;
			rightWing.zRot = WING_REST * 0.5F;
		}

		// Under the water: nose down, flying with its wings, feet trailing behind.
		float dive = state.dive;
		if (dive > 0) {
			body.xRot = Mth.lerp(dive, body.xRot, 1.2F);
			body.zRot = Mth.lerp(dive, body.zRot, 0);
			head.xRot = Mth.lerp(dive, head.xRot, -0.9F);
			head.yRot = Mth.lerp(dive, head.yRot, 0);
			leftLeg.xRot = Mth.lerp(dive, leftLeg.xRot, 1.4F);
			rightLeg.xRot = Mth.lerp(dive, rightLeg.xRot, 1.4F);
			tail.xRot = Mth.lerp(dive, tail.xRot, 0.1F);
		}

		// Its wings beating: a whirring blur aloft, a quick shake to stretch, long rowing strokes under the water.
		float flap = state.flapStrength;
		if (flap > 0) {
			float beat = 0.5F + 0.5F * Mth.sin(state.flapPhase);
			float lift = (0.3F + 1.2F * beat) * flap;
			leftWing.zRot = Mth.lerp(flap, leftWing.zRot, -lift);
			rightWing.zRot = Mth.lerp(flap, rightWing.zRot, lift);
			leftWing.xRot = rightWing.xRot = Mth.cos(state.flapPhase) * 0.3F * flap * dive;
			// Aloft its feet dangle back and its body leans forward a touch.
			float aloft = flap * (1 - swim) * (1 - dive);
			body.xRot += 0.25F * aloft;
			leftLeg.xRot = Mth.lerp(aloft, leftLeg.xRot, 0.7F);
			rightLeg.xRot = Mth.lerp(aloft, rightLeg.xRot, 0.7F);
		}

		// Preening: its head turned right round and buried in the feathers of its wing, the bill working.
		float preen = state.bow;
		if (preen > 0) {
			boolean leftSide = Mth.sin(state.seed) > 0;
			float side = leftSide ? 1 : -1;
			head.yRot = Mth.lerp(preen, head.yRot, side * (2.1F + Mth.sin(t * 0.1F) * 0.2F));
			head.xRot = Mth.lerp(preen, head.xRot, 0.55F + Mth.sin(t * 1.3F) * 0.1F);
			head.zRot = Mth.lerp(preen, head.zRot, 0);
			ModelPart wing = leftSide ? leftWing : rightWing;
			wing.zRot = Mth.lerp(preen, wing.zRot, -side * 0.5F);
			beak.xRot = Mth.abs(Mth.sin(t * 1.3F)) * 0.15F * preen;
		}

		// Bill-tapping: head bobbing toward its mate, its bill rattling quickly from side to side.
		float tap = state.knock;
		if (tap > 0) {
			head.xRot = Mth.lerp(tap, head.xRot, 0.25F + Mth.sin(t * 0.6F) * 0.15F);
			head.yRot = Mth.lerp(tap, head.yRot, Mth.sin(t * 2.4F) * 0.35F);
			head.zRot = Mth.lerp(tap, head.zRot, Mth.sin(t * 2.4F) * 0.15F);
			body.xRot += 0.15F * tap;
			leftWing.zRot -= 0.15F * tap;
			rightWing.zRot += 0.15F * tap;
		}

		// Gulping its fish: head tipped back, bill working, a little shake.
		float munch = state.graze;
		if (munch > 0) {
			head.xRot = Mth.lerp(munch, head.xRot, -0.6F + Mth.sin(t * 1.6F) * 0.12F);
			head.zRot = Mth.lerp(munch, head.zRot, Mth.sin(t * 0.9F) * 0.1F);
			beak.xRot = Mth.abs(Mth.sin(t * 1.6F)) * 0.18F * munch;
		}

		// Asleep: settled down on its feet, head tucked back over its shoulder with the bill in its feathers, breathing slow.
		float sleep = state.settle;
		if (sleep > 0) {
			body.y = Mth.lerp(sleep, body.y, body.y + 1.2F);
			body.zRot = Mth.lerp(sleep, body.zRot, 0);
			body.xRot = Mth.lerp(sleep, body.xRot, 0);
			head.yRot = Mth.lerp(sleep, head.yRot, 2.5F);
			head.xRot = Mth.lerp(sleep, head.xRot, 0.35F);
			head.zRot = Mth.lerp(sleep, head.zRot, 0);
			head.y = Mth.lerp(sleep, HEAD_Y, HEAD_Y + 1);
			leftWing.zRot = Mth.lerp(sleep, leftWing.zRot, 0);
			rightWing.zRot = Mth.lerp(sleep, rightWing.zRot, 0);
			leftLeg.y = rightLeg.y = Mth.lerp(sleep, leftLeg.y, leftLeg.y + 0.8F);
			body.yScale = 1 + Mth.sin(t * 0.08F) * 0.03F * sleep;
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.3F;
		}
	}
}
