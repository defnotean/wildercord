package dev.wildercord.client.fx;

import dev.wildercord.player.Spellbooks;
import dev.wildercord.world.LeyLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ley lines, as a Cord-wearer sees them: ribbons of pale violet light flowing along each vein just
 * above the ground, following its bends, with now and then a mote rising from it. Worked out on
 * the client from the ley seed the server sends at login, so nothing is streamed and only players
 * with a Cord see them. A few dozen ribbons at most are out at once.
 */
public final class LeyMotes {
	private LeyMotes() {}

	private static long seed;
	private static boolean known;

	/** How far from the player ribbons appear, in blocks either way. */
	private static final double RANGE = 30;
	private static final Glimmer.Budget RIBBONS = new Glimmer.Budget(36);
	private static final Glimmer.Budget MOTES = new Glimmer.Budget(24);
	/** Extra ribbons while a mana storm makes the lines surge. */
	private static final Glimmer.Budget SURGING = new Glimmer.Budget(24);
	/** Ribbons flow the way of this (roughly east-north-east), so a whole line runs one way. */
	private static final double FLOW_X = 0.85;
	private static final double FLOW_Z = 0.53;

	public static void setSeed(long leySeed) {
		seed = leySeed;
		known = true;
	}

	/** Forgets the seed on leaving a world, so the last world's lines don't show in the next before its seed comes. */
	public static void forget() {
		seed = 0;
		known = false;
		CROSSINGS.clear();
		SHIMMERING.clear();
		nearCrossings = List.of();
	}

	// ------------------------------------------------------------------ ley crossings

	/** How far off a ley crossing's shimmer shows (it's a place to find, so further than the ribbons). */
	private static final double CROSSING_RANGE = 56;
	/** How long one shimmer's rings last before the next takes over. */
	private static final int SHIMMER_LIFE = 160;
	/** Each chunk's crossing heart, worked out once ({@link #NO_CROSSING} where there's none): crossings never move. */
	private static final java.util.Map<Long, double[]> CROSSINGS = new java.util.HashMap<>();
	private static final double[] NO_CROSSING = new double[0];
	/** The crossings shimmering now, and the tick each one's rings end. */
	private static final java.util.Map<Long, Integer> SHIMMERING = new java.util.HashMap<>();
	private static final Glimmer.Budget CROSSING_MOTES = new Glimmer.Budget(30);
	private static List<double[]> nearCrossings = List.of();
	private static int clock;

	/**
	 * Where two ley lines cross, a place of power: rings of pale light turning flat on the ground round its heart,
	 * a faint column of light over it and motes of mana lifting off. Seen from further than the ribbons, so a
	 * crossing can be found by following a line to it.
	 */
	private static void crossings(Minecraft mc, ClientLevel level, LocalPlayer player, RandomSource random) {
		clock++;
		CROSSING_MOTES.tick();
		if (clock % 20 == 0) {
			nearCrossings = findCrossings(player);
			if (CROSSINGS.size() > 4096) {
				CROSSINGS.clear();
			}
		}
		for (double[] heart : nearCrossings) {
			long key = (long) Math.floor(heart[0]) << 32 ^ (long) Math.floor(heart[1]) & 0xFFFFFFFFL;
			Double y = ground(level, heart[0], heart[1]);
			if (y == null || Math.abs(y - player.getY()) > 32) {
				continue;
			}
			Vec3 at = new Vec3(heart[0], y - 0.1, heart[1]);
			Integer ends = SHIMMERING.get(key);
			if (ends == null || clock >= ends - 20) {
				mc.particleEngine.add(new RingGlow(level, at.add(0, 0.04, 0), 0, 0, SHIMMER_LIFE)
					.ring(2.4F, 0.09F, 0xE6DEFF, 0.8F)
					.ring(2.4F, 0.5F, 0xA890F0, 0.22F)
					.ring(1.2F, 0.05F, 0xFFFFFF, 0.55F)
					.line(-2.4F, 0, 2.4F, 0, 0.05F, 0xD8CCFF, 0.45F)
					.line(0, -2.4F, 0, 2.4F, 0.05F, 0xD8CCFF, 0.45F)
					.beads(4, 0.045F, 0xFFFFFF));
				mc.particleEngine.add(Glimmer.shaft(level, at.add(0, 7, 0), 0xC8B8FF, 0.9F, 7.0F, 0.16F, SHIMMER_LIFE));
				SHIMMERING.put(key, clock + SHIMMER_LIFE);
			}
			if (random.nextInt(3) == 0 && CROSSING_MOTES.hasRoom()) {
				double a = random.nextDouble() * Math.PI * 2;
				double r = random.nextDouble() * 2.2;
				int life = 50 + random.nextInt(30);
				mc.particleEngine.add(Glimmer.mote(level, at.add(Math.cos(a) * r, 0.15, Math.sin(a) * r), random.nextBoolean() ? 0xE6DEFF : 0xB8A0FF, 0.13F,
					0.6F, life, 0, 0.03 + random.nextDouble() * 0.02, 0, 0.004F));
				CROSSING_MOTES.spend(life);
			}
		}
		SHIMMERING.values().removeIf(end -> end < clock - 40);
	}

