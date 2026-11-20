package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.ShrineCatRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Custom multi-jointed 128x128 Shrine Cat rig matching the turnaround &amp; pose references for
 * {@code Yuki}, {@code Odie}, {@code Freya}, and {@code Kuro}: ribcage + hindquarters, 3-tier
 * chest ruff, swept cheek ruffs, individual 3D whiskers, pink collar + rear bow + swinging
 * Suzu bell, notched left ear for Kuro, 2-segment articulated legs &amp; paws, and 3-segment tails.
 */
public class ShrineCatModel extends EntityModel<ShrineCatRenderState> {
	private final ModelPart body;
	private final ModelPart hindquarters;
	private final ModelPart fluffyCoat;
	private final ModelPart chestRuff;
	private final ModelPart collar;
	private final ModelPart collarKnot;
	private final ModelPart bell;
	private final ModelPart head;
	private final ModelPart chinTuft;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart leftEarNotched;
	private final ModelPart leftCheekRuff;
	private final ModelPart rightCheekRuff;
	private final ModelPart whiskersLeft;
	private final ModelPart whiskersRight;
	private final ModelPart frontLeft;
	private final ModelPart frontLeftLower;
	private final ModelPart frontRight;
	private final ModelPart frontRightLower;
	private final ModelPart backLeft;
	private final ModelPart backLeftLower;
	private final ModelPart backRight;
	private final ModelPart backRightLower;
	private final ModelPart sleekTail;
	private final ModelPart sleekTailMid;
	private final ModelPart sleekTailTip;
	private final ModelPart plumeTail;
	private final ModelPart plumeTailMid;
	private final ModelPart plumeTailTip;

