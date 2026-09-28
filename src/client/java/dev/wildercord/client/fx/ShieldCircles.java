package dev.wildercord.client.fx;

import dev.wildercord.content.ShieldOption;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.spell.SpellSigil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Shield's magic circles, on each client. A Shield is invisible until a spell comes at the creature
 * it guards; then its circles (the spell that raised it, written out like any other) spawn in between
 * the spell and them, facing the spell, stacked one behind another: a stronger Shield stacks more.
 * <ul>
 *   <li>A spell it stops shatters as many circles from the front as its mana paid for, one after
 *       another; the next one holds, light flaring at its heart and ripples racing out across it, and
 *       the stack fades, spent.</li>
 *   <li>A spell that breaks it shatters every circle, front to back, the way real glass goes: cracks
 *       shoot out from the heart across the circle, and a moment later it bursts, the rim breaking into
 *       curved slivers and the rest into shards (small near the heart, larger further out) that fly on
 *       the way the spell was going, tumble, fall and glint.</li>
 *   <li>A spell it parries (raised at the last moment) breaks nothing: every circle flushes gold, the
 *       front one flashing at its heart as bright rings race out past its rim.</li>
 * </ul>
 * A spell flying at them makes the stack spawn in, back to front, a moment before it arrives
 * ({@link ShieldOption#APPEAR}); one that lands at once (a beam, a blast) makes it snap open as it lands.
 */
public final class ShieldCircles {
	private ShieldCircles() {}

	/** Ticks between one circle of a stack breaking and the next. */
	static final int STEP = 2;
	/** Ticks a circle shows its cracks before it bursts. */
	static final int CRACKING = 2;

	/** The stack showing for each creature, by id. */
	private static final Map<Integer, Stack> SHOWING = new HashMap<>();

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			SHOWING.clear();
			return;
		}
		SHOWING.values().removeIf(s -> !s.alive() || s.level != mc.level);
	}

	public static class Provider implements ParticleProvider<ShieldOption> {
		@Override
		public Particle createParticle(ShieldOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			Entity found = option.entity() == 0 ? null : level.getEntity(option.entity());
			LivingEntity entity = found instanceof LivingEntity living ? living : null;
			Vector3f dir = new Vector3f(option.dx(), option.dy(), option.dz());
			if (dir.lengthSquared() < 1.0E-6F) {
				dir.set(0, 0, 1);
			}
			dir.normalize();
			Stack showing = entity == null ? null : SHOWING.get(entity.getId());
			if (showing != null && showing.alive()) {
				if (option.kind() != ShieldOption.APPEAR) {
					showing.meet(option.kind(), option.broken(), dir);
				}
				return null;
			}
			Stack stack = new Stack(level, entity, new Vec3(x, y, z), dir, option, random);
			if (entity != null) {
				SHOWING.put(entity.getId(), stack);
			}
			if (option.kind() != ShieldOption.APPEAR) {
				stack.meet(option.kind(), option.broken(), dir);
			}
			return null;
		}
	}

	/** One Shield's circles, front (0) to back. */
	static final class Stack {
		final ClientLevel level;
		final LivingEntity entity;
		final Vec3 origin;
		final Vector3f dir;
		final float offset;
		final float radius;
		final int color;
		final boolean early;
		final RandomSource random;
		final List<ShieldCircle> layers = new ArrayList<>();
		/** Raised just in time: it parried, and the circles ring gold instead of breaking. */
		boolean parry;

		Stack(ClientLevel level, LivingEntity entity, Vec3 origin, Vector3f dir, ShieldOption option, RandomSource random) {
			this.level = level;
			this.entity = entity;
			this.origin = origin;
			this.dir = new Vector3f(dir);
			this.offset = option.offset();
			this.radius = option.radius();
			this.color = option.color() & 0xFFFFFF;
			this.early = option.kind() == ShieldOption.APPEAR;
			this.random = random;
			int n = option.layers();
			Minecraft mc = Minecraft.getInstance();
			for (int i = 0; i < n; i++) {
				ShieldCircle circle = new ShieldCircle(this, i, n, option.runes());
				layers.add(circle);
				mc.particleEngine.add(circle);
			}
		}

		boolean alive() {
			return layers.stream().anyMatch(ShieldCircle::isAlive);
		}

		/** Where the creature is now. */
		Vec3 creature(float partial) {
			if (entity == null || entity.isRemoved()) {
				return origin;
			}
			return new Vec3(Mth.lerp(partial, entity.xo, entity.getX()), Mth.lerp(partial, entity.yo, entity.getY()) + entity.getBbHeight() / 2,
				Mth.lerp(partial, entity.zo, entity.getZ()));
		}

		/**
		 * The spell arrives: {@code broken} circles from the front shatter; if that isn't all of them, the
		 * next one holds. A parry breaks none: the front one holds and every one rings gold.
		 */
		void meet(int kind, int broken, Vector3f dir) {
			this.dir.set(dir);
			int n = layers.size();
			parry = kind == ShieldOption.PARRY;
			int stop = kind == ShieldOption.BREAK ? -1 : parry ? 0 : Math.min(broken, n - 1);
			int shattered = kind == ShieldOption.BREAK ? n : stop;
			for (int i = 0; i < n; i++) {
				ShieldCircle layer = layers.get(i);
				if (i < shattered) {
					layer.shatterAt(i * STEP);
				} else if (i == stop) {
					layer.stopAt(i * STEP);
				} else {
					layer.behindAt(stop * STEP + 1 + (i - stop));
				}
			}
		}
	}

	/** One circle of a stack. */
	static final class ShieldCircle extends SpellCircleParticle {
		private static final int WAITING = 0;
		private static final int SHATTER = 1;
		private static final int STOP = 2;
		private static final int BEHIND = 3;
		/** Ticks a stack that spawned in waits for its spell before it gives up and fades. */
		private static final int WAIT = 40;
		private static final int FADE = 10;
		/** A parry's gold. */
		private static final int PARRY = 0xFFD54A;

		private final Stack stack;
		private final int index;
		private final int count;
		/** When it spawns in, in ticks after the stack: the back one first. */
		private float delay;
		private final TextureAtlasSprite crack;
		private int role = WAITING;
		/** Ticks since the spell arrived, and when this circle's part in it starts. */
		private int since = -1;
		private int start;
		private int fading = -1;
		private final List<float[]> cracks = new ArrayList<>();

		ShieldCircle(Stack stack, int index, int count, List<String> runes) {
			super(stack.level, stack.origin.x, stack.origin.y, stack.origin.z, new SpellCircleOption(runes, stack.color, stack.radius, 0, 0, 20 * 60));
			this.stack = stack;
			this.index = index;
			this.count = count;
			this.delay = (count - 1 - index) * (stack.early ? 0.35F : 0.25F);
			this.crack = particleSprite("shield_crack");
			Vec3 at = centre(0);
			setPos(at.x, at.y, at.z);
			xo = x;
			yo = y;
			zo = z;
		}

		/** The spell is here: one that hasn't spawned in yet does so now. */
		private void arrive() {
			delay = Math.min(delay, age);
		}

		void shatterAt(int start) {
			arrive();
			role = SHATTER;
			since = 0;
			this.start = start;
			fading = -1;
			buildCracks();
		}

		void stopAt(int start) {
			arrive();
			role = STOP;
			since = 0;
			this.start = start;
			fading = -1;
		}

		void behindAt(int start) {
			arrive();
			role = BEHIND;
			since = 0;
			this.start = start;
			fading = -1;
		}

		/** Its distance out from the creature: the front one furthest, facing the spell. */
		private float distance() {
			return stack.offset + ShieldOption.SPACING * (count - 1 - index);
		}

		@Override
		protected Vec3 centre(float partial) {
			float d = distance();
			return stack.creature(partial).add(stack.dir.x * d, stack.dir.y * d, stack.dir.z * d);
		}

		@Override
		protected Quaternionf orientation(float partial) {
			float yaw = (float) Math.atan2(-stack.dir.x, stack.dir.z);
			float pitch = (float) Math.asin(Mth.clamp(-stack.dir.y, -1, 1));
			return new Quaternionf().rotationYXZ(-yaw, pitch, 0);
		}

		/** Ticks since this circle's part began (negative before). */
		private float part(float partial) {
			return since < 0 ? -1 : since + partial - start;
		}

		@Override
		protected float opening(float partial) {
			float t = age + partial - delay;
			// Ahead of its spell it opens over an eighth of a second; struck at once, it snaps open.
			return Mth.clamp(stack.early ? t / 2.5F : t / 1.5F, 0, 1);
		}

		@Override
		protected float fade(float partial) {
			float t = age + partial - delay;
			if (t <= 0) {
				return 0;
			}
			float in = Mth.clamp(t / (stack.early ? 2F : 1F), 0, 1);
			float out = fading < 0 ? 1 : Mth.clamp((fading - partial) / FADE, 0, 1);
			float p = part(partial);
			float flicker = role == SHATTER && p >= 0 ? 0.8F + 0.2F * Mth.sin(p * 3.1F) : 1;
			// The ones further back glow a little less.
			float depth = 1 - 0.08F * index;
			return in * out * flicker * depth;
		}

		@Override
		protected float size(float partial) {
			float p = part(partial);
			// The one that holds gives a little as it's struck, like a drumskin.
			return role == STOP && p >= 0 ? radius * (1 + 0.07F * (float) Math.exp(-p * 0.6F)) : radius;
		}

		@Override
		public void tick() {
			super.tick();
			if (since >= 0) {
				since++;
			}
			Vec3 at = centre(1);
			setPos(at.x, at.y, at.z);
			if (stack.entity != null && stack.entity.isRemoved() && fading < 0) {
				fading = FADE;
			}
			if (role == WAITING && age > WAIT && fading < 0) {
				// Its spell never came (it missed, or fizzled): the stack fades away, unspent.
				fading = FADE;
			}
			float p = part(0);
			if (role == SHATTER && p >= CRACKING) {
				shatter();
				remove();
				return;
			}
			if ((role == STOP && p > 14 || role == BEHIND && p > 10) && fading < 0) {
				fading = FADE;
			}
			if (fading >= 0 && --fading <= 0) {
				remove();
			}
		}

		@Override
		protected void extras(float r, float a, float fine, float partial) {
			// A barrier, not just lines: the circle's face glows softly all over.
			piece(glow, 0, 0, 0, r * 1.02F, argb(a * 0.42F, color), 0.001F);
			float t = part(partial);
			if (t < 0) {
				return;
			}
			if (stack.parry && (role == STOP || role == BEHIND)) {
				parried(r, a, fine, t);
				return;
			}
			if (role == STOP) {
				// The spell's light spent against its heart, and ripples racing out across it.
				float flare = Math.max(0, 1 - t / 7F);
				if (flare > 0) {
					piece(glow, 0, 0, 0, r * (0.35F + 0.35F * flare), argb(a * flare, 0xFFFFFF), 0.012F);
				}
				for (int k = 0; k < 3; k++) {
					float f = (t - k * 2.5F) / 11F;
					if (f > 0 && f < 1) {
						ring(0, 0, r * (0.12F + 1.05F * f), fine * (2.6F - 1.2F * f), argb(a * (1 - f) * 0.95F, lighter(color, 0.6F)), 0.012F);
					}
				}
			} else if (role == BEHIND) {
				// The blow still felt behind the one that held: one soft ripple.
				float f = t / 9F;
				if (f < 1) {
					ring(0, 0, r * (0.15F + f), fine * 1.6F, argb(a * (1 - f) * 0.6F, lighter(color, 0.5F)), 0.012F);
				}
			} else if (role == SHATTER) {
				float grown = Mth.clamp(t / 1.5F, 0, 1);
				int argb = argb(Math.min(1, a * 1.3F), lighter(color, 0.8F));
				for (float[] c : cracks) {
					if (c[4] <= grown) {
						crackLine(c[0] * r, c[1] * r, c[2] * r, c[3] * r, fine * 1.6F, argb);
					}
				}
				float flare = Math.max(0, 1 - t / 3F);
				if (flare > 0) {
					piece(glow, 0, 0, 0, r * 0.45F, argb(a * flare, 0xFFFFFF), 0.012F);
				}
			}
		}

		/**
		 * A parry: the whole circle flushes gold, a white-gold flash bursts at its heart, and a bright ring
		 * races out past its rim, then a second, softer one. The front circle rings loudest; the ones
		 * behind it echo, a beat later.
		 */
		private void parried(float r, float a, float fine, float t) {
			float echo = role == STOP ? 1 : 0.6F;
			float flush = Math.max(0, 1 - t / 12F);
			if (flush > 0) {
				piece(glow, 0, 0, 0, r * 1.05F, argb(a * flush * 0.7F * echo, PARRY), 0.002F);
			}
			float flare = Math.max(0, 1 - t / 5F);
			if (flare > 0 && role == STOP) {
				piece(glow, 0, 0, 0, r * (0.5F + 0.5F * flare), argb(a * flare, 0xFFF6D8), 0.012F);
			}
			for (int k = 0; k < 2; k++) {
				float f = (t - k * 3F) / 9F;
				if (f > 0 && f < 1) {
					float out = r * (0.2F + 1.5F * (float) Math.sqrt(f));
					ring(0, 0, out, fine * (3.4F - 2F * f) * echo, argb(a * (1 - f) * echo, k == 0 ? lighter(PARRY, 0.35F) : PARRY), 0.013F);
				}
			}
		}

		/** A crack segment: pieces of the crack texture laid end to end, in the circle's plane. */
		private void crackLine(float u0, float v0, float u1, float v1, float width, int argb) {
			float du = u1 - u0;
			float dv = v1 - v0;
			float length = Mth.sqrt(du * du + dv * dv);
			if (length < 1.0E-4F) {
				return;
			}
			int n = Math.max(1, (int) Math.ceil(length / (width * 10)));
			float half = length / n / 2 * 1.06F;
			float rot = (float) Math.atan2(dv, du);
			for (int i = 0; i < n; i++) {
				float f = (i + 0.5F) / n;
				piece(crack, u0 + du * f, v0 + dv * f, rot, half, argb, 0.011F);
			}
		}

		/** Real glass struck in the middle: long cracks radiating out to the rim, forking, and rings of short ones joining them. */
		private void buildCracks() {
			cracks.clear();
			RandomSource random = stack.random;
			int radial = 9 + random.nextInt(4);
			float[] bearings = new float[radial];
			for (int i = 0; i < radial; i++) {
				bearings[i] = Mth.TWO_PI * (i + (random.nextFloat() - 0.5F) * 0.6F) / radial;
				walk(0.03F, bearings[i], 0.98F, 2);
			}
			for (float ring : new float[] {0.18F, 0.38F, 0.62F}) {
				for (int i = 0; i < radial; i++) {
					if (random.nextFloat() < 0.3F) {
						continue;
					}
					float a0 = bearings[i];
					float a1 = bearings[(i + 1) % radial] + (i + 1 == radial ? Mth.TWO_PI : 0);
					float rr = ring * (0.88F + random.nextFloat() * 0.24F);
					float pu = Mth.cos(a0) * rr;
					float pv = Mth.sin(a0) * rr;
					for (int s = 1; s <= 3; s++) {
						float ang = Mth.lerp(s / 3F, a0, a1);
						float q = rr * (0.95F + random.nextFloat() * 0.1F);
						float qu = Mth.cos(ang) * q;
						float qv = Mth.sin(ang) * q;
						cracks.add(new float[] {pu, pv, qu, qv, ring});
						pu = qu;
						pv = qv;
					}
				}
			}
		}

		private void walk(float from, float bearing, float to, int forks) {
			RandomSource random = stack.random;
			float rr = from;
			float pu = Mth.cos(bearing) * rr;
			float pv = Mth.sin(bearing) * rr;
			while (rr < to) {
				float next = Math.min(to, rr + 0.08F + random.nextFloat() * 0.05F);
				bearing += (random.nextFloat() - 0.5F) * 0.16F;
				float qu = Mth.cos(bearing) * next;
				float qv = Mth.sin(bearing) * next;
				cracks.add(new float[] {pu, pv, qu, qv, rr});
				if (forks > 0 && rr > 0.2F && random.nextFloat() < 0.14F) {
					walk(rr, bearing + (random.nextBoolean() ? 0.4F : -0.4F), Math.min(to, rr + 0.25F + random.nextFloat() * 0.3F), forks - 1);
				}
				pu = qu;
				pv = qv;
				rr = next;
			}
		}

		/** It bursts: the rim into curved slivers, the rest into shards that fly on the way the spell was going. */
		private void shatter() {
			Minecraft mc = Minecraft.getInstance();
			RandomSource random = stack.random;
			Vec3 c = centre(1);
			Quaternionf q = orientation(1);
			Vector3f right = q.transform(new Vector3f(1, 0, 0));
			Vector3f up = q.transform(new Vector3f(0, 1, 0));
			Vector3f normal = new Vector3f(stack.dir);
			// The spell was going toward the creature: the way the pieces are punched.
			Vector3f through = new Vector3f(stack.dir).negate();
			float r = radius * SpellSigil.FRAME;
			float[] rings = {0, 0.12F, 0.27F, 0.45F, 0.66F, 0.9F};
			for (int k = 0; k < rings.length - 1; k++) {
				float r0 = rings[k] * r;
				float r1 = rings[k + 1] * r;
				float mid = (r0 + r1) / 2;
				float band = r1 - r0;
				int sectors = k == 0 ? 5 : Math.max(5, Math.round(Mth.TWO_PI * mid / (band * 1.1F)));
				float turned = random.nextFloat() * Mth.TWO_PI;
				for (int s = 0; s < sectors; s++) {
					float ang = turned + Mth.TWO_PI * (s + 0.5F + (random.nextFloat() - 0.5F) * 0.5F) / sectors;
					float rr = k == 0 ? band * 0.5F : mid + (random.nextFloat() - 0.5F) * band * 0.4F;
					Vector3f radial = new Vector3f(right).mul(Mth.cos(ang)).add(new Vector3f(up).mul(Mth.sin(ang)));
					Vec3 at = c.add(radial.x * rr, radial.y * rr, radial.z * rr);
					float width = k == 0 ? band : Mth.TWO_PI * mid / sectors;
					float size = Math.min(band, width) * (0.55F + random.nextFloat() * 0.25F);
					float near = (float) Math.exp(-rr / r * 2.5F);
					Vector3f v = new Vector3f(through).mul(0.06F + 0.2F * near)
						.add(new Vector3f(radial).mul(0.04F + 0.09F * rr / r))
						.add((random.nextFloat() - 0.5F) * 0.04F, 0.02F + random.nextFloat() * 0.05F, (random.nextFloat() - 0.5F) * 0.04F);
					mc.particleEngine.add(new ShieldBreak.Shard(level, at, v, normal, size, color, random.nextInt(2), random));
				}
			}
			// The rim: the circle's frame breaks into curved slivers flung outward.
			int slivers = 8 + random.nextInt(3);
			for (int i = 0; i < slivers; i++) {
				float span = Mth.TWO_PI / slivers * (0.7F + random.nextFloat() * 0.2F);
				float ang = Mth.TWO_PI * (i + 0.5F) / slivers + (random.nextFloat() - 0.5F) * 0.2F;
				Vector3f radial = new Vector3f(right).mul(Mth.cos(ang)).add(new Vector3f(up).mul(Mth.sin(ang)));
				Vector3f tangent = new Vector3f(normal).cross(radial).normalize();
				Vec3 at = c.add(radial.x * r, radial.y * r, radial.z * r);
				Vector3f v = new Vector3f(radial).mul(0.1F + random.nextFloat() * 0.06F).add(new Vector3f(through).mul(0.08F))
					.add(0, 0.04F + random.nextFloat() * 0.04F, 0);
				mc.particleEngine.add(new ShieldBreak.RimShard(level, at, v, radial, tangent, normal, r, span, Math.max(0.012F, radius * 0.03F), color, random));
			}
		}
	}
}
