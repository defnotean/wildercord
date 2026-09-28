package dev.wildercord.client.fx;

import dev.wildercord.content.MoteOption;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Small soft lights and vapours (see {@link MoteOption}): a glowing speck that drifts and twinkles; a
 * speck that spirals in to a point, trailing light; a butterfly of light, wings beating as it
 * flutters; and a soft billow of steam or smoke that swells, rises, lingers and thins away. Lights
 * add to what's behind them at full brightness; a billow is lit like the world and hides a little of
 * what's behind it. The server sends them, or the client makes them itself (an attunement's motes).
 */
public class MoteParticle extends SingleQuadParticle implements SigilGroup.Extent {
	private static final int TRAIL = 5;

	private final int kind;
	private final int color;
	private final float size;
	private final float extra;
	private final float phase;
	/** A steady drift a tick (a glow, a butterfly, a cloud), or the way from start to end (a seeker). */
	private double driftX;
	private double driftY;
	private double driftZ;
	/** Where a seeker set out from, and the two directions round its way it spirals in. */
	private final double startX;
	private final double startY;
	private final double startZ;
	private Vector3f spiralU;
	private Vector3f spiralV;
	private float spiral;
	/** A seeker's last few places, newest first, for its tail. */
	private final double[] trail = new double[TRAIL * 3];
	/** A butterfly's heading, and how it swings. */
	private float heading;
	private float turn;
	private final TextureAtlasSprite glow;
	private final TextureAtlasSprite soft;
	private final TextureAtlasSprite wing;
	/** A billow's slow turn. */
	private final float spin;

