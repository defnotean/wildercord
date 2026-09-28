package dev.wildercord.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
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
 * The pieces a Shield's circle breaks into (see {@link ShieldCircles}): flat shards of glassy light
 * from its face, and curved slivers of its rim. Each holds its place for a moment, then flies,
 * tumbles, falls, skitters over the ground and glints as it turns to the light.
 */
public final class ShieldBreak {
	private ShieldBreak() {}

	/** {@code a} blended toward {@code b} by {@code f}. */
	static int lerp(int a, int b, float f) {
		int r = Math.round(Mth.lerp(f, (a >> 16) & 0xFF, (b >> 16) & 0xFF));
		int g = Math.round(Mth.lerp(f, (a >> 8) & 0xFF, (b >> 8) & 0xFF));
		int bl = Math.round(Mth.lerp(f, a & 0xFF, b & 0xFF));
		return (r << 16) | (g << 8) | bl;
	}

	// ------------------------------------------------------------------ the pieces

	/** One piece of the shell: a flat shard of glassy light that holds its place for a moment, then flies, tumbles and falls. */
	static class Shard extends SingleQuadParticle {
		/** Where light comes from, for the glints: high and a little to one side, like the sun. */
		private static final Vector3f LIGHT = new Vector3f(0.35F, 1, 0.25F).normalize();

		private final Quaternionf spin = new Quaternionf();
		private final Quaternionf oSpin = new Quaternionf();
		private final Vector3f axis;
		private float turn;
		private final float size;
		private final int color;
		private final int hold;
		private final float vx;
		private final float vy;
		private final float vz;
		private final TextureAtlasSprite glass;

