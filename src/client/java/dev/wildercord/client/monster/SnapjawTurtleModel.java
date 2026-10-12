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
 * The Snapjaw Turtle: a broad high shell with three ridges of spikes along it, a flat plastron under it, a great blunt
 * head with a hooked upper beak and a lower jaw that drops open, a little pink worm of a lure on its tongue, four thick
 * flipper-clawed legs and a long ridged tail.
 *
 * <p>Lurking ({@code veil}) it lies low with its jaws wide and its lure wriggling. Before a snap ({@code windup}) its head
 * draws back; the snap or a bite ({@code acting}) shoots it out with its jaws slammed shut. Holding on ({@code alt}) its head
 * is out and shaking. A missed snap ({@code stunned}) leaves its jaws shut and its head hanging. Pulled in ({@code guard})
 * its head, legs and tail go into its shell.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code tools/snapjaw_art.py}):</p>
 * <pre>
 *   shell (0,0) 18x6x20       head (76,0) 7x6x7        beak (104,0) 5x2x4
 *   jaw (76,13) 5x2x5         lure (96,13) 1x1x3
 *   plastron (0,26) 14x2x16   leg (60,26) 5x4x5        tail (80,26) 3x3x8
 *   ridge (0,44) 2x2x18
 * </pre>
 */
public class SnapjawTurtleModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 19.0F;
	private static final float HEAD_Z = -9.5F;
	private static final float LEG_Y = 20.0F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart lure;
	private final ModelPart tail;
	private final ModelPart rightFront;
	private final ModelPart leftFront;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public SnapjawTurtleModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.jaw = head.getChild("jaw");
		this.lure = jaw.getChild("lure");
		this.tail = body.getChild("tail");
		this.rightFront = root.getChild("right_front_leg");
		this.leftFront = root.getChild("left_front_leg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-9.0F, -6.0F, -10.0F, 18.0F, 6.0F, 20.0F)
				.texOffs(0, 26).addBox(-7.0F, 0.0F, -8.0F, 14.0F, 2.0F, 16.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));
		body.addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, -2.0F, -9.0F, 2.0F, 2.0F, 18.0F),
			PartPose.offset(0.0F, -6.0F, 0.0F));
		body.addOrReplaceChild("right_ridge", CubeListBuilder.create().texOffs(0, 44).addBox(-1.0F, -2.0F, -9.0F, 2.0F, 2.0F, 18.0F),
			PartPose.offsetAndRotation(-5.5F, -5.5F, 0.0F, 0.0F, 0.0F, -0.25F));
		body.addOrReplaceChild("left_ridge", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-1.0F, -2.0F, -9.0F, 2.0F, 2.0F, 18.0F),
			PartPose.offsetAndRotation(5.5F, -5.5F, 0.0F, 0.0F, 0.0F, 0.25F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(76, 0).addBox(-3.5F, -4.0F, -7.0F, 7.0F, 6.0F, 7.0F)
				.texOffs(104, 0).addBox(-2.5F, -1.0F, -10.0F, 5.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, -1.0F, HEAD_Z));
		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(76, 13).addBox(-2.5F, 0.0F, -5.0F, 5.0F, 2.0F, 5.0F),
			PartPose.offset(0.0F, 1.0F, -4.5F));
		jaw.addOrReplaceChild("lure", CubeListBuilder.create().texOffs(96, 13).addBox(-0.5F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
			PartPose.offset(0.0F, 0.2F, -1.0F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(80, 26).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 8.0F),
			PartPose.offsetAndRotation(0.0F, -0.5F, 9.0F, 0.25F, 0.0F, 0.0F));

		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(60, 26).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F),
			PartPose.offset(-7.0F, LEG_Y, -6.0F));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(60, 26).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F),
			PartPose.offset(7.0F, LEG_Y, -6.0F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(60, 26).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F),
			PartPose.offset(-7.0F, LEG_Y, 6.5F));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(60, 26).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 4.0F, 5.0F),
			PartPose.offset(7.0F, LEG_Y, 6.5F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = state.walkAnimationPos * 0.6F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.5F);
		float lurk = state.veil;
		float tell = state.windup;
		float snap = state.acting;
		float hold = state.alt;
		float dazed = state.stunned;
		float shut = state.guard;
		float out = 1 - shut;

		// The body sways as it plods; lurking it settles low onto the riverbed, pulled in it drops onto its plastron.
		body.y = BODY_Y + 0.8F * lurk + 2.0F * shut + Mth.sin(walk * 2) * 0.3F * stride;
		body.zRot = Mth.sin(walk) * 0.04F * stride;
		body.xRot = 0.04F * tell - 0.03F * snap;

		// The head: drawn back for the snap, shot out on it, out and shaking as it holds on, drawn right in when it hides.
		head.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD * out * (1 - hold) + hold * Mth.sin(t * 1.3F) * 0.3F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * out * (1 - lurk) + 0.12F * lurk + 0.2F * dazed;
		head.z = HEAD_Z + 2.0F * tell - 3.0F * snap - 1.5F * hold + 7.0F * shut;
		head.y = -1.0F + 0.6F * lurk + 1.0F * shut;
		float headScale = 1 - 0.25F * shut;
		head.xScale = head.yScale = head.zScale = headScale;

		// The jaw: wide open lurking, a little shut drawing back, slammed shut on the snap, a few chomps of the tell.
		float open = 0.75F * lurk + 0.4F * tell + Math.max(0, Mth.sin(t * 0.05F) - 0.95F) * 6 * out * (1 - lurk);
		jaw.xRot = open * (1 - snap) * (1 - hold) * (1 - dazed) * out;

		// The lure: a pink worm wriggling on its tongue, only while it lurks.
		lure.yRot = Mth.sin(t * 0.55F) * 0.45F * lurk;
		lure.xRot = -0.2F * lurk + Mth.sin(t * 0.9F + 0.6F) * 0.25F * lurk;
		lure.visible = lurk > 0.05F;

		// The tail drags, swaying; in, it tucks up under the shell.
		tail.yRot = Mth.sin(walk) * 0.25F * stride + Mth.sin(t * 0.04F) * 0.08F;
		tail.xRot = 0.25F + 0.5F * shut;
		tail.zScale = 1 - 0.7F * shut;

		// Legs: a slow paddling plod, splayed flat lurking, drawn in and up when it hides.
		float swing = Mth.cos(walk) * 0.5F * stride;
		rightFront.xRot = swing;
		leftHind.xRot = swing;
		leftFront.xRot = -swing;
		rightHind.xRot = -swing;
		float splay = 0.3F * lurk;
		rightFront.zRot = rightHind.zRot = splay;
		leftFront.zRot = leftHind.zRot = -splay;
		float legScale = 1 - 0.7F * shut;
		for (ModelPart leg : new ModelPart[] {rightFront, leftFront, rightHind, leftHind}) {
			leg.yScale = legScale;
			leg.y = LEG_Y + 0.6F * lurk + 1.0F * shut;
		}
	}
}
