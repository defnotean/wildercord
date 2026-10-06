package dev.wildercord.client.fx;

import dev.wildercord.net.WildercordNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * The client side of {@link dev.wildercord.cast.ScreenFx}: camera shake, field-of-view kicks and
 * punches, and a Domain's tint on the screen's edges. Shaking and kicking follow the vanilla
 * "Screen Effect Scale" setting, so players who turned screen effects down get less of it.
 */
public final class ScreenEffects {
	private ScreenEffects() {}

	private static final RandomSource RANDOM = RandomSource.create();

	private static float shake;
	private static int shakeTicks;
	private static int shakeTotal;
	private static float kick;
	private static int kickTicks;
	private static int kickTotal;
	private static int tint;
	private static int tintTicks;
	private static float tintAlpha;

	/** Drop only local camera effects; preserve warning/domain tint and accepted aim. */
	public static void clearCameraMotion() {
		shake=kick=0;shakeTicks=shakeTotal=kickTicks=kickTotal=0;
		nudgeYaw=nudgePitch=0;nudgeAt=0;nudgeMillis=0;
	}

	public static void receive(WildercordNetworking.ScreenFx fx) {
		if ((!MagicQuality.cameraShake || dev.wildercord.client.combat.ArticulatedCombat.stableCamera())
			&& fx.kind() != WildercordNetworking.ScreenFx.TINT) return;
		switch (fx.kind()) {
			case WildercordNetworking.ScreenFx.SHAKE -> {
				if (fx.strength() >= remaining(shake, shakeTicks, shakeTotal)) {
					shake = fx.strength();
					shakeTicks = shakeTotal = fx.ticks();
				}
			}
			case WildercordNetworking.ScreenFx.KICK -> {
				kick = 0.06F + 0.06F * fx.strength();
				kickTicks = kickTotal = fx.ticks();
			}
			case WildercordNetworking.ScreenFx.PUNCH -> {
				kick = -0.04F * fx.strength();
				kickTicks = kickTotal = fx.ticks();
				if (0.35F * fx.strength() >= remaining(shake, shakeTicks, shakeTotal)) {
					shake = 0.35F * fx.strength();
					shakeTicks = shakeTotal = fx.ticks();
				}
			}
			case WildercordNetworking.ScreenFx.TINT -> {
				tint = Float.floatToRawIntBits(fx.strength()) & 0xFFFFFF;
				tintTicks = fx.ticks();
			}
			default -> { }
		}
	}

	private static float remaining(float strength, int ticks, int total) {
		return total <= 0 ? 0 : strength * ticks / total;
	}

	/** The world these effects began in: they stop when you leave it. */
	private static net.minecraft.client.multiplayer.ClientLevel seen;

	public static void tick(Minecraft mc) {
		if (mc.level != seen) {
			seen = mc.level;
			shakeTicks = 0;
			kickTicks = 0;
			tintTicks = 0;
			tintAlpha = 0;
			nudgeMillis = 0;
		}
		if (shakeTicks > 0) {
			shakeTicks--;
		}
		if (kickTicks > 0) {
			kickTicks--;
		}
		if (tintTicks > 0) {
			tintTicks--;
		}
		// The tint eases in and out rather than snapping.
		tintAlpha = Mth.lerp(0.15F, tintAlpha, tintTicks > 0 ? 1 : 0);
	}

	private static float scale() {
		if (MagicQuality.reducedFlash) return 0.15F;
		Minecraft mc = Minecraft.getInstance();
		return mc.options == null ? 1 : mc.options.screenEffectScale().get().floatValue();
	}

	/** Aura's nudge: a small turn of the view as a blow lands (degrees), and when it began (nanoseconds) and how long it lasts (ms). */
	private static float nudgeYaw;
	private static float nudgePitch;
	private static long nudgeAt;
	private static int nudgeMillis;

	/**
	 * A blow landing nudges the view: a quick turn of {@code yaw} and {@code pitch} degrees, eased back over {@code millis}. Left
	 * out with camera motion switched off, and softened by the screen effect setting.
	 */
	public static void nudge(float yaw, float pitch, int millis) {
		if (!MagicQuality.cameraShake || dev.wildercord.client.combat.ArticulatedCombat.stableCamera()
			|| millis <= 0 || (yaw == 0 && pitch == 0)) {
			return;
		}
		nudgeYaw = yaw;
		nudgePitch = pitch;
		nudgeAt = System.nanoTime();
		nudgeMillis = millis;
	}

	/** Whether a blow's nudge is under way (the game tests read it). */
	public static boolean nudging() {
		return nudgeMillis > 0 && (System.nanoTime() - nudgeAt) / 1.0E6F < nudgeMillis;
	}

	/** How far through its nudge the view is (0 to 1, out fast and eased back), or 0 when none is under way. */
	static float nudgeCurve() {
		if (nudgeMillis <= 0) {
			return 0;
		}
		float t = (System.nanoTime() - nudgeAt) / 1.0E6F / nudgeMillis;
		if (t >= 1 || t < 0) {
			nudgeMillis = 0;
			return 0;
		}
		return t < 0.18F ? t / 0.18F : (1 - (t - 0.18F) / 0.82F) * (1 - (t - 0.18F) / 0.82F);
	}

