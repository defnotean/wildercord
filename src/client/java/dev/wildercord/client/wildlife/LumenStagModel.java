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
 * A lumen stag: a slender deer on long jointed legs (thick thighs behind, a knee in every leg), a deep chest with a
 * pale ruff at the throat, a long neck carrying a narrow head and big ears, and branching crystal antlers, each a beam
 * with a brow tine, a forward tine, a fork and a swept-back tip. It walks with a bend at every knee, bounds when it
 * runs (fore and hind pairs together, its body rocking), grazes with its neck down to the grass, watches with its
 * head high and ears forward, and bows when it sheds. The UVs match {@code stag_skin} in tools/wildlife_art.py.
 */
public final class LumenStagModel extends EntityModel<WildlifeRenderState> {
	private final ModelPart body, neck, head, leftEar, rightEar, tail;
	private final ModelPart leftAntler;
	private final ModelPart[] leftTines;
	private final ModelPart frontLeft, frontRight, hindLeft, hindRight;
	private final ModelPart frontLeftShin, frontRightShin, hindLeftShin, hindRightShin;

	private static final float NECK_REST = 0.45F;

	public LumenStagModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		neck = body.getChild("neck");
		head = neck.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		leftAntler = head.getChild("left_antler");
		leftTines = new ModelPart[] {leftAntler.getChild("left_brow"), leftAntler.getChild("left_tine"), leftAntler.getChild("left_fork"),
			leftAntler.getChild("left_tip")};
		tail = body.getChild("tail");
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
		frontLeftShin = frontLeft.getChild("front_left_shin");
		frontRightShin = frontRight.getChild("front_right_shin");
		hindLeftShin = hindLeft.getChild("hind_left_shin");
		hindRightShin = hindRight.getChild("hind_right_shin");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-4, -4, -9, 8, 8, 17)
			.texOffs(0, 25).addBox(-3.5F, 1, -10, 7, 5, 4), PartPose.offset(0, 10, 1));
		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
			.texOffs(50, 0).addBox(-2, -8, -2.5F, 4, 9, 5)
			.texOffs(68, 0).addBox(-2.5F, -5, -3.4F, 5, 6, 2), PartPose.offsetAndRotation(0, -3, -8, NECK_REST, 0, 0));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(82, 0).addBox(-2.5F, -3, -5, 5, 5, 6)
			.texOffs(104, 0).addBox(-1.5F, -1.5F, -9, 3, 3, 4), PartPose.offsetAndRotation(0, -8, -0.5F, -NECK_REST, 0, 0));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(104, 7).addBox(0, -1, -0.5F, 3, 2, 1),
			PartPose.offsetAndRotation(2.2F, -2.5F, -1.2F, 0.2F, 0, -0.55F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(104, 7).mirror().addBox(-3, -1, -0.5F, 3, 2, 1),
			PartPose.offsetAndRotation(-2.2F, -2.5F, -1.2F, 0.2F, 0, 0.55F));
		antler(head, "left", 1);
		antler(head, "right", -1);
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 36).addBox(-1.5F, 0, -0.5F, 3, 4, 2),
			PartPose.offsetAndRotation(0, -3, 8.5F, 0.5F, 0, 0));
		leg(root, "front_left", 2.3F, 13, -6.5F, false, false);
		leg(root, "front_right", -2.3F, 13, -6.5F, false, true);
		leg(root, "hind_left", 2.4F, 11, 6.5F, true, false);
		leg(root, "hind_right", -2.4F, 11, 6.5F, true, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** One antler: a beam leaning up, back and out, with its brow tine, forward tine, fork and swept-back tip. */
	private static void antler(PartDefinition head, String side, int out) {
		boolean mirror = out < 0;
		PartDefinition beam = head.addOrReplaceChild(side + "_antler", cube(112, 7, 1, 8, mirror),
			PartPose.offsetAndRotation(out * 1.5F, -3, -2, -0.3F, 0, -out * 0.38F));
		beam.addOrReplaceChild(side + "_brow", cube(116, 12, 1, 3, mirror), PartPose.offsetAndRotation(0, -1.5F, 0, -1.25F, 0, out * 0.1F));
		beam.addOrReplaceChild(side + "_tine", cube(116, 7, 1, 4, mirror), PartPose.offsetAndRotation(0, -4, 0, -0.9F, 0, out * 0.15F));
		beam.addOrReplaceChild(side + "_fork", cube(120, 7, 1, 4, mirror), PartPose.offsetAndRotation(0, -6, 0, 0.1F, 0, -out * 0.85F));
		beam.addOrReplaceChild(side + "_tip", cube(124, 7, 1, 3, mirror), PartPose.offsetAndRotation(0, -8, 0, 0.45F, 0, out * 0.12F));
	}

	/** A length of antler (a column one pixel thick, rising from its pivot). */
	private static CubeListBuilder cube(int u, int v, int size, int length, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
		if (mirror) {
			cube.mirror();
		}
		return cube.addBox(-0.5F, -length, -0.5F, size, length, size);
	}

	/** A jointed leg: a thigh (thick behind) and a shin with its hoof, hinged at the knee. */
	private static void leg(PartDefinition root, String name, float x, float y, float z, boolean hind, boolean mirror) {
		CubeListBuilder upper = CubeListBuilder.create().texOffs(hind ? 20 : 0, 36);
		CubeListBuilder shin = CubeListBuilder.create().texOffs(hind ? 36 : 12, 36);
		if (mirror) {
			upper.mirror();
			shin.mirror();
		}
		PartDefinition leg = root.addOrReplaceChild(name + "_leg", hind ? upper.addBox(-2, 0, -2, 4, 6, 4) : upper.addBox(-1.5F, 0, -1.5F, 3, 5, 3),
			PartPose.offset(x, y, z));
		leg.addOrReplaceChild(name + "_shin", shin.addBox(-1, 0, -1, 2, hind ? 7 : 6, 2), PartPose.offset(0, hind ? 6 : 5, hind ? 0.5F : 0));
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float pos = state.walkAnimationPos;
		float speed = Math.min(1, state.walkAnimationSpeed);
		float run = Mth.clamp((state.walkAnimationSpeed - 0.55F) * 2.5F, 0, 1);

		// Walking: diagonal pairs; bounding: the fore pair together, then the hind pair. Each knee folds while its leg
		// swings forward, off the ground.
		float walk = pos * 0.6662F;
		float bound = pos * 0.5F;
		legPose(frontLeft, frontLeftShin, walk, bound, speed, run, false);
		legPose(frontRight, frontRightShin, walk + Mth.PI, bound + 0.25F, speed, run, false);
		legPose(hindLeft, hindLeftShin, walk + Mth.PI, bound + 1.9F, speed, run, true);
		legPose(hindRight, hindRightShin, walk, bound + 2.15F, speed, run, true);
		body.xRot = run * Mth.sin(bound + 0.6F) * 0.14F;
		body.y = 10 - run * Mth.abs(Mth.sin(bound)) * 1.2F;

		// The neck: carried proud, lowered to graze, raised higher to watch, bowed to shed.
		float breathe = Mth.sin(t * 0.08F) * 0.015F;
		neck.xRot = NECK_REST + breathe + state.graze * 1.55F - state.watch * 0.28F + state.bow * 0.95F + run * 0.35F;
		head.xRot = -NECK_REST - state.graze * 0.55F + state.watch * 0.18F - state.bow * 0.35F - run * 0.2F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
		float look = Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD;
		neck.yRot = look * 0.45F * (1 - state.graze);
		head.yRot = look * 0.5F * (1 - state.graze);
		// Grazing, it chews; bowing, it gives its head a slow shake as the antler comes loose.
		head.zRot = state.graze * Mth.sin(t * 0.9F) * 0.05F + state.bow * Mth.sin(t * 0.55F) * 0.16F;

		// Ears: flicked now and then, pricked forward when it watches, laid back when it runs.
		float flick = Mth.clamp(Mth.sin(t * 0.07F) * 6 - 5, 0, 1);
		leftEar.xRot = 0.2F - state.watch * 0.55F + run * 0.6F + flick * 0.4F;
		rightEar.xRot = 0.2F - state.watch * 0.55F + run * 0.6F;
		leftEar.zRot = -0.55F + state.watch * 0.25F - run * 0.2F;
		rightEar.zRot = 0.55F - state.watch * 0.25F + run * 0.2F;

		// The tail flicks up, showing its white, when it runs or is wary.
		tail.xRot = 0.5F + Mth.sin(t * 0.21F) * 0.06F + (run + state.watch * 0.5F) * 0.9F;

		// Today's shed antler is gone down to a nub, growing back by tomorrow.
		leftAntler.yScale = state.shed ? 0.3F : 1;
		for (ModelPart tine : leftTines) {
			tine.visible = !state.shed;
		}
	}

	/** Poses one leg from its phase in the walk and in the bound, blended by how fast it's running. */
	private static void legPose(ModelPart leg, ModelPart shin, float walkPhase, float boundPhase, float speed, float run, boolean hind) {
		float walkSwing = Mth.cos(walkPhase) * 0.7F * speed;
		float walkFold = Math.max(0, Mth.sin(walkPhase)) * speed;
		float boundSwing = Mth.cos(boundPhase) * (hind ? 1.05F : 0.95F);
		float boundFold = Math.max(0, Mth.sin(boundPhase)) * 1.4F;
		leg.xRot = Mth.lerp(run, walkSwing, boundSwing);
		float fold = Mth.lerp(run, walkFold, boundFold);
		// A foreleg folds its hoof back; a hind leg, hinged the other way at the hock, tucks it forward.
		shin.xRot = hind ? -fold * 0.8F : fold * 1.1F;
	}
}
