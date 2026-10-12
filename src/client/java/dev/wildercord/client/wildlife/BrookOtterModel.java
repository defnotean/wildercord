package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.OtterRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A brook otter: a long sleek body hung from its middle, so it can roll over onto its back; a broad flat head with a blunt muzzle,
 * a dark nose and little round ears; short legs; and a thick tail tapering to a point. Ashore it lopes; in the water it swims
 * low, legs paddling and tail sculling. On its back ({@link WildlifeRenderState#onBack}) it floats belly up with its forepaws
 * folded on its chest, and from there it knocks a shell on a pebble on its belly or eats a fish laid across its chest
 * ({@link WildlifeRenderState#crack}), or reaches out one forepaw to hold its partner's ({@link WildlifeRenderState#hold}).
 * Ashore it sits up to juggle a pebble from paw to paw ({@link WildlifeRenderState#sit}). A pup has a bigger head. UVs match
 * tools/otter_art.py.
 */
public final class BrookOtterModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 19.5F, BACK_Y = 17.5F, FRONT_Z = -4.5F, HIND_Z = 4.5F, LEG_Y = 2.5F, TAIL_REST = -0.15F;

	private final ModelPart body, head, snout, leftEar, rightEar, tail, tip, frontLeft, frontRight, hindLeft, hindRight, shell, pebble, fish;

	public BrookOtterModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		snout = head.getChild("snout");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		tail = body.getChild("tail");
		tip = tail.getChild("tip");
		frontLeft = body.getChild("front_left_leg");
		frontRight = body.getChild("front_right_leg");
		hindLeft = body.getChild("hind_left_leg");
		hindRight = body.getChild("hind_right_leg");
		shell = body.getChild("shell");
		pebble = body.getChild("pebble");
		fish = body.getChild("fish");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Hung from its middle, so it rolls over in place.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2.5F, -6, 6, 5, 12),
			PartPose.offset(0, BODY_Y, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(38, 0).addBox(-2.5F, -2.5F, -4, 5, 4, 4),
			PartPose.offset(0, -0.5F, -6));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(38, 10).addBox(-1.5F, -1, -2, 3, 2, 2),
			PartPose.offset(0, 0.5F, -4));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(50, 10).addBox(-1, -1.3F, -2.4F, 2, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 10).addBox(-0.5F, -1, -0.5F, 1, 1, 1), PartPose.offset(2, -2.5F, -1));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 10).mirror().addBox(-0.5F, -1, -0.5F, 1, 1, 1), PartPose.offset(-2, -2.5F, -1));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 18).addBox(-2, -1.5F, 0, 4, 3, 6),
			PartPose.offsetAndRotation(0, -0.5F, 6, TAIL_REST, 0, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(20, 18).addBox(-1.5F, -1, 0, 3, 2, 6), PartPose.offset(0, 0.2F, 5.5F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(40, 18).addBox(-1, 0, -1, 2, 2, 2);
		CubeListBuilder mirrored = CubeListBuilder.create().texOffs(40, 18).mirror().addBox(-1, 0, -1, 2, 2, 2);
		body.addOrReplaceChild("front_left_leg", leg, PartPose.offset(2, LEG_Y, FRONT_Z));
		body.addOrReplaceChild("front_right_leg", mirrored, PartPose.offset(-2, LEG_Y, FRONT_Z));
		body.addOrReplaceChild("hind_left_leg", leg, PartPose.offset(2, LEG_Y, HIND_Z));
		body.addOrReplaceChild("hind_right_leg", mirrored, PartPose.offset(-2, LEG_Y, HIND_Z));
		// What it plays with: a shell, the pebble it knocks it on (or juggles), a fish it caught.
		body.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, -0.5F, -1.5F, 3, 1, 3), PartPose.ZERO);
		body.addOrReplaceChild("pebble", CubeListBuilder.create().texOffs(40, 28).addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1), PartPose.ZERO);
		PartDefinition fish = body.addOrReplaceChild("fish", CubeListBuilder.create().texOffs(16, 28).addBox(-0.5F, -1, -3, 1, 2, 6), PartPose.ZERO);
		fish.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(32, 28).addBox(-0.5F, -1.5F, 0, 1, 3, 2), PartPose.offset(0, 0, 3));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float walk = state.walkAnimationPos * 0.9F;
		float back = state.onBack, swim = state.swim, sit = state.sit;

		// Ashore, a humping lope: forepaws together, then hind paws, the back rising between them. In the water the legs paddle
		// and the body ripples, the tail sculling behind.
		float lope = Mth.cos(walk) * speed * (1 - swim);
		float paddle = Mth.sin(t * 0.6F) * swim;
		frontLeft.xRot = lope * 0.9F + paddle * 0.8F;
		frontRight.xRot = lope * 0.9F - paddle * 0.8F;
		hindLeft.xRot = -lope * 0.9F - paddle * 0.6F + 0.6F * swim;
		hindRight.xRot = -lope * 0.9F + paddle * 0.6F + 0.6F * swim;
		frontLeft.zRot = frontRight.zRot = hindLeft.zRot = hindRight.zRot = 0;
		body.xRot = Mth.sin(walk) * 0.1F * speed * (1 - swim) + Mth.sin(t * 0.3F) * 0.05F * swim;
		body.yRot = 0;
		body.zRot = 0;
		body.y = BODY_Y - Mth.abs(Mth.sin(walk)) * speed * (1 - swim) + 1.5F * swim;

		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD * (1 - back);
		head.xRot = state.xRot * Mth.DEG_TO_RAD * (1 - back) - body.xRot;
		snout.xRot = Mth.sin(t * 2.0F) * 0.04F;
		float flick = Mth.clamp(Mth.sin(t * 0.06F) * 5 - 4, 0, 1);
		leftEar.zRot = flick * 0.3F;
		rightEar.zRot = -flick * 0.3F;
		tail.xRot = TAIL_REST + Mth.sin(walk) * 0.2F * speed;
		tail.yRot = Mth.sin(t * 0.3F) * 0.35F * swim + Mth.sin(t * 0.07F) * 0.1F * (1 - swim);
		tip.yRot = Mth.sin(t * 0.3F - 0.9F) * 0.45F * swim;
		tip.xRot = 0.08F;

		shell.visible = false;
		pebble.visible = false;
		fish.visible = false;

		// On its back at the surface: rolled belly up and bobbing, forepaws folded on its chest, hind feet up, the head lifted to
		// look along its body, the tail trailing flat.
		if (back > 0) {
			body.zRot = Mth.PI * back;
			body.xRot = Mth.lerp(back, body.xRot, Mth.sin(t * 0.1F) * 0.04F);
			body.y = Mth.lerp(back, body.y, BACK_Y + Mth.sin(t * 0.12F) * 0.3F);
			body.yRot = Mth.sin(t * 0.05F) * 0.06F * back;
			head.xRot = Mth.lerp(back, head.xRot, 0.35F);
			head.yRot += Mth.sin(t * 0.04F) * 0.25F * back;
			frontLeft.xRot = Mth.lerp(back, frontLeft.xRot, 1.0F);
			frontRight.xRot = Mth.lerp(back, frontRight.xRot, 1.0F);
			hindLeft.xRot = Mth.lerp(back, hindLeft.xRot, -0.4F + Mth.sin(t * 0.1F) * 0.15F);
			hindRight.xRot = Mth.lerp(back, hindRight.xRot, -0.4F - Mth.sin(t * 0.1F) * 0.15F);
			tail.xRot = Mth.lerp(back, tail.xRot, 0.1F);
			tail.yRot = Mth.lerp(back, tail.yRot, Mth.sin(t * 0.08F) * 0.2F);
		}

		// Knocking a shell on the pebble on its belly: forepaws up, then down hard, again and again. Or a fish laid across its
		// chest, held in both forepaws, the head bobbing down to nibble at it.
		float crack = state.crack;
		if (crack > 0) {
			if (state.held == OtterRules.SHELL) {
				float a = 0.35F + 0.85F * state.knock;
				frontLeft.xRot = Mth.lerp(crack, frontLeft.xRot, a);
				frontRight.xRot = Mth.lerp(crack, frontRight.xRot, a);
				float aa = frontLeft.xRot;
				shell.visible = true;
				shell.setPos(0, LEG_Y + 2 * Mth.cos(aa) + 0.5F, FRONT_Z + 2 * Mth.sin(aa));
				shell.xRot = aa - 0.3F;
				pebble.visible = back > 0.5F;
				pebble.setPos(0, LEG_Y + 0.6F, -1.5F);
				head.xRot += 0.25F * crack;
			} else {
				fish.visible = true;
				fish.setPos(0, LEG_Y + 1.2F, -2.5F);
				fish.yRot = Mth.HALF_PI;
				fish.zRot = Mth.sin(t * 0.25F) * 0.1F;
				frontLeft.xRot = Mth.lerp(crack, frontLeft.xRot, 0.8F);
				frontRight.xRot = Mth.lerp(crack, frontRight.xRot, 0.8F);
				head.xRot += (0.3F + Mth.abs(Mth.sin(t * 0.35F)) * 0.35F) * crack;
			}
		}

		// Holding paws: one forepaw reaches out to the side, toward the otter floating there. On its back the body's +x is its
		// right, so a partner on its right takes the +x paw.
		float hold = state.hold;
		if (hold > 0) {
			ModelPart paw = state.reachRight ? frontLeft : frontRight;
			float side = state.reachRight ? -1 : 1;
			paw.xRot = Mth.lerp(hold, paw.xRot, 0.15F);
			paw.zRot = side * 1.3F * hold + Mth.sin(t * 0.1F) * 0.05F * hold;
			head.yRot += -side * 0.3F * hold;
		}

		// Sitting up ashore to juggle a pebble: up on its haunches, the pebble tossed from paw to paw, its eyes on it.
		if (sit > 0) {
			body.xRot = Mth.lerp(sit, body.xRot, -1.1F);
			body.y = Mth.lerp(sit, body.y, BODY_Y - 2.5F);
			hindLeft.xRot = Mth.lerp(sit, hindLeft.xRot, 1.1F);
			hindRight.xRot = Mth.lerp(sit, hindRight.xRot, 1.1F);
			tail.xRot = Mth.lerp(sit, tail.xRot, 1.0F);
			if (state.otterPose == OtterRules.JUGGLE) {
				float age = state.ageInTicks;
				float side = OtterRules.tossSide(age);
				pebble.visible = true;
				pebble.setPos(side * 1.6F, LEG_Y + 2.5F, FRONT_Z - 0.5F - OtterRules.tossHeight(age));
				frontLeft.xRot = Mth.lerp(sit, frontLeft.xRot, 0.2F - Math.max(0, side) * 0.4F);
				frontRight.xRot = Mth.lerp(sit, frontRight.xRot, 0.2F + Math.min(0, side) * 0.4F);
				head.xRot = Mth.lerp(sit, head.xRot, 0.6F - OtterRules.tossHeight(age) * 0.08F);
				head.yRot = Mth.lerp(sit, head.yRot, -side * 0.25F);
			} else {
				head.xRot = Mth.lerp(sit, head.xRot, 1.1F);
			}
		}

		float scale = state.isBaby ? 1.35F : 1;
		head.xScale = head.yScale = head.zScale = scale;
	}
}
