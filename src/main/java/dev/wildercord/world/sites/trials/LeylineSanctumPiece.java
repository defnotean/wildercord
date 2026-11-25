package dev.wildercord.world.sites.trials;

import dev.wildercord.trials.CoopTrialContent;
import dev.wildercord.trials.VaelkhorRules;
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
 * 41x32x41 monumental warded Leyline Sanctum arena with grand stepped entrance,
 * tall ruined perimeter arches, 4 floating chained obelisks, 4 interactive Leyline Conduits,
 * and a rear Trial Treasury reliquary.
 */
public final class LeylineSanctumPiece extends DungeonPiece implements TrialArenaPiece {
	public static final int SIZE = 41;
	public static final int HEIGHT = 32;
	public static final int FLOOR = 6;

	public LeylineSanctumPiece(int x, int y, int z, Direction facing) {
		super(TrialSites.LEYLINE_SANCTUM, x, y, z, SIZE, HEIGHT, SIZE, facing);
	}

	public LeylineSanctumPiece(CompoundTag tag) {
		super(TrialSites.LEYLINE_SANCTUM, tag);
	}

	@Override
	public String trialKind() {
		return "leyline";
	}

	@Override
	public String structureId() {
		return TrialSites.SANCTUM_ID;
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
		var piece = new LeylineSanctumPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(),
			Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos center = piece.localPosition(20, 0, 20);
		int sea = c.chunkGenerator().getSeaLevel();
		int surface = surface(c, center);
		if (surface <= sea) {
			return Optional.empty();
		}
		int minH = surface;
		int maxH = surface;
		for (int[] off : new int[][] {{-14, -14}, {14, -14}, {-14, 14}, {14, 14}}) {
			int h = surface(c, center.offset(off[0], 0, off[1]));
			if (h <= sea - 2) {
				return Optional.empty();
			}
			minH = Math.min(minH, h);
			maxH = Math.max(maxH, h);
		}
		if (maxH - minH > 6) {
			return Optional.empty();
		}
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

		BlockState tile = Blocks.DEEPSLATE_TILES.defaultBlockState();
		BlockState crackedTile = Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
		BlockState brick = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		BlockState gold = Blocks.GILDED_BLACKSTONE.defaultBlockState();
		BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
		BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
		BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
		Block cyanBanner = Blocks.WALL_BANNER.cyan();
		Block purpleBanner = Blocks.WALL_BANNER.purple();

		// Circular arena basin, carved leyline inlays, and clear combat dome
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				int dx = x - 20;
				int dz = z - 20;
				double r = Math.hypot(dx, dz);
				if (r <= 20.5) {
					fill(level, bb, x, 0, z, x, FLOOR - 1, z, tile);
					BlockState floorBlock = ((Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && r < 18.0) ? lantern
						: (Math.abs(r - 12.0) < 0.9 ? amethyst : (r > 17.5 ? gold : brick));
					set(level, bb, floorBlock, x, FLOOR, z);
					fill(level, bb, x, FLOOR + 1, z, x, HEIGHT - 2, z, AIR);
					if (r >= 19.2) {
						boolean gateOpening = Math.abs(dx) <= 2 && z <= 4;
						if (!gateOpening) {
							fill(level, bb, x, FLOOR + 1, z, x, FLOOR + 5, z, tile);
							set(level, bb, gold, x, FLOOR + 6, z);
						}
					}
				}
			}
		}

