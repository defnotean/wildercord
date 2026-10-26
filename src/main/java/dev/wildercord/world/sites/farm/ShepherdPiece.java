package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A shepherd's steading on the hillside: a long stone-footed hut under a steep spruce roof with a stone chimney at its
 * gable, a loom, a smoker and a chest inside; a dry-stone pen of sheep beside it with a byre over the hay and a gate to
 * shut; a round stone fold out on the slope, and a cairn with a lamp for whoever is out after the flock at night.
 */
public final class ShepherdPiece extends FarmPiece {
	public ShepherdPiece(int x, int z, Direction facing) { super(FarmSites.SHEPHERD, x, 0, z, 27, 12, 23, facing); }
	public ShepherdPiece(CompoundTag tag) { super(FarmSites.SHEPHERD, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		clearCanopy(level, bb);
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 7, 0, 0, 7, 0, 12, s(Blocks.DIRT_PATH));
		fill(level, bb, 8, 0, 2, 20, 0, 2, s(Blocks.DIRT_PATH));
		for (int z = 0; z <= 12; z++) if (noise(6, z) < 40) set(level, bb, s(Blocks.DIRT_PATH), noise(6, z) < 20 ? 6 : 8, 0, z);
		hut(level, bb);
		pen(level, bb);
		fold(level, bb, 3, 5);
		// A cairn on the slope with a lamp on it, and boulders about.
		fill(level, bb, 24, 1, 21, 24, 2, 21, s(Blocks.MOSSY_COBBLESTONE));
		set(level, bb, s(Blocks.COBBLESTONE_WALL), 24, 3, 21);
		set(level, bb, s(Blocks.LANTERN), 24, 4, 21);
		set(level, bb, s(Blocks.MOSSY_COBBLESTONE_SLAB), 23, 1, 21);
		for (int[] b : new int[][]{{1, 1}, {12, 7}, {1, 11}, {13, 21}, {25, 1}}) {
			set(level, bb, s(Blocks.MOSSY_COBBLESTONE), b[0], 1, b[1]);
			if (noise(b[0], b[1]) < 50) set(level, bb, s(Blocks.MOSSY_COBBLESTONE_SLAB), b[0] + 1, 1, b[1]);
		}
	}

