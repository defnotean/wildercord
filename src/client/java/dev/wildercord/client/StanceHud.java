package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stance, as a swordsman reads it (the rules are {@code aura.StanceRules}, what's synced {@code aura.Stance.STANCE}):
 * <ul>
 * <li><b>A foe's stance</b>: a thin bar over its head while it's worn, filling from the middle out to both ends as it gives (pale
 * gold, warming to amber, running hot as it nears breaking, and beating when it's close), small diamonds at its ends marking where
 * it breaks; a boss's wider, with its quarters marked. Only the nearest few show, fading with distance; in your own first-person
 * view thinner and a little fainter, so it sits over the foe and never across the middle.</li>
 * <li><b>Opened</b>: the bar breaks apart (its halves flying off), and a cracked gold seal beats over the foe's head with a thread
 * under it running out with the opening; when the foe under your crosshair is opened, two small gold brackets close in either side
 * of the crosshair, dim until your swing is full again (a finisher needs a full swing), bright when it is.</li>
 * <li><b>Your own stance</b> (in a duel): along the top edge of the aura strip, from its middle out, only while it's worn; without the
 * strip, a slim bar of its own right of the hotbar.</li>
 * </ul>
 * Drawn under the crosshair, after the world; hidden with the rest of the HUD.
 */
public final class StanceHud {
	private StanceHud() {}

	/** The most foes' bars shown at once (the nearest), besides the one under the crosshair. */
	private static final int MOST = 8;
	/** Bars are whole within this far, and gone at the second. */
	private static final double CLEAR = 10;
	private static final double GONE = 24;

	private static final int FRAME = 0x14101C;
	private static final int TRACK = 0x2A2236;
	private static final int PALE = 0xF0E2B0;
	private static final int AMBER = 0xF4B04A;
	private static final int HOT = 0xFF6A3A;
	private static final int GOLD = Stance.OPENED_COLOR;

	/** What the game tests read: bars and opened seals drawn last frame, and crosshair cues. */
	private static int bars;
	private static int seals;
	private static boolean cue;
	private static float ownShown;

	static void init() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Wildercord.id("stance"), StanceHud::extract);
	}

	/** One foe to draw: it, its stance and how far it is. */
	private record Foe(LivingEntity entity, Stance.State state, double distance) {}

	static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		bars = 0;
		seals = 0;
		cue = false;
		if (level == null || player == null) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		long now = level.getGameTime();
		float time = now + partial;
		Camera camera = mc.gameRenderer.mainCamera();
		Vec3 cam = camera.position();
		boolean firstPerson = mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson();
		Entity target = mc.crosshairPickEntity;
		List<Foe> foes = new ArrayList<>();
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity living) || living == player || living.isInvisible() || !living.isAlive()) {
				continue;
			}
			Stance.State s = living.getAttached(Stance.STANCE);
			if (s == null) {
				continue;
			}
			double d = living.position().distanceTo(cam);
			if (d > GONE) {
				continue;
			}
			if (!s.opened(now) && s.left(now) >= 0.995) {
				continue;
			}
			foes.add(new Foe(living, s, d));
		}
		foes.sort(Comparator.comparingDouble(Foe::distance));
		int shown = 0;
		for (Foe foe : foes) {
			if (shown >= MOST && foe.entity() != target) {
				continue;
			}
			if (draw(g, mc, camera, foe, now, time, partial, firstPerson)) {
				shown++;
			}
		}
		crosshair(g, mc, player, target, time);
		own(g, mc, player, now, time);
	}

	// ------------------------------------------------------------------ over a foe

	/** One foe's bar or seal over its head. Returns whether it drew. */
	private static boolean draw(GuiGraphicsExtractor g, Minecraft mc, Camera camera, Foe foe, long now, float time, float partial, boolean firstPerson) {
		LivingEntity e = foe.entity();
		Vec3 over = new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()) + e.getBbHeight() + 0.42, Mth.lerp(partial, e.zo, e.getZ()));
		float[] at = screen(mc, camera, over, g.guiWidth(), g.guiHeight());
		if (at == null) {
			return false;
		}
		float far = (float) Mth.clamp(1 - (foe.distance() - CLEAR) / (GONE - CLEAR), 0, 1);
		float alpha = far * (firstPerson ? 0.85F : 1.0F);
		if (alpha < 0.03F) {
			return false;
		}
		Stance.State s = foe.state();
		boolean boss = s.kindOf() == StanceRules.Kind.BOSS;
		int width = (int) Math.round(Mth.clamp(150 / Math.max(1.5, foe.distance()), 22, 40) * (boss ? 1.4 : 1.0));
		int cx = Math.round(at[0]);
		int cy = Math.round(at[1]);
		if (s.opened(now)) {
			seal(g, cx, cy, width, s, now, time, alpha);
			seals++;
		} else {
			bar(g, cx, cy, width, firstPerson ? 2 : 3, 1 - s.left(now), boss, time, alpha);
			bars++;
		}
		return true;
	}

	/**
	 * A stance bar centred at ({@code cx}, {@code cy}), {@code width} across and {@code height} tall: worn {@code share} of it,
	 * filling from the middle out.
	 */
	static void bar(GuiGraphicsExtractor g, int cx, int cy, int width, int height, double share, boolean boss, float time, float alpha) {
		int half = width / 2;
		int x0 = cx - half;
		int x1 = cx + half;
		int y0 = cy;
		int y1 = cy + height;
		// Fades in as it begins to wear, so a bar never pops.
		float a = alpha * (float) Mth.clamp(share * 10, 0.35, 1.0);
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, argb(FRAME, 0.85F * a));
		g.fill(x0, y0, x1, y1, argb(TRACK, 0.75F * a));
		if (boss) {
			for (int q = 1; q <= 3; q++) {
				int dx = Math.round(half * q / 4F);
				g.fill(cx - dx, y0, cx - dx + 1, y1, argb(0x4A4058, 0.9F * a));
				g.fill(cx + dx - 1, y0, cx + dx, y1, argb(0x4A4058, 0.9F * a));
			}
		}
		double s = Mth.clamp(share, 0, 1);
		int fill = (int) Math.round(half * s);
		int color = ramp(s);
		if (s > 0.75) {
			// Close to breaking: it beats.
			float beat = 0.5F + 0.5F * Mth.sin(time * 0.9F);
			color = mix(color, 0xFFFFFF, 0.25F * beat * (float) ((s - 0.75) / 0.25));
		}
		if (fill > 0) {
			g.fill(cx - fill, y0, cx + fill, y1, argb(color, a));
			g.fill(cx - fill, y0, cx + fill, y0 + 1, argb(mix(color, 0xFFFFFF, 0.45F), a));
			// A bright edge where it's giving.
			g.fill(cx - fill, y0, cx - fill + 1, y1, argb(0xFFFFFF, 0.6F * a));
			g.fill(cx + fill - 1, y0, cx + fill, y1, argb(0xFFFFFF, 0.6F * a));
		}
		// Where it breaks: a small diamond at each end, hot when it's near.
		int cap = s > 0.85 ? mix(HOT, 0xFFFFFF, 0.3F) : 0xC8B88A;
		int mid = y0 + height / 2;
		diamond(g, x0 - 2, mid, argb(cap, a));
		diamond(g, x1 + 1, mid, argb(cap, a));
	}

	/** The colour of a bar worn {@code share}: pale gold, warming to amber, hot as it nears breaking. */
	static int ramp(double share) {
		if (share < 0.5) {
			return mix(PALE, AMBER, (float) (share / 0.5));
		}
		return mix(AMBER, HOT, (float) Math.min(1, (share - 0.5) / 0.4));
	}

	/**
	 * An opened foe's seal: the bar's halves flying apart, a cracked gold diamond beating over its head, glowing, and a thread under it
	 * running out with the opening.
	 */
	private static void seal(GuiGraphicsExtractor g, int cx, int cy, int width, Stance.State s, long now, float time, float alpha) {
		int ticks = StanceRules.openTicks(s.kindOf());
		float age = time - (s.openUntil() - ticks);
		float left = Mth.clamp((s.openUntil() + 1 - time) / ticks, 0, 1);
		// The broken bar, flying apart and fading in its first moments.
		float burst = Mth.clamp(age / 8F, 0, 1);
		if (burst < 1) {
			float ease = 1 - (1 - burst) * (1 - burst);
			int half = width / 2;
			int off = Math.round(ease * (half * 0.6F + 6));
			float fade = alpha * (1 - burst);
			g.fill(cx - half - off, cy, cx - off - 2, cy + 3, argb(HOT, fade));
			g.fill(cx + off + 2, cy, cx + half + off, cy + 3, argb(HOT, fade));
		}
		float beat = 0.5F + 0.5F * Mth.sin(time * 0.75F);
		int r = 5 + (beat > 0.6F ? 1 : 0);
		int y = cy - 3;
		// The glow under it, then the seal: a gold diamond with a crack down it.
		diamondFill(g, cx, y, r + 3, argb(GOLD, (0.12F + 0.1F * beat) * alpha));
		diamondFill(g, cx, y, r + 1, argb(GOLD, (0.18F + 0.12F * beat) * alpha));
		diamondFill(g, cx, y, r - 1, argb(0x2A1A0C, 0.7F * alpha));
		diamondOutline(g, cx, y, r, argb(mix(GOLD, 0xFFFFFF, 0.3F * beat), alpha));
		int crack = argb(0xFFFFFF, 0.95F * alpha);
		g.fill(cx, y - r + 1, cx + 1, y - 1, crack);
		g.fill(cx - 1, y - 1, cx, y + 1, crack);
		g.fill(cx, y + 1, cx + 1, y + r - 1, crack);
		// The opening's time, running out under it.
		int line = Math.round((width / 2F) * left);
		if (line > 0) {
			g.fill(cx - line, y + r + 3, cx + line, y + r + 4, argb(GOLD, 0.85F * alpha));
		}
	}

	// ------------------------------------------------------------------ the crosshair

	/** The foe under the crosshair is opened: brackets closing in either side of it, bright once a full swing is ready. */
	private static void crosshair(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, Entity target, float time) {
		if (!(target instanceof LivingEntity living) || !Stance.opened(living) || !mc.options.getCameraType().isFirstPerson()
				|| player.isSpectator()) {
			return;
		}
		cue = true;
		float strength = player.getAttackStrengthScale(0.5F);
		boolean ready = strength >= AuraRules.FULL_SWING - 1.0E-4;
		float beat = 0.5F + 0.5F * Mth.sin(time * 0.9F);
		float a = ready ? 0.75F + 0.25F * beat : 0.35F;
		int color = ready ? GOLD : 0xB8A890;
		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2;
		// Closing in as the swing fills.
		int gap = 7 + Math.round(4 * (1 - Math.min(1, strength / (float) AuraRules.FULL_SWING)));
		int argb = argb(color, a);
		// Left: a small chevron pointing in.
		g.fill(cx - gap - 2, cy - 2, cx - gap - 1, cy - 1, argb);
		g.fill(cx - gap - 1, cy - 1, cx - gap, cy, argb);
		g.fill(cx - gap - 2, cy, cx - gap - 1, cy + 1, argb);
		// Right.
		g.fill(cx + gap + 1, cy - 2, cx + gap + 2, cy - 1, argb);
		g.fill(cx + gap, cy - 1, cx + gap + 1, cy, argb);
		g.fill(cx + gap + 1, cy, cx + gap + 2, cy + 1, argb);
	}

	// ------------------------------------------------------------------ your own

	/** Your own stance, where the aura strip isn't drawn to carry it: a slim bar right of the hotbar, only while it's worn. */
	private static void own(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, long now, float time) {
		Stance.State s = player.getAttached(Stance.STANCE);
		ownShown = s == null ? 0 : (float) (1 - s.left(now));
		if (s == null || AuraHud.showing(player)) {
			return;
		}
		if (!s.opened(now) && s.left(now) >= 0.995) {
			return;
		}
		int x = g.guiWidth() / 2 + 91 + 8;
		int width = Math.min(70, g.guiWidth() - x - 6);
		if (width < 24) {
			return;
		}
		int y = g.guiHeight() - 8;
		if (s.opened(now)) {
			float beat = 0.5F + 0.5F * Mth.sin(time * 0.9F);
			g.fill(x - 1, y - 1, x + width + 1, y + 3, argb(FRAME, 0.85F));
			g.fill(x, y, x + width, y + 2, argb(mix(HOT, GOLD, beat), 0.95F));
			return;
		}
		bar(g, x + width / 2, y, width, 2, 1 - s.left(now), false, time, 1.0F);
	}

	/**
	 * Your own stance on the aura strip's top edge ({@code AuraHud}): from its middle out across {@code width} at {@code y}, only
	 * while it's worn; the whole edge beating gold and hot while you're opened.
	 */
	static void ownOnStrip(GuiGraphicsExtractor g, LocalPlayer player, int x, int y, int width, float time) {
		Stance.State s = player.getAttached(Stance.STANCE);
		if (s == null) {
			return;
		}
		long now = player.level().getGameTime();
		if (s.opened(now)) {
			float beat = 0.5F + 0.5F * Mth.sin(time * 0.9F);
			g.fill(x, y, x + width, y + 1, argb(mix(HOT, GOLD, beat), 1.0F));
			return;
		}
		double share = 1 - s.left(now);
		if (share <= 0.005) {
			return;
		}
		int half = width / 2;
		int fill = (int) Math.round(half * share);
		int cx = x + half;
		g.fill(cx - fill, y, cx + fill, y + 1, argb(ramp(share), 1.0F));
	}

	// ------------------------------------------------------------------ drawing

	/** Where {@code at} falls on the screen (gui pixels), or null when it's behind the camera or off the edge. */
	private static float[] screen(Minecraft mc, Camera camera, Vec3 at, int width, int height) {
		Vec3 offset = at.subtract(camera.position());
		Vector3fc forward = camera.forwardVector();
		if (offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() < 0.4) {
			return null;
		}
		Vec3 ndc = mc.gameRenderer.projectPointToScreen(at);
		if (!Double.isFinite(ndc.x) || !Double.isFinite(ndc.y) || Math.abs(ndc.x) > 0.97 || Math.abs(ndc.y) > 0.97) {
			return null;
		}
		return new float[] {(float) ((ndc.x + 1) / 2 * width), (float) ((1 - ndc.y) / 2 * height)};
	}

	/** A diamond three pixels tall at ({@code x}, {@code y} its middle row). */
	private static void diamond(GuiGraphicsExtractor g, int x, int y, int argb) {
		g.fill(x, y - 1, x + 1, y, argb);
		g.fill(x - 1, y, x + 2, y + 1, argb);
		g.fill(x, y + 1, x + 1, y + 2, argb);
	}

	/** A filled diamond of radius {@code r} round ({@code cx}, {@code cy}). */
	private static void diamondFill(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
		for (int dy = -r; dy <= r; dy++) {
			int w = r - Math.abs(dy);
			g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, argb);
		}
	}

	/** A diamond's outline, a pixel thick, of radius {@code r} round ({@code cx}, {@code cy}). */
	private static void diamondOutline(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
		for (int dy = -r; dy <= r; dy++) {
			int w = r - Math.abs(dy);
			g.fill(cx - w, cy + dy, cx - w + 1, cy + dy + 1, argb);
			if (w > 0) {
				g.fill(cx + w, cy + dy, cx + w + 1, cy + dy + 1, argb);
			}
		}
	}

	private static int argb(int rgb, float alpha) {
		return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int mix(int a, int b, float t) {
		float k = Mth.clamp(t, 0, 1);
		int r = Math.round(((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
		int gr = Math.round(((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
		int bl = Math.round((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
		return (r << 16) | (gr << 8) | bl;
	}

	// ------------------------------------------------------------------ for the game tests

	/** Foes' stance bars and opened seals drawn last frame, and whether the crosshair cue showed. */
	public static int[] drawn() {
		return new int[] {bars, seals, cue ? 1 : 0};
	}

	/** How worn your own stance looked last frame (0 to 1). */
	public static float ownShown() {
		return ownShown;
	}
}
