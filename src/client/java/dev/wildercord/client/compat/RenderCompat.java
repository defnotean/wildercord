package dev.wildercord.client.compat;

import java.util.function.BiConsumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.McQuadParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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





	/** Where 1.21.5+ names a pipeline, 1.21.4 wants this: the gui textured layer for a sprite sheet. */
	public static final java.util.function.Function<ResourceLocation, RenderType> GUI_TEXTURED = RenderType::guiTextured;

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
		stack.mulPose(new org.joml.Quaternionf(orientation));
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

	public static float swingProgress(LivingEntityRenderState state) {
		return state instanceof HumanoidRenderState humanoid ? humanoid.attackTime : 0F;
	}

	public static void setSwingProgress(LivingEntityRenderState state, float progress) {
		if (state instanceof HumanoidRenderState humanoid) {
			humanoid.attackTime = progress;
		}
	}

	/** 1.21.3 render states already carry the real hand stacks; there is nothing to note on extraction. */
	public static void captureHandStacks(net.minecraft.world.entity.LivingEntity entity, Object state) {
		// 1.21.3: the state's *HandItem fields are read directly by the seams below.
	}

	private static ItemStack[] carriedStacks(LivingEntityRenderState state) {
		ItemStack main = state.getMainHandItem();
		ItemStack left = state.mainArm == net.minecraft.world.entity.HumanoidArm.LEFT ? main : state.leftHandItem;
		ItemStack right = state.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT ? main : state.rightHandItem;
		return new ItemStack[]{main, left, right};
	}

	public static ItemStack mainHandStack(LivingEntityRenderState state) {
		return carriedStacks(state)[0];
	}

	public static ItemStack leftHandStack(LivingEntityRenderState state) {
		return carriedStacks(state)[1];
	}

	public static ItemStack rightHandStack(LivingEntityRenderState state) {
		return carriedStacks(state)[2];
	}

	/** 1.21.3 carries no item render state for the hands: only what the entity wears on its head. */
	public static net.minecraft.client.renderer.item.ItemStackRenderState mainHandItemState(LivingEntityRenderState state) {
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
		int[] v = quad.getVertices();
		int base = vertex * 8;
		return new org.joml.Vector3f(Float.intBitsToFloat(v[base]), Float.intBitsToFloat(v[base + 1]), Float.intBitsToFloat(v[base + 2]));
	}

	public static void overrideStabWithWhackGlaive(PlayerRenderState state) {
		// 1.21.8 has no stab/whack distinction; nothing to override.
	}

	/** 1.21.3 has no arm-pose fields on its render states; the 1.21.4 spear-to-item fix is inert. */
	public static void stripSpearArmPose(LivingEntityRenderState state) {
		// no-op on 1.21.3: LivingEntityRenderState answers poses through hasPose(Pose).
	}

	public static void clearSwingForCustomRig(LivingEntityRenderState state) {
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
		// 1.21.4’s dispatcher only answers for entities: this seam serves the local player’s view model.
		net.minecraft.client.player.AbstractClientPlayer player = Minecraft.getInstance().player;
		return player != null
			&& Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof LivingEntityRenderer<?, ?, ?> living
			? living.getModel()
			: null;
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
		return new McQuadParticle.Layer(translucentParticleRenderType());
	}

	/** 1.21.3 has no particle render types; the translucent and cutout types stand in. */
	public static RenderType translucentParticleRenderType() {
		return RenderType.translucent();
	}

	public static RenderType opaqueParticleRenderType() {
		return RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES);
	}

	/** 1.21.3 has no {@code Minecraft#getItemModelResolver}; the stand-in records what the item renderer needs. */
	public static void resolveForLiving(net.minecraft.client.renderer.item.ItemStackRenderState state, ItemStack stack,
			net.minecraft.world.item.ItemDisplayContext context, net.minecraft.world.entity.LivingEntity entity) {
		itemModelResolver().updateForLiving(state, stack, context, false, entity);
	}

	public static net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver() {
		return new net.minecraft.client.renderer.item.ItemModelResolver();
	}

	public static boolean shouldPlaySwapAnimation(ItemStack stack) {
		return itemModelResolver().shouldPlaySwapAnimation(stack);
	}

	public static net.minecraft.client.resources.model.EquipmentAssetManager equipmentAssets(
			net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		return new net.minecraft.client.resources.model.EquipmentAssetManager(context.getEquipmentModels());
	}

	/** 1.21.3 names worn stacks {@code *Item} on Living/HumanoidRenderState, not {@code *Equipment}. */
	public static ItemStack equipment(LivingEntityRenderState state, net.minecraft.world.entity.EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> state.headItem;
			case CHEST -> state instanceof HumanoidRenderState humanoid ? humanoid.chestItem : ItemStack.EMPTY;
			case LEGS -> state instanceof HumanoidRenderState humanoid ? humanoid.legsItem : ItemStack.EMPTY;
			case FEET -> state instanceof HumanoidRenderState humanoid ? humanoid.feetItem : ItemStack.EMPTY;
			default -> ItemStack.EMPTY;
		};
	}

	public static ItemStack heldOnHead(LivingEntityRenderState state) {
		return state.headItem;
	}

	/** 1.21.3's {@code Equippable#model} is the id 1.21.4 renamed to {@code assetId}. */
	public static java.util.Optional<ResourceLocation> assetId(net.minecraft.world.item.equipment.Equippable equippable) {
		return equippable == null ? java.util.Optional.empty() : equippable.model();
	}

	/** 1.21.3's {@code Level#addParticle} has no trailing colour arguments. */
	public static void spawnParticle(net.minecraft.world.level.Level level, net.minecraft.core.particles.ParticleOptions options,
			double x, double y, double z) {
		level.addParticle(options, true, x, y, z, 0.0, 0.0, 0.0);
	}

	/**
	 * Sprite lookup for an atlas. 1.21.8's atlas set only carries the block and entity sheets: the
	 * particle atlas belongs to the particle engine, which registers it with the texture manager,
	 * so the vanilla lookup throws before it can answer.
	 */
	public static java.util.function.Function<ResourceLocation, TextureAtlasSprite> atlas(ResourceLocation atlas) {
		Minecraft mc = Minecraft.getInstance();
		try {
			net.minecraft.client.renderer.texture.AbstractTexture held = mc.getTextureManager().getTexture(atlas);
			if (held instanceof net.minecraft.client.renderer.texture.TextureAtlas stitched) {
				return stitched::getSprite;
			}
		} catch (RuntimeException absent) {
			// not registered with the texture manager; the atlas set may still know it
		}
		return mc.getTextureAtlas(atlas);
	}
}