	/** The long hut: a stone course, spruce over it on log frames, a steep gable roof, a chimney on the east gable. */
	private void hut(WorldGenLevel level, BoundingBox bb) {
		Palette stone = Palette.of(Blocks.COBBLESTONE, 5, Blocks.MOSSY_COBBLESTONE, 3, Blocks.STONE, 1);
		fill(level, bb, 2, 0, 13, 12, 0, 19, s(Blocks.COBBLESTONE));
		fill(level, bb, 3, 0, 14, 11, 0, 18, s(Blocks.SPRUCE_PLANKS));
		walls(level, bb, 2, 1, 13, 12, 1, 19, stone);
		walls(level, bb, 2, 2, 13, 12, 3, 19, s(Blocks.SPRUCE_PLANKS));
		for (int x : new int[]{2, 7, 12}) for (int z : new int[]{13, 19}) fill(level, bb, x, 1, z, x, 3, z, s(Blocks.SPRUCE_LOG));
		fill(level, bb, 2, 1, 16, 2, 3, 16, s(Blocks.SPRUCE_LOG));
		// A beam along each eave.
		fill(level, bb, 2, 4, 13, 12, 4, 13, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, bb, 2, 4, 19, 12, 4, 19, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
		for (int i = 0; i < 4; i++) {
			int y = 4 + i;
			fill(level, bb, 1, y, 12 + i, 13, y, 12 + i, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
			fill(level, bb, 1, y, 20 - i, 13, y, 20 - i, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
			if (i > 0) {
				fill(level, bb, 2, y, 13 + i, 2, y, 19 - i, s(Blocks.SPRUCE_PLANKS));
				fill(level, bb, 12, y, 13 + i, 12, y, 19 - i, s(Blocks.SPRUCE_PLANKS));
			}
		}
		fill(level, bb, 1, 7, 16, 13, 7, 16, s(Blocks.DARK_OAK_PLANKS));
		fill(level, bb, 1, 8, 16, 13, 8, 16, s(Blocks.DARK_OAK_SLAB));
		set(level, bb, s(Blocks.SPRUCE_FENCE), 1, 9, 16);
		// Eaves corbelled out over both long walls.
		for (int x = 2; x <= 12; x++) {
			set(level, bb, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), x, 3, 12);
			set(level, bb, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), x, 3, 20);
		}
		// The chimney: a broad stone stack against the east gable, narrowing above the eaves.
		fill(level, bb, 13, 0, 15, 13, 5, 17, stone);
		fill(level, bb, 13, 6, 16, 13, 9, 16, stone);
		set(level, bb, s(Blocks.COBBLESTONE_SLAB), 13, 10, 16);
		set(level, bb, s(Blocks.CAMPFIRE), 13, 10, 16);
		fill(level, bb, 12, 1, 16, 12, 1, 16, s(Blocks.COBBLESTONE));
		// Windows with shutters on the long walls, glass in the west gable.
		for (int x : new int[]{4, 10}) {
			set(level, bb, s(Blocks.GLASS_PANE), x, 2, 13);
			set(level, bb, s(Blocks.GLASS_PANE), x, 2, 19);
			for (int dx : new int[]{-1, 1}) {
				set(level, bb, facing(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH).setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true), x + dx, 2, 12);
				set(level, bb, facing(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH).setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true), x + dx, 2, 20);
			}
			set(level, bb, s(Blocks.FLOWER_POT), x, 1, 12);
		}
		set(level, bb, s(Blocks.GLASS_PANE), 2, 5, 16);
		set(level, bb, s(Blocks.GLASS_PANE), 2, 2, 15); set(level, bb, s(Blocks.GLASS_PANE), 2, 2, 17);
		door(level, bb, Blocks.SPRUCE_DOOR, 7, 1, 13, Direction.NORTH, true);
		set(level, bb, s(Blocks.SPRUCE_PLANKS), 7, 3, 13);
		// The porch: posts, a slab roof and lanterns either side of the door.
		for (int x : new int[]{5, 9}) fill(level, bb, x, 1, 11, x, 2, 11, s(Blocks.SPRUCE_FENCE));
		fill(level, bb, 5, 3, 11, 9, 3, 11, s(Blocks.SPRUCE_SLAB));
		set(level, bb, s(Blocks.LANTERN), 6, 1, 12); set(level, bb, s(Blocks.LANTERN), 8, 1, 12);
		// Within: the loom and spinning corner, the hearth, the chest, fleece, a rug and hanging lamps.
		set(level, bb, facing(Blocks.LOOM, Direction.EAST), 3, 1, 14);
		set(level, bb, s(Blocks.WOOL.white()), 3, 1, 18);
		set(level, bb, s(Blocks.WOOL.pick(DyeColor.LIGHT_GRAY)), 3, 2, 18);
		set(level, bb, s(Blocks.BARREL), 4, 1, 18);
		set(level, bb, facing(Blocks.SMOKER, Direction.WEST), 11, 1, 16);
		set(level, bb, s(Blocks.HAY_BLOCK), 11, 1, 14);
		chest(level, bb, 11, 1, 18, FarmSites.SHEPHERD_HUT, Direction.WEST, 1);
		fill(level, bb, 6, 1, 16, 8, 1, 17, s(Blocks.CARPET.white()));
		set(level, bb, s(Blocks.CARPET.pick(DyeColor.BROWN)), 7, 1, 16);
		set(level, bb, hangingLantern(), 5, 6, 16);
		set(level, bb, hangingLantern(), 9, 6, 16);
		// Shrubs under the windows, fleece stacked against the west wall.
		for (int z : new int[]{14, 18}) set(level, bb, s(z == 14 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA), 1, 1, z);
		for (int x : new int[]{4, 10}) set(level, bb, s(Blocks.FLOWERING_AZALEA), x, 1, 20);
		set(level, bb, s(Blocks.WOOL.white()), 1, 1, 17); set(level, bb, s(Blocks.WOOL.pick(DyeColor.BROWN)), 1, 1, 15);
	}

	/** The pen: a dry-stone wall with lamp piers, a gate shut on the flock, a trough, and a gabled byre over the hay. */
	private void pen(WorldGenLevel level, BoundingBox bb) {
		int x0 = 15, z0 = 4, x1 = 25, z1 = 20;
		walls(level, bb, x0, 1, z0, x1, 1, z1, Palette.of(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 3, Blocks.STONE, 1));
		for (int x = x0; x <= x1; x++) for (int z : new int[]{z0, z1}) set(level, bb, s(noise(x, z) < 45 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL), x, 2, z);
		for (int z = z0; z <= z1; z++) for (int x : new int[]{x0, x1}) set(level, bb, s(noise(x, z) < 45 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL), x, 2, z);
		for (int x : new int[]{x0, x1}) for (int z : new int[]{z0, z1}) fill(level, bb, x, 1, z, x, 3, z, s(Blocks.STONE_BRICKS));
		// The gate between lamp piers, opening on the track.
		fill(level, bb, 19, 1, z0, 21, 2, z0, AIR);
		for (int x : new int[]{18, 22}) {
			fill(level, bb, x, 1, z0, x, 3, z0, s(Blocks.STONE_BRICKS));
			set(level, bb, s(Blocks.STONE_BRICK_SLAB), x, 4, z0);
			set(level, bb, s(Blocks.LANTERN), x, 5, z0);
		}
		for (int x = 19; x <= 21; x++) set(level, bb, facing(Blocks.SPRUCE_FENCE_GATE, Direction.SOUTH), x, 1, z0);
		fill(level, bb, 20, 0, 0, 20, 0, z0, s(Blocks.DIRT_PATH));
		// A trough along the west wall, hay by the byre, grass the flock hasn't got to.
		fill(level, bb, 16, 0, 8, 16, 0, 11, s(Blocks.WATER));
		for (int z = 8; z <= 11; z++) set(level, bb, s(Blocks.SPRUCE_SLAB), 17, 1, z);
		for (int x = 16; x <= 24; x++) for (int z = 5; z <= 19; z++) if (noise(x, z) < 15 && x != 16) set(level, bb, s(Blocks.SHORT_GRASS), x, 1, z);
		sentry(level, bb, EntityTypes.SHEEP, 19, 1, 8);
		sentry(level, bb, EntityTypes.SHEEP, 22, 1, 11);
		sentry(level, bb, EntityTypes.SHEEP, 19, 1, 13);
		sentry(level, bb, EntityTypes.SHEEP, 23, 1, 7);
		// The byre in the far corner, open toward the pen.
		for (int[] p : new int[][]{{19, 15}, {24, 15}, {19, 19}, {24, 19}}) fill(level, bb, p[0], 1, p[1], p[0], 3, p[1], s(Blocks.SPRUCE_LOG));
		fill(level, bb, 20, 1, 19, 23, 3, 19, s(Blocks.SPRUCE_PLANKS));
		for (int x = 18; x <= 25; x++) {
			set(level, bb, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false), x, 4, 14);
			fill(level, bb, x, 4, 15, x, 4, 19, s(Blocks.DARK_OAK_PLANKS));
			set(level, bb, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false), x, 4, 20);
			set(level, bb, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false), x, 5, 15);
			fill(level, bb, x, 5, 16, x, 5, 18, s(Blocks.DARK_OAK_PLANKS));
			set(level, bb, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false), x, 5, 19);
			set(level, bb, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false), x, 6, 16);
			set(level, bb, s(Blocks.DARK_OAK_SLAB), x, 6, 17);
			set(level, bb, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false), x, 6, 18);
		}
		fill(level, bb, 21, 1, 17, 23, 1, 18, s(Blocks.HAY_BLOCK));
		set(level, bb, s(Blocks.HAY_BLOCK), 22, 2, 18);
		hangingLantern(level, bb, 21, 3, 16, 0, false);
	}

	/** A round fold of piled stone out on the slope, a gap on the uphill side and a post with a crook beside it. */
	private void fold(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
			double d = Math.sqrt(dx * dx + dz * dz);
			if (d < 2.2 || d > 3.2 || (dx == 3 && Math.abs(dz) <= 0) || cx + dx < 0 || cz + dz < 0) continue;
			int x = cx + dx, z = cz + dz;
			set(level, bb, s(noise(x, z) < 50 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE), x, 1, z);
			if (noise(x, 3, z) < 55) set(level, bb, s(noise(x, z) < 40 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL), x, 2, z);
		}
		set(level, bb, s(Blocks.SPRUCE_FENCE), cx + 3, 1, cz + 1);
		set(level, bb, s(Blocks.SPRUCE_FENCE), cx + 3, 2, cz + 1);
		set(level, bb, s(Blocks.HAY_BLOCK), cx, 1, cz);
	}
}
