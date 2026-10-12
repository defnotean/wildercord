package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.RedPandaRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A cinnabar panda: a stocky body hinged at the hips, a round head with a short white muzzle and big white-rimmed ears, short black
 * legs and a great thick ringed tail. It trots with a diagonal gait; rears up from the hips with its forepaws spread wide
 * ({@link WildlifeRenderState#alert}); sits back to wash its face, rubbing its paws over it ({@link WildlifeRenderState#rest}), or
 * to eat with its paws at its mouth ({@link WildlifeRenderState#graze}); naps lying down with its head on its paws and its tail
 * curled round over its face ({@link WildlifeRenderState#hide}); and tumbles, rolling right over and over
 * ({@link WildlifeRenderState#dance}). All four legs hang from the body so they roll with it. A cub has a bigger head. UVs match
 * tools/red_panda_art.py.
 */
public final class CinnabarPandaModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 19, BODY_Z = 4.5F, FRONT_Z = -7.5F, HIND_Z = -1.5F, TAIL_REST = -0.15F, LIE_Y = 21, HALF = 2.5F;

	private final ModelPart body, head, snout, leftEar, rightEar, tail, tip, frontLeft, frontRight, hindLeft, hindRight;

	public CinnabarPandaModel(ModelPart root) {
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
		// The body hinges at the hips (its back end, at the bottom), so it can rear up and sit back from them.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -5, -9, 6, 5, 9),
			PartPose.offset(0, BODY_Y, BODY_Z));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-3, -4, -4, 6, 5, 4),
			PartPose.offset(0, -3, -9));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(32, 10).addBox(-1.5F, -1, -2, 3, 2, 2),
			PartPose.offset(0, -0.5F, -4));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(44, 10).addBox(-0.5F, -1.3F, -2.3F, 1, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-1, -2, -0.5F, 2, 2, 1),
			PartPose.offsetAndRotation(2.3F, -4, -1.5F, 0, 0, 0.3F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1, -2, -0.5F, 2, 2, 1),
			PartPose.offsetAndRotation(-2.3F, -4, -1.5F, 0, 0, -0.3F));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -1.5F, 0, 3, 3, 6),
			PartPose.offsetAndRotation(0, -3.5F, 0, TAIL_REST, 0, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(20, 16).addBox(-1.5F, -1.5F, 0, 3, 3, 5), PartPose.offset(0, 0, 6));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(52, 4).addBox(-1, 0, -1, 2, 5, 2);
		CubeListBuilder mirrored = CubeListBuilder.create().texOffs(52, 4).mirror().addBox(-1, 0, -1, 2, 5, 2);
		body.addOrReplaceChild("front_left_leg", leg, PartPose.offset(2, 0, FRONT_Z));
		body.addOrReplaceChild("front_right_leg", mirrored, PartPose.offset(-2, 0, FRONT_Z));
		body.addOrReplaceChild("hind_left_leg", leg, PartPose.offset(2, 0, HIND_Z));
		body.addOrReplaceChild("hind_right_leg", mirrored, PartPose.offset(-2, 0, HIND_Z));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float walk = state.walkAnimationPos * 0.9F;

		// A trot: diagonal pairs together, the body rocking a little.
		float swing = Mth.cos(walk) * 0.9F * speed;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		body.y = BODY_Y - Mth.abs(Mth.sin(walk)) * 0.5F * speed;

		// Head: where it looks, the muzzle sniffing, the ears turning now and then.
		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		snout.xRot = Mth.sin(t * 1.9F) * 0.04F;
		float turn = Mth.clamp(Mth.sin(t * 0.06F) * 5 - 4, 0, 1);
		leftEar.yRot = -turn * 0.4F;
		rightEar.yRot = turn * 0.2F;
		// The tail: held out low behind, swaying lazily from side to side.
		tail.xRot = TAIL_REST + Mth.sin(walk) * 0.1F * speed;
		tail.yRot = Mth.sin(t * 0.07F) * 0.25F;
		tip.yRot = Mth.sin(t * 0.07F - 0.8F) * 0.2F;
		tip.xRot = -0.15F;

		// Sitting back on its haunches (to wash or to eat): the body tipped up from the hips, the hind legs folded under.
		float wash = state.rest, munch = state.graze;
		float sit = Math.max(wash, munch);
		if (sit > 0) {
			float up = 0.85F * sit;
			body.xRot = -up;
			head.xRot += up;
			hindLeft.xRot = Mth.lerp(sit, hindLeft.xRot, up - 1.3F);
			hindRight.xRot = Mth.lerp(sit, hindRight.xRot, up - 1.3F);
			body.y = Mth.lerp(sit, body.y, BODY_Y + 2.5F);
			tail.xRot = Mth.lerp(sit, tail.xRot, up - 0.25F);
		}
		// Washing: both forepaws up over its face, rubbing round and round, the head ducking into them.
		if (wash > 0) {
			float rub = Mth.sin(t * 0.7F);
			frontLeft.xRot = Mth.lerp(wash, frontLeft.xRot, -1.75F + rub * 0.25F);
			frontRight.xRot = Mth.lerp(wash, frontRight.xRot, -1.75F - rub * 0.25F);
			frontLeft.zRot = 0.2F * wash;
			frontRight.zRot = -0.2F * wash;
			head.xRot += (0.35F + rub * 0.1F) * wash;
			head.zRot = Mth.sin(t * 0.35F) * 0.2F * wash;
		} else {
			frontLeft.zRot = 0;
			frontRight.zRot = 0;
			head.zRot = 0;
		}
		// Eating: forepaws held up to its mouth, chewing.
		if (munch > 0) {
			frontLeft.xRot = Mth.lerp(munch, frontLeft.xRot, -1.45F);
			frontRight.xRot = Mth.lerp(munch, frontRight.xRot, -1.45F);
			head.xRot += (0.25F + Mth.sin(t * 1.4F) * 0.08F) * munch;
			snout.xRot += Mth.abs(Mth.sin(t * 1.4F)) * 0.12F * munch;
		}

		// Rearing up: tall on its hind legs from the hips, forepaws flung wide, head up and looking at what startled it.
		float stand = state.alert;
		if (stand > 0) {
			body.xRot = Mth.lerp(stand, body.xRot, -1.3F);
			body.y = Mth.lerp(stand, body.y, BODY_Y);
			float up = -body.xRot;
			head.xRot = state.xRot * Mth.DEG_TO_RAD + up;
			hindLeft.xRot = Mth.lerp(stand, hindLeft.xRot, up + 0.1F);
			hindRight.xRot = Mth.lerp(stand, hindRight.xRot, up + 0.1F);
			frontLeft.xRot = Mth.lerp(stand, frontLeft.xRot, -0.2F);
			frontRight.xRot = Mth.lerp(stand, frontRight.xRot, -0.2F);
			frontLeft.zRot = -1.1F * stand + Mth.sin(t * 0.5F) * 0.08F * stand;
			frontRight.zRot = 1.1F * stand - Mth.sin(t * 0.5F) * 0.08F * stand;
			tail.xRot = Mth.lerp(stand, tail.xRot, up - 0.2F);
		}

		// Napping: lying down with its legs tucked, its head on its paws turned aside, and its tail curled round over its face.
		float curl = state.hide;
		if (curl > 0) {
			body.y = Mth.lerp(curl, body.y, BODY_Y + 3.2F);
			body.xRot = Mth.lerp(curl, body.xRot, 0);
			frontLeft.xRot = Mth.lerp(curl, frontLeft.xRot, -1.5F);
			frontRight.xRot = Mth.lerp(curl, frontRight.xRot, -1.5F);
			hindLeft.xRot = Mth.lerp(curl, hindLeft.xRot, 1.5F);
			hindRight.xRot = Mth.lerp(curl, hindRight.xRot, 1.5F);
			head.xRot = Mth.lerp(curl, head.xRot, 0.35F);
			head.yRot = Mth.lerp(curl, head.yRot, -0.5F);
			head.y = -3 + 1.2F * curl;
			tail.xRot = Mth.lerp(curl, tail.xRot, -0.45F);
			tail.yRot = Mth.lerp(curl, tail.yRot, 2.3F);
			tip.yRot = Mth.lerp(curl, tip.yRot, 1.2F);
			tip.xRot = Mth.lerp(curl, tip.xRot, 0);
			// Breathing, slow.
			body.yScale = 1 + Mth.sin(t * 0.08F) * 0.025F * curl;
		} else {
			head.y = -3;
			body.yScale = 1;
		}

		// Tumbling: rolling right over, again and again, round the middle of its body, legs paddling in the air.
		float tumble = state.dance;
		if (tumble > 0) {
			// Rolled round the middle of its body (not its belly), so it stays where it is, lying low, as it goes over.
			float roll = RedPandaRules.roll(t) * tumble;
			body.zRot = roll;
			body.x = -HALF * Mth.sin(roll);
			body.y = Mth.lerp(tumble, body.y, LIE_Y + HALF * Mth.cos(roll));
			body.xRot = Mth.lerp(tumble, body.xRot, 0);
			float paddle = Mth.sin(t * 1.1F) * tumble;
			frontLeft.xRot = Mth.lerp(tumble, frontLeft.xRot, -1.2F) + paddle * 0.6F;
			frontRight.xRot = Mth.lerp(tumble, frontRight.xRot, -1.2F) - paddle * 0.6F;
			hindLeft.xRot = Mth.lerp(tumble, hindLeft.xRot, 1.2F) - paddle * 0.5F;
			hindRight.xRot = Mth.lerp(tumble, hindRight.xRot, 1.2F) + paddle * 0.5F;
			tail.yRot += Mth.sin(t * 0.6F) * 0.5F * tumble;
		} else {
			body.zRot = 0;
			body.x = 0;
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.3F;
		}
	}
}
