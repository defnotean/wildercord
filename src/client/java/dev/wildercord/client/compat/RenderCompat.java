package dev.wildercord.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BiConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** 1.21.1 compat seam for the dropped render-state client: every method is inert. */
public final class RenderCompat {
	private RenderCompat() {}

	/** 1.21.2 names a render type for every GUI blit; 1.21.1 has one text type and takes no argument. */
	public static final java.util.function.Function<ResourceLocation, RenderType> GUI_TEXTURED = RenderType::text;

	public static <S> void submitModel(MultiBufferSource buffers, Model model, S state, PoseStack pose, RenderType type,
			int light, int overlay, int outlineColor) {}

	public static <S> void submitModel(MultiBufferSource buffers, Model model, S state, PoseStack pose, RenderType type,
			int light, int overlay, int color, int outlineColor) {}

	public static void rotateDegrees(PoseStack stack, Axis axis, float degrees) {
		stack.mulPose(axis.rotationDegrees(degrees));
	}

	/** 1.21.1 hands the camera's turn as a joml quaternion, not as a {@code Quaternionfc} view. */
	public static void rotate(PoseStack stack, Quaternionf orientation) {
		stack.mulPose(orientation);
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

	public static void setTextInputArea(Minecraft mc, Screen screen, int x1, int y1, int x2, int y2) {
		// 1.21.1 positions the IME candidate box with the focused widget.
	}

	/**
	 * Magic's added light. 1.21.1 has no per-particle layer object to hand the engine, so a layer is just
	 * the render type its quads go on: vanilla's glowing-eyes type, which adds light and writes no depth.
	 */
	public static McQuadParticle.Layer glowParticleLayer() {
		return new McQuadParticle.Layer(RenderType.eyes(TextureAtlas.LOCATION_PARTICLES));
	}

	/**
	 * Darkness. Drawn with the translucent particles: 1.21.1 can only build a particle layer out of a
	 * vanilla render type, and the translucent one writes no depth, so it never hides what's behind it.
	 */
	public static McQuadParticle.Layer darkParticleLayer() {
		return new McQuadParticle.Layer(translucentParticleRenderType());
	}

	/** 1.21.1 has no particle render types; the translucent and cutout types stand in. */
	public static RenderType translucentParticleRenderType() {
		return RenderType.translucent();
	}

	public static RenderType opaqueParticleRenderType() {
		return RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_PARTICLES);
	}

	/** 1.21.1's {@code Level#addParticle} has no trailing colour arguments. */
	public static void spawnParticle(Level level, ParticleOptions options, double x, double y, double z) {
		level.addParticle(options, true, x, y, z, 0.0, 0.0, 0.0);
	}

	/**
	 * Sprite lookup for an atlas. 1.21.1's atlas set only carries the block and entity sheets: the
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

	public static void startTextInput(Minecraft mc, Screen screen) {}

	public static void stopTextInput(Minecraft mc, Screen screen) {}

	public static float swingProgress(Object state) { return 0F; }

	public static void setSwingProgress(Object state, float progress) {}

	public static void overrideStabWithWhackGlaive(Object state) {}

	public static void clearSwingForCustomRig(Object state) {}

	public static Object swingObservation(Object state) { return null; }

	public static boolean isStabSwingType(Object state) { return false; }

	public static boolean hasActiveStabSwing(Object state) { return false; }

	public static Object activeMainHandSwingType(Object state) { return null; }

	public static Model livingModel(Object state) { return null; }

	public static RenderType wispGlowRenderType(ResourceLocation texture) { return RenderType.eyes(texture); }

	public static RenderType auraBodyGlowRenderType(ResourceLocation texture) { return RenderType.eyes(texture); }

	public static int lightOf(Object state) { return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT; }

	public static int outlineOf(Object state) { return 0; }
}
