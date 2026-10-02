package dev.wildercord.client.fx;

import dev.wildercord.aura.AuraFxRules;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * A blade's trail ({@link AuraFxRules.Stroke}): a ribbon of the aura's light along the arc the blade cut, its head racing across
 * in a few ticks, thick and bright, the ribbon thinning and fading behind it, then the whole of it drawing in after the head and
 * fading. A white-hot core runs down its middle. It grows with the stage: wider and longer, and from Flow it sheds motes, from
 * Edge it has a crisp bright edge, from Form a faint echo follows it, and at Sovereign sparks fly off its tip.
 *
 * <p>It's laid round the swordsman as they faced when they cut and carried along with them. In the swordsman's own first-person
 * view it's drawn thin, short and low (see {@link AuraFxRules#OWN_WIDTH} and the rest): it keeps part of its arc and runs down the
 * side of the view under the eye line, laid in the view as they looked when they cut (looking down at a foe tips it with the look),
 * fading out before it reaches the middle of the view, and leaves out the extras. Third person and everyone else see it whole.
 * Decided every frame, so pressing F5 mid-swing changes it at once.</p>
 */
public class AuraTrail extends SingleQuadParticle implements SigilGroup.Extent {
	private final Entity anchor;
	/** Where the swordsman stood when they cut (used once they're gone). */
	private double baseX;
	private double baseY;
	private double baseZ;
	private final AuraFxRules.Stroke stroke;
	private final int color;
	private final int stage;
	private final float power;
	/** Level forward, and the blade's side (by hand and by mirror), as they faced when they cut. */
	private final double fx;
	private final double fz;
	private final double sx;
	private final double sz;
	/** The look's pitch when they cut (radians, down positive), for a thrust and for your own view. */
	private final double pitch;
	/** Ticks before it starts (the second cut of a cross). */
	private final int delay;
	/** Plain ribbon only: no motes, edge, echo or sparks. */
	private final boolean plain;
	private boolean sparked;

	// While drawing: its light, and by day a rim of shade under it.
	private LightStrokes paint;
	private LightStrokes shade;
	private float cx;
	private float cy;
	private float cz;
	/** While drawing your own view: where the middle of the view looks (the trail clears out of it). */
	private Vector3fc look;

	private static final Glimmer.Budget SHED = new Glimmer.Budget(180);

	/**
	 * A trail cut by {@code anchor} (a living creature: its hand decides the blade's side), laid round it as it faces now.
	 *
	 * @param mirror cut back the other way (the second of a run of cuts)
	 * @param power  how much wider than usual
	 * @param plain  the ribbon alone, without the stage's extras
	 */
	public AuraTrail(ClientLevel level, LivingEntity anchor, AuraFxRules.Stroke stroke, boolean mirror, int color, int stage, float power, int delay,
			boolean plain) {
		super(level, anchor.getX(), anchor.getY(), anchor.getZ(), SpellCircleParticle.particleSprite("sigil_beam"));
		this.anchor = anchor;
		this.baseX = anchor.getX();
		this.baseY = anchor.getY();
		this.baseZ = anchor.getZ();
		this.stroke = stroke;
		this.color = color & 0xFFFFFF;
		this.stage = stage;
		this.power = Math.max(0.2F, power);
		double yaw = Math.toRadians(anchor.getYHeadRot());
		this.fx = -Math.sin(yaw);
		this.fz = Math.cos(yaw);
		double side = (anchor.getMainArm() == HumanoidArm.RIGHT ? 1 : -1) * (mirror ? -1 : 1);
		// The player's right, as they face: (-forward.z, 0, forward.x).
		this.sx = -fz * side;
		this.sz = fx * side;
		this.pitch = Math.toRadians(anchor.getXRot());
		this.delay = Math.max(0, delay);
		this.plain = plain;
		this.lifetime = stroke.life + this.delay;
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
		if (anchor != null && !anchor.isRemoved()) {
			baseX = anchor.getX();
			baseY = anchor.getY();
			baseZ = anchor.getZ();
		}
		x = baseX;
		y = baseY;
		z = baseZ;
		float t = age - delay;
		boolean own = ownView();
		if (!plain && !own && t >= 0 && t <= stroke.sweep && AuraFxRules.sheds(stage)) {
			shed(t);
		}
		if (!plain && !own && !sparked && t >= stroke.sweep && AuraFxRules.sparks(stage)) {
			sparked = true;
			sparks();
		}
		if (age++ >= lifetime) {
			remove();
		}
	}

