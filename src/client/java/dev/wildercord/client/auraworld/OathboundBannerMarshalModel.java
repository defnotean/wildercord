package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.OathboundBannerMarshalRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/**
 * Articulated 1:1 model for Knight #4: Oathbound Banner-Marshal (Way of the Sovereign Banner).
 *
 * <p>Texture atlas: {@code 128x128} ({@code textures/entity/oathbound_banner_marshal/skin.png} and {@code glow.png}).
 * Every integer cube matches {@code BANNER_MARSHAL_UV_BOXES} in {@code tools/oathbound_knights_art.py}.</p>
 */
public final class OathboundBannerMarshalModel extends EntityModel<OathboundBannerMarshalRenderState> {
	private static final float HIPS = 12.0F;

	private final ModelPart hips;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tabardFront;
	private final ModelPart tabardBack;
	private final ModelPart tassetR;
	private final ModelPart tassetL;
	private final ModelPart bannerCloth;
	private final ModelPart bannerHalo;
	private final ModelPart right;
	private final ModelPart foreR;
	private final ModelPart sunblade;
	private final ModelPart left;
	private final ModelPart foreL;
	private final ModelPart aegisShield;
	private final ModelPart shieldSolarFlare;
	private final ModelPart legR;
	private final ModelPart shinR;
	private final ModelPart legL;
	private final ModelPart shinL;

	private final PoseBlend blender;

	public OathboundBannerMarshalModel(ModelPart root) {
		super(root);
		hips = root.getChild("hips");
		body = hips.getChild("body");
		head = body.getChild("head");
		tabardFront = body.getChild("tabardFront");
		tabardBack = body.getChild("tabardBack");
		tassetR = body.getChild("tassetR");
		tassetL = body.getChild("tassetL");
		ModelPart backpack = body.getChild("backpack");
		bannerCloth = backpack.getChild("bannerCloth");
		bannerHalo = backpack.getChild("bannerHalo");
		right = body.getChild("right");
		foreR = right.getChild("foreR");
		sunblade = foreR.getChild("sunblade");
		left = body.getChild("left");
		foreL = left.getChild("foreL");
		aegisShield = foreL.getChild("aegisShield");
		shieldSolarFlare = aegisShield.getChild("shieldSolarFlare");
		legR = hips.getChild("legR");
		shinR = legR.getChild("shinR");
		legL = hips.getChild("legL");
		shinL = legL.getChild("shinL");

		ModelPart[] posed = new ModelPart[]{
			hips, body, head, tabardFront, tabardBack, tassetR, tassetL, bannerCloth,
			right, foreR, sunblade, left, foreL, aegisShield,
			legR, shinR, legL, shinL
		};
		blender = new PoseBlend(posed);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var hips = root.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(0.0F, HIPS, 0.0F));

