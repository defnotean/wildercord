package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.OathboundHalberdierRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;

/**
 * Articulated 1:1 model for Knight #3: Oathbound Halberdier (Way of the Storm-Glaive).
 *
 * <p>Texture atlas: {@code 128x128} ({@code textures/entity/oathbound_halberdier/skin.png} and {@code glow.png}).
 * Every integer cube matches {@code HALBERDIER_UV_BOXES} in {@code tools/oathbound_knights_art.py}.</p>
 */
public final class OathboundHalberdierModel extends EntityModel<OathboundHalberdierRenderState> {
	private static final float HIPS = 12.0F;

	private final ModelPart hips;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart wingR;
	private final ModelPart wingL;
	private final ModelPart scarfBase;
	private final ModelPart scarfTip;
	private final ModelPart tabardFront;
	private final ModelPart tabardBack;
	private final ModelPart tassetR;
	private final ModelPart tassetL;
	private final ModelPart right;
	private final ModelPart foreR;
	private final ModelPart halberd;
	private final ModelPart halberdTassel;
	private final ModelPart halberdStormArc;
	private final ModelPart left;
	private final ModelPart foreL;
	private final ModelPart legR;
	private final ModelPart shinR;
	private final ModelPart legL;
	private final ModelPart shinL;

	private final PoseBlend blender;

	public OathboundHalberdierModel(ModelPart root) {
		super(root);
		hips = root.getChild("hips");
		body = hips.getChild("body");
		head = body.getChild("head");
		wingR = head.getChild("wingR");
		wingL = head.getChild("wingL");
		scarfBase = body.getChild("scarfBase");
		scarfTip = scarfBase.getChild("scarfTip");
		tabardFront = body.getChild("tabardFront");
		tabardBack = body.getChild("tabardBack");
		tassetR = body.getChild("tassetR");
		tassetL = body.getChild("tassetL");
		right = body.getChild("right");
		foreR = right.getChild("foreR");
		halberd = foreR.getChild("halberd");
		halberdTassel = halberd.getChild("halberdTassel");
		halberdStormArc = halberd.getChild("halberdStormArc");
		left = body.getChild("left");
		foreL = left.getChild("foreL");
		legR = hips.getChild("legR");
		shinR = legR.getChild("shinR");
		legL = hips.getChild("legL");
		shinL = legL.getChild("shinL");

		ModelPart[] posed = new ModelPart[]{
			hips, body, head, wingR, wingL, scarfBase, scarfTip,
			tabardFront, tabardBack, tassetR, tassetL,
			right, foreR, halberd, halberdTassel, left, foreL,
			legR, shinR, legL, shinL
		};
		blender = new PoseBlend(posed);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		var root = mesh.getRoot();
		var hips = root.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(0.0F, HIPS, 0.0F));

