package dev.wildercord.client.compat;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.client.fx.GlowLayers;
import dev.wildercord.client.compat.McQuadParticle;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Stands in for 1.21.9's {@code QuadParticleRenderState}, which 1.21.8 doesn't have: it collects the
 * quads a particle wants drawn and writes each one straight to the buffer its layer asks for. Positions
 * are camera-relative, the way every caller here already passes them.
 */
public class QuadParticleRenderState {
	private final MultiBufferSource buffers;

	public QuadParticleRenderState(MultiBufferSource buffers) {
		this.buffers = buffers;
	}

	/** For a test that only counts what a particle hands over: there is nothing to draw to. */
	protected QuadParticleRenderState() {
		this.buffers = null;
	}

	/** The buffer a layer's quads go on. */
	public VertexConsumer bufferFor(McQuadParticle.Layer layer) {
		return buffers == null ? null : buffers.getBuffer(layer.renderType());
	}

	/**
	 * One quad: centred at ({@code x}, {@code y}, {@code z}), turned by the quaternion, reaching
	 * {@code scale} from the centre along its own right and up, with the sprite's corners at
	 * ({@code u0}..{@code u1}, {@code v0}..{@code v1}) and {@code color} as ARGB.
	 */
	public void add(McQuadParticle.Layer layer, float x, float y, float z, float xRot, float yRot, float zRot, float wRot,
			float scale, float u0, float u1, float v0, float v1, int color, int lightCoords) {
		if ((color >>> 24) == 0 || scale <= 0F) {
			return;
		}
		// Under a shader pack magic goes on vanilla's translucent layer (see ShaderCompat): light keeps
		// its colour, darkness becomes the dark shade that taking light away would leave behind.
		McQuadParticle.Layer drawn = layer;
		int argb = color;
		if ((layer == GlowLayers.GLOW || layer == GlowLayers.DARK) && ShaderCompat.active()) {
			drawn = McQuadParticle.Layer.TRANSLUCENT;
			if (layer == GlowLayers.DARK) {
				argb = GlowLayers.darkAsShade(color);
			}
		}
		VertexConsumer buffer = bufferFor(drawn);
		if (buffer == null) {
			return;
		}
		Quaternionf turn = new Quaternionf(xRot, yRot, zRot, wRot);
		Vector3f right = new Vector3f(1F, 0F, 0F);
		Vector3f up = new Vector3f(0F, 1F, 0F);
		Vector3f back = new Vector3f(right).cross(up).normalize().mul(-1F);
		right.rotate(turn);
		up.rotate(turn);
		back.rotate(turn);
		float red = (argb >> 16 & 0xFF) / 255F;
		float green = (argb >> 8 & 0xFF) / 255F;
		float blue = (argb & 0xFF) / 255F;
		float alpha = (argb >>> 24) / 255F;
		vertex(buffer, x, y, z, right, up, back, -scale, -scale, u0, v1, red, green, blue, alpha, lightCoords);
		vertex(buffer, x, y, z, right, up, back, scale, -scale, u1, v1, red, green, blue, alpha, lightCoords);
		vertex(buffer, x, y, z, right, up, back, scale, scale, u1, v0, red, green, blue, alpha, lightCoords);
		vertex(buffer, x, y, z, right, up, back, -scale, scale, u0, v0, red, green, blue, alpha, lightCoords);
	}

	private static void vertex(VertexConsumer buffer, float x, float y, float z, Vector3f right, Vector3f up, Vector3f back,
			float along, float across, float u, float v, float red, float green, float blue, float alpha, int lightCoords) {
		buffer.addVertex(
				x + right.x * along + up.x * across,
				y + right.y * along + up.y * across,
				z + right.z * along + up.z * across)
			.setUv(u, v)
			.setColor(red, green, blue, alpha)
			.setLight(lightCoords)
			.setNormal(-back.x, -back.y, -back.z);
	}
}
