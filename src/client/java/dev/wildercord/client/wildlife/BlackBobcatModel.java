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
 * A black bobcat, built soft and round: a plush body with a smoky bib, a big head with wide golden eyes, fluffy cheeks and a
 * small rosy-nosed muzzle, rosy-cupped ears with short black tufts, stubby legs on big toe-beaned paws and a fluffy bob of a
 * tail. Drawn larger by its renderer (to a polar bear's size). The head and tail hang from the body, which pivots at its rump,
 * so when it sits up the head rides along on the shoulders and is turned back level. It pads with its legs in diagonal pairs,
 * flicks its bob and swivels its ears when idle. A kitten has a bigger head. UVs match tools/bobcat_art.py.
 * <p>Its kin share the build: the Frost Lynx as it is, the Dune Cougar with {@link #createLongTailLayer} in place of the bob,
 * a long rope of a tail in two lengths that hangs low and curls up at its tip (UVs in tools/predator_art.py).
 */
public final class BlackBobcatModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 18, LEG_Y = 18, FRONT_Z = -3.5F, HIND_Z = 4, LEG_X = 3.3F;
	/** How far the body tips up about its rump when it sits. */
	private static final float SIT_TILT = -0.85F;
	/** A long tail hangs down and back from the rump, and its far length curls up. */
	private static final float LONG_TAIL_HANG = -1.0F, LONG_TAIL_CURL = 1.1F;

	private final ModelPart body, head, leftEar, rightEar, tail, frontLeft, frontRight, hindLeft, hindRight;
	/** The far length of a long tail, or null for a bob. */
	private final ModelPart tailTip;

	public BlackBobcatModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		tail = body.getChild("tail");
		tailTip = tail.hasChild("tip") ? tail.getChild("tip") : null;
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
	}

	public static LayerDefinition createLayer() {
		return createLayer(false);
	}

	/** The cougar's build: the bob swapped for a long tail. */
	public static LayerDefinition createLongTailLayer() {
		return createLayer(true);
	}

	private static LayerDefinition createLayer(boolean longTail) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body pivots at the rump, low and behind, so sitting tips it up about the haunches.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-6, -8.5F, -12, 12, 9, 13)
			.texOffs(42, 28).addBox(-4.5F, -7, -13.5F, 9, 7, 2), PartPose.offset(0, BODY_Y, HIND_Z + 2));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 23).addBox(-6, -7, -7.5F, 12, 10, 9)
			.texOffs(0, 42).addBox(-7.5F, 0, -6, 15, 4, 4)
			.texOffs(42, 23).addBox(-2, 0, -9, 4, 3, 2), PartPose.offset(0, -7.5F, -12));
		ear(head, "left_ear", 3.4F, 0.3F, false);
		ear(head, "right_ear", -3.4F, -0.3F, true);
		if (longTail) {
			PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 50).addBox(-1.5F, -1.5F, 0, 3, 3, 5),
				PartPose.offsetAndRotation(0, -6.5F, 0.5F, LONG_TAIL_HANG, 0, 0));
			tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(38, 42).addBox(-1.5F, -1.5F, 0, 3, 3, 5),
				PartPose.offsetAndRotation(0, 0, 4.5F, LONG_TAIL_CURL, 0, 0));
		} else {
			body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 50).addBox(-2, -2, 0, 4, 4, 4),
				PartPose.offsetAndRotation(0, -7, 0.5F, 0.6F, 0, 0));
		}
		leg(root, "front_left", LEG_X, FRONT_Z, false);
		leg(root, "front_right", -LEG_X, FRONT_Z, true);
		leg(root, "hind_left", LEG_X, HIND_Z, false);
		leg(root, "hind_right", -LEG_X, HIND_Z, true);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void ear(PartDefinition head, String name, float x, float lean, boolean mirror) {
		PartDefinition ear = head.addOrReplaceChild(name, cubes(50, 0, mirror).addBox(-2, -3, -1, 4, 3, 2),
			PartPose.offsetAndRotation(x, -6.5F, -3.5F, -0.15F, 0, lean));
		PartDefinition tip = ear.addOrReplaceChild("tip", cubes(50, 5, mirror).addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(0, -3, 0));
		tip.addOrReplaceChild("tuft", cubes(56, 5, mirror).addBox(-0.5F, -1, -0.5F, 1, 1, 1), PartPose.offset(0, -2, 0));
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		PartDefinition leg = root.addOrReplaceChild(name + "_leg", cubes(0, 50, mirror).addBox(-2.5F, 0, -2.5F, 5, 5, 5), PartPose.offset(x, LEG_Y, z));
		leg.addOrReplaceChild("paw", cubes(20, 50, mirror).addBox(-3, 4, -3.5F, 6, 2, 6), PartPose.ZERO);
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
		float walk = state.walkAnimationPos * 0.6662F;
		float sit = state.sit;
		float stand = 1 - sit;

		float swing = Mth.cos(walk) * 0.8F * speed * stand;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		// A soft, bouncy pad: the body bobs and rolls a little over each stride.
		body.y = BODY_Y - Mth.abs(Mth.cos(walk)) * 0.5F * speed;
		body.zRot = Mth.sin(walk) * 0.04F * speed;

		head.yRot = Mth.clamp(state.yRot, -45, 45) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD + speed * 0.08F;
		// Now and then it tips its head, curious.
		head.zRot = Mth.clamp(Mth.sin(t * 0.031F) * 5 - 4, 0, 1) * 0.18F * stand;
		float leftSwivel = Mth.clamp(Mth.sin(t * 0.07F) * 6 - 5, 0, 1);
		float rightSwivel = Mth.clamp(Mth.sin(t * 0.083F + 1.7F) * 6 - 5, 0, 1);
		leftEar.yRot = -leftSwivel * 0.5F;
		rightEar.yRot = rightSwivel * 0.5F;
		leftEar.xRot = rightEar.xRot = -0.15F - speed * 0.2F;

		// The bob flicks now and then, and lifts at a trot.
		float flick = Mth.clamp(Mth.sin(t * 0.05F) * 4 - 3, 0, 1);
		tail.xRot = 0.6F + speed * 0.3F + flick * Mth.sin(t * 0.9F) * 0.25F;
		tail.yRot = Mth.sin(t * 0.06F) * 0.15F;
		if (tailTip != null) {
			// A long tail sways slowly, lifts level at a run, and its tip twitches on its own.
			tail.xRot = LONG_TAIL_HANG + speed * 0.55F + flick * 0.1F;
			tail.yRot = Mth.sin(t * 0.045F) * 0.25F;
			tailTip.xRot = LONG_TAIL_CURL - speed * 0.3F + Mth.sin(t * 0.11F) * 0.12F;
			tailTip.yRot = Mth.sin(t * 0.045F - 0.9F) * 0.3F;
		}

		if (sit > 0) {
			// Up on its haunches: the body tips up about its rump and the head, riding on the shoulders, is turned back level.
			// Forelegs stand straight under the chest, hind legs fold flat beside the rump, and the bob rests on the ground.
			body.xRot = SIT_TILT * sit;
			body.y = Mth.lerp(sit, body.y, 21.5F);
			head.xRot -= SIT_TILT * sit;
			tail.xRot = Mth.lerp(sit, tail.xRot, 1.9F);
			if (tailTip != null) {
				// Sitting, a long tail drops steeply to the ground behind and its tip lies back along it.
				tail.xRot = Mth.lerp(sit, LONG_TAIL_HANG, -0.45F);
				tailTip.xRot = Mth.lerp(sit, tailTip.xRot, 1.2F);
			}
			for (ModelPart leg : new ModelPart[] {frontLeft, frontRight}) {
				leg.xRot = Mth.lerp(sit, leg.xRot, 0);
				leg.y = Mth.lerp(sit, LEG_Y, 15.2F);
				leg.z = Mth.lerp(sit, FRONT_Z, -3);
				leg.yScale = Mth.lerp(sit, 1, 8.8F / 6);
			}
			for (ModelPart leg : new ModelPart[] {hindLeft, hindRight}) {
				leg.xRot = Mth.lerp(sit, leg.xRot, -1.5F);
				leg.y = Mth.lerp(sit, LEG_Y, 21.2F);
				leg.z = Mth.lerp(sit, HIND_Z, 3);
			}
			hindLeft.x = Mth.lerp(sit, LEG_X, 4.6F);
			hindRight.x = Mth.lerp(sit, -LEG_X, -4.6F);
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		}
	}
}
