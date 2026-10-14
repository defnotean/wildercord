package dev.wildercord.world.sites.emberwood;

import dev.wildercord.wildlife.CinderFernBlock;
import dev.wildercord.wildlife.EmberContent;
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
 * Emberwood Charcoal Yard: a brick-and-timber burner's yard in the Emberwood Reach.
 * Holds a cultivated Cinder Fern ash bed along a coarse-dirt firebreak, a kiln draft lamp and lever,
 * a lectern with Eleven Winters at the Gate, a supply chest, and a persistent Kiln Keeper resident.
 */
public final class CharcoalYardPiece extends EmberwoodPiece {
	public static final int DECK = 2;
	public static final int LAMP_X = 16, LAMP_Y = DECK + 4, LAMP_Z = 5;
	public static final int LEVER_X = 16, LEVER_Y = DECK + 4, LEVER_Z = 6;
	private boolean residentPlaced;

	public CharcoalYardPiece(int x, int y, int z, Direction facing) {
		super(EmberwoodSites.YARD_PIECE, x, y, z, 21, 12, 21, facing);
	}
	public CharcoalYardPiece(CompoundTag tag) {
		super(EmberwoodSites.YARD_PIECE, tag);
		residentPlaced = tag.getBooleanOr("KilnKeeperPlaced", false);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext ctx, CompoundTag tag) {
		super.addAdditionalSaveData(ctx, tag);
		tag.putBoolean("KilnKeeperPlaced", residentPlaced);
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
		// Broad mud-brick and coarse-dirt firebreak yard footing.
		footing(level, clip, 2, 2, 18, 18, DECK - 1);
		for (int x = 2; x <= 18; x++) for (int z = 2; z <= 18; z++) {
			boolean firebreak = x == 2 || x == 18 || z == 2 || z == 18;
			int n = noise(x, DECK, z);
			BlockState floor = firebreak ? COARSE_DIRT : (n < 28 ? MUD_BRICKS : n < 62 ? TERRACOTTA : STONE_BRICK);
			set(level, clip, floor, x, DECK, z);
		}
		// Stepped approach path on the south side.
		for (int x = 8; x <= 11; x++) {
			footing(level, clip, x, 0, x, 1, DECK - 1);
			set(level, clip, MUD_BRICKS, x, DECK - 1, 0);
			set(level, clip, Blocks.MUD_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH), x, DECK, 1);
		}
		// North brick wind-and-spark screen wall.
		for (int x = 2; x <= 18; x++) {
			for (int y = DECK + 1; y <= DECK + 3; y++) {
				int n = noise(x, y, 2);
				set(level, clip, n < 28 ? MUD_BRICKS : n < 56 ? CRACKED_BRICK : BRICKS, x, y, 2);
			}
			set(level, clip, Blocks.MUD_BRICK_WALL.defaultBlockState(), x, DECK + 4, 2);
		}
		set(level, clip, CHISELED_BRICK, 9, DECK + 2, 2);
		set(level, clip, CHISELED_BRICK, 11, DECK + 2, 2);

		// Cultivated Cinder Fern ash bed on the west side (x = 3..5, z = 5..12), open to the sky for natural regrowth.
		for (int x = 3; x <= 5; x++) for (int z = 5; z <= 12; z++) {
			set(level, clip, (x + z & 1) == 0 ? PODZOL : COARSE_DIRT, x, DECK, z);
		}
		for (int z : new int[]{6, 8, 10, 11}) {
			set(level, clip, PODZOL, 4, DECK, z);
			set(level, clip, EmberContent.FERN.defaultBlockState()
				.setValue(CinderFernBlock.AGE, z == 8 ? 1 : 2)
				.setValue(CinderFernBlock.COOLED, true), 4, DECK + 1, z);
		}
		for (int z : new int[]{7, 9}) {
			set(level, clip, COARSE_DIRT, 3, DECK, z);
			set(level, clip, EmberContent.FERN.defaultBlockState()
				.setValue(CinderFernBlock.AGE, 2)
				.setValue(CinderFernBlock.COOLED, true), 3, DECK + 1, z);
		}

