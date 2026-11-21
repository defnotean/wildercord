package dev.wildercord.client.compat;

import java.util.function.BiConsumer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.mixin.RenderPipelinesAccessor;
import dev.wildercord.client.compat.McQuadParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

/**
 * Version-specific rendering, pose, pipeline and input seams for Minecraft 1.21.8.
 *
 * <p>1.21.8 predates the render overhaul: there is no submit-node collector, so a model, a block or a
 * run of text is drawn straight into a buffer from {@link MultiBufferSource}, and a particle draws itself
 * rather than filling a render state (see {@link McQuadParticle} and {@link QuadParticleRenderState}).
 * The pipelines here are still the ones magic is described by, so a shader pack can be told how to draw
 * them; the quads themselves go on vanilla's particle and eye layers, which every pack handles.
 */
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
		// 1.21.8's BlendFunction carries only factors, no blend op: vanilla's INVERT is the closest
		// darkening blend to the SRC_ALPHA/ONE REVERSE_SUBTRACT the newer versions use.
		.withBlend(BlendFunction.INVERT)
		.withDepthWrite(false)
		.build();

	private RenderCompat() {}

	/** Draws a model, posed for {@code state}, in {@code type}. 1.21.8 has no submit queue: it goes on now. */
	public static <S> void submitModel(MultiBufferSource buffers, Model model, S state, PoseStack pose, RenderType type,
			int light, int overlay, int outlineColor) {
		poseFor(model, state);
		model.renderToBuffer(pose, buffers.getBuffer(type), light, overlay, outlineColor);
	}

	/** As above, with a tint as well as an outline colour; 1.21.8's models take one colour, so the tint wins. */
	public static <S> void submitModel(MultiBufferSource buffers, Model model, S state, PoseStack pose, RenderType type,
			int light, int overlay, int color, int outlineColor) {
		poseFor(model, state);
		model.renderToBuffer(pose, buffers.getBuffer(type), light, overlay, color);
	}

	/** The frame's partial tick, for the few 1.21.8 APIs that still ask for one. */
	public static float partialTick() {
		return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	/** Poses a model for its state, which 1.21.8's {@code renderToBuffer} does not do for you. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <S> void poseFor(Model model, S state) {
		if (model instanceof McModel posed) {
			posed.setupAnim(state);
		} else if (model instanceof EntityModel && state instanceof EntityRenderState) {
			((EntityModel) model).setupAnim((EntityRenderState) state);
		}
	}

	/** 1.21.8 keeps no packed light in the render state: full brightness. */
	public static int lightOf(Object state) {
		return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
	}

	/** 1.21.8 render states carry no outline tint: outlines are drawn without one. */
	public static int outlineOf(Object state) {
		return 0;
	}

	/** Draws something with its own geometry in {@code type}; the body writes to the buffer it is handed. */
	public static void submitCustomGeometry(MultiBufferSource buffers, PoseStack pose, RenderType type,
			BiConsumer<PoseStack.Pose, VertexConsumer> body) {
		body.accept(pose.last(), buffers.getBuffer(type));
	}

	/** Draws a block at the pose stack's position. */
	public static void submitBlock(MultiBufferSource buffers, PoseStack pose, BlockState block, int light, int overlay) {
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block, pose, buffers, light, overlay);
	}

	/** Draws text as glowing sign writing: filled in {@code color}, outlined in {@code outline}. */
	public static void submitText(MultiBufferSource buffers, PoseStack pose, Font font, FormattedCharSequence text,
			float x, float y, boolean shadow, int light, int color, int outline) {
		Matrix4f position = pose.last().pose();
		if (outline != 0) {
			font.drawInBatch8xOutline(text, x, y, color, outline, position, buffers, light);
			return;
		}
		font.drawInBatch(text, x, y, color, shadow, position, buffers, Font.DisplayMode.NORMAL, light, 0);
	}

	/** Draws a component as glowing sign writing. */
	public static void submitText(MultiBufferSource buffers, PoseStack pose, Font font, Component text,
			float x, float y, boolean shadow, int light, int color, int outline) {
		submitText(buffers, pose, font, text.getVisualOrderText(), x, y, shadow, light, color, outline);
	}

	public static void rotate(PoseStack stack, Quaternionfc orientation) {
		stack.mulPose(orientation);
	}

	/** The frame's camera, in the shape the newer render states hand to a renderer. */
	public static CameraRenderState cameraState() {
		CameraRenderState state = new CameraRenderState();
		net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		state.pos = dispatcher.camera.getPosition();
		state.orientation = dispatcher.cameraOrientation();
		return state;
	}

	public static void rotateDegrees(PoseStack stack, Axis axis, float degrees) {
		stack.mulPose(axis.rotationDegrees(degrees));
	}

	public static void startTextInput(Minecraft mc, Screen screen) {
		// 1.21.8 has no text input manager to tell; the focused edit box owns the IME itself.
	}

	public static void setTextInputArea(Minecraft mc, Screen screen, int x1, int y1, int x2, int y2) {
		// 1.21.8 positions the IME candidate box with the focused widget.
	}

	public static void stopTextInput(Minecraft mc, Screen screen) {
		// 1.21.8 clears the IME area with the widget focus.
	}

	/** 1.21.8 has no swing animation component: every native swing is a plain whack. */
	public enum SwingAnim { NONE, WHACK, STAB }

	public static float swingProgress(ArmedEntityRenderState state) {
		return state instanceof HumanoidRenderState humanoid ? humanoid.attackTime : 0F;
	}

	public static void setSwingProgress(ArmedEntityRenderState state, float progress) {
		if (state instanceof HumanoidRenderState humanoid) {
			humanoid.attackTime = progress;
		}
	}

	/** 1.21.8 render states carry no ItemStacks: the renderer mixin notes the entity's real stacks as each state is extracted. */
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

	/** 1.21.8 carries no item render state for the hands: only what the entity wears on its head. */
	public static net.minecraft.client.renderer.item.ItemStackRenderState mainHandItemState(ArmedEntityRenderState state) {
		return null;
	}

	/** Draws an item render state, if the platform has one. 1.21.8 draws hands from the stack instead. */
	public static void submitItemState(net.minecraft.client.renderer.item.ItemStackRenderState item, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay, int outlineColor) {
		if (item != null) {
			item.render(pose, buffers, light, overlay);
		}
	}

	/** A baked quad's corner position: 1.21.8 packs the vertices as ints (stride 8, position first). */
	public static org.joml.Vector3fc quadPosition(net.minecraft.client.renderer.block.model.BakedQuad quad, int vertex) {
		int[] v = quad.vertices();
		int base = vertex * 8;
		return new org.joml.Vector3f(Float.intBitsToFloat(v[base]), Float.intBitsToFloat(v[base + 1]), Float.intBitsToFloat(v[base + 2]));
	}

	public static void overrideStabWithWhackGlaive(PlayerRenderState state) {
		// 1.21.8 has no stab/whack distinction; nothing to override.
	}

	public static void clearSwingForCustomRig(ArmedEntityRenderState state) {
		setSwingProgress(state, 0F);
	}

	public static Object swingObservation(PlayerRenderState state) {
		return activeMainHandSwingType(state);
	}

	public static boolean isStabSwingType(PlayerRenderState state) {
		// 1.21.8 has no piercing weapons: nothing stabs.
		return false;
	}

	public static boolean hasActiveStabSwing(PlayerRenderState state) {
		return false;
	}

	public static SwingAnim activeMainHandSwingType(PlayerRenderState state) {
		return state.attackTime > 0 && state.attackArm == state.mainArm ? SwingAnim.WHACK : null;
	}

	public static Model livingModel(EntityRenderState state) {
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
		// 1.21.8 has no RenderSetup: vanilla's emissive eyes layer is the same additive, depthless glow.
		return RenderType.eyes(texture);
	}

	public static RenderType auraBodyGlowRenderType(ResourceLocation texture) {
		return RenderType.eyes(texture);
	}

	/**
	 * Magic's added light. 1.21.8 has no per-particle layer object to hand the engine, so a layer is just
	 * the render type its quads go on: vanilla's glowing-eyes type, which adds light and writes no depth.
	 */
	public static McQuadParticle.Layer glowParticleLayer() {
		return new McQuadParticle.Layer(RenderType.eyes(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES));
	}

	/**
	 * Darkness. Drawn with the translucent particles: 1.21.8 can only build a particle layer out of a
	 * vanilla render type, and the translucent one writes no depth, so it never hides what's behind it.
	 */
	public static McQuadParticle.Layer darkParticleLayer() {
		return new McQuadParticle.Layer(RenderType.translucentParticle(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES));
	}
}
