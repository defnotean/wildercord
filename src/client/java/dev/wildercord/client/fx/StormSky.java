package dev.wildercord.client.fx;

import dev.wildercord.cast.events.ManaStorm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Under a mana storm the sky and the fog take a faint violet cast, easing in over a few seconds and
 * out again as it passes. Read from the synced {@link ManaStorm#STORM_UNTIL}; drawn by the sky and
 * fog mixins. Kept subtle: the world should look charged, not dyed.
 */
public final class StormSky {
	private StormSky() {}

	/** The violet the sky leans toward, and how far at most (sky, then fog). */
	private static final float R = 0.46F;
	private static final float G = 0.30F;
	private static final float B = 0.78F;
	private static final float SKY = 0.22F;
	private static final float FOG = 0.14F;

	private static float amount;
	private static long lastNanos;

	/** 0 (no storm) to 1 (well inside one), eased over about three seconds. */
	public static float amount() {
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0 : Math.min(0.25F, (now - lastNanos) / 1.0E9F);
		lastNanos = now;
		LocalPlayer player = Minecraft.getInstance().player;
		float target = player != null && ManaStorm.inside(player) ? 1.0F : 0.0F;
		float step = dt / 3.0F;
		amount = target > amount ? Math.min(target, amount + step) : Math.max(target, amount - step);
		return amount;
	}

	public static float surge() {
		return amount;
	}

	public static void tintSky(SkyRenderState state) {
		float k = amount() * SKY;
		if (k <= 0.001F || state.skyColor == null) {
			return;
		}
		state.skyColor = new Vector3f(
			state.skyColor.x() + (R - state.skyColor.x()) * k,
			state.skyColor.y() + (G - state.skyColor.y()) * k,
			state.skyColor.z() + (B - state.skyColor.z()) * k);
	}

	public static void tintFog(Vector4f color) {
		float k = amount() * FOG;
		if (k <= 0.001F) {
			return;
		}
		color.set(color.x + (R - color.x) * k, color.y + (G - color.y) * k, color.z + (B - color.z) * k, color.w);
	}
}
