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
		// Low: well under the crosshair (clear of the sword string marks just under it), above the hotbar's words.
		int y = g.guiHeight() / 2 + 34;
		Crossroads.Standard aimed = Crossroads.aimed(player, s);
		AuraApi.Way way = aimed == null ? null : AuraApi.way(aimed.way()).orElse(null);
		if (way == null) {
			long left = Math.max(0, (s.until() - now + 19) / 20);
			Component hint = Component.translatable("hud.wildercord.crossroads.hint", left);
			g.centeredText(font, hint, cx, y, 0x99E8D8B0);
			return;
		}
		labelFrames++;
		lastNamed = way.id();
		boolean leaning = way.id().equals(s.leaningAt(now));
		int color = 0xFF000000 | way.color();
		Component name = Component.translatable(way.nameKey());
		g.centeredText(font, name, cx, y, color);
		Component what = Component.translatable(leaning ? "hud.wildercord.crossroads.walk" : "hud.wildercord.crossroads.lean");
		g.centeredText(font, what, cx, y + 11, leaning ? 0xFFFFE7A0 : 0xCCE8E0F0);
		List<String> nodes = new ArrayList<>();
		for (AuraApi.WayNode node : way.nodes()) {
			nodes.add(Component.translatable(node.nameKey()).getString());
		}
		g.pose().pushMatrix();
		g.pose().translate(cx, y + 22);
		g.pose().scale(0.75F, 0.75F);
		g.centeredText(font, String.join("  ·  ", nodes), 0, 0, 0xAAB8A8D8);
		g.pose().popMatrix();
	}

	/** How many frames a standard's label has been drawn, and the last Way it named (for the game tests). */
	public static int labelFrames() {
		return labelFrames;
	}

	public static String lastNamed() {
		return lastNamed;
	}
}
