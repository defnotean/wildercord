package dev.wildercord.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A wisp of aura: a soft light rising off a swordsman's shoulders or blade, swaying as it goes like smoke in still air, a tapering
 * tail of light behind it along the way it came; it swells in, thins and is gone. Flow's mark (wisps rising from the shoulders
 * and blade), and from Edge up part of the body's aura in a fight. Drawn with the other lights ({@link SigilGroup}).
 */
public class AuraWisp extends SingleQuadParticle implements SigilGroup.Extent {
	private static final int TAIL = 10;

	private final int color;
	private final float width;
	private final float peak;
	private final double rise;
	private final double sway;
	private final float phase;
	private final double driftX;
	private final double driftZ;
	/** The level line it sways along. */
	private final double swayX;
	private final double swayZ;
	/** Where it has been, a tick at a time, newest first. */
	private final double[] trail = new double[TAIL * 3];
	private int kept;

	/**
	 * @param rise  blocks a tick it climbs
	 * @param sway  how far it sways from side to side
	 * @param drift which way the air carries it (blocks a tick, level)
	 */
	public AuraWisp(ClientLevel level, Vec3 at, int color, float width, float alpha, int lifetime, double rise, double sway, Vec3 drift) {
		super(level, at.x, at.y, at.z, SpellCircleParticle.particleSprite("sigil_beam"));
		this.color = color & 0xFFFFFF;
		this.width = width;
		this.peak = alpha;
		this.rise = rise;
		this.sway = sway;
		this.phase = random.nextFloat() * Mth.TWO_PI;
		this.driftX = drift.x;
		this.driftZ = drift.z;
		double a = random.nextDouble() * Math.PI * 2;
		this.swayX = Math.cos(a);
		this.swayZ = Math.sin(a);
		this.lifetime = Math.max(4, lifetime);
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
		System.arraycopy(trail, 0, trail, 3, trail.length - 3);
		trail[0] = x;
		trail[1] = y;
		trail[2] = z;
		kept = Math.min(TAIL, kept + 1);
		// Climbing, swaying across the way it drifts, the sway growing as it rises.
		double s = Math.cos(age * 0.33 + phase) * sway * (0.4 + age / (double) lifetime);
		x += driftX + swayX * s;
		y += rise * (1 - 0.5 * age / (double) lifetime);
		z += driftZ + swayZ * s;
		if (age++ >= lifetime) {
			remove();
		}
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float t = (age + partial) / lifetime;
		float alpha = peak * Math.min(1, t / 0.18F) * Math.min(1, (1 - t) / 0.4F);
		if (alpha <= 0.01F || kept == 0) {
			return;
		}
		Vec3 cam = camera.position();
		LightStrokes paint = new LightStrokes(state, GlowLayers.GLOW);
		// By day under the open sky, a faint rim of shade under it, so a pale wisp still reads against the sky.
		LightStrokes shade = level.isBrightOutside() && level.canSeeSky(net.minecraft.core.BlockPos.containing(x, y, z))
			? new LightStrokes(state, GlowLayers.DARK) : null;
		float hx = (float) (Mth.lerp(partial, xo, x) - cam.x);
		float hy = (float) (Mth.lerp(partial, yo, y) - cam.y);
		float hz = (float) (Mth.lerp(partial, zo, z) - cam.z);
		int hot = LightStrokes.hot(color, 0.55F);
		float px = hx;
		float py = hy;
		float pz = hz;
		for (int i = 0; i < kept; i++) {
			float nx = (float) (trail[i * 3] - cam.x);
			float ny = (float) (trail[i * 3 + 1] - cam.y);
			float nz = (float) (trail[i * 3 + 2] - cam.z);
			float f = 1 - (i + 0.5F) / TAIL;
			if (shade != null) {
				shade.segment(false, nx, ny, nz, px, py, pz, width * 1.35F * (0.25F + 0.75F * f), LightStrokes.argb(alpha * f * 0.5F,
					GlowLayers.darkColor(color)));
			}
			paint.segment(false, nx, ny, nz, px, py, pz, width * (0.35F + 0.65F * f), LightStrokes.argb(alpha * (0.2F + 0.8F * f) * 0.8F, color));
			paint.segment(true, nx, ny, nz, px, py, pz, width * 0.3F * (0.3F + 0.7F * f), LightStrokes.argb(alpha * f, hot));
			px = nx;
			py = ny;
			pz = nz;
		}
		paint.glow(hx, hy, hz, width * 1.3F, LightStrokes.argb(alpha * 0.55F, hot));
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
		return y - 0.3;
	}

	@Override
	public double centreZ() {
		return z;
	}

	@Override
	public double reach() {
		return 1.2;
	}
}
