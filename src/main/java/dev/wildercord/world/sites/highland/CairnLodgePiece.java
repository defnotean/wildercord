package dev.wildercord.world.sites.highland;

import dev.wildercord.wildlife.HighlandContent;
import dev.wildercord.wildlife.WindreedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Highland Cairn Lodge: a Marchkeeper stone-and-timber ridge shelter built into the lee of a dry-stone wind-break
 * wall in the Windscar Highlands. Holds a cultivated Windreed terrace open to the sky, a ridge cairn beacon with
 * a shuttered lamp and lever, a lectern with The Paired Cairns, a supply chest, and a persistent Cairn Keeper.
 */
public final class CairnLodgePiece extends HighlandPiece {
	public static final int DECK = 2;
	public static final int LAMP_X = 16, LAMP_Y = DECK + 4, LAMP_Z = 5;
	public static final int LEVER_X = 16, LEVER_Y = DECK + 4, LEVER_Z = 6;
	private boolean residentPlaced;

	public CairnLodgePiece(int x, int y, int z, Direction facing) {
		super(HighlandSites.LODGE_PIECE, x, y, z, 21, 12, 21, facing);
	}
	public CairnLodgePiece(CompoundTag tag) {
		super(HighlandSites.LODGE_PIECE, tag);
		residentPlaced = tag.getBooleanOr("CairnKeeperPlaced", false);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext ctx, CompoundTag tag) {
		super.addAdditionalSaveData(ctx, tag);
		tag.putBoolean("CairnKeeperPlaced", residentPlaced);
	}

	public BlockPos lampPos() { return world(LAMP_X, LAMP_Y, LAMP_Z); }
	public BlockPos leverPos() { return world(LEVER_X, LEVER_Y, LEVER_Z); }

