package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.StringReader;
import dev.wildercord.aura.SwordString;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The sword string indicator: a short row of small marks a little below the crosshair (or, by the player's choice, above the aura
 * bar), one for each swing of the string being played, drawn as what it was:
 * <pre>
 *    ·  ◆  ▼          a swing, a full swing, a low swing   (▲ leaping, ► running, ◎ a counter, » a step cut)
 *   ────────          the time left for the next swing, shrinking
 * </pre>
 * It shows only while a string is being played and for a moment after: marks pop in as swings land, in the aura's colour; a
 * string completed flashes gold and fades; a fumble (a swing too late, or an art that can't go) shakes, dulls and falls away; a
 * string left alone fades quietly. A perfect guard or an Aura Step lights a faint mark of what the next swing will be. Nothing is
 * drawn otherwise, and nothing ever sits on the crosshair itself.
 */
public final class StringHud {
	private StringHud() {}

	/** A mark's size and the step from one to the next (GUI pixels). */
	static final int GLYPH = 7;
	static final int PITCH = 9;
	/** How far below the crosshair's centre the row sits: under vanilla's attack indicator, clear of the crosshair. */
	static final int BELOW_CROSSHAIR = 19;

	private static final int GOLD = AuraGuard.PERFECT_COLOR;
	private static final int DULL_RED = 0xB0504A;
	private static final int GREY = 0x8A84A0;

	/** What the indicator is showing. */
	enum Phase { NONE, LIVE, DONE, FUMBLE, REFUSED, LAPSE }

	/** How long each ending plays (ticks) before the row is gone. */
	private static final int DONE_TICKS = 24;
	private static final int FUMBLE_TICKS = 18;
	private static final int LAPSE_TICKS = 8;

	private static final List<StringReader.Stroke> BEADS = new ArrayList<>();
	private static Phase phase = Phase.NONE;
	private static long phaseAt;
	private static long deadline;
	private static int window;
	/** The colour the row is drawn in (the aura's, taken as it changes). */
	private static int color = 0xFFFFFF;
	/** A cue's mark (a counter or a step cut waiting for its swing), and when it came. */
	private static StringReader.Cue cue;
	private static long cueAt;
	private static long now;

	static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Wildercord.id("sword_strings"), StringHud::extract);
	}

	// ------------------------------------------------------------------ what happened

	static void live(List<StringReader.Stroke> chain, long deadline, int window) {
		now = time();
		BEADS.clear();
		BEADS.addAll(chain);
		phase = Phase.LIVE;
		StringHud.deadline = deadline;
		StringHud.window = Math.max(1, window);
		if (!chain.isEmpty() && cue != null && chain.getLast().at() >= cueAt) {
			cue = null;
		}
	}

	static void completed(List<StringReader.Stroke> strokes) {
		end(Phase.DONE, strokes);
		cue = null;
	}

	static void refused(List<StringReader.Stroke> strokes) {
		end(Phase.REFUSED, strokes);
		cue = null;
	}

	static void fumbled(List<StringReader.Stroke> strokes) {
		end(Phase.FUMBLE, strokes);
		cue = null;
	}

	/** The server refused what the client thought it could do: a completion still showing turns to a refusal. */
	static void refusedByServer() {
		if (phase == Phase.DONE && now - phaseAt <= DONE_TICKS) {
			List<StringReader.Stroke> shown = new ArrayList<>(BEADS);
			end(Phase.REFUSED, shown);
		}
	}

	static void lapsed() {
		if (phase == Phase.LIVE) {
			phase = Phase.LAPSE;
			phaseAt = time();
		}
	}

	static void cued(StringReader.Cue cue, long at) {
		StringHud.cue = cue;
		cueAt = at;
		if (phase != Phase.LIVE) {
			// Only the cue's waiting mark shows until the swing comes.
			BEADS.clear();
			phase = Phase.NONE;
		}
	}

	private static void end(Phase ending, List<StringReader.Stroke> strokes) {
		BEADS.clear();
		BEADS.addAll(strokes);
		phase = ending;
		phaseAt = time();
		now = phaseAt;
	}

	/** The level's game time now (the HUD's clock; between ticks an event's own moment). */
	private static long time() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? now : mc.level.getGameTime();
	}

	static void tick(long time) {
		now = time;
		switch (phase) {
			case DONE -> {
				if (now - phaseAt > DONE_TICKS) {
					clear();
				}
			}
			case FUMBLE, REFUSED -> {
				if (now - phaseAt > FUMBLE_TICKS) {
					clear();
				}
			}
			case LAPSE -> {
				if (now - phaseAt > LAPSE_TICKS) {
					clear();
				}
			}
			default -> {
			}
		}
		if (cue != null && now - cueAt > cue.window()) {
			cue = null;
		}
	}

	private static void clear() {
		BEADS.clear();
		phase = Phase.NONE;
	}

	static void reset() {
		clear();
		cue = null;
	}

	/** What the indicator shows now (the game tests read it). */
	public static String phase() {
		return phase.name().toLowerCase(java.util.Locale.ROOT);
	}

	/** How many marks it shows now. */
	public static int shown() {
		return BEADS.size();
	}

	// ------------------------------------------------------------------ drawing

	/** By the crosshair (the HUD element after vanilla's crosshair). */
	private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (MagicQuality.stringIndicator != MagicQuality.StringIndicator.CROSSHAIR) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.isSpectator()) {
			return;
		}
		draw(g, player, g.guiWidth() / 2, g.guiHeight() / 2 + BELOW_CROSSHAIR, true, delta.getGameTimeDeltaPartialTick(false));
	}

	/**
	 * Above the aura bar ({@link AuraHud} calls this with the top of what it drew), when the player keeps the indicator there.
	 * Returns the new top.
	 */
	static int aboveStrip(GuiGraphicsExtractor g, LocalPlayer player, int left, int top, float partial) {
		if (MagicQuality.stringIndicator != MagicQuality.StringIndicator.HOTBAR || !showing()) {
			return top;
		}
		int width = Math.max(1, BEADS.size() + (cue != null ? 1 : 0)) * PITCH - (PITCH - GLYPH);
		draw(g, player, left + width / 2, top - GLYPH - 4, false, partial);
		return top - GLYPH - 5;
	}

	static boolean showing() {
		return phase != Phase.NONE || cue != null;
	}

	/**
	 * Draws the row centred on {@code cx}, its marks' tops at {@code y}.
	 *
	 * @param timer whether to draw the shrinking line of the time left under it
	 */
	static void draw(GuiGraphicsExtractor g, LocalPlayer player, int cx, int y, boolean timer, float partial) {
		if (!showing()) {
			return;
		}
		int aura = Aura.color(player);
		if (aura != 0) {
			color = aura;
		}
		float t = now + partial;
		int n = BEADS.size();
		boolean waiting = cue != null && phase != Phase.DONE && phase != Phase.FUMBLE && phase != Phase.REFUSED;
		int slots = n + (waiting ? 1 : 0);
		if (slots == 0) {
			return;
		}
		int width = slots * PITCH - (PITCH - GLYPH);
		int x0 = cx - width / 2;
		float age = t - phaseAt;
		// The whole row's fade and shake for its ending.
		float alpha = switch (phase) {
			case DONE -> age < 12 ? 1.0F : Math.max(0, 1 - (age - 12) / (DONE_TICKS - 12));
			case FUMBLE, REFUSED -> Math.max(0, 1 - Math.max(0, age - 6) / (FUMBLE_TICKS - 6));
			case LAPSE -> Math.max(0, 1 - age / LAPSE_TICKS);
			default -> 1.0F;
		};
		int shake = 0;
		int drop = 0;
		if (phase == Phase.FUMBLE || phase == Phase.REFUSED) {
			shake = age < 8 ? (int) Math.round(Math.sin(age * 2.4) * (phase == Phase.FUMBLE ? 2 : 1)) : 0;
			drop = (int) Math.min(3, Math.max(0, age - 6) / 3);
		}
		boolean flash = !MagicQuality.reducedFlash;
		for (int i = 0; i < n; i++) {
			StringReader.Stroke s = BEADS.get(i);
			SwordString.Token token = SwordString.Token.shown(s.marks());
			boolean full = SwordString.Token.FULL.fits(s.marks());
			float born = t - s.at();
			int fill;
			float a = alpha;
			int rise = 0;
			switch (phase) {
				case DONE -> {
					// Gold, white-hot for its first moments.
					double hot = flash ? Math.max(0, 1 - age / 5.0) : 0;
					fill = mix(GOLD, 0xFFFFFF, 0.25 + 0.6 * hot);
					rise = age < 3 ? -1 : 0;
				}
				case FUMBLE -> fill = mix(lit(token, full), DULL_RED, Math.min(1, age / 3.0));
				case REFUSED -> fill = mix(lit(token, full), GREY, Math.min(1, age / 3.0));
				default -> {
					fill = lit(token, full);
					if (flash && born < 3 && i == n - 1) {
						// The newest mark lands white and settles into the aura's colour.
						fill = mix(fill, 0xFFFFFF, 0.7 * (1 - born / 3.0));
					}
					// Popping in: it rises a pixel and brightens over its first ticks.
					a *= Math.min(1, 0.35F + born / 3.0F);
					rise = born < 2 ? 1 : 0;
				}
			}
			int x = x0 + i * PITCH + shake;
			glyph(g, token, x, y + rise + drop, fill, a);
		}
		if (waiting) {
			// The swing a cue waits for: its mark, faint and breathing, where the next swing will land.
			float since = t - cueAt;
			float left = Math.max(0, 1 - since / cue.window());
			float pulse = 0.55F + 0.25F * (float) Math.sin(t * 0.8);
			int fill = cue == StringReader.Cue.GUARD ? GOLD : lift(color);
			glyph(g, cue.token(), x0 + n * PITCH, y, fill, pulse * (0.4F + 0.6F * left));
		}
		if (timer && phase == Phase.LIVE && n > 0) {
			// The time left for the next swing: a thin line under the row, shrinking toward its middle.
			double left = Math.max(0, Math.min(1, (deadline - t) / window));
			int half = (int) Math.round((width / 2.0 + 2) * left);
			if (half > 0) {
				int ly = y + GLYPH + 2;
				int c = left < 0.25 ? mix(lift(color), 0xFFFFFF, 0.4) : lift(color);
				g.fill(cx - half, ly, cx + half, ly + 1, argb(0x000000, 0.5F));
				g.fill(cx - half, ly - 1, cx + half, ly, argb(c, 0.75F));
			}
		}
	}

	/** A mark's colour while a string is being played: the aura's, brighter for a full swing, and the counter's in the parry's gold. */
	private static int lit(SwordString.Token token, boolean full) {
		if (token == SwordString.Token.COUNTER) {
			return GOLD;
		}
		int c = lift(color);
		return full || token != SwordString.Token.SWING ? mix(c, 0xFFFFFF, 0.3) : mix(c, 0x000000, 0.1);
	}

	/** The aura's colour lifted toward white, so a dark method's marks still read. */
	private static int lift(int c) {
		return AuraRules.color(c, 0xFFFFFF, 3);
	}

	// ------------------------------------------------------------------ the marks

	/** Each mark, five pixels square, drawn inside a pixel of dark outline (seven across in all). */
	private static final String[][] SHAPES = {
		// SWING: a small diamond.
		{".....", "..#..", ".###.", "..#..", "....."},
		// FULL: a whole diamond.
		{"..#..", ".###.", "#####", ".###.", "..#.."},
		// LOW: pointing down.
		{".....", "#####", ".###.", "..#..", "....."},
		// LEAP: pointing up.
		{".....", "..#..", ".###.", "#####", "....."},
		// RUN: pointing ahead.
		{".#...", ".##..", ".###.", ".##..", ".#..."},
		// COUNTER: a ring round a point.
		{".###.", "#...#", "#.#.#", "#...#", ".###."},
		// STEP: two chevrons, a step through.
		{"#.#..", ".#.#.", "..#.#", ".#.#.", "#.#.."},
	};

	/** Filled cells and their outline, per token, worked out once. */
	private static final boolean[][][] FILL = new boolean[SHAPES.length][][];
	private static final boolean[][][] EDGE = new boolean[SHAPES.length][][];

	static {
		for (int k = 0; k < SHAPES.length; k++) {
			boolean[][] fill = new boolean[GLYPH][GLYPH];
			for (int r = 0; r < 5; r++) {
				for (int c = 0; c < 5; c++) {
					fill[r + 1][c + 1] = SHAPES[k][r].charAt(c) == '#';
				}
			}
			boolean[][] edge = new boolean[GLYPH][GLYPH];
			for (int r = 0; r < GLYPH; r++) {
				for (int c = 0; c < GLYPH; c++) {
					if (fill[r][c]) {
						continue;
					}
					for (int dr = -1; dr <= 1 && !edge[r][c]; dr++) {
						for (int dc = -1; dc <= 1; dc++) {
							int rr = r + dr;
							int cc = c + dc;
							if (rr >= 0 && rr < GLYPH && cc >= 0 && cc < GLYPH && fill[rr][cc]) {
								edge[r][c] = true;
								break;
							}
						}
					}
				}
			}
			FILL[k] = fill;
			EDGE[k] = edge;
		}
	}

	/** Draws {@code token}'s mark with its top left at ({@code x}, {@code y}), in {@code rgb} at {@code alpha}, outlined in dark. */
	public static void glyph(GuiGraphicsExtractor g, SwordString.Token token, int x, int y, int rgb, float alpha) {
		if (alpha <= 0.01F) {
			return;
		}
		int k = token.ordinal();
		runs(g, EDGE[k], x, y, argb(0x14101C, 0.7F * alpha));
		runs(g, FILL[k], x, y, argb(rgb, alpha));
	}

	/** Fills each row's runs of set cells as one rectangle apiece. */
	private static void runs(GuiGraphicsExtractor g, boolean[][] cells, int x, int y, int color) {
		for (int r = 0; r < GLYPH; r++) {
			int c = 0;
			while (c < GLYPH) {
				if (!cells[r][c]) {
					c++;
					continue;
				}
				int from = c;
				while (c < GLYPH && cells[r][c]) {
					c++;
				}
				g.fill(x + from, y + r, x + c, y + r + 1, color);
			}
		}
	}

	/**
	 * Draws a whole string's marks in a row from ({@code x}, {@code y}), as the Aura page writes an art's string. Returns the
	 * width drawn.
	 */
	public static int string(GuiGraphicsExtractor g, SwordString string, int x, int y, int rgb, float alpha) {
		for (int i = 0; i < string.length(); i++) {
			SwordString.Token token = string.token(i);
			glyph(g, token, x + i * PITCH, y, token == SwordString.Token.COUNTER ? GOLD : rgb, alpha);
		}
		return string.length() * PITCH - (PITCH - GLYPH);
	}

	private static int argb(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private static int mix(int a, int b, double t) {
		return AuraHud.mix(a, b, Math.max(0, Math.min(1, t)));
	}
}
