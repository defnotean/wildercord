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

	public static void receive(WildercordNetworking.ScreenFx fx) {
		if (!MagicQuality.cameraShake && fx.kind() != WildercordNetworking.ScreenFx.TINT) return;
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

	/** The camera's shake right now, as a small rotation to multiply into the view. */
	public static void applyShake(Matrix4f pose, float partial) {
		// Paused, the world behind the menu holds still.
		if (shakeTicks <= 0 || shakeTotal <= 0 || Minecraft.getInstance().isPaused()) {
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
		if (kickTicks <= 0 || kickTotal <= 0) {
			return fov;
		}
		float t = 1 - (kickTicks - partial) / kickTotal;
		float curve = t < 0.2F ? t / 0.2F : 1 - (t - 0.2F) / 0.8F;
		return fov * (1 + kick * curve * scale());
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
