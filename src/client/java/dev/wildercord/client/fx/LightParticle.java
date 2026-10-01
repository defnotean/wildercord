package dev.wildercord.client.fx;

import dev.wildercord.content.LightOption;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Shaped light (see {@link LightOption}): shockwave rings, beams, crescent slashes, orbs and
 * crackling lightning arcs, each a
 * soft coloured halo under a bright, nearly white core, at full brightness. Lines are laid down as
 * short pieces of a band texture, so they keep their width at any size; a beam's pieces turn to
 * face the viewer.
 */
public class LightParticle extends SingleQuadParticle implements SigilGroup.Extent {
	private final int kind;
	private final boolean ownOutbound;
	private final int color;
	/** Drawn as darkness (void): its halo takes light away, under a thin glowing rim. */
	private final boolean dark;
	/** The layer the next pieces go on: glow, or darkness for a dark light's halo. */
	private Layer drawing = GlowLayers.GLOW;
	private final float a;
	private final float b;
	private final float c;
	private final float width;
	private final float roll;
	private final Quaternionf plane;
	private final TextureAtlasSprite line;
	private final TextureAtlasSprite soft;
	private final TextureAtlasSprite glow;
	private float spin;
	private float oSpin;
	/** An arc's own dice: each tick of its life draws a fresh path from them. */
	private final long seed;
	/** An arc's forks, whether it keeps flat, and how far it jags (0 for the usual). */
	private final int forks;
	private final boolean flat;
	private final float jag;

	// While drawing.
	private QuadParticleRenderState state;
	private final Vector3f at = new Vector3f();
	private final Quaternionf turn = new Quaternionf();
	private float cx;
	private float cy;
	private float cz;

	protected LightParticle(ClientLevel level, double x, double y, double z, LightOption option) {
		super(level, x, y, z, SpellCircleParticle.particleSprite("sigil_beam"));
		this.kind = option.kind();
		var local = net.minecraft.client.Minecraft.getInstance().player;
		Vec3 delta = new Vec3(option.a(), option.b(), option.c());
		ownOutbound = option.kind() == LightOption.RAY && local != null
			&& new Vec3(x,y,z).distanceToSqr(local.getEyePosition()) < 2.25
			&& delta.normalize().dot(local.getLookAngle()) > .9;
		this.color = option.color() & 0xFFFFFF;
		this.dark = (option.color() & GlowLayers.DARK_FLAG) != 0;
		this.a = option.a();
		this.b = option.b();
		this.c = option.c();
		this.width = option.width();
		this.roll = option.roll();
		this.forks = kind == LightOption.ARC ? Math.max(0, Math.min(6, Math.round(option.yaw()))) : 0;
		this.flat = kind == LightOption.ARC && option.pitch() >= 0.5F;
		this.jag = kind == LightOption.ARC ? option.roll() : 0;
		this.plane = new Quaternionf().rotationYXZ((float) Math.toRadians(-option.yaw()), (float) Math.toRadians(option.pitch()), 0);
		this.line = SpellCircleParticle.particleSprite("sigil_band");
		this.soft = SpellCircleParticle.particleSprite("sigil_beam");
		this.glow = SpellCircleParticle.particleSprite("sigil_glow");
		this.lifetime = Math.max(2, option.lifetime());
		// From the world's clock, so an orb redrawn every tick (a moving one) turns smoothly instead of jumping.
		this.spin = (float) (level.getGameTime() * 0.15 % Mth.TWO_PI);
		this.oSpin = spin;
		this.seed = random.nextLong();
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
		oSpin = spin;
		spin += 0.15F;
		if (spin > Mth.TWO_PI) {
			// Wrapped (both, so the frame between still turns smoothly): a float this small stays exact.
			spin -= Mth.TWO_PI;
			oSpin -= Mth.TWO_PI;
		}
		if (age++ >= lifetime) {
			remove();
		}
	}

	/** A halo's colour: the light itself, or for a dark light what to take away (and it goes on the dark layer). */
	private int halo(float alpha, int rgb) {
		drawing = dark ? GlowLayers.DARK : GlowLayers.GLOW;
		return argb(dark ? Math.min(1, alpha * 1.6F) : alpha, dark ? GlowLayers.darkColor(rgb) : rgb);
	}

	/** A core's colour: always light (for a dark light, a thinner, dimmer rim). */
	private int core(float alpha, int rgb) {
		drawing = GlowLayers.GLOW;
		return argb(dark ? alpha * 0.55F : alpha, rgb);
	}