	/** The ley crossings within reach, from the chunks round the player (each chunk worked out once). */
	private static List<double[]> findCrossings(LocalPlayer player) {
		List<double[]> found = new ArrayList<>();
		int pcx = (int) Math.floor(player.getX()) >> 4;
		int pcz = (int) Math.floor(player.getZ()) >> 4;
		int reach = (int) Math.ceil(CROSSING_RANGE / 16);
		for (int cx = pcx - reach; cx <= pcx + reach; cx++) {
			for (int cz = pcz - reach; cz <= pcz + reach; cz++) {
				long key = (long) cx << 32 ^ cz & 0xFFFFFFFFL;
				int x = cx;
				int z = cz;
				double[] heart = CROSSINGS.computeIfAbsent(key, k -> {
					double[] h = LeyLines.crossingIn(seed, x, z);
					return h == null ? NO_CROSSING : h;
				});
				if (heart.length == 0 || Math.hypot(heart[0] - player.getX(), heart[1] - player.getZ()) > CROSSING_RANGE) {
					continue;
				}
				// A crossing on a chunk's edge shows in both: keep the first.
				boolean twin = false;
				for (double[] other : found) {
					twin |= Math.hypot(other[0] - heart[0], other[1] - heart[1]) < 8;
				}
				if (!twin) {
					found.add(heart);
				}
			}
		}
		return found;
	}

	public static boolean known() {
		return known;
	}

	public static long seed() {
		return seed;
	}

	/** Ley strength where the player stands: 0 to 1 (0 until the seed arrives). */
	public static double strengthAt(LocalPlayer player) {
		return known && player.level().dimension() == Level.OVERWORLD ? LeyLines.strength(seed, player.getX(), player.getZ()) : 0;
	}

	public static void tick(Minecraft mc) {
		RIBBONS.tick();
		MOTES.tick();
		SURGING.tick();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (!known || player == null || level == null || player.level().dimension() != Level.OVERWORLD || Spellbooks.tier(player) == null) {
			return;
		}
		RandomSource random = player.getRandom();
		// Ley crossings shimmer (unless the server doesn't count them).
		if (dev.wildercord.cast.Climate.shownTuning().crossing() > 0) {
			crossings(mc, level, player, random);
		}
		// Under a mana storm the lines surge: more ribbons, brighter, wider and quicker.
		float storm = StormSky.surge();
		for (int i = 0; i < 24; i++) {
			Glimmer.Budget budget = RIBBONS.hasRoom() ? RIBBONS : storm > 0.2F && SURGING.hasRoom() ? SURGING : null;
			if (budget == null) {
				break;
			}
			double x = player.getX() + (random.nextDouble() - 0.5) * RANGE * 2;
			double z = player.getZ() + (random.nextDouble() - 0.5) * RANGE * 2;
			double strength = LeyLines.strength(seed, x, z);
			if (strength < 0.55 || random.nextDouble() > (0.2 + 0.25 * storm) * strength) {
				continue;
			}
			List<Vec3> path = trace(level, x, z, player.getY());
			if (path == null) {
				continue;
			}
			LeyRibbon ribbon = new LeyRibbon(level, path, (0.05F + (float) strength * 0.03F) * (1 + 0.5F * storm),
				Math.min(1.0F, (0.55F + (float) strength * 0.4F) * (1 + 0.35F * storm)), (0.06F + random.nextFloat() * 0.03F) * (1 + storm));
			mc.particleEngine.add(ribbon);
			budget.spend(ribbon.getLifetime());
			// Sometimes a mote of mana lifts off the line.
			if (random.nextInt(4) == 0 && MOTES.hasRoom()) {
				Vec3 p = path.get(random.nextInt(path.size()));
				int life = 50 + random.nextInt(40);
				mc.particleEngine.add(Glimmer.mote(level, p.add(0, 0.1, 0), random.nextBoolean() ? 0xD8CCFF : 0xA890F0, 0.12F, 0.5F, life,
					0, 0.012 + random.nextDouble() * 0.01, 0, 0.004F));
				MOTES.spend(life);
			}
		}
	}

