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
 * The Bog Witch-Frog: a great squat frog, warty and mossy, with a broad flat head, bulging lamp-eyes, a wide mouth and a
 * throat sac hanging under it, folded hind legs and splayed forefeet. A lily pad sits on its head like a hat, a toadstool
 * growing out of it, and moss hangs off its jowls. It moves in heavy hops; its throat swells huge (the tell) before it
 * spits; its mouth gapes before the tongue lashes out; swallowing, its eyes sink in, as a frog's do.
 *
 * <p>Laid out on a 128x64 skin (see {@code bog_witch_frog} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   body (0,0) 14x8x14    head (56,0) 15x5x10   eye (106,0) 4x3x4     jaw (0,22) 15x2x10    sac (50,22) 8x5x5
 *   foreleg (76,22) 3x6x3 forefoot (88,22) 5x1x5  thigh (0,36) 5x5x9  hindfoot (28,36) 6x1x8  lily pad (56,36) 10x0x10
 *   stem (96,36) 1x2x1    cap (100,36) 3x2x3    moss (112,36) 0x5x6   tongue (60,48) 2x1x12
 * </pre>
 */
public class BogWitchFrogModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart sac;
	private final ModelPart tongue;
	private final ModelPart rightEye;
	private final ModelPart leftEye;
	private final ModelPart rightFore;
	private final ModelPart leftFore;
	private final ModelPart rightHind;
	private final ModelPart leftHind;
	private final ModelPart lilyPad;

	public BogWitchFrogModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.jaw = head.getChild("jaw");
		this.sac = jaw.getChild("sac");
		this.tongue = jaw.getChild("tongue");
		this.rightEye = head.getChild("right_eye");
		this.leftEye = head.getChild("left_eye");
		this.lilyPad = head.getChild("lily_pad");
		this.rightFore = root.getChild("right_fore");
		this.leftFore = root.getChild("left_fore");
		this.rightHind = root.getChild("right_hind");
		this.leftHind = root.getChild("left_hind");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// A squat body, sitting up at the front.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -5.0F, -7.0F, 14.0F, 8.0F, 14.0F),
			PartPose.offsetAndRotation(0.0F, 17.0F, 2.0F, -0.22F, 0.0F, 0.0F));
		// A broad, flat head jutting forward, and the wide jaw under it.
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(56, 0).addBox(-7.5F, -3.5F, -9.0F, 15.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, -3.5F, -6.0F));
		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 22).addBox(-7.5F, 0.0F, -9.0F, 15.0F, 2.0F, 10.0F),
			PartPose.offset(0.0F, 1.5F, 0.0F));
		jaw.addOrReplaceChild("sac", CubeListBuilder.create().texOffs(50, 22).addBox(-4.0F, -1.0F, -2.5F, 8.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, 2.0F, -5.0F));
		jaw.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(60, 48).addBox(-1.0F, -0.5F, -12.0F, 2.0F, 1.0F, 12.0F),
			PartPose.offset(0.0F, 0.3F, -8.0F));
		jaw.addOrReplaceChild("right_moss", CubeListBuilder.create().texOffs(112, 36).addBox(0.0F, 0.0F, -3.0F, 0.0F, 5.0F, 6.0F),
			PartPose.offsetAndRotation(-7.6F, 1.0F, -4.5F, 0.0F, 0.0F, 0.12F));
		jaw.addOrReplaceChild("left_moss", CubeListBuilder.create().texOffs(112, 36).mirror().addBox(0.0F, 0.0F, -3.0F, 0.0F, 5.0F, 6.0F),
			PartPose.offsetAndRotation(7.6F, 1.0F, -3.0F, 0.0F, 0.0F, -0.12F));
		// Bulging lamp-eyes on top.
		head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(106, 0).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F),
			PartPose.offset(-4.6F, -3.0F, -5.5F));
		head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(106, 0).mirror().addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F),
			PartPose.offset(4.6F, -3.0F, -5.5F));
		// A lily pad worn like a hat, a toadstool growing out of it.
		PartDefinition pad = head.addOrReplaceChild("lily_pad", CubeListBuilder.create().texOffs(56, 36).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 0.0F, 10.0F),
			PartPose.offsetAndRotation(0.5F, -3.6F, -2.0F, -0.12F, 0.3F, 0.16F));
		PartDefinition stem = pad.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(96, 36).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
			PartPose.offsetAndRotation(2.5F, 0.0F, 1.5F, 0.0F, 0.0F, 0.15F));
		stem.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(100, 36).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, -1.8F, 0.0F));

		// Splayed forefeet, and great hind legs folded along its sides.
		fore(root, "right_fore", -5.5F, false);
		fore(root, "left_fore", 5.5F, true);
		hind(root, "right_hind", -6.8F, false);
		hind(root, "left_hind", 6.8F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void fore(PartDefinition root, String name, float x, boolean mirror) {
		CubeListBuilder leg = CubeListBuilder.create().texOffs(76, 22);
		PartDefinition part = root.addOrReplaceChild(name, (mirror ? leg.mirror() : leg).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
			PartPose.offsetAndRotation(x, 18.0F, -5.0F, -0.15F, 0.0F, mirror ? -0.15F : 0.15F));
		CubeListBuilder foot = CubeListBuilder.create().texOffs(88, 22);
		part.addOrReplaceChild("foot", (mirror ? foot.mirror() : foot).addBox(-2.5F, 0.0F, -3.5F, 5.0F, 1.0F, 5.0F),
			PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.15F, 0.0F, mirror ? 0.15F : -0.15F));
	}

	private static void hind(PartDefinition root, String name, float x, boolean mirror) {
		CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 36);
		PartDefinition part = root.addOrReplaceChild(name, (mirror ? thigh.mirror() : thigh).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 9.0F),
			PartPose.offset(x, 19.5F, 5.0F));
		CubeListBuilder foot = CubeListBuilder.create().texOffs(28, 36);
		part.addOrReplaceChild("foot", (mirror ? foot.mirror() : foot).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 1.0F, 8.0F),
			PartPose.offset(mirror ? 0.8F : -0.8F, 3.5F, -3.0F));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos;
		float swell = state.windup;
		float gape = state.alt;
		float gulp = state.guard;

		// Heavy hops: up and forward on the hind legs' kick, landing on the forefeet.
		float hop = Math.max(0, Mth.sin(pos * 0.55F)) * walk;
		body.y = 17.0F - hop * 3.0F;
		body.xRot = -0.22F - hop * 0.2F;
		rightHind.xRot = hop * 0.9F;
		leftHind.xRot = hop * 0.9F;
		rightHind.y = 19.5F - hop * 1.5F;
		leftHind.y = 19.5F - hop * 1.5F;
		rightFore.xRot = -0.15F - hop * 0.6F;
		leftFore.xRot = -0.15F - hop * 0.6F;
		rightFore.y = 18.0F - hop * 2.5F;
		leftFore.y = 18.0F - hop * 2.5F;

		// The head looks about; it lifts as the throat swells.
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
		head.xRot = Mth.clamp(state.xRot, -20, 20) * Mth.DEG_TO_RAD - 0.25F * swell + 0.15F * gulp * Mth.sin(t * 0.9F);

		// The throat sac: a slow pulse at rest, swelling huge before a spit, fat after a meal, bulging as it swallows.
		float pulse = 1 + Mth.sin(t * 0.3F) * 0.07F;
		float size = pulse * (1 + 0.95F * swell + 0.4F * gulp + (state.engorged ? 0.25F : 0));
		sac.xScale = size;
		sac.yScale = size;
		sac.zScale = size;

		// The mouth gapes, and the tongue unrolls out of it.
		jaw.xRot = 0.75F * gape + 0.1F * gulp * Math.max(0, Mth.sin(t * 0.9F));
		tongue.visible = gape > 0.6F;
		tongue.zScale = Math.max(0.05F, (gape - 0.6F) / 0.4F);

		// Swallowing, its eyes sink in.
		rightEye.y = -3.0F + 1.6F * gulp;
		leftEye.y = -3.0F + 1.6F * gulp;
		lilyPad.zRot = 0.16F + Mth.sin(t * 0.05F) * 0.03F;
	}
}
