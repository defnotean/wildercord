package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
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
		walls(level, bb, 8, 1, 10, 14, 5, 16, Palette.of(Blocks.COBBLESTONE, 5, Blocks.MOSSY_COBBLESTONE, 2, Blocks.STONE_BRICKS, 3));
		walls(level, bb, 8, 6, 10, 14, 12, 16, planks);
		for (int x : new int[]{8, 14}) for (int z : new int[]{10, 16}) fill(level, bb, x, 1, z, x, 12, z, s(Blocks.STRIPPED_SPRUCE_LOG));
		// A hipped roof stepped in dark oak, stairs on every edge.
		for (int i = 0; i < 4; i++) {
			int y = 13 + i, x0 = 7 + i, x1 = 15 - i, z0 = 9 + i, z1 = 17 - i;
			fill(level, bb, x0, y, z0, x1, y, z1, s(Blocks.DARK_OAK_PLANKS));
			for (int x = x0; x <= x1; x++) {
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false), x, y, z0);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false), x, y, z1);
			}
			for (int z = z0 + 1; z < z1; z++) {
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false), x0, y, z);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false), x1, y, z);
			}
		}
		set(level, bb, s(Blocks.LIGHTNING_ROD.weathering().unaffected()), 11, 17, 13);
		// Timber beams across the plank storey, where it meets the stone and under the eaves.
		for (int y : new int[]{6, 12}) {
			for (int z : new int[]{10, 16}) fill(level, bb, 9, y, z, 13, y, z, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(BlockStateProperties.AXIS, Direction.Axis.X));
			for (int x : new int[]{8, 14}) fill(level, bb, x, y, 11, x, y, 15, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
		}
		// Corbels under the roof's overhang all round.
		for (int z = 10; z <= 16; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 7, 12, z);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 15, 12, z);
		}
		for (int x = 8; x <= 14; x++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), x, 12, 9);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), x, 12, 17);
		}
		// The miller's stage: a slab gallery round the tower on corbels, posts for a rail.
		var deck = s(Blocks.SPRUCE_SLAB);
		var post = s(Blocks.SPRUCE_FENCE);
		for (int z = 9; z <= 17; z++) {
			set(level, bb, deck, 7, 6, z); set(level, bb, deck, 15, 6, z);
			if (z % 2 == 1) { set(level, bb, post, 7, 7, z); set(level, bb, post, 15, 7, z); }
			if (z >= 10 && z <= 16 && z % 2 == 0) {
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 7, 5, z);
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 15, 5, z);
			}
		}
		for (int x = 8; x <= 14; x++) {
			set(level, bb, deck, x, 6, 9); set(level, bb, deck, x, 6, 17);
			if (x % 2 == 1) { set(level, bb, post, x, 7, 9); set(level, bb, post, x, 7, 17); }
			else {
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), x, 5, 9);
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), x, 5, 17);
			}
		}
		// Sills and hoods on the upper windows, a sill on the back one below.
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 7, 8, 13);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false), 7, 10, 13);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 15, 8, 13);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 15, 10, 13);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), 11, 8, 17);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false), 11, 10, 17);
		// A battered stone skirt round the back of the base.
		for (int x = 8; x <= 14; x++) set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false), x, 1, 17);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), 11, 2, 17);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false), 7, 1, 15);
		// The door under a pent hood, a harvest banner over it and lanterns either side.
		for (int x = 10; x <= 12; x++) set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false), x, 3, 9);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.YELLOW), 11, 4, 9, Direction.SOUTH);
		for (int x : new int[]{9, 13}) { set(level, bb, s(Blocks.COBBLESTONE_WALL), x, 1, 9); set(level, bb, s(Blocks.LANTERN), x, 2, 9); }
		// A hoist beam out over the yard, a bale on its chain.
		fill(level, bb, 15, 11, 15, 16, 11, 15, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(BlockStateProperties.AXIS, Direction.Axis.X));
		set(level, bb, s(Blocks.IRON_CHAIN), 16, 10, 15);
		set(level, bb, s(Blocks.HAY_BLOCK), 16, 9, 15);
		// Lantern posts up the path, a lily on the pond.
		for (int x : new int[]{9, 13}) for (int z : new int[]{2, 6}) {
			fill(level, bb, x, 1, z, x, 2, z, post);
			set(level, bb, s(Blocks.LANTERN), x, 3, z);
		}
		set(level, bb, s(Blocks.LILY_PAD), 4, 1, 10);
		// A crank housing and a little roof on the well head.
		set(level, bb, s(Blocks.COBBLESTONE_WALL), 18, 2, 10);
		set(level, bb, post, 18, 3, 10);
		set(level, bb, s(Blocks.SPRUCE_PLANKS), 18, 4, 10);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false), 17, 4, 10);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 19, 4, 10);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false), 18, 4, 9);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false), 18, 4, 11);
		set(level, bb, deck, 18, 5, 10);
		// Flowering hedges round the outer edges of both fields; the inner sides stay open.
		for (int x = 0; x <= 22; x++) {
			if (x >= 8 && x <= 14) continue;
			if (x != 18) set(level, bb, leaves(noise(x, 5) < 40 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), x, 1, 5);
			if (x != 7 && x != 15) set(level, bb, leaves(noise(x, 15) < 40 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), x, 1, 15);
		}
		for (int z = 6; z <= 14; z++) {
			set(level, bb, leaves(noise(0, z) < 40 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), 0, 1, z);
			set(level, bb, leaves(noise(22, z) < 40 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), 22, 1, z);
		}
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
		// Lattice bars along the outer edge of each sail, a boss on the hub.
		fill(level, bb, 13, 13, 8, 13, 17, 8, arm);
		fill(level, bb, 12, 12, 8, 13, 12, 8, arm);
		fill(level, bb, 13, 8, 8, 18, 8, 8, arm);
		fill(level, bb, 12, 8, 8, 12, 9, 8, arm);
		fill(level, bb, 9, 4, 8, 9, 8, 8, arm);
		fill(level, bb, 9, 9, 8, 10, 9, 8, arm);
		fill(level, bb, 4, 12, 8, 9, 12, 8, arm);
		fill(level, bb, 10, 11, 8, 10, 12, 8, arm);
		set(level, bb, s(Blocks.DARK_OAK_PLANKS), 11, 10, 7);
		// Sacks of the last harvest by the door.
		set(level, bb, s(Blocks.HAY_BLOCK), 15, 1, 16);
		set(level, bb, s(Blocks.HAY_BLOCK), 16, 1, 16);
		set(level, bb, s(Blocks.HAY_BLOCK), 15, 2, 16);
		set(level, bb, s(Blocks.COMPOSTER), 7, 1, 16);
	}
}
