package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.OathboundBladeChampionRules;
import dev.wildercord.aura.world.OathboundTechnique;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/**
 * Articulated 1:1 model for Knight #2: Oathbound Blade-Champion (Way of the Blade).
 *
 * <p>Texture atlas: {@code 128x128} ({@code textures/entity/oathbound_blade_champion/skin.png} and {@code glow.png}).
 * Uses exact integer box dimensions matching {@code BLADE_CHAMPION_UV_BOXES} in {@code tools/oathbound_knights_art.py}
 * and {@link PoseBlend} for smooth transitions across all six keyframe moves from the animation storyboard.</p>
 */
public final class OathboundBladeChampionModel extends EntityModel<OathboundBladeChampionRenderState> {
	private static final float HIPS = 12.0F;

	private final ModelPart hips;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart plumeBase;
	private final ModelPart plumeTip;
	private final ModelPart tabardFront;
	private final ModelPart tabardBack;
	private final ModelPart tassetR;
	private final ModelPart tassetL;
	private final ModelPart capeYoke;
	private final ModelPart capeTailR;
	private final ModelPart capeTailL;
	private final ModelPart right;
	private final ModelPart foreR;
	private final ModelPart greatsword;
	private final ModelPart swordRiposteStar;
	private final ModelPart left;
	private final ModelPart foreL;
	private final ModelPart legR;
	private final ModelPart shinR;
	private final ModelPart legL;
	private final ModelPart shinL;

	private final PoseBlend blender;
	private final OathboundPose.Rig rig;

	public OathboundBladeChampionModel(ModelPart root) {
		super(root);
		hips = root.getChild("hips");
		body = hips.getChild("body");
		head = body.getChild("head");
		plumeBase = head.getChild("plumeBase");
		plumeTip = plumeBase.getChild("plumeTip");
		tabardFront = body.getChild("tabardFront");
		tabardBack = body.getChild("tabardBack");
		tassetR = body.getChild("tassetR");
		tassetL = body.getChild("tassetL");
		capeYoke = body.getChild("capeYoke");
		capeTailR = capeYoke.getChild("capeTailR");
		capeTailL = capeYoke.getChild("capeTailL");
		right = body.getChild("right");
		foreR = right.getChild("foreR");
		greatsword = foreR.getChild("greatsword");
		swordRiposteStar = greatsword.getChild("swordRiposteStar");
		left = body.getChild("left");
		foreL = left.getChild("foreL");
		legR = hips.getChild("legR");
		shinR = legR.getChild("shinR");
		legL = hips.getChild("legL");
		shinL = legL.getChild("shinL");

		ModelPart[] posed = new ModelPart[]{
			hips, body, head, plumeBase, plumeTip,
			tabardFront, tabardBack, tassetR, tassetL,
			capeYoke, capeTailR, capeTailL,
			right, foreR, greatsword, left, foreL,
			legR, shinR, legL, shinL
		};
		blender = new PoseBlend(posed);
		rig = new OathboundPose.Rig(body, head, right, foreR, left, foreL, greatsword, null, legL, shinL, legR, shinR);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var hips = root.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(0.0F, HIPS, 0.0F));

