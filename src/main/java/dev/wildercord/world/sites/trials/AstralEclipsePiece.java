package dev.wildercord.world.sites.trials;

import dev.wildercord.trials.CoopTrialContent;
import dev.wildercord.trials.NyxaraRules;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * 41x32x41 warded Astral Eclipse Vault housing Nyxara, 4 alignable Astral Prisms,
 * starlit quartz colonnades, suspended astrolabe spires, and a rear Trial Treasury.
 */
public final class AstralEclipsePiece extends DungeonPiece implements TrialArenaPiece {
	public static final int SIZE = 41;
	public static final int HEIGHT = 32;
	public static final int FLOOR = 6;

	public AstralEclipsePiece(int x, int y, int z, Direction facing) {
		super(TrialSites.ASTRAL_ECLIPSE, x, y, z, SIZE, HEIGHT, SIZE, facing);
	}

	public AstralEclipsePiece(CompoundTag tag) {
		super(TrialSites.ASTRAL_ECLIPSE, tag);
	}

	@Override
	public String trialKind() {
		return "eclipse";
	}

	@Override
	public String structureId() {
		return TrialSites.ECLIPSE_ID;
	}

	@Override
	public BlockPos gatePosition() {
		return localPosition(20, FLOOR + 1, 3);
	}

	@Override
	public BlockPos arenaCenter() {
		return localPosition(20, FLOOR + 1, 20);
	}