	protected MoteParticle(ClientLevel level, double x, double y, double z, MoteOption option, double pushX, double pushY, double pushZ) {
		super(level, x, y, z, cloudSprite(option.kind(), level.getRandom()));
		this.kind = option.kind();
		this.color = option.color() & 0xFFFFFF;
		this.size = Math.max(0.02F, option.size());
		this.extra = option.extra();
		this.lifetime = Math.max(2, option.lifetime());
		this.phase = random.nextFloat() * Mth.TWO_PI;
		this.driftX = option.dx();
		this.driftY = option.dy();
		this.driftZ = option.dz();
		this.startX = x;
		this.startY = y;
		this.startZ = z;
		this.glow = SpellCircleParticle.particleSprite("sigil_glow");
		this.soft = SpellCircleParticle.particleSprite("sigil_soft");
		this.wing = SpellCircleParticle.particleSprite("mote_wing");
		this.spin = (random.nextFloat() - 0.5F) * 0.03F;
		this.gravity = 0;
		this.hasPhysics = false;
		this.friction = 1;
		// A push from the packet (or a burst): it dies away, while the drift keeps on.
		this.xd = pushX;
		this.yd = pushY;
		this.zd = pushZ;
		this.roll = random.nextFloat() * Mth.TWO_PI;
		this.oRoll = roll;
		if (kind == MoteOption.SEEK) {
			Vector3f d = new Vector3f((float) driftX, (float) driftY, (float) driftZ);
			float length = d.length();
			if (length < 1.0E-4F) {
				d.set(0, 1, 0);
			} else {
				d.div(length);
			}
			Vector3f side = Math.abs(d.y) > 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
			spiralU = new Vector3f(side).cross(d).normalize();
			spiralV = new Vector3f(d).cross(spiralU).normalize();
			spiral = Mth.clamp(length * 0.28F, 0.12F, 0.8F);
		}
		if (kind == MoteOption.BUTTERFLY) {
			heading = (float) Math.atan2(driftZ, driftX);
			if (driftX * driftX + driftZ * driftZ < 1.0E-6) {
				heading = random.nextFloat() * Mth.TWO_PI;
			}
		}
		for (int i = 0; i < TRAIL; i++) {
			trail[i * 3] = x;
			trail[i * 3 + 1] = y;
			trail[i * 3 + 2] = z;
		}
		if (kind == MoteOption.CLOUD) {
			setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F);
		}
		setAlpha(0);
	}

	private static TextureAtlasSprite cloudSprite(int kind, RandomSource random) {
		return SpellCircleParticle.particleSprite(kind == MoteOption.CLOUD ? "mote_cloud_" + random.nextInt(4) : "sigil_glow");
	}

	// ------------------------------------------------------------------ made on the client

	/** A speck flying from {@code from} to {@code to} over {@code lifetime} ticks, spiralling {@code turns} times round the way. */
	public static MoteParticle seek(ClientLevel level, Vec3 from, Vec3 to, int color, float size, int lifetime, float turns) {
		Vec3 d = to.subtract(from);
		return new MoteParticle(level, from.x, from.y, from.z,
			new MoteOption(MoteOption.SEEK, color, size, lifetime, (float) d.x, (float) d.y, (float) d.z, turns), 0, 0, 0);
	}

	/** A speck drifting along {@code drift} a tick, pushed off along {@code push} (which dies away). */
	public static MoteParticle glow(ClientLevel level, Vec3 at, int color, float size, int lifetime, Vec3 drift, Vec3 push, float wander) {
		return new MoteParticle(level, at.x, at.y, at.z,
			new MoteOption(MoteOption.GLOW, color, size, lifetime, (float) drift.x, (float) drift.y, (float) drift.z, wander), push.x, push.y, push.z);
	}

	// ------------------------------------------------------------------ moving

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		oRoll = roll;
		if (age++ >= lifetime) {
			remove();
			return;
		}
		System.arraycopy(trail, 0, trail, 3, (TRAIL - 1) * 3);
		trail[0] = x;
		trail[1] = y;
		trail[2] = z;
		switch (kind) {
			case MoteOption.SEEK -> {
				float t = Math.min(1, age / (float) lifetime);
				Vector3f p = seekAt(t);
				x = startX + p.x;
				y = startY + p.y;
				z = startZ + p.z;
			}
			case MoteOption.BUTTERFLY -> {
				// It wanders off its line in lazy swings, and bobs with every wingbeat.
				turn = (turn + (random.nextFloat() - 0.5F) * 0.12F * extra) * 0.92F;
				heading += turn;
				float speed = (float) Math.sqrt(driftX * driftX + driftZ * driftZ);
				double lean = 0.02 * extra;
				x += driftX + Mth.cos(heading) * lean + xd;
				z += driftZ + Mth.sin(heading) * lean + zd;
				y += driftY + Mth.sin(age * 0.9F + phase) * 0.025F + yd;
				if (speed > 1.0E-4F) {
					// The drift turns with it, a little.
					float base = (float) Math.atan2(driftZ, driftX);
					float delta = Mth.wrapDegrees((heading - base) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
					float swing = base + delta * 0.04F;
					driftX = Mth.cos(swing) * speed;
					driftZ = Mth.sin(swing) * speed;
				}
				xd *= 0.9;
				yd *= 0.9;
				zd *= 0.9;
			}
			case MoteOption.CLOUD -> {
				x += driftX + xd;
				y += driftY + yd;
				z += driftZ + zd;
				// It rises slower as it goes, and any push it had dies away.
				driftX *= 0.975;
				driftY *= 0.975;
				driftZ *= 0.975;
				xd *= 0.88;
				yd *= 0.88;
				zd *= 0.88;
				roll += spin;
			}
			default -> {
				if (extra > 0) {
					xd += (random.nextFloat() - 0.5F) * extra;
					yd += (random.nextFloat() - 0.5F) * extra * 0.5F;
					zd += (random.nextFloat() - 0.5F) * extra;
				}
				x += driftX + xd;
				y += driftY + yd;
				z += driftZ + zd;
				xd *= 0.9;
				yd *= 0.9;
				zd *= 0.9;
			}
		}
	}

	/** Where a seeker is at {@code t} (0 to 1) of its flight, from where it set out: slow off the ground, quickening in, the spiral closing. */
	private Vector3f seekAt(float t) {
		float along = t * t * (3 - 2 * t) * 0.35F + t * t * 0.65F;
		float r = spiral * (1 - along) * Mth.sin(Math.min(1, t * 4) * Mth.HALF_PI);
		float a = phase + extra * Mth.TWO_PI * along;
		return new Vector3f((float) driftX * along, (float) driftY * along, (float) driftZ * along)
			.add(new Vector3f(spiralU).mul(Mth.cos(a) * r))
			.add(new Vector3f(spiralV).mul(Mth.sin(a) * r));
	}

	// ------------------------------------------------------------------ drawing

	/** In over the first part of its life, out over the last. */
	private float envelope(float t) {
		float f = t / lifetime;
		return switch (kind) {
			case MoteOption.CLOUD -> Math.min(Mth.clamp(f / 0.15F, 0, 1), Mth.clamp((1 - f) / 0.55F, 0, 1));
			case MoteOption.SEEK -> Math.min(Mth.clamp(f / 0.15F, 0, 1), Mth.clamp((1 - f) / 0.12F, 0, 1));
			default -> Math.min(Mth.clamp(f / 0.2F, 0, 1), Mth.clamp((1 - f) / 0.35F, 0, 1));
		};
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	/** {@code rgb} moved {@code t} of the way to white. */
	private static int hot(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}

	@Override
	public float getQuadSize(float partial) {
		// A billow swells as it rises.
		float f = Mth.clamp((age + partial) / lifetime, 0, 1);
		return size / 2 * (0.45F + 0.55F * (1 - (1 - f) * (1 - f)));
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float t = age + partial;
		float a = envelope(t);
		if (a <= 0.01F) {
			return;
		}
		if (kind == MoteOption.CLOUD) {
			alpha = a * Mth.clamp(extra <= 0 ? 0.45F : extra, 0, 1);
			super.extract(state, camera, partial);
			return;
		}
		Vec3 cam = camera.position();
		float px = (float) (Mth.lerp(partial, xo, x) - cam.x);
		float py = (float) (Mth.lerp(partial, yo, y) - cam.y);
		float pz = (float) (Mth.lerp(partial, zo, z) - cam.z);
		switch (kind) {
			case MoteOption.SEEK -> {
				// The tail: where it just was, fainter and smaller the further back.
				for (int i = TRAIL - 1; i >= 0; i--) {
					float k = 1 - (i + 1) / (float) (TRAIL + 1);
					float tx = (float) (trail[i * 3] - cam.x);
					float ty = (float) (trail[i * 3 + 1] - cam.y);
					float tz = (float) (trail[i * 3 + 2] - cam.z);
					billboard(state, soft, tx, ty, tz, size * 0.5F * (0.35F + 0.5F * k), argb(a * 0.45F * k, color));
				}
				billboard(state, glow, px, py, pz, size * 0.75F, argb(a, color));
				billboard(state, soft, px, py, pz, size * 0.3F, argb(a, hot(color, 0.7F)));
			}
			case MoteOption.BUTTERFLY -> butterfly(state, px, py, pz, t, a);
			default -> {
				float shimmer = 0.7F + 0.3F * Mth.sin(t * 0.45F + phase);
				billboard(state, glow, px, py, pz, size * 0.5F, argb(a * shimmer, color));
				billboard(state, soft, px, py, pz, size * 0.22F, argb(a * shimmer, hot(color, 0.7F)));
			}
		}
	}

	/** Two wings beating on a glowing body, and a faint halo of its colour round it all. */
	private void butterfly(QuadParticleRenderState state, float px, float py, float pz, float t, float a) {
		float h = heading - turn * (1 - Math.min(1, t - (int) t));
		Vector3f f = new Vector3f(Mth.cos(h), 0, Mth.sin(h));
		Vector3f up = new Vector3f(0, 1, 0);
		Vector3f right = new Vector3f(f).cross(up).normalize();
		// Wings sweep from nearly flat to nearly closed above the back, quickly, again and again.
		float beat = 0.5F + 0.5F * Mth.sin(t * 1.35F + phase);
		float lift = 0.12F + 1.15F * beat * beat;
		float half = size / 4;
		int tint = argb(a * 0.95F, color);
		for (int side = -1; side <= 1; side += 2) {
			Vector3f out = new Vector3f(right).mul(side * Mth.cos(lift)).add(new Vector3f(up).mul(Mth.sin(lift)));
			Vector3f normal = new Vector3f(out).cross(f).normalize();
			Quaternionf q = new Quaternionf().setFromNormalized(new Matrix3f(out, f, normal));
			float wx = px + out.x * half;
			float wy = py + out.y * half;
			float wz = pz + out.z * half;
			quad(state, wing, wx, wy, wz, q, half, tint, false);
			quad(state, wing, wx, wy, wz, new Quaternionf(q).rotateY(Mth.PI), half, tint, true);
		}
		// The body: a short bright streak along its heading.
		for (int i = -1; i <= 1; i++) {
			float k = i * size * 0.09F;
			billboard(state, soft, px + f.x * k, py + f.y * k, pz + f.z * k, size * 0.07F, argb(a, hot(color, 0.75F)));
		}
		billboard(state, soft, px, py, pz, size * 0.55F, argb(a * 0.22F, color));
	}

	/** A glow at ({@code px}, {@code py}, {@code pz}) (camera-relative), facing the camera. */
	private void billboard(QuadParticleRenderState state, TextureAtlasSprite sprite, float px, float py, float pz, float half, int argb) {
		if ((argb >>> 24) < 2 || half <= 0) {
			return;
		}
		Vector3f z = new Vector3f(-px, -py, -pz);
		if (z.lengthSquared() < 1.0E-6F) {
			return;
		}
		z.normalize();
		Vector3f upAxis = Math.abs(z.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
		Vector3f xAxis = new Vector3f(upAxis).cross(z).normalize();
		Vector3f yAxis = new Vector3f(z).cross(xAxis).normalize();
		quad(state, sprite, px, py, pz, new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, z)), half, argb, false);
	}

	private void quad(QuadParticleRenderState state, TextureAtlasSprite sprite, float x, float y, float z, Quaternionf q, float half, int argb,
			boolean mirrored) {
		state.add(GlowLayers.GLOW, x, y, z, q.x, q.y, q.z, q.w, half, mirrored ? sprite.getU1() : sprite.getU0(),
			mirrored ? sprite.getU0() : sprite.getU1(), sprite.getV0(), sprite.getV1(), argb, LightCoordsUtil.FULL_BRIGHT);
	}

	@Override
	public int getLightCoords(float partial) {
		return kind == MoteOption.CLOUD ? super.getLightCoords(partial) : LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return kind == MoteOption.CLOUD ? Layer.TRANSLUCENT : GlowLayers.GLOW;
	}

	@Override
	public ParticleRenderType getGroup() {
		return SigilGroup.TYPE;
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
		return size + (kind == MoteOption.SEEK ? 1.0 : 0.3);
	}

	public static class Provider implements ParticleProvider<MoteOption> {
		@Override
		public Particle createParticle(MoteOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			return new MoteParticle(level, x, y, z, option, xa, ya, za);
		}
	}
}
