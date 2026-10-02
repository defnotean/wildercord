package dev.wildercord.client.fx;

import dev.wildercord.aura.AuraFx;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A burst of aura's light ({@link AuraFx.Burst}): a soft flash swelling and gone in a few ticks, a four-pointed glint across it, a
 * sharp impact strokes and sparks flung off that slow and fall as streaks. Ground impact rings are handled as physical scars. An impact is one of these, sized
 * by its weight; a perfect guard another, in the parry's gold.
 *
 * <p>One that would open close to its owner's eyes in their own first-person view is drawn as a whisper of itself: small, faint,
 * moved down and aside to the bottom of the view. Everyone else, and the owner in third
 * person, see it whole.</p>
 */
public class AuraBurst extends SingleQuadParticle implements SigilGroup.Extent {
	private final int color;
	private final float size;
	private final int style;
	/** The plane the ring lies in, by its facing (zero: it faces whoever looks). */
	private final Vec3 facing;
	/** Drawn as a whisper (the owner's own first-person view). */
	private final boolean whisper;
	private final float strength;

	private final int sparks;
	private final float[] sp;
	private final float[] sv;

	private static final int FLASH_TICKS = 5;
	private static final int RING_TICKS = 9;
	private static final int STAR_TICKS = 5;
	private static final int SPARK_TICKS = 14;

	/**
	 * @param whisper  drawn small and low (its owner's own first-person view, close to their eyes)
	 * @param strength how strong it shows (the player's impact setting and reduced flash)
	 */
	public AuraBurst(ClientLevel level, Vec3 at, Vec3 facing, int color, float size, int style, boolean whisper, float strength) {
		super(level, at.x, at.y, at.z, SpellCircleParticle.particleSprite("sigil_glow"));
		this.color = color & 0xFFFFFF;
		this.size = Math.max(0.05F, size);
		this.style = style;
		this.facing = facing == null || facing.lengthSqr() < 1.0E-6 ? null : facing.normalize();
		this.whisper = whisper;
		this.strength = Mth.clamp(strength, 0, 1.5F);
		this.sparks = (style & AuraFx.Burst.SPARKS) != 0 ? (whisper ? 3 : Math.max(3, Math.min(14, Math.round(4 + this.size * 6)))) : 0;
		this.sp = new float[sparks * 3];
		this.sv = new float[sparks * 3];
		for (int i = 0; i < sparks; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double up = random.nextDouble() * 0.7 + 0.05;
			double speed = (0.07 + random.nextDouble() * 0.08) * Math.sqrt(this.size) * (whisper ? 0.4 : 1);
			sv[i * 3] = (float) (Math.cos(a) * speed);
			sv[i * 3 + 1] = (float) (up * speed * 1.2);
			sv[i * 3 + 2] = (float) (Math.sin(a) * speed);
		}
		int life = FLASH_TICKS;
		if ((style & AuraFx.Burst.RING) != 0) {
			life = Math.max(life, RING_TICKS);
		}
		if ((style & AuraFx.Burst.ECHO) != 0) {
			life = Math.max(life, RING_TICKS + 3);
		}
		if (sparks > 0) {
			life = Math.max(life, SPARK_TICKS);
		}
		this.lifetime = life;
		this.gravity = 0;
		this.hasPhysics = false;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		for (int i = 0; i < sparks; i++) {
			sp[i * 3] += sv[i * 3];
			sp[i * 3 + 1] += sv[i * 3 + 1];
			sp[i * 3 + 2] += sv[i * 3 + 2];
			sv[i * 3] *= 0.84F;
			sv[i * 3 + 1] = sv[i * 3 + 1] * 0.84F - 0.01F;
			sv[i * 3 + 2] *= 0.84F;
		}
		if (age++ >= lifetime) {
			remove();
		}
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		Vec3 cam = camera.position();
		float t = age + partial;
		float k = (whisper ? 0.5F : 1.0F) * strength;
		float s = size * (whisper ? 0.32F : 1.0F);
		Vec3 centre = new Vec3(x, y, z);
		Vector3f right = camera.leftVector().negate(new Vector3f());
		Vector3f up = new Vector3f(camera.upVector());
		if (whisper) {
			// Down by the bottom of the view, a little toward the blade hand: never in the middle of it.
			Vector3fc fwd = camera.forwardVector();
			centre = cam.add(fwd.x() * 1.25 + right.x * 0.22 - up.x * 0.6, fwd.y() * 1.25 + right.y * 0.22 - up.y * 0.6,
				fwd.z() * 1.25 + right.z * 0.22 - up.z * 0.6);
		}
		float px = (float) (centre.x - cam.x);
		float py = (float) (centre.y - cam.y);
		float pz = (float) (centre.z - cam.z);
		LightStrokes paint = new LightStrokes(state, GlowLayers.GLOW);
		int hot = LightStrokes.hot(color, 0.6F);
		if ((style & AuraFx.Burst.FLASH) != 0 && t < FLASH_TICKS) {
			float f = t / FLASH_TICKS;
			float swell = Math.min(1, t / 1.2F);
			float fade = (1 - f) * (1 - f);
			paint.glow(px, py, pz, s * (0.55F + 0.45F * swell), LightStrokes.argb(0.75F * fade * k, color));
			paint.glow(px, py, pz, s * 0.45F * (0.6F + 0.4F * swell), LightStrokes.argb(0.9F * fade * k, hot));
		}
		if ((style & AuraFx.Burst.STAR) != 0 && t < STAR_TICKS) {
			float f = t / STAR_TICKS;
			float fade = (1 - f) * (1 - f);
			float reach = s * (0.3F + 0.25F * Math.min(1, t / 1.5F)) * (whisper ? 0.8F : 1);
			int glint = LightStrokes.hot(color, 0.6F);
			// Four points, the level pair longer: a glint caught on an edge, quick and tinted, never a white cross over the view.
			star(paint, px, py, pz, right, reach, Math.max(0.01F, s * 0.03F), LightStrokes.argb(0.85F * fade * k, glint));
			star(paint, px, py, pz, new Vector3f(up), reach * 0.6F, Math.max(0.01F, s * 0.026F), LightStrokes.argb(0.75F * fade * k, glint));
		}
		if (sparks > 0 && t < SPARK_TICKS) {
			float fade = 1 - t / SPARK_TICKS;
			int white = LightStrokes.hot(color, 0.7F);
			for (int i = 0; i < sparks; i++) {
				float ox = Mth.lerp(partial, sp[i * 3] - sv[i * 3], sp[i * 3]) * (whisper ? 0.45F : 1);
				float oy = Mth.lerp(partial, sp[i * 3 + 1] - sv[i * 3 + 1], sp[i * 3 + 1]) * (whisper ? 0.45F : 1);
				float oz = Mth.lerp(partial, sp[i * 3 + 2] - sv[i * 3 + 2], sp[i * 3 + 2]) * (whisper ? 0.45F : 1);
				float vx = sv[i * 3];
				float vy = sv[i * 3 + 1];
				float vz = sv[i * 3 + 2];
				float speed = Mth.sqrt(vx * vx + vy * vy + vz * vz);
				if (speed < 1.0E-4F) {
					continue;
				}
				// A streak along its flight, longer the faster it goes.
				float len = Math.min(0.35F, speed * 2.4F + 0.04F) * (whisper ? 0.5F : 1);
				float hx = px + ox;
				float hy = py + oy;
				float hz = pz + oz;
				paint.segment(true, hx - vx / speed * len, hy - vy / speed * len, hz - vz / speed * len, hx, hy, hz, whisper ? 0.012F : 0.03F,
					LightStrokes.argb(0.95F * fade * k, white));
			}
		}
	}

