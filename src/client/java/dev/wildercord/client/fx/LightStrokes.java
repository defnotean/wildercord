package dev.wildercord.client.fx;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Painting light out of the mod's three soft sprites, for aura's feel ({@link AuraTrail}, {@link AuraWisp}, {@link AuraBurst}):
 * a ribbon of glow laid along a line in short pieces that turn to face the camera (so it keeps its width from any side), a
 * thinner white-hot core inside it, and round glows. Camera-relative coordinates throughout; render thread only.
 *
 * <p>The sprites are {@code sigil_beam} (a soft band across its middle: about 0.62 of a piece shows), {@code sigil_band} (a hard
 * thin band: about 0.31 shows) and {@code sigil_glow} (a round soft glow).</p>
 */
final class LightStrokes {
	private final QuadParticleRenderState state;
	private final SingleQuadParticle.Layer layer;
	private final Quaternionf turn = new Quaternionf();
	private final Vector3f dir = new Vector3f();
	final TextureAtlasSprite soft;
	final TextureAtlasSprite line;
	final TextureAtlasSprite glow;

	LightStrokes(QuadParticleRenderState state, SingleQuadParticle.Layer layer) {
		this.state = state;
		this.layer = layer;
		this.soft = SpellCircleParticle.particleSprite("sigil_beam");
		this.line = SpellCircleParticle.particleSprite("sigil_band");
		this.glow = SpellCircleParticle.particleSprite("sigil_glow");
	}

	/** How much of a piece of each band sprite shows across: a piece is the band's width over this. */
	static final float SOFT_SHOWS = 0.62F;
	static final float LINE_SHOWS = 0.31F;

	/**
	 * One piece of a ribbon at ({@code x}, {@code y}, {@code z}), lying along ({@code dx}, {@code dy}, {@code dz}) (need not be a
	 * unit length), facing the camera, {@code width} across where it shows, in {@code argb}.
	 */
	void piece(boolean core, float x, float y, float z, float dx, float dy, float dz, float width, int argb) {
		if ((argb >>> 24) < 3 || width <= 1.0E-4F) {
			return;
		}
		float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0E-6F || Vector3f.lengthSquared(x, y, z) < 1.0E-6F) {
			return;
		}
		dir.set(dx / len, dy / len, dz / len);
		float half = width / (core ? LINE_SHOWS : SOFT_SHOWS) / 2;
		quad(core ? line : soft, x, y, z, Facing.along(dir, x, y, z, turn), half, argb);
	}

	/**
	 * A ribbon from ({@code x0}, {@code y0}, {@code z0}) to ({@code x1}, {@code y1}, {@code z1}), {@code width} across, cut into
	 * pieces as long as it's wide, each in {@code argb}.
	 */
	void segment(boolean core, float x0, float y0, float z0, float x1, float y1, float z1, float width, int argb) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dz = z1 - z0;
		float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0E-4F || (argb >>> 24) < 3) {
			return;
		}
		float piece = Math.max(0.01F, width / (core ? LINE_SHOWS : SOFT_SHOWS));
		int n = Math.max(1, (int) Math.ceil(len / piece));
		// Each piece exactly its share of the segment, so they meet end to end (a square: a hair narrower than asked, at most).
		float half = len / n / 2;
		dir.set(dx / len, dy / len, dz / len);
		TextureAtlasSprite sprite = core ? line : soft;
		for (int i = 0; i < n; i++) {
			float f = (i + 0.5F) / n;
			float x = x0 + dx * f;
			float y = y0 + dy * f;
			float z = z0 + dz * f;
			if (Vector3f.lengthSquared(x, y, z) > 1.0E-6F) {
				quad(sprite, x, y, z, Facing.along(dir, x, y, z, turn), half, argb);
			}
		}
	}

	/** A round glow {@code size} across at ({@code x}, {@code y}, {@code z}), facing the camera. */
	void glow(float x, float y, float z, float size, int argb) {
		if ((argb >>> 24) < 3 || size <= 0 || Vector3f.lengthSquared(x, y, z) < 1.0E-6F) {
			return;
		}
		quad(glow, x, y, z, Facing.toward(x, y, z, turn), size / 2, argb);
	}

	/** A square of {@code sprite} {@code half} wide each way, turned by {@code q}. */
	void quad(TextureAtlasSprite sprite, float x, float y, float z, Quaternionf q, float half, int argb) {
		state.add(layer, x, y, z, q.x, q.y, q.z, q.w, half, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb,
			LightCoordsUtil.FULL_BRIGHT);
	}

	/** {@code rgb} at {@code alpha} (0 to 1). */
	static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	/** {@code rgb} moved {@code t} of the way to white: a light's hot core. */
	static int hot(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}
}
