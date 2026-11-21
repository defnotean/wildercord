package dev.wildercord.client.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.21.11 has no {@code GuiGraphicsExtractor}: the deferred GUI drawing the mod was written against is
 * {@link GuiGraphics} itself there, under its older immediate-mode names. This wraps one so the HUD elements,
 * screens and toasts keep their 26.x call sites ({@code text}, {@code centeredText}, {@code item},
 * {@code itemDecorations}, {@code outline}, {@code entity}, ...) and only the boundary methods that vanilla
 * hands a {@code GuiGraphics} to need wrapping.
 */
public final class GuiGraphicsExtractor {
	private final GuiGraphics g;

	public GuiGraphicsExtractor(GuiGraphics g) {
		this.g = g;
	}

	/** The wrapped 1.21.11 graphics, for the few places that need it directly. */
	public GuiGraphics delegate() {
		return g;
	}

	public int guiWidth() {
		return g.guiWidth();
	}

	public int guiHeight() {
		return g.guiHeight();
	}

	public Matrix3x2fStack pose() {
		return g.pose();
	}

	public void nextStratum() {
		g.nextStratum();
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

	public void fill(RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color) {
		g.fill(pipeline, x1, y1, x2, y2, color);
	}

	public void fill(RenderPipeline pipeline, TextureSetup setup, int x1, int y1, int x2, int y2) {
		g.fill(pipeline, setup, x1, y1, x2, y2);
	}

	public void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
		g.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
	}

	public void outline(int x1, int y1, int x2, int y2, int color) {
		g.renderOutline(x1, y1, x2, y2, color);
	}

	public void textHighlight(int x, int y, int width, int height, boolean below) {
		g.textHighlight(x, y, width, height, below);
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

	public void blit(RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height) {
		g.blit(pipeline, atlas, x, y, u, v, regionWidth, regionHeight, width, height);
	}

	public void blit(RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, int blitOffset) {
		g.blit(pipeline, atlas, x, y, u, v, regionWidth, regionHeight, width, height, blitOffset);
	}

	public void blit(RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, int textureU, int textureV, int blitOffset) {
		// 1.21.11 has no colour on a raw blit; the circle textures read fine untinted.
		g.blit(pipeline, atlas, x, y, (float) (u + textureU), (float) (v + textureV), regionWidth, regionHeight, width, height, 256, 256);
	}

	public void blit(RenderPipeline pipeline, Identifier atlas, int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, int textureU, int textureV, int textureWidth, int textureHeight) {
		g.blit(pipeline, atlas, x, y, (float) (u + textureU), (float) (v + textureV), regionWidth, regionHeight, width, height, textureWidth, textureHeight);
	}

	public void blit(Identifier atlas, int x, int y, int width, int height, float u, float v, float regionWidth, float regionHeight) {
		g.blit(atlas, x, y, width, height, u, v, regionWidth, regionHeight);
	}

	public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		g.blitSprite(pipeline, sprite, x, y, width, height);
	}

	public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, float blitOffset) {
		g.blitSprite(pipeline, sprite, x, y, width, height, blitOffset);
	}

	public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
		g.blitSprite(pipeline, sprite, x, y, width, height, color);
	}

	public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight) {
		g.blitSprite(pipeline, sprite, x, y, width, height, u, v, regionWidth, regionHeight);
	}

	public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight, int blitOffset) {
		g.blitSprite(pipeline, sprite, x, y, width, height, u, v, regionWidth, regionHeight, blitOffset);
	}

	public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
		g.blitSprite(pipeline, sprite, x, y, width, height);
	}

	public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int blitOffset) {
		g.blitSprite(pipeline, sprite, x, y, width, height, blitOffset);
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

	public void map(MapRenderState state) {
		g.submitMapRenderState(state);
	}

	public void entity(EntityRenderState state, float yBottom, Vector3f pos, Quaternionf leftRotation, Quaternionf rightRotation, int x1, int y1, int x2, int y2) {
		g.submitEntityRenderState(state, yBottom, pos, leftRotation, rightRotation, x1, y1, x2, y2);
	}

	public void setTooltipForNextFrame(Component tooltip, int x, int y) {
		g.setTooltipForNextFrame(tooltip, x, y);
	}

	public void setTooltipForNextFrame(List<? extends FormattedCharSequence> lines, int x, int y) {
		g.setTooltipForNextFrame(new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>(lines), x, y);
	}

	public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
		g.setTooltipForNextFrame(font, lines, x, y);
	}

	public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
		g.setTooltipForNextFrame(font, stack, x, y);
	}

	public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> custom, int x, int y) {
		g.setTooltipForNextFrame(font, lines, custom, x, y);
	}

	public void setTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
		g.setTooltipForNextFrame(font, lines, Optional.empty(), x, y);
	}

	public void setTooltipForNextFrame(Font font, Component tooltip, int x, int y) {
		g.setTooltipForNextFrame(font, tooltip, x, y);
	}

	public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
		g.setComponentTooltipForNextFrame(font, lines, x, y);
	}
}
