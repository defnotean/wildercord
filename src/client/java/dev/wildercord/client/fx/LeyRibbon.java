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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * A ribbon of ley light: a thin streak of pale violet that runs along a stretch of ley line just
 * above the ground, bright at its head and fading behind, following the line's bends and the lie
 * of the land. It fades in, flows the length of its path and fades away. Made on the client by
 * {@link LeyMotes}; the path is traced there, this only draws it.
 */
public class LeyRibbon extends SingleQuadParticle implements SigilGroup.Extent {
	private static final int CORE = 0xE4DCFF;
	private static final int HALO = 0x9C84F0;

	/** The path, relative to the particle's position, and how far along it each point is. */
	private final float[] px;
	private final float[] py;
	private final float[] pz;
	private final float[] along;
	private final float length;
	private final float tail;
	private final float width;
	private final float peak;
	private final double cx;
	private final double cy;
	private final double cz;
	private final double reach;
	private final TextureAtlasSprite soft;

	public LeyRibbon(ClientLevel level, List<Vec3> path, float width, float peak, float speed) {
		super(level, path.getFirst().x, path.getFirst().y, path.getFirst().z, SpellCircleParticle.particleSprite("sigil_beam"));
		int n = path.size();
		this.px = new float[n];
		this.py = new float[n];
		this.pz = new float[n];
		this.along = new float[n];
		Vec3 origin = path.getFirst();
		double minX = origin.x, minY = origin.y, minZ = origin.z, maxX = minX, maxY = minY, maxZ = minZ;
		float total = 0;
		for (int i = 0; i < n; i++) {
			Vec3 p = path.get(i);
			px[i] = (float) (p.x - origin.x);
			py[i] = (float) (p.y - origin.y);
			pz[i] = (float) (p.z - origin.z);
			if (i > 0) {
				total += (float) p.distanceTo(path.get(i - 1));
			}
			along[i] = total;
			minX = Math.min(minX, p.x);
			minY = Math.min(minY, p.y);
			minZ = Math.min(minZ, p.z);
			maxX = Math.max(maxX, p.x);
			maxY = Math.max(maxY, p.y);
			maxZ = Math.max(maxZ, p.z);
		}
		this.length = total;
		this.tail = Math.min(2.4F, Math.max(0.8F, total * 0.55F));
		this.width = width;
		this.peak = peak;
		this.cx = (minX + maxX) / 2;
		this.cy = (minY + maxY) / 2;
		this.cz = (minZ + maxZ) / 2;
		this.reach = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) / 2 + 0.5;
		this.soft = SpellCircleParticle.particleSprite("sigil_beam");
		this.lifetime = Math.max(10, (int) Math.ceil((total + tail) / Math.max(0.01F, speed)));
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
		if (age++ >= lifetime) {
			remove();
		}
	}

	/** The point {@code s} blocks along the path (relative to the particle), into {@code out}. */
	private Vector3f at(float s, Vector3f out) {
		int i = 1;
		while (i < along.length - 1 && along[i] < s) {
			i++;
		}
		float span = along[i] - along[i - 1];
		float f = span <= 0 ? 0 : Mth.clamp((s - along[i - 1]) / span, 0, 1);
		return out.set(Mth.lerp(f, px[i - 1], px[i]), Mth.lerp(f, py[i - 1], py[i]), Mth.lerp(f, pz[i - 1], pz[i]));
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | rgb;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float t = age + partial;
		float f = t / lifetime;
		float fade = Math.min(Mth.clamp(f / 0.15F, 0, 1), Mth.clamp((1 - f) / 0.25F, 0, 1));
		if (fade <= 0.01F) {
			return;
		}
		Vec3 cam = camera.position();
		Vector3f base = new Vector3f((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z));
		// The head runs from the start to past the end, so the streak slides in and out along the line.
		float head = f * (length + tail);
		float from = Math.max(0, head - tail);
		float to = Math.min(length, head);
		if (to - from < 0.05F) {
			return;
		}
		stroke(state, base, from, to, head, width * 2.8F, argb(0.3F * peak * fade, HALO), 0.5F);
		stroke(state, base, from, to, head, width, argb(0.75F * peak * fade, CORE), 1.0F);
	}

	/**
	 * Lays the ribbon from {@code from} to {@code to} blocks along the path in pieces {@code w} wide,
	 * each turned about the path to face the camera, brightest toward the head.
	 */
	private void stroke(QuadParticleRenderState state, Vector3f base, float from, float to, float head, float w, int argb, float taper) {
		float alpha = (argb >>> 24) / 255F;
		int rgb = argb & 0xFFFFFF;
		if (alpha < 0.01F) {
			return;
		}
		float pieceLength = Math.max(0.03F, w / 0.62F);
		int n = Math.max(1, (int) Math.ceil((to - from) / pieceLength));
		float step = (to - from) / n;
		Vector3f a = new Vector3f();
		Vector3f b = new Vector3f();
		Vector3f mid = new Vector3f();
		Vector3f d = new Vector3f();
		Quaternionf q = new Quaternionf();
		for (int i = 0; i < n; i++) {
			float s0 = from + step * i;
			float s1 = s0 + step;
			at(s0, a).add(base);
			at(s1, b).add(base);
			d.set(b).sub(a);
			float len = d.length();
			if (len < 1.0E-4F) {
				continue;
			}
			d.div(len);
			a.lerp(b, 0.5F, mid);
			// 0 at the tail's end, 1 at the head; the very tip softens off.
			float u = Mth.clamp(1 - (head - (s0 + s1) / 2) / tail, 0, 1);
			float bright = (float) Math.pow(u, 1.3F) * Math.min(1, (1 - u) * 8 + 0.35F);
			float pieceAlpha = alpha * (1 - taper + taper * bright);
			if (pieceAlpha < 0.01F) {
				continue;
			}
			Facing.along(d, mid.x, mid.y, mid.z, q);
			state.add(getLayer(), mid.x, mid.y, mid.z, q.x, q.y, q.z, q.w, len / 2 * 1.04F, soft.getU0(), soft.getU1(), soft.getV0(), soft.getV1(),
				argb(pieceAlpha, rgb), LightCoordsUtil.FULL_BRIGHT);
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

	@Override
	public double centreX() {
		return cx;
	}

	@Override
	public double centreY() {
		return cy;
	}

	@Override
	public double centreZ() {
		return cz;
	}

	@Override
	public double reach() {
		return reach;
	}
}