		// Indigo-cobalt plate torso, navy cowl, 2-segment trailing scarf, pointed tabards, tassets
		var body = hips.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 18).addBox(-4.5F, -12.0F, -2.5F, 9, 12, 5),
			PartPose.ZERO);
		body.addOrReplaceChild("cowl",
			CubeListBuilder.create().texOffs(28, 18).addBox(-5.0F, -13.4F, -3.5F, 10, 3, 7),
			PartPose.ZERO);
		var scarfBase = body.addOrReplaceChild("scarfBase",
			CubeListBuilder.create().texOffs(62, 18).addBox(-1.5F, 0.0F, -0.5F, 3, 8, 1),
			PartPose.offset(-3.2F, -11.2F, 3.0F));
		scarfBase.addOrReplaceChild("scarfTip",
			CubeListBuilder.create().texOffs(70, 18).addBox(-1.5F, 0.0F, -0.5F, 3, 8, 1),
			PartPose.offset(0.0F, 7.6F, 0.0F));
		body.addOrReplaceChild("tabardFront",
			CubeListBuilder.create().texOffs(28, 28).addBox(-2.5F, 0.0F, -0.5F, 5, 8, 1),
			PartPose.offset(0.0F, -1.5F, -2.7F));
		body.addOrReplaceChild("tabardBack",
			CubeListBuilder.create().texOffs(40, 28).addBox(-2.5F, 0.0F, -0.5F, 5, 8, 1),
			PartPose.offset(0.0F, -1.5F, 2.5F));
		body.addOrReplaceChild("tassetR",
			CubeListBuilder.create().texOffs(52, 28).addBox(-1.2F, 0.0F, -2.5F, 2, 6, 5),
			PartPose.offset(-4.5F, -1.5F, 0.0F));
		body.addOrReplaceChild("tassetL",
			CubeListBuilder.create().texOffs(52, 28).mirror().addBox(-0.8F, 0.0F, -2.5F, 2, 6, 5),
			PartPose.offset(4.5F, -1.5F, 0.0F));

		// Houndskull / falcon-beak winged helmet with swept silver wings
		var head = body.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.5F, -4.0F, 8, 8, 8),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		head.addOrReplaceChild("beakUpper",
			CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -4.8F, -5.8F, 4, 2, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("beakTip",
			CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -3.2F, -6.5F, 2, 2, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("wingR",
			CubeListBuilder.create().texOffs(52, 0).addBox(0.0F, -4.0F, -1.0F, 0, 5, 6),
			PartPose.offsetAndRotation(-4.2F, -7.2F, 0.5F, 0.35F, -0.25F, -0.22F));
		head.addOrReplaceChild("wingL",
			CubeListBuilder.create().texOffs(52, 0).mirror().addBox(0.0F, -4.0F, -1.0F, 0, 5, 6),
			PartPose.offsetAndRotation(4.2F, -7.2F, 0.5F, 0.35F, 0.25F, 0.22F));

		// Arms, layered pauldrons, rune gauntlets + Storm-Halberd
		var right = body.addOrReplaceChild("right",
			CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, -2.0F, -2.0F, 4, 6, 4),
			PartPose.offset(-6.0F, -10.5F, 0.0F));
		right.addOrReplaceChild("pauldronR",
			CubeListBuilder.create().texOffs(16, 40).addBox(-3.2F, -3.2F, -3.0F, 6, 5, 6),
			PartPose.ZERO);
		var foreR = right.addOrReplaceChild("foreR",
			CubeListBuilder.create().texOffs(0, 52).addBox(-2.0F, 0.0F, -2.0F, 4, 7, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		// Two-handed Storm-Halberd anchored in right hand grip (-Y = spear head, -Z = crescent axe blade, +Z = fluke hook)
		var halberd = foreR.addOrReplaceChild("halberd",
			CubeListBuilder.create().texOffs(0, 66).addBox(-1.0F, -22.0F, -1.0F, 2, 32, 2),
			PartPose.offset(0.0F, 6.0F, -0.5F));
		halberd.addOrReplaceChild("halberdButt",
			CubeListBuilder.create().texOffs(8, 66).addBox(-1.5F, 10.0F, -1.5F, 3, 3, 3),
			PartPose.ZERO);
		halberd.addOrReplaceChild("halberdTassel",
			CubeListBuilder.create().texOffs(20, 66).addBox(-0.5F, 0.0F, 0.0F, 1, 7, 2),
			PartPose.offset(0.0F, -15.0F, 1.0F));
		halberd.addOrReplaceChild("halberdAxe",
			CubeListBuilder.create().texOffs(26, 66).addBox(0.0F, -23.0F, -7.0F, 0, 9, 6),
			PartPose.ZERO);
		halberd.addOrReplaceChild("halberdHook",
			CubeListBuilder.create().texOffs(40, 66).addBox(0.0F, -21.0F, 1.0F, 0, 4, 4),
			PartPose.ZERO);
		halberd.addOrReplaceChild("halberdSpear",
			CubeListBuilder.create().texOffs(50, 66).addBox(-1.0F, -30.0F, -1.0F, 2, 8, 2),
			PartPose.ZERO);
		halberd.addOrReplaceChild("halberdStormArc",
			CubeListBuilder.create()
				.texOffs(60, 66).addBox(-6.0F, -6.0F, 0.0F, 12, 12, 0)
				.texOffs(84, 66).addBox(0.0F, -6.0F, -6.0F, 0, 12, 12),
			PartPose.offset(0.0F, -19.0F, -2.5F));

		var left = body.addOrReplaceChild("left",
			CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-2.0F, -2.0F, -2.0F, 4, 6, 4),
			PartPose.offset(6.0F, -10.5F, 0.0F));
		left.addOrReplaceChild("pauldronL",
			CubeListBuilder.create().texOffs(16, 40).mirror().addBox(-2.8F, -3.2F, -3.0F, 6, 5, 6),
			PartPose.ZERO);
		left.addOrReplaceChild("foreL",
			CubeListBuilder.create().texOffs(0, 52).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 7, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		// Rune-etched legs, knee cops, greaves
		var legR = hips.addOrReplaceChild("legR",
			CubeListBuilder.create().texOffs(40, 40).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offset(-2.3F, 0.0F, 0.0F));
		var shinR = legR.addOrReplaceChild("shinR",
			CubeListBuilder.create().texOffs(40, 52).addBox(-2.2F, 0.0F, -2.5F, 4, 6, 5),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		shinR.addOrReplaceChild("kneeR",
			CubeListBuilder.create().texOffs(58, 52).addBox(-2.3F, -1.2F, -3.0F, 4, 3, 2),
			PartPose.ZERO);

		var legL = hips.addOrReplaceChild("legL",
			CubeListBuilder.create().texOffs(40, 40).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4),
			PartPose.offset(2.3F, 0.0F, 0.0F));
		var shinL = legL.addOrReplaceChild("shinL",
			CubeListBuilder.create().texOffs(40, 52).mirror().addBox(-1.8F, 0.0F, -2.5F, 4, 6, 5),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		shinL.addOrReplaceChild("kneeL",
			CubeListBuilder.create().texOffs(58, 52).mirror().addBox(-1.7F, -1.2F, -3.0F, 4, 3, 2),
			PartPose.ZERO);

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OathboundHalberdierRenderState s) {
		super.setupAnim(s);
		pose(s, s.move, s.elapsed, s.tell);
		if (s.blend < 1.0F && s.prevMove >= 0) {
			blender.store();
			root().resetPose();
			pose(s, s.prevMove, s.prevElapsed, s.prevTell);
			float k = s.blend * s.blend * (3.0F - 2.0F * s.blend);
			blender.mix(k);
		}
		applyStormVisuals(s);
		hurt(s);
		die(s);
		ground();
	}

	private void applyStormVisuals(OathboundHalberdierRenderState s) {
		halberdStormArc.visible = false;
	}

	private static float ramp(float t, float from, float ticks) {
		float k = Mth.clamp((t - from) / Math.max(1.0F, ticks), 0.0F, 1.0F);
		return 1.0F - (1.0F - k) * (1.0F - k);
	}

	private static float smooth(float t, float from, float to) {
		float k = Mth.clamp((t - from) / Math.max(0.001F, to - from), 0.0F, 1.0F);
		return k * k * (3.0F - 2.0F * k);
	}

	private void pose(OathboundHalberdierRenderState s, int move, float el, int tellTicks) {
		float t = s.ageInTicks;
		head.yRot = s.yRot * Mth.DEG_TO_RAD * 0.65F;
		head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.45F;
		body.xRot = Mth.sin(t * 0.08F) * 0.02F;

		float walk = Math.min(1.0F, s.walkAnimationSpeed);
		float step = s.walkAnimationPos * 0.65F;

		// Wide, alert Panel 1 stance + streaming scarf & tassel
		legL.xRot = Mth.cos(step) * walk * 0.65F;
		legR.xRot = -Mth.cos(step) * walk * 0.65F;
		shinL.xRot = Math.max(0.0F, -Mth.sin(step)) * walk * 0.88F;
		shinR.xRot = Math.max(0.0F, Mth.sin(step)) * walk * 0.88F;
		legL.zRot = -0.18F;
		legR.zRot = 0.18F;
		bend(0.18F);

		tassetL.zRot = -0.22F - Math.max(0.0F, -Mth.cos(step)) * walk * 0.14F;
		tassetR.zRot = 0.22F + Math.max(0.0F, Mth.cos(step)) * walk * 0.14F;
		tabardFront.xRot = -0.06F - Math.abs(Mth.cos(step)) * walk * 0.25F;
		tabardBack.xRot = 0.08F + Math.abs(Mth.cos(step)) * walk * 0.25F;

		scarfBase.xRot = 0.52F + walk * 0.35F + Mth.sin(t * 0.13F) * 0.06F;
		scarfBase.zRot = 0.38F;
		scarfTip.xRot = 0.32F + Mth.cos(t * 0.13F) * 0.08F;
		scarfTip.zRot = 0.25F;
		halberdTassel.xRot = 0.22F + Mth.sin(t * 0.14F) * 0.08F;

		// Panel 1: Two-handed diagonal halberd guard (right at lower hip, left forward on mid-shaft, blade up-left)
		right.xRot = -0.32F - Mth.cos(step) * walk * 0.15F;
		right.yRot = -0.35F;
		right.zRot = 0.12F;
		foreR.xRot = -0.78F;

		left.xRot = -0.82F + Mth.cos(step) * walk * 0.18F;
		left.yRot = 0.58F;
		left.zRot = -0.08F;
		foreL.xRot = -0.68F;

		halberd.xRot = 1.82F;
		halberd.zRot = 0.55F;

		float tell = Math.max(1.0F, tellTicks);
		switch (move) {
			case OathboundHalberdierRules.TEMPEST_WHIRL -> {
				// Panel 2: TEMPEST WHIRL CLEAVE — low crouching 360 horizontal halberd sweep
				float wind = ramp(el, 0.0F, tell);
				float swing = ramp(el, tell, 5.0F);
				float rec = smooth(el, tell + 7.0F, OathboundHalberdierRules.WHIRL_DURATION);
				float active = (1.0F - rec) * Math.max(wind * 0.5F, swing);

				body.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.78F * wind, 1.15F), 0.0F);
				body.xRot = Mth.lerp(active, body.xRot, 0.22F);
				head.yRot = -body.yRot * 0.45F;
				bend(0.24F * active);
				legL.zRot = -0.24F;
				legR.zRot = 0.24F;

				right.xRot = Mth.lerp(rec, -0.92F, -0.32F);
				right.yRot = Mth.lerp(rec, Mth.lerp(swing, -0.58F, -0.28F), -0.35F);
				foreR.xRot = Mth.lerp(rec, -0.48F, -0.78F);

				left.xRot = Mth.lerp(rec, -0.92F, -0.82F);
				left.yRot = Mth.lerp(rec, Mth.lerp(swing, 0.42F, 0.65F), 0.58F);
				foreL.xRot = Mth.lerp(rec, -0.52F, -0.68F);

				halberd.xRot = Mth.lerp(rec, 2.95F, 1.82F);
				halberd.zRot = Mth.lerp(rec, Mth.lerp(swing, 0.85F, 0.45F), 0.55F);

				scarfBase.xRot = 0.52F + 0.58F * active;
				scarfBase.zRot = 0.38F + 0.45F * active;
			}
			case OathboundHalberdierRules.SHADOWSTEP_THRUST -> {
				// Panel 3: SHADOWSTEP BLINK-THRUST — explosive forward sprinter lunge + long horizontal spear thrust
				float wind = ramp(el, 0.0F, tell);
				float dash = ramp(el, tell, 3.0F);
				float rec = smooth(el, tell + OathboundHalberdierRules.THRUST_DASH_TICKS + 2.0F,
					OathboundHalberdierRules.THRUST_DURATION);

				body.xRot = Mth.lerp(rec, Mth.lerp(dash, 0.10F * wind, 0.36F), 0.0F);
				head.xRot = Mth.lerp(dash * (1.0F - rec), head.xRot, -0.24F);
				legL.xRot = Mth.lerp(rec, Mth.lerp(dash, -0.25F, -0.68F), 0.0F);
				shinL.xRot = Mth.lerp(rec, Mth.lerp(dash, 0.45F, 0.82F), 0.0F);
				legR.xRot = Mth.lerp(rec, Mth.lerp(dash, 0.25F, 0.72F), 0.0F);
				shinR.xRot = Mth.lerp(rec, 0.14F, 0.0F);

				right.xRot = Mth.lerp(rec, Mth.lerp(dash, -0.55F, -1.10F), -0.32F);
				right.yRot = Mth.lerp(rec, -0.32F, -0.35F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(dash, -0.82F, -0.45F), -0.78F);

				left.xRot = Mth.lerp(rec, Mth.lerp(dash, -0.65F, -1.16F), -0.82F);
				left.yRot = Mth.lerp(rec, 0.48F, 0.58F);
				foreL.xRot = Mth.lerp(rec, Mth.lerp(dash, -0.78F, -0.48F), -0.68F);

				// Net xRot ~ +1.55: spear (-Y) shoots straight forward (-Z)!
				halberd.xRot = Mth.lerp(rec, Mth.lerp(dash, 2.25F, 3.10F), 1.82F);
				halberd.zRot = Mth.lerp(rec, 0.06F, 0.55F);

				scarfBase.xRot = 0.52F + 0.68F * dash * (1.0F - rec);
			}
			case OathboundHalberdierRules.HOOK_AND_REAP -> {
				// Panel 4: HOOK-AND-REAP PULL — extended reach -> leaning back pull
				float reachOut = ramp(el, 0.0F, tell);
				float yank = ramp(el, tell, 4.0F);
				float rec = smooth(el, tell + 6.0F, OathboundHalberdierRules.HOOK_DURATION);

				body.xRot = Mth.lerp(rec, Mth.lerp(yank, 0.22F * reachOut, -0.22F), 0.0F);
				body.yRot = Mth.lerp(rec, Mth.lerp(yank, 0.15F, -0.32F), 0.0F);
				legL.xRot = Mth.lerp(rec, Mth.lerp(yank, -0.35F, -0.52F), 0.0F);
				shinL.xRot = Mth.lerp(rec, 0.65F, 0.18F);
				legR.xRot = Mth.lerp(rec, Mth.lerp(yank, 0.25F, 0.50F), 0.0F);

				right.xRot = Mth.lerp(rec, Mth.lerp(yank, -1.05F, -0.55F), -0.32F);
				right.yRot = Mth.lerp(rec, -0.38F, -0.35F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(yank, -0.35F, -0.95F), -0.78F);

				left.xRot = Mth.lerp(rec, Mth.lerp(yank, -1.12F, -0.78F), -0.82F);
				left.yRot = Mth.lerp(rec, 0.52F, 0.58F);
				foreL.xRot = Mth.lerp(rec, Mth.lerp(yank, -0.35F, -0.85F), -0.68F);

				halberd.xRot = Mth.lerp(rec, Mth.lerp(yank, 2.88F, 2.35F), 1.82F);
				halberd.zRot = Mth.lerp(rec, 0.22F, 0.55F);
			}
			case OathboundHalberdierRules.SKY_VAULT_CRASH -> {
				// Panels 5 & 6: VALKYRIE SKY-VAULT WINDUP (clear wide overhead arch) -> THUNDERBOLT CRASH (spear & axe floor plunge)
				float wind = ramp(el, 0.0F, tell - 2.0F);
				float slam = ramp(el, tell - 2.0F, 3.0F);
				float rec = smooth(el, tell + 8.0F, OathboundHalberdierRules.CRASH_DURATION);

				legL.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.45F * wind, -0.68F), 0.0F);
				shinL.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.78F * wind, 0.82F), 0.0F);
				legL.zRot = Mth.lerp(rec, -0.28F, -0.18F);
				legR.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.35F * wind, 0.62F), 0.0F);
				shinR.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.22F * wind, 0.14F), 0.0F);
				legR.zRot = Mth.lerp(rec, 0.28F, 0.18F);

				body.xRot = Mth.lerp(rec, Mth.lerp(slam, -0.18F * wind, 0.26F), 0.0F);
				head.xRot = Mth.lerp(rec, Mth.lerp(slam, 0.06F * wind, -0.16F), 0.0F);

				// Windup: raise both arms almost straight vertical (-2.95 rad) with wide outward zRot clear of face!
				right.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.32F, -2.95F), -1.02F), -0.32F);
				right.yRot = Mth.lerp(rec, Mth.lerp(slam, 0.0F, -0.38F), -0.35F);
				right.zRot = Mth.lerp(rec, Mth.lerp(slam, -0.14F * wind, -0.05F), 0.12F);
				foreR.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.78F, -0.18F), -0.22F), -0.78F);

				left.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.82F, -2.85F), -1.04F), -0.82F);
				left.yRot = Mth.lerp(rec, Mth.lerp(slam, 0.0F, 0.55F), 0.58F);
				left.zRot = Mth.lerp(rec, Mth.lerp(slam, 0.14F * wind, 0.05F), -0.08F);
				foreL.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, -0.68F, -0.22F), -0.24F), -0.68F);

				// Panel 5: overhead crossbar visible right above winged helm -> Panel 6: spear/axe plunging visibly into stone
				halberd.xRot = Mth.lerp(rec, Mth.lerp(slam, Mth.lerp(wind, 1.82F, 1.55F), -1.96F), 1.82F);
				halberd.zRot = Mth.lerp(rec, Mth.lerp(slam, -1.42F * wind, 0.16F), 0.55F);

				float flare = (1.0F - rec) * Math.max(wind * 0.4F, slam);
				scarfBase.xRot = 0.52F + flare * 0.55F;
				scarfBase.zRot = 0.38F + flare * 0.35F;
			}
			case OathboundHalberdierRules.STAGGERED -> {
				float sway = Mth.sin(el * 0.38F) * 0.09F;
				body.xRot = -0.18F;
				body.zRot = sway;
				head.xRot = 0.30F;
				right.xRot = 0.40F;
				right.zRot = 0.52F;
				foreR.xRot = -0.25F;
				halberd.xRot = 0.55F;
				left.xRot = 0.32F;
				left.zRot = -0.62F;
				foreL.xRot = -0.28F;
				bend(0.24F);
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

	private void hurt(OathboundHalberdierRenderState s) {
		if (s.hurt <= 0.0F || s.deathTime > 0) {
			return;
		}
		float k = s.hurt * s.hurt;
		body.xRot -= 0.16F * k;
		head.xRot -= 0.10F * k;
		scarfBase.xRot += 0.25F * k;
	}

	private void die(OathboundHalberdierRenderState s) {
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
		scarfBase.xRot = Mth.lerp(fall, scarfBase.xRot, 0.15F);
	}
}
