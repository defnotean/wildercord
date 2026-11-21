package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.OathboundBastionRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/**
 * Articulated 1:1 model for Knight #1: Oathbound Bastion (Way of the Bulwark).
 *
 * <p>Texture atlas: {@code 128x128} ({@code textures/entity/oathbound_bastion/skin.png} and {@code glow.png}).
 * Every piece uses non-overlapping integer-dimension box UVs and {@link PoseBlend} for smooth transitions
 * across all six keyframe moves from the animation storyboard.</p>
 */
public final class OathboundBastionModel extends EntityModel<OathboundBastionRenderState> {
	private static final float HIPS = 12.0F;

	private final ModelPart hips;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tabardFront;
	private final ModelPart tabardBack;
	private final ModelPart tassetR;
	private final ModelPart tassetL;
	private final ModelPart cape;
	private final ModelPart right;
	private final ModelPart foreR;
	private final ModelPart mace;
	private final ModelPart left;
	private final ModelPart foreL;
	private final ModelPart shield;
	private final ModelPart shieldAuraInner;
	private final ModelPart shieldAuraOuter;
	private final ModelPart legR;
	private final ModelPart shinR;
	private final ModelPart legL;
	private final ModelPart shinL;

	private final ModelPart[] posed;
	private final PoseBlend blender;

	public OathboundBastionModel(ModelPart root) {
		super(root);
		hips = root.getChild("hips");
		body = hips.getChild("body");
		head = body.getChild("head");
		tabardFront = body.getChild("tabardFront");
		tabardBack = body.getChild("tabardBack");
		tassetR = body.getChild("tassetR");
		tassetL = body.getChild("tassetL");
		cape = body.getChild("cape");
		right = body.getChild("right");
		foreR = right.getChild("foreR");
		mace = foreR.getChild("mace");
		left = body.getChild("left");
		foreL = left.getChild("foreL");
		shield = foreL.getChild("shield");
		shieldAuraInner = shield.getChild("shieldAuraInner");
		shieldAuraOuter = shield.getChild("shieldAuraOuter");
		legR = hips.getChild("legR");
		shinR = legR.getChild("shinR");
		legL = hips.getChild("legL");
		shinL = legL.getChild("shinL");

		posed = new ModelPart[]{
			hips, body, head, tabardFront, tabardBack, tassetR, tassetL, cape,
			right, foreR, mace, left, foreL, shield, legR, shinR, legL, shinL
		};
		blender = new PoseBlend(posed);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var hips = root.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(0.0F, HIPS, 0.0F));