		// Grand stepped entrance threshold & tall twin gatehouse towers
		for (int step = 0; step < 3; step++) {
			int z = step;
			int h = FLOOR - 2 + step;
			fill(level, bb, 18, 0, z, 22, h - 1, z, tile);
			for (int x = 18; x <= 22; x++) {
				set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, false), x, h, z);
			}
		}
		for (int px : new int[]{16, 24}) {
			fill(level, bb, px - 1, FLOOR + 1, 0, px + 1, FLOOR + 10, 3, tile);
			pillar(level, bb, px, 0, FLOOR + 1, FLOOR + 12, gold, brick, gold);
			set(level, bb, lantern, px, FLOOR + 13, 0);
			set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, px < 20 ? Direction.EAST : Direction.WEST, true),
				px + (px < 20 ? 1 : -1), FLOOR + 9, 2);
		}
		fill(level, bb, 17, FLOOR + 9, 1, 23, FLOOR + 10, 2, tile);
		fill(level, bb, 18, FLOOR + 11, 1, 22, FLOOR + 11, 2, gold);
		crenellate(level, bb, 16, 0, 24, 3, FLOOR + 12, gold);
		hangingLantern(level, bb, 18, FLOOR + 8, 2, 1, true);
		hangingLantern(level, bb, 22, FLOOR + 8, 2, 1, true);
		banner(level, bb, cyanBanner, 17, FLOOR + 5, 4, Direction.NORTH);
		banner(level, bb, purpleBanner, 23, FLOOR + 5, 4, Direction.NORTH);
		set(level, bb, CoopTrialContent.TRIAL_GATE.defaultBlockState(), 20, FLOOR + 1, 3);

		// Eight tall ruined perimeter arches around the outer rim (r ~ 19, outside combat floor)
		int[][] archPositions = {
			{3, 13}, {3, 27}, {37, 13}, {37, 27},
			{13, 37}, {27, 37}, {11, 5}, {29, 5}
		};
		for (int i = 0; i < archPositions.length; i++) {
			int ax = archPositions[i][0];
			int az = archPositions[i][1];
			int topY = FLOOR + (i % 2 == 0 ? 13 : 11);
			pillar(level, bb, ax, az, FLOOR + 1, topY, gold, tile, gold);
			set(level, bb, i % 3 == 0 ? amethyst : lantern, ax, topY + 1, az);
			int ix = Integer.compare(20, ax);
			int iz = Integer.compare(20, az);
			if (ax + ix >= 0 && ax + ix < SIZE && az + iz >= 0 && az + iz < SIZE) {
				set(level, bb, gold, ax + ix, topY - 1, az + iz);
				set(level, bb, crackedTile, ax + ix, topY, az + iz);
				Block inwardBanner = (i % 2 == 0) ? cyanBanner : purpleBanner;
				Direction faceDir = Math.abs(20 - ax) >= Math.abs(20 - az)
					? (ax < 20 ? Direction.EAST : Direction.WEST)
					: (az < 20 ? Direction.NORTH : Direction.SOUTH);
				banner(level, bb, inwardBanner, ax + ix, FLOOR + 6, az + iz, faceDir);
			}
		}

		// Connect rear ruined arches along z=37 into a high broken crown over the Treasury
		fill(level, bb, 13, FLOOR + 12, 37, 17, FLOOR + 12, 37, tile);
		fill(level, bb, 23, FLOOR + 12, 37, 27, FLOOR + 12, 37, tile);
		fill(level, bb, 18, FLOOR + 13, 37, 22, FLOOR + 13, 37, gold);
		hangingLantern(level, bb, 20, FLOOR + 12, 37, 2, true);

		// Four suspended chained Leyline Obelisks floating high in the corners
		for (int[] ob : new int[][]{{5, 5}, {35, 5}, {5, 35}, {35, 35}}) {
			int ox = ob[0];
			int oz = ob[1];
			set(level, bb, chain, ox, FLOOR + 12, oz);
			hangingLantern(level, bb, ox, FLOOR + 11, oz, 1, true);
			set(level, bb, gold, ox, FLOOR + 13, oz);
			fill(level, bb, ox, FLOOR + 14, oz, ox, FLOOR + 17, oz, crying);
			set(level, bb, amethyst, ox, FLOOR + 18, oz);
			set(level, bb, lantern, ox, FLOOR + 19, oz);
			set(level, bb, gold, ox, FLOOR + 20, oz);
			fill(level, bb, ox, FLOOR + 21, oz, ox, FLOOR + 22, oz, chain);
			for (int[] side : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
				set(level, bb, crackedTile, ox + side[0], FLOOR + 15, oz + side[1]);
			}
		}

		// Four interactive Leyline Conduit pillars around the arena
		for (int[] off : VaelkhorRules.CONDUIT_OFFSETS) {
			int px = 20 + off[0];
			int pz = 20 + off[1];
			set(level, bb, gold, px, FLOOR + 1, pz);
			set(level, bb, CoopTrialContent.LEYLINE_CONDUIT.defaultBlockState(), px, FLOOR + 2, pz);
			set(level, bb, lantern, px, FLOOR + 3, pz);
		}

		// Rear Treasury shrine & instanced Trial Treasury block
		fill(level, bb, 18, FLOOR + 1, 35, 22, FLOOR + 1, 38, brick);
		set(level, bb, amethyst, 18, FLOOR + 2, 37);
		set(level, bb, amethyst, 22, FLOOR + 2, 37);
		set(level, bb, CoopTrialContent.TRIAL_TREASURY.defaultBlockState(), 20, FLOOR + 1, 36);
		chest(level, bb, 21, FLOOR + 1, 36, TrialSites.TREASURY_LOOT, Direction.SOUTH, 20261125);
	}
}
