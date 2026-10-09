package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A stone-footed windmill with four cloth sails, a milling floor and a loft. The west field drinks from a pond;
 * the east field is dry, and a button on the old well drops water into its empty cell to wake it.
 */
public final class WindmillPiece extends FarmPiece {
	public WindmillPiece(int x, int z, Direction facing) { super(FarmSites.WINDMILL, x, 0, z, 23, 19, 23, facing); }
	public WindmillPiece(CompoundTag tag) { super(FarmSites.WINDMILL, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 10, 0, 0, 12, 0, 9, s(Blocks.DIRT_PATH));
		// The fields: west one wet and ripening, east one dry and barely sprouting.
		for (int x = 1; x <= 21; x++) for (int z = 6; z <= 14; z++) {
			if (x >= 8 && x <= 14) continue;
			boolean wet = x < 8;
			set(level, bb, farmland(wet), x, 0, z);
			boolean bare = !wet && x >= 17 && x <= 19 && z >= 9 && z <= 11;
			if (!bare) set(level, bb, crop(Blocks.WHEAT, wet ? 4 + noise(x, z) % 4 : noise(x, z) % 3), x, 1, z);
		}
		set(level, bb, s(Blocks.WATER), 4, 0, 10);
		set(level, bb, AIR, 4, 1, 10);
		// The well: a dispenser holding water over the dry field's empty cell, and a button to wake it.
		set(level, bb, AIR, 18, 0, 10);
		well(level, bb, 18, 1, 10, Direction.DOWN);
		set(level, bb, facing(Blocks.STONE_BUTTON, Direction.SOUTH).setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL), 18, 1, 9);
		sign(level, bb, standingSign(), 18, 1, 5, "farm_windmill", 3);
		// The mill: stone below, spruce above, a pyramid roof.
		var cobble = s(Blocks.COBBLESTONE);
		var planks = s(Blocks.SPRUCE_PLANKS);
		fill(level, bb, 8, 0, 10, 14, 0, 16, s(Blocks.STONE_BRICKS));
		walls(level, bb, 8, 1, 10, 14, 5, 16, cobble);
		walls(level, bb, 8, 6, 10, 14, 12, 16, planks);
		for (int x : new int[]{8, 14}) for (int z : new int[]{10, 16}) fill(level, bb, x, 1, z, x, 12, z, s(Blocks.STRIPPED_SPRUCE_LOG));
		for (int i = 0; i < 4; i++) fill(level, bb, 7 + i, 13 + i, 9 + i, 15 - i, 13 + i, 17 - i, s(Blocks.DARK_OAK_PLANKS));
		set(level, bb, s(Blocks.LIGHTNING_ROD.weathering().unaffected()), 11, 17, 13);
		for (int y : new int[]{3, 9}) {
			set(level, bb, s(Blocks.GLASS), 8, y, 13);
			set(level, bb, s(Blocks.GLASS), 14, y, 13);
			set(level, bb, s(Blocks.GLASS), 11, y, 16);
		}
		door(level, bb, Blocks.SPRUCE_DOOR, 11, 1, 10, Direction.NORTH, true);
		// The milling floor.
		set(level, bb, floorAttached(Blocks.GRINDSTONE, Direction.EAST), 9, 1, 11);
		set(level, bb, s(Blocks.COMPOSTER), 13, 1, 11);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 9, 1, 13);
		set(level, bb, s(Blocks.HAY_BLOCK), 9, 1, 15);
		set(level, bb, s(Blocks.HAY_BLOCK), 13, 1, 14);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 13, 1, 15);
		set(level, bb, hangingLantern(), 11, 5, 12);
		// The loft, up the ladder by the back wall.
		fill(level, bb, 9, 6, 11, 13, 6, 15, planks);
		ladder(level, bb, 11, 1, 7, 15, Direction.SOUTH);
		chest(level, bb, 10, 7, 12, FarmSites.WINDMILL_LOFT, Direction.EAST, 1);
		set(level, bb, s(Blocks.HAY_BLOCK), 13, 7, 15);
		set(level, bb, s(Blocks.HAY_BLOCK), 12, 7, 15);
		// The sails: a hub on the front wall and four cloth arms turning one way.
		fill(level, bb, 11, 10, 8, 11, 10, 9, s(Blocks.SPRUCE_LOG).setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
		var arm = s(Blocks.SPRUCE_FENCE);
		var cloth = s(Blocks.WOOL.white());
		fill(level, bb, 11, 11, 8, 11, 17, 8, arm);
		fill(level, bb, 12, 13, 8, 12, 17, 8, cloth);
		fill(level, bb, 12, 10, 8, 18, 10, 8, arm);
		fill(level, bb, 13, 9, 8, 18, 9, 8, cloth);
		fill(level, bb, 11, 4, 8, 11, 9, 8, arm);
		fill(level, bb, 10, 4, 8, 10, 8, 8, cloth);
		fill(level, bb, 4, 10, 8, 10, 10, 8, arm);
		fill(level, bb, 4, 11, 8, 9, 11, 8, cloth);
		// Sacks of the last harvest by the door.
		set(level, bb, s(Blocks.HAY_BLOCK), 15, 1, 16);
		set(level, bb, s(Blocks.HAY_BLOCK), 16, 1, 16);
		set(level, bb, s(Blocks.HAY_BLOCK), 15, 2, 16);
		set(level, bb, s(Blocks.COMPOSTER), 7, 1, 16);
	}
}