	@Override
	public BlockPos treasuryPosition() {
		return localPosition(20, FLOOR + 1, 36);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(0, FLOOR, 0, SIZE - 1, HEIGHT - 1, SIZE - 1));
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		var piece = new AstralEclipsePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(),
			Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos center = piece.localPosition(20, 0, 20);
		int sea = c.chunkGenerator().getSeaLevel();
		int surface = surface(c, center);
		if (surface <= sea) return Optional.empty();
		int minH = surface, maxH = surface;
		for (int[] off : new int[][]{{-14, -14}, {14, -14}, {-14, 14}, {14, 14}}) {
			int h = surface(c, center.offset(off[0], 0, off[1]));
			if (h <= sea - 2) return Optional.empty();
			minH = Math.min(minH, h);
			maxH = Math.max(maxH, h);
		}
		if (maxH - minH > 6) return Optional.empty();
		piece.move(0, minH - FLOOR, 0);
		return Optional.of(new Structure.GenerationStub(piece.arenaCenter(), b -> b.addPiece(piece)));
	}

	private static int surface(Structure.GenerationContext c, BlockPos pos) {
		return c.chunkGenerator().getFirstOccupiedHeight(pos.getX(), pos.getZ(),
			Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
		BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);

		BlockState quartz = Blocks.QUARTZ_BRICKS.defaultBlockState();
		BlockState smoothQuartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
		BlockState obsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
		BlockState tile = Blocks.DEEPSLATE_TILES.defaultBlockState();
		BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
		BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState starGlass = Blocks.STAINED_GLASS.purple().defaultBlockState();
		BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
		Block purpleBanner = Blocks.WALL_BANNER.purple();
		Block cyanBanner = Blocks.WALL_BANNER.cyan();

		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				int dx = x - 20;
				int dz = z - 20;
				double r = Math.hypot(dx, dz);
				if (r <= 20.5) {
					fill(level, bb, x, 0, z, x, FLOOR - 2, z, tile);
					boolean starLine = (Math.abs(dx) == Math.abs(dz) || dx == 0 || dz == 0) && r < 18.0 && r > 3.5;
					if (starLine) {
						set(level, bb, lantern, x, FLOOR - 1, z);
						set(level, bb, starGlass, x, FLOOR, z);
					} else {
						set(level, bb, obsidian, x, FLOOR - 1, z);
						BlockState top = Math.abs(r - 12.0) < 0.9 ? amethyst : (r > 17.5 ? quartz : smoothQuartz);
						set(level, bb, top, x, FLOOR, z);
					}
					fill(level, bb, x, FLOOR + 1, z, x, HEIGHT - 2, z, AIR);
					if (r >= 19.2 && !(Math.abs(dx) <= 2 && z <= 4)) {
						fill(level, bb, x, FLOOR + 1, z, x, FLOOR + 5, z, tile);
						set(level, bb, quartz, x, FLOOR + 6, z);
					}
				}
			}
		}

		// Stepped astral gatehouse & Trial Gate pedestal
		for (int step = 0; step < 3; step++) {
			int z = step;
			int h = FLOOR - 2 + step;
			fill(level, bb, 18, 0, z, 22, h - 1, z, tile);
			for (int x = 18; x <= 22; x++) {
				set(level, bb, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH, false), x, h, z);
			}
		}
		for (int px : new int[]{16, 24}) {
			fill(level, bb, px - 1, FLOOR + 1, 0, px + 1, FLOOR + 10, 3, quartz);
			pillar(level, bb, px, 0, FLOOR + 1, FLOOR + 12, amethyst, quartz, amethyst);
			set(level, bb, lantern, px, FLOOR + 13, 0);
		}
		fill(level, bb, 17, FLOOR + 9, 1, 23, FLOOR + 10, 2, quartz);
		fill(level, bb, 18, FLOOR + 11, 1, 22, FLOOR + 11, 2, amethyst);
		hangingLantern(level, bb, 18, FLOOR + 8, 2, 1, true);
		hangingLantern(level, bb, 22, FLOOR + 8, 2, 1, true);
		banner(level, bb, purpleBanner, 17, FLOOR + 5, 4, Direction.NORTH);
		banner(level, bb, cyanBanner, 23, FLOOR + 5, 4, Direction.NORTH);
		set(level, bb, CoopTrialContent.TRIAL_GATE.defaultBlockState(), 20, FLOOR + 1, 3);

		// Perimeter astral colonnades & suspended astrolabe spires
		for (int[] col : new int[][]{{4, 13}, {4, 27}, {36, 13}, {36, 27}, {13, 36}, {27, 36}}) {
			pillar(level, bb, col[0], col[1], FLOOR + 1, FLOOR + 13, obsidian, quartz, amethyst);
			set(level, bb, lantern, col[0], FLOOR + 14, col[1]);
		}
		for (int[] sp : new int[][]{{6, 6}, {34, 6}, {6, 34}, {34, 34}}) {
			fill(level, bb, sp[0], FLOOR + 13, sp[1], sp[0], FLOOR + 19, sp[1], obsidian);
			set(level, bb, amethyst, sp[0], FLOOR + 16, sp[1]);
			fill(level, bb, sp[0], FLOOR + 20, sp[1], sp[0], FLOOR + 22, sp[1], chain);
			hangingLantern(level, bb, sp[0], FLOOR + 12, sp[1], 1, true);
		}

		// Four interactive Astral Prism pylons
		for (int[] off : NyxaraRules.PRISM_OFFSETS) {
			int px = 20 + off[0];
			int pz = 20 + off[1];
			set(level, bb, amethyst, px, FLOOR + 1, pz);
			set(level, bb, CoopTrialContent.ASTRAL_PRISM.defaultBlockState(), px, FLOOR + 2, pz);
			set(level, bb, lantern, px, FLOOR + 3, pz);
		}

		// Rear Treasury shrine
		fill(level, bb, 18, FLOOR + 1, 35, 22, FLOOR + 1, 38, smoothQuartz);
		set(level, bb, amethyst, 18, FLOOR + 2, 37);
		set(level, bb, amethyst, 22, FLOOR + 2, 37);
		set(level, bb, CoopTrialContent.TRIAL_TREASURY.defaultBlockState(), 20, FLOOR + 1, 36);
		chest(level, bb, 21, FLOOR + 1, 36, TrialSites.ECLIPSE_TREASURY_LOOT, Direction.SOUTH, 20261127);
	}
}
