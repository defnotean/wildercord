package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Crossroads;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The crossroads on the swordsman's own screen: only words, small and low, never over the middle of the view. Looking at a standard,
 * its Way's name in its colour, what a strike does now (lean toward it, or walk it), and its three nodes by name; looking at none, a
 * faint line saying the crossroads stands and how long it waits. The standards themselves are light in the world (see
 * {@code aura.Crossroads}), seen by everyone near.
 */
public final class WayHud {
	private WayHud() {}

	private static final Identifier ID = Wildercord.id("aura_crossroads");
	/** The frames a standard's label was drawn, and the last Way named (the game tests read them). */
	private static int labelFrames;
	private static String lastNamed = "";

	public static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ID, WayHud::draw);
	}

	static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
			return;
		}
		Crossroads.State s = Crossroads.state(player);
		if (s == null) {
			return;
		}
		long now = mc.level.getGameTime();
		Font font = mc.font;
		int cx = g.guiWidth() / 2;
		// Low: above the hotbar, the held item's name and the action bar (a lean's own line lands there), well clear of the middle of
		// the view and the standards in it.
		int y = g.guiHeight() - 110;
		Crossroads.Standard aimed = Crossroads.aimed(player, s);
		AuraApi.Way way = aimed == null ? null : AuraApi.way(aimed.way()).orElse(null);
		if (way == null) {
			long left = Math.max(0, (s.until() - now + 19) / 20);
			Component hint = Component.translatable("hud.wildercord.crossroads.hint", left);
			back(g, cx, y + 20, Math.min(font.width(hint), g.guiWidth() - 8), 9);
			centred(g, font, hint, cx, y + 20, 1, 0xCCE8D8B0);
			return;
		}
		labelFrames++;
		lastNamed = way.id();
		boolean leaning = way.id().equals(s.leaningAt(now));
		int color = 0xFF000000 | way.color();
		Component name = Component.translatable(way.nameKey());
		Component what = Component.translatable(leaning ? "hud.wildercord.crossroads.walk" : "hud.wildercord.crossroads.lean");
		List<String> nodes = new ArrayList<>();
		for (AuraApi.WayNode node : way.nodes()) {
			nodes.add(Component.translatable(node.nameKey()).getString());
		}
		String line = String.join("  ·  ", nodes);
		int wide = Math.max(Math.max(font.width(name), font.width(what)), Math.round(font.width(line) * 0.75F));
		back(g, cx, y, Math.min(wide, g.guiWidth() - 8), 29);
		centred(g, font, name, cx, y, 1, color);
		centred(g, font, what, cx, y + 11, 1, leaning ? 0xFFFFE7A0 : 0xDDE8E0F0);
		centred(g, font, Component.literal(line), cx, y + 22, 0.75F, 0xCCB8A8D8);
	}

	/** {@code text} centred on {@code cx} at {@code scale}, shrunk further if it would run past the window's edges (a narrow window). */
	private static void centred(GuiGraphicsExtractor g, Font font, Component text, int cx, int y, float scale, int color) {
		// Prepared-text bounds run a few pixels past the advance width (shadow, italic lean), so leave 8 a side.
		float s = Math.min(scale, (g.guiWidth() - 16) / (float) Math.max(1, font.width(text)));
		if (s >= 1) {
			g.centeredText(font, text, cx, y, color);
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(cx, y);
		g.pose().scale(s, s);
		g.centeredText(font, text, 0, 0, color);
		g.pose().popMatrix();
	}

	/** A soft dark backing under {@code height} pixels of words {@code width} across, centred on {@code cx}, so they read over bright light. */
	private static void back(GuiGraphicsExtractor g, int cx, int y, int width, int height) {
		int half = Math.min(width / 2 + 6, Math.min(cx, g.guiWidth() - cx));
		g.fill(cx - half, y - 3, cx + half, y + height + 1, 0x55000000);
		g.fill(cx - half + 2, y - 4, cx + half - 2, y - 3, 0x33000000);
		g.fill(cx - half + 2, y + height + 1, cx + half - 2, y + height + 2, 0x33000000);
	}

	/** How many frames a standard's label has been drawn, and the last Way it named (for the game tests). */
	public static int labelFrames() {
		return labelFrames;
	}

	public static String lastNamed() {
		return lastNamed;
	}
}