		// Torso, surcoat, fur mantle, rondels, belt buckle, tabards, tassets, cape
		var body = hips.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -12.0F, -3.0F, 10, 12, 6),
			PartPose.ZERO);
		body.addOrReplaceChild("furFront",
			CubeListBuilder.create().texOffs(68, 6).addBox(-6.0F, -12.6F, -4.0F, 12, 3, 8),
			PartPose.ZERO);
		body.addOrReplaceChild("furBack",
			CubeListBuilder.create().texOffs(32, 18).addBox(-5.5F, -13.2F, 1.5F, 11, 4, 3),
			PartPose.ZERO);
		body.addOrReplaceChild("rondelR",
			CubeListBuilder.create().texOffs(60, 18).addBox(-4.3F, -10.2F, -3.6F, 2, 2, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("rondelL",
			CubeListBuilder.create().texOffs(60, 18).mirror().addBox(2.3F, -10.2F, -3.6F, 2, 2, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("beltBuckle",
			CubeListBuilder.create().texOffs(64, 37).addBox(-2.0F, -3.5F, -3.6F, 4, 3, 1),
			PartPose.ZERO);

		body.addOrReplaceChild("tabardFront",
			CubeListBuilder.create().texOffs(0, 37).addBox(-3.0F, 0.0F, -0.5F, 6, 10, 1),
			PartPose.offset(0.0F, -1.0F, -3.2F));
		body.addOrReplaceChild("tabardBack",
			CubeListBuilder.create().texOffs(0, 37).addBox(-3.0F, 0.0F, -0.5F, 6, 10, 1),
			PartPose.offset(0.0F, -1.0F, 2.7F));
		body.addOrReplaceChild("tassetR",
			CubeListBuilder.create().texOffs(14, 37).addBox(-2.0F, 0.0F, -3.0F, 4, 7, 6),
			PartPose.offsetAndRotation(-5.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.18F));
		body.addOrReplaceChild("tassetL",
			CubeListBuilder.create().texOffs(14, 37).mirror().addBox(-2.0F, 0.0F, -3.0F, 4, 7, 6),
			PartPose.offsetAndRotation(5.0F, -1.5F, 0.0F, 0.0F, 0.0F, -0.18F));
		body.addOrReplaceChild("cape",
			CubeListBuilder.create().texOffs(36, 37).addBox(-6.0F, 0.0F, -0.5F, 12, 17, 1),
			PartPose.offsetAndRotation(0.0F, -11.5F, 3.2F, 0.08F, 0.0F, 0.0F));

		// Greathelm & 3D crenellated golden crown
		var head = body.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8, 9, 8),
			PartPose.offset(0.0F, -12.5F, 0.0F));
		head.addOrReplaceChild("crownBand",
			CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -10.5F, -4.5F, 9, 2, 9),
			PartPose.ZERO);
		head.addOrReplaceChild("crownPeakFront",
			CubeListBuilder.create().texOffs(68, 0).addBox(-1.0F, -13.2F, -4.6F, 2, 3, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("crownPeakFL",
			CubeListBuilder.create().texOffs(76, 0).addBox(2.5F, -12.3F, -4.5F, 2, 2, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("crownPeakFR",
			CubeListBuilder.create().texOffs(76, 0).addBox(-4.5F, -12.3F, -4.5F, 2, 2, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("crownPeakBL",
			CubeListBuilder.create().texOffs(76, 0).addBox(2.5F, -12.3F, 2.5F, 2, 2, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("crownPeakBR",
			CubeListBuilder.create().texOffs(76, 0).addBox(-4.5F, -12.3F, 2.5F, 2, 2, 2),
			PartPose.ZERO);

		// Right arm + Sun-Flanged Greatmace (pointing along -Z out of fist)
		var right = body.addOrReplaceChild("right",
			CubeListBuilder.create().texOffs(0, 56).addBox(-2.5F, -1.5F, -2.5F, 5, 7, 5),
			PartPose.offset(-7.6F, -10.0F, 0.0F));
		right.addOrReplaceChild("pauldronR",
			CubeListBuilder.create().texOffs(68, 18).addBox(-3.8F, -3.5F, -3.5F, 7, 6, 7),
			PartPose.ZERO);
		right.addOrReplaceChild("pauldronRidgeR",
			CubeListBuilder.create().texOffs(96, 18).addBox(-1.5F, -4.9F, -3.5F, 3, 2, 7),
			PartPose.ZERO);
		var foreR = right.addOrReplaceChild("foreR",
			CubeListBuilder.create().texOffs(20, 56).addBox(-2.5F, 0.0F, -2.5F, 5, 7, 5, new CubeDeformation(-0.04F)),
			PartPose.offset(0.0F, 5.5F, 0.0F));
		var mace = foreR.addOrReplaceChild("mace",
			CubeListBuilder.create()
				.texOffs(0, 70).addBox(-1.0F, -1.0F, -10.0F, 2, 2, 16)
				.texOffs(110, 70).addBox(-1.5F, -1.5F, 5.5F, 3, 3, 2)
				.texOffs(36, 70).addBox(-2.0F, -2.0F, -15.5F, 4, 4, 6)
				.texOffs(56, 70).addBox(-4.0F, -1.0F, -15.5F, 8, 2, 6)
				.texOffs(84, 70).addBox(-1.0F, -4.0F, -15.5F, 2, 8, 6)
				.texOffs(100, 70).addBox(-1.5F, -1.5F, -17.5F, 3, 3, 2),
			PartPose.offset(-0.3F, 5.6F, -0.5F));
		// Suppress unused local warning while keeping clean hierarchy
		mace.getClass();

		// Left arm + Tower Greatshield (front emblem facing +X so bringing arm across chest aims shield straight -Z)
		var left = body.addOrReplaceChild("left",
			CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2.5F, -1.5F, -2.5F, 5, 7, 5),
			PartPose.offset(7.6F, -10.0F, 0.0F));
		left.addOrReplaceChild("pauldronL",
			CubeListBuilder.create().texOffs(68, 18).mirror().addBox(-3.2F, -3.5F, -3.5F, 7, 6, 7),
			PartPose.ZERO);
		left.addOrReplaceChild("pauldronRidgeL",
			CubeListBuilder.create().texOffs(96, 18).addBox(-1.5F, -4.9F, -3.5F, 3, 2, 7),
			PartPose.ZERO);
		var foreL = left.addOrReplaceChild("foreL",
			CubeListBuilder.create().texOffs(20, 56).mirror().addBox(-2.5F, 0.0F, -2.5F, 5, 7, 5, new CubeDeformation(-0.04F)),
			PartPose.offset(0.0F, 5.5F, 0.0F));
		var shield = foreL.addOrReplaceChild("shield",
			CubeListBuilder.create()
				.texOffs(0, 92).addBox(0.0F, -12.0F, -6.0F, 2, 16, 12)
				.texOffs(28, 92).addBox(0.0F, 4.0F, -5.0F, 2, 3, 10)
				.texOffs(28, 106).addBox(0.0F, 7.0F, -3.0F, 2, 3, 6)
				.texOffs(44, 106).addBox(0.0F, 10.0F, -1.5F, 2, 2, 3)
				.texOffs(54, 92).addBox(1.8F, -4.5F, -4.0F, 1, 9, 8),
			PartPose.offset(2.6F, 2.2F, -0.5F));
		shield.addOrReplaceChild("shieldAuraInner",
			CubeListBuilder.create().texOffs(74, 92).addBox(0.0F, -8.0F, -8.0F, 0, 16, 16),
			PartPose.offset(4.2F, -2.0F, 0.0F));
		shield.addOrReplaceChild("shieldAuraOuter",
			CubeListBuilder.create().texOffs(74, 92).addBox(0.0F, -8.0F, -8.0F, 0, 16, 16),
			PartPose.offset(6.4F, -2.0F, 0.0F));

		// Jointed Armored Legs (thighs, shins, kneecaps, sabaton toes)
		for (int sign : new int[]{-1, 1}) {
			CubeListBuilder thighCube = CubeListBuilder.create().texOffs(40, 56);
			if (sign == 1) thighCube.mirror();
			var leg = hips.addOrReplaceChild(sign == -1 ? "legR" : "legL",
				thighCube.addBox(-2.5F, 0.0F, -2.5F, 5, 6, 5),
				PartPose.offset(sign * 2.7F, 0.0F, 0.0F));
			CubeListBuilder shinCube = CubeListBuilder.create().texOffs(60, 56);
			if (sign == 1) shinCube.mirror();
			shinCube.addBox(-2.5F, 0.0F, -2.5F, 5, 6, 5, new CubeDeformation(-0.04F))
				.texOffs(80, 56).addBox(-2.0F, -1.5F, -3.2F, 4, 4, 2)
				.texOffs(92, 56).addBox(-2.5F, 4.0F, -4.0F, 5, 2, 2);
			leg.addOrReplaceChild(sign == -1 ? "shinR" : "shinL", shinCube, PartPose.offset(0.0F, 6.0F, 0.0F));
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OathboundBastionRenderState s) {
		super.setupAnim(s);
		pose(s, s.move, s.elapsed, s.tell);
		if (s.blend < 1.0F && s.prevMove >= 0) {
			blender.store();
			root().resetPose();
			pose(s, s.prevMove, s.prevElapsed, s.prevTell);
			float k = s.blend * s.blend * (3.0F - 2.0F * s.blend);
			blender.mix(k);
		}
		applyBarrierVisuals(s);
		hurt(s);
		die(s);
		ground();
	}

	private void applyBarrierVisuals(OathboundBastionRenderState s) {
		boolean show = s.deathTime <= 0
			&& (s.barrier >= 0.5F || s.move == OathboundBastionRules.PHALANX_BRACE || s.move == OathboundBastionRules.SHIELD_RAM);
		shieldAuraInner.visible = show;
		shieldAuraOuter.visible = show;
		if (!show) {
			return;
		}
		float pulse = 0.96F + 0.06F * Mth.sin(s.ageInTicks * 0.25F);
		float scaleBase = Mth.clamp(0.72F + s.barrier * 0.35F, 0.7F, 1.15F);
		shieldAuraInner.yScale = scaleBase * pulse;
		shieldAuraInner.zScale = scaleBase * pulse;
		shieldAuraOuter.yScale = scaleBase * 1.16F * (1.03F - 0.05F * Mth.sin(s.ageInTicks * 0.25F));
		shieldAuraOuter.zScale = scaleBase * 1.16F * (1.03F - 0.05F * Mth.sin(s.ageInTicks * 0.25F));
		shieldAuraOuter.x = 6.2F + 0.4F * Mth.sin(s.ageInTicks * 0.18F);
	}

	private static float ramp(float t, float from, float ticks) {
		float k = Mth.clamp((t - from) / Math.max(1.0F, ticks), 0.0F, 1.0F);
		return 1.0F - (1.0F - k) * (1.0F - k);
	}

	private static float smooth(float t, float from, float to) {
		float k = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private static float stop(float el, float at) {
		return el < at ? el : el < at + 2.0F ? at : el - 2.0F;
	}

	private void pose(OathboundBastionRenderState s, int move, float el, int tellTicks) {
		float t = s.ageInTicks;
		// Subtle gaze + breathing
		head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.55F;
		head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.4F;
		body.xRot = Mth.sin(t * 0.06F) * 0.015F;

		// Default Panel 1 (HEAVY GUARDED MARCH) stance
		float walk = Math.min(1.0F, s.walkAnimationSpeed);
		float step = s.walkAnimationPos * 0.55F;
		legL.xRot = Mth.cos(step) * walk * 0.58F;
		legR.xRot = -Mth.cos(step) * walk * 0.58F;
		shinL.xRot = Math.max(0.0F, -Mth.sin(step)) * walk * 0.85F;
		shinR.xRot = Math.max(0.0F, Mth.sin(step)) * walk * 0.85F;
		tassetL.zRot = -0.18F - Math.max(0.0F, -Mth.cos(step)) * walk * 0.14F;
		tassetR.zRot = 0.18F + Math.max(0.0F, Mth.cos(step)) * walk * 0.14F;
		tabardFront.xRot = -0.05F - Math.abs(Mth.cos(step)) * walk * 0.25F;
		tabardBack.xRot = 0.06F + Math.abs(Mth.cos(step)) * walk * 0.28F;
		cape.xRot = 0.09F + walk * 0.22F + Mth.sin(t * 0.08F) * 0.03F;

		// Idle/March weapon & shield pose: greatmace low-diagonal right, tower shield angled forward on left forearm
		right.xRot = 0.25F - Mth.cos(step) * walk * 0.35F;
		right.zRot = 0.16F;
		foreR.xRot = -0.35F;
		mace.xRot = 0.55F;
		mace.yRot = -0.15F;

		left.xRot = -0.48F + Mth.cos(step) * walk * 0.22F;
		left.yRot = 1.05F;
		left.zRot = -0.14F;
		foreL.xRot = -0.48F;
		shield.xRot = 0.0F;
		shield.yRot = 0.38F;
		shield.zRot = 0.0F;

		float tell = Math.max(1.0F, tellTicks);
		switch (move) {
			case OathboundBastionRules.PHALANX_BRACE -> {
				// Panel 2: AEGIS PHALANX BRACE — deep crouched fortress stance, vertical shield dead center, mace cocked back right
				float in = smooth(el, 0.0F, 5.0F);
				float out = smooth(el, OathboundBastionRules.PHALANX_DURATION - 5.0F, OathboundBastionRules.PHALANX_DURATION);
				float k = in * (1.0F - out);
				bend(0.55F * k);
				legL.zRot = -0.22F * k;
				legR.zRot = 0.22F * k;
				body.xRot = Mth.lerp(k, body.xRot, 0.18F);
				head.xRot = Mth.lerp(k, head.xRot, -0.08F);

				// Giant shield planted straight vertically across front center
				left.xRot = Mth.lerp(k, left.xRot, -0.82F);
				left.yRot = Mth.lerp(k, left.yRot, 1.52F);
				left.zRot = Mth.lerp(k, left.zRot, -0.04F);
				foreL.xRot = Mth.lerp(k, foreL.xRot, -0.45F);
				shield.xRot = Mth.lerp(k, shield.xRot, -0.10F);
				shield.yRot = Mth.lerp(k, shield.yRot, 0.22F);

				// Greatmace cocked back behind right hip ready to riposte
				right.xRot = Mth.lerp(k, right.xRot, 0.55F);
				right.zRot = Mth.lerp(k, right.zRot, 0.28F);
				foreR.xRot = Mth.lerp(k, foreR.xRot, -0.45F);
				mace.xRot = Mth.lerp(k, mace.xRot, 0.65F);
				cape.xRot = Mth.lerp(k, cape.xRot, 0.35F);
			}
			case OathboundBastionRules.SHIELD_RAM -> {
				// Panel 3: SHIELD-RAM STAMPEDE — 30 deg forward pitch, shield locked flat straight ahead, mace tucked right, cape flying back
				float wind = ramp(el, 0.0F, tell);
				float dash = ramp(el, tell, 3.0F);
				float sprint = el * 0.95F;

				body.xRot = 0.15F * wind * (1.0F - dash) + 0.52F * dash;
				head.xRot = -0.35F * dash;

				left.xRot = Mth.lerp(dash, -0.55F, -1.25F);
				left.yRot = Mth.lerp(dash, 0.95F, 1.48F);
				left.zRot = 0.0F;
				foreL.xRot = Mth.lerp(dash, -0.55F, -0.25F);
				shield.yRot = 0.10F * dash;

				// Right arm holds glowing mace horizontally tucked at right waist
				right.xRot = Mth.lerp(dash, 0.35F, 0.55F);
				right.zRot = Mth.lerp(dash, 0.18F, 0.25F);
				foreR.xRot = Mth.lerp(dash, -0.45F, -0.95F);
				mace.xRot = Mth.lerp(dash, 0.45F, 0.08F);

				legL.xRot = Mth.lerp(dash, -0.35F, Mth.cos(sprint) * 0.75F);
				legR.xRot = Mth.lerp(dash, 0.45F, -Mth.cos(sprint) * 0.75F);
				shinL.xRot = Mth.lerp(dash, 0.45F, Math.max(0.0F, -Mth.sin(sprint)) * 1.15F);
				shinR.xRot = Mth.lerp(dash, 0.20F, Math.max(0.0F, Mth.sin(sprint)) * 1.15F);
				cape.xRot = Mth.lerp(dash, 0.25F, 0.94F + Mth.sin(t * 0.7F) * 0.08F);
				tabardFront.xRot = -0.35F * dash;
				tabardBack.xRot = 0.55F * dash;
			}
			case OathboundBastionRules.ANVIL_SLAM -> {
				// Panel 4 (Windup): overhead mace clear of crown + shield counter-balanced low left
				// Panel 5 (Impact): deep one-knee drop, BOTH arms center-front holding vertical mace slammed into ground, cape whipped up
				float wind = ramp(el, 0.0F, Math.max(2.0F, tell - 3.0F));
				float slamEl = stop(el, tell + 2.0F);
				float go = ramp(slamEl, tell - 1.5F, 3.0F);
				float rec = smooth(el, tell + 12.0F, tell + OathboundBastionRules.ANVIL_RECOVERY);
				float imp = go * (1.0F - rec * 0.35F);

				float windBodyX = -0.18F * wind;
				float impBodyX = 0.42F;
				body.xRot = Mth.lerp(go, windBodyX, Mth.lerp(rec, impBodyX, 0.18F));
				body.yRot = Mth.lerp(go, -0.18F * wind, 0.0F);
				head.xRot = Mth.lerp(go, -0.22F * wind, 0.18F * imp);

				// Right arm: hoisted high up & OUTSIDE crown (-zRot lifts overhead outward to -X) -> swept down-center holding vertical mace shaft
				float wRightX = Mth.lerp(wind, 0.25F, -2.90F);
				float wRightZ = Mth.lerp(wind, 0.16F, -0.38F);
				float wForeRX = Mth.lerp(wind, -0.35F, -0.22F);
				float wMaceX = Mth.lerp(wind, 0.55F, 0.95F);

				right.xRot = Mth.lerp(imp, wRightX, -1.25F);
				right.zRot = Mth.lerp(imp, wRightZ, -0.38F);
				right.yRot = Mth.lerp(imp, 0.0F, -0.38F);
				foreR.xRot = Mth.lerp(imp, wForeRX, -0.95F);
				mace.xRot = Mth.lerp(imp, wMaceX, 3.05F);
				mace.yRot = Mth.lerp(imp, -0.15F, 0.0F);

				// Left arm: counter-balanced open left on windup -> pulled inward to grip shaft at impact while shield stays clear
				float wLeftX = Mth.lerp(wind, -0.48F, 0.25F);
				float wLeftY = Mth.lerp(wind, 1.05F, 0.45F);
				float wLeftZ = Mth.lerp(wind, -0.14F, -0.48F);
				left.xRot = Mth.lerp(imp, wLeftX, -1.22F);
				left.yRot = Mth.lerp(imp, wLeftY, 0.38F);
				left.zRot = Mth.lerp(imp, wLeftZ, 0.35F);
				foreL.xRot = Mth.lerp(imp, -0.48F, -0.92F);
				shield.yRot = Mth.lerp(imp, 0.38F, 0.35F);

				// Legs: windup brace -> true one-knee drop (left foot planted forward, right knee grounded behind)
				float braceK = 0.20F * wind;
				float iLegLX = -1.25F, iShinLX = 1.25F;
				float iLegRX = 0.22F, iShinRX = 1.35F;
				legL.xRot = Mth.lerp(imp, -braceK, iLegLX);
				shinL.xRot = Mth.lerp(imp, 2.0F * braceK, iShinLX);
				legR.xRot = Mth.lerp(imp, -braceK, iLegRX);
				shinR.xRot = Mth.lerp(imp, 2.0F * braceK, iShinRX);

				tabardFront.xRot = Mth.lerp(imp, -0.15F * wind, -0.75F);
				tassetL.xRot = Mth.lerp(imp, 0.0F, -0.75F);
				tassetR.xRot = Mth.lerp(imp, 0.0F, 0.15F);
				// Cape whips sharply upward right on ground impact (Panel 5 signature!)
				float whip = smooth(el, tell - 1.0F, tell + 2.0F) * (1.0F - smooth(el, tell + 5.0F, tell + OathboundBastionRules.ANVIL_RECOVERY));
				cape.xRot = Mth.lerp(imp, 0.15F, 0.35F) + whip * 1.65F;
			}
			case OathboundBastionRules.SHIELD_BASH -> {
				// Panel 6: SHIELD-BASH SWEEP — torso turned cleanly forward as giant shield sweeps across front
				float wind = ramp(el, 0.0F, tell);
				float swing = ramp(el, tell, 4.0F);
				float rec = smooth(el, tell + 6.0F, OathboundBastionRules.BASH_DURATION);

				float yawWind = -0.55F * wind;
				float yawEnd = 0.28F;
				body.yRot = Mth.lerp(rec, Mth.lerp(swing, yawWind, yawEnd), 0.0F);
				body.xRot = 0.16F * (1.0F - rec);

				left.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.75F, -1.25F), -0.48F);
				left.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.45F, 1.35F), 1.05F);
				left.zRot = Mth.lerp(rec, Mth.lerp(swing, -0.45F, -0.42F), -0.14F);
				foreL.xRot = Mth.lerp(rec, -0.38F, -0.48F);
				shield.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.10F, 0.35F), 0.38F);

				right.xRot = Mth.lerp(rec, Mth.lerp(swing, 0.25F, 0.48F), 0.25F);
				right.zRot = Mth.lerp(rec, 0.32F, 0.16F);
				bend(0.32F * (1.0F - rec));
				legL.zRot = -0.18F * (1.0F - rec);
				legR.zRot = 0.18F * (1.0F - rec);
				cape.xRot = 0.25F + swing * 0.45F * (1.0F - rec);
			}
			case OathboundBastionRules.MACE_CLEAVE -> {
				float wind = ramp(el, 0.0F, tell);
				float swing = ramp(el, tell, 4.0F);
				float rec = smooth(el, tell + 6.0F, OathboundBastionRules.CLEAVE_DURATION);

				body.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.65F * wind, 0.78F), 0.0F);
				body.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.15F * wind, 0.35F), 0.0F);

				right.xRot = Mth.lerp(rec, Mth.lerp(swing, -2.15F * wind, 0.55F), 0.25F);
				right.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.55F * wind, 0.65F), 0.0F);
				right.zRot = Mth.lerp(rec, 0.42F, 0.16F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.55F, -0.25F), -0.35F);
				mace.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.25F, 0.65F), 0.55F);

				left.xRot = -0.55F;
				left.yRot = 0.95F;
				bend(0.28F * (1.0F - rec));
			}
			case OathboundBastionRules.STAGGERED -> {
				float sway = Mth.sin(el * 0.35F) * 0.08F;
				body.xRot = -0.16F;
				body.zRot = sway;
				head.xRot = 0.32F;
				head.zRot = -sway * 1.4F;
				// Shield thrown wide open on stagger
				left.xRot = -0.25F;
				left.yRot = -0.45F;
				left.zRot = -0.82F;
				foreL.xRot = -0.25F;
				right.xRot = 0.45F;
				right.zRot = 0.38F;
				bend(0.20F);
			}
			default -> {}
		}
	}

	private void bend(float k) {
		legL.xRot -= k;
		legR.xRot -= k;
		shinL.xRot += 2.0F * k;
		shinR.xRot += 2.0F * k;
	}

	private void ground() {
		hips.y = 24.0F - Math.max(reach(legL.xRot, shinL.xRot), reach(legR.xRot, shinR.xRot));
	}

	private static float reach(float thigh, float shin) {
		float a = thigh + shin;
		float knee = 6.0F * Mth.cos(thigh);
		float top = Math.max(0.0F, knee) + 2.5F * Math.abs(Mth.sin(thigh));
		float low = knee + Math.max(0.0F, 6.0F * Mth.cos(a)) + 2.5F * Math.abs(Mth.sin(a));
		return Math.max(top, low);
	}

	private void hurt(OathboundBastionRenderState s) {
		if (s.hurt <= 0.0F || s.deathTime > 0) {
			return;
		}
		float k = s.hurt * s.hurt;
		body.xRot -= 0.16F * k;
		head.xRot -= 0.10F * k;
		cape.xRot += 0.25F * k;
	}

	private void die(OathboundBastionRenderState s) {
		if (s.deathTime <= 0) {
			return;
		}
		float knees = smooth(s.deathTime, 0.0F, 8.0F);
		float fall = smooth(s.deathTime, 6.0F, 19.0F);
		legL.xRot = Mth.lerp(knees, legL.xRot, 0.0F);
		shinL.xRot = Mth.lerp(knees, shinL.xRot, Mth.HALF_PI);
		legR.xRot = Mth.lerp(knees, legR.xRot, 0.0F);
		shinR.xRot = Mth.lerp(knees, shinR.xRot, Mth.HALF_PI);
		body.xRot = Mth.lerp(fall, Mth.lerp(knees, body.xRot, 0.1F), 1.32F);
		body.yRot *= 1.0F - knees;
		body.zRot *= 1.0F - knees;
		head.xRot = Mth.lerp(fall, head.xRot, 0.35F);
		float arm = -body.xRot - 1.15F * fall;
		right.xRot = Mth.lerp(knees, right.xRot, arm);
		left.xRot = Mth.lerp(knees, left.xRot, arm);
		right.zRot = Mth.lerp(knees, right.zRot, 0.32F);
		left.zRot = Mth.lerp(knees, left.zRot, -0.32F);
		foreR.xRot = Mth.lerp(knees, foreR.xRot, -0.15F);
		foreL.xRot = Mth.lerp(knees, foreL.xRot, -0.15F);
		cape.xRot = Mth.lerp(fall, cape.xRot, 0.15F);
	}
}
