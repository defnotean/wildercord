package dev.wildercord.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.BiConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** 1.21.1 compat seam for the dropped render-state client: every method is inert. */
public final class RenderCompat {
	private RenderCompat() {}

	public static final java.util.function.Function<ResourceLocation, RenderType> GUI_TEXTURED = RenderType::guiTextured;

	public static <S> void submitModel(MultiBufferSource buffers, Model model, S state, PoseStack pose, RenderType type,
			int light, int overlay, int outlineColor) {}

	public static <S> void submitModel(MultiBufferSource buffers, Model model, S state, PoseStack pose, RenderType type,
			int light, int overlay, int color, int outlineColor) {}

	public static void rotate(PoseStack stack, com.mojang.math.Quaternionfc orientation) {}

	public static void rotateDegrees(PoseStack stack, Axis axis, float degrees) {}

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

	public static Object glowParticleLayer() { return null; }

	public static Object darkParticleLayer() { return null; }

	public static int lightOf(Object state) { return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT; }

	public static int outlineOf(Object state) { return 0; }
}