		Shard(ClientLevel level, Vec3 at, Vector3f velocity, Vector3f normal, float size, int color, int hold, RandomSource random) {
			super(level, at.x, at.y, at.z, SpellCircleParticle.particleSprite("glass_shard_" + random.nextInt(6)));
			this.glass = SpellCircleParticle.particleSprite("glass_shard_" + random.nextInt(6));
			this.size = Math.max(0.035F, size);
			this.color = color;
			this.hold = hold;
			this.vx = velocity.x;
			this.vy = velocity.y;
			this.vz = velocity.z;
			this.xd = 0;
			this.yd = 0;
			this.zd = 0;
			this.gravity = 0;
			this.friction = 1;
			this.hasPhysics = true;
			this.lifetime = hold + 45 + random.nextInt(35);
			setSize(Math.min(0.2F, this.size), Math.min(0.2F, this.size));
			// Lying on the shell to begin with, turned at random within it.
			Vector3f up = Math.abs(normal.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
			Vector3f x = new Vector3f(up).cross(normal).normalize();
			Vector3f y = new Vector3f(normal).cross(x).normalize();
			spin.setFromNormalized(new Matrix3f(x, y, normal)).rotateZ(random.nextFloat() * Mth.TWO_PI);
			oSpin.set(spin);
			axis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
			if (axis.lengthSquared() < 1.0E-4F) {
				axis.set(0, 1, 0);
			}
			axis.normalize();
			turn = (0.12F + random.nextFloat() * 0.35F) * (random.nextBoolean() ? 1 : -1);
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			oSpin.set(spin);
			if (age++ >= lifetime) {
				remove();
				return;
			}
			if (age < hold) {
				return;
			}
			if (age == hold) {
				xd = vx;
				yd = vy;
				zd = vz;
			}
			yd -= 0.035;
			move(xd, yd, zd);
			xd *= 0.97;
			yd *= 0.98;
			zd *= 0.97;
			if (onGround) {
				// Skitters, then lies still.
				xd *= 0.55;
				zd *= 0.55;
				if (Math.abs(yd) < 0.02) {
					yd = 0;
				}
				turn *= 0.55F;
			}
			spin.rotateAxis(turn, axis.x, axis.y, axis.z);
		}

		@Override
		public void extract(QuadParticleRenderState state, Camera camera, float partial) {
			float t = age + partial;
			float fade = Math.min(1, (lifetime - t) / 20F);
			if (fade <= 0.01F) {
				return;
			}
			Vec3 cam = camera.position();
			float px = (float) (Mth.lerp(partial, xo, x) - cam.x);
			float py = (float) (Mth.lerp(partial, yo, y) - cam.y);
			float pz = (float) (Mth.lerp(partial, zo, z) - cam.z);
			float distSq = px * px + py * py + pz * pz;
			if (distSq < 0.16F) {
				// Right against the eye: left out rather than filling the view.
				return;
			}
			Quaternionf q = new Quaternionf(oSpin).slerp(spin, partial);
			Vector3f normal = q.transform(new Vector3f(0, 0, 1));
			Vector3f view = new Vector3f(-px, -py, -pz).normalize();
			if (normal.dot(view) < 0) {
				// Seen from behind: the same piece, mirrored, as the back of real glass is.
				q.rotateY(Mth.PI);
				normal.negate();
			}
			// A glint as it turns to catch the light toward the viewer.
			Vector3f reflected = new Vector3f(LIGHT).reflect(normal).negate();
			float glint = (float) Math.pow(Math.max(0, reflected.dot(view)), 18);
			float alpha = Math.min(1, (0.8F + 0.9F * glint) * fade);
			int rgb = lerp(color, 0xFFFFFF, Math.min(1, 0.45F + glint));
			int argb = (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | rgb;
			state.add(GlowLayers.GLOW, px, py, pz, q.x, q.y, q.z, q.w, size, glass.getU0(), glass.getU1(), glass.getV0(), glass.getV1(), argb,
				LightCoordsUtil.FULL_BRIGHT);
		}

		@Override
		public int getLightCoords(float partial) {
			return LightCoordsUtil.FULL_BRIGHT;
		}

		@Override
		protected Layer getLayer() {
			return GlowLayers.GLOW;
		}

		@Override
		public ParticleRenderType getGroup() {
			return SigilGroup.TYPE;
		}
	}

	/** A curved sliver of the circle's rim: a short arc of its frame line, tumbling as it falls. */
	static class RimShard extends SingleQuadParticle {
		private final Quaternionf spin = new Quaternionf();
		private final Quaternionf oSpin = new Quaternionf();
		private final Vector3f axis;
		private float turn;
		private final float arcRadius;
		private final float span;
		private final float width;
		private final int color;
		private final TextureAtlasSprite band;
		private final TextureAtlasSprite glow;

		/**
		 * @param radial  from the circle's centre out through this sliver
		 * @param tangent along the rim, at this sliver
		 * @param normal  the circle's facing
		 * @param arcRadius the rim's radius, {@code span} the sliver's length round it (radians)
		 */
		RimShard(ClientLevel level, Vec3 at, Vector3f velocity, Vector3f radial, Vector3f tangent, Vector3f normal, float arcRadius, float span,
				float width, int color, RandomSource random) {
			super(level, at.x, at.y, at.z, SpellCircleParticle.particleSprite("sigil_band"));
			this.band = SpellCircleParticle.particleSprite("sigil_band");
			this.glow = SpellCircleParticle.particleSprite("sigil_glow");
			this.arcRadius = arcRadius;
			this.span = span;
			this.width = width;
			this.color = color;
			this.xd = velocity.x;
			this.yd = velocity.y;
			this.zd = velocity.z;
			this.gravity = 0;
			this.friction = 1;
			this.hasPhysics = true;
			this.lifetime = 40 + random.nextInt(30);
			setSize(0.15F, 0.15F);
			// Local x out from the circle's centre, y along the rim, z the circle's facing.
			spin.setFromNormalized(new Matrix3f(radial, tangent, normal));
			oSpin.set(spin);
			axis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
			if (axis.lengthSquared() < 1.0E-4F) {
				axis.set(1, 0, 0);
			}
			axis.normalize();
			turn = (0.15F + random.nextFloat() * 0.3F) * (random.nextBoolean() ? 1 : -1);
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			oSpin.set(spin);
			if (age++ >= lifetime) {
				remove();
				return;
			}
			yd -= 0.035;
			move(xd, yd, zd);
			xd *= 0.97;
			yd *= 0.98;
			zd *= 0.97;
			if (onGround) {
				xd *= 0.55;
				zd *= 0.55;
				if (Math.abs(yd) < 0.02) {
					yd = 0;
				}
				turn *= 0.55F;
			}
			spin.rotateAxis(turn, axis.x, axis.y, axis.z);
		}

		@Override
		public void extract(QuadParticleRenderState state, Camera camera, float partial) {
			float t = age + partial;
			float fade = Math.min(1, (lifetime - t) / 18F);
			if (fade <= 0.01F) {
				return;
			}
			Vec3 cam = camera.position();
			Vector3f centre = new Vector3f((float) (Mth.lerp(partial, xo, x) - cam.x), (float) (Mth.lerp(partial, yo, y) - cam.y),
				(float) (Mth.lerp(partial, zo, z) - cam.z));
			if (centre.lengthSquared() < 0.16F) {
				return;
			}
			Quaternionf q = new Quaternionf(oSpin).slerp(spin, partial);
			int core = (Mth.clamp((int) (fade * 255), 0, 255) << 24) | lerp(color, 0xFFFFFF, 0.55F);
			int halo = (Mth.clamp((int) (fade * 90), 0, 255) << 24) | color;
			int n = 6;
			Vector3f prev = null;
			for (int i = 0; i <= n; i++) {
				float a = -span / 2 + span * i / n;
				// On the arc, with the sliver's middle at its own origin.
				Vector3f p = q.transform(new Vector3f(arcRadius * Mth.cos(a) - arcRadius, arcRadius * Mth.sin(a), 0)).add(centre);
				if (prev != null) {
					segment(state, prev, p, width, core, band);
					segment(state, prev, p, width * 3.2F, halo, glow);
				}
				prev = p;
			}
		}

		/** A piece of line from {@code a} to {@code b} (camera-relative), turned to face the camera as far as it can. */
		private void segment(QuadParticleRenderState state, Vector3f a, Vector3f b, float w, int argb, TextureAtlasSprite sprite) {
			Vector3f d = new Vector3f(b).sub(a);
			float length = d.length();
			if (length < 1.0E-4F) {
				return;
			}
			d.div(length);
			Vector3f mid = new Vector3f(a).add(b).mul(0.5F);
			Vector3f z = new Vector3f(mid).negate();
			z.sub(new Vector3f(d).mul(z.dot(d)));
			if (z.lengthSquared() < 1.0E-6F) {
				return;
			}
			z.normalize();
			Vector3f y = new Vector3f(z).cross(d).normalize();
			Quaternionf q = new Quaternionf().setFromNormalized(new Matrix3f(d, y, z));
			float half = Math.max(length / 2 * 1.1F, w);
			state.add(GlowLayers.GLOW, mid.x, mid.y, mid.z, q.x, q.y, q.z, q.w, half, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
				argb, LightCoordsUtil.FULL_BRIGHT);
		}

		@Override
		public int getLightCoords(float partial) {
			return LightCoordsUtil.FULL_BRIGHT;
		}

		@Override
		protected Layer getLayer() {
			return GlowLayers.GLOW;
		}

		@Override
		public ParticleRenderType getGroup() {
			return SigilGroup.TYPE;
		}
	}
}
