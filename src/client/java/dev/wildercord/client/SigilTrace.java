package dev.wildercord.client;

import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.TraceGlyph;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Sigil tracing, the client's half. While a spell charges, its glyph ({@link TraceGlyph}) is drawn
 * faintly round the crosshair. Holding sneak steadies the hands: the camera holds still and the mouse
 * moves a point of light along the glyph instead (see {@code mixin.MouseHandlerMixin}). As the spell
 * is let go, how well the glyph was followed is scored here and sent with the release
 * ({@link WildercordNetworking.TraceSpell}); the server believes it only as far as the time spent
 * steadying allows.
 *
 * <p>Built to work for everyone: the glyph is a fixed share of the screen at every GUI scale; the
 * mouse is read at the player's own sensitivity but held within a range, so neither the slowest nor
 * the fastest setting makes it unusable; a client option turns tracing off, and one sets how much
 * the line forgives and how strongly it draws the point back onto itself.</p>
 */
public final class SigilTrace {
	private SigilTrace() {}

	/** How many degrees of a turn of the view the glyph's half-width is: the mouse travel tracing takes. */
	private static final double GLYPH_DEGREES = 20.0;
	/** Degrees of turn a count of mouse movement may give, whatever the sensitivity: the slowest and fastest it's allowed to be. */
	private static final double MIN_RATE = 0.04;
	private static final double MAX_RATE = 0.3;
	/** How far past the glyph's ring the point may wander. */
	private static final double REACH = 1.25;
	/** The most points a path keeps. */
	private static final int MAX_PATH = 800;

	/** The charge whose glyph this is (its start time), or none. */
	private static long chargeStart = Long.MIN_VALUE;
	private static List<double[]> glyph = List.of();
	private static final List<double[]> PATH = new ArrayList<>();
	private static double[] cursor = {0, 0};
	/** Whether the hands are steadied this tick (sneak held while charging): the mouse traces instead of turning. */
	private static boolean steadying;
	/** The word on how to trace: shown for the first few charges of a session, until the player first steadies, and never again after. */
	private static final int HINTED_CHARGES = 3;
	private static final int HINT_TICKS = 40;
	private static int hinted;
	private static boolean everSteadied;
	private static boolean hintThisCharge;

	/** Every client tick: follows the local player's charge, and whether they're steadying. */
	public static void tick(Minecraft mc) {
		CastingOptions.ensureLoaded();
		LocalPlayer player = mc.player;
		WildercordAttachments.Charge charge = player == null ? null : player.getAttached(WildercordAttachments.CHARGE);
		if (charge == null || !charge.traceable() || !CastingOptions.tracing) {
			reset();
			return;
		}
		if (charge.start() != chargeStart) {
			reset();
			chargeStart = charge.start();
			glyph = TraceGlyph.of(charge.runes());
			cursor = glyph.isEmpty() ? new double[] {0, 0} : glyph.getFirst().clone();
			hintThisCharge = !everSteadied && hinted < HINTED_CHARGES;
			if (hintThisCharge) {
				hinted++;
			}
		}
		boolean was = steadying;
		steadying = !glyph.isEmpty() && mc.gui.screen() == null && player.isAlive() && mc.options.keyShift.isDown();
		if (steadying) {
			everSteadied = true;
		}
		if (steadying && !was && PATH.isEmpty()) {
			PATH.add(cursor.clone());
		}
	}

	private static void reset() {
		chargeStart = Long.MIN_VALUE;
		glyph = List.of();
		PATH.clear();
		steadying = false;
	}

	/** Whether mouse movement traces the glyph right now (and the camera holds still). */
	public static boolean capturing() {
		return steadying && chargeStart != Long.MIN_VALUE;
	}

	/**
	 * Mouse movement while steadying ({@code dx}, {@code dy} in the window's own counts): moves the point
	 * along at the player's sensitivity (held within reason), drawn back toward the line by the assist.
	 */
	public static void mouse(double dx, double dy) {
		Minecraft mc = Minecraft.getInstance();
		double s = mc.options.sensitivity().get() * 0.6 + 0.2;
		// What vanilla would turn the view by for one count, in degrees, held between the slowest and fastest usable.
		double rate = Mth.clamp(s * s * s * 8.0 * 0.15, MIN_RATE, MAX_RATE);
		double mx = mc.options.invertMouseX().get() ? -dx : dx;
		double my = mc.options.invertMouseY().get() ? -dy : dy;
		double du = mx * rate / GLYPH_DEGREES;
		// The window counts downward; the glyph's y is up.
		double dv = -my * rate / GLYPH_DEGREES;
		double moved = Math.hypot(du, dv);
		if (moved <= 0) {
			return;
		}
		double[] next = {cursor[0] + du, cursor[1] + dv};
		TraceGlyph.Assist assist = CastingOptions.assist;
		// The assist pulls in proportion to the movement, so a resting hand's point stays where it is.
		next = TraceGlyph.assisted(next, glyph, assist.pull * Math.min(1, moved / 0.08));
		double r = Math.hypot(next[0], next[1]);
		if (r > REACH) {
			next[0] *= REACH / r;
			next[1] *= REACH / r;
		}
		cursor = next;
		if (PATH.isEmpty() || TraceGlyph.distance(PATH.getLast(), cursor) >= 0.01) {
			if (PATH.size() < MAX_PATH) {
				PATH.add(cursor.clone());
			}
		}
	}

