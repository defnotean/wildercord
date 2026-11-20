package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.ShrineCatRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Custom 3D Shrine Cat model matching the four real cats from reference photos:
 * fluffy cheek ruffs, chest bib, whiskers, and plumed tail for {@code Yuki} &amp; {@code Odie};
 * pink collar &amp; swinging silver bell charm for {@code Freya};
 * sleek shorthair body and geometrically notched left ear for {@code Kuro}.
 */
public class ShrineCatModel extends EntityModel<ShrineCatRenderState> {
	private final ModelPart body;
	private final ModelPart fluffyCoat;
	private final ModelPart collar;
	private final ModelPart bell;
	private final ModelPart chestRuff;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart leftEarNotched;
	private final ModelPart leftCheekRuff;
	private final ModelPart rightCheekRuff;
	private final ModelPart whiskersLeft;
	private final ModelPart whiskersRight;
	private final ModelPart frontLeft;
	private final ModelPart frontRight;
	private final ModelPart backLeft;
	private final ModelPart backRight;
	private final ModelPart sleekTail;
	private final ModelPart sleekTailTip;
	private final ModelPart plumeTail;
	private final ModelPart plumeTailTip;

	public ShrineCatModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.fluffyCoat = this.body.getChild("fluffy_coat");
		this.collar = this.body.getChild("collar");
		this.bell = this.body.getChild("bell");
		this.chestRuff = this.body.getChild("chest_ruff");
		this.head = root.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.leftEarNotched = this.head.getChild("left_ear_notched");
		this.leftCheekRuff = this.head.getChild("left_cheek_ruff");
		this.rightCheekRuff = this.head.getChild("right_cheek_ruff");
		this.whiskersLeft = this.head.getChild("whiskers_left");
		this.whiskersRight = this.head.getChild("whiskers_right");
		this.frontLeft = root.getChild("front_left");
		this.frontRight = root.getChild("front_right");
		this.backLeft = root.getChild("back_left");
		this.backRight = root.getChild("back_right");
		this.sleekTail = root.getChild("sleek_tail");
		this.sleekTailTip = this.sleekTail.getChild("sleek_tail_tip");
		this.plumeTail = root.getChild("plume_tail");
		this.plumeTailTip = this.plumeTail.getChild("plume_tail_tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, 17.0F, 0.0F));
		body.addOrReplaceChild("fluffy_coat",
			CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -2.2F, -4.5F, 6.0F, 5.0F, 9.0F),
			PartPose.ZERO);
		body.addOrReplaceChild("collar",
			CubeListBuilder.create().texOffs(32, 0).addBox(-2.6F, -2.6F, -5.6F, 5.2F, 3.2F, 2.0F),
			PartPose.ZERO);
		body.addOrReplaceChild("bell",
			CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, 0.4F, -5.8F));
		body.addOrReplaceChild("chest_ruff",
			CubeListBuilder.create().texOffs(32, 8).addBox(-2.5F, -1.8F, -6.2F, 5.0F, 4.0F, 2.0F),
			PartPose.ZERO);

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 32).addBox(-3.0F, -3.5F, -4.5F, 6.0F, 5.0F, 5.0F)
				.texOffs(24, 32).addBox(-1.5F, -0.8F, -5.8F, 3.0F, 2.0F, 2.0F),
			PartPose.offset(0.0F, 14.5F, -5.0F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create().texOffs(24, 38).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(-2.0F, -3.5F, -1.5F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create().texOffs(24, 38).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(2.0F, -3.5F, -1.5F));
		// Clipped / notched left ear silhouette for Kuro.
		head.addOrReplaceChild("left_ear_notched",
			CubeListBuilder.create()
				.texOffs(32, 38).addBox(-1.0F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(32, 38).addBox(0.0F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.offset(2.0F, -3.5F, -1.5F));
		head.addOrReplaceChild("left_cheek_ruff",
			CubeListBuilder.create().texOffs(36, 32).mirror().addBox(0.0F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F),
			PartPose.offset(2.8F, 0.0F, -2.2F));
		head.addOrReplaceChild("right_cheek_ruff",
			CubeListBuilder.create().texOffs(36, 32).addBox(-1.0F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F),
			PartPose.offset(-2.8F, 0.0F, -2.2F));
		head.addOrReplaceChild("whiskers_left",
			CubeListBuilder.create().texOffs(46, 32).mirror().addBox(0.0F, -1.0F, -0.5F, 3.0F, 2.0F, 1.0F),
			PartPose.offset(1.8F, 0.2F, -5.2F));
		head.addOrReplaceChild("whiskers_right",
			CubeListBuilder.create().texOffs(46, 32).addBox(-3.0F, -1.0F, -0.5F, 3.0F, 2.0F, 1.0F),
			PartPose.offset(-1.8F, 0.2F, -5.2F));

		root.addOrReplaceChild("front_left", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(1.3F, 18.0F, -3.5F));
		root.addOrReplaceChild("front_right", CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(-1.3F, 18.0F, -3.5F));
		root.addOrReplaceChild("back_left", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(1.3F, 18.0F, 3.8F));
		root.addOrReplaceChild("back_right", CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
			PartPose.offset(-1.3F, 18.0F, 3.8F));

		PartDefinition sleekTail = root.addOrReplaceChild("sleek_tail",
			CubeListBuilder.create().texOffs(12, 46).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
			PartPose.offset(0.0F, 15.5F, 4.8F));
		sleekTail.addOrReplaceChild("sleek_tail_tip",
			CubeListBuilder.create().texOffs(28, 46).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, 4.6F));

		PartDefinition plumeTail = root.addOrReplaceChild("plume_tail",
			CubeListBuilder.create().texOffs(12, 54).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
			PartPose.offset(0.0F, 15.5F, 4.8F));
		plumeTail.addOrReplaceChild("plume_tail_tip",
			CubeListBuilder.create().texOffs(28, 54).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, 4.6F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(ShrineCatRenderState state) {
		super.setupAnim(state);
		ShrineCatRules.Variant v = ShrineCatRules.Variant.byId(state.variant);
		fluffyCoat.visible = v.fluffyCoat;
		chestRuff.visible = v.fluffyCoat;
		leftCheekRuff.visible = v.fluffyCoat;
		rightCheekRuff.visible = v.fluffyCoat;
		whiskersLeft.visible = v.fluffyCoat;
		whiskersRight.visible = v.fluffyCoat;
		collar.visible = v.bellCollar || state.tamed;
		bell.visible = v.bellCollar || state.tamed;
		leftEar.visible = !v.notchedLeftEar;
		leftEarNotched.visible = v.notchedLeftEar;
		plumeTail.visible = v.fluffyCoat;
		sleekTail.visible = !v.fluffyCoat;

		float t = state.ageInTicks;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float stride = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.72F * state.walkAnimationSpeed;
		frontLeft.xRot = backRight.xRot = stride;
		frontRight.xRot = backLeft.xRot = -stride;

		// Default resting / standing offsets.
		body.y = 17.0F;
		body.xRot = 0.0F;
		head.y = 14.5F;
		head.z = -5.0F;
		frontLeft.y = frontRight.y = 18.0F;
		backLeft.y = backRight.y = 18.0F;
		backLeft.z = backRight.z = 3.8F;
		sleekTail.y = plumeTail.y = 15.5F;

		if (state.loafing) {
			// Cozy cat loaf: body & head lowered cleanly, legs tucked beneath.
			float breathe = Mth.sin(t * 0.09F) * 0.03F;
			body.y = 21.3F + breathe;
			body.xRot = 0.06F;
			head.y = 18.8F + breathe;
			head.z = -4.8F;
			frontLeft.y = frontRight.y = 21.5F;
			backLeft.y = backRight.y = 21.5F;
			frontLeft.xRot = frontRight.xRot = -1.35F;
			backLeft.xRot = backRight.xRot = -1.35F;
			sleekTail.y = plumeTail.y = 20.6F;
		} else if (state.sitting) {
			// Prim sitting pose (like Freya perched beside the calendar!).
			body.xRot = -0.52F;
			body.y = 16.0F;
			head.y = 11.8F;
			head.z = -4.2F;
			frontLeft.y = frontRight.y = 17.5F;
			frontLeft.xRot = frontRight.xRot = -0.15F;
			backLeft.y = backRight.y = 21.0F;
			backLeft.z = backRight.z = 1.6F;
			backLeft.xRot = backRight.xRot = -1.15F;
			sleekTail.y = plumeTail.y = 19.6F;
		}

		// Expressive tail wave, ear flick, bell chime, and signature action animations.
		float tailBasePitch = state.loafing ? 0.15F : 0.42F + state.walkAnimationSpeed * 0.35F;
		float tailWag = Mth.sin(t * 0.14F) * 0.32F;
		sleekTail.xRot = plumeTail.xRot = tailBasePitch;
		sleekTail.yRot = plumeTail.yRot = tailWag;
		sleekTailTip.yRot = plumeTailTip.yRot = Mth.sin(t * 0.14F - 0.9F) * 0.28F;
		sleekTailTip.xRot = plumeTailTip.xRot = 0.25F + Mth.cos(t * 0.12F) * 0.12F;

		float flick = state.earFlick > 0 ? Mth.sin(state.earFlick * 0.9F) * 0.28F : 0.0F;
		rightEar.zRot = flick;
		leftEar.zRot = -flick * 0.5F;
		leftEarNotched.zRot = -flick * 0.5F;

		bell.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.45F * state.walkAnimationSpeed;
		bell.zRot = Mth.sin(t * 0.18F) * 0.06F;

		if (state.actionTick > 0) {
			float pulse = Mth.sin((20 - state.actionTick) * 0.314F);
			if (v == ShrineCatRules.Variant.FREYA) {
				bell.zRot = Mth.sin(t * 2.1F) * 0.72F;
				head.zRot = Mth.sin(t * 0.8F) * 0.12F;
			} else if (v == ShrineCatRules.Variant.ODIE) {
				body.y -= pulse * 1.4F;
				head.y -= pulse * 1.4F;
			} else {
				// Pounce / shadow lunge pose for Yuki and Kuro.
				frontLeft.xRot -= pulse * 0.75F;
				frontRight.xRot -= pulse * 0.75F;
				body.xRot -= pulse * 0.22F;
			}
		}
	}
}