	private static boolean furniture(int x, int y, int z) {
		return (y == DECK + 1 && ((x == 12 && (z == 9 || z == 12)) || (x == 11 && z == 7)))
			|| (x == LAMP_X && y == LAMP_Y && z == LAMP_Z)
			|| (x == LEVER_X && y == LEVER_Y && z == LEVER_Z);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
		for (int x = 0; x <= 20; x++) for (int y = DECK + 1; y <= 11; y++) for (int z = 0; z <= 20; z++) {
			if (!furniture(x, y, z)) set(level, clip, AIR, x, y, z);
		}
		// Broad dry-stone shelf and leeward courtyard footing.
		footing(level, clip, 2, 2, 18, 18, DECK - 1);
		for (int x = 2; x <= 18; x++) for (int z = 2; z <= 18; z++) {
			int n = noise(x, DECK, z);
			BlockState floor = n < 20 ? MOSSY_COBBLE : n < 55 ? Blocks.TUFF.defaultBlockState() : ANDESITE;
			set(level, clip, floor, x, DECK, z);
		}
		// Stepped approach path on the south side so travellers can walk up from the shelf.
		for (int x = 8; x <= 11; x++) {
			footing(level, clip, x, 0, x, 1, DECK - 1);
			set(level, clip, COBBLE, x, DECK - 1, 0);
			set(level, clip, Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), x, DECK, 1);
		}
		// Windward dry-stone wind-break wall along the north and west edge, with the Scout's carved wall-turn mark.
		for (int x = 2; x <= 18; x++) {
			for (int y = DECK + 1; y <= DECK + 3; y++) {
				int n = noise(x, y, 2);
				set(level, clip, n < 22 ? MOSSY_BRICK : n < 48 ? CRACKED_BRICK : BRICK, x, y, 2);
			}
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), x, DECK + 4, 2);
		}
		set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 9, DECK + 2, 2);
		set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 11, DECK + 2, 2);

		// Cultivated leeward Windreed terrace on the west side (x = 3..5, z = 5..12), open to the sky for natural regrowth.
		for (int x = 3; x <= 5; x++) for (int z = 5; z <= 12; z++) {
			set(level, clip, (x + z & 1) == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState(), x, DECK, z);
			if (x == 3 || x == 5) set(level, clip, Blocks.COBBLESTONE_WALL.defaultBlockState(), x, DECK + 1, z == 5 || z == 12 ? z : -1);
		}
		for (int z : new int[]{6, 8, 10, 11}) {
			set(level, clip, Blocks.GRASS_BLOCK.defaultBlockState(), 4, DECK, z);
			set(level, clip, HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE, z == 8 ? 1 : 2), 4, DECK + 1, z);
		}
		for (int z : new int[]{7, 9}) {
			set(level, clip, Blocks.GRASS_BLOCK.defaultBlockState(), 3, DECK, z);
			set(level, clip, HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE, 2), 3, DECK + 1, z);
		}

		// The Cairn Lodge cabin (x = 6..13, z = 6..14), framed in spruce logs and stone brick wainscot.
		fill(level, clip, 6, DECK, 6, 13, DECK, 14, SPRUCE);
		for (int y = DECK + 1; y <= DECK + 4; y++) {
			for (int x = 6; x <= 13; x++) for (int z = 6; z <= 14; z++) {
				if (x > 6 && x < 13 && z > 6 && z < 14) continue;
				boolean corner = (x == 6 || x == 13) && (z == 6 || z == 14);
				BlockState wall = corner ? Blocks.SPRUCE_LOG.defaultBlockState() : y == DECK + 1 ? BRICK : SPRUCE;
				set(level, clip, wall, x, y, z);
			}
		}
		// Windows and south door.
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 6, DECK + 2, 9);
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 6, DECK + 2, 11);
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 13, DECK + 2, 9);
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 9, DECK + 2, 6);
		door(level, clip, Blocks.SPRUCE_DOOR, 9, DECK + 1, 14, Direction.NORTH);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, DECK + 3, 15);

		// Gabled deepslate-tile and spruce roof.
		for (int z = 5; z <= 15; z++) {
			set(level, clip, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 5, DECK + 4, z);
			set(level, clip, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 14, DECK + 4, z);
			set(level, clip, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 6, DECK + 5, z);
			set(level, clip, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 13, DECK + 5, z);
			fill(level, clip, 7, DECK + 5, z, 12, DECK + 5, z, SPRUCE);
			set(level, clip, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 7, DECK + 6, z);
			set(level, clip, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 12, DECK + 6, z);
			fill(level, clip, 8, DECK + 6, z, 11, DECK + 6, z, Blocks.DEEPSLATE_TILES.defaultBlockState());
		}

		// Interior hearth, bed, cartography bench, supply chest, and lectern with The Paired Cairns.
		fill(level, clip, 10, DECK + 1, 6, 12, DECK + 6, 6, BRICK);
		set(level, clip, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 11, DECK + 1, 7);
		set(level, clip, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.TOP), 11, DECK + 2, 7);
		bed(level, clip, Blocks.BED.red(), 7, DECK + 1, 8, Direction.NORTH);
		set(level, clip, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 7, DECK + 1, 11);
		set(level, clip, Blocks.CRAFTING_TABLE.defaultBlockState(), 7, DECK + 1, 12);
		chest(level, clip, 12, DECK + 1, 12, Direction.WEST, HighlandSites.loot(HighlandSites.CAIRN_LODGE), 19L);
		lectern(level, clip, 12, DECK + 1, 9, Direction.WEST,
			HighlandSites.book("cairn_keeper", "The Paired Cairns", "Marchkeeper Survey"));
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, DECK + 4, 10);

		// Paired dry-stone cairns on the eastern ridge shelf + the shuttered Cairn Beacon Lamp and lever.
		for (int y = DECK + 1; y <= DECK + 3; y++) {
			set(level, clip, y == DECK + 2 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : BRICK, LAMP_X, y, LAMP_Z);
			set(level, clip, COBBLE, 16, y - 1, 9);
		}
		set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), 16, DECK + 3, 9);
		set(level, clip, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, false), LAMP_X, LAMP_Y, LAMP_Z);
		set(level, clip, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), LAMP_X, LAMP_Y + 1, LAMP_Z);
		set(level, clip, Blocks.LEVER.defaultBlockState()
			.setValue(LeverBlock.FACE, AttachFace.WALL)
			.setValue(LeverBlock.FACING, Direction.SOUTH)
			.setValue(LeverBlock.POWERED, false), LEVER_X, LEVER_Y, LEVER_Z);

		var home = localPosition(9, DECK + 1, 11);
		if (!residentPlaced && clip.isInside(home)) residentPlaced = CairnKeeperResident.spawn(level, home);
	}
}