		// Fluted silver cuirass, diagonal crimson sash, medallion crest, gorget, tabards, tassets, split cape
		var body = hips.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 18).addBox(-4.5F, -12.0F, -2.5F, 9, 12, 5),
			PartPose.ZERO);
		body.addOrReplaceChild("chestCrest",
			CubeListBuilder.create().texOffs(28, 18).addBox(-3.0F, -11.0F, -3.2F, 6, 5, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("sashClasp",
			CubeListBuilder.create().texOffs(28, 24).addBox(-1.5F, -3.5F, -3.1F, 3, 3, 1),
			PartPose.ZERO);
		body.addOrReplaceChild("gorget",
			CubeListBuilder.create().texOffs(42, 18).addBox(-4.0F, -13.2F, -3.0F, 8, 2, 6),
			PartPose.ZERO);
		body.addOrReplaceChild("tabardFront",
			CubeListBuilder.create().texOffs(36, 26).addBox(-2.5F, 0.0F, -0.5F, 5, 8, 1),
			PartPose.offset(0.0F, -1.5F, -2.7F));
		body.addOrReplaceChild("tabardBack",
			CubeListBuilder.create().texOffs(48, 26).addBox(-3.0F, 0.0F, -0.5F, 6, 8, 1),
			PartPose.offset(0.0F, -1.5F, 2.5F));
		body.addOrReplaceChild("tassetR",
			CubeListBuilder.create().texOffs(62, 26).addBox(-1.2F, 0.0F, -2.5F, 2, 6, 5),
			PartPose.offset(-4.5F, -1.5F, 0.0F));
		body.addOrReplaceChild("tassetL",
			CubeListBuilder.create().texOffs(62, 26).mirror().addBox(-0.8F, 0.0F, -2.5F, 2, 6, 5),
			PartPose.offset(4.5F, -1.5F, 0.0F));

		var capeYoke = body.addOrReplaceChild("capeYoke",
			CubeListBuilder.create().texOffs(76, 20).addBox(-5.0F, 0.0F, -0.5F, 10, 6, 1),
			PartPose.offset(0.0F, -11.5F, 2.7F));
		var capeTailR = capeYoke.addOrReplaceChild("capeTailR",
			CubeListBuilder.create().texOffs(76, 28).addBox(-2.5F, 0.0F, -0.5F, 5, 13, 1),
			PartPose.offset(-2.8F, 5.5F, 0.0F));
		capeTailR.addOrReplaceChild("capeTipR",
			CubeListBuilder.create().texOffs(88, 28).addBox(-1.5F, 13.0F, -0.5F, 3, 3, 1),
			PartPose.ZERO);
		var capeTailL = capeYoke.addOrReplaceChild("capeTailL",
			CubeListBuilder.create().texOffs(76, 28).mirror().addBox(-2.5F, 0.0F, -0.5F, 5, 13, 1),
			PartPose.offset(2.8F, 5.5F, 0.0F));
		capeTailL.addOrReplaceChild("capeTipL",
			CubeListBuilder.create().texOffs(88, 28).mirror().addBox(-1.5F, 13.0F, -0.5F, 3, 3, 1),
			PartPose.ZERO);

		// Armet helm, pointed visor prow, flowing 2-segment crimson plume
		var head = body.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.5F, -4.0F, 8, 8, 8),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		head.addOrReplaceChild("visorPeak",
			CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -6.2F, -5.0F, 6, 3, 2),
			PartPose.ZERO);
		var plumeBase = head.addOrReplaceChild("plumeBase",
			CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -3.5F, -1.5F, 3, 4, 7),
			PartPose.offset(0.0F, -8.5F, 0.5F));
		plumeBase.addOrReplaceChild("plumeTip",
			CubeListBuilder.create().texOffs(68, 0).addBox(-1.5F, -1.0F, 0.0F, 3, 4, 7),
			PartPose.offset(0.0F, -1.5F, 5.2F));

		// Right arm + flared haute-piece pauldron + forearm + ornate Zweihänder greatsword
		var right = body.addOrReplaceChild("right",
			CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, -2.0F, -2.0F, 4, 6, 4),
			PartPose.offset(-6.0F, -10.5F, 0.0F));
		right.addOrReplaceChild("pauldronR",
			CubeListBuilder.create().texOffs(16, 38).addBox(-3.5F, -3.4F, -3.0F, 6, 5, 6),
			PartPose.ZERO);
		right.addOrReplaceChild("hauteR",
			CubeListBuilder.create().texOffs(40, 38).addBox(-0.8F, -5.5F, -2.5F, 2, 3, 5),
			PartPose.ZERO);
		var foreR = right.addOrReplaceChild("foreR",
			CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4, 7, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));
		foreR.addOrReplaceChild("couterR",
			CubeListBuilder.create().texOffs(16, 49).addBox(-2.8F, -0.5F, 1.2F, 2, 3, 2),
			PartPose.ZERO);

		// Greatsword centered at gauntlet grip, -Y points upward from grip to blade tip, +Y toward pommel
		var greatsword = foreR.addOrReplaceChild("greatsword",
			CubeListBuilder.create().texOffs(0, 62).addBox(-1.0F, -3.5F, -1.0F, 2, 8, 2),
			PartPose.offset(0.0F, 6.0F, -0.5F));
		greatsword.addOrReplaceChild("swordPommel",
			CubeListBuilder.create().texOffs(8, 62).addBox(-1.5F, 4.5F, -1.5F, 3, 3, 3),
			PartPose.ZERO);
		greatsword.addOrReplaceChild("swordGuard",
			CubeListBuilder.create().texOffs(20, 62).addBox(-4.5F, -5.0F, -1.5F, 9, 2, 3),
			PartPose.ZERO);
		greatsword.addOrReplaceChild("swordGuardTipR",
			CubeListBuilder.create().texOffs(44, 62).addBox(-5.2F, -6.5F, -1.0F, 2, 2, 2),
			PartPose.ZERO);
		greatsword.addOrReplaceChild("swordGuardTipL",
			CubeListBuilder.create().texOffs(44, 62).mirror().addBox(3.2F, -6.5F, -1.0F, 2, 2, 2),
			PartPose.ZERO);
		greatsword.addOrReplaceChild("swordRicasso",
			CubeListBuilder.create().texOffs(52, 62).addBox(-1.5F, -10.0F, -1.0F, 3, 5, 2),
			PartPose.ZERO);
		greatsword.addOrReplaceChild("swordBlade",
			CubeListBuilder.create().texOffs(0, 72).addBox(-1.5F, -28.0F, -0.5F, 3, 18, 1),
			PartPose.ZERO);
		greatsword.addOrReplaceChild("swordTip",
			CubeListBuilder.create().texOffs(8, 72).addBox(-1.0F, -32.0F, -0.5F, 2, 4, 1),
			PartPose.ZERO);

		// Double-sided emissive parry/overdrive spark flare (visible during Wrathful Riposte & Guillotine windup)
		greatsword.addOrReplaceChild("swordRiposteStar",
			CubeListBuilder.create()
				.texOffs(20, 72).addBox(-6.0F, -6.0F, 0.0F, 12, 12, 0)
				.texOffs(44, 72).addBox(0.0F, -6.0F, -6.0F, 0, 12, 12),
			PartPose.offset(0.0F, -14.0F, 0.0F));

		// Left arm + flared haute-piece pauldron + forearm + gauntlet
		var left = body.addOrReplaceChild("left",
			CubeListBuilder.create().texOffs(0, 38).mirror().addBox(-2.0F, -2.0F, -2.0F, 4, 6, 4),
			PartPose.offset(6.0F, -10.5F, 0.0F));
		left.addOrReplaceChild("pauldronL",
			CubeListBuilder.create().texOffs(16, 38).mirror().addBox(-2.5F, -3.4F, -3.0F, 6, 5, 6),
			PartPose.ZERO);
		left.addOrReplaceChild("hauteL",
			CubeListBuilder.create().texOffs(40, 38).mirror().addBox(-1.2F, -5.5F, -2.5F, 2, 3, 5),
			PartPose.ZERO);
		var foreL = left.addOrReplaceChild("foreL",
			CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 7, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));
		foreL.addOrReplaceChild("couterL",
			CubeListBuilder.create().texOffs(16, 49).mirror().addBox(0.8F, -0.5F, 1.2F, 2, 3, 2),
			PartPose.ZERO);

		// Articulated armored legs + knee cops + sabatons
		var legR = hips.addOrReplaceChild("legR",
			CubeListBuilder.create().texOffs(54, 38).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offset(-2.3F, 0.0F, 0.0F));
		var shinR = legR.addOrReplaceChild("shinR",
			CubeListBuilder.create().texOffs(54, 48).addBox(-2.2F, 0.0F, -2.5F, 4, 6, 5),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		shinR.addOrReplaceChild("kneeR",
			CubeListBuilder.create().texOffs(72, 48).addBox(-2.3F, -1.2F, -3.0F, 4, 3, 2),
			PartPose.ZERO);

		var legL = hips.addOrReplaceChild("legL",
			CubeListBuilder.create().texOffs(54, 38).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offset(2.3F, 0.0F, 0.0F));
		var shinL = legL.addOrReplaceChild("shinL",
			CubeListBuilder.create().texOffs(54, 48).mirror().addBox(-1.8F, 0.0F, -2.5F, 4, 6, 5),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		shinL.addOrReplaceChild("kneeL",
			CubeListBuilder.create().texOffs(72, 48).mirror().addBox(-1.7F, -1.2F, -3.0F, 4, 3, 2),
			PartPose.ZERO);

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OathboundBladeChampionRenderState s) {
		super.setupAnim(s);
		pose(s, s.move, s.elapsed, s.tell, s.riposteTriggered, s.riposteCounterElapsed);
		if (s.blend < 1.0F && s.prevMove >= 0) {
			blender.store();
			root().resetPose();
			pose(s, s.prevMove, s.prevElapsed, s.prevTell, s.prevRiposteTriggered, s.prevRiposteCounterElapsed);
			float k = s.blend * s.blend * (3.0F - 2.0F * s.blend);
			blender.mix(k);
		}
		applyRiposteVisuals(s);
		hurt(s);
		die(s);
		ground();
	}

	private void applyRiposteVisuals(OathboundBladeChampionRenderState s) {
		boolean show = s.deathTime <= 0
			&& ((s.move == OathboundBladeChampionRules.WRATHFUL_RIPOSTE && !s.riposteTriggered)
				|| (s.move == OathboundBladeChampionRules.GUILLOTINE_SLAM && s.elapsed < s.tell));
		swordRiposteStar.visible = show;
		if (show) {
			float pulse = 0.92F + 0.14F * Mth.sin(s.ageInTicks * 0.45F);
			swordRiposteStar.xScale = pulse;
			swordRiposteStar.yScale = pulse;
			swordRiposteStar.zScale = pulse;
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

	private void pose(OathboundBladeChampionRenderState s, int move, float el, int tellTicks,
			boolean counterTriggered, float counterEl) {
		float t = s.ageInTicks;
		// Confident tracking gaze & flowing crimson helm plume
		head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.6F;
		head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.45F;
		body.xRot = Mth.sin(t * 0.07F) * 0.015F;

		float walk = Math.min(1.0F, s.walkAnimationSpeed);
		float step = s.walkAnimationPos * 0.62F;
		plumeBase.xRot = -0.10F + walk * 0.18F + Mth.sin(t * 0.11F) * 0.04F;
		plumeTip.xRot = -0.12F + walk * 0.25F + Mth.sin(t * 0.11F - 0.7F) * 0.06F;

		// Default Panel 1 (ONE-HANDED SHOULDERED STALK)
		legL.xRot = Mth.cos(step) * walk * 0.62F;
		legR.xRot = -Mth.cos(step) * walk * 0.62F;
		shinL.xRot = Math.max(0.0F, -Mth.sin(step)) * walk * 0.88F;
		shinR.xRot = Math.max(0.0F, Mth.sin(step)) * walk * 0.88F;
		tassetL.zRot = -0.16F - Math.max(0.0F, -Mth.cos(step)) * walk * 0.16F;
		tassetR.zRot = 0.16F + Math.max(0.0F, Mth.cos(step)) * walk * 0.16F;
		tabardFront.xRot = -0.06F - Math.abs(Mth.cos(step)) * walk * 0.26F;
		tabardBack.xRot = 0.08F + Math.abs(Mth.cos(step)) * walk * 0.28F;

		// Split dual-tail cape
		capeYoke.xRot = 0.12F + walk * 0.22F;
		capeTailR.xRot = 0.08F + Mth.sin(t * 0.09F) * 0.04F;
		capeTailL.xRot = 0.08F + Mth.cos(t * 0.09F) * 0.04F;
		capeTailR.zRot = 0.12F;
		capeTailL.zRot = -0.12F;

		// Panel 1: Greatsword rested cleanly over right shoulder, left hand poised at hip
		right.xRot = -0.82F - Mth.cos(step) * walk * 0.14F;
		right.yRot = -0.28F;
		right.zRot = 0.28F;
		foreR.xRot = -1.55F;
		greatsword.xRot = 1.15F;
		greatsword.zRot = 0.14F;

		left.xRot = 0.18F + Mth.cos(step) * walk * 0.24F;
		left.zRot = -0.22F;
		foreL.xRot = -0.45F;

		float tell = Math.max(1.0F, tellTicks);
		switch (move) {
			case OathboundBladeChampionRules.WRATHFUL_RIPOSTE -> {
				if (!counterTriggered) {
					// Panel 2: WRATHFUL RIPOSTE PARRY — wide planted stance, two-handed 45° diagonal blade across chest
					float in = smooth(el, 0.0F, 4.0F);
					bend(0.30F * in);
					legL.zRot = -0.20F * in;
					legR.zRot = 0.20F * in;
					body.yRot = Mth.lerp(in, body.yRot, -0.15F);
					body.xRot = Mth.lerp(in, body.xRot, 0.10F);
					head.yRot = Mth.lerp(in, head.yRot, 0.12F);

					// Right yRot < 0 swings -Z inward (+X); Left yRot > 0 swings -Z inward (-X) so BOTH hands clasp hilt!
					right.xRot = Mth.lerp(in, right.xRot, -0.68F);
					right.yRot = Mth.lerp(in, right.yRot, -0.42F);
					right.zRot = Mth.lerp(in, right.zRot, 0.06F);
					foreR.xRot = Mth.lerp(in, foreR.xRot, -0.85F);

					left.xRot = Mth.lerp(in, left.xRot, -0.78F);
					left.yRot = Mth.lerp(in, left.yRot, 0.72F);
					left.zRot = Mth.lerp(in, left.zRot, -0.06F);
					foreL.xRot = Mth.lerp(in, foreL.xRot, -0.82F);

					greatsword.xRot = Mth.lerp(in, greatsword.xRot, 2.15F);
					greatsword.zRot = Mth.lerp(in, greatsword.zRot, 0.72F);
					capeYoke.xRot = 0.24F * in + 0.12F;
				} else {
					// Explosive golden-crimson riposte counter cleave!
					float swing = ramp(counterEl, 0.0F, 5.0F);
					float rec = smooth(counterEl, 7.0F, OathboundBladeChampionRules.RIPOSTE_COUNTER_TICKS);
					body.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.55F, -0.95F), 0.0F);
					body.xRot = Mth.lerp(rec, 0.24F, 0.0F);
					bend(0.35F * (1.0F - rec));

					right.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.88F, -0.95F), -0.82F);
					right.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.72F, 0.45F), -0.28F);
					foreR.xRot = Mth.lerp(rec, -0.45F, -1.55F);

					left.xRot = Mth.lerp(rec, -0.92F, 0.18F);
					left.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.28F, 0.85F), 0.0F);
					foreL.xRot = Mth.lerp(rec, -0.58F, -0.45F);

					greatsword.xRot = Mth.lerp(rec, 2.88F, 1.15F);
					greatsword.zRot = Mth.lerp(rec, Mth.lerp(swing, 0.95F, -1.15F), 0.14F);
					capeYoke.xRot = 0.48F * (1.0F - rec) + 0.12F;
				}
			}
			case OathboundBladeChampionRules.CRESCENT_CLEAVE -> {
				// Panel 3: CRIMSON CRESCENT CLEAVE — coiled right windup -> waist-high horizontal 180-deg sweep
				float wind = ramp(el, 0.0F, tell);
				float swing = ramp(el, tell, 5.0F);
				float rec = smooth(el, tell + 7.0F, OathboundBladeChampionRules.CRESCENT_DURATION);

				body.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.72F * wind, 0.92F), 0.0F);
				body.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.08F * wind, 0.26F), 0.0F);
				head.yRot = -body.yRot * 0.55F;

				bend(0.40F * Math.max(wind * 0.5F, swing) * (1.0F - rec));
				legL.zRot = -0.22F * (1.0F - rec);
				legR.zRot = 0.22F * (1.0F - rec);

				right.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.82F, -0.95F), -0.82F);
				right.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.42F * wind, -0.68F), -0.28F);
				right.zRot = Mth.lerp(rec, 0.18F, 0.28F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.55F, -0.45F), -1.55F);

				left.xRot = Mth.lerp(rec, Mth.lerp(swing, -0.88F, -0.92F), 0.18F);
				left.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.92F * wind, 0.35F), 0.0F);
				left.zRot = Mth.lerp(rec, -0.08F, -0.22F);
				foreL.xRot = Mth.lerp(rec, -0.52F, -0.45F);

				// Net xRot = -0.95 - 0.45 + 2.95 = +1.55 rad (horizontal waist-high blade sweeping -Z..+X!)
				greatsword.xRot = Mth.lerp(rec, 2.95F, 1.15F);
				greatsword.zRot = Mth.lerp(rec, Mth.lerp(swing, -0.75F, 0.55F), 0.14F);

				float fan = (1.0F - rec) * swing;
				capeYoke.xRot = 0.12F + fan * 0.45F;
				capeTailR.zRot = 0.12F + fan * 0.28F;
				capeTailL.zRot = -0.12F - fan * 0.35F;
				plumeTip.xRot = -0.12F - swing * 0.22F;
			}
			case OathboundBladeChampionRules.POMMEL_BREAK -> {
				// Panel 4: POMMEL-BREAK LUNGE — coiled crouch -> explosive forward lunge with two-handed pommel punch
				float wind = ramp(el, 0.0F, tell);
				float lunge = ramp(el, tell, 4.0F);
				float rec = smooth(el, tell + OathboundBladeChampionRules.POMMEL_LUNGE_TICKS,
					OathboundBladeChampionRules.POMMEL_DURATION);
				float active = (1.0F - rec) * Math.max(wind * 0.6F, lunge);

				body.xRot = Mth.lerp(rec, Mth.lerp(lunge, 0.12F * wind, 0.34F), 0.0F);
				body.yRot = Mth.lerp(rec, Mth.lerp(lunge, 0.32F * wind, -0.20F), 0.0F);
				head.xRot = Mth.lerp(active, head.xRot, -0.20F);

				// Deep asymmetric sprinter/martial lunge
				legL.xRot = Mth.lerp(rec, Mth.lerp(lunge, -0.30F, -0.62F), 0.0F);
				shinL.xRot = Mth.lerp(rec, Mth.lerp(lunge, 0.55F, 0.80F), 0.0F);
				legR.xRot = Mth.lerp(rec, Mth.lerp(lunge, 0.30F, 0.68F), 0.0F);
				shinR.xRot = Mth.lerp(rec, 0.15F, 0.0F);

				// Both arms drive forward-across chest so left gauntlet clasps grip/ricasso behind pommel
				right.xRot = Mth.lerp(rec, Mth.lerp(lunge, -0.55F, -1.15F), -0.82F);
				right.yRot = Mth.lerp(rec, Mth.lerp(lunge, -0.20F, -0.45F), -0.28F);
				right.zRot = Mth.lerp(rec, 0.15F, 0.28F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(lunge, -0.85F, -0.45F), -1.55F);

				left.xRot = Mth.lerp(rec, Mth.lerp(lunge, -0.58F, -1.18F), 0.18F);
				left.yRot = Mth.lerp(rec, Mth.lerp(lunge, 0.45F, 0.62F), 0.0F);
				left.zRot = Mth.lerp(rec, 0.05F, -0.22F);
				foreL.xRot = Mth.lerp(rec, Mth.lerp(lunge, -0.88F, -0.48F), -0.45F);

				// Pommel thrust forward (-Z), blade trailing upward-back behind left shoulder
				greatsword.xRot = Mth.lerp(rec, Mth.lerp(lunge, 0.85F, 0.48F), 1.15F);
				greatsword.zRot = Mth.lerp(rec, Mth.lerp(lunge, 0.22F, -0.36F), 0.14F);

				capeYoke.xRot = 0.12F + lunge * 0.58F * (1.0F - rec);
				capeTailR.zRot = 0.12F + lunge * 0.22F * (1.0F - rec);
				capeTailL.zRot = -0.12F - lunge * 0.22F * (1.0F - rec);
			}
			case OathboundBladeChampionRules.GUILLOTINE_SLAM -> {
				// Panels 5 & 6: SKY-RENDING GUILLOTINE — symmetrical overhead arch -> vertical ground-buried split lunge
				float wind = ramp(el, 0.0F, tell - 2.0F);
				float slam = ramp(el, tell - 2.0F, 3.0F);
				float rec = smooth(el, tell + 10.0F, OathboundBladeChampionRules.GUILLOTINE_DURATION);

				// Stance opens into wide heroic overhead pull then deep forward split lunge on impact
				legL.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.15F * wind, -0.78F), 0.0F);
				shinL.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.28F * wind, 0.86F), 0.0F);
				legR.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.15F * wind, 0.72F), 0.0F);
				shinR.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.12F * wind, 0.12F), 0.0F);

				body.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.18F * wind, 0.34F), 0.0F);
				head.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.12F * wind, -0.22F), 0.0F);

				// Overhead (-Y) arm: +zRot tilts right INWARD toward center; -zRot tilts left INWARD toward center!
				// On slam: right.yRot < 0 & left.yRot > 0 bring both hands together onto vertical sword hilt!
				right.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.82F, -2.55F), -1.18F), -0.82F);
				right.yRot = Mth.lerp(rec, Mth.lerp(slam, -0.18F * wind, -0.45F), -0.28F);
				right.zRot = Mth.lerp(rec, Mth.lerp(slam, 0.32F * wind, -0.06F), 0.28F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -1.55F, -0.35F), -0.18F), -1.55F);

				left.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, 0.18F, -2.55F), -1.18F), 0.18F);
				left.yRot = Mth.lerp(rec, Mth.lerp(slam, 0.18F * wind, 0.58F), 0.0F);
				left.zRot = Mth.lerp(rec, Mth.lerp(slam, -0.32F * wind, 0.06F), -0.22F);
				foreL.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.45F, -0.35F), -0.20F), -0.45F);

				// Windup net xRot ~ -0.55 (blade up-back centered behind plume); Impact net xRot ~ -3.14 (vertical ground plunge!)
				greatsword.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, 1.15F, 2.34F), -2.12F), 1.15F);
				greatsword.zRot = Mth.lerp(rec, Mth.lerp(slam, -0.18F * wind, 0.08F), 0.14F);

				float flare = (1.0F - rec) * Math.max(wind * 0.25F, slam);
				capeYoke.xRot = 0.12F + flare * 0.68F;
				capeTailR.zRot = 0.12F + flare * 0.28F;
				capeTailL.zRot = -0.12F - flare * 0.28F;
				plumeBase.xRot = -0.10F + flare * 0.35F;
			}
			case OathboundBladeChampionRules.STAGGERED -> {
				float sway = Mth.sin(el * 0.38F) * 0.09F;
				body.xRot = -0.18F;
				body.zRot = sway;
				head.xRot = 0.32F;
				head.zRot = -sway * 1.4F;
				right.xRot = 0.42F;
				right.zRot = 0.55F;
				foreR.xRot = -0.28F;
				greatsword.xRot = 0.45F;
				left.xRot = 0.35F;
				left.zRot = -0.68F;
				foreL.xRot = -0.30F;
				bend(0.24F);
			}
			default -> {
				OathboundTechnique tech = OathboundBladeChampionRules.technique(move);
				if (tech != null) {
					OathboundPose.apply(rig, tech.archetype(), el, tellTicks,
						OathboundBladeChampionRules.moveDuration(move, false), move);
				}
			}
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

	private void hurt(OathboundBladeChampionRenderState s) {
		if (s.hurt <= 0.0F || s.deathTime > 0) {
			return;
		}
		float k = s.hurt * s.hurt;
		body.xRot -= 0.16F * k;
		head.xRot -= 0.10F * k;
		capeYoke.xRot += 0.25F * k;
	}

	private void die(OathboundBladeChampionRenderState s) {
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
		capeYoke.xRot = Mth.lerp(fall, capeYoke.xRot, 0.15F);
	}
}
