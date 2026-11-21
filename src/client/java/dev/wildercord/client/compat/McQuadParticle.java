package dev.wildercord.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.client.compat.McQuadParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The shape 1.21.9's {@code SingleQuadParticle} gives spell effects, on 1.21.8. There the particle fills
 * a {@code QuadParticleRenderState} and names the layer each quad belongs to; here a particle still draws
 * itself into one buffer. This base keeps the newer shape: subclasses override {@link #extract} and
 * {@link #getLayer} exactly as they do on 1.21.9, and the quads go to the buffer their layer asks for.
 * Particles report {@code CUSTOM} so the engine hands over the whole buffer source.
 */
public abstract class McQuadParticle extends SingleQuadParticle {
	/** How a quad is blended: 1.21.9's {@code McQuadParticle.Layer}, which 1.21.8 has no equivalent of. */
	public static final class Layer {
		/** Vanilla's translucent particles: light that hides a little of what's behind it. */
		public static final Layer TRANSLUCENT = new Layer(RenderType.translucentParticle(TextureAtlas.LOCATION_PARTICLES));
		/** Vanilla's solid particles. */
		public static final Layer OPAQUE = new Layer(RenderType.opaqueParticle(TextureAtlas.LOCATION_PARTICLES));

		private final RenderType renderType;

		Layer(RenderType renderType) {
			this.renderType = renderType;
		}

		public RenderType renderType() {
			return renderType;
		}
	}

	protected TextureAtlasSprite sprite;

	protected McQuadParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	protected McQuadParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
		super(level, x, y, z);
		this.sprite = sprite;
	}

	protected McQuadParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
		super(level, x, y, z, vx, vy, vz);
	}

	protected McQuadParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
			TextureAtlasSprite sprite) {
		super(level, x, y, z, vx, vy, vz);
		this.sprite = sprite;
	}

	public void setSprite(TextureAtlasSprite sprite) {
		this.sprite = sprite;
	}

	/** The layer this particle's own quad (the one {@link #extractDefault} draws) belongs to. */
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	/** The group the engine batches this particle under; {@code CUSTOM} so {@link #renderCustom} gets the buffers. */
	public ParticleRenderType getGroup() {
		return ParticleRenderType.CUSTOM;
	}

	/** Fills {@code state} with this particle's quads, in camera-relative space. A particle that only
	 * draws its own sprite gets vanilla's billboard for free. */
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		extractDefault(state, camera, partial);
	}

	/** One billboard quad, turned by {@code rotation}: 1.21.9's {@code extractRotatedQuad}. */
	protected void extractRotatedQuad(QuadParticleRenderState state, Camera camera, Quaternionf rotation, float partial) {
		Vec3 eye = camera.getPosition();
		float px = (float) (Mth.lerp(partial, xo, x) - eye.x);
		float py = (float) (Mth.lerp(partial, yo, y) - eye.y);
		float pz = (float) (Mth.lerp(partial, zo, z) - eye.z);
		state.add(getLayer(), px, py, pz, rotation.x(), rotation.y(), rotation.z(), rotation.w(),
				getQuadSize(partial), getU0(), getU1(), getV0(), getV1(), particleArgb(), getLightColor(partial));
	}

	/** The particle's own colour as ARGB, which 1.21.8 keeps as four floats. */
	protected int particleArgb() {
		int a = (int) (Mth.clamp(alpha, 0F, 1F) * 255F);
		int r = (int) (Mth.clamp(rCol, 0F, 1F) * 255F);
		int g = (int) (Mth.clamp(gCol, 0F, 1F) * 255F);
		int b = (int) (Mth.clamp(bCol, 0F, 1F) * 255F);
		return a << 24 | r << 16 | g << 8 | b;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return getGroup();
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partial) {
		extract(new QuadParticleRenderState(Minecraft.getInstance().renderBuffers().bufferSource()), camera, partial);
	}

	@Override
	public void renderCustom(PoseStack pose, MultiBufferSource buffers, Camera camera, float partial) {
		extract(new QuadParticleRenderState(buffers), camera, partial);
	}

	/** The plain quad vanilla would draw for this particle: its own sprite, colour, roll and light. */
	protected void extractDefault(QuadParticleRenderState state, Camera camera, float partial) {
		super.render(state.bufferFor(getLayer()), camera, partial);
	}

	@Override
	protected float getU0() {
		return sprite.getU0();
	}

	@Override
	protected float getU1() {
		return sprite.getU1();
	}

	@Override
	protected float getV0() {
		return sprite.getV0();
	}

	@Override
	protected float getV1() {
		return sprite.getV1();
	}
}