	/** Whether this is the swordsman's own trail, seen through their own eyes. */
	private boolean ownView() {
		Minecraft mc = Minecraft.getInstance();
		return anchor != null && anchor == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
	}

	// ------------------------------------------------------------------ geometry

	/**
	 * The arc's centre (world), its radius, its first and last angles (degrees), and the frame it's laid in: forward ({@code g}) and
	 * up ({@code u}), level for the full view, tipped with the look for the own view (the side is the blade's either way).
	 */
	private record Arc(double x, double y, double z, double radius, double from, double to, double gx, double gy, double gz, double ux, double uy,
			double uz) {}

	private Arc arc(double bx, double by, double bz, boolean own) {
		if (!own) {
			return new Arc(bx + fx * stroke.ahead, by + stroke.height, bz + fz * stroke.ahead, stroke.radius, stroke.from, stroke.to, fx, 0, fz, 0, 1, 0);
		}
		// Your own: about your eyes, in your view as you looked when you cut.
		double cp = Math.cos(pitch);
		double sp = Math.sin(pitch);
		double gx = fx * cp;
		double gy = -sp;
		double gz = fz * cp;
		double ux = fx * sp;
		double uy = cp;
		double uz = fz * sp;
		double eye = eyeHeight();
		double up = stroke.height - AuraFxRules.OWN_DROP - eye;
		double ahead = stroke.ahead;
		double aside = AuraFxRules.OWN_ASIDE;
		return new Arc(bx + gx * ahead + sx * aside + ux * up, by + eye + gy * ahead + uy * up, bz + gz * ahead + sz * aside + uz * up, stroke.radius,
			stroke.ownFrom(), stroke.ownTo(), gx, gy, gz, ux, uy, uz);
	}

	private double eyeHeight() {
		return anchor != null ? anchor.getEyeHeight() : 1.62;
	}

	/** The point at {@code angle} (degrees) round an arc tilted by the stroke, world coordinates. */
	private Vec3 at(Arc arc, double angle, double radius) {
		double a = Math.toRadians(angle);
		double tilt = Math.toRadians(stroke.tilt);
		double c = Math.cos(a) * radius;
		double s = Math.sin(a) * radius;
		// The plane's side axis: the blade's side leaning up (the frame's up) by the tilt.
		double ct = Math.cos(tilt);
		double st = Math.sin(tilt);
		double px = sx * ct + arc.ux * st;
		double py = arc.uy * st;
		double pz = sz * ct + arc.uz * st;
		return new Vec3(arc.x + arc.gx * c + px * s, arc.y + arc.gy * c + py * s, arc.z + arc.gz * c + pz * s);
	}

