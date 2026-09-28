package dev.wildercord.client.fx;

import dev.wildercord.cast.Archivist;
import dev.wildercord.content.ArchiveLecternBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Archive, alive: all of it made on the client, so nothing is streamed and it costs the server
 * nothing. Inside an Archive (found by its lectern's block entity, then only below ground and on the
 * halls' level):
 * <ul>
 *   <li>faint shafts of light fall from the dome's lamps and the hanging soul lanterns, with dust
 *       drifting through them, and motes of dust hang wherever the halls are lit;</li>
 *   <li>braziers flicker: a wavering glow and rising embers over a lit campfire, the odd dying
 *       ember over a cold one;</li>
 *   <li>the magic circle inlaid in the arena floor glows softly (its two rings and its hexagram),
 *       brighter while the Archivist is abroad;</li>
 *   <li>now and then a page turns among the shelves, something whispers, a chime sounds by the
 *       lectern.</li>
 * </ul>
 * The blocks around the player are read a slice a tick, and every kind of light has a small budget.
 */
public final class ArchiveAmbience {
	private ArchiveAmbience() {}

	/** How far the halls are searched for lamps, braziers and shelves, in blocks either side. */
	private static final int SCAN = 14;
	private static final int FLOOR_LIFE = 400;

	private static final Glimmer.Budget DUST = new Glimmer.Budget(60);
	private static final Glimmer.Budget SHAFTS = new Glimmer.Budget(8);
	private static final Glimmer.Budget FIRE = new Glimmer.Budget(48);

	/** Lamps seen nearby (ceiling shroomlights, hanging soul lanterns), and the tick each may next let a shaft fall. */
	private static final Map<BlockPos, Integer> LAMPS = new HashMap<>();
	private static final Set<BlockPos> BRAZIERS = new HashSet<>();
	private static final Set<BlockPos> SHELVES = new HashSet<>();
	/** The shafts falling right now, for the dust to drift through. */
	private static final List<Shaft> FALLING = new ArrayList<>();

	private record Shaft(Vec3 top, float length, float width, int color, int until) {}

	private static @Nullable ClientLevel seen;
	/** The Archive Lectern at the heart of the Archive the player is in or near. */
	private static @Nullable BlockPos heart;
	/** Which way the arena's hexagram points: 0 not known yet, 1 along z, 2 along x, 3 not found. */
	private static int hexagram;
	private static int floorEnds;
	private static @Nullable RingGlow floorGlow;
	private static boolean fight;
	private static int clock;
	private static int slice;
	private static int nextSound;

	public static void tick(Minecraft mc) {
		DUST.tick();
		SHAFTS.tick();
		FIRE.tick();
		clock++;
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level != seen) {
			seen = level;
			forget();
		}
		if (level == null || player == null) {
			return;
		}
		if (clock % 40 == 0) {
			BlockPos found = findHeart(level, player);
			if (found == null ? heart != null : !found.equals(heart)) {
				forget();
				heart = found;
			}
			fight = heart != null && !level.getEntitiesOfClass(Archivist.class, new AABB(heart).inflate(40)).isEmpty();
		}
		if (heart == null) {
			return;
		}
		RandomSource random = player.getRandom();
		arenaFloor(mc, level, player);
		if (!inside(level, player)) {
			return;
		}
		scan(level, player);
		lamps(mc, level, player, random);
		dust(mc, level, player, random);
		braziers(mc, level, player, random);
		sounds(level, player, random);
	}

	private static void forget() {
		heart = null;
		hexagram = 0;
		floorEnds = 0;
		floorGlow = null;
		fight = false;
		LAMPS.clear();
		BRAZIERS.clear();
		SHELVES.clear();
		FALLING.clear();
	}

	// ------------------------------------------------------------------ finding the Archive

	/** The nearest Archive Lectern in the loaded chunks around the player, or null. */
	private static @Nullable BlockPos findHeart(ClientLevel level, LocalPlayer player) {
		int pcx = player.blockPosition().getX() >> 4;
		int pcz = player.blockPosition().getZ() >> 4;
		BlockPos best = null;
		double bestDistance = 72 * 72;
		for (int cx = pcx - 5; cx <= pcx + 5; cx++) {
			for (int cz = pcz - 5; cz <= pcz + 5; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof ArchiveLecternBlockEntity)) {
						continue;
					}
					BlockPos pos = entity.getBlockPos();
					double dx = pos.getX() + 0.5 - player.getX();
					double dz = pos.getZ() + 0.5 - player.getZ();
					if (dx * dx + dz * dz < bestDistance && Math.abs(pos.getY() - player.getY()) < 40) {
						bestDistance = dx * dx + dz * dz;
						best = pos;
					}
				}
			}
		}
		return best;
	}

	/** In the Archive's halls: near its heart, on its level, and out of the sky's light. */
	private static boolean inside(ClientLevel level, LocalPlayer player) {
		return Math.abs(player.getX() - heart.getX()) < 64 && Math.abs(player.getZ() - heart.getZ()) < 64
			&& player.getY() > heart.getY() - 4 && player.getY() < heart.getY() + 12
			&& level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition())) == 0;
	}

	/** Reads one slice of the blocks around the player, remembering lamps, braziers and shelves. */
	private static void scan(ClientLevel level, LocalPlayer player) {
		int width = SCAN * 2 + 1;
		int dx = slice % width - SCAN;
		if (slice % width == 0) {
			prune(player);
		}
		slice++;
		BlockPos centre = player.blockPosition();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dz = -SCAN; dz <= SCAN; dz++) {
			for (int dy = -3; dy <= 12; dy++) {
				pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
				BlockState state = level.getBlockState(pos);
				if (state.isAir()) {
					continue;
				}
				if (state.is(Blocks.SHROOMLIGHT) ? level.getBlockState(pos.below()).isAir()
						: state.is(Blocks.SOUL_LANTERN) && state.getValue(LanternBlock.HANGING)) {
					LAMPS.putIfAbsent(pos.immutable(), clock + 20 + level.getRandom().nextInt(200));
				} else if (state.getBlock() instanceof CampfireBlock) {
					BRAZIERS.add(pos.immutable());
				} else if ((state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF)) && SHELVES.size() < 64) {
					SHELVES.add(pos.immutable());
				}
			}
		}
	}

	private static void prune(LocalPlayer player) {
		double far = (SCAN + 6) * (SCAN + 6);
		LAMPS.keySet().removeIf(p -> p.distToCenterSqr(player.position()) > far);
		BRAZIERS.removeIf(p -> p.distToCenterSqr(player.position()) > far);
		SHELVES.removeIf(p -> p.distToCenterSqr(player.position()) > far);
	}

	// ------------------------------------------------------------------ light

	/** Now and then a lamp lets a faint shaft of light fall to the floor below it. */
	private static void lamps(Minecraft mc, ClientLevel level, LocalPlayer player, RandomSource random) {
		FALLING.removeIf(s -> s.until <= clock);
		for (Iterator<Map.Entry<BlockPos, Integer>> it = LAMPS.entrySet().iterator(); it.hasNext() && SHAFTS.hasRoom(); ) {
			Map.Entry<BlockPos, Integer> lamp = it.next();
			if (lamp.getValue() > clock) {
				continue;
			}
			BlockPos pos = lamp.getKey();
			BlockState state = level.getBlockState(pos);
			boolean dome = state.is(Blocks.SHROOMLIGHT);
			if (!dome && !state.is(Blocks.SOUL_LANTERN)) {
				it.remove();
				continue;
			}
			double top = dome ? pos.getY() : pos.getY() + 0.35;
			int floor = pos.getY() - 1;
			while (floor > pos.getY() - 18 && level.getBlockState(new BlockPos(pos.getX(), floor, pos.getZ())).isAir()) {
				floor--;
			}
			float length = (float) (top - (floor + 1));
			int life = 150 + random.nextInt(90);
			lamp.setValue(clock + life + 60 + random.nextInt(240));
			if (length < 1.5F) {
				continue;
			}
			int color = dome ? 0xFFD9A8 : 0xB4E4FF;
			float width = dome ? 1.1F : 0.75F;
			Vec3 at = new Vec3(pos.getX() + 0.5, top, pos.getZ() + 0.5);
			mc.particleEngine.add(Glimmer.shaft(level, at, color, width, length, dome ? 0.075F : 0.055F, life));
			SHAFTS.spend(life);
			FALLING.add(new Shaft(at, length, width, color, clock + life));
		}
	}

	/** Dust: mostly drifting through the shafts, the rest hanging wherever the halls are lit. */
	private static void dust(Minecraft mc, ClientLevel level, LocalPlayer player, RandomSource random) {
		if (!DUST.hasRoom() || random.nextInt(3) == 0) {
			return;
		}
		int life = 70 + random.nextInt(80);
		if (!FALLING.isEmpty() && random.nextInt(5) < 3) {
			Shaft shaft = FALLING.get(random.nextInt(FALLING.size()));
			Vec3 at = shaft.top.add((random.nextDouble() - 0.5) * shaft.width * 0.8, -random.nextDouble() * shaft.length,
				(random.nextDouble() - 0.5) * shaft.width * 0.8);
			mc.particleEngine.add(Glimmer.mote(level, at, pale(shaft.color), 0.05F + random.nextFloat() * 0.03F, 0.55F, life,
				0, -0.002 - random.nextDouble() * 0.003, 0, 0.0015F));
			DUST.spend(life);
			return;
		}
		Vec3 at = player.position().add((random.nextDouble() - 0.5) * 14, random.nextDouble() * 4, (random.nextDouble() - 0.5) * 14);
		BlockPos pos = BlockPos.containing(at);
		if (!level.getBlockState(pos).isAir() || level.getBrightness(LightLayer.BLOCK, pos) < 7) {
			return;
		}
		mc.particleEngine.add(Glimmer.mote(level, at, 0xFFF1DA, 0.045F + random.nextFloat() * 0.025F, 0.32F, life,
			0, -0.001, 0, 0.002F));
		DUST.spend(life);
	}

	private static int pale(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + (255 - r) * 2 / 3) << 16) | ((g + (255 - g) * 2 / 3) << 8) | (b + (255 - b) * 2 / 3);
	}

	/** Firelight wavering over lit braziers, embers rising; a cold brazier only glints now and then. */
	private static void braziers(Minecraft mc, ClientLevel level, LocalPlayer player, RandomSource random) {
		for (Iterator<BlockPos> it = BRAZIERS.iterator(); it.hasNext(); ) {
			BlockPos pos = it.next();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CampfireBlock)) {
				it.remove();
				continue;
			}
			if (pos.distToCenterSqr(player.position()) > 22 * 22) {
				continue;
			}
			boolean soul = state.is(Blocks.SOUL_CAMPFIRE);
			boolean lit = state.getValue(CampfireBlock.LIT);
			Vec3 fire = Vec3.atBottomCenterOf(pos).add(0, 0.55, 0);
			int beat = (int) Math.floorMod(clock + pos.asLong(), 40L);
			if (lit) {
				if (beat == 0 && FIRE.hasRoom()) {
					mc.particleEngine.add(Glimmer.flicker(level, fire, soul ? 0x70D8FF : 0xFF9A48, 2.6F, 0.28F, 46));
					FIRE.spend(46);
				}
				if (random.nextFloat() < 0.35F && FIRE.hasRoom()) {
					int life = 18 + random.nextInt(18);
					int[] embers = soul ? new int[] {0xB8F4FF, 0x6CD6FF, 0x40B8F0} : new int[] {0xFFD070, 0xFF9030, 0xFF6420};
					Vec3 at = fire.add((random.nextDouble() - 0.5) * 0.5, -0.1, (random.nextDouble() - 0.5) * 0.5);
					mc.particleEngine.add(Glimmer.mote(level, at, embers[random.nextInt(3)], 0.05F + random.nextFloat() * 0.035F, 0.95F, life,
						0, 0.035 + random.nextDouble() * 0.03, 0, 0.012F));
					FIRE.spend(life);
				}
			} else {
				if (beat == 0 && random.nextInt(2) == 0 && FIRE.hasRoom()) {
					mc.particleEngine.add(Glimmer.flicker(level, fire.add(0, -0.3, 0), 0xB04420, 1.2F, 0.06F, 46));
					FIRE.spend(46);
				}
				if (random.nextFloat() < 0.04F && FIRE.hasRoom()) {
					int life = 14 + random.nextInt(12);
					Vec3 at = fire.add((random.nextDouble() - 0.5) * 0.5, -0.3, (random.nextDouble() - 0.5) * 0.5);
					mc.particleEngine.add(Glimmer.mote(level, at, 0xD05424, 0.045F, 0.6F, life, 0, 0.012, 0, 0.004F));
					FIRE.spend(life);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the arena floor

	/** Keeps the inlaid circle glowing while the arena is near, each glow fading into the next. */
	private static void arenaFloor(Minecraft mc, ClientLevel level, LocalPlayer player) {
		if (floorGlow != null) {
			floorGlow.setStrength(fight ? 1.8F : 1.0F);
		}
		if (heart.distToCenterSqr(player.position()) > 48 * 48 || clock < floorEnds - 20) {
			return;
		}
		if (hexagram == 0) {
			hexagram = findHexagram(level, heart);
		}
		Vec3 centre = Vec3.atBottomCenterOf(heart).add(0, 0.03, 0);
		RingGlow glow = new RingGlow(level, centre, 0, 0, FLOOR_LIFE)
			.ring(9, 0.95F, 0xB48CFF, 0.22F)
			.ring(5, 0.8F, 0xF5C46A, 0.2F);
		if (hexagram == 1 || hexagram == 2) {
			for (int t = 0; t < 2; t++) {
				for (int k = 0; k < 3; k++) {
					double a1 = Math.PI / 2 + t * Math.PI + k * Math.PI * 2 / 3;
					double a2 = a1 + Math.PI * 2 / 3;
					float[] p = hexPoint(a1, hexagram);
					float[] q = hexPoint(a2, hexagram);
					glow.line(p[0], p[1], q[0], q[1], 0.6F, 0x9A50E8, 0.16F);
				}
			}
		}
		glow.setStrength(fight ? 1.8F : 1.0F);
		mc.particleEngine.add(glow);
		floorGlow = glow;
		floorEnds = clock + FLOOR_LIFE;
	}

	/** A point of the hexagram (as the Archive lays it out) in world (x, z) from the circle's centre. */
	private static float[] hexPoint(double angle, int orientation) {
		float a = (float) (Math.cos(angle) * 9);
		float b = (float) (Math.sin(angle) * 9);
		return orientation == 1 ? new float[] {a, b} : new float[] {b, a};
	}

	/**
	 * Which way the arena's hexagram lies (the Archive can face any way): the orientation whose
	 * lines best match the crying obsidian in the floor, or 3 when neither does.
	 */
	private static int findHexagram(ClientLevel level, BlockPos centre) {
		int best = 3;
		double bestScore = 0.6;
		for (int orientation = 1; orientation <= 2; orientation++) {
			int total = 0;
			int found = 0;
			for (int i = -9; i <= 9; i++) {
				for (int k = -9; k <= 9; k++) {
					double d = Math.sqrt(i * i + k * k);
					if (d < 1.6 || d > 9.2 || Math.abs(d - 9) < 0.55 || Math.abs(d - 5) < 0.5) {
						continue;
					}
					if (onHexagram(orientation == 1 ? i : k, orientation == 1 ? k : i)) {
						total++;
						if (level.getBlockState(centre.offset(i, -1, k)).is(Blocks.CRYING_OBSIDIAN)) {
							found++;
						}
					}
				}
			}
			double score = total == 0 ? 0 : found / (double) total;
			if (score > bestScore) {
				bestScore = score;
				best = orientation;
			}
		}
		return best;
	}

	/** As the Archive's floor is laid: within 0.6 blocks of a line of the two triangles (in its own x, z). */
	private static boolean onHexagram(double x, double z) {
		for (int t = 0; t < 2; t++) {
			for (int k = 0; k < 3; k++) {
				double a1 = Math.PI / 2 + t * Math.PI + k * Math.PI * 2 / 3;
				double a2 = a1 + Math.PI * 2 / 3;
				double x1 = Math.cos(a1) * 9, z1 = Math.sin(a1) * 9;
				double x2 = Math.cos(a2) * 9, z2 = Math.sin(a2) * 9;
				double dx = x2 - x1, dz = z2 - z1;
				double s = Math.max(0, Math.min(1, ((x - x1) * dx + (z - z1) * dz) / (dx * dx + dz * dz)));
				double px = x1 + s * dx - x, pz = z1 + s * dz - z;
				if (px * px + pz * pz < 0.36) {
					return true;
				}
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ sound

	/** Pages among the shelves, whispers in the dark, a chime by the lectern: one every few seconds, quietly. */
	private static void sounds(ClientLevel level, LocalPlayer player, RandomSource random) {
		if (clock < nextSound) {
			return;
		}
		nextSound = clock + 90 + random.nextInt(170);
		int roll = random.nextInt(10);
		if (roll < 5 && !SHELVES.isEmpty()) {
			BlockPos shelf = new ArrayList<>(SHELVES).get(random.nextInt(SHELVES.size()));
			level.playLocalSound(shelf.getX() + 0.5, shelf.getY() + 0.5, shelf.getZ() + 0.5, SoundEvents.BOOK_PAGE_TURN, SoundSource.AMBIENT,
				0.35F, 0.75F + random.nextFloat() * 0.35F, false);
		} else if (roll < 8) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 5 + random.nextDouble() * 5;
			level.playLocalSound(player.getX() + Math.cos(a) * r, player.getY() + 1 + random.nextDouble() * 2, player.getZ() + Math.sin(a) * r,
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 0.16F, 0.5F + random.nextFloat() * 0.25F, false);
		} else if (heart.distToCenterSqr(player.position()) < 18 * 18) {
			level.playLocalSound(heart.getX() + 0.5, heart.getY() + 1, heart.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT,
				0.45F, 0.6F + random.nextFloat() * 0.3F, false);
		} else {
			level.playLocalSound(player.getX() + (random.nextDouble() - 0.5) * 10, player.getY() + 1, player.getZ() + (random.nextDouble() - 0.5) * 10,
				SoundEvents.BOOK_PAGE_TURN, SoundSource.AMBIENT, 0.25F, 0.6F + random.nextFloat() * 0.3F, false);
		}
	}
}
