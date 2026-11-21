package dev.wildercord.client.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
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
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;

/** Version-specific rendering, pose, pipeline, and input seams for Minecraft 26.2. */
public final class RenderCompat {
	private static final RenderPipeline WISP_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$entityEmissiveSnippet())
		.withLocation(Wildercord.id("pipeline/wisp_glow"))
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withShaderDefine("NO_OVERLAY")
		.withShaderDefine("NO_CARDINAL_LIGHTING")
		.withBlend(BlendFunction.LIGHTNING)
		.withCull(false)
		.withDepthWrite(false)
		.build();

	private static final RenderPipeline AURA_BODY_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$entityEmissiveSnippet())
		.withLocation(Wildercord.id("pipeline/aura_body_glow"))
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withShaderDefine("NO_OVERLAY")
		.withShaderDefine("NO_CARDINAL_LIGHTING")
		.withBlend(BlendFunction.LIGHTNING)
		.withCull(false)
		.withDepthWrite(false)
		.build();

	private static final RenderPipeline GLOW_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/glow_particle"))
		.withBlend(BlendFunction.LIGHTNING)
		.withDepthWrite(false)
		.build();

	private static final RenderPipeline DARK_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelinesAccessor.wildercord$particleSnippet())
		.withLocation(Wildercord.id("pipeline/dark_particle"))
		// 26.1.2 BlendFunction carries only factors, no blend op: vanilla's INVERT is the
		// closest darkening blend to the 26.2 SRC_ALPHA/ONE REVERSE_SUBTRACT.
		.withBlend(BlendFunction.INVERT)
		.withDepthWrite(false)
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
		// 1.21.11 routes IME focus through the focused widget itself; nothing to start.
	}

	public static void setTextInputArea(Minecraft mc, Screen screen, int x1, int y1, int x2, int y2) {
		// 1.21.11 positions the IME candidate box with the focused widget.
	}

	public static void stopTextInput(Minecraft mc, Screen screen) {
		// 1.21.11 clears the IME area with the widget focus.
	}

	/** 1.21.10 has no swing animation component: every native swing is a plain whack. */
	public enum SwingAnim { NONE, WHACK, STAB }

	public static float swingProgress(ArmedEntityRenderState state) {
		return state instanceof HumanoidRenderState humanoid ? humanoid.attackTime : 0F;
	}

	public static void setSwingProgress(ArmedEntityRenderState state, float progress) {
		if (state instanceof HumanoidRenderState humanoid) {
			humanoid.attackTime = progress;
		}
	}

	/** 1.21.10 render states carry no ItemStacks: the renderer mixin notes the entity's real stacks as each state is extracted. */
	public static void captureHandStacks(net.minecraft.world.entity.LivingEntity entity, Object state) {
		if (!(state instanceof ArmedEntityRenderState armed) || !(armed instanceof dev.wildercord.client.HandStacks carried)) {
			return;
		}
		ItemStack main = entity.getMainHandItem();
		ItemStack off = entity.getOffhandItem();
		ItemStack left = entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? main : off;
		ItemStack right = entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? main : off;
		carried.wildercord$setHandStacks(new ItemStack[]{main.copy(), left.copy(), right.copy()});
	}

	private static ItemStack[] carriedStacks(ArmedEntityRenderState state) {
		return state instanceof dev.wildercord.client.HandStacks carried ? carried.wildercord$handStacks() : null;
	}

	public static ItemStack mainHandStack(ArmedEntityRenderState state) {
		ItemStack[] stacks = carriedStacks(state);
		return stacks != null && stacks.length == 3 ? stacks[0] : ItemStack.EMPTY;
	}

	public static ItemStack leftHandStack(ArmedEntityRenderState state) {
		ItemStack[] stacks = carriedStacks(state);
		return stacks != null && stacks.length == 3 ? stacks[1] : ItemStack.EMPTY;
	}

	public static ItemStack rightHandStack(ArmedEntityRenderState state) {
		ItemStack[] stacks = carriedStacks(state);
		return stacks != null && stacks.length == 3 ? stacks[2] : ItemStack.EMPTY;
	}

	public static net.minecraft.client.renderer.item.ItemStackRenderState mainHandItemState(ArmedEntityRenderState state) {
		return state.getMainHandItem();
	}

	/** A baked quad's corner position: 1.21.10 packs the vertices as ints (stride 8, position first). */
	public static org.joml.Vector3fc quadPosition(net.minecraft.client.renderer.block.model.BakedQuad quad, int vertex) {
		int[] v = quad.vertices();
		int base = vertex * 8;
		return new org.joml.Vector3f(Float.intBitsToFloat(v[base]), Float.intBitsToFloat(v[base + 1]), Float.intBitsToFloat(v[base + 2]));
	}

	public static void overrideStabWithWhackGlaive(AvatarRenderState state) {
		// 1.21.10 has no stab/whack distinction; nothing to override.
	}

	public static void clearSwingForCustomRig(ArmedEntityRenderState state) {
		setSwingProgress(state, 0F);
	}

	public static Object swingObservation(AvatarRenderState state) {
		return activeMainHandSwingType(state);
	}

	public static boolean isStabSwingType(AvatarRenderState state) {
		// 1.21.10 has no piercing weapons: nothing stabs.
		return false;
	}

	public static boolean hasActiveStabSwing(AvatarRenderState state) {
		return false;
	}

	public static SwingAnim activeMainHandSwingType(AvatarRenderState state) {
		return state.attackTime > 0 && state.attackArm == state.mainArm ? SwingAnim.WHACK : null;
	}

	public static  Model<?> livingModel(EntityRenderState state) {
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

	public static RenderType wispGlowRenderType(ResourceLocation texture) {
		// 1.21.10 has no RenderSetup: vanilla's emissive eyes layer is the same additive, depthless glow.
		return RenderType.eyes(texture);
	}

	public static RenderType auraBodyGlowRenderType(ResourceLocation texture) {
		return RenderType.eyes(texture);
	}

	public static SingleQuadParticle.Layer glowParticleLayer() {
		return new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, GLOW_PARTICLE_PIPELINE);
	}

	public static SingleQuadParticle.Layer darkParticleLayer() {
		return new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_PARTICLES, DARK_PARTICLE_PIPELINE);
	}
}
