package dev.wildercord.client.fx;

import dev.wildercord.cast.events.ManaStorm;
import dev.wildercord.content.LightOption;
import dev.wildercord.world.LeyLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Under a mana storm the sky, fog and clouds lean violet, easing in over three seconds and out
 * again as it passes; violet lightning crawls across the sky now and then (the sky flashing pale
 * with it), and arcs jump between points of the ley line near the player. Read from the synced
 * {@link ManaStorm#STORM_UNTIL}. The tint goes in as layers on the client level's environment
 * attributes (see {@code ClientLevelMixin}), so the sky disc, the fog and everything that reads
 * the sky colour agree. It keeps the sky's own brightness: a storm by day is lavender, by night a
 * deep violet, never a glowing purple night.
 */
public final class StormSky {
	private StormSky() {}

	/** The violet the world leans toward, at unit brightness (its luminance is {@link #VIOLET_LUM}). */
	private static final float R = 0.60F;
	private static final float G = 0.40F;
	private static final float B = 0.98F;
	private static final float VIOLET_LUM = 0.2126F * R + 0.7152F * G + 0.0722F * B;
	/** How far at most the sky, the fog and the clouds lean. */
	private static final float SKY = 0.55F;
	private static final float FOG = 0.45F;
	private static final float CLOUD = 0.4F;
	/** A storm is a little darker than the day around it. */
	private static final float DIM = 0.1F;
	private static final int EASE_TICKS = 60;

	private static final int VIOLET = ManaStorm.VIOLET;
	private static final int CORE = ManaStorm.CORE;

	private static float amount;
	/** 1 as lightning crosses the sky, fading over a few ticks. */
	private static float flash;
	private static int nextSky = 30;
	private static int nextGround = 12;

	/** 0 (no storm) to 1 (well inside one). */
	public static float amount() {
		return amount;
	}

	/** How strongly the ley lines surge (the same as {@link #amount()}). */
	public static float surge() {
		return amount;
	}

	/** Every client tick: eases the tint in or out, and throws the storm's arcs. */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (player == null || level == null) {
			amount = 0;
			flash = 0;
			return;
		}
		float target = ManaStorm.inside(player) ? 1.0F : 0.0F;
		float step = 1.0F / EASE_TICKS;
		amount = target > amount ? Math.min(target, amount + step) : Math.max(target, amount - step);
		flash = Math.max(0, flash - 0.25F);
		if (amount < 0.35F) {
			return;
		}
		RandomSource random = player.getRandom();
		if (--nextSky <= 0) {
			nextSky = 18 + random.nextInt(34);
			skyArc(mc, level, player, random);
		}
		if (--nextGround <= 0) {
			nextGround = 8 + random.nextInt(14);
			groundArc(mc, level, player, random);
		}
	}

	// ------------------------------------------------------------------ the tint

	public static Vector3fc tintSky(Vector3fc color) {
		Vector3fc tinted = lean(color, amount * SKY);
		if (flash > 0) {
			float k = flash * 0.28F * amount;
			tinted = new Vector3f(tinted.x() + (0.86F - tinted.x()) * k, tinted.y() + (0.80F - tinted.y()) * k, tinted.z() + (1.0F - tinted.z()) * k);
		}
		return tinted;
	}

	public static Vector3fc tintFog(Vector3fc color) {
		return lean(color, amount * FOG);
	}

	public static Vector4fc tintCloud(Vector4fc color) {
		Vector3fc c = lean(new Vector3f(color.x(), color.y(), color.z()), amount * CLOUD);
		return new Vector4f(c.x(), c.y(), c.z(), color.w());
	}

	/** {@code color} leaned {@code k} of the way to violet of the same brightness, and dimmed a touch. */
	private static Vector3fc lean(Vector3fc color, float k) {
		if (k <= 0.001F) {
			return color;
		}
		float lum = 0.2126F * color.x() + 0.7152F * color.y() + 0.0722F * color.z();
		float scale = lum / VIOLET_LUM;
		float dim = 1 - DIM * k / SKY;
		return new Vector3f(
			Mth.clamp((color.x() + (R * scale - color.x()) * k) * dim, 0, 1),
			Mth.clamp((color.y() + (G * scale - color.y()) * k) * dim, 0, 1),
			Mth.clamp((color.z() + (B * scale - color.z()) * k) * dim, 0, 1));
	}

	// ------------------------------------------------------------------ the arcs

	/**
	 * Violet lightning from {@code from} to {@code to} right now, as the storm throws it: wide and
	 * forked with a flash of the sky if it's up in the sky, a thin crackle if it's near the ground.
	 * For showing a storm off (the showcase); the storm throws its own arcs as it goes.
	 */
	public static void strike(Minecraft mc, Vec3 from, Vec3 to, boolean sky) {
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		RandomSource random = level.getRandom();
		if (sky) {
			bolt(mc, level, random, from, to, 0.32F, 3, 9);
			bolt(mc, level, random, from, to, 0.2F, 1, 5);
			flash = 1;
		} else {
			bolt(mc, level, random, from, to, 0.07F, 2, 6);
		}
	}

	/** Violet lightning crawling across the sky, well above and away from the player, with a fork or two. */
	private static void skyArc(Minecraft mc, ClientLevel level, LocalPlayer player, RandomSource random) {
		if (!level.canSeeSky(player.blockPosition().above())) {
			return;
		}
		double a = random.nextDouble() * Math.PI * 2;
		double away = 22 + random.nextDouble() * 26;
		Vec3 mid = player.position().add(Math.cos(a) * away, 26 + random.nextDouble() * 18, Math.sin(a) * away);
		double b = random.nextDouble() * Math.PI * 2;
		double half = 7 + random.nextDouble() * 9;
		Vec3 along = new Vec3(Math.cos(b) * half, (random.nextDouble() - 0.5) * half * 0.5, Math.sin(b) * half);
		bolt(mc, level, random, mid.subtract(along), mid.add(along), 0.32F, 3, 9);
		// A second, fainter path along the same way, so it looks forked and flickering.
		bolt(mc, level, random, mid.subtract(along), mid.add(along), 0.2F, 1, 5);
		flash = 1;
	}

	/** An arc jumping between two points of the ley line near the player (or of the ground, off the line). */
	private static void groundArc(Minecraft mc, ClientLevel level, LocalPlayer player, RandomSource random) {
		boolean ley = LeyMotes.known() && level.dimension() == Level.OVERWORLD;
		Vec3 first = null;
		for (int i = 0; i < 16; i++) {
			double x = player.getX() + (random.nextDouble() - 0.5) * 36;
			double z = player.getZ() + (random.nextDouble() - 0.5) * 36;
			// Prefer the line itself; late tries take any ground, so a storm off a strong line still crackles.
			if (ley && i < 12 && LeyLines.strength(LeyMotes.seed(), x, z) < 0.5) {
				continue;
			}
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
			Vec3 p = new Vec3(x, top + 0.3 + random.nextDouble() * 1.6, z);
			if (Math.abs(p.y - player.getY()) > 14 || p.distanceTo(player.position()) < 4) {
				continue;
			}
			if (first == null) {
				first = p;
			} else if (first.distanceTo(p) >= 3 && first.distanceTo(p) <= 10) {
				bolt(mc, level, random, first, p, 0.07F, 2, 6);
				mc.particleEngine.add(new LightParticle(level, p.x, p.y, p.z, new LightOption(LightOption.ORB, CORE, 0.16F, 0, 0, 0.015F, 0, 0, 0, 8)));
				mc.particleEngine.add(new LightParticle(level, first.x, first.y, first.z,
					new LightOption(LightOption.ORB, CORE, 0.12F, 0, 0, 0.015F, 0, 0, 0, 8)));
				return;
			}
		}
	}

	/** A jagged line of light from {@code from} to {@code to}, with {@code forks} short branches. */
	private static void bolt(Minecraft mc, ClientLevel level, RandomSource random, Vec3 from, Vec3 to, float width, int forks, int life) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		if (length < 0.1) {
			return;
		}
		Vec3 dir = d.scale(1 / length);
		Vec3 u = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0).cross(dir).normalize() : new Vec3(0, 1, 0).cross(dir).normalize();
		Vec3 v = dir.cross(u);
		int segments = Mth.clamp((int) Math.round(length / 1.1), 4, 16);
		double jag = Math.min(1.6, 0.15 + length * 0.07);
		Vec3[] points = new Vec3[segments + 1];
		points[0] = from;
		for (int i = 1; i <= segments; i++) {
			Vec3 p = from.add(d.scale(i / (double) segments));
			if (i < segments) {
				p = p.add(u.scale((random.nextDouble() - 0.5) * 2 * jag)).add(v.scale((random.nextDouble() - 0.5) * 2 * jag));
			}
			points[i] = p;
			ray(mc, level, points[i - 1], p, i % 3 == 0 ? CORE : VIOLET, width, life);
		}
		for (int k = 0; k < forks; k++) {
			Vec3 start = points[1 + random.nextInt(segments - 1)];
			Vec3 side = u.scale(random.nextDouble() - 0.5).add(v.scale(random.nextDouble() - 0.5)).normalize();
			double forkLength = length * (0.15 + 0.15 * random.nextDouble());
			Vec3 bend = start.add(dir.scale(forkLength * 0.4)).add(side.scale(forkLength * 0.5));
			Vec3 end = bend.add(dir.scale(forkLength * 0.3)).add(side.scale(forkLength * 0.4));
			ray(mc, level, start, bend, VIOLET, width * 0.6F, life - 2);
			ray(mc, level, bend, end, VIOLET, width * 0.45F, life - 2);
		}
	}

	private static void ray(Minecraft mc, ClientLevel level, Vec3 from, Vec3 to, int color, float width, int life) {
		Vec3 d = to.subtract(from);
		mc.particleEngine.add(new LightParticle(level, from.x, from.y, from.z,
			new LightOption(LightOption.RAY, color, (float) d.x, (float) d.y, (float) d.z, width, 0, 0, 0, Math.max(3, life))));
	}
}
