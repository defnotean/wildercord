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
 * A sunwatch meerkat: a slim body hinged at the hips, a small round head with a pointed muzzle and little ears low on its sides, thin
 * forelegs, longer hind legs and a long thin tail. It scurries with a quick diagonal gait; stands sentry tall on its hind legs,
 * leaning back on its tail with its forepaws held at its chest and its head turning this way and that, holding still to look
 * ({@link WildlifeRenderState#watch}); sinks straight down its hole, the same way up, so that peeking only its head shows above the
 * ground ({@link WildlifeRenderState#hide}); sits back with its legs out and its belly to the sun, face tipped up
 * ({@link WildlifeRenderState#rest}); digs with its nose down and its forepaws flying ({@link WildlifeRenderState#dig}); sits up to
 * eat with its paws at its mouth ({@link WildlifeRenderState#graze}); and sleeps lying low, legs tucked and its tail curled round
 * ({@link WildlifeRenderState#settle}). All four legs hang from the body so they tip with it. A pup has a bigger head. UVs match
 * tools/meerkat_art.py.
 */
public final class SunwatchMeerkatModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 20, BODY_Z = 4, TAIL_REST = 0.3F, UPRIGHT = 1.45F, SINK = 15;

	private final ModelPart body, head, snout, leftEar, rightEar, tail, tip, frontLeft, frontRight, hindLeft, hindRight;

	public SunwatchMeerkatModel(ModelPart root) {
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
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body hinges at the hips (its back end, at the bottom), so it can stand up and sit back from them.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -4, -8, 4, 4, 8),
			PartPose.offset(0, BODY_Y, BODY_Z));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-1.5F, -3, -3, 3, 3, 3),
			PartPose.offset(0, -2.5F, -7.5F));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(28, 8).addBox(-1, -1, -2, 2, 1, 2),
			PartPose.offset(0, -0.3F, -3));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, -1.2F, -2.3F, 1, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(40, 4).addBox(-0.5F, -1, -0.5F, 1, 1, 1),
			PartPose.offset(1.7F, -1.8F, -1.2F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(40, 4).mirror().addBox(-0.5F, -1, -0.5F, 1, 1, 1),
			PartPose.offset(-1.7F, -1.8F, -1.2F));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5F, -0.5F, 0, 1, 1, 6),
			PartPose.offsetAndRotation(0, -1, 0, TAIL_REST, 0, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 16).addBox(-0.5F, -0.5F, 0, 1, 1, 4), PartPose.offset(0, 0, 6));
		CubeListBuilder front = CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0, -0.5F, 1, 4, 1);
		CubeListBuilder frontMirrored = CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-0.5F, 0, -0.5F, 1, 4, 1);
		CubeListBuilder hind = CubeListBuilder.create().texOffs(48, 8).addBox(-1, 0, -1, 2, 4, 2);
		CubeListBuilder hindMirrored = CubeListBuilder.create().texOffs(48, 8).mirror().addBox(-1, 0, -1, 2, 4, 2);
		body.addOrReplaceChild("front_left_leg", front, PartPose.offset(1.1F, 0, -6.5F));
		body.addOrReplaceChild("front_right_leg", frontMirrored, PartPose.offset(-1.1F, 0, -6.5F));
		body.addOrReplaceChild("hind_left_leg", hind, PartPose.offset(1.1F, 0, -1.5F));
		body.addOrReplaceChild("hind_right_leg", hindMirrored, PartPose.offset(-1.1F, 0, -1.5F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float walk = state.walkAnimationPos * 1.2F;

		// A scurry: quick diagonal pairs, the body bobbing, the tail up.
		float swing = Mth.cos(walk) * 1.0F * speed;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		frontLeft.zRot = frontRight.zRot = hindLeft.zRot = hindRight.zRot = 0;
		body.xRot = 0;
		body.zRot = 0;
		body.y = BODY_Y - Mth.abs(Mth.sin(walk)) * 0.6F * speed;
		body.yScale = 1;

		// Head: where it looks, the muzzle twitching, now and then a quick glance aside.
		float glance = Mth.clamp(Mth.sin(t * 0.09F) * 6 - 5, 0, 1) - Mth.clamp(Mth.sin(t * 0.07F + 2) * 6 - 5, 0, 1);
		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD + glance * 0.6F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.zRot = 0;
		head.y = -2.5F;
		snout.xRot = Mth.sin(t * 2.3F) * 0.04F;
		leftEar.zRot = 0;
		rightEar.zRot = 0;
		tail.xRot = TAIL_REST + Mth.sin(walk) * 0.12F * speed;
		tail.yRot = Mth.sin(t * 0.09F) * 0.15F;
		tip.xRot = -0.25F;
		tip.yRot = Mth.sin(t * 0.09F - 0.7F) * 0.15F;

		// Sitting back on its haunches, to sun itself or to eat: the body tipped up from the hips.
		float sun = state.rest, munch = state.graze;
		float sit = Math.max(sun, munch);
		if (sit > 0) {
			float up = (sun >= munch ? 1.05F : 1.2F) * sit;
			body.xRot = -up;
			body.y = Mth.lerp(sit, body.y, BODY_Y + 2);
			head.xRot += up;
			tail.xRot = Mth.lerp(sit, tail.xRot, up - 0.3F);
			tail.yRot = Mth.lerp(sit, tail.yRot, 0);
			tip.xRot = Mth.lerp(sit, tip.xRot, 0);
		}
		// Sunning: legs out in front, forepaws hanging loose, belly to the sun and face tipped up to it, swaying a little.
		if (sun > 0) {
			hindLeft.xRot = Mth.lerp(sun, hindLeft.xRot, 1.05F - 1.4F);
			hindRight.xRot = Mth.lerp(sun, hindRight.xRot, 1.05F - 1.4F);
			hindLeft.zRot = -0.25F * sun;
			hindRight.zRot = 0.25F * sun;
			frontLeft.xRot = Mth.lerp(sun, frontLeft.xRot, 1.15F);
			frontRight.xRot = Mth.lerp(sun, frontRight.xRot, 1.15F);
			frontLeft.zRot = -0.2F * sun;
			frontRight.zRot = 0.2F * sun;
			head.xRot -= 0.45F * sun;
			head.yRot = Mth.lerp(sun, head.yRot, 0);
			body.zRot = Mth.sin(t * 0.05F) * 0.04F * sun;
		}
		// Eating: forepaws up to its mouth, chewing fast.
		if (munch > 0) {
			hindLeft.xRot = Mth.lerp(munch, hindLeft.xRot, 1.2F - 1.3F);
			hindRight.xRot = Mth.lerp(munch, hindRight.xRot, 1.2F - 1.3F);
			frontLeft.xRot = Mth.lerp(munch, frontLeft.xRot, -1.0F);
			frontRight.xRot = Mth.lerp(munch, frontRight.xRot, -1.0F);
			head.xRot += (0.3F + Mth.sin(t * 1.8F) * 0.07F) * munch;
			snout.xRot += Mth.abs(Mth.sin(t * 1.8F)) * 0.12F * munch;
		}

		// Digging: nose to the ground, forepaws flying one after the other, hind legs braced, tail up for balance.
		float dig = state.dig;
		if (dig > 0) {
			body.xRot = Mth.lerp(dig, body.xRot, 0.3F);
			head.xRot += 0.45F * dig;
			float scrape = Mth.sin(t * 2.2F);
			frontLeft.xRot = Mth.lerp(dig, frontLeft.xRot, -0.8F + scrape * 0.8F);
			frontRight.xRot = Mth.lerp(dig, frontRight.xRot, -0.8F - scrape * 0.8F);
			hindLeft.xRot = Mth.lerp(dig, hindLeft.xRot, -0.45F);
			hindRight.xRot = Mth.lerp(dig, hindRight.xRot, -0.45F);
			tail.xRot = Mth.lerp(dig, tail.xRot, 0.7F);
			body.zRot += Mth.sin(t * 1.1F) * 0.05F * dig;
		}

		// Standing tall: on watch, or down its hole (sunk straight down that way up, so peeking only its head shows). Up on its hind
		// legs from the hips, leaning back on its tail, forepaws held at its chest.
		float watch = state.watch, down = state.hide;
		float upright = Math.max(watch, Math.min(1, down * 4));
		if (upright > 0) {
			float up = UPRIGHT * upright;
			body.xRot = Mth.lerp(upright, body.xRot, -UPRIGHT);
			body.y = Mth.lerp(upright, body.y, BODY_Y + 1.5F);
			head.xRot = Mth.lerp(upright, head.xRot, state.xRot * Mth.DEG_TO_RAD + UPRIGHT);
			hindLeft.xRot = Mth.lerp(upright, hindLeft.xRot, UPRIGHT - 0.2F);
			hindRight.xRot = Mth.lerp(upright, hindRight.xRot, UPRIGHT - 0.2F);
			frontLeft.xRot = Mth.lerp(upright, frontLeft.xRot, UPRIGHT - 0.75F);
			frontRight.xRot = Mth.lerp(upright, frontRight.xRot, UPRIGHT - 0.75F);
			frontLeft.zRot = Mth.lerp(upright, frontLeft.zRot, 0.15F);
			frontRight.zRot = Mth.lerp(upright, frontRight.zRot, -0.15F);
			tail.xRot = Mth.lerp(upright, tail.xRot, up - 0.36F);
			tail.yRot = Mth.lerp(upright, tail.yRot, 0);
			tip.xRot = Mth.lerp(upright, tip.xRot, 0.05F);
		}
		// On watch the head sweeps round, holds still a moment to look, then sweeps on; the ears flick at what it hears.
		if (watch > 0) {
			float scan = Mth.clamp(Mth.sin(t * 0.07F) * 1.8F, -1, 1);
			head.yRot = Mth.lerp(watch, head.yRot, scan * 1.0F);
			head.xRot += Mth.sin(t * 0.21F) * 0.05F * watch;
			float flick = Mth.clamp(Mth.sin(t * 0.13F) * 8 - 7, 0, 1);
			leftEar.zRot = flick * 0.4F * watch;
			rightEar.zRot = -flick * 0.4F * watch;
		}
		if (down > 0) {
			body.y += SINK * down;
			head.yRot = Mth.lerp(down, head.yRot, Mth.clamp(Mth.sin(t * 0.11F) * 2, -1, 1) * 0.7F);
		}

		// Asleep: lying low with its legs tucked, head down on its paws turned aside, tail curled round, breathing slow.
		float sleep = state.settle;
		if (sleep > 0) {
			body.y = Mth.lerp(sleep, body.y, BODY_Y + 3.2F);
			body.xRot = Mth.lerp(sleep, body.xRot, 0);
			body.zRot = Mth.lerp(sleep, body.zRot, 0.2F);
			frontLeft.xRot = Mth.lerp(sleep, frontLeft.xRot, -1.5F);
			frontRight.xRot = Mth.lerp(sleep, frontRight.xRot, -1.5F);
			hindLeft.xRot = Mth.lerp(sleep, hindLeft.xRot, 1.5F);
			hindRight.xRot = Mth.lerp(sleep, hindRight.xRot, 1.5F);
			head.xRot = Mth.lerp(sleep, head.xRot, 0.35F);
			head.yRot = Mth.lerp(sleep, head.yRot, -0.6F);
			head.y = -2.5F + 1.3F * sleep;
			tail.xRot = Mth.lerp(sleep, tail.xRot, -0.1F);
			tail.yRot = Mth.lerp(sleep, tail.yRot, 1.9F);
			tip.yRot = Mth.lerp(sleep, tip.yRot, 1.1F);
			tip.xRot = Mth.lerp(sleep, tip.xRot, 0);
			body.yScale = 1 + Mth.sin(t * 0.09F) * 0.03F * sleep;
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.3F;
		}
	}
}
