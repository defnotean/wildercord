package dev.wildercord.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Small lights made on the client for places and creatures, never sent by the server:
 * <ul>
 *   <li>a <b>mote</b>: a soft speck that drifts, wanders a little and twinkles (dust in a shaft of
 *       light, an ember, a spark of a Runebound's aura);</li>
 *   <li>a <b>shaft</b>: a faint column of light falling from a lamp, turned to face the viewer;</li>
 *   <li>a <b>flicker</b>: a glow that wavers like firelight.</li>
 * </ul>
 * All of them fade in and out and are drawn at full brightness with the other lights (see
 * {@link SigilGroup}).
 */
public class Glimmer extends SingleQuadParticle implements SigilGroup.Extent {
	private static final int MOTE = 0;
	private static final int SHAFT = 1;
	private static final int FLICKER = 2;

	private final int kind;
	private final int color;
	private final float size;
	private final float peak;
	private final float phase;
	/** A mote's wander per tick; a shaft's length. */
	private final float extra;
	private final TextureAtlasSprite glow;
	private final TextureAtlasSprite beam;
	/** A mote's steady drift a tick; its wander is added on top and dies away. */
	private double driftX;
	private double driftY;
	private double driftZ;
	private float twinkle = 0.3F;
	private @Nullable Entity anchor;
	private double anchorY;

	private Glimmer(ClientLevel level, double x, double y, double z, int kind, int color, float size, float peak, int lifetime, float extra) {
		super(level, x, y, z, SpellCircleParticle.particleSprite("sigil_glow"));
		this.kind = kind;
		this.color = color & 0xFFFFFF;
		this.size = size;
		this.peak = peak;
		this.extra = extra;
		this.phase = random.nextFloat() * Mth.TWO_PI;
		this.lifetime = Math.max(2, lifetime);
		this.glow = SpellCircleParticle.particleSprite("sigil_glow");
		this.beam = SpellCircleParticle.particleSprite("sigil_beam");
		this.gravity = 0;
		this.hasPhysics = false;
		this.friction = 1;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
	}

	/** A drifting, twinkling speck {@code size} blocks across, moving ({@code vx}, {@code vy}, {@code vz}) a tick. */
	public static Glimmer mote(ClientLevel level, Vec3 at, int color, float size, float alpha, int lifetime, double vx, double vy, double vz,
			float wander) {
		Glimmer g = new Glimmer(level, at.x, at.y, at.z, MOTE, color, size / 2, alpha, lifetime, wander);
		g.driftX = vx;
		g.driftY = vy;
		g.driftZ = vz;
		return g;
	}

	/** A glow {@code size} blocks across that keeps {@code dy} above an entity's feet as it moves, without twinkling. */
	public static Glimmer haze(ClientLevel level, Entity entity, double dy, int color, float size, float alpha, int lifetime) {
		Glimmer g = new Glimmer(level, entity.getX(), entity.getY() + dy, entity.getZ(), MOTE, color, size / 2, alpha, lifetime, 0);
		g.anchor = entity;
		g.anchorY = dy;
		g.twinkle = 0;
		return g;
	}

	/** A column of light {@code width} across, from {@code top} straight down for {@code length} blocks. */
	public static Glimmer shaft(ClientLevel level, Vec3 top, int color, float width, float length, float alpha, int lifetime) {
		return new Glimmer(level, top.x, top.y, top.z, SHAFT, color, width, alpha, lifetime, length);
	}

	/** Firelight: a glow {@code size} blocks across that wavers in size and strength. */
	public static Glimmer flicker(ClientLevel level, Vec3 at, int color, float size, float alpha, int lifetime) {
		return new Glimmer(level, at.x, at.y, at.z, FLICKER, color, size / 2, alpha, lifetime, 0);
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
			return;
		}
		if (anchor != null) {
			if (anchor.isRemoved()) {
				remove();
				return;
			}
			x = anchor.getX();
			y = anchor.getY() + anchorY;
			z = anchor.getZ();
		} else if (kind == MOTE) {
			if (extra > 0) {
				xd = (xd + (random.nextFloat() - 0.5F) * extra) * 0.94;
				yd = (yd + (random.nextFloat() - 0.5F) * extra * 0.5F) * 0.94;
				zd = (zd + (random.nextFloat() - 0.5F) * extra) * 0.94;
			}
			x += driftX + xd;
			y += driftY + yd;
			z += driftZ + zd;
		}
	}

	/** In over the first fifth of its life, out over the last third. */
	private float envelope(float t) {
		float f = t / lifetime;
		return Math.min(Mth.clamp(f / 0.2F, 0, 1), Mth.clamp((1 - f) / 0.33F, 0, 1));
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | rgb;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		Vec3 cam = camera.position();
		float cx = (float) (Mth.lerp(partial, xo, x) - cam.x);
		float cy = (float) (Mth.lerp(partial, yo, y) - cam.y);
		float cz = (float) (Mth.lerp(partial, zo, z) - cam.z);
		float t = age + partial;
		float a = envelope(t) * peak;
		switch (kind) {
			case MOTE -> {
				float shimmer = 1 - twinkle + twinkle * Mth.sin(t * 0.35F + phase);
				billboard(state, cx, cy, cz, size, argb(a * shimmer, color));
			}
			case FLICKER -> {
				// Two quick waves and a slow one, never quite repeating.
				float waver = 0.78F + 0.12F * Mth.sin(t * 0.9F + phase) + 0.07F * Mth.sin(t * 2.3F + phase * 2) + 0.05F * Mth.sin(t * 0.21F);
				billboard(state, cx, cy, cz, size * (0.9F + 0.14F * waver), argb(a * waver, color));
			}
			case SHAFT -> shaft(state, cx, cy, cz, a);
			default -> { }
		}
	}

	/** A soft round glow at ({@code px}, {@code py}, {@code pz}) (camera-relative), facing the camera. */
	private void billboard(QuadParticleRenderState state, float px, float py, float pz, float half, int argb) {
		if ((argb >>> 24) < 2) {
			return;
		}
		if (Vector3f.lengthSquared(px, py, pz) < 1.0E-6F) {
			return;
		}
		quad(state, glow, px, py, pz, Facing.toward(px, py, pz, new Quaternionf()), half, argb);
	}

	/** Pieces of the soft beam laid down the column, each turned about the vertical to face the camera. */
	private void shaft(QuadParticleRenderState state, float px, float py, float pz, float a) {
		if (a < 0.008F) {
			return;
		}
		float length = extra;
		float side = size / 0.62F;
		int n = Math.max(1, (int) Math.ceil(length / side));
		float half = length / n / 2;
		// The beam sprite's band runs along its width, so the pieces lie with their x axis down the column.
		Vector3f d = new Vector3f(0, -1, 0);
		for (int i = 0; i < n; i++) {
			float along = (i + 0.5F) / n;
			float my = py - length * along;
			Vector3f zAxis = new Vector3f(-px, 0, -pz);
			if (zAxis.lengthSquared() < 1.0E-6F) {
				zAxis.set(0, 0, 1);
			}
			zAxis.normalize();
			Vector3f yAxis = new Vector3f(zAxis).cross(d).normalize();
			Quaternionf q = new Quaternionf().setFromNormalized(new Matrix3f(d, yAxis, zAxis));
			// Brightest near the lamp, thinning toward the floor.
			float fall = 0.35F + 0.65F * (1 - along);
			quad(state, beam, px, my, pz, q, half, argb(a * fall, color));
		}
	}

	private void quad(QuadParticleRenderState state, TextureAtlasSprite sprite, float x, float y, float z, Quaternionf q, float half, int argb) {
		state.add(getLayer(), x, y, z, q.x, q.y, q.z, q.w, half, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb,
			LightCoordsUtil.FULL_BRIGHT);
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

	@Override
	public double centreX() {
		return x;
	}

	@Override
	public double centreY() {
		return kind == SHAFT ? y - extra / 2 : y;
	}

	@Override
	public double centreZ() {
		return z;
	}

	@Override
	public double reach() {
		return kind == SHAFT ? extra / 2 + size : size + 0.3;
	}

	// ------------------------------------------------------------------ keeping count

	/**
	 * How many lights a spawner has out, counted by when each is due to end (so a light cleared
	 * early, as when the world changes, still frees its place in a moment). Spawners call
	 * {@link #tick} once a client tick.
	 */
	static final class Budget {
		private final int max;
		private final java.util.PriorityQueue<Long> ends = new java.util.PriorityQueue<>();
		private long now;

		Budget(int max) {
			this.max = max;
		}

		void tick() {
			now++;
			while (!ends.isEmpty() && ends.peek() <= now) {
				ends.poll();
			}
		}

		boolean hasRoom() {
			return ends.size() < max;
		}

		int out() {
			return ends.size();
		}

		void spend(int lifetime) {
			ends.add(now + lifetime + 1);
		}
	}
}
