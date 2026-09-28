package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.SpellSigil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * A spell's magic circle drawn in a screen, laid out exactly like the one in the world (see
 * {@link SpellSigil}): the frame and its rays, the band of script, the element's pattern, the star
 * with a roundel for every rune, and the seal. It opens over half a second whenever the spell
 * changes, and turns slowly.
 */
public final class GuiSpellCircle {
	private GuiSpellCircle() {}

	private static final Identifier LINE = Wildercord.id("textures/particle/sigil_band.png");
	private static final Identifier GLOW = Wildercord.id("textures/particle/sigil_glow.png");

	private static Identifier mark(RuneDef rune) {
		return Wildercord.id("textures/particle/circle/" + art(rune) + "_mark.png");
	}

	private static Identifier band(RuneDef rune) {
		return Wildercord.id("textures/particle/circle/" + art(rune) + "_band.png");
	}

	/** Whose ring and emblem a rune wears: its own, or for a Knot (a spell tied up, like a link) the links' shared ones. */
	private static String art(RuneDef rune) {
		return rune.family() == RuneFamily.KNOT ? "_link" : rune.path();
	}

	/**
	 * Draws {@code runes}' circle centred on (cx, cy), {@code radius} GUI pixels to its frame.
	 *
	 * @param time   seconds, for the turning
	 * @param open   0 to 1: how far it has opened
	 */
	public static void draw(GuiGraphicsExtractor g, float cx, float cy, float radius, List<RuneDef> runes, float time, float open) {
		if (runes.isEmpty() || open <= 0) {
			return;
		}
		int n = Math.min(runes.size(), SpellSigil.MAX_RUNES);
		RuneDef pattern = runes.getFirst();
		for (RuneDef rune : runes) {
			if (rune.family() == RuneFamily.EFFECT) {
				pattern = rune;
				break;
			}
		}
		int color = RuneColors.of(pattern);
		float r = radius * (0.6F + 0.4F * ease(part(open, 0, 0.3F)));
		float fine = Math.max(1F, radius * SpellSigil.FINE);
		float heavy = Math.max(1.5F, radius * SpellSigil.HEAVY);
		float spin = time * 0.35F;
		int points = SpellSigil.points(n);
		int step = SpellSigil.step(points);

		// A soft glow behind it all.
		quad(g, GLOW, cx, cy, 0, r * 2.6F, r * 2.6F, argb(0.25F * open, color));
		// The frame and its rays.
		float frame = part(open, 0, 0.25F);
		ring(g, cx, cy, r * SpellSigil.FRAME, heavy, argb(frame, color));
		ring(g, cx, cy, r * SpellSigil.FRAME_INNER, fine, argb(frame * 0.8F, color));
		for (int k = 0; k < points; k++) {
			float a = spin * 0.3F - Mth.HALF_PI + Mth.TWO_PI * k / points;
			line(g, cx + Mth.cos(a) * r, cy + Mth.sin(a) * r, cx + Mth.cos(a) * r * (1 + 0.08F * frame), cy + Mth.sin(a) * r * (1 + 0.08F * frame), fine,
				argb(frame, color));
		}
		// The script: the spell's emblems, round and round.
		float writing = part(open, 0.1F, 0.3F);
		float scriptR = r * SpellSigil.SCRIPT;
		float glyph = r * SpellSigil.SCRIPT_HEIGHT;
		int around = Math.max(n, Math.round(Mth.TWO_PI * scriptR / glyph / n) * n);
		for (int i = 0; i < around; i++) {
			float a = spin * 0.5F + Mth.TWO_PI * i / around;
			RuneDef rune = runes.get(i % n);
			quad(g, mark(rune), cx + Mth.cos(a) * scriptR, cy + Mth.sin(a) * scriptR, a + Mth.HALF_PI, glyph, glyph, argb(writing * 0.9F, lighter(color, 0.25F)));
		}
		ring(g, cx, cy, r * SpellSigil.SCRIPT_INNER, fine, argb(writing, color));
		// The pattern band.
		float patternR = r * SpellSigil.PATTERN;
		float tile = r * SpellSigil.PATTERN_HEIGHT;
		int tiles = Math.max(8, Math.round(Mth.TWO_PI * patternR / tile));
		for (int i = 0; i < tiles; i++) {
			float a = -spin * 0.7F + Mth.TWO_PI * i / tiles;
			quad(g, band(pattern), cx + Mth.cos(a) * patternR, cy + Mth.sin(a) * patternR, a + Mth.HALF_PI, Mth.TWO_PI * patternR / tiles, tile,
				argb(writing, RuneColors.of(pattern)));
		}
		// The star, drawing itself.
		float drawn = part(open, 0.2F, 0.35F);
		float starR = r * SpellSigil.STAR;
		ring(g, cx, cy, starR, fine, argb(drawn * 0.7F, color));
		for (int k = 0; k < points; k++) {
			float a0 = spin * 0.2F - Mth.HALF_PI + Mth.TWO_PI * k / points;
			float a1 = spin * 0.2F - Mth.HALF_PI + Mth.TWO_PI * (k + step) / points;
			float x0 = cx + Mth.cos(a0) * starR;
			float y0 = cy + Mth.sin(a0) * starR;
			float x1 = cx + Mth.cos(a1) * starR;
			float y1 = cy + Mth.sin(a1) * starR;
			line(g, x0, y0, x0 + (x1 - x0) * drawn, y0 + (y1 - y0) * drawn, fine, argb(Math.min(1, drawn * 1.5F), lighter(color, 0.35F)));
		}
		// The inner rings and the seal.
		float middle = part(open, 0.3F, 0.3F);
		ring(g, cx, cy, r * SpellSigil.INNER, fine, argb(middle, color));
		ring(g, cx, cy, r * SpellSigil.MEDALLION, fine, argb(middle, lighter(color, 0.3F)));
		float seal = r * SpellSigil.SEAL * (0.6F + 0.4F * middle);
		quad(g, mark(runes.getFirst()), cx, cy, -spin * 0.4F, seal, seal, argb(middle, RuneColors.of(runes.getFirst())));
		// The roundels, one by one.
		float s = r * SpellSigil.roundel(n);
		for (int i = 0; i < n; i++) {
			float shown = part(open, 0.35F + 0.55F * i / n, 0.1F);
			if (shown <= 0) {
				continue;
			}
			RuneDef rune = runes.get(i);
			int rc = RuneColors.of(rune);
			float a = spin * 0.2F - Mth.HALF_PI + Mth.TWO_PI * SpellSigil.pointOf(i, n) / points;
			float u = cx + Mth.cos(a) * starR;
			float v = cy + Mth.sin(a) * starR;
			float rs = s * (0.5F + 0.5F * shown);
			quad(g, GLOW, u, v, 0, rs * 2.4F, rs * 2.4F, argb(0.3F * shown, rc));
			ring(g, u, v, rs, fine, argb(shown, rc));
			quad(g, mark(rune), u, v, a + Mth.HALF_PI, rs, rs, argb(shown, lighter(rc, 0.15F)));
		}
	}

