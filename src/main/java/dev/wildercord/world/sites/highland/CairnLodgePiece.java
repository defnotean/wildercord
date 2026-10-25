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
		super(HighlandSites.LODGE_PIECE, x, y, z, 21, 15, 21, facing);
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
		for (int x = 0; x <= 20; x++) for (int y = DECK + 1; y <= 14; y++) for (int z = 0; z <= 20; z++) {
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
		// Buttresses on the lee face, tall cairn piers at the wall ends, and a gateway through to the approach steps.
		for (int x : new int[]{4, 8, 12, 16}) {
			fill(level, clip, x, DECK + 1, 3, x, DECK + 2, 3, BRICK);
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false), x, DECK + 3, 3);
		}
		for (int x : new int[]{2, 18}) {
			for (int y = DECK + 1; y <= DECK + 5; y++) set(level, clip, noise(x, y, 2) < 40 ? MOSSY_COBBLE : COBBLE, x, y, 2);
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), x, DECK + 6, 2);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 7, 2);
		}
		fill(level, clip, 9, DECK + 1, 2, 10, DECK + 2, 2, AIR);
		for (int x : new int[]{8, 11}) {
			fill(level, clip, x, DECK + 1, 2, x, DECK + 4, 2, Blocks.SPRUCE_LOG.defaultBlockState());
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 5, 2);
		}
		fill(level, clip, 9, DECK + 3, 2, 10, DECK + 3, 2, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, clip, 9, DECK + 4, 2, 10, DECK + 4, 2, Blocks.STONE_BRICK_WALL.defaultBlockState());
		fill(level, clip, 9, DECK, 2, 10, DECK, 2, COBBLE);
		// A worn path from the gateway round the cabin's east side to its door.
		fill(level, clip, 9, DECK, 3, 15, DECK, 4, Blocks.DIRT_PATH.defaultBlockState());
		fill(level, clip, 15, DECK, 5, 15, DECK, 16, Blocks.DIRT_PATH.defaultBlockState());
		fill(level, clip, 9, DECK, 15, 14, DECK, 15, Blocks.DIRT_PATH.defaultBlockState());

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
		// Log mid posts and a log wall plate over the plank walls.
		for (int y = DECK + 2; y <= DECK + 3; y++) {
			set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState(), 6, y, 10);
			set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState(), 13, y, 11);
			set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState(), 7, y, 14);
			set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState(), 11, y, 14);
		}
		fill(level, clip, 7, DECK + 4, 6, 12, DECK + 4, 6, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, clip, 7, DECK + 4, 14, 12, DECK + 4, 14, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, clip, 6, DECK + 4, 7, 6, DECK + 4, 13, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		fill(level, clip, 13, DECK + 4, 7, 13, DECK + 4, 13, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		// Stone sills and hoods at the windows, spruce brackets under the eaves.
		for (int z : new int[]{9, 11}) {
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 5, DECK + 1, z);
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 5, DECK + 3, z);
		}
		set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 14, DECK + 1, 9);
		set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 14, DECK + 3, 9);
		set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), 9, DECK + 1, 5);
		set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), 9, DECK + 3, 5);
		for (int z : new int[]{6, 14}) set(level, clip, st(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 5, DECK + 3, z);
		for (int z : new int[]{6, 11, 14}) set(level, clip, st(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 14, DECK + 3, z);
		// Windows and south door.
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 6, DECK + 2, 9);
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 6, DECK + 2, 11);
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 13, DECK + 2, 9);
		set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), 9, DECK + 2, 6);
		door(level, clip, Blocks.SPRUCE_DOOR, 9, DECK + 1, 14, Direction.NORTH);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, DECK + 3, 15);
		set(level, clip, Blocks.SPRUCE_FENCE.defaultBlockState(), 9, DECK + 4, 15);

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

		// A tile ridge along the roof, and wall finials at the gable ends.
		fill(level, clip, 9, DECK + 7, 5, 10, DECK + 7, 15, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
		for (int z : new int[]{5, 15}) {
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), 9, DECK + 8, z);
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), 10, DECK + 8, z);
		}

		// Interior hearth, bed, cartography bench, supply chest, and lectern with The Paired Cairns.
		fill(level, clip, 10, DECK + 1, 6, 12, DECK + 6, 6, BRICK);
		set(level, clip, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 11, DECK + 1, 7);
		set(level, clip, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.TOP), 11, DECK + 2, 7);
		// The hearth carried up through the roof as a corbelled fieldstone chimney, a banked fire smoking on top.
		for (int x = 10; x <= 12; x++) for (int z = 5; z <= 6; z++) for (int y = DECK + 6; y <= DECK + 9; y++) {
			set(level, clip, noise(x, y, z) < 30 ? MOSSY_COBBLE : noise(x, y, z) < 70 ? COBBLE : BRICK, x, y, z);
		}
		for (int x = 10; x <= 12; x++) {
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), x, DECK + 9, 4);
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), x, DECK + 9, 7);
		}
		for (int z = 5; z <= 6; z++) {
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 9, DECK + 9, z);
			set(level, clip, st(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 13, DECK + 9, z);
		}
		for (int x : new int[]{10, 12}) for (int z = 5; z <= 6; z++) set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), x, DECK + 10, z);
		set(level, clip, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 11, DECK + 10, 5);
		bed(level, clip, Blocks.BED.red(), 7, DECK + 1, 8, Direction.NORTH);
		set(level, clip, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 7, DECK + 1, 11);
		set(level, clip, Blocks.CRAFTING_TABLE.defaultBlockState(), 7, DECK + 1, 12);
		chest(level, clip, 12, DECK + 1, 12, Direction.WEST, HighlandSites.loot(HighlandSites.CAIRN_LODGE), 19L);
		lectern(level, clip, 12, DECK + 1, 9, Direction.WEST,
			HighlandSites.book("cairn_keeper", "The Paired Cairns", "Marchkeeper Survey"));
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, DECK + 4, 10);
		// Stores and light: barrels by the bench and chest, candles on the map table.
		set(level, clip, Blocks.BARREL.defaultBlockState(), 7, DECK + 1, 13);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 12, DECK + 1, 13);
		set(level, clip, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), 7, DECK + 2, 12);

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

		// A stone skirt round the beacon cairn and its partner (left open on the path side), and a post behind the lever for it to hang on.
		set(level, clip, st(Blocks.COBBLESTONE_STAIRS, Direction.WEST, false), LAMP_X + 1, DECK + 1, LAMP_Z);
		set(level, clip, st(Blocks.COBBLESTONE_STAIRS, Direction.NORTH, false), LAMP_X, DECK + 1, LAMP_Z - 1);
		for (int[] c : new int[][]{{17, 9}, {16, 8}, {16, 10}}) set(level, clip, MOSSY_COBBLE, c[0], DECK + 1, c[1]);
		for (int y = DECK + 1; y <= LEVER_Y; y++) set(level, clip, y == LEVER_Y ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : BRICK, LEVER_X, y, LEVER_Z + 1);
		set(level, clip, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), LEVER_X, LEVER_Y + 1, LEVER_Z + 1);
		// Yard life: a roofed woodpile and water butt by the path, barrels at the door, lamp posts in the corners.
		for (int z = 11; z <= 13; z++) {
			fill(level, clip, 17, DECK + 1, z, 17, DECK + 2, z, Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
			set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 16, DECK + 1, z);
			set(level, clip, Blocks.SPRUCE_SLAB.defaultBlockState(), 17, DECK + 3, z);
		}
		set(level, clip, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 16, DECK + 1, 15);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 12, DECK + 1, 16);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 13, DECK + 1, 16);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 13, DECK + 2, 16);
		for (int[] c : new int[][]{{3, 3}, {17, 3}, {3, 17}, {17, 17}}) {
			fill(level, clip, c[0], DECK + 1, c[1], c[0], DECK + 2, c[1], Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 3, c[1]);
		}

		var home = localPosition(9, DECK + 1, 11);
		if (!residentPlaced && clip.isInside(home)) residentPlaced = CairnKeeperResident.spawn(level, home);
	}

	private static BlockState st(Block stairs, Direction facing, boolean top) {
		return stairs.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}
}
