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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * 41x25x41 warded Leyline Sanctum arena with entrance Trial Gate, 4 interactive Leyline Conduits,
 * glowing leyline channels, and a rear Trial Treasury reliquary.
 */
public final class LeylineSanctumPiece extends DungeonPiece {
	public static final int SIZE = 41;
	public static final int HEIGHT = 25;
	public static final int FLOOR = 6;

	public LeylineSanctumPiece(int x, int y, int z, Direction facing) {
		super(TrialSites.LEYLINE_SANCTUM, x, y, z, SIZE, HEIGHT, SIZE, facing);
	}

	public LeylineSanctumPiece(CompoundTag tag) {
		super(TrialSites.LEYLINE_SANCTUM, tag);
	}

	public BlockPos gatePosition() {
		return localPosition(20, FLOOR + 1, 3);
	}

	public BlockPos arenaCenter() {
		return localPosition(20, FLOOR + 1, 20);
	}

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
		BlockState brick = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		BlockState gold = Blocks.GILDED_BLACKSTONE.defaultBlockState();
		BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();

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
						boolean gateOpening = Math.abs(dx) <= 2 && z <= 3;
						if (!gateOpening) {
							fill(level, bb, x, FLOOR + 1, z, x, FLOOR + 5, z, tile);
							set(level, bb, gold, x, FLOOR + 6, z);
						}
					}
				}
			}
		}

		// Gatehouse entrance arch & Trial Gate pedestal
		fill(level, bb, 17, FLOOR + 1, 0, 17, FLOOR + 6, 3, gold);
		fill(level, bb, 23, FLOOR + 1, 0, 23, FLOOR + 6, 3, gold);
		fill(level, bb, 17, FLOOR + 6, 0, 23, FLOOR + 7, 3, tile);
		set(level, bb, CoopTrialContent.TRIAL_GATE.defaultBlockState(), 20, FLOOR + 1, 3);

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
