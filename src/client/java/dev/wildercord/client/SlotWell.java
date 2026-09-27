package dev.wildercord.client;

import dev.wildercord.Wildercord;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** The Cord slot's well: vanilla's slot bevel with gold rivets, for a slot the screen texture doesn't have. */
public final class SlotWell {
	private SlotWell() {}

	private static final Identifier SPRITE = Wildercord.id("cord/inventory_slot");

	public static void draw(GuiGraphicsExtractor g, int x, int y) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, x, y, 18, 18);
	}
}
