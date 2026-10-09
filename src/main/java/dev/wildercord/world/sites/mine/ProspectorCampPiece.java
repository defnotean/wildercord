package dev.wildercord.world.sites.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A prospector's camp on the mesas: two canvas tents, a sluice box of still water, a panning cauldron, a campfire and a
 * lookout tower flying a red flag that can be seen across the badlands. Peaceful. Local y 0 is the trodden ground.
 */
public final class ProspectorCampPiece extends MinePiece {
	static final int WIDTH = 25, HEIGHT = 15, DEPTH = 25;

	public ProspectorCampPiece(int x, int y, int z, Direction facing) {
		super(MineSites.PROSPECTOR_CAMP, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public ProspectorCampPiece(CompoundTag tag) {
		super(MineSites.PROSPECTOR_CAMP, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new ProspectorCampPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos entrance = piece.getWorldPos(12, 0, 0), middle = piece.getWorldPos(12, 0, 12);
		if (wet(c, entrance) || wet(c, middle)) return Optional.empty();
		int y = ground(c, middle);
		piece.move(0, y - 1, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), y, entrance.getZ()), b -> b.addPiece(piece)));
	}

	private void tent(WorldGenLevel level, BoundingBox bb, int x0, BlockState canvas) {
		for (int k = 0; k <= 3; k++) {
			fill(level, bb, x0 + k, 1 + k, 3, x0 + k, 1 + k, 9, canvas);
			fill(level, bb, x0 + 6 - k, 1 + k, 3, x0 + 6 - k, 1 + k, 9, canvas);
		}
		for (int k = 0; k <= 2; k++) fill(level, bb, x0 + k + 1, 1 + k, 9, x0 + 5 - k, 1 + k, 9, canvas);
		set(level, bb, Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState(), x0 + 2, 1, 6);
		set(level, bb, Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState(), x0 + 2, 1, 7);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState plank = s(Blocks.SPRUCE_PLANKS), log = s(Blocks.SPRUCE_LOG);
		fill(level, bb, 0, 1, 0, 24, 14, 24, AIR);
		for (int x = 1; x <= 23; x++) for (int z = 1; z <= 23; z++) {
			int n = noise(x, z);
			set(level, bb, s(n < 40 ? Blocks.COARSE_DIRT : n < 75 ? Blocks.TERRACOTTA : n < 90 ? Blocks.PACKED_MUD : Blocks.COBBLESTONE), x, 0, z);
		}
		// Two tents, the crew's chest in the first.
		tent(level, bb, 2, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
		tent(level, bb, 16, Blocks.WOOL.pick(DyeColor.LIGHT_GRAY).defaultBlockState());
		chest(level, bb, 5, 1, 8, MineSites.CAMP, Direction.SOUTH, 471);
		barrel(level, bb, 19, 1, 8, null, 0);
		// The campfire ring.
		set(level, bb, s(Blocks.CAMPFIRE), 12, 1, 6);
		for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) set(level, bb, s(Blocks.COBBLESTONE_SLAB), 12 + o[0] * 2, 1, 6 + o[1] * 2);
		// The sluice: a plank box of still water, riffled with cobble, and the panning cauldron beside it.
		fill(level, bb, 10, 0, 12, 14, 2, 22, plank);
		fill(level, bb, 12, 1, 13, 12, 1, 21, s(Blocks.WATER));
		fill(level, bb, 12, 2, 13, 12, 2, 21, AIR);
		fill(level, bb, 11, 2, 12, 11, 2, 22, AIR);
		fill(level, bb, 13, 2, 12, 13, 2, 22, AIR);
		fill(level, bb, 10, 2, 12, 10, 2, 22, s(Blocks.SPRUCE_FENCE));
		fill(level, bb, 14, 2, 12, 14, 2, 22, s(Blocks.SPRUCE_FENCE));
		// Riffles of cobble, not gravel: the sluice floor holds the water, so it must not fall.
		for (int z = 14; z <= 20; z += 3) set(level, bb, s(Blocks.COBBLESTONE), 12, 0, z);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 2), 8, 1, 14);
		barrel(level, bb, 8, 1, 16, null, 0);
		set(level, bb, s(Blocks.GOLD_ORE), 8, 1, 18);
		// The lookout: four posts, a ladder, a railed deck with the second chest, and the flag.
		for (int x : new int[] {18, 21}) for (int z : new int[] {15, 18}) fill(level, bb, x, 1, z, x, 8, z, log);
		fill(level, bb, 19, 1, 15, 20, 7, 15, plank);
		fill(level, bb, 18, 8, 15, 21, 8, 18, plank);
		fill(level, bb, 19, 1, 16, 19, 8, 16, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
		for (int x = 18; x <= 21; x++) for (int z = 15; z <= 18; z++) if (x == 18 || x == 21 || z == 15 || z == 18) set(level, bb, s(Blocks.SPRUCE_FENCE), x, 9, z);
		chest(level, bb, 20, 9, 17, MineSites.CAMP, Direction.WEST, 472);
		fill(level, bb, 21, 10, 18, 21, 13, 18, s(Blocks.SPRUCE_FENCE));
		fill(level, bb, 22, 12, 18, 23, 13, 18, Blocks.WOOL.pick(DyeColor.RED).defaultBlockState());
		set(level, bb, s(Blocks.LANTERN), 18, 10, 15);
	}
}