	private static int argb(float alpha, int rgb) {
		if (MagicQuality.reducedFlash) alpha *= 0.45F;
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	/** {@code rgb} moved {@code t} of the way to white: the hot core of a light. */
	private static int hot(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int bl = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (bl + Math.round((255 - bl) * t));
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		this.state = state;
		Vec3 cam = camera.position();
		cx = (float) (Mth.lerp(partial, xo, x) - cam.x);
		cy = (float) (Mth.lerp(partial, yo, y) - cam.y);
		cz = (float) (Mth.lerp(partial, zo, z) - cam.z);
		float t = age + partial;
		float f = Mth.clamp(t / lifetime, 0, 1);
		switch (kind) {
			case LightOption.RING -> ring(f);
			case LightOption.RAY -> ray(t, f);
			case LightOption.SLASH -> slash(t);
			case LightOption.ORB -> orb(camera, t, f, partial);
			case LightOption.ARC -> arc(t, f);
			default -> { }
		}
		this.state = null;
	}

	/** A shockwave: out fast, easing off, thinning and fading as it goes. */
	private void ring(float f) {
		float ease = 1 - (1 - f) * (1 - f) * (1 - f);
		float r = a + (b - a) * ease;
		float fade = (float) Math.pow(1 - f, 1.3);
		float w = width * (1 - 0.6F * f);
		circle(r, w * 3.2F, soft, halo(0.55F * fade, color));
		circle(r, w, line, core(fade, hot(color, 0.6F)));
	}

	/** A beam: shoots out in two ticks, holds, then thins away. */
	private void ray(float t, float f) {
		float reach = Mth.clamp(t / 2F, 0, 1);
		float fade = f < 0.3F ? 1 : 1 - (f - 0.3F) / 0.7F;
		float w = width * (0.35F + 0.65F * fade);
		boolean firstPerson = ownOutbound && net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson();
		if (firstPerson) w = Math.min(w, .045F);
		Vector3f from = new Vector3f(cx, cy, cz);
		Vector3f to = new Vector3f(cx + a * reach, cy + b * reach, cz + c * reach);
		if (firstPerson) {
			Vector3f direction = new Vector3f(a,b,c);
			float length = direction.length();
			if (length > .001F) {
				float trim = Math.min(2F, length * .8F);
				if (length * reach <= trim) return;
				from.add(direction.normalize().mul(trim));
			}
		}
		ribbon(from, to, w * 2.8F, halo(0.5F * fade, color));
		ribbon(from, to, w, core(fade, hot(color, 0.75F)));
		if (!firstPerson) billboard(from, w * 2.2F, halo(0.8F * fade, hot(color, 0.3F)));
		billboard(to, w * 2.6F * reach, halo(0.8F * fade * reach, hot(color, 0.3F)));
	}

	/** A crescent: sweeps across, brightest at its leading edge, then widens and fades. */
	private void slash(float t) {
		float sweepTicks = Math.max(1, c);
		float sweep = Mth.clamp(t / sweepTicks, 0, 1);
		float after = Mth.clamp((t - sweepTicks) / Math.max(1, lifetime - sweepTicks), 0, 1);
		float r = a * (1 + 0.14F * after);
		float fade = 1 - after;
		float start = roll - b / 2;
		float head = b * sweep;
		// Walk the arc in steps as long as the blade is thick there: fine at the tips, broad in the middle.
		float s = 0;
		while (s < head) {
			float along = s / b;
			float thick = Math.max(0.012F, width * (float) Math.pow(Math.sin(Math.PI * along), 0.6));
			float lead = sweep < 1 ? 0.25F + 0.75F * (float) Math.pow(s / Math.max(head, 1.0E-3F), 1.5) : 1;
			float step = Math.max(thick * 0.9F, 0.015F) / r;
			float ang = start + s + step / 2;
			float u = Mth.cos(ang) * r;
			float v = Mth.sin(ang) * r;
			inPlane(soft, u, v, ang + Mth.HALF_PI, step * r / 2 * 2.4F, halo(0.5F * fade * lead, color));
			inPlane(soft, u, v, ang + Mth.HALF_PI, step * r / 2 * 0.95F, core(fade * lead, hot(color, 0.7F)));
			s += step;
		}
	}

	/** An orb: a glowing core, and three rings turning around it. */
	private void orb(Camera camera, float t, float f, float partial) {
		// A moving orb is redrawn every tick with a short life: no fade for those.
		float fade = lifetime <= 4 ? 1 : Math.min(Mth.clamp(t / 3F, 0, 1), Mth.clamp((1 - f) / 0.3F, 0, 1));
		Vector3f centre = new Vector3f(cx, cy, cz);
		billboard(centre, a * 1.9F, halo(0.55F * fade, color));
		if (dark) {
			// A black heart: the centre darkest of all.
			billboard(centre, a * 0.9F, halo(0.9F * fade, color));
		} else {
			billboard(centre, a * 0.9F, core(0.9F * fade, hot(color, 0.7F)));
		}
		float sp = Mth.lerp(partial, oSpin, spin);
		for (int i = 0; i < 3; i++) {
			Quaternionf q = new Quaternionf().rotationYXZ(sp * (0.6F + 0.35F * i) + i * 2.1F, 1.1F * i + sp * 0.4F, 0);
			Quaternionf saved = new Quaternionf(plane);
			plane.set(q);
			circle(a * 1.2F, Math.max(0.012F, width), line, core(0.9F * fade, hot(color, 0.45F)));
			plane.set(saved);
		}
	}

	/**
	 * A lightning arc: a jagged run from here to its end with forks branching off, a fresh path every
	 * tick (so it crackles), flickering, then guttering out. A flat one jags only sideways, so it
	 * skitters over a surface instead of dipping into it.
	 */
	private void arc(float t, float f) {
		int step = (int) t;
		long dice = seed + step * 0x9E3779B97F4A7C15L;
		float flicker = 0.55F + 0.45F * unit(dice, 0);
		float fade = f < 0.35F ? 1 : 1 - (f - 0.35F) / 0.65F;
		float alpha = fade * flicker;
		if (alpha < 0.02F) {
			return;
		}
		Vector3f from = new Vector3f(cx, cy, cz);
		Vector3f d = new Vector3f(a, b, c);
		float length = d.length();
		if (length < 0.05F) {
			return;
		}
		Vector3f dir = new Vector3f(d).div(length);
		Vector3f side = new Vector3f(dir).cross(0, 1, 0);
		if (side.lengthSquared() < 1.0E-4F) {
			side.set(1, 0, 0);
		}
		side.normalize();
		Vector3f lift = flat ? new Vector3f(0, 1, 0) : new Vector3f(dir).cross(side).normalize();
		float jag = Math.min(0.45F, 0.08F + length * 0.07F) * (this.jag > 0 ? this.jag : 1);
		int segments = Mth.clamp(Math.round(length / 0.45F), 3, 14);
		Vector3f[] points = new Vector3f[segments + 1];
		points[0] = from;
		int n = 1;
		for (int i = 1; i <= segments; i++) {
			Vector3f p = new Vector3f(from).add(new Vector3f(d).mul(i / (float) segments));
			if (i < segments) {
				p.add(new Vector3f(side).mul((unit(dice, n++) * 2 - 1) * jag));
				p.add(new Vector3f(lift).mul(flat ? unit(dice, n++) * 0.04F : (unit(dice, n++) * 2 - 1) * jag));
			}
			points[i] = p;
		}
		float w = width * (0.6F + 0.4F * fade);
		for (int i = 1; i <= segments; i++) {
			ribbon(new Vector3f(points[i - 1]), new Vector3f(points[i]), w * 2.8F, halo(0.5F * alpha, color));
			ribbon(new Vector3f(points[i - 1]), new Vector3f(points[i]), w, core(alpha, hot(color, 0.75F)));
		}
		for (int k = 0; k < forks && segments > 2; k++) {
			Vector3f start = points[1 + (int) (unit(dice, n++) * (segments - 2))];
			float sign = unit(dice, n++) < 0.5F ? -1 : 1;
			Vector3f way = new Vector3f(dir).mul(0.55F).add(new Vector3f(side).mul(sign * (0.6F + 0.4F * unit(dice, n++))));
			if (!flat) {
				way.add(new Vector3f(lift).mul(unit(dice, n++) - 0.5F));
			}
			way.normalize();
			float reach = length * (0.15F + 0.2F * unit(dice, n++));
			Vector3f bend = new Vector3f(start).add(new Vector3f(way).mul(reach * 0.5F))
				.add(new Vector3f(side).mul((unit(dice, n++) - 0.5F) * jag));
			Vector3f end = new Vector3f(start).add(new Vector3f(way).mul(reach));
			ribbon(new Vector3f(start), new Vector3f(bend), w * 1.6F, halo(0.35F * alpha, color));
			ribbon(new Vector3f(start), new Vector3f(bend), w * 0.6F, core(alpha * 0.9F, hot(color, 0.6F)));
			ribbon(new Vector3f(bend), new Vector3f(end), w * 1.2F, halo(0.3F * alpha, color));
			ribbon(new Vector3f(bend), new Vector3f(end), w * 0.45F, core(alpha * 0.8F, hot(color, 0.6F)));
		}
		// A spark where it strikes.
		billboard(points[segments], w * 3.5F, halo(0.75F * alpha, hot(color, 0.4F)));
	}

	/** A number from 0 to 1, the same every time for the same dice and index. */
	private static float unit(long dice, int index) {
		long z = dice + index * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		z ^= z >>> 31;
		return (z >>> 40) / (float) (1L << 24);
	}

	/** A ring in this light's plane, of line {@code w}, made of pieces of {@code sprite}. */
	private void circle(float r, float w, TextureAtlasSprite sprite, int argb) {
		if ((argb >>> 24) < 3 || r <= 0) {
			return;
		}
		float visible = sprite == soft ? 0.62F : 0.31F;
		float pieceLength = Math.max(0.01F, w / visible);
		int n = Math.max(16, (int) Math.ceil(Mth.TWO_PI * r / pieceLength));
		float half = Mth.PI * r / n;
		for (int i = 0; i < n; i++) {
			float ang = Mth.TWO_PI * i / n;
			inPlane(sprite, Mth.cos(ang) * r, Mth.sin(ang) * r, ang + Mth.HALF_PI, half * 1.02F, argb);
		}
	}

	/** One square piece in this light's plane at (u, v), turned {@code rot}, drawn from both sides. */
	private void inPlane(TextureAtlasSprite sprite, float u, float v, float rot, float half, int argb) {
		if ((argb >>> 24) < 3 || half <= 0) {
			return;
		}
		plane.transform(at.set(u, v, 0));
		turn.set(plane).rotateZ(rot);
		quad(sprite, cx + at.x, cy + at.y, cz + at.z, turn, half, argb);
		Facing.flip(turn);
		quad(sprite, cx + at.x, cy + at.y, cz + at.z, turn, half, argb);
	}

	/** A beam from {@code from} to {@code to} (camera-relative), {@code w} wide, its pieces turned to face the camera. */
	private void ribbon(Vector3f from, Vector3f to, float w, int argb) {
		Vector3f d = new Vector3f(to).sub(from);
		float length = d.length();
		if ((argb >>> 24) < 3 || length < 1.0E-3F) {
			return;
		}
		d.div(length);
		float pieceLength = Math.max(0.02F, w / 0.62F);
		int n = Math.max(1, (int) Math.ceil(length / pieceLength));
		float half = length / n / 2;
		Vector3f mid = new Vector3f();
		for (int i = 0; i < n; i++) {
			from.lerp(to, (i + 0.5F) / n, mid);
			// Face the camera (at the origin) as squarely as the beam's direction allows.
			quad(soft, mid.x, mid.y, mid.z, Facing.along(d, mid.x, mid.y, mid.z, turn), half, argb);
		}
	}

	/** A soft glow at {@code p} (camera-relative), facing the camera. */
	private void billboard(Vector3f p, float half, int argb) {
		if ((argb >>> 24) < 3 || half <= 0) {
			return;
		}
		if (Vector3f.lengthSquared(p.x, p.y, p.z) < 1.0E-6F) {
			return;
		}
		quad(glow, p.x, p.y, p.z, Facing.toward(p.x, p.y, p.z, turn), half, argb);
	}

	private void quad(TextureAtlasSprite sprite, float x, float y, float z, Quaternionf q, float half, int argb) {
		state.add(drawing, x, y, z, q.x, q.y, q.z, q.w, half, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb,
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

	@Override
	public double centreX() {
		return kind == LightOption.RAY || kind == LightOption.ARC ? x + a / 2 : x;
	}

	@Override
	public double centreY() {
		return kind == LightOption.RAY || kind == LightOption.ARC ? y + b / 2 : y;
	}

	@Override
	public double centreZ() {
		return kind == LightOption.RAY || kind == LightOption.ARC ? z + c / 2 : z;
	}

	@Override
	public double reach() {
		return switch (kind) {
			case LightOption.RAY -> Math.sqrt(a * a + b * b + c * c) / 2 + width * 3 + 0.5;
			case LightOption.ARC -> Math.sqrt(a * a + b * b + c * c) * 0.75 + width * 3 + 0.8;
			case LightOption.RING -> Math.max(a, b) + width * 3 + 0.5;
			default -> a * 1.3 + width * 3 + 0.5;
		};
	}

	public static class Provider implements ParticleProvider<LightOption> {
		@Override
		public Particle createParticle(LightOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			return new LightParticle(level, x, y, z, option);
		}
	}
}
