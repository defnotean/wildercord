package dev.wildercord.client.fx;

import dev.wildercord.content.SigilOption;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A magic circle: one large flat textured quad, tinted, facing any direction, turning around its
 * own centre, drawn from both sides at full brightness. It opens with a quick grow and fades out
 * over the last part of its life.
 */
public class SigilParticle extends SingleQuadParticle implements SigilGroup.Extent {
	/** Sprites in {@link SigilOption} style order, set when the provider is created. */
	static FabricSpriteSet sprites;

	protected float yaw;
	protected float pitch;
	protected float spin;
	protected float size;
	/** Steady sigils (aim previews) skip the grow and fade: they're replaced every few ticks. */
	protected boolean steady;
	/** A glow faces the camera and bursts: full at once, then fading over its whole life. */
	protected final boolean glow;
	/** While drawing a glow's white-hot core, a smaller second quad over the tinted one. */
	private boolean core;
	/** A band is drawn as short straight pieces around the circle, so its line is equally thin at any size. */
	protected final boolean band;
	/** How long one piece of a band is, in blocks, at the band's full size. */
	private static final float BAND_PIECE = 0.13F;

	protected SigilParticle(ClientLevel level, double x, double y, double z, SigilOption option) {
		super(level, x, y, z, sprite(option.style()));
		this.yaw = option.yaw();
		this.pitch = option.pitch();
		this.spin = option.spin();
		this.size = option.size();
		this.quadSize = option.size();
		this.glow = option.style() == SigilOption.GLOW;
		this.band = option.style() == SigilOption.BAND;
		this.lifetime = Math.max(2, option.lifetime());
		this.gravity = 0;
		this.hasPhysics = false;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		this.roll = random.nextFloat() * Mth.TWO_PI;
		this.oRoll = roll;
		int color = option.color();
		setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F);
		setAlpha(0);
	}

	static TextureAtlasSprite sprite(int style) {
		var list = sprites.getSprites();
		return list.get(Math.floorMod(style, list.size()));
	}

	/** A client-only sigil, e.g. the aim preview. */
	public static SigilParticle make(ClientLevel level, Vec3 at, SigilOption option, boolean steady) {
		SigilParticle particle = new SigilParticle(level, at.x, at.y, at.z, option);
		particle.steady = steady;
		return particle;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		oRoll = roll;
		roll += spin;
		if (age++ >= lifetime) {
			remove();
		}
	}

	/** Opening, then fading: 0 to 1. */
	protected float fade(float partial) {
		if (steady) {
			return 0.85F;
		}
		float t = age + partial;
		if (glow) {
			return Mth.clamp(t / 1.5F, 0, 1) * Mth.clamp((lifetime - t) / (lifetime * 0.75F), 0, 1);
		}
		float in = Mth.clamp(t / 4F, 0, 1);
		float out = Mth.clamp((lifetime - t) / (lifetime * 0.3F), 0, 1);
		return Math.min(in, out);
	}

	@Override
	public float getQuadSize(float partial) {
		if (steady) {
			return size;
		}
		if (glow) {
			float t = Mth.clamp((age + partial) / 3F, 0, 1);
			return size * (0.55F + 0.45F * (1 - (1 - t) * (1 - t))) * (core ? 0.42F : 1F);
		}
		float t = Mth.clamp((age + partial) / 6F, 0, 1);
		float ease = 1 - (1 - t) * (1 - t) * (1 - t);
		return size * (0.35F + 0.65F * ease);
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		alpha = fade(partial);
		if (alpha <= 0.01F) {
			return;
		}
		if (glow) {
			super.extract(state, camera, partial);
			float r = rCol;
			float g = gCol;
			float b = bCol;
			rCol = 1 - (1 - r) * 0.2F;
			gCol = 1 - (1 - g) * 0.2F;
			bCol = 1 - (1 - b) * 0.2F;
			core = true;
			super.extract(state, camera, partial);
			core = false;
			rCol = r;
			gCol = g;
			bCol = b;
			return;
		}
		float turn = Mth.lerp(partial, oRoll, roll);
		Quaternionf front = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), turn);
		if (band) {
			Vec3 cam = camera.position();
			extractBand(state, front, (float) (Mth.lerp(partial, xo, x) - cam.x), (float) (Mth.lerp(partial, yo, y) - cam.y),
				(float) (Mth.lerp(partial, zo, z) - cam.z), partial);
			return;
		}
		extractRotatedQuad(state, camera, front, partial);
		Quaternionf back = new Quaternionf(front).rotateY(Mth.PI);
		extractRotatedQuad(state, camera, back, partial);
	}

	/**
	 * A band around ({@code px}, {@code py}, {@code pz}) (relative to the camera) in the plane of
	 * {@code plane}, its radius {@link #getQuadSize}: straight pieces laid end to end, each turned
	 * along the circle and drawn from both sides.
	 */
	protected void extractBand(QuadParticleRenderState state, Quaternionf plane, float px, float py, float pz, float partial) {
		float r = getQuadSize(partial);
		int pieces = Math.max(16, (int) Math.ceil(Mth.TWO_PI * size / BAND_PIECE));
		float half = Mth.PI * r / pieces * 1.06F;
		int color = ARGB.colorFromFloat(alpha, rCol, gCol, bCol);
		int light = getLightCoords(partial);
		Layer layer = getLayer();
		Vector3f at = new Vector3f();
		for (int i = 0; i < pieces; i++) {
			float a = Mth.TWO_PI * i / pieces;
			plane.transform(at.set(Mth.cos(a) * r, Mth.sin(a) * r, 0));
			Quaternionf piece = new Quaternionf(plane).rotateZ(a + Mth.HALF_PI);
			for (int side = 0; side < 2; side++) {
				if (side == 1) {
					piece.rotateY(Mth.PI);
				}
				state.add(layer, px + at.x, py + at.y, pz + at.z, piece.x, piece.y, piece.z, piece.w, half,
					getU0(), getU1(), getV0(), getV1(), color, light);
			}
		}
	}

	@Override
	public int getLightCoords(float partial) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	@Override
	public ParticleRenderType getGroup() {
		return SigilGroup.TYPE;
	}

	public float size() {
		return size;
	}

	@Override
	public double centreX() {
		return x;
	}

	@Override
	public double centreY() {
		return y;
	}

	@Override
	public double centreZ() {
		return z;
	}

	@Override
	public double reach() {
		return size + 0.5;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}

	public static class Provider implements ParticleProvider<SigilOption> {
		public Provider(FabricSpriteSet set) {
			sprites = set;
		}

		@Override
		public Particle createParticle(SigilOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			return new SigilParticle(level, x, y, z, option);
		}
	}
}
