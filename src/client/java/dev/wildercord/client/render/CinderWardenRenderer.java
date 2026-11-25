package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.CinderWarden;
import dev.wildercord.cast.CinderWardenRules;
import dev.wildercord.cast.DungeonBoss;
import dev.wildercord.client.compat.CullableEntityRenderer;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.client.trials.BedrockGeoRig;
import dev.wildercord.client.trials.TrialTelegraphs;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Cinder Warden rendered on its 25-bone Bedrock geo rig with dynamic magma-heat and furnace-eye emissive layers.
 */
public class CinderWardenRenderer extends CullableEntityRenderer<CinderWarden, DungeonBossRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cinder_warden"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/overhaul/cinder_warden.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/overhaul/cinder_warden_glow.png");
	private static final Identifier EYES = Wildercord.id("textures/entity/overhaul/cinder_warden_eyes.png");
	private static final Identifier GEO = Wildercord.id("models/entity/overhaul/cinder_warden.geo.json");
	private static final Identifier ANIM = Wildercord.id("animations/entity/overhaul/cinder_warden.animation.json");

	private static BedrockGeoRig sharedRig;

	public CinderWardenRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 1.1F;
	}

	private static BedrockGeoRig rig() {
		if (sharedRig == null) {
			sharedRig = BedrockGeoRig.load(GEO, ANIM);
		}
		return sharedRig;
	}

	@Override
	public DungeonBossRenderState createRenderState() {
		return new DungeonBossRenderState();
	}

	@Override
	public void extractRenderState(CinderWarden boss, DungeonBossRenderState state, float partial) {
		super.extractRenderState(boss, state, partial);
		state.bodyYaw = Mth.lerp(partial, boss.yBodyRotO, boss.yBodyRot);
		state.hurtFlash = boss.hurtTime > 0 ? Math.max(0.0F, (boss.hurtTime - partial) / 10.0F) : 0.0F;
		state.casting = boss.castPose(partial);
		state.shifting = boss.shiftPose(partial);
		state.guarded = boss.guardPose(partial);
		state.exposed = boss.exposedPose(partial);
		state.slamming = boss.slamPose(partial);
		state.phase = boss.phase();
		state.move = boss.currentMove();
		state.since = boss.moveSince(partial);
		state.tell = boss.moveTell();
		for (int i = 1; i < DungeonBoss.MOVES; i++) {
			state.moves[i] = boss.moveBlend(i, partial);
		}
		for (int i = 0; i < DungeonBoss.EXTRA; i++) {
			state.extra[i] = boss.extra(i, partial);
		}
	}

	@Override
	public void submit(DungeonBossRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) return;
		BedrockGeoRig rig = rig();
		String baseClip = state.walkAnimationSpeed > 0.08F ? "animation.cinder_warden.walk" : "animation.cinder_warden.idle";
		float baseSec = state.ageInTicks / 20.0F;
		String actionClip;
		if (state.moves[CinderWardenRules.SPENT] > 0.35F) {
			actionClip = "animation.cinder_warden.spent_slump";
		} else {
			actionClip = switch (state.move) {
				case CinderWardenRules.FLARE, CinderWardenRules.VENT -> "animation.cinder_warden.flare_roar";
				case CinderWardenRules.SWEEP, CinderWardenRules.HAUL -> "animation.cinder_warden.chain_sweep";
				case CinderWardenRules.HAMMERFALL, CinderWardenRules.SEAM, CinderWardenRules.SLAG, CinderWardenRules.BELLOWS ->
					"animation.cinder_warden.hammerfall";
				default -> state.casting > 0.25F ? "animation.cinder_warden.flare_roar"
					: (state.slamming > 0.25F ? "animation.cinder_warden.hammerfall" : null);
			};
		}
		float actionSec = actionClip == null ? 0.0F : Math.max(0.0F, state.since / 20.0F);
		float actionWeight = actionClip == null ? 0.0F : Math.min(1.0F, Math.max(Math.max(state.casting, state.slamming),
			state.move > 0 && state.move < state.moves.length ? Math.max(0.55F, state.moves[state.move]) : 0.65F));

		int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(state.hurtFlash > 0.0F));
		float flare = Math.max(Math.max(state.casting, state.slamming), Math.max(state.exposed, state.shifting));
		float flicker = 0.08F * Mth.sin(state.ageInTicks * 0.37F) + 0.05F * Mth.sin(state.ageInTicks * 1.13F);
		float level = Math.max(0.5F + flicker + 0.5F * flare, heat(state));
		float cold = state.moves[CinderWardenRules.SPENT] * (1.0F - state.exposed);
		float alive = state.deathTime > 0 ? Math.max(0.0F, 1.0F - state.deathTime / (float) DungeonBoss.DEATH_TICKS) : 1.0F;
		int glowArgb = white(Mth.lerp(cold, level, 0.12F + 0.5F * flicker) * alive);
		int eyeArgb = white(alive);

		poseStack.pushPose();
		RenderCompat.rotateDegrees(poseStack, Axis.YP, 180.0F - state.bodyYaw);
		float s = 0.90F / 16.0F;
		poseStack.scale(s, s, s);

		collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(SKIN),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
				TrialTelegraphs.poseStackFrom(pose), buffer, state.lightCoords, overlay, 0xFFFFFFFF));
		if ((glowArgb >>> 24) >= 4) {
			collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(GLOW),
				(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
					TrialTelegraphs.poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, glowArgb));
		}
		if ((eyeArgb >>> 24) >= 4) {
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.eyes(EYES),
				(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
					TrialTelegraphs.poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, eyeArgb));
		}

		poseStack.popPose();
	}

	private static int white(float alpha) {
		return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | 0xFFFFFF;
	}

	/** How hot its current move burns it, 0 to 1. */
	private static float heat(DungeonBossRenderState state) {
		float s = state.since;
		int tell = Math.max(1, state.tell);
		return switch (state.move) {
			case CinderWardenRules.HAMMERFALL -> s < tell ? 0.5F + 0.5F * s / tell : Math.max(0.0F, 1.0F - (s - tell) / 10.0F);
			case CinderWardenRules.BELLOWS -> s >= tell && s < tell + CinderWardenRules.BREATH_TICKS ? 0.95F : 0.0F;
			case CinderWardenRules.FLARE -> {
				float gather = CinderWardenRules.FLARE_GATHER;
				if (s >= tell + gather) {
					yield 1.0F;
				}
				float g = Mth.clamp(s / (tell + gather), 0.0F, 1.0F);
				float beat = 0.5F + 0.5F * Mth.sin(s * (0.3F + 0.9F * g));
				yield 0.4F + 0.3F * g + 0.3F * g * beat;
			}
			default -> 0.0F;
		};
	}
}
