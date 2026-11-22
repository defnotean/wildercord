package dev.wildercord.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.21.5 is on this side of the GUI render state: drawing is immediate and {@code GuiGraphics} still answers
 * to the older names, and there is no {@code GuiGraphicsExtractor} to hand the mod's HUD elements, screens and
 * toasts to. This wraps one so those call sites keep the 26.x vocabulary ({@code text}, {@code centeredText},
 * {@code item}, {@code itemDecorations}, {@code outline}, {@code entity}, ...) and only the boundary methods
 * that vanilla hands a {@code GuiGraphics} to need wrapping.
 *
 * <p>Pipelines are dropped on the floor: this version picks its blend state from a {@link RenderType}, and the
 * mod only ever asks for the plain textured GUI path. The {@code pose()} adapter speaks the newer
 * {@code Matrix3x2fStack} verbs ({@code pushMatrix}, {@code translate}, {@code scale}, {@code rotate}) over a
 * {@link PoseStack}.</p>
 */
public final class GuiGraphicsExtractor {
	private final GuiGraphics g;

	public GuiGraphicsExtractor(GuiGraphics g) {
		this.g = g;
	}

	/** The wrapped 1.21.5 graphics, for the few places that need it directly. */
	public GuiGraphics delegate() {
		return g;
	}

	public int guiWidth() {
		return g.guiWidth();
	}

	public int guiHeight() {
		return g.guiHeight();
	}

	/** The 26.x matrix verbs over this version's {@link PoseStack}. */
	public final class Pose {
		private final PoseStack ps;

		Pose(PoseStack ps) {
			this.ps = ps;
		}

		public void pushMatrix() {
			ps.pushPose();
		}

		public void popMatrix() {
			ps.popPose();
		}

		public void translate(float x, float y) {
			ps.translate(x, y, 0.0F);
		}

		public void translate(float x, float y, float z) {
			ps.translate(x, y, z);
		}

		public void scale(float x, float y) {
			ps.scale(x, y, 1.0F);
		}

		public void rotate(float radians) {
			ps.mulPose(new Quaternionf().rotateZ(radians));
		}
	}

	public Pose pose() {
		return new Pose(g.pose());
	}

	public void nextStratum() {
		// No strata before the GUI render state; draws land in submission order.
	}

	public void enableScissor(int x, int y, int width, int height) {
		g.enableScissor(x, y, width, height);
	}

	public void disableScissor() {
		g.disableScissor();
	}

	public boolean containsPointInScissor(int x, int y) {
		return g.containsPointInScissor(x, y);
	}

	public void horizontalLine(int x1, int x2, int y, int color) {
		g.hLine(x1, x2, y, color);
	}

	public void verticalLine(int x, int y1, int y2, int color) {
		g.vLine(x, y1, y2, color);
	}

