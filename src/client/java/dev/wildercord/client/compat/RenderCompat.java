package dev.wildercord.client.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.mixin.RenderPipelinesAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.Model;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SwingAnimationType;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

/** Version-specific rendering, pose, pipeline, and input seams for Minecraft 26.2. */
public final class RenderCompat {
	private static final RenderPipeline WISP_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
		.withLocation(Wildercord.id("pipeline/wisp_glow"))
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withShaderDefine("NO_OVERLAY")
		.withShaderDefine("NO_CARDINAL_LIGHTING")
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withCull(false)
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	private static final RenderPipeline AURA_BODY_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
		.withLocation(Wildercord.id("pipeline/aura_body_glow"))
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withShaderDefine("NO_OVERLAY")
		.withShaderDefine("NO_CARDINAL_LIGHTING")
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withCull(false)
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	private static final RenderPipeline GLOW_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/glow_particle"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	private static final RenderPipeline DARK_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/dark_particle"))
		// 26.1.2 BlendFunction carries only factors, no blend op: vanilla's INVERT is the
		// closest darkening blend to the 26.2 SRC_ALPHA/ONE REVERSE_SUBTRACT.
		.withColorTargetState(new ColorTargetState(BlendFunction.INVERT))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	private RenderCompat() {}

	public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
			PoseStack pose, RenderType type, int light, int overlay, int outlineColor) {
		collector.submitModel(model, state, pose, type, light, overlay, outlineColor, null);
	}

	public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
			PoseStack pose, RenderType type, int light, int overlay, int color, int outlineColor) {
		collector.submitModel(model, state, pose, type, light, overlay, color, null, outlineColor, null);
	}

	public static void rotate(PoseStack stack, Quaternionfc orientation) {
		stack.mulPose(orientation);
	}

	public static void rotateDegrees(PoseStack stack, Axis axis, float degrees) {
		stack.mulPose(axis.rotationDegrees(degrees));
	}

	public static void startTextInput(Minecraft mc, Screen screen) {
		mc.textInputManager().startTextInput();
	}

	public static void stopTextInput(Minecraft mc, Screen screen) {
		mc.textInputManager().stopTextInput();
	}

	public static float swingProgress(ArmedEntityRenderState state) {
		return state.attackTime;
	}

	public static void setSwingProgress(ArmedEntityRenderState state, float progress) {
		state.attackTime = progress;
	}

	public static void overrideStabWithWhackGlaive(AvatarRenderState state) {
		if (state.swingAnimationType == SwingAnimationType.STAB) {
			state.swingAnimationType = SwingAnimationType.WHACK;
		}
	}

	public static void clearSwingForCustomRig(ArmedEntityRenderState state) {
		state.attackTime = 0;
		state.swingAnimationType = SwingAnimationType.WHACK;
	}

	public static Object swingObservation(AvatarRenderState state) {
		return state.swingAnimationType;
	}

	public static boolean isStabSwingType(AvatarRenderState state) {
		return state.swingAnimationType == SwingAnimationType.STAB;
	}

	public static boolean hasActiveStabSwing(AvatarRenderState state) {
		return state.attackTime > 0 && state.swingAnimationType == SwingAnimationType.STAB;
	}

	public static @Nullable SwingAnimationType activeMainHandSwingType(AvatarRenderState state) {
		return state.attackTime > 0 && state.attackArm == state.mainArm ? state.swingAnimationType : null;
	}

	public static @Nullable Model<?> livingModel(EntityRenderState state) {
		return Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(state) instanceof LivingEntityRenderer<?, ?, ?> living
			? living.getModel()
			: null;
	}

	public static RenderPipeline wispGlowPipeline() {
		return WISP_GLOW_PIPELINE;
	}

	public static RenderPipeline auraBodyGlowPipeline() {
		return AURA_BODY_GLOW_PIPELINE;
	}

	public static RenderPipeline glowParticlePipeline() {
		return GLOW_PARTICLE_PIPELINE;
	}

	public static RenderPipeline darkParticlePipeline() {
		return DARK_PARTICLE_PIPELINE;
	}

	public static RenderType wispGlowRenderType(Identifier texture) {
		return RenderType.create("wildercord_wisp_glow",
			RenderSetup.builder(WISP_GLOW_PIPELINE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup());
	}

	public static RenderType auraBodyGlowRenderType(Identifier texture) {
		return RenderType.create("wildercord_aura_body_glow",
			RenderSetup.builder(AURA_BODY_GLOW_PIPELINE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup());
	}

	public static SingleQuadParticle.Layer glowParticleLayer() {
		return new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, GLOW_PARTICLE_PIPELINE);
	}

	public static SingleQuadParticle.Layer darkParticleLayer() {
		return new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, DARK_PARTICLE_PIPELINE);
	}
}