	/** How much of a point of your own trail shows: none near the middle of your view, all of it from a little way out. */
	private float clear(double x, double y, double z) {
		if (look == null) {
			return 1;
		}
		double dx = x + cx;
		double dy = y + cy;
		double dz = z + cz;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0E-4) {
			return 0;
		}
		double cos = (dx * look.x() + dy * look.y() + dz * look.z()) / len;
		return AuraFxRules.ownClear(Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0, 1.0))));
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float t = age + partial - delay;
		if (t < 0) {
			return;
		}
		boolean own = ownView();
		Vec3 cam = camera.position();
		double bx = Mth.lerp(partial, xo, x);
		double by = Mth.lerp(partial, yo, y);
		double bz = Mth.lerp(partial, zo, z);
		if (anchor != null && !anchor.isRemoved()) {
			bx = Mth.lerp(partial, anchor.xo, anchor.getX());
			by = Mth.lerp(partial, anchor.yo, anchor.getY());
			bz = Mth.lerp(partial, anchor.zo, anchor.getZ());
		}
		cx = (float) -cam.x;
		cy = (float) -cam.y;
		cz = (float) -cam.z;
		paint = new LightStrokes(state, GlowLayers.GLOW);
		look = own ? camera.forwardVector() : null;
		// By day, under the open sky, added light alone washes out: a rim of the mod's darkness a little wider than the glow goes under
		// it (as the slash's crescent has), so it reads against a bright sky. Not in your own first-person trail, kept a whisper.
		shade = !own && brightBehind(bx, by + 1.2, bz) ? new LightStrokes(state, GlowLayers.DARK) : null;
		int life = (int) Math.max(stroke.sweep + 2, Math.round(stroke.life * (own ? AuraFxRules.OWN_LIFE : 1)));
		if (t >= life) {
			paint = null;
			shade = null;
			return;
		}
		// The head races across (easing out); after it, the tail draws in behind it and the whole fades.
		float sweep = Math.max(1, stroke.sweep);
		float p = Math.min(1, t / sweep);
		float head = 1 - (1 - p) * (1 - p);
		float after = t <= sweep ? 0 : Math.min(1, (t - sweep) / Math.max(1, life - sweep));
		float fade = 1 - after * after;
		float width = (float) (AuraFxRules.trailWidth(stage) * power * (own ? AuraFxRules.OWN_WIDTH : 1));
		float alpha = AuraFxRules.trailAlpha(stage) * (own ? AuraFxRules.OWN_ALPHA : 1) * fade;
		double tail = AuraFxRules.trailTail(stage) * (own ? 0.8 : 1);
		if (stroke == AuraFxRules.Stroke.THRUST) {
			thrust(bx, by, bz, own, head, after, tail, width, alpha);
		} else {
			Arc arc = arc(bx, by, bz, own);
			double span = arc.to - arc.from;
			double hd = arc.from + span * head;
			double tl = hd - span * tail;
			// The tail never reaches back past where the cut began, and after the sweep it catches the head up.
			tl = span >= 0 ? Math.max(arc.from, tl) : Math.min(arc.from, tl);
			tl = tl + (hd - tl) * (after * 0.85);
			ribbon(arc, tl, hd, arc.radius, width, alpha, own);
			if (!plain && !own && AuraFxRules.echoes(stage)) {
				// An echo a moment behind, a little inside the arc.
				double lag = 16 * Math.signum(span);
				ribbon(arc, tl - lag, hd - lag, arc.radius * 0.9, width * 0.8F, alpha * 0.35F, true);
			}
			if (!plain && AuraFxRules.edged(stage)) {
				edge(arc, tl, hd, width, alpha * (own ? 0.7F : 1));
			}
			if (after < 1) {
				// The head's glow, bright while it cuts.
				Vec3 h = at(arc, hd, arc.radius);
				paint.glow((float) h.x + cx, (float) h.y + cy, (float) h.z + cz, width * 2.4F,
					LightStrokes.argb(0.65F * (1 - after) * (own ? 0.4F * clear(h.x, h.y, h.z) : 1), LightStrokes.hot(color, 0.5F)));
			}
		}
		paint = null;
		shade = null;
		look = null;
	}

	/** Whether the sky is bright behind a point: day, in a world with a sky, and nothing overhead. */
	private boolean brightBehind(double x, double y, double z) {
		return level.isBrightOutside() && level.canSeeSky(net.minecraft.core.BlockPos.containing(x, y, z));
	}

	/** A line the ribbon is laid along, from its tail ({@code q} 0) to its head (1), world coordinates. */
	@FunctionalInterface
	private interface Path {
		Vec3 at(double q);
	}

	/**
	 * One pass of light along {@code path} ({@code length} blocks): pieces walked from tail to head, each a half-piece on from the
	 * last, so every point is lit by two and the ribbon is even all along, however it curves or thins (pieces that only met end to
	 * end, or overlapped by chance, showed as beads). Thick and bright at the head, thinning and fading to the tail.
	 *
	 * @param core  the white-hot core (the hard band sprite) rather than the glow (the soft one)
	 * @param width the glow's width at the head (the core is a share of it)
	 */
	private void pass(Path path, double length, boolean core, float width, float alpha, int rgb, float floor) {
		pass(paint, path, length, core, width, alpha, rgb, floor);
	}

	private void pass(LightStrokes painter, Path path, double length, boolean core, float width, float alpha, int rgb, float floor) {
		if (length < 1.0E-3 || alpha <= 0.004F) {
			return;
		}
		double s = 0;
		while (s < length) {
			double q = s / length;
			float shape = (float) (floor + (1 - floor) * Math.pow(q, 0.7));
			float w = (float) (width * shape * (core ? AuraFxRules.CORE_SHARE : 1));
			float side = Math.max(0.012F, w / (core ? LightStrokes.LINE_SHOWS : LightStrokes.SOFT_SHOWS));
			double mid = Math.min(1, (s + side * 0.25) / length);
			Vec3 p = path.at(mid);
			Vec3 on = path.at(Math.min(1, mid + 0.01)).subtract(path.at(Math.max(0, mid - 0.01)));
			float lit = (float) Math.pow(mid, 1.25);
			// Two pieces light every point: each carries a little over half.
			float a = alpha * lit * 0.58F * clear(p.x, p.y, p.z);
			if (a > 0.003F) {
				painter.piece(core, (float) p.x + cx, (float) p.y + cy, (float) p.z + cz, (float) on.x, (float) on.y, (float) on.z, w,
					LightStrokes.argb(a, rgb));
			}
			s += side * 0.5;
		}
	}

	/** The ribbon from angle {@code from} (its faint tail) to {@code to} (its bright head) at {@code radius}. */
	private void ribbon(Arc arc, double from, double to, double radius, float width, float alpha, boolean thin) {
		double span = to - from;
		double length = Math.abs(Math.toRadians(span)) * radius;
		Path path = q -> at(arc, from + span * q, radius);
		if (shade != null && !thin) {
			pass(shade, path, length, false, width * 1.3F, Math.min(1, alpha * 1.1F), GlowLayers.darkColor(color), 0.3F);
		}
		pass(path, length, false, width, alpha, color, 0.3F);
		if (!thin || alpha > 0.1F) {
			pass(path, length, true, width, Math.min(1, alpha * 2.4F), LightStrokes.hot(color, 0.72F), 0.3F);
		}
	}

	/** Edge: a crisp line of white-hot light along the outside of the ribbon's leading part, as a crystal blade's edge catches light. */
	private void edge(Arc arc, double from, double to, float width, float alpha) {
		double span = to - from;
		double start = from + span * 0.4;
		double radius = arc.radius + width * 0.28;
		double length = Math.abs(Math.toRadians(to - start)) * radius;
		pass(q -> at(arc, start + (to - start) * q, radius), length, true, Math.max(0.04F, width * 0.36F), Math.min(1, alpha * 1.8F),
			LightStrokes.hot(color, 0.88F), 0.0F);
	}

	/** A thrust: a lance of light from the hand out along the look, its head racing out, then drawing in after it. */
	private void thrust(double bx, double by, double bz, boolean own, float head, float after, double tail, float width, float alpha) {
		double len = stroke.radius * Math.max(0.8, power * 0.85);
		double cp = Math.cos(pitch * 0.7);
		double dx = fx * cp;
		double dy = -Math.sin(pitch * 0.7);
		double dz = fz * cp;
		double ox = bx + fx * stroke.ahead;
		double oy = by + stroke.height;
		double oz = bz + fz * stroke.ahead;
		if (own) {
			// From low by the blade hand, out ahead and a little in, staying under the eye line, in your view as you looked.
			double side = 0.34;
			double lc = Math.cos(pitch);
			double ls = Math.sin(pitch);
			double gx = fx * lc;
			double gy = -ls;
			double gz = fz * lc;
			double ux = fx * ls;
			double uy = lc;
			double uz = fz * ls;
			double eye = eyeHeight();
			double up = stroke.height - AuraFxRules.OWN_DROP - 0.05 - eye;
			ox = bx + gx * 0.35 + sx * side + ux * up;
			oy = by + eye + gy * 0.35 + uy * up;
			oz = bz + gz * 0.35 + sz * side + uz * up;
			dx = gx * 0.97 - sx * 0.14 - ux * 0.08;
			dy = gy * 0.97 - uy * 0.08;
			dz = gz * 0.97 - sz * 0.14 - uz * 0.08;
			len = 1.7;
		}
		double reach = len * head;
		double back = Math.max(0, reach - len * tail);
		back = back + (reach - back) * (after * 0.85);
		double from = back;
		double x0 = ox;
		double y0 = oy;
		double z0 = oz;
		double ddx = dx;
		double ddy = dy;
		double ddz = dz;
		Path path = q -> {
			double d = from + (reach - from) * q;
			return new Vec3(x0 + ddx * d, y0 + ddy * d, z0 + ddz * d);
		};
		if (shade != null) {
			pass(shade, path, reach - back, false, width * 1.3F, Math.min(1, alpha * 1.1F), GlowLayers.darkColor(color), 0.35F);
		}
		pass(path, reach - back, false, width, alpha, color, 0.35F);
		pass(path, reach - back, true, width, Math.min(1, alpha * 2.4F), LightStrokes.hot(color, 0.75F), 0.35F);
		if (after < 1) {
			paint.glow((float) (ox + dx * reach) + cx, (float) (oy + dy * reach) + cy, (float) (oz + dz * reach) + cz, width * 2.2F,
				LightStrokes.argb(0.6F * (1 - after) * (own ? 0.5F : 1), LightStrokes.hot(color, 0.5F)));
		}
	}

	// ------------------------------------------------------------------ extras

	/** Flow: a mote shed off the head as it cuts, drifting out and up. */
	private void shed(float t) {
		if (!SHED.hasRoom() || stroke == AuraFxRules.Stroke.THRUST) {
			return;
		}
		Arc arc = arc(baseX, baseY, baseZ, false);
		float p = Math.min(1, t / Math.max(1, stroke.sweep));
		double angle = arc.from + (arc.to - arc.from) * (1 - (1 - p) * (1 - p));
		Vec3 at = at(arc, angle, arc.radius);
		Vec3 out = at.subtract(arc.x, arc.y, arc.z).normalize().scale(0.02);
		int life = 10 + random.nextInt(8);
		Minecraft.getInstance().particleEngine.add(Glimmer.mote(level, at, random.nextInt(3) == 0 ? LightStrokes.hot(color, 0.5F) : color,
			0.06F + random.nextFloat() * 0.04F, 0.7F, life, out.x, out.y + 0.01, out.z, 0.003F));
		SHED.spend(life);
	}

	/** Sovereign: sparks flung off the tip as the cut ends. */
	private void sparks() {
		Vec3 tip;
		Vec3 onward;
		if (stroke == AuraFxRules.Stroke.THRUST) {
			tip = new Vec3(baseX + fx * (stroke.ahead + stroke.radius), baseY + stroke.height, baseZ + fz * (stroke.ahead + stroke.radius));
			onward = new Vec3(fx, 0, fz);
		} else {
			Arc arc = arc(baseX, baseY, baseZ, false);
			tip = at(arc, arc.to, arc.radius);
			onward = at(arc, arc.to + Math.signum(arc.to - arc.from) * 8, arc.radius).subtract(tip).normalize();
		}
		for (int i = 0; i < 6 && SHED.hasRoom(); i++) {
			double spread = 0.35;
			double vx = onward.x * 0.09 + (random.nextDouble() - 0.5) * spread * 0.12;
			double vy = onward.y * 0.09 + random.nextDouble() * 0.05;
			double vz = onward.z * 0.09 + (random.nextDouble() - 0.5) * spread * 0.12;
			int life = 8 + random.nextInt(8);
			Minecraft.getInstance().particleEngine.add(Glimmer.mote(level, tip, LightStrokes.hot(color, 0.65F), 0.05F + random.nextFloat() * 0.03F,
				0.95F, life, vx, vy, vz, 0.0F));
			SHED.spend(life);
		}
	}

	/** Called once a client tick by whoever spawns trails. */
	public static void tickBudget() {
		SHED.tick();
	}

	// ------------------------------------------------------------------ the particle's housekeeping

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
		return y + 1.0;
	}

	@Override
	public double centreZ() {
		return z;
	}

	@Override
	public double reach() {
		return stroke.radius + stroke.ahead + 2.5;
	}
}