	private static float part(float open, float from, float length) {
		return Mth.clamp((open - from) / length, 0, 1);
	}

	private static float ease(float t) {
		return 1 - (1 - t) * (1 - t);
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int lighter(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}

	/** A ring of line {@code width}, made of short straight pieces. */
	public static void ring(GuiGraphicsExtractor g, float cx, float cy, float rad, float width, int argb) {
		if ((argb >>> 24) < 3 || rad <= 0) {
			return;
		}
		int n = Math.max(16, (int) Math.ceil(Mth.TWO_PI * rad / 3F));
		float length = Mth.TWO_PI * rad / n * 1.1F;
		for (int i = 0; i < n; i++) {
			float a = Mth.TWO_PI * i / n;
			quad(g, LINE, cx + Mth.cos(a) * rad, cy + Mth.sin(a) * rad, a + Mth.HALF_PI, length, width * 3.2F, argb);
		}
	}

	/** A straight line of light from (x0, y0) to (x1, y1). */
	public static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float width, int argb) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float length = Mth.sqrt(dx * dx + dy * dy);
		if ((argb >>> 24) < 3 || length < 0.3F) {
			return;
		}
		quad(g, LINE, (x0 + x1) / 2, (y0 + y1) / 2, (float) Math.atan2(dy, dx), length, width * 3.2F, argb);
	}

	/** A whole 16×16 (or any square) texture stretched to w×h, centred on (x, y), turned {@code rot}. */
	private static void quad(GuiGraphicsExtractor g, Identifier texture, float x, float y, float rot, float w, float h, int argb) {
		if ((argb >>> 24) < 3 || w <= 0 || h <= 0) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate(rot);
		g.pose().scale(w / 16F, h / 16F);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, -8, -8, 0, 0, 16, 16, 16, 16, 16, 16, argb);
		g.pose().popMatrix();
	}
}