	/** How well the glyph has been traced so far, 0 to 1 (0 when it hasn't been). */
	public static double accuracy() {
		return glyph.isEmpty() ? 0 : TraceGlyph.score(glyph, PATH, CastingOptions.assist.tolerance);
	}

	/** The glyph being traced now (empty when none), and how many points the path has: for tests. */
	public static List<double[]> glyph() {
		return List.copyOf(glyph);
	}

	public static int pathSize() {
		return PATH.size();
	}

	/** Whether anything worth reporting was traced for the charge in hand. */
	public static boolean traced() {
		return chargeStart != Long.MIN_VALUE && PATH.size() >= 2 && TraceGlyph.length(PATH) >= TraceGlyph.MIN_PATH;
	}

	/** The charge is being let go: reports how well its glyph was traced (if it was at all), just before the release itself. */
	public static void release() {
		if (traced() && ClientPlayNetworking.canSend(WildercordNetworking.TraceSpell.TYPE)) {
			ClientPlayNetworking.send(new WildercordNetworking.TraceSpell(chargeStart, (float) accuracy()));
		}
		reset();
	}

	// ------------------------------------------------------------------ drawing

	/** The glyph round the crosshair while charging: faint until the hands are steadied, then bright, with the path traced so far. */
	public static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (chargeStart == Long.MIN_VALUE || glyph.isEmpty() || mc.player == null) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		// A fixed share of the screen, so it's the same size at every GUI scale.
		float radius = Math.min(w, h) * 0.16F;
		float cx = w / 2F;
		float cy = h / 2F;
		int lineWidth = Math.max(1, Math.round(radius / 70F));
		long time = mc.level == null ? 0 : mc.level.getGameTime();
		float pulse = 0.85F + 0.15F * Mth.sin((time + delta.getGameTimeDeltaPartialTick(false)) * 0.3F);
		float alpha = steadying ? 0.75F * pulse : 0.22F;
		int glow = 0xB8A8FF;
		// The glyph's strokes.
		for (int i = 1; i < glyph.size(); i++) {
			segment(g, cx, cy, radius, glyph.get(i - 1), glyph.get(i), lineWidth, argb(alpha, glow));
		}
		// Where it starts: a small ring of light.
		double[] first = glyph.getFirst();
		dot(g, cx + (float) first[0] * radius, cy - (float) first[1] * radius, lineWidth + 2, argb(Math.min(1, alpha + 0.2F), 0xFFFFFF));
		if (!PATH.isEmpty()) {
			// The path so far, in gold.
			for (int i = 1; i < PATH.size(); i++) {
				segment(g, cx, cy, radius, PATH.get(i - 1), PATH.get(i), lineWidth, argb(steadying ? 0.9F : 0.4F, 0xF5D56A));
			}
		}
		if (steadying) {
			// The point of light, and how well it's going.
			dot(g, cx + (float) cursor[0] * radius, cy - (float) cursor[1] * radius, lineWidth + 1, 0xFFFFFFFF);
			String score = Math.round(accuracy() * 100) + "%";
			g.centeredText(mc.font, score, (int) cx, (int) (cy + radius * 1.12F) + 2, 0xFFF5D56A);
		} else if (PATH.isEmpty() && hintThisCharge && time - chargeStart < HINT_TICKS) {
			// A quiet word on how, for the first moments of the first few charges, until the hands are first steadied.
			Component hint = Component.translatable("hud.wildercord.trace_hint", mc.options.keyShift.getTranslatedKeyMessage());
			g.centeredText(mc.font, hint, (int) cx, (int) (cy + radius * 1.12F) + 2, argb(0.55F, 0xB8A8FF));
		}
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	/** A straight stroke between two glyph points, as one rotated bar. */
	private static void segment(GuiGraphicsExtractor g, float cx, float cy, float radius, double[] a, double[] b, int width, int color) {
		float x0 = cx + (float) a[0] * radius;
		float y0 = cy - (float) a[1] * radius;
		float x1 = cx + (float) b[0] * radius;
		float y1 = cy - (float) b[1] * radius;
		float length = (float) Math.hypot(x1 - x0, y1 - y0);
		if (length < 0.5F) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x0, y0);
		g.pose().rotate((float) Math.atan2(y1 - y0, x1 - x0));
		int half = Math.max(0, width / 2);
		g.fill(0, -half, Math.round(length), -half + width, color);
		g.pose().popMatrix();
	}

	private static void dot(GuiGraphicsExtractor g, float x, float y, int size, int color) {
		int half = size / 2;
		g.fill(Math.round(x) - half, Math.round(y) - half, Math.round(x) - half + size, Math.round(y) - half + size, color);
	}
}