		// Ivory-gold cuirass over emerald surcoat, glowing sun-core, rondels, shoulder angel wings, Sovereign War-Banner
		var body = hips.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -12.0F, -3.0F, 10, 12, 6),
			PartPose.ZERO);
		body.addOrReplaceChild("chestCore",
			CubeListBuilder.create().texOffs(32, 18).addBox(-2.0F, -10.5F, -3.6F, 4, 4, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("rondelR",
			CubeListBuilder.create().texOffs(40, 38).addBox(-5.8F, -11.5F, -3.5F, 3, 3, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("rondelL",
			CubeListBuilder.create().texOffs(40, 38).mirror().addBox(2.8F, -11.5F, -3.5F, 3, 3, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("wingR",
			CubeListBuilder.create().texOffs(48, 38).addBox(0.0F, -7.0F, 0.0F, 0, 7, 6),
			PartPose.offsetAndRotation(-5.2F, -11.2F, 1.5F, 0.25F, -0.38F, -0.36F));
		body.addOrReplaceChild("wingL",
			CubeListBuilder.create().texOffs(48, 38).mirror().addBox(0.0F, -7.0F, 0.0F, 0, 7, 6),
			PartPose.offsetAndRotation(5.2F, -11.2F, 1.5F, 0.25F, 0.38F, 0.36F));

		body.addOrReplaceChild("tabardFront",
			CubeListBuilder.create().texOffs(42, 18).addBox(-3.0F, 0.0F, -0.5F, 6, 9, 1),
			PartPose.offset(0.0F, -1.5F, -3.2F));
		body.addOrReplaceChild("tabardBack",
			CubeListBuilder.create().texOffs(56, 18).addBox(-3.0F, 0.0F, -0.5F, 6, 9, 1),
			PartPose.offset(0.0F, -1.5F, 3.0F));
		body.addOrReplaceChild("tassetR",
			CubeListBuilder.create().texOffs(70, 18).addBox(-1.2F, 0.0F, -2.5F, 2, 7, 5),
			PartPose.offset(-5.0F, -1.5F, 0.0F));
		body.addOrReplaceChild("tassetL",
			CubeListBuilder.create().texOffs(70, 18).mirror().addBox(-0.8F, 0.0F, -2.5F, 2, 7, 5),
			PartPose.offset(5.0F, -1.5F, 0.0F));

		// Back-mounted Sovereign War-Banner on crossbar standard
		var backpack = body.addOrReplaceChild("backpack",
			CubeListBuilder.create().texOffs(64, 38).addBox(-3.0F, 0.0F, 0.0F, 6, 7, 2),
			PartPose.offset(0.0F, -10.5F, 3.0F));
		backpack.addOrReplaceChild("bannerPole",
			CubeListBuilder.create().texOffs(0, 66).addBox(-1.0F, -24.0F, 1.0F, 2, 28, 2),
			PartPose.ZERO);
		backpack.addOrReplaceChild("bannerCrossbar",
			CubeListBuilder.create().texOffs(8, 66).addBox(-6.0F, -22.5F, 1.0F, 12, 2, 2),
			PartPose.ZERO);
		var bannerCloth = backpack.addOrReplaceChild("bannerCloth",
			CubeListBuilder.create().texOffs(8, 72).addBox(-5.0F, 0.0F, -0.5F, 10, 12, 1),
			PartPose.offset(0.0F, -20.5F, 2.2F));
		bannerCloth.addOrReplaceChild("bannerTipR",
			CubeListBuilder.create().texOffs(30, 72).addBox(-5.0F, 12.0F, -0.5F, 4, 3, 1),
			PartPose.ZERO);
		bannerCloth.addOrReplaceChild("bannerTipL",
			CubeListBuilder.create().texOffs(30, 72).mirror().addBox(1.0F, 12.0F, -0.5F, 4, 3, 1),
			PartPose.ZERO);
		backpack.addOrReplaceChild("bannerHalo",
			CubeListBuilder.create().texOffs(40, 66).addBox(-6.0F, -22.0F, 2.5F, 12, 14, 0),
			PartPose.ZERO);

		// Greathelm crowned with golden laurel wreath & glowing T-visor
		var head = body.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.5F, -4.0F, 8, 8, 8),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		head.addOrReplaceChild("laurelBand",
			CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9, 2, 9, new CubeDeformation(0.15F)),
			PartPose.ZERO);
		head.addOrReplaceChild("laurelCrest",
			CubeListBuilder.create().texOffs(68, 0).addBox(-1.5F, -11.0F, -4.5F, 3, 3, 2),
			PartPose.ZERO);

		// Right arm + Sovereign Sunblade (gold guard & glowing broadsword along -Y)
		var right = body.addOrReplaceChild("right",
			CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, -2.0F, -2.0F, 4, 6, 4),
			PartPose.offset(-6.5F, -10.5F, 0.0F));
		right.addOrReplaceChild("pauldronR",
			CubeListBuilder.create().texOffs(16, 38).addBox(-3.5F, -3.2F, -3.0F, 6, 5, 6),
			PartPose.ZERO);
		var foreR = right.addOrReplaceChild("foreR",
			CubeListBuilder.create().texOffs(0, 52).addBox(-2.0F, 0.0F, -2.0F, 4, 7, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		var sunblade = foreR.addOrReplaceChild("sunblade",
			CubeListBuilder.create().texOffs(0, 98).addBox(-1.0F, -3.0F, -1.0F, 2, 6, 2),
			PartPose.offset(0.0F, 6.0F, -0.5F));
		sunblade.addOrReplaceChild("swordGuard",
			CubeListBuilder.create().texOffs(8, 98).addBox(-3.5F, -5.0F, -1.0F, 7, 2, 2),
			PartPose.ZERO);
		sunblade.addOrReplaceChild("swordBlade",
			CubeListBuilder.create().texOffs(26, 98).addBox(-1.5F, -21.0F, -0.5F, 3, 16, 1),
			PartPose.ZERO);
		sunblade.addOrReplaceChild("swordTip",
			CubeListBuilder.create().texOffs(34, 98).addBox(-1.0F, -24.0F, -0.5F, 2, 3, 1),
			PartPose.ZERO);

		// Left arm + Octagonal Golden Sun-Crest Shield + Solar Aegis Flare
		var left = body.addOrReplaceChild("left",
			CubeListBuilder.create().texOffs(0, 38).mirror().addBox(-2.0F, -2.0F, -2.0F, 4, 6, 4),
			PartPose.offset(6.5F, -10.5F, 0.0F));
		left.addOrReplaceChild("pauldronL",
			CubeListBuilder.create().texOffs(16, 38).mirror().addBox(-2.5F, -3.2F, -3.0F, 6, 5, 6),
			PartPose.ZERO);
		var foreL = left.addOrReplaceChild("foreL",
			CubeListBuilder.create().texOffs(0, 52).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 7, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		var aegisShield = foreL.addOrReplaceChild("aegisShield",
			CubeListBuilder.create().texOffs(64, 66).addBox(2.2F, -6.5F, -5.5F, 0, 13, 11),
			PartPose.offset(0.0F, 3.0F, 0.0F));
		aegisShield.addOrReplaceChild("shieldBoss",
			CubeListBuilder.create().texOffs(90, 66).addBox(2.3F, -4.5F, -4.5F, 1, 9, 9),
			PartPose.ZERO);
		aegisShield.addOrReplaceChild("shieldSolarFlare",
			CubeListBuilder.create().texOffs(90, 86).addBox(3.5F, -7.0F, -7.0F, 0, 14, 14),
			PartPose.ZERO);

		// Armored legs, knee cops, greaves
		var legR = hips.addOrReplaceChild("legR",
			CubeListBuilder.create().texOffs(16, 52).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offset(-2.4F, 0.0F, 0.0F));
		var shinR = legR.addOrReplaceChild("shinR",
			CubeListBuilder.create().texOffs(32, 52).addBox(-2.2F, 0.0F, -2.5F, 4, 6, 5),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		shinR.addOrReplaceChild("kneeR",
			CubeListBuilder.create().texOffs(50, 52).addBox(-2.3F, -1.2F, -3.0F, 4, 3, 2),
			PartPose.ZERO);

		var legL = hips.addOrReplaceChild("legL",
			CubeListBuilder.create().texOffs(16, 52).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offset(2.4F, 0.0F, 0.0F));
		var shinL = legL.addOrReplaceChild("shinL",
			CubeListBuilder.create().texOffs(32, 52).mirror().addBox(-1.8F, 0.0F, -2.5F, 4, 6, 5),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		shinL.addOrReplaceChild("kneeL",
			CubeListBuilder.create().texOffs(50, 52).mirror().addBox(-1.7F, -1.2F, -3.0F, 4, 3, 2),
			PartPose.ZERO);

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OathboundBannerMarshalRenderState s) {
		super.setupAnim(s);
		pose(s, s.move, s.elapsed, s.tell, s.guardCountered, s.guardCounterElapsed);
		if (s.blend < 1.0F && s.prevMove >= 0) {
			blender.store();
			root().resetPose();
			pose(s, s.prevMove, s.prevElapsed, s.prevTell, false, 0.0F);
			float k = s.blend * s.blend * (3.0F - 2.0F * s.blend);
			blender.mix(k);
		}
		applyAuraVisuals(s);
		hurt(s);
		die(s);
		ground();
	}

	private void applyAuraVisuals(OathboundBannerMarshalRenderState s) {
		boolean showBanner = s.deathTime <= 0
			&& (s.move == OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD
				|| s.move == OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT);
		bannerHalo.visible = showBanner;
		if (showBanner) {
			float pulse = 0.94F + 0.08F * Mth.sin(s.ageInTicks * 0.35F);
			bannerHalo.xScale = pulse;
			bannerHalo.yScale = pulse;
		}
		boolean showFlare = s.deathTime <= 0
			&& s.move == OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD
			&& !s.guardCountered;
		shieldSolarFlare.visible = showFlare;
		if (showFlare) {
			float pulse = 0.90F + 0.12F * Mth.sin(s.ageInTicks * 0.4F);
			shieldSolarFlare.yScale = pulse;
			shieldSolarFlare.zScale = pulse;
		}
	}

	private static float ramp(float t, float from, float ticks) {
		float k = Mth.clamp((t - from) / Math.max(1.0F, ticks), 0.0F, 1.0F);
		return 1.0F - (1.0F - k) * (1.0F - k);
	}

	private static float smooth(float t, float from, float to) {
		float k = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private void pose(OathboundBannerMarshalRenderState s, int move, float el, int tellTicks,
			boolean countered, float counterEl) {
		float t = s.ageInTicks;
		head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.55F;
		head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.4F;
		body.xRot = Mth.sin(t * 0.06F) * 0.015F;

		float walk = Math.min(1.0F, s.walkAnimationSpeed);
		float step = s.walkAnimationPos * 0.55F;
		legL.xRot = Mth.cos(step) * walk * 0.56F;
		legR.xRot = -Mth.cos(step) * walk * 0.56F;
		shinL.xRot = Math.max(0.0F, -Mth.sin(step)) * walk * 0.80F;
		shinR.xRot = Math.max(0.0F, Mth.sin(step)) * walk * 0.80F;
		tassetL.zRot = -0.18F - Math.max(0.0F, -Mth.cos(step)) * walk * 0.12F;
		tassetR.zRot = 0.18F + Math.max(0.0F, Mth.cos(step)) * walk * 0.12F;
		tabardFront.xRot = -0.05F - Math.abs(Mth.cos(step)) * walk * 0.22F;
		tabardBack.xRot = 0.06F + Math.abs(Mth.cos(step)) * walk * 0.22F;
		bannerCloth.xRot = 0.08F + walk * 0.14F + Mth.sin(t * 0.10F) * 0.04F;

		// Panel 1 (SOVEREIGN MARCH): Sunblade angled proudly forward-down at right, Sun-shield angled outward on left
		right.xRot = -0.45F - Mth.cos(step) * walk * 0.20F;
		right.yRot = -0.14F;
		right.zRot = -0.12F;
		foreR.xRot = -0.35F;
		sunblade.xRot = 1.85F;
		sunblade.yRot = 0.0F;
		sunblade.zRot = 0.0F;

		left.xRot = -0.42F + Mth.cos(step) * walk * 0.18F;
		left.yRot = 0.42F;
		left.zRot = -0.10F;
		foreL.xRot = -0.48F;
		aegisShield.xRot = 0.55F;
		aegisShield.yRot = 0.0F;
		aegisShield.zRot = 0.28F;

		float tell = Math.max(1.0F, tellTicks);
		switch (move) {
			case OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD -> {
				// Panel 2: DECREE OF THE STANDARD — tall commanding stance, glowing Sunblade thrust straight skyward!
				float in = smooth(el, 0.0F, 5.0F);
				float out = smooth(el, OathboundBannerMarshalRules.DECREE_DURATION - 5.0F,
					OathboundBannerMarshalRules.DECREE_DURATION);
				float k = in * (1.0F - out);

				legL.zRot = -0.18F * k;
				legR.zRot = 0.18F * k;
				body.xRot = Mth.lerp(k, body.xRot, -0.10F);
				right.xRot = Mth.lerp(k, right.xRot, -2.45F);
				right.yRot = Mth.lerp(k, right.yRot, 0.0F);
				right.zRot = Mth.lerp(k, right.zRot, -0.22F);
				foreR.xRot = Mth.lerp(k, foreR.xRot, -0.35F);
				sunblade.xRot = Mth.lerp(k, sunblade.xRot, 3.00F);

				left.xRot = Mth.lerp(k, left.xRot, -0.35F);
				left.yRot = Mth.lerp(k, left.yRot, 0.35F);
				aegisShield.xRot = Mth.lerp(k, aegisShield.xRot, 0.45F);
				aegisShield.zRot = Mth.lerp(k, aegisShield.zRot, 0.25F);
				bannerCloth.xRot = 0.18F + 0.18F * k;
			}
			case OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD -> {
				if (!countered) {
					// Panel 3: SOLAR AEGIS GUARD — left shield locked forward across center, Sunblade cocked right!
					float in = smooth(el, 0.0F, 4.0F);
					bend(0.24F * in);
					legL.zRot = -0.18F * in;
					legR.zRot = 0.18F * in;
					body.yRot = Mth.lerp(in, body.yRot, 0.24F);

					// Positive yRot swings left forearm inward (-X) to front chest center
					left.xRot = Mth.lerp(in, left.xRot, -0.78F);
					left.yRot = Mth.lerp(in, left.yRot, 0.85F);
					left.zRot = Mth.lerp(in, left.zRot, 0.0F);
					foreL.xRot = Mth.lerp(in, foreL.xRot, -0.58F);
					aegisShield.xRot = Mth.lerp(in, aegisShield.xRot, 0.85F);
					aegisShield.yRot = 0.0F;
					aegisShield.zRot = Mth.lerp(in, aegisShield.zRot, 0.45F);

					right.xRot = Mth.lerp(in, right.xRot, -0.38F);
					right.yRot = Mth.lerp(in, right.yRot, -0.25F);
					right.zRot = Mth.lerp(in, right.zRot, -0.28F);
					foreR.xRot = Mth.lerp(in, foreR.xRot, -0.52F);
					sunblade.xRot = Mth.lerp(in, sunblade.xRot, 1.85F);
				} else {
					// Counter-thrust!
					float thrust = ramp(counterEl, 0.0F, 4.0F);
					float rec = smooth(counterEl, 6.0F, OathboundBannerMarshalRules.GUARD_COUNTER_TICKS);
					body.yRot = Mth.lerp(rec, Mth.lerp(thrust, 0.24F, -0.55F), 0.0F);
					right.xRot = Mth.lerp(rec, -1.45F, -0.45F);
					foreR.xRot = Mth.lerp(rec, -0.15F, -0.35F);
					sunblade.xRot = Mth.lerp(rec, 1.57F, 1.85F);
					left.yRot = Mth.lerp(rec, 0.25F, 0.42F);
				}
			}
			case OathboundBannerMarshalRules.RADIANT_SUNBLADE_CLEAVE -> {
				// Panel 4: RADIANT SUNBLADE CLEAVE — high-right windup -> sweeping horizontal low-left slash
				float wind = ramp(el, 0.0F, tell);
				float swing = ramp(el, tell, 4.0F);
				float rec = smooth(el, tell + 6.0F, OathboundBannerMarshalRules.CLEAVE_DURATION);

				body.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.55F * wind, 0.65F), 0.0F);
				body.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.08F * wind, 0.22F), 0.0F);
				bend(0.28F * Math.max(wind * 0.4F, swing) * (1.0F - rec));

				right.xRot = Mth.lerp(rec, Mth.lerp(swing, -1.65F * wind, -0.75F), -0.45F);
				right.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.35F * wind, -0.55F), -0.14F);
				right.zRot = Mth.lerp(rec, Mth.lerp(swing, -0.35F * wind, -0.42F), -0.12F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.45F, -0.35F), -0.35F);
				sunblade.xRot = Mth.lerp(rec, Mth.lerp(swing, 2.25F, 2.15F), 1.85F);
				sunblade.zRot = Mth.lerp(rec, Mth.lerp(swing, 0.15F * wind, -0.55F), 0.0F);
			}
			case OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT -> {
				// Panels 5 & 6: HEAVEN'S LANCE WINDUP -> SOVEREIGN JUDGMENT DOWNWARD THRUST
				float wind = ramp(el, 0.0F, tell - 2.0F);
				float slam = ramp(el, tell - 2.0F, 3.0F);
				float rec = smooth(el, tell + 8.0F, OathboundBannerMarshalRules.JUDGMENT_DURATION);

				legL.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.15F * wind, -0.60F), 0.0F);
				shinL.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.25F * wind, 0.72F), 0.0F);
				legR.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.15F * wind, 0.56F), 0.0F);

				body.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.14F * wind, 0.30F), 0.0F);

				// Panel 5: Sword hoisted high above golden crown -> Panel 6: Driven forward-down in command lunge
				right.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.45F, -2.55F), -0.65F), -0.45F);
				right.yRot = Mth.lerp(rec, Mth.lerp(slam, 0.0F, -0.15F), -0.14F);
				right.zRot = Mth.lerp(rec, Mth.lerp(slam, -0.25F * wind, -0.18F), -0.12F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.30F, -0.25F), -0.35F);
				sunblade.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, 1.85F, 2.95F), 2.95F), 1.85F);

				left.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.35F, -0.75F), -0.42F);
				left.yRot = Mth.lerp(rec, 0.45F, 0.42F);
				bannerCloth.xRot = 0.08F + 0.25F * Math.max(wind, slam) * (1.0F - rec);
			}
			case OathboundBannerMarshalRules.STAGGERED -> {
				float sway = Mth.sin(el * 0.35F) * 0.08F;
				body.xRot = -0.16F;
				body.zRot = sway;
				head.xRot = 0.30F;
				left.xRot = -0.25F;
				left.yRot = -0.40F;
				left.zRot = -0.78F;
				right.xRot = 0.42F;
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

	private void hurt(OathboundBannerMarshalRenderState s) {
		if (s.hurt <= 0.0F || s.deathTime > 0) {
			return;
		}
		float k = s.hurt * s.hurt;
		body.xRot -= 0.14F * k;
		head.xRot -= 0.09F * k;
		bannerCloth.xRot += 0.20F * k;
	}

	private void die(OathboundBannerMarshalRenderState s) {
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
	}
}
