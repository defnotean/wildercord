package dev.wildercord.client.trials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.CullableEntityRenderer;
import dev.wildercord.client.compat.RenderCompat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Reusable entity renderer backed by {@link BedrockGeoRig} (cutout diffuse + emissive eyes glow).
 */
public final class GeoMobRenderer<T extends LivingEntity> extends CullableEntityRenderer<T, GeoMobRenderState> {
	private final Identifier skin;
	private final Identifier glow;
	private final Identifier geo;
	private final Identifier anim;
	private final String idleClip;
	private final String walkClip;
	private final float modelScale;
	private BedrockGeoRig rig;

	public GeoMobRenderer(
			EntityRendererProvider.Context ctx,
			String assetBase,
			String clipPrefix,
			float shadowRadius,
			float modelScale
	) {
		super(ctx);
		this.shadowRadius = shadowRadius;
		this.modelScale = modelScale;
		this.skin = Wildercord.id("textures/entity/" + assetBase + ".png");
		this.glow = Wildercord.id("textures/entity/" + assetBase + "_glow.png");
		this.geo = Wildercord.id("models/entity/" + assetBase + ".geo.json");
		this.anim = Wildercord.id("animations/entity/" + assetBase + ".animation.json");
		this.idleClip = "animation." + clipPrefix + ".idle";
		this.walkClip = "animation." + clipPrefix + ".walk";
	}

	private BedrockGeoRig rig() {
		if (rig == null) {
			rig = BedrockGeoRig.load(geo, anim);
		}
		return rig;
	}

	@Override
	public GeoMobRenderState createRenderState() {
		return new GeoMobRenderState();
	}

	@Override
	public void extractRenderState(T entity, GeoMobRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.bodyYaw = Mth.lerp(partial, entity.yBodyRotO, entity.yBodyRot);
		state.hurtFlash = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partial) / 10.0F) : 0.0F;
		state.deathTime = entity.deathTime > 0 ? entity.deathTime + partial : 0.0F;
		state.walkPos = entity.walkAnimation.position(partial);
		state.walkSpeed = entity.walkAnimation.speed(partial);
	}

	@Override
	public void submit(GeoMobRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) {
			return;
		}
		BedrockGeoRig r = rig();
		boolean moving = state.walkSpeed > 0.05F;
		String baseClip = moving ? walkClip : idleClip;
		float baseSec = moving ? state.walkPos * 0.28F : state.ageInTicks / 20.0F;

		int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(state.hurtFlash > 0.0F || state.deathTime > 0.0F));
		poseStack.pushPose();
		RenderCompat.rotateDegrees(poseStack, Axis.YP, 180.0F - state.bodyYaw);
		float s = modelScale / 16.0F;
		poseStack.scale(s, s, s);

		collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(skin),
			(pose, buffer) -> r.poseAndSubmit(baseClip, baseSec, null, 0.0F, 0.0F, poseStackFrom(pose), buffer, state.lightCoords, overlay, 0xFFFFFFFF));
		collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(glow),
			(pose, buffer) -> r.poseAndSubmit(baseClip, baseSec, null, 0.0F, 0.0F, poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF));

		poseStack.popPose();
	}

	private static PoseStack poseStackFrom(PoseStack.Pose basePose) {
		PoseStack local = new PoseStack();
		local.last().pose().set(basePose.pose());
		local.last().normal().set(basePose.normal());
		return local;
	}
}
