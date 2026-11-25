package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.Archivist;
import dev.wildercord.cast.ArchivistRules;
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
 * The Archivist rendered on its 28-bone Bedrock geo rig with two dynamic emissive layers:
 * eyes + chest sigil (always lit while alive) and tome runes (dim at rest, blazing while casting/rewriting).
 */
public class ArchivistRenderer extends CullableEntityRenderer<Archivist, ArchivistRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("archivist"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/overhaul/archivist.png");
	private static final Identifier EYES = Wildercord.id("textures/entity/overhaul/archivist_eyes.png");
	private static final Identifier RUNES = Wildercord.id("textures/entity/overhaul/archivist_runes.png");
	private static final Identifier GEO = Wildercord.id("models/entity/overhaul/archivist.geo.json");
	private static final Identifier ANIM = Wildercord.id("animations/entity/overhaul/archivist.animation.json");

	private static BedrockGeoRig sharedRig;

	public ArchivistRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.6F;
	}

	private static BedrockGeoRig rig() {
		if (sharedRig == null) {
			sharedRig = BedrockGeoRig.load(GEO, ANIM);
		}
		return sharedRig;
	}

	@Override
	public ArchivistRenderState createRenderState() {
		return new ArchivistRenderState();
	}

	@Override
	public void extractRenderState(Archivist archivist, ArchivistRenderState state, float partial) {
		super.extractRenderState(archivist, state, partial);
		state.bodyYaw = Mth.lerp(partial, archivist.yBodyRotO, archivist.yBodyRot);
		state.hurtFlash = archivist.hurtTime > 0 ? Math.max(0.0F, (archivist.hurtTime - partial) / 10.0F) : 0.0F;
		state.casting = archivist.castPose(partial);
		state.rewriting = archivist.rewritePose(partial);
		state.move = archivist.currentMove();
		state.tell = archivist.moveTell();
		state.since = archivist.moveSince(partial);
		for (int i = 0; i < ArchivistRules.MOVES; i++) {
			state.moves[i] = archivist.moveBlend(i, partial);
		}
	}

	@Override
	public void submit(ArchivistRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) return;
		BedrockGeoRig rig = rig();
		String baseClip = state.walkAnimationSpeed > 0.08F ? "animation.archivist.walk" : "animation.archivist.idle";
		float baseSec = state.ageInTicks / 20.0F;
		String actionClip;
		if (state.rewriting > 0.15F) {
			actionClip = "animation.archivist.rewrite";
		} else if (state.casting > 0.15F) {
			actionClip = "animation.archivist.cast";
		} else if (state.move != ArchivistRules.NONE && !ArchivistRules.open(state.move)) {
			actionClip = "animation.archivist.trick_slam";
		} else {
			actionClip = null;
		}
		float actionSec = actionClip == null ? 0.0F : Math.max(0.0F, state.since / 20.0F);
		float actionWeight = actionClip == null ? 0.0F : Math.min(1.0F, Math.max(Math.max(state.rewriting, state.casting),
			state.move >= 0 && state.move < state.moves.length ? Math.max(0.55F, state.moves[state.move]) : 0.65F));

		int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(state.hurtFlash > 0.0F));
		float eyeAlpha = state.deathTime > 0 ? Math.max(0.0F, 1.0F - state.deathTime / 14.0F) : 1.0F;
		int eyeArgb = (Mth.clamp((int) (eyeAlpha * 255.0F), 0, 255) << 24) | 0xFFFFFF;

		float glow = Math.max(state.casting, state.rewriting);
		for (int i = 1; i < ArchivistRules.MOVES; i++) {
			if (!ArchivistRules.open(i)) {
				glow = Math.max(glow, state.moves[i]);
			}
		}
		float runeAlpha = 0.35F + 0.1F * Mth.sin(state.ageInTicks * 0.1F) + 0.6F * glow;
		runeAlpha *= 1.0F - 0.8F * state.moves[ArchivistRules.BLANK];
		if (state.deathTime > 0) {
			runeAlpha *= Math.max(0.0F, 1.0F - state.deathTime / 20.0F);
		}
		int runeArgb = (Mth.clamp((int) (runeAlpha * 255.0F), 0, 255) << 24) | 0xFFFFFF;

		poseStack.pushPose();
		RenderCompat.rotateDegrees(poseStack, Axis.YP, 180.0F - state.bodyYaw);
		float s = 1.12F / 16.0F;
		poseStack.scale(s, s, s);

		collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(SKIN),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
				TrialTelegraphs.poseStackFrom(pose), buffer, state.lightCoords, overlay, 0xFFFFFFFF));
		if ((eyeArgb >>> 24) >= 4) {
			collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(EYES),
				(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
					TrialTelegraphs.poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, eyeArgb));
		}
		if ((runeArgb >>> 24) >= 4) {
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.eyes(RUNES),
				(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
					TrialTelegraphs.poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, runeArgb));
		}

		poseStack.popPose();
	}
}
