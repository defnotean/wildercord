package dev.wildercord.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.BlendOp;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
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
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

/**
 * Client rendering, model submission, matrix rotation, swing-state and pipeline seam (Minecraft 26.3).
 */
public final class RenderCompat {
	private static final SwingAnimation GLAIVE_SWING = new SwingAnimation(SwingAnimationType.WHACK, 10);

	private static final RenderPipeline WISP_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET)
		.withLocation(Wildercord.id("pipeline/wisp_glow"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();
	private static final OitPipelineSet WISP_GLOW_OIT = OitPipelineSet.builder("wildercord_wisp_glow",
		RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build();

	private static final RenderPipeline AURA_BODY_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET)
		.withLocation(Wildercord.id("pipeline/aura_body_glow"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();
	private static final OitPipelineSet AURA_BODY_GLOW_OIT = OitPipelineSet.builder("wildercord_aura_body_glow",
		RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build();

	private static final RenderPipeline GLOW_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/glow_particle"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();
	private static final OitPipelineSet GLOW_PARTICLE_OIT = OitPipelineSet.builder("wildercord_glow_particle",
		RenderPipeline.builder(RenderPipelines.OIT_PARTICLE_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build();

	private static final RenderPipeline DARK_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/dark_particle"))
		.withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendOp.REVERSE_SUBTRACT)))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();

	private RenderCompat() {}

	public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
			PoseStack pose, RenderType type, int light, int overlay, int outlineColor) {
		collector.submitModel(model, state, pose, type, light, overlay, outlineColor);
	}

	public static <S> void submitModel(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
			PoseStack pose, RenderType type, int light, int overlay, int color, int outlineColor) {
		collector.submitModel(model, state, pose, type, light, overlay, color, null, outlineColor);
	}

	public static void rotate(PoseStack stack, Quaternionfc orientation) {
		stack.rotate(orientation);
	}

	public static void rotateDegrees(PoseStack stack, Axis axis, float degrees) {
		stack.rotateDegrees(axis, degrees);
	}

	public static void startTextInput(Minecraft mc, Screen screen) {
		mc.textInputManager().startTextInput(screen);
	}

	public static void stopTextInput(Minecraft mc, Screen screen) {
		mc.textInputManager().stopTextInput(screen);
	}

	public static float swingProgress(ArmedEntityRenderState state) {
		return state.swingAnimation;
	}

	public static void setSwingProgress(ArmedEntityRenderState state, float progress) {
		state.swingAnimation = progress;
	}

	public static void overrideStabWithWhackGlaive(AvatarRenderState state) {
		if (state.currentSwing != null && state.currentSwing.animation().type() == SwingAnimationType.STAB) {
			state.currentSwing = new LivingEntity.SwingDescription(state.currentSwing.hand(), GLAIVE_SWING, 10);
		}
	}

	public static void clearSwingForCustomRig(ArmedEntityRenderState state) {
		state.swingAnimation = 0;
		state.currentSwing = null;
	}

	public static Object swingObservation(AvatarRenderState state) {
		return state.swingAnimation;
	}

	public static boolean isStabSwingType(AvatarRenderState state) {
		return state.currentSwing != null && state.currentSwing.animation().type() == SwingAnimationType.STAB;
	}

	public static boolean hasActiveStabSwing(AvatarRenderState state) {
		return state.currentSwing != null && state.currentSwing.animation().type() == SwingAnimationType.STAB;
	}

	public static @Nullable SwingAnimationType activeMainHandSwingType(AvatarRenderState state) {
		return state.currentSwing != null && state.currentSwing.hand() == InteractionHand.MAIN_HAND
			? state.currentSwing.animation().type()
			: null;
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
			RenderSetup.builder(WISP_GLOW_PIPELINE).setOitPipelines(WISP_GLOW_OIT).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup());
	}

	public static RenderType auraBodyGlowRenderType(Identifier texture) {
		return RenderType.create("wildercord_aura_body_glow",
			RenderSetup.builder(AURA_BODY_GLOW_PIPELINE).setOitPipelines(AURA_BODY_GLOW_OIT).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup());
	}

	public static SingleQuadParticle.Layer glowParticleLayer() {
		return new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, GLOW_PARTICLE_PIPELINE, GLOW_PARTICLE_OIT);
	}

	public static SingleQuadParticle.Layer darkParticleLayer() {
		return new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, DARK_PARTICLE_PIPELINE);
	}
}
