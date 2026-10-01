package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasterySigil;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.SpellSigil;
import dev.wildercord.spell.WovenRunes;
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

	/** A fused rune's second half ({@code band2} or {@code mark2}), shown in {@link RuneColors#second}. */
	private static Identifier second(RuneDef rune, String part) {
		return Wildercord.id("textures/particle/circle/" + rune.path() + "_" + part + ".png");
	}

	/**
	 * Whose ring and emblem a rune wears: its own, or for a Knot (a spell tied up, like a link) the links'
	 * shared ones. An add-on's rune has no art here and wears its family's, as its circle in the world does
	 * (see SpellCircleParticle.runeSprite).
	 */
	private static String art(RuneDef rune) {
		if (rune.family() == RuneFamily.KNOT) {
			return "_link";
		}
		if (WovenRunes.isWoven(rune)) {
			return "_effect";
		}
		return rune.id().startsWith(Wildercord.MOD_ID + ":") ? rune.path() : "_" + rune.family().name().toLowerCase(java.util.Locale.ROOT);
	}

	/**
	 * Draws {@code runes}' circle centred on (cx, cy), {@code radius} GUI pixels to its frame.
	 *
	 * @param time   seconds, for the turning
	 * @param open   0 to 1: how far it has opened
	 */
	public static void draw(GuiGraphicsExtractor g, float cx, float cy, float radius, List<RuneDef> runes, float time, float open) {
		draw(g, cx, cy, radius, runes, time, open, MasteryAttachments.Look.NONE);
	}

	/**
	 * As {@link #draw(GuiGraphicsExtractor, float, float, float, List, float, float)}, for a spell with {@code look}'s
	 * mastery: its owner's sigil in place of the seal, a deeper colour from Adept, and the rings mastery adds outside
	 * the frame (a fine ring from Practised, a second with ticks from Master, a slow shimmer at Mythic).
	 */
	public static void draw(GuiGraphicsExtractor g, float cx, float cy, float radius, List<RuneDef> runes, float time, float open,
			MasteryAttachments.Look look) {
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
		if (look.rank() >= MasteryRules.ADEPT || look.has(MasteryAttachments.Look.HUE)) {
			float depth = (look.rank() >= MasteryRules.MYTHIC ? 0.3F : look.rank() >= MasteryRules.MASTER ? 0.22F : look.rank() >= MasteryRules.ADEPT ? 0.14F : 0F)
				+ (look.has(MasteryAttachments.Look.HUE) ? 0.12F : 0F);
			color = dev.wildercord.client.fx.SpellCircleParticle.deeper(color, depth);
		}
		float r = radius * (0.6F + 0.4F * ease(part(open, 0, 0.3F)));
		float fine = Math.max(1F, radius * SpellSigil.FINE);
		float heavy = Math.max(1.5F, radius * SpellSigil.HEAVY);
		float spin = time * 0.35F;
		int points = SpellSigil.points(n);

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
		masteryRings(g, cx, cy, r, fine, frame, color, look.rank(), time);
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
			if (RuneColors.second(pattern) >= 0) {
				quad(g, second(pattern, "band2"), cx + Mth.cos(a) * patternR, cy + Mth.sin(a) * patternR, a + Mth.HALF_PI, Mth.TWO_PI * patternR / tiles, tile,
					argb(writing, RuneColors.second(pattern)));
			}
		}
		// The star, drawing itself.
		float drawn = part(open, 0.2F, 0.35F);
		float starR = r * SpellSigil.STAR;
		ring(g, cx, cy, starR, fine, argb(drawn * 0.7F, color));
		var defs=dev.wildercord.spell.Knots.flatten(runes);
		var circle=dev.wildercord.spell.CircleDisciplines.design(defs);
		java.util.List<Integer> colors=new java.util.ArrayList<>();
		for(var def:defs)if(def.family()==RuneFamily.EFFECT){
			for(var leaf:WovenRunes.isWoven(def)?WovenRunes.contents(def):List.of(def)){
				int primary=RuneColors.of(leaf),secondary=RuneColors.second(leaf);
				int ink=argb(drawn*.8F,primary);if(!colors.contains(ink)&&colors.size()<10)colors.add(ink);
				if(secondary>=0){ink=argb(drawn*.8F,secondary);if(!colors.contains(ink)&&colors.size()<10)colors.add(ink);}
			}
		}
		dev.wildercord.spell.CircleGeometry.draw(circle,starR,drawn,fine,spin*.2F,time*20,argb(drawn,lighter(color,.35F)),colors,
			(x0,y0,x1,y1,width,ink)->line(g,cx+x0,cy-y0,cx+x1,cy-y1,width,ink));
		// The inner rings and the seal.
		float middle = part(open, 0.3F, 0.3F);
		ring(g, cx, cy, r * SpellSigil.INNER, fine, argb(middle, color));
		ring(g, cx, cy, r * SpellSigil.MEDALLION, fine, argb(middle, lighter(color, 0.3F)));
		if (look.seed() != 0) {
			// The owner's own sigil, where the seal would be.
			boolean bright = look.rank() >= MasteryRules.ADEPT;
			if (bright) {
				quad(g, GLOW, cx, cy, 0, r * 0.7F, r * 0.7F, argb(0.35F * middle, color));
			}
			sigil(g, cx, cy, r * 0.26F * (0.6F + 0.4F * middle), look.seed(), argb(middle, lighter(color, bright ? 0.55F : 0.3F)), fine * (bright ? 1.4F : 1.1F));
		} else {
			float seal = r * SpellSigil.SEAL * (0.6F + 0.4F * middle);
			quad(g, mark(runes.getFirst()), cx, cy, -spin * 0.4F, seal, seal, argb(middle, RuneColors.of(runes.getFirst())));
			if (RuneColors.second(runes.getFirst()) >= 0) {
				quad(g, second(runes.getFirst(), "mark2"), cx, cy, -spin * 0.4F, seal, seal, argb(middle, RuneColors.second(runes.getFirst())));
			}
		}
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
			int second = RuneColors.second(rune);
			if (second >= 0) {
				// A fused rune: its partner element's half, in that element's colour.
				quad(g, second(rune, "mark2"), u, v, a + Mth.HALF_PI, rs, rs, argb(shown, lighter(second, 0.15F)));
			}
		}
	}

	/** The rings mastery adds outside the frame of a circle of radius {@code r}. */
	private static void masteryRings(GuiGraphicsExtractor g, float cx, float cy, float r, float fine, float shown, int color, int rank, float time) {
		if (rank < 2 || shown <= 0) {
			return;
		}
		ring(g, cx, cy, r * 1.12F, fine, argb(shown * 0.7F, color));
		if (rank >= MasteryRules.MASTER) {
			ring(g, cx, cy, r * 1.19F, fine * 0.8F, argb(shown * 0.5F, lighter(color, 0.2F)));
			for (int k = 0; k < 24; k++) {
				float a = Mth.TWO_PI * k / 24;
				float inner = k % 2 == 0 ? 1.12F : 1.15F;
				line(g, cx + Mth.cos(a) * r * inner, cy + Mth.sin(a) * r * inner, cx + Mth.cos(a) * r * 1.19F, cy + Mth.sin(a) * r * 1.19F, fine * 0.8F,
					argb(shown * 0.45F, color));
			}
		}
		if (rank >= MasteryRules.MYTHIC) {
			float head = time * 0.9F;
			int bright = argb(shown * 0.85F, lighter(color, 0.65F));
			for (int i = 0; i < 8; i++) {
				float a0 = head + i * 0.09F;
				float a1 = a0 + 0.09F;
				line(g, cx + Mth.cos(a0) * r * 1.155F, cy + Mth.sin(a0) * r * 1.155F, cx + Mth.cos(a1) * r * 1.155F, cy + Mth.sin(a1) * r * 1.155F, fine * 1.6F, bright);
			}
			quad(g, GLOW, cx + Mth.cos(head + 0.72F) * r * 1.155F, cy + Mth.sin(head + 0.72F) * r * 1.155F, 0, r * 0.16F, r * 0.16F, argb(shown * 0.6F, lighter(color, 0.7F)));
		}
	}

	/** A spell's personal sigil (see {@link MasterySigil}) centred on (cx, cy), {@code rad} from its middle to its edge. */
	public static void sigil(GuiGraphicsExtractor g, float cx, float cy, float rad, long seed, int argb, float width) {
		if (seed == 0 || rad <= 0) {
			return;
		}
		MasterySigil.Glyph glyph = glyph(seed);
		for (MasterySigil.Stroke s : glyph.strokes()) {
			// The screen's y runs down; the sigil's up.
			line(g, cx + s.x0() * rad, cy - s.y0() * rad, cx + s.x1() * rad, cy - s.y1() * rad, width, argb);
		}
		for (MasterySigil.Dot d : glyph.dots()) {
			ring(g, cx + d.x() * rad, cy - d.y() * rad, Math.max(width, d.r() * rad), width, argb);
		}
	}

	private static final java.util.Map<Long, MasterySigil.Glyph> GLYPHS = new java.util.LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<Long, MasterySigil.Glyph> eldest) {
			return size() > 64;
		}
	};

	private static MasterySigil.Glyph glyph(long seed) {
		return GLYPHS.computeIfAbsent(seed, MasterySigil::glyph);
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