	public void fill(int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, color);
	}

	public void fill(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, color);
	}

	public void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
		g.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
	}

	public void outline(int x1, int y1, int x2, int y2, int color) {
		g.renderOutline(x1, y1, x2 - x1, y2 - y1, color);
	}

	public void textHighlight(int x, int y, int width, int height, boolean below) {
		// 1.21.5 has no GuiGraphics#textHighlight and no GUI_INVERT pipeline. The blue fill is the
		// half of the newer vanilla call that carries the selection itself; the invert pass only
		// reversed what was underneath, which this version has no pipeline for.
		g.fill(RenderType.guiTextHighlight(), x, y, x + width, y + height, 0xFF0000FF);
	}

	public void text(Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color);
	}

	public void text(Font font, String text, int x, int y, int color, boolean shadow) {
		g.drawString(font, text, x, y, color, shadow);
	}

	public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
		g.drawString(font, text, x, y, color);
	}

	public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		g.drawString(font, text, x, y, color, shadow);
	}

	public void text(Font font, Component text, int x, int y, int color) {
		g.drawString(font, text, x, y, color);
	}

	public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
		g.drawString(font, text, x, y, color, shadow);
	}

	public void centeredText(Font font, String text, int x, int y, int color) {
		g.drawCenteredString(font, text, x, y, color);
	}

	public void centeredText(Font font, Component text, int x, int y, int color) {
		g.drawCenteredString(font, text, x, y, color);
	}

	public void centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
		g.drawCenteredString(font, text, x, y, color);
	}

	public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
		g.drawWordWrap(font, text, x, y, width, color);
	}

	public void textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color, boolean shadow) {
		g.drawWordWrap(font, text, x, y, width, color, shadow);
	}

	public void textWithBackdrop(Font font, Component text, int x, int y, int width, int color) {
		g.drawStringWithBackdrop(font, text, x, y, width, color);
	}

	public void blit(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height) {
		g.blit(RenderType::guiTextured, atlas, x, y, u, v, regionWidth, regionHeight, width, height);
	}

	public void blit(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, int blitOffset) {
		g.blit(RenderType::guiTextured, atlas, x, y, u, v, regionWidth, regionHeight, width, height, blitOffset);
	}

	public void blit(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, int textureU, int textureV, int blitOffset) {
		// 1.21.5 has no colour on a raw blit; the circle textures read fine untinted.
		g.blit(RenderType::guiTextured, atlas, x, y, (float) (u + textureU), (float) (v + textureV),
			regionWidth, regionHeight, width, height, 256, 256);
	}

	public void blit(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, int textureU, int textureV, int textureWidth, int textureHeight) {
		g.blit(RenderType::guiTextured, atlas, x, y, (float) (u + textureU), (float) (v + textureV),
			regionWidth, regionHeight, width, height, textureWidth, textureHeight);
	}

	public void blit(ResourceLocation atlas, int x, int y, int width, int height, float u, float v, float regionWidth, float regionHeight) {
		g.blit(RenderType::guiTextured, atlas, x, y, u, v, (int) regionWidth, (int) regionHeight, width, height, 0);
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height);
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation sprite, int x, int y, int width, int height, float blitOffset) {
		// No offset on this version's sprite blits; the depth is carried on the pose instead.
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, blitOffset);
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height);
		g.pose().popPose();
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height, color);
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation sprite, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight) {
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height, u, v, regionWidth, regionHeight);
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, ResourceLocation sprite, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int blitOffset) {
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, blitOffset);
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height, u, v, regionWidth, regionHeight);
		g.pose().popPose();
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height);
	}

	public void blitSprite(java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.RenderType> pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset) {
		g.pose().pushPose();
		g.pose().translate(0.0F, 0.0F, blitOffset);
		g.blitSprite(RenderType::guiTextured, sprite, x, y, width, height);
		g.pose().popPose();
	}

	public void item(ItemStack stack, int x, int y) {
		g.renderItem(stack, x, y);
	}

	public void item(ItemStack stack, int x, int y, int light) {
		g.renderItem(stack, x, y, light);
	}

	public void item(LivingEntity entity, ItemStack stack, int x, int y, int light) {
		g.renderItem(entity, stack, x, y, light);
	}

	public void fakeItem(ItemStack stack, int x, int y) {
		g.renderFakeItem(stack, x, y);
	}

	public void fakeItem(ItemStack stack, int x, int y, int light) {
		g.renderFakeItem(stack, x, y, light);
	}

	public void itemDecorations(Font font, ItemStack stack, int x, int y) {
		g.renderItemDecorations(font, stack, x, y);
	}

	public void itemDecorations(Font font, ItemStack stack, int x, int y, String text) {
		g.renderItemDecorations(font, stack, x, y, text);
	}

	/**
	 * 1.21.5 has no {@code GuiGraphics#submitEntityRenderState}, so the figure is drawn here and now, through
	 * the renderer that built the state. The box is the frame the caller drew, {@code yBottom} is the pixels
	 * per world unit and {@code pos} shifts the figure inside the frame.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void entity(EntityRenderer renderer, EntityRenderState state, float yBottom, Vector3f pos,
			Quaternionf leftRotation, Quaternionf rightRotation, int x1, int y1, int x2, int y2) {
		PoseStack ps = g.pose();
		ps.pushPose();
		ps.translate(x1 + (x2 - x1) / 2.0, y1, 0.0);
		ps.scale(yBottom, -yBottom, yBottom);
		if (leftRotation != null) {
			ps.mulPose(leftRotation);
		}
		if (rightRotation != null) {
			ps.mulPose(rightRotation);
		}
		ps.translate(pos.x, pos.y, pos.z);
		renderer.render(state, ps, Minecraft.getInstance().renderBuffers().bufferSource(), 0xF000F0);
		ps.popPose();
	}

	public void setTooltipForNextFrame(Component tooltip, int x, int y) {
		g.renderTooltip(Minecraft.getInstance().font, tooltip, x, y);
	}

	public void setTooltipForNextFrame(List<? extends FormattedCharSequence> lines, int x, int y) {
		g.renderTooltip(Minecraft.getInstance().font, new ArrayList<FormattedCharSequence>(lines), x, y);
	}

	public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
		g.renderTooltip(font, lines, x, y);
	}

	public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
		g.renderTooltip(font, stack, x, y);
	}

	public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> custom, int x, int y) {
		g.renderTooltip(font, lines, custom, x, y);
	}

	public void setTooltipForNextFrame(Font font, Component tooltip, int x, int y) {
		g.renderTooltip(font, tooltip, x, y);
	}

	public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
		g.renderComponentTooltip(font, lines, x, y);
	}
}
