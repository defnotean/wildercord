package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A crossroads inn: two gravel roads meet at its door. Inside, a lit hearth, tables, three beds to sleep in, the
 * guest ledger on a lectern (leads to the other roadside sites) and the pantry chest. Notices hang beside the door.
 */
public final class WayfarerInnPiece extends TravelPiece {
	public WayfarerInnPiece(int x, int y, int z, Direction facing) { super(TravelSites.INN_PIECE, x, y, z, 23, 15, 23, facing); }
	public WayfarerInnPiece(CompoundTag tag) { super(TravelSites.INN_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState planks = b(Blocks.SPRUCE_PLANKS), log = b(Blocks.STRIPPED_SPRUCE_LOG), stone = b(Blocks.STONE_BRICKS);
		// The crossroads: one road across the front, one up to the door.
		for (int x = 0; x <= 22; x++) for (int z = 1; z <= 3; z++) footing(level, bb, x, z, x, z, noise(x, z) < 25 ? b(Blocks.COARSE_DIRT) : b(Blocks.GRAVEL), DIRT, 3);
		for (int z = 0; z <= 5; z++) footing(level, bb, 10, z, 12, z, z == 0 || noise(11, z) < 25 ? b(Blocks.COARSE_DIRT) : b(Blocks.GRAVEL), DIRT, 3);
		for (int x : new int[]{8, 14}) { footing(level, bb, x, 4, x, 4, GRASS, DIRT, 0); fill(level, bb, x, 1, 4, x, 2, 4, b(Blocks.SPRUCE_FENCE)); set(level, bb, b(Blocks.LANTERN), x, 3, 4); }
		// The house: stone footing, spruce walls, a gable roof with a chimney.
		footing(level, bb, 4, 6, 18, 18, stone, COBBLE, 14);
		room(level, bb, 4, 0, 6, 18, 5, 18, planks);
		fill(level, bb, 4, 0, 6, 18, 0, 18, stone);
		fill(level, bb, 5, 0, 7, 17, 0, 17, b(Blocks.SPRUCE_PLANKS));
		for (int[] c : new int[][]{{4, 6}, {18, 6}, {4, 18}, {18, 18}}) fill(level, bb, c[0], 1, c[1], c[0], 4, c[1], log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
		fill(level, bb, 4, 5, 6, 18, 5, 6, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, bb, 4, 5, 18, 18, 5, 18, log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		gable(level, bb, 4, 18, 6, 18, 5, Blocks.DARK_OAK_STAIRS, b(Blocks.DARK_OAK_PLANKS), planks);
		for (int x : new int[]{7, 15}) fill(level, bb, x, 2, 6, x, 3, 6, b(Blocks.GLASS_PANE));
		for (int z : new int[]{9, 15}) { fill(level, bb, 4, 2, z, 4, 3, z, b(Blocks.GLASS_PANE)); fill(level, bb, 18, 2, z, 18, 3, z, b(Blocks.GLASS_PANE)); }
		door(level, bb, Blocks.SPRUCE_DOOR, 11, 1, 6, Direction.NORTH);
		// The hearth on the back wall; its chimney rises through the ridge.
		fill(level, bb, 10, 1, 17, 12, 3, 17, COBBLE);
		fill(level, bb, 11, 1, 18, 11, 13, 18, COBBLE);
		set(level, bb, b(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.FACING, Direction.SOUTH), 11, 1, 16);
		set(level, bb, AIR, 11, 1, 17);
		fill(level, bb, 10, 1, 16, 10, 1, 16, b(Blocks.COBBLESTONE_WALL));
		fill(level, bb, 12, 1, 16, 12, 1, 16, b(Blocks.COBBLESTONE_WALL));
		// Tables and benches, barrels along the west wall, beds along the east wall.
		for (int[] t : new int[][]{{7, 10}, {7, 14}, {11, 12}}) {
			set(level, bb, b(Blocks.SPRUCE_FENCE), t[0], 1, t[1]);
			set(level, bb, b(Blocks.SPRUCE_PRESSURE_PLATE), t[0], 2, t[1]);
			set(level, bb, b(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.EAST), t[0] - 1, 1, t[1]);
			set(level, bb, b(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.WEST), t[0] + 1, 1, t[1]);
		}
		for (int z = 8; z <= 16; z += 2) set(level, bb, b(Blocks.BARREL), 5, 1, z);
		Block[] beds = {Blocks.BED.pick(DyeColor.RED), Blocks.BED.pick(DyeColor.GREEN), Blocks.BED.pick(DyeColor.BLUE)};
		for (int i = 0; i < 3; i++) bed(level, bb, beds[(i + variant()) % 3], 16, 1, 8 + i * 3, Direction.EAST);
		for (int[] l : new int[][]{{8, 12}, {14, 12}}) set(level, bb, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), l[0], 4, l[1]);
		lectern(level, bb, 8, 1, 8, Direction.SOUTH, TravelSites.book("travel_ledger", "Guest Ledger", "The Innkeeper"));
		chest(level, bb, 16, 1, 16, TravelSites.loot(TravelSites.INN), Direction.WEST, 1);
		// The notice board outside: notes and a blank map for the road.
		frame(level, bb, 9, 2, 5, Direction.SOUTH, new ItemStack(Items.PAPER));
		frame(level, bb, 13, 2, 5, Direction.SOUTH, new ItemStack(Items.MAP));
		resident(level, bb, EntityTypes.CAT, 9, 1, 13, 6);
	}
}
