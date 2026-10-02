package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Awakening on the swordsman's own screen, all of it small and at the edges:
 * <ul>
 * <li>while the Aura key's tap-and-hold charges, a thin ring of the aura's colour filling round the crosshair (and a ring snapping
 *     out as it completes), so a held press can be let go in time;</li>
 * <li>as they awaken, in first person, the edges of the view glowing once in the aura's colour (at most a sixth opaque at the very
 *     edge, nothing in the middle), gone within two seconds; while it lasts only the faint glow at the bottom edge the body's aura
 *     always leaves ({@code AuraFxClient.whisper}) and the strip's own marks;</li>
 * <li>{@link #mark}: the awakening's mark on the aura strip, after the stage diamonds (ready, burning, spent or resting).</li>
 * </ul>
 */
public final class AwakeningHud {
	private AwakeningHud() {}

	/** What the game tests read: the charge ring's frames drawn, and the edge glow's. */
	private static int chargeFrames;
	private static int glowFrames;

	public static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Wildercord.id("aura_awakening_charge"), AwakeningHud::charge);
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, Wildercord.id("aura_awakening_glow"), AwakeningHud::glow);
	}

	// ------------------------------------------------------------------ the charge by the crosshair

	/** The tap-and-hold's charge: segments of a ring round the crosshair lighting one by one, a ring snapping out as it completes. */
	static void charge(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gui.screen() != null) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		float charge = WildercordKeys.awakeningCharge(partial);
		long now = mc.level.getGameTime();
		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2;
		int color = Aura.color(player);
		if (charge > 0) {
			chargeFrames++;
			int segments = 24;
			float radius = 10.5F;
			int lit = Math.round(charge * segments);
			for (int i = 0; i < segments; i++) {
				double a = -Math.PI / 2 + Math.PI * 2 * i / segments;
				int x = cx + Math.round((float) Math.cos(a) * radius);
				int y = cy + Math.round((float) Math.sin(a) * radius);
				// Each segment on a dark backing, so it reads against fire and a bright sky alike.
				g.fill(x - 2, y - 2, x + 2, y + 2, i < lit ? 0x70100C18 : 0x38100C18);
				int c = i < lit ? 0xF0000000 | AuraHud.mix(color, 0xFFFFFF, 0.35 + 0.55 * charge) : 0x60000000 | AuraHud.mix(color, 0x1A1420, 0.45);
				g.fill(x - 1, y - 1, x + 1, y + 1, c);
			}
			return;
		}
		float since = now - WildercordKeys.tapHoldSentAt() + partial;
		if (since >= 0 && since < 8) {
			// Complete: the ring snapping out and fading.
			float t = since / 8F;
			int segments = 32;
			float radius = 10.5F + 9F * t;
			int alpha = Math.round(200 * (1 - t));
			for (int i = 0; i < segments; i++) {
				double a = Math.PI * 2 * i / segments;
				int x = cx + Math.round((float) Math.cos(a) * radius);
				int y = cy + Math.round((float) Math.sin(a) * radius);
				g.fill(x, y, x + 1, y + 1, (alpha << 24) | AuraHud.mix(color, 0xFFFFFF, 0.6));
			}
		}
	}

	// ------------------------------------------------------------------ the moment, through your own eyes

	/**
	 * Your own awakening in first person: the edges of the view glow once in the aura's colour as it bursts (a band down each side and
	 * along the top and bottom, at most a sixth opaque at the edge and fading to nothing inward), gone in under two seconds.
	 */
	static void glow(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.getCameraEntity() != player || !mc.options.getCameraType().isFirstPerson()
				|| MagicQuality.bodyAura == MagicQuality.BodyAura.OFF || player.isSpectator()) {
			return;
		}
		Awakening.State s = Awakening.state(player);
		float time = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
		float age = time - s.since();
		float strength = edge(age);
		if (s.since() < 0 || strength <= 0) {
			return;
		}
		glowFrames++;
		float alpha = strength * (MagicQuality.reducedFlash ? 0.5F : 1.0F);
		// Light, not a frame: the colour taken well toward white, so over a dark scene it reads as a glow and not as a tint.
		int color = AuraHud.mix(Aura.color(player), 0xFFFFFF, 0.45);
		int w = g.guiWidth();
		int h = g.guiHeight();
		int side = Math.max(8, w / 16);
		int band = Math.max(6, h / 18);
		int foot = Math.round(alpha * 255);
		// Top (fainter) and bottom: one gradient each, from the edge inward.
		g.fillGradient(0, 0, w, band, ((foot / 2) << 24) | color, color);
		g.fillGradient(0, h - band, w, h, color, (foot << 24) | color);
		// The sides, in thin steps fading inward (the GUI draws only vertical gradients).
		int steps = 10;
		for (int i = 0; i < steps; i++) {
			float k = 1 - i / (float) steps;
			int a = Math.round(foot * k * k);
			if (a <= 0) {
				continue;
			}
			int x0 = side * i / steps;
			int x1 = side * (i + 1) / steps;
			g.fill(x0, band / 2, x1, h - band / 2, (a << 24) | color);
			g.fill(w - x1, band / 2, w - x0, h - band / 2, (a << 24) | color);
		}
	}

	/** The most the edges glow (at the very edge, as the burst lands). */
	static final float EDGE_PEAK = 0.17F;

	/** How strongly the edges glow {@code age} ticks into an awakening: nothing while it gathers, {@link #EDGE_PEAK} at the burst, gone by 36. */
	static float edge(float age) {
		float burst = AwakeningRules.BURST_AT;
		if (age < burst - 1 || age > burst + 30) {
			return 0;
		}
		if (age < burst + 2) {
			return EDGE_PEAK * Mth.clamp((age - burst + 1) / 3F, 0, 1);
		}
		float t = (age - burst - 2) / 28F;
		return EDGE_PEAK * (1 - t) * (1 - t);
	}

	// ------------------------------------------------------------------ the mark on the aura strip

	/** The mark's width on the strip (it sits after the stage diamonds). */
	static final int MARK = 5;

	/**
	 * The awakening's mark at ({@code x}, {@code y}), a small flame after the stage diamonds: lit and breathing gold-white while an
	 * awakening is ready, blazing while it burns, ash-grey while spent, and dark with a thread under it filling back while it rests;
	 * a held tap-and-hold fills it up. Returns its width with a gap, or 0 where there's none (below Edge, or off).
	 */
	static int mark(GuiGraphicsExtractor g, LocalPlayer player, int x, int y, int color, long now, float partial) {
		if (Aura.stage(player) < AwakeningRules.FROM || !dev.wildercord.config.Config.awakening(player)) {
			return 0;
		}
		Awakening.State s = Awakening.state(player);
		float time = now + partial;
		int fill;
		int edge;
		if (s.awakened(now)) {
			double flicker = 0.5 + 0.5 * Math.sin(time * 0.9) * Math.sin(time * 0.37);
			fill = 0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.45 + 0.4 * flicker);
			edge = 0xFF000000 | AuraHud.mix(color, 0xFFF0C0, 0.5);
		} else if (s.spent(now)) {
			fill = 0xFF5A5060;
			edge = 0xFF3A3448;
		} else if (s.resting(now)) {
			fill = 0xFF000000 | AuraHud.mix(color, 0x2A2438, 0.7);
			edge = 0xFF4A4058;
		} else if (Awakening.ready(player)) {
			double pulse = 0.5 + 0.5 * Math.sin(time * 0.3);
			fill = 0xFF000000 | AuraHud.mix(color, 0xFFF0B0, 0.35 + 0.45 * pulse);
			edge = 0xFF000000 | AuraHud.mix(0xC8A050, 0xFFF0B0, pulse);
		} else {
			fill = 0xFF3A3450;
			edge = 0xFF6A6080;
		}
		float charge = WildercordKeys.awakeningCharge(partial);
		if (charge > 0) {
			fill = 0xFF000000 | AuraHud.mix(fill & 0xFFFFFF, 0xFFFFFF, charge);
		}
		flame(g, x, y, fill, edge);
		if (s.resting(now) && !s.awakened(now)) {
			// Resting: a thread under it filling back as the rest runs out (the spent time inside it in ash).
			long total = Math.max(1, s.readyAt() - s.until());
			double share = 1 - (s.readyAt() - now) / (double) total;
			g.fill(x, y + 6, x + MARK, y + 7, 0xFF2A2438);
			g.fill(x, y + 6, x + (int) Math.round(MARK * Math.max(0, Math.min(1, share))), y + 7, s.spent(now) ? 0xFF7A7080 : 0xFF000000 | color);
		}
		return MARK + 2;
	}

	/** A small flame, five pixels across: a point, a body, a broad foot. */
	private static void flame(GuiGraphicsExtractor g, int x, int y, int fill, int edge) {
		g.fill(x + 2, y, x + 3, y + 1, edge);
		g.fill(x + 1, y + 1, x + 4, y + 2, edge);
		g.fill(x, y + 2, x + 5, y + 5, edge);
		g.fill(x + 2, y + 1, x + 3, y + 2, fill);
		g.fill(x + 1, y + 2, x + 4, y + 4, fill);
		g.fill(x + 2, y + 4, x + 3, y + 5, fill);
	}

	// ------------------------------------------------------------------ for the game tests

	/** The charge ring's frames and the first-person edge glow's frames drawn since the game started. */
	public static int[] counts() {
		return new int[] {chargeFrames, glowFrames};
	}
}