	/** The camera's shake right now, as a small rotation to multiply into the view (and a blow's nudge). */
	public static void applyShake(Matrix4f pose, float partial) {
		if (dev.wildercord.client.combat.ArticulatedCombat.stableCamera()) return;
		// Paused, the world behind the menu holds still.
		if (Minecraft.getInstance().isPaused()) {
			return;
		}
		float nudge = nudgeCurve() * scale();
		if (nudge > 0.001F) {
			pose.rotateX((float) Math.toRadians(nudgePitch * nudge));
			pose.rotateY((float) Math.toRadians(nudgeYaw * nudge));
		}
		if (shakeTicks <= 0 || shakeTotal <= 0) {
			return;
		}
		float t = (shakeTicks - partial) / shakeTotal;
		float amount = shake * t * t * scale();
		if (amount <= 0.001F) {
			return;
		}
		float degrees = 2.2F * amount;
		pose.rotateZ((float) Math.toRadians((RANDOM.nextFloat() - 0.5F) * 2 * degrees));
		pose.rotateX((float) Math.toRadians((RANDOM.nextFloat() - 0.5F) * degrees));
		pose.rotateY((float) Math.toRadians((RANDOM.nextFloat() - 0.5F) * degrees));
	}

	/** The field of view, multiplied by any kick or punch in progress (quick out, eased back). */
	public static float fov(float fov, float partial) {
		if (dev.wildercord.client.combat.ArticulatedCombat.stableCamera()) return fov;
		if (kickTicks <= 0 || kickTotal <= 0) {
			return fov;
		}
		float t = 1 - (kickTicks - partial) / kickTotal;
		float curve = t < 0.2F ? t / 0.2F : 1 - (t - 0.2F) / 0.8F;
		return fov * (1 + kick * curve * scale());
	}

	/**
	 * An overchannelling caster's own view: the edges of the screen close in a little more with each
	 * stage, breathing with the hum, with a hairline crack running in from each corner per stage; at the
	 * last stage they redden and close faster as the moment it would tear loose comes on. Only the
	 * caster sees it (everyone sees the circle crack), and the screen-effect setting softens it.
	 */
	public static void drawStrain(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		dev.wildercord.player.WildercordAttachments.Charge charge = mc.player.getAttached(dev.wildercord.player.WildercordAttachments.CHARGE);
		if (charge == null || charge.stage() <= 0) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		float now = mc.level.getGameTime() + partial;
		int stage = charge.stage();
		float tension = stage >= charge.stages()
			? Mth.clamp((now - charge.stageTime()) / dev.wildercord.spell.Overchannel.GRACE, 0, 1) : 0;
		float soften = Math.max(0.4F, scale());
		float breath = 0.85F + 0.15F * Mth.sin(now * (0.25F + 0.1F * stage));
		int w = g.guiWidth();
		int h = g.guiHeight();
		int band = Math.max(16, (int) (Math.min(w, h) * (0.06F + 0.045F * stage + 0.08F * tension)));
		float strength = (0.16F + 0.08F * stage + 0.2F * tension) * breath * soften;
		int tint = tension > 0 ? lerpColor(0x12051E, 0x3A0808, tension) : 0x12051E;
		for (int x = 0; x < band; x += 2) {
			float f = 1 - x / (float) band;
			int a = (int) (255 * strength * f * f);
			if (a <= 0) {
				continue;
			}
			int color = (Math.min(255, a) << 24) | tint;
			g.fill(x, x, w - x, x + 2, color);
			g.fill(x, h - x - 2, w - x, h - x, color);
			g.fill(x, x + 2, x + 2, h - x - 2, color);
			g.fill(w - x - 2, x + 2, w - x, h - x - 2, color);
		}
		// A hairline crack in from each corner for every stage, pale, wandering a little.
		int crack = ((int) (150 * soften * breath) << 24) | (tension > 0.5F ? 0xFFB0A0 : 0xD8CCFF);
		int length = (int) (band * 1.6F);
		for (int s = 0; s < stage; s++) {
			for (int corner = 0; corner < 4; corner++) {
				int sx = (corner & 1) == 0 ? 1 : -1;
				int sy = (corner & 2) == 0 ? 1 : -1;
				int x0 = sx > 0 ? 0 : w - 1;
				int y0 = sy > 0 ? 0 : h - 1;
				// Each stage's crack leaves its corner at its own angle.
				float angle = 0.35F + 0.4F * s + 0.1F * corner;
				int px = x0;
				int py = y0;
				for (int k = 1; k <= 4; k++) {
					float d = length * k / 4F;
					float wobble = ((s * 7 + corner * 3 + k * 5) % 5 - 2) * 0.12F;
					int qx = x0 + sx * (int) (Mth.cos(angle + wobble) * d);
					int qy = y0 + sy * (int) (Mth.sin(angle + wobble) * d);
					hairline(g, px, py, qx, qy, crack);
					px = qx;
					py = qy;
				}
			}
		}
	}

	/** A one-pixel line from one point to another, as a single turned bar. */
	private static void hairline(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		float length = (float) Math.hypot(x1 - x0, y1 - y0);
		if (length < 1) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x0, y0);
		g.pose().rotate((float) Math.atan2(y1 - y0, x1 - x0));
		g.fill(0, 0, Math.round(length), 1, color);
		g.pose().popMatrix();
	}

	private static int lerpColor(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int gr = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (gr << 8) | bl;
	}

	/** A Domain's tint: a soft coloured band around the edges of the screen. */
	public static void drawTint(GuiGraphicsExtractor g) {
		if (tintAlpha <= 0.01F) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		int band = Math.max(24, Math.min(w, h) / 6);
		int edge = ((int) (tintAlpha * 110) << 24) | tint;
		int clear = tint;
		g.fillGradient(0, 0, w, band, edge, clear);
		g.fillGradient(0, h - band, w, h, clear, edge);
		for (int x = 0; x < band; x += 2) {
			float f = 1 - x / (float) band;
			int a = (int) (tintAlpha * 110 * f * f);
			g.fill(x, 0, x + 2, h, (a << 24) | tint);
			g.fill(w - x - 2, 0, w - x, h, (a << 24) | tint);
		}
	}
}
