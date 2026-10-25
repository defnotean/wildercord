package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A caravan resting on the road: three hoop-covered wagons around a stone-ringed campfire, a striped trader's stall,
 * a trader who stays with the camp and two pack llamas. Two wagons carry the caravan's stores; the trader trades as any wandering trader does.
 */
public final class CaravanCampPiece extends TravelPiece {
	/** Wagon corners (low x, low z); each wagon is 3 wide and 5 long, its open end toward low z. */
	public static final int[][] WAGONS = {{3, 3}, {17, 3}, {10, 12}};

	public CaravanCampPiece(int x, int y, int z, Direction facing) { super(TravelSites.CARAVAN_PIECE, x, y, z, 23, 8, 19, facing); }
	public CaravanCampPiece(CompoundTag tag) { super(TravelSites.CARAVAN_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		for (int x = 1; x <= 21; x++) for (int z = 1; z <= 17; z++) {
			int n = noise(x, z);
			footing(level, bb, x, z, x, z, dist(x, z, 11, 9) < 4 || n < 20 ? b(Blocks.COARSE_DIRT) : n < 30 ? b(Blocks.DIRT_PATH) : GRASS, DIRT, 7);
		}
		// The fire, ringed by log benches.
		set(level, bb, b(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 11, 1, 9);
		BlockState bench = b(Blocks.STRIPPED_OAK_LOG);
		fill(level, bb, 9, 1, 7, 13, 1, 7, bench.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, bb, 8, 1, 8, 8, 1, 10, bench.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		fill(level, bb, 14, 1, 8, 14, 1, 10, bench.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		// A stone hearth ring around the fire and a cauldron kept warm beside it.
		for (int x = 10; x <= 12; x++) for (int z = 8; z <= 10; z++) if (x != 11 || z != 9) set(level, bb, noise(x, z) < 40 ? b(Blocks.MOSSY_COBBLESTONE) : COBBLE, x, 0, z);
		set(level, bb, b(Blocks.CAULDRON), 13, 1, 11);
		// The trader's stall: a striped awning on posts over rugs, a counter either side of the way in.
		for (int[] p : new int[][]{{8, 3}, {14, 3}, {8, 6}, {14, 6}}) fill(level, bb, p[0], 1, p[1], p[0], 3, p[1], b(Blocks.SPRUCE_FENCE));
		Block[] stripes = {Blocks.WOOL.pick(DyeColor.RED), Blocks.WOOL.pick(DyeColor.WHITE)};
		for (int x = 8; x <= 14; x++) fill(level, bb, x, 4, 3, x, 4, 6, b(stripes[x % 2]));
		for (int x = 8; x <= 14; x++) set(level, bb, b(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.SOUTH)
			.setValue(TrapDoorBlock.OPEN, true), x, 3, 2);
		for (int x = 9; x <= 13; x++) for (int z = 4; z <= 6; z++) set(level, bb, b(Blocks.CARPET.pick(x == 11 ? DyeColor.ORANGE : DyeColor.BROWN)), x, 1, z);
		for (int x : new int[]{9, 13}) { set(level, bb, b(Blocks.BARREL), x, 1, 3); set(level, bb, b(Blocks.SPRUCE_SLAB), x, 2, 3); }
		set(level, bb, b(Blocks.BARREL), 10, 1, 3); set(level, bb, b(Blocks.BARREL), 12, 1, 3);
		hangingLantern(level, bb, 11, 3, 4, 0, false);
		// Firewood stacked by the second wagon.
		fill(level, bb, 19, 1, 13, 20, 1, 14, b(Blocks.OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		set(level, bb, b(Blocks.OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 19, 2, 13);
		Block[] covers = {Blocks.WOOL.pick(DyeColor.WHITE), Blocks.WOOL.pick(DyeColor.BROWN), Blocks.WOOL.pick(DyeColor.LIGHT_GRAY)};
		for (int i = 0; i < WAGONS.length; i++) wagon(level, bb, WAGONS[i][0], WAGONS[i][1], b(covers[(i + variant()) % 3]), i);
		// Hay and crates by the third wagon, a lantern post at the road.
		set(level, bb, b(Blocks.HAY_BLOCK), 15, 1, 14); set(level, bb, b(Blocks.HAY_BLOCK), 15, 1, 15); set(level, bb, b(Blocks.HAY_BLOCK), 15, 2, 14);
		set(level, bb, b(Blocks.BARREL), 7, 1, 14); set(level, bb, b(Blocks.BARREL), 7, 1, 15);
		fill(level, bb, 11, 1, 1, 11, 2, 1, b(Blocks.OAK_FENCE)); set(level, bb, b(Blocks.LANTERN), 11, 3, 1);
		resident(level, bb, EntityTypes.WANDERING_TRADER, 11, 1, 5, 6);
		resident(level, bb, EntityTypes.LLAMA, 6, 1, 11, 6);
		resident(level, bb, EntityTypes.LLAMA, 17, 1, 11, 6);
	}

	/** Wheels, a plank bed, a wool cover open at the low-z end; the first two carry a chest just inside. */
	private void wagon(WorldGenLevel level, BoundingBox bb, int x, int z, BlockState cover, int index) {
		BlockState wheel = b(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		for (int dz : new int[]{1, 3}) { set(level, bb, wheel, x, 1, z + dz); set(level, bb, wheel, x + 2, 1, z + dz); }
		fill(level, bb, x, 2, z, x + 2, 2, z + 4, b(Blocks.SPRUCE_PLANKS));
		// Sideboards, then a hooped cover: two courses on each side, one along the crown.
		fill(level, bb, x, 3, z + 1, x, 4, z + 4, cover);
		fill(level, bb, x + 2, 3, z + 1, x + 2, 4, z + 4, cover);
		fill(level, bb, x + 1, 3, z + 4, x + 1, 4, z + 4, cover);
		fill(level, bb, x + 1, 5, z + 1, x + 1, 5, z + 4, cover);
		for (int side : new int[]{x, x + 2}) set(level, bb, stair(Blocks.SPRUCE_STAIRS, side == x ? Direction.EAST : Direction.WEST, true), side, 2, z);
		BlockState rib = b(Blocks.STRIPPED_DARK_OAK_LOG);
		fill(level, bb, x, 3, z + 1, x, 4, z + 1, rib);
		fill(level, bb, x + 2, 3, z + 1, x + 2, 4, z + 1, rib);
		set(level, bb, wheel, x + 1, 5, z + 1);
		hangingLantern(level, bb, x + 1, 4, z + 2, 0, false);
		set(level, bb, b(Blocks.SPRUCE_FENCE), x + 1, 2, z + 5);
		set(level, bb, b(Blocks.SPRUCE_FENCE), x + 1, 1, z + 6);
		if (index < 2) chest(level, bb, x + 1, 3, z + 1, TravelSites.loot(TravelSites.CARAVAN), Direction.SOUTH, 6 + index);
		else set(level, bb, b(Blocks.BARREL), x + 1, 3, z + 1);
	}
}