		// The Charcoal Burner's Shed (x = 6..13, z = 6..14), framed in stripped dark-oak logs and brick wainscot.
		fill(level, clip, 6, DECK, 6, 13, DECK, 14, DARK_OAK);
		for (int y = DECK + 1; y <= DECK + 4; y++) {
			for (int x = 6; x <= 13; x++) for (int z = 6; z <= 14; z++) {
				if (x > 6 && x < 13 && z > 6 && z < 14) continue;
				boolean corner = (x == 6 || x == 13) && (z == 6 || z == 14);
				BlockState wall = corner ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : y == DECK + 1 ? BRICKS : MUD_BRICKS;
				set(level, clip, wall, x, y, z);
			}
		}
		// Windows and south door.
		set(level, clip, Blocks.IRON_BARS.defaultBlockState(), 6, DECK + 2, 9);
		set(level, clip, Blocks.IRON_BARS.defaultBlockState(), 6, DECK + 2, 11);
		set(level, clip, Blocks.IRON_BARS.defaultBlockState(), 13, DECK + 2, 9);
		set(level, clip, Blocks.IRON_BARS.defaultBlockState(), 9, DECK + 2, 6);
		door(level, clip, Blocks.DARK_OAK_DOOR, 9, DECK + 1, 14, Direction.NORTH);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, DECK + 3, 15);

		// Gabled brick-and-mud-brick roof.
		for (int z = 5; z <= 15; z++) {
			set(level, clip, Blocks.BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 5, DECK + 4, z);
			set(level, clip, Blocks.BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 14, DECK + 4, z);
			set(level, clip, Blocks.BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 6, DECK + 5, z);
			set(level, clip, Blocks.BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 13, DECK + 5, z);
			fill(level, clip, 7, DECK + 5, z, 12, DECK + 5, z, DARK_OAK);
			set(level, clip, Blocks.MUD_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 7, DECK + 6, z);
			set(level, clip, Blocks.MUD_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST), 12, DECK + 6, z);
			fill(level, clip, 8, DECK + 6, z, 11, DECK + 6, z, MUD_BRICKS);
		}

		// Interior charcoal kiln, orange bed, smithing table, supply chest, and lectern with Eleven Winters at the Gate.
		fill(level, clip, 10, DECK + 1, 6, 12, DECK + 6, 6, BRICKS);
		set(level, clip, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 11, DECK + 1, 7);
		set(level, clip, Blocks.BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.TOP), 11, DECK + 2, 7);
		bed(level, clip, Blocks.BED.orange(), 7, DECK + 1, 8, Direction.NORTH);
		set(level, clip, Blocks.SMITHING_TABLE.defaultBlockState(), 7, DECK + 1, 11);
		set(level, clip, Blocks.CRAFTING_TABLE.defaultBlockState(), 7, DECK + 1, 12);
		chest(level, clip, 12, DECK + 1, 12, Direction.WEST, EmberwoodSites.loot(EmberwoodSites.CHARCOAL_YARD), 37L);
		lectern(level, clip, 12, DECK + 1, 9, Direction.WEST,
			EmberwoodSites.book("emberwood_yard", "Eleven Winters at the Gate", "Ashwin, Kiln Keeper"));
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 9, DECK + 4, 10);

		// Eastern Kiln Draft Lamp stack + shuttered Redstone Lamp and Lever, plus stacked charcoal/oak pile.
		for (int y = DECK + 1; y <= DECK + 3; y++) {
			set(level, clip, y == DECK + 2 ? COPPER : BRICKS, LAMP_X, y, LAMP_Z);
			set(level, clip, Blocks.COAL_BLOCK.defaultBlockState(), 16, y - 1, 9);
		}
		set(level, clip, Blocks.MUD_BRICK_WALL.defaultBlockState(), 16, DECK + 3, 9);
		if (!level.getBlockState(lampPos()).is(Blocks.REDSTONE_LAMP)) {
			set(level, clip, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, false), LAMP_X, LAMP_Y, LAMP_Z);
		}
		set(level, clip, Blocks.BRICK_SLAB.defaultBlockState(), LAMP_X, LAMP_Y + 1, LAMP_Z);
		if (!level.getBlockState(leverPos()).is(Blocks.LEVER)) {
			set(level, clip, Blocks.LEVER.defaultBlockState()
				.setValue(LeverBlock.FACE, AttachFace.WALL)
				.setValue(LeverBlock.FACING, Direction.SOUTH)
				.setValue(LeverBlock.POWERED, false), LEVER_X, LEVER_Y, LEVER_Z);
		}

		var home = localPosition(9, DECK + 1, 11);
		if (!residentPlaced && clip.isInside(home)) residentPlaced = KilnKeeperResident.spawn(level, home);
	}
}
