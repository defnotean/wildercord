package dev.wildercord.client.fx;

import dev.wildercord.cast.RuneBolt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Every bolt in flight, drawn by the client as a comet: a white-hot core in a coloured glow, and a
 * trail of light behind it that tapers and fades. It follows the bolt's smoothed position every
 * frame, so it glides at any distance instead of stepping from tick to tick.
 */
public final class BoltComets {
	private BoltComets() {}

	/** Bolts that already have their comet, by entity id. */
	private static final Set<Integer> DRAWN = new HashSet<>();
	/** The world those ids belong to: a new one (another dimension, another server) starts afresh. */
	private static ClientLevel seen;

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level != seen) {
			seen = level;
			DRAWN.clear();
		}
		if (level == null) {
			return;
		}
		for (Entity e : level.entitiesForRendering()) {
			if (e instanceof RuneBolt bolt && DRAWN.add(bolt.getId())) {
				mc.particleEngine.add(new Comet(level, bolt));
			}
		}
	}

	/** One bolt's comet. */
	private static final class Comet extends SingleQuadParticle implements SigilGroup.Extent {
		/** How many past positions the trail remembers (one per tick). */
		private static final int TRAIL = 7;

		private final RuneBolt bolt;
		private final Deque<Vec3> trail = new ArrayDeque<>();
		private final TextureAtlasSprite soft;
		private final TextureAtlasSprite glow;
		private int color;
		private int secondary;
		private int gone = -1;

		Comet(ClientLevel level, RuneBolt bolt) {
			super(level, bolt.getX(), bolt.getY(), bolt.getZ(), SpellCircleParticle.particleSprite("sigil_beam"));
			this.bolt = bolt;
			this.soft = SpellCircleParticle.particleSprite("sigil_beam");
			this.glow = SpellCircleParticle.particleSprite("sigil_glow");
			this.lifetime = 20 * 60;
			this.gravity = 0;
			this.hasPhysics = false;
			trail.addFirst(bolt.position());
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			age++;
			color = bolt.getEntityData().get(RuneBolt.DATA_COLOR);
			secondary = bolt.getEntityData().get(RuneBolt.DATA_SECONDARY);
			if (bolt.isRemoved()) {
				// The trail catches up with where the bolt ended, then the comet goes.
				if (gone < 0) {
					gone = TRAIL;
				}
				if (!trail.isEmpty()) {
					trail.removeLast();
				}
				if (--gone <= 0 || trail.isEmpty()) {
					DRAWN.remove(bolt.getId());
					remove();
				}
				return;
			}
			Vec3 at = bolt.position();
			x = at.x;
			y = at.y;
			z = at.z;
			trail.addFirst(at);
			while (trail.size() > TRAIL) {
				trail.removeLast();
			}
		}

		private static int argb(float alpha, int rgb) {
			return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
		}

		private static int hot(int rgb, float t) {
			int r = (rgb >> 16) & 0xFF;
			int g = (rgb >> 8) & 0xFF;
			int b = rgb & 0xFF;
			return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
		}

		@Override
		public void extract(QuadParticleRenderState state, Camera camera, float partial) {
			Vec3 cam = camera.position();
			Vec3 head = bolt.isRemoved() ? (trail.isEmpty() ? bolt.position() : trail.peekFirst()) : bolt.getPosition(partial);
			float fade = gone >= 0 ? Math.max(0, gone / (float) TRAIL) : Mth.clamp((age + partial) / 2F, 0, 1);
			Vector3f h = new Vector3f((float) (head.x - cam.x), (float) (head.y - cam.y), (float) (head.z - cam.z));
			// The trail: from the head back through the remembered positions, tapering and fading.
			Vector3f prev = h;
			int i = 0;
			Iterator<Vec3> it = trail.iterator();
			if (it.hasNext() && !bolt.isRemoved()) {
				it.next();
			}
			while (it.hasNext()) {
				Vec3 p = it.next();
				Vector3f q = new Vector3f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z));
				float t = 1 - i / (float) TRAIL;
				ribbon(state, prev, q, 0.24F * t, argb(0.5F * t * fade, color));
				ribbon(state, prev, q, 0.09F * t, argb(0.9F * t * fade, hot(color, 0.7F)));
				prev = q;
				i++;
			}
			if (gone < 0) {
				billboard(state, h, 0.42F, argb(0.55F * fade, color));
				billboard(state, h, 0.2F, argb(0.95F * fade, hot(secondary, 0.8F)));
			}
		}

		private void ribbon(QuadParticleRenderState state, Vector3f from, Vector3f to, float w, int argb) {
			Vector3f d = new Vector3f(to).sub(from);
			float length = d.length();
			if ((argb >>> 24) < 3 || length < 1.0E-3F || w <= 0) {
				return;
			}
			d.div(length);
			int n = Math.max(1, (int) Math.ceil(length / Math.max(0.02F, w / 0.62F)));
			float half = length / n / 2;
			Vector3f mid = new Vector3f();
			for (int k = 0; k < n; k++) {
				from.lerp(to, (k + 0.5F) / n, mid);
				Vector3f z = new Vector3f(mid).negate();
				z.sub(new Vector3f(d).mul(z.dot(d)));
				if (z.lengthSquared() < 1.0E-6F) {
					z.set(0, 1, 0);
				}
				z.normalize();
				Vector3f y = new Vector3f(z).cross(d).normalize();
				Quaternionf q = new Quaternionf().setFromNormalized(new Matrix3f(d, y, z));
				quad(state, soft, mid, q, half, argb);
			}
		}

		private void billboard(QuadParticleRenderState state, Vector3f p, float half, int argb) {
			Vector3f z = new Vector3f(p).negate();
			if ((argb >>> 24) < 3 || z.lengthSquared() < 1.0E-6F) {
				return;
			}
			z.normalize();
			Vector3f up = Math.abs(z.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
			Vector3f xAxis = new Vector3f(up).cross(z).normalize();
			Vector3f yAxis = new Vector3f(z).cross(xAxis).normalize();
			quad(state, glow, p, new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, z)), half, argb);
		}

		private void quad(QuadParticleRenderState state, TextureAtlasSprite sprite, Vector3f p, Quaternionf q, float half, int argb) {
			state.add(GlowLayers.GLOW, p.x, p.y, p.z, q.x, q.y, q.z, q.w, half, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb,
				LightCoordsUtil.FULL_BRIGHT);
		}

		@Override
		protected Layer getLayer() {
			return GlowLayers.GLOW;
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
			// The trail reaches back a few ticks of flight.
			return bolt.getDeltaMovement().length() * TRAIL + 1;
		}
	}
}
