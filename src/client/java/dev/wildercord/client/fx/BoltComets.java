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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Authored effect bodies follow live bolts; uncovered groups retain a comet with a
 * coloured core and a trail that tapers and fades. The fallback follows the smoothed position every
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
		DRAWN.removeIf(id -> level.getEntity(id) == null || level.getEntity(id).isRemoved());
		for (Entity e : level.entitiesForRendering()) {
			if (e instanceof RuneBolt bolt) {
				if (DRAWN.add(bolt.getId())) mc.particleEngine.add(new Comet(level, bolt));
				if (bolt.tickCount % 2 == 0) {
					var quality = bolt.getOwner() == mc.player ? MagicQuality.own : MagicQuality.others;
					int style = bolt.getEntityData().get(RuneBolt.DATA_STYLE);
					float width = 1 + .18F * (style & RuneBolt.STYLE_POWER);
					if ((style & RuneBolt.STYLE_FRUGAL) != 0) width *= .7F;
					if ((style & RuneBolt.STYLE_PIERCE) != 0) width *= .65F;
					for (String id : bolt.getEntityData().get(RuneBolt.DATA_EFFECTS).split(",")) {
                        if(!FrostFlights.supports(id) && !VoidForms.supports(id) && !LifeForms.supports(id))
                            NextSignatureForms.flight(id,bolt.tickCount,width,(style & RuneBolt.STYLE_PIERCE)!=0?1.8:1,
                                bolt.position(),bolt.getDeltaMovement(),quality==MagicQuality.Level.MINIMAL,
                                (option,pos)->level.addParticle(option,pos.x,pos.y,pos.z,0,0,0));
						FireFlights.draw(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
							bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
							(option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
						FrostFlights.draw(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
							bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
							(option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
                        WindForms.flight(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
                            bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
                            (option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
                        LifeFlights.draw(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
                            bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
                            (option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
                        VoidFlights.draw(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
                            bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
                            (option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
                        EarthFlights.draw(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
                            bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
                            (option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
						StormFlights.draw(id, bolt.tickCount, width, (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.8 : 1,
							bolt.position(), bolt.getDeltaMovement(), quality == MagicQuality.Level.MINIMAL,
							(option, pos) -> level.addParticle(option, pos.x, pos.y, pos.z, 0, 0, 0));
					}
				}
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
		private int style;
		private int gone = -1;
		private String effects = "";
		private boolean authored;

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
			style = bolt.getEntityData().get(RuneBolt.DATA_STYLE);
			String current = bolt.getEntityData().get(RuneBolt.DATA_EFFECTS);
			if (!current.equals(effects)) {
				effects = current;
				authored = dev.wildercord.cast.FlightBodies.covers(current);
			}
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

		/** How wide the comet is drawn: fatter for Amplify and Overcharge, thin for Frugal, a needle for Pierce. */
		private float girth() {
			float g = 1 + 0.18F * (style & RuneBolt.STYLE_POWER);
			if ((style & RuneBolt.STYLE_FRUGAL) != 0) {
				g *= 0.7F;
			}
			if ((style & RuneBolt.STYLE_PIERCE) != 0) {
				g *= 0.65F;
			}
			return g;
		}

		/** The white-hot core: a pierce's needle burns hotter along its length. */
		private float core() {
			return (style & RuneBolt.STYLE_PIERCE) != 0 ? 1.2F : girth();
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
			// Authored fire flights replace the generic comet rather than layering another full comet over it.
			if (authored) return;
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
				ribbon(state, prev, q, 0.24F * t * girth(), argb(0.5F * t * fade, color));
				ribbon(state, prev, q, 0.09F * t * core(), argb(0.9F * t * fade, hot(color, 0.7F)));
				prev = q;
				i++;
			}
			if (gone < 0) {
				billboard(state, h, 0.42F * girth(), argb(0.55F * fade, color));
				billboard(state, h, 0.2F * girth(), argb(0.95F * fade, hot(secondary, 0.8F)));
				if ((style & RuneBolt.STYLE_POWER) >= 2) {
					// Overcharged: a second, wider halo.
					billboard(state, h, 0.75F, argb(0.25F * fade, secondary));
				}
				if ((style & RuneBolt.STYLE_HOMING) != 0) {
					// Seeking: a mote circling the head, a corkscrew in flight.
					float a = (age + partial) * 0.9F;
					Vec3 v = bolt.getDeltaMovement();
					Vector3f dir = new Vector3f((float) v.x, (float) v.y, (float) v.z);
					if (dir.lengthSquared() > 1.0E-6F) {
						dir.normalize();
						Vector3f side = new Vector3f(dir).cross(0, 1, 0);
						if (side.lengthSquared() < 1.0E-4F) {
							side.set(1, 0, 0);
						}
						side.normalize();
						Vector3f up = new Vector3f(side).cross(dir).normalize();
						Vector3f p = new Vector3f(h).add(side.mul(Mth.cos(a) * 0.3F)).add(up.mul(Mth.sin(a) * 0.3F));
						billboard(state, p, 0.1F, argb(0.9F * fade, hot(secondary, 0.6F)));
					}
				}
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
			Quaternionf q = new Quaternionf();
			for (int k = 0; k < n; k++) {
				from.lerp(to, (k + 0.5F) / n, mid);
				quad(state, soft, mid, Facing.along(d, mid.x, mid.y, mid.z, q), half, argb);
			}
		}

		private void billboard(QuadParticleRenderState state, Vector3f p, float half, int argb) {
			if ((argb >>> 24) < 3 || Vector3f.lengthSquared(p.x, p.y, p.z) < 1.0E-6F) {
				return;
			}
			quad(state, glow, p, Facing.toward(p.x, p.y, p.z, new Quaternionf()), half, argb);
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
