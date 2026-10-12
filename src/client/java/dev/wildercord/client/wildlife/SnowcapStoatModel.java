package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.StoatRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A snowcap stoat: a long low body hinged at the hips, a small wedge of a head with round ears and a black nose, short legs and a
 * long tail ending in a black tip. It bounds along with its back arching, stands up tall from the hips with its forepaws held to
 * its chest ({@link WildlifeRenderState#alert}), dances its war dance in twisting hops with its tail whipping
 * ({@link WildlifeRenderState#dance}), and goes down headfirst into the snow ({@link WildlifeRenderState#dive}). A kit has a
 * bigger head. UVs match tools/stoat_art.py.
 */
public final class SnowcapStoatModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 21, BODY_Z = 3.5F, LEG_Y = 21, HIND_Z = 2, FRONT_Z = -8.5F, TAIL_REST = -0.45F;

	private final ModelPart body, head, snout, leftEar, rightEar, tail, tip, frontLeft, frontRight, hindLeft, hindRight;

	public SnowcapStoatModel(ModelPart root) {
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
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body hinges at the hips, so it can stand up from them.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -4, -10, 4, 4, 10),
			PartPose.offset(0, BODY_Y, BODY_Z));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0).addBox(-2, -3, -4, 4, 3, 4),
			PartPose.offset(0, -2, -10));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(48, 0).addBox(-1, -1, -2, 2, 2, 2),
			PartPose.offset(0, -1, -4));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, -1.2F, -2.4F, 1, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(48, 6).addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(1.5F, -3, -1.5F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(48, 6).mirror().addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(-1.5F, -3, -1.5F));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 16).addBox(-1, -1, 0, 2, 2, 6),
			PartPose.offsetAndRotation(0, -3, 0, TAIL_REST, 0, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(18, 16).addBox(-1, -1, 0, 2, 2, 3), PartPose.offset(0, 0, 6));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(30, 10).addBox(-1, 0, -1, 2, 3, 2);
		CubeListBuilder mirrored = CubeListBuilder.create().texOffs(30, 10).mirror().addBox(-1, 0, -1, 2, 3, 2);
		body.addOrReplaceChild("front_left_leg", leg, PartPose.offset(1, 0, FRONT_Z));
		body.addOrReplaceChild("front_right_leg", mirrored, PartPose.offset(-1, 0, FRONT_Z));
		root.addOrReplaceChild("hind_left_leg", leg, PartPose.offset(1, LEG_Y, HIND_Z));
		root.addOrReplaceChild("hind_right_leg", mirrored, PartPose.offset(-1, LEG_Y, HIND_Z));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float walk = state.walkAnimationPos * 1.1F;

		// A bounding gait: forepaws together, then hind paws together, the back arching and flattening between them.
		float bound = Mth.cos(walk) * speed;
		frontLeft.xRot = bound * 1.0F;
		frontRight.xRot = bound * 0.9F;
		hindLeft.xRot = -bound * 1.0F;
		hindRight.xRot = -bound * 0.9F;
		body.xRot = Mth.sin(walk) * 0.12F * speed;
		body.y = BODY_Y - Mth.abs(Mth.sin(walk)) * 1.2F * speed;

		// Head: where it looks, a quick nose-twitch, ears flicking now and then.
		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - body.xRot;
		snout.xRot = Mth.sin(t * 2.2F) * 0.05F;
		float flick = Mth.clamp(Mth.sin(t * 0.07F) * 5 - 4, 0, 1);
		leftEar.zRot = flick * 0.3F;
		rightEar.zRot = -flick * 0.3F;
		// Tail: held up a little, swaying, its tip curling.
		tail.xRot = TAIL_REST + Mth.sin(walk) * 0.25F * speed;
		tail.yRot = Mth.sin(t * 0.09F) * 0.2F * (1 - speed);
		tip.xRot = 0.2F + Mth.sin(t * 0.12F) * 0.12F;

		// Standing up tall from the hips: forepaws held to its chest, head level and looking about, tail flat behind it.
		float stand = state.alert;
		if (stand > 0) {
			float up = 1.35F * stand;
			body.xRot -= up;
			body.y = Mth.lerp(stand, body.y, BODY_Y - 0.5F);
			head.xRot += up;
			head.yRot += Mth.sin(t * 0.08F) * 0.5F * stand;
			frontLeft.xRot = Mth.lerp(stand, frontLeft.xRot, up * 0.75F);
			frontRight.xRot = Mth.lerp(stand, frontRight.xRot, up * 0.75F);
			tail.xRot = Mth.lerp(stand, tail.xRot, up * 0.95F);
			tail.yRot = Mth.lerp(stand, tail.yRot, Mth.sin(t * 0.15F) * 0.3F);
			hindLeft.xRot = Mth.lerp(stand, hindLeft.xRot, -0.15F);
			hindRight.xRot = Mth.lerp(stand, hindRight.xRot, -0.15F);
		}

		// The war dance: hop after twisting hop, the body corkscrewing in the air, legs flung out and the tail lashing.
		float dance = state.dance;
		if (dance > 0) {
			float lift = StoatRules.hop(state.ageInTicks) * 16 * dance;
			body.y -= lift;
			hindLeft.y = LEG_Y - lift;
			hindRight.y = LEG_Y - lift;
			body.yRot = Mth.sin(t * 0.63F) * 0.9F * dance;
			body.zRot = Mth.sin(t * 0.95F) * 0.45F * dance;
			body.xRot -= (0.25F + Mth.sin(t * 1.3F) * 0.15F) * dance;
			head.xRot += 0.2F * dance;
			tail.yRot = Mth.sin(t * 0.8F) * 0.9F * dance;
			tail.xRot -= 0.3F * dance;
			tip.yRot = Mth.sin(t * 0.8F - 1) * 0.6F * dance;
			float fling = Mth.sin(t * 1.26F) * dance;
			frontLeft.xRot += fling * 0.8F;
			frontRight.xRot -= fling * 0.8F;
			hindLeft.xRot -= fling * 0.6F;
			hindRight.xRot += fling * 0.6F;
		} else {
			body.yRot = 0;
			body.zRot = 0;
			hindLeft.y = LEG_Y;
			hindRight.y = LEG_Y;
			tip.yRot = 0;
		}

		// Into the snow headfirst: nose down, and down it goes, the tail last.
		float dive = state.dive;
		if (dive > 0) {
			body.xRot += 0.5F * dive;
			body.y += 8 * dive;
			hindLeft.y += 8 * dive;
			hindRight.y += 8 * dive;
			tail.xRot -= 0.4F * dive;
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		}
	}
}