	public ShrineCatModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.hindquarters = this.body.getChild("hindquarters");
		this.fluffyCoat = this.body.getChild("fluffy_coat");
		this.chestRuff = this.body.getChild("chest_ruff");
		this.collar = this.body.getChild("collar");
		this.collarKnot = this.collar.getChild("collar_knot");
		this.bell = this.collar.getChild("bell");
		this.head = root.getChild("head");
		this.chinTuft = this.head.getChild("chin_tuft");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.leftEarNotched = this.head.getChild("left_ear_notched");
		this.leftCheekRuff = this.head.getChild("left_cheek_ruff");
		this.rightCheekRuff = this.head.getChild("right_cheek_ruff");
		this.whiskersLeft = this.head.getChild("whiskers_left");
		this.whiskersRight = this.head.getChild("whiskers_right");
		this.frontLeft = root.getChild("front_left");
		this.frontLeftLower = this.frontLeft.getChild("front_left_lower");
		this.frontRight = root.getChild("front_right");
		this.frontRightLower = this.frontRight.getChild("front_right_lower");
		this.backLeft = root.getChild("back_left");
		this.backLeftLower = this.backLeft.getChild("back_left_lower");
		this.backRight = root.getChild("back_right");
		this.backRightLower = this.backRight.getChild("back_right_lower");
		this.sleekTail = root.getChild("sleek_tail");
		this.sleekTailMid = this.sleekTail.getChild("sleek_tail_mid");
		this.sleekTailTip = this.sleekTailMid.getChild("sleek_tail_tip");
		this.plumeTail = root.getChild("plume_tail");
		this.plumeTailMid = this.plumeTail.getChild("plume_tail_mid");
		this.plumeTailTip = this.plumeTailMid.getChild("plume_tail_tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Core torso: ribcage + articulated hindquarters + longhair coat + 3-tier chest ruff.
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -3.5F, 5, 5, 6),
			PartPose.offset(0.0F, 16.5F, -1.0F));
		body.addOrReplaceChild("hindquarters",
			CubeListBuilder.create().texOffs(24, 0).addBox(-2.5F, -2.0F, 0.0F, 5, 4, 5, new CubeDeformation(0.05F)),
			PartPose.offset(0.0F, 0.0F, 2.0F));
		body.addOrReplaceChild("fluffy_coat",
			CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, -2.5F, -3.0F, 6, 5, 9, new CubeDeformation(0.18F)),
			PartPose.ZERO);
		body.addOrReplaceChild("chest_ruff",
			CubeListBuilder.create()
				.texOffs(34, 14).addBox(-2.5F, -1.2F, -4.4F, 5, 2, 2, new CubeDeformation(0.22F))
				.texOffs(34, 20).addBox(-2.0F, 0.7F, -4.6F, 4, 2, 2, new CubeDeformation(0.14F))
				.texOffs(48, 20).addBox(-1.0F, 2.3F, -4.2F, 2, 1, 2, new CubeDeformation(0.08F)),
			PartPose.ZERO);

		// Freya's signature rose-pink collar, rear bow knot & dangling silver Suzu bell.
		PartDefinition collar = body.addOrReplaceChild("collar",
			CubeListBuilder.create().texOffs(50, 0).addBox(-2.5F, -2.5F, -4.0F, 5, 3, 2, new CubeDeformation(0.28F)),
			PartPose.ZERO);
		collar.addOrReplaceChild("collar_knot",
			CubeListBuilder.create().texOffs(66, 0).addBox(-1.5F, -0.5F, -0.5F, 3, 2, 1),
			PartPose.offset(0.0F, -2.8F, -2.3F));
		collar.addOrReplaceChild("bell",
			CubeListBuilder.create().texOffs(76, 0).addBox(-1.0F, 0.0F, -1.0F, 2, 2, 2, new CubeDeformation(-0.18F)),
			PartPose.offset(0.0F, 0.4F, -4.8F));

		// Head, muzzle, longhair chin tuft, ears (regular & Kuro's notched left ear), cheek ruffs & whiskers.
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 32).addBox(-3.0F, -3.5F, -4.5F, 6, 5, 5)
				.texOffs(24, 32).addBox(-1.5F, -0.7F, -5.8F, 3, 2, 2),
			PartPose.offset(0.0F, 14.5F, -4.8F));
		head.addOrReplaceChild("chin_tuft",
			CubeListBuilder.create().texOffs(24, 38).addBox(-1.0F, 0.9F, -5.4F, 2, 1, 2, new CubeDeformation(0.12F)),
			PartPose.ZERO);

		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create()
				.texOffs(36, 32).addBox(-1.0F, -2.0F, -0.5F, 2, 2, 1)
				.texOffs(44, 32).addBox(-0.5F, -3.0F, -0.5F, 1, 1, 1),
			PartPose.offset(-2.0F, -3.5F, -1.6F));
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create()
				.texOffs(36, 32).mirror().addBox(-1.0F, -2.0F, -0.5F, 2, 2, 1)
				.texOffs(44, 32).mirror().addBox(-0.5F, -3.0F, -0.5F, 1, 1, 1),
			PartPose.offset(2.0F, -3.5F, -1.6F));
		// Clipped / notched left ear silhouette on outer edge (+X side facing viewer's right) for Kuro.
		head.addOrReplaceChild("left_ear_notched",
			CubeListBuilder.create()
				.texOffs(36, 38).addBox(-1.0F, -2.0F, -0.5F, 1, 2, 1)
				.texOffs(44, 38).addBox(0.0F, -1.0F, -0.5F, 1, 1, 1),
			PartPose.offset(2.0F, -3.5F, -1.6F));

		head.addOrReplaceChild("left_cheek_ruff",
			CubeListBuilder.create()
				.texOffs(50, 32).mirror().addBox(-0.2F, -1.5F, -1.5F, 2, 3, 3, new CubeDeformation(-0.15F))
				.texOffs(62, 32).mirror().addBox(0.4F, 0.8F, -1.0F, 1, 2, 2),
			PartPose.offsetAndRotation(2.5F, -0.2F, -2.0F, 0.0F, 0.0F, 0.14F));
		head.addOrReplaceChild("right_cheek_ruff",
			CubeListBuilder.create()
				.texOffs(50, 32).addBox(-1.8F, -1.5F, -1.5F, 2, 3, 3, new CubeDeformation(-0.15F))
				.texOffs(62, 32).addBox(-1.4F, 0.8F, -1.0F, 1, 2, 2),
			PartPose.offsetAndRotation(-2.5F, -0.2F, -2.0F, 0.0F, 0.0F, -0.14F));

		PartDefinition wl = head.addOrReplaceChild("whiskers_left", CubeListBuilder.create(), PartPose.offset(1.5F, -0.2F, -5.2F));
		PartDefinition wr = head.addOrReplaceChild("whiskers_right", CubeListBuilder.create(), PartPose.offset(-1.5F, -0.2F, -5.2F));
		for (int i = 0; i < 3; i++) {
			float y = (i - 1) * 0.35F;
			float spreadYaw = (i - 1) * 0.16F;
			wl.addOrReplaceChild("w_" + i,
				CubeListBuilder.create().texOffs(70, 32).addBox(0.0F, 0.0F, 0.0F, 3, 0.16F, 0.16F),
				PartPose.offsetAndRotation(0.0F, y, 0.0F, 0.0F, spreadYaw, 0.06F));
			wr.addOrReplaceChild("w_" + i,
				CubeListBuilder.create().texOffs(70, 32).addBox(-3.0F, 0.0F, 0.0F, 3, 0.16F, 0.16F),
				PartPose.offsetAndRotation(0.0F, y, 0.0F, 0.0F, -spreadYaw, -0.06F));
		}

		// Articulated 2-segment forelegs and hindlegs (feet rest on ground Y = 24.0).
		PartDefinition fl = root.addOrReplaceChild("front_left",
			CubeListBuilder.create().texOffs(0, 50).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
			PartPose.offset(1.4F, 17.5F, -3.4F));
		fl.addOrReplaceChild("front_left_lower",
			CubeListBuilder.create().texOffs(10, 50).mirror().addBox(-1.0F, 0.0F, -1.2F, 2, 3, 2, new CubeDeformation(0.05F)),
			PartPose.offset(0.0F, 3.5F, 0.2F));

		PartDefinition fr = root.addOrReplaceChild("front_right",
			CubeListBuilder.create().texOffs(0, 50).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2),
			PartPose.offset(-1.4F, 17.5F, -3.4F));
		fr.addOrReplaceChild("front_right_lower",
			CubeListBuilder.create().texOffs(10, 50).addBox(-1.0F, 0.0F, -1.2F, 2, 3, 2, new CubeDeformation(0.05F)),
			PartPose.offset(0.0F, 3.5F, 0.2F));

		PartDefinition bl = root.addOrReplaceChild("back_left",
			CubeListBuilder.create().texOffs(20, 50).mirror().addBox(-1.0F, -0.5F, -1.5F, 2, 4, 3),
			PartPose.offset(1.4F, 17.5F, 4.0F));
		bl.addOrReplaceChild("back_left_lower",
			CubeListBuilder.create().texOffs(32, 50).mirror().addBox(-1.0F, 0.0F, -1.3F, 2, 3, 2, new CubeDeformation(0.05F)),
			PartPose.offset(0.0F, 3.5F, 0.4F));

		PartDefinition br = root.addOrReplaceChild("back_right",
			CubeListBuilder.create().texOffs(20, 50).addBox(-1.0F, -0.5F, -1.5F, 2, 4, 3),
			PartPose.offset(-1.4F, 17.5F, 4.0F));
		br.addOrReplaceChild("back_right_lower",
			CubeListBuilder.create().texOffs(32, 50).addBox(-1.0F, 0.0F, -1.3F, 2, 3, 2, new CubeDeformation(0.05F)),
			PartPose.offset(0.0F, 3.5F, 0.4F));

		// 3-segment tails: sleek (Freya & Kuro) and fluffy plume (Yuki & Odie).
		PartDefinition sleek = root.addOrReplaceChild("sleek_tail",
			CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 3),
			PartPose.offset(0.0F, 15.2F, 5.2F));
		PartDefinition sleekMid = sleek.addOrReplaceChild("sleek_tail_mid",
			CubeListBuilder.create().texOffs(12, 64).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 3),
			PartPose.offset(0.0F, 0.0F, 2.8F));
		sleekMid.addOrReplaceChild("sleek_tail_tip",
			CubeListBuilder.create().texOffs(24, 64).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 3, new CubeDeformation(-0.15F)),
			PartPose.offset(0.0F, 0.0F, 2.8F));

		PartDefinition plume = root.addOrReplaceChild("plume_tail",
			CubeListBuilder.create().texOffs(0, 74).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 3),
			PartPose.offset(0.0F, 15.2F, 5.2F));
		PartDefinition plumeMid = plume.addOrReplaceChild("plume_tail_mid",
			CubeListBuilder.create().texOffs(14, 74).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 3, new CubeDeformation(0.1F)),
			PartPose.offset(0.0F, 0.0F, 2.8F));
		plumeMid.addOrReplaceChild("plume_tail_tip",
			CubeListBuilder.create().texOffs(28, 74).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 3),
			PartPose.offset(0.0F, 0.0F, 2.8F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(ShrineCatRenderState state) {
		super.setupAnim(state);
		ShrineCatRules.Variant v = ShrineCatRules.Variant.byId(state.variant);
		fluffyCoat.visible = v.fluffyCoat;
		chestRuff.visible = v.fluffyCoat;
		chinTuft.visible = v.fluffyCoat;
		leftCheekRuff.visible = v.fluffyCoat;
		rightCheekRuff.visible = v.fluffyCoat;
		whiskersLeft.visible = v.fluffyCoat;
		whiskersRight.visible = v.fluffyCoat;
		collar.visible = v.bellCollar || state.tamed;
		collarKnot.visible = v.bellCollar || state.tamed;
		bell.visible = v.bellCollar || state.tamed;
		leftEar.visible = !v.notchedLeftEar;
		leftEarNotched.visible = v.notchedLeftEar;
		plumeTail.visible = v.fluffyCoat;
		sleekTail.visible = !v.fluffyCoat;

		float t = state.ageInTicks;
		float breathe = Mth.sin(t * 0.09F);

		// Reset all animated pivots and rotations cleanly.
		body.y = 16.5F + breathe * 0.05F;
		body.z = -1.0F;
		body.xRot = 0.0F;
		body.zRot = 0.0F;
		hindquarters.xRot = 0.0F;

		head.y = 14.5F + breathe * 0.04F;
		head.z = -4.8F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.zRot = 0.0F;

		frontLeft.y = frontRight.y = 17.5F;
		frontLeft.z = frontRight.z = -3.4F;
		backLeft.y = backRight.y = 17.5F;
		backLeft.z = backRight.z = 4.0F;
		frontLeft.xRot = frontRight.xRot = frontLeft.zRot = frontRight.zRot = 0.0F;
		frontLeftLower.xRot = frontRightLower.xRot = 0.0F;
		backLeft.xRot = backRight.xRot = -0.22F;
		backLeftLower.xRot = backRightLower.xRot = 0.22F;

		sleekTail.y = plumeTail.y = 15.2F;
		sleekTail.z = plumeTail.z = 5.2F;

		if (state.loafing) {
			// Tucked cozy loaf pose matching turnaround Strip 1.
			body.y = 21.0F + breathe * 0.04F;
			body.z = -0.5F;
			body.xRot = 0.04F;
			hindquarters.xRot = -0.04F;
			head.y = 18.6F + breathe * 0.03F;
			head.z = -4.4F;

			frontLeft.y = frontRight.y = 21.2F;
			frontLeft.z = frontRight.z = -3.4F;
			frontLeft.xRot = frontRight.xRot = -1.28F;
			frontLeftLower.xRot = frontRightLower.xRot = 2.25F;

			backLeft.y = backRight.y = 21.2F;
			backLeft.z = backRight.z = 3.2F;
			backLeft.xRot = backRight.xRot = -1.35F;
			backLeftLower.xRot = backRightLower.xRot = 1.45F;

			sleekTail.y = plumeTail.y = 21.5F;
			sleekTail.z = plumeTail.z = 4.5F;
			setTail(-0.25F, 0.12F, 0.14F, 0.65F + Mth.sin(t * 0.07F) * 0.08F, 0.55F, 0.45F);
		} else if (state.sitting) {
			// Prim upright desk-perch sit inspired by Freya's reference photo & Strip 2.
			body.y = 17.8F + breathe * 0.04F;
			body.z = 0.4F;
			body.xRot = -0.58F;
			hindquarters.xRot = 0.36F;
			head.y = 11.4F;
			head.z = -3.8F;

			frontLeft.y = frontRight.y = 17.5F;
			frontLeft.z = frontRight.z = -3.2F;
			frontLeft.xRot = frontRight.xRot = -0.05F;
			frontLeftLower.xRot = frontRightLower.xRot = 0.05F;

			backLeft.y = backRight.y = 21.5F;
			backLeft.z = backRight.z = 2.4F;
			backLeft.xRot = backRight.xRot = -1.25F;
			backLeftLower.xRot = backRightLower.xRot = 1.25F;

			sleekTail.y = plumeTail.y = 22.0F;
			sleekTail.z = plumeTail.z = 4.2F;
			setTail(-0.32F, 0.10F, 0.12F, 0.68F + Mth.sin(t * 0.09F) * 0.10F, 0.65F, 0.52F);
		} else {
			// 4-beat articulated walk stride with knee/hock flexion & torso counter-balance.
			float wp = state.walkAnimationPos * 0.75F;
			float ws = Math.min(1.0F, state.walkAnimationSpeed * 1.4F);
			if (ws > 0.02F) {
				float pFL = wp;
				float pBR = wp + 1.57F;
				float pFR = wp + Mth.PI;
				float pBL = wp + 4.71F;

				float sFL = Mth.sin(pFL), sFR = Mth.sin(pFR);
				float sBL = Mth.sin(pBL), sBR = Mth.sin(pBR);

				frontLeft.xRot = sFL * 0.68F * ws;
				frontRight.xRot = sFR * 0.68F * ws;
				frontLeftLower.xRot = Math.max(0.0F, -sFL) * 0.72F * ws;
				frontRightLower.xRot = Math.max(0.0F, -sFR) * 0.72F * ws;

				backLeft.xRot = -0.22F + sBL * 0.62F * ws;
				backRight.xRot = -0.22F + sBR * 0.62F * ws;
				backLeftLower.xRot = 0.22F + Math.max(0.0F, -sBL) * 0.65F * ws;
				backRightLower.xRot = 0.22F + Math.max(0.0F, -sBR) * 0.65F * ws;

				body.y += Mth.sin(wp * 2.0F) * 0.35F * ws;
				body.zRot = Mth.cos(wp) * 0.05F * ws;
				head.xRot -= Mth.sin(wp * 2.0F) * 0.06F * ws;
			}
			float wag = Mth.sin(t * (0.14F + ws * 0.10F)) * (0.24F + 0.16F * ws);
			setTail(
				0.52F + ws * 0.22F,
				0.32F + Mth.cos(t * 0.12F) * 0.10F,
				0.24F + Mth.sin(t * 0.12F) * 0.10F,
				wag,
				Mth.sin(t * 0.14F - 0.8F) * (0.28F + 0.18F * ws),
				Mth.sin(t * 0.14F - 1.6F) * (0.32F + 0.20F * ws));
		}

		// Ear flicks & Suzu collar bell swing.
		float flick = state.earFlick > 0 ? Mth.sin(state.earFlick * 0.9F) * 0.28F : 0.0F;
		rightEar.zRot = flick;
		leftEar.zRot = -flick * 0.5F;
		leftEarNotched.zRot = -flick * 0.5F;

		bell.xRot = Mth.cos(state.walkAnimationPos * 0.75F) * 0.42F * state.walkAnimationSpeed - body.xRot * 0.35F;
		bell.zRot = Mth.sin(t * 0.18F) * 0.08F;

		// Variant-specific signature ability animations matching Strip 4 references.
		if (state.actionTick > 0) {
			float prog = (20 - state.actionTick) / 20.0F;
			float pulse = Mth.sin(prog * Mth.PI);
			switch (v) {
				case YUKI -> {
					// Leaping Frost-Gust Swipe: coiled rear push + sweeping right forepaw swipe.
					body.y -= pulse * 1.4F;
					body.xRot -= pulse * 0.28F;
					frontRight.xRot = -1.25F * pulse;
					frontRight.zRot = -0.35F * pulse;
					frontRightLower.xRot = -0.45F * pulse;
					frontLeft.xRot = -0.65F * pulse;
					backLeft.xRot = 0.55F * pulse;
					backRight.xRot = 0.55F * pulse;
					setTail(-0.20F, 0.15F, 0.20F, 0.0F, 0.0F, 0.0F);
				}
				case ODIE -> {
					// Biscuit-Kneading Hearth Purr Stretch: low cozy shoulder stretch & alternating forepaw knead.
					float knead = Mth.sin(t * 0.95F);
					body.y += 1.4F * pulse;
					body.xRot = 0.24F * pulse;
					head.y += 0.9F * pulse;
					frontLeft.xRot = -0.75F * pulse + knead * 0.32F;
					frontLeftLower.xRot = 0.35F * pulse - knead * 0.28F;
					frontRight.xRot = -0.75F * pulse - knead * 0.32F;
					frontRightLower.xRot = 0.35F * pulse + knead * 0.28F;
				}
				case FREYA -> {
					// Silver-Bell Chime Prance & Head-Tilt Shake.
					body.y -= Math.abs(Mth.sin(prog * Mth.PI * 2.0F)) * 1.1F;
					head.zRot = Mth.sin(t * 0.9F) * 0.26F;
					bell.zRot = Mth.sin(t * 2.2F) * 0.85F;
					bell.xRot = Mth.cos(t * 1.7F) * 0.45F;
					frontLeft.xRot = -0.85F * Math.max(0.0F, Mth.sin(prog * Mth.PI * 2.0F));
					frontRight.xRot = -0.85F * Math.max(0.0F, -Mth.sin(prog * Mth.PI * 2.0F));
				}
				case KURO -> {
					// Low Predator Stalk (first 35%) into Outstretched Shadow-Step Pounce.
					if (prog < 0.35F) {
						float k = prog / 0.35F;
						body.y += 1.8F * k;
						head.y += 1.6F * k;
						frontLeft.xRot = frontRight.xRot = -0.65F * k;
						backLeft.xRot = backRight.xRot = -0.75F * k;
					} else {
						float p = Mth.sin(((prog - 0.35F) / 0.65F) * Mth.PI);
						body.y -= 1.6F * p;
						body.xRot = -0.18F * p;
						frontLeft.xRot = frontRight.xRot = -1.15F * p;
						backLeft.xRot = backRight.xRot = 0.95F * p;
						setTail(0.05F, 0.08F, 0.12F, 0.0F, 0.0F, 0.0F);
					}
				}
			}
		}
	}

	private void setTail(float x0, float x1, float x2, float y0, float y1, float y2) {
		sleekTail.xRot = plumeTail.xRot = x0;
		sleekTailMid.xRot = plumeTailMid.xRot = x1;
		sleekTailTip.xRot = plumeTailTip.xRot = x2;
		sleekTail.yRot = plumeTail.yRot = y0;
		sleekTailMid.yRot = plumeTailMid.yRot = y1;
		sleekTailTip.yRot = plumeTailTip.yRot = y2;
	}
}
