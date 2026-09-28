package dev.wildercord.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft light laid out in a plane: rings, and straight lines between points, drawn in pieces of a
 * soft band so they keep their width, from both sides, breathing slowly. The plane can lie flat or
 * be tilted, its tilt slowly swinging round, with beads of light running along the first ring so
 * the turning shows. Made on the client for the Archive's inlaid floor and the Wellstone's halo;
 * a spawner keeps one alive per place and replaces it as it ends.
 */
public class RingGlow extends SingleQuadParticle implements SigilGroup.Extent {
	private record Ring(float radius, float width, int color, float alpha) {}

	private record Line(float x0, float y0, float x1, float y1, float width, int color, float alpha) {}

	private final List<Ring> rings = new ArrayList<>();
	private final List<Line> lines = new ArrayList<>();
	private final float tilt;
	private final float swing;
	private final float phase;
	private final TextureAtlasSprite soft;
	private final TextureAtlasSprite glow;
	private int beads;
	private float beadSpeed;
	private int beadColor;
	private float strength = 1;
	private float reach = 1;

	// While drawing.
	private QuadParticleRenderState state;
	private final Quaternionf plane = new Quaternionf();
	private final Quaternionf turn = new Quaternionf();
	private final Vector3f at = new Vector3f();
	private float cx;
	private float cy;
	private float cz;

	/**
	 * @param tilt     how far the plane leans from flat, in radians
	 * @param swing    how fast the lean swings round, in radians a tick
	 * @param lifetime ticks (it fades in and out over the first and last 20)
	 */
	public RingGlow(ClientLevel level, Vec3 centre, float tilt, float swing, int lifetime) {
		super(level, centre.x, centre.y, centre.z, SpellCircleParticle.particleSprite("sigil_beam"));
		this.tilt = tilt;
		this.swing = swing;
		this.phase = random.nextFloat() * Mth.TWO_PI;
		this.soft = SpellCircleParticle.particleSprite("sigil_beam");
		this.glow = SpellCircleParticle.particleSprite("sigil_glow");
		this.lifetime = Math.max(42, lifetime);
		this.gravity = 0;
		this.hasPhysics = false;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
	}

	public RingGlow ring(float radius, float width, int color, float alpha) {
		rings.add(new Ring(radius, width, color & 0xFFFFFF, alpha));
		reach = Math.max(reach, radius + width + 0.5F);
		return this;
	}

	/** A straight line in the plane, from ({@code x0}, {@code y0}) to ({@code x1}, {@code y1}). */
	public RingGlow line(float x0, float y0, float x1, float y1, float width, int color, float alpha) {
		lines.add(new Line(x0, y0, x1, y1, width, color & 0xFFFFFF, alpha));
		reach = Math.max(reach, (float) Math.max(Math.hypot(x0, y0), Math.hypot(x1, y1)) + width + 0.5F);
		return this;
	}

	/** {@code count} beads of light running round the first ring at {@code speed} radians a tick. */
	public RingGlow beads(int count, float speed, int color) {
		this.beads = count;
		this.beadSpeed = speed;
		this.beadColor = color & 0xFFFFFF;
		return this;
	}

	/** Brightens or dims the whole glow (1 is as built); may be changed while it's alive. */
	public void setStrength(float strength) {
		this.strength = strength;
	}

	@Override
	public void tick() {
		if (age++ >= lifetime) {
			remove();
		}
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | rgb;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float t = age + partial;
		float fade = Math.min(Mth.clamp(t / 20F, 0, 1), Mth.clamp((lifetime - t) / 20F, 0, 1));
		float breath = (0.78F + 0.22F * Mth.sin(t * 0.045F + phase)) * fade * strength;
		if (breath < 0.01F) {
			return;
		}
		this.state = state;
		Vec3 cam = camera.position();
		cx = (float) (x - cam.x);
		cy = (float) (y - cam.y);
		cz = (float) (z - cam.z);
		// Turning by the world's clock, so a glow that takes over from another carries on from where it was.
		// A still one keeps its lines where they were put: (u, v) in the plane is (x, z) in the world.
		double clock = level.getGameTime() + (double) partial;
		plane.rotationY((float) (clock * swing % Mth.TWO_PI)).rotateX(tilt).rotateX(Mth.HALF_PI);
		for (Ring ring : rings) {
			circle(ring.radius, ring.width, argb(ring.alpha * breath, ring.color));
		}
		for (Line line : lines) {
			segment(line.x0, line.y0, line.x1, line.y1, line.width, argb(line.alpha * breath, line.color));
		}
		if (beads > 0 && !rings.isEmpty()) {
			Ring first = rings.getFirst();
			for (int i = 0; i < beads; i++) {
				float a = (float) (clock * beadSpeed % Mth.TWO_PI) + Mth.TWO_PI * i / beads;
				plane.transform(at.set(Mth.cos(a) * first.radius, Mth.sin(a) * first.radius, 0));
				billboard(cx + at.x, cy + at.y, cz + at.z, first.width * 2.2F, argb(Math.min(1, first.alpha * 1.4F) * breath, beadColor));
			}
		}
		this.state = null;
	}

	private void circle(float r, float w, int argb) {
		if ((argb >>> 24) < 2) {
			return;
		}
		float pieceLength = Math.max(0.02F, w / 0.62F);
		int n = Math.max(12, (int) Math.ceil(Mth.TWO_PI * r / pieceLength));
		float half = Mth.PI * r / n * 1.04F;
		for (int i = 0; i < n; i++) {
			float a = Mth.TWO_PI * i / n;
			piece(Mth.cos(a) * r, Mth.sin(a) * r, a + Mth.HALF_PI, half, argb);
		}
	}

	private void segment(float x0, float y0, float x1, float y1, float w, int argb) {
		float length = (float) Math.hypot(x1 - x0, y1 - y0);
		if ((argb >>> 24) < 2 || length < 1.0E-3F) {
			return;
		}
		float pieceLength = Math.max(0.02F, w / 0.62F);
		int n = Math.max(1, (int) Math.ceil(length / pieceLength));
		float half = length / n / 2 * 1.04F;
		float rot = (float) Math.atan2(y1 - y0, x1 - x0);
		for (int i = 0; i < n; i++) {
			float f = (i + 0.5F) / n;
			piece(Mth.lerp(f, x0, x1), Mth.lerp(f, y0, y1), rot, half, argb);
		}
	}

	/** One square piece of the soft band at (u, v) in the plane, its length turned {@code rot}, from both sides. */
	private void piece(float u, float v, float rot, float half, int argb) {
		plane.transform(at.set(u, v, 0));
		turn.set(plane).rotateZ(rot);
		quad(soft, cx + at.x, cy + at.y, cz + at.z, turn, half, argb);
		turn.rotateY(Mth.PI);
		quad(soft, cx + at.x, cy + at.y, cz + at.z, turn, half, argb);
	}

	private void billboard(float px, float py, float pz, float half, int argb) {
		Vector3f zAxis = new Vector3f(-px, -py, -pz);
		if ((argb >>> 24) < 2 || zAxis.lengthSquared() < 1.0E-6F) {
			return;
		}
		zAxis.normalize();
		Vector3f up = Math.abs(zAxis.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
		Vector3f xAxis = new Vector3f(up).cross(zAxis).normalize();
		Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();
		quad(glow, px, py, pz, new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, zAxis)), half, argb);
	}

	private void quad(TextureAtlasSprite sprite, float x, float y, float z, Quaternionf q, float half, int argb) {
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
		return y;
	}

	@Override
	public double centreZ() {
		return z;
	}

	@Override
	public double reach() {
		return reach;
	}
}
