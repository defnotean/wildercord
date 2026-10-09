package dev.wildercord.world.sites.mine;

import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A basalt foundry in the deltas: a blackstone hall on basalt posts, two lava troughs sunk in its floor, crucibles, a line of
 * blast furnaces and a smoking chimney, held by a Runebound wither skeleton with a blaze and a magma cube. The troughs are
 * closed on every side, so their lava never spreads. Local y 0 is the floor.
 */
public final class BasaltFoundryPiece extends MinePiece {
	static final int WIDTH = 19, HEIGHT = 14, DEPTH = 21;

	public BasaltFoundryPiece(int x, int y, int z, Direction facing) {
		super(MineSites.BASALT_FOUNDRY, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public BasaltFoundryPiece(CompoundTag tag) {
		super(MineSites.BASALT_FOUNDRY, tag);
	}

	/** Stands on the deltas' own floor: the highest dry ground with room above it, below the Nether's roof. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new BasaltFoundryPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos middle = piece.getWorldPos(9, 0, 10), entrance = piece.getWorldPos(9, 0, 0);
		var column = c.chunkGenerator().getBaseColumn(middle.getX(), middle.getZ(), c.heightAccessor(), c.randomState());
		for (int y = 96; y >= 33; y--) {
			BlockState ground = column.getBlock(y);
			if (ground.isAir() || !ground.getFluidState().isEmpty()) continue;
			boolean room = true;
			for (int up = 1; up <= 6 && room; up++) room = column.getBlock(y + up).isAir();
			if (!room) continue;
			piece.move(0, y, 0);
			return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), y + 1, entrance.getZ()), b -> b.addPiece(piece)));
		}
		return Optional.empty();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState black = s(Blocks.BLACKSTONE), bricks = s(Blocks.POLISHED_BLACKSTONE_BRICKS), basalt = s(Blocks.POLISHED_BASALT);
		fill(level, bb, 0, 1, 0, 18, 13, 20, AIR);
		footing(level, bb, 1, 1, 17, 19, 0, s(Blocks.BASALT));
		room(level, bb, 1, 0, 1, 17, 9, 19, black);
		fill(level, bb, 1, 0, 1, 17, 0, 19, bricks);
		for (int x : new int[] {1, 17}) for (int z : new int[] {1, 7, 13, 19}) fill(level, bb, x, 0, z, x, 10, z, basalt);
		for (int z = 4; z <= 16; z += 6) {
			fill(level, bb, 1, 4, z, 1, 6, z + 1, s(Blocks.IRON_BARS));
			fill(level, bb, 17, 4, z, 17, 6, z + 1, s(Blocks.IRON_BARS));
		}
		fill(level, bb, 7, 1, 1, 11, 5, 1, AIR);
		fill(level, bb, 6, 6, 1, 12, 6, 1, s(Blocks.GILDED_BLACKSTONE));
		// The chimney over the back corner, smoking.
		room(level, bb, 12, 9, 14, 16, 13, 18, bricks);
		set(level, bb, s(Blocks.CAMPFIRE), 14, 12, 16);
		set(level, bb, AIR, 14, 13, 16);
		// Two lava troughs: brick below and on every side, so the lava stays put.
		for (int x : new int[] {4, 14}) {
			fill(level, bb, x - 1, -1, 4, x + 1, -1, 14, bricks);
			fill(level, bb, x, 0, 5, x, 0, 13, s(Blocks.LAVA));
			fill(level, bb, x - 1, 1, 5, x - 1, 1, 13, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
			fill(level, bb, x + 1, 1, 5, x + 1, 1, 13, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
		}
		set(level, bb, s(Blocks.LAVA_CAULDRON), 7, 1, 9);
		set(level, bb, s(Blocks.LAVA_CAULDRON), 11, 1, 9);
		set(level, bb, s(Blocks.CAULDRON), 9, 1, 7);
		for (int x = 4; x <= 14; x += 2) if (x != 10) set(level, bb, s(Blocks.BLAST_FURNACE).setValue(BlastFurnaceBlock.FACING, Direction.SOUTH), x, 1, 18);
		chest(level, bb, 10, 1, 18, MineSites.FOUNDRY, Direction.SOUTH, 451);
		chest(level, bb, 2, 1, 16, MineSites.FOUNDRY, Direction.EAST, 452);
		set(level, bb, s(Blocks.ANVIL), 9, 1, 14);
		for (int x : new int[] {6, 12}) for (int z : new int[] {6, 13}) hangingLantern(level, bb, x, 8, z);
		guard(level, bb, EntityTypes.WITHER_SKELETON, 9, 1, 12, List.of(Runes.BOLT, Runes.EMBER), true);
		sentry(level, bb, EntityTypes.BLAZE, 12, 2, 4);
		sentry(level, bb, EntityTypes.MAGMA_CUBE, 6, 1, 15);
	}
}