	private static double strength(double x, double z) {
		return LeyLines.strength(seed, x, z);
	}

	/**
	 * Follows the line from ({@code x}, {@code z}) for a few blocks, keeping to its heart: points just
	 * above the ground, or null where the line is too faint, the ground too far from {@code nearY},
	 * or the land too broken to lay a ribbon on.
	 */
	private static @Nullable List<Vec3> trace(ClientLevel level, double x, double z, double nearY) {
		// The line's direction: the way along which it stays strongest, both ahead and behind.
		double best = -1;
		double angle = 0;
		for (int k = 0; k < 12; k++) {
			double a = Math.PI * k / 12;
			double dx = Math.cos(a) * 2.5;
			double dz = Math.sin(a) * 2.5;
			double score = strength(x + dx, z + dz) + strength(x - dx, z - dz);
			if (score > best) {
				best = score;
				angle = a;
			}
		}
		double dx = Math.cos(angle);
		double dz = Math.sin(angle);
		if (dx * FLOW_X + dz * FLOW_Z < 0) {
			dx = -dx;
			dz = -dz;
		}
		Double y = ground(level, x, z);
		if (y == null || Math.abs(y - nearY) > 20) {
			return null;
		}
		List<Vec3> path = new ArrayList<>();
		path.add(new Vec3(x, y, z));
		double cx = x;
		double cz = z;
		double lastY = y;
		for (int step = 0; step < 12; step++) {
			double nx = cx + dx * 0.5;
			double nz = cz + dz * 0.5;
			// Lean back toward the heart of the line.
			double side = strength(nx - dz * 0.6, nz + dx * 0.6) - strength(nx + dz * 0.6, nz - dx * 0.6);
			if (Math.abs(side) > 0.02) {
				nx += -dz * 0.25 * Math.signum(side);
				nz += dx * 0.25 * Math.signum(side);
			}
			if (strength(nx, nz) < 0.3) {
				break;
			}
			Double ny = ground(level, nx, nz);
			if (ny == null || Math.abs(ny - lastY) > 1.1) {
				break;
			}
			double len = Math.sqrt((nx - cx) * (nx - cx) + (nz - cz) * (nz - cz));
			dx = (nx - cx) / len;
			dz = (nz - cz) / len;
			cx = nx;
			cz = nz;
			lastY = ny;
			path.add(new Vec3(nx, ny, nz));
		}
		return path.size() >= 4 ? path : null;
	}

	/** Just above the ground (through leaves, onto water) at a spot, or null where it isn't loaded. */
	private static @Nullable Double ground(ClientLevel level, double x, double z) {
		int bx = (int) Math.floor(x);
		int bz = (int) Math.floor(z);
		if (!level.hasChunk(bx >> 4, bz >> 4)) {
			return null;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(bx, y - 1, bz);
		// Under a canopy the line runs on the forest floor, not over the treetops.
		if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
			for (int i = 0; i < 24 && y > level.getMinY(); i++) {
				BlockState below = level.getBlockState(pos.setY(y - 1));
				if (!below.is(BlockTags.LEAVES) && (below.isSolid() || below.liquid())) {
					break;
				}
				y--;
			}
		}
		return y + 0.18;
	}
}