	/** Two points of a glint, out either way from the centre along {@code axis}, tapering. */
	private static void star(LightStrokes paint, float x, float y, float z, Vector3f axis, float reach, float width, int argb) {
		for (int side = -1; side <= 1; side += 2) {
			float ex = x + axis.x * reach * side;
			float ey = y + axis.y * reach * side;
			float ez = z + axis.z * reach * side;
			float mx = x + axis.x * reach * 0.45F * side;
			float my = y + axis.y * reach * 0.45F * side;
			float mz = z + axis.z * reach * 0.45F * side;
			paint.segment(true, x, y, z, mx, my, mz, width, argb);
			paint.segment(true, mx, my, mz, ex, ey, ez, width * 0.55F, LightStrokes.argb(((argb >>> 24) / 255F) * 0.7F, argb));
		}
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
		// A whisper is drawn by the camera, wherever it was cast: never culled.
		return whisper ? 4096 : size * 1.6 + 1.5;
	}

	/** Whether {@code at} is close enough to the local player's eyes, in their own first-person view, to be drawn as a whisper. */
	public static boolean nearOwnEyes(Minecraft mc, int owner, Vec3 at) {
		if (mc.player == null || owner != mc.player.getId() || mc.getCameraEntity() != mc.player || !mc.options.getCameraType().isFirstPerson()) {
			return false;
		}
		return mc.player.getEyePosition().distanceToSqr(at) < 2.6 * 2.6;
	}
}
